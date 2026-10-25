@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.example.ui.screens.desk

import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import android.content.res.Configuration
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.data.model.UserProfile
import com.example.ui.screens.OFFICIAL_CATALOG_TYPES
import com.example.ui.screens.matchesCatalogType
import com.example.ui.screens.getOrderedCatalogTypes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.platform.LocalConfiguration
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
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
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
import androidx.compose.ui.graphics.Brush
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
import com.example.ui.components.ContributionLiveStatusCard
import com.example.ui.components.FluentIcons
import com.example.ui.components.fluidGlassPressEffect
import com.example.ui.reader.ReaderActivity
import com.example.ui.theme.InterFamily
import com.example.ui.theme.LocalMulberryColors
import com.example.ui.theme.PoppinsFamily
import com.example.util.slugify
import com.example.viewmodel.DeskScheduleViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

@Composable
fun DeskRootScreen(
    modifier: Modifier = Modifier,
    deskViewModel: DeskScheduleViewModel = viewModel(),
    onDeskAtHomeChanged: (Boolean) -> Unit = {},
    onNavigateToLibrary: () -> Unit = {}
) {
    val context = LocalContext.current
    val colors = LocalMulberryColors.current
    val coroutineScope = rememberCoroutineScope()

    val downloadedBooks by deskViewModel.downloadedBooks.collectAsState()
    val allCatalogBooks by deskViewModel.allCatalogBooks.collectAsState()
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

    val userProfileRepo = remember { com.example.data.repository.UserProfileRepository.getInstance(context) }
    val userProfile by userProfileRepo.userProfile.collectAsState()
    val uploadState by com.example.util.BookContributionManager.uploadState.collectAsState()
    var showProfileModal by remember { mutableStateOf(false) }

    // Shelf long-press actions bottom sheet state
    var selectedShelfBook by remember { mutableStateOf<BookEntity?>(null) }
    var bookPendingDelete by remember { mutableStateOf<BookEntity?>(null) }
    val shelfSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Collapsible folders on The Shelf (collapsed set, default empty so all subjects are expanded)
    var collapsedFolders by remember { mutableStateOf(setOf<String>()) }

    // Subject filter chip state for Shelf
    var selectedSubject by remember { mutableStateOf("All") }

    // Dynamic list of subjects from downloaded books
    val availableSubjects = remember(downloadedBooks) {
        val list = mutableListOf("All")
        val uniqueSubjects = downloadedBooks.map { it.subject.trim() }
            .filter { it.isNotBlank() && !it.equals("General", ignoreCase = true) }
            .distinct()
            .sorted()
        list.addAll(uniqueSubjects)
        list
    }

    // Filter downloaded books by selected subject chip
    val filteredDownloadedBooks = remember(downloadedBooks, selectedSubject) {
        if (selectedSubject == "All") {
            downloadedBooks
        } else {
            downloadedBooks.filter { book ->
                book.subject.equals(selectedSubject, ignoreCase = true) ||
                        book.tagsCsv.contains(selectedSubject, ignoreCase = true)
            }
        }
    }

    // Group downloaded books dynamically with Standard Textbooks and Notes pinned at top, then A-Z
    val dynamicDeskCategories = remember(filteredDownloadedBooks) {
        getOrderedCatalogTypes(filteredDownloadedBooks)
    }

    val catalogCategories = remember(filteredDownloadedBooks, dynamicDeskCategories) {
        dynamicDeskCategories.mapNotNull { type ->
            val matching = filteredDownloadedBooks.filter { matchesCatalogType(it, type) }
            if (matching.isNotEmpty()) type to matching else null
        }
    }

    // Catch any downloaded books not categorized into the standard 5 types
    val uncategorizedBooks = remember(filteredDownloadedBooks, catalogCategories) {
        val categorizedIds = catalogCategories.flatMap { it.second }.map { it.id }.toSet()
        filteredDownloadedBooks.filter { it.id !in categorizedIds }
    }

    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val isWideScreen = configuration.screenWidthDp >= 600 || isLandscape

    val onOpenBook: (BookEntity) -> Unit = { book ->
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
    }

    val onOpenSession: (ReadingSession) -> Unit = { session ->
        val binFile = if (session.bookSlug.isNotBlank()) {
            com.example.util.MulberryStorageManager.getBinFileForBook(context, session.bookSlug)
        } else null
        val fullPath = if (binFile != null && binFile.exists()) {
            binFile.absolutePath
        } else {
            resolveBookPath(
                vaultPath = com.example.util.MulberryStorageManager.getMulberryDirectory(context).absolutePath,
                bookFileName = session.bookFileName,
                bookRelativePath = session.bookRelativePath
            )
        }
        if (fullPath.isEmpty() || (!fullPath.startsWith("content://") && !File(fullPath).exists())) {
            Toast.makeText(context, "Textbook '${session.bookTitle}' is not downloaded yet. Please download it from Library.", Toast.LENGTH_SHORT).show()
        } else {
            ReaderActivity.launchSession(
                context = context,
                bookPath = fullPath,
                bookTitle = session.bookTitle,
                startPage = session.startPage,
                sessionId = session.id,
                bookSlug = session.bookSlug,
                endPage = session.endPage
            )
        }
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(colors.backgroundGradient),
        containerColor = Color.Transparent,
        topBar = {
            // CLEAN TOP BAR: Single clean title "Desk" + Profile Avatar
            DeskTopAppBar(
                userProfile = userProfile,
                onProfileClick = {
                    showProfileModal = true
                }
            )
        }
    ) { innerPadding ->
        PullToRefreshBox(
            isRefreshing = isRefreshing,
            onRefresh = {
                if (!isRefreshing) {
                    coroutineScope.launch(Dispatchers.IO) {
                        runCatching { deskViewModel.reload() }
                    }
                }
            },
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (isWideScreen) {
                // RESPONSIVE DUAL-PANE LAYOUT: Left pane for goals & schedule, Right pane for shelves
                Row(
                    modifier = Modifier.fillMaxSize()
                ) {
                    // LEFT PANE (Goals & Today's Schedule Sessions)
                    Column(
                        modifier = Modifier
                            .width(360.dp)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState())
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        if (uploadState !is com.example.util.ContributionUploadState.Idle) {
                            ContributionLiveStatusCard(
                                state = uploadState,
                                onCancel = { slug -> com.example.util.BookContributionManager.cancelContribution(context, slug) },
                                onDismiss = { com.example.util.BookContributionManager.dismissUploadState() }
                            )
                        }

                        WeeklyProgressCard(
                            completedPages = weeklyCompletedPages,
                            goalPages = weeklyGoalPages,
                            plannedPages = weeklyPlannedPages,
                            progressPercent = weeklyProgressPercent,
                            onEditGoalClick = { showGoalSheet = true }
                        )

                        val totalTodayCount = activeTodaySessions.size + completedTodaySessions.size
                        SectionHeader(
                            title = "Today's Focus",
                            badge = if (totalTodayCount > 0) "$totalTodayCount SESSIONS • $todayPagesCount PAGES" else null,
                            icon = FluentIcons.CalendarLtr24Regular
                        )

                        if (activeTodaySessions.isEmpty() && completedTodaySessions.isEmpty()) {
                            PlanSessionActionCard(
                                onClick = {
                                    sessionToEdit = null
                                    showCreateSheet = true
                                },
                                modifier = Modifier.fillMaxWidth()
                            )
                        } else {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                activeTodaySessions.forEach { session ->
                                    TodayFocusSessionCard(
                                        session = session,
                                        isCompleted = false,
                                        onReadClick = { onOpenSession(session) },
                                        onToggleComplete = { deskViewModel.markCompleted(session.id) },
                                        onLongClick = {
                                            sessionToEdit = session
                                            showCreateSheet = true
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                completedTodaySessions.forEach { session ->
                                    TodayFocusSessionCard(
                                        session = session,
                                        isCompleted = true,
                                        onReadClick = { onOpenSession(session) },
                                        onToggleComplete = { deskViewModel.toggleCompleted(session.id) },
                                        onLongClick = {
                                            sessionToEdit = session
                                            showCreateSheet = true
                                        },
                                        modifier = Modifier.fillMaxWidth()
                                    )
                                }
                                PlanSessionActionCard(
                                    onClick = {
                                        sessionToEdit = null
                                        showCreateSheet = true
                                    },
                                    modifier = Modifier.fillMaxWidth()
                                )
                            }
                        }
                    }

                // Vertical Hairline Divider
                Box(
                    modifier = Modifier
                        .width(0.5.dp)
                        .fillMaxHeight()
                        .background(colors.borderSubtle)
                )

                // RIGHT PANE (The Shelf - Downloaded Storage with embedded progress covers)
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight(),
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    item(key = "shelf_header_wide") {
                        SectionHeader(
                            title = "Shelf",
                            badge = "${filteredDownloadedBooks.size} BOOKS",
                            icon = FluentIcons.LibrarySpines24Regular
                        )
                    }

                    // Horizontal Subject Filter Chips Row
                    if (downloadedBooks.isNotEmpty() && availableSubjects.size > 2) {
                        item(key = "shelf_subject_chips_wide") {
                            LazyRow(
                                contentPadding = PaddingValues(vertical = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(
                                    items = availableSubjects,
                                    key = { subject -> "desk_chip_wide_$subject" }
                                ) { subject ->
                                    val isSelected = selectedSubject.equals(subject, ignoreCase = true)
                                    DeskSubjectFilterChip(
                                        label = subject,
                                        isSelected = isSelected,
                                        testTag = "desk_subject_chip_wide_$subject",
                                        onClick = {
                                            selectedSubject = if (isSelected && subject != "All") "All" else subject
                                        }
                                    )
                                }
                            }
                        }
                    }

                    if (downloadedBooks.isEmpty()) {
                        item(key = "shelf_empty_wide") {
                            ShelfEmptyStateCard()
                        }
                    } else if (catalogCategories.isEmpty() && uncategorizedBooks.isEmpty()) {
                        item(key = "shelf_empty_filter_wide") {
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = colors.surfaceCard,
                                border = androidx.compose.foundation.BorderStroke(1.dp, colors.borderSubtle),
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 8.dp)
                            ) {
                                Column(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(20.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = "No books downloaded for \"$selectedSubject\"",
                                        fontFamily = PoppinsFamily,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp,
                                        color = colors.textPrimary
                                    )
                                    TextButton(onClick = { selectedSubject = "All" }) {
                                        Text("Show All Books", color = colors.primary, fontFamily = InterFamily)
                                    }
                                }
                            }
                        }
                    } else {
                        val displayCategories = catalogCategories + if (uncategorizedBooks.isNotEmpty()) {
                            listOf("Other Resources" to uncategorizedBooks)
                        } else emptyList()

                        displayCategories.forEach { (catName, catBooks) ->
                            item(key = "desk_cat_wide_$catName") {
                                val isExpanded = !collapsedFolders.contains(catName)
                                DeskShelfCategorySection(
                                    folderName = catName,
                                    books = catBooks,
                                    isExpanded = isExpanded,
                                    onToggleExpand = {
                                        collapsedFolders = if (isExpanded) {
                                            collapsedFolders + catName
                                        } else {
                                            collapsedFolders - catName
                                        }
                                    },
                                    onBookClick = onOpenBook,
                                    onBookLongClick = { selectedShelfBook = it }
                                )
                            }
                        }
                    }

                    item(key = "wide_bottom_space") {
                        Spacer(modifier = Modifier.height(48.dp))
                    }
                }
            }
        } else {
            // PORTRAIT SINGLE-COLUMN FLUID LAYOUT
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                if (uploadState !is com.example.util.ContributionUploadState.Idle) {
                    item(key = "contribution_live_status_card") {
                        ContributionLiveStatusCard(
                            state = uploadState,
                            onCancel = { slug -> com.example.util.BookContributionManager.cancelContribution(context, slug) },
                            onDismiss = { com.example.util.BookContributionManager.dismissUploadState() }
                        )
                    }
                }

                // 1. Shortened Weekly Progress Card
                item(key = "weekly_progress_card") {
                    WeeklyProgressCard(
                        completedPages = weeklyCompletedPages,
                        goalPages = weeklyGoalPages,
                        plannedPages = weeklyPlannedPages,
                        progressPercent = weeklyProgressPercent,
                        onEditGoalClick = { showGoalSheet = true }
                    )
                }

                // 2. TODAY'S FOCUS SECTION
                item(key = "today_focus_header") {
                    val totalTodayCount = activeTodaySessions.size + completedTodaySessions.size
                    SectionHeader(
                        title = "Today's Focus",
                        badge = if (totalTodayCount > 0) "$totalTodayCount SESSIONS • $todayPagesCount PAGES" else null,
                        icon = FluentIcons.CalendarLtr24Regular
                    )
                }

                if (activeTodaySessions.isEmpty() && completedTodaySessions.isEmpty()) {
                    item(key = "today_empty_plan_card") {
                        PlanSessionActionCard(
                            onClick = {
                                sessionToEdit = null
                                showCreateSheet = true
                            }
                        )
                    }
                } else {
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
                                    onReadClick = { onOpenSession(session) },
                                    onToggleComplete = { deskViewModel.markCompleted(session.id) },
                                    onLongClick = {
                                        sessionToEdit = session
                                        showCreateSheet = true
                                    }
                                )
                            }

                            items(completedTodaySessions, key = { "completed_${it.id}" }) { session ->
                                TodayFocusSessionCard(
                                    session = session,
                                    isCompleted = true,
                                    onReadClick = { onOpenSession(session) },
                                    onToggleComplete = { deskViewModel.toggleCompleted(session.id) },
                                    onLongClick = {
                                        sessionToEdit = session
                                        showCreateSheet = true
                                    }
                                )
                            }

                            item(key = "plan_session_card") {
                                PlanSessionActionCard(
                                    onClick = {
                                        sessionToEdit = null
                                        showCreateSheet = true
                                    }
                                )
                            }
                        }
                    }
                }

                // 3. THE SHELF (Downloaded Storage)
                item(key = "shelf_header") {
                    SectionHeader(
                        title = "Shelf",
                        badge = "${filteredDownloadedBooks.size} BOOKS",
                        icon = FluentIcons.LibrarySpines24Regular
                    )
                }

                // Horizontal Subject Filter Chips Row
                if (downloadedBooks.isNotEmpty() && availableSubjects.size > 2) {
                    item(key = "shelf_subject_chips") {
                        LazyRow(
                            contentPadding = PaddingValues(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            items(
                                items = availableSubjects,
                                key = { subject -> "desk_chip_$subject" }
                            ) { subject ->
                                val isSelected = selectedSubject.equals(subject, ignoreCase = true)
                                DeskSubjectFilterChip(
                                    label = subject,
                                    isSelected = isSelected,
                                    testTag = "desk_subject_chip_$subject",
                                    onClick = {
                                        selectedSubject = if (isSelected && subject != "All") "All" else subject
                                    }
                                )
                            }
                        }
                    }
                }

                if (downloadedBooks.isEmpty()) {
                    item(key = "shelf_empty_state") {
                        ShelfEmptyStateCard()
                    }
                } else if (catalogCategories.isEmpty() && uncategorizedBooks.isEmpty()) {
                    item(key = "shelf_empty_filter") {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = colors.surfaceCard,
                            border = androidx.compose.foundation.BorderStroke(1.dp, colors.borderSubtle),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 8.dp)
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "No books downloaded for \"$selectedSubject\"",
                                    fontFamily = PoppinsFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    color = colors.textPrimary
                                )
                                TextButton(onClick = { selectedSubject = "All" }) {
                                    Text("Show All Books", color = colors.primary, fontFamily = InterFamily)
                                }
                            }
                        }
                    }
                } else {
                    val displayCategories = catalogCategories + if (uncategorizedBooks.isNotEmpty()) {
                        listOf("Other Resources" to uncategorizedBooks)
                    } else emptyList()

                    displayCategories.forEach { (catName, catBooks) ->
                        item(key = "subject_shelf_$catName") {
                            val isExpanded = !collapsedFolders.contains(catName)
                            DeskShelfCategorySection(
                                folderName = catName,
                                books = catBooks,
                                isExpanded = isExpanded,
                                onToggleExpand = {
                                    collapsedFolders = if (isExpanded) {
                                        collapsedFolders + catName
                                    } else {
                                        collapsedFolders - catName
                                    }
                                },
                                onBookClick = onOpenBook,
                                onBookLongClick = { selectedShelfBook = it }
                            )
                        }
                    }
                }

                item(key = "bottom_spacer") {
                    Spacer(modifier = Modifier.height(100.dp))
                }
            }
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

                // Action 5: Community Library Contribution (for local imported external books)
                val externalIndex = remember(book, uploadState) {
                    com.example.util.MulberryStorageManager.loadDownloadedExternal(context)
                }
                val externalMeta = remember(book, externalIndex) {
                    externalIndex.books.firstOrNull {
                        it.id == book.slug || it.localPath == book.binFilePath || it.title.equals(book.title, ignoreCase = true)
                    }
                }
                val isExternalBook = book.author == "Local Import" || externalMeta != null
                val contributedStatus = externalMeta?.contributedStatus ?: if (isExternalBook) "NONE" else ""

                if (isExternalBook) {
                    if (contributedStatus == "PENDING" || contributedStatus == "SUBMITTED") {
                        ShelfActionRow(
                            icon = FluentIcons.Checkmark24Regular,
                            label = "Submitted for Review",
                            textColor = colors.accentSuccess,
                            onClick = {
                                selectedShelfBook = null
                                Toast.makeText(
                                    context,
                                    "This book has been submitted for community review.",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        )
                    } else {
                        ShelfActionRow(
                            icon = FluentIcons.ArrowUpload24Regular,
                            label = "Contribute to Community Library",
                            textColor = colors.primary,
                            onClick = {
                                selectedShelfBook = null
                                val slugToUse = externalMeta?.id ?: book.slug.ifEmpty { book.title.slugify() }
                                com.example.util.BookContributionManager.enqueueContribution(
                                    context = context,
                                    slug = slugToUse,
                                    title = book.title,
                                    author = book.author.ifEmpty { "Local Import" },
                                    prof = book.prof.ifEmpty { userProfile.selectedProf.ifEmpty { "1st Prof" } },
                                    subject = book.subject.ifEmpty { "General" },
                                    type = book.type.ifEmpty { "Standard Textbook" },
                                    totalPages = book.pageCount,
                                    sizeMb = book.sizeMb
                                )
                                Toast.makeText(
                                    context,
                                    "Enqueued '${book.title}' for library upload",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        )
                    }
                }

                // Action 6: Delete Download (Triggers confirmation dialog)
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
            vaultBooks = downloadedBooks.ifEmpty { allCatalogBooks },
            existingSession = sessionToEdit,
            onDismiss = {
                coroutineScope.launch {
                    sheetState.hide()
                    showCreateSheet = false
                    sessionToEdit = null
                }
            },
            onDelete = { sessionId ->
                deskViewModel.deleteSession(sessionId)
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

    if (showProfileModal) {
        com.example.ui.screens.UserProfileBottomSheet(
            userProfile = userProfile,
            onDismissRequest = { showProfileModal = false },
            onSaveProfile = { updated ->
                coroutineScope.launch {
                    userProfileRepo.updateProfile(updated)
                }
            }
        )
    }
}

@Composable
private fun DeskTopAppBar(
    userProfile: UserProfile,
    onProfileClick: () -> Unit
) {
    com.example.ui.components.MulberryUnifiedHeader(
        title = "Desk",
        userProfile = userProfile,
        onProfileClick = onProfileClick,
        modifier = Modifier.statusBarsPadding(),
        showGlassBackground = true,
        avatarTestTag = "desk_profile_avatar_button"
    )
}

@Composable
private fun DeskSubjectFilterChip(
    label: String,
    isSelected: Boolean,
    testTag: String,
    onClick: () -> Unit
) {
    val colors = LocalMulberryColors.current

    val topBgColor by animateColorAsState(
        targetValue = if (isSelected) {
            colors.primary.copy(alpha = 0.96f)
        } else {
            if (colors.isDark) colors.surfaceCard.copy(alpha = 0.85f)
            else colors.surfaceTint.copy(alpha = 0.72f)
        },
        animationSpec = tween(durationMillis = 200),
        label = "desk_chip_top_bg"
    )

    val bottomBgColor by animateColorAsState(
        targetValue = if (isSelected) {
            colors.primary.copy(alpha = 0.88f)
        } else {
            if (colors.isDark) colors.surface.copy(alpha = 0.78f)
            else colors.surfaceCard.copy(alpha = 0.92f)
        },
        animationSpec = tween(durationMillis = 200),
        label = "desk_chip_bottom_bg"
    )

    val topBorderColor by animateColorAsState(
        targetValue = if (isSelected) {
            Color.White.copy(alpha = if (colors.isDark) 0.45f else 0.72f)
        } else {
            if (colors.isDark) Color.White.copy(alpha = 0.22f)
            else colors.primary.copy(alpha = 0.30f)
        },
        animationSpec = tween(durationMillis = 200),
        label = "desk_chip_top_border"
    )

    val bottomBorderColor by animateColorAsState(
        targetValue = if (isSelected) {
            colors.primary
        } else {
            colors.borderSubtle.copy(alpha = if (colors.isDark) 0.70f else 0.95f)
        },
        animationSpec = tween(durationMillis = 200),
        label = "desk_chip_bottom_border"
    )

    val textColor by animateColorAsState(
        targetValue = if (isSelected) Color.White else colors.textPrimary.copy(alpha = 0.85f),
        animationSpec = tween(durationMillis = 200),
        label = "desk_chip_text"
    )

    Box(
        modifier = Modifier
            .testTag(testTag)
            .fluidGlassPressEffect(onClick = onClick)
            .clip(CircleShape)
            .background(
                brush = Brush.verticalGradient(listOf(topBgColor, bottomBgColor)),
                shape = CircleShape
            )
            .border(
                width = if (isSelected) 1.25.dp else 1.dp,
                brush = Brush.verticalGradient(listOf(topBorderColor, bottomBorderColor)),
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = label,
            fontFamily = InterFamily,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            fontSize = 12.5.sp,
            color = textColor,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
        )
    }
}

private fun getInitials(name: String): String {
    val trimmed = name.trim()
    if (trimmed.isEmpty()) return "M"
    val parts = trimmed.split("\\s+".toRegex()).filter { it.isNotEmpty() }
    return if (parts.size >= 2) {
        "${parts[0].first().uppercaseChar()}${parts[1].first().uppercaseChar()}"
    } else {
        trimmed.take(2).uppercase()
    }
}

@Composable
private fun TodayFocusSessionCard(
    session: ReadingSession,
    isCompleted: Boolean,
    onReadClick: () -> Unit,
    onToggleComplete: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier.width(280.dp)
) {
    val colors = LocalMulberryColors.current

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = colors.surfaceCard,
        border = androidx.compose.foundation.BorderStroke(0.5.dp, colors.borderSubtle),
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .combinedClickable(
                onClick = onReadClick,
                onLongClick = onLongClick
            )
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
                        fontWeight = FontWeight.Bold,
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
            val totalPages = session.totalPages.coerceAtLeast(1)
            val readPages = session.actualPagesRead.coerceIn(0, totalPages)
            val readProgress = if (isCompleted) 1f else (readPages.toFloat() / totalPages.toFloat()).coerceIn(0f, 1f)
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
                val progressLabel = when {
                    isCompleted -> "Completed ($totalPages/$totalPages pages)"
                    readPages > 0 -> "$readPages/$totalPages pages"
                    else -> "0/$totalPages pages"
                }
                Text(
                    text = progressLabel,
                    fontFamily = InterFamily,
                    fontSize = 11.sp,
                    color = if (isCompleted) colors.primary else colors.textSecondary,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = if (isCompleted) "Review" else "Tap to read",
                    fontFamily = InterFamily,
                    fontSize = 11.sp,
                    color = colors.textSecondary
                )
            }
        }
    }
}

@Composable
private fun PlanSessionActionCard(
    onClick: () -> Unit,
    modifier: Modifier = Modifier.width(280.dp)
) {
    val colors = LocalMulberryColors.current

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = colors.surfaceCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.primary.copy(alpha = 0.35f)),
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .testTag("plan_session_action_card")
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(colors.primary.copy(alpha = 0.14f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = FluentIcons.CalendarAdd24Regular,
                        contentDescription = "Plan Session",
                        tint = colors.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }

                Spacer(modifier = Modifier.width(10.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "Plan a Session",
                        fontFamily = PoppinsFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = colors.textPrimary,
                        maxLines = 1
                    )
                    Text(
                        text = "Set target pages & reminder",
                        fontFamily = InterFamily,
                        fontSize = 12.sp,
                        color = colors.textSecondary,
                        maxLines = 1
                    )
                }
            }

            Surface(
                shape = RoundedCornerShape(12.dp),
                color = colors.primary.copy(alpha = 0.1f),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = FluentIcons.Add24Regular,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Add Schedule",
                        fontFamily = PoppinsFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = colors.primary
                    )
                }
            }
        }
    }
}

