package com.example.ui.screens.desk

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.data.model.BookEntity
import com.example.ui.components.FluentIcons
import com.example.ui.components.MulberryExpressiveLogo
import com.example.ui.theme.InterFamily
import com.example.ui.theme.LocalMulberryColors
import com.example.ui.theme.PoppinsFamily
import com.example.util.CustomCollectionItem
import com.example.util.MulberryHistoryManager
import com.example.util.MulberryStorageManager

data class DynamicTypeFolder(
    val typeName: String,
    val count: Int,
    val icon: ImageVector,
    val books: List<BookEntity>
)

/**
 * Curated palette of eye-pleasing, vibrant solid gradients providing high theme contrast.
 */
val VIBRANT_FOLDER_GRADIENTS = listOf(
    Brush.linearGradient(listOf(Color(0xFF4338CA), Color(0xFF6366F1))), // Royal Indigo
    Brush.linearGradient(listOf(Color(0xFF047857), Color(0xFF10B981))), // Vibrant Emerald
    Brush.linearGradient(listOf(Color(0xFFBE123C), Color(0xFFF43F5E))), // Rich Crimson Rose
    Brush.linearGradient(listOf(Color(0xFFD97706), Color(0xFFF59E0B))), // Warm Amber Sun
    Brush.linearGradient(listOf(Color(0xFF0369A1), Color(0xFF0EA5E9))), // Deep Cyan Ocean
    Brush.linearGradient(listOf(Color(0xFF7E22CE), Color(0xFFA855F7))), // Imperial Purple
    Brush.linearGradient(listOf(Color(0xFF0F766E), Color(0xFF14B8A6))), // Sea Teal
    Brush.linearGradient(listOf(Color(0xFFC2410C), Color(0xFFFB923C)))  // Sunset Coral
)

/**
 * My Library Screen (Wireframe 02)
 * Refined per user specifications:
 * - Fetches downloaded books directly from /sdcard/Mulberry/downloaded_books.json
 * - Removed subtext from My Library header
 * - First section renamed to "Downloaded Books" with enlarged font size (18sp) and removed subtext
 * - Second section "YOUR COLLECTIONS" with enlarged font size (18sp) and removed subtext
 * - Type folders dynamically generated when >= 1 book present
 * - Eye-pleasing vibrant solid gradient cards with theme contrast
 * - Functional custom collections backed by /sdcard/Mulberry/.history/collections.json
 */
