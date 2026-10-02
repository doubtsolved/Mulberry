# Mulberry — Application Architecture & Specification Brief

> **Application Name:** Mulberry  
> **Tagline:** Ergonomic Study Organizer & Native Dual-Engine PDF Reader  
> **Target Audience:** Medical scholars, academic researchers, university students, and deep readers  
> **Platform:** Android (API 26+, optimized for Android 11–15, tablets, foldables, and e-ink displays)  
> **Tech Stack:** Kotlin, Jetpack Compose (Material 3), Room Database, AndroidX PDF Viewer, Coroutines & Flow  

---

## 1. Executive Summary & App Brief

**Mulberry** is an offline-first, distraction-free study companion and academic library manager. Built specifically to eliminate the friction students and researchers face when organizing massive textbook repositories and reading heavy academic PDFs, Mulberry unifies three key pillars:

1. **Vault-Based Library Management:** Organizes documents by pointing to real storage folders (Vaults), automatically scanning directory trees, sanitizing messy PDF filenames into clean book titles, generating cached thumbnail covers, and maintaining sync through a zero-database portable `.mulberry/` metadata format.
2. **Hybrid Dual-Engine PDF Reader:** Features both a continuous vertical scroll reader (leveraging AndroidX `PdfViewerFragment`) and a paginated single-page study reader (powered by Compose `HorizontalPager` with LRU caching, pinch-to-zoom, one-handed tap zones, inking, highlighting, vector shapes, sticky notes, and true PDF document baking).
3. **Integrated Study & Exam Agenda:** Bridges the gap between reading and coursework with daily study checklists, direct links to specific textbook chapters and video lectures, dynamic countdown timers for upcoming examinations, and interactive Markdown syllabus checklists.

The app is completely local, respecting user privacy and battery life, with zero cloud lock-in.

---

## 2. High-Level Architecture & Design Structure

```
+-----------------------------------------------------------------------------------+
|                                 USER INTERFACE                                    |
|                                                                                   |
|  +---------------------+   +---------------------+   +--------------------------+ |
|  |    LibraryScreen    |   |    AgendaScreen     |   |      SettingsScreen      | |
|  | (Vaults/Books/Grid) |   |  (Tasks/Exams/Sync) |   | (Profile/Theme/Portab.)  | |
|  +---------------------+   +---------------------+   +--------------------------+ |
|                                                                                   |
|  +------------------------------------------------------------------------------+ |
|  |                ReaderActivity (Hybrid Dual-Engine PDF Viewer)                | |
|  |  [Continuous Scroll Engine]  <--->  [Single-Page Pager Engine with Inking]   | |
|  +------------------------------------------------------------------------------+ |
+-----------------------------------------+-----------------------------------------+
                                          | StateFlows & User Actions
                                          v
+-----------------------------------------------------------------------------------+
|                            VIEWMODEL LAYER (MVVM)                                 |
|                                                                                   |
|   MulberryViewModel                 SettingsViewModel                             |
|   - Vault & Book state              - Preference mutations                        |
|   - Search, Filters, Breadcrumbs    - Storage & Cache metrics                     |
|   - Inking & Annotation state       - App theme & Icon styling                    |
|   - Agenda tasks & Exam states                                                    |
+-----------------------------------------+-----------------------------------------+
                                          |
                                          v
+-----------------------------------------------------------------------------------+
|                           REPOSITORY & DOMAIN LAYER                               |
|                                                                                   |
|  +----------------------+  +-------------------------+  +-----------------------+ |
|  |  MulberryRepository  |  |  UserProfileRepository  |  | VaultMetadataManager  | |
|  +----------------------+  +-------------------------+  +-----------------------+ |
|  |  BackupManager       |  |  PdfAnnotationManager   |  | PdfEditorManager      | |
|  +----------------------+  +-------------------------+  +-----------------------+ |
+--------------------+------------------------------------+-------------------------+
                     |                                    |
          Room Local SQLite Database              Real File System (.mulberry)
                     v                                    v
+-----------------------------------------+  +--------------------------------------+
|            MULBERRY DATABASE            |  |             STORAGE VAULT            |
|  - VaultEntity     - BookEntity         |  | /VaultRoot/                          |
|  - AgendaTaskEntity - ExamEntity        |  |   /Subject A/Book1.pdf               |
|  - AnnotationEntity                     |  |   /.mulberry/                        |
|                                         |  |       library_state.json             |
|                                         |  |       study_tasks.json               |
|                                         |  |       user_profile.json              |
+-----------------------------------------+  +--------------------------------------+
```

