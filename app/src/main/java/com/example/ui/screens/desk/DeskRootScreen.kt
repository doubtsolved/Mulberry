@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.example.ui.screens.desk

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.BookEntity
import com.example.data.model.VaultEntity
import com.example.data.schedule.model.ReadingSession
import com.example.ui.components.FluentIcons
import com.example.ui.reader.ReaderActivity
import com.example.ui.theme.InterFamily
import com.example.ui.theme.LocalMulberryColors
import com.example.ui.theme.PoppinsFamily
import com.example.viewmodel.DeskScheduleViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun DeskRootScreen(
    modifier: Modifier = Modifier,
    deskViewModel: DeskScheduleViewModel = viewModel()
) {
    val context = LocalContext.current
    val colors = LocalMulberryColors.current
    val coroutineScope = rememberCoroutineScope()

    val activeVault by deskViewModel.activeVault.collectAsState()
    val vaultBooks by deskViewModel.vaultBooks.collectAsState()
    val downloadedBooks by deskViewModel.downloadedBooks.collectAsState()
    val isRefreshing by deskViewModel.isRefreshing.collectAsState()

    val activeTodaySessions by deskViewModel.activeTodaySessions.collectAsState()
    val completedTodaySessions by deskViewModel.completedTodaySessions.collectAsState()

    val weeklyGoalPages by deskViewModel.weeklyGoalPages.collectAsState()
    val weeklyCompletedPages by deskViewModel.weeklyCompletedPages.collectAsState()
    val weeklyPlannedPages by deskViewModel.weeklyPlannedPages.collectAsState()
    val weeklyProgressPercent by deskViewModel.weeklyProgressPercent.collectAsState()
    val todayPagesCount by deskViewModel.todayPagesCount.collectAsState()

    var showCreateSheet by remember { mutableStateOf(false) }
    var sessionToEdit by remember { mutableStateOf<ReadingSession?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    var showGoalSheet by remember { mutableStateOf(false) }
    val goalSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Shelf long-press actions bottom sheet state
    var selectedShelfBook by remember { mutableStateOf<BookEntity?>(null) }
    var bookPendingDelete by remember { mutableStateOf<BookEntity?>(null) }
    val shelfSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Collapsible folders on The Shelf (collapsed set, default empty so all subjects are expanded)
    var collapsedFolders by remember { mutableStateOf(setOf<String>()) }

    // Group downloaded books into Subject folders based on catalog metadata
    val booksBySubject = remember(downloadedBooks) {
        downloadedBooks
            .groupBy { it.subject.ifBlank { "General" }.trim() }
            .toSortedMap(String.CASE_INSENSITIVE_ORDER)
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(colors.backgroundGradient),
        containerColor = Color.Transparent,
        topBar = {
            // CLEAN TOP BAR: Single clean title "Desk" + Schedule Action Pill "[ + Plan Goal ]"
            DeskTopAppBar(
                isRefreshing = isRefreshing,
                onPlanGoalClick = {
                    sessionToEdit = null
                    showCreateSheet = true
                },
                onRefresh = {
                    if (!isRefreshing) {
                        coroutineScope.launch(Dispatchers.IO) {
                            runCatching { deskViewModel.reload() }
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Weekly Progress Card
            item(key = "weekly_progress_card") {
                WeeklyProgressCard(
                    completedPages = weeklyCompletedPages,
                    goalPages = weeklyGoalPages,
                    plannedPages = weeklyPlannedPages,
                    progressPercent = weeklyProgressPercent,
                    onEditGoalClick = { showGoalSheet = true }
                )
            }

            // 2. TODAY'S FOCUS SECTION (Horizontal row of cards for books scheduled for today)
            item(key = "today_focus_header") {
                val totalTodayCount = activeTodaySessions.size + completedTodaySessions.size
                SectionHeader(
                    title = "TODAY'S FOCUS",
                    badge = if (totalTodayCount > 0) "$totalTodayCount SESSIONS • $todayPagesCount PAGES" else null
                )
            }

            if (activeTodaySessions.isEmpty() && completedTodaySessions.isEmpty()) {
                item(key = "today_empty_state") {
                    TodayEmptyState(onScheduleClick = {
                        sessionToEdit = null
                        showCreateSheet = true
                    })
                }
            } else {
                // Horizontal Carousel of Today's Focus Goal Cards
                item(key = "today_focus_row") {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        contentPadding = PaddingValues(vertical = 4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(activeTodaySessions, key = { "active_${it.id}" }) { session ->
                            TodayFocusSessionCard(
                                session = session,
                                isCompleted = false,
                                onReadClick = {
                                    val fullPath = resolveBookPath(
                                        vaultPath = session.vaultPath,
                                        bookFileName = session.bookFileName,
                                        bookRelativePath = session.bookRelativePath
                                    )
                                    ReaderActivity.launchSession(
                                        context = context,
                                        bookPath = fullPath,
                                        bookTitle = session.bookTitle,
                                        startPage = session.startPage,
                                        sessionId = session.id
                                    )
                                },
                                onToggleComplete = {
                                    deskViewModel.markCompleted(session.id)
                                }
                            )
                        }

                        items(completedTodaySessions, key = { "completed_${it.id}" }) { session ->
                            TodayFocusSessionCard(
                                session = session,
                                isCompleted = true,
                                onReadClick = {
                                    val fullPath = resolveBookPath(
                                        vaultPath = session.vaultPath,
                                        bookFileName = session.bookFileName,
                                        bookRelativePath = session.bookRelativePath
                                    )
                                    ReaderActivity.launchSession(
                                        context = context,
                                        bookPath = fullPath,
                                        bookTitle = session.bookTitle,
                                        startPage = session.startPage,
                                        sessionId = session.id
                                    )
                                },
                                onToggleComplete = {
                                    deskViewModel.toggleCompleted(session.id)
                                }
                            )
                        }
                    }
                }
            }

            // 3. THE SHELF (Downloaded Storage)
            item(key = "shelf_header") {
                SectionHeader(
                    title = "THE SHELF (DOWNLOADED STORAGE)",
                    badge = "${downloadedBooks.size} BOOKS"
                )
            }

            if (downloadedBooks.isEmpty()) {
                item(key = "shelf_empty_state") {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = colors.surfaceCard,
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, colors.borderSubtle),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = FluentIcons.ArrowDownload24Regular,
                                contentDescription = null,
                                tint = colors.primary,
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(10.dp))
                            Text(
                                text = "Your shelf is empty",
                                fontFamily = PoppinsFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp,
                                color = colors.textPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Browse the Library to download textbooks for offline study.",
                                fontFamily = InterFamily,
                                fontSize = 12.sp,
                                color = colors.textSecondary
                            )
                        }
                    }
                }
            } else {
                // Grouped into Subject folders based on catalog metadata
                booksBySubject.forEach { (subject, subjectBooks) ->
                    item(key = "subject_folder_$subject") {
                        val isExpanded = !collapsedFolders.contains(subject)
                        ShelfFolderSection(
                            folderName = subject,
                            books = subjectBooks,
                            isExpanded = isExpanded,
                            onToggleExpand = {
                                collapsedFolders = if (isExpanded) {
                                    collapsedFolders + subject
                                } else {
                                    collapsedFolders - subject
                                }
                            },
                            onBookClick = { book ->
                                val finalPath = book.binFilePath.ifEmpty { book.uriString }
                                    .ifEmpty {
                                        com.example.util.MulberryStorageManager.getBinFileForBook(context, book.slug)
                                            .takeIf { it.exists() }?.absolutePath ?: ""
                                    }
                                ReaderActivity.launch(
                                    context = context,
                                    bookPath = finalPath,
                                    bookTitle = book.title,
                                    bookId = book.id
                                )
                            },
                            onBookLongClick = { book ->
                                selectedShelfBook = book
                            }
                        )
                    }
                }
            }

            // Bottom Spacing
            item(key = "bottom_spacer") {
                Spacer(modifier = Modifier.height(60.dp))
            }
        }
    }

    // SHELF ACTIONS BOTTOM SHEET (Triggered on Long Press)
    selectedShelfBook?.let { book ->
        ModalBottomSheet(
            onDismissRequest = { selectedShelfBook = null },
            sheetState = shelfSheetState,
            containerColor = colors.surfaceCard,
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(top = 12.dp, bottom = 8.dp)
                        .width(36.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(colors.borderSubtle)
                )
            }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Text(
                    text = book.title,
                    fontFamily = PoppinsFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${book.author.ifEmpty { book.edition }} • Page ${book.lastReadPage} of ${book.pageCount}",
                    fontFamily = InterFamily,
                    fontSize = 12.sp,
                    color = colors.textSecondary
                )

                Spacer(modifier = Modifier.height(16.dp))
                HorizontalDivider(color = colors.borderSubtle, thickness = 0.5.dp)
                Spacer(modifier = Modifier.height(8.dp))

                // Action 1: Read Book
                ShelfActionRow(
                    icon = FluentIcons.Book24Regular,
                    label = "Read Book (Resume Page ${book.lastReadPage})",
                    onClick = {
                        selectedShelfBook = null
                        val finalPath = book.binFilePath.ifEmpty { book.uriString }
                            .ifEmpty { File(File(context.filesDir, "vault"), "${book.slug}.bin").takeIf { it.exists() }?.absolutePath ?: "" }
                        ReaderActivity.launch(
                            context = context,
                            bookPath = finalPath,
                            bookTitle = book.title,
                            bookId = book.id
                        )
                    }
                )

                // Action 2: Add to Today's Study Goals
                ShelfActionRow(
                    icon = FluentIcons.CalendarAdd24Regular,
                    label = "Add to Today's Goals",
                    onClick = {
                        selectedShelfBook = null
                        sessionToEdit = null
                        showCreateSheet = true
                    }
                )

                // Action 3: Move to Folder
                ShelfActionRow(
                    icon = FluentIcons.Folder24Regular,
                    label = "Move to Shelf Folder",
                    onClick = {
                        selectedShelfBook = null
                        Toast.makeText(context, "Moved to ${book.prof.ifEmpty { "1st Prof Core" }}", Toast.LENGTH_SHORT).show()
                    }
                )

                // Action 4: View Book Information
                ShelfActionRow(
                    icon = FluentIcons.Info24Regular,
                    label = "View Book Details",
                    onClick = {
                        selectedShelfBook = null
                        Toast.makeText(
                            context,
                            "${book.title}\n${book.author}\nPages: ${book.pageCount} • Size: ${book.sizeMb} MB",
                            Toast.LENGTH_LONG
                        ).show()
                    }
                )

                // Action 5: Delete Download (Triggers confirmation dialog)
                ShelfActionRow(
                    icon = FluentIcons.Delete24Regular,
                    label = "Delete Download",
                    textColor = colors.accentDanger,
                    onClick = {
                        bookPendingDelete = book
                        selectedShelfBook = null
                    }
                )
            }
        }
    }

    // Delete Confirmation Dialog
    bookPendingDelete?.let { book ->
        AlertDialog(
            onDismissRequest = { bookPendingDelete = null },
            title = {
                Text(
                    text = "Delete Download?",
                    fontFamily = PoppinsFamily,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
            },
            text = {
                Text(
                    text = "Are you sure you want to remove \"${book.title}\" from your offline shelf? The encrypted payload file will be deleted from your device.",
                    fontFamily = InterFamily,
                    color = colors.textSecondary,
                    fontSize = 14.sp
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        deskViewModel.deleteDownloadedBook(book)
                        bookPendingDelete = null
                        Toast.makeText(context, "Download removed from shelf", Toast.LENGTH_SHORT).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.accentDanger)
                ) {
                    Text("Delete", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { bookPendingDelete = null }) {
                    Text("Cancel", color = colors.textPrimary)
                }
            },
            containerColor = colors.surfaceCard
        )
    }

    // Schedule Session Creation Modal Bottom Sheet
    if (showCreateSheet) {
        ScheduleSessionSheet(
            sheetState = sheetState,
            vaultBooks = downloadedBooks.ifEmpty { vaultBooks },
            existingSession = sessionToEdit,
            onDismiss = {
                coroutineScope.launch {
                    sheetState.hide()
                    showCreateSheet = false
                    sessionToEdit = null
                }
            },
            onSchedule = { book, startPage, endPage, scheduledEpochMillis, reminderMinutesBefore, microNote ->
                if (sessionToEdit != null) {
                    deskViewModel.editSession(
                        sessionToEdit!!.copy(
                            startPage = startPage,
                            endPage = endPage,
                            scheduledEpochMillis = scheduledEpochMillis,
                            reminderMinutesBefore = reminderMinutesBefore,
                            microNote = microNote
                        )
                    )
                } else {
                    deskViewModel.addSession(
                        book = book,
                        startPage = startPage,
                        endPage = endPage,
                        scheduledEpochMillis = scheduledEpochMillis,
                        reminderMinutesBefore = reminderMinutesBefore,
                        microNote = microNote
                    )
                }

                coroutineScope.launch {
                    sheetState.hide()
                    showCreateSheet = false
                    sessionToEdit = null
                }
            }
        )
    }

    // Weekly Goal Configuration Sheet
    if (showGoalSheet) {
        WeeklyGoalSheet(
            sheetState = goalSheetState,
            currentGoal = weeklyGoalPages,
            onDismiss = {
                coroutineScope.launch {
                    goalSheetState.hide()
                    showGoalSheet = false
                }
            },
            onSaveGoal = { newGoal ->
                deskViewModel.updateWeeklyGoal(newGoal)
                coroutineScope.launch {
                    goalSheetState.hide()
                    showGoalSheet = false
                }
            }
        )
    }
}

