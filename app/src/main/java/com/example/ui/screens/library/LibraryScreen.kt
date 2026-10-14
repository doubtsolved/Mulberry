@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.ui.screens.library

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.local.entities.BookEntity
import com.example.ui.components.FluentIcons
import com.example.ui.theme.LocalMulberryColors
import com.example.viewmodel.EphemeralMulberryViewModel

val MBBS_19_SUBJECTS = listOf(
    "Anatomy", "Physiology", "Biochemistry",
    "Pathology", "Pharmacology", "Microbiology", "Forensic Medicine",
    "Community Medicine", "Ophthalmology", "ENT",
    "General Medicine", "General Surgery", "Obstetrics & Gynaecology",
    "Paediatrics", "Orthopaedics", "Dermatology", "Psychiatry",
    "Radiology", "Anaesthesiology"
)

val PROF_FILTERS = listOf("All", "1st Prof", "2nd Prof", "3rd Prof Part 1", "Final Prof")

@Composable
fun LibraryScreen(
    viewModel: EphemeralMulberryViewModel,
    modifier: Modifier = Modifier
) {
    val colors = LocalMulberryColors.current
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    val allBooks by viewModel.allCatalogBooks.collectAsState()
    val profFilter by viewModel.selectedProfFilter.collectAsState()
    val subjectFilter by viewModel.selectedSubjectFilter.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isSyncing by viewModel.isSyncingCatalog.collectAsState()
    val downloadProgressMap by viewModel.downloadProgressMap.collectAsState()

    var selectedBookForDetail by remember { mutableStateOf<BookEntity?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Filter books based on Prof, Subject, and Search
    val filteredBooks = remember(allBooks, profFilter, subjectFilter, searchQuery) {
        allBooks.filter { book ->
            val matchesProf = if (profFilter == "All") true else book.prof.equals(profFilter, ignoreCase = true)
            val matchesSubject = if (subjectFilter == null) true else book.subject.equals(subjectFilter, ignoreCase = true)
            val matchesSearch = if (searchQuery.isBlank()) true else {
                book.title.contains(searchQuery, ignoreCase = true) ||
                book.author.contains(searchQuery, ignoreCase = true) ||
                book.tagsCsv.contains(searchQuery, ignoreCase = true) ||
                book.subject.contains(searchQuery, ignoreCase = true)
            }
            matchesProf && matchesSubject && matchesSearch
        }
    }

    // High yield books for carousel
    val highYieldBooks = remember(allBooks, profFilter) {
        val pool = if (profFilter == "All") allBooks else allBooks.filter { it.prof.equals(profFilter, ignoreCase = true) }
        pool.take(8)
    }

    // Detail Bottom Sheet
    selectedBookForDetail?.let { book ->
        BookDetailBottomSheet(
            book = book,
            sheetState = sheetState,
            downloadProgress = downloadProgressMap[book.id],
            onDismiss = { selectedBookForDetail = null },
            onDownload = { viewModel.downloadBook(it) },
            onOpenInReader = { viewModel.openBookInReader(it) }
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        // App Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "Mulberry",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                    letterSpacing = (-0.5).sp
                )
                Text(
                    text = "Medical Catalog • 19 Subjects",
                    fontSize = 12.sp,
                    color = colors.textSecondary
                )
            }

            IconButton(
                onClick = { viewModel.syncCatalog() },
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .background(colors.surfaceCard)
            ) {
                if (isSyncing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = colors.primary,
                        strokeWidth = 2.dp
                    )
                } else {
                    Icon(
                        imageVector = FluentIcons.ArrowSync24Regular,
                        contentDescription = "Sync Catalog",
                        tint = colors.textSecondary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        // Search Bar
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { viewModel.setSearchQuery(it) },
            placeholder = {
                Text("Search Guyton, Robbins, Anatomy...", fontSize = 14.sp, color = colors.textMuted)
            },
            leadingIcon = {
                Icon(
                    imageVector = FluentIcons.Search24Regular,
                    contentDescription = "Search",
                    tint = colors.textSecondary,
                    modifier = Modifier.size(18.dp)
                )
            },
            trailingIcon = {
                if (searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.setSearchQuery("") }) {
                        Icon(
                            imageVector = FluentIcons.Dismiss24Regular,
                            contentDescription = "Clear",
                            tint = colors.textMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = colors.primary,
                unfocusedBorderColor = colors.borderSubtle,
                focusedContainerColor = colors.surfaceCard,
                unfocusedContainerColor = colors.surfaceCard,
                focusedTextColor = colors.textPrimary,
                unfocusedTextColor = colors.textPrimary
            ),
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 6.dp)
        )

        // Prof Filter Chips
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(PROF_FILTERS) { prof ->
                val isSelected = profFilter == prof
                FilterChip(
                    selected = isSelected,
                    onClick = { viewModel.setProfFilter(prof) },
                    label = {
                        Text(
                            text = prof,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                        )
                    },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = colors.primary,
                        selectedLabelColor = colors.onPrimary,
                        containerColor = colors.surfaceCard,
                        labelColor = colors.textSecondary
                    ),
                    border = FilterChipDefaults.filterChipBorder(
                        borderColor = colors.borderSubtle,
                        selectedBorderColor = colors.primary,
                        enabled = true,
                        selected = isSelected
                    ),
                    shape = RoundedCornerShape(8.dp)
                )
            }
        }

        // Main Scrollable Body
        LazyColumn(
            contentPadding = PaddingValues(bottom = 90.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            // High-Yield Vault Carousel (shown when no specific search query)
            if (searchQuery.isBlank() && highYieldBooks.isNotEmpty()) {
                item {
                    Text(
                        text = "High-Yield Vault",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary,
                        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 8.dp)
                    )

                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp),
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        items(highYieldBooks) { book ->
                            HighYieldBookCard(
                                book = book,
                                onClick = { selectedBookForDetail = book }
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))
                }
            }

            // 19-Subject Directory Grid (shown when no specific search query)
            if (searchQuery.isBlank()) {
                item {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "19-Subject Directory",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )

                        if (subjectFilter != null) {
                            Text(
                                text = "Clear filter",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.primary,
                                modifier = Modifier
                                    .clickable { viewModel.setSubjectFilter(null) }
                                    .padding(4.dp)
                            )
                        }
                    }

                    // 19 Subject Pills Grid
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 6.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        items(MBBS_19_SUBJECTS) { subject ->
                            val isSelected = subjectFilter == subject
                            val count = allBooks.count { it.subject.equals(subject, ignoreCase = true) }

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) colors.primary else colors.surfaceCard,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    if (isSelected) colors.primary else colors.borderSubtle
                                ),
                                modifier = Modifier.clickable {
                                    if (isSelected) viewModel.setSubjectFilter(null)
                                    else viewModel.setSubjectFilter(subject)
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = FluentIcons.Folder24Regular,
                                        contentDescription = null,
                                        tint = if (isSelected) colors.onPrimary else colors.accentWarning,
                                        modifier = Modifier.size(15.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = subject,
                                        fontSize = 12.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) colors.onPrimary else colors.textPrimary
                                    )
                                    if (count > 0) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Surface(
                                            shape = RoundedCornerShape(4.dp),
                                            color = if (isSelected) colors.onPrimary.copy(alpha = 0.2f) else colors.surfaceTint
                                        ) {
                                            Text(
                                                text = "$count",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                color = if (isSelected) colors.onPrimary else colors.primary,
                                                modifier = Modifier.padding(horizontal = 5.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(18.dp))
                }
            }

            // Resource Feed Section Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (subjectFilter != null) "$subjectFilter Resources" else "Curriculum Resources",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = colors.textPrimary
                    )
                    Text(
                        text = "${filteredBooks.size} books",
                        fontSize = 12.sp,
                        color = colors.textSecondary
                    )
                }
            }

            // Feed Items
            if (filteredBooks.isEmpty()) {
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
                                text = "No books found matching criteria",
                                fontSize = 14.sp,
                                color = colors.textSecondary
                            )
                        }
                    }
                }
            } else {
                items(filteredBooks, key = { it.id }) { book ->
                    BookFeedCard(
                        book = book,
                        isDownloading = downloadProgressMap.containsKey(book.id),
                        onClick = { selectedBookForDetail = book }
                    )
                }
            }
        }
    }
}

