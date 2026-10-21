package com.example.ui.screens.desk

import android.widget.Toast
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
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
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
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.BookEntity
import com.example.data.model.UserProfile
import com.example.ui.components.FluentIcons
import com.example.ui.components.MulberryExpressiveLogo
import com.example.ui.theme.InterFamily
import com.example.ui.theme.LocalMulberryColors
import com.example.ui.theme.PoppinsFamily
import com.example.util.MulberryHistoryManager
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/**
 * Desk (Home Dashboard) Screen (Wireframe 01)
 * Purpose: Help users resume reading, check weekly progress, and discover recently added resources.
 */
@Composable
fun DeskDashboardScreen(
    downloadedBooks: List<BookEntity>,
    userProfile: UserProfile,
    onOpenBook: (BookEntity) -> Unit,
    onBookCardClick: (BookEntity) -> Unit,
    onViewPlansClick: () -> Unit,
    onQuickAccessMyLibrary: () -> Unit,
    onQuickAccessStudyPlanner: () -> Unit,
    onProfileClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = LocalMulberryColors.current
    var showCoffeeDialog by remember { mutableStateOf(false) }

    // Date Header formatted like "TUESDAY, OCTOBER 20"
    val dateHeaderStr = remember {
        val sdf = SimpleDateFormat("EEEE, MMMM d", Locale.getDefault())
        sdf.format(Date()).uppercase()
    }

    // Recently opened books derived purely from /sdcard/Mulberry/.history/activity_events.json
    val recentHistoryBooks = remember(downloadedBooks) {
        MulberryHistoryManager.getRecentlyOpenedBooks(context, downloadedBooks)
    }

    // Determine last read book for "PICK UP WHERE YOU LEFT OFF" purely from history
    val resumeBook = remember(recentHistoryBooks, downloadedBooks) {
        recentHistoryBooks.firstOrNull() ?: downloadedBooks.firstOrNull()
    }

    // Recently opened / added books from history file
    val recentlyAddedBooks = remember(recentHistoryBooks, downloadedBooks) {
        if (recentHistoryBooks.isNotEmpty()) recentHistoryBooks.take(6) else downloadedBooks.take(6)
    }

    // Weekly reading calculation tracked solely from book reading history ledger
    val weeklyMinutesRead = remember {
        MulberryHistoryManager.calculateWeeklyReadingMinutes(context)
    }
    val weeklyTargetHours = 7.0
    val weeklyHoursRead = remember(weeklyMinutesRead) {
        String.format(Locale.US, "%.1f", (weeklyMinutesRead / 60.0f))
    }
    val progressPercent = remember(weeklyMinutesRead) {
        ((weeklyMinutesRead.toFloat() / (weeklyTargetHours * 60f)) * 100f).toInt().coerceIn(0, 100)
    }
    val weeklyDaysActive = remember {
        MulberryHistoryManager.getWeeklyDayActivity(context)
    }

    val currentCal = Calendar.getInstance()
    val todayDayOfWeek = currentCal.get(Calendar.DAY_OF_WEEK) // SUNDAY=1, MONDAY=2, etc.
    val todayDateNumber = currentCal.get(Calendar.DAY_OF_MONTH)

    val dayNames = listOf("M", "T", "W", "T", "F", "S", "S")
    // M=2, T=3, W=4, T=5, F=6, S=7, S=1
    val dayOfWeekIndices = listOf(Calendar.MONDAY, Calendar.TUESDAY, Calendar.WEDNESDAY, Calendar.THURSDAY, Calendar.FRIDAY, Calendar.SATURDAY, Calendar.SUNDAY)

    Scaffold(
        modifier = modifier
            .fillMaxSize()
            .background(colors.backgroundGradient),
        containerColor = Color.Transparent,
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    MulberryExpressiveLogo(size = 32.dp, showContainer = false)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Desk",
                        fontFamily = PoppinsFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 24.sp,
                        color = colors.textPrimary,
                        letterSpacing = (-0.5).sp
                    )
                }

                // Profile Avatar Button
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(colors.primary.copy(alpha = 0.14f))
                        .border(1.dp, colors.primary.copy(alpha = 0.35f), RoundedCornerShape(12.dp))
                        .clickable { onProfileClick() }
                        .testTag("desk_avatar_button"),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = getInitials(userProfile.displayName),
                        fontFamily = PoppinsFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.5.sp,
                        color = colors.primary
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
                verticalArrangement = Arrangement.spacedBy(18.dp)
            ) {
            // 1. DATE HEADER
            item(key = "desk_date_header") {
                Text(
                    text = dateHeaderStr,
                    fontFamily = InterFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    letterSpacing = 1.2.sp,
                    color = colors.textMuted
                )
            }

            // 2. RESUME READING CARD (DOMINANT ELEMENT)
            item(key = "resume_reading_card") {
                if (resumeBook != null) {
                    val totalPages = resumeBook.pageCount.coerceAtLeast(1)
                    val lastPage = resumeBook.lastReadPage.coerceIn(1, totalPages)
                    val bookPercent = if (resumeBook.progressPercent > 0) {
                        resumeBook.progressPercent
                    } else {
                        ((lastPage.toFloat() / totalPages.toFloat()) * 100).toInt().coerceIn(0, 100)
                    }

                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = colors.surfaceCard,
                        border = androidx.compose.foundation.BorderStroke(1.dp, colors.primary.copy(alpha = 0.35f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(22.dp))
                            .clickable { onBookCardClick(resumeBook) }
                            .testTag("card_resume_reading")
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
                        ) {
                            Text(
                                text = "PICK UP WHERE YOU LEFT OFF",
                                fontFamily = InterFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                letterSpacing = 1.sp,
                                color = colors.primary
                            )

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Surface(
                                    shape = RoundedCornerShape(10.dp),
                                    color = colors.primary.copy(alpha = 0.12f),
                                    modifier = Modifier
                                        .size(width = 54.dp, height = 72.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                ) {
                                    if (resumeBook.coverUrl.isNotBlank()) {
                                        AsyncImage(
                                            model = resumeBook.coverUrl,
                                            contentDescription = resumeBook.title,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    } else {
                                        Box(contentAlignment = Alignment.Center) {
                                            Icon(
                                                imageVector = FluentIcons.Book24Filled,
                                                contentDescription = null,
                                                tint = colors.primary,
                                                modifier = Modifier.size(28.dp)
                                            )
                                        }
                                    }
                                }

                                Spacer(modifier = Modifier.width(14.dp))

                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = resumeBook.title,
                                        fontFamily = PoppinsFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.sp,
                                        color = colors.textPrimary,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = if (lastPage > 1) "Chapter 04 · Page $lastPage ($bookPercent%)" else "Page 1 · Ready to read",
                                        fontFamily = InterFamily,
                                        fontSize = 13.sp,
                                        color = colors.textSecondary
                                    )
                                }
                            }

                            // Primary CTA with background progress indicator
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(46.dp)
                                    .clip(RoundedCornerShape(14.dp))
                                    .background(colors.surface)
                                    .border(1.dp, colors.primary.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                                    .clickable { onOpenBook(resumeBook) }
                                    .testTag("btn_resume_reading_cta")
                            ) {
                                // Progress bar fill inside button
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(bookPercent.toFloat() / 100f)
                                        .height(46.dp)
                                        .background(colors.primary.copy(alpha = 0.22f))
                                )

                                Row(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(horizontal = 16.dp),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "RESUME READING",
                                        fontFamily = PoppinsFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 13.5.sp,
                                        color = colors.primary,
                                        letterSpacing = 0.5.sp
                                    )
                                    Icon(
                                        imageVector = FluentIcons.Open24Regular,
                                        contentDescription = "Resume",
                                        tint = colors.primary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }
                } else {
                    // Empty reading state
                    Surface(
                        shape = RoundedCornerShape(22.dp),
                        color = colors.surfaceCard,
                        border = androidx.compose.foundation.BorderStroke(1.dp, colors.borderSubtle),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                text = "START YOUR FIRST READING SESSION",
                                fontFamily = InterFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 11.sp,
                                letterSpacing = 1.sp,
                                color = colors.primary
                            )
                            Text(
                                text = "Choose a textbook or note from your library to begin reading and tracking your progress.",
                                fontFamily = InterFamily,
                                fontSize = 13.sp,
                                color = colors.textSecondary
                            )
                        }
                    }
                }
            }

            // 3. QUICK ACCESS SHORTCUTS (Moved directly after Pick up where you left off)
            item(key = "quick_access_header") {
                Text(
                    text = "QUICK ACCESS",
                    fontFamily = PoppinsFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 12.sp,
                    letterSpacing = 0.8.sp,
                    color = colors.primary
                )
            }

            item(key = "quick_access_cards") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Shortcut 1: My Library
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = colors.surfaceCard,
                        border = androidx.compose.foundation.BorderStroke(1.dp, colors.borderSubtle),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(18.dp))
                            .clickable { onQuickAccessMyLibrary() }
                            .testTag("quick_access_my_library")
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(colors.primary.copy(alpha = 0.14f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = FluentIcons.Book24Filled,
                                    contentDescription = null,
                                    tint = colors.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "My Library",
                                    fontFamily = PoppinsFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = colors.textPrimary
                                )
                                Text(
                                    text = "Books & files",
                                    fontFamily = InterFamily,
                                    fontSize = 12.sp,
                                    color = colors.textSecondary
                                )
                            }
                        }
                    }

                    // Shortcut 2: Study Planner
                    Surface(
                        shape = RoundedCornerShape(18.dp),
                        color = colors.surfaceCard,
                        border = androidx.compose.foundation.BorderStroke(1.dp, colors.borderSubtle),
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(18.dp))
                            .clickable { onQuickAccessStudyPlanner() }
                            .testTag("quick_access_study_planner")
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(38.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(colors.primary.copy(alpha = 0.14f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = FluentIcons.CalendarLtr24Filled,
                                    contentDescription = null,
                                    tint = colors.primary,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Column {
                                Text(
                                    text = "Study Planner",
                                    fontFamily = PoppinsFamily,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = colors.textPrimary
                                )
                                Text(
                                    text = "Schedule & goals",
                                    fontFamily = InterFamily,
                                    fontSize = 12.sp,
                                    color = colors.textSecondary
                                )
                            }
                        }
                    }
                }
            }

            // 4. YOUR WEEK CARD
            item(key = "your_week_card") {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "YOUR WEEK",
                            fontFamily = PoppinsFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            letterSpacing = 0.8.sp,
                            color = colors.primary
                        )

                        TextButton(
                            onClick = onViewPlansClick,
                            contentPadding = PaddingValues(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "View plans →",
                                fontFamily = InterFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 13.sp,
                                color = colors.primary
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = colors.surfaceCard,
                        border = androidx.compose.foundation.BorderStroke(1.dp, colors.borderSubtle),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { onViewPlansClick() }
                            .testTag("card_your_week")
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(14.dp)
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
                                        text = "$weeklyHoursRead / ${weeklyTargetHours.toInt()} hours",
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
                                        text = "$progressPercent%",
                                        fontFamily = PoppinsFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.sp,
                                        color = colors.primary,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                    )
                                }
                            }

                            // 7-day strip (M T W T F S S)
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                dayNames.forEachIndexed { idx, name ->
                                    val dayCalCode = dayOfWeekIndices[idx]
                                    val isToday = dayCalCode == todayDayOfWeek
                                    val isCompletedOnDay = weeklyDaysActive.getOrElse(idx) { false }

                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Text(
                                            text = name,
                                            fontFamily = InterFamily,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 11.sp,
                                            color = if (isToday) colors.primary else colors.textMuted
                                        )

                                        Box(
                                            modifier = Modifier
                                                .size(28.dp)
                                                .clip(CircleShape)
                                                .background(if (isToday) colors.primary.copy(alpha = 0.15f) else Color.Transparent)
                                                .border(
                                                    width = if (isToday) 1.5.dp else 0.5.dp,
                                                    color = if (isToday) colors.primary else colors.borderSubtle,
                                                    shape = CircleShape
                                                ),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(
                                                text = when {
                                                    isCompletedOnDay && !isToday -> "✓"
                                                    isToday -> if (isCompletedOnDay) "✓" else todayDateNumber.toString()
                                                    else -> "○"
                                                },
                                                fontFamily = InterFamily,
                                                fontWeight = if (isToday || isCompletedOnDay) FontWeight.Bold else FontWeight.Normal,
                                                fontSize = if (isToday) 11.5.sp else 11.sp,
                                                color = if (isToday || isCompletedOnDay) colors.primary else colors.textMuted
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 4. RECENTLY ADDED
            if (recentlyAddedBooks.isNotEmpty()) {
                item(key = "recently_added_header") {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "RECENTLY ADDED",
                            fontFamily = PoppinsFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            letterSpacing = 0.8.sp,
                            color = colors.primary
                        )
                    }
                }

                item(key = "recently_added_carousel") {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(recentlyAddedBooks, key = { "add_${it.id}" }) { book ->
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = colors.surfaceCard,
                                border = androidx.compose.foundation.BorderStroke(1.dp, colors.borderSubtle),
                                modifier = Modifier
                                    .width(135.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable { onBookCardClick(book) }
                                    .testTag("recent_book_card_${book.id}")
                            ) {
                                Column(modifier = Modifier.padding(10.dp)) {
                                    Surface(
                                        shape = RoundedCornerShape(10.dp),
                                        color = colors.primary.copy(alpha = 0.1f),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .aspectRatio(0.75f)
                                            .clip(RoundedCornerShape(10.dp))
                                    ) {
                                        if (book.coverUrl.isNotBlank()) {
                                            AsyncImage(
                                                model = book.coverUrl,
                                                contentDescription = book.title,
                                                contentScale = ContentScale.Crop,
                                                modifier = Modifier.fillMaxSize()
                                            )
                                        } else {
                                            Box(contentAlignment = Alignment.Center) {
                                                Icon(
                                                    imageVector = FluentIcons.Book24Filled,
                                                    contentDescription = null,
                                                    tint = colors.primary,
                                                    modifier = Modifier.size(28.dp)
                                                )
                                            }
                                        }
                                    }

                                    Spacer(modifier = Modifier.height(8.dp))

                                    Text(
                                        text = book.title,
                                        fontFamily = PoppinsFamily,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 13.sp,
                                        color = colors.textPrimary,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = book.subject.ifEmpty { "Medical" },
                                        fontFamily = InterFamily,
                                        fontSize = 11.sp,
                                        color = colors.textSecondary,
                                        maxLines = 1
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 5. DONATION PROMPT
            item(key = "donation_card") {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = colors.surfaceCard.copy(alpha = 0.7f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.borderSubtle),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .clickable { showCoffeeDialog = true }
                        .testTag("card_coffee_donation")
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(text = "☕", fontSize = 24.sp)
                        Spacer(modifier = Modifier.width(14.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "Keep Mulberry growing",
                                fontFamily = PoppinsFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = colors.textPrimary
                            )
                            Text(
                                text = "Support this free app & fuel late-night revisions",
                                fontFamily = InterFamily,
                                fontSize = 12.sp,
                                color = colors.textSecondary
                            )
                        }
                    }
                }
            }

            item(key = "desk_bottom_space") {
                Spacer(modifier = Modifier.height(24.dp))
            }
        }
    }
}

    // Humorous Donation Modal
    if (showCoffeeDialog) {
        AlertDialog(
            onDismissRequest = { showCoffeeDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = "☕", fontSize = 22.sp)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Fuel Mulberry's Caffeine",
                        fontFamily = PoppinsFamily,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Medical school runs on caffeine, glucose, and sheer determination.",
                        fontFamily = InterFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 13.5.sp,
                        color = colors.textPrimary
                    )
                    Text(
                        text = "Mulberry is 100% free, private, and open for students worldwide. Buying a coffee helps maintain high-speed CDN sync and zero-latency PDF rendering!",
                        fontFamily = InterFamily,
                        fontSize = 12.5.sp,
                        color = colors.textSecondary
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        showCoffeeDialog = false
                        Toast.makeText(context, "Thank you for supporting Mulberry! ❤️", Toast.LENGTH_LONG).show()
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
                ) {
                    Text("Support with Coffee", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCoffeeDialog = false }) {
                    Text("Maybe Later", color = colors.textSecondary)
                }
            },
            containerColor = colors.surfaceCard
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
