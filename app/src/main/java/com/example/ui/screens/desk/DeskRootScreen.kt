@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.ui.screens.desk

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.VaultEntity
import com.example.viewmodel.rootPath
import com.example.data.schedule.model.ReadingSession
import com.example.ui.components.FluentIcons
import com.example.ui.reader.ReaderActivity
import com.example.ui.theme.LocalMulberryColors
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
    val isRefreshing by deskViewModel.isRefreshing.collectAsState()

    val activeTodaySessions by deskViewModel.activeTodaySessions.collectAsState()
    val completedTodaySessions by deskViewModel.completedTodaySessions.collectAsState()
    val tomorrowSessions by deskViewModel.tomorrowSessions.collectAsState()
    val upcomingSessions by deskViewModel.upcomingSessions.collectAsState()

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

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background),
        containerColor = colors.background,
        topBar = {
            DeskTopAppBar(
                vault = activeVault,
                isRefreshing = isRefreshing,
                onRefresh = {
                    if (!isRefreshing) {
                        coroutineScope.launch(Dispatchers.IO) {
                            runCatching { deskViewModel.reload() }
                        }
                    }
                }
            )
        },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = {
                    sessionToEdit = null
                    showCreateSheet = true
                },
                shape = RoundedCornerShape(20.dp),
                containerColor = colors.primary,
                contentColor = colors.onPrimary,
                modifier = Modifier.testTag("fab_schedule_session")
            ) {
                Icon(
                    imageVector = FluentIcons.CalendarAdd24Regular,
                    contentDescription = "Schedule",
                    modifier = Modifier.size(20.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Schedule",
                    fontWeight = FontWeight.Bold,
                    fontSize = 15.sp
                )
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Refined Weekly Progress Hero Card
            item(key = "weekly_progress_card") {
                WeeklyProgressCard(
                    completedPages = weeklyCompletedPages,
                    goalPages = weeklyGoalPages,
                    plannedPages = weeklyPlannedPages,
                    progressPercent = weeklyProgressPercent,
                    onEditGoalClick = { showGoalSheet = true }
                )
            }

            // 2. Section Header: TODAY
            item(key = "today_header") {
                val totalTodayCount = activeTodaySessions.size + completedTodaySessions.size
                SectionHeader(
                    title = "TODAY",
                    badge = if (totalTodayCount > 0) "$totalTodayCount SESSIONS ($todayPagesCount PAGES)" else null
                )
            }

            // Empty state if no sessions for today
            if (activeTodaySessions.isEmpty() && completedTodaySessions.isEmpty()) {
                item(key = "today_empty_state") {
                    TodayEmptyState(onScheduleClick = {
                        sessionToEdit = null
                        showCreateSheet = true
                    })
                }
            }

            // Active Today Hero Sessions
            items(
                items = activeTodaySessions,
                key = { "active_${it.id}" }
            ) { session ->
                HeroReadingSessionCard(
                    session = session,
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
                    onCompleteClick = {
                        deskViewModel.markCompleted(session.id)
                    },
                    onBumpClick = {
                        deskViewModel.bumpSession(session.id, 1)
                    },
                    onEditClick = {
                        sessionToEdit = session
                        showCreateSheet = true
                    },
                    onDeleteClick = {
                        deskViewModel.deleteSession(session.id)
                    },
                    onRelocateClick = {
                        activeVault?.let { v ->
                            deskViewModel.autoResolveMissingBook(session, v.rootPath)
                        }
                    }
                )
            }

            // Completed Today Cards (with Reopen support)
            items(
                items = completedTodaySessions,
                key = { "completed_${it.id}" }
            ) { session ->
                CompletedSessionCard(
                    session = session,
                    onReopenClick = {
                        deskViewModel.toggleCompleted(session.id)
                    },
                    onDeleteClick = {
                        deskViewModel.deleteSession(session.id)
                    }
                )
            }

            // 3. Section Header: TOMORROW (if present)
            if (tomorrowSessions.isNotEmpty()) {
                item(key = "tomorrow_header") {
                    SectionHeader(
                        title = "TOMORROW",
                        badge = "${tomorrowSessions.size} SESSIONS"
                    )
                }

                items(
                    items = tomorrowSessions,
                    key = { "tomorrow_${it.id}" }
                ) { session ->
                    UpcomingSessionCard(
                        session = session,
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
                        onCompleteClick = {
                            deskViewModel.markCompleted(session.id)
                        },
                        onBumpClick = {
                            deskViewModel.bumpSession(session.id, 1)
                        },
                        onEditClick = {
                            sessionToEdit = session
                            showCreateSheet = true
                        },
                        onDeleteClick = {
                            deskViewModel.deleteSession(session.id)
                        }
                    )
                }
            }

            // 4. Section Header: UPCOMING THIS WEEK (if present)
            if (upcomingSessions.isNotEmpty()) {
                item(key = "upcoming_header") {
                    SectionHeader(
                        title = "LATER THIS WEEK",
                        badge = "${upcomingSessions.size} SESSIONS"
                    )
                }

                items(
                    items = upcomingSessions,
                    key = { "upcoming_${it.id}" }
                ) { session ->
                    UpcomingSessionCard(
                        session = session,
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
                        onCompleteClick = {
                            deskViewModel.markCompleted(session.id)
                        },
                        onBumpClick = {
                            deskViewModel.bumpSession(session.id, 1)
                        },
                        onEditClick = {
                            sessionToEdit = session
                            showCreateSheet = true
                        },
                        onDeleteClick = {
                            deskViewModel.deleteSession(session.id)
                        }
                    )
                }
            }

            // Bottom Spacing for FAB
            item(key = "bottom_fab_spacer") {
                Spacer(modifier = Modifier.height(72.dp))
            }
        }
    }

    // Schedule Session Creation & Edit Modal Bottom Sheet
    if (showCreateSheet) {
        ScheduleSessionSheet(
            sheetState = sheetState,
            vaultBooks = vaultBooks,
            existingSession = sessionToEdit,
            onDismiss = {
                coroutineScope.launch {
                    sheetState.hide()
                    showCreateSheet = false
                    sessionToEdit = null
                }
            },
            onSchedule = { book, startPage, endPage, scheduledEpochMillis, reminderMinutesBefore, microNote ->
                val currentVault = activeVault
                val relPath = if (currentVault != null && book.uriString.startsWith(currentVault.rootPath)) {
                    book.uriString.removePrefix(currentVault.rootPath).trimStart('/')
                } else {
                    book.fileName
                }

                if (sessionToEdit != null) {
                    val updated = sessionToEdit!!.copy(
                        bookFileName = book.fileName.ifEmpty { book.uriString },
                        bookRelativePath = relPath,
                        bookTitle = book.title,
                        startPage = startPage,
                        endPage = endPage,
                        scheduledEpochMillis = scheduledEpochMillis,
                        reminderMinutesBefore = reminderMinutesBefore,
                        microNote = microNote.trim()
                    )
                    deskViewModel.editSession(updated)
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
                sessionToEdit = null
            }
        )
    }

    // Weekly Goal Configuration Sheet
    if (showGoalSheet) {
        WeeklyGoalSheet(
            sheetState = goalSheetState,
            currentGoal = weeklyGoalPages,
            onSaveGoal = { newGoal ->
                deskViewModel.updateWeeklyGoal(newGoal)
            },
            onDismiss = {
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
    vault: VaultEntity?,
    isRefreshing: Boolean,
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
            .padding(horizontal = 16.dp, vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "Desk",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            color = colors.textPrimary,
            fontSize = 24.sp
        )

        Row(verticalAlignment = Alignment.CenterVertically) {
            // Vault Badge Pill
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = colors.surfaceTint.copy(alpha = 0.7f),
                modifier = Modifier.testTag("desk_vault_badge")
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = FluentIcons.Folder24Regular,
                        contentDescription = "Active Vault",
                        tint = colors.primary,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = vault?.name ?: "Vault",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            IconButton(
                onClick = onRefresh,
                enabled = !isRefreshing,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("btn_refresh_desk")
            ) {
                Icon(
                    imageVector = FluentIcons.ArrowSync24Regular,
                    contentDescription = "Refresh Schedule",
                    tint = if (isRefreshing) colors.primary else colors.textSecondary,
                    modifier = Modifier
                        .size(18.dp)
                        .then(if (isRefreshing) Modifier.rotate(rotationAngle) else Modifier)
                )
            }
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
        colors = CardDefaults.elevatedCardColors(
            containerColor = colors.surfaceCard
        ),
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
                            contentDescription = "Configure Weekly Target",
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
                        fontWeight = FontWeight.Bold,
                        color = colors.primary,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "$completedPages / $goalPages pages read",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary
            )

            Text(
                text = "Scheduled for this week: $plannedPages pages",
                style = MaterialTheme.typography.bodySmall,
                color = colors.textSecondary
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Smooth animated M3 progress bar
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
            fontSize = 12.sp,
            fontWeight = FontWeight.Bold,
            color = colors.textSecondary,
            letterSpacing = 1.sp
        )

        if (badge != null) {
            Text(
                text = badge,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = colors.textSecondary.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun TodayEmptyState(
    onScheduleClick: () -> Unit
) {
    val colors = LocalMulberryColors.current

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = colors.surfaceCard.copy(alpha = 0.7f),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(colors.surfaceTint),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = FluentIcons.CalendarLtr24Regular,
                    contentDescription = "No sessions",
                    tint = colors.primary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "No study sessions scheduled for today",
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = "Tap Schedule (+ icon) to set your reading goals and pace.",
                style = MaterialTheme.typography.bodySmall,
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

    // Recursive search fallback
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
