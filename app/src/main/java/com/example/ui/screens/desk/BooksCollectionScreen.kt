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

/**
 * Materials Folder / Collection Screen
 * Displays items for a specific material type (Books, Notes, PYQs, Handouts) or a Custom Collection.
 * Features:
 * - Dynamic subject filters derived strictly from the items present
 * - In-progress, bookmarked, finished cards removed
 * - In-progress, unread, finished chips removed
 * - Clean 2-column grid with shared book-action modal trigger
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BooksCollectionScreen(
    books: List<BookEntity>,
    title: String = "Books",
    parentCategory: String = "DOWNLOADED",
    onBackClick: () -> Unit,
    onBookClick: (BookEntity) -> Unit,
    onBookLongClick: (BookEntity) -> Unit,
    onAddBooksClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalMulberryColors.current
    var searchQuery by remember { mutableStateOf("") }
    var selectedSubject by remember { mutableStateOf("All") }

    // Dynamically derive subject filters from books present in this collection
    val availableSubjects = remember(books) {
        val distinct = books
            .map { it.subject.trim() }
            .filter { it.isNotBlank() }
            .distinct()
            .sorted()
        listOf("All") + distinct
    }

    // Filter books based on search query and selected subject
    val filteredBooks = remember(books, searchQuery, selectedSubject) {
        books.filter { book ->
            val matchesQuery = searchQuery.isBlank() ||
                    book.title.contains(searchQuery, ignoreCase = true) ||
                    book.author.contains(searchQuery, ignoreCase = true) ||
                    book.subject.contains(searchQuery, ignoreCase = true)

            val matchesSubject = selectedSubject == "All" ||
                    book.subject.equals(selectedSubject, ignoreCase = true)

            matchesQuery && matchesSubject
        }
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
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = "$parentCategory / $title",
                                fontFamily = InterFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 11.5.sp,
                                letterSpacing = 0.8.sp,
                                color = colors.primary
                            )
                            Text(
                                text = title,
                                fontFamily = PoppinsFamily,
                                fontWeight = FontWeight.Bold,
                                fontSize = 20.sp,
                                color = colors.textPrimary
                            )
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = colors.primary.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = "${filteredBooks.size} items",
                            fontFamily = PoppinsFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp,
                            color = colors.primary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                // Search input
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("collection_search_input"),
                    placeholder = {
                        Text(
                            text = "Search in $title...",
                            fontFamily = InterFamily,
                            fontSize = 13.sp,
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
                    trailingIcon = {
                        if (searchQuery.isNotBlank()) {
                            IconButton(onClick = { searchQuery = "" }, modifier = Modifier.size(24.dp)) {
                                Icon(
                                    imageVector = FluentIcons.Dismiss24Regular,
                                    contentDescription = "Clear",
                                    tint = colors.textMuted,
                                    modifier = Modifier.size(16.dp)
                                )
                            }
                        }
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

                // Dynamic Subject Filter Chips (derived dynamically from items present)
                if (availableSubjects.size > 1) {
                    Spacer(modifier = Modifier.height(10.dp))
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(availableSubjects) { subject ->
                            val isSelected = selectedSubject == subject
                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) colors.primary else colors.surfaceCard,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelected) colors.primary else colors.borderSubtle
                                ),
                                modifier = Modifier
                                    .clip(RoundedCornerShape(10.dp))
                                    .clickable { selectedSubject = subject }
                                    .testTag("subject_chip_$subject")
                            ) {
                                Text(
                                    text = subject,
                                    fontFamily = InterFamily,
                                    fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                                    fontSize = 12.sp,
                                    color = if (isSelected) colors.onPrimary else colors.textPrimary,
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
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
                    onClick = onAddBooksClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp)
                        .testTag("btn_add_more_books"),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = colors.primary,
                        contentColor = colors.onPrimary
                    )
                ) {
                    Icon(
                        imageVector = FluentIcons.ArrowDownload24Regular,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Browse online catalog for more",
                        fontFamily = PoppinsFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp
                    )
                }
            }
        }
    ) { innerPadding ->
        if (filteredBooks.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Icon(
                        imageVector = FluentIcons.Book24Filled,
                        contentDescription = null,
                        tint = colors.textMuted,
                        modifier = Modifier.size(48.dp)
                    )
                    Text(
                        text = if (searchQuery.isNotBlank() || selectedSubject != "All") "No matches found" else "No $title yet",
                        fontFamily = PoppinsFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        color = colors.textPrimary
                    )
                    Text(
                        text = if (searchQuery.isNotBlank() || selectedSubject != "All") "Try adjusting your search or subject filter." else "Download or add materials to access them here offline.",
                        fontFamily = InterFamily,
                        fontSize = 13.sp,
                        color = colors.textSecondary
                    )
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Fixed(2),
                contentPadding = PaddingValues(
                    start = 16.dp,
                    end = 16.dp,
                    top = 10.dp,
                    bottom = innerPadding.calculateBottomPadding() + 8.dp
                ),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = innerPadding.calculateTopPadding())
            ) {
                items(filteredBooks, key = { it.id }) { book ->
                    BooksGridCard(
                        book = book,
                        onClick = { onBookClick(book) },
                        onLongClick = { onBookLongClick(book) }
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun BooksGridCard(
    book: BookEntity,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val colors = LocalMulberryColors.current

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = colors.surfaceCard,
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.borderSubtle),
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
            .testTag("book_card_${book.id}")
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            // Book cover
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = colors.primary.copy(alpha = 0.12f),
                border = androidx.compose.foundation.BorderStroke(0.5.dp, colors.borderSubtle),
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
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = FluentIcons.Book24Filled,
                            contentDescription = null,
                            tint = colors.primary,
                            modifier = Modifier.size(32.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = book.title,
                fontFamily = PoppinsFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.5.sp,
                color = colors.textPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = book.author.ifEmpty { book.edition }.ifEmpty { "Medical" },
                fontFamily = InterFamily,
                fontSize = 11.sp,
                color = colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )

            Spacer(modifier = Modifier.height(6.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = colors.primary.copy(alpha = 0.12f)
                ) {
                    Text(
                        text = book.subject.ifEmpty { "General" },
                        fontFamily = InterFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 10.sp,
                        color = colors.primary,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                Text(
                    text = "${book.pageCount}p",
                    fontFamily = InterFamily,
                    fontSize = 10.5.sp,
                    color = colors.textMuted
                )
            }
        }
    }
}