@Composable
private fun DeskShelfCategorySection(
    folderName: String,
    books: List<BookEntity>,
    isExpanded: Boolean,
    onToggleExpand: () -> Unit,
    onBookClick: (BookEntity) -> Unit,
    onBookLongClick: (BookEntity) -> Unit
) {
    val colors = LocalMulberryColors.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        // Library-style Section Header with Title, Count Pill Badge, and Expand/Collapse Icon
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .clickable { onToggleExpand() }
                .padding(horizontal = 4.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = folderName,
                    fontFamily = PoppinsFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = colors.textPrimary,
                    letterSpacing = (-0.3).sp
                )

                Surface(
                    shape = CircleShape,
                    color = colors.surfaceCard,
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, colors.borderSubtle),
                    modifier = Modifier.size(22.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = "${books.size}",
                            fontFamily = InterFamily,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textSecondary
                        )
                    }
                }
            }

            IconButton(
                onClick = onToggleExpand,
                modifier = Modifier.size(34.dp)
            ) {
                Icon(
                    imageVector = if (isExpanded) FluentIcons.ChevronDown24Regular else FluentIcons.ChevronRight24Regular,
                    contentDescription = if (isExpanded) "Collapse $folderName" else "Expand $folderName",
                    tint = colors.textSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        AnimatedVisibility(visible = isExpanded) {
            // Horizontal Carousel of High-Fidelity Shelf Book Cards
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                contentPadding = PaddingValues(horizontal = 4.dp, vertical = 6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(books, key = { it.slug.ifEmpty { "book_${it.id}" } }) { book ->
                    DeskShelfBookCard(
                        book = book,
                        onClick = { onBookClick(book) },
                        onLongClick = { onBookLongClick(book) }
                    )
                }
            }
        }
    }
}

