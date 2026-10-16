
# 📚 Mulberry

> A resilient, zero-cost digital textbook desk and high-yield revision engine built for medical students across all 19 MBBS subjects.

[![Platform](https://img.shields.io/badge/Platform-Android-3DDC84?style=flat&logo=android&logoColor=white)](https://github.com/doubtsolved/mulberry-catalog)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.0-7F52FF?style=flat&logo=kotlin&logoColor=white)](https://kotlinlang.org)
[![Compose](https://img.shields.io/badge/Jetpack-Compose-4285F4?style=flat&logo=jetpackcompose&logoColor=white)](https://developer.android.com/jetpack/compose)
[![Security](https://img.shields.io/badge/Security-AES--256--GCM-008080?style=flat)](https://github.com/doubtsolved/mulberry-catalog)
[![Telegram](https://img.shields.io/badge/Telegram-Channel-2CA5E0?style=flat&logo=telegram&logoColor=white)](https://t.me/YOUR_TELEGRAM_HANDLE)
[![WhatsApp](https://img.shields.io/badge/WhatsApp-Channel-25D366?style=flat&logo=whatsapp&logoColor=white)](https://whatsapp.com/channel/YOUR_WHATSAPP_ID)

---

## 🩺 The Problem Mulberry Solves

Medical school textbooks are massive, expensive, and impractical to lug around clinical postings. Pirated PDF drives break every few weeks, download links expire, and phone PDF readers choke on 1,400-page histology and pathology manuals.

**Mulberry** replaces messy Google Drive folders and spam-heavy Telegram chats with a dedicated, native study desk. It operates on a **zero-backend, static edge CDN topology**—distributing encrypted medical textbooks, handwritten topper notes, and practical manuals directly to your phone with instant search and zero subscription fees.

---

## ✨ Core Features

* **🏛️ All 19 MBBS Disciplines Cataloged:** Instant filtering across academic phases:
  * **1st Prof:** Anatomy, Physiology, Biochemistry
  * **2nd Prof:** Pathology, Pharmacology, Microbiology, Forensic Medicine
  * **3rd Prof (Part 1):** Community Medicine (PSM), Ophthalmology, ENT
  * **Final Prof:** General Medicine, Surgery, OBG, Pediatrics, Orthopedics, Dermatology, Psychiatry, Radiology, Anesthesiology
* **🔒 Ephemeral Decryption Engine:** Downloaded books are stored permanently as encrypted `.bin` payloads (`AES-256-GCM`). Plaintext PDFs only exist in an ephemeral session cache while actively open in the reader and are zero-wiped upon exit.
* **📖 Medical-Grade PDF Reader:** True `#FFFFFF` white-canvas rendering preserves the exact color fidelity of histology stains, hematology smears, and anatomical plates. Includes full Table of Contents (TOC), page scrubbing, and bookmarks.
* **✂️ Diagram Snip Tool:** Drag a bounding box over any brachial plexus layout or biochemical cycle inside the viewer to instantly save high-yield diagram cards into your personal **Snips Gallery**.
* **🗄️ Study Desk & Daily Goals:** Set daily chapter and page targets with progress rings to track your exam milestones without leaving the app.
* **🎨 6 Clinical Reading Atmospheres:** Powered by Mulberry's adaptive 6-palette engine:
  * `Olded Sepia` (Warm antique paper)
  * `Paper White` (Clinical high contrast)
  * `Midnight OLED` (Pitch black battery saver)
  * `Forest Sage` (Organic deep pine)
  * `Espresso Mocha` (Dark roast warmth)
  * `Dusk Lavender` (Subtle twilight indigo)

---

## 📡 Architecture: Zero-Cost, Serverless Edge Pipeline

Mulberry operates with zero recurring server costs by leveraging GitHub Releases as binary object stores and jsDelivr as an edge CDN:

```text
[ Contributor Google Form ]
              │
              ▼
    [ Google Sheet Intake ] ──(1-Click Approval Checkbox)──┐
                                                           │
                                                           ▼
                                            [ GitHub Actions Engine ]
                                            • Downloads & extracts WebP cover
                                            • Encrypts PDF (AES-256-GCM)
                                            • Commits to catalog.json
                                                           │
                                                           ▼
   ┌───────────────────────────────────────────────────────┴────────────────────────┐
   │                                                                                │
   ▼                                                                                ▼
[ jsDelivr Edge CDN ]                                                 [ 4 Vault Repositories ]
• Global Cloudflare/Fastly Caching                                    • mulberry-vault-prof1
• Real-time catalog.json & covers                                     • mulberry-vault-prof2
   │                                                                  • mulberry-vault-prof3
   │                                                                  • mulberry-vault-prof4
   └───────────────────────────────────┬────────────────────────────────────────────┘
                                       │
                                       ▼
                         [ Mulberry Android Client ]
                         • SQLite / Room local cache
                         • Background OkHttp stream (.bin)
                         • In-Memory / Ephemeral Decryption
                         • Native Hardware-Accelerated Canvas

```

---

## 📥 Community Channels & Downloads

Stay notified about new textbook drops, university question banks (PYQs), and app releases:

* **Telegram Channel:** [Join Mulberry on Telegram](https://www.google.com/url?sa=E&source=gmail&q=https://t.me/mulberrysupport) *(Direct book drops, release alerts & backup links)*
* **Contributor Form:** [Submit Textbooks & Notes](https://forms.gle/FGAupTdNXCWkGu7g9) *(Upload files via Google Drive to expand the vault)*

---

## 🤝 How to Contribute Resources

1. Make sure your PDF is clean, complete, and legible.
2. Upload the file to your Google Drive and set sharing to **"Anyone with the link can view"**.
3. Fill out the [Mulberry Vault Submission Form](https://forms.gle/FGAupTdNXCWkGu7g9).
4. Once reviewed, our automated GitHub Actions pipeline handles cover rendering, AES encryption, CDN indexing, and channel notifications automatically.

---

## 👨‍💻 Maintainer's Disclaimer

> *"I am an MBBS medical student, not a Silicon Valley software engineer. This app was built between dissection hall sessions, hospital postings, and late-night viva revisions because carrying 15 kg of standard textbooks makes no sense in 2026.*
> *If something works smoothly: it's pure architectural stubbornness.*
> *If something breaks: restart the app and file a bug report. Happy studying!"*
> — **Sahabul Mondal** ([@doubtsolved](https://github.com/doubtsolved))

---

## ⚖️ Academic Fair-Use Disclaimer

Mulberry is a non-commercial, student-built revision platform designed strictly for educational exchange and personal study among medical undergraduates. All cataloged materials are indexed from community contributions.

```
