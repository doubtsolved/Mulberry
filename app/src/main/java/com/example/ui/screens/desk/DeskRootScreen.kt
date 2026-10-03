package com.example.ui.screens.desk

import android.view.HapticFeedbackConstants
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.desk.DeskFolder
import com.example.data.desk.DeskRepository
import com.example.data.desk.DeskTask
import com.example.ui.components.FluentIcons
import com.example.ui.theme.LocalMulberryColors
import com.example.viewmodel.MulberryViewModel
import kotlinx.coroutines.launch
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DeskRootScreen(
    viewModel: MulberryViewModel,
    repository: DeskRepository,
    onOpenFolder: (String) -> Unit,
    onOpenNote: (folderName: String, file: File) -> Unit,
    onOpenBookAtPage: (bookName: String, page: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalMulberryColors.current
    val coroutineScope = rememberCoroutineScope()
    val view = LocalView.current
    val books by viewModel.allBooks.collectAsState()
    val activeVault by viewModel.activeVault.collectAsState()
    val allVaults by viewModel.allVaults.collectAsState()

    var showVaultMenu by remember { mutableStateOf(false) }
    var activeViewTab by remember { mutableIntStateOf(0) } // 0 = Folders, 1 = Agenda
    var folders by remember { mutableStateOf<List<DeskFolder>>(emptyList()) }
    var allTasks by remember { mutableStateOf<List<DeskTask>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Dialog & Sheet States
    var showNewFolderDialog by remember { mutableStateOf(false) }
    var newFolderNameInput by remember { mutableStateOf("") }
    var showSearchDialog by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    var selectedFolderForOptions by remember { mutableStateOf<DeskFolder?>(null) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var renameInput by remember { mutableStateOf("") }
    var showBindBookDialog by remember { mutableStateOf(false) }
    var showDeleteConfirmDialog by remember { mutableStateOf(false) }

    val reloadData: () -> Unit = {
        coroutineScope.launch {
            folders = repository.getFolders()
            allTasks = repository.harvestAllTasks()
            isLoading = false
        }
    }

    LaunchedEffect(activeVault) {
        if (activeVault != null) {
            val rootPath = activeVault!!.pathDisplay.ifEmpty { activeVault!!.uriString }
            repository.setVaultDirectory(File(rootPath))
        }
        reloadData()
    }

    val pendingTasksTotal = allTasks.count { !it.isCompleted }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        // TOP APP BAR (Strictly Icon-Only Actions)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Active Vault Chip with Switcher Dropdown
            Box {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = colors.surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.borderSubtle),
                    modifier = Modifier
                        .clip(RoundedCornerShape(14.dp))
                        .clickable {
                            if (allVaults.size > 1) {
                                showVaultMenu = true
                            }
                        }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = FluentIcons.Folder24Filled,
                            contentDescription = "Vault",
                            tint = colors.primary,
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = activeVault?.name ?: "MedicalVault",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            color = colors.textPrimary
                        )
                        if (allVaults.size > 1) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                imageVector = FluentIcons.ChevronDown24Regular,
                                contentDescription = "Switch Vault",
                                tint = colors.textSecondary,
                                modifier = Modifier.size(12.dp)
                            )
                        }
                    }
                }

                if (allVaults.size > 1) {
                    DropdownMenu(
                        expanded = showVaultMenu,
                        onDismissRequest = { showVaultMenu = false },
                        containerColor = colors.surface
                    ) {
                        allVaults.forEach { vault ->
                            DropdownMenuItem(
                                text = {
                                    Text(
                                        text = vault.name,
                                        fontWeight = if (vault.id == activeVault?.id) FontWeight.Bold else FontWeight.Normal,
                                        color = if (vault.id == activeVault?.id) colors.primary else colors.textPrimary
                                    )
                                },
                                onClick = {
                                    showVaultMenu = false
                                    viewModel.setActiveVault(vault.id)
                                    val rootPath = vault.pathDisplay.ifEmpty { vault.uriString }
                                    repository.setVaultDirectory(File(rootPath))
                                    reloadData()
                                }
                            )
                        }
                    }
                }
            }

            // Icon-Only Actions: Search [🔍] and Add Folder [📁+]
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = { showSearchDialog = true },
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = FluentIcons.Search24Regular,
                        contentDescription = "Search Notes",
                        tint = colors.textPrimary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                IconButton(
                    onClick = {
                        newFolderNameInput = ""
                        showNewFolderDialog = true
                    },
                    modifier = Modifier.size(44.dp)
                ) {
                    Icon(
                        imageVector = FluentIcons.FolderAdd24Regular,
                        contentDescription = "New Folder",
                        tint = colors.textPrimary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }

        // VIEW SWITCHER: Folders <---> Agenda (Count)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp)
        ) {
            TabRow(
                selectedTabIndex = activeViewTab,
                containerColor = colors.surface,
                contentColor = colors.primary,
                divider = {},
                modifier = Modifier
                    .clip(RoundedCornerShape(14.dp))
                    .border(1.dp, colors.borderSubtle, RoundedCornerShape(14.dp))
            ) {
                Tab(
                    selected = activeViewTab == 0,
                    onClick = {
                        activeViewTab = 0
                        reloadData()
                    },
                    text = {
                        Text(
                            text = "Folders",
                            fontSize = 13.sp,
                            fontWeight = if (activeViewTab == 0) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (activeViewTab == 0) colors.primary else colors.textSecondary
                        )
                    }
                )
                Tab(
                    selected = activeViewTab == 1,
                    onClick = {
                        activeViewTab = 1
                        reloadData()
                    },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = "Agenda",
                                fontSize = 13.sp,
                                fontWeight = if (activeViewTab == 1) FontWeight.SemiBold else FontWeight.Normal,
                                color = if (activeViewTab == 1) colors.primary else colors.textSecondary
                            )
                            if (pendingTasksTotal > 0) {
                                Spacer(modifier = Modifier.width(6.dp))
                                Box(
                                    modifier = Modifier
                                        .clip(CircleShape)
                                        .background(if (activeViewTab == 1) colors.primary else colors.surfaceTint)
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(
                                        text = "$pendingTasksTotal",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = if (activeViewTab == 1) Color.White else colors.textPrimary
                                    )
                                }
                            }
                        }
                    }
                )
            }
        }

        // VIEW CONTENT
        if (activeViewTab == 0) {
            // VIEW A: 2-Column Responsive Folder Grid
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(16.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(folders, key = { it.id }) { folder ->
                    FolderGridCard(
                        folder = folder,
                        onClick = { onOpenFolder(folder.name) },
                        onLongClick = {
                            view.performHapticFeedback(HapticFeedbackConstants.LONG_PRESS)
                            selectedFolderForOptions = folder
                        }
                    )
                }
            }
        } else {
            // VIEW B: Harvested Task Stream
            AgendaStreamView(
                tasks = allTasks,
                onToggleTask = { task ->
                    coroutineScope.launch {
                        view.performHapticFeedback(HapticFeedbackConstants.KEYBOARD_TAP)
                        repository.toggleTaskStatus(task)
                        allTasks = repository.harvestAllTasks()
                        folders = repository.getFolders()
                    }
                },
                onOpenNote = onOpenNote,
                onOpenBookAtPage = onOpenBookAtPage
            )
        }
    }

    // New Folder Dialog
    if (showNewFolderDialog) {
        AlertDialog(
            onDismissRequest = { showNewFolderDialog = false },
            title = { Text("New Subject Folder", color = colors.textPrimary) },
            text = {
                OutlinedTextField(
                    value = newFolderNameInput,
                    onValueChange = { newFolderNameInput = it },
                    placeholder = { Text("e.g. Pathology, Biochemistry") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (newFolderNameInput.isNotBlank()) {
                            coroutineScope.launch {
                                repository.createFolder(newFolderNameInput.trim())
                                showNewFolderDialog = false
                                reloadData()
                            }
                        }
                    }
                ) {
                    Text("Create", color = colors.primary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showNewFolderDialog = false }) {
                    Text("Cancel", color = colors.textSecondary)
                }
            }
        )
    }

    // Global Search Dialog
    if (showSearchDialog) {
        SearchNotesDialog(
            query = searchQuery,
            onQueryChange = { searchQuery = it },
            allTasks = allTasks,
            folders = folders,
            onDismiss = { showSearchDialog = false },
            onSelectFolder = { folderName ->
                showSearchDialog = false
                onOpenFolder(folderName)
            },
            onSelectNote = { folderName, file ->
                showSearchDialog = false
                onOpenNote(folderName, file)
            }
        )
    }

    // Folder Context Options Bottom Sheet
    selectedFolderForOptions?.let { folder ->
        val sheetState = rememberModalBottomSheetState()
        ModalBottomSheet(
            onDismissRequest = { selectedFolderForOptions = null },
            sheetState = sheetState,
            containerColor = colors.surface
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                Text(
                    text = folder.name,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // Rename
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            renameInput = folder.name
                            showRenameDialog = true
                        }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(FluentIcons.Edit24Regular, contentDescription = "Rename", tint = colors.textPrimary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Rename Folder", fontSize = 15.sp, color = colors.textPrimary)
                }

                // Bind Textbook
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { showBindBookDialog = true }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(FluentIcons.BookOpen24Regular, contentDescription = "Bind Book", tint = colors.textPrimary, modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Text(
                        text = if (folder.boundBookFileName != null) "Change Bound Textbook" else "Bind Default Textbook",
                        fontSize = 15.sp,
                        color = colors.textPrimary
                    )
                }

                // Delete
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable { showDeleteConfirmDialog = true }
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(FluentIcons.Dismiss24Regular, contentDescription = "Delete", tint = Color(0xFFD32F2F), modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(16.dp))
                    Text("Delete Folder", fontSize = 15.sp, color = Color(0xFFD32F2F))
                }

                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }

    // Rename Folder Dialog
    if (showRenameDialog && selectedFolderForOptions != null) {
        val target = selectedFolderForOptions!!
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
                                repository.renameFolder(target.name, renameInput.trim())
                                showRenameDialog = false
                                selectedFolderForOptions = null
                                reloadData()
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

    // Bind Textbook Dialog
    if (showBindBookDialog && selectedFolderForOptions != null) {
        val target = selectedFolderForOptions!!
        AlertDialog(
            onDismissRequest = { showBindBookDialog = false },
            title = { Text("Bind Textbook to ${target.name}", color = colors.textPrimary) },
            text = {
                Column {
                    Text("Select a textbook to bind to this topic folder for quick 1-tap reference:", fontSize = 13.sp, color = colors.textSecondary)
                    Spacer(modifier = Modifier.height(12.dp))
                    LazyColumn(modifier = Modifier.height(200.dp)) {
                        item {
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        repository.bindBookToFolder(target.name, null)
                                        showBindBookDialog = false
                                        selectedFolderForOptions = null
                                        reloadData()
                                    }
                                    .padding(vertical = 8.dp)
                            ) {
                                Text("(None - Unbind)", color = colors.textSecondary, fontSize = 14.sp)
                            }
                        }
                        items(books) { book ->
                            Surface(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        repository.bindBookToFolder(target.name, book.fileName)
                                        showBindBookDialog = false
                                        selectedFolderForOptions = null
                                        reloadData()
                                    }
                                    .padding(vertical = 8.dp)
                            ) {
                                Text(book.title, color = colors.textPrimary, fontSize = 14.sp, maxLines = 1, overflow = TextOverflow.Ellipsis)
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = { showBindBookDialog = false }) {
                    Text("Close", color = colors.textSecondary)
                }
            }
        )
    }

    // Delete Folder Safeguard Dialog
    if (showDeleteConfirmDialog && selectedFolderForOptions != null) {
        val target = selectedFolderForOptions!!
        AlertDialog(
            onDismissRequest = { showDeleteConfirmDialog = false },
            title = { Text("Delete '${target.name}'?", color = colors.textPrimary) },
            text = {
                Text("This will permanently delete this folder and all ${target.noteCount} notes inside it from disk. This cannot be undone.", color = colors.textSecondary)
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        coroutineScope.launch {
                            repository.deleteFolder(target.name)
                            showDeleteConfirmDialog = false
                            selectedFolderForOptions = null
                            reloadData()
                        }
                    }
                ) {
                    Text("Delete", color = Color(0xFFD32F2F))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmDialog = false }) {
                    Text("Cancel", color = colors.textSecondary)
                }
            }
        )
    }
}

