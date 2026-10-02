package com.example.ui.screens

import android.graphics.BitmapFactory
import android.net.Uri
import com.example.ui.reader.ReaderActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.GridView
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.StarBorder
import androidx.compose.material.icons.filled.ViewList
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.RadioButtonDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.BookEntity
import com.example.data.model.VaultEntity
import com.example.ui.components.FluentIcons
import com.example.ui.theme.InterFamily
import com.example.ui.theme.LocalMulberryColors
import com.example.ui.theme.PoppinsFamily
import com.example.util.ThumbnailManager
import com.example.util.toCleanBookTitle
import com.example.viewmodel.BookSortOption
import com.example.viewmodel.MulberryViewModel
import com.example.viewmodel.rootPath
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LibraryScreen(
    viewModel: MulberryViewModel,
    modifier: Modifier = Modifier
) {
    val colors = LocalMulberryColors.current
    val context = LocalContext.current
    val focusManager = LocalFocusManager.current
    val scope = rememberCoroutineScope()

    val books by viewModel.filteredBooks.collectAsState()
    val continueReading by viewModel.continueReadingBooks.collectAsState()
    val availableFolders by viewModel.availableFolders.collectAsState()
    val activeBreadcrumbs by viewModel.activeBreadcrumbs.collectAsState()
    val activeFolderFilter by viewModel.activeFolderFilter.collectAsState()
    val searchQuery by viewModel.searchQuery.collectAsState()
    val isSearchExpanded by viewModel.isSearchExpanded.collectAsState()
    val isGridView by viewModel.isGridView.collectAsState()
    val userProfile by viewModel.userProfile.collectAsState()

    val activeVault by viewModel.activeVault.collectAsState()
    val allVaults by viewModel.allVaults.collectAsState()
    val isSyncing by viewModel.isSyncing.collectAsState()
    var showVaultSwitcherSheet by remember { mutableStateOf(false) }
    val vaultSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // 1. Lifecycle Resume Listener: Auto-sync on app resume
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refreshLibrary()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // BackHandler: Navigate up if inside subfolder
    BackHandler(enabled = activeBreadcrumbs.size > 1 || activeFolderFilter != null) {
        viewModel.navigateBreadcrumb(activeBreadcrumbs.size - 2)
    }

    // Long-press Bottom Sheet State
    var selectedBookForDetails by remember { mutableStateOf<BookEntity?>(null) }
    var bookToRename by remember { mutableStateOf<BookEntity?>(null) }
    var bookToDelete by remember { mutableStateOf<BookEntity?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // SAF folder picker launcher
    val folderPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            val segment = uri.lastPathSegment ?: "Vault"
            val folderName = segment.substringAfterLast(":").ifEmpty { "Vault" }
            viewModel.bindFolderUri(uri, folderName)
        }
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
            .statusBarsPadding()
    ) {
        // 1. Header & Quick Search Bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "Hi, ${userProfile.displayName}",
                    fontFamily = PoppinsFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 28.sp,
                    color = colors.textPrimary,
                    letterSpacing = (-0.25).sp
                )
                Text(
                    text = "Ready to resume your ${userProfile.targetExamOrSubject} vault?",
                    fontFamily = InterFamily,
                    fontWeight = FontWeight.Normal,
                    fontSize = 13.sp,
                    color = colors.textSecondary
                )
            }

            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                var showSortMenu by remember { mutableStateOf(false) }
                val currentSort by viewModel.sortOption.collectAsState()

                Box {
                    Surface(
                        shape = CircleShape,
                        color = colors.surfaceCard,
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, colors.borderSubtle),
                        modifier = Modifier.size(42.dp)
                    ) {
                        IconButton(
                            onClick = { showSortMenu = true },
                            modifier = Modifier.testTag("library_sort_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Sort,
                                contentDescription = "Sort Books",
                                tint = colors.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false },
                        modifier = Modifier
                            .width(220.dp)
                            .clip(RoundedCornerShape(16.dp))
                            .background(MaterialTheme.colorScheme.surfaceContainerHigh)
                            .border(0.5.dp, colors.borderSubtle, RoundedCornerShape(16.dp))
                    ) {
                        DropdownMenuItem(
                            text = { Text("Recently Read", fontFamily = InterFamily, fontSize = 13.sp) },
                            leadingIcon = {
                                if (currentSort == BookSortOption.RECENTLY_READ) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = colors.primary, modifier = Modifier.size(16.dp))
                                }
                            },
                            onClick = {
                                viewModel.setSortOption(BookSortOption.RECENTLY_READ)
                                showSortMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Title (A to Z)", fontFamily = InterFamily, fontSize = 13.sp) },
                            leadingIcon = {
                                if (currentSort == BookSortOption.TITLE_AZ) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = colors.primary, modifier = Modifier.size(16.dp))
                                }
                            },
                            onClick = {
                                viewModel.setSortOption(BookSortOption.TITLE_AZ)
                                showSortMenu = false
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Date Added / Modified", fontFamily = InterFamily, fontSize = 13.sp) },
                            leadingIcon = {
                                if (currentSort == BookSortOption.DATE_MODIFIED) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = colors.primary, modifier = Modifier.size(16.dp))
                                }
                            },
                            onClick = {
                                viewModel.setSortOption(BookSortOption.DATE_MODIFIED)
                                showSortMenu = false
                            }
                        )
                    }
                }

                Surface(
                    shape = CircleShape,
                    color = if (isSearchExpanded) colors.surfaceTint else colors.surfaceCard,
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, colors.borderSubtle),
                    modifier = Modifier.size(42.dp)
                ) {
                    IconButton(
                        onClick = { viewModel.toggleSearch() },
                        modifier = Modifier.testTag("library_search_button")
                    ) {
                        Icon(
                            imageVector = if (isSearchExpanded) Icons.Default.Close else FluentIcons.Search24Regular,
                            contentDescription = "Search",
                            tint = colors.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
            }
        }

        // Expandable In-Memory Search Input
        AnimatedVisibility(
            visible = isSearchExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(colors.surfaceCard)
                    .border(0.5.dp, colors.primary.copy(alpha = 0.5f), RoundedCornerShape(12.dp))
                    .padding(horizontal = 14.dp, vertical = 10.dp)
            ) {
                BasicTextField(
                    value = searchQuery,
                    onValueChange = { viewModel.setSearchQuery(it) },
                    singleLine = true,
                    textStyle = androidx.compose.ui.text.TextStyle(
                        fontFamily = InterFamily,
                        fontSize = 14.sp,
                        color = colors.textPrimary
                    ),
                    cursorBrush = SolidColor(colors.primary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("library_search_input"),
                    decorationBox = { innerTextField ->
                        if (searchQuery.isEmpty()) {
                            Text(
                                text = "Search books, authors, or topics...",
                                fontFamily = InterFamily,
                                fontSize = 14.sp,
                                color = colors.textMuted
                            )
                        }
                        innerTextField()
                    }
                )
            }
        }

        // 2. Pull-To-Refresh Container: Recent Shelf + Folders + Books Grid
        PullToRefreshBox(
            isRefreshing = isSyncing,
            onRefresh = { viewModel.refreshLibrary() },
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            // Empty State: Prompt to link folder vault or load sample data
            if (books.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(horizontal = 24.dp),
                    contentAlignment = Alignment.Center
                ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(72.dp)
                            .clip(CircleShape)
                            .background(colors.surfaceTint),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = FluentIcons.Book24Regular,
                            contentDescription = "Library",
                            tint = colors.primary,
                            modifier = Modifier.size(36.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Text(
                        text = "Your Library is Empty",
                        fontFamily = PoppinsFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 20.sp,
                        color = colors.textPrimary
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = "Link a folder vault from your device to organize and read your textbooks, or explore with demo academic books.",
                        fontFamily = InterFamily,
                        fontSize = 13.sp,
                        color = colors.textSecondary,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(horizontal = 16.dp)
                    )

                    Spacer(modifier = Modifier.height(24.dp))

                    Button(
                        onClick = { folderPicker.launch(null) },
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = colors.primary,
                            contentColor = colors.onPrimary
                        ),
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .height(48.dp)
                            .testTag("bind_vault_button")
                    ) {
                        Icon(
                            imageVector = FluentIcons.Folder24Filled,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Link Folder Vault",
                            fontFamily = PoppinsFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 14.sp
                        )
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    OutlinedButton(
                        onClick = { viewModel.seedSampleData() },
                        shape = RoundedCornerShape(12.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, colors.primary),
                        modifier = Modifier
                            .fillMaxWidth(0.8f)
                            .height(44.dp)
                            .testTag("load_sample_data_button")
                    ) {
                        Text(
                            text = "Load Academic Demo (Guyton, HCV)",
                            fontFamily = InterFamily,
                            fontWeight = FontWeight.Medium,
                            fontSize = 13.sp,
                            color = colors.primary
                        )
                    }
                }
            }
        } else {
            // Main Library Grid with Header Shelf
            LazyVerticalGrid(
                columns = if (isGridView) GridCells.Fixed(2) else GridCells.Fixed(1),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("library_books_grid")
            ) {
                // Shelf: Continue Reading
                if (continueReading.isNotEmpty() && searchQuery.isEmpty()) {
                    item(span = { GridItemSpan(maxLineSpan) }) {
                        Column {
                            Text(
                                text = "Continue Reading",
                                fontFamily = PoppinsFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp,
                                color = colors.textPrimary,
                                modifier = Modifier.padding(bottom = 10.dp)
                            )

                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                items(continueReading, key = { "cr_${it.id}" }) { book ->
                                    ContinueReadingCard(
                                        book = book,
                                        onClick = { ReaderActivity.launch(context, book.uriString, book.title, book.id) },
                                        onLongClick = { selectedBookForDetails = book }
                                    )
                                }
                            }

                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }

                // Breadcrumbs & View Toggle
                item(span = { GridItemSpan(maxLineSpan) }) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Interactive Breadcrumb Strip
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .weight(1f)
                                .horizontalScroll(rememberScrollState())
                        ) {
                            // Breadcrumb Vault Selector Pill
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.surfaceContainerHigh,
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable { showVaultSwitcherSheet = true }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = FluentIcons.Folder24Filled,
                                        contentDescription = "Vault",
                                        tint = colors.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    val activeVaultName = activeVault?.name ?: "Vault"
                                    val clampedVaultName = if (activeVaultName.length > 16) {
                                        activeVaultName.take(15) + "…"
                                    } else {
                                        activeVaultName
                                    }
                                    Text(
                                        text = clampedVaultName,
                                        fontFamily = PoppinsFamily,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp,
                                        color = colors.textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = FluentIcons.ChevronDown16Regular,
                                        contentDescription = "Switch Vault",
                                        tint = colors.textSecondary,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }

                            // Subfolders after Vault root
                            if (activeBreadcrumbs.size > 1) {
                                activeBreadcrumbs.drop(1).forEachIndexed { subIndex, segment ->
                                    val actualIndex = subIndex + 1
                                    val isLast = actualIndex == activeBreadcrumbs.lastIndex
                                    Text(
                                        text = " › ",
                                        fontFamily = InterFamily,
                                        fontSize = 13.sp,
                                        color = colors.textMuted,
                                        modifier = Modifier.padding(horizontal = 2.dp)
                                    )
                                    Text(
                                        text = segment,
                                        fontFamily = PoppinsFamily,
                                        fontWeight = if (isLast) FontWeight.SemiBold else FontWeight.Normal,
                                        fontSize = 13.sp,
                                        color = if (isLast) colors.primary else colors.textSecondary,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .clickable { viewModel.navigateBreadcrumb(actualIndex) }
                                            .padding(horizontal = 4.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }

                        // Grid / List View Switcher
                        IconButton(
                            onClick = { viewModel.toggleGridView() },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = if (isGridView) Icons.Default.ViewList else Icons.Default.GridView,
                                contentDescription = "Toggle View",
                                tint = colors.textSecondary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // Dynamic Folders (Compact Tiles, 2 per row)
                if (availableFolders.isNotEmpty() && activeBreadcrumbs.size <= 1 && searchQuery.isEmpty()) {
                    items(availableFolders, key = { "folder_${it.first}" }) { (folderName, count) ->
                        FolderTile(
                            folderName = folderName,
                            bookCount = count,
                            onClick = { viewModel.setFolderFilter(folderName) }
                        )
                    }
                }

                // Books (Frameless Cover Cards with clean titles & thumbnails)
                items(books, key = { "book_${it.id}" }) { book ->
                    BookCoverCard(
                        book = book,
                        isGridView = isGridView,
                        onOpen = { ReaderActivity.launch(context, book.uriString, book.title, book.id) },
                        onLongClick = { selectedBookForDetails = book },
                        onToggleFavorite = { viewModel.toggleBookFavorite(book.id, !book.isFavorite) }
                    )
                }

                item(span = { GridItemSpan(maxLineSpan) }) {
                    Spacer(modifier = Modifier.height(72.dp))
                }
            }
        }
    }
}

    // Long-Press Book Details Modal Bottom Sheet
    if (selectedBookForDetails != null) {
        val targetBook = selectedBookForDetails!!
        val cleanTitle = targetBook.title.toCleanBookTitle()

        ModalBottomSheet(
            onDismissRequest = { selectedBookForDetails = null },
            sheetState = sheetState,
            containerColor = colors.surface,
            shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 12.dp)
            ) {
                // Header: Clean Title
                Text(
                    text = cleanTitle,
                    fontFamily = PoppinsFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp,
                    color = colors.textPrimary,
                    lineHeight = 24.sp
                )

                // Original Filename
                Text(
                    text = targetBook.fileName,
                    fontFamily = FontFamily.Monospace,
                    fontSize = 11.sp,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Spacer(modifier = Modifier.height(16.dp))

                // Metadata Details Box
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = colors.surfaceTint,
                    border = androidx.compose.foundation.BorderStroke(0.5.dp, colors.borderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        val mb = String.format("%.2f MB", targetBook.fileSizeBytes / (1024f * 1024f))
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("File Size", fontFamily = InterFamily, fontSize = 12.sp, color = colors.textSecondary)
                            Text(mb, fontFamily = InterFamily, fontWeight = FontWeight.Medium, fontSize = 12.sp, color = colors.textPrimary)
                        }
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("Page Count", fontFamily = InterFamily, fontSize = 12.sp, color = colors.textSecondary)
                            Text("${targetBook.pageCount} pages", fontFamily = InterFamily, fontWeight = FontWeight.Medium, fontSize = 12.sp, color = colors.textPrimary)
                        }
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("Folder", fontFamily = InterFamily, fontSize = 12.sp, color = colors.textSecondary)
                            Text(targetBook.parentFolder, fontFamily = InterFamily, fontWeight = FontWeight.Medium, fontSize = 12.sp, color = colors.textPrimary)
                        }
                        Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                            Text("Raw Path", fontFamily = InterFamily, fontSize = 12.sp, color = colors.textSecondary)
                            Text(
                                text = targetBook.uriString,
                                fontFamily = FontFamily.Monospace,
                                fontSize = 10.sp,
                                color = colors.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(start = 12.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                // Action Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Button(
                        onClick = {
                            val b = targetBook
                            selectedBookForDetails = null
                            ReaderActivity.launch(context, b.uriString, b.title, b.id)
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.primary),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.MenuBook, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Open", fontFamily = InterFamily)
                    }

                    OutlinedButton(
                        onClick = {
                            viewModel.toggleBookFavorite(targetBook.id, !targetBook.isFavorite)
                            selectedBookForDetails = targetBook.copy(isFavorite = !targetBook.isFavorite)
                        },
                        shape = RoundedCornerShape(10.dp),
                        border = androidx.compose.foundation.BorderStroke(1.dp, colors.borderSubtle),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            imageVector = if (targetBook.isFavorite) Icons.Default.Star else Icons.Default.StarBorder,
                            contentDescription = null,
                            tint = if (targetBook.isFavorite) Color(0xFFF59E0B) else colors.textSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(if (targetBook.isFavorite) "Favorite" else "Star", fontFamily = InterFamily, color = colors.textPrimary)
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = {
                            bookToRename = targetBook
                            selectedBookForDetails = null
                        },
                        shape = RoundedCornerShape(10.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Rename", fontFamily = InterFamily, fontSize = 12.sp)
                    }

                    OutlinedButton(
                        onClick = {
                            bookToDelete = targetBook
                            selectedBookForDetails = null
                        },
                        shape = RoundedCornerShape(10.dp),
                        colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.accentDanger),
                        border = androidx.compose.foundation.BorderStroke(1.dp, colors.accentDanger.copy(alpha = 0.5f)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Delete", fontFamily = InterFamily, fontSize = 12.sp)
                    }
                }

                Spacer(modifier = Modifier.height(28.dp))
            }
        }
    }

    // Modal: Rename Book Dialog
    if (bookToRename != null) {
        val target = bookToRename!!
        var renameInput by remember { mutableStateOf(target.title.toCleanBookTitle()) }

        AlertDialog(
            onDismissRequest = { bookToRename = null },
            title = { Text("Rename Book", fontFamily = PoppinsFamily, fontWeight = FontWeight.SemiBold) },
            text = {
                OutlinedTextField(
                    value = renameInput,
                    onValueChange = { renameInput = it },
                    label = { Text("New Book Title") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (renameInput.isNotBlank()) {
                            viewModel.renameBook(target, renameInput.trim())
                            bookToRename = null
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
                ) {
                    Text("Rename", fontFamily = InterFamily)
                }
            },
            dismissButton = {
                TextButton(onClick = { bookToRename = null }) {
                    Text("Cancel", fontFamily = InterFamily)
                }
            }
        )
    }

    // Modal: Delete Book Confirmation Dialog
    if (bookToDelete != null) {
        val target = bookToDelete!!
        var deleteStorageFile by remember { mutableStateOf(false) }

        AlertDialog(
            onDismissRequest = { bookToDelete = null },
            title = { Text("Delete Document", fontFamily = PoppinsFamily, fontWeight = FontWeight.SemiBold) },
            text = {
                Column {
                    Text(
                        text = "Are you sure you want to remove \"${target.title.toCleanBookTitle()}\" from your library?",
                        fontFamily = InterFamily,
                        fontSize = 13.sp,
                        color = colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { deleteStorageFile = !deleteStorageFile }
                            .padding(vertical = 4.dp)
                    ) {
                        androidx.compose.material3.Checkbox(
                            checked = deleteStorageFile,
                            onCheckedChange = { deleteStorageFile = it }
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Also delete file from device storage (SAF)",
                            fontFamily = InterFamily,
                            fontSize = 12.sp,
                            color = colors.accentDanger
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteBook(target, deleteStorageFile)
                        bookToDelete = null
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.accentDanger)
                ) {
                    Text("Delete", fontFamily = InterFamily)
                }
            },
            dismissButton = {
                TextButton(onClick = { bookToDelete = null }) {
                    Text("Cancel", fontFamily = InterFamily)
                }
            }
        )
    }

    // Vault Switcher Modal Bottom Sheet
    if (showVaultSwitcherSheet) {
        ModalBottomSheet(
            onDismissRequest = { showVaultSwitcherSheet = false },
            sheetState = vaultSheetState,
            containerColor = colors.surface,
            contentColor = colors.textPrimary,
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight(0.6f)
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 8.dp)
            ) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Switch Study Vault",
                            fontFamily = PoppinsFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 18.sp,
                            color = colors.textPrimary
                        )
                        Text(
                            text = "${allVaults.size} Vaults linked",
                            fontFamily = InterFamily,
                            fontSize = 12.sp,
                            color = colors.textMuted
                        )
                    }
                    IconButton(
                        onClick = {
                            scope.launch {
                                vaultSheetState.hide()
                                showVaultSwitcherSheet = false
                            }
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Close",
                            tint = colors.textSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                // Content List (LazyColumn)
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(allVaults, key = { "vault_${it.id}" }) { vault ->
                        val isActive = vault.id == activeVault?.id
                        val rootPath = vault.rootPath

                        Card(
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = if (isActive) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.35f) else colors.surfaceCard
                            ),
                            border = if (isActive) {
                                androidx.compose.foundation.BorderStroke(1.5.dp, MaterialTheme.colorScheme.primary)
                            } else {
                                androidx.compose.foundation.BorderStroke(1.dp, colors.borderSubtle.copy(alpha = 0.6f))
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    viewModel.setActiveVault(vault.id)
                                    scope.launch {
                                        vaultSheetState.hide()
                                        showVaultSwitcherSheet = false
                                    }
                                }
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 14.dp, vertical = 12.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        Text(
                                            text = vault.name,
                                            fontFamily = PoppinsFamily,
                                            fontWeight = FontWeight.Medium,
                                            fontSize = 15.sp,
                                            color = colors.textPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Surface(
                                            shape = RoundedCornerShape(6.dp),
                                            color = colors.surfaceTint.copy(alpha = 0.5f)
                                        ) {
                                            Text(
                                                text = "${vault.bookCount} books",
                                                fontFamily = InterFamily,
                                                fontWeight = FontWeight.Medium,
                                                fontSize = 11.sp,
                                                color = colors.textPrimary,
                                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                                            )
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(4.dp))
                                    Text(
                                        text = rootPath,
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 11.sp,
                                        color = colors.textMuted,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }

                                Spacer(modifier = Modifier.width(12.dp))

                                RadioButton(
                                    selected = isActive,
                                    onClick = null,
                                    colors = RadioButtonDefaults.colors(
                                        selectedColor = colors.primary,
                                        unselectedColor = colors.textMuted
                                    )
                                )
                            }
                        }
                    }
                }

                // Footer Section
                HorizontalDivider(
                    color = colors.borderSubtle.copy(alpha = 0.6f),
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable {
                            scope.launch {
                                vaultSheetState.hide()
                                showVaultSwitcherSheet = false
                            }
                            folderPicker.launch(null)
                        }
                        .padding(vertical = 10.dp, horizontal = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = FluentIcons.FolderAdd24Regular,
                        contentDescription = null,
                        tint = colors.primary,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Bind New Vault Folder...",
                        fontFamily = PoppinsFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        color = colors.primary
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ContinueReadingCard(
    book: BookEntity,
    onClick: () -> Unit,
    onLongClick: () -> Unit
) {
    val colors = LocalMulberryColors.current
    val context = LocalContext.current
    var thumbnailBitmap by remember(book.id, book.uriString) { mutableStateOf<android.graphics.Bitmap?>(null) }

    LaunchedEffect(book.id, book.uriString) {
        withContext(Dispatchers.IO) {
            val thumbFile = ThumbnailManager.getThumbnailFile(context, book.id, book.uriString)
            if (thumbFile.exists() && thumbFile.length() > 0) {
                thumbnailBitmap = BitmapFactory.decodeFile(thumbFile.absolutePath)
            } else {
                val generated = ThumbnailManager.getOrGenerateThumbnail(context, book.id, book.uriString)
                if (generated != null) {
                    thumbnailBitmap = BitmapFactory.decodeFile(generated.absolutePath)
                }
            }
        }
    }

    Card(
        shape = RoundedCornerShape(10.dp),
        colors = CardDefaults.cardColors(containerColor = colors.surfaceCard),
        border = androidx.compose.foundation.BorderStroke(0.5.dp, colors.borderSubtle),
        modifier = Modifier
            .width(86.dp)
            .height(130.dp)
            .clip(RoundedCornerShape(10.dp))
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (thumbnailBitmap != null) {
                Image(
                    bitmap = thumbnailBitmap!!.asImageBitmap(),
                    contentDescription = book.title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                // Dark bottom overlay for text readability
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Transparent,
                                    Color.Black.copy(alpha = 0.85f)
                                )
                            )
                        )
                )
            } else {
                // Elegant Academic Book spine gradient placeholder
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    colors.surfaceTint,
                                    colors.primary.copy(alpha = 0.25f),
                                    colors.primary.copy(alpha = 0.85f)
                                )
                            )
                        )
                )
            }

            // Spine line
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .drawBehind {
                        drawLine(
                            color = Color.White.copy(alpha = 0.3f),
                            start = Offset(6.dp.toPx(), 0f),
                            end = Offset(6.dp.toPx(), size.height),
                            strokeWidth = 1.dp.toPx()
                        )
                    }
            )

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = book.title.toCleanBookTitle(),
                    fontFamily = PoppinsFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    color = Color.White,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 13.sp
                )

                // Floating progress badge
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = Color.Black.copy(alpha = 0.6f),
                    modifier = Modifier.padding(bottom = 4.dp)
                ) {
                    Text(
                        text = "p. ${book.lastReadPage} • ${book.progressPercent}%",
                        fontFamily = InterFamily,
                        fontSize = 9.sp,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp)
                    )
                }
            }

            // Accent progress strip attached to bottom edge
            Box(
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .fillMaxWidth()
                    .height(3.dp)
                    .background(Color.Black.copy(alpha = 0.2f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth((book.progressPercent / 100f).coerceIn(0f, 1f))
                        .height(3.dp)
                        .background(colors.accentWarning)
                )
            }
        }
    }
}

