package com.example.ui.screens.desk

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.BookEntity
import com.example.data.schedule.model.ReadingSession
import com.example.data.schedule.model.ScheduleHistoryEntry
import com.example.ui.components.FluentIcons
import com.example.ui.theme.InterFamily
import com.example.ui.theme.LocalMulberryColors
import com.example.ui.theme.PoppinsFamily
import com.example.viewmodel.DeskScheduleViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

data class PlannerDay(
    val dayName: String,
    val dayNumber: Int,
    val epochMillis: Long,
    val isToday: Boolean,
    val isFuture: Boolean,
    val isCompleted: Boolean
)

/**
 * Study Planner Screen (Wireframe 04)
 * Refined per user specifications:
 * - Week navigation clamped at present date (no forward future scrolling)
 * - Compact schedule cards matching ASCII layout with book cover, title, additional info, and read icon
 * - Allows editing existing schedules with persistent history updates
 * - Structured schedule history file /sdcard/Mulberry/.schedule/schedule_history.json displayed in history viewer
 * - Overdue uncompleted schedules automatically shifted to next/present date with Warm Amber accent background
 * - Responsive to landscape and large tablet orientations
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyPlannerScreen(
    deskViewModel: DeskScheduleViewModel,
    downloadedBooks: List<BookEntity> = emptyList(),
    onBackClick: () -> Unit,
    onOpenSession: (ReadingSession) -> Unit,
    onAddSessionClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalMulberryColors.current
    val scheduleState by deskViewModel.scheduleState.collectAsState()
    val activeTodaySessions by deskViewModel.activeTodaySessions.collectAsState()
    val completedTodaySessions by deskViewModel.completedTodaySessions.collectAsState()
    val scheduleHistory by deskViewModel.scheduleHistory.collectAsState()

    var sessionToEdit by remember { mutableStateOf<ReadingSession?>(null) }
    var showHistoryModal by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        deskViewModel.loadScheduleHistory()
    }

    // Determine current day of week index: Mon=0, Tue=1, ..., Sun=6
    val currentCal = Calendar.getInstance()
    val todayDayOfWeekIndex = when (currentCal.get(Calendar.DAY_OF_WEEK)) {
        Calendar.MONDAY -> 0
        Calendar.TUESDAY -> 1
        Calendar.WEDNESDAY -> 2
        Calendar.THURSDAY -> 3
        Calendar.FRIDAY -> 4
        Calendar.SATURDAY -> 5
        Calendar.SUNDAY -> 6
        else -> 0
    }

    var selectedDayIndex by remember { mutableIntStateOf(todayDayOfWeekIndex) }
    var weekOffset by remember { mutableIntStateOf(0) } // 0 = present week, negative = past weeks

    // Generate 7-day strip based on current week and weekOffset
    val weekDays = remember(weekOffset) {
        val cal = Calendar.getInstance().apply {
            firstDayOfWeek = Calendar.MONDAY
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            add(Calendar.WEEK_OF_YEAR, weekOffset)
        }
        val todayCal = Calendar.getInstance()
        val list = mutableListOf<PlannerDay>()
        val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())

        for (i in 0 until 7) {
            val isToday = cal.get(Calendar.YEAR) == todayCal.get(Calendar.YEAR) &&
                    cal.get(Calendar.DAY_OF_YEAR) == todayCal.get(Calendar.DAY_OF_YEAR)
            val isFuture = cal.after(todayCal) && !isToday

            list.add(
                PlannerDay(
                    dayName = dayFormat.format(cal.time).uppercase(),
                    dayNumber = cal.get(Calendar.DAY_OF_MONTH),
                    epochMillis = cal.timeInMillis,
                    isToday = isToday,
                    isFuture = isFuture,
                    isCompleted = (i < todayDayOfWeekIndex && weekOffset == 0) || weekOffset < 0
                )
            )
            cal.add(Calendar.DAY_OF_MONTH, 1)
        }
        list
    }

    val totalSessions = scheduleState.sessions.size.coerceAtLeast(1)
    val completedSessionsCount = scheduleState.sessions.count { it.isCompleted }
    val goalHours = 7.0
    val completedHours = 4.5
    val percent = ((completedHours / goalHours) * 100).toInt().coerceIn(0, 100)

    val selectedDay = weekDays.getOrNull(selectedDayIndex) ?: weekDays.first()

    // Daily sessions for current view
    val allSessions = (activeTodaySessions + completedTodaySessions).ifEmpty {
        scheduleState.sessions
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(colors.backgroundGradient),
        containerColor = Color.Transparent,
        topBar = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBackClick, modifier = Modifier.size(36.dp)) {
                            Icon(
                                imageVector = FluentIcons.ArrowLeft24Regular,
                                contentDescription = "Back",
                                tint = colors.textPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "STUDY PLANNER",
                            fontFamily = PoppinsFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = colors.textPrimary
                        )
                    }

                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(
                            onClick = {
                                deskViewModel.loadScheduleHistory()
                                showHistoryModal = true
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = FluentIcons.History24Regular,
                                contentDescription = "Schedule History",
                                tint = colors.textPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        IconButton(onClick = onAddSessionClick, modifier = Modifier.size(36.dp)) {
                            Icon(
                                imageVector = FluentIcons.CalendarAdd24Regular,
                                contentDescription = "Schedule",
                                tint = colors.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Text(
                    text = if (weekOffset == 0) "Current week • in focus" else "Week past (${-weekOffset}w ago)",
                    fontFamily = InterFamily,
                    fontSize = 13.sp,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(start = 40.dp)
                )
            }
        },
        bottomBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                color = Color.Transparent
            ) {
                Button(
                    onClick = onAddSessionClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("btn_planner_add_session"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.primary,
                        contentColor = colors.onPrimary
                    )
                ) {
                    Icon(
                        imageVector = FluentIcons.Add24Regular,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Add session",
                        fontFamily = PoppinsFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentAlignment = Alignment.TopCenter
        ) {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .widthIn(max = 760.dp),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // 1. WEEKLY PROGRESS CARD
                item(key = "weekly_progress") {
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = colors.surfaceCard,
                        border = androidx.compose.foundation.BorderStroke(1.dp, colors.borderSubtle),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column {
                                    Text(
                                        text = "Weekly reading goal",
                                        fontFamily = InterFamily,
                                        fontSize = 12.5.sp,
                                        color = colors.textSecondary
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "$completedHours / ${goalHours.toInt()} hours",
                                        fontFamily = PoppinsFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 19.sp,
                                        color = colors.textPrimary
                                    )
                                }

                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = colors.primary.copy(alpha = 0.15f)
                                ) {
                                    Text(
                                        text = "$percent%",
                                        fontFamily = PoppinsFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = colors.primary,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            LinearProgressIndicator(
                                progress = { percent.toFloat() / 100f },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(6.dp)
                                    .clip(RoundedCornerShape(3.dp)),
                                color = colors.primary,
                                trackColor = colors.primary.copy(alpha = 0.15f),
                            )

                            Text(
                                text = "$completedSessionsCount of $totalSessions sessions completed",
                                fontFamily = InterFamily,
                                fontSize = 12.sp,
                                color = colors.textMuted
                            )
                        }
                    }
                }

                // 2. WEEK CONTROLS STRIP (Clamped so users cannot navigate beyond present date)
                item(key = "week_strip") {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "WEEK VIEW",
                                fontFamily = PoppinsFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.5.sp,
                                letterSpacing = 0.8.sp,
                                color = colors.primary
                            )

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                // Previous week button (allows scrolling to the past)
                                IconButton(
                                    onClick = {
                                        weekOffset -= 1
                                        selectedDayIndex = 0
                                    },
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = FluentIcons.ChevronLeft24Regular,
                                        contentDescription = "Previous week",
                                        tint = colors.textPrimary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }

                                Text(
                                    text = if (weekOffset == 0) "This week" else "${-weekOffset}w ago",
                                    fontFamily = InterFamily,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = colors.textPrimary,
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )

                                // Next week button: STRICTLY CLAMPED - disabled when at present week (weekOffset == 0)
                                IconButton(
                                    onClick = {
                                        if (weekOffset < 0) {
                                            weekOffset += 1
                                            selectedDayIndex = if (weekOffset == 0) todayDayOfWeekIndex else 0
                                        }
                                    },
                                    enabled = weekOffset < 0,
                                    modifier = Modifier.size(32.dp)
                                ) {
                                    Icon(
                                        imageVector = FluentIcons.ChevronRight24Regular,
                                        contentDescription = "Next week",
                                        tint = if (weekOffset < 0) colors.textPrimary else colors.textMuted.copy(alpha = 0.35f),
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }

                        // 7-day pill strip
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            weekDays.forEachIndexed { idx, day ->
                                val isSelected = idx == selectedDayIndex
                                val isClickable = !day.isFuture

                                Surface(
                                    shape = RoundedCornerShape(14.dp),
                                    color = when {
                                        isSelected -> colors.primary
                                        day.isToday -> colors.primary.copy(alpha = 0.12f)
                                        else -> colors.surfaceCard
                                    },
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        when {
                                            isSelected -> colors.primary
                                            day.isToday -> colors.primary
                                            else -> colors.borderSubtle
                                        }
                                    ),
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 2.dp)
                                        .clip(RoundedCornerShape(14.dp))
                                        .clickable(enabled = isClickable) {
                                            selectedDayIndex = idx
                                        }
                                        .testTag("day_pill_$idx")
                                ) {
                                    Column(
                                        modifier = Modifier.padding(vertical = 10.dp, horizontal = 2.dp),
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = day.dayName.take(1),
                                            fontFamily = InterFamily,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 11.sp,
                                            color = when {
                                                isSelected -> colors.onPrimary
                                                day.isFuture -> colors.textMuted.copy(alpha = 0.4f)
                                                else -> colors.textSecondary
                                            }
                                        )
                                        Text(
                                            text = day.dayNumber.toString(),
                                            fontFamily = PoppinsFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 13.sp,
                                            color = when {
                                                isSelected -> colors.onPrimary
                                                day.isFuture -> colors.textMuted.copy(alpha = 0.4f)
                                                else -> colors.textPrimary
                                            }
                                        )
                                        Box(
                                            modifier = Modifier
                                                .size(6.dp)
                                                .clip(CircleShape)
                                                .background(
                                                    when {
                                                        isSelected -> colors.onPrimary
                                                        day.isCompleted -> colors.primary
                                                        day.isToday -> colors.primary
                                                        else -> Color.Transparent
                                                    }
                                                )
                                        )
                                    }
                                }
                            }
                        }
                    }
                }

                // 3. SESSIONS FOR SELECTED DAY
                item(key = "day_sessions_header") {
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Text(
                            text = if (selectedDay.isToday) "TODAY'S SCHEDULE" else "${selectedDay.dayName} SCHEDULE",
                            fontFamily = PoppinsFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.5.sp,
                            letterSpacing = 0.5.sp,
                            color = colors.textPrimary
                        )
                        Text(
                            text = "Sessions for this study block",
                            fontFamily = InterFamily,
                            fontSize = 12.sp,
                            color = colors.textSecondary
                        )
                    }
                }

                if (allSessions.isEmpty()) {
                    item(key = "empty_sessions") {
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = colors.surfaceCard,
                            border = androidx.compose.foundation.BorderStroke(1.dp, colors.borderSubtle),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(
                                modifier = Modifier.padding(22.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Icon(
                                    imageVector = FluentIcons.CalendarAdd24Regular,
                                    contentDescription = null,
                                    tint = colors.textMuted,
                                    modifier = Modifier.size(32.dp)
                                )
                                Text(
                                    text = "No sessions planned yet",
                                    fontFamily = PoppinsFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    color = colors.textPrimary
                                )
                                Text(
                                    text = "Tap '+ Add session' to set up textbook targets with reminders.",
                                    fontFamily = InterFamily,
                                    fontSize = 12.sp,
                                    color = colors.textSecondary
                                )
                            }
                        }
                    }
                } else {
                    items(allSessions, key = { it.id }) { session ->
                        val matchedBook = downloadedBooks.find {
                            it.title.equals(session.bookTitle, ignoreCase = true) ||
                                    (session.bookFileName.isNotBlank() && it.binFilePath.contains(session.bookFileName))
                        }
                        CompactDailyScheduleCard(
                            session = session,
                            coverUrl = matchedBook?.coverUrl ?: "",
                            onStartSession = { onOpenSession(session) },
                            onToggleComplete = { deskViewModel.toggleCompleted(session.id) },
                            onEditSession = { sessionToEdit = session }
                        )
                    }
                }

                item(key = "bottom_space") {
                    Spacer(modifier = Modifier.height(20.dp))
                }
            }
        }
    }

    // Edit Schedule Dialog
    sessionToEdit?.let { session ->
        EditScheduleDialog(
            session = session,
            onDismiss = { sessionToEdit = null },
            onSave = { updated ->
                deskViewModel.editSession(updated)
                sessionToEdit = null
            },
            onDelete = {
                deskViewModel.deleteSession(session.id)
                sessionToEdit = null
            }
        )
    }

    // Schedule History Modal
    if (showHistoryModal) {
        ScheduleHistoryModal(
            history = scheduleHistory,
            onDismiss = { showHistoryModal = false }
        )
    }
}

/**
 * Compact Schedule Card
 * Specification:
 * ┌────────────────────────────────────────────────────────┐
 * │ [COVER] Name of book                        [✎]        │
 * │         additional text                       [▶ Read] │
 * └────────────────────────────────────────────────────────┘
 * Overdue rollover items are highlighted with a Warm Amber accent background.
 */
