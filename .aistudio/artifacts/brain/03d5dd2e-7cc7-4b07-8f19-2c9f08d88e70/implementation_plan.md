# Desk & Library Refinement Plan

Refine the Desk dashboard header and layout order, compact the Study Planner cards with a present-date scroll clamp, bind My Library directly to `/sdcard/Mulberry/downloaded_books.json`, implement dynamic subject-based filtering for type folders, establish functional custom collections with dedicated material views and JSON persistence, and constrain HorizontalPager gesture switching to the root Desk Home.

## User Review & Critical Decisions

> [!IMPORTANT]
> The following confirmed user choices govern this refinement:
> - **Desk Page Header & Layout**: Header displays "Desk" only (no subtitle). Quick Access shortcuts move directly beneath the "Pick up where you left off" card, before "Your Week".
> - **Study Planner Clamping & Compact Cards**: Week selector cannot navigate to future weeks beyond the current week (`weekOffset <= 0`). Schedule cards are compressed into slim, horizontal rows displaying cover, book title, details, and a quick read button.
> - **Pager Gesture Gating**: Sideways swipe gestures on the main pager are disabled when navigating inside Desk sub-screens (My Library, Books Collection, Study Planner, Collection View), re-enabling only on root Desk Home.
> - **My Library & Type Folders**:
>   - Downloaded items loaded directly from `/sdcard/Mulberry/downloaded_books.json`.
>   - Sub-texts removed from "My Library", "Downloaded Books", and "YOUR COLLECTIONS". Font sizes increased.
>   - Type folders (Books, Notes, PYQs, Handouts, etc.) appear dynamically only when $\ge 1$ item exists.
>   - Entering a type folder displays dynamic **Subject filter chips** (Biochemistry, Physiology, Anatomy, etc.), with in-progress/bookmarked metric cards and status chips removed.
> - **Functional Custom Collections**:
>   - Backed by `/sdcard/Mulberry/.history/collections.json`.
>   - Users can add any book to any collection via a selection dialog.
>   - Tapping a collection launches a dedicated collection materials screen with subject filters.
>   - Eye-pleasing, theme-adaptive vibrant solid gradient cards for all type and collection folders.

---

## 1. Overview & Core Concept

- **Purpose**: Polish user navigation and material management. Elevates primary actions, eliminates redundant visual clutter, enforces realistic reading schedule constraints, and enables true intent-based material organization.
- **Key Deliverables**:
  1. Desk visual reorganization (compact title, reordered Quick Access).
  2. Compact Study Planner timeline cards and future-week lock.
  3. Real JSON-backed offline textbook sync in My Library.
  4. Dynamic subject-filtered bookshelf view.
  5. Interactive user collection engine with item assignment and dedicated viewer.
  6. Sub-screen touch gesture protection preventing accidental tab switches.

---

## 2. User Experience & Visual Design

### 01. Desk Refinement
- **Header**: Clean, bold "Desk" (28sp, Poppins Bold), profile avatar on right. Sub-text removed.
- **Section Sequence**:
  1. Date label (`TUESDAY, OCTOBER 20`)
  2. `PICK UP WHERE YOU LEFT OFF` card with progress-filled `RESUME READING ↗` CTA.
  3. `QUICK ACCESS` (My Library & Study Planner cards immediately accessible).
  4. `YOUR WEEK` reading goal card with 7-day strip.
  5. `RECENTLY ADDED` horizontal carousel.
  6. `☕ Keep Mulberry growing` support card.

### 02. Compact Schedule Cards (Study Planner)
```text
┌────────────────────────────────────────────────────────┐
│ [COVER] Guyton & Hall Physiology                       │
│         Pages 140–160 · 40 min            [▶ Read]    │
└────────────────────────────────────────────────────────┘
```
- Slim card height (~58–64dp), book cover on left, title and page/duration detail in center, compact circular or pill read action on right.
- Week navigation clamp: Next week button (`>`) is disabled when `weekOffset == 0`.

