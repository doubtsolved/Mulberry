package com.example.ui.screens.desk.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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

@Composable
fun ScheduleProgressCard(
    session: ReadingSession,
    modifier: Modifier = Modifier,
    onReadClick: () -> Unit,
    onToggleComplete: () -> Unit
) {
    val colors = LocalMulberryColors.current
    val isCompleted = session.isCompleted
    val progressFraction = session.progressFraction
    val progressPercent = session.progressPercent

    val animatedProgress by animateFloatAsState(
        targetValue = progressFraction,
        animationSpec = tween(durationMillis = 600, easing = FastOutSlowInEasing),
        label = "ButtonProgressFill"
    )

    Surface(
        shape = RoundedCornerShape(22.dp),
        color = colors.surfaceCard,
        border = BorderStroke(1.dp, colors.borderSubtle),
        shadowElevation = 2.dp,
        modifier = modifier
            .width(300.dp)
            .clip(RoundedCornerShape(22.dp))
            .clickable { onReadClick() }
            .testTag("schedule_progress_card_${session.id}")
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp)
        ) {
            // Header Row: Tag Badge + Checkmark Button
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Header category tag badge
                val (tagLabel, tagIcon, tagColor) = when {
                    isCompleted -> Triple("COMPLETED", FluentIcons.Checkmark24Regular, colors.primary)
                    progressPercent > 0 -> Triple("CONTINUE READING", FluentIcons.Book24Regular, colors.primary)
                    else -> Triple("SCHEDULED", FluentIcons.CalendarLtr24Regular, colors.textSecondary)
                }

                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = tagColor.copy(alpha = 0.12f),
                    modifier = Modifier.testTag("schedule_tag_badge_${session.id}")
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Icon(
                            imageVector = tagIcon,
                            contentDescription = null,
                            tint = tagColor,
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(5.dp))
                        Text(
                            text = tagLabel,
                            fontFamily = PoppinsFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.sp,
                            color = tagColor,
                            letterSpacing = 0.6.sp
                        )
                    }
                }

                // Checkmark toggle button
                IconButton(
                    onClick = onToggleComplete,
                    modifier = Modifier
                        .size(32.dp)
                        .testTag("btn_toggle_session_${session.id}")
                ) {
                    Surface(
                        shape = CircleShape,
                        color = if (isCompleted) colors.primary else colors.surfaceTint,
                        border = if (!isCompleted) BorderStroke(1.dp, colors.borderSubtle) else null,
                        modifier = Modifier.size(24.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            if (isCompleted) {
                                Icon(
                                    imageVector = FluentIcons.Checkmark24Regular,
                                    contentDescription = "Completed",
                                    tint = colors.onPrimary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Prominent Book Title
            Text(
                text = session.bookTitle.ifEmpty { session.bookFileName.removeSuffix(".pdf") },
                fontFamily = PoppinsFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp,
                color = colors.textPrimary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 22.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Sub-metadata: Pages X–Y · Currently at page Z
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = FluentIcons.DocumentPageNumber24Regular,
                    contentDescription = null,
                    tint = colors.textSecondary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = "Pages ${session.startPage}–${session.endPage}",
                    fontFamily = InterFamily,
                    fontSize = 12.sp,
                    color = colors.textSecondary,
                    fontWeight = FontWeight.Medium
                )

                Text(
                    text = "  ·  ",
                    color = colors.borderSubtle,
                    fontWeight = FontWeight.Bold
                )

                Icon(
                    imageVector = FluentIcons.Location24Regular,
                    contentDescription = null,
                    tint = if (progressPercent > 0) colors.primary else colors.textSecondary,
                    modifier = Modifier.size(14.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(
                    text = if (isCompleted) "Finished" else "At page ${session.currentPageReached.coerceAtLeast(session.startPage)}",
                    fontFamily = InterFamily,
                    fontSize = 12.sp,
                    color = if (progressPercent > 0 && !isCompleted) colors.primary else colors.textSecondary,
                    fontWeight = if (progressPercent > 0) FontWeight.SemiBold else FontWeight.Normal
                )
            }

            if (session.microNote.isNotBlank()) {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = session.microNote,
                    fontFamily = InterFamily,
                    fontSize = 11.5.sp,
                    color = colors.textSecondary.copy(alpha = 0.85f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Interactive Progress-Filled Resume Button
            // The container fills horizontally with an accent fill proportional to completion percentage
            val buttonActionLabel = when {
                isCompleted -> "Review completed"
                progressPercent > 0 -> "Resume reading"
                else -> "Start reading"
            }
            val buttonSubtext = when {
                isCompleted -> "100% completed • Target reached"
                progressPercent > 0 -> "$progressPercent% completed"
                else -> "Ready to begin • ${session.totalPages} pages"
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(colors.surfaceTint.copy(alpha = 0.7f))
                    .border(1.dp, if (progressPercent > 0) colors.primary.copy(alpha = 0.35f) else colors.borderSubtle, RoundedCornerShape(14.dp))
                    .clickable { onReadClick() }
                    .testTag("btn_resume_reading_${session.id}")
            ) {
                // Background animated progress fill
                if (animatedProgress > 0f) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(animatedProgress)
                            .background(
                                if (isCompleted) colors.primary.copy(alpha = 0.20f)
                                else colors.primary.copy(alpha = 0.28f)
                            )
                    )
                }

                // Foreground Text and Action Icon
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(
                        modifier = Modifier.weight(1f),
                        verticalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = buttonActionLabel,
                            fontFamily = PoppinsFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.5.sp,
                            color = colors.textPrimary
                        )
                        Text(
                            text = buttonSubtext,
                            fontFamily = InterFamily,
                            fontSize = 11.sp,
                            color = if (progressPercent > 0) colors.primary else colors.textSecondary,
                            fontWeight = if (progressPercent > 0) FontWeight.Medium else FontWeight.Normal
                        )
                    }

                    // Upward-right arrow action icon
                    Surface(
                        shape = CircleShape,
                        color = colors.primary.copy(alpha = 0.15f),
                        modifier = Modifier.size(28.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = FluentIcons.ArrowUpRight24Regular,
                                contentDescription = "Open",
                                tint = colors.primary,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