@Composable
private fun DeskTopAppBar(
    isRefreshing: Boolean,
    onPlanGoalClick: () -> Unit,
    onRefresh: () -> Unit
) {
    val colors = LocalMulberryColors.current

    val infiniteTransition = rememberInfiniteTransition(label = "RefreshRotation")
    val rotationAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Rotation"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Desk",
            fontFamily = PoppinsFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 28.sp,
            color = colors.textPrimary,
            letterSpacing = (-0.5).sp
        )

        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            // Schedule Action Pill: [ + Plan Goal ]
            Surface(
                shape = RoundedCornerShape(20.dp),
                color = colors.primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(20.dp))
                    .clickable { onPlanGoalClick() }
                    .testTag("btn_desk_plan_goal")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                ) {
                    Icon(
                        imageVector = FluentIcons.CalendarAdd24Regular,
                        contentDescription = "Plan Goal",
                        tint = colors.onPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "+ Plan Goal",
                        fontFamily = PoppinsFamily,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.onPrimary
                    )
                }
            }

            IconButton(
                onClick = onRefresh,
                enabled = !isRefreshing,
                modifier = Modifier
                    .size(38.dp)
                    .testTag("btn_refresh_desk")
            ) {
                Icon(
                    imageVector = FluentIcons.ArrowSync24Regular,
                    contentDescription = "Refresh Schedule",
                    tint = if (isRefreshing) colors.primary else colors.textPrimary,
                    modifier = Modifier
                        .size(20.dp)
                        .then(if (isRefreshing) Modifier.rotate(rotationAngle) else Modifier)
                )
            }
        }
    }
}

