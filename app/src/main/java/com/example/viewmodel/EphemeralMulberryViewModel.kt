package com.example.viewmodel

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.crypto.EphemeralCacheManager
import com.example.data.local.MulberryDatabase
import com.example.data.local.entities.BookEntity
import com.example.data.local.entities.DailyScheduleEntity
import com.example.data.local.entities.SnipEntity
import com.example.data.repository.CatalogRepository
import com.example.data.repository.SnipRepository
import com.example.data.repository.VaultRepository
import com.example.ui.theme.IconStyle
import com.example.ui.theme.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Deprecated("Dead ViewModel. DeskScheduleViewModel + ScheduleRepository is the single scheduling engine")
class EphemeralMulberryViewModel(application: Application) : AndroidViewModel(application) {

    private val db = MulberryDatabase.getInstance(application)
    val bookDao = db.bookDao()
    val scheduleDao = db.scheduleDao()
    val snipDao = db.snipDao()

    val catalogRepository = CatalogRepository(application, bookDao)
    val vaultRepository = VaultRepository(application, bookDao)
    val snipRepository = SnipRepository(application, snipDao)
    val ephemeralCacheManager = EphemeralCacheManager(application)

    // Catalog & Shelf StateFlows
    val allCatalogBooks: StateFlow<List<BookEntity>> = bookDao.getAllCatalogBooks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val shelfBooks: StateFlow<List<BookEntity>> = bookDao.getShelfBooks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val allSnips: StateFlow<List<SnipEntity>> = snipRepository.getAllSnips()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Schedules for today
    val todayEpochDay = System.currentTimeMillis() / 86400000L
    val todaySchedules: StateFlow<List<DailyScheduleEntity>> = scheduleDao.getSchedulesForDay(todayEpochDay)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // UI State
    private val _selectedProfFilter = MutableStateFlow("All")
    val selectedProfFilter: StateFlow<String> = _selectedProfFilter.asStateFlow()

    private val _selectedSubjectFilter = MutableStateFlow<String?>(null)
    val selectedSubjectFilter: StateFlow<String?> = _selectedSubjectFilter.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _isSyncingCatalog = MutableStateFlow(false)
    val isSyncingCatalog: StateFlow<Boolean> = _isSyncingCatalog.asStateFlow()

    private val _downloadProgressMap = MutableStateFlow<Map<String, Float>>(emptyMap())
    val downloadProgressMap: StateFlow<Map<String, Float>> = _downloadProgressMap.asStateFlow()

    // Active Reader State
    private val _activeReaderBook = MutableStateFlow<BookEntity?>(null)
    val activeReaderBook: StateFlow<BookEntity?> = _activeReaderBook.asStateFlow()

    private val _activeReaderTempPdf = MutableStateFlow<File?>(null)
    val activeReaderTempPdf: StateFlow<File?> = _activeReaderTempPdf.asStateFlow()

    private val _activeReaderStartPage = MutableStateFlow(1)
    val activeReaderStartPage: StateFlow<Int> = _activeReaderStartPage.asStateFlow()

    // Theme & IconStyle preferences
    private val prefs = application.getSharedPreferences("mulberry_prefs", Application.MODE_PRIVATE)
    private val _themeMode = MutableStateFlow(
        ThemeMode.from(prefs.getString("theme_mode", ThemeMode.OLDED.name))
    )
    val themeMode: StateFlow<ThemeMode> = _themeMode.asStateFlow()

    private val _iconStyle = MutableStateFlow(
        try {
            IconStyle.valueOf(prefs.getString("icon_style", IconStyle.VIBRANT.name) ?: IconStyle.VIBRANT.name)
        } catch (_: Exception) {
            IconStyle.VIBRANT
        }
    )
    val iconStyle: StateFlow<IconStyle> = _iconStyle.asStateFlow()

    init {
        // Automatically sync catalog from jsDelivr CDN on startup
        syncCatalog()
    }

    fun setProfFilter(prof: String) {
        _selectedProfFilter.value = prof
        _selectedSubjectFilter.value = null
    }

