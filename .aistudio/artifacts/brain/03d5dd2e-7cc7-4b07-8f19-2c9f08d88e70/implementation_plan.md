# Desk Dashboard & Multi-Screen Reading Suite Revamp

Revamp the Mulberry home dashboard (Desk) into a high-focus reading hub, add My Library and Books Collection screens, introduce the full Study Planner screen and shared book-action modal, retire the Snips tab, and track all user movements in a synced JSON history ledger under `/sdcard/Mulberry/.history/`.

## User Review & Critical Decisions

> [!IMPORTANT]
> The following decisions and clarifications guide this implementation:
> - **Accurate History & Books**: History and reading metrics reflect real user activity without mock simulations. Books and resources are downloaded from the online Library catalog.
> - **Hidden History Storage**: All reading events and app movements are stored in `/sdcard/Mulberry/.history/activity_events.json` and synchronized with the Room database for dashboard statistics.
> - **Navigation Hierarchy**: Primary bottom navigation includes **Desk**, **Library**, and **Settings**. Quick-access tiles on Desk launch **My Library** and the **Study Planner**, with full `BackHandler` support returning directly to Desk.
> - **Snips Retirement**: The Snips tab is completely removed from the navigation bars and view hierarchies.

---

## 1. Overview & Core Concept

- **What It Does**: Unifies reading resumption, study scheduling, personal collection management, and activity tracking into a structured, distraction-free environment.
- **Target Audience**: Medical students and healthcare professionals managing dense curricula, requiring rapid resumption of textbooks, structured study time tracking, and organized material categorization.
- **Key Value**: One-tap resumption of active reading materials, truth-based progress analytics, and clear separation between format classification (material types) and intent-based curation (user collections).

---

## 2. Screen Specifications & ASCII Wireframes

### Screen 01: Desk (Home Dashboard)

Purpose: Help users resume reading, check weekly progress, and discover recently added resources.

```text
┌──────────────────────────────────┐
│                                  │
│  TUESDAY, OCTOBER 20             │
│                                  │
│  ┌────────────────────────────┐  │
│  │ PICK UP WHERE YOU LEFT OFF │  │
│  │                            │  │
│  │ [COVER] Harper's           │  │
│  │         Illustrated        │  │
│  │         Biochemistry       │  │
│  │         Chapter 04         │  │
│  │                            │  │
│  │ ┌────────────────────────┐ │  │
│  │ │ RESUME READING      ↗  │ │  │
│  │ │████████░░░░░░░░░░░░░░░ │ │  │
│  │ └────────────────────────┘ │  │
│  └────────────────────────────┘  │
│                                  │
│  YOUR WEEK          View plans → │
│  ┌────────────────────────────┐  │
│  │ Weekly reading goal        │  │
│  │ 4.5 / 7 hours       (64%)  │  │
│  │                            │  │
│  │ M    T    W    T    F   S S│  │
│  │ ✓   [20]  ○    ○    ○   ○ ○│  │
│  └────────────────────────────┘  │
│                                  │
│  RECENTLY ADDED               →  │
│  ┌──────────────┐ ┌────────────┐ │
│  │ [THUMBNAIL]  │ │ [THUMBNAIL]│ │
│  │ AETCOM       │ │ Jambulkar  │ │
│  │ Resources    │ │ Notes      │ │
│  └──────────────┘ └────────────┘ │
│                                  │
│  QUICK ACCESS                    │
│  ┌──────────────┐ ┌────────────┐ │
│  │ My Library   │ │ Study      │ │
│  │ Books & files│ │ Planner    │ │
│  └──────────────┘ └────────────┘ │
│                                  │
│  ┌────────────────────────────┐  │
│  │ ☕ Keep Mulberry growing    │  │
│  │ Support this free app      │  │
│  └────────────────────────────┘  │
│                                  │
├──────────────────────────────────┤
│   ⌂ Desk   ◉ Library   ⚙ Settings│
└──────────────────────────────────┘
```

#### Desk Component Specification

| Component | Description and Behavior |
| :--- | :--- |
| **Header** | Current date, Mulberry logo, profile avatar with initials opening the Profile bottom sheet. |
| **Resume-Reading Card** | Displays the last opened book cover, title, chapter or page, and progress percentage. The primary CTA button's background doubles as the visual progress fill (`RESUME READING ↗`). Tapping the card opens the Shared Book-Action Modal; tapping the CTA directly opens the PDF reader. |
| **Your Week** | Weekly reading time vs target (e.g. `4.5 / 7 hours (64%)`) with a compact M–S day strip showing completed days (`✓`), current day (`[20]`), and upcoming days (`○`). |
| **View Plans** | Header text button linking directly into the full Study Planner screen. |
| **Recently Added** | Horizontal carousel of newly downloaded or imported resources with thumbnail and title badges. |
| **Quick Access** | Shortcut cards navigating to **My Library** (Books & files) and **Study Planner**. |
| **Donation Prompt** | Subtle support card opening a humorous donation / coffee modal for Mulberry. |
| **Bottom Navigation** | Persistent bottom navigation across Desk, Library (Online Catalogue), and Settings. |

