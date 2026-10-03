package com.example.ui.screens.desk

import android.graphics.BitmapFactory
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.data.desk.DeskMarkdownPatterns
import com.example.data.desk.DeskRepository
import com.example.ui.components.FluentIcons
import com.example.ui.theme.LocalMulberryColors
import com.example.viewmodel.MulberryViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NoteEditorScreen(
    file: File,
    folderName: String,
    repository: DeskRepository,
    viewModel: MulberryViewModel,
    onBack: () -> Unit,
    onOpenBookAtPage: (bookName: String, page: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalMulberryColors.current
    val coroutineScope = rememberCoroutineScope()
    val books by viewModel.allBooks.collectAsState()

    var textFieldValue by remember { mutableStateOf(TextFieldValue(text = "")) }
    var isPreviewMode by remember { mutableStateOf(false) }
    var noteTitle by remember { mutableStateOf(file.nameWithoutExtension) }

    // Modals
    var showBookCitationModal by remember { mutableStateOf(false) }
    var citationPageInput by remember { mutableStateOf("") }
    var showSnipPickerModal by remember { mutableStateOf(false) }
    var snipFiles by remember { mutableStateOf<List<File>>(emptyList()) }
    var selectedSnipForLightbox by remember { mutableStateOf<File?>(null) }
    var showBacklinksDialog by remember { mutableStateOf(false) }

    var autoSaveJob by remember { mutableStateOf<Job?>(null) }

    // Load initial content
    LaunchedEffect(file) {
        val content = repository.getNoteContent(file)
        textFieldValue = TextFieldValue(text = content)
        snipFiles = repository.getSnips()
    }

    // Auto-save logic (Debounced simultaneous disk save)
    val saveContent: (String) -> Unit = { textToSave ->
        coroutineScope.launch {
            repository.saveNote(file, textToSave)
        }
    }

    fun onContentChange(newVal: TextFieldValue) {
        textFieldValue = newVal
        autoSaveJob?.cancel()
        autoSaveJob = coroutineScope.launch {
            delay(400) // 400ms debounce simultaneous write to disk
            repository.saveNote(file, newVal.text)
        }
    }

    val handleExit: () -> Unit = {
        saveContent(textFieldValue.text)
        onBack()
    }

    BackHandler {
        handleExit()
    }

    DisposableEffect(Unit) {
        onDispose {
            saveContent(textFieldValue.text)
        }
    }

    // Formatting Actions with Precise Cursor Centering
    fun applyBold() {
        val text = textFieldValue.text
        val selection = textFieldValue.selection
        val start = selection.min
        val end = selection.max

        if (start < end) {
            val selectedText = text.substring(start, end)
            val newContent = text.substring(0, start) + "**" + selectedText + "**" + text.substring(end)
            val newCursor = end + 4
            onContentChange(TextFieldValue(text = newContent, selection = TextRange(newCursor)))
        } else {
            // No selection: insert **** and position cursor right in the middle (**|**)
            val newContent = text.substring(0, start) + "****" + text.substring(start)
            val newCursor = start + 2
            onContentChange(TextFieldValue(text = newContent, selection = TextRange(newCursor)))
        }
    }

    fun applyItalic() {
        val text = textFieldValue.text
        val selection = textFieldValue.selection
        val start = selection.min
        val end = selection.max

        if (start < end) {
            val selectedText = text.substring(start, end)
            val newContent = text.substring(0, start) + "*" + selectedText + "*" + text.substring(end)
            val newCursor = end + 2
            onContentChange(TextFieldValue(text = newContent, selection = TextRange(newCursor)))
        } else {
            // No selection: insert ** and position cursor in the middle (*|*)
            val newContent = text.substring(0, start) + "**" + text.substring(start)
            val newCursor = start + 1
            onContentChange(TextFieldValue(text = newContent, selection = TextRange(newCursor)))
        }
    }

    fun prefixCurrentLine(prefix: String) {
        val text = textFieldValue.text
        val selection = textFieldValue.selection
        val cursor = selection.start

        val lastNewline = text.lastIndexOf('\n', (cursor - 1).coerceAtLeast(0))
        val lineStart = if (lastNewline == -1) 0 else lastNewline + 1

        val newContent = text.substring(0, lineStart) + prefix + text.substring(lineStart)
        val newCursor = cursor + prefix.length
        onContentChange(TextFieldValue(text = newContent, selection = TextRange(newCursor)))
    }

    fun insertAtCursor(prefix: String, suffix: String = "") {
        val text = textFieldValue.text
        val selection = textFieldValue.selection
        val start = selection.min
        val end = selection.max

        val selectedText = if (start < end) text.substring(start, end) else ""
        val newContent = text.substring(0, start) + prefix + selectedText + suffix + text.substring(end)
        val newCursor = start + prefix.length + selectedText.length + suffix.length
        onContentChange(TextFieldValue(text = newContent, selection = TextRange(newCursor)))
    }

    val bookCitationsCount = remember(textFieldValue.text) {
        DeskMarkdownPatterns.BOOK_WIKILINK_PATTERN.findAll(textFieldValue.text).count()
    }

    val visualTransformation = remember(colors) {
        MarkdownVisualTransformation(colors)
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        // TOP APP BAR (Icon-Only Actions)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = handleExit, modifier = Modifier.size(44.dp)) {
                Icon(
                    imageVector = FluentIcons.ArrowLeft24Regular,
                    contentDescription = "Back",
                    tint = colors.textPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(horizontal = 6.dp)
            ) {
                Text(
                    text = folderName,
                    fontSize = 11.sp,
                    color = colors.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = noteTitle,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Mode Switcher Action [👁️] / [✏️]
            IconButton(
                onClick = {
                    saveContent(textFieldValue.text)
                    isPreviewMode = !isPreviewMode
                },
                modifier = Modifier.size(44.dp)
            ) {
                Icon(
                    imageVector = if (isPreviewMode) FluentIcons.Edit24Regular else FluentIcons.BookOpen24Regular,
                    contentDescription = if (isPreviewMode) "Edit Mode" else "Clean Reading View",
                    tint = colors.primary,
                    modifier = Modifier.size(20.dp)
                )
            }

            // Backlinks Action [🔗]
            IconButton(
                onClick = { showBacklinksDialog = true },
                modifier = Modifier.size(44.dp)
            ) {
                Icon(
                    imageVector = FluentIcons.Link24Regular,
                    contentDescription = "Citations and Links",
                    tint = if (bookCitationsCount > 0) colors.primary else colors.textSecondary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // MAIN CANVAS (Obsidian-Style Live In-Place Rendering OR Full Rendered View)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 16.dp)
        ) {
            if (isPreviewMode) {
                // CLEAN RENDERED VIEW
                RenderedMarkdownCanvas(
                    markdown = textFieldValue.text,
                    repository = repository,
                    onToggleCheckbox = { lineIndex, currentLine ->
                        val lines = textFieldValue.text.lines().toMutableList()
                        if (lineIndex in lines.indices) {
                            val original = lines[lineIndex]
                            val updated = if (original.contains("- [x]", ignoreCase = true)) {
                                original.replaceFirst(Regex("""-\s*\[[xX]\]"""), "- [ ]")
                            } else {
                                original.replaceFirst(Regex("""-\s*\[\s*\]"""), "- [x]")
                            }
                            lines[lineIndex] = updated
                            val newText = lines.joinToString("\n")
                            onContentChange(TextFieldValue(text = newText))
                        }
                    },
                    onOpenBookAtPage = onOpenBookAtPage,
                    onOpenSnip = { selectedSnipForLightbox = it }
                )
            } else {
                // OBSIDIAN-STYLE LIVE IN-PLACE EDIT CANVAS
                val scrollState = rememberScrollState()
                BasicTextField(
                    value = textFieldValue,
                    onValueChange = { onContentChange(it) },
                    visualTransformation = visualTransformation,
                    cursorBrush = SolidColor(colors.primary),
                    textStyle = TextStyle(
                        fontSize = 15.sp,
                        lineHeight = 22.sp,
                        color = colors.textPrimary
                    ),
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(bottom = 68.dp)
                )
            }
        }

        // DOCKED FORMATTING TOOLBAR (Sits cleanly above virtual keyboard)
        if (!isPreviewMode) {
            UnifiedAccessoryDock(
                onH1 = { prefixCurrentLine("# ") },
                onH2 = { prefixCurrentLine("## ") },
                onBold = { applyBold() },
                onItalic = { applyItalic() },
                onBookCitation = { showBookCitationModal = true },
                onSnip = {
                    coroutineScope.launch {
                        snipFiles = repository.getSnips()
                        showSnipPickerModal = true
                    }
                },
                onTask = { prefixCurrentLine("- [ ] ") },
                onQuote = { prefixCurrentLine("> ") }
            )
        }
    }

    // Modal: Book Citation Picker ([[)
    if (showBookCitationModal) {
        BookCitationPickerModal(
            books = books.map { it.title to it.fileName },
            onDismiss = {
                showBookCitationModal = false
                citationPageInput = ""
            },
            onInsert = { bookName, pageNum ->
                val tag = if (pageNum != null && pageNum > 0) "[[$bookName#p.$pageNum]]" else "[[$bookName]]"
                insertAtCursor(tag)
                showBookCitationModal = false
                citationPageInput = ""
            }
        )
    }

    // Modal: Visual Snip Transclusion Picker (![[)
    if (showSnipPickerModal) {
        SnipPickerModal(
            snips = snipFiles,
            onDismiss = { showSnipPickerModal = false },
            onInsert = { snipFileName, caption ->
                val embed = if (caption.isNotBlank()) "![[$snipFileName|$caption]]" else "![[$snipFileName]]"
                insertAtCursor(embed)
                showSnipPickerModal = false
            }
        )
    }

    // Lightbox Dialog for Snip Image
    selectedSnipForLightbox?.let { snipFile ->
        Dialog(onDismissRequest = { selectedSnipForLightbox = null }) {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = colors.surface,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    val bitmap = remember(snipFile) {
                        try {
                            BitmapFactory.decodeFile(snipFile.absolutePath)?.asImageBitmap()
                        } catch (_: Exception) {
                            null
                        }
                    }

                    if (bitmap != null) {
                        Image(
                            bitmap = bitmap,
                            contentDescription = snipFile.name,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(280.dp)
                                .clip(RoundedCornerShape(12.dp)),
                            contentScale = ContentScale.Fit
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(12.dp))
                                .background(colors.surfaceTint),
                            contentAlignment = Alignment.Center
                        ) {
                            Text("Visual Snip: ${snipFile.name}", color = colors.textPrimary)
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))
                    Text(snipFile.nameWithoutExtension, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = colors.textPrimary)

                    Spacer(modifier = Modifier.height(16.dp))
                    TextButton(onClick = { selectedSnipForLightbox = null }) {
                        Text("Close", color = colors.primary)
                    }
                }
            }
        }
    }

    // Backlinks Dialog
    if (showBacklinksDialog) {
        AlertDialog(
            onDismissRequest = { showBacklinksDialog = false },
            title = { Text("Note Citations & References", color = colors.textPrimary) },
            text = {
                val citations = DeskMarkdownPatterns.BOOK_WIKILINK_PATTERN.findAll(textFieldValue.text).toList()
                if (citations.isEmpty()) {
                    Text("No interactive book citations in this note yet. Use the 📖 button on the dock to cite textbook pages.", color = colors.textMuted, fontSize = 13.sp)
                } else {
                    LazyColumn(modifier = Modifier.height(200.dp)) {
                        items(citations) { match ->
                            val book = match.groupValues[1]
                            val page = match.groupValues.getOrNull(2)?.toIntOrNull()
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        showBacklinksDialog = false
                                        onOpenBookAtPage(book, page ?: 1)
                                    }
                                    .padding(vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(FluentIcons.BookOpen24Regular, contentDescription = null, tint = colors.primary, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "$book${if (page != null) " (Page $page)" else ""}",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = colors.textPrimary
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showBacklinksDialog = false }) {
                    Text("Close", color = colors.primary)
                }
            }
        )
    }
}

/**
 * Redesigned, perfectly aligned unified accessory tool dock
 */
@Composable
private fun UnifiedAccessoryDock(
    onH1: () -> Unit,
    onH2: () -> Unit,
    onBold: () -> Unit,
    onItalic: () -> Unit,
    onBookCitation: () -> Unit,
    onSnip: () -> Unit,
    onTask: () -> Unit,
    onQuote: () -> Unit
) {
    val colors = LocalMulberryColors.current
    val scrollState = rememberScrollState()

    Surface(
        color = colors.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.borderSubtle),
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .imePadding()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .horizontalScroll(scrollState)
                .padding(horizontal = 8.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            DockItem(label = "H1", onClick = onH1)
            DockItem(label = "H2", onClick = onH2)
            DockItem(label = "B", fontWeight = FontWeight.Bold, onClick = onBold)
            DockItem(label = "I", fontStyle = FontStyle.Italic, onClick = onItalic)
            DockItem(icon = FluentIcons.BookOpen24Regular, contentDescription = "Book Citation", onClick = onBookCitation)
            DockItem(icon = FluentIcons.Scissors24Regular, contentDescription = "Snip Embed", onClick = onSnip)
            DockItem(icon = FluentIcons.TaskListSquareLtr24Regular, contentDescription = "Task Checkbox", onClick = onTask)
            DockItem(label = "“", onClick = onQuote)
        }
    }
}

@Composable
private fun DockItem(
    label: String? = null,
    icon: ImageVector? = null,
    contentDescription: String? = null,
    fontWeight: FontWeight = FontWeight.SemiBold,
    fontStyle: FontStyle = FontStyle.Normal,
    onClick: () -> Unit
) {
    val colors = LocalMulberryColors.current
    Surface(
        shape = RoundedCornerShape(10.dp),
        color = colors.surfaceTint.copy(alpha = 0.7f),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, colors.borderSubtle),
        modifier = Modifier
            .height(38.dp)
            .width(42.dp)
            .clip(RoundedCornerShape(10.dp))
            .clickable(onClick = onClick)
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = contentDescription,
                    tint = colors.primary,
                    modifier = Modifier.size(19.dp)
                )
            } else if (label != null) {
                Text(
                    text = label,
                    fontSize = 13.sp,
                    fontWeight = fontWeight,
                    fontStyle = fontStyle,
                    color = colors.textPrimary
                )
            }
        }
    }
}

