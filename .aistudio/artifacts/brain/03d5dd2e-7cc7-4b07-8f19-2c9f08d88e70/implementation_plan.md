# Redesign "Contribute to Mulberry" Modal Sheet

Update the **Contribute to Mulberry** bottom sheet modal to match the new grouped layout, copy, and action routing across all entry points in the app (Settings and Announcement Carousel `OPEN_CONTRIBUTE` actions), using Microsoft Fluent vector icons exclusively.

## User Review & Critical Decisions

> [!IMPORTANT]
> **Cross-App Synchronization & Fluent Iconography**:
> - **No Emojis**: All leading icons use Microsoft Fluent 24dp vector icons (`DrinkCoffee24Regular`, `ArrowUpload24Regular` / `DocumentAdd24Regular`, `Code24Regular`, `Send24Regular`, and `Share24Regular`).
> - **Unified Modal Across App**: Both the Settings screen ("Contribute to Mulberry" row) and the Library Shelf (`OPEN_CONTRIBUTE` banner action) will render this single shared modal component so behavior and UI stay strictly synchronized.
> - **External PDF Reader Workflow**: Selecting a PDF via **Contribute resources** opens the chosen file directly inside `ReaderActivity` as an external PDF (`Intent.ACTION_VIEW` with read URI permission), triggering the built-in reader preview and "Add to Desk / Contribute to Community" workflow.

---

## 1. Overview & Core Concept

- **What It Does**: Replaces the legacy 3-section Contribute sheet with a streamlined, scrollable bottom sheet featuring a standalone support card (**Buy me a coffee**) and two grouped cards (**CONTRIBUTE** and **STAY CONNECTED**).
- **Target Audience**: Medical students using Mulberry who want to support development via UPI, contribute textbooks/notes/PYQs through the built-in PDF reader, report issues on GitHub, join the Telegram support channel, or share the app with classmates.
- **Key Value**: Makes contributing PDFs seamless by previewing them immediately in Mulberry's native PDF reader as an external PDF, while providing direct one-tap intents for UPI, GitHub, Telegram, and sharing.

---

## 2. User Experience & Visual Design

### Layout & Visual Hierarchy
```
├──────────────────────────────────────────────┤
│                    ━━━                       │
│                                              │
│  Help Mulberry grow                          │
│  Keep the library free, open, and growing.   │
│                                              │
│  ┌────────────────────────────────────────┐  │
│  │  [Coffee Icon]  Buy me a coffee     ›  │  │
│  │                 Fuel late-night fixes  │  │
│  │                 and new resources.     │  │
│  └────────────────────────────────────────┘  │
│                                              │
│  C O N T R I B U T E                        │
│  ┌────────────────────────────────────────┐  │
│  │  [Upload Icon]  Contribute resources › │  │
│  │                 Share books, notes &   │  │
│  │                 PYQs                   │  │
│  │  ────────────────────────────────────  │  │
│  │  [Code Icon]    GitHub repository   ›  │  │
│  │                 Report issues or help  │  │
│  │                 build Mulberry         │  │
│  └────────────────────────────────────────┘  │
│                                              │
│  S T A Y   C O N N E C T E D                │
│  ┌────────────────────────────────────────┐  │
│  │  [Send Icon]    Telegram channel    ›  │  │
│  │                 Stay Updated           │  │
│  │  ────────────────────────────────────  │  │
│  │  [Share Icon]   Share Mulberry      ›  │  │
│  │                 Share the latest       │  │
│  │                 release with classmates│  │
│  └────────────────────────────────────────┘  │
└──────────────────────────────────────────────┘
```

- **Header**:
  - Title: `Help Mulberry grow` (`PoppinsFamily`, `FontWeight.Bold`, `20.sp`, `colors.textPrimary`)
  - Subtitle: `Keep the library free, open, and growing.` (`InterFamily`, `13.sp`, `colors.textSecondary`)