---

### Screen 02: My Library

Purpose: Provide access to the user's own imported, downloaded, and locally available materials, distinct from the online Library catalogue.

```text
┌──────────────────────────────────┐
│  [LOGO] My Library           [⋮]  │
│  Everything you've collected,    │
│  in one place.                   │
│                                  │
│  ┌────────────────────────────┐  │
│  │ [SEARCH YOUR BOOKS/FILES]  │  │
│  │                       [⚙]  │  │
│  └────────────────────────────┘  │
│                                  │
│  YOUR COLLECTION                 │
│  Browse by material type         │
│                                  │
│  ┌──────────┐ ┌──────────┐ ┌───→ │
│  │  [BOOK]  │ │ [NOTES]  │ │PYQs│
│  │          │ │          │ │    │
│  │  Books   │ │  Notes   │ │    │
│  └──────────┘ └──────────┘ └────│
│    24 items    38 items          │
│                                  │
│  ← HORIZONTAL TYPE SCROLLER →    │
│                                  │
│  ──────────────────────────────  │
│                                  │
│  YOUR COLLECTIONS           [+]  │
│  Your own curated reading lists  │
│                                  │
│  ┌──────────────┐ ┌─────────────┐│
│  │              │ │             ││
│  │  [ARTWORK]   │ │  [ARTWORK]  ││
│  │              │ │             ││
│  │ First Year   │ │ Must Read   ││
│  │ MBBS         │ │             ││
│  │ 18 resources │ │ 9 resources ││
│  └──────────────┘ └─────────────┘│
│                                  │
│  ← HORIZONTAL COLLECTION LIST →  │
│                                  │
│  RECENTLY OPENED              →  │
│  ┌────────────────────────────┐  │
│  │ [COVER] Harper's            │  │
│  │         Biochemistry        │  │
│  │         PDF · Page 86    [⋮]│  │
│  ├────────────────────────────┤  │
│  │ [FILE]  Jambulkar Notes     │  │
│  │         PDF · Biochemistry  │  │
│  └────────────────────────────┘  │
│                                  │
│  ┌────────────────────────────┐  │
│  │       [+] Add files        │  │
│  └────────────────────────────┘  │
│                                  │
├──────────────────────────────────┤
│   ⌂ Desk   ◉ Library   ⚙ Settings│
└──────────────────────────────────┘
```

#### My Library Component Specification

| Component | Description and Behavior |
| :--- | :--- |
| **Header & Subtitle** | "My Library - Everything you've collected, in one place." with search and overflow actions. |
| **Search & Filters** | Instant search field querying title, author, and tags across offline/downloaded items. |
| **Material-Type Scroller** | Horizontal cards: Books, Notes, PYQs, Handouts, Presentations, Other Files. Selecting **Books** opens the Books Collection screen. |
| **Your Collections** | Curated custom playlists with resource counts and `[+]` button to create a new collection. |
| **Recently Opened** | List of recently read files with cover/icon, format, current page, and quick action overflow. |
| **Add Files CTA** | Primary action button allowing import from external storage or file picker into Mulberry. |

---

### Screen 03: Books Collection

Purpose: Display all resources categorized as books, with book-specific filtering, reading progress, and management actions.

