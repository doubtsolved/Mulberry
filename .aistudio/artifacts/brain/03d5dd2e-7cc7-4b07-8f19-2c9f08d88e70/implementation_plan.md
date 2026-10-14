# Mulberry Shelf: Settings Prof Migration, Full-Width Search, Fluid Sideways Tabs & Theme Gradients

Elevate the **Mulberry** medical workspace by moving MBBS Prof selection into Settings under the Profile sheet—unlocking an edge-to-edge full-width search bar in the Library—introducing seamless horizontal tab switching with `HorizontalPager`, subtle top-to-bottom background gradient washes across all 6 themes, a clean vector-animated medical bookmark graphic for the newly cleared Snips workspace, and highly detailed physics-based fluid animations across every UI interaction.

---

### User Review & Critical Decisions

> [!IMPORTANT]
> The following architectural and UX decisions synthesize your answers from Phase 1 and ground the technical implementation:

- **Confirmed Decision 1 (Tab Gesture & Edge Arbitration)**: Sideways sliding between tabs is implemented with `HorizontalPager` paired with Compose's nested scroll arbitration. Touch events originating on horizontal book shelves (`LazyRow`) scroll the shelves smoothly, while edge swipes and standard surface drags seamlessly translate between the main tabs (Library, Snips, Desk, Settings).
- **Confirmed Decision 2 (Snips Canvas Motif)**: All static mock samples in Snips are cleared. In their place, a custom vector-animated graphic featuring a medical study sheet with floating bookmarks, pulsating clipping guides, and shimmering highlighter sweeps illustrates the upcoming clipping workflow.
- **Confirmed Decision 3 (Theme Gradient Strategy)**: Subtle top-to-bottom gradient washes are infused into each of the 6 theme palettes (`Olded`, `Paper`, `Midnight`, `Forest`, `Espresso`, `Dusk`) on root backgrounds, screen headers, and card surfaces for atmospheric depth without visual clutter.
- **Confirmed Decision 4 (Prof Migration & Full-Width Search)**: The Prof selection pill is moved from the Library search row into the Settings Profile section and persisted in `UserProfile` / `SettingsViewModel`. The Library search bar expands to 100% width (`Modifier.fillMaxWidth()`), with the current Prof dynamically reflected in the top bar and subject filter chips.

---

## 1. Overview & Core Concept

- **What It Does**: Streamlines the core Mulberry UI:
  1. **Full-Width Library Search**: Removing the Prof pill frees horizontal space, providing an expansive, distraction-free search bar for instantaneous filtering across book titles, authors, subjects, and tags.
  2. **Profile-Anchored Curriculum**: Medical students select their active MBBS year (1st Prof, 2nd Prof, 3rd Prof Part 1, Final Prof) inside their personal Profile in Settings, where it naturally belongs alongside study goals and academic targets.
  3. **Seamless Sideways Tab Navigation**: Users can swipe naturally between tabs or tap bottom navigation icons with synchronized slide animations and haptic feedback.
  4. **Atmospheric Theme Gradients**: Elevates flat backgrounds into luxurious, eye-friendly gradient washes tailored to each of Mulberry's reading themes.
  5. **Animated Snips Graphic**: A clean, vector-animated illustration showing a medical bookmark and clipping reticle, marking the clean slate for the future snips architecture.
  6. **Fluid Motion System**: Physics-based springs (`spring(dampingRatio = 0.8f, stiffness = 400f)`), scale-down press states, and staggered reveals bring every screen to life.

- **Target Persona**: MBBS medical students and clinicians who need a distraction-free, fluid, aesthetically refined digital textbook and study environment.

---

## 2. User Experience & Visual Design

