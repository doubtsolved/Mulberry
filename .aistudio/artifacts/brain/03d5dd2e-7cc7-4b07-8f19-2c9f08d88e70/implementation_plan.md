# Implementation Plan - Schedule Completion Threshold, In-Progress Bar & Header Typography

Ensure reading schedule sessions are marked completed **only** when the user reads to or beyond the maximum scheduled end page, dynamically preserve and display partial reading progress on session cards, and increase the heading typography for "TODAY'S FOCUS" and "Shelf" to a prominent 20sp bold title style.

---

## 1. Confirmed User Requirements & Design Choices

| Requirement | Specification | Implementation Detail |
| :--- | :--- | :--- |
| **Schedule Completion Threshold** | Mark complete **only** when reading reaches or exceeds scheduled `endPage` (`maxPageReached >= sessionEndPage`). | In `ReaderActivity.kt`, remove `|| isFinishing` from completion check. Closing/exiting before reaching `sessionEndPage` saves partial progress via `updateIncrementalSessionProgress` without flagging `isCompleted = true`. |
| **In-Progress Reading Indicator** | Dynamically fill the progress bar on `TodayFocusSessionCard` as reading proceeds. | Replace hardcoded `0.35f` progress with `(actualPagesRead.toFloat() / totalPages.toFloat()).coerceIn(0f, 1f)`. Display read page count (e.g., `"5 / 20 pages"`) below the bar. |
| **Heading Font Size** | Increase "TODAY'S FOCUS" and "Shelf" heading font size to prominent 20sp bold. | Update `SectionHeader` in `DeskRootScreen.kt` with `fontSize = 20.sp`, `fontWeight = FontWeight.Bold`, `color = colors.textPrimary`, `letterSpacing = (-0.3).sp`, and proportional icon sizing. |

---

## 2. Proposed Changes

### A. Reading Session Completion & Progress Tracking (`ReaderActivity.kt`)
* **File**: `app/src/main/java/com/example/ui/reader/ReaderActivity.kt`
* **Changes**:
  1. In `saveReadingProgress()` (around line 927):
     * Change completion condition from `if (isTargetReached || isFinishing)` to strictly `if (isTargetReached)`.
     * `isTargetReached` evaluates `(sessionEndPage > 0 && maxPageReached >= sessionEndPage)`.
     * If `isTargetReached == false` when exiting or pausing: invoke `ScheduleRepository.updateIncrementalSessionProgress(sessionId, actualReadPages = pagesVisited, actualDurationMinutes = activeMinutes)`.
     * If `isTargetReached == true`: invoke `ScheduleRepository.markCompleted(sessionId, actualReadPages = pagesVisited, actualDurationMinutes = activeMinutes)`.
  2. Compute `pagesVisited` accurately relative to `sessionStartPage`:
     * `val pagesVisited = if (sessionStartPage > 0) (maxPageReached - sessionStartPage + 1).coerceAtLeast(1) else visitedPages.size.coerceAtLeast(1)`

---

### B. Desk Session Card In-Progress Visualizer (`DeskRootScreen.kt`)
* **File**: `app/src/main/java/com/example/ui/screens/desk/DeskRootScreen.kt`
* **Changes**:
  1. In `TodayFocusSessionCard` (lines 1066-1089):
     * Compute actual progress fraction:
       ```kotlin
       val readProgress = if (isCompleted) 1f else {
           (session.actualPagesRead.toFloat() / session.totalPages.toFloat()).coerceIn(0f, 1f)
       }
       ```
     * In the status row below the progress bar:
       * When completed: `"Completed"` (in `colors.primary`)
       * When in-progress with pages read: `"${session.actualPagesRead}/${session.totalPages} pages"` (in `colors.textSecondary`)
       * When not yet started: `"0/${session.totalPages} pages"` or `"Not started"`

---

### C. Prominent Section Headers (`DeskRootScreen.kt`)
* **File**: `app/src/main/java/com/example/ui/screens/desk/DeskRootScreen.kt`
* **Changes**:
  1. In `SectionHeader` (lines 1512-1555):
     * Increase title `fontSize` from `12.sp` to `20.sp`.
     * Update title `fontWeight` to `FontWeight.Bold` and `color` to `colors.textPrimary` (for high visual hierarchy).
     * Adjust title `letterSpacing` to `(-0.3).sp` suitable for modern title typography.
     * Scale accompanying icon (e.g. for Shelf) from `16.dp` to `20.dp` to align with the larger title text.
     * Ensure vertical padding and alignment remain balanced on both mobile and tablet viewport widths.

---

## 3. Verification & Testing

1. **Compilation Check**: Run `compile_applet` to confirm clean compilation with no syntax or type mismatches.
2. **Session Partial Progress Verification**:
   - Create/open a scheduled session (e.g., pages 1–20).
   - Read pages 1 to 5 and exit via back navigation (`isFinishing`).
   - Verify in `DeskRootScreen` that the session remains uncompleted, the progress bar shows 25% (5/20 pages), and sub-label shows `"5/20 pages"`.
3. **Session Completion Verification**:
   - Reopen the session, navigate to or past page 20 (`maxPageReached >= 20`).
   - Exit the reader; verify session is marked complete (`isCompleted = true`), checkmark displays, and progress bar shows 100%.
4. **Header Visual Inspection**:
   - Verify both "TODAY'S FOCUS" and "Shelf" headers render at 20sp bold with crisp typography and matching icon heights.