/**
 * Clean Rendered Reading Mode Canvas
 */
@Composable
private fun RenderedMarkdownCanvas(
    markdown: String,
    repository: DeskRepository,
    onToggleCheckbox: (Int, String) -> Unit,
    onOpenBookAtPage: (String, Int) -> Unit,
    onOpenSnip: (File) -> Unit
) {
    val colors = LocalMulberryColors.current
    val lines = remember(markdown) { markdown.lines() }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 60.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(lines.size) { index ->
            val line = lines[index]
            val trimmed = line.trim()

            when {
                trimmed.startsWith("# ") -> {
                    Text(
                        text = trimmed.removePrefix("# ").trim(),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary,
                        letterSpacing = (-0.5).sp,
                        modifier = Modifier.padding(top = 12.dp, bottom = 4.dp)
                    )
                }
                trimmed.startsWith("## ") -> {
                    Text(
                        text = trimmed.removePrefix("## ").trim(),
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary,
                        modifier = Modifier.padding(top = 10.dp, bottom = 2.dp)
                    )
                }
                trimmed.startsWith("### ") -> {
                    Text(
                        text = trimmed.removePrefix("### ").trim(),
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }
                DeskMarkdownPatterns.TASK_PATTERN.containsMatchIn(line) -> {
                    val match = DeskMarkdownPatterns.TASK_PATTERN.find(line)!!
                    val isChecked = match.groupValues[2].trim().equals("x", ignoreCase = true)
                    val rawContent = match.groupValues[3].trim()

                    val bookMatch = DeskMarkdownPatterns.BOOK_WIKILINK_PATTERN.find(rawContent)
                    val linkedBook = bookMatch?.groupValues?.get(1)?.trim()
                    val linkedPage = bookMatch?.groupValues?.getOrNull(2)?.toIntOrNull()

                    val displayContent = rawContent
                        .replace(DeskMarkdownPatterns.BOOK_WIKILINK_PATTERN, "")
                        .replace(DeskMarkdownPatterns.DUE_DATE_PATTERN, "")
                        .trim()

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Checkbox(
                            checked = isChecked,
                            onCheckedChange = { onToggleCheckbox(index, line) },
                            colors = CheckboxDefaults.colors(
                                checkedColor = colors.primary,
                                uncheckedColor = colors.textMuted
                            ),
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = displayContent,
                                fontSize = 14.sp,
                                color = if (isChecked) colors.textMuted else colors.textPrimary,
                                textDecoration = if (isChecked) TextDecoration.LineThrough else TextDecoration.None
                            )
                            if (linkedBook != null) {
                                Spacer(modifier = Modifier.height(2.dp))
                                InteractiveBookPill(
                                    bookName = linkedBook,
                                    page = linkedPage ?: 1,
                                    onClick = { onOpenBookAtPage(linkedBook, linkedPage ?: 1) }
                                )
                            }
                        }
                    }
                }
                DeskMarkdownPatterns.SNIP_EMBED_PATTERN.containsMatchIn(line) -> {
                    val match = DeskMarkdownPatterns.SNIP_EMBED_PATTERN.find(line)!!
                    val fileName = match.groupValues[1].trim()
                    val caption = match.groupValues.getOrNull(2)?.trim()
                    val snipFile = File(repository.snipsDir, fileName)

                    RenderedSnipCard(
                        snipFile = snipFile,
                        caption = caption,
                        onClick = { if (snipFile.exists()) onOpenSnip(snipFile) }
                    )
                }
                DeskMarkdownPatterns.BOOK_WIKILINK_PATTERN.containsMatchIn(line) -> {
                    val match = DeskMarkdownPatterns.BOOK_WIKILINK_PATTERN.find(line)!!
                    val bookName = match.groupValues[1].trim()
                    val page = match.groupValues.getOrNull(2)?.toIntOrNull() ?: 1

                    Row(modifier = Modifier.padding(vertical = 4.dp)) {
                        InteractiveBookPill(
                            bookName = bookName,
                            page = page,
                            onClick = { onOpenBookAtPage(bookName, page) }
                        )
                    }
                }
                trimmed.startsWith("> ") -> {
                    val quoteText = trimmed.removePrefix("> ").trim()
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clip(RoundedCornerShape(topEnd = 8.dp, bottomEnd = 8.dp))
                            .background(colors.surfaceTint.copy(alpha = 0.5f))
                            .border(
                                width = 3.dp,
                                color = colors.primary,
                                shape = RoundedCornerShape(0.dp)
                            )
                            .padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = quoteText,
                            fontSize = 14.sp,
                            fontStyle = FontStyle.Italic,
                            color = colors.textPrimary
                        )
                    }
                }
                trimmed.isNotEmpty() -> {
                    Text(
                        text = trimmed,
                        fontSize = 15.sp,
                        lineHeight = 22.sp,
                        color = colors.textPrimary
                    )
                }
            }
        }
    }
}

