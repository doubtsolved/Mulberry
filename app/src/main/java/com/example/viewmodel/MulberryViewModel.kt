package com.example.viewmodel

import android.app.Application
import android.content.Context
import android.graphics.pdf.PdfRenderer
import android.os.Environment
import android.os.ParcelFileDescriptor
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.MulberryDatabase
import com.example.data.model.AgendaTaskEntity
import com.example.data.model.AnnotationEntity
import com.example.data.model.BookEntity
import com.example.data.model.ExamEntity
import com.example.data.model.VaultEntity
import com.example.data.model.UserProfile
import com.example.data.repository.MulberryRepository
import com.example.data.repository.UserProfileRepository
import com.example.data.repository.VaultMetadataManager
import com.example.ui.navigation.MulberryTab
import com.example.ui.theme.ThemeMode
import com.example.util.PdfAnnotationManager
import com.example.util.ThumbnailManager
import com.example.util.toCleanBookTitle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File

val VaultEntity.rootPath: String get() = pathDisplay.ifEmpty { uriString }

enum class InkingTool {
    ERASER,
    PEN_BLACK,
    PEN_RED,
    PEN_BLUE,
    HIGHLIGHTER,
    NOTE
}

enum class ShapeType {
    RECTANGLE,
    CIRCLE,
    ARROW,
    LINE
}

sealed class ActiveTool {
    object None : ActiveTool()
    data class Pen(val color: Color, val width: Float = 1.5f) : ActiveTool()
    data class Highlighter(val color: Color = Color(0xFFFACC15), val width: Float = 18f) : ActiveTool()
    object Eraser : ActiveTool()
    object Text : ActiveTool()
    data class Shape(val shapeType: ShapeType = ShapeType.RECTANGLE, val color: Color = Color(0xFF0284C7), val width: Float = 2.5f) : ActiveTool()
    object StickyNote : ActiveTool()
}

enum class ReaderViewMode {
    VERTICAL_CONTINUOUS,
    HORIZONTAL_PAGINATED
}

enum class BookSortOption(val title: String) {
    RECENTLY_READ("Recently Read"),
    TITLE_AZ("Title (A to Z)"),
    DATE_MODIFIED("Date Added / Modified"),
    FILE_SIZE("File Size (Largest)")
}

class MulberryViewModel(application: Application) : AndroidViewModel(application) {
    private val db = MulberryDatabase.getInstance(application)
    private val repository = MulberryRepository(
        vaultDao = db.vaultDao(),
        bookDao = db.bookDao(),
        agendaTaskDao = db.agendaTaskDao(),
        examDao = db.examDao(),
        annotationDao = db.annotationDao()
    )

    private val prefs = application.getSharedPreferences("mulberry_prefs", Context.MODE_PRIVATE)
    val userProfileRepository = UserProfileRepository.getInstance(application)
    val userProfile: StateFlow<UserProfile> = userProfileRepository.userProfile

    fun saveUserProfile(updated: UserProfile) {
        viewModelScope.launch {
            userProfileRepository.updateProfile(updated, activeVault.value?.rootPath)
        }
    }

    // Global Theme persisted in SharedPreferences
    private val _themeMode = MutableStateFlow(
        ThemeMode.from(prefs.getString("theme_mode", ThemeMode.PAPER.name))
    )
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    // Dynamic Icon Style
    private val _iconStyle = MutableStateFlow(
        try {
            com.example.ui.theme.IconStyle.valueOf(
                prefs.getString("ICON_STYLE_KEY", com.example.ui.theme.IconStyle.VIBRANT.name) ?: com.example.ui.theme.IconStyle.VIBRANT.name
            )
        } catch (_: Exception) {
            com.example.ui.theme.IconStyle.VIBRANT
        }
    )
    val iconStyle: StateFlow<com.example.ui.theme.IconStyle> = _iconStyle.asStateFlow()

    fun setIconStyle(style: com.example.ui.theme.IconStyle) {
        _iconStyle.value = style
        prefs.edit().putString("ICON_STYLE_KEY", style.name).apply()
    }

    // Navigation Tab
    private val _currentTab = MutableStateFlow(MulberryTab.LIBRARY)
    val currentTab: StateFlow<MulberryTab> = _currentTab.asStateFlow()

    // Search
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isSearchExpanded = MutableStateFlow(false)
    val isSearchExpanded: StateFlow<Boolean> = _isSearchExpanded.asStateFlow()

    // Sorting
    private val _sortOption = MutableStateFlow(BookSortOption.RECENTLY_READ)
    val sortOption: StateFlow<BookSortOption> = _sortOption.asStateFlow()

    // View switcher (2-Column Grid vs Compact List)
    private val _isGridView = MutableStateFlow(true)
    val isGridView: StateFlow<Boolean> = _isGridView.asStateFlow()

    // Breadcrumb path: e.g. ["Vault", "Medical", "Physiology"]
    private val _activeBreadcrumbs = MutableStateFlow(listOf("Vault"))
    val activeBreadcrumbs: StateFlow<List<String>> = _activeBreadcrumbs.asStateFlow()