@Composable
private fun CompactDailyScheduleCard(
    session: ReadingSession,
    coverUrl: String = "",
    onStartSession: () -> Unit,
    onToggleComplete: () -> Unit,
    onEditSession: () -> Unit
) {
    val colors = LocalMulberryColors.current

    val isOverdue = session.isOverdueRollover && !session.isCompleted

    // Warm amber accent background per user specification for overdue schedules shifted to next day
    val cardBg = when {
        isOverdue -> Color(0xFFFEF3C7) // Warm amber light surface
        session.isCompleted -> colors.surfaceCard
        else -> colors.surfaceCard
    }

    val cardBorder = when {
        isOverdue -> Color(0xFFF59E0B) // Warm amber accent border
        session.isCompleted -> colors.primary.copy(alpha = 0.35f)
        else -> colors.borderSubtle
    }

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = cardBg,
        border = androidx.compose.foundation.BorderStroke(1.dp, cardBorder),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable { onStartSession() }
            .testTag("timeline_session_${session.id}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Book cover thumbnail on the left
            Surface(
                shape = RoundedCornerShape(8.dp),
                color = colors.primary.copy(alpha = 0.12f),
                border = androidx.compose.foundation.BorderStroke(0.5.dp, colors.borderSubtle),
                modifier = Modifier
                    .size(width = 38.dp, height = 50.dp)
                    .clip(RoundedCornerShape(8.dp))
            ) {
                if (coverUrl.isNotBlank()) {
                    AsyncImage(
                        model = coverUrl,
                        contentDescription = session.bookTitle,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = FluentIcons.Book24Filled,
                            contentDescription = null,
                            tint = colors.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Center: Name of book & additional text
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = session.bookTitle,
                    fontFamily = PoppinsFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = if (isOverdue) Color(0xFF78350F) else colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(3.dp))
                val additionalText = if (session.microNote.isNotBlank()) {
                    "${session.microNote} · P.${session.startPage}–${session.endPage}"
                } else {
                    "Pages ${session.startPage}–${session.endPage} · ${session.estimatedMinutes}m"
                }
                Text(
                    text = additionalText,
                    fontFamily = InterFamily,
                    fontSize = 12.sp,
                    color = if (isOverdue) Color(0xFF92400E) else colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Edit icon button
            IconButton(
                onClick = onEditSession,
                modifier = Modifier.size(32.dp)
            ) {
                Icon(
                    imageVector = FluentIcons.Edit24Regular,
                    contentDescription = "Edit session",
                    tint = if (isOverdue) Color(0xFFB45309) else colors.textMuted,
                    modifier = Modifier.size(17.dp)
                )
            }

            Spacer(modifier = Modifier.width(4.dp))

            // Right: Read icon / complete button
            if (session.isCompleted) {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = colors.primary.copy(alpha = 0.14f),
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onToggleComplete() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "✓ Done",
                            fontFamily = PoppinsFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.5.sp,
                            color = colors.primary
                        )
                    }
                }
            } else {
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = colors.primary,
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .clickable { onStartSession() }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = FluentIcons.BookOpen24Regular,
                            contentDescription = "Read",
                            tint = colors.onPrimary,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = "Read",
                            fontFamily = PoppinsFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 11.5.sp,
                            color = colors.onPrimary
                        )
                    }
                }
            }
        }
    }
}

