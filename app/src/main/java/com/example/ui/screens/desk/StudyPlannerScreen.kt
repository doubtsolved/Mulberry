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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
 * Clamped so users cannot navigate beyond the present date.
 * Features compact schedule cards per ASCII specification.
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
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            add(Calendar.WEEK_OF_YEAR, weekOffset)
        }
        val todayCal = Calendar.getInstance()
        val list = mutableListOf<PlannerDay>()
        val dayFormat = SimpleDateFormat("EEE", Locale.getDefault())

        for (i in 0 until 7) {
            val isToday = cal.get(Calendar.YEAR) == todayCal.get(Calendar.YEAR) &&
                    cal.get(Calendar.DAY_OF_YEAR) == todayCal.get(Calendar.DAY_OF_YEAR)
            val isFuture = if (weekOffset == 0) i > todayDayOfWeekIndex else weekOffset > 0

            list.add(
                PlannerDay(
                    dayName = dayFormat.format(cal.time).uppercase(),
                    dayNumber = cal.get(Calendar.DAY_OF_MONTH),
                    epochMillis = cal.timeInMillis,
                    isToday = isToday,
                    isFuture = isFuture,
                    isCompleted = i < todayDayOfWeekIndex && weekOffset == 0 || weekOffset < 0
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

                    Row {
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
                        .height(50.dp)
                        .testTag("btn_planner_add_session"),
                    shape = RoundedCornerShape(16.dp),
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
                        fontSize = 14.5.sp
                    )
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // 1. Weekly Reading Goal Card
            item(key = "weekly_reading_goal_card") {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = colors.surfaceCard,
                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.borderSubtle),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("planner_weekly_goal_card")
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            text = "Weekly reading goal",
                            fontFamily = InterFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.5.sp,
                            color = colors.textSecondary
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Text(
                                text = "$completedHours / ${goalHours.toInt()} hours",
                                fontFamily = PoppinsFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 22.sp,
                                color = colors.textPrimary
                            )
                            Text(
                                text = "($percent%)",
                                fontFamily = PoppinsFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp,
                                color = colors.primary
                            )
                        }

                        // Progress bar
                        LinearProgressIndicator(
                            progress = { (percent.toFloat() / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(7.dp)
                                .clip(RoundedCornerShape(4.dp)),
                            color = colors.primary,
                            trackColor = colors.borderSubtle
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Sessions: $completedSessionsCount of $totalSessions",
                                fontFamily = InterFamily,
                                fontSize = 12.sp,
                                color = colors.textSecondary
                            )
                            Text(
                                text = "Remaining: 2h 30m",
                                fontFamily = InterFamily,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                color = colors.primary
                            )
                        }
                    }
                }
            }

            // 2. THIS WEEK Selector Strip (Clamped so cannot go beyond present week)
            item(key = "this_week_header") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (weekOffset == 0) "THIS WEEK" else "PAST WEEK",
                        fontFamily = PoppinsFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        letterSpacing = 0.8.sp,
                        color = colors.primary
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                        // Previous Week (always allowed)
                        IconButton(
                            onClick = { weekOffset -= 1 },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = FluentIcons.ChevronLeft24Regular,
                                contentDescription = "Previous week",
                                tint = colors.textSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }

                        // Next Week: Clamped! Never allow week scroll beyond present week (weekOffset >= 0)
                        val canAdvanceWeek = weekOffset < 0
                        IconButton(
                            onClick = {
                                if (canAdvanceWeek) {
                                    weekOffset = (weekOffset + 1).coerceAtMost(0)
                                }
                            },
                            enabled = canAdvanceWeek,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = FluentIcons.ChevronRight24Regular,
                                contentDescription = "Next week",
                                tint = if (canAdvanceWeek) colors.textSecondary else colors.textMuted.copy(alpha = 0.25f),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }

            item(key = "this_week_strip") {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(weekDays.indices.toList()) { index ->
                        val day = weekDays[index]
                        val isSelected = selectedDayIndex == index
                        // Cannot select future dates when in present week
                        val isClickable = !day.isFuture

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = when {
                                isSelected -> colors.primary
                                day.isFuture -> colors.surfaceCard.copy(alpha = 0.45f)
                                else -> colors.surfaceCard
                            },
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                when {
                                    isSelected -> colors.primary
                                    day.isFuture -> colors.borderSubtle.copy(alpha = 0.3f)
                                    else -> colors.borderSubtle
                                }
                            ),
                            modifier = Modifier
                                .width(56.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .clickable(enabled = isClickable) {
                                    selectedDayIndex = index
                                }
                                .testTag("planner_day_chip_${day.dayNumber}")
                        ) {
                            Column(
                                modifier = Modifier.padding(vertical = 12.dp, horizontal = 4.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = day.dayName,
                                    fontFamily = InterFamily,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 11.sp,
                                    color = when {
                                        isSelected -> colors.onPrimary.copy(alpha = 0.85f)
                                        day.isFuture -> colors.textMuted.copy(alpha = 0.4f)
                                        else -> colors.textMuted
                                    }
                                )
                                Text(
                                    text = day.dayNumber.toString(),
                                    fontFamily = PoppinsFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = when {
                                        isSelected -> colors.onPrimary
                                        day.isFuture -> colors.textMuted.copy(alpha = 0.4f)
                                        else -> colors.textPrimary
                                    }
                                )
                                Text(
                                    text = when {
                                        day.isFuture -> "·"
                                        day.isCompleted -> "✓"
                                        day.isToday -> "●"
                                        else -> "○"
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = when {
                                        isSelected -> colors.onPrimary
                                        day.isFuture -> colors.textMuted.copy(alpha = 0.4f)
                                        else -> colors.primary
                                    }
                                )
                            }
                        }
                    }
                }
            }

            // 3. DAILY SESSION LIST (Compact Schedule Cards)
            item(key = "daily_plan_header") {
                Column {
                    Text(
                        text = "${selectedDay.dayName}'S PLAN",
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
                        onToggleComplete = { deskViewModel.toggleCompleted(session.id) }
                    )
                }
            }

            item(key = "bottom_space") {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

/**
 * Compact Schedule Card
 * Specification:
 * ┌────────────────────────────────────────────────────────┐
 * │ [COVER] Name of book                                   │
 * │         additional text                       [▶ Read] │
 * └────────────────────────────────────────────────────────┘
 */
@Composable
private fun CompactDailyScheduleCard(
    session: ReadingSession,
    coverUrl: String = "",
    onStartSession: () -> Unit,
    onToggleComplete: () -> Unit
) {
    val colors = LocalMulberryColors.current

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = colors.surfaceCard,
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            if (session.isCompleted) colors.primary.copy(alpha = 0.35f) else colors.borderSubtle
        ),
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
                    color = colors.textPrimary,
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
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

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