```text
┌──────────────────────────────────┐
│  [←]  MY LIBRARY / MATERIALS      │
│       Books               [⌕][⋮] │
│                                  │
│  Your bookshelf.                 │
│  24 books                        │
│                                  │
│  ┌──────────┐ ┌──────────┐ ┌────┐│
│  │    12    │ │    8     │ │ 4  ││
│  │ In       │ │ Book-    │ │Fin-││
│  │ progress │ │ marked   │ │ish.││
│  └──────────┘ └──────────┘ └────┘│
│                                  │
│  ┌────────────────────────────┐  │
│  │ [SEARCH BOOKS]          [⚙]│  │
│  └────────────────────────────┘  │
│                                  │
│  [All books] [In progress] [Unread]│
│                                  │
│  ALL BOOKS                       │
│  Recently opened                 │
│                                  │
│  ┌───────────────┐ ┌────────────┐│
│  │               │ │            ││
│  │    [COVER]    │ │  [COVER]   ││
│  │               │ │            ││
│  ├───────────────┤ ├────────────┤│
│  │ Harper's      │ │ Gray's     ││
│  │ Biochemistry  │ │ Anatomy    ││
│  │ PDF · Page 86 │ │ PDF · New  ││
│  │ ███░░░░░░░░░  │ │ ░░░░░░░░░  ││
│  └───────────────┘ └────────────┘│
│                                  │
│  ┌───────────────┐ ┌────────────┐│
│  │    [COVER]    │ │  [COVER]   ││
│  ├───────────────┤ ├────────────┤│
│  │ Guyton & Hall │ │ Lippincott ││
│  │ Physiology    │ │ Biochem.   ││
│  │ PDF ·    Pg142│ │ PDF · Pg32 ││
│  │ ████░░░░░░░░  │ │ █░░░░░░░░░ ││
│  └───────────────┘ └────────────┘│
│                                  │
│  ┌────────────────────────────┐  │
│  │         [+] Add books      │  │
│  └────────────────────────────┘  │
│                                  │
├──────────────────────────────────┤
│   ⌂ Desk   ◉ Library   ⚙ Settings│
└──────────────────────────────────┘
```

#### Books Collection Component Specification

| Component | Description and Behavior |
| :--- | :--- |
| **Top Bar & Back Navigation** | Returns to My Library while preserving scroll state. |
| **Metrics Row** | In progress, Bookmarked, Finished counts computed from real database states. |
| **Search & Filter Chips** | Interactive chips (*All books*, *In progress*, *Unread*, *Finished*) to filter bookshelf grid. |
| **Book Cards** | Single tap opens the Shared Book-Action Modal (or directly resumes reading); long-press triggers actions. Shows cover, title, subject, page number, and progress bar. |
| **Add Books CTA** | Navigates directly to the online Library catalog to download more textbooks. |

---

### Screen 04: Study Planner

Purpose: Turn the compact "Your Week" summary on Desk into a full interface for planning, scheduling, and tracking reading sessions.

```text
┌──────────────────────────────────┐
│  [←]  STUDY PLANNER    [CAL][⋮] │
│                                  │
│  Your week, in focus.            │
│  October 19–25, 2026             │
│                                  │
│  ┌────────────────────────────┐  │
│  │ Weekly reading goal        │  │
│  │                            │  │
│  │ 4.5 / 7 hours       (64%)  │  │
│  │ ████████████░░░░░░░         │  │
│  │                            │  │
│  │ Sessions: 6 of 10          │  │
│  │ Remaining: 2h 30m          │  │
│  └────────────────────────────┘  │
│                                  │
│  THIS WEEK             [<] [>]   │
│  ┌───┐ ┌───┐ ┌───┐ ┌───┐ ┌───┐  │
│  │MON│ │TUE│ │WED│ │THU│ │FRI│→ │
│  │ 19│ │ 20│ │ 21│ │ 22│ │ 23│  │
│  │ ✓ │ │ ● │ │ ○ │ │ ○ │ │ ○ │  │
│  └───┘ └───┘ └───┘ └───┘ └───┘  │
│                                  │
│  TUESDAY'S PLAN                  │
│  October 20 · Today's sessions   │
│                                  │
│  09:00 ─┬─────────────────────┐ │
│         │ [BOOK] Enzymes       │ │
│         │ Harper's Biochem.    │ │
│         │ 45 min               │ │
│         │ ✓ Completed          │ │
│         └──────────────────────┘ │
│                                  │
│  14:00 ─┬─────────────────────┐ │
│         │ [BOOK] Cellular      │ │
│         │ Physiology            │ │
│         │ Guyton & Hall · 60min│ │
│         │ [▶ Start session]     │ │
│         └──────────────────────┘ │
│                                  │
│  17:00 ─┬─────────────────────┐ │
│         │ [NOTES] Revise       │ │
│         │ Biochemistry · 30min │ │
│         │ ○ Not started        │ │
│         └──────────────────────┘ │
│                                  │
│  ┌────────────────────────────┐  │
│  │       [+] Add session      │  │
│  └────────────────────────────┘  │
│                                  │
├──────────────────────────────────┤
│   ⌂ Desk   ◉ Library   ⚙ Settings│
└──────────────────────────────────┘
```

#### Study Planner Component Specification