/**
 * Material 3 expressive rounded edit dialog for reading schedule sessions.
 */
@Composable
private fun EditScheduleDialog(
    session: ReadingSession,
    onDismiss: () -> Unit,
    onSave: (ReadingSession) -> Unit,
    onDelete: () -> Unit
) {
    val colors = LocalMulberryColors.current
    var startPageText by remember { mutableStateOf(session.startPage.toString()) }
    var endPageText by remember { mutableStateOf(session.endPage.toString()) }
    var noteText by remember { mutableStateOf(session.microNote) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = colors.surfaceCard,
        title = {
            Text(
                text = "Edit Schedule",
                fontFamily = PoppinsFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = colors.textPrimary
            )
        },
        text = {
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = session.bookTitle,
                    fontFamily = InterFamily,
                    fontSize = 13.sp,
                    color = colors.primary,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedTextField(
                        value = startPageText,
                        onValueChange = { startPageText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("Start Page", fontSize = 11.5.sp) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.primary,
                            unfocusedBorderColor = colors.borderSubtle
                        )
                    )
                    OutlinedTextField(
                        value = endPageText,
                        onValueChange = { endPageText = it.filter { ch -> ch.isDigit() } },
                        label = { Text("End Page", fontSize = 11.5.sp) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.primary,
                            unfocusedBorderColor = colors.borderSubtle
                        )
                    )
                }

                OutlinedTextField(
                    value = noteText,
                    onValueChange = { noteText = it },
                    label = { Text("Topic or micro-note", fontSize = 11.5.sp) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = colors.primary,
                        unfocusedBorderColor = colors.borderSubtle
                    )
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val sPage = startPageText.toIntOrNull() ?: session.startPage
                    val ePage = (endPageText.toIntOrNull() ?: session.endPage).coerceAtLeast(sPage)
                    onSave(
                        session.copy(
                            startPage = sPage,
                            endPage = ePage,
                            microNote = noteText.trim()
                        )
                    )
                },
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
            ) {
                Text("Save Changes", fontFamily = PoppinsFamily, fontWeight = FontWeight.SemiBold)
            }
        },
        dismissButton = {
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                TextButton(
                    onClick = onDelete,
                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFEF4444))
                ) {
                    Text("Delete")
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancel", color = colors.textSecondary)
                }
            }
        }
    )
}