### Architectural Principles
- **MVI/MVVM Pattern:** State is hosted in `StateFlow` streams exposed by `MulberryViewModel` and collected cleanly via Jetpack Compose's `collectAsState()`.
- **Offline-First & Zero Lock-In:** While Room SQLite is utilized for instant queries, all reading progress, favorites, and tasks are also serialized into atomic JSON files (`.mulberry/`) directly inside the user's storage folder, allowing users to move their vault to another device without losing reading states.
- **Direct Disk Binding:** Mulberry works directly with `java.io.File` using Android's `MANAGE_EXTERNAL_STORAGE` permission, avoiding slow SAF (Storage Access Framework) IPC bridges for multi-hundred-megabyte medical textbooks.

---

## 3. Design System & Theme Engine

Mulberry features an academic-focused Material 3 design system with customized typography, spacious 8.dp padding grid, tactile card elevations, and six meticulously tuned color palettes.

### 3.1 Color Themes (`ThemeMode`)

| Theme Name | Background | Primary Tone | Characteristic & Ergonomics |
|:---|:---|:---|:---|
| **Olded (Sepia)** | `#FBF0D9` (Warm Parchment) | `#C26D38` (Amber Rust) | High legibility for long study hours; mimics antique book paper, reduces eye strain. |
| **Paper (Clean White)** | `#FFFFFF` (Pure Crisp White) | `#2563EB` (Cobalt Royal) | Modern, bright, clinical look with subtle slate card backgrounds (`#F8FAFC`). |
| **Midnight (Pitch OLED)** | `#000000` (Pure Black) | `#38BDF8` (Sky Cyan) | Extreme battery saving on AMOLED screens, high contrast for night reading. |
| **Forest (Sage Dark)** | `#111A15` (Deep Pine) | `#4ADE80` (Emerald Sage) | Organic, calming dark theme designed for extended late-night sessions. |
| **Espresso (Mocha Dark)** | `#1A1412` (Dark Roast) | `#F59E0B` (Warm Ochre) | Warm, earthy dark mode with rich brown hues and amber accents. |
| **Dusk (Lavender/Indigo)** | `#13111C` (Deep Twilight) | `#A78BFA` (Soft Purple) | Stylish, muted purple-gray palette with deep indigo cards. |

### 3.2 Icon Styling (`IconStyle`)
- **Vibrant Style:** Dynamic semantic coloring for folders (Amber `#F59E0B`), PDFs (Crimson `#EF4444`), targets/progress (Emerald `#10B981`), and sync (Sky Blue `#0EA5E9`).
- **Adaptive Style:** Monochromatic theme-aware tinting matching `onSurfaceVariant` for a minimal, distraction-free interface.

### 3.3 Typography & Shapes
- **Headings & Badges:** `Poppins` font family (Medium, SemiBold, Bold) for book titles, progress meters, and section headers.
- **Body & Captions:** `Inter` font family (Regular, Medium) for metadata, authors, and task notes.
- **Corner Radii:** Consistent shape scales: 8.dp (Chips & Inputs), 14.dp (Cards & Buttons), 24.dp (Modal Sheets).

---

## 4. UI Layout & Screen Hierarchies

The primary app interface is organized around a persistent `Scaffold` containing a floating bottom navigation bar and three top-level screens:

```
                  +-----------------------------------+
                  |           MainActivity            |
                  +-----------------+-----------------+
                                    |
         +--------------------------+--------------------------+
         |                          |                          |
         v                          v                          v
  +--------------+           +--------------+           +--------------+
  |   LIBRARY    |           |    STUDY     |           |   SETTINGS   |
  |  (Tab 1)     |           |  (Tab 2)     |           |  (Tab 3)     |
  +--------------+           +--------------+           +--------------+
         |
         +----------------- Click Book -----------------+
                                                        v
                                              +-------------------+
                                              |  ReaderActivity   |
                                              +-------------------+
```

