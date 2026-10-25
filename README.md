# 🍇 Mulberry — The Medical Library & Study Companion

[![Platform](https://img.shields.io/badge/Platform-Android-3DDC84?style=flat-square&logo=android&logoColor=white)](#)
[![Stack](https://img.shields.io/badge/Stack-Kotlin%20%7C%20FastAPI%20%7C%20Cloudflare-purple?style=flat-square)](#)
[![Vault](https://img.shields.io/badge/Vault-AES--256--GCM%20%7C%20HF%20LFS-yellow?style=flat-square)](#)
[![Status](https://img.shields.io/badge/Version-v4.0--Stable-pink?style=flat-square)](#)

> *"Coded between dissection hall sessions and late-night viva prep."*  
> Mulberry is an open, distraction-free medical textbook library, study desk, and offline vault designed specifically for MBBS students across 1st to Final Prof.

---

## 🩺 The Philosophy

Medical school backpacks are heavy, physical textbooks cost a fortune, and standard PDF readers aren't built for 1,200-page clinical treatises. Mulberry is built from the ground up by a medical student to make studying calmer, lighter, and completely accessible:

- **Zero Clutter:** No intrusive ads, popups, or unnecessary bloat.
- **Offline Vault:** Download once, read forever with instant page rendering.
- **Built for Clinical Learning:** Dual-page reading for histology diagrams, Eye Comfort filters for 2 AM speedruns, and active revision tools.

---

## ✨ Features at a Glance

### 📚 Mulberry Shelf & Curated Catalog
- **Academic Phases:** Filter seamlessly across **1st Prof**, **2nd Prof**, **3rd Prof Part 1**, and **Final Prof**.
- **Specialty Categories:** Instant filtering across Standard Textbooks, Question Banks (PYQs), Handwritten Revision Notes, Practical Manuals, and High-Yield Atlases.
- **16:6 Interactive Carousel:** Catch real-time book drops, high-yield exam alerts, and subject spotlights directly from the top of your shelf.

### 📖 Tailored PDF Reader
- **Dual-Page Spread:** Side-by-side reading on tablets, foldables, and wide viewports.
- **Eye Relax Mode:** Warmth adjustment, dimmer controls, and Sepia/OLED Dark filters.
- **High-Yield Snip & Circle-to-Search:** Crop histology diagrams and clinical flowcharts instantly for quick revision.
- **Instant Full-Text Search:** Fast text scrubbing across heavy 800+ page medical books.

### 🗓️ Study Desk & Session Tracker
- **Session Goals:** Set page targets, track daily reading streaks, and manage study cards.
- **Snooze & Reschedule:** Real-world schedule recovery for when hospital postings run late.

### 📤 Crowdsourced Community Pipeline
- **Direct App Uploads:** Students can submit missing textbooks, notes, and university PYQs directly to the Mulberry Vault.
- **Quarantine Security:** Submissions are queued in an edge quarantine pipeline for review before publishing to the main library catalog.

### ✈️ Upcoming: Mulberry Air (v5.0)
- Read your phone's entire library on your laptop browser over local Wi-Fi/Hotspot without internet access or cloud fees.

---

## 🏛️ System Architecture

Mulberry splits storage, edge routing, and catalog curation into a modular architecture:

```
┌────────────────────────────────────────────────────────┐
│                      Android App                       │
│    Kotlin • Jetpack • Room DB • AES-256-GCM Engine     │
└───────────────▲────────────────────────▲───────────────┘
                │                        │
       Catalog & Banners        Binary Payload Stream
      (announcements.json)       (Encrypted .bin via S3)
                │                        │
┌───────────────┴──────────────┐ ┌───────┴───────────────┐
│     GitHub Catalog Repo      │ │   Cloudflare Worker   │
│  (Catalog, Metadata, Covers) │ │   (mulberry-vault)    │
└──────────────────────────────┘ └───────▲───────────────┘
                                         │
                                 Batch LFS Pointer
                                         │
                                 ┌───────┴───────────────┐
                                 │ Hugging Face Datasets │
                                 │   (Storage Vault)     │
                                 └───────────────────────┘
```

1. **Android Client (`Mulberry`):** Native Android app handling SQLite indexing, offline caching, AES-GCM streaming decryption, and reader rendering.
2. **Edge Proxy (`Cloudflare Worker`):** Manages access quotas, VIP contributor routing, and bridges Git LFS batch upload requests to the vault.
3. **Storage Vault (`Hugging Face Datasets`):** Stores encrypted `.bin` payloads via Git LFS pointers to eliminate costly file hosting bills.
4. **Operations Portal (`portal.py`):** An administrative CMS built with FastAPI for batch URL/Drive ingestion, Hugging Face quarantine inspection, and one-click GitHub catalog syncing.

---

## 🛠️ Repository & CMS Setup

The administrative dashboard lives in `portal.py` and manages ingestion, reviews, and catalog publishing:

### Prerequisites
- Python 3.10+
- Hugging Face Access Token (`HF_TOKEN` with write permission)
- GitHub Personal Access Token (`GH_TOKEN`)

### Quickstart
```bash
# Clone the repository
git clone https://github.com/doubtsolved/mulberry-catalog.git
cd mulberry-catalog

# Install dependencies
pip install fastapi uvicorn requests pymupdf gdown beautifulsoup4 pillow python-dotenv cryptography huggingface_hub

# Configure environment
cat <<EOF > .env
VAULT_ENCRYPTION_KEY=your_aes_hex_key
HF_TOKEN=your_huggingface_token
HF_REPO_ID=sahabul1064/mulberry-archives
GH_TOKEN=your_github_token
CATALOG_REPO=doubtsolved/mulberry-catalog
CF_WORKER_URL=https://mulberry-vault.sahabulmondal2023.workers.dev
EOF

# Launch the Admin Portal
python portal.py
```
Open **`http://localhost:8000`** to access the Ingestion Engine, Community Submissions Quarantine, Catalog CMS, and 16:6 Banner Manager.

---

## 📁 Repository Structure

```
.
├── assets/
│   ├── covers/            # Production WebP book cover thumbnails
│   └── banners/           # 16:6 carousel announcement banners
├── templates/
│   └── index.html         # Administrative portal UI (Poppins & Inter)
├── announcements.json     # Remote configuration for 16:6 shelf carousel
├── catalog.json           # Master library catalog, categories, and mirror maps
├── portal.py              # FastAPI administration CMS and batch ingestion pipeline
└── README.md
```

---

## 🤝 Contributing

Contributions to Mulberry are welcome:
1. **Academic Content:** Submit high-yield notes or missing syllabus books directly through the in-app contribution sheet or via the admin portal.
2. **Code & Issues:** Fork the repo, report bugs, or submit pull requests for features.

---

## 📜 Disclaimer & Fair Use

Mulberry is an open-source educational project developed for academic reference, revision, and study purposes among medical students. All materials are community-curated and meant for personal study.

---

## 👨‍💻 Developer Note

Built with chai, late-night grit, and terminal sessions by **Sahabul Mondal** — Medical Student & Accidental App Builder.  
Special thanks to Google AI Studio, the Android developer community, and every student sharing resources instead of gatekeeping before exams! 🩺☕