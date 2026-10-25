package com.example.ui.screens.desk.dashboard

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.analytics.StudyMetricsManager
import com.example.data.analytics.model.StudyMetricsData
import com.example.data.model.UserProfile
import com.example.ui.components.FluentIcons
import com.example.ui.theme.InterFamily
import com.example.ui.theme.LocalMulberryColors
import com.example.ui.theme.PoppinsFamily
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StudyDashboardScreen(
    userProfile: UserProfile,
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = LocalMulberryColors.current
    val metricsManager = remember { StudyMetricsManager.getInstance(context) }
    val metrics by metricsManager.metrics.collectAsState()

    BackHandler {
        onClose()
    }

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(colors.backgroundGradient),
        containerColor = Color.Transparent,
        topBar = {
            DashboardTopBar(
                onClose = onClose,
                onShare = {
                    StudyShareHelper.shareMetrics(context, metrics, userProfile)
                }
            )
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            // Student Welcome Banner Card
            item(key = "student_profile_card") {
                StudentGreetingCard(userProfile = userProfile, streakDays = metrics.currentStreakDays)
            }

            // 1. Overview Summary Metric Cards (Total Time, Consistency Streak, Reading Velocity)
            item(key = "overview_metrics_row") {
                OverviewMetricsSection(metrics = metrics)
            }

            // 2. Weekly Consistency (7-Day Matrix)
            item(key = "weekly_consistency_matrix") {
                WeeklyConsistencyCard(metrics = metrics)
            }

            // 3. Time Per Subject Breakdown
            item(key = "subject_breakdown_card") {
                SubjectBreakdownCard(metrics = metrics)
            }

            // 4. Recent Study Sessions
            item(key = "recent_study_sessions") {
                RecentSessionsCard(metrics = metrics)
            }

            item(key = "bottom_space") {
                Spacer(modifier = Modifier.height(40.dp))
            }
        }
    }
}

@Composable
private fun DashboardTopBar(
    onClose: () -> Unit,
    onShare: () -> Unit
) {
    val colors = LocalMulberryColors.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .statusBarsPadding()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .size(40.dp)
                    .testTag("btn_close_study_dashboard")
            ) {
                Icon(
                    imageVector = FluentIcons.ArrowLeft24Regular,
                    contentDescription = "Back",
                    tint = colors.textPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.width(6.dp))
            Text(
                text = "Study Hub & Metrics",
                fontFamily = PoppinsFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = colors.textPrimary
            )
        }

        // Elegant Share Button
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = colors.primary,
            modifier = Modifier
                .clip(RoundedCornerShape(20.dp))
                .clickable { onShare() }
                .testTag("btn_share_study_metrics")
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
            ) {
                Icon(
                    imageVector = FluentIcons.Share24Regular,
                    contentDescription = "Share",
                    tint = colors.onPrimary,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = "Share",
                    fontFamily = PoppinsFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 12.5.sp,
                    color = colors.onPrimary
                )
            }
        }
    }
}

@Composable
private fun StudentGreetingCard(
    userProfile: UserProfile,
    streakDays: Int
) {
    val colors = LocalMulberryColors.current
    val studentName = userProfile.displayName.ifBlank { "Dr. Sharma" }
    val doctorTitle = if (studentName.startsWith("Dr.", ignoreCase = true)) studentName else "Dr. $studentName"

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = colors.surfaceCard,
        border = BorderStroke(1.dp, colors.borderSubtle),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(18.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Welcome back, $doctorTitle",
                    fontFamily = PoppinsFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = colors.textPrimary
                )
                val institutionName = userProfile.institution.ifBlank { "MBBS Scholar" }
                val profName = userProfile.selectedProf.ifBlank { "2nd Prof Core" }
                Text(
                    text = "$institutionName • $profName",
                    fontFamily = InterFamily,
                    fontSize = 12.sp,
                    color = colors.textSecondary
                )
            }

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (colors.isDark) Color(0xFF451A03) else Color(0xFFFEF3C7),
                border = BorderStroke(1.dp, Color(0xFFF59E0B).copy(alpha = 0.5f))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = FluentIcons.Flame24Filled,
                        contentDescription = null,
                        tint = Color(0xFFD97706),
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = "$streakDays Days",
                        fontFamily = PoppinsFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = if (colors.isDark) Color(0xFFFDE68A) else Color(0xFF92400E)
                    )
                }
            }
        }
    }
}

