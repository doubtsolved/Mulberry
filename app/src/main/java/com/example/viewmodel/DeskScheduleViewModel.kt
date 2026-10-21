package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.MulberryDatabase
import com.example.data.model.BookEntity
import com.example.data.model.VaultEntity
import com.example.viewmodel.rootPath
import com.example.data.schedule.ScheduleRepository
import com.example.data.schedule.model.ReadingSession
import com.example.data.schedule.model.ScheduleVaultFile
import com.mulberry.app.analytics.MulberryAnalytics
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.File
import java.util.Calendar

class DeskScheduleViewModel(private val app: Application) : AndroidViewModel(app) {

    private val scheduleRepo = ScheduleRepository.getInstance(app)
    private val db = MulberryDatabase.getInstance(app)

    val scheduleState: StateFlow<ScheduleVaultFile> = scheduleRepo.scheduleState

    private val _activeVault = MutableStateFlow<VaultEntity?>(null)
    val activeVault: StateFlow<VaultEntity?> = _activeVault.asStateFlow()

    private val _vaultBooks = MutableStateFlow<List<BookEntity>>(emptyList())
    val vaultBooks: StateFlow<List<BookEntity>> = _vaultBooks.asStateFlow()

    private val downloadManager = com.example.util.BookDownloadManager.getInstance(app, db.bookDao())

    val downloadedBooks: StateFlow<List<BookEntity>> = db.bookDao().getDownloadedBooks().map { roomBooks ->
        val jsonBooks = com.example.util.MulberryStorageManager.loadDownloadedBooksAsEntities(app)
        if (jsonBooks.isNotEmpty()) {
            jsonBooks
        } else if (roomBooks.isNotEmpty()) {
            for (b in roomBooks) {
                val bin = File(b.binFilePath)
                com.example.util.MulberryStorageManager.recordDownloadedBook(app, b, bin)
            }
            com.example.util.MulberryStorageManager.loadDownloadedBooksAsEntities(app).ifEmpty { roomBooks }
        } else {
            emptyList()
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = com.example.util.MulberryStorageManager.loadDownloadedBooksAsEntities(app)
    )

    val allCatalogBooks: StateFlow<List<BookEntity>> = db.bookDao().getAllBooks().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun deleteDownloadedBook(book: BookEntity) {
        viewModelScope.launch {
            com.example.util.MulberryStorageManager.removeDownloadedBook(app, book.slug.ifEmpty { "book_${book.id}" })
            downloadManager.deleteDownload(book)
        }
    }

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        // Synchronize physical /sdcard/Mulberry/ storage with database
        viewModelScope.launch(Dispatchers.IO) {
            com.example.util.MulberryStorageManager.syncDiskWithDatabase(app, db.bookDao())
        }

        // Observe database vaults and active vault selection
        viewModelScope.launch {
            db.vaultDao().getAllVaults().collect { vaults ->
                val prefs = app.getSharedPreferences("mulberry_prefs", android.content.Context.MODE_PRIVATE)
                val activeId = prefs.getString("active_vault_id", null)
                val current = vaults.find { it.id.toString() == activeId } ?: vaults.firstOrNull()
                _activeVault.value = current
                if (current != null) {
                    scheduleRepo.setActiveVaultPath(current.rootPath)
                    refreshBooksForVault(current.id)
                }
            }
        }
    }

    fun syncActiveVault(vault: VaultEntity?) {
        if (vault == null) return
        _activeVault.value = vault
        scheduleRepo.setActiveVaultPath(vault.rootPath)
        refreshBooksForVault(vault.id)
    }

    private fun refreshBooksForVault(vaultId: Int) {
        viewModelScope.launch {
            db.bookDao().getBooksByVault(vaultId).collect { books ->
                _vaultBooks.value = books
            }
        }
    }

    // Date Bounds Helpers
    private fun getTodayBounds(): Pair<Long, Long> {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfToday = cal.timeInMillis

        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        val endOfToday = cal.timeInMillis

        return Pair(startOfToday, endOfToday)
    }