@Composable
private fun InteractiveBookPill(
    bookName: String,
    page: Int,
    onClick: () -> Unit
) {
    val colors = LocalMulberryColors.current
    Surface(
        shape = RoundedCornerShape(8.dp),
        color = colors.primary.copy(alpha = 0.12f),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, colors.primary.copy(alpha = 0.35f)),
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = FluentIcons.BookOpen24Regular,
                contentDescription = null,
                tint = colors.primary,
                modifier = Modifier.size(13.dp)
            )
            Spacer(modifier = Modifier.width(5.dp))
            Text(
                text = "📖 $bookName · p.$page",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.primary
            )
        }
    }
}

@Composable
private fun RenderedSnipCard(
    snipFile: File,
    caption: String?,
    onClick: () -> Unit
) {
    val colors = LocalMulberryColors.current
    val bitmap = remember(snipFile) {
        if (snipFile.exists()) {
            try {
                BitmapFactory.decodeFile(snipFile.absolutePath)?.asImageBitmap()
            } catch (_: Exception) {
                null
            }
        } else null
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = colors.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.borderSubtle),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap,
                    contentDescription = caption ?: snipFile.name,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(110.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(colors.surfaceTint),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(FluentIcons.Scissors24Regular, contentDescription = null, tint = colors.primary, modifier = Modifier.size(24.dp))
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(snipFile.name, fontSize = 12.sp, color = colors.textPrimary)
                    }
                }
            }

            if (!caption.isNullOrBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = caption,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.textSecondary
                )
            }
        }
    }
}

