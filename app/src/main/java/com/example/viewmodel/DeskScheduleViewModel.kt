package com.example.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.MulberryDatabase
import com.example.data.model.BookEntity
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
import java.util.Calendar

class DeskScheduleViewModel(private val app: Application) : AndroidViewModel(app) {

    private val scheduleRepo = ScheduleRepository.getInstance(app)
    private val db = MulberryDatabase.getInstance(app)

    val scheduleState: StateFlow<ScheduleVaultFile> = scheduleRepo.scheduleState

    private val downloadManager = com.example.util.BookDownloadManager.getInstance(app, db.bookDao())

    val downloadedBooks: StateFlow<List<BookEntity>> = db.bookDao().getDownloadedBooks().stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    val allCatalogBooks: StateFlow<List<BookEntity>> = db.bookDao().getAllBooks().stateIn(
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
        // Synchronize physical /sdcard/Mulberry/ storage with database immediately
        viewModelScope.launch(Dispatchers.IO) {
            com.example.util.MulberryStorageManager.syncDiskWithDatabase(app, db.bookDao())
            scheduleRepo.loadSchedule()
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
        file.sessions
            .filter { !it.isCompleted && it.scheduledEpochMillis <= endOfToday }
            .sortedBy { it.snoozedUntilEpochMillis ?: it.scheduledEpochMillis }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Completed Today Sessions
    val completedTodaySessions: StateFlow<List<ReadingSession>> = scheduleState.map { file ->
        val (startOfToday, endOfToday) = getTodayBounds()
        file.sessions
            .filter { it.isCompleted && (it.completedEpochMillis ?: it.scheduledEpochMillis) in startOfToday..endOfToday }
            .sortedByDescending { it.completedEpochMillis ?: it.scheduledEpochMillis }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Tomorrow Sessions (Not completed)
    val tomorrowSessions: StateFlow<List<ReadingSession>> = scheduleState.map { file ->
        val (startOfTomorrow, endOfTomorrow) = getTomorrowBounds()
        file.sessions
            .filter { !it.isCompleted && it.scheduledEpochMillis in startOfTomorrow..endOfTomorrow }
            .sortedBy { it.scheduledEpochMillis }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Upcoming Sessions (After tomorrow, not completed)
    val upcomingSessions: StateFlow<List<ReadingSession>> = scheduleState.map { file ->
        val (_, endOfTomorrow) = getTomorrowBounds()
        file.sessions
            .filter { !it.isCompleted && it.scheduledEpochMillis > endOfTomorrow }
            .sortedBy { it.scheduledEpochMillis }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // Weekly Pacing Metrics
    val weeklyGoalPages: StateFlow<Int> = scheduleState.map { file ->
        file.weeklyGoalPages.coerceAtLeast(10)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 200)

    val weeklyGoalMinutes: StateFlow<Int> = scheduleState.map { file ->
        file.weeklyGoalMinutes.coerceAtLeast(30)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 420)

    val weeklyCompletedPages: StateFlow<Int> = scheduleState.map { file ->
        val weekKey = scheduleRepo.getCurrentWeekKey()
        scheduleRepo.getWeeklyCompletedPages(file.sessions, file.extraWeeklyPages[weekKey] ?: 0)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), 0)

    val weeklyCompletedMinutes: StateFlow<Int> = scheduleState.map { file ->
        scheduleRepo.getWeeklyCompletedMinutes(file.sessions)
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
        val totalPages = (endPage - startPage + 1).coerceAtLeast(1)
        val plannedMinutes = totalPages * 2
        val session = ReadingSession(
            bookSlug = book.slug.ifEmpty { "book_${book.id}" },
            bookTitle = book.title,
            subject = book.subject.ifEmpty { "General" },
            resourceType = book.type.ifEmpty { "BOOK" },
            startPage = startPage,
            endPage = endPage,
            plannedDurationMinutes = plannedMinutes,
            scheduledEpochMillis = scheduledEpochMillis,
            reminderMinutesBefore = reminderMinutesBefore,
            microNote = microNote.trim()
        )
        viewModelScope.launch {
            scheduleRepo.addSession(session)
            MulberryAnalytics.logStudyGoalCreated(
                subject = book.subject.ifEmpty { "General" },
                chapter = book.title,
                pageCount = totalPages
            )
            // Bug Fix 4: Do NOT call MulberryStorageManager.recordReadingProgress() here!
            // Scheduling a future session must NEVER overwrite lastReadPage or log false reading progress.
        }
    }

    fun editSession(updatedSession: ReadingSession) {
        viewModelScope.launch(Dispatchers.IO) {
            scheduleRepo.updateSession(updatedSession)
        }
    }

    fun snoozeSession(sessionId: String, minutes: Int = 30) {
        viewModelScope.launch(Dispatchers.IO) {
            scheduleRepo.snoozeSession(sessionId, minutes)
        }
    }

    fun markCompleted(sessionId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val session = scheduleRepo.scheduleState.value.sessions.find { it.id == sessionId }
            scheduleRepo.markCompleted(sessionId)
            if (session != null) {
                // Bug Fix 5: Direct Room DAO lookup by slug instead of cold StateFlow matching
                val book = db.bookDao().getBookBySlug(session.bookSlug)
                    ?: db.bookDao().getAllBooksSync().find { it.title.equals(session.bookTitle, ignoreCase = true) }
                MulberryAnalytics.logStudyGoalCompleted(
                    subject = book?.subject?.ifEmpty { session.subject.ifEmpty { "General" } } ?: "General",
                    chapter = session.bookTitle
                )
                if (book != null) {
                    val targetPage = session.endPage.coerceIn(1, book.pageCount.coerceAtLeast(1))
                    val percent = ((targetPage.toFloat() / book.pageCount.coerceAtLeast(1).toFloat()) * 100f).toInt()
                    db.bookDao().updateReadingProgress(book.id, targetPage, percent)
                    com.example.util.MulberryStorageManager.recordReadingActivity(
                        context = app,
                        bookSlug = book.slug,
                        bookTitle = book.title,
                        subject = book.subject,
                        lastReadPage = targetPage,
                        totalPages = book.pageCount,
                        durationSeconds = (session.plannedDurationMinutes * 60L),
                        startPage = session.startPage,
                        endPage = session.endPage,
                        pagesVisitedCount = session.totalPages,
                        sessionId = session.id
                    )
                }
            }
        }
    }

    fun toggleCompleted(sessionId: String) {
        viewModelScope.launch(Dispatchers.IO) {
            val session = scheduleRepo.scheduleState.value.sessions.find { it.id == sessionId } ?: return@launch
            val willBeCompleted = !session.isCompleted
            scheduleRepo.toggleCompleted(sessionId)

            // Bug Fix 5: Direct Room DAO lookup by slug
            val book = db.bookDao().getBookBySlug(session.bookSlug)
                ?: db.bookDao().getAllBooksSync().find { it.title.equals(session.bookTitle, ignoreCase = true) }

            if (willBeCompleted) {
                MulberryAnalytics.logStudyGoalCompleted(
                    subject = book?.subject?.ifEmpty { session.subject.ifEmpty { "General" } } ?: "General",
                    chapter = session.bookTitle
                )
            }
            if (book != null && willBeCompleted) {
                val targetPage = session.endPage.coerceIn(1, book.pageCount.coerceAtLeast(1))
                val percent = ((targetPage.toFloat() / book.pageCount.coerceAtLeast(1).toFloat()) * 100f).toInt()
                db.bookDao().updateReadingProgress(book.id, targetPage, percent)
                com.example.util.MulberryStorageManager.recordReadingActivity(
                    context = app,
                    bookSlug = book.slug,
                    bookTitle = book.title,
                    subject = book.subject,
                    lastReadPage = targetPage,
                    totalPages = book.pageCount,
                    durationSeconds = (session.plannedDurationMinutes * 60L),
                    startPage = session.startPage,
                    endPage = session.endPage,
                    pagesVisitedCount = session.totalPages,
                    sessionId = session.id
                )
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

    fun updateWeeklyGoal(newGoalPages: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            scheduleRepo.updateWeeklyGoal(newGoalPages)
        }
    }

    fun updateWeeklyGoalMinutes(newGoalMinutes: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            scheduleRepo.updateWeeklyGoalMinutes(newGoalMinutes)
        }
    }

    fun reload() {
        if (_isRefreshing.value) return
        viewModelScope.launch(Dispatchers.IO) {
            _isRefreshing.value = true
            try {
                com.example.util.MulberryStorageManager.syncDiskWithDatabase(getApplication(), db.bookDao())
                scheduleRepo.loadSchedule()
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                delay(300)
                _isRefreshing.value = false
            }
        }
    }
}
