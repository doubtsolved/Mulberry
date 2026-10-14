# Mulberry Web — Full Architectural Blueprint & Implementation Specification

> **Application Name:** Mulberry Web  
> **Tagline:** Ergonomic Study Organizer & Local Dual-Engine PDF Reader  
> **Target Audience:** Medical scholars, academic researchers, university students, and deep readers  
> **Web Target Stack:** React 19, Vite, TypeScript, Tailwind CSS, IndexedDB (Dexie.js), OPFS / File System Access API, Mozilla PDF.js, pdf-lib  
> **Architecture Pattern:** 100% Client-Side Offline-First, Zero Cloud Dependency, Portable `.mulberry/` Vault Sync  

---

## Table of Contents
1. [Executive Summary & Product Vision](#1-executive-summary--product-vision)
2. [Tech Stack & Dependency Manifest](#2-tech-stack--dependency-manifest)
3. [Design System, Color Themes & Typography](#3-design-system-color-themes--typography)
4. [Application Shell & Responsive Navigation](#4-application-shell--responsive-navigation)
5. [Complete Screen Layouts & UI Element Functions](#5-complete-screen-layouts--ui-element-functions)
   - [5.1 Screen 1: Library View](#51-screen-1-library-view)
   - [5.2 Screen 2: Desk & Study Agenda View](#52-screen-2-desk--study-agenda-view)
   - [5.3 Screen 3: Snips (Scratchpad / Lab) View](#53-screen-3-snips-scratchpad--lab-view)
   - [5.4 Screen 4: Settings & Data Portability View](#54-screen-4-settings--data-portability-view)
   - [5.5 Screen 5: Full-Screen Dual-Engine PDF Reader View](#55-screen-5-full-screen-dual-engine-pdf-reader-view)
6. [Inking, Annotations & PDF Baking Subsystem](#6-inking-annotations--pdf-baking-subsystem)
7. [Offline Data Storage, Schemas & Portable Metadata](#7-offline-data-storage-schemas--portable-metadata)
8. [End-to-End System Workflows](#8-end-to-end-system-workflows)
   - [Workflow 1: Vault Folder Ingestion & Name Sanitization](#workflow-1-vault-folder-ingestion--name-sanitization)
   - [Workflow 2: PDF Thumbnail Generation & Caching](#workflow-2-pdf-thumbnail-generation--caching)
   - [Workflow 3: Reading Session Tracking & Weekly Goal Math](#workflow-3-reading-session-tracking--weekly-goal-math)
   - [Workflow 4: Dual-Engine PDF Rendering & Navigation](#workflow-4-dual-engine-pdf-rendering--navigation)
   - [Workflow 5: Vector Inking & PDF Document Baking](#workflow-5-vector-inking--pdf-document-baking)
   - [Workflow 6: Portable Vault Backup (.ZIP) Export & Restore](#workflow-6-portable-vault-backup-zip-export--restore)
9. [Project Scaffolding & Directory Structure](#9-project-scaffolding--directory-structure)

---

## 1. Executive Summary & Product Vision

**Mulberry Web** is an offline-first, distraction-free study companion and academic library manager built as a high-performance web application. Designed specifically to eliminate the friction scholars face when managing gigabytes of medical and academic textbooks, Mulberry unifies three key pillars:

1. **Vault-Based Local Library Management**: Organizes documents by mounting real local filesystem folders (Vaults) via the modern browser **File System Access API** (with OPFS fallback), automatically crawling subdirectories, sanitizing messy PDF filenames into publication titles, caching crisp page-1 thumbnails, and synchronizing reading states in a portable, human-readable `/.mulberry/` metadata directory.
2. **Hybrid Dual-Engine PDF Reader**: Features two swappable rendering engines:
   - **Continuous Vertical Scroll Engine**: Smooth virtualized scrolling for rapid reading, skimming, and continuous review.
   - **Paginated Single-Page Study Engine**: Distraction-free single-page viewer with intelligent 20/60/20% tap zones, double-tap zoom, vector pen inking, highlighters, geometric shapes, sticky notes, and true client-side PDF document baking (saving annotations directly into the `.pdf` file).
3. **Desk & Study Agenda Integration**: Unifies reading with academic deadlines through structured reading sessions (Today, Tomorrow, Upcoming), weekly reading goal progress gauges, exam countdown timers, and interactive Markdown syllabus checklists that jump directly to linked book pages.

Everything runs entirely in the user's browser: **zero servers, zero authentication gates, zero tracking, and absolute data sovereignty**.

---

## 2. Tech Stack & Dependency Manifest

### 2.1 Core Web Technologies
| Layer | Technology | Version | Purpose |
|:---|:---|:---|:---|
| **Framework** | React + TypeScript | `^19.0.0` | UI component tree, hooks, and strict type safety |
| **Build Tool** | Vite | `^6.0.0` | Ultra-fast HMR and bundle optimization |
| **Styling** | Tailwind CSS | `^4.0.0` / `^3.4.0` | Utility-first ergonomic design tokens and themes |
| **Icons** | Lucide React | `^0.460.0` | Feather/Fluent-aligned semantic and clean UI iconography |
| **Client Database** | Dexie.js (IndexedDB) | `^4.0.0` | Fast local queries, reactive live queries, and indices |
| **File Storage** | File System Access API / OPFS | Native Web APIs | Direct local directory mounting without uploading files |
| **PDF Rendering** | Mozilla PDF.js | `^4.8.0` | Web Worker multi-threaded PDF decoding and canvas rasterization |
| **PDF Baking** | `pdf-lib` | `^1.17.9` | Client-side vector inking and annotation flattening into PDF files |
| **Virtualization** | `@tanstack/react-virtual` | `^3.10.0` | 60 FPS continuous vertical PDF scrolling |
| **State Management**| Zustand | `^5.0.0` | Lightweight store for active vault, theme, and reader state |
| **Backup Compression**| `jszip` + `file-saver` | `^3.10.1` | Portable `.mulberry` ZIP backup export and restoration |

### 2.2 Recommended `package.json`
```json
{
  "name": "mulberry-web",
  "private": true,
  "version": "1.0.0",
  "type": "module",
  "scripts": {
    "dev": "vite",
    "build": "tsc -b && vite build",
    "preview": "vite preview"
  },
  "dependencies": {
    "@tanstack/react-virtual": "^3.10.8",
    "clsx": "^2.1.1",
    "dexie": "^4.0.8",
    "dexie-react-hooks": "^1.1.7",
    "file-saver": "^2.0.5",
    "jszip": "^3.10.1",
    "lucide-react": "^0.460.0",
    "pdf-lib": "^1.17.9",
    "pdfjs-dist": "^4.8.69",
    "react": "^19.0.0",
    "react-dom": "^19.0.0",
    "tailwind-merge": "^2.5.4",
    "zustand": "^5.0.1"
  },
  "devDependencies": {
    "@types/file-saver": "^2.0.7",
    "@types/node": "^22.9.0",
    "@types/react": "^19.0.0",
    "@types/react-dom": "^19.0.0",
    "@vitejs/plugin-react": "^4.3.3",
    "autoprefixer": "^10.4.20",
    "postcss": "^8.4.49",
    "tailwindcss": "^3.4.15",
    "typescript": "^5.6.3",
    "vite": "^6.0.0"
  }
}
```

---

## 3. Design System, Color Themes & Typography

### 3.1 Color Theme Matrix
Mulberry defines 6 intentional color themes calibrated for academic reading, low eye strain, and OLED efficiency. In the web app, these are implemented via CSS custom properties on `document.documentElement` (`data-theme="olded | paper | midnight | forest | espresso | dusk"`).

| Theme Mode | Visual Characteristic | Background | Surface Card | Primary Accent | Text Primary | Text Muted | Border Subtle |
|:---|:---|:---|:---|:---|:---|:---|:---|
| **`olded`** (Sepia) | Antique paper, low blue light, e-ink friendly | `#FBF0D9` | `#F4E6C3` | `#C26D38` (Amber Rust) | `#2C2216` | `#8C7356` | `#E8D4A2` |
| **`paper`** (Clean White) | Clinical, crisp white, high contrast | `#FFFFFF` | `#F8FAFC` | `#2563EB` (Cobalt Royal) | `#0F172A` | `#94A3B8` | `#E2E8F0` |
| **`midnight`** (OLED Black) | 0% pixel power on OLED, night reading | `#000000` | `#111111` | `#38BDF8` (Sky Cyan) | `#F8FAFC` | `#64748B` | `#1E293B` |
| **`forest`** (Pine Dark) | Calming organic sage & deep spruce | `#111A15` | `#17231D` | `#4ADE80` (Emerald Sage) | `#E2E8F0` | `#6B7280` | `#1F2E26` |
| **`espresso`** (Mocha Dark) | Earthy dark coffee roast, amber glow | `#1A1412` | `#261E1A` | `#F59E0B` (Warm Ochre) | `#F5F5F4` | `#78716C` | `#352924` |
| **`dusk`** (Indigo Twilight)| Soft lavender on deep twilight purple | `#13111C` | `#1B1828` | `#A78BFA` (Muted Purple)| `#F1F5F9` | `#64748B` | `#27223A` |

### 3.2 Semantic Icon Styling (`IconStyle`)
- **Vibrant Mode**: Icons carry purposeful category colors:
  - Folders / Categories: **Amber** (`#F59E0B`)
  - PDF Documents: **Crimson Red** (`#EF4444`)
  - Target / Progress / Goals: **Emerald Green** (`#10B981`)
  - Sync / Storage / Cloud: **Sky Blue** (`#0EA5E9`)
- **Adaptive Mode**: Monochromatic theme-aware tint matching `var(--text-secondary)`, keeping the entire UI quiet and focused.

### 3.3 Typography Tokens
- **Headings & Badges**: `'Poppins', 'Outfit', system-ui, sans-serif` (Weights: 500, 600, 700) for book titles, statistics, and section headings.
- **Body & Content**: `'Inter', -apple-system, BlinkMacSystemFont, sans-serif` (Weights: 400, 500) for metadata, task descriptions, and dialog copy.
- **Monospace**: `'JetBrains Mono', 'Fira Code', monospace` for page codes, file sizes, and timestamps.

### 3.4 Corner Radii & Elevation
- **`rounded-lg` (8px)**: Filter chips, search inputs, badges, small buttons.
- **`rounded-2xl` (14px)**: Book cards, session cards, action buttons, dialogs.
- **`rounded-3xl` (24px)**: Bottom sheets, modal overlays, floating docks.
- **Borders**: Sub-pixel `0.5px` or `1px` subtle borders (`border-[var(--border-subtle)]`) rather than heavy drop shadows.

---

## 4. Application Shell & Responsive Navigation

The application shell adapts dynamically between mobile, tablet, and desktop:

```
+-----------------------------------------------------------------------------------------+
|                                    DESKTOP / TABLET (>= 768px)                          |
| +------------+  +---------------------------------------------------------------------+ |
| | RAIL NAV   |  | TOP HEADER (Active Vault Dropdown | Search | Theme Quick-Toggle)    | |
| |            |  +---------------------------------------------------------------------+ |
| | [B] Library|  |                                                                     | |
| | [D] Desk   |  | MAIN CONTENT AREA                                                   | |
| | [S] Snips  |  | (Switches between LibraryScreen, DeskRootScreen, etc.)              | |
| | [*] Sett.  |  |                                                                     | |
| |            |  |                                                                     | |
| +------------+  +---------------------------------------------------------------------+ |
+-----------------------------------------------------------------------------------------+
|                                    MOBILE (< 768px)                                     |
| +-------------------------------------------------------------------------------------+ |
| | TOP HEADER (Active Vault Dropdown | Search | Theme Toggle)                          | |
| +-------------------------------------------------------------------------------------+ |
| | MAIN SCROLLABLE CONTENT VIEW                                                        | |
| +-------------------------------------------------------------------------------------+ |
| | FLOATING FROSTED BOTTOM NAVBAR (Library | Desk | Snips | Settings)                 | |
| +-------------------------------------------------------------------------------------+ |
+-----------------------------------------------------------------------------------------+
```

### 4.1 Navigation Elements & Interactions
- **Tabs**:
  1. `LIBRARY`: Vault explorer, book grid/list, continue reading carousel.
  2. `DESK`: Study sessions (Today, Tomorrow, Upcoming), weekly goal progress, task checklists, exam countdown.
  3. `SNIPS`: Quick academic scratchpad & culinary-themed focus lab.
  4. `SETTINGS`: Profile management, vault directory binding, theme switcher, storage metrics, ZIP backup.
- **Tab Customization (`CustomizeTabsSheet`)**:
  - Long-pressing (or right-clicking) any tab triggers the Customize Tabs sheet, allowing users to reorder tabs or toggle visibility.
- **Back / Route Management**:
  - Single-page application using client-side state routing (`currentTab: 'LIBRARY' | 'DESK' | 'SNIPS' | 'SETTINGS' | 'READER'`).
  - Browser history integration via `window.history.pushState` ensures the browser back button closes modals or returns from reader view to library.

---

## 5. Complete Screen Layouts & UI Element Functions

### 5.1 Screen 1: Library View

The Library is the central academic dashboard where PDF documents are scanned, cataloged, and read.

```
+-------------------------------------------------------------------------------+
| [Vault Icon] Medical Vault v        [Search Bar / Trigger]  [List/Grid Toggle]|
+-------------------------------------------------------------------------------+
| Breadcrumbs:  Home  >  1st_Prof  >  Physiology                                |
+-------------------------------------------------------------------------------+
| Category Chips: [All (18)]  [Biochemistry (4)]  [Physiology (8)]  [Anatomy (6)]|
+-------------------------------------------------------------------------------+
| CONTINUE READING                                                              |
| +-----------------------+  +-----------------------+                          |
| | [Cover]  Guyton Hall  |  | [Cover]  Robbins Path |                          |
| | Page 142 of 1150 (12%)|  | Page 45 of 980 (5%)   |                          |
| +-----------------------+  +-----------------------+                          |
+-------------------------------------------------------------------------------+
| ALL BOOKS (Grid or List View)                                                 |
| +-------------------+  +-------------------+  +-------------------+           |
| | [Cover Thumbnail] |  | [Cover Thumbnail] |  | [Cover Thumbnail] |           |
| | [*] Favorite Star |  | [*] Favorite Star |  | [*] Favorite Star |           |
| | Guyton & Hall     |  | Robbins Basic Path|  | Ganong Physiology |           |
| | 1,150 pages | 92MB|  | 980 pages | 114MB |  | 780 pages | 64MB  |           |
| | [3-Dot Menu]      |  | [3-Dot Menu]      |  | [3-Dot Menu]      |           |
| +-------------------+  +-------------------+  +-------------------+           |
+-------------------------------------------------------------------------------+
```

#### Element Functionality & Event Handlers:
1. **Vault Selector Dropdown**:
   - Displays current active vault name with down-chevron.
   - Click opens dropdown menu displaying all registered vaults plus an `+ Add New Vault` button that calls `window.showDirectoryPicker()`.
2. **Search Bar**:
   - Debounced search input (200ms) filtering books dynamically by `title`, `author`, `fileName`, and `parentFolder`.
   - Clear button (X) resets query.
3. **Breadcrumb Strip**:
   - Shows folder hierarchy pills (e.g., `VaultRoot > 1st_Prof > Physiology`).
   - Clicking any ancestor pill instantly filters the book view to that directory subtree.
4. **Category Filter Chips**:
   - Horizontally scrollable row showing unique immediate subfolders with item count badges.
   - Clicking filters books by selected subfolder; "All" clears folder filters.
5. **Layout Mode Toggle**:
   - Toggles between **2-to-4 Column Grid View** and **Compact List View**. Persisted in settings.
6. **"Continue Reading" Carousel**:
   - Filtered to books where `progressPercent > 0 && progressPercent < 100`, sorted descending by `lastReadTimestamp`.
   - Shows cover thumbnail, title, reading percentage progress bar, and "Page X of Y".
   - Click immediately launches the book in **Reader View** at `lastReadPage`.
7. **Book Card (Grid & List View)**:
   - **Thumbnail Cover**: Canvas-generated 3:4 aspect ratio image from page 1 of the PDF. If loading, renders an elegant academic vector fallback with the book's initials.
   - **Favorite Star Badge**: Quick toggle that updates `isFavorite` in IndexedDB and commits to `.mulberry/library_state.json`.
   - **Title**: Clean, sanitized human title with tooltip on hover for long names.
   - **Metadata Row**: Formatted page count (`1,150 pages`) and human file size (`92 MB`).
   - **3-Dot Context Menu**:
     - *Favorite / Unfavorite*: Toggles starred status.
     - *Rename*: Opens modal to rename the book title and physical file.
     - *Duplicate*: Creates a duplicate file in OPFS/local directory (`[Title]_Copy.pdf`).
     - *Export / Download*: Triggers browser download of the physical PDF file.
     - *Delete*: Two-tier dialog: (1) Remove from Mulberry Library, or (2) Permanently delete file from local storage.

---

### 5.2 Screen 2: Desk & Study Agenda View

The Desk screen connects reading goals with daily study tasks and academic examination schedules.

```
+-------------------------------------------------------------------------------+
| Good Morning, Dr. Saha              [Edit Profile] [Week: Oct 06 - Oct 12]    |
| MBBS 1st Year - Medical College                                               |
+-------------------------------------------------------------------------------+
| WEEKLY READING GOAL                                                           |
| 142 / 200 Pages Read (71%)                  [======----------]                |
| Today: 38 Pages | Planned: 60 Pages                                           |
+-------------------------------------------------------------------------------+
| TABS: [Scheduled Sessions (4)]   [Study Tasks (6)]   [Exam Countdown (2)]     |
+-------------------------------------------------------------------------------+
| [TODAY'S ACTIVE SESSIONS]                                                     |
| +---------------------------------------------------------------------------+ |
| | [Book Icon] Guyton Physiology — Cardiac Cycle                             | |
| | Pages 140 - 165 (25 pages ~ 50 mins)   Time: 10:00 AM                     | |
| | Note: "Review aortic stenosis murmurs"                                    | |
| | [Start Reading Button]                          [Checkmark Complete Button| |
| +---------------------------------------------------------------------------+ |
|                                                                               |
| [UPCOMING SESSIONS (TOMORROW / LATER)]                                        |
| +---------------------------------------------------------------------------+ |
| | [Book Icon] Robbins Pathology — Acute Inflammation                        | |
| | Pages 45 - 60 (15 pages ~ 30 mins)     Tomorrow at 2:00 PM                | |
| +---------------------------------------------------------------------------+ |
|                                                    [+ Schedule Session FAB]   |
+-------------------------------------------------------------------------------+
```

#### Sub-Components & Functionality:
1. **Header & Profile Badge**:
   - Displays user avatar, greeting, role, and institution.
   - Click opens `UserProfileModal` to edit name, role, institution, and weekly goal target.
2. **Weekly Goal Meter**:
   - Calculates total `actualPagesRead` across sessions completed within the current calendar week.
   - Displays percentage progress bar, completed pages vs. target (e.g., `142 / 200 pages`), and remaining pages.
   - Click opens `WeeklyGoalModal` to update the weekly target (e.g., 200 pages).
3. **Scheduled Reading Sessions List**:
   - Grouped into collapsible sections: `Today (Active)`, `Today (Completed)`, `Tomorrow`, and `Upcoming`.
   - **Session Card Elements**:
     - Book title and target chapter/page range (`Pages 140 - 165`).
     - Estimated study time calculated as `totalPages * 2 minutes`.
     - Micro-focus note badge (e.g., `"Focus on aortic valves"`).
     - **Start Reading Button**: Launches the Reader directly at `startPage`. When reader closes, prompts user with actual pages read.
     - **Complete Button**: Marks session completed, records completion timestamp, and updates weekly progress.
     - **Edit / Delete**: Context actions for rescheduling or removing sessions.
4. **Schedule Session Modal (`ScheduleSessionSheet`)**:
   - Book picker dropdown (filtered to active vault books).
   - Start Page & End Page number inputs (with automatic page validation against book total).
   - Scheduled Date & Time pickers.
   - Micro-note text field.
   - Notification reminder selector (`None`, `15 min before`, `30 min before`, `1 hour before` via Web Notifications API).
5. **Study Tasks Checklist**:
   - Interactive checkbox for completing tasks.
   - Linked textbook badge: clicking immediately opens the book to the assigned chapter/page.
   - Linked video lecture URL: external link button opening reference videos.
   - Add Task dialog with form inputs for book link, page, and lecture URL.
6. **Exam Countdown & Markdown Syllabus Tracker**:
   - Displays exam title and target date with calculated **"X Days Remaining"** badge.
   - Radial readiness meter calculated from completed syllabus topics.
   - **Interactive Markdown Syllabus**: Parses Markdown checklists (`- [ ]`, `- [x]`). Toggling a checkbox updates the database and recalculates exam readiness in real time.

---

### 5.3 Screen 3: Snips (Scratchpad / Lab) View

The Snips screen provides a lightweight, whimsical scratchpad and upcoming experimental lab.

#### Visual Elements:
- Minimal clean top header with title "Snips".
- **Interactive Chef Cooking Animation (HTML5 Canvas)**:
  - Whimsical animated chef hat with bubbling steam particles and culinary shimmer accents.
  - Rendered programmatically via Canvas animation loop (`requestAnimationFrame`) or SVG path animation.
- Headline: *"Something's Cooking. Don't Enter the Kitchen!"*
- Subtitle: *"Snips & Fast Excerpts are preparing in the oven. Soon you will crop textbook figures, capture tables, and generate high-yield flashcards with one gesture."*
- **Active Quick Scratchpad Shelf**:
  - Below the animation, a quick Markdown scratchpad allows scholars to jot down transient thoughts, formulas, or copy-pasted citations during reading sessions without switching apps.

---

### 5.4 Screen 4: Settings & Data Portability View

Comprehensive control center for user profile, directory bindings, themes, storage, and portable backups.

```
+-------------------------------------------------------------------------------+
| SETTINGS & DATA PORTABILITY                                                   |
+-------------------------------------------------------------------------------+
| [Avatar] Sahabul Mondal                             [Edit Profile Button]     |
| Medical Student — MBBS 1st Year | Target: 200 pages/week                      |
+-------------------------------------------------------------------------------+
| VAULT DIRECTORY                                                               |
| Current: C:/Users/Docs/MedicalVault (18 books detected)                       |
| [Switch / Mount Vault]                             [Sync Now (Force Scan)]    |
+-------------------------------------------------------------------------------+
| READING PREFERENCES                                                           |
| Default Reading Engine:  (o) Single-Page Paginated   ( ) Continuous Scroll    |
| Page Turn Animation:     [Slide / Snap]                                       |
+-------------------------------------------------------------------------------+
| APPEARANCE & THEMES                                                           |
| Palette Selector:                                                             |
| [ Olded (Sepia) ]  [ Paper (White) ]  [ Midnight (OLED) ]                     |
| [ Forest (Pine) ]  [ Espresso (Mocha)] [ Dusk (Indigo) ]                      |
| Icon Style: (o) Vibrant Colors   ( ) Adaptive Monochromatic                   |
+-------------------------------------------------------------------------------+
| STORAGE & CACHE METRICS                                                       |
| Total Library Size: 1.84 GB                                                   |
| IndexedDB Metadata: 3.4 MB | Thumbnail Cache: 14.2 MB                         |
| [Clear Thumbnail Cache Button]                                                |
+-------------------------------------------------------------------------------+
| PORTABLE BACKUP & ARCHIVE (.mulberry)                                         |
| [Export Vault Backup (.ZIP)]                 [Restore Backup Archive (.ZIP)]  |
+-------------------------------------------------------------------------------+
| About Mulberry Web v1.0.0 — 100% Local & Privacy Guaranteed                   |
+-------------------------------------------------------------------------------+
```

#### Settings Functionality:
1. **User Profile Editor**:
   - Edits name, institution, academic role, and target weekly pages. Stored in IndexedDB and synced to `/.mulberry/user_profile.json`.
2. **Vault Management**:
   - Shows active mounted directory handle, total books, and last sync timestamp.
   - `Switch Vault`: Calls `window.showDirectoryPicker()` to bind a new local folder.
   - `Sync Now`: Re-crawls folder structure, detects new/removed PDF files, and synchronizes state.
3. **Appearance Controls**:
   - Interactive preview tiles for all 6 themes. Switching immediately updates CSS root variables.
   - Icon style toggle (`Vibrant` vs `Adaptive`).
4. **Storage & Cache Cleanup**:
   - Queries `navigator.storage.estimate()` to show browser quota usage.
   - `Clear Thumbnail Cache`: Reclaims IndexedDB space by deleting cached page-1 image blobs.
5. **Portable Vault Backup (Export / Restore)**:
   - **Export**: Uses `JSZip` to package all `.mulberry/` files (`library_state.json`, `study_tasks.json`, `schedules.json`, `user_profile.json`) into `Mulberry_Backup_[Timestamp].zip`.
   - **Restore**: File upload input accepting `.zip` archive; unpacks JSON files, updates IndexedDB, and updates current vault metadata.

---

### 5.5 Screen 5: Full-Screen Dual-Engine PDF Reader View

The distraction-free reading experience that handles multi-hundred-megabyte textbooks with high responsiveness.

```
+-------------------------------------------------------------------------------+
| [<- Back] Guyton & Hall Physiology (Pg 142/1150)   [Search] [TOC] [Inking] [X]|
+-------------------------------------------------------------------------------+
|                                                                               |
|  [20% Tap: Prev Pg]             [60% Center Tap]            [20% Tap: Next Pg]|
|                                                                               |
|                               +-------------------+                           |
|                               |                   |                           |
|                               |   PDF PAGE VIEW   |                           |
|                               |  (Single or Scroll|                           |
|                               |   with Inking)    |                           |
|                               |                   |                           |
|                               +-------------------+                           |
|                                                                               |
+-------------------------------------------------------------------------------+
| FLOATING SCRUBBER DOCK: [|<] [<] [====o================] [>] [>|]  Pg 142/1150|
+-------------------------------------------------------------------------------+
| INKING DOCK (Expandable):                                                     |
| [Pen] [Highlighter] [Rect] [Circle] [Arrow] [Sticky Note] [Stamp] [Undo][Save]|
+-------------------------------------------------------------------------------+
```

#### Reader Components & Controls:
1. **Reader Top Chrome (Auto-Hiding)**:
   - Back button: Saves current page and reading timestamp to IndexedDB & `.mulberry/`, returns to Library.
   - Book title & current progress indicator (`Page 142 of 1,150 (12%)`).
   - Engine Toggle button: Switches between **Single-Page Paginated** and **Continuous Scroll**.
   - Search button: Expands PDF text search bar.
   - Table of Contents (TOC) button: Opens hierarchical outline drawer.
   - Inking Toggle button: Shows/hides vector annotation toolbar dock.
2. **Engine A: Continuous Vertical Scroll**:
   - Built using `@tanstack/react-virtual` for virtualized rendering of all document pages.
   - Only renders visible pages and a 2-page overscan window to maintain smooth 60 FPS scrolling and low RAM usage.
   - Smooth trackpad and mouse-wheel scrolling.
3. **Engine B: Paginated Single-Page Viewer**:
   - Renders exactly the active page (with pre-rendering of page $N-1$ and $N+1$).
   - **Ergonomic One-Handed Tap Zones**:
     - **Left 20% of screen**: Navigate to Previous Page.
     - **Right 20% of screen**: Navigate to Next Page.
     - **Center 60% of screen**: Toggle visibility of top chrome and bottom scrubber dock.
   - **Touch & Gesture Routing**:
     - At normal zoom (1.0x), horizontal swipe / arrow keys turn pages.
     - When zoomed in (> 1.05x), drag gestures pan around the zoomed page without turning pages.
     - **Double-Tap / Double-Click**: Instantly toggles between 1.0x and 2.5x zoom centered exactly at the cursor/tap coordinates.
4. **Floating Page Scrubber Dock**:
   - Floating pill at screen bottom.
   - Discrete page slider, Previous / Next buttons, First / Last page buttons, and direct numeric page jump input.
5. **Table of Contents (TOC) & Thumbnail Drawer**:
   - Slide-over sidebar displaying:
     - **Outline Tab**: Hierarchical tree extracted via `pdfDoc.getOutline()`. Clicking any node jumps to destination page.
     - **Thumbnails Tab**: Virtualized grid of small page thumbnails with active page indicator.
6. **In-Page Text Search**:
   - Live search input querying document text via `PDFPageProxy.getTextContent()`.
   - Displays match counter (`Match 4 of 28`), Next/Prev match buttons, and highlights matches on the canvas.

---

## 6. Inking, Annotations & PDF Baking Subsystem

Mulberry features an academic markup subsystem allowing students to write, highlight, and place vector notes over PDF pages, and permanently bake them into the PDF file.

```
+------------------------------------------------------------------------------------+
| INKING CANVAS LAYER (Absolute overlay positioned over PDF page canvas)            |
|                                                                                    |
|  Normalized Coordinates: (x / pageWidth, y / pageHeight) in [0.0, 1.0]            |
|                                                                                    |
|  [Pencil Stroke: #0F172A, width: 2]                                                |
|  [Highlighter Stroke: #77FACC15, blend: multiply, width: 24]                       |
|  [Vector Arrow: (0.2, 0.4) -> (0.35, 0.4)]                                         |
|  [Sticky Note Pill: "Remember this for pathology viva!"]                           |
+------------------------------------------------------------------------------------+
```

### 6.1 Available Inking Tools
| Tool | Stroke Width | Default Color Presets | Behavior |
|:---|:---|:---|:---|
| **Pen / Pencil** | 1.5px – 4px | Obsidian (`#0F172A`), Cobalt (`#0284C7`), Crimson (`#EF4444`) | Freehand vector stroke with quadratic bezier smoothing |
| **Highlighter** | 18px – 28px | Yellow (`#FACC15`), Green (`#4ADE80`), Pink (`#F472B6`) with 40% alpha | Rendered with `mix-blend-mode: multiply` so underlying text remains crisp |
| **Rectangle** | 2px | Crimson, Cobalt, Obsidian | Drag to bound diagrams or formulas |
| **Circle / Oval** | 2px | Crimson, Cobalt, Obsidian | Drag to encircle key terms |
| **Vector Arrow** | 2.5px | Crimson, Cobalt | Drag to point from margin note to diagram |
| **Sticky Note** | N/A | Warm Canary Yellow (`#FEF08A`) | Tap to drop an expandable markdown note box |
| **Date Stamp** | N/A | Neutral Slate | One-tap insertion of current date (`MMM dd, yyyy`) |
| **Eraser** | Variable | N/A | Vector stroke eraser: deletes entire stroke upon hit |
| **Undo / Redo** | N/A | N/A | Comprehensive stack tracking all canvas actions |

### 6.2 Coordinate Normalization
To ensure annotations scale perfectly across window resizing, zoom levels, rotation, and mobile/desktop displays:
$$\text{point}_{\text{norm}} = \left( \frac{x_{\text{pixel}}}{\text{canvasWidth}}, \frac{y_{\text{pixel}}}{\text{canvasHeight}} \right) \in [0.0, 1.0]$$
All strokes are stored as normalized coordinates in IndexedDB. When rendering, the canvas simply multiplies normalized values by current rendered dimensions.

### 6.3 Client-Side PDF Document Baking Workflow (`pdf-lib`)
Mulberry does not trap notes inside a proprietary database. Users can permanently bake annotations into the actual PDF file:
1. User clicks **"Bake & Save PDF"** or **"Save Annotated Copy"**.
2. Web Worker loads the original PDF binary array into `pdf-lib` (`PDFDocument.load(bytes)`).
3. For each annotated page, `pdf-lib` retrieves the target page (`doc.getPage(pageIndex)`):
   - Translates normalized points $(x_{\text{norm}}, y_{\text{norm}})$ to PDF points:
     $$x_{\text{pdf}} = x_{\text{norm}} \times \text{page.getWidth()}$$
     $$y_{\text{pdf}} = (1.0 - y_{\text{norm}}) \times \text{page.getHeight()} \quad \text{(inverting Y-axis)}$$
   - Freehand pen strokes are drawn using `page.drawSvgPath()` or series of lines.
   - Highlighters are drawn as semi-transparent rectangles or thick paths with opacity.
   - Text notes and date stamps are embedded using standard PDF fonts.
4. The modified document is serialized (`const bakedBytes = await doc.save()`).
5. Overwrites the file in the mounted local directory (via File System Access API) or triggers a download of `[Title]_Annotated.pdf`.

---

## 7. Offline Data Storage, Schemas & Portable Metadata

### 7.1 IndexedDB Schema (Dexie.js)
IndexedDB is used for lightning-fast querying, sorting, and reactive UI subscriptions.

```typescript
// db/MulberryDB.ts
import Dexie, { type Table } from 'dexie';

export interface VaultRecord {
  id?: number;
  name: string;
  pathDisplay: string;
  bookCount: number;
  addedAt: number;
}

export interface BookRecord {
  id?: number;
  vaultId: number;
  title: string;
  author: string;
  fileName: string;
  relativePath: string;
  pageCount: number;
  fileSizeBytes: number;
  isFavorite: boolean;
  lastReadPage: number;
  progressPercent: number;
  lastReadTimestamp: number;
  parentFolder: string;
  thumbnailBlob?: Blob;
}

export interface ReadingSessionRecord {
  id: string; // UUID
  vaultPath: string;
  bookFileName: string;
  bookRelativePath: string;
  bookTitle: string;
  startPage: number;
  endPage: number;
  scheduledEpochMillis: number;
  reminderMinutesBefore: number;
  microNote: string;
  isCompleted: boolean;
  completedEpochMillis?: number;
  actualPagesRead: number;
}

export interface AgendaTaskRecord {
  id?: number;
  title: string;
  isCompleted: boolean;
  dueDate: string;
  linkedBookId?: number;
  linkedBookTitle?: string;
  linkedChapterPage?: number;
  linkedLectureUrl?: string;
  orderIndex: number;
}

export interface ExamRecord {
  id?: number;
  title: string;
  examDateMillis: number;
  totalChaptersCount: number;
  remainingChaptersCount: number;
  syllabusMarkdown: string;
}

export interface AnnotationRecord {
  id?: number;
  bookRelativePath: string;
  pageIndex: number; // 0-based
  strokePointsJson: string; // JSON array of {x, y} in [0, 1]
  colorHex: string;
  strokeWidth: number;
  type: 'PEN' | 'HIGHLIGHTER' | 'RECTANGLE' | 'CIRCLE' | 'ARROW' | 'NOTE' | 'STAMP';
  timestamp: number;
}

export interface UserProfileRecord {
  id: string; // 'active_profile'
  name: string;
  role: string;
  institution: string;
  weeklyGoalPages: number;
}

export class MulberryDatabase extends Dexie {
  vaults!: Table<VaultRecord, number>;
  books!: Table<BookRecord, number>;
  sessions!: Table<ReadingSessionRecord, string>;
  tasks!: Table<AgendaTaskRecord, number>;
  exams!: Table<ExamRecord, number>;
  annotations!: Table<AnnotationRecord, number>;
  userProfile!: Table<UserProfileRecord, string>;

  constructor() {
    super('MulberryWebDB');
    this.version(1).stores({
      vaults: '++id, name, addedAt',
      books: '++id, vaultId, title, relativePath, isFavorite, parentFolder, lastReadTimestamp',
      sessions: 'id, vaultPath, bookRelativePath, scheduledEpochMillis, isCompleted',
      tasks: '++id, isCompleted, dueDate, linkedBookId',
      exams: '++id, examDateMillis',
      annotations: '++id, [bookRelativePath+pageIndex], timestamp',
      userProfile: 'id'
    });
  }
}

export const db = new MulberryDatabase();
```

---

### 7.2 Portable Metadata Format (`.mulberry/`)
To achieve zero cloud lock-in and allow moving the study vault to another device or operating system, Mulberry automatically writes atomic JSON files inside a hidden `/.mulberry/` directory at the vault root:

#### 1. `/.mulberry/library_state.json`
```json
{
  "version": 1,
  "lastUpdated": 1774902000000,
  "books": [
    {
      "relativePath": "1st_Prof/Physiology/Guyton_Hall.pdf",
      "title": "Guyton & Hall Textbook of Medical Physiology",
      "lastPage": 142,
      "totalPages": 1150,
      "progressPercent": 12.3,
      "isFavorite": true,
      "lastReadTimestamp": 1774901850000
    }
  ]
}
```

#### 2. `/.mulberry/schedules.json`
```json
{
  "version": 1,
  "weeklyGoalPages": 200,
  "sessions": [
    {
      "id": "f81d4fae-7dec-11d0-a765-00a0c91e6bf6",
      "bookRelativePath": "1st_Prof/Physiology/Guyton_Hall.pdf",
      "bookTitle": "Guyton & Hall",
      "startPage": 140,
      "endPage": 165,
      "scheduledEpochMillis": 1774940400000,
      "reminderMinutesBefore": 15,
      "microNote": "Cardiac Cycle & murmurs",
      "isCompleted": false,
      "actualPagesRead": 0
    }
  ]
}
```

#### 3. `/.mulberry/study_tasks.json`
```json
{
  "version": 1,
  "tasks": [
    {
      "id": 1,
      "title": "Read Aortic Murmurs Summary",
      "isCompleted": false,
      "dueDate": "2026-10-15",
      "linkedBookRelativePath": "1st_Prof/Physiology/Guyton_Hall.pdf",
      "linkedPage": 142,
      "linkedLectureUrl": "https://youtube.com/watch?v=example"
    }
  ]
}
```

#### 4. `/.mulberry/user_profile.json`
```json
{
  "name": "Sahabul Mondal",
  "role": "Medical Student — MBBS 1st Year",
  "institution": "Calcutta National Medical College",
  "weeklyGoalPages": 200
}
```

---

## 8. End-to-End System Workflows

### Workflow 1: Vault Folder Ingestion & Name Sanitization
When a user mounts a folder via `window.showDirectoryPicker()`:

```
[User clicks "Mount Vault"]
            |
            v
[window.showDirectoryPicker()] -> Obtains FileSystemDirectoryHandle
            |
            v
[Recursive Directory Crawl] -> Scans all files matching /\.pdf$/i
            |
            +---> Sanitizes filenames:
            |     - Strips underscores, dashes, trailing editions:
            |       "guyton_and_hall_physiology_13th_ed_2016.pdf" -> "Guyton & Hall Physiology"
            |     - Preserves clean relative path ("1st_Prof/Physiology/Guyton.pdf")
            |
            +---> Checks for existing /.mulberry/ directory:
            |     - IF FOUND: Ingests existing progress, favorite states, and schedules into IndexedDB.
            |     - IF NOT FOUND: Creates /.mulberry/ and initializes empty state manifests.
            |
            v
[Stores in IndexedDB] -> React UI updates immediately via useLiveQuery()
```

---

### Workflow 2: PDF Thumbnail Generation & Caching
Generating fast, crisp page-1 thumbnails without freezing the UI:
1. When books are loaded into the Library view, `ThumbnailManager` checks if a cached `thumbnailBlob` exists in IndexedDB for the book.
2. If absent, the book file is sent to a dedicated Web Worker running Mozilla PDF.js.
3. The worker calls:
   ```javascript
   const pdf = await pdfjsLib.getDocument({ data: arrayBuffer }).promise;
   const page = await pdf.getPage(1);
   const viewport = page.getViewport({ scale: 0.5 }); // Compact resolution
   const canvas = new OffscreenCanvas(viewport.width, viewport.height);
   const ctx = canvas.getContext('2d');
   await page.render({ canvasContext: ctx, viewport }).promise;
   const blob = await canvas.convertToBlob({ type: 'image/webp', quality: 0.8 });
   ```
4. The blob is stored in IndexedDB and an `ObjectUrl` is created for immediate display in the book card.

---

### Workflow 3: Reading Session Tracking & Weekly Goal Math
1. **Planning**: Scholar schedules a session for "Guyton & Hall", Pages 140–165 on Tuesday at 10:00 AM.
2. **Start Reading**: Scholar taps "Start Session".
   - The app navigates to `ReaderView` with props `startPage: 140, targetEndPage: 165`.
   - The reader tracks pages turned and maximum page reached.
3. **Completion**: Upon finishing or closing the reader:
   - Dialog prompts: *"You read 26 pages (140 to 165). Mark session as complete?"*
   - Updating `session.isCompleted = true` and `session.actualPagesRead = 26`.
4. **Weekly Goal Aggregation**:
   ```typescript
   function calculateWeeklyProgress(sessions: ReadingSessionRecord[], targetGoal: number) {
     const startOfWeek = getStartOfWeekEpoch(); // Monday 00:00
     const endOfWeek = getEndOfWeekEpoch();     // Sunday 23:59
     
     const completedThisWeek = sessions
       .filter(s => s.isCompleted && s.completedEpochMillis! >= startOfWeek && s.completedEpochMillis! <= endOfWeek)
       .reduce((sum, s) => sum + s.actualPagesRead, 0);

     const percent = Math.min(100, Math.round((completedThisWeek / targetGoal) * 100));
     return { completedThisWeek, targetGoal, percent };
   }
   ```
5. Reactive state automatically updates the Desk screen's progress bar.

---

### Workflow 4: Dual-Engine PDF Rendering & Navigation
```
                              [Book Selected]
                                     |
                                     v
                  +--------------------------------------+
                  |           Reader Shell               |
                  | Checks user default reader engine    |
                  +------------------+-------------------+
                                     |
             +-----------------------+-----------------------+
             |                                               |
             v                                               v
+-----------------------------+               +-----------------------------+
| Engine A: Continuous Scroll |               | Engine B: Single-Page Pager |
| - @tanstack/react-virtual   |               | - Touch zones: 20% / 60% / 20%
| - Renders visible pages     |               | - Gesture routing:          |
| - Ideal for rapid skimming  |               |   * Scale <= 1.05: Turn page|
|                             |               |   * Scale > 1.05: Pan page  |
|                             |               | - Double tap: 2.5x zoom     |
+-----------------------------+               +-----------------------------+
```

---

### Workflow 5: Vector Inking & PDF Document Baking
```
[User draws on HTML5 Canvas overlay]
            |
            v
[Stroke captures array of {x, y} normalized to [0.0, 1.0]]
            |
            v
[Stored in IndexedDB: AnnotationRecord]
            |
            v
[User taps "Bake Annotations into PDF"]
            |
            v
[pdf-lib loads original PDF binary in Web Worker]
            |
            v
[Translates normalized coordinates to PDF dimensions & draws vectors]
            |
            v
[pdf-lib generates final Uint8Array]
            |
            v
[Overwrites original file via FileSystemWritableFileStream OR downloads file]
```

---

### Workflow 6: Portable Vault Backup (.ZIP) Export & Restore
- **Exporting Backup**:
  1. `BackupManager` queries IndexedDB for `library_state`, `schedules`, `study_tasks`, `user_profile`, and `annotations`.
  2. Creates a new `JSZip` instance.
  3. Appends formatted JSON files into a root `.mulberry/` folder inside the ZIP.
  4. Generates ZIP blob and triggers `saveAs(blob, "Mulberry_Vault_Backup_2026-10-09.zip")`.
- **Restoring Backup**:
  1. User selects `.zip` file via file input.
  2. `JSZip` decompresses and parses the JSON manifests.
  3. Validates schema versions.
  4. Merges or replaces records in IndexedDB.
  5. If a local vault folder is currently mounted, writes the JSON files back to disk.

---

## 9. Project Scaffolding & Directory Structure

To build this application from scratch, organize the React + Vite repository as follows:

```
mulberry-web/
├── public/
│   ├── favicon.svg
│   └── pdf.worker.min.mjs          # Mozilla PDF.js web worker asset
├── src/
│   ├── assets/                     # Brand logos, icons, illustrations
│   ├── components/
│   │   ├── common/
│   │   │   ├── Button.tsx
│   │   │   ├── Card.tsx
│   │   │   ├── Modal.tsx
│   │   │   ├── Toast.tsx
│   │   │   └── ProgressBar.tsx
│   │   ├── layout/
│   │   │   ├── AppShell.tsx        # Responsive scaffold with bottom nav / rail
│   │   │   ├── BottomNavBar.tsx    # Frosted mobile bottom nav
│   │   │   └── NavigationRail.tsx  # Desktop left navigation rail
│   │   ├── library/
│   │   │   ├── BookCardGrid.tsx    # 3:4 thumbnail cover card
│   │   │   ├── BookCardList.tsx    # Compact list view row
│   │   │   ├── BreadcrumbStrip.tsx # Folder hierarchy navigation pills
│   │   │   ├── CategoryChips.tsx   # Folder category pills
│   │   │   └── ContinueReading.tsx # Horizontal carousel
│   │   ├── desk/
│   │   │   ├── ReadingSessionCard.tsx # Session cards (Today/Upcoming)
│   │   │   ├── ScheduleSessionModal.tsx # Schedule session sheet
│   │   │   ├── WeeklyGoalMeter.tsx    # Progress bar and stats
│   │   │   ├── StudyTaskItem.tsx      # Task checkbox with PDF jump link
│   │   │   └── ExamCountdownCard.tsx  # Exam card with syllabus accordion
│   │   ├── reader/
│   │   │   ├── ReaderShell.tsx        # Top chrome, engine container, scrubber
│   │   │   ├── ContinuousScrollReader.tsx # TanStack Virtual + PDF.js engine
│   │   │   ├── SinglePagePdfViewer.tsx   # Paginated engine with tap zones
│   │   │   ├── InkingCanvas.tsx       # Vector markup overlay layer
│   │   │   ├── InkingDock.tsx         # Floating pen/highlighter/shape dock
│   │   │   ├── TocDrawer.tsx          # Table of contents & thumbnail sidebar
│   │   │   └── PageScrubber.tsx       # Bottom floating page slider
│   │   └── settings/
│   │       ├── ThemeSelector.tsx      # 6-theme preview tiles
│   │       ├── ProfileModal.tsx       # User credential editor
│   │       └── BackupRestoreCard.tsx  # ZIP export and import
│   ├── db/
│   │   ├── MulberryDB.ts           # Dexie IndexedDB instance & schema
│   │   └── VaultMetadataSync.ts    # JSON sync engine with .mulberry/
│   ├── hooks/
│   │   ├── useTheme.ts             # Theme manager & CSS token switcher
│   │   ├── useVault.ts             # FileSystemDirectoryHandle management
│   │   ├── usePdfDocument.ts       # PDF.js document loader hook
│   │   └── useInking.ts            # Pointer events, strokes, and undo/redo
│   ├── services/
│   │   ├── FileSystemManager.ts    # File System Access API & OPFS wrapper
│   │   ├── PdfBakingService.ts     # pdf-lib annotation flattening engine
│   │   ├── ThumbnailService.ts     # Web Worker thumbnail generator
│   │   └── BackupService.ts        # JSZip backup packaging & restoration
│   ├── store/
│   │   ├── useAppStore.ts          # Active tab, search, layout mode
│   │   └── useReaderStore.ts       # Current book, page, zoom, inking tool
│   ├── types/
│   │   └── index.ts                # TypeScript interfaces for all entities
│   ├── App.tsx                     # Root router and layout container
│   ├── index.css                   # Tailwind directives & CSS theme tokens
│   └── main.tsx                    # React DOM entry point
├── index.html
├── package.json
├── tailwind.config.js
├── tsconfig.json
└── vite.config.ts
```

---

## 10. Summary: Why This Blueprint Creates an Exact Replica

By following this specification:
1. **The Visuals & Ergonomics Match 1:1**: All 6 themes (`Olded`, `Paper`, `Midnight`, `Forest`, `Espresso`, `Dusk`) and icon semantics are preserved with the exact same color tokens and typography hierarchy.
2. **The Offline & Privacy Architecture is Maintained**: The web application never needs a cloud backend or login gate; it mounts local directories directly using the File System Access API and syncs portable `.mulberry/` metadata.
3. **The Dual-Engine PDF Experience is Recreated**: Combines continuous scroll for skimming and paginated single-page mode with 20/60/20% tap zones, double-tap zoom, and vector inking.
4. **The Academic Workflows are Seamless**: Reading sessions, weekly page goals, linked task jumps, exam countdowns, and Markdown syllabi operate as a cohesive study operating system.