    // Filter folder
    private val _activeFolderFilter = MutableStateFlow<String?>(null)
    val activeFolderFilter: StateFlow<String?> = _activeFolderFilter.asStateFlow()

    // Active Vault tracking in SharedPreferences
    private val _activeVaultId = MutableStateFlow<String?>(
        prefs.getString("active_vault_id", null)
    )
    val activeVaultId: StateFlow<String?> = _activeVaultId.asStateFlow()

    // Database flows
    val allVaults: StateFlow<List<VaultEntity>> = repository.allVaults.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val activeVault: StateFlow<VaultEntity?> = combine(allVaults, _activeVaultId) { vaults, idStr ->
        if (vaults.isEmpty()) return@combine null
        vaults.find { it.id.toString() == idStr } ?: vaults.firstOrNull()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    val allBooks: StateFlow<List<BookEntity>> = repository.allBooks.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Filter continueReadingBooks by paths starting with activeVault.rootPath
    val continueReadingBooks: StateFlow<List<BookEntity>> = combine(
        repository.continueReadingBooks,
        activeVault
    ) { books, vault ->
        if (vault == null) {
            books
        } else {
            val root = vault.pathDisplay.ifEmpty { vault.uriString }
            books.filter { book ->
                book.vaultId == vault.id || (root.isNotEmpty() && book.uriString.startsWith(root))
            }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allTasks: StateFlow<List<AgendaTaskEntity>> = repository.allTasks.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allExams: StateFlow<List<ExamEntity>> = repository.allExams.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Filtered & Sorted Books based on active vault root, search query, folder filter, and sort order
    val filteredBooks: StateFlow<List<BookEntity>> = combine(
        repository.allBooks,
        _searchQuery,
        _activeFolderFilter,
        _sortOption,
        activeVault
    ) { books, query, folder, sort, vault ->
        var list = if (vault != null) {
            val root = vault.pathDisplay.ifEmpty { vault.uriString }
            books.filter { it.vaultId == vault.id || (root.isNotEmpty() && it.uriString.startsWith(root)) }
        } else {
            books
        }
        if (!folder.isNullOrEmpty()) {
            list = list.filter { it.parentFolder.equals(folder, ignoreCase = true) }
        }
        if (query.isNotBlank()) {
            val q = query.trim().lowercase()
            list = list.filter {
                it.title.lowercase().contains(q) ||
                    it.author.lowercase().contains(q) ||
                    it.fileName.lowercase().contains(q) ||
                    it.parentFolder.lowercase().contains(q)
            }
        }
        when (sort) {
            BookSortOption.RECENTLY_READ -> list.sortedByDescending { it.lastReadTimestamp }
            BookSortOption.TITLE_AZ -> list.sortedBy { it.title.lowercase() }
            BookSortOption.DATE_MODIFIED -> list.sortedByDescending {
                val f = File(it.uriString)
                if (f.exists()) f.lastModified() else it.id.toLong()
            }
            BookSortOption.FILE_SIZE -> list.sortedByDescending { it.fileSizeBytes }
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Unique folders derived from books in active vault
    val availableFolders: StateFlow<List<Pair<String, Int>>> = combine(
        repository.allBooks,
        activeVault,
        _activeFolderFilter
    ) { books, vault, _ ->
        val vaultBooks = if (vault != null) {
            val root = vault.pathDisplay.ifEmpty { vault.uriString }
            books.filter { it.vaultId == vault.id || (root.isNotEmpty() && it.uriString.startsWith(root)) }
        } else {
            books
        }
        val counts = mutableMapOf<String, Int>()
        vaultBooks.forEach { book ->
            val folder = book.parentFolder.ifEmpty { "General" }
            counts[folder] = (counts[folder] ?: 0) + 1
        }
        counts.map { it.key to it.value }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Full-screen Reader State
    private val _isReaderOpen = MutableStateFlow(false)
    val isReaderOpen: StateFlow<Boolean> = _isReaderOpen.asStateFlow()

    private val _currentReadingBook = MutableStateFlow<BookEntity?>(null)
    val currentReadingBook: StateFlow<BookEntity?> = _currentReadingBook.asStateFlow()

    private val _readerCurrentPage = MutableStateFlow(1)
    val readerCurrentPage: StateFlow<Int> = _readerCurrentPage.asStateFlow()

    private val _readerTotalPages = MutableStateFlow(1)
    val readerTotalPages: StateFlow<Int> = _readerTotalPages.asStateFlow()

    private val _readerNightMode = MutableStateFlow(false)
    val readerNightMode: StateFlow<Boolean> = _readerNightMode.asStateFlow()

    private val _readerViewMode = MutableStateFlow(ReaderViewMode.VERTICAL_CONTINUOUS)
    val readerViewMode: StateFlow<ReaderViewMode> = _readerViewMode.asStateFlow()

    private val _isInkingDockOpen = MutableStateFlow(false)
    val isInkingDockOpen: StateFlow<Boolean> = _isInkingDockOpen.asStateFlow()

    private val _activeInkingTool = MutableStateFlow(InkingTool.PEN_BLUE)
    val activeInkingTool: StateFlow<InkingTool> = _activeInkingTool.asStateFlow()

    private val _activeInkingColor = MutableStateFlow(Color(0xFF0284C7))
    val activeInkingColor: StateFlow<Color> = _activeInkingColor.asStateFlow()

    private val _activeInkingStrokeWidth = MutableStateFlow(1.5f)
    val activeInkingStrokeWidth: StateFlow<Float> = _activeInkingStrokeWidth.asStateFlow()

    private val _activeTool = MutableStateFlow<ActiveTool>(ActiveTool.None)
    val activeTool: StateFlow<ActiveTool> = _activeTool.asStateFlow()

    private val _redoStack = mutableListOf<AnnotationEntity>()
    private val _canRedo = MutableStateFlow(false)
    val canRedo: StateFlow<Boolean> = _canRedo.asStateFlow()

    // Reactive flow of ALL annotations for the currently active book
    val allBookAnnotations: StateFlow<List<AnnotationEntity>> = _currentReadingBook.flatMapLatest { book ->
        if (book != null) {
            repository.getAllAnnotationsForBook(book.id)
        } else {
            flowOf(emptyList())
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Page annotations flow (for active page)
    val currentAnnotations: StateFlow<List<AnnotationEntity>> = combine(
        allBookAnnotations,
        _readerCurrentPage
    ) { all, page ->
        all.filter { it.pageIndex == page }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val canUndo: StateFlow<Boolean> = allBookAnnotations.combine(_currentReadingBook) { list, _ ->
        list.isNotEmpty()
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = false
    )

    // Delta Sync State
    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()
    private var syncJob: Job? = null

    init {
        // Startup cleanup: prevent and eliminate duplicate books in Room DB
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val all = repository.getAllBooksSync()
                val seenUris = mutableSetOf<String>()
                val duplicates = mutableListOf<BookEntity>()
                for (b in all) {
                    if (!seenUris.add(b.uriString)) {
                        duplicates.add(b)
                    }
                }
                for (dup in duplicates) {
                    repository.deleteBook(dup)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Sync breadcrumbs with active vault name and auto-sync delta on vault change/initialization
        viewModelScope.launch {
            activeVault.collect { vault ->
                val vaultName = vault?.name ?: "Vault"
                val folder = _activeFolderFilter.value
                _activeBreadcrumbs.value = if (folder != null) listOf(vaultName, folder) else listOf(vaultName)
                userProfileRepository.setActiveVaultPath(vault?.rootPath)
                if (vault != null) {
                    refreshLibrary()
                }
            }
        }
    }

    fun refreshLibrary() {
        val currentVault = activeVault.value ?: allVaults.value.firstOrNull() ?: return
        val rootPath = currentVault.pathDisplay.ifEmpty { currentVault.uriString }
        if (rootPath.isBlank()) return

        if (syncJob?.isActive == true) return

        syncJob = viewModelScope.launch(Dispatchers.IO) {
            _isSyncing.value = true
            try {
                repository.syncVaultWithDisk(rootPath, currentVault.id.toString())
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                _isSyncing.value = false
            }
        }
    }

    fun onAppResume() {
        refreshLibrary()
    }

    // Actions
    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        prefs.edit().putString("theme_mode", mode.name).apply()
    }

    fun setActiveVault(vaultId: Int) {
        setActiveVault(vaultId.toString())
    }

    fun setActiveVault(vaultId: String?) {
        _activeVaultId.value = vaultId
        prefs.edit().putString("active_vault_id", vaultId).apply()
        _activeFolderFilter.value = null
        val currentVault = allVaults.value.find { it.id.toString() == vaultId }
        _activeBreadcrumbs.value = listOf(currentVault?.name ?: "Vault")
        refreshLibrary()
    }

    fun setTab(tab: MulberryTab) {
        _currentTab.value = tab
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun toggleSearch() {
        _isSearchExpanded.value = !_isSearchExpanded.value
        if (!_isSearchExpanded.value) {
            _searchQuery.value = ""
        }
    }

    fun setSortOption(option: BookSortOption) {
        _sortOption.value = option
    }

    fun toggleGridView() {
        _isGridView.value = !_isGridView.value
    }

    fun setFolderFilter(folder: String?) {
        _activeFolderFilter.value = folder
        val vaultName = activeVault.value?.name ?: "Vault"
        if (folder != null) {
            _activeBreadcrumbs.value = listOf(vaultName, folder)
        } else {
            _activeBreadcrumbs.value = listOf(vaultName)
        }
    }

    fun navigateBreadcrumb(index: Int) {
        val current = _activeBreadcrumbs.value
        val vaultName = activeVault.value?.name ?: "Vault"
        if (index <= 0) {
            _activeBreadcrumbs.value = listOf(vaultName)
            _activeFolderFilter.value = null
        } else if (index < current.size) {
            val selected = current[index]
            _activeBreadcrumbs.value = current.take(index + 1)
            _activeFolderFilter.value = selected
        }
    }

    // SAF URI to Raw Path conversion & binding
    fun bindFolderUri(uri: android.net.Uri, vaultName: String) {
        val rawPath = resolveUriToFilePath(uri)
        bindVaultPath(rawPath, vaultName)
    }

    private fun resolveUriToFilePath(treeUri: android.net.Uri): String {
        return try {
            val docId = android.provider.DocumentsContract.getTreeDocumentId(treeUri)
            if (docId.startsWith("primary:")) {
                val relative = docId.removePrefix("primary:")
                Environment.getExternalStorageDirectory().absolutePath + if (relative.startsWith("/")) relative else "/$relative"
            } else if (docId.startsWith("raw:")) {
                docId.removePrefix("raw:")
            } else {
                val path = treeUri.path ?: ""
                if (path.contains("primary:")) {
                    val relative = path.substringAfter("primary:")
                    Environment.getExternalStorageDirectory().absolutePath + if (relative.startsWith("/")) relative else "/$relative"
                } else {
                    File(Environment.getExternalStorageDirectory(), docId.substringAfterLast(":")).absolutePath
                }
            }
        } catch (e: Exception) {
            val path = treeUri.path ?: ""
            if (path.contains("primary:")) {
                val relative = path.substringAfter("primary:")
                Environment.getExternalStorageDirectory().absolutePath + if (relative.startsWith("/")) relative else "/$relative"
            } else {
                File(Environment.getExternalStorageDirectory(), "Documents").absolutePath
            }
        }
    }

    // Raw Path Vault Scanning (MANAGE_EXTERNAL_STORAGE / Direct java.io.File)
    fun bindVaultPath(folderPath: String, vaultName: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val folder = File(folderPath)
            if (!folder.exists()) {
                folder.mkdirs()
            }

            val canonicalPath = folder.absolutePath
            prefs.edit()
                .putString("last_vault_path", canonicalPath)
                .apply()

            val vaultId = repository.addVault(
                name = vaultName,
                uriString = canonicalPath,
                pathDisplay = canonicalPath
            ).toInt()

            setActiveVault(vaultId)

            scanAndImportDirectory(folder, vaultId, vaultName)
        }
    }

    private suspend fun scanAndImportDirectory(
        rootFolder: File,
        vaultId: Int,
        vaultName: String
    ) {
        try {
            val foundBooks = mutableListOf<BookEntity>()
            val existingMap = repository.getAllBooksSync().associateBy { it.uriString }

            // Standard java.io.File walkTopDown traversal
            val pdfFiles = rootFolder.walkTopDown()
                .filter { it.isFile && it.extension.equals("pdf", ignoreCase = true) }
                .toList()

            for (file in pdfFiles) {
                val rawPath = file.absolutePath
                val fileName = file.name
                val sanitizedTitle = fileName.toCleanBookTitle()
                val sizeBytes = file.length()
                val parentCategory = file.parentFile?.name ?: vaultName

                var pageCount = 1
                try {
                    ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)?.use { pfd ->
                        PdfRenderer(pfd).use { renderer ->
                            pageCount = renderer.pageCount.coerceAtLeast(1)
                        }
                    }
                } catch (e: Exception) {
                    pageCount = 1
                }

                val existing = existingMap[rawPath]
                if (existing != null) {
                    foundBooks.add(
                        existing.copy(
                            title = if (existing.title.isBlank()) sanitizedTitle else existing.title,
                            pageCount = if (pageCount > 1) pageCount else existing.pageCount,
                            fileSizeBytes = sizeBytes,
                            parentFolder = parentCategory
                        )
                    )
                } else {
                    foundBooks.add(
                        BookEntity(
                            vaultId = vaultId,
                            title = sanitizedTitle,
                            author = "",
                            fileName = fileName,
                            uriString = rawPath,
                            pageCount = pageCount,
                            fileSizeBytes = sizeBytes,
                            isFavorite = false,
                            lastReadPage = 1,
                            progressPercent = 0,
                            lastReadTimestamp = 0L,
                            parentFolder = parentCategory
                        )
                    )
                }
            }

            if (foundBooks.isNotEmpty()) {
                val distinctBooks = foundBooks.distinctBy { it.uriString }
                repository.addBooks(distinctBooks)

                val allVaultsList = repository.getAllVaultsSync()
                val vault = allVaultsList.find { it.id == vaultId }
                if (vault != null) {
                    repository.updateVault(vault.copy(bookCount = distinctBooks.size))
                }

                // Pre-generate thumbnails
                val app = getApplication<Application>()
                distinctBooks.forEach { b ->
                    ThumbnailManager.getOrGenerateThumbnail(app, b.id, b.uriString)
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    fun renameVault(vault: VaultEntity, newName: String) {
        viewModelScope.launch {
            repository.updateVault(vault.copy(name = newName))
        }
    }

    fun deleteVault(vault: VaultEntity) {
        viewModelScope.launch {
            repository.deleteVault(vault)
        }
    }

    fun toggleBookFavorite(bookId: Int, isFavorite: Boolean) {
        viewModelScope.launch {
            repository.toggleFavorite(bookId, isFavorite)
            val reading = _currentReadingBook.value
            if (reading != null && reading.id == bookId) {
                _currentReadingBook.value = reading.copy(isFavorite = isFavorite)
            }
            activeVault.value?.let { vault ->
                val root = vault.pathDisplay.ifEmpty { vault.uriString }
                if (root.isNotBlank()) {
                    VaultMetadataManager.persistVaultMetadata(root, vault.id.toString(), getApplication())
                }
            }
        }
    }

    // Direct Disk File Operations (Rename & Delete)
    fun deleteBook(book: BookEntity, deletePhysicalFile: Boolean = false) {
        viewModelScope.launch(Dispatchers.IO) {
            if (deletePhysicalFile) {
                try {
                    val file = File(book.uriString)
                    if (file.exists()) {
                        file.delete()
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }
            try {
                val thumb = ThumbnailManager.getThumbnailFile(getApplication(), book.id, book.uriString)
                if (thumb.exists()) thumb.delete()
            } catch (e: Exception) {
                e.printStackTrace()
            }
            repository.deleteBook(book)
            if (_currentReadingBook.value?.id == book.id) {
                closeReader()
            }
        }
    }

    fun renameBook(book: BookEntity, newCleanTitle: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val oldFile = File(book.uriString)
            val safeExtension = if (newCleanTitle.endsWith(".pdf", ignoreCase = true)) newCleanTitle else "$newCleanTitle.pdf"
            val newFile = File(oldFile.parentFile, safeExtension)
            val renamedSuccessfully = if (oldFile.exists()) oldFile.renameTo(newFile) else true

            if (renamedSuccessfully) {
                val updated = book.copy(
                    title = newCleanTitle.toCleanBookTitle(),
                    fileName = safeExtension,
                    uriString = if (newFile.exists()) newFile.absolutePath else book.uriString
                )
                repository.updateBook(updated)
                if (_currentReadingBook.value?.id == book.id) {
                    _currentReadingBook.value = updated
                }
            }
        }
    }

    // Reader
    fun openReader(book: BookEntity) {
        _currentReadingBook.value = book
        _readerCurrentPage.value = book.lastReadPage.coerceAtLeast(1)
        _readerTotalPages.value = book.pageCount.coerceAtLeast(1)
        _isReaderOpen.value = true
        _isInkingDockOpen.value = false
        _redoStack.clear()
        _canRedo.value = false
        setInkingTool(InkingTool.PEN_BLUE)
    }

    fun closeReader() {
        val book = _currentReadingBook.value
        val page = _readerCurrentPage.value
        val total = _readerTotalPages.value
        if (book != null) {
            viewModelScope.launch {
                val percent = if (total > 0) ((page.toFloat() / total.toFloat()) * 100).toInt().coerceIn(0, 100) else 0
                repository.updateReadingProgress(book.id, page, percent)
            }
        }
        _isReaderOpen.value = false
        _currentReadingBook.value = null
    }

    fun setReaderPage(page: Int) {
        val total = _readerTotalPages.value
        val clamped = page.coerceIn(1, total.coerceAtLeast(1))
        _readerCurrentPage.value = clamped
        val book = _currentReadingBook.value
        if (book != null) {
            viewModelScope.launch {
                val percent = if (total > 0) ((clamped.toFloat() / total.toFloat()) * 100).toInt().coerceIn(0, 100) else 0
                repository.updateReadingProgress(book.id, clamped, percent)
            }
        }
    }

    fun setReaderTotalPages(total: Int) {
        if (total > 0) {
            _readerTotalPages.value = total
            val current = _currentReadingBook.value
            if (current != null && current.pageCount != total) {
                val updated = current.copy(pageCount = total)
                _currentReadingBook.value = updated
                viewModelScope.launch(Dispatchers.IO) {
                    repository.updateBook(updated)
                }
            }
        }
    }

    fun toggleReaderNightMode() {
        _readerNightMode.value = !_readerNightMode.value
    }

    fun setReaderViewMode(mode: ReaderViewMode) {
        _readerViewMode.value = mode
    }

    fun toggleInkingDock() {
        val next = !_isInkingDockOpen.value
        _isInkingDockOpen.value = next
        if (!next) {
            _activeTool.value = ActiveTool.None
        } else if (_activeTool.value is ActiveTool.None) {
            _activeTool.value = ActiveTool.Pen(Color(0xFF0284C7), 1.5f)
        }
    }

    fun setActiveTool(tool: ActiveTool) {
        _activeTool.value = tool
        if (tool !is ActiveTool.None) {
            _isInkingDockOpen.value = true
        }
        // Keep inkingTool in sync for backwards compatibility
        when (tool) {
            is ActiveTool.Eraser -> _activeInkingTool.value = InkingTool.ERASER
            is ActiveTool.Highlighter -> {
                _activeInkingTool.value = InkingTool.HIGHLIGHTER
                _activeInkingColor.value = tool.color
                _activeInkingStrokeWidth.value = tool.width
            }
            is ActiveTool.Pen -> {
                _activeInkingTool.value = InkingTool.PEN_BLUE
                _activeInkingColor.value = tool.color
                _activeInkingStrokeWidth.value = tool.width
            }
            is ActiveTool.StickyNote -> _activeInkingTool.value = InkingTool.NOTE
            else -> {}
        }
    }

    fun setInkingTool(tool: InkingTool) {
        _activeInkingTool.value = tool
        when (tool) {
            InkingTool.ERASER -> {
                _activeTool.value = ActiveTool.Eraser
            }
            InkingTool.PEN_BLACK -> {
                val color = Color(0xFF0F172A)
                _activeInkingColor.value = color
                _activeInkingStrokeWidth.value = 1.5f
                _activeTool.value = ActiveTool.Pen(color, 1.5f)
            }
            InkingTool.PEN_RED -> {
                val color = Color(0xFFEF4444)
                _activeInkingColor.value = color
                _activeInkingStrokeWidth.value = 1.5f
                _activeTool.value = ActiveTool.Pen(color, 1.5f)
            }
            InkingTool.PEN_BLUE -> {
                val color = Color(0xFF0284C7)
                _activeInkingColor.value = color
                _activeInkingStrokeWidth.value = 1.5f
                _activeTool.value = ActiveTool.Pen(color, 1.5f)
            }
            InkingTool.HIGHLIGHTER -> {
                val color = Color(0xFFFACC15)
                _activeInkingColor.value = Color(0x77FACC15)
                _activeInkingStrokeWidth.value = 18f
                _activeTool.value = ActiveTool.Highlighter(color, 18f)
            }
            InkingTool.NOTE -> {
                _activeInkingColor.value = Color(0xFFF59E0B)
                _activeInkingStrokeWidth.value = 2f
                _activeTool.value = ActiveTool.StickyNote
            }
        }
    }

    fun setInkingColor(color: Color) {
        _activeInkingColor.value = color
    }

    fun setInkingStrokeWidth(width: Float) {
        _activeInkingStrokeWidth.value = width
    }

    // Normalized stroke saving (0.0f .. 1.0f)
    fun saveNormalizedStroke(
        pageIndex: Int,
        pointsJson: String,
        colorHex: String,
        strokeWidth: Float,
        type: String
    ) {
        val book = _currentReadingBook.value ?: return
        viewModelScope.launch {
            repository.saveAnnotation(
                AnnotationEntity(
                    bookId = book.id,
                    pageIndex = pageIndex,
                    strokePointsJson = pointsJson,
                    colorHex = colorHex,
                    strokeWidth = strokeWidth,
                    type = type
                )
            )
            _redoStack.removeAll { it.bookId == book.id && it.pageIndex == pageIndex }
            _canRedo.value = _redoStack.any { it.bookId == book.id }
        }
    }

    // Legacy fallback for pixel strokes
    fun saveAnnotationStroke(
        pointsJson: String,
        tool: InkingTool = _activeInkingTool.value,
        color: Color = _activeInkingColor.value,
        strokeWidth: Float = _activeInkingStrokeWidth.value
    ) {
        val book = _currentReadingBook.value ?: return
        val page = _readerCurrentPage.value
        val hex = String.format("#%08X", color.value.toLong())
        saveNormalizedStroke(page, pointsJson, hex, strokeWidth, tool.name)
    }

    fun saveNormalizedShape(
        pageIndex: Int,
        shapeType: ShapeType,
        startX: Float,
        startY: Float,
        endX: Float,
        endY: Float,
        colorHex: String = "#FF0284C7",
        strokeWidth: Float = 2.5f
    ) {
        val book = _currentReadingBook.value ?: return
        val json = JSONObject().apply {
            put("shape", shapeType.name)
            put("startX", startX)
            put("startY", startY)
            put("endX", endX)
            put("endY", endY)
        }.toString()
        viewModelScope.launch {
            repository.saveAnnotation(
                AnnotationEntity(
                    bookId = book.id,
                    pageIndex = pageIndex,
                    strokePointsJson = json,
                    colorHex = colorHex,
                    strokeWidth = strokeWidth,
                    type = "SHAPE"
                )
            )
            _redoStack.removeAll { it.bookId == book.id && it.pageIndex == pageIndex }
            _canRedo.value = _redoStack.any { it.bookId == book.id }
        }
    }

    fun saveNormalizedText(
        pageIndex: Int,
        text: String,
        x: Float,
        y: Float,
        colorHex: String = "#FF0F172A",
        fontSize: Float = 2.5f
    ) {
        val book = _currentReadingBook.value ?: return
        val json = JSONObject().apply {
            put("text", text)
            put("x", x)
            put("y", y)
        }.toString()
        viewModelScope.launch {
            repository.saveAnnotation(
                AnnotationEntity(
                    bookId = book.id,
                    pageIndex = pageIndex,
                    strokePointsJson = json,
                    colorHex = colorHex,
                    strokeWidth = fontSize,
                    type = "TEXT"
                )
            )
            _redoStack.removeAll { it.bookId == book.id && it.pageIndex == pageIndex }
            _canRedo.value = _redoStack.any { it.bookId == book.id }
        }
    }

    fun saveNormalizedNote(
        pageIndex: Int,
        text: String,
        x: Float,
        y: Float
    ) {
        val book = _currentReadingBook.value ?: return
        val json = JSONObject().apply {
            put("text", text)
            put("x", x)
            put("y", y)
        }.toString()
        viewModelScope.launch {
            repository.saveAnnotation(
                AnnotationEntity(
                    bookId = book.id,
                    pageIndex = pageIndex,
                    strokePointsJson = json,
                    colorHex = "#FFF59E0B",
                    strokeWidth = 2f,
                    type = "NOTE"
                )
            )
            _redoStack.removeAll { it.bookId == book.id && it.pageIndex == pageIndex }
            _canRedo.value = _redoStack.any { it.bookId == book.id }
        }
    }

    fun deleteAnnotation(annotationId: Int) {
        viewModelScope.launch {
            repository.deleteAnnotationById(annotationId)
        }
    }

    fun undoLastAnnotation() {
        val book = _currentReadingBook.value ?: return
        val page = _readerCurrentPage.value
        viewModelScope.launch {
            val last = repository.getLastAnnotation(book.id, page) ?: repository.getAnnotationsListForBook(book.id).lastOrNull()
            if (last != null) {
                _redoStack.add(last)
                repository.deleteAnnotationById(last.id)
                _canRedo.value = true
            }
        }
    }

    fun redoAnnotation() {
        val book = _currentReadingBook.value ?: return
        val page = _readerCurrentPage.value
        viewModelScope.launch {
            val lastUndone = _redoStack.findLast { it.bookId == book.id && it.pageIndex == page }
                ?: _redoStack.findLast { it.bookId == book.id }
            if (lastUndone != null) {
                _redoStack.remove(lastUndone)
                repository.saveAnnotation(lastUndone.copy(id = 0, timestamp = System.currentTimeMillis()))
                _canRedo.value = _redoStack.any { it.bookId == book.id }
            }
        }
    }

    fun savePageNote(noteText: String, x: Float = 0.1f, y: Float = 0.15f) {
        val page = _readerCurrentPage.value
        saveNormalizedNote(page, noteText, if (x > 1f) x / 400f else x, if (y > 1f) y / 600f else y)
    }

    fun saveDateStamp(x: Float = 0.1f, y: Float = 0.1f) {
        val formattedDate = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault()).format(java.util.Date())
        savePageNote("Date: $formattedDate", x, y)
    }

    fun saveShape(shapeType: String, startX: Float = 0.1f, startY: Float = 0.2f, endX: Float = 0.45f, endY: Float = 0.35f) {
        val page = _readerCurrentPage.value
        val st = try { ShapeType.valueOf(shapeType) } catch (_: Exception) { ShapeType.RECTANGLE }
        saveNormalizedShape(
            pageIndex = page,
            shapeType = st,
            startX = if (startX > 1f) startX / 400f else startX,
            startY = if (startY > 1f) startY / 600f else startY,
            endX = if (endX > 1f) endX / 400f else endX,
            endY = if (endY > 1f) endY / 600f else endY
        )
    }

    // True PDF Saving (Save vs Save As)
    fun saveBakedPdf(context: Context, onResult: (Boolean) -> Unit) {
        val book = _currentReadingBook.value ?: run {
            onResult(false)
            return
        }
        viewModelScope.launch {
            val annotations = repository.getAnnotationsListForBook(book.id)
            val success = PdfAnnotationManager.saveDocument(context, book, annotations)
            if (success) {
                val f = File(book.uriString)
                if (f.exists()) {
                    val updated = book.copy(fileSizeBytes = f.length(), lastReadTimestamp = System.currentTimeMillis())
                    repository.updateBook(updated)
                    _currentReadingBook.value = updated
                }
            }
            onResult(success)
        }
    }

    fun saveAsBakedPdf(context: Context, newTitle: String, onResult: (BookEntity?) -> Unit) {
        val book = _currentReadingBook.value ?: run {
            onResult(null)
            return
        }
        viewModelScope.launch {
            val annotations = repository.getAnnotationsListForBook(book.id)
            val newFile = PdfAnnotationManager.saveAsDocument(context, book, newTitle, annotations)
            if (newFile != null) {
                val newBook = BookEntity(
                    vaultId = book.vaultId,
                    title = newTitle,
                    author = book.author,
                    fileName = newFile.name,
                    uriString = newFile.absolutePath,
                    pageCount = book.pageCount,
                    fileSizeBytes = newFile.length(),
                    isFavorite = false,
                    lastReadPage = 1,
                    progressPercent = 0,
                    lastReadTimestamp = System.currentTimeMillis(),
                    parentFolder = book.parentFolder
                )
                val newId = repository.addBook(newBook).toInt()
                val created = newBook.copy(id = newId)
                onResult(created)
            } else {
                onResult(null)
            }
        }
    }

    fun duplicateBook(book: BookEntity, onComplete: (BookEntity?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val originalFile = File(book.uriString)
                if (!originalFile.exists()) {
                    withContext(Dispatchers.Main) { onComplete(null) }
                    return@launch
                }
                val baseName = originalFile.nameWithoutExtension
                val ext = originalFile.extension
                val copyFile = File(originalFile.parentFile, "${baseName}_Copy.$ext")
                originalFile.copyTo(copyFile, overwrite = true)

                val newBook = BookEntity(
                    vaultId = book.vaultId,
                    title = "${book.title} (Copy)",
                    author = book.author,
                    fileName = copyFile.name,
                    uriString = copyFile.absolutePath,
                    pageCount = book.pageCount,
                    fileSizeBytes = copyFile.length(),
                    isFavorite = false,
                    lastReadPage = 1,
                    progressPercent = 0,
                    lastReadTimestamp = System.currentTimeMillis(),
                    parentFolder = book.parentFolder
                )
                val newId = repository.addBook(newBook).toInt()
                val created = newBook.copy(id = newId)
                withContext(Dispatchers.Main) {
                    onComplete(created)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                withContext(Dispatchers.Main) { onComplete(null) }
            }
        }
    }

    // Agenda tasks
    fun toggleTaskCompleted(task: AgendaTaskEntity) {
        viewModelScope.launch {
            repository.toggleTaskCompleted(task.id, !task.isCompleted)
            activeVault.value?.let { vault ->
                val root = vault.pathDisplay.ifEmpty { vault.uriString }
                if (root.isNotBlank()) {
                    VaultMetadataManager.persistVaultMetadata(root, vault.id.toString(), getApplication())
                }
            }
        }
    }

    fun addNewTask(
        title: String,
        linkedBookId: Int? = null,
        linkedBookTitle: String? = null,
        linkedChapterPage: Int? = null,
        linkedLectureUrl: String? = null,
        linkedLectureTitle: String? = null
    ) {
        viewModelScope.launch {
            repository.addTask(
                AgendaTaskEntity(
                    title = title,
                    dateString = "18/06/2026",
                    linkedBookId = linkedBookId,
                    linkedBookTitle = linkedBookTitle,
                    linkedChapterPage = linkedChapterPage,
                    linkedLectureUrl = linkedLectureUrl,
                    linkedLectureTitle = linkedLectureTitle
                )
            )
            activeVault.value?.let { vault ->
                val root = vault.pathDisplay.ifEmpty { vault.uriString }
                if (root.isNotBlank()) {
                    VaultMetadataManager.persistVaultMetadata(root, vault.id.toString(), getApplication())
                }
            }
        }
    }

    fun updateTask(task: AgendaTaskEntity) {
        viewModelScope.launch {
            repository.updateTask(task)
            activeVault.value?.let { vault ->
                val root = vault.pathDisplay.ifEmpty { vault.uriString }
                if (root.isNotBlank()) {
                    VaultMetadataManager.persistVaultMetadata(root, vault.id.toString(), getApplication())
                }
            }
        }
    }

    fun deleteTask(taskId: Int) {
        viewModelScope.launch {
            repository.deleteTask(taskId)
            activeVault.value?.let { vault ->
                val root = vault.pathDisplay.ifEmpty { vault.uriString }
                if (root.isNotBlank()) {
                    VaultMetadataManager.persistVaultMetadata(root, vault.id.toString(), getApplication())
                }
            }
        }
    }

    // Exams
    fun addNewExam(title: String, examDateMillis: Long, syllabusMarkdown: String) {
        viewModelScope.launch {
            val totalChapters = syllabusMarkdown.lines().count { it.trimStart().startsWith("- ") || it.trimStart().startsWith("* ") }.coerceAtLeast(1)
            repository.addExam(
                ExamEntity(
                    title = title,
                    examDateMillis = examDateMillis,
                    totalChaptersCount = totalChapters,
                    remainingChaptersCount = totalChapters,
                    syllabusJson = syllabusMarkdown
                )
            )
        }
    }

    fun updateExam(exam: ExamEntity) {
        viewModelScope.launch {
            repository.updateExam(exam)
        }
    }

    fun deleteExam(examId: Int) {
        viewModelScope.launch {
            repository.deleteExam(examId)
        }
    }

    // Seed Demo Sample data
    fun seedSampleData() {
        viewModelScope.launch {
            repository.seedSampleMedicalBundle()
        }
    }
}