/**
 * 2-Column Responsive Folder Grid Card
 */
@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
private fun FolderGridCard(
    folder: DeskFolder,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val colors = LocalMulberryColors.current
    val totalTasks = folder.pendingTasksCount + folder.completedTasksCount

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = colors.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.borderSubtle),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Column(
            modifier = Modifier.padding(14.dp)
        ) {
            // Folder Icon + Note Count Badge
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.surfaceTint),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = FluentIcons.Folder24Filled,
                        contentDescription = "Folder",
                        tint = colors.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = colors.background,
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, colors.borderSubtle)
                ) {
                    Text(
                        text = "${folder.noteCount} notes",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = colors.textSecondary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Folder Name
            Text(
                text = folder.name,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Task Progress Mini Bar
            if (totalTasks > 0) {
                val progress = folder.completedTasksCount.toFloat() / totalTasks.toFloat()
                LinearProgressIndicator(
                    progress = { progress },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp)),
                    color = colors.primary,
                    trackColor = colors.borderSubtle
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${folder.completedTasksCount}/$totalTasks tasks done",
                    fontSize = 11.sp,
                    color = colors.textMuted
                )
            } else {
                Text(
                    text = "No pending tasks",
                    fontSize = 11.sp,
                    color = colors.textMuted
                )
            }

            // Bound Textbook Pill (if mapped)
            if (!folder.boundBookFileName.isNullOrEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(colors.surfaceTint.copy(alpha = 0.5f))
                        .padding(horizontal = 6.dp, vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = FluentIcons.BookOpen24Regular,
                        contentDescription = "Book",
                        tint = colors.primary,
                        modifier = Modifier.size(12.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = folder.boundBookFileName.removeSuffix(".pdf"),
                        fontSize = 10.sp,
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

/**
 * Agenda Stream View: Aggregated task stream parsed across all .md files
 */
@Composable
private fun AgendaStreamView(
    tasks: List<DeskTask>,
    onToggleTask: (DeskTask) -> Unit,
    onOpenNote: (folderName: String, file: File) -> Unit,
    onOpenBookAtPage: (bookName: String, page: Int) -> Unit
) {
    val colors = LocalMulberryColors.current

    if (tasks.isEmpty()) {
        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    imageVector = FluentIcons.TaskListSquareLtr24Regular,
                    contentDescription = "No tasks",
                    tint = colors.textMuted,
                    modifier = Modifier.size(48.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "No checklists found in notes",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = colors.textSecondary
                )
                Text(
                    text = "Type - [ ] in any markdown note to harvest tasks here",
                    fontSize = 12.sp,
                    color = colors.textMuted
                )
            }
        }
        return
    }

    val overdueAndToday = tasks.filter { !it.isCompleted && (it.dueDate != null && it.dueDate <= "2026-10-15") }
    val upcoming = tasks.filter { !it.isCompleted && (it.dueDate == null || it.dueDate > "2026-10-15") }
    val completed = tasks.filter { it.isCompleted }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (overdueAndToday.isNotEmpty()) {
            item {
                SectionHeader(title = "PRIORITY & TODAY (${overdueAndToday.size})", color = colors.primary)
            }
            items(overdueAndToday, key = { it.id }) { task ->
                TaskItemCard(task = task, onToggle = { onToggleTask(task) }, onOpenNote = onOpenNote, onOpenBookAtPage = onOpenBookAtPage)
            }
        }

        if (upcoming.isNotEmpty()) {
            item {
                SectionHeader(title = "UPCOMING TASKS (${upcoming.size})", color = colors.textSecondary)
            }
            items(upcoming, key = { it.id }) { task ->
                TaskItemCard(task = task, onToggle = { onToggleTask(task) }, onOpenNote = onOpenNote, onOpenBookAtPage = onOpenBookAtPage)
            }
        }

        if (completed.isNotEmpty()) {
            item {
                SectionHeader(title = "COMPLETED (${completed.size})", color = colors.textMuted)
            }
            items(completed, key = { it.id }) { task ->
                TaskItemCard(task = task, onToggle = { onToggleTask(task) }, onOpenNote = onOpenNote, onOpenBookAtPage = onOpenBookAtPage)
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String, color: Color) {
    Text(
        text = title,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        color = color,
        letterSpacing = 0.5.sp,
        modifier = Modifier.padding(top = 10.dp, bottom = 4.dp, start = 4.dp)
    )
}

@Composable
private fun TaskItemCard(
    task: DeskTask,
    onToggle: () -> Unit,
    onOpenNote: (folderName: String, file: File) -> Unit,
    onOpenBookAtPage: (bookName: String, page: Int) -> Unit
) {
    val colors = LocalMulberryColors.current

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = colors.surface,
        border = androidx.compose.foundation.BorderStroke(0.5.dp, colors.borderSubtle),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.Top
        ) {
            Checkbox(
                checked = task.isCompleted,
                onCheckedChange = { onToggle() },
                colors = CheckboxDefaults.colors(
                    checkedColor = colors.primary,
                    uncheckedColor = colors.textMuted
                ),
                modifier = Modifier.size(24.dp)
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                // Task Content Text
                Text(
                    text = task.content,
                    fontSize = 14.sp,
                    fontWeight = if (task.isCompleted) FontWeight.Normal else FontWeight.Medium,
                    color = if (task.isCompleted) colors.textMuted else colors.textPrimary,
                    textDecoration = if (task.isCompleted) TextDecoration.LineThrough else TextDecoration.None
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Metadata: Folder • Note Title
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onOpenNote(task.folderName, task.sourceFile) }
                ) {
                    Text(
                        text = "${task.folderName} • ${task.noteTitle}",
                        fontSize = 11.sp,
                        color = colors.primary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                // Metadata Chips: Due Date & Book Wikilink
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (task.dueDate != null) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = colors.surfaceTint,
                            modifier = Modifier.padding(vertical = 2.dp)
                        ) {
                            Text(
                                text = "📅 ${task.dueDate}",
                                fontSize = 10.sp,
                                color = colors.textPrimary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }

                    if (task.linkedBook != null) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = colors.primary.copy(alpha = 0.12f),
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .clickable {
                                    onOpenBookAtPage(task.linkedBook, task.linkedPage ?: 1)
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    imageVector = FluentIcons.BookOpen24Regular,
                                    contentDescription = "Book",
                                    tint = colors.primary,
                                    modifier = Modifier.size(11.dp)
                                )
                                Spacer(modifier = Modifier.width(3.dp))
                                Text(
                                    text = "${task.linkedBook}${if (task.linkedPage != null) " · p.${task.linkedPage}" else ""}",
                                    fontSize = 10.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.primary
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SearchNotesDialog(
    query: String,
    onQueryChange: (String) -> Unit,
    allTasks: List<DeskTask>,
    folders: List<DeskFolder>,
    onDismiss: () -> Unit,
    onSelectFolder: (String) -> Unit,
    onSelectNote: (String, File) -> Unit
) {
    val colors = LocalMulberryColors.current
    val filteredTasks = if (query.isBlank()) emptyList() else allTasks.filter {
        it.content.contains(query, ignoreCase = true) || it.noteTitle.contains(query, ignoreCase = true)
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            OutlinedTextField(
                value = query,
                onValueChange = onQueryChange,
                placeholder = { Text("Search notes and tasks...") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            LazyColumn(modifier = Modifier.height(260.dp)) {
                if (filteredTasks.isEmpty() && query.isNotBlank()) {
                    item {
                        Text("No matching notes or tasks found.", color = colors.textMuted, fontSize = 13.sp)
                    }
                }
                items(filteredTasks) { task ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onSelectNote(task.folderName, task.sourceFile) }
                            .padding(vertical = 8.dp)
                    ) {
                        Text(
                            text = "${task.noteTitle}: ${task.content}",
                            fontSize = 13.sp,
                            color = colors.textPrimary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Close", color = colors.textSecondary)
            }
        }
    )
}
