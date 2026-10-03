# Implementation Plan — Obsidian-Style In-Place Live Markdown Editor, Dock Alignment & Active Vault Linkage

Refine the **Desk** tab experience to deliver a true Obsidian-like live editing environment, a polished formatting accessory dock, distraction-free full-screen note editing, and strict active-vault directory binding.

---

## 🎯 Key Improvements

### 1. Distraction-Free Workspace & Hidden Bottom Navigation
- When a note is opened in `NoteEditorScreen`, **hide the bottom navigation bar completely** (`isNoteEditorOpen` state in `MainActivity` / `DeskNavigationHost`).
- When exiting back to `FolderDetailScreen` or `DeskRootScreen`, the bottom navigation bar smoothly reappears.

### 2. Dock Alignment & Cursor Auto-Centering
- **Dock Redesign**: Rebuild the accessory bar as a unified, beautifully styled floating/docked toolbar matching the active Mulberry palette (no mismatched backgrounds or clipped icon boxes).
- **Cursor Placement**:
  - **Bold (`**`): If text is selected $\rightarrow$ wrap `**selected**`. If no text is selected $\rightarrow$ insert `****` and place cursor right in the middle: `**|**`.
  - **Italic (`*`): If no text is selected $\rightarrow$ insert `**` and place cursor in the middle: `*|*`.
  - **Code/Inline (` ` `): Insert ```` `` ```` with cursor inside.
  - **Book Citation / Snip / Task / Quote**: Insert at cursor or line start and focus correctly.

### 3. Obsidian-Style Live In-Place Markdown Rendering
- **In-Place Styling (`MarkdownVisualTransformation` / Real-Time Annotated Transformation)**:
  - While typing in the editor, lines starting with `# ` dynamically render as **H1** (larger font size, bold, heading line-height) directly in place.
  - `## ` renders dynamically as **H2** (19sp bold).
  - `### ` renders dynamically as **H3** (16sp bold).
  - `**bold**` renders with **bold** font weight in place.
  - `*italic*` renders with *italic* font style in place.
  - `- [ ]` and `- [x]` render as distinct checklist items in place.
  - `[[Book#p.123]]` renders with citation highlight styling in place.
  - The underlying file remains clean UTF-8 Markdown text and saves simultaneously to disk on every keystroke with debounced background coroutines.

### 4. Active Vault Dynamic Binding
- Instead of hardcoding a separate `MedicalVault`, `DeskRepository` binds directly to `viewModel.activeVault` (`activeVault.rootPath`).
- **Conditional Starter Seeding**:
  - When an active vault is opened, check its `.mulberry/notes/` directory.
  - **Only if** that vault has no notes/folders, seed the initial sample starter notes into that specific vault's `.mulberry/notes/`.
  - If that vault already contains notes, display the user's existing files without overwriting.
  - Allow switching vaults directly from the top chip dropdown.

---

## 🛠️ Step-by-Step Implementation

### Step 1: Update `DeskRepository.kt`
- Accept dynamic active vault path from `viewModel.activeVault`.
- Seed starter notes strictly inside `${activeVault.rootPath}/.mulberry/notes/` when empty.
- Provide vault switching support.

### Step 2: In-Place Markdown Live Renderer (`MarkdownVisualTransformation.kt`)
- Implement a custom `VisualTransformation` for `BasicTextField` that applies live `SpanStyle`s:
  - `# ` headings: scaled font sizes (24sp for H1, 19sp for H2, 16sp for H3) with dimmed `#` prefix.
  - `**text**`: `FontWeight.Bold` with dimmed `**` delimiters.
  - `*text*`: `FontStyle.Italic` with dimmed `*` delimiters.
  - `[[Book#p.123]]`: Primary tinted citation pill style.
  - `- [ ]`: Accent colored task checkbox marker.
- Maintains 1:1 character index mapping so cursor movement and selection work flawlessly.

### Step 3: Redesign Accessory Tool Dock in `NoteEditorScreen.kt`
- Unify tool buttons with consistent 38dp height, rounded 10dp squircle shapes, and cohesive theme tinting.
- Implement cursor auto-centering:
  - Bold: `text.substring(0, start) + "**" + selected + "**" + text.substring(end)` $\rightarrow$ cursor at `start + 2`.
  - Italic: `start + 1`.
  - Strikethrough / Code: auto-centered cursor.
- Dock rests above the IME keyboard with `imePadding()` and at the screen bottom when keyboard is dismissed.

### Step 4: Hide Bottom Navigation Bar in `MainActivity.kt` & `DeskNavigationHost.kt`
- Lift `isNoteEditorOpen` state to hide `MulberryBottomNavBar` when inside `NoteEditorScreen`.

---

## 🧪 Verification Plan
1. **Tool Dock Alignment**: Verify toolbar appears directly above the virtual keyboard without clipping or awkward gaps.
2. **Bottom Nav Bar**: Verify tabs disappear when opening a note and reappear upon exiting.
3. **Cursor Auto-Centering**: Tap Bold `[B]` with no selection $\rightarrow$ verify text becomes `**|**` with cursor placed inside.
4. **Obsidian In-Place Rendering**: Type `# My Title` $\rightarrow$ verify it immediately scales to large bold H1 in the active editing canvas.
5. **Active Vault Seeding**: Verify notes are stored in `${activeVault.rootPath}/.mulberry/notes/` and samples only seed if that vault is empty.
