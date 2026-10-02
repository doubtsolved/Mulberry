# Mulberry v1.0.0 — Official Launch Release 🚀📖

> **Release Tag:** `v1.0.0`  
> **Release Name:** *Mulberry v1.0 — The Ergonomic Study Organizer & Native PDF Reader*  
> **Target SDK:** Android 15 (API 35/36)  
> **Minimum Supported:** Android 7.0 (API 24+)  
> **Package Name:** `com.aistudio.mulberry.study`  
> **Release Date:** October 2026  

---

## 🌟 What is Mulberry?

**Mulberry** is an offline-first, distraction-free study companion and academic library manager built for medical students, researchers, university scholars, and deep readers. It eliminates friction when organizing massive textbook repositories and reading heavy academic PDFs by combining **vault-based direct-storage indexing**, an **ultra-smooth single-page reader engine**, and an **integrated study & exam agenda**.

---

## 🚀 Key Highlights in this Launch Release

### 1. 📖 Immersive Single-Page PDF Engine
- **Sleek Spring Bar Animations**:
  - Tapping the center 60% of the screen seamlessly slides the top bar up (`-100% Y`) and the bottom scrubber bar down (`+100% Y`) with spring easing (`Spring.DampingRatioLowBouncy`), while keeping the PDF viewport fixed with zero canvas jumping.
  - Full immersion mode coordinates with system status and navigation bars.
- **High-Definition Page Sharing**:
  - Located at the far right of the scrubber bar (after the page number badge).
  - Renders the open page at **2.5x supersampled crystal-clear print resolution** and launches the native Android Share Sheet (`Intent.ACTION_SEND` with `image/png`).
- **Google Circle to Search Long-Press Gesture**:
  - Press and hold anywhere on a document page to activate **Google Circle to Search**.
  - Features tactile haptic confirmation, an animated glowing Google-colored lens ring, and a 4-tier fallback cascade (native Omnient activity $\rightarrow$ system `ACTION_ASSIST` with screenshot payload $\rightarrow$ Google Lens $\rightarrow$ visual search chooser).
- **One-Handed Navigation Zones**:
  - **Left 20%**: Smoothly animates to the previous page.
  - **Center 60%**: Toggles reader bars and system bars with spring animation.
  - **Right 20%**: Smoothly animates to the next page.
- **Fluid Zoom & Transform Routing**:
  - Double-tap zoom between 1.0x and 2.5x centered on touch coordinates.
  - Multi-touch pinch-to-zoom up to 5.0x with intelligent gesture pass-through.
- **Live In-Document Search & Fast TOC**:
  - Progressive streaming text search with match badges and jump buttons.
  - Expandable Table of Contents hierarchy and 2-state visual Thumbnail Grid Sheet.

---

### 2. 📂 Vault-Based Library Management
- **Direct Disk Folder Binding**: Point Mulberry directly to storage folders without slow Storage Access Framework (SAF) bottlenecks.
- **Intelligent Title Sanitization**: Automatically strips hashes, editions, and file symbols into elegant human-readable book titles.
- **Automatic Cover Thumbnail Caching**: High-speed asynchronous cover generation with in-memory LRU caching.
- **Zero Cloud Lock-in**: All reading progress, bookmarks, and syllabus tasks sync to a portable `.mulberry/` folder directly on disk.

---

### 3. 📅 Integrated Study & Exam Agenda
- **Coursework Checklists**: Daily task management for readings, problem sets, and lectures.
- **Dynamic Exam Countdowns**: Real-time countdown badges keep you on track for major exams.
- **Chapter Deep Linking**: Jump from an agenda task directly to the corresponding textbook page.

---

### 4. 🎨 6 Curated Eye-Friendly Color Palettes
1. **Olded (Sepia)** — Mimics antique book parchment, reducing blue-light eye strain during marathon study sessions.
2. **Paper (Clean White)** — Crisp, modern clinical aesthetic with royal cobalt accents.
3. **Midnight (Pitch OLED)** — True pure-black `#000000` theme for maximum battery savings on AMOLED screens.
4. **Forest (Sage Dark)** — Deep pine and calming emerald sage for late-night review.
5. **Espresso (Mocha Dark)** — Earthy dark roast with warm amber highlights.
6. **Dusk (Lavender/Indigo)** — Twilight indigo cards paired with soft purple typography.

---

## 📋 Changelog (v1.0.0)

### Added
- **Core Architecture**:
  - Kotlin 2.0 + Jetpack Compose with Material Design 3.
  - Offline Room SQLite database with atomic `.mulberry/` portable JSON synchronization.
- **Reader Enhancements**:
  - Spring-eased slide and fade animations for top bar and scrubber bar.
  - Dedicated High-Definition Page Share button on the scrubber bar.
  - Long-press Google Circle to Search gesture with native Omnient Activity overlay and universal system `ACTION_ASSIST` screenshot fallback.
  - One-handed tap zones (20% prev, 60% toggle, 20% next) and animated double-tap zoom.
  - Dual-mode thumbnail grid sheet and live streaming in-document search.
- **Library & Agenda**:
  - Vault-based folder scanning with automatic cover rasterization and LRU caching.
  - Syllabus checklists, study tasks, and countdown timers.
- **Theming & Design**:
  - 6 ergonomic palettes with custom typography (Poppins + Inter).
  - Edge-to-edge support with transient system bar immersion.

---

## 📱 System Requirements

| Specification | Requirement |
|:---|:---|
| **OS Version** | Android 7.0 (API 24) or higher |
| **Recommended OS** | Android 11 to Android 15 |
| **Form Factors** | Smartphones, Foldables, Tablets, and E-Ink Android readers |
| **Permissions** | Storage Access (`MANAGE_EXTERNAL_STORAGE` / `READ_EXTERNAL_STORAGE`) |
| **Internet** | Not required (100% offline functionality) |

---

## 📥 Installation & Verification

### Download Pre-built APK
Download `app-release.apk` or `app-debug.apk` from the [GitHub Releases](https://github.com/your-username/mulberry/releases) section.

### Verification (SHA-256 Checksum)
```bash
# Verify downloaded APK checksum
sha256sum app-release.apk
```

### Install via ADB
```bash
adb install -r app-release.apk
```

---

## 📣 Google Play / Store "What's New" Copy

```text
Welcome to Mulberry v1.0!
• Sleek Reader Controls: Tap the center to smoothly slide & fade toolbars without page jumps.
• Instant HD Page Sharing: Share high-definition page captures directly from the scrubber bar.
• Circle to Search: Long-press anywhere on a document page to search visuals and text instantly.
• One-Handed Reading: Tap left/right edges for page turns, double-tap to zoom.
• 6 Eye-Friendly Palettes: Study comfortably with Olded Sepia, Midnight OLED, Forest, and more.
• 100% Offline & Private: Direct folder vaults with zero cloud lock-in.
```

---

## 🙏 Credits & Acknowledgments
Thank you to all scholars, researchers, and testers who provided feedback during development. For bug reports or feature suggestions, visit our [GitHub Issues](https://github.com/your-username/mulberry/issues).