### 2.1 Full-Width Library Header & Search Bar
- **Top Bar**: Clean "Mulberry Shelf" title in Poppins SemiBold (26sp), paired with a subtle subtitle indicating the active curriculum (e.g., `1st Prof • 3 Subjects • 19 Books`).
- **Search Bar**: Full-bleed `OutlinedTextField` with rounded corners (16.dp), leading search icon (`FluentIcons.Search24Regular`), placeholder `"Search titles, authors, subjects, or tags..."`, and an animated clear icon (`FluentIcons.DismissCircle24Filled`) with spring scale reveal.
- **Subject Filter Chips**: Horizontally scrollable chips with bouncy spring scale animations when tapped, dynamically populated based on the active Prof configured in Settings.

### 2.2 Profile & Settings Prof Selection
- **Profile Hero Card**: Shows user avatar, name, and now displays an active badge: e.g., `1st Prof (1st Year) • MBBS`. Tapping the card opens the Profile Modal Bottom Sheet.
- **Curriculum Year Selector**: Inside `UserProfileBottomSheet`, a dedicated section titled **"MBBS Curriculum & Prof Year"** features interactive selectable cards for each of the 4 Profs:
  - 1st Prof (1st Year): Anatomy, Physiology, Biochemistry
  - 2nd Prof (2nd Year): Pathology, Pharmacology, Microbiology
  - 3rd Prof Part 1 (3rd Year): Forensic Medicine, Community Medicine, Ophthalmology, ENT
  - Final Prof (4th Year): General Medicine, General Surgery, OBG, Paediatrics, Orthopaedics, etc.
- **Real-time Sync**: Selecting a Prof immediately updates the Library tab's books, subjects, and shelves without requiring a manual refresh.

### 2.3 Sideways Tab Switching (`HorizontalPager`)
- Replaces static tab switching with a high-performance Compose `HorizontalPager(state = pagerState)`.
- Bottom navigation bar icons update smoothly in sync with the pager offset.
- Tapping a bottom bar item smoothly animates the pager using physics-based spring interpolation (`pagerState.animateScrollToPage(targetPage)`).

### 2.4 Subtle Atmospheric Gradients Across Themes
- **Olded (Sepia)**: Soft warm parchment top `0xFFFBF0D9` to muted amber sepia `0xFFF4E5C0`.
- **Paper (Clean White)**: Crisp alabaster `0xFFFFFFFF` to soft ice-tinted slate `0xFFF1F5F9`.
- **Midnight (OLED)**: Deep space pitch `0xFF0D1117` down to true black `0xFF000000`.
- **Forest (Sage Dark)**: Deep botanical moss `0xFF14201A` to dark pine `0xFF0C130F`.
- **Espresso (Mocha Dark)**: Roasted espresso crema `0xFF221A17` to deep dark cacao `0xFF140F0D`.
- **Dusk (Lavender/Indigo)**: Twilight violet `0xFF191526` to nocturnal indigo `0xFF0E0C16`.

### 2.5 Animated Snips Graphic (Medical Bookmark Motif)
- Clears the mock data list in `SnipsScreen.kt`.
- Renders an interactive Canvas with:
  - Floating stylized textbook page with rounded corners and faint ruled study lines.
  - A glowing ribbon bookmark with subtle sinusoidal vertical bobbing (`animateFloat` infinite transition).
  - Animated clipping guides and corner reticles indicating an area being snipped.
  - A gentle highlighter beam sweeping horizontally across high-yield text lines.
- Accompanied by a headline `"High-Yield Snips"`, subtitle `"Your study clipping workspace will be rebuilt here from scratch"`, and an interactive badge `"Ready for Ingestion"`.

---

## 3. Key Product Decisions & Trade-Offs

- **Decision 1: Full-Width Search Bar vs. Header Space**
  - *Chosen Approach*: Move Prof selector to Profile in Settings; expand search bar to 100% width.
  - *Why*: The search bar is the primary discovery tool for students searching hundreds of textbook topics. Dedicated full width prevents text truncation and awkward touch targets.
  - *Alternatives Considered*: Retaining a compact icon button next to search—rejected because it competes with search clear actions and clutter the header.

