# Privacy-Respecting Background Aptabase Telemetry Integration

Integrate asynchronous, privacy-preserving analytics across Mulberry using the official `aptabase-kotlin` SDK (`A-US-2780652101`). Operates with zero UI footprint, zero diagnostic toggles, and silent failure isolation so offline reading and ephemeral vault security remain uninterrupted.

## User Review & Critical Decisions

> [!IMPORTANT]
> The following parameters and architectural behaviors were confirmed during clarification:
> - **Initialization Entry Point**: Centralized within a custom `MulberryApp : Application` class on cold start, automatically hydrating cached student metadata (`userName`, `institution`, `profYear`) from `UserProfileRepository`.
> - **Reading Session Telemetry**: In addition to `book_opened`, the reader lifecycle will track `book_session_completed` with reading duration (seconds) and pages traversed upon closing `ReaderActivity`.
> - **Network Dispatching**: Operates on background IO dispatchers over any active internet connection (Wi-Fi or cellular) without deferral, failing silently without retries or blocking UI when offline.

---

## 1. Overview & Core Concept

- **Zero-UI Background Architecture**: No user-facing analytics switches, opt-in modals, or diagnostic banners. Telemetry functions purely as transparent operational insight for understanding textbook usage patterns, search drop-offs, and curriculum distribution.
- **Privacy & Anonymity**: No PII, passwords, Google Drive auth tokens, or private notes are captured. Only academic profile fields (`user_name`, `institution`/`college`, `prof_year`) and aggregated book/subject interactions are transmitted.
- **Fail-Safe Offline Guarantee**: Telemetry calls run on a detached CoroutineScope with `Dispatchers.IO` and try-catch guards. Network timeouts, airplane mode, or server failures will never crash the app or stall PDF rendering.

---

## 2. User Experience & Visual Design

- **Visual Footprint**: Strictly zero frontend changes. No settings toggles, toasts, or indicators.
- **Performance Budget**: Zero impact on main thread (0ms frame drops during scrolling, book opening, or searching).
- **Graceful Offline Mode**: Medical students studying in basements, dissection halls, or airplane mode experience zero difference in UX or latency.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Application-Level vs Activity-Level Lifecycle**:
  - *Chosen Approach*: Initialize in `MulberryApp : Application` before any Activity renders.
  - *Why*: Ensures background receivers, deep-links, and cold launches have an active analytics context without race conditions or duplicated init calls.
  - *Alternatives Considered*: `MainActivity.onCreate()`—rejected because `ReaderActivity` can be launched independently or resumed after process death.

- **Decision 2: Reading Session Tracking**:
  - *Chosen Approach*: Record `book_opened` upon initial decrypt and `book_session_completed` on `ReaderActivity.onDestroy()` or back navigation.
  - *Why*: Provides vital insight into whether textbooks are actively read or immediately closed due to formatting/loading errors.

- **Decision 3: Standalone Decoupled Helper**:
  - *Chosen Approach*: Package as `com.mulberry.app.analytics.MulberryAnalytics` with typed helper methods.
  - *Why*: Keeps third-party SDK dependencies strictly quarantined; if Aptabase is swapped or disabled, only one file changes.

---

## 4. Technical Architecture & Data Strategy

### System Component & Telemetry Pipeline

