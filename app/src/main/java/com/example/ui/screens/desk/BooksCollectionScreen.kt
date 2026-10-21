package com.example.ui.screens.desk

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.ExperimentalFoundationApi
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
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
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
import com.example.ui.components.FluentIcons
import com.example.ui.theme.InterFamily
import com.example.ui.theme.LocalMulberryColors
import com.example.ui.theme.PoppinsFamily

enum class BookFilterChip(val label: String) {
    ALL("All books"),
    IN_PROGRESS("In progress"),
    UNREAD("Unread"),
    FINISHED("Finished")
}

/**
 * Books Collection Screen (Wireframe 03)
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BooksCollectionScreen(
    books: List<BookEntity>,
    onBackClick: () -> Unit,
    onBookClick: (BookEntity) -> Unit,
    onBookLongClick: (BookEntity) -> Unit,
    onAddBooksClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalMulberryColors.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedFilter by remember { mutableStateOf(BookFilterChip.ALL) }

    // Metrics calculations
    val inProgressCount = books.count { it.lastReadPage > 1 && it.lastReadPage < it.pageCount }
    val bookmarkedCount = books.count { it.isFavorite }
    val finishedCount = books.count { it.pageCount > 0 && it.lastReadPage >= it.pageCount }

    // Filter books
    val filteredBooks = books.filter { book ->
        val matchesQuery = searchQuery.isBlank() ||
                book.title.contains(searchQuery, ignoreCase = true) ||
                book.author.contains(searchQuery, ignoreCase = true) ||
                book.subject.contains(searchQuery, ignoreCase = true)

        val matchesFilter = when (selectedFilter) {
            BookFilterChip.ALL -> true
            BookFilterChip.IN_PROGRESS -> book.lastReadPage > 1 && book.lastReadPage < book.pageCount
            BookFilterChip.UNREAD -> book.lastReadPage <= 1
            BookFilterChip.FINISHED -> book.pageCount > 0 && book.lastReadPage >= book.pageCount
        }

        matchesQuery && matchesFilter
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
                    .padding(horizontal = 16.dp, vertical = 8.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { onBackClick() }
                            .padding(vertical = 4.dp, horizontal = 2.dp)
                    ) {
                        Icon(
                            imageVector = FluentIcons.ArrowLeft24Regular,
                            contentDescription = "Back",
                            tint = colors.textPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "MY LIBRARY / MATERIALS",
                            fontFamily = InterFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.5.sp,
                            letterSpacing = 0.8.sp,
                            color = colors.textMuted
                        )
                    }

                    Row {
                        IconButton(onClick = {}, modifier = Modifier.size(36.dp)) {
                            Icon(
                                imageVector = FluentIcons.MoreVertical24Regular,
                                contentDescription = "More",
                                tint = colors.textPrimary,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = "Books",
                    fontFamily = PoppinsFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 26.sp,
                    color = colors.textPrimary
                )
                Text(
                    text = "Your bookshelf • ${books.size} books",
                    fontFamily = InterFamily,
                    fontSize = 13.sp,
                    color = colors.textSecondary
                )
            }
        },
        bottomBar = {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(horizontal = 16.dp, vertical = 10.dp),
                color = Color.Transparent
            ) {
                Button(
                    onClick = onAddBooksClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("btn_books_add_more"),
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
                        text = "Add books from Library",
                        fontFamily = PoppinsFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.5.sp
                    )
                }
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Summary Metrics Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                MetricSummaryCard(
                    count = inProgressCount,
                    label = "In progress",
                    modifier = Modifier.weight(1f)
                )
                MetricSummaryCard(
                    count = bookmarkedCount,
                    label = "Bookmarked",
                    modifier = Modifier.weight(1f)
                )
                MetricSummaryCard(
                    count = finishedCount,
                    label = "Finished",
                    modifier = Modifier.weight(1f)
                )
            }

            // Search Bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .testTag("books_search_input"),
                placeholder = {
                    Text(
                        text = "Search books...",
                        fontFamily = InterFamily,
                        fontSize = 13.5.sp,
                        color = colors.textMuted
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = FluentIcons.Search24Regular,
                        contentDescription = null,
                        tint = colors.textSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                },
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = colors.primary,
                    unfocusedBorderColor = colors.borderSubtle,
                    focusedContainerColor = colors.surfaceCard,
                    unfocusedContainerColor = colors.surfaceCard
                ),
                singleLine = true
            )

            // Filter Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(BookFilterChip.entries) { chip ->
                    val isSelected = selectedFilter == chip
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = if (isSelected) colors.primary.copy(alpha = 0.2f) else colors.surfaceCard,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (isSelected) colors.primary else colors.borderSubtle
                        ),
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .clickable { selectedFilter = chip }
                            .testTag("filter_chip_${chip.name}")
                    ) {
                        Text(
                            text = chip.label,
                            fontFamily = InterFamily,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 12.5.sp,
                            color = if (isSelected) colors.primary else colors.textSecondary,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
                        )
                    }
                }
            }

            // Books Grid
            if (filteredBooks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(
                            imageVector = FluentIcons.Book24Regular,
                            contentDescription = null,
                            tint = colors.textMuted,
                            modifier = Modifier.size(40.dp)
                        )
                        Text(
                            text = if (searchQuery.isNotBlank()) "No books match \"$searchQuery\"" else "No books in this category",
                            fontFamily = PoppinsFamily,
                            fontSize = 14.sp,
                            color = colors.textSecondary
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    items(filteredBooks, key = { it.id }) { book ->
                        BookShelfGridCard(
                            book = book,
                            onClick = { onBookClick(book) },
                            onLongClick = { onBookLongClick(book) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MetricSummaryCard(
    count: Int,
    label: String,
    modifier: Modifier = Modifier
) {
    val colors = LocalMulberryColors.current
    Surface(
        shape = RoundedCornerShape(14.dp),
        color = colors.surfaceCard,
        border = androidx.compose.foundation.BorderStroke(0.5.dp, colors.borderSubtle),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = count.toString(),
                fontFamily = PoppinsFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = colors.primary
            )
            Spacer(modifier = Modifier.height(2.dp))
            Text(
                text = label,
                fontFamily = InterFamily,
                fontSize = 11.sp,
                color = colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BookShelfGridCard(
    book: BookEntity,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val colors = LocalMulberryColors.current
    val totalPages = book.pageCount.coerceAtLeast(1)
    val currentPage = book.lastReadPage.coerceIn(0, totalPages)
    val progressPercent = if (book.progressPercent > 0) {
        book.progressPercent
    } else if (currentPage > 0) {
        ((currentPage.toFloat() / totalPages.toFloat()) * 100f).toInt().coerceIn(0, 100)
    } else 0

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = colors.surfaceCard,
        border = androidx.compose.foundation.BorderStroke(0.5.dp, colors.borderSubtle),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .testTag("book_shelf_card_${book.id}")
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            // Book Cover Aspect Ratio
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = colors.primary.copy(alpha = 0.1f),
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.72f)
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
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = FluentIcons.Book24Filled,
                            contentDescription = null,
                            tint = colors.primary,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            Text(
                text = book.title,
                fontFamily = PoppinsFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.5.sp,
                color = colors.textPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(3.dp))

            Text(
                text = if (currentPage > 1) "PDF · Pg $currentPage" else "PDF · New",
                fontFamily = InterFamily,
                fontSize = 11.5.sp,
                color = colors.textSecondary
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Reading Progress Bar
            LinearProgressIndicator(
                progress = { (progressPercent.toFloat() / 100f).coerceIn(0f, 1f) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = colors.primary,
                trackColor = colors.borderSubtle
            )
        }
    }
}
