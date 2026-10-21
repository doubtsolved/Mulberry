@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.example.ui.screens.desk

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.data.model.BookEntity
import com.example.data.schedule.model.ReadingSession
import com.example.ui.components.SharedBookActionModal
import com.example.ui.reader.ReaderActivity
import com.example.ui.screens.UserProfileBottomSheet
import com.example.util.MulberryHistoryManager
import com.example.util.MulberryStorageManager
import com.example.viewmodel.DeskScheduleViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

enum class DeskDestination {
    DASHBOARD,
    MY_LIBRARY,
    BOOKS_COLLECTION,
    STUDY_PLANNER
}

/**
 * Root coordinator for Desk and its sub-screens (My Library, Books Collection, Study Planner).
 * Maintains navigation context, Shared Book-Action Modal, and telemetry sync in /sdcard/Mulberry/.history/.
 */
@Composable
fun DeskRootScreen(
    modifier: Modifier = Modifier,
    deskViewModel: DeskScheduleViewModel = viewModel(),
    onNavigateToLibrary: () -> Unit = {}
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    var currentDestination by remember { mutableStateOf(DeskDestination.DASHBOARD) }

    val downloadedBooks by deskViewModel.downloadedBooks.collectAsState()
    val allCatalogBooks by deskViewModel.allCatalogBooks.collectAsState()
    val vaultBooks by deskViewModel.vaultBooks.collectAsState()

    val userProfileRepo = remember { com.example.data.repository.UserProfileRepository.getInstance(context) }
    val userProfile by userProfileRepo.userProfile.collectAsState()
    var showProfileModal by remember { mutableStateOf(false) }

    // Shared Book-Action Modal state
    var selectedBookForAction by remember { mutableStateOf<BookEntity?>(null) }

    // Schedule Session creation sheet state
    var showCreateSheet by remember { mutableStateOf(false) }
    var sessionToEdit by remember { mutableStateOf<ReadingSession?>(null) }
    val scheduleSheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // Initial history synchronization
    LaunchedEffect(Unit) {
        MulberryHistoryManager.loadHistoryIndex(context)
        MulberryHistoryManager.loadCollections(context)
    }

    // Contextual Back Navigation for sub-screens within Desk
    BackHandler(enabled = currentDestination != DeskDestination.DASHBOARD) {
        when (currentDestination) {
            DeskDestination.BOOKS_COLLECTION -> currentDestination = DeskDestination.MY_LIBRARY
            DeskDestination.MY_LIBRARY -> currentDestination = DeskDestination.DASHBOARD
            DeskDestination.STUDY_PLANNER -> currentDestination = DeskDestination.DASHBOARD
            DeskDestination.DASHBOARD -> {}
        }
    }

    val onOpenBook: (BookEntity) -> Unit = { book ->
        val finalPath = book.binFilePath.ifEmpty { book.uriString }
            .ifEmpty {
                MulberryStorageManager.getBinFileForBook(context, book.slug)
                    .takeIf { it.exists() }?.absolutePath ?: ""
            }

        // Record user activity in /sdcard/Mulberry/.history/activity_events.json
        coroutineScope.launch(Dispatchers.IO) {
            MulberryHistoryManager.recordMovement(
                context = context,
                eventType = "OPEN_BOOK",
                book = book,
                page = book.lastReadPage.coerceAtLeast(1)
            )
        }

        ReaderActivity.launch(
            context = context,
            bookPath = finalPath,
            bookTitle = book.title,
            bookId = book.id
        )
    }

    val onOpenSession: (ReadingSession) -> Unit = { session ->
        val fullPath = resolveBookPath(
            vaultPath = session.vaultPath,
            bookFileName = session.bookFileName,
            bookRelativePath = session.bookRelativePath
        )

        // Record session start event in history ledger
        coroutineScope.launch(Dispatchers.IO) {
            MulberryHistoryManager.recordMovement(
                context = context,
                eventType = "START_SESSION",
                page = session.startPage,
                durationMinutes = session.totalPages * 2,
                extraMetadata = mapOf("sessionId" to session.id, "bookTitle" to session.bookTitle)
            )
        }

        ReaderActivity.launchSession(
            context = context,
            bookPath = fullPath,
            bookTitle = session.bookTitle,
            startPage = session.startPage,
            sessionId = session.id
        )
    }

    Box(modifier = modifier.fillMaxSize()) {
        AnimatedContent(
            targetState = currentDestination,
            transitionSpec = {
                fadeIn(animationSpec = tween(220)) togetherWith fadeOut(animationSpec = tween(220))
            },
            label = "DeskSubDestinationTransition"
        ) { destination ->
            when (destination) {
                DeskDestination.DASHBOARD -> {
                    DeskDashboardScreen(
                        downloadedBooks = downloadedBooks,
                        userProfile = userProfile,
                        onOpenBook = onOpenBook,
                        onBookCardClick = { selectedBookForAction = it },
                        onViewPlansClick = { currentDestination = DeskDestination.STUDY_PLANNER },
                        onQuickAccessMyLibrary = { currentDestination = DeskDestination.MY_LIBRARY },
                        onQuickAccessStudyPlanner = { currentDestination = DeskDestination.STUDY_PLANNER },
                        onProfileClick = { showProfileModal = true },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                DeskDestination.MY_LIBRARY -> {
                    MyLibraryScreen(
                        downloadedBooks = downloadedBooks,
                        onBackClick = { currentDestination = DeskDestination.DASHBOARD },
                        onOpenBooksCollection = { currentDestination = DeskDestination.BOOKS_COLLECTION },
                        onOpenBook = onOpenBook,
                        onBookActionClick = { selectedBookForAction = it },
                        onImportFilesClick = {
                            Toast.makeText(context, "Ready to import: check /sdcard/Mulberry/ or storage", Toast.LENGTH_SHORT).show()
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }

                DeskDestination.BOOKS_COLLECTION -> {
                    BooksCollectionScreen(
                        books = downloadedBooks,
                        onBackClick = { currentDestination = DeskDestination.MY_LIBRARY },
                        onBookClick = { selectedBookForAction = it },
                        onBookLongClick = { selectedBookForAction = it },
                        onAddBooksClick = onNavigateToLibrary,
                        modifier = Modifier.fillMaxSize()
                    )
                }

                DeskDestination.STUDY_PLANNER -> {
                    StudyPlannerScreen(
                        deskViewModel = deskViewModel,
                        onBackClick = { currentDestination = DeskDestination.DASHBOARD },
                        onOpenSession = onOpenSession,
                        onAddSessionClick = {
                            sessionToEdit = null
                            showCreateSheet = true
                        },
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }
        }

        // Shared Book-Action Modal (Wireframe 05)
        selectedBookForAction?.let { book ->
            SharedBookActionModal(
                book = book,
                onDismiss = { selectedBookForAction = null },
                onResumeReading = { targetBook ->
                    onOpenBook(targetBook)
                },
                onOpenFromBeginning = { targetBook ->
                    val finalPath = targetBook.binFilePath.ifEmpty { targetBook.uriString }
                    coroutineScope.launch(Dispatchers.IO) {
                        MulberryHistoryManager.recordMovement(
                            context = context,
                            eventType = "OPEN_BOOK_START",
                            book = targetBook,
                            page = 1
                        )
                    }
                    ReaderActivity.launchSession(
                        context = context,
                        bookPath = finalPath,
                        bookTitle = targetBook.title,
                        startPage = 1,
                        sessionId = "beginning_${targetBook.id}"
                    )
                },
                onAddToCollection = { targetBook ->
                    Toast.makeText(context, "Added \"${targetBook.title}\" to collection", Toast.LENGTH_SHORT).show()
                    coroutineScope.launch(Dispatchers.IO) {
                        MulberryHistoryManager.recordMovement(
                            context = context,
                            eventType = "ADD_TO_COLLECTION",
                            book = targetBook,
                            collectionName = "First Year MBBS"
                        )
                    }
                },
                onViewDetails = { targetBook ->
                    Toast.makeText(
                        context,
                        "${targetBook.title}\n${targetBook.author.ifEmpty { targetBook.edition }}\nPages: ${targetBook.pageCount} • Subject: ${targetBook.subject}",
                        Toast.LENGTH_LONG
                    ).show()
                },
                onRemoveFromLibrary = { targetBook ->
                    deskViewModel.deleteDownloadedBook(targetBook)
                    Toast.makeText(context, "Removed from My Library", Toast.LENGTH_SHORT).show()
                    coroutineScope.launch(Dispatchers.IO) {
                        MulberryHistoryManager.recordMovement(
                            context = context,
                            eventType = "REMOVE_BOOK",
                            book = targetBook
                        )
                    }
                }
            )
        }

        // User Profile Modal
        if (showProfileModal) {
            UserProfileBottomSheet(
                userProfile = userProfile,
                onDismissRequest = { showProfileModal = false },
                onSaveProfile = { updated ->
                    coroutineScope.launch {
                        userProfileRepo.updateProfile(updated)
                    }
                }
            )
        }

        // Schedule Session Sheet
        if (showCreateSheet) {
            ScheduleSessionSheet(
                sheetState = scheduleSheetState,
                vaultBooks = downloadedBooks.ifEmpty { vaultBooks }.ifEmpty { allCatalogBooks },
                existingSession = sessionToEdit,
                onDismiss = {
                    coroutineScope.launch {
                        scheduleSheetState.hide()
                        showCreateSheet = false
                        sessionToEdit = null
                    }
                },
                onSchedule = { book, startPage, endPage, scheduledEpochMillis, reminderMinutesBefore, microNote ->
                    if (sessionToEdit != null) {
                        deskViewModel.editSession(
                            sessionToEdit!!.copy(
                                startPage = startPage,
                                endPage = endPage,
                                scheduledEpochMillis = scheduledEpochMillis,
                                reminderMinutesBefore = reminderMinutesBefore,
                                microNote = microNote
                            )
                        )
                    } else {
                        deskViewModel.addSession(
                            book = book,
                            startPage = startPage,
                            endPage = endPage,
                            scheduledEpochMillis = scheduledEpochMillis,
                            reminderMinutesBefore = reminderMinutesBefore,
                            microNote = microNote
                        )
                    }
                    coroutineScope.launch {
                        scheduleSheetState.hide()
                        showCreateSheet = false
                        sessionToEdit = null
                    }
                }
            )
        }
    }
}

private fun resolveBookPath(vaultPath: String, bookFileName: String, bookRelativePath: String): String {
    if (bookRelativePath.isNotBlank()) {
        val relFile = File(vaultPath, bookRelativePath)
        if (relFile.exists()) return relFile.absolutePath
    }
    if (File(bookFileName).isAbsolute && File(bookFileName).exists()) {
        return bookFileName
    }
    val directFile = File(vaultPath, bookFileName)
    if (directFile.exists()) return directFile.absolutePath

    return bookFileName
}