---

### 4.1 Screen 1: Library Screen (`LibraryScreen.kt`)

The Library is the central dashboard where documents are cataloged and accessed.

#### UI Header & Navigation Bar
- **App Bar:** Displays active Vault name with quick dropdown switcher, search button, layout toggle (2-column Grid vs Compact List), and overflow options.
- **Breadcrumb Navigation Strip:** Interactive pill bar allowing users to jump up folder levels (e.g., `Vault` > `Medical` > `Biochemistry`).
- **Category & Folder Filter Chips:** Horizontal scrolling pills with item count badges for filtering by subfolder.
- **Search Bar (Collapsible):** Live filtering across title, author, filename, and subfolder names.

#### Content Layouts
1. **"Continue Reading" Carousel:**
   - Displays books with active reading progress (> 0%).
   - Card displays full-bleed book cover thumbnail, title, author, reading progress percentage bar, and last-read page indicator.
2. **Main Book Display (Adaptive 2-Column Grid or List):**
   - **Grid View:** 2-column cards featuring a 3:4 aspect ratio thumbnail cover, favorite star badge, page count, file size, and 3-dot context menu.
   - **List View:** Dense horizontal row with a compact thumbnail, multi-line title, reading progress bar, and quick action buttons.

#### Context Menu & Document Actions
- **Favorite Toggle:** Stars the book for quick filtering.
- **Rename:** In-app and disk-level renaming with automatic `.pdf` extension preservation.
- **Duplicate:** Creates a duplicate file on disk (`_Copy.pdf`) with its own tracking record.
- **Share:** Invokes Android share sheet via secure `FileProvider`.
- **Open in External Reader:** Dispatches intent to Microsoft 365, Xodo, Adobe, or Drive.
- **Delete:** Two-tier deletion dialog: Remove from Library only, or permanently delete the physical file from disk.

---

### 4.2 Screen 2: Study Agenda & Exam Planner (`AgendaScreen.kt`)

Designed to keep students on schedule with upcoming deadlines, study targets, and exam syllabi.

#### Top Section: User Profile & Goals
- User greeting with academic role (`Medical Student - MBBS 1st Year`).
- Daily page goal indicator (e.g., `14/20 pages read today`).

#### Tab 1: Daily Tasks (`Tasks`)
- **Task Item Card:**
  - Interactive checkmark box with haptic vibration feedback.
  - Linked textbook badge: clicking immediately opens the linked PDF to the exact target chapter or page.
  - Linked video lecture pill: opens browser/YouTube to the lecture URL.
  - Edit and Delete action buttons.
- **Floating Action Button:** Opens modal dialog to add a new task with title, due date, linked document picker, chapter page, and optional lecture link.

#### Tab 2: Exam Tracker (`Exams`)
- **Exam Countdown Cards:**
  - Title and target exam date with dynamic "Days Remaining" counter.
  - Radial progress ring showing syllabus completion.
  - Expandable / Collapsible syllabus accordion.
- **Interactive Markdown Syllabus:**
  - Parses Markdown syllabus lists (`- [ ] Topic A`, `- [x] Topic B`).
  - Checking items directly updates the database and recalculates exam readiness percentage.
- **Add / Edit Exam Dialog:** Full date picker dialog and Markdown editor input for syllabus definition.

---

### 4.3 Screen 3: Settings & Data Portability (`SettingsScreen.kt`)

A comprehensive control panel organized into clean visual sections:

1. **Student Profile Card:**
   - Shows avatar, name, institution, and target subject.
   - Edit button opens `UserProfileBottomSheet` to modify credentials.
2. **Vault Management:**
   - Displays current vault path, book count, and last sync timestamp.
   - "Add / Switch Vault" button with SAF directory picker.
   - "Sync Now" button to force a disk scan.
3. **Reading Preferences:**
   - **Default Reading Mode:** Toggle between *Continuous Vertical Scroll* and *Single-Page Paginated*.
   - **Default External Editor:** Choice between Microsoft 365, Xodo, Adobe Acrobat, or Google Drive.
