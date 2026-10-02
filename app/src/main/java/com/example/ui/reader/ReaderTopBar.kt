package com.example.ui.reader

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.FluentIcons
import com.example.ui.theme.InterFamily
import com.example.ui.theme.LocalMulberryColors
import com.example.ui.theme.PoppinsFamily
import com.example.util.toCleanBookTitle
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderTopBarScreen(
    bookTitle: String,
    bookPath: String,
    totalPages: Int,
    currentPageIndex: Int,
    tocItems: List<TocItem>,
    isTocLoading: Boolean,
    thumbnailManager: PdfThumbnailManager,
    searchManager: PdfTextSearchManager,
    onBackClick: () -> Unit,
    onJumpToPage: (Int) -> Unit,
    onOpenWith: () -> Unit,
    onShare: () -> Unit,
    onFeedback: () -> Unit,
    onMatchesChanged: (List<SearchMatch>, Int) -> Unit = { _, _ -> }
) {
    val colors = LocalMulberryColors.current
    var showThumbnailSheet by remember { mutableStateOf(false) }
    var showTocSheet by remember { mutableStateOf(false) }
    var showMoreMenu by remember { mutableStateOf(false) }
    var showDocInfoDialog by remember { mutableStateOf(false) }
    var isSearchActive by remember { mutableStateOf(false) }

    LaunchedEffect(isSearchActive) {
        if (!isSearchActive) {
            onMatchesChanged(emptyList(), -1)
        }
    }

    // Active search context snippet for floating hint
    var activeSnippet by remember { mutableStateOf<String?>(null) }

    // 2-state thumbnail bottom sheet
    val thumbnailSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)
    val tocSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val moreSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()

    Column(modifier = Modifier.fillMaxWidth()) {
        // Unified 56.dp persistent header surface matching the active theme background
        Surface(
            color = colors.background,
            shadowElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            AnimatedContent(
                targetState = isSearchActive,
                transitionSpec = {
                    if (targetState) {
                        // Enter Search: title & actions slide left & fade out, search controls slide in from right & fade in
                        (slideInHorizontally(
                            animationSpec = tween(240, easing = FastOutSlowInEasing),
                            initialOffsetX = { it / 4 }
                        ) + fadeIn(animationSpec = tween(240, delayMillis = 40)))
                            .togetherWith(
                                slideOutHorizontally(
                                    animationSpec = tween(180, easing = FastOutSlowInEasing),
                                    targetOffsetX = { -it / 4 }
                                ) + fadeOut(animationSpec = tween(160))
                            )
                    } else {
                        // Exit Search: search controls slide right & fade out, standard title bar slides in from left & fades in
                        (slideInHorizontally(
                            animationSpec = tween(240, easing = FastOutSlowInEasing),
                            initialOffsetX = { -it / 4 }
                        ) + fadeIn(animationSpec = tween(240, delayMillis = 40)))
                            .togetherWith(
                                slideOutHorizontally(
                                    animationSpec = tween(180, easing = FastOutSlowInEasing),
                                    targetOffsetX = { it / 4 }
                                ) + fadeOut(animationSpec = tween(160))
                            )
                    }
                },
                label = "HeaderSearchTransformation"
            ) { searchActive ->
                if (!searchActive) {
                    // Standard Top Bar
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(56.dp)
                            .padding(horizontal = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Back Button
                        IconButton(
                            onClick = onBackClick,
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(
                                imageVector = FluentIcons.ArrowLeft24Regular,
                                contentDescription = "Back",
                                tint = colors.textPrimary,
                                modifier = Modifier.size(24.dp)
                            )
                        }

                        // Title
                        Text(
                            text = bookTitle.toCleanBookTitle(),
                            fontFamily = PoppinsFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 16.sp,
                            color = colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 8.dp)
                        )

                        // Action 1: Broad View (Page Thumbnail Grid)
                        IconButton(
                            onClick = { showThumbnailSheet = true },
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(
                                imageVector = FluentIcons.Grid24Regular,
                                contentDescription = "Page Thumbnails Overview",
                                tint = colors.textPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        // Action 2: In-Document Search (triggers transformation)
                        IconButton(
                            onClick = { isSearchActive = true },
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(
                                imageVector = FluentIcons.Search24Regular,
                                contentDescription = "Search in Document",
                                tint = colors.textPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        // Action 3: Table of Contents
                        IconButton(
                            onClick = { showTocSheet = true },
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(
                                imageVector = FluentIcons.Toc24Regular,
                                contentDescription = "Table of Contents",
                                tint = colors.textPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }

                        // Action 4: More Options
                        IconButton(
                            onClick = { showMoreMenu = true },
                            modifier = Modifier.size(44.dp)
                        ) {
                            Icon(
                                imageVector = FluentIcons.MoreVertical24Regular,
                                contentDescription = "More Options",
                                tint = colors.textPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                } else {
                    // In-Header Search Bar (smoothly replaces standard header at 56.dp)
                    SearchTopBarContent(
                        searchManager = searchManager,
                        onJumpToPage = { pageIndex ->
                            onJumpToPage(pageIndex)
                        },
                        onSnippetUpdated = { snippet ->
                            activeSnippet = snippet
                        },
                        onMatchesChanged = onMatchesChanged,
                        onClose = {
                            isSearchActive = false
                            activeSnippet = null
                        }
                    )
                }
            }
        }

        // Context Snippet Preview Ribbon (Smoothly slides in below header when a match is active)
        AnimatedVisibility(
            visible = isSearchActive && !activeSnippet.isNullOrBlank(),
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            Surface(
                color = colors.surfaceCard,
                border = BorderStroke(0.5.dp, colors.borderSubtle),
                shadowElevation = 3.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Context:",
                        fontFamily = InterFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        color = colors.primary,
                        modifier = Modifier.padding(end = 6.dp)
                    )
                    Text(
                        text = activeSnippet ?: "",
                        fontFamily = InterFamily,
                        fontSize = 12.sp,
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }

    // 2-State Thumbnail Overview BottomSheet
    if (showThumbnailSheet) {
        ThumbnailBottomSheet(
            sheetState = thumbnailSheetState,
            totalPages = totalPages,
            currentPageIndex = currentPageIndex,
            thumbnailManager = thumbnailManager,
            onPageSelected = { pageIndex ->
                coroutineScope.launch {
                    thumbnailSheetState.hide()
                    showThumbnailSheet = false
                    onJumpToPage(pageIndex)
                }
            },
            onDismiss = {
                showThumbnailSheet = false
            }
        )
    }

    // Compose Table of Contents ModalBottomSheet
    if (showTocSheet) {
        ModalBottomSheet(
            onDismissRequest = { showTocSheet = false },
            sheetState = tocSheetState,
            containerColor = colors.surface,
            contentColor = colors.textPrimary,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            ReaderTocSheetContent(
                tocItems = tocItems,
                isLoading = isTocLoading,
                onItemClick = { targetPageIdx ->
                    coroutineScope.launch {
                        tocSheetState.hide()
                        showTocSheet = false
                        onJumpToPage(targetPageIdx.toInt())
                    }
                },
                onClose = {
                    coroutineScope.launch {
                        tocSheetState.hide()
                        showTocSheet = false
                    }
                }
            )
        }
    }

    // Compose More Options ModalBottomSheet
    if (showMoreMenu) {
        ModalBottomSheet(
            onDismissRequest = { showMoreMenu = false },
            sheetState = moreSheetState,
            containerColor = colors.surface,
            contentColor = colors.textPrimary,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            ReaderMoreMenuContent(
                onOpenWith = {
                    coroutineScope.launch {
                        moreSheetState.hide()
                        showMoreMenu = false
                        onOpenWith()
                    }
                },
                onShare = {
                    coroutineScope.launch {
                        moreSheetState.hide()
                        showMoreMenu = false
                        onShare()
                    }
                },
                onDocumentInfo = {
                    coroutineScope.launch {
                        moreSheetState.hide()
                        showMoreMenu = false
                        showDocInfoDialog = true
                    }
                },
                onFeedback = {
                    coroutineScope.launch {
                        moreSheetState.hide()
                        showMoreMenu = false
                        onFeedback()
                    }
                }
            )
        }
    }

    // Compose Document Info AlertDialog
    if (showDocInfoDialog) {
        ReaderDocInfoDialog(
            bookTitle = bookTitle,
            bookPath = bookPath,
            totalPages = totalPages,
            onDismiss = { showDocInfoDialog = false }
        )
    }
}

/**
 * In-Header Search Bar Composable rendered directly inside the 56.dp top bar.
 * Harmonizes with the active theme background and provides streaming matches with navigation.
 */
@Composable
fun SearchTopBarContent(
    searchManager: PdfTextSearchManager,
    onJumpToPage: (Int) -> Unit,
    onSnippetUpdated: (String?) -> Unit,
    onMatchesChanged: (List<SearchMatch>, Int) -> Unit = { _, _ -> },
    onClose: () -> Unit
) {
    val colors = LocalMulberryColors.current
    val coroutineScope = rememberCoroutineScope()
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }

    var query by remember { mutableStateOf("") }
    var isSearching by remember { mutableStateOf(false) }
    var matches by remember { mutableStateOf<List<SearchMatch>>(emptyList()) }
    var currentMatchIndex by remember { mutableIntStateOf(-1) }
    var searchJob by remember { mutableStateOf<Job?>(null) }

    // Auto-focus the text field when search header opens
    LaunchedEffect(Unit) {
        delay(120)
        focusRequester.requestFocus()
    }

    // Keep parent and highlight layer in sync
    LaunchedEffect(matches, currentMatchIndex) {
        onMatchesChanged(matches, currentMatchIndex)
    }

    // Debounced, streaming page-by-page search
    LaunchedEffect(query) {
        searchJob?.cancel()
        val trimmed = query.trim()
        if (trimmed.length >= 2) {
            isSearching = true
            searchJob = coroutineScope.launch {
                delay(220) // Snappy debounce
                matches = emptyList()
                currentMatchIndex = -1

                val finalResults = searchManager.searchStreaming(trimmed) { updatedMatches ->
                    // Progressive progress callback
                    matches = updatedMatches
                    if (currentMatchIndex == -1 && updatedMatches.isNotEmpty()) {
                        currentMatchIndex = 0
                        onJumpToPage(updatedMatches[0].pageIndex)
                        onSnippetUpdated(updatedMatches[0].snippet)
                    }
                }

                matches = finalResults
                isSearching = false
                if (finalResults.isNotEmpty() && currentMatchIndex == -1) {
                    currentMatchIndex = 0
                    onJumpToPage(finalResults[0].pageIndex)
                    onSnippetUpdated(finalResults[0].snippet)
                }
            }
        } else {
            matches = emptyList()
            currentMatchIndex = -1
            isSearching = false
            onSnippetUpdated(null)
        }
    }

    fun jumpToMatch(index: Int) {
        if (matches.isNotEmpty() && index in matches.indices) {
            currentMatchIndex = index
            val match = matches[index]
            onJumpToPage(match.pageIndex)
            onSnippetUpdated(match.snippet)
        }
    }

    fun goToNextMatch() {
        if (matches.isNotEmpty()) {
            val nextIndex = (currentMatchIndex + 1) % matches.size
            jumpToMatch(nextIndex)
        }
    }

    fun goToPreviousMatch() {
        if (matches.isNotEmpty()) {
            val prevIndex = if (currentMatchIndex <= 0) matches.size - 1 else currentMatchIndex - 1
            jumpToMatch(prevIndex)
        }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .padding(horizontal = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Back / Close Button
        IconButton(
            onClick = {
                keyboardController?.hide()
                onClose()
            },
            modifier = Modifier.size(44.dp)
        ) {
            Icon(
                imageVector = FluentIcons.ArrowLeft24Regular,
                contentDescription = "Exit Search",
                tint = colors.textPrimary,
                modifier = Modifier.size(24.dp)
            )
        }

        // Search Input Field Container (Styled to theme card surface with subtle border)
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = colors.surfaceCard,
            border = BorderStroke(0.75.dp, colors.borderSubtle),
            modifier = Modifier
                .weight(1f)
                .height(40.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = FluentIcons.Search24Regular,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(18.dp)
                )

                Spacer(modifier = Modifier.width(8.dp))

                BasicTextField(
                    value = query,
                    onValueChange = { query = it },
                    singleLine = true,
                    textStyle = TextStyle(
                        fontFamily = InterFamily,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Normal,
                        color = colors.textPrimary
                    ),
                    cursorBrush = SolidColor(colors.primary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(
                        onSearch = {
                            keyboardController?.hide()
                            if (matches.isNotEmpty()) {
                                goToNextMatch()
                            }
                        }
                    ),
                    decorationBox = { innerTextField ->
                        Box(
                            modifier = Modifier.fillMaxWidth(),
                            contentAlignment = Alignment.CenterStart
                        ) {
                            if (query.isEmpty()) {
                                Text(
                                    text = "Search in document...",
                                    fontFamily = InterFamily,
                                    fontSize = 13.5.sp,
                                    color = colors.textSecondary.copy(alpha = 0.7f)
                                )
                            }
                            innerTextField()
                        }
                    },
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester)
                )

                if (query.isNotEmpty()) {
                    IconButton(
                        onClick = {
                            query = ""
                            matches = emptyList()
                            currentMatchIndex = -1
                            onSnippetUpdated(null)
                        },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Clear",
                            tint = colors.textSecondary,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }

        // Match Counter Badge or Progress Spinner
        when {
            isSearching -> {
                CircularProgressIndicator(
                    strokeWidth = 2.dp,
                    color = colors.primary,
                    modifier = Modifier
                        .padding(horizontal = 8.dp)
                        .size(18.dp)
                )
            }
            query.trim().length >= 2 -> {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = colors.primary.copy(alpha = 0.12f),
                    modifier = Modifier.padding(horizontal = 6.dp)
                ) {
                    Text(
                        text = if (matches.isNotEmpty()) "${currentMatchIndex + 1}/${matches.size}" else "0/0",
                        fontFamily = InterFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.5.sp,
                        color = colors.primary,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                    )
                }
            }
        }

        // Previous Match Button
        IconButton(
            onClick = { goToPreviousMatch() },
            enabled = matches.isNotEmpty(),
            modifier = Modifier.size(38.dp)
        ) {
            Icon(
                imageVector = FluentIcons.ChevronUp24Regular,
                contentDescription = "Previous Match",
                tint = if (matches.isNotEmpty()) colors.textPrimary else colors.textSecondary.copy(alpha = 0.35f),
                modifier = Modifier.size(18.dp)
            )
        }

        // Next Match Button
        IconButton(
            onClick = { goToNextMatch() },
            enabled = matches.isNotEmpty(),
            modifier = Modifier.size(38.dp)
        ) {
            Icon(
                imageVector = FluentIcons.ChevronDown24Regular,
                contentDescription = "Next Match",
                tint = if (matches.isNotEmpty()) colors.textPrimary else colors.textSecondary.copy(alpha = 0.35f),
                modifier = Modifier.size(18.dp)
            )
        }
    }
}

data class FlatTocItem(
    val title: String,
    val pageIdx: Long,
    val level: Int
)

fun flattenToc(items: List<TocItem>, level: Int = 0): List<FlatTocItem> {
    val result = mutableListOf<FlatTocItem>()
    for (item in items) {
        result.add(FlatTocItem(title = item.title, pageIdx = item.pageIdx, level = level))
        if (item.children.isNotEmpty()) {
            result.addAll(flattenToc(item.children, level + 1))
        }
    }
    return result
}

@Composable
fun ReaderTocSheetContent(
    tocItems: List<TocItem>,
    isLoading: Boolean,
    onItemClick: (Long) -> Unit,
    onClose: () -> Unit
) {
    val colors = LocalMulberryColors.current
    val flattened = remember(tocItems) { flattenToc(tocItems) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight(0.68f)
            .navigationBarsPadding()
            .padding(horizontal = 20.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Table of Contents",
                    fontFamily = PoppinsFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp,
                    color = colors.textPrimary
                )
                Text(
                    text = if (flattened.isNotEmpty()) "${flattened.size} Sections found" else "Document Outline",
                    fontFamily = InterFamily,
                    fontSize = 12.sp,
                    color = colors.textSecondary
                )
            }
            IconButton(onClick = onClose) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Close",
                    tint = colors.textPrimary,
                    modifier = Modifier.size(22.dp)
                )
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        when {
            isLoading -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(
                        color = colors.primary,
                        modifier = Modifier.size(36.dp)
                    )
                }
            }
            flattened.isEmpty() -> {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Text(
                            text = "📑",
                            fontSize = 38.sp
                        )
                        Text(
                            text = "No table of contents found in this PDF.",
                            fontFamily = PoppinsFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp,
                            color = colors.textSecondary,
                            textAlign = TextAlign.Center
                        )
                        Text(
                            text = "This document does not contain embedded outline bookmarks.",
                            fontFamily = InterFamily,
                            fontSize = 12.sp,
                            color = colors.textSecondary.copy(alpha = 0.7f),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    itemsIndexed(flattened, key = { index, item -> "toc_${index}_${item.pageIdx}" }) { _, item ->
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = Color.Transparent,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onItemClick(item.pageIdx) }
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(horizontal = 8.dp, vertical = 10.dp)
                                    .padding(start = (item.level * 16).dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = item.title,
                                    fontFamily = InterFamily,
                                    fontWeight = FontWeight.Medium,
                                    fontSize = 14.sp,
                                    color = colors.textPrimary,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Surface(
                                    shape = RoundedCornerShape(8.dp),
                                    color = colors.surfaceTint,
                                    modifier = Modifier.padding(start = 8.dp)
                                ) {
                                    Text(
                                        text = "p. ${item.pageIdx + 1}",
                                        fontFamily = InterFamily,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 11.5.sp,
                                        color = colors.textPrimary,
                                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ReaderMoreMenuContent(
    onOpenWith: () -> Unit,
    onShare: () -> Unit,
    onDocumentInfo: () -> Unit,
    onFeedback: () -> Unit
) {
    val colors = LocalMulberryColors.current

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .navigationBarsPadding()
            .padding(vertical = 12.dp, horizontal = 20.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(2.dp),
            color = colors.borderSubtle,
            modifier = Modifier
                .size(width = 36.dp, height = 4.dp)
                .align(Alignment.CenterHorizontally)
        ) {}

        Spacer(modifier = Modifier.height(14.dp))

        Text(
            text = "Options",
            fontFamily = PoppinsFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 17.sp,
            color = colors.textPrimary,
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        Spacer(modifier = Modifier.height(8.dp))

        SingleLineMenuItem(
            icon = FluentIcons.Open24Regular,
            label = "Open With...",
            onClick = onOpenWith
        )

        SingleLineMenuItem(
            icon = FluentIcons.Share24Regular,
            label = "Share Document",
            onClick = onShare
        )

        HorizontalDivider(
            color = colors.borderSubtle.copy(alpha = 0.5f),
            modifier = Modifier.padding(vertical = 4.dp)
        )

        SingleLineMenuItem(
            icon = FluentIcons.Info24Regular,
            label = "Document Info",
            onClick = onDocumentInfo
        )

        SingleLineMenuItem(
            icon = FluentIcons.Mail24Regular,
            label = "Feedback",
            onClick = onFeedback
        )
    }
}

@Composable
fun SingleLineMenuItem(
    icon: ImageVector,
    label: String,
    onClick: () -> Unit
) {
    val colors = LocalMulberryColors.current
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = Color.Transparent,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 13.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Start
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = colors.primary,
                modifier = Modifier.size(20.dp)
            )
            Spacer(modifier = Modifier.width(14.dp))
            Text(
                text = label,
                fontFamily = InterFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 14.5.sp,
                color = colors.textPrimary
            )
        }
    }
}

@Composable
fun ReaderDocInfoDialog(
    bookTitle: String,
    bookPath: String,
    totalPages: Int,
    onDismiss: () -> Unit
) {
    val colors = LocalMulberryColors.current
    val file = remember(bookPath) { File(bookPath) }
    val fileSizeStr = remember(file) {
        if (file.exists()) {
            String.format(Locale.US, "%.2f MB", file.length() / (1024f * 1024f))
        } else {
            "Unknown"
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = colors.surface,
        titleContentColor = colors.textPrimary,
        textContentColor = colors.textPrimary,
        shape = RoundedCornerShape(18.dp),
        title = {
            Text(
                text = "Document Info",
                fontFamily = PoppinsFamily,
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp
            )
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                DocInfoRow(label = "Title", value = bookTitle.toCleanBookTitle())
                DocInfoRow(label = "Raw File Name", value = file.name.ifEmpty { "document.pdf" })
                DocInfoRow(label = "Size", value = fileSizeStr)
                DocInfoRow(label = "Total Pages", value = totalPages.toString())
                DocInfoRow(label = "Storage Path", value = bookPath)
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = "OK",
                    fontFamily = InterFamily,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.primary
                )
            }
        }
    )
}

@Composable
private fun DocInfoRow(label: String, value: String) {
    val colors = LocalMulberryColors.current
    Column(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = label,
            fontFamily = InterFamily,
            fontWeight = FontWeight.SemiBold,
            fontSize = 12.sp,
            color = colors.textSecondary
        )
        Text(
            text = value,
            fontFamily = InterFamily,
            fontSize = 13.5.sp,
            color = colors.textPrimary
        )
    }
}
