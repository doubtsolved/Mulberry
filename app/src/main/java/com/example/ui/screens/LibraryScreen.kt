@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.ui.screens

import android.content.res.Configuration
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.togetherWith
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.example.data.model.BookEntity
import com.example.ui.components.FluentIcons
import com.example.ui.reader.ReaderActivity
import com.example.ui.theme.InterFamily
import com.example.ui.theme.LocalMulberryColors
import com.example.ui.theme.PoppinsFamily
import com.example.util.DownloadProgressState
import com.example.viewmodel.MulberryViewModel
import java.io.File

/**
 * MBBS Curriculum definitions for all 4 Prof years and official 19 subjects.
 */
data class ProfCurriculum(
    val id: String,
    val title: String,
    val shortName: String,
    val subjects: List<String>
)

val MBBS_CURRICULUM = listOf(
    ProfCurriculum(
        id = "1st Prof",
        title = "1st Prof (1st Year)",
        shortName = "1st Prof",
        subjects = listOf("Anatomy", "Physiology", "Biochemistry")
    ),
    ProfCurriculum(
        id = "2nd Prof",
        title = "2nd Prof (2nd Year)",
        shortName = "2nd Prof",
        subjects = listOf("Pathology", "Pharmacology", "Microbiology")
    ),
    ProfCurriculum(
        id = "3rd Prof Part 1",
        title = "3rd Prof Part 1 (3rd Year)",
        shortName = "3rd Prof Part 1",
        subjects = listOf("Forensic Medicine", "Community Medicine", "Ophthalmology", "ENT")
    ),
    ProfCurriculum(
        id = "Final Prof",
        title = "Final Prof (4th Year)",
        shortName = "Final Prof",
        subjects = listOf(
            "General Medicine",
            "General Surgery",
            "Obstetrics & Gynaecology",
            "Paediatrics",
            "Orthopaedics",
            "Dermatology",
            "Psychiatry",
            "Radiology",
            "Anaesthesiology"
        )
    )
)

/**
 * The exact 5 official catalog resource types from catalog.json
 */
val OFFICIAL_CATALOG_TYPES = listOf(
    "Standard Textbook",
    "Handwritten Notes",
    "Review / High-Yield",
    "Atlas / Practical",
    "Question Bank (PYQ)"
)

/**
 * Matches a book's type property against the official catalog types.
 */
fun matchesCatalogType(book: BookEntity, targetType: String): Boolean {
    val bType = book.type.trim()
    val bTags = book.tagsCsv.lowercase().trim()

    if (bType.equals(targetType, ignoreCase = true)) return true

    return when (targetType) {
        "Standard Textbook" -> bType.contains("Standard", ignoreCase = true) || (bTags.contains("standard textbook") && !bType.contains("Handwritten", ignoreCase = true))
        "Handwritten Notes" -> bType.contains("Handwritten", ignoreCase = true) || bTags.contains("handwritten")
        "Atlas / Practical" -> bType.contains("Atlas", ignoreCase = true) || bType.contains("Practical", ignoreCase = true) || bTags.contains("atlas") || bTags.contains("practical")
        "Review / High-Yield" -> bType.contains("Review", ignoreCase = true) || bType.contains("High-Yield", ignoreCase = true) || bTags.contains("high-yield") || bTags.contains("review")
        "Question Bank (PYQ)" -> bType.contains("Question", ignoreCase = true) || bType.contains("PYQ", ignoreCase = true) || bTags.contains("pyq") || bTags.contains("question bank")
        else -> false
    }
}