- **Section Headers**:
  - `CONTRIBUTE` and `STAY CONNECTED` styled with wide letter spacing (`letterSpacing = 2.5.sp`, `InterFamily`, `FontWeight.SemiBold`, `11.5.sp`, `colors.primary`).
- **Cards & Dividers**:
  - `RoundedCornerShape(16.dp)` cards with `colors.surfaceCard` background and `1.dp` `colors.borderSubtle` border.
  - Multi-item cards (`CONTRIBUTE` and `STAY CONNECTED`) separate rows with a subtle `0.5.dp` horizontal divider (`colors.borderSubtle.copy(alpha = 0.6f)`).
  - Each row features a `40.dp` circular icon container (`colors.primary.copy(alpha = 0.12f)`), a Microsoft Fluent 24dp vector icon tinted with `colors.primary`, title + subtitle text column, and a trailing `FluentIcons.ChevronRight16Regular`.

---

## 3. Key Product Decisions & Action Routing

1. **Buy me a coffee (`7029279707@upi`)**:
   - Launches an Android `Intent(Intent.ACTION_VIEW, Uri.parse("upi://pay?pa=7029279707@upi&pn=Mulberry&cu=INR"))`.
   - If no UPI payment app is installed on the device, copies `7029279707@upi` to the clipboard and shows a toast (`"UPI ID (7029279707@upi) copied to clipboard"`).
2. **Contribute resources (External PDF in Built-in Reader)**:
   - Launches the system file manager PDF selector (`ActivityResultContracts.OpenDocument` for `arrayOf("application/pdf")`).
   - Upon selecting a PDF `Uri`, grants read permission and launches `ReaderActivity` with `Intent.ACTION_VIEW` and `data = uri`.
   - `ReaderActivity.handleExternalPdfUri(uri)` automatically loads the PDF as an external document with the top banner/menu to add to Desk and contribute to the Mulberry Community Library.
3. **GitHub repository**:
   - Opens `https://github.com/doubtsolved/Mulberry` via `Intent.ACTION_VIEW`.
4. **Telegram channel (`t.me/mulberrysupport`)**:
   - Attempts to open `tg://resolve?domain=mulberrysupport` (or `https://t.me/mulberrysupport` targeted at installed Telegram clients `org.telegram.messenger` / `org.thunderdog.challegram`), and falls back seamlessly to opening `https://t.me/mulberrysupport` in the browser.
5. **Share Mulberry**:
   - Launches the Android system share sheet (`Intent.ACTION_SEND`) with the latest release link (`https://github.com/doubtsolved/Mulberry/releases/latest`).

---

## 4. Technical Architecture & Data Strategy

```
┌─────────────────────────────────────────────────────────────┐
│                ContributeSheetContent (Shared)              │
│   Used by SettingsScreen & LibraryScreen (OPEN_CONTRIBUTE)  │
└───────────────┬─────────────────────────────┬───────────────┘
                │                             │
     ┌──────────▼──────────┐       ┌──────────▼──────────┐
     │  PDF Document Picker │       │   External Intents  │
     │  (application/pdf)   │       │  • UPI: 7029279707  │
     └──────────┬──────────┘       │  • GitHub Repo      │
                │                  │  • Telegram Support │
     ┌──────────▼──────────┐       │  • Share Release    │
     │   ReaderActivity    │       └─────────────────────┘
     │ (ACTION_VIEW + Uri) │
     │ Opens as External   │
     │ PDF with Contribute │
     └─────────────────────┘
```

- **Icon Addition**: Add `DrinkCoffee24Regular` to `FluentIcons` using clean vector strokes matching the existing 24dp Fluent icon set.
- **Shared Composable**: Expose `ContributeSheetContent(onDismiss: () -> Unit, onContributeResourcesClick: () -> Unit)` so both `SettingsScreen` and `LibraryScreen` invoke the exact same UI and PDF picker -> `ReaderActivity` external PDF launch pipeline.
