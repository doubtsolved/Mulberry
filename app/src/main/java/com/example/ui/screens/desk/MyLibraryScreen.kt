package com.example.ui.screens.desk

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
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

data class MaterialTypeItem(
    val title: String,
    val count: Int,
    val icon: ImageVector
)

/**
 * My Library Screen (Wireframe 02)
 * Manages user's downloaded, imported, and locally organized materials.
 */
@Composable
fun MyLibraryScreen(
    downloadedBooks: List<BookEntity>,
    onBackClick: () -> Unit,
    onOpenBooksCollection: () -> Unit,
    onOpenBook: (BookEntity) -> Unit,
    onBookActionClick: (BookEntity) -> Unit,
    onImportFilesClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = LocalMulberryColors.current
    var searchQuery by remember { mutableStateOf("") }

    val collections by MulberryHistoryManager.collectionsFlow.collectAsState()
    var showCreateCollectionDialog by remember { mutableStateOf(false) }
    var newCollectionName by remember { mutableStateOf("") }
    var newCollectionDesc by remember { mutableStateOf("") }

    // Derive Material Types dynamically from user's content
    val bookCount = downloadedBooks.count { it.type.contains("Book", ignoreCase = true) || it.type.isBlank() || it.type.contains("Textbook", ignoreCase = true) }
    val notesCount = downloadedBooks.count { it.type.contains("Note", ignoreCase = true) }
    val pyqCount = downloadedBooks.count { it.type.contains("PYQ", ignoreCase = true) || it.type.contains("Question", ignoreCase = true) }
    val handoutsCount = downloadedBooks.count { it.type.contains("Handout", ignoreCase = true) }
    val presentationsCount = downloadedBooks.count { it.type.contains("Slide", ignoreCase = true) || it.type.contains("Presentation", ignoreCase = true) }

    val materialTypes = listOf(
        MaterialTypeItem("Books", if (bookCount > 0) bookCount else downloadedBooks.size, FluentIcons.Book24Filled),
        MaterialTypeItem("Notes", notesCount, FluentIcons.Note24Filled),
        MaterialTypeItem("PYQs", pyqCount, FluentIcons.TaskListSquareLtr24Filled),
        MaterialTypeItem("Handouts", handoutsCount, FluentIcons.DocumentBulletList24Regular),
        MaterialTypeItem("Presentations", presentationsCount, FluentIcons.Grid24Regular),
        MaterialTypeItem("Other Files", (downloadedBooks.size - bookCount - notesCount).coerceAtLeast(0), FluentIcons.Folder24Filled)
    )

    // Recently Opened books (sorted by lastReadTimestamp or lastReadPage)
    val recentlyOpened = remember(downloadedBooks) {
        downloadedBooks
            .filter { it.lastReadPage > 0 || it.lastReadTimestamp > 0L }
            .sortedByDescending { it.lastReadTimestamp }
            .take(6)
            .ifEmpty { downloadedBooks.take(4) }
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
                    .padding(horizontal = 20.dp, vertical = 8.dp)
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
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(4.dp))
                        MulberryExpressiveLogo(size = 28.dp, showContainer = false)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "My Library",
                            fontFamily = PoppinsFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            color = colors.textPrimary
                        )
                    }

                    IconButton(onClick = {}, modifier = Modifier.size(36.dp)) {
                        Icon(
                            imageVector = FluentIcons.MoreVertical24Regular,
                            contentDescription = "Options",
                            tint = colors.textPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                Text(
                    text = "Everything you've collected, in one place.",
                    fontFamily = InterFamily,
                    fontSize = 13.sp,
                    color = colors.textSecondary,
                    modifier = Modifier.padding(start = 40.dp)
                )
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
            verticalArrangement = Arrangement.spacedBy(20.dp)
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
                            text = "Search your books & files...",
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

            // 2. YOUR COLLECTION - Material Types Scroller
            item(key = "material_types_header") {
                Column {
                    Text(
                        text = "YOUR COLLECTION",
                        fontFamily = PoppinsFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        letterSpacing = 0.8.sp,
                        color = colors.primary
                    )
                    Text(
                        text = "Browse by material type",
                        fontFamily = InterFamily,
                        fontSize = 13.sp,
                        color = colors.textSecondary
                    )
                }
            }

            item(key = "material_types_row") {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(materialTypes) { matType ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = colors.surfaceCard,
                            border = androidx.compose.foundation.BorderStroke(1.dp, colors.borderSubtle),
                            modifier = Modifier
                                .width(120.dp)
                                .height(115.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .clickable {
                                    if (matType.title == "Books") {
                                        onOpenBooksCollection()
                                    } else {
                                        Toast.makeText(context, "${matType.title} filter selected", Toast.LENGTH_SHORT).show()
                                    }
                                }
                                .testTag("material_type_${matType.title}")
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(12.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                        .background(colors.primary.copy(alpha = 0.12f)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = matType.icon,
                                        contentDescription = matType.title,
                                        tint = colors.primary,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }

                                Column {
                                    Text(
                                        text = matType.title,
                                        fontFamily = PoppinsFamily,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 14.sp,
                                        color = colors.textPrimary
                                    )
                                    Text(
                                        text = "${matType.count} items",
                                        fontFamily = InterFamily,
                                        fontSize = 11.5.sp,
                                        color = colors.textSecondary
                                    )
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

            // 3. YOUR COLLECTIONS (Curated Reading Lists)
            item(key = "curated_collections_header") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "YOUR COLLECTIONS",
                            fontFamily = PoppinsFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp,
                            letterSpacing = 0.8.sp,
                            color = colors.primary
                        )
                        Text(
                            text = "Your own curated reading lists",
                            fontFamily = InterFamily,
                            fontSize = 13.sp,
                            color = colors.textSecondary
                        )
                    }

                    IconButton(
                        onClick = { showCreateCollectionDialog = true },
                        modifier = Modifier
                            .size(34.dp)
                            .clip(CircleShape)
                            .background(colors.surfaceCard)
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
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    items(collections) { col ->
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = colors.surfaceCard,
                            border = androidx.compose.foundation.BorderStroke(1.dp, colors.borderSubtle),
                            modifier = Modifier
                                .width(160.dp)
                                .height(130.dp)
                                .clip(RoundedCornerShape(16.dp))
                                .clickable {
                                    Toast.makeText(context, "${col.name}: ${col.description.ifEmpty { "Curated collection" }}", Toast.LENGTH_SHORT).show()
                                }
                                .testTag("collection_card_${col.id}")
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(14.dp),
                                verticalArrangement = Arrangement.SpaceBetween
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(RoundedCornerShape(10.dp))
                                            .background(colors.primary.copy(alpha = 0.15f)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        Icon(
                                            imageVector = FluentIcons.Folder24Filled,
                                            contentDescription = null,
                                            tint = colors.primary,
                                            modifier = Modifier.size(18.dp)
                                        )
                                    }
                                }

                                Column {
                                    Text(
                                        text = col.name,
                                        fontFamily = PoppinsFamily,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 14.5.sp,
                                        color = colors.textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Spacer(modifier = Modifier.height(2.dp))
                                    Text(
                                        text = "${col.resourceCount} resources",
                                        fontFamily = InterFamily,
                                        fontSize = 11.5.sp,
                                        color = colors.textSecondary
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 4. RECENTLY OPENED
            item(key = "recently_opened_header") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "RECENTLY OPENED",
                        fontFamily = PoppinsFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        letterSpacing = 0.8.sp,
                        color = colors.primary
                    )
                }
            }

            if (recentlyOpened.isEmpty()) {
                item(key = "recently_opened_empty") {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = colors.surfaceCard,
                        border = androidx.compose.foundation.BorderStroke(1.dp, colors.borderSubtle),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                text = "No books opened yet",
                                fontFamily = PoppinsFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 14.sp,
                                color = colors.textPrimary
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Books you start reading will appear here for fast access.",
                                fontFamily = InterFamily,
                                fontSize = 12.sp,
                                color = colors.textSecondary
                            )
                        }
                    }
                }
            } else {
                items(recentlyOpened, key = { "recent_${it.id}" }) { book ->
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = colors.surfaceCard,
                        border = androidx.compose.foundation.BorderStroke(0.5.dp, colors.borderSubtle),
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable { onOpenBook(book) }
                            .testTag("recent_opened_item_${book.id}")
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = colors.primary.copy(alpha = 0.1f),
                                modifier = Modifier
                                    .size(width = 40.dp, height = 52.dp)
                                    .clip(RoundedCornerShape(8.dp))
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
                                            modifier = Modifier.size(20.dp)
                                        )
                                    }
                                }
                            }

                            Spacer(modifier = Modifier.width(14.dp))

                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = book.title,
                                    fontFamily = PoppinsFamily,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 14.sp,
                                    color = colors.textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = if (book.lastReadPage > 1) "PDF · Page ${book.lastReadPage}" else "PDF · ${book.subject.ifEmpty { "General" }}",
                                    fontFamily = InterFamily,
                                    fontSize = 12.sp,
                                    color = colors.textSecondary
                                )
                            }

                            IconButton(onClick = { onBookActionClick(book) }) {
                                Icon(
                                    imageVector = FluentIcons.MoreVertical24Regular,
                                    contentDescription = "Options",
                                    tint = colors.textSecondary,
                                    modifier = Modifier.size(18.dp)
                                )
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

    // Create Collection Dialog
    if (showCreateCollectionDialog) {
        AlertDialog(
            onDismissRequest = { showCreateCollectionDialog = false },
            title = {
                Text(
                    text = "New Reading List",
                    fontFamily = PoppinsFamily,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Group textbooks and study resources according to your personal study priorities.",
                        fontFamily = InterFamily,
                        fontSize = 13.sp,
                        color = colors.textSecondary
                    )
                    OutlinedTextField(
                        value = newCollectionName,
                        onValueChange = { newCollectionName = it },
                        label = { Text("Collection Name (e.g. Clinical Postings)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.primary,
                            unfocusedBorderColor = colors.borderSubtle
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                    OutlinedTextField(
                        value = newCollectionDesc,
                        onValueChange = { newCollectionDesc = it },
                        label = { Text("Description (Optional)") },
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = colors.primary,
                            unfocusedBorderColor = colors.borderSubtle
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newCollectionName.isNotBlank()) {
                            MulberryHistoryManager.addCollection(context, newCollectionName, newCollectionDesc)
                            showCreateCollectionDialog = false
                            newCollectionName = ""
                            newCollectionDesc = ""
                            Toast.makeText(context, "Collection created", Toast.LENGTH_SHORT).show()
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
                ) {
                    Text("Create", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateCollectionDialog = false }) {
                    Text("Cancel", color = colors.textPrimary)
                }
            },
            containerColor = colors.surfaceCard
        )
    }
}
