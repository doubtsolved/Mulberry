# External PDF Ingestion, VIP Contributor Code & Community Contributions

Enable Mulberry to act as a system PDF viewer with in-reader "Add to Desk" local encryption, add VIP student contributor verification via Cloudflare Worker, and support reliable background uploads of textbooks to the community library via direct Hugging Face LFS S3 uploads with Android WorkManager.

---

### User Review & Critical Decisions

> [!IMPORTANT]
> The following architectural decisions were confirmed during the interactive clarification phase:

- **Confirmed Decision 1 (Upload Execution Model)**: Community textbook contributions will be scheduled and executed via **Android WorkManager** (`CoroutineWorker`) with a persistent progress notification (`ForegroundInfo`), ensuring uploads safely complete even if the user minimizes or closes Mulberry.
- **Confirmed Decision 2 (VIP Contributor Badge Placement)**: The verified VIP Contributor Badge (e.g. *Core Architect*, *Verified Contributor*, or *Scholar*) will be rendered **beside the student's name in the Desk header**.

---

### 1. Overview & Core Concept

- **What It Does**:
  1. **External PDF Handling & Ingestion**: Opens any external PDF from files, WhatsApp, or browsers, previews it instantly in `ReaderActivity`, and displays a non-intrusive floating pill card `[External PDF] <Title> • [+ Add to Desk]`. When tapped, it displays a metadata modal, creates an AES-256-GCM encrypted replica in `/sdcard/Mulberry/<slug>.bin`, generates a 150 DPI WebP cover, writes to `/sdcard/Mulberry/downloaded_external.json`, and registers it in Room `BookDao` so it appears in the Desk Shelf.
  2. **VIP Contributor Code Verification**: Adds an expandable access code panel in the Student Profile. Validates codes via `POST https://mulberry-vault.sahabulmondal2023.workers.dev/api/verify-code` and grants tier privileges (`CORE_VIP`, `AMBASSADOR`, `STANDARD`) with dynamic daily upload limits and a custom badge.
  3. **Community Contribution Pipeline**: Automatically computes SHA-256 and file size, checks rate limits via the Cloudflare Worker `/api/contribute/init`, streams large encrypted `.bin` files directly to Hugging Face LFS AWS S3 using presigned upload URLs with an 8 KB buffer, and commits metadata via `/api/contribute/finalize`.

- **Target Audience**: Medical students importing uncataloged lecture notes, college handouts, or rare regional medical textbooks into Mulberry for offline study and sharing them with peers.

---

### 2. User Experience & Visual Design

#### A. External PDF Viewport & Floating "Add to Desk" Banner
```
┌─────────────────────────────────────────────────────────────┐
│ Reader Top Bar:  [< Back]   External Document        [⋮]    │
├─────────────────────────────────────────────────────────────┤
│                                                             │
│                    [ PDF Page Viewport ]                    │
│                                                             │
│  ┌───────────────────────────────────────────────────────┐  │
│  │ 📄 External PDF • BD Chaurasia Anatomy  [+ Add to Desk]│  │
│  └───────────────────────────────────────────────────────┘  │
│  [==================== Scrubber Slider ====================]│
└─────────────────────────────────────────────────────────────┘
```

#### B. "Add to Desk" Bottom Sheet
```
┌─────────────────────────────────────────────────────────────┐
│ Add to Desk                                             [X] │
│                                                             │
│ Book Title                                                  │
│ [ BD Chaurasia Human Anatomy                              ] │
│                                                             │
│ Professional Year                Subject                    │
│ [ 1st Prof                   ▼]  [ Anatomy               ▼] │
│                                                             │
│ Asset Type                                                  │
│ [ Standard Textbook                                       ▼]│
│                                                             │
│ [✓] Contribute to Mulberry Community Library                │
│     Shares encrypted copy with verified medical peers       │
│                                                             │
│ [                  Import & Add to Desk                   ] │
└─────────────────────────────────────────────────────────────┘
```

#### C. Desk Header with VIP Contributor Badge
```
┌─────────────────────────────────────────────────────────────┐
│ [M] Desk                                              ( S ) │
│ Dr. Sahabul  [ ★ Core Architect ]                           │
└─────────────────────────────────────────────────────────────┘
```

#### D. Profile VIP Access Code Section
```
┌─────────────────────────────────────────────────────────────┐
│ Special Contributor Access Code                             │
│ Enter access code from community coordinators               │
│ [ CORE-ARCHITECT-2026                      ] [ Verify Code ]│
│ ✓ Verified: Core Architect (Unlimited Uploads)              │
└─────────────────────────────────────────────────────────────┘
```