- **Decision 2: HorizontalPager for Tab Navigation**
  - *Chosen Approach*: Standard Jetpack Compose `HorizontalPager` with nested scrolling support.
  - *Why*: Provides native Android physics, gesture-driven page transitions, and effortless bidirectional synchronization with the M3 bottom bar.
  - *Preserving Shelf Scrolls*: Because Compose `LazyRow` has built-in horizontal scroll consumption, dragging within a book shelf scrolls the shelf until its scroll extent is reached, at which point page gestures seamlessly take over.

- **Decision 3: Universal Microsoft Fluent Icons**
  - *Chosen Approach*: Strictly use `FluentIcons` definitions across all screens, tabs, and modals (`Search24Regular`, `DismissCircle24Filled`, `Bookmark24Regular`, `Crop24Regular`, `Settings24Regular`, etc.).

---

## 4. Technical Architecture & Data Strategy

```
┌────────────────────────────────────────────────────────────────────────┐
│                              MainActivity                              │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │     HorizontalPager(state = pagerState, count = visibleTabs)     │  │
│  │  ┌──────────────┬──────────────┬──────────────┬───────────────┐  │  │
│  │  │   Page 0:    │   Page 1:    │   Page 2:    │    Page 3:    │  │  │
│  │  │LibraryScreen │ SnipsScreen  │   DeskRoot   │SettingsScreen │  │  │
│  │  └──────────────┴──────────────┴──────────────┴───────────────┘  │  │
│  └──────────────────────────────────────────────────────────────────┘  │
│  ┌──────────────────────────────────────────────────────────────────┐  │
│  │                MulberryBottomNavBar (Synced with Pager)          │  │
│  └──────────────────────────────────────────────────────────────────┘  │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
          ┌─────────────────────────┴─────────────────────────┐
          ▼                                                   ▼
┌───────────────────────────────────┐       ┌───────────────────────────────────┐
│        MulberryViewModel          │       │         SettingsViewModel         │
│  - allBooks (Flow)                │       │  - userProfile (Flow)             │
│  - selectedProf (Flow/State)      │◄─────►│    * selectedProfId               │
│  - currentTab (Flow)              │       │  - activeTheme (Flow)             │
└───────────────────────────────────┘       └───────────────────────────────────┘
```

### Component & File Mapping:
1. `app/src/main/java/com/example/ui/theme/Color.kt`:
   - Add `backgroundGradient: Brush` and `gradientColors: List<Color>` to `MulberryColors`.
   - Update all 6 theme palettes with distinctive, subtle top-to-bottom gradients.
2. `app/src/main/java/com/example/data/model/UserProfile.kt`:
   - Add `selectedProfId: String = "1st Prof"` to `UserProfile`.
3. `app/src/main/java/com/example/viewmodel/MulberryViewModel.kt` & `SettingsViewModel.kt`:
   - Bind `selectedProf` between `UserProfile` and `MulberryViewModel`.
4. `app/src/main/java/com/example/ui/screens/LibraryScreen.kt`:
   - Expand `OutlinedTextField` search bar to `Modifier.fillMaxWidth()`.
   - Remove redundant Prof pill button and bottom sheet from the search row.
   - Read `selectedProf` directly from the ViewModel's state.
   - Apply subtle top-to-bottom background gradient wash and fluid spring animations.
5. `app/src/main/java/com/example/ui/screens/UserProfileBottomSheet.kt` & `SettingsScreen.kt`:
   - Add MBBS Prof selection radio card group under Profile settings with Year & Subject tags.
   - Display active Prof badge in the Settings profile hero card.
6. `app/src/main/java/com/example/MainActivity.kt`:
   - Replace `AnimatedContent` tab switcher with `HorizontalPager(state = pagerState)`.
   - Synchronize pager state with `MulberryBottomNavBar` selections.
7. `app/src/main/java/com/example/ui/screens/SnipsScreen.kt`:
   - Clear all mock items (`CURATED_HIGH_YIELD_SNIPS`).
   - Implement custom vector animation with medical bookmark motif, floating shimmer cards, and highlighter pulses.