@Composable
fun FolderTile(
    folderName: String,
    bookCount: Int,
    onClick: () -> Unit
) {
    val colors = LocalMulberryColors.current

    Surface(
        shape = RoundedCornerShape(14.dp),
        color = colors.surfaceCard,
        border = androidx.compose.foundation.BorderStroke(0.5.dp, colors.borderSubtle),
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(colors.surfaceTint),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = FluentIcons.Folder24Filled,
                    contentDescription = null,
                    tint = colors.primary,
                    modifier = Modifier.size(18.dp)
                )
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = folderName,
                    fontFamily = PoppinsFamily,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    color = colors.textPrimary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    text = "$bookCount ${if (bookCount == 1) "book" else "books"}",
                    fontFamily = InterFamily,
                    fontSize = 11.sp,
                    color = colors.textSecondary
                )
            }
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun BookCoverCard(
    book: BookEntity,
    isGridView: Boolean,
    onOpen: () -> Unit,
    onLongClick: () -> Unit,
    onToggleFavorite: () -> Unit
) {
    val colors = LocalMulberryColors.current
    val context = LocalContext.current
    val cleanTitle = remember(book.title, book.fileName) {
        book.title.ifBlank { book.fileName }.toCleanBookTitle()
    }

    var thumbnailBitmap by remember(book.id, book.uriString) { mutableStateOf<android.graphics.Bitmap?>(null) }

    LaunchedEffect(book.id, book.uriString) {
        withContext(Dispatchers.IO) {
            val thumbFile = ThumbnailManager.getThumbnailFile(context, book.id, book.uriString)
            if (thumbFile.exists() && thumbFile.length() > 0) {
                thumbnailBitmap = BitmapFactory.decodeFile(thumbFile.absolutePath)
            } else {
                val generated = ThumbnailManager.getOrGenerateThumbnail(context, book.id, book.uriString)
                if (generated != null) {
                    thumbnailBitmap = BitmapFactory.decodeFile(generated.absolutePath)
                }
            }
        }
    }

    if (isGridView) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .combinedClickable(
                    onClick = onOpen,
                    onLongClick = onLongClick
                )
        ) {
            // 3:4 aspect ratio container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .aspectRatio(0.75f)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        Brush.linearGradient(
                            listOf(
                                colors.surfaceTint,
                                colors.primary.copy(alpha = 0.35f),
                                colors.primary
                            )
                        )
                    )
                    .border(0.5.dp, colors.borderSubtle, RoundedCornerShape(8.dp))
            ) {
                if (thumbnailBitmap != null) {
                    Image(
                        bitmap = thumbnailBitmap!!.asImageBitmap(),
                        contentDescription = cleanTitle,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                    // Gradient overlay to keep top-right star and text readable
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(
                                Brush.verticalGradient(
                                    listOf(
                                        Color.Black.copy(alpha = 0.35f),
                                        Color.Transparent,
                                        Color.Black.copy(alpha = 0.6f)
                                    )
                                )
                            )
                    )
                }

                // Top-right frosted favorite button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(8.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.4f),
                        modifier = Modifier
                            .size(30.dp)
                            .clickable(
                                interactionSource = remember { MutableInteractionSource() },
                                indication = null,
                                onClick = onToggleFavorite
                            )
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = if (book.isFavorite) FluentIcons.Star20Filled else FluentIcons.Star20Regular,
                                contentDescription = "Favorite",
                                tint = if (book.isFavorite) Color(0xFFF59E0B) else Color.White,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // Fallback title on cover art if thumbnail is missing
                if (thumbnailBitmap == null) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.Bottom
                    ) {
                        Text(
                            text = cleanTitle,
                            fontFamily = PoppinsFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 13.sp,
                            color = Color.White,
                            lineHeight = 17.sp,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(6.dp))

            // Bottom text stack
            Text(
                text = cleanTitle,
                fontFamily = PoppinsFamily,
                fontWeight = FontWeight.Medium,
                fontSize = 13.sp,
                color = colors.textPrimary,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                lineHeight = 16.sp
            )

            val mb = String.format("%.1f", book.fileSizeBytes / (1024f * 1024f))
            Text(
                text = "${book.pageCount} p • $mb MB",
                fontFamily = InterFamily,
                fontSize = 11.sp,
                color = colors.textSecondary
            )
        }
    } else {
        // List Row Layout
        Surface(
            shape = RoundedCornerShape(12.dp),
            color = colors.surfaceCard,
            border = androidx.compose.foundation.BorderStroke(0.5.dp, colors.borderSubtle),
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(12.dp))
                .combinedClickable(
                    onClick = onOpen,
                    onLongClick = onLongClick
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Mini 3:4 cover thumbnail
                Box(
                    modifier = Modifier
                        .width(42.dp)
                        .height(56.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(colors.primary.copy(alpha = 0.3f)),
                    contentAlignment = Alignment.Center
                ) {
                    if (thumbnailBitmap != null) {
                        Image(
                            bitmap = thumbnailBitmap!!.asImageBitmap(),
                            contentDescription = cleanTitle,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Icon(
                            imageVector = FluentIcons.Book24Regular,
                            contentDescription = null,
                            tint = colors.primary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = cleanTitle,
                        fontFamily = PoppinsFamily,
                        fontWeight = FontWeight.Medium,
                        fontSize = 14.sp,
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${book.pageCount} p • ${book.parentFolder}",
                        fontFamily = InterFamily,
                        fontSize = 11.sp,
                        color = colors.textSecondary
                    )
                }

                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        imageVector = if (book.isFavorite) FluentIcons.Star20Filled else FluentIcons.Star20Regular,
                        contentDescription = "Favorite",
                        tint = if (book.isFavorite) Color(0xFFF59E0B) else colors.textMuted,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
        }
    }
}