---

### 3. Key Product Decisions & Trade-Offs

- **Decision 1: WorkManager vs. In-Activity Coroutines for Uploads**:
  - *Chosen Approach*: `androidx.work.CoroutineWorker` with `setForeground()` notification.
  - *Why*: Large medical textbooks often range from 50 MB to 350 MB. Foreground activity coroutines risk termination when the system reclaims memory or the student switches apps. WorkManager guarantees execution and battery-aware retries.
- **Decision 2: Direct-to-S3 Presigned Streaming vs. Worker Proxying**:
  - *Chosen Approach*: Request presigned URL from Cloudflare Worker, then stream `.bin` straight from disk to AWS S3 using 8 KB buffer chunks with OkHttp `RequestBody`.
  - *Why*: Cloudflare Workers have a 100 MB request payload limit and memory constraints. Direct S3 uploads bypass intermediate proxies and achieve maximum bandwidth.
- **Decision 3: Dual Indexing (Room + `downloaded_external.json`)**:
  - *Chosen Approach*: Store metadata in atomic JSON `/sdcard/Mulberry/downloaded_external.json` AND upsert into Room `BookEntity`.
  - *Why*: Ensures data survives app re-installations while keeping Room as the fast, reactive single source of truth for the Compose UI.

---

### 4. Technical Architecture & Data Strategy

#### System Architecture & Data Flow
```
┌─────────────────────────────────────────────────────────────────────────┐
│                             ReaderActivity                              │
│                                                                         │
│  Intent.ACTION_VIEW (PDF) ──► Copy to external_preview/<sanitized>.pdf   │
│  Check if cataloged / downloaded ──► Show AddToDesk Floating Banner     │
└───────────────┬─────────────────────────────────────────────────────────┘
                │ User clicks "+ Add to Desk"
                ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                          AddToDeskBottomSheet                           │
│                                                                         │
│  1. Compute clean slug: title.toCleanBookTitle().slugify()              │
│  2. Encrypt: VaultCryptoManager.encryptFileToVault() ──► <slug>.bin     │
│  3. Render Cover: Page 0 @ 150 DPI ──► .covers/<slug>.webp              │
│  4. Index: SafeFileWriter.writeAtomically() ──► downloaded_external.json│
│  5. Room Upsert: BookDao.upsertBook(isDownloaded = true, binFilePath)   │
└───────────────┬─────────────────────────────────────────────────────────┘
                │ If "Contribute" checked
                ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                         BookContributionManager                         │
│                                                                         │
│  Enqueues WorkManager: BookContributionWorker                           │
│  1. POST /api/contribute/init (with X-Device-Id, X-Vip-Code)            │
│  2. Direct S3 PUT streaming encrypted .bin (8 KB buffer)                │
│  3. POST /api/contribute/finalize (with Base64 cover + metadata)        │
│  4. Update contributedStatus = "PENDING" in downloaded_external.json    │
└─────────────────────────────────────────────────────────────────────────┘
```

#### Files to Create / Modify:
1. **`app/build.gradle.kts`**:
   - Add WorkManager dependency: `implementation("androidx.work:work-runtime-ktx:2.10.0")`.
2. **`AndroidManifest.xml`**:
   - Register `ACTION_VIEW` intent filter for `ReaderActivity` (`application/pdf`, schemes `content` and `file`, `exported=true`).
3. **`ReaderActivity.kt`**:
   - Handle incoming PDF URI from `intent.data`, resolve filename via `OpenableColumns`, cache to `external_preview`, check duplicate state.
   - Render floating `AddToDeskBanner` and overflow menu item `Add to Desk`.
4. **`AddToDeskBottomSheet.kt` (New)**:
   - Compose sheet with Title, Prof, Subject, Asset Type, and Contribute checkbox.
   - Background encryption, WebP cover generator, `downloaded_external.json` updater, and Room insertion.
5. **`UserProfile.kt` & `UserProfileRepository.kt`**:
   - Add VIP capability fields (`vipCode`, `vipTier`, `vipBadgeLabel`, `dailyUploadLimit`).
   - Persist to preferences, `.profile`, and active vault.
6. **`UserProfileBottomSheet.kt` & `DeskRootScreen.kt`**:
   - Expandable "Special Contributor Access Code" card in profile with verify button and worker verification.
   - Render `VipBadge` beside student's name in `DeskTopAppBar`.
7. **`BookContributionManager.kt` & `BookContributionWorker.kt` (New)**:
   - WorkManager integration for LFS S3 uploads with progress notifications and error handling.
