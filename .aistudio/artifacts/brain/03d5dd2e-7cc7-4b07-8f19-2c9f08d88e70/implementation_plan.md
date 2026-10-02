# Sleek Tap Animation, HD Page Sharing & Circle to Search Gesture

Add spring-eased slide and fade animations when tapping the center 60% to hide/show reader bars, introduce an instant high-definition page share button on the scrubber bar, and activate Google Circle to Search (Omnient / ACTION_ASSIST fallback) on long-press.

## User Review & Critical Decisions

> [!IMPORTANT]
> The following implementation decisions were confirmed during the clarification phase:

- **Top & Bottom Bar Animation**: Confirmed **Smooth slide and fade with spring easing**. When tapping the middle 60% zone, the top bar smoothly slides up offscreen (-100% Y) and fades out, while the bottom scrubber bar slides down (+100% Y) and fades out with low-bounciness spring easing. System bars (status & navigation) synchronize with `WindowInsetsControllerCompat`.
- **Page Sharing Behavior**: Confirmed **Open Android system share sheet immediately**. Tapping the new share button at the very end of the scrubber bar immediately renders the current page at 2.5x high definition, saves it to an isolated cache file, generates a secure `content://` URI via `FileProvider`, and presents the native Android system share sheet (`Intent.ACTION_SEND` with `image/png`).
- **Circle to Search Gesture**: Confirmed **Native Omnient Activity overlay with automatic ACTION_ASSIST screenshot payload fallback**. Long-pressing on any page displays a glowing circular ripple animation with haptic feedback, renders the current page view, checks for native Omnient activity (`com.google.android.apps.search.omnient.OmnientActivity`), and gracefully falls back to `Intent.ACTION_ASSIST` with the screenshot extra and Google Lens package resolution for universal unrooted third-party device compatibility.

---

## 1. Overview & Core Concept

- **What It Does**: Enhances the PDF reading experience by replacing abrupt controls toggling with fluid spring motion, adding a one-tap high-definition page exporter/sharer directly to the reading bar, and empowering users to search any visual element in their documents via Circle to Search by long-pressing anywhere on a page.
- **Target Audience / Persona**: Readers, students, researchers, and professionals who need an immersive, uninterrupted reading canvas with immediate sharing and visual lookups.
- **Key Value**: Delivers a fluid, premium reading experience matching top-tier Android 14+ system apps without disruptive layout jumps or jarring transitions.

---

## 2. User Experience & Visual Design

### Key User Flows

1. **Center 60% Tap (Immersive Toggle)**:
   - User taps the center of the reading canvas.
   - Top navigation bar slides upward with spring easing and fades out; bottom floating scrubber bar slides downward with spring easing and fades out.
   - Status bar and navigation bar transition into transient immersive mode.
   - Tapping again smoothly slides and fades both controls back into view without content repositioning or canvas jumping.
2. **Scrubber Bar HD Page Share**:
   - User views the floating scrubber bar at the bottom.
   - At the far right end, following the page number badge (`X / Y`), a subtle vertical separator precedes a polished `Share Page` icon button (`FluentIcons.Share24Regular`).
   - Tapping triggers an instant haptic micro-tick, renders the active page at crystal-clear high definition (2.5x supersampled), and opens the native Android Share Sheet with page preview, ready to be sent to messaging apps, email, or drive.
3. **Long-Press Circle to Search**:
   - User presses and holds on any word, image, chart, or diagram within the PDF.
   - A soft, glowing circular lens indicator pulses from the touch coordinates with haptic confirmation.
   - The reader prepares the high-res snapshot of the screen/page and initiates the Omnient / Circle to Search overlay or `ACTION_ASSIST` with screenshot payload.

### Visual Identity & Theme Tokens

- **Palette Continuity**: Inherits the active Mulberry theme (Paper, Midnight, Forest, Espresso, Dusk, Olded) from `LocalMulberryColors.current`.
- **Scrubber Bar Layout**:
  - Left: Previous page chevron (20.dp, 36.dp touch target).
  - Center: Material 3 Slider with smooth thumb tracking and theme-tinted active track.
  - Right 1: Next page chevron (20.dp, 36.dp touch target).
  - Right 2: Page pill badge (`X / Y`) with 12.sp Inter SemiBold.
  - Right 3: Subtle vertical divider (1.dp width, 16.dp height, `colors.borderSubtle`).
  - Right 4: High-Def Share Button (36.dpIconButton, `FluentIcons.Share24Regular`, 18.dp icon, `colors.primary`).