4. **Appearance & Themes:**
   - Interactive palette selector previewing all 6 color schemes.
   - Icon style switch: *Vibrant* vs *Adaptive*.
5. **Data Portability & Vault Backup:**
   - **Export Vault Backup:** Packages the entire `.mulberry/` metadata directory into a compressed `.zip` file for sharing or archiving.
   - **Restore Vault Backup:** Imports a `.zip` archive to restore all reading states, tasks, and syllabus items.
6. **Storage & Cache Metrics:**
   - Computes total library size on disk, thumbnail cache size, and annotation database size.
   - "Clear Thumbnail Cache" button to reclaim device storage.
7. **About & Version:**
   - Build info, privacy statement, and local data confirmation.

---

### 4.4 The Dual-Engine PDF Reader (`ReaderActivity.kt`)

`ReaderActivity` provides a distraction-free, full-screen reading environment that seamlessly toggles between two specialized rendering engines:

#### Engine A: Continuous Vertical Scroll Mode
- Built upon Google's official AndroidX `PdfViewerFragment`.
- Smooth vertical physics, pinch-to-zoom, native text selection, and hardware-accelerated document rendering.

#### Engine B: Paginated Single-Page Mode (`SinglePagePdfViewer.kt`)
- Custom Jetpack Compose implementation using `HorizontalPager`.
- Multi-threaded LRU bitmap caching with render queue mutex.
- **Smart Touch Zones:**
  - Left 20% tap: Previous page.
  - Right 20% tap: Next page.
  - Center 60% tap: Toggle reader chrome / floating toolbar.
- **Gesture Routing:**
  - At normal zoom (1.0x), horizontal swipe gestures navigate pages seamlessly.
  - When zoomed in (> 1.05x), gestures pan around the zoomed page without triggering accidental page turns.
  - Double-tap instantly zooms to 2.5x centered on the tap location.
- **Floating Page Scrubber:**
  - Bottom floating pill with page slider, Next/Previous controls, and current page badge (`Page 42 / 380`).

#### Reading Tools & Overlays
- **Night / Invert Mode:** Inverts PDF luminance for comfortable reading in pitch-black environments.
- **Table of Contents (TOC) Bottom Sheet:** Hierarchical outline extractor displaying chapters, sub-sections, and page jumps.
- **External PDF Launch:** Quick-handoff button opening the active file in Microsoft 365, Xodo, or Acrobat.

---

### 4.5 Inking & Vector Annotation Subsystem

Mulberry features an on-screen inking dock for academic markup:

#### Available Tools
1. **Pencil / Ballpoint Pen:** Variable stroke width (1.5dp) with color presets: Obsidian Black (`#0F172A`), Cobalt Blue (`#0284C7`), and Crimson Red (`#EF4444`).
2. **Highlighter:** Semi-transparent wide stroke (18dp, `#77FACC15`) with blend mode preservation.
3. **Geometric Shapes:** Rectangle, Circle, Arrow, and Line annotations.
4. **Sticky Notes & Text Boxes:** Tap anywhere to place an expandable yellow sticky note or text label.
5. **Date Stamp:** One-tap timestamp insertion (`MMM dd, yyyy`).
6. **Eraser & Undo/Redo:** Vector-based undo stack allowing step-by-step reversal or clearing of markings.

#### Coordinate Normalization & PDF Baking
- **Normalized Vector Coordinates:** All strokes are stored in relative units ($x \in [0.0, 1.0]$, $y \in [0.0, 1.0]$), guaranteeing that markings scale accurately across all screen densities, rotations, and window sizes.
- **True PDF Baking (`PdfAnnotationManager.kt`):**
  - **Save:** Bakes all annotations into the original PDF document.
  - **Save As:** Generates a new annotated copy (`[Title]_Annotated.pdf`) and registers it in the library.

---

## 5. Screen Navigation Flow & Connections