    private fun getTomorrowBounds(): Pair<Long, Long> {
        val (_, endOfToday) = getTodayBounds()
        val cal = Calendar.getInstance().apply {
            timeInMillis = endOfToday + 1L
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfTomorrow = cal.timeInMillis

        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        val endOfTomorrow = cal.timeInMillis

        return Pair(startOfTomorrow, endOfTomorrow)
    }

    // Active Today Sessions (Scheduled for today, or overdue from past, not completed)
    val activeTodaySessions: StateFlow<List<ReadingSession>> = scheduleState.map { file ->
        val (_, endOfToday) = getTodayBounds()
        file.sessions.filterIsInstance<ReadingSession>()
            .filter { !it.isCompleted && it.scheduledEpochMillis <= endOfToday }
            .sortedBy { it.scheduledEpochMillis }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Completed Today Sessions
    val completedTodaySessions: StateFlow<List<ReadingSession>> = scheduleState.map { file ->
        val (startOfToday, endOfToday) = getTodayBounds()
        file.sessions.filterIsInstance<ReadingSession>()
            .filter { it.isCompleted && (it.completedEpochMillis ?: it.scheduledEpochMillis) in startOfToday..endOfToday }
            .sortedByDescending { it.completedEpochMillis ?: it.scheduledEpochMillis }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Tomorrow Sessions (Not completed)
    val tomorrowSessions: StateFlow<List<ReadingSession>> = scheduleState.map { file ->
        val (startOfTomorrow, endOfTomorrow) = getTomorrowBounds()
        file.sessions.filterIsInstance<ReadingSession>()
            .filter { !it.isCompleted && it.scheduledEpochMillis in startOfTomorrow..endOfTomorrow }
            .sortedBy { it.scheduledEpochMillis }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Upcoming Sessions (After tomorrow, not completed)
    val upcomingSessions: StateFlow<List<ReadingSession>> = scheduleState.map { file ->
        val (_, endOfTomorrow) = getTomorrowBounds()
        file.sessions.filterIsInstance<ReadingSession>()
            .filter { !it.isCompleted && it.scheduledEpochMillis > endOfTomorrow }
            .sortedBy { it.scheduledEpochMillis }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Weekly Pacing Metrics
    val weeklyGoalPages: StateFlow<Int> = scheduleState.map { file ->
        file.weeklyGoalPages.coerceAtLeast(10)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 200)

    val weeklyCompletedPages: StateFlow<Int> = scheduleState.map { file ->
        scheduleRepo.getWeeklyCompletedPages(file.sessions)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val weeklyPlannedPages: StateFlow<Int> = scheduleState.map { file ->
        scheduleRepo.getWeeklyPlannedPages(file.sessions)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val weeklyProgressPercent: StateFlow<Int> = combine(weeklyCompletedPages, weeklyGoalPages) { completed, goal ->
        if (goal <= 0) 0 else ((completed.toFloat() / goal.toFloat()) * 100f).toInt().coerceIn(0, 100)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val todayPagesCount: StateFlow<Int> = activeTodaySessions.map { sessions ->
        sessions.sumOf { it.totalPages }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    // Actions
    fun addSession(
        book: BookEntity,
        startPage: Int,
        endPage: Int,
        scheduledEpochMillis: Long,
        reminderMinutesBefore: Int,
        microNote: String
    ) {
        val vaultPath = _activeVault.value?.rootPath
            ?: com.example.util.MulberryStorageManager.getMulberryDirectory(app).absolutePath
        val relPath = if (book.uriString.startsWith(vaultPath)) {
            book.uriString.removePrefix(vaultPath).trimStart('/')
        } else {
            book.fileName
        }
        val session = ReadingSession(
            vaultPath = vaultPath,
            bookFileName = book.fileName.ifEmpty { book.uriString },
            bookRelativePath = relPath,
            bookTitle = book.title,
            startPage = startPage,
            endPage = endPage,
            scheduledEpochMillis = scheduledEpochMillis,
            reminderMinutesBefore = reminderMinutesBefore,
            microNote = microNote.trim()
        )
        viewModelScope.launch {
            scheduleRepo.addSession(session)
            MulberryAnalytics.logStudyGoalCreated(
                subject = book.subject.ifEmpty { "General" },
                chapter = book.title,
                pageCount = (endPage - startPage + 1).coerceAtLeast(1)
            )
            // Persist progress to /sdcard/Mulberry/.schedule/reading_progress.json
            runCatching {
                com.example.util.MulberryStorageManager.recordReadingProgress(
                    context = app,
                    book = book,
                    newLastReadPage = startPage
                )
            }
        }
    }

    fun editSession(updatedSession: ReadingSession) {
        viewModelScope.launch(Dispatchers.IO) {
            scheduleRepo.updateSession(updatedSession)
        }
    }

    fun markCompleted(sessionId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            scheduleRepo.markCompleted(sessionId)
            val session = scheduleRepo.scheduleState.value.sessions.find { it.id == sessionId }
            if (session != null) {
                val book = downloadedBooks.value.find { it.title == session.bookTitle }
                MulberryAnalytics.logStudyGoalCompleted(
                    subject = book?.subject?.ifEmpty { "General" } ?: "General",
                    chapter = session.bookTitle
                )
                if (book != null) {
                    com.example.util.MulberryStorageManager.recordReadingProgress(
                        context = app,
                        book = book,
                        newLastReadPage = session.endPage
                    )
                }
            }
        }
    }

    fun toggleCompleted(sessionId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            scheduleRepo.toggleCompleted(sessionId)
            val session = scheduleRepo.scheduleState.value.sessions.find { it.id == sessionId }
            if (session != null) {
                val book = downloadedBooks.value.find { it.title == session.bookTitle }
                if (session.isCompleted) {
                    MulberryAnalytics.logStudyGoalCompleted(
                        subject = book?.subject?.ifEmpty { "General" } ?: "General",
                        chapter = session.bookTitle
                    )
                }
                if (book != null) {
                    val targetPage = if (session.isCompleted) session.endPage else session.startPage
                    com.example.util.MulberryStorageManager.recordReadingProgress(
                        context = app,
                        book = book,
                        newLastReadPage = targetPage
                    )
                }
            }
        }
    }

    fun bumpSession(sessionId: String, daysToAdd: Int = 1) {
        viewModelScope.launch(Dispatchers.IO) {
            scheduleRepo.bumpSession(sessionId, daysToAdd)
        }
    }

    fun deleteSession(sessionId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            scheduleRepo.deleteSession(sessionId)
        }
    }

    fun updateWeeklyGoal(newGoal: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            scheduleRepo.updateWeeklyGoal(newGoal)
        }
    }

    fun autoResolveMissingBook(session: ReadingSession, vaultPath: String) {
        viewModelScope.launch(Dispatchers.IO) {
            runCatching {
                val vaultDir = File(vaultPath)
                if (!vaultDir.exists() || !vaultDir.isDirectory) return@runCatching
                val found = vaultDir.walkTopDown().firstOrNull {
                    it.isFile && (it.name.equals(session.bookFileName, ignoreCase = true) ||
                            (session.bookRelativePath.isNotBlank() && it.name.equals(File(session.bookRelativePath).name, ignoreCase = true)))
                }
                if (found != null) {
                    val rel = found.absolutePath.removePrefix(vaultPath).trimStart('/')
                    scheduleRepo.updateBookRelativePath(session.id, rel)
                }
            }
        }
    }

    fun reload() {
        if (_isRefreshing.value) return
        viewModelScope.launch(Dispatchers.IO) {
            _isRefreshing.value = true
            try {
                com.example.util.MulberryStorageManager.syncDiskWithDatabase(getApplication(), db.bookDao())
                scheduleRepo.loadSchedule()
                val current = _activeVault.value
                if (current != null) {
                    refreshBooksForVault(current.id)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                delay(400)
                _isRefreshing.value = false
            }
        }
    }
}
