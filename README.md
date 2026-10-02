# Mulberry 📖🌿

[![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-purple.svg?style=flat&logo=kotlin)](https://kotlinlang.org)
[![Jetpack Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4.svg?style=flat&logo=android)](https://developer.android.com/jetpack/compose)
[![Min SDK](https://img.shields.io/badge/Min%20SDK-26-blue.svg)](https://developer.android.com/about/dashboards)
[![Target SDK](https://img.shields.io/badge/Target%20SDK-35-green.svg)](https://developer.android.com)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)
[![Platform](https://img.shields.io/badge/Platform-Android%20%7C%20Tablets%20%7C%20Foldables-orange.svg)](https://developer.android.com)

> **Ergonomic Study Organizer & Native PDF Reader**  
> Designed for medical scholars, academic researchers, and deep readers with distraction-free reading, vault-based library management, and integrated study planning.

---

## 🌟 Key Features

### 📖 Immersive PDF Reader Engine
- **Sleek Spring Controls Toggle**: Tapping the middle 60% of the screen smoothly animates the top bar up (`-100% Y`) and the bottom scrubber bar down (`+100% Y`) with spring easing (`Spring.DampingRatioLowBouncy`), while keeping the PDF viewport fixed with zero layout shifts or jitter.
- **High-Definition Page Sharing**: A dedicated share button at the very end of the floating scrubber bar (following the page counter badge) renders the current open page at crisp **2.5x supersampled high definition** and immediately opens the native Android Share Sheet.
- **Circle to Search Gesture**: Long-pressing anywhere on a document page emits tactile haptic feedback, displays an animated Google-colored circular lens ring, and activates **Google Circle to Search** (native Omnient Activity overlay with automatic fallback to system `ACTION_ASSIST` screenshot payload).
- **One-Handed Navigation Zones**:
  - **Left 20%**: Smoothly animates to the previous page.
  - **Center 60%**: Toggles reader bars and system bars with spring animation.
  - **Right 20%**: Smoothly animates to the next page.
- **Fluid Gesture Zoom & Pan**:
  - Double-tap animates zoom between 1.0x and 2.5x centered on touch coordinates.
  - Pinch-to-zoom (up to 5.0x) with conditional gesture routing (single-finger swipes pass through to page turns when zoomed out).
- **Fast Search & Navigation**:
  - Live streaming full-text search with match counter badges and jump-to-result navigation.
  - Interactive Table of Contents (TOC) with collapsible section hierarchies.
  - Visual 2-state Thumbnail Grid Sheet for quick page browsing.

---

### 📂 Vault-Based Library Management
- **Direct Disk Folder Binding**: Point Mulberry directly to your textbook directories without slow Storage Access Framework (SAF) IPC bottlenecks.
- **Intelligent Title Sanitization**: Strips underscores, edition metadata, and hashes from messy filenames into clean, readable book titles.
- **Automatic Cover Thumbnail Caching**: High-speed asynchronous cover generation with in-memory LRU caching.
- **Zero Cloud Lock-in**: All reading progress, bookmarks, and syllabus tasks sync to a portable `.mulberry/` folder directly on disk.

---

### 📅 Integrated Study & Exam Agenda
- **Daily Checklist**: Track textbook chapter readings, lecture viewings, and problem sets.
- **Exam Countdown**: Dynamic real-time countdown badges for upcoming exams and milestones.
- **Coursework Deep Linking**: Jump directly from an agenda task into the exact page of your textbook.

---

### 🎨 6 Curated Eye-Friendly Palettes

| Palette | Background | Primary Tone | Ergonomics |
|:---|:---|:---|:---|
| **Olded (Sepia)** | `#FBF0D9` (Warm Parchment) | `#C26D38` (Amber Rust) | Mimics vintage book paper, reduces blue-light fatigue |
| **Paper (Clean White)** | `#FFFFFF` (Crisp White) | `#2563EB` (Cobalt Blue) | High contrast, bright clinical aesthetic |
| **Midnight (Pitch OLED)** | `#000000` (Pure Black) | `#38BDF8` (Sky Cyan) | Maximum battery savings on AMOLED screens |
| **Forest (Sage Dark)** | `#111A15` (Deep Pine) | `#4ADE80` (Emerald Sage) | Calming dark palette for late-night reading |
| **Espresso (Mocha Dark)** | `#1A1412` (Dark Roast) | `#F59E0B` (Warm Ochre) | Earthy dark mode with warm amber accents |
| **Dusk (Lavender/Indigo)** | `#13111C` (Deep Twilight) | `#A78BFA` (Soft Purple) | Stylish indigo-slate palette with soft lavender |

---

## 🖐️ Reader Gestures & Controls Cheat Sheet

| Gesture / Control | Location | Action |
|:---|:---|:---|
| **Single Tap** | Left 20% of page | Previous Page turn |
| **Single Tap** | Center 60% of page | Toggle Top Bar & Scrubber Bar (Spring Animation) |
| **Single Tap** | Right 20% of page | Next Page turn |
| **Double Tap** | Anywhere on page | Toggle 1.0x $\leftrightarrow$ 2.5x Zoom centered on tap |
| **Pinch / Pan** | When zoomed in | Smooth 2-finger zoom (up to 5x) & fluid panning |
| **Long Press** | Anywhere on PDF | **Circle to Search** (Omnient / ACTION_ASSIST screenshot) |
| **Share Button** | Far right of Scrubber Bar | Export & share current page as high-def PNG image |
| **Scrubber Slider** | Bottom bar center | Instant scrub to any page in document |

---

## 🏗️ Technical Architecture

```
┌────────────────────────────────────────────────────────────────────────┐
│                              UI LAYER                                  │
│   LibraryScreen          AgendaScreen               SettingsScreen     │
│   (Vaults/Covers)        (Tasks/Deadlines)          (Themes/Storage)   │
│                                                                        │
│                      ReaderActivity (Full Screen)                      │
│   ┌────────────────────────────────────────────────────────────────┐   │
│   │ SinglePagePdfViewer                                            │   │
│   │   ├── HorizontalPager (Single-Page Viewport)                   │   │
│   │   ├── AnimatedVisibility (Spring-Eased Scrubber Bar)           │   │
│   │   │    └── [Prev] [Slider] [Next] [Badge] [Divider] [Share]    │   │
│   │   └── CircleToSearchOverlay (Lens Ring Animation)              │   │
│   ├────────────────────────────────────────────────────────────────┤   │
│   │ ReaderTopBarScreen (Overlaid Top Bar with Spring Animation)    │   │
│   └────────────────────────────────────────────────────────────────┘   │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │ StateFlows & UI Events
                                    ▼
┌────────────────────────────────────────────────────────────────────────┐
│                        VIEWMODEL & REPOSITORY                          │
│   MulberryViewModel                                                    │
│   ├── PdfRenderer & Mutex (Thread-Safe Page Rasterization)             │
│   ├── CircleToSearchHelper (Omnient / ACTION_ASSIST Multi-Tier)        │
│   ├── PdfThumbnailManager & PdfTextSearchManager                       │
│   └── MulberryRepository                                               │
└───────────────────────────────────┬────────────────────────────────────┘
                                    │
                  ┌─────────────────┴─────────────────┐
                  ▼                                   ▼
        Room Local Database                 Storage Vault (.mulberry/)
        (Instant query cache)               (Atomic, portable JSON state)
```

### Tech Stack
- **Language**: [Kotlin](https://kotlinlang.org/) (Coroutines, Flow, Mutex)
- **UI Toolkit**: [Jetpack Compose](https://developer.android.com/jetpack/compose) with Material Design 3 (M3)
- **PDF Rendering**: Native Android `PdfRenderer` + `PdfiumCore`
- **Local Persistence**: [Room Database](https://developer.android.com/training/data-storage/room) (SQLite) + Portable JSON sync
- **Image Handling**: Custom bitmap supersampling pipeline & in-memory LRU cache
- **Edge-to-Edge**: `WindowCompat` with full gesture navigation and transient system bar immersion

---

## 🚀 Getting Started

### Prerequisites
- Android Studio Ladybug / Meerkat (or newer)
- JDK 17 or higher
- Android SDK 35 (compile SDK)
- A physical Android device or emulator running Android 8.0 (API 26) or higher

### Building from Source

```bash
# Clone the repository
git clone https://github.com/your-username/mulberry.git

# Navigate to project directory
cd mulberry

# Build the debug APK
gradle :app:assembleDebug
```

The compiled APK will be located at:
```
app/build/outputs/apk/debug/app-debug.apk
```

---

## 🔒 Permissions & Privacy

Mulberry is **100% offline and local**:
- `MANAGE_EXTERNAL_STORAGE` / `READ_EXTERNAL_STORAGE`: Required to access, index, and render PDF documents from real user storage folders.
- `INTERNET`: Optional for feedback email or in-app external resource linking.
- **Zero analytics, zero tracking, zero cloud dependencies.** Your books and notes never leave your device.

---

## 📄 License

```text
Copyright 2026 Mulberry Project Contributors

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