@Composable
fun MyLibraryScreen(
    downloadedBooks: List<BookEntity>,
    onBackClick: () -> Unit,
    onOpenTypeFolder: (typeName: String, books: List<BookEntity>) -> Unit,
    onOpenCollection: (collection: CustomCollectionItem, books: List<BookEntity>) -> Unit,
    onOpenBook: (BookEntity) -> Unit,
    onBookActionClick: (BookEntity) -> Unit,
    onImportFilesClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = LocalMulberryColors.current
    var searchQuery by remember { mutableStateOf("") }

    // Direct fetch from /sdcard/Mulberry/downloaded_books.json with fallback to prop
    val activeBooks = remember(downloadedBooks) {
        val directJson = MulberryStorageManager.loadDownloadedBooksAsEntities(context)
        if (directJson.isNotEmpty()) directJson else downloadedBooks
    }

    val collections by MulberryHistoryManager.collectionsFlow.collectAsState()
    var showCreateCollectionDialog by remember { mutableStateOf(false) }
    var newCollectionName by remember { mutableStateOf("") }
    var newCollectionDesc by remember { mutableStateOf("") }

    // Dynamically derive Type Folders based on the metadata in downloaded_books.json
    // ONLY show a folder when >= 1 book is present for that type!
    val dynamicTypeFolders = remember(activeBooks) {
        val map = linkedMapOf<String, MutableList<BookEntity>>()

        for (book in activeBooks) {
            val raw = book.type.trim()
            val category = when {
                raw.contains("Book", ignoreCase = true) || raw.contains("Textbook", ignoreCase = true) -> "Books"
                raw.contains("Note", ignoreCase = true) -> "Notes"
                raw.contains("PYQ", ignoreCase = true) || raw.contains("Question", ignoreCase = true) -> "PYQs"
                raw.contains("Handout", ignoreCase = true) -> "Handouts"
                raw.contains("Slide", ignoreCase = true) || raw.contains("Presentation", ignoreCase = true) -> "Presentations"
                raw.isNotBlank() -> raw.replaceFirstChar { it.uppercase() }
                else -> "Books"
            }
            map.getOrPut(category) { mutableListOf() }.add(book)
        }

        // Map into folder objects only when count >= 1
        map.filter { it.value.isNotEmpty() }.map { (type, list) ->
            val icon = when (type) {
                "Books" -> FluentIcons.Book24Filled
                "Notes" -> FluentIcons.Note24Filled
                "PYQs" -> FluentIcons.TaskListSquareLtr24Filled
                "Handouts" -> FluentIcons.DocumentBulletList24Regular
                "Presentations" -> FluentIcons.Grid24Regular
                else -> FluentIcons.Folder24Filled
            }
            DynamicTypeFolder(
                typeName = type,
                count = list.size,
                icon = icon,
                books = list
            )
        }
    }

    // Recently Opened books (filtered by last read activity)
    val recentlyOpened = remember(activeBooks) {
        activeBooks
            .filter { it.lastReadPage > 0 || it.lastReadTimestamp > 0L }
            .sortedByDescending { it.lastReadTimestamp }
            .take(6)
            .ifEmpty { activeBooks.take(4) }
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
                    .padding(horizontal = 20.dp, vertical = 12.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        IconButton(onClick = onBackClick, modifier = Modifier.size(36.dp)) {
                            Icon(
                                imageVector = FluentIcons.ArrowLeft24Regular,
                                contentDescription = "Back to Desk",
                                tint = colors.textPrimary,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(6.dp))
                        MulberryExpressiveLogo(size = 30.dp, showContainer = false)
                        Spacer(modifier = Modifier.width(10.dp))
                        // Increased font size of "My Library" (26sp Poppins Bold), subtext removed
                        Text(
                            text = "My Library",
                            fontFamily = PoppinsFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 26.sp,
                            color = colors.textPrimary,
                            letterSpacing = (-0.5).sp
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = colors.primary.copy(alpha = 0.12f)
                    ) {
                        Text(
                            text = "${activeBooks.size} files",
                            fontFamily = PoppinsFamily,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.5.sp,
                            color = colors.primary,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                        )
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
                    onClick = onImportFilesClick,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("btn_my_library_add_files"),
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
                        text = "Add files & imports",
                        fontFamily = PoppinsFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.5.sp
                    )
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            contentPadding = PaddingValues(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(22.dp)
        ) {
            // 1. Search Bar
            item(key = "search_bar") {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                        .testTag("my_library_search_input"),
                    placeholder = {
                        Text(
                            text = "Search books, notes, materials...",
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
            }

            // 2. DOWNLOADED BOOKS (Renamed from YOUR COLLECTION, enlarged font size 18sp, subtext removed)
            item(key = "downloaded_books_header") {
                Text(
                    text = "Downloaded Books",
                    fontFamily = PoppinsFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp,
                    color = colors.textPrimary,
                    letterSpacing = (-0.3).sp
                )
            }

            // Dynamic Type Folders in Downloaded Books
            item(key = "downloaded_books_row") {
                if (dynamicTypeFolders.isEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = colors.surfaceCard,
                        border = androidx.compose.foundation.BorderStroke(1.dp, colors.borderSubtle),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "No downloaded materials yet",
                                fontFamily = PoppinsFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = colors.textPrimary
                            )
                            Text(
                                text = "Download books or notes from the catalog to organize them here.",
                                fontFamily = InterFamily,
                                fontSize = 12.5.sp,
                                color = colors.textSecondary
                            )
                        }
                    }
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        itemsIndexed(dynamicTypeFolders) { index, folder ->
                            val gradient = VIBRANT_FOLDER_GRADIENTS[index % VIBRANT_FOLDER_GRADIENTS.size]

                            // Vibrant solid gradient card with theme contrast
                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                shadowElevation = 3.dp,
                                modifier = Modifier
                                    .width(132.dp)
                                    .height(125.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .clickable {
                                        onOpenTypeFolder(folder.typeName, folder.books)
                                    }
                                    .testTag("folder_type_${folder.typeName}")
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(gradient)
                                        .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(18.dp))
                                        .padding(14.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        // Top Icon Badge
                                        Box(
                                            modifier = Modifier
                                                .size(38.dp)
                                                .clip(RoundedCornerShape(10.dp))
                                                .background(Color.White.copy(alpha = 0.22f)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = folder.icon,
                                                contentDescription = folder.typeName,
                                                tint = Color.White,
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }

                                        // Bottom Text
                                        Column {
                                            Text(
                                                text = folder.typeName,
                                                fontFamily = PoppinsFamily,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp,
                                                color = Color.White,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = "${folder.count} items",
                                                fontFamily = InterFamily,
                                                fontWeight = FontWeight.Medium,
                                                fontSize = 12.sp,
                                                color = Color.White.copy(alpha = 0.88f)
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // Divider
            item(key = "divider_1") {
                HorizontalDivider(color = colors.borderSubtle, thickness = 0.5.dp)
            }

            // 3. YOUR COLLECTIONS (Functional, enlarged font size 18sp, subtext removed)
            item(key = "curated_collections_header") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "YOUR COLLECTIONS",
                        fontFamily = PoppinsFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 18.sp,
                        color = colors.textPrimary,
                        letterSpacing = (-0.3).sp
                    )

                    IconButton(
                        onClick = { showCreateCollectionDialog = true },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(colors.primary.copy(alpha = 0.14f))
                            .testTag("btn_create_custom_collection")
                    ) {
                        Icon(
                            imageVector = FluentIcons.Add24Regular,
                            contentDescription = "Create collection",
                            tint = colors.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }

            item(key = "curated_collections_row") {
                if (collections.isEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = colors.surfaceCard,
                        border = androidx.compose.foundation.BorderStroke(1.dp, colors.borderSubtle),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            Text(
                                text = "Create custom collections",
                                fontFamily = PoppinsFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = colors.textPrimary
                            )
                            Text(
                                text = "Group any textbooks, notes, or handouts into custom lists. Tap '+' to create.",
                                fontFamily = InterFamily,
                                fontSize = 12.5.sp,
                                color = colors.textSecondary
                            )
                        }
                    }
                } else {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(14.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        itemsIndexed(collections) { index, col ->
                            // Vibrant solid gradient cards for collections as well
                            val gradient = VIBRANT_FOLDER_GRADIENTS[(index + 3) % VIBRANT_FOLDER_GRADIENTS.size]

                            // Filter books that belong to this collection
                            val collectionBooks = activeBooks.filter { b ->
                                val slug = b.slug.ifEmpty { "book_${b.id}" }
                                col.bookSlugs.contains(slug) || col.bookSlugs.contains(b.title)
                            }
                            val displayCount = if (collectionBooks.isNotEmpty()) collectionBooks.size else col.resourceCount

                            Surface(
                                shape = RoundedCornerShape(18.dp),
                                shadowElevation = 3.dp,
                                modifier = Modifier
                                    .width(160.dp)
                                    .height(130.dp)
                                    .clip(RoundedCornerShape(18.dp))
                                    .clickable {
                                        onOpenCollection(col, collectionBooks)
                                    }
                                    .testTag("collection_card_${col.id}")
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(gradient)
                                        .border(1.dp, Color.White.copy(alpha = 0.25f), RoundedCornerShape(18.dp))
                                        .padding(14.dp)
                                ) {
                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Box(
                                                modifier = Modifier
                                                    .size(38.dp)
                                                    .clip(RoundedCornerShape(10.dp))
                                                .background(Color.White.copy(alpha = 0.22f)),
                                                contentAlignment = Alignment.Center
                                            ) {
                                                Icon(
                                                    imageVector = FluentIcons.Folder24Filled,
                                                    contentDescription = null,
                                                    tint = Color.White,
                                                    modifier = Modifier.size(20.dp)
                                                )
                                            }

                                            Surface(
                                                shape = RoundedCornerShape(8.dp),
                                                color = Color.Black.copy(alpha = 0.18f)
                                            ) {
                                                Text(
                                                    text = "$displayCount items",
                                                    fontFamily = InterFamily,
                                                    fontWeight = FontWeight.SemiBold,
                                                    fontSize = 11.sp,
                                                    color = Color.White,
                                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                                )
                                            }
                                        }

                                        Column {
                                            Text(
                                                text = col.name,
                                                fontFamily = PoppinsFamily,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 15.sp,
                                                color = Color.White,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            if (col.description.isNotBlank()) {
                                                Spacer(modifier = Modifier.height(2.dp))
                                                Text(
                                                    text = col.description,
                                                    fontFamily = InterFamily,
                                                    fontSize = 11.5.sp,
                                                    color = Color.White.copy(alpha = 0.85f),
                                                    maxLines = 1,
                                                    overflow = TextOverflow.Ellipsis
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

            // Divider
            item(key = "divider_2") {
                HorizontalDivider(color = colors.borderSubtle, thickness = 0.5.dp)
            }

            // 4. RECENTLY OPENED
            if (recentlyOpened.isNotEmpty()) {
                item(key = "recently_opened_header") {
                    Text(
                        text = "RECENTLY OPENED",
                        fontFamily = PoppinsFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        letterSpacing = 0.8.sp,
                        color = colors.primary
                    )
                }

                item(key = "recently_opened_carousel") {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        items(recentlyOpened, key = { "rec_${it.id}" }) { book ->
                            Surface(
                                shape = RoundedCornerShape(16.dp),
                                color = colors.surfaceCard,
                                border = androidx.compose.foundation.BorderStroke(1.dp, colors.borderSubtle),
                                modifier = Modifier
                                    .width(140.dp)
                                    .clip(RoundedCornerShape(16.dp))
                                    .clickable { onBookActionClick(book) }
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
                                        text = if (book.lastReadPage > 0) "Page ${book.lastReadPage}" else book.subject.ifEmpty { "Medical" },
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

            item(key = "bottom_space") {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }

    // Create New Custom Collection Dialog
    if (showCreateCollectionDialog) {
        AlertDialog(
            onDismissRequest = { showCreateCollectionDialog = false },
            title = {
                Text(
                    text = "New Collection",
                    fontFamily = PoppinsFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = newCollectionName,
                        onValueChange = { newCollectionName = it },
                        label = { Text("Collection name") },
                        placeholder = { Text("e.g. 1st Prof Biochemistry") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newCollectionDesc,
                        onValueChange = { newCollectionDesc = it },
                        label = { Text("Description (optional)") },
                        placeholder = { Text("e.g. High-yield enzymes & metabolic pathways") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newCollectionName.isNotBlank()) {
                            MulberryHistoryManager.addCollection(context, newCollectionName, newCollectionDesc)
                            Toast.makeText(context, "Collection \"$newCollectionName\" created!", Toast.LENGTH_SHORT).show()
                            newCollectionName = ""
                            newCollectionDesc = ""
                            showCreateCollectionDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
                ) {
                    Text("Create", color = colors.onPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateCollectionDialog = false }) {
                    Text("Cancel", color = colors.textSecondary)
                }
            }
        )
    }
}
