# Fix HTTP 408 Request Timeout on Book Contribution Finalize

Resolves the `HTTP 408 Request Timeout` occurring on `POST /api/contribute/finalize` after large S3 textbook uploads complete by isolating the contribution API client from stale pooled sockets and forcing a fresh TCP/TLS handshake on finalization.

## User Review & Critical Decisions

> [!IMPORTANT]
> The contribution pipeline performs a short `init` request to the Cloudflare Worker, followed by a multi-minute direct binary upload to S3, and then a `finalize` call back to the Cloudflare Worker (which in turn commits LFS pointers and metadata to Hugging Face Git, taking 15–25 seconds). Reusing an idle pooled socket across the long S3 upload causes Cloudflare edge to drop or time out the connection (`HTTP 408`).

- **Confirmed Decision 1 — Dedicated `contributionHttpClient` with Zero-Idle Connection Pool**: Configure `contributionHttpClient` with `connectTimeout(30, TimeUnit.SECONDS)`, `readTimeout(60, TimeUnit.SECONDS)`, `writeTimeout(60, TimeUnit.SECONDS)`, `retryOnConnectionFailure(true)`, and `connectionPool(ConnectionPool(0, 1, TimeUnit.NANOSECONDS))` so `init` and `finalize` never reuse stale sockets across long S3 uploads.
- **Confirmed Decision 2 — Explicit `Connection: close` Header on Finalize**: Add `.addHeader("Connection", "close")` (alongside `Content-Type`, `X-Device-Id`, and `X-Vip-Code`) and use `.toRequestBody("application/json".toMediaType())` when constructing `finalizeRequest` to guarantee a fresh handshake at the Cloudflare edge.
- **Confirmed Decision 3 — Explicit Finalize Status Verification**: Check `finalizeResponse.isSuccessful` and surface non-2xx responses (including HTTP 408/5xx) as a clear error state rather than silently marking failed finalizations as succeeded.

---

## 1. Overview & Core Concept

- **What It Does**: Eliminates socket reuse timeouts when finalizing community textbook contributions after 100% S3 upload completion.
- **Target Audience / Persona**: Medical students and contributors uploading textbooks to the Mulberry Community Library over variable mobile or Wi-Fi connections.
- **Key Value**: Ensures that once a user's textbook finishes uploading 100% to S3, the quarantine commit to Hugging Face completes reliably without failing at 98% with an `HTTP 408 Request Timeout`.

---

## 2. User Experience & Visual Design

- **Key User Flows**:
  1. User selects a local textbook and initiates a community contribution upload.
  2. The progress banner advances through `Preparing SHA-256 & Vault...` -> `Connecting to Mulberry Cloud...` (`init`) -> `Uploading to Community Library...` (`0%` to `100%` S3 stream).
  3. At `98%` (`Finalizing review submission...`), the worker establishes a brand-new connection to the Cloudflare Worker endpoint (`POST /api/contribute/finalize`) with `Connection: close` and waits up to 60 seconds for the Hugging Face LFS verification and commit.
  4. On HTTP 200, the textbook status transitions to `PENDING` review and displays the completion notification and toast. If finalization fails, the upload banner accurately reports the HTTP status code so the user can retry.
- **Visual Identity & Theme**:
  - Preserves the existing Mulberry contribution progress card, notification progress bar, and Microsoft Fluent iconography without introducing visual regressions.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Separate `contributionHttpClient` (No Socket Reuse) vs. Shared `s3UploadClient`**
  - *Chosen Approach*: Provide a dedicated `contributionHttpClient` configured with `ConnectionPool(0, 1, TimeUnit.NANOSECONDS)` and 30s/60s/60s timeouts for the `init` and `finalize` JSON API calls, while keeping the 300-second write timeout client for streaming large binary payloads to S3.
  - *Why*: Cloudflare edge closes idle HTTP/1.1 & HTTP/2 connections while the client is busy streaming puluhan/hundreds of megabytes to S3. Disabling socket pooling and sending `Connection: close` forces OkHttp to open a clean socket for `finalize`.
  - *Alternatives Considered*: Increasing timeouts on a single shared default `OkHttpClient` was rejected because OkHttp still attempts to reuse the half-closed Cloudflare socket from the earlier `init` call.

---

## 4. Technical Architecture & Data Strategy *(Technical Reference)*

### Architecture & Component Diagram

```
┌─────────────────────────────────────────────────────────────────────────┐
│                        BookContributionManager                          │
│  • Holds ContributionUploadState (Idle / Uploading / Success / Error)   │
│  • Provides contributionHttpClient:                                     │
│      - connectTimeout = 30s, readTimeout = 60s, writeTimeout = 60s      │
│      - retryOnConnectionFailure = true                                  │
│      - connectionPool = ConnectionPool(0, 1, TimeUnit.NANOSECONDS)      │
└───────────────────────────────────┬─────────────────────────────────────┘
                                    │ Enqueues & Supplies Client
                                    ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                        BookContributionWorker                           │
│                                                                         │
│  1. POST /api/contribute/init      ──► via contributionHttpClient       │
│  2. PUT  <presigned S3 URL>        ──► via s3UploadHttpClient (300s)    │
│  3. POST /api/contribute/finalize  ──► via contributionHttpClient       │
│       Headers:                                                          │
│         • Content-Type: application/json                                │
│         • Connection: close                                             │
│         • X-Device-Id: <deviceId>                                       │
│         • X-Vip-Code: <vipCode>                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

### Interactive Component & State Mapping
- **Init Stage (`10%`)**: Executes `initRequest` using `contributionHttpClient` with `Connection: close`. Handles HTTP 429 daily quota errors and non-200 responses cleanly.
- **S3 Upload Stage (`10% -> 98%`)**: Streams 8 KB chunks to S3 with throttled 250 ms UI state updates.
- **Finalize Stage (`98% -> 100%`)**: Builds `finalizeRequest` with `Connection: close` and `finalizeJsonBody.toRequestBody("application/json".toMediaType())`, executes via `contributionHttpClient`, verifies HTTP success before marking `contributedStatus = "PENDING"`, and updates `ContributionUploadState.Success`.