### 03. My Library & Type Folders
- Large header: **My Library** (26sp), no subtitle.
- **Downloaded Books** section title (18sp Bold, no subtitle).
- Dynamic Type Folders: Horizontally scrollable vibrant gradient cards (e.g., Deep Crimson, Royal Indigo, Teal, Warm Amber) reflecting only existing types in `downloaded_books.json`.
- Type Folder View:
  - Header: `[←] DOWNLOADED / [Type Name]`
  - Dynamic subject chips: `[All]`, `[Biochemistry]`, `[Physiology]`, `[Anatomy]`, etc.
  - Clean 2-column bookshelf grid.

### 04. Custom Collections (Functional)
- Section title: **YOUR COLLECTIONS** (no subtitle).
- Curated cards with item count and vibrant theme-adaptive gradients.
- `[+]` button creates new collections stored in `/sdcard/Mulberry/.history/collections.json`.
- Tapping a collection opens the dedicated collection view with subject filters, reading launch, and remove-from-collection actions.
- Shared Book-Action Modal includes "Add to collection" dialog with checklist of existing collections.

---

## 3. Technical Architecture & Data Strategy

```text
┌─────────────────────────────────────────────────────────────────────────┐
│                             MainActivity.kt                             │
│   HorizontalPager (userScrollEnabled = isAtRootDeskHome || !isDeskTab)  │
└────────────────────────────────────┬────────────────────────────────────┘
                                     │
                                     ▼
                          DeskRootScreen.kt
                 (Destination State + BackHandler)
   ┌─────────────────┬─────────────────┬─────────────────┬────────────────┐
   ▼                 ▼                 ▼                 ▼                ▼
DeskDashboard    MyLibraryScreen  BooksCollection  StudyPlanner   CollectionDetail
 - Header: "Desk" - Downloaded    - Subject chips  - Clamped      - Dedicated
 - Quick Access     Books JSON    - No status        week <= 0      materials
   under Resume   - Dynamic         cards          - Compact      - Subject
 - Your Week        Type Folders  - 2-Col Grid       cards          filters
 - Recent / Coffee- Collections
```

### Data Synchronization
1. **Downloaded Books**: Directly loads `/sdcard/Mulberry/downloaded_books.json` via `MulberryStorageManager.loadTrackingIndex(context)`, falling back to Room database if file is absent.
2. **Collections JSON**: Stored at `/sdcard/Mulberry/.history/collections.json`:
   ```json
   {
     "version": 1,
     "collections": [
       {
         "id": "col_1",
         "name": "First Year MBBS",
         "description": "Core preclinical resources",
         "bookSlugs": ["harpers-illustrated-biochemistry", "guiding-notes-anatomy"]
       }
     ]
   }
   ```
3. **Pager Gesture Gating**: `isAtDeskHome: Boolean` tracked and passed upward to `MainActivity` so `userScrollEnabled` on `HorizontalPager` is set to `false` when user enters any sub-screen.

---

## 4. Execution Steps

1. **Desk Page Layout Update**:
   - Update `DeskDashboardScreen.kt`: Change header to "Desk", remove subtitle, move Quick Access cards above "Your Week".
2. **Study Planner Update**:
   - Update `StudyPlannerScreen.kt`: Disable forward week navigation when at current week, convert session cards to compact horizontal rows.
3. **Pager Gesture Gating**:
   - Update `DeskRootScreen.kt` and `MainActivity.kt` to disable `userScrollEnabled` when inside Desk sub-destinations.
4. **My Library Data & Folder Overhaul**:
   - Update `MyLibraryScreen.kt` to load from `downloaded_books.json`, remove subtexts, enlarge section headings, generate dynamic type cards with vibrant gradient backgrounds.
5. **Books & Type Folder Subject Filter Overhaul**:
   - Update `BooksCollectionScreen.kt` (or generalized `TypeMaterialsScreen.kt`): remove metric summary cards and status chips; inject dynamic subject chips derived from downloaded items.
6. **Functional Collections Engine**:
   - Update `MulberryHistoryManager.kt` to support adding/removing book slugs in collections.
   - Add collection picker dialog to `SharedBookActionModal.kt`.
   - Add dedicated `CollectionDetailScreen.kt` to view collection contents.
7. **Verify & Test**:
   - Run `compile_applet` and unit tests to ensure clean compilation.