@Composable
fun LibraryScreen(
    viewModel: MulberryViewModel,
    modifier: Modifier = Modifier
) {
    val colors = LocalMulberryColors.current
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current

    val books by viewModel.allBooks.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    val downloadState by viewModel.downloadState.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()

    val selectedProf = remember(userProfile.selectedProf) {
        MBBS_CURRICULUM.find { it.id.equals(userProfile.selectedProf, ignoreCase = true) }
            ?: MBBS_CURRICULUM[0]
    }

    var searchQuery by remember { mutableStateOf("") }
    var selectedSubject by remember { mutableStateOf("All") }
    var selectedBookId by remember { mutableStateOf<String?>(null) }
    var expandedCategory by remember { mutableStateOf<String?>(null) }

    val bookSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Validate selectedSubject when selectedProf changes
    LaunchedEffect(selectedProf) {
        if (selectedSubject != "All" && selectedSubject !in selectedProf.subjects) {
            selectedSubject = "All"
        }
    }

    // Handle back button when inside an expanded category view
    BackHandler(enabled = expandedCategory != null) {
        expandedCategory = null
    }

    // Auto-sync on resume
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshLibrary()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    // Broadened Multi-Property Search & Prof/Subject Filter
    val filteredBooks = remember(books, selectedProf, selectedSubject, searchQuery) {
        val searchTokens = searchQuery.trim().split(Regex("\\s+")).filter { it.isNotBlank() }

        books.filter { book ->
            val matchesProf = book.prof.equals(selectedProf.id, ignoreCase = true) ||
                    book.tagsCsv.contains(selectedProf.id, ignoreCase = true) ||
                    (selectedProf.id == "Final Prof" && (book.prof.contains("Final", ignoreCase = true) || book.year.contains("4th", ignoreCase = true)))

            val matchesSubject = if (selectedSubject == "All") true else {
                book.subject.equals(selectedSubject, ignoreCase = true) ||
                        book.tagsCsv.contains(selectedSubject, ignoreCase = true)
            }

            val matchesSearch = if (searchTokens.isEmpty()) true else {
                val corpus = buildString {
                    append(book.title).append(" ")
                    append(book.author).append(" ")
                    append(book.subject).append(" ")
                    append(book.type).append(" ")
                    append(book.edition).append(" ")
                    append(book.prof).append(" ")
                    append(book.year).append(" ")
                    append(book.tagsCsv).append(" ")
                    append(book.uploader).append(" ")
                    append(book.slug)
                }.lowercase()
                searchTokens.all { token -> corpus.contains(token.lowercase()) }
            }

            matchesProf && matchesSubject && matchesSearch
        }
    }

    // Group filtered books according to the 5 official catalog types
    val categoryRows = remember(filteredBooks) {
        OFFICIAL_CATALOG_TYPES.mapNotNull { type ->
            val matching = filteredBooks.filter { matchesCatalogType(it, type) }
            if (matching.isNotEmpty()) type to matching else null
        }
    }

    // Selected book for detail sheet
    val selectedBook = remember(books, selectedBookId) {
        if (selectedBookId == null) null
        else books.find { it.slug == selectedBookId || it.id.toString() == selectedBookId }
    }

    // Books for the currently expanded category
    val expandedBooks = remember(categoryRows, expandedCategory) {
        if (expandedCategory == null) emptyList()
        else categoryRows.find { it.first == expandedCategory }?.second ?: emptyList()
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.backgroundGradient)
            .statusBarsPadding()
    ) {
        val configuration = LocalConfiguration.current
        val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        val isWideScreen = configuration.screenWidthDp >= 600 || isLandscape

        if (isWideScreen) {
            // RESPONSIVE DUAL-PANE VIEW FOR LANDSCAPE / LARGE SCREENS
            Row(modifier = Modifier.fillMaxSize()) {
                // Left Pane: Main Library shelves list
                Box(
                    modifier = Modifier
                        .then(
                            if (expandedCategory != null) Modifier.width(380.dp) else Modifier.fillMaxWidth()
                        )
                        .fillMaxHeight()
                ) {
                    PullToRefreshBox(
                        isRefreshing = isSyncing,
                        onRefresh = { viewModel.refreshLibrary() },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        LibraryMainShelfContent(
                            colors = colors,
                            searchQuery = searchQuery,
                            onSearchQueryChange = { searchQuery = it },
                            selectedProf = selectedProf,
                            selectedSubject = selectedSubject,
                            onSelectedSubjectChange = { selectedSubject = it },
                            categoryRows = categoryRows,
                            expandedCategory = expandedCategory,
                            onExpandCategory = { cat ->
                                expandedCategory = if (expandedCategory == cat) null else cat
                            },
                            onBookClick = { book ->
                                selectedBookId = book.slug.ifEmpty { book.id.toString() }
                            },
                            onClearFocus = { focusManager.clearFocus() }
                        )
                    }
                }

                // Vertical Hairline Divider with smooth fade
                AnimatedVisibility(
                    visible = expandedCategory != null,
                    enter = fadeIn(animationSpec = tween(220)),
                    exit = fadeOut(animationSpec = tween(160))
                ) {
                    Box(
                        modifier = Modifier
                            .width(0.5.dp)
                            .fillMaxHeight()
                            .background(colors.borderSubtle)
                    )
                }

                // Right Pane: Supporting Expanded Category View with subtle horizontal slide from right + gentle fade
                AnimatedVisibility(
                    visible = expandedCategory != null,
                    enter = slideInHorizontally(
                        initialOffsetX = { fullWidth -> (fullWidth * 0.25f).toInt() },
                        animationSpec = spring(
                            dampingRatio = 0.82f,
                            stiffness = Spring.StiffnessMediumLow
                        )
                    ) + fadeIn(
                        animationSpec = tween(220, easing = LinearOutSlowInEasing)
                    ),
                    exit = slideOutHorizontally(
                        targetOffsetX = { fullWidth -> (fullWidth * 0.25f).toInt() },
                        animationSpec = tween(200, easing = FastOutSlowInEasing)
                    ) + fadeOut(
                        animationSpec = tween(160)
                    ),
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                ) {
                    val activeCat = expandedCategory
                    if (activeCat != null) {
                        // Smoothly transition content when switching between categories while open
                        AnimatedContent(
                            targetState = activeCat,
                            transitionSpec = {
                                (fadeIn(animationSpec = tween(200)) + slideInHorizontally { width -> (width * 0.12f).toInt() })
                                    .togetherWith(fadeOut(animationSpec = tween(150)) + slideOutHorizontally { width -> (-width * 0.12f).toInt() })
                            },
                            label = "wide_category_content_transition"
                        ) { catTitle ->
                            val booksForCat = remember(categoryRows, catTitle) {
                                categoryRows.find { it.first == catTitle }?.second ?: emptyList()
                            }
                            ExpandedCategoryPane(
                                categoryTitle = catTitle,
                                books = booksForCat,
                                onClose = { expandedCategory = null },
                                onBookClick = { book ->
                                    selectedBookId = book.slug.ifEmpty { book.id.toString() }
                                },
                                modifier = Modifier.fillMaxSize()
                            )
                        }
                    }
                }
            }
        } else {
            // MOBILE SCREEN: Push & Pop Horizontal Slide with gentle fade
            AnimatedContent(
                targetState = expandedCategory,
                transitionSpec = {
                    if (targetState != null) {
                        // Forward push: Dedicated screen slides in from right + soft fade, main shelf slides left
                        (slideInHorizontally(
                            initialOffsetX = { fullWidth -> (fullWidth * 0.28f).toInt() },
                            animationSpec = spring(
                                dampingRatio = 0.85f,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        ) + fadeIn(animationSpec = tween(220, easing = LinearOutSlowInEasing)))
                            .togetherWith(
                                slideOutHorizontally(
                                    targetOffsetX = { fullWidth -> (-fullWidth * 0.15f).toInt() },
                                    animationSpec = tween(200, easing = FastOutSlowInEasing)
                                ) + fadeOut(animationSpec = tween(160))
                            )
                    } else {
                        // Backward pop: Main shelf slides in from left, dedicated screen slides out to right
                        (slideInHorizontally(
                            initialOffsetX = { fullWidth -> (-fullWidth * 0.15f).toInt() },
                            animationSpec = spring(
                                dampingRatio = 0.85f,
                                stiffness = Spring.StiffnessMediumLow
                            )
                        ) + fadeIn(animationSpec = tween(220, easing = LinearOutSlowInEasing)))
                            .togetherWith(
                                slideOutHorizontally(
                                    targetOffsetX = { fullWidth -> (fullWidth * 0.28f).toInt() },
                                    animationSpec = tween(200, easing = FastOutSlowInEasing)
                                ) + fadeOut(animationSpec = tween(160))
                            )
                    }
                },
                label = "mobile_category_transition",
                modifier = Modifier.fillMaxSize()
            ) { targetCategory ->
                if (targetCategory != null) {
                    val booksForCategory = remember(categoryRows, targetCategory) {
                        categoryRows.find { it.first == targetCategory }?.second ?: emptyList()
                    }
                    DedicatedCategoryScreen(
                        categoryTitle = targetCategory,
                        books = booksForCategory,
                        onBack = { expandedCategory = null },
                        onBookClick = { book ->
                            selectedBookId = book.slug.ifEmpty { book.id.toString() }
                        }
                    )
                } else {
                    PullToRefreshBox(
                        isRefreshing = isSyncing,
                        onRefresh = { viewModel.refreshLibrary() },
                        modifier = Modifier.fillMaxSize()
                    ) {
                        LibraryMainShelfContent(
                            colors = colors,
                            searchQuery = searchQuery,
                            onSearchQueryChange = { searchQuery = it },
                            selectedProf = selectedProf,
                            selectedSubject = selectedSubject,
                            onSelectedSubjectChange = { selectedSubject = it },
                            categoryRows = categoryRows,
                            expandedCategory = null,
                            onExpandCategory = { expandedCategory = it },
                            onBookClick = { book ->
                                selectedBookId = book.slug.ifEmpty { book.id.toString() }
                            },
                            onClearFocus = { focusManager.clearFocus() }
                        )
                    }
                }
            }
        }
    }

    // 5. BOOK DETAIL BOTTOM SHEET
    selectedBook?.let { book ->
        ModalBottomSheet(
            onDismissRequest = { selectedBookId = null },
            sheetState = bookSheetState,
            containerColor = colors.surfaceCard,
            dragHandle = {
                Box(
                    modifier = Modifier
                        .padding(top = 12.dp, bottom = 8.dp)
                        .width(36.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(colors.borderSubtle)
                )
            }
        ) {
            CompactBookDetailBottomSheetContent(
                book = book,
                downloadState = downloadState,
                onDownloadClick = {
                    viewModel.downloadBook(book)
                },
                onCancelDownloadClick = {
                    viewModel.cancelDownload(book.slug.ifEmpty { "book_${book.id}" })
                },
                onOpenReaderClick = {
                    selectedBookId = null
                    val verifiedPath = book.binFilePath.takeIf { it.isNotBlank() && File(it).exists() }
                        ?: com.example.util.MulberryStorageManager.getBinFileForBook(context, book.slug).takeIf { it.exists() }?.absolutePath
                        ?: book.uriString.takeIf { it.isNotBlank() }
                        ?: File(File(context.filesDir, "vault"), "${book.slug}.bin").takeIf { it.exists() }?.absolutePath
                        ?: ""

                    ReaderActivity.launch(
                        context = context,
                        bookPath = verifiedPath,
                        bookTitle = book.title,
                        bookId = book.id
                    )
                },
                onDismiss = { selectedBookId = null }
            )
        }
    }
}

/**
 * Main Shelf content containing the title, search bar, subject filter chips, and category shelves.
 */
@Composable
private fun LibraryMainShelfContent(
    colors: com.example.ui.theme.MulberryColors,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedProf: ProfCurriculum,
    selectedSubject: String,
    onSelectedSubjectChange: (String) -> Unit,
    categoryRows: List<Pair<String, List<BookEntity>>>,
    expandedCategory: String?,
    onExpandCategory: (String) -> Unit,
    onBookClick: (BookEntity) -> Unit,
    onClearFocus: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxSize()) {
        // 1. TOP HEADER ("Mulberry Shelf")
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Mulberry Shelf",
                fontFamily = PoppinsFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                color = colors.textPrimary,
                letterSpacing = (-0.5).sp,
                modifier = Modifier.testTag("mulberry_shelf_title")
            )
        }

        // 2. FULL-WIDTH SEARCH BAR ROW
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = colors.surfaceCard,
                border = BorderStroke(1.dp, colors.borderSubtle),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(46.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = FluentIcons.Search24Regular,
                        contentDescription = "Search",
                        tint = colors.textSecondary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    BasicTextField(
                        value = searchQuery,
                        onValueChange = onSearchQueryChange,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("search_textbooks_input"),
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                        keyboardActions = KeyboardActions(onSearch = { onClearFocus() }),
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            color = colors.textPrimary,
                            fontFamily = InterFamily,
                            fontSize = 13.5.sp
                        ),
                        cursorBrush = SolidColor(colors.primary),
                        decorationBox = { innerTextField ->
                            if (searchQuery.isEmpty()) {
                                Text(
                                    text = "Search by tag, subject, book, or author...",
                                    style = MaterialTheme.typography.bodyMedium.copy(
                                        color = colors.textSecondary.copy(alpha = 0.7f),
                                        fontFamily = InterFamily,
                                        fontSize = 13.5.sp
                                    )
                                )
                            }
                            innerTextField()
                        }
                    )
                    if (searchQuery.isNotEmpty()) {
                        IconButton(
                            onClick = { onSearchQueryChange("") },
                            modifier = Modifier.size(22.dp)
                        ) {
                            Icon(
                                imageVector = FluentIcons.Dismiss24Regular,
                                contentDescription = "Clear",
                                tint = colors.textSecondary,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(4.dp))

        // 3. HORIZONTAL SUBJECT FILTER CHIPS ROW
        LazyRow(
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            item {
                val isSelected = selectedSubject == "All"
                SubjectFilterChip(
                    label = "All",
                    isSelected = isSelected,
                    onClick = { onSelectedSubjectChange("All") }
                )
            }

            items(selectedProf.subjects) { subject ->
                val isSelected = selectedSubject == subject
                SubjectFilterChip(
                    label = subject,
                    isSelected = isSelected,
                    onClick = {
                        onSelectedSubjectChange(if (isSelected) "All" else subject)
                    }
                )
            }
        }

        // 4. MAIN BODY - CATEGORY ROWS WITH EXPAND ACTION
        if (categoryRows.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Surface(
                        shape = CircleShape,
                        color = colors.surfaceCard,
                        border = BorderStroke(1.dp, colors.borderSubtle),
                        modifier = Modifier.size(64.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = FluentIcons.Book24Regular,
                                contentDescription = null,
                                tint = colors.textSecondary.copy(alpha = 0.6f),
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                    Text(
                        text = "No books found on shelf",
                        fontFamily = PoppinsFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 17.sp,
                        color = colors.textPrimary
                    )
                    Text(
                        text = if (searchQuery.isNotBlank()) "No match for \"$searchQuery\" in ${selectedProf.shortName}"
                        else "No textbooks currently cataloged under $selectedSubject",
                        fontFamily = InterFamily,
                        fontSize = 13.sp,
                        color = colors.textSecondary,
                        textAlign = TextAlign.Center
                    )
                    if (selectedSubject != "All" || searchQuery.isNotBlank()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedButton(
                            onClick = {
                                onSelectedSubjectChange("All")
                                onSearchQueryChange("")
                            },
                            shape = RoundedCornerShape(12.dp),
                            border = BorderStroke(1.dp, colors.borderSubtle)
                        ) {
                            Text(
                                text = "Reset Filters",
                                fontFamily = InterFamily,
                                fontSize = 12.sp,
                                color = colors.primary
                            )
                        }
                    }
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(bottom = 90.dp)
            ) {
                categoryRows.forEach { (categoryName, categoryBooks) ->
                    item(key = "header_$categoryName") {
                        CategoryRowHeader(
                            title = categoryName,
                            count = categoryBooks.size,
                            isExpanded = expandedCategory == categoryName,
                            onExpandClick = {
                                onExpandCategory(categoryName)
                            }
                        )
                    }

                    item(key = "row_$categoryName") {
                        LazyRow(
                            contentPadding = PaddingValues(horizontal = 20.dp),
                            horizontalArrangement = Arrangement.spacedBy(14.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 18.dp)
                        ) {
                            items(categoryBooks, key = { it.slug.ifEmpty { "book_${it.id}" } }) { book ->
                                ShelfBookCard(
                                    book = book,
                                    onClick = { onBookClick(book) }
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Dedicated Full-Screen Category View displaying all books of a category in a 2-column vertical grid.
 */
@Composable
private fun DedicatedCategoryScreen(
    categoryTitle: String,
    books: List<BookEntity>,
    onBack: () -> Unit,
    onBookClick: (BookEntity) -> Unit
) {
    val colors = LocalMulberryColors.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.backgroundGradient)
    ) {
        // Category Top App Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(
                onClick = onBack,
                modifier = Modifier.testTag("btn_back_category")
            ) {
                Icon(
                    imageVector = FluentIcons.ArrowLeft24Regular,
                    contentDescription = "Back to Library",
                    tint = colors.textPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = categoryTitle,
                    fontFamily = PoppinsFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = colors.textPrimary
                )
                Text(
                    text = "${books.size} resources available",
                    fontFamily = InterFamily,
                    fontSize = 12.sp,
                    color = colors.textSecondary
                )
            }
        }

        HorizontalDivider(color = colors.borderSubtle, thickness = 0.5.dp)

        // 2-Column Vertical Grid for smooth vertical browsing
        LazyVerticalGrid(
            columns = GridCells.Fixed(2),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(books, key = { it.slug.ifEmpty { "cat_book_${it.id}" } }) { book ->
                ShelfBookCard(
                    book = book,
                    onClick = { onBookClick(book) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/**
 * Dual-Pane Supporting Side Pane for Landscape & Large Screen devices.
 */
@Composable
private fun ExpandedCategoryPane(
    categoryTitle: String,
    books: List<BookEntity>,
    onClose: () -> Unit,
    onBookClick: (BookEntity) -> Unit,
    modifier: Modifier = Modifier
) {
    val colors = LocalMulberryColors.current

    Column(
        modifier = modifier
            .background(colors.backgroundGradient)
    ) {
        // Supporting Pane Header with Close Action
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = categoryTitle,
                    fontFamily = PoppinsFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "${books.size} resources available",
                    fontFamily = InterFamily,
                    fontSize = 12.sp,
                    color = colors.textSecondary
                )
            }

            IconButton(
                onClick = onClose,
                modifier = Modifier
                    .size(36.dp)
                    .testTag("btn_close_category_pane")
            ) {
                Icon(
                    imageVector = FluentIcons.Dismiss24Regular,
                    contentDescription = "Close $categoryTitle pane",
                    tint = colors.textPrimary,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        HorizontalDivider(color = colors.borderSubtle, thickness = 0.5.dp)

        // Adaptive Multi-Column Grid
        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 136.dp),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
            horizontalArrangement = Arrangement.spacedBy(14.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            items(books, key = { it.slug.ifEmpty { "cat_pane_${it.id}" } }) { book ->
                ShelfBookCard(
                    book = book,
                    onClick = { onBookClick(book) },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

/**
 * Filter Chip Pill for Subject Selection.
 */
@Composable
private fun SubjectFilterChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val colors = LocalMulberryColors.current

    Surface(
        shape = RoundedCornerShape(20.dp),
        color = if (isSelected) colors.primary.copy(alpha = 0.22f) else colors.surfaceCard,
        border = BorderStroke(
            1.dp,
            if (isSelected) colors.primary else colors.borderSubtle
        ),
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .clickable { onClick() }
            .testTag("subject_chip_$label")
    ) {
        Text(
            text = label,
            fontFamily = InterFamily,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            fontSize = 13.sp,
            color = if (isSelected) colors.primary else colors.textSecondary,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 7.dp)
        )
    }
}

/**
 * Section Header for each category row with title, item count pill badge, and right-end expand icon.
 */
@Composable
private fun CategoryRowHeader(
    title: String,
    count: Int,
    isExpanded: Boolean = false,
    onExpandClick: () -> Unit
) {
    val colors = LocalMulberryColors.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 20.dp, end = 16.dp, top = 16.dp, bottom = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = title,
                fontFamily = PoppinsFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                color = if (isExpanded) colors.primary else colors.textPrimary,
                letterSpacing = (-0.3).sp
            )

            Surface(
                shape = CircleShape,
                color = if (isExpanded) colors.primary.copy(alpha = 0.18f) else colors.surfaceCard,
                border = BorderStroke(0.5.dp, if (isExpanded) colors.primary else colors.borderSubtle),
                modifier = Modifier.size(22.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Text(
                        text = "$count",
                        fontFamily = InterFamily,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isExpanded) colors.primary else colors.textSecondary
                    )
                }
            }
        }

        // Expand Action on right end
        IconButton(
            onClick = onExpandClick,
            modifier = Modifier
                .size(36.dp)
                .testTag("btn_expand_$title")
        ) {
            Icon(
                imageVector = if (isExpanded) FluentIcons.Dismiss24Regular else FluentIcons.ChevronRight24Regular,
                contentDescription = if (isExpanded) "Close $title pane" else "Expand $title into full vertical view",
                tint = if (isExpanded) colors.primary else colors.textSecondary,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

/**
 * High-fidelity Shelf Book Card matching the provided visual reference.
 */
@Composable
private fun ShelfBookCard(
    book: BookEntity,
    onClick: () -> Unit,
    modifier: Modifier = Modifier.width(136.dp)
) {
    val colors = LocalMulberryColors.current
    val context = LocalContext.current

    val editionLabel = remember(book.edition, book.tagsCsv) {
        when {
            book.edition.isNotBlank() && !book.edition.equals("General", ignoreCase = true) -> {
                if (book.edition.contains("Edition", ignoreCase = true)) {
                    book.edition.replace("Edition", "Ed").trim()
                } else {
                    book.edition
                }
            }
            book.tagsCsv.contains("8th", ignoreCase = true) -> "8th Ed"
            book.tagsCsv.contains("14th", ignoreCase = true) -> "14th Ed"
            book.tagsCsv.contains("10th", ignoreCase = true) -> "10th Ed"
            book.tagsCsv.contains("9th", ignoreCase = true) -> "9th Ed"
            else -> null
        }
    }

    val interactionSource = remember { MutableInteractionSource() }
    val isPressed by interactionSource.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (isPressed) 0.94f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "shelf_book_press_scale"
    )

    Column(
        modifier = modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clip(RoundedCornerShape(12.dp))
            .clickable(
                interactionSource = interactionSource,
                indication = null
            ) { onClick() }
            .testTag("shelf_book_${book.slug.ifEmpty { book.id.toString() }}")
    ) {
        // Book Cover Art with Badges
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .aspectRatio(3f / 4.1f)
                .clip(RoundedCornerShape(12.dp))
                .background(colors.surfaceCard)
                .border(0.5.dp, colors.borderSubtle, RoundedCornerShape(12.dp))
        ) {
            AsyncImage(
                model = ImageRequest.Builder(context)
                    .data(book.coverUrl)
                    .crossfade(true)
                    .build(),
                contentDescription = "Cover of ${book.title}",
                contentScale = ContentScale.Crop,
                modifier = Modifier.matchParentSize()
            )

            // Top-Right Edition Pill Badge
            if (!editionLabel.isNullOrBlank()) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Color.Black.copy(alpha = 0.72f),
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                ) {
                    Text(
                        text = editionLabel,
                        fontFamily = InterFamily,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
            }

            // Bottom-Right Download Verified Circle Badge
            if (book.isDownloaded) {
                Surface(
                    shape = CircleShape,
                    color = colors.primary,
                    modifier = Modifier
                        .align(Alignment.BottomEnd)
                        .padding(6.dp)
                        .size(24.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = FluentIcons.Checkmark24Regular,
                            contentDescription = "Downloaded",
                            tint = colors.onPrimary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(7.dp))

        // Book Title (2 lines max)
        Text(
            text = book.title,
            fontFamily = PoppinsFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 13.sp,
            color = colors.textPrimary,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
            lineHeight = 16.sp
        )

        Spacer(modifier = Modifier.height(2.dp))

        // Author (1 line max)
        Text(
            text = book.author.ifEmpty { "Faculty Edition" },
            fontFamily = InterFamily,
            fontSize = 11.sp,
            color = colors.textSecondary,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Spacer(modifier = Modifier.height(2.dp))

        // Metadata: Page Count • File Size
        val pageText = if (book.pageCount > 1) "${book.pageCount}p" else ""
        val sizeText = if (book.sizeMb > 0) "${book.sizeMb} MB" else ""
        val specText = listOf(pageText, sizeText).filter { it.isNotEmpty() }.joinToString(" • ")

        if (specText.isNotEmpty()) {
            Text(
                text = specText,
                fontFamily = InterFamily,
                fontSize = 11.sp,
                color = colors.textMuted,
                maxLines = 1
            )
        }
    }
}

/**
 * Prof Selection Bottom Sheet Modal displaying all 4 MBBS Prof years and their curriculum subjects.
 */
@Composable
private fun ProfSelectionBottomSheetContent(
    curricula: List<ProfCurriculum>,
    selectedProf: ProfCurriculum,
    onProfSelected: (ProfCurriculum) -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalMulberryColors.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 12.dp)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Select Curriculum Stage",
                    fontFamily = PoppinsFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = colors.textPrimary
                )
                Text(
                    text = "Browse textbooks & resources tailored to your MBBS year",
                    fontFamily = InterFamily,
                    fontSize = 12.sp,
                    color = colors.textSecondary
                )
            }

            IconButton(onClick = onDismiss) {
                Icon(
                    imageVector = FluentIcons.Dismiss24Regular,
                    contentDescription = "Close",
                    tint = colors.textSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        HorizontalDivider(color = colors.borderSubtle, thickness = 0.5.dp)

        curricula.forEach { prof ->
            val isSelected = prof.id == selectedProf.id

            Surface(
                shape = RoundedCornerShape(14.dp),
                color = if (isSelected) colors.primary.copy(alpha = 0.12f) else colors.surfaceCard,
                border = BorderStroke(
                    1.dp,
                    if (isSelected) colors.primary else colors.borderSubtle
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .clickable { onProfSelected(prof) }
                    .testTag("prof_select_option_${prof.id}")
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = prof.title,
                            fontFamily = PoppinsFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = if (isSelected) colors.primary else colors.textPrimary
                        )
                        Spacer(modifier = Modifier.height(3.dp))
                        Text(
                            text = prof.subjects.joinToString(" • "),
                            fontFamily = InterFamily,
                            fontSize = 12.sp,
                            color = colors.textSecondary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }

                    if (isSelected) {
                        Surface(
                            shape = CircleShape,
                            color = colors.primary,
                            modifier = Modifier.size(24.dp)
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = FluentIcons.Checkmark24Regular,
                                    contentDescription = "Selected",
                                    tint = colors.onPrimary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))
    }
}

/**
 * Compact Bottom Sheet displaying book details, download progress, and instant read action.
 */
@Composable
private fun CompactBookDetailBottomSheetContent(
    book: BookEntity,
    downloadState: DownloadProgressState,
    onDownloadClick: () -> Unit,
    onCancelDownloadClick: () -> Unit = {},
    onOpenReaderClick: () -> Unit,
    onDismiss: () -> Unit
) {
    val colors = LocalMulberryColors.current
    val context = LocalContext.current

    val isThisDownloading = downloadState is DownloadProgressState.Downloading &&
            (downloadState.bookSlug == book.slug || downloadState.bookSlug == "book_${book.id}")
    val downloadProgress = if (isThisDownloading) (downloadState as DownloadProgressState.Downloading).progressPercent else 0

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 24.dp, vertical = 12.dp)
            .navigationBarsPadding(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Book Header (Cover + Info)
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = colors.borderSubtle,
                modifier = Modifier
                    .width(72.dp)
                    .height(100.dp)
            ) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(book.coverUrl)
                        .crossfade(true)
                        .build(),
                    contentDescription = "Cover of ${book.title}",
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = book.title,
                    fontFamily = PoppinsFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = colors.textPrimary,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = book.author.ifEmpty { "Faculty Edition" },
                    fontFamily = InterFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 13.sp,
                    color = colors.textSecondary
                )

                Text(
                    text = "${book.edition.ifEmpty { "General Edition" }} · ${book.prof.ifEmpty { "MBBS Core" }}",
                    fontFamily = InterFamily,
                    fontSize = 12.sp,
                    color = colors.textSecondary.copy(alpha = 0.8f)
                )

                val specs = buildList {
                    if (book.pageCount > 1) add("${book.pageCount} Pages")
                    if (book.sizeMb > 0) add("${book.sizeMb} MB")
                    if (book.uploader.isNotBlank()) add("By ${book.uploader}")
                }.joinToString(" · ")

                if (specs.isNotBlank()) {
                    Text(
                        text = specs,
                        fontFamily = InterFamily,
                        fontSize = 11.sp,
                        color = colors.textSecondary.copy(alpha = 0.7f)
                    )
                }
            }
        }

        // Tag Pills
        if (book.tagsCsv.isNotBlank()) {
            val tags = book.tagsCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }.take(5)
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(tags) { tag ->
                    Surface(
                        shape = RoundedCornerShape(6.dp),
                        color = colors.borderSubtle.copy(alpha = 0.5f)
                    ) {
                        Text(
                            text = tag,
                            fontFamily = InterFamily,
                            fontSize = 11.sp,
                            color = colors.textSecondary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }

        HorizontalDivider(color = colors.borderSubtle, thickness = 0.5.dp)

        // Action Buttons with Crossfade animation
        Crossfade(
            targetState = Triple(book.isDownloaded, isThisDownloading, downloadProgress),
            label = "BottomActionCrossfade"
        ) { (isDownloaded, isDownloading, progress) ->
            when {
                isDownloading -> {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Downloading to Shelf...",
                                fontFamily = InterFamily,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = colors.primary
                            )
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Text(
                                    text = "$progress%",
                                    fontFamily = InterFamily,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.primary
                                )
                                TextButton(
                                    onClick = onCancelDownloadClick,
                                    contentPadding = PaddingValues(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Icon(
                                        imageVector = FluentIcons.Dismiss24Regular,
                                        contentDescription = "Cancel",
                                        tint = colors.textSecondary,
                                        modifier = Modifier.size(14.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = "Cancel",
                                        fontFamily = InterFamily,
                                        fontSize = 11.sp,
                                        color = colors.textSecondary
                                    )
                                }
                            }
                        }
                        LinearProgressIndicator(
                            progress = { progress / 100f },
                            color = colors.primary,
                            trackColor = colors.borderSubtle,
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp)
                                .clip(RoundedCornerShape(3.dp))
                        )
                    }
                }
                isDownloaded -> {
                    Button(
                        onClick = onOpenReaderClick,
                        colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("btn_read_book_sheet")
                    ) {
                        Icon(
                            imageVector = FluentIcons.Book24Filled,
                            contentDescription = "Read",
                            tint = colors.onPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "Read Textbook",
                            fontFamily = PoppinsFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            color = colors.onPrimary
                        )
                    }
                }
                else -> {
                    Button(
                        onClick = onDownloadClick,
                        colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp)
                            .testTag("btn_download_book_sheet")
                    ) {
                        Icon(
                            imageVector = FluentIcons.ArrowDownload24Regular,
                            contentDescription = "Download",
                            tint = colors.onPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        val sizeText = if (book.sizeMb > 0) " (${book.sizeMb} MB)" else ""
                        Text(
                            text = "Download to Shelf$sizeText",
                            fontFamily = PoppinsFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            color = colors.onPrimary
                        )
                    }
                }
            }
        }

        // Secondary Close Button
        OutlinedButton(
            onClick = onDismiss,
            shape = RoundedCornerShape(14.dp),
            border = BorderStroke(1.dp, colors.borderSubtle),
            colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.textPrimary),
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp)
        ) {
            Icon(
                imageVector = FluentIcons.Dismiss24Regular,
                contentDescription = "Close",
                tint = colors.textSecondary,
                modifier = Modifier.size(16.dp)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "Close",
                fontFamily = InterFamily,
                fontSize = 13.sp,
                color = colors.textSecondary
            )
        }
    }
}