@Composable
private fun TodayFocusSessionCard(
    session: ReadingSession,
    isCompleted: Boolean,
    onReadClick: () -> Unit,
    onToggleComplete: () -> Unit
) {
    val colors = LocalMulberryColors.current

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = colors.surfaceCard,
        border = androidx.compose.foundation.BorderStroke(0.5.dp, colors.borderSubtle),
        modifier = Modifier
            .width(280.dp)
            .clip(RoundedCornerShape(20.dp))
            .clickable { onReadClick() }
            .testTag("today_focus_card_${session.id}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.primary.copy(alpha = 0.12f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = FluentIcons.Book24Regular,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = session.bookTitle,
                        fontFamily = PoppinsFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "Pages ${session.startPage}–${session.endPage}",
                        fontFamily = InterFamily,
                        fontSize = 12.sp,
                        color = colors.textSecondary
                    )
                }

                // Checkmark toggle
                IconButton(
                    onClick = onToggleComplete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (isCompleted) colors.primary else colors.surfaceTint,
                        modifier = Modifier.size(26.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (isCompleted) {
                                Icon(
                                    imageVector = FluentIcons.Checkmark24Regular,
                                    contentDescription = "Completed",
                                    tint = colors.onPrimary,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Page progress bar
            val readProgress = if (isCompleted) 1f else 0.35f
            LinearProgressIndicator(
                progress = { readProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(RoundedCornerShape(3.dp)),
                color = colors.primary,
                trackColor = colors.surfaceTint
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = if (isCompleted) "Completed" else "In Progress",
                    fontFamily = InterFamily,
                    fontSize = 11.sp,
                    color = if (isCompleted) colors.primary else colors.textSecondary,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "Tap to read",
                    fontFamily = InterFamily,
                    fontSize = 11.sp,
                    color = colors.textSecondary
                )
            }
        }
    }
}

@Composable
private fun ShelfFolderSection(
    folderName: String,
    books: List<BookEntity>,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onBookClick: (BookEntity) -> Unit,
    onBookLongClick: (BookEntity) -> Unit
) {
    val colors = LocalMulberryColors.current

    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Collapsible Folder Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable { onToggleExpand() }
                .padding(vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (isExpanded) FluentIcons.ChevronDown24Regular else FluentIcons.ChevronRight24Regular,
                contentDescription = if (isExpanded) "Collapse $folderName" else "Expand $folderName",
                tint = colors.primary,
                modifier = Modifier.size(18.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = FluentIcons.Folder24Regular,
                contentDescription = null,
                tint = colors.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "$folderName (${books.size} ${if (books.size == 1) "Book" else "Books"})",
                fontFamily = PoppinsFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = colors.textPrimary
            )
        }

        AnimatedVisibility(visible = isExpanded) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                books.forEach { book ->
                    ShelfBookCard(
                        book = book,
                        onClick = { onBookClick(book) },
                        onLongClick = { onBookLongClick(book) }
                    )
                }
            }
        }
    }
}