/**
 * High-fidelity Desk Shelf Book Card matching Library page aesthetics,
 * with an embedded reading progress bar across the bottom edge of the cover art.
 */
@Composable
private fun DeskShelfBookCard(
    book: BookEntity,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    modifier: Modifier = Modifier.width(136.dp)
) {
    val colors = LocalMulberryColors.current
    val context = LocalContext.current

    val editionLabel = remember(book.edition, book.tagsCsv) {
        when {
            book.edition.isNotBlank() && !book.edition.equals("General", ignoreCase = true) -> {
                if (book.edition.contains("Edition", ignoreCase = true)) {
                    book.edition.replace("Edition", "Ed").trim()
                } else {
                    book.edition
                }
            }
            book.tagsCsv.contains("8th", ignoreCase = true) -> "8th Ed"
            book.tagsCsv.contains("14th", ignoreCase = true) -> "14th Ed"
            book.tagsCsv.contains("10th", ignoreCase = true) -> "10th Ed"
            book.tagsCsv.contains("9th", ignoreCase = true) -> "9th Ed"
            else -> null
        }
    }

    val totalPages = book.pageCount.coerceAtLeast(1)
    val readPages = book.lastReadPage.coerceIn(0, totalPages)
    val percent = ((readPages.toFloat() / totalPages.toFloat()) * 100f).toInt().coerceIn(0, 100)
    val progressFraction = (percent / 100f).coerceIn(0f, 1f)

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "desk_book_press_scale"
    )

    Column(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(14.dp))
            .combinedClickable(
                interactionSource = interactionSource,
                indication = null,
                onClick = onClick,
                onLongClick = onLongClick
            )
            .testTag("desk_shelf_book_${book.slug.ifEmpty { book.id.toString() }}")
    ) {
        // Book Cover Art with Badges & Embedded Progress Line
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(3f / 4.1f)
                .clip(RoundedCornerShape(14.dp))
                .background(colors.surfaceCard)
                .border(0.5.dp, colors.borderSubtle, RoundedCornerShape(14.dp))
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(book.coverUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = "Cover of ${book.title}",
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )

            // Top-Right Edition Pill Badge
            if (!editionLabel.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.Black.copy(alpha = 0.72f),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                ) {
                    Text(
                        text = editionLabel,
                        fontFamily = InterFamily,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // Bottom-Right Reading Progress Pill Badge
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = Color.Black.copy(alpha = 0.78f),
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .padding(bottom = 9.dp, end = 6.dp)
            ) {
                Text(
                    text = if (percent > 0) "p. $readPages • $percent%" else "${book.pageCount}p",
                    fontFamily = InterFamily,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (percent >= 100) colors.primary else Color.White,
                    modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                )
            }

            // Embedded Reading Progress Line across bottom edge of the cover container
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(5.dp)
                    .background(Color.Black.copy(alpha = 0.5f))
            ) {
                if (progressFraction > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth(progressFraction)
                            .fillMaxHeight()
                            .background(colors.primary)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(7.dp))

        // Book Title (2 lines max)
        Text(
            text = book.title,
            fontFamily = PoppinsFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            color = colors.textPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 17.sp
        )

        // Author / Subject Subtitle (1 line)
        Text(
            text = book.author.ifBlank { book.subject }.ifBlank { "Textbook" },
            fontFamily = InterFamily,
            fontSize = 11.sp,
            color = colors.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
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
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "WeeklyProgress"
    )

    ElevatedCard(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = colors.surfaceCard),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.5.dp),
        modifier = Modifier
            .fillMaxWidth()
            .testTag("weekly_progress_card")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp)
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
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Weekly Goal",
                        fontFamily = PoppinsFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = colors.textPrimary
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    IconButton(
                        onClick = onEditGoalClick,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Icon(
                            imageVector = FluentIcons.Edit24Regular,
                            contentDescription = "Configure Target",
                            tint = colors.primary,
                            modifier = Modifier.size(13.dp)
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = colors.primary.copy(alpha = 0.14f)
                ) {
                    Text(
                        text = "$progressPercent%",
                        fontSize = 12.sp,
                        fontFamily = InterFamily,
                        fontWeight = FontWeight.Bold,
                        color = colors.primary,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Single Progress Bar with Embedded Stat Text
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(38.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.surfaceTint)
                    .border(0.5.dp, colors.borderSubtle, RoundedCornerShape(14.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedProgress)
                        .fillMaxHeight()
                        .clip(RoundedCornerShape(14.dp))
                        .background(colors.primary)
                )

                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val label = buildString {
                        append("$completedPages of $goalPages pages read")
                        if (plannedPages > 0) {
                            append(" • $plannedPages planned")
                        }
                    }
                    Text(
                        text = label,
                        fontFamily = PoppinsFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = if (animatedProgress > 0.55f) colors.onPrimary else colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        }
    }
}

@Composable
private fun SectionHeader(
    title: String,
    badge: String?,
    icon: ImageVector? = null
) {
    val colors = LocalMulberryColors.current
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, bottom = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Text(
                text = title,
                fontFamily = PoppinsFamily,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
                letterSpacing = (-0.3).sp
            )
        }
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
private fun ShelfEmptyStateCard() {
    val colors = LocalMulberryColors.current
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
                fontWeight = FontWeight.Bold,
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
