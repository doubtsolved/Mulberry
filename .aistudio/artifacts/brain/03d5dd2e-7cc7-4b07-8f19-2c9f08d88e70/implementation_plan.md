# Customizable Navigation Tabs: Drag-and-Drop Reordering, Visibility Toggles, and Quick Long-Press Access

## Overview
This plan introduces full tab customization to Mulberry's navigation system. Users will be able to rearrange the bottom navigation tabs using an intuitive drag-and-drop list with grip handles, hide and reveal non-essential tabs, and access this customization directly by long-pressing any tab in the bottom bar or via the Settings screen.

---

## 1. User Decisions & Clarifications Summary
Based on the clarifying questions:
- **Rearrangement Interaction**: Drag-and-drop reorder list with tactile handle grips.
- **Tab Visibility**: Support both reordering and toggling visibility for non-essential tabs (with protection ensuring at least 2 tabs, including Library, remain active so the app is always navigable).
- **Access Point**: Direct shortcut when long-pressing any bottom tab on the navigation bar, plus a dedicated "Navigation & Tabs" row in Settings under "READING & APPEARANCE".

---

## 2. Architecture & Data Flow

### A. Data Model & Persistence
- **Model**:
  ```kotlin
  data class NavTabConfig(
      val tab: MulberryTab,
      val isVisible: Boolean = true
  )
  ```
- **Storage**: Persist in `mulberry_prefs` SharedPreferences using a lightweight serialized format (e.g., `LIBRARY:true,SNIPS:true,DESK:true,SETTINGS:true`).
- **Default State**: All 4 tabs visible in default order `[LIBRARY, SNIPS, DESK, SETTINGS]`.
- **Validation Rules**:
  - `LIBRARY` is the core anchor and cannot be hidden.
  - A minimum of 2 tabs must remain visible at all times.
  - If the currently active tab is hidden, active tab automatically transitions to the first visible tab (`LIBRARY`).

### B. State Management in `MulberryViewModel`
- Add StateFlow properties:
  - `tabConfigs: StateFlow<List<NavTabConfig>>`
  - `visibleTabs: StateFlow<List<MulberryTab>>`
- Add actions:
  - `reorderTabs(fromIndex: Int, toIndex: Int)`
  - `toggleTabVisibility(tab: MulberryTab, visible: Boolean)`
  - `resetTabsToDefault()`

### C. Bottom Navigation Bar Enhancement (`BottomNavBar.kt`)
- Update `MulberryBottomNavBar`:
  - Accept `tabs: List<MulberryTab>` (the active visible tabs) instead of hardcoded `MulberryTab.entries`.
  - Add `onTabLongClick: (MulberryTab) -> Unit` callback.
  - Use `Modifier.combinedClickable` (or custom pointer gesture) with haptic feedback on tab items:
    - Tap: selects tab (`onTabSelected(tab)`)
    - Long-press: opens customization modal (`onTabLongClick(tab)`)
  - Keep spring animations and active pill indicator adapting smoothly to 2, 3, or 4 tabs.

---

## 3. UI Implementation: `CustomizeTabsSheet.kt`

Create a Material 3 Expressive Bottom Sheet modal for tab customization:
1. **Sheet Header**:
   - Title: "Customize Navigation"
   - Subtitle: "Drag to reorder tabs • Toggle to hide or show"
   - Action: "Reset to Default" button with Fluent `ArrowSync24Regular` icon
2. **Interactive Reorderable Tab List**:
   - Each item row contains:
     - **Leading Drag Handle Grip**: `FluentIcons.Reorder24Regular` with drag gesture listener allowing live vertical dragging and dynamic reordering.
     - **Tab Icon & Label**: Fluent icon (Book, Scissors, Calendar, Settings) with tab name.
     - **Visibility Switch**: Material 3 switch / toggle button.
     - **Protection Badge**: Locked indicator for Library ("Required").
     - **Visual Feedback**: Elevated shadow, primary border accent, and scale effect when an item is currently being dragged.
3. **Live Bottom Bar Preview**:
   - A compact, real-time mini preview of the bottom navigation bar at the bottom of the sheet, reflecting the reordered tabs immediately.
4. **Done Action Button**:
   - Primary filled button to save and dismiss sheet.

---

## 4. Settings Screen Integration (`SettingsScreen.kt`)
- Under the `READING & APPEARANCE` category group:
  - Add `SettingsItemRow`:
    - Icon: `FluentIcons.Navigation24Regular` (or `Reorder24Regular`)
    - Title: "Navigation & Tabs"
    - Value label: e.g. "4 Tabs Active"
    - OnClick: opens the `CustomizeTabsSheet`

---

## 5. Icon Support in `FluentIcons.kt`
- Add official Microsoft Fluent icon vectors:
  - `Reorder24Regular` (classic 3-line drag grip handle)
  - `Navigation24Regular` (navigation compass/pointer)

---

## 6. Verification & Quality Assurance
1. **Compilation Check**: Run `compile_applet` to confirm error-free compilation and imports.
2. **Unit Tests**: Run `gradle :app:testDebugUnitTest` to verify repository and ViewModel state.
3. **Interaction Verification**:
   - Long-press on any bottom tab opens the sheet.
   - Dragging an item changes order dynamically and persists across app restarts.
   - Hiding non-essential tabs updates the bottom bar layout evenly.
   - Library tab cannot be hidden.
   - Reset button restores default 4-tab configuration.