@Composable
private fun ShelfBookCard(
    book: BookEntity,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val colors = LocalMulberryColors.current

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = colors.surfaceCard,
        border = androidx.compose.foundation.BorderStroke(0.5.dp, colors.borderSubtle),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .testTag("shelf_book_${book.slug}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Cover Image Preview
            Box(
                modifier = Modifier
                    .width(52.dp)
                    .height(72.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.surfaceTint),
                contentAlignment = Alignment.Center
            ) {
                if (book.coverUrl.isNotBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(book.coverUrl)
                            .crossfade(true)
                            .build(),
                        contentDescription = book.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        imageVector = FluentIcons.Book24Regular,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = book.title,
                    fontFamily = PoppinsFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                val percent = if (book.pageCount > 0) {
                    ((book.lastReadPage.toFloat() / book.pageCount.toFloat()) * 100f).toInt().coerceIn(0, 100)
                } else 0

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Page ${book.lastReadPage} of ${book.pageCount}",
                        fontFamily = InterFamily,
                        fontSize = 12.sp,
                        color = colors.textSecondary
                    )
                    Text(
                        text = "[$percent%]",
                        fontFamily = InterFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = colors.primary
                    )
                }

                Spacer(modifier = Modifier.height(6.dp))

                // Progress Bar (No delete 'X' button on card face!)
                LinearProgressIndicator(
                    progress = { (percent / 100f).coerceIn(0f, 1f) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(5.dp)
                        .clip(RoundedCornerShape(2.5.dp)),
                    color = colors.primary,
                    trackColor = colors.surfaceTint
                )
            }
        }
    }
}