@Composable
fun HighYieldBookCard(
    book: BookEntity,
    onClick: () -> Unit
) {
    val colors = LocalMulberryColors.current
    val context = LocalContext.current

    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surfaceCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.borderSubtle),
        modifier = Modifier
            .width(136.dp)
            .clickable(onClick = onClick)
    ) {
        Column(modifier = Modifier.padding(8.dp)) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(3f / 4f)
                    .clip(RoundedCornerShape(10.dp))
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

                if (book.isDownloaded) {
                    Surface(
                        shape = RoundedCornerShape(bottomStart = 8.dp),
                        color = colors.accentSuccess,
                        modifier = Modifier.align(Alignment.TopEnd)
                    ) {
                        Text(
                            text = "SAVED",
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            color = androidx.compose.ui.graphics.Color.White,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = book.title,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 16.sp
            )

            Spacer(modifier = Modifier.height(2.dp))

            Text(
                text = book.subject,
                fontSize = 11.sp,
                color = colors.textSecondary,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
fun BookFeedCard(
    book: BookEntity,
    isDownloading: Boolean,
    onClick: () -> Unit
) {
    val colors = LocalMulberryColors.current
    val context = LocalContext.current

    Card(
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surfaceCard),
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.borderSubtle),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 5.dp)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Thumbnail
            Box(
                modifier = Modifier
                    .width(52.dp)
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

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = book.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "${book.author} • ${book.edition}",
                    fontSize = 12.sp,
                    color = colors.textSecondary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = colors.surfaceTint.copy(alpha = 0.6f)
                    ) {
                        Text(
                            text = book.subject,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = colors.primary,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }

                    Text(
                        text = "${book.totalPages} pgs • ${book.sizeMb} MB",
                        fontSize = 11.sp,
                        color = colors.textMuted
                    )
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Download Status icon
            if (isDownloading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = colors.primary,
                    strokeWidth = 2.dp
                )
            } else if (book.isDownloaded) {
                Icon(
                    imageVector = FluentIcons.Checkmark24Regular,
                    contentDescription = "Downloaded",
                    tint = colors.accentSuccess,
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Icon(
                    imageVector = FluentIcons.ArrowDownload24Regular,
                    contentDescription = "Not Downloaded",
                    tint = colors.textMuted,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