@Composable
private fun BookCitationPickerModal(
    books: List<Pair<String, String>>,
    onDismiss: () -> Unit,
    onInsert: (bookName: String, pageNum: Int?) -> Unit
) {
    val colors = LocalMulberryColors.current
    var selectedBook by remember { mutableStateOf<String?>(books.firstOrNull()?.first) }
    var pageInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Insert Book Citation", color = colors.textPrimary) },
        text = {
            Column {
                Text("Select textbook from vault library:", fontSize = 13.sp, color = colors.textSecondary)
                Spacer(modifier = Modifier.height(8.dp))
                LazyColumn(modifier = Modifier.height(140.dp)) {
                    items(books) { (title, _) ->
                        val isSelected = selectedBook == title
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = if (isSelected) colors.surfaceTint else colors.surface,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { selectedBook = title }
                                .padding(vertical = 4.dp)
                        ) {
                            Text(
                                text = title,
                                fontSize = 13.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) colors.primary else colors.textPrimary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = pageInput,
                    onValueChange = { pageInput = it.filter { ch -> ch.isDigit() } },
                    label = { Text("Page number (optional)") },
                    placeholder = { Text("e.g. 412") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (selectedBook != null) {
                        onInsert(selectedBook!!, pageInput.toIntOrNull())
                    }
                }
            ) {
                Text("Insert [[ ]]", color = colors.primary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = colors.textSecondary)
            }
        }
    )
}

@Composable
private fun SnipPickerModal(
    snips: List<File>,
    onDismiss: () -> Unit,
    onInsert: (snipFileName: String, caption: String) -> Unit
) {
    val colors = LocalMulberryColors.current
    var selectedFile by remember { mutableStateOf<File?>(snips.firstOrNull()) }
    var captionInput by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Insert Visual Snip", color = colors.textPrimary) },
        text = {
            Column {
                if (snips.isEmpty()) {
                    Text("No snips found in .mulberry/snips/ yet.", color = colors.textMuted, fontSize = 13.sp)
                } else {
                    Text("Select a diagram or slide snip:", fontSize = 13.sp, color = colors.textSecondary)
                    Spacer(modifier = Modifier.height(8.dp))
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(2),
                        modifier = Modifier.height(180.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(snips) { file ->
                            val isSelected = selectedFile == file
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = if (isSelected) colors.surfaceTint else colors.surface,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelected) colors.primary else colors.borderSubtle
                                ),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable { selectedFile = file }
                            ) {
                                Column(modifier = Modifier.padding(6.dp)) {
                                    Icon(FluentIcons.Scissors24Regular, contentDescription = null, tint = colors.primary, modifier = Modifier.size(16.dp))
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(file.nameWithoutExtension, fontSize = 11.sp, maxLines = 1, overflow = TextOverflow.Ellipsis, color = colors.textPrimary)
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(10.dp))
                    OutlinedTextField(
                        value = captionInput,
                        onValueChange = { captionInput = it },
                        label = { Text("Caption (optional)") },
                        placeholder = { Text("e.g. Brachial Plexus Cords") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    if (selectedFile != null) {
                        onInsert(selectedFile!!.name, captionInput.trim())
                    }
                }
            ) {
                Text("Insert ![[ ]]", color = colors.primary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = colors.textSecondary)
            }
        }
    )
}
