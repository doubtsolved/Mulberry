# Implementation Plan — Minimalist Study Page Revamp Placeholder & Snips Tab

Update the Study page to an animated thinking state, introduce a new "Snips" tab positioned second in the navigation bar, remove all accent pills, badges, or tags, and keep both placeholder screens minimal, focused, and beautifully animated.

---

## User Preferences & Revisions
- **No Accent Pills / Badges**: Do not display any accent pills, status tags, or chip badges on either screen. Keep the design sleek and distraction-free.
- **No Subtitle on Snips Tab**: Omit the long descriptive subtitle.
- **Copy on Study Page**:
  - **Headline**: *"Hold Tight, Ajit is Thinking..."*
  - **Supporting Line**: *"Synapses are firing and notes are brewing. A smarter way to master your coursework is dropping soon."*
- **Copy on Snips Tab**:
  - **Headline**: *"Something's Cooking. Don't Enter the Kitchen!"*
  - **Supporting Line**: *"Master chefs at work. Simmering the finest visual scrapbook to perfection."*
- **Tab Bar Ordering**: `Library` $\rightarrow$ `Snips` $\rightarrow$ `Study` $\rightarrow$ `Settings`.

---

## Proposed Changes

### Navigation & Tab Bar

#### [BottomNavBar.kt](file:///app/src/main/java/com/example/ui/navigation/BottomNavBar.kt)
- Update `MulberryTab` enum:
  1. `LIBRARY` ("Library", Book icon)
  2. `SNIPS` ("Snips", Scissors / Scrapbook icon)
  3. `STUDY` ("Study", Lightbulb / TaskList icon)
  4. `SETTINGS` ("Settings", Gear icon)
- Ensure all 4 tabs maintain 48dp touch targets, smooth pill animation, and active theme colors.

#### [FluentIcons.kt](file:///app/src/main/java/com/example/ui/components/FluentIcons.kt)
- Add vector icons for `Snips24Regular` and `Snips24Filled` (scissors / visual cut motif).

---

### Screens & UI Components

#### [AgendaScreen.kt](file:///app/src/main/java/com/example/ui/screens/AgendaScreen.kt)
- Clear existing checklist/exam UI cards while retaining underlying database models and DAOs for future use.
- Implement animated Canvas & Compose graphical thinking scene:
  - **Pulsing Lightbulb Graphic**: Gentle breathing scale oscillation with an illuminated warm radial glow.
  - **Ascending Thought Bubbles**: Particle bubbles drifting upward with smooth sinusoidal drift.
  - **Clean Typography (No Accent Pills)**:
    - Title: *"Hold Tight, Ajit is Thinking..."* (Bold, 22sp, `colors.textPrimary`)
    - Subtext: *"Synapses are firing and notes are brewing. A smarter way to master your coursework is dropping soon."* (14sp, `colors.textMuted`, centered)

#### [SnipsScreen.kt](file:///app/src/main/java/com/example/ui/screens/SnipsScreen.kt)
- Create a dedicated Composable `SnipsScreen`:
  - **Top Bar**: Minimal title header *"Snips"*.
  - **Playful Cooking Graphic**:
    - Floating and slightly bobbing chef hat with subtle tilt animation.
    - Ascending culinary steam curves and rotating sparkle stars.
  - **Clean Copy (Zero Subtitles, Zero Accent Pills)**:
    - Primary Header: *"Something's Cooking. Don't Enter the Kitchen!"*
    - Supporting Subtext: *"Master chefs at work. Simmering the finest visual scrapbook to perfection."*

#### [MainActivity.kt](file:///app/src/main/java/com/example/MainActivity.kt)
- Route `MulberryTab.SNIPS` to `SnipsScreen` in `AnimatedContent`.
- Update back-press behavior so pressing back on `SNIPS` returns smoothly to `LIBRARY`.

---

## Verification Plan

### Automated Verification
- Run `compile_applet` to confirm clean compilation with no syntax or type errors.

### Manual Verification
1. **Tab Structure**: Verify 4 tabs in bottom navigation: `Library`, `Snips`, `Study`, `Settings`.
2. **Snips Screen**:
   - Verify chef hat floating/steam animation.
   - Verify copy: *"Something's Cooking. Don't Enter the Kitchen!"*.
   - Confirm **zero accent pills**, chips, or subtitles appear.
3. **Study Screen**:
   - Verify lightbulb pulse and drifting thought bubbles animation.
   - Verify copy: *"Hold Tight, Ajit is Thinking..."*.
   - Confirm **zero accent pills** or chips appear.
4. **Theme Adaptation**: Verify seamless rendering across all 6 themes (Olded Sepia, Paper, Midnight OLED, Forest, Espresso, Dusk).