```
[Launcher / Android OS]
         |
         v
+------------------+
|   MainActivity   | <=================================================+
+--------+---------+                                                   |
         |                                                             |
         +---> BackHandler: Root tab double-press to exit              |
         +---> BackHandler: Sub-tabs (Study/Settings) back to Library   |
         |                                                             |
         +------------------------- Tabs --------------------------+   |
         |                            |                            |   |
         v                            v                            v   |
+------------------+         +------------------+         +--------+---+-----+
|  LibraryScreen   |         |   AgendaScreen   |         |  SettingsScreen  |
+--------+---------+         +--------+---------+         +--------+---------+
         |                            |                            |
         |-- Filter by Folder         |-- Add/Edit Task Dialog     |-- Profile Sheet
         |-- Search Query Filter      |-- Add/Edit Exam Dialog     |-- Vault Picker
         |-- Sort Mode Dropdown       |-- Linked Book Click ----+  |-- Backup/Restore
         |-- Layout Mode Toggle       |                         |  |-- Theme Chooser
         |-- Book Context Menu        +-------------------------+  |-- Cache Cleanup
         |                                                      |  +-----------------+
         +---------------------- Open Book ---------------------+
                                      |
                                      v
                         +--------------------------+
                         |      ReaderActivity      |
                         +------------+-------------+
                                      |
       +------------------------------+------------------------------+
       |                              |                              |
       v                              v                              v
[Continuous Scroll]          [Single Page Viewer]            [Inking Dock]
       |                              |                              |
       +---> TOC Outline Sheet        +---> Page Scrubber Slider     +---> Pen / Highl.
       +---> Invert Night Mode        +---> Pinch & Double-Tap Zoom  +---> Shapes / Note
       +---> External App Launch      +---> 20/60/20 Tap Zones       +---> Save / Save As
```

### System Back Navigation Logic
- **Reader Activity:** Hardware back closes the reader, saves current page progress & timestamp to Room and `.mulberry/`, and returns to the Library.
- **Library Sub-Folders:** Hardware back pops one folder level up the breadcrumb trail.
- **Study / Settings Tabs:** Hardware back redirects the user back to the Library tab.
- **Root Library Tab:** Requires a double-tap within 2 seconds to exit the application to prevent accidental exits.

---

## 6. Data Storage & Schema Details

Mulberry utilizes a dual-layer persistence model: Room SQLite for local fast querying, and structured JSON files in `.mulberry/` for cross-device portability.

### 6.1 Room Database Entities (`Entities.kt`)

#### 1. `VaultEntity` (`vaults` table)
| Column | Type | Description |
|:---|:---|:---|
| `id` | `Int` (PK, AutoGen) | Unique vault identifier |
| `name` | `String` | Display name of the vault |
| `uriString` | `String` | Raw canonical directory path or tree URI |
| `pathDisplay` | `String` | User-friendly path shown in UI |
| `bookCount` | `Int` | Cached count of PDF documents in vault |
| `addedAt` | `Long` | Timestamp when the vault was registered |

#### 2. `BookEntity` (`books` table)
| Column | Type | Description |
|:---|:---|:---|
| `id` | `Int` (PK, AutoGen) | Unique document ID |
| `vaultId` | `Int` | Foreign key referencing parent `VaultEntity` |
| `title` | `String` | Clean, sanitized title (e.g., "Robbins Pathology") |
| `author` | `String` | Author name (if extracted) |
| `fileName` | `String` | Raw file name on disk (e.g., `pathology_9th_ed.pdf`) |
| `uriString` | `String` | Absolute file path |
| `pageCount` | `Int` | Total number of pages |
| `fileSizeBytes` | `Long` | File size in bytes |
| `isFavorite` | `Boolean` | User favorite star flag |
| `lastReadPage` | `Int` | Last viewed page index (1-based) |
| `progressPercent` | `Int` | Calculated completion (0–100%) |
| `lastReadTimestamp`| `Long` | Unix timestamp of last reading session |
| `parentFolder` | `String` | Immediate parent directory name (category) |