```
┌────────────────────────────────────────────────────────────────────────┐
│                          Mulberry Application                          │
├────────────────────────────────────────────────────────────────────────┤
│                                                                        │
│   [Cold Start] ────────> MulberryApp.onCreate()                       │
│                                 │                                      │
│                                 ▼                                      │
│                     MulberryAnalytics.init()                           │
│                     UserProfileRepository.load() ──> setUserContext()  │
│                                                                        │
│   [User Interaction Flow]                                              │
│   ┌────────────────────┐   ┌────────────────────┐   ┌───────────────┐ │
│   │ Setup / Edit Prof  │   │ Catalog & Search   │   │ PDF Reader    │ │
│   └─────────┬──────────┘   └─────────┬──────────┘   └───────┬───────┘ │
│             │                        │                      │         │
│             ▼                        ▼                      ▼         │
│    logProfileRegistered()       logSearch()           logBookOpened() │
│    logProfPhaseChanged()   logBookDownloadStart/Done()logBookSession()│
│             │                        │                      │         │
│             └────────────────────────┴──────────────────────┘         │
│                                      │                                 │
│                                      ▼                                 │
│                         MulberryAnalytics.track()                     │
│                        (Dispatchers.IO + Silent)                       │
│                                      │                                 │
└──────────────────────────────────────┼─────────────────────────────────┘
                                       │ HTTPS Event
                                       ▼
                     ┌───────────────────────────────────┐
                     │ Aptabase Ingest (Region: US)      │
                     │ App Key: A-US-2780652101          │
                     └───────────────────────────────────┘
```

### Event Payload Schema

| Event Name | Trigger Location | Custom Properties |
|---|---|---|
| `app_open` | `MainActivity.onCreate()` | `user_name`, `college`, `prof_year`, `theme` |
| `user_onboarded` | `SetupWizardScreen` completion | `user_name`, `college`, `prof_year` |
| `prof_phase_changed` | `UserProfileBottomSheet` save | `user_name`, `old_prof`, `new_prof` |
| `catalog_search` | Catalog/Search query submit | `query`, `results_count`, `prof_year`, `is_zero_result` |
| `book_download_started` | Catalog download initiate | `user_name`, `book_id`, `book_title`, `subject`, `size_mb` |
| `book_download_completed` | Catalog download finish | `user_name`, `prof_year`, `book_id`, `book_title`, `subject` |
| `book_opened` | ReaderActivity display ready | `user_name`, `college`, `prof_year`, `book_id`, `book_title`, `subject`, `current_page` |
| `book_session_completed`| ReaderActivity exit | `user_name`, `book_id`, `duration_seconds`, `pages_read` |
| `study_goal_created` | Daily schedule goal created | `user_name`, `prof_year`, `subject`, `chapter`, `page_count` |
| `study_goal_completed` | Daily schedule goal check | `user_name`, `prof_year`, `subject`, `chapter` |

### Step-by-Step Implementation Sequence

1. **Gradle Dependency**:
   - Add `implementation("com.github.aptabase:aptabase-kotlin:0.0.7")` to `app/build.gradle.kts`.
   - Verify `maven { url = uri("https://jitpack.io") }` in `settings.gradle.kts` (already present).
2. **`MulberryAnalytics.kt`**:
   - Create `app/src/main/java/com/mulberry/app/analytics/MulberryAnalytics.kt` matching the requested specification.
   - Include `logBookSessionCompleted(bookId, durationSeconds, pagesRead)`.
3. **Application Subclass & Manifest**:
   - Create `app/src/main/java/com/example/MulberryApp.kt` inheriting `android.app.Application`.
   - Initialize `MulberryAnalytics.init(this)` and populate user context from `UserProfileRepository`.
   - Update `AndroidManifest.xml` with `android:name=".MulberryApp"`.
4. **Lifecycle Hooks**:
   - Wire `logAppOpen` in `MainActivity.kt`.
   - Wire `logProfileRegistered` in `SetupWizardScreen.kt`.
   - Wire `logProfPhaseChanged` in `UserProfileBottomSheet.kt`.
   - Wire `logSearch` in Catalog/Search screens.
   - Wire `logBookDownloadStarted` and `logBookDownloadCompleted` in `CatalogSyncEngine.kt` / `BookRepository.kt`.
   - Wire `logBookOpened` and `logBookSessionCompleted` in `ReaderActivity.kt`.
   - Wire `logStudyGoalCreated` and `logStudyGoalCompleted` in `ScheduleViewModel.kt` / `DailyScheduleScreen.kt`.
5. **Compilation & Validation**:
   - Run `compile_applet` and local unit tests (`testDebugUnitTest`).
