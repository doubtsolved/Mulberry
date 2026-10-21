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
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
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
    val isCompleted: Boolean
)

/**
 * Study Planner Screen (Wireframe 04)
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyPlannerScreen(
    deskViewModel: DeskScheduleViewModel,
    onBackClick: () -> Unit,
    onOpenSession: (ReadingSession) -> Unit,
    onAddSessionClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalMulberryColors.current
    val scheduleState by deskViewModel.scheduleState.collectAsState()
    val activeTodaySessions by deskViewModel.activeTodaySessions.collectAsState()
    val completedTodaySessions by deskViewModel.completedTodaySessions.collectAsState()

    var selectedDayIndex by remember { mutableIntStateOf(1) } // Default to Tuesday / Today
    var weekOffset by remember { mutableIntStateOf(0) }

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

            list.add(
                PlannerDay(
                    dayName = dayFormat.format(cal.time).uppercase(),
                    dayNumber = cal.get(Calendar.DAY_OF_MONTH),
                    epochMillis = cal.timeInMillis,
                    isToday = isToday,
                    isCompleted = i == 0 // Mock completed for earlier days or derive
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
                    text = "Your week, in focus • October 19–25, 2026",
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
            verticalArrangement = Arrangement.spacedBy(18.dp)
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
                        modifier = Modifier.padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Weekly reading goal",
                            fontFamily = InterFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp,
                            color = colors.textSecondary
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.Bottom
                        ) {
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = "$completedHours / ${goalHours.toInt()} hours",
                                    fontFamily = PoppinsFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 24.sp,
                                    color = colors.textPrimary
                                )
                            }
                            Text(
                                text = "($percent%)",
                                fontFamily = PoppinsFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp,
                                color = colors.primary
                            )
                        }

                        // Progress bar
                        LinearProgressIndicator(
                            progress = { (percent.toFloat() / 100f).coerceIn(0f, 1f) },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(8.dp)
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
                                fontSize = 12.5.sp,
                                color = colors.textSecondary
                            )
                            Text(
                                text = "Remaining: 2h 30m",
                                fontFamily = InterFamily,
                                fontSize = 12.5.sp,
                                fontWeight = FontWeight.Medium,
                                color = colors.primary
                            )
                        }
                    }
                }
            }

            // 2. THIS WEEK Selector Strip
            item(key = "this_week_header") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "THIS WEEK",
                        fontFamily = PoppinsFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        letterSpacing = 0.8.sp,
                        color = colors.primary
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
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
                        IconButton(
                            onClick = { weekOffset += 1 },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = FluentIcons.ChevronRight24Regular,
                                contentDescription = "Next week",
                                tint = colors.textSecondary,
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

                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = if (isSelected) colors.primary else colors.surfaceCard,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (isSelected) colors.primary else colors.borderSubtle
                            ),
                            modifier = Modifier
                                .width(56.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .clickable { selectedDayIndex = index }
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
                                    color = if (isSelected) colors.onPrimary.copy(alpha = 0.8f) else colors.textMuted
                                )
                                Text(
                                    text = day.dayNumber.toString(),
                                    fontFamily = PoppinsFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 16.sp,
                                    color = if (isSelected) colors.onPrimary else colors.textPrimary
                                )
                                Text(
                                    text = when {
                                        day.isCompleted -> "✓"
                                        day.isToday -> "●"
                                        else -> "○"
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) colors.onPrimary else colors.primary
                                )
                            }
                        }
                    }
                }
            }

            // 3. DAILY SESSION LIST
            item(key = "daily_plan_header") {
                Column {
                    Text(
                        text = "${selectedDay.dayName}'S PLAN",
                        fontFamily = PoppinsFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 14.sp,
                        color = colors.textPrimary
                    )
                    Text(
                        text = "Sessions for this study block",
                        fontFamily = InterFamily,
                        fontSize = 12.5.sp,
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
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = FluentIcons.CalendarAdd24Regular,
                                contentDescription = null,
                                tint = colors.textMuted,
                                modifier = Modifier.size(36.dp)
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
                    DailyTimelineSessionCard(
                        session = session,
                        onStartSession = { onOpenSession(session) },
                        onToggleComplete = { deskViewModel.toggleCompleted(session.id) }
                    )
                }
            }

            item(key = "bottom_space") {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

@Composable
private fun DailyTimelineSessionCard(
    session: ReadingSession,
    onStartSession: () -> Unit,
    onToggleComplete: () -> Unit
) {
    val colors = LocalMulberryColors.current
    val timeFormat = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
    val displayTime = remember(session.scheduledEpochMillis) {
        if (session.scheduledEpochMillis > 0L) timeFormat.format(Date(session.scheduledEpochMillis)) else "09:00"
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top
    ) {
        // Left Time Marker
        Text(
            text = displayTime,
            fontFamily = InterFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 13.sp,
            color = colors.textMuted,
            modifier = Modifier
                .width(44.dp)
                .padding(top = 14.dp)
        )

        Spacer(modifier = Modifier.width(6.dp))

        // Session Card
        Surface(
            shape = RoundedCornerShape(16.dp),
            color = colors.surfaceCard,
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                if (session.isCompleted) colors.primary.copy(alpha = 0.4f) else colors.borderSubtle
            ),
            modifier = Modifier
                .weight(1f)
                .clip(RoundedCornerShape(16.dp))
                .clickable { onStartSession() }
                .testTag("timeline_session_${session.id}")
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(6.dp),
                            color = colors.primary.copy(alpha = 0.12f),
                            modifier = Modifier.padding(end = 6.dp)
                        ) {
                            Text(
                                text = "BOOK",
                                fontFamily = InterFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 10.sp,
                                color = colors.primary,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        Text(
                            text = session.bookTitle,
                            fontFamily = PoppinsFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.5.sp,
                            color = colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    Text(
                        text = "Pages ${session.startPage}–${session.endPage}",
                        fontFamily = InterFamily,
                        fontSize = 11.5.sp,
                        color = colors.textSecondary
                    )
                }

                if (session.microNote.isNotBlank()) {
                    Text(
                        text = session.microNote,
                        fontFamily = InterFamily,
                        fontSize = 12.sp,
                        color = colors.textSecondary,
                        maxLines = 2
                    )
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${session.totalPages} pages",
                        fontFamily = InterFamily,
                        fontSize = 12.sp,
                        color = colors.textMuted
                    )

                    if (session.isCompleted) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = colors.primary.copy(alpha = 0.15f),
                            modifier = Modifier.clickable { onToggleComplete() }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "✓ Completed",
                                    fontFamily = InterFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 12.sp,
                                    color = colors.primary
                                )
                            }
                        }
                    } else {
                        Button(
                            onClick = onStartSession,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = colors.primary,
                                contentColor = colors.onPrimary
                            ),
                            modifier = Modifier.height(34.dp)
                        ) {
                            Text(
                                text = "▶ Start session",
                                fontFamily = InterFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }
        }
    }
}
