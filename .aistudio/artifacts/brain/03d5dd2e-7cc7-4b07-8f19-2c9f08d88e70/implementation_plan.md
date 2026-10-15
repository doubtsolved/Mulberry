# Implementation Plan: Subtle Fluid Translation Animations for Category Expand & Collapse

Deliver subtle, fluid motion choreography when opening and closing expanded book categories across both large-screen/landscape dual-pane layouts and mobile phone portrait screens.

---

## User Review & Critical Decisions

> [!IMPORTANT]
> **Confirmed Motion Physics (from user interview):**
> 1. **Large Screen / Landscape Dual-Pane:** Subtle horizontal slide from the right with gentle fade (`slideInHorizontally` + `fadeIn`, spring-damped, low bounce) for the supporting pane, accompanied by continuous scale feedback.
> 2. **Mobile Portrait Screen:** Horizontal push-and-pop translation with gentle fade (`AnimatedContent` with forward slide-in / subtle parallax slide-out on open, and reverse slide-out on back/dismiss).

---

## 1. Overview & Motion Concept

- **What It Does**: Enhances the Mulberry Library with fluid spatial transitions. When a medical student taps to expand a resource type (e.g., "Standard Textbook" or "Handwritten Notes"):
  - On **tablets / landscape**: The main shelves remain anchored on the left while the supporting category pane glides in gracefully from the right edge with a soft fade and spring physics. Dismissing via `X` or selecting another category smoothly animates out or morphs content in place.
  - On **phones**: The screen transitions smoothly from the main shelves to the dedicated category view via an organic horizontal push transition (28% offset translation + gentle crossfade), reversing naturally when pressing back.
- **Visual Feel**: Fluid, purposeful, and lightweight (duration $\le$ 300ms) with zero layout jarring.

---

## 2. User Experience & Visual Design

### Motion Choreography Specifications

```
Large Screen / Landscape (Dual-Pane)
┌───────────────────────────┬───────────────────────────────────────────┐
│ Left Pane (Anchored)      │ Right Supporting Pane (Animated)          │
│ • Header & Search Bar     │ ➔ Slides in from right (+25% offset)      │
│ • Subject Filter Chips    │ ➔ Soft Alpha Fade (0.0 -> 1.0)            │
│ • Active Category Pill    │ ➔ Spring Damping: Low Bouncy              │
│   (Tinted Highlight)      │ ➔ Reverses on 'X' (Dismiss)               │
└───────────────────────────┴───────────────────────────────────────────┘

Mobile Screen (Single-Pane Push / Pop)
┌───────────────────────────┐         ┌───────────────────────────┐
│ Main Library Shelf        │         │ Dedicated Category View   │
│ (Slides left -15% & fades)│ ──────> │ (Slides in +28% from right│
│                           │ <────── │  with gentle fade)        │
└───────────────────────────┘  [Back] └───────────────────────────┘
```

- **Easing & Specs**:
  - `Enter`: `spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessMediumLow)` combined with `fadeIn(animationSpec = tween(220, easing = LinearOutSlowInEasing))`.
  - `Exit`: `tween(durationMillis = 200, easing = FastOutSlowInEasing)` combined with `fadeOut(animationSpec = tween(160))`.

---

## 3. Key Technical Decisions & Trade-Offs

- **Decision 1: AnimatedVisibility vs AnimatedContent**:
  - *Large Screen*: Use `AnimatedVisibility` for the right supporting pane alongside an animated divider, allowing the left pane to dynamically occupy full width when collapsed or settle at `380.dp` with an animated transition.
  - *Mobile Phone*: Use `AnimatedContent` keyed on `expandedCategory != null` (and preserving category name) to achieve standard push/pop navigation physics without needing an extra Navigation fragment.
- **Decision 2: Sub-pixel Clamping**:
  - Offsets calculated relative to container width using percentage lambda (`{ (it * 0.25f).toInt() }`) ensuring consistent fluid velocity across devices of different pixel densities.

---

## 4. Technical Implementation & State Strategy

```
                          ┌───────────────────────────┐
                          │  expandedCategory State   │
                          └─────────────┬─────────────┘
                                        │
                    ┌───────────────────┴───────────────────┐
                    ▼                                       ▼
       [isWideScreen == true]                  [isWideScreen == false]
   ┌───────────────────────────────┐       ┌───────────────────────────────┐
   │ Row Layout                    │       │ AnimatedContent               │
   │ Left: LibraryMainShelfContent │       │ Null -> Main Shelf Screen     │
   │ Divider: AnimatedVisibility   │       │ Non-null -> Dedicated Screen  │
   │ Right: AnimatedVisibility     │       │ Forward/Backward Push & Pop   │
   │  - slideInHorizontally (+25%) │       │  - slideInHorizontally (+28%) │
   │  - fadeIn / fadeOut           │       │  - slideOutHorizontally (-15%)│
   └───────────────────────────────┘       └───────────────────────────────┘
```

### Affected Files
- `app/src/main/java/com/example/ui/screens/LibraryScreen.kt`:
  - Wrap large-screen supporting pane in `AnimatedVisibility` with custom enter/exit slide and fade.
  - Wrap mobile single-pane transition in `AnimatedContent` with push/pop slide specs.

---

## Verification Plan

### Automated Verification
- Run `compile_applet` to verify clean compilation of Jetpack Compose animation transitions and specs.

### Manual / Visual Verification
1. **Large Screen / Landscape View**:
   - In landscape orientation or wide screen, tap the expand button (`>`) on any category (e.g. *Standard Textbook*).
   - Verify the right supporting pane glides in smoothly from the right with a soft fade.
   - Tap `X` or another category header to dismiss/switch; verify smooth dismissal without flicker.
2. **Mobile Portrait View**:
   - In portrait mode, tap expand on any category.
   - Verify the dedicated full-screen view slides in horizontally from the right with a push transition.
   - Tap the back button; verify the view slides out to the right with a pop transition back to the main shelf.
