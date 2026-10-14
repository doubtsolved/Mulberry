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

class DeskScheduleViewModel(application: Application) : AndroidViewModel(application) {

    private val scheduleRepo = ScheduleRepository.getInstance(application)
    private val db = MulberryDatabase.getInstance(application)

    val scheduleState: StateFlow<ScheduleVaultFile> = scheduleRepo.scheduleState

    private val _activeVault = MutableStateFlow<VaultEntity?>(null)
    val activeVault: StateFlow<VaultEntity?> = _activeVault.asStateFlow()

    private val _vaultBooks = MutableStateFlow<List<BookEntity>>(emptyList())
    val vaultBooks: StateFlow<List<BookEntity>> = _vaultBooks.asStateFlow()

    private val downloadManager = com.example.util.BookDownloadManager.getInstance(application, db.bookDao())

    val downloadedBooks: StateFlow<List<BookEntity>> = db.bookDao().getDownloadedBooks().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    fun deleteDownloadedBook(book: BookEntity) {
        viewModelScope.launch {
            downloadManager.deleteDownload(book)
        }
    }

    private val _isRefreshing = MutableStateFlow(false)
    val isRefreshing: StateFlow<Boolean> = _isRefreshing.asStateFlow()

    init {
        // Synchronize physical /sdcard/Mulberry/ storage with database
        viewModelScope.launch(Dispatchers.IO) {
            com.example.util.MulberryStorageManager.syncDiskWithDatabase(application, db.bookDao())
        }

        // Observe database vaults and active vault selection
        viewModelScope.launch {
            db.vaultDao().getAllVaults().collect { vaults ->
                val prefs = application.getSharedPreferences("mulberry_prefs", android.content.Context.MODE_PRIVATE)
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
        val vault = _activeVault.value ?: return
        val relPath = if (book.uriString.startsWith(vault.rootPath)) {
            book.uriString.removePrefix(vault.rootPath).trimStart('/')
        } else {
            book.fileName
        }
        val session = ReadingSession(
            vaultPath = vault.rootPath,
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
        }
    }

    fun toggleCompleted(sessionId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            scheduleRepo.toggleCompleted(sessionId)
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