@Composable
private fun OverviewMetricsSection(metrics: StudyMetricsData) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        MetricTile(
            title = "TOTAL TIME",
            value = metrics.formattedTotalTime,
            subtitle = "${metrics.totalPagesRead} pages read",
            icon = FluentIcons.Clock24Regular,
            modifier = Modifier.weight(1f)
        )
        MetricTile(
            title = "CONSISTENCY",
            value = "${metrics.currentStreakDays}-Day Streak",
            subtitle = "Best: ${metrics.bestStreakDays} days",
            icon = FluentIcons.Flame24Filled,
            modifier = Modifier.weight(1f)
        )
        MetricTile(
            title = "VELOCITY",
            value = metrics.formattedVelocity,
            subtitle = "Average pacing",
            icon = FluentIcons.Star20Regular,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun MetricTile(
    title: String,
    value: String,
    subtitle: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    val colors = LocalMulberryColors.current

    Surface(
        shape = RoundedCornerShape(18.dp),
        color = colors.surfaceCard,
        border = BorderStroke(1.dp, colors.borderSubtle),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = title,
                    fontFamily = PoppinsFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 10.sp,
                    color = colors.textSecondary,
                    letterSpacing = 0.5.sp
                )
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(15.dp)
                )
            }

            Text(
                text = value,
                fontFamily = PoppinsFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 14.5.sp,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Text(
                text = subtitle,
                fontFamily = InterFamily,
                fontSize = 10.5.sp,
                color = colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun WeeklyConsistencyCard(metrics: StudyMetricsData) {
    val colors = LocalMulberryColors.current

    val daysOfWeek = listOf("Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun")
    val cal = Calendar.getInstance()
    cal.firstDayOfWeek = Calendar.MONDAY
    cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
    val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)

    val currentWeekDays = (0 until 7).map { i ->
        val c = Calendar.getInstance()
        c.firstDayOfWeek = Calendar.MONDAY
        c.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
        c.add(Calendar.DAY_OF_YEAR, i)
        val dateStr = format.format(c.time)
        val minutes = metrics.dailyMinutes[dateStr] ?: 0
        Triple(daysOfWeek[i], minutes, dateStr)
    }

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = colors.surfaceCard,
        border = BorderStroke(1.dp, colors.borderSubtle),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "WEEKLY STUDY CONSISTENCY",
                    fontFamily = PoppinsFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    color = colors.textSecondary,
                    letterSpacing = 0.8.sp
                )
                Text(
                    text = "7-Day Target",
                    fontFamily = InterFamily,
                    fontSize = 11.sp,
                    color = colors.primary,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 7-day Matrix Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                currentWeekDays.forEach { (dayLabel, minutes, _) ->
                    val hasStudied = minutes > 0
                    val hoursStr = if (minutes >= 60) {
                        String.format("%.1fh", minutes / 60f)
                    } else if (minutes > 0) {
                        "${minutes}m"
                    } else "--"

                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = dayLabel,
                            fontFamily = InterFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 11.sp,
                            color = colors.textSecondary
                        )

                        // Indicator Circle
                        Surface(
                            shape = CircleShape,
                            color = if (hasStudied) colors.primary else colors.surfaceTint,
                            border = if (!hasStudied) BorderStroke(1.dp, colors.borderSubtle) else null,
                            modifier = Modifier.size(28.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                if (hasStudied) {
                                    Icon(
                                        imageVector = FluentIcons.Checkmark24Regular,
                                        contentDescription = "Active",
                                        tint = colors.onPrimary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                }
                            }
                        }

                        Text(
                            text = hoursStr,
                            fontFamily = InterFamily,
                            fontSize = 11.sp,
                            fontWeight = if (hasStudied) FontWeight.SemiBold else FontWeight.Normal,
                            color = if (hasStudied) colors.textPrimary else colors.textSecondary.copy(alpha = 0.6f)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SubjectBreakdownCard(metrics: StudyMetricsData) {
    val colors = LocalMulberryColors.current
    val totalMinutes = metrics.subjectMinutes.values.sum().coerceAtLeast(1)

    // Palette of vibrant subject accent bars
    val subjectColors = listOf(
        colors.primary,
        Color(0xFF3B82F6), // Blue
        Color(0xFF10B981), // Emerald
        Color(0xFFF59E0B), // Amber
        Color(0xFF8B5CF6), // Purple
        Color(0xFFEC4899)  // Rose
    )

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = colors.surfaceCard,
        border = BorderStroke(1.dp, colors.borderSubtle),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = "TIME PER SUBJECT BREAKDOWN",
                fontFamily = PoppinsFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = colors.textSecondary,
                letterSpacing = 0.8.sp
            )

            Spacer(modifier = Modifier.height(14.dp))

            val sortedSubjects = metrics.subjectMinutes.entries.sortedByDescending { it.value }
            if (sortedSubjects.isEmpty()) {
                Text(
                    text = "Start reading to log subject minutes",
                    fontFamily = InterFamily,
                    fontSize = 12.sp,
                    color = colors.textSecondary
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    sortedSubjects.forEachIndexed { index, entry ->
                        val (subjectName, mins) = entry
                        val percent = ((mins.toFloat() / totalMinutes.toFloat()) * 100f).toInt()
                        val hours = mins / 60
                        val remMins = mins % 60
                        val timeStr = if (hours > 0) "${hours}h ${remMins}m" else "${remMins}m"
                        val barColor = subjectColors[index % subjectColors.size]

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = subjectName,
                                    fontFamily = PoppinsFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.5.sp,
                                    color = colors.textPrimary
                                )
                                Text(
                                    text = "$timeStr ($percent%)",
                                    fontFamily = InterFamily,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 12.sp,
                                    color = colors.textSecondary
                                )
                            }

                            LinearProgressIndicator(
                                progress = { (mins.toFloat() / totalMinutes.toFloat()).coerceIn(0f, 1f) },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = barColor,
                                trackColor = colors.surfaceTint
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun RecentSessionsCard(metrics: StudyMetricsData) {
    val colors = LocalMulberryColors.current

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = colors.surfaceCard,
        border = BorderStroke(1.dp, colors.borderSubtle),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(
                text = "RECENT STUDY SESSIONS",
                fontFamily = PoppinsFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                color = colors.textSecondary,
                letterSpacing = 0.8.sp
            )

            Spacer(modifier = Modifier.height(12.dp))

            if (metrics.recentSessions.isEmpty()) {
                Text(
                    text = "No study sessions recorded yet",
                    fontFamily = InterFamily,
                    fontSize = 12.sp,
                    color = colors.textSecondary
                )
            } else {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    metrics.recentSessions.take(5).forEach { session ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.weight(1f)
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = colors.primary.copy(alpha = 0.12f),
                                    modifier = Modifier.size(34.dp)
                                ) {
                                    Box(contentAlignment = Alignment.Center) {
                                        Icon(
                                            imageVector = FluentIcons.Book24Regular,
                                            contentDescription = null,
                                            tint = colors.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                    }
                                }

                                Spacer(modifier = Modifier.width(10.dp))

                                Column {
                                    Text(
                                        text = session.bookTitle,
                                        fontFamily = PoppinsFamily,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp,
                                        color = colors.textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = "${session.subject} • ${session.pagesRead} pages read",
                                        fontFamily = InterFamily,
                                        fontSize = 11.5.sp,
                                        color = colors.textSecondary
                                    )
                                }
                            }

                            Text(
                                text = "${session.durationMinutes} min",
                                fontFamily = InterFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.5.sp,
                                color = colors.primary
                            )
                        }
                    }
                }
            }
        }
    }
}