| Component | Description and Behavior |
| :--- | :--- |
| **Top Bar** | Back button returning to Desk, title, calendar icon, and settings overflow. |
| **Weekly Goal Card** | Planned target, recorded reading time, progress bar, completed sessions counter, and remaining study duration. |
| **Week Selector Strip** | Interactive horizontal date strip (M T W T F S S). Selecting a day filters the schedule to that specific date; `<` and `>` navigate weeks. |
| **Daily Session Timeline** | Chronological study session cards with resource type, title, planned duration, and status (Completed `✓` or `▶ Start session`). |
| **Start Session Action** | Launches the linked book directly into the reader and starts tracking the session in `/sdcard/Mulberry/.history/`. |
| **Add Session Sheet** | Dialog to pick book/notes, date, start time, target duration, and reminders. |

---

### Component 05: Shared Book-Action Modal

Reusable across Desk, My Library, Books Collection, and Study Planner.

```text
┌──────────────────────────────────┐
│                                  │
│             [HANDLE]             │
│                                  │
│  ┌───────┐                       │
│  │ COVER │  Harper's Illustrated │
│  │       │  Biochemistry         │
│  └───────┘  PDF · Page 86        │
│                                  │
│  █████░░░░░░░░░░░░░  21%         │
│                                  │
│  ┌────────────────────────────┐  │
│  │       Resume Reading       │  │
│  └────────────────────────────┘  │
│                                  │
│  [BOOK] Open from beginning      │
│  [FOLDER] Add to collection      │
│  [INFO] View details             │
│  [MORE] More options             │
│                                  │
│  ──────────────────────────────  │
│  [Remove from My Library]        │
│                                  │
└──────────────────────────────────┘
```

#### Modal Interaction Behavior
- **Primary CTA**: "Resume Reading" (opens at `lastReadPage`) or "Open Book" (if unread).
- **Secondary Actions**: "Open from beginning", "Add to collection", "View details".
- **Destructive Separation**: "Remove from My Library" separated by a subtle divider with danger accent. Does not delete underlying encrypted files without separate confirmation.

---

## 3. Data Structure & History Synchronization

### User Activity Ledger (`/sdcard/Mulberry/.history/activity_events.json`)

```json
{
  "version": 1,
  "storageDirectory": "/sdcard/Mulberry/.history",
  "lastSyncedEpoch": 1729468800000,
  "totalEventsTracked": 142,
  "events": [
    {
      "id": "evt_1729468801_001",
      "eventType": "SESSION_START",
      "timestamp": 1729468801000,
      "bookSlug": "harpers-illustrated-biochemistry",
      "bookTitle": "Harper's Illustrated Biochemistry",
      "pageNumber": 86,
      "metadata": { "source": "desk_resume_card" }
    },
    {
      "id": "evt_1729469701_002",
      "eventType": "PAGE_TURNED",
      "timestamp": 1729469701000,
      "bookSlug": "harpers-illustrated-biochemistry",
      "pageNumber": 92,
      "durationSeconds": 900
    },
    {
      "id": "evt_1729470601_003",
      "eventType": "SESSION_COMPLETED",
      "timestamp": 1729470601000,
      "bookSlug": "harpers-illustrated-biochemistry",
      "durationSeconds": 1800,
      "pagesRead": 12
    }
  ]
}
```

The `MulberryHistoryManager` updates this file atomically on IO threads and computes:
- Total weekly hours read.
- Current active streak.
- Day-by-day reading activity for the 7-day strip.
- Resumption points and last-read book states.

---

## 4. Execution Steps

1. **Retire Snips Tab**:
   - Update `MulberryTab` enum to `DESK`, `LIBRARY`, `SETTINGS`.
   - Update `MulberryBottomNavBar.kt`, `NavigationRail.kt`, and `MainActivity.kt`.
2. **Implement History Manager**:
   - Create `MulberryHistoryManager.kt` managing `/sdcard/Mulberry/.history/activity_events.json` and sync with Room.
3. **Build Shared Book-Action Modal**:
   - Create `SharedBookActionModal.kt` implementing spec 05.
4. **Implement Desk Screen Revamp**:
   - Create updated `DeskDashboardScreen.kt` implementing spec 01.
5. **Implement My Library & Books Collection**:
   - Create `MyLibraryScreen.kt` (spec 02) and `BooksCollectionScreen.kt` (spec 03).
6. **Implement Full Study Planner**:
   - Create `StudyPlannerScreen.kt` (spec 04) with week selector and session timeline.
7. **Integrate Sub-Destination Navigation**:
   - Wire Desk quick access to My Library, Books Collection, and Study Planner with `BackHandler`.
8. **Verify Compilation**:
   - Run `compile_applet` and verify all screens build cleanly without errors.
