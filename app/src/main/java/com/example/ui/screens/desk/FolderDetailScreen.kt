package com.example.ui.screens.desk

import android.text.format.DateUtils
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.desk.DeskNote
import com.example.data.desk.DeskRepository
import com.example.ui.components.FluentIcons
import com.example.ui.theme.LocalMulberryColors
import com.example.viewmodel.MulberryViewModel
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun FolderDetailScreen(
    folderName: String,
    repository: DeskRepository,
    viewModel: MulberryViewModel,
    onBack: () -> Unit,
    onOpenNote: (File) -> Unit,
    onOpenBook: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalMulberryColors.current
    val coroutineScope = rememberCoroutineScope()
    val books by viewModel.allBooks.collectAsState()

    var notes by remember { mutableStateOf<List<DeskNote>>(emptyList()) }
    var boundBookFileName by remember { mutableStateOf<String?>(null) }
    var searchQuery by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }

    var sortOrder by remember { mutableStateOf(0) } // 0 = Date Modified, 1 = Title A-Z
    var showSortMenu by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }

    var showNewNoteDialog by remember { mutableStateOf(false) }
    var newNoteTitleInput by remember { mutableStateOf("") }

    var showRenameDialog by remember { mutableStateOf(false) }
    var renameInput by remember { mutableStateOf(folderName) }
    var currentFolderName by remember { mutableStateOf(folderName) }

    val reloadNotes: () -> Unit = {
        coroutineScope.launch {
            notes = repository.getNotesInFolder(currentFolderName)
            val folders = repository.getFolders()
            boundBookFileName = folders.find { it.name == currentFolderName }?.boundBookFileName
        }
    }

    LaunchedEffect(currentFolderName) {
        reloadNotes()
    }

    BackHandler {
        onBack()
    }

    val displayNotes = remember(notes, searchQuery, sortOrder) {
        val filtered = if (searchQuery.isBlank()) notes else notes.filter {
            it.title.contains(searchQuery, ignoreCase = true) || it.excerpt.contains(searchQuery, ignoreCase = true)
        }
        when (sortOrder) {
            1 -> filtered.sortedBy { it.title.lowercase() }
            else -> filtered.sortedByDescending { it.lastModified }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // TOP APP BAR (Strictly Icon-Only Actions)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 8.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Back Button [←] (Icon only)
                IconButton(onClick = onBack, modifier = Modifier.size(44.dp)) {
                    Icon(
                        imageVector = FluentIcons.ArrowLeft24Regular,
                        contentDescription = "Back",
                        tint = colors.textPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }

                // Folder Title
                if (isSearching) {
                    OutlinedTextField(
                        value = searchQuery,
                        onValueChange = { searchQuery = it },
                        placeholder = { Text("Filter notes...", fontSize = 14.sp) },
                        singleLine = true,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 4.dp)
                    )
                    IconButton(
                        onClick = {
                            searchQuery = ""
                            isSearching = false
                        },
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = FluentIcons.Dismiss24Regular,
                            contentDescription = "Close Search",
                            tint = colors.textSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                } else {
                    Text(
                        text = currentFolderName,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp)
                    )

                    // Icon-Only Action: Search [🔍]
                    IconButton(
                        onClick = { isSearching = true },
                        modifier = Modifier.size(44.dp)
                    ) {
                        Icon(
                            imageVector = FluentIcons.Search24Regular,
                            contentDescription = "Search Notes",
                            tint = colors.textPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    // Icon-Only Action: Sort [⇅]
                    Box {
                        IconButton(
                            onClick = { showSortMenu = true },
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(
                                imageVector = FluentIcons.ArrowSort24Regular,
                                contentDescription = "Sort Notes",
                                tint = colors.textPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = showSortMenu,
                            onDismissRequest = { showSortMenu = false },
                            containerColor = colors.surface
                        ) {
                            DropdownMenuItem(
                                text = { Text("Recently Modified", color = colors.textPrimary) },
                                onClick = {
                                    sortOrder = 0
                                    showSortMenu = false
                                }
                            )
                            DropdownMenuItem(
                                text = { Text("Alphabetical (A-Z)", color = colors.textPrimary) },
                                onClick = {
                                    sortOrder = 1
                                    showSortMenu = false
                                }
                            )
                        }
                    }

                    // Icon-Only Action: More Options [⋮]
                    Box {
                        IconButton(
                            onClick = { showMoreMenu = true },
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(
                                imageVector = FluentIcons.MoreVertical24Regular,
                                contentDescription = "Folder Options",
                                tint = colors.textPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        DropdownMenu(
                            expanded = showMoreMenu,
                            onDismissRequest = { showMoreMenu = false },
                            containerColor = colors.surface
                        ) {
                            DropdownMenuItem(
                                text = { Text("Rename Folder", color = colors.textPrimary) },
                                onClick = {
                                    renameInput = currentFolderName
                                    showRenameDialog = true
                                    showMoreMenu = false
                                }
                            )
                        }
                    }
                }
            }

            // PRIMARY TEXTBOOK BANNER (If bound)
            if (!boundBookFileName.isNullOrEmpty()) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = colors.surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.borderSubtle),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { onOpenBook(boundBookFileName!!) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(RoundedCornerShape(8.dp))
                                .background(colors.surfaceTint),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = FluentIcons.BookOpen24Regular,
                                contentDescription = "Book",
                                tint = colors.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "PRIMARY TEXTBOOK",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = colors.primary,
                                letterSpacing = 0.5.sp
                            )
                            Text(
                                text = boundBookFileName!!.removeSuffix(".pdf"),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        // Icon-only reader launch trigger
                        Icon(
                            imageVector = FluentIcons.ArrowRight24Regular,
                            contentDescription = "Open Book",
                            tint = colors.primary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }

            // NOTES LIST
            if (displayNotes.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = FluentIcons.DocumentText24Regular,
                            contentDescription = "No Notes",
                            tint = colors.textMuted,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "No notes in this folder yet",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.textSecondary
                        )
                        Text(
                            text = "Tap the + button to create a markdown note",
                            fontSize = 12.sp,
                            color = colors.textMuted
                        )
                    }
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    items(displayNotes, key = { it.file.absolutePath }) { note ->
                        NoteCardItem(
                            note = note,
                            onClick = { onOpenNote(note.file) }
                        )
                    }
                }
            }
        }

        // FLOATING ACTION BUTTON (Strictly Icon-Only: DocumentAdd [✏️+])
        FloatingActionButton(
            onClick = {
                newNoteTitleInput = ""
                showNewNoteDialog = true
            },
            containerColor = colors.primary,
            contentColor = Color.White,
            shape = CircleShape,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(24.dp)
                .size(56.dp)
        ) {
            Icon(
                imageVector = FluentIcons.DocumentAdd24Regular,
                contentDescription = "New Note",
                modifier = Modifier.size(24.dp)
            )
        }
    }

    // New Note Dialog
    if (showNewNoteDialog) {
        AlertDialog(
            onDismissRequest = { showNewNoteDialog = false },
            title = { Text("New Note", color = colors.textPrimary) },
            text = {
                OutlinedTextField(
                    value = newNoteTitleInput,
                    onValueChange = { newNoteTitleInput = it },
                    placeholder = { Text("e.g. Brachial Plexus Notes") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val title = newNoteTitleInput.trim().ifEmpty { "Untitled" }
                        coroutineScope.launch {
                            val newFile = repository.createNote(currentFolderName, title)
                            showNewNoteDialog = false
                            onOpenNote(newFile)
                        }
                    }
                ) {
                    Text("Create", color = colors.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewNoteDialog = false }) {
                    Text("Cancel", color = colors.textSecondary)
                }
            }
        )
    }

    // Rename Folder Dialog
    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Rename Folder", color = colors.textPrimary) },
            text = {
                OutlinedTextField(
                    value = renameInput,
                    onValueChange = { renameInput = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (renameInput.isNotBlank()) {
                            coroutineScope.launch {
                                val success = repository.renameFolder(currentFolderName, renameInput.trim())
                                if (success) {
                                    currentFolderName = renameInput.trim()
                                    showRenameDialog = false
                                    reloadNotes()
                                }
                            }
                        }
                    }
                ) {
                    Text("Rename", color = colors.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("Cancel", color = colors.textSecondary)
                }
            }
        )
    }
}

@Composable
private fun NoteCardItem(
    note: DeskNote,
    onClick: () -> Unit
) {
    val colors = LocalMulberryColors.current
    val relativeTime = DateUtils.getRelativeTimeSpanString(
        note.lastModified,
        System.currentTimeMillis(),
        DateUtils.MINUTE_IN_MILLIS
    ).toString()

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = colors.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.borderSubtle),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            // Note Title
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = FluentIcons.DocumentText24Regular,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = note.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f)
                )
            }

            // Excerpt
            if (note.excerpt.isNotEmpty()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = note.excerpt,
                    fontSize = 13.sp,
                    color = colors.textSecondary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 18.sp
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Metadata Footer: Relative Time & Tasks Chip
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = relativeTime,
                    fontSize = 11.sp,
                    color = colors.textMuted
                )

                if (note.tasksCount > 0) {
                    val pending = note.tasksCount - note.completedTasksCount
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = if (pending > 0) colors.surfaceTint else colors.background
                    ) {
                        Text(
                            text = if (pending > 0) "$pending tasks pending" else "All tasks done ✓",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            color = if (pending > 0) colors.primary else colors.textMuted,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }
        }
    }
}
