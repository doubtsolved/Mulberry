@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.ui.screens.desk

import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.local.entities.BookEntity
import com.example.data.local.entities.DailyScheduleEntity
import com.example.ui.components.FluentIcons
import com.example.ui.theme.LocalMulberryColors
import com.example.viewmodel.EphemeralMulberryViewModel

@Composable
fun DeskScreen(
    viewModel: EphemeralMulberryViewModel,
    modifier: Modifier = Modifier
) {
    val colors = LocalMulberryColors.current
    val context = LocalContext.current

    val shelfBooks by viewModel.shelfBooks.collectAsState()
    val todaySchedules by viewModel.todaySchedules.collectAsState()
    val allBooks by viewModel.allCatalogBooks.collectAsState()

    var showSchedulerSheet by remember { mutableStateOf(false) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    if (showSchedulerSheet) {
        SchedulerBottomSheet(
            downloadedBooks = if (shelfBooks.isNotEmpty()) shelfBooks else allBooks,
            sheetState = sheetState,
            onDismiss = { showSchedulerSheet = false },
            onScheduleAdded = { bookId, bookTitle, chapter, startPage, endPage ->
                viewModel.addDailySchedule(bookId, bookTitle, chapter, startPage, endPage)
            }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        // Desk Top Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Study Desk",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                    letterSpacing = (-0.5).sp
                )
                Text(
                    text = "Today's Focus • Downloaded Shelf",
                    fontSize = 12.sp,
                    color = colors.textSecondary
                )
            }

            IconButton(
                onClick = { showSchedulerSheet = true },
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(colors.surfaceCard)
            ) {
                Icon(
                    imageVector = FluentIcons.Add24Regular,
                    contentDescription = "Add Goal",
                    tint = colors.primary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        LazyColumn(
            contentPadding = PaddingValues(bottom = 90.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // Section 1: Today's Focus Carousel
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Today's Focus",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )

                    val completedCount = todaySchedules.count { it.isCompleted }
                    if (todaySchedules.isNotEmpty()) {
                        Text(
                            text = "$completedCount of ${todaySchedules.size} completed",
                            fontSize = 12.sp,
                            color = colors.textSecondary
                        )
                    }
                }

                if (todaySchedules.isEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = colors.surfaceCard,
                        border = androidx.compose.foundation.BorderStroke(1.dp, colors.borderSubtle),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 6.dp)
                            .clickable { showSchedulerSheet = true }
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = FluentIcons.CalendarLtr24Regular,
                                contentDescription = null,
                                tint = colors.primary,
                                modifier = Modifier.size(28.dp)
                            )
                            Spacer(modifier = Modifier.width(14.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "No reading goals scheduled today",
                                    fontSize = 14.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = colors.textPrimary
                                )
                                Text(
                                    text = "Tap to set a chapter goal from your textbooks",
                                    fontSize = 12.sp,
                                    color = colors.textMuted
                                )
                            }
                            Icon(
                                imageVector = FluentIcons.Add24Regular,
                                contentDescription = "Add",
                                tint = colors.primary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                } else {
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(todaySchedules, key = { it.id }) { schedule ->
                            ScheduleFocusCard(
                                schedule = schedule,
                                onToggleCompleted = { isDone ->
                                    viewModel.toggleScheduleCompleted(schedule.id, isDone)
                                },
                                onOpenInReader = {
                                    val matchedBook = allBooks.find { it.id == schedule.bookId }
                                    if (matchedBook != null) {
                                        viewModel.openBookInReader(matchedBook, schedule.startPage)
                                    }
                                },
                                onDelete = { viewModel.deleteSchedule(schedule.id) }
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))
            }

            // Section 2: The Shelf (Downloaded Books)
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "The Shelf (Offline Vault)",
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )

                    Text(
                        text = "${shelfBooks.size} books stored",
                        fontSize = 12.sp,
                        color = colors.textSecondary
                    )
                }
            }

            if (shelfBooks.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 40.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                imageVector = FluentIcons.Book24Regular,
                                contentDescription = null,
                                tint = colors.textMuted,
                                modifier = Modifier.size(48.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "Your Shelf is empty",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Download books from the Library to read offline",
                                fontSize = 13.sp,
                                color = colors.textSecondary
                            )
                        }
                    }
                }
            } else {
                items(shelfBooks, key = { it.id }) { book ->
                    ShelfBookCard(
                        book = book,
                        onRead = { viewModel.openBookInReader(book) },
                        onDelete = { viewModel.deleteDownloadedBook(book.id) }
                    )
                }
            }
        }
    }
}

@Composable
fun ScheduleFocusCard(
    schedule: DailyScheduleEntity,
    onToggleCompleted: (Boolean) -> Unit,
    onOpenInReader: () -> Unit,
    onDelete: () -> Unit
) {
    val colors = LocalMulberryColors.current

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surfaceCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.borderSubtle),
        modifier = Modifier.width(220.dp)
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Checkbox(
                    checked = schedule.isCompleted,
                    onCheckedChange = onToggleCompleted,
                    colors = CheckboxDefaults.colors(
                        checkedColor = colors.accentSuccess,
                        uncheckedColor = colors.borderSubtle
                    ),
                    modifier = Modifier.size(24.dp)
                )

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        imageVector = FluentIcons.Dismiss24Regular,
                        contentDescription = "Delete",
                        tint = colors.textMuted,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = schedule.targetChapter,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textDecoration = if (schedule.isCompleted) TextDecoration.LineThrough else TextDecoration.None
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = schedule.bookTitle,
                fontSize = 12.sp,
                color = colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Pages ${schedule.startPage} – ${schedule.endPage} (${schedule.totalPages} pgs ~ ${schedule.estimatedMinutes}m)",
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                color = colors.primary
            )

            Spacer(modifier = Modifier.height(12.dp))

            Button(
                onClick = onOpenInReader,
                colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                shape = RoundedCornerShape(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(36.dp)
            ) {
                Text(
                    text = "Open Reader",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.onPrimary
                )
            }
        }
    }
}

@Composable
fun ShelfBookCard(
    book: BookEntity,
    onRead: () -> Unit,
    onDelete: () -> Unit
) {
    val colors = LocalMulberryColors.current
    val context = LocalContext.current

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surfaceCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.borderSubtle),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .clickable(onClick = onRead)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Book Cover
            Box(
                modifier = Modifier
                    .width(60.dp)
                    .aspectRatio(3f / 4f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(colors.surfaceTint)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(book.coverUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = book.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.matchParentSize()
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = book.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "${book.subject} • ${book.edition}",
                    fontSize = 12.sp,
                    color = colors.textSecondary
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Progress Bar & Page stats
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Page ${book.lastReadPage} of ${book.totalPages}",
                        fontSize = 11.sp,
                        color = colors.textMuted
                    )
                    Text(
                        text = "${book.readingProgressPercent}%",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.primary
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                LinearProgressIndicator(
                    progress = { (book.readingProgressPercent / 100f).coerceIn(0f, 1f) },
                    color = colors.primary,
                    trackColor = colors.borderSubtle,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            IconButton(onClick = onDelete) {
                Icon(
                    imageVector = FluentIcons.Dismiss24Regular,
                    contentDescription = "Remove from Shelf",
                    tint = colors.textMuted,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