- **Motion Specs**:
  - Spring Spec: `spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessMediumLow)`.
  - Stagger / Alpha: Combined with `fadeIn(spring())` and `fadeOut(spring())` to prevent visual clipping at screen edges.

---

## 3. Key Product Decisions & Trade-Offs

### Decision 1: Overlaid FrameLayout vs. Linear Vertical Flow
- **Chosen Approach**: Switch `activity_reader.xml` root layout from `LinearLayout` to `FrameLayout`, allowing `single_page_compose_view` to occupy `match_parent` underneath and `top_bar_compose_view` to sit overlaid at `layout_gravity="top"`.
- **Why**: In a `LinearLayout`, hiding the top bar changes the height of the pager container below it, causing the PDF bitmap to stretch or jump during the transition. In a `FrameLayout`, the PDF viewport is fixed and stable while the top bar floats and glides above it.
- **Alternatives Considered**: Hosting the entire top bar inside `SinglePagePdfViewer` composable. (Rejected to avoid unnecessary refactoring of the existing `ReaderTopBarScreen` sheet state and search controllers).

### Decision 2: Multi-Tier Circle to Search Invocation
- **Chosen Approach**: A 3-tier cascade:
  1. Try native Google Omnient component (`com.google.android.apps.search.omnient.OmnientActivity` / `com.google.android.googlequicksearchbox`).
  2. Fall back to standard Android `Intent.ACTION_ASSIST` with assist bundle and screenshot URI payload (`Intent.EXTRA_STREAM` / `android.intent.extra.ASSIST_INPUT_HINT_KEYBOARD`).
  3. Fall back to Google Lens visual search activity if installed (`com.google.ar.lens` or `com.google.android.googlequicksearchbox`).
- **Why**: Circle to Search is a privileged system feature on newer Pixel and Galaxy devices. A multi-tier fallback ensures it works gracefully on every physical phone, emulator, and tablet without crashes.

---

## 4. Technical Architecture & Data Strategy

```
┌────────────────────────────────────────────────────────────────────────┐
│                          ReaderActivity                                │
│                                                                        │
│   FrameLayout (Full Screen Bounds)                                     │
│   ┌────────────────────────────────────────────────────────────────┐   │
│   │ SinglePagePdfViewer (single_page_compose_view)                 │   │
│   │                                                                │   │
│   │   [HorizontalPager & PdfRenderer]                              │   │
│   │    - Tap Left 20%   -> Previous Page                           │   │
│   │    - Tap Right 20%  -> Next Page                               │   │
│   │    - Tap Center 60% -> toggleControlsState                     │   │
│   │    - Long-Press     -> triggerCircleToSearch(pageBitmap, pt)   │   │
│   │                                                                │   │
│   │   [AnimatedVisibility (Spring Easing)]                         │   │
│   │    └── Floating Page Scrubber Bar                              │   │
│   │         [Prev] [Slider] [Next] [X / Y Badge] [Divider] [Share] │   │
│   │                                                          │     │   │
│   │                                                          ▼     │   │
│   │                                            shareCurrentPage()  │   │
│   └────────────────────────────────────────────────────────────────┘   │
│                                                                        │
│   ┌────────────────────────────────────────────────────────────────┐   │
│   │ ReaderTopBarScreen (top_bar_compose_view)                      │   │
│   │   [AnimatedVisibility (Spring Easing)]                         │   │
│   │    └── 56.dp Persistent App Bar & Controls                     │   │
│   └────────────────────────────────────────────────────────────────┘   │
└────────────────────────────────────────────────────────────────────────┘
```

### State & Handler Mapping

| Interaction | Trigger Source | State / Handler | Visual / Functional Output |
|---|---|---|---|
| **Center Tap** | SinglePdfPageItem `onTapZone` (0.20f..0.80f) | `onToggleControls(visible)` | Coordinates `AnimatedVisibility` for TopBar (-100% Y) & Scrubber Bar (+100% Y) with spring easing + system bar immersive toggle. |
| **Share Page Tap** | Scrubber Bar `IconButton` (new) | `onSharePage(currentPage)` | Renders 2.5x supersampled Bitmap of current page, writes to `cache/shared_pages/`, opens `Intent.createChooser` with `Intent.ACTION_SEND`. |
| **Long-Press Page** | SinglePdfPageItem `detectTapGestures(onLongPress)` | `onLongPress(offset)` | Emits haptic vibration, renders visual ripple lens, resolves Omnient/ACTION_ASSIST/Lens intent. |