    fun setSubjectFilter(subject: String?) {
        _selectedSubjectFilter.value = subject
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setThemeMode(mode: ThemeMode) {
        _themeMode.value = mode
        prefs.edit().putString("theme_mode", mode.name).apply()
    }

    fun setIconStyle(style: IconStyle) {
        _iconStyle.value = style
        prefs.edit().putString("icon_style", style.name).apply()
    }

    fun syncCatalog(onComplete: ((Boolean, String) -> Unit)? = null) {
        viewModelScope.launch {
            _isSyncingCatalog.value = true
            val result = catalogRepository.syncCatalog()
            _isSyncingCatalog.value = false
            if (result.isSuccess) {
                onComplete?.invoke(true, "Catalog updated (${result.getOrNull()} books)")
            } else {
                // If offline and DB has books, we consider it ok
                val count = allCatalogBooks.value.size
                if (count > 0) {
                    onComplete?.invoke(true, "Offline mode ($count books available)")
                } else {
                    onComplete?.invoke(false, result.exceptionOrNull()?.message ?: "Sync failed")
                }
            }
        }
    }

    fun downloadBook(book: BookEntity, onComplete: ((Boolean) -> Unit)? = null) {
        viewModelScope.launch {
            _downloadProgressMap.value = _downloadProgressMap.value + (book.id to 0.05f)
            val result = vaultRepository.downloadVaultPayload(book) { bytesRead, totalBytes ->
                if (totalBytes > 0) {
                    val progress = (bytesRead.toFloat() / totalBytes.toFloat()).coerceIn(0f, 1f)
                    _downloadProgressMap.value = _downloadProgressMap.value + (book.id to progress)
                }
            }
            _downloadProgressMap.value = _downloadProgressMap.value - book.id
            onComplete?.invoke(result.isSuccess)
        }
    }

    fun deleteDownloadedBook(bookId: String) {
        viewModelScope.launch {
            vaultRepository.deleteDownloadedBook(bookId)
        }
    }

    fun openBookInReader(book: BookEntity, startPage: Int = -1) {
        viewModelScope.launch(Dispatchers.IO) {
            val binFile = File(book.localBinPath ?: vaultRepository.getLocalBinFile(book.id).absolutePath)
            if (!binFile.exists() || binFile.length() < 12) {
                return@launch
            }
            try {
                val tempPdf = ephemeralCacheManager.openEphemeralPdf(binFile)
                val targetPage = if (startPage > 0) startPage else book.lastReadPage.coerceAtLeast(1)
                withContext(Dispatchers.Main) {
                    _activeReaderBook.value = book
                    _activeReaderTempPdf.value = tempPdf
                    _activeReaderStartPage.value = targetPage
                }
            } catch (_: Exception) {}
        }
    }

    fun closeReader() {
        val book = _activeReaderBook.value
        _activeReaderBook.value = null
        _activeReaderTempPdf.value = null
        viewModelScope.launch(Dispatchers.IO) {
            ephemeralCacheManager.purgeEphemeralSession()
        }
    }

    fun updateReadingProgress(bookId: String, page: Int, totalPages: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val percent = if (totalPages > 0) ((page.toFloat() / totalPages.toFloat()) * 100).toInt() else 0
            bookDao.updateProgress(bookId, page, percent)
        }
    }

    fun addDailySchedule(bookId: String, bookTitle: String, chapter: String, startPage: Int, endPage: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val entity = DailyScheduleEntity(
                bookId = bookId,
                bookTitle = bookTitle,
                targetDateEpochDay = todayEpochDay,
                targetChapter = chapter,
                startPage = startPage,
                endPage = endPage,
                isCompleted = false
            )
            scheduleDao.insertSchedule(entity)
        }
    }

    fun toggleScheduleCompleted(scheduleId: Long, isCompleted: Boolean) {
        viewModelScope.launch(Dispatchers.IO) {
            scheduleDao.updateCompleted(scheduleId, isCompleted)
        }
    }

    fun deleteSchedule(scheduleId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            scheduleDao.deleteSchedule(scheduleId)
        }
    }

    fun saveSnip(
        bookId: String,
        bookTitle: String,
        subject: String,
        pageNumber: Int,
        bitmap: Bitmap,
        title: String,
        tags: String
    ) {
        viewModelScope.launch(Dispatchers.IO) {
            snipRepository.saveSnip(
                bookId = bookId,
                bookTitle = bookTitle,
                subject = subject,
                pageNumber = pageNumber,
                croppedBitmap = bitmap,
                title = title,
                tagsCsv = tags
            )
        }
    }

    fun deleteSnip(snip: SnipEntity) {
        viewModelScope.launch(Dispatchers.IO) {
            snipRepository.deleteSnip(snip)
        }
    }
}