/**
 * Schedule History Modal showing archived schedules from /sdcard/Mulberry/.schedule/schedule_history.json
 */
@Composable
private fun ScheduleHistoryModal(
    history: List<ScheduleHistoryEntry>,
    onDismiss: () -> Unit
) {
    val colors = LocalMulberryColors.current
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = colors.surfaceCard,
        title = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Schedule History",
                    fontFamily = PoppinsFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = colors.textPrimary
                )
                IconButton(onClick = onDismiss, modifier = Modifier.size(32.dp)) {
                    Icon(
                        imageVector = FluentIcons.Dismiss24Regular,
                        contentDescription = "Close",
                        tint = colors.textSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        },
        text = {
            if (history.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "No recorded schedule history yet.",
                        fontFamily = InterFamily,
                        fontSize = 13.sp,
                        color = colors.textMuted
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(340.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(history, key = { it.id }) { item ->
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = colors.surface,
                            border = androidx.compose.foundation.BorderStroke(0.5.dp, colors.borderSubtle),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Column(modifier = Modifier.padding(10.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Text(
                                        text = item.bookTitle.ifEmpty { "Textbook" },
                                        fontFamily = PoppinsFamily,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp,
                                        color = colors.textPrimary,
                                        modifier = Modifier.weight(1f),
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Surface(
                                        shape = RoundedCornerShape(6.dp),
                                        color = when (item.action) {
                                            "COMPLETED" -> colors.primary.copy(alpha = 0.15f)
                                            "ROLLED_OVER" -> Color(0xFFFEF3C7)
                                            else -> colors.surfaceCard
                                        }
                                    ) {
                                        Text(
                                            text = item.action,
                                            fontFamily = InterFamily,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 9.sp,
                                            color = when (item.action) {
                                                "COMPLETED" -> colors.primary
                                                "ROLLED_OVER" -> Color(0xFFB45309)
                                                else -> colors.textSecondary
                                            },
                                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                        )
                                    }
                                }
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Pages ${item.startPage}–${item.endPage} · ${dateFormat.format(Date(if (item.scheduledEpochMillis > 0L) item.scheduledEpochMillis else item.recordedAt))}",
                                    fontFamily = InterFamily,
                                    fontSize = 11.5.sp,
                                    color = colors.textSecondary
                                )
                                if (item.note.isNotBlank()) {
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = item.note,
                                        fontFamily = InterFamily,
                                        fontSize = 11.sp,
                                        color = colors.textMuted
                                    )
                                }
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onDismiss,
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
            ) {
                Text("Close", fontFamily = PoppinsFamily, fontWeight = FontWeight.SemiBold)
            }
        }
    )
}
