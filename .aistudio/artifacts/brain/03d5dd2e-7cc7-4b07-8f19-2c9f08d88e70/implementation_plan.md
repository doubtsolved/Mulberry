# Universal Fluid Physics, Tactile Haptics & Glass Styling Plan (Tabs & Setup Wizard)

Implement a universal, physics-based fluid interaction model and glassmorphic styling system across all tab buttons, filter chips, and the Setup Wizard UI in Mulberry.

---

## User Review & Critical Decisions

> [!IMPORTANT]
> The following user preferences were confirmed during clarification:
> - **Extended Universal Scope**: Applied across all tab buttons (bottom glass dock, navigation rail, catalog curriculum stage tabs, subject filter chips) **AND** the Setup Wizard UI buttons (Get Started, Continue, Launch Mulberry, Back, and Permission grant buttons).
> - **Press Physics Reaction**: Spring-loaded bouncy scale-down to `0.94f` on touch down, springing back to `1.0f` on release with `Spring.DampingRatioMediumBouncy`.
> - **Tactile Haptic Pattern**: Dual-stage tactile feedback with a light tick on touch down (`HapticFeedbackType.TextHandleMove`) and a crisp selection confirmation click on release.
> - **Setup Wizard Button Styling**:
>   - Primary CTA buttons (Get Started, Continue, Launch Mulberry) styled with a frosted glass glow, spring bounce, and dual haptics.
>   - Secondary buttons (Back and Permission grant buttons) styled with translucent frosted glass fill and hairline borders.

---

## 1. Overview & Core Concept

Create a shared, reusable Compose modifier and button interaction system (`FluidGlassInteractiveSystem`):
1. **Interactive Touch Physics**:
   - Detects press gestures cooperatively via `pointerInput`.
   - On down press: immediately compresses scale to `0.94f` via bouncy spring physics and emits light tactile tick (`TextHandleMove`).
   - On tap completion: emits selection confirmation click, executes callback, and snaps back to `1.0f` with elastic overshoot.
   - On gesture cancellation (finger dragged away): smoothly relaxes back to `1.0f` without trigger.
2. **Glassmorphic Surface System**:
   - Frosted translucent fill derived strictly from the active theme (`colors.surfaceCard` and `colors.surface.copy(alpha = 0.70f..0.85f)`).
   - Hairline specular border (`BorderStroke(1.dp)`) with top rim reflection.
   - Ambient drop shadow on active selection and primary actions.

---

## 2. Interaction & Component Coverage

```
┌─────────────────────────────────────────────────────────────────────────────┐
│                   Universal Fluid Tab & Button Lifecycle                    │
│                                                                             │
│   Touch Down                    Hold / Transit                 Release      │
│   ├── Light Tick Haptic         ├── Compressed (0.94f)         ├── Confirm  │
│   └── Spring scale: 1.0 -> 0.94 └── Dynamic Glass Highlight    └── Spring 1 │
│                                                                             │
│                  Spring Spec: DampingRatioMediumBouncy                      │
│                  Scale Range: 0.94f (pressed) <-> 1.0f (idle)               │
└─────────────────────────────────────────────────────────────────────────────┘
```

### Components Receiving the Universal System:
1. **Floating Dock Tabs** (`BottomNavBar.kt`): Desk, Library, Settings items in `ElasticGlassBottomBar`.
2. **Navigation Rail Tabs** (`NavigationRail.kt`): Rail items for tablets and foldables.
3. **Curriculum Stage Tabs** (`LibraryScreen.kt` & `library/LibraryScreen.kt`): 1st Prof, 2nd Prof, 3rd Prof Part 1, Final Prof.
4. **Subject Filter Chips** (`LibraryScreen.kt`): "All", "Anatomy", "Physiology", "Biochemistry", "Pathology", etc.
5. **Setup Wizard Buttons** (`SetupWizardScreen.kt`):
   - **Step 1**: `ShimmeringGetStartedButton` ("Get Started ->") with frosted glass glow and spring compression.
   - **Step 2**:
     - `profile_prof_picker` interactive card.
     - Secondary "Back" button (frosted glass outline).
     - Primary "Continue ->" CTA button (frosted glass glow with spring bounce & dual haptics).
   - **Step 3**:
     - Storage permission & notification permission grant buttons (frosted glass pill with primary accent).
     - Secondary "Back" button (frosted glass outline).
     - Primary "Launch Mulberry ->" CTA button (frosted glass glow with spring bounce & dual haptics).

---

## 3. Technical Architecture & Component Changes

```
┌────────────────────────────────────────────────────────────────────────┐
│                   FluidGlassInteractiveSystem                          │
│   • Spring Animatable(scale: 1.0f)                                     │
│   • pointerInput(press/release/cancel)                                 │
│   • LocalHapticFeedback (tick on down, click on select)                │
│   • Frosted glass background & specular border brush                   │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
    ┌───────────────────────────────┼───────────────────────────────┐
    ▼                               ▼                               ▼
┌───────────────────────┐  ┌───────────────────────┐  ┌───────────────────────┐
│   Navigation Tabs     │  │   Catalog & Chips     │  │   Setup Wizard UI     │
│ • Floating Glass Dock │  │ • Prof Stage Tabs     │  │ • Get Started CTA     │
│ • Navigation Rail     │  │ • Subject Filter Chips│  │ • Continue & Launch   │
│                       │  │                       │  │ • Back & Permissions  │
└───────────────────────┘  └───────────────────────┘  └───────────────────────┘
```

---

## 4. Verification & Validation Plan

- Verify compilation via `compile_applet`.
- Verify spring bounce compression (scale down to 0.94f) and rebound across:
  - Bottom bar dock tabs
  - Catalog curriculum selectors and subject filter chips
  - Setup Wizard action buttons (Get Started, Continue, Launch Mulberry, Back, Permissions).
- Verify tactile dual-stage haptics on real device / preview.
- Ensure zero hardcoded hex colors and strict theme synchronization across all 6 themes.