@Composable
private fun ShelfActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    textColor: Color? = null,
    onClick: () -> Unit
) {
    val colors = LocalMulberryColors.current
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = textColor ?: colors.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = label,
                fontFamily = InterFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
                color = textColor ?: colors.textPrimary
            )
        }
    }
}

@Composable
private fun WeeklyProgressCard(
    completedPages: Int,
    goalPages: Int,
    plannedPages: Int,
    progressPercent: Int,
    onEditGoalClick: () -> Unit
) {
    val colors = LocalMulberryColors.current
    val animatedProgress by animateFloatAsState(
        targetValue = (progressPercent / 100f).coerceIn(0f, 1f),
        animationSpec = tween(durationMillis = 600),
        label = "WeeklyProgress"
    )

    ElevatedCard(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = colors.surfaceCard),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("weekly_progress_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = FluentIcons.DataTrending24Regular,
                        contentDescription = "Weekly Goal",
                        tint = colors.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Weekly Goal",
                        fontFamily = PoppinsFamily,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    IconButton(
                        onClick = onEditGoalClick,
                        modifier = Modifier.size(26.dp)
                    ) {
                        Icon(
                            imageVector = FluentIcons.Edit24Regular,
                            contentDescription = "Configure Target",
                            tint = colors.primary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = colors.primary.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = "$progressPercent%",
                        fontSize = 13.sp,
                        fontFamily = InterFamily,
                        fontWeight = FontWeight.Bold,
                        color = colors.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "$completedPages / $goalPages pages read",
                fontFamily = InterFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = colors.textPrimary
            )

            Text(
                text = "Scheduled for this week: $plannedPages pages",
                fontFamily = InterFamily,
                fontSize = 12.sp,
                color = colors.textSecondary
            )

            Spacer(modifier = Modifier.height(10.dp))

            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = colors.primary,
                trackColor = colors.surfaceTint
            )
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    badge: String?
) {
    val colors = LocalMulberryColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 4.dp, bottom = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = title,
            fontFamily = PoppinsFamily,
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = colors.textSecondary,
            letterSpacing = 1.sp
        )
        if (badge != null) {
            Text(
                text = badge,
                fontFamily = InterFamily,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = colors.textSecondary.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun TodayEmptyState(onScheduleClick: () -> Unit) {
    val colors = LocalMulberryColors.current
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = colors.surfaceCard.copy(alpha = 0.7f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .background(colors.surfaceTint),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = FluentIcons.CalendarLtr24Regular,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(22.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = "No study goals scheduled for today",
                fontFamily = PoppinsFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = colors.textPrimary
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Tap [+ Plan Goal] to set your daily reading pace.",
                fontFamily = InterFamily,
                fontSize = 12.sp,
                color = colors.textSecondary
            )
        }
    }
}

private fun resolveBookPath(vaultPath: String, bookFileName: String, bookRelativePath: String = ""): String {
    if (bookRelativePath.isNotBlank()) {
        val relFile = File(vaultPath, bookRelativePath)
        if (relFile.exists()) return relFile.absolutePath
    }
    if (bookFileName.startsWith("/")) {
        val directFile = File(bookFileName)
        if (directFile.exists()) return directFile.absolutePath
    }
    val rootFile = File(vaultPath, bookFileName)
    if (rootFile.exists()) return rootFile.absolutePath

    if (vaultPath.isNotBlank()) {
        val found = runCatching {
            File(vaultPath).walkTopDown().firstOrNull {
                it.isFile && (it.name.equals(bookFileName, ignoreCase = true) ||
                        (bookRelativePath.isNotBlank() && it.name.equals(File(bookRelativePath).name, ignoreCase = true)))
            }
        }.getOrNull()
        if (found != null) return found.absolutePath
    }
    return File(vaultPath, bookFileName).absolutePath
}