#### 3. `AgendaTaskEntity` (`agenda_tasks` table)
| Column | Type | Description |
|:---|:---|:---|
| `id` | `Int` (PK, AutoGen) | Unique task ID |
| `title` | `String` | Task description |
| `isCompleted` | `Boolean` | Completion status |
| `dateString` | `String` | Target due date string |
| `linkedBookId` | `Int?` | Optional linked book reference |
| `linkedBookTitle` | `String?` | Title of linked book |
| `linkedChapterPage`| `Int?` | Target page jump upon tapping task |
| `linkedLectureUrl` | `String?` | Optional video lecture or website URL |
| `linkedLectureTitle`| `String?` | Label for the lecture URL |
| `orderIndex` | `Int` | Sorting position |

#### 4. `ExamEntity` (`exams` table)
| Column | Type | Description |
|:---|:---|:---|
| `id` | `Int` (PK, AutoGen) | Unique exam ID |
| `title` | `String` | Exam subject or course name |
| `examDateMillis` | `Long` | Timestamp of scheduled exam |
| `totalChaptersCount`| `Int` | Total items in syllabus |
| `remainingChaptersCount`| `Int` | Incomplete chapters count |
| `syllabusJson` | `String` | Raw Markdown syllabus content with checkboxes |

#### 5. `AnnotationEntity` (`annotations` table)
| Column | Type | Description |
|:---|:---|:---|
| `id` | `Int` (PK, AutoGen) | Unique annotation ID |
| `bookId` | `Int` | Target book ID |
| `pageIndex` | `Int` | Page number the stroke belongs to |
| `strokePointsJson`| `String` | JSON array of normalized $[x, y]$ points or shape parameters |
| `colorHex` | `String` | Stroke color in `#AARRGGBB` hex |
| `strokeWidth` | `Float` | Stroke thickness |
| `type` | `String` | `PEN`, `HIGHLIGHTER`, `NOTE`, `SHAPE`, `TEXT` |
| `timestamp` | `Long` | Creation timestamp |

---

### 6.2 Portable Vault Metadata Format (`.mulberry/`)

Whenever reading progress or tasks are updated, `VaultMetadataManager` atomically writes:

1. **`/.mulberry/library_state.json`**:
   ```json
   {
     "version": 1,
     "lastUpdated": 1774902000000,
     "books": [
       {
         "relativePath": "Physiology/Guyton_Hall.pdf",
         "lastPage": 142,
         "totalPages": 1150,
         "progressPercent": 12.3,
         "isFavorite": true,
         "lastReadTimestamp": 1774901850000
       }
     ]
   }
   ```
2. **`/.mulberry/study_tasks.json`**:
   ```json
   {
     "version": 1,
     "lastUpdated": 1774902000000,
     "tasks": [
       {
         "id": "task_101",
         "title": "Read Cardiac Cycle",
         "isCompleted": false,
         "dueDate": 1775000000000,
         "linkedBookRelativePath": "Physiology/Guyton_Hall.pdf",
         "linkedPage": 142
       }
     ]
   }
   ```
3. **`/.mulberry/user_profile.json`**:
   Stores display name, institution, academic role, and daily study targets.

---

## 7. Permissions & System Integration

| Permission | Scope & Purpose |
|:---|:---|
| `MANAGE_EXTERNAL_STORAGE` | Direct access to raw storage directories for subfolder tree scanning, atomic metadata writing, and fast PDF file access. |
| `READ_EXTERNAL_STORAGE` | Legacy file reading fallback for Android 9 & 10. |
| `WRITE_EXTERNAL_STORAGE` | Legacy file writing fallback for Android 9 & 10 (maxSdk 29). |
| `FileProvider` (`com.mulberry.fileprovider`) | Generates secure content URIs for opening files in external editors and sharing files via system share dialogs. |

---

## 8. Summary of Key Differentiators

1. **Zero Cloud Lock-in:** All library state travels with the files in `.mulberry/` metadata.
2. **Hybrid PDF Architecture:** Best of both worlds: fluid continuous vertical scroll for casual browsing, and paginated single-page view for focused pen inking, highlighting, and study.
3. **True PDF Document Baking:** Does not keep notes locked inside proprietary databases; annotations can be permanently flattened into real PDF files.
4. **Contextual Study Integration:** Tasks directly connect to specific pages in specific textbooks, removing all friction from study workflows.
5. **Ergonomic Visuals:** Built specifically for reading with 6 specialized themes (including genuine Olded Sepia and OLED Pitch Black) and refined typography.
