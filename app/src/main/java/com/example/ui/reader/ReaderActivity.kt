package com.example.ui.reader

import android.app.Activity
import android.content.ContentResolver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.provider.OpenableColumns
import androidx.core.app.ActivityOptionsCompat
import com.example.R
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import com.example.data.repository.UserProfileRepository
import com.example.ui.screens.desk.AddToDeskBottomSheet
import com.mulberry.app.analytics.MulberryAnalytics
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import com.example.util.slugify
import com.example.util.toCleanBookTitle
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import com.example.ui.components.ContributionLiveStatusCard
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.lifecycleScope
import com.example.data.MulberryDatabase
import com.example.data.repository.VaultMetadataManager
import com.example.ui.components.FluentIcons
import com.example.ui.theme.InterFamily
import com.example.ui.theme.LocalMulberryColors
import com.example.ui.theme.MulberryTheme
import com.example.ui.theme.PoppinsFamily
import com.example.ui.theme.ThemeMode
import android.graphics.pdf.PdfRenderer
import android.util.Log
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline
import com.tom_roush.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File

sealed interface ReaderLoadState {
    data object Loading : ReaderLoadState
    data object Ready : ReaderLoadState
    data class Error(val message: String) : ReaderLoadState
}

class ReaderActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_BOOK_PATH = "BOOK_PATH"
        const val EXTRA_BOOK_TITLE = "BOOK_TITLE"
        const val EXTRA_BOOK_ID = "BOOK_ID"
        const val EXTRA_START_PAGE = "EXTRA_START_PAGE"
        const val EXTRA_END_PAGE = "EXTRA_END_PAGE"
        const val EXTRA_SESSION_ID = "EXTRA_SESSION_ID"
        const val EXTRA_BOOK_SLUG = "EXTRA_BOOK_SLUG"

        fun launch(context: Context, bookPath: String, bookTitle: String, bookId: Int = -1) {
            val intent = Intent(context, ReaderActivity::class.java).apply {
                putExtra(EXTRA_BOOK_PATH, bookPath)
                putExtra(EXTRA_BOOK_TITLE, bookTitle)
                putExtra(EXTRA_BOOK_ID, bookId)
            }
            if (context is Activity) {
                val options = ActivityOptionsCompat.makeCustomAnimation(
                    context,
                    R.anim.book_open_enter,
                    R.anim.book_open_exit
                )
                context.startActivity(intent, options.toBundle())
            } else {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            }
        }

        fun launchSession(
            context: Context,
            bookPath: String,
            bookTitle: String,
            startPage: Int,
            sessionId: String,
            bookSlug: String = "",
            endPage: Int = -1
        ) {
            val intent = Intent(context, ReaderActivity::class.java).apply {
                putExtra(EXTRA_BOOK_PATH, bookPath)
                putExtra(EXTRA_BOOK_TITLE, bookTitle)
                putExtra(EXTRA_START_PAGE, startPage)
                putExtra(EXTRA_SESSION_ID, sessionId)
                if (bookSlug.isNotBlank()) putExtra(EXTRA_BOOK_SLUG, bookSlug)
                if (endPage > 0) putExtra(EXTRA_END_PAGE, endPage)
            }
            if (context is Activity) {
                val options = ActivityOptionsCompat.makeCustomAnimation(
                    context,
                    R.anim.book_open_enter,
                    R.anim.book_open_exit
                )
                context.startActivity(intent, options.toBundle())
            } else {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(intent)
            }
        }
    }

    private var bookPath: String = ""
    private var bookTitle: String = ""
    private var bookId: Int = -1
    private var bookSlug: String = ""
    private var sessionId: String? = null
    private var sessionStartPage: Int = -1
    private var sessionEndPage: Int = -1
    private var maxPageReached: Int = 1
    private var sessionStartTimeMs: Long = 0L
    private var resumeTimestamp: Long = 0L
    private var accumulatedActiveSeconds: Long = 0L
    private val visitedPages = mutableSetOf<Int>()
    private var recordedExtraPagesCount: Int = 0
    private var initialOpenedPage: Int = 1
    private var hasLoggedSessionCompleted: Boolean = false

    private val readerLoadState = mutableStateOf<ReaderLoadState>(ReaderLoadState.Loading)
    private var prepareJob: Job? = null

    // Single-Page Reader Page States
    private val currentSinglePage = mutableIntStateOf(0)
    private val singlePageTargetJump = mutableIntStateOf(-1)
    private val showControlsState = mutableStateOf(true)

    // Document Metadata States
    private val totalPagesState = mutableIntStateOf(1)
    private val tocItemsState = mutableStateOf<List<TocItem>>(emptyList())
    private val isTocLoadingState = mutableStateOf(true)

    // Search highlights state
    private val searchMatchesState = mutableStateOf<List<SearchMatch>>(emptyList())
    private val activeSearchMatchIndexState = mutableIntStateOf(-1)

    private var activeTheme: ThemeMode = ThemeMode.LIGHT
    private var ephemeralPdfFile: File? = null
    private var activeRenderingPath: String = ""

    private var thumbnailManager: PdfThumbnailManager? = null
    private var searchManager: PdfTextSearchManager? = null

    // External PDF States
    private val isExternalPdfState = mutableStateOf(false)
    private val isAlreadyInDeskState = mutableStateOf(false)
    private val showAddToDeskBannerState = mutableStateOf(false)
    private val showAddToDeskSheetState = mutableStateOf(false)
    private var externalPdfFile: File? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable edge-to-edge layout for sleek immersive bar transitions
        WindowCompat.setDecorFitsSystemWindows(window, false)

        val prefs = getSharedPreferences("mulberry_prefs", Context.MODE_PRIVATE)
        val savedTheme = prefs.getString("theme_mode", ThemeMode.PAPER.name)
        activeTheme = ThemeMode.from(savedTheme)
        ReaderPreferencesManager.init(applicationContext)

        setContentView(R.layout.activity_reader)

        val palette = when (activeTheme) {
            ThemeMode.OLDED -> com.example.ui.theme.MulberryOldedPalette
            ThemeMode.PAPER, ThemeMode.LIGHT -> com.example.ui.theme.MulberryPaperPalette
            ThemeMode.MIDNIGHT, ThemeMode.DARK -> com.example.ui.theme.MulberryMidnightPalette
            ThemeMode.FOREST -> com.example.ui.theme.MulberryForestPalette
            ThemeMode.ESPRESSO -> com.example.ui.theme.MulberryEspressoPalette
            ThemeMode.DUSK, ThemeMode.SLATE -> com.example.ui.theme.MulberryDuskPalette
        }
        findViewById<View>(R.id.reader_root_layout)?.setBackgroundColor(palette.background.toArgb())

        bookPath = intent.getStringExtra(EXTRA_BOOK_PATH) ?: ""
        bookTitle = intent.getStringExtra(EXTRA_BOOK_TITLE) ?: ""
        bookId = intent.getIntExtra(EXTRA_BOOK_ID, -1)
        bookSlug = intent.getStringExtra(EXTRA_BOOK_SLUG) ?: ""
        sessionId = intent.getStringExtra(EXTRA_SESSION_ID)
        sessionStartPage = intent.getIntExtra(EXTRA_START_PAGE, -1)
        sessionEndPage = intent.getIntExtra(EXTRA_END_PAGE, -1)

        val incomingUri = intent?.data
        if (incomingUri != null && (intent.action == Intent.ACTION_VIEW || bookPath.isEmpty())) {
            handleExternalPdfUri(incomingUri)
        }

        if (sessionStartPage > 0) {
            val startIdx = (sessionStartPage - 1).coerceAtLeast(0)
            currentSinglePage.intValue = startIdx
            singlePageTargetJump.intValue = startIdx
            maxPageReached = sessionStartPage
            visitedPages.add(sessionStartPage)
        }

        // Hook system back gesture
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                saveReadingProgress()
                finish()
            }
        })

        // Setup Compose Single-Page Viewport & Loading Overlay
        setupSinglePageView()

        // Asynchronously prepare book (decryption, path resolution, metadata)
        startAsyncPreparation()
    }

    override fun onResume() {
        super.onResume()
        resumeTimestamp = System.currentTimeMillis()
    }

    private fun handleExternalPdfUri(uri: Uri) {
        try {
            var displayName = "External Document.pdf"

            if (uri.scheme == ContentResolver.SCHEME_CONTENT) {
                contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                    val nameIndex = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
                    if (cursor.moveToFirst() && nameIndex != -1) {
                        cursor.getString(nameIndex)?.let { displayName = it }
                    }
                }
            } else if (uri.scheme == "file") {
                uri.lastPathSegment?.let { displayName = it }
            }

            val sanitizedName = displayName.replace(Regex("[^a-zA-Z0-9._-]"), "_").ifEmpty { "external_preview.pdf" }
            val previewDir = File(cacheDir, "external_preview").apply { mkdirs() }
            val tempFile = File(previewDir, sanitizedName)

            contentResolver.openInputStream(uri)?.use { input ->
                tempFile.outputStream().use { output ->
                    input.copyTo(output, bufferSize = 8192)
                }
            } ?: run {
                Log.w("ReaderActivity", "Could not open stream for external URI: $uri")
            }

            if (tempFile.exists() && tempFile.length() > 0) {
                externalPdfFile = tempFile
                bookPath = tempFile.absolutePath
                if (bookTitle.isEmpty()) {
                    bookTitle = displayName.toCleanBookTitle()
                }
                isExternalPdfState.value = true

                val alreadyInDesk = com.example.util.MulberryStorageManager.isBookInMulberry(
                    context = applicationContext,
                    fileName = tempFile.name,
                    fileSizeBytes = tempFile.length()
                )
                isAlreadyInDeskState.value = alreadyInDesk
                showAddToDeskBannerState.value = !alreadyInDesk
            }
        } catch (e: Exception) {
            Log.e("ReaderActivity", "Failed to handle external PDF URI: ${e.message}", e)
        }
    }

    private fun startAsyncPreparation() {
        prepareJob?.cancel()
        readerLoadState.value = ReaderLoadState.Loading

        prepareJob = lifecycleScope.launch(Dispatchers.IO) {
            try {
                var resolvedPath = bookPath.trim()

                // 0. Primary resolution via bookSlug in /sdcard/Mulberry/<slug>.bin
                if (bookSlug.isNotBlank()) {
                    val directBin = com.example.util.MulberryStorageManager.getBinFileForBook(applicationContext, bookSlug)
                    if (directBin.exists()) {
                        resolvedPath = directBin.absolutePath
                    }
                }

                // 1. Resolve path from Mulberry storage (/sdcard/Mulberry/), tracking JSON, or Room DB
                val mulberryDir = com.example.util.MulberryStorageManager.getMulberryDirectory(applicationContext)
                val vaultDir = File(filesDir, "vault")

                if (resolvedPath.isEmpty() || (!resolvedPath.startsWith("content://") && !File(resolvedPath).exists())) {
                    val db = MulberryDatabase.getInstance(applicationContext)
                    val bookEntity = if (bookId != -1) {
                        db.bookDao().getBookById(bookId)
                    } else if (bookTitle.isNotBlank()) {
                        db.bookDao().getAllBooksSync().find { it.title.equals(bookTitle, ignoreCase = true) }
                    } else null

                    if (bookEntity != null) {
                        if (bookTitle.isEmpty()) {
                            bookTitle = bookEntity.title
                        }
                        if (bookEntity.binFilePath.isNotBlank() && File(bookEntity.binFilePath).exists()) {
                            resolvedPath = bookEntity.binFilePath
                        } else if (bookEntity.slug.isNotBlank()) {
                            val mulberryBin = com.example.util.MulberryStorageManager.getBinFileForBook(applicationContext, bookEntity.slug)
                            if (mulberryBin.exists()) {
                                resolvedPath = mulberryBin.absolutePath
                            } else {
                                val directVaultFile = File(vaultDir, "${bookEntity.slug}.bin")
                                if (directVaultFile.exists()) {
                                    resolvedPath = directVaultFile.absolutePath
                                }
                            }
                        } else if (bookEntity.uriString.isNotBlank() && (bookEntity.uriString.startsWith("content://") || File(bookEntity.uriString).exists())) {
                            resolvedPath = bookEntity.uriString
                        }
                    }
                }

                // Check /sdcard/Mulberry/downloaded_books.json index
                if (resolvedPath.isEmpty() || (!resolvedPath.startsWith("content://") && !File(resolvedPath).exists())) {
                    val index = com.example.util.MulberryStorageManager.loadTrackingIndex(applicationContext)
                    val match = index.books.firstOrNull {
                        (bookId != -1 && it.id == "book_$bookId") ||
                        (bookTitle.isNotBlank() && it.title.equals(bookTitle, ignoreCase = true)) ||
                        (bookTitle.isNotBlank() && it.id.contains(bookTitle.take(6).lowercase()))
                    }
                    if (match != null && File(match.localPath).exists()) {
                        resolvedPath = match.localPath
                    }
                }

                // Check /sdcard/Mulberry/ directly for matching files
                if (resolvedPath.isEmpty() || (!resolvedPath.startsWith("content://") && !File(resolvedPath).exists())) {
                    if (mulberryDir.exists()) {
                        val normalizedTitle = bookTitle.lowercase().replace(" ", "_").replace("'", "")
                        val matchingFile = mulberryDir.listFiles()?.firstOrNull { file ->
                            file.isFile && file.name.endsWith(".bin") && (
                                (bookId != -1 && file.name.contains("book_$bookId")) ||
                                (bookTitle.isNotBlank() && (
                                    file.name.contains(normalizedTitle.take(8)) ||
                                    normalizedTitle.contains(file.nameWithoutExtension.take(8))
                                ))
                            )
                        }
                        if (matchingFile != null) {
                            resolvedPath = matchingFile.absolutePath
                        }
                    }
                }

                // Legacy fallback: check filesDir/vault
                if (resolvedPath.isEmpty() || (!resolvedPath.startsWith("content://") && !File(resolvedPath).exists())) {
                    if (vaultDir.exists()) {
                        val normalizedTitle = bookTitle.lowercase().replace(" ", "_").replace("'", "")
                        val matchingFile = vaultDir.listFiles()?.firstOrNull { file ->
                            file.isFile && (
                                (bookId != -1 && file.name.contains("book_$bookId")) ||
                                (bookTitle.isNotBlank() && (
                                    file.name.contains(normalizedTitle.take(8)) ||
                                    normalizedTitle.contains(file.nameWithoutExtension.take(8))
                                ))
                            )
                        }
                        if (matchingFile != null) {
                            resolvedPath = matchingFile.absolutePath
                        }
                    }
                }

                if (resolvedPath.isEmpty() || (!resolvedPath.startsWith("content://") && !File(resolvedPath).exists())) {
                    withContext(Dispatchers.Main) {
                        readerLoadState.value = ReaderLoadState.Error("Textbook file not found on device.")
                    }
                    return@launch
                }

                bookPath = resolvedPath

                // 2. Ephemeral Decryption for encrypted .bin Vault books
                var targetPdfPath = resolvedPath
                if (resolvedPath.endsWith(".bin", ignoreCase = true) || File(resolvedPath).name.endsWith(".bin")) {
                    val sourceFile = File(resolvedPath)
                    if (sourceFile.exists()) {
                        val sessionFile = com.example.util.VaultCryptoManager.createEphemeralSessionFile(
                            applicationContext,
                            if (bookId != -1) bookId.toString() else "vault"
                        )
                        val success = com.example.util.VaultCryptoManager.decryptVaultFile(sourceFile, sessionFile)
                        if (success && sessionFile.exists() && sessionFile.length() > 0) {
                            ephemeralPdfFile = sessionFile
                            targetPdfPath = sessionFile.absolutePath
                        } else {
                            withContext(Dispatchers.Main) {
                                readerLoadState.value = ReaderLoadState.Error("Failed to decrypt secure vault payload.")
                            }
                            return@launch
                        }
                    }
                }

                activeRenderingPath = targetPdfPath

                // 3. Open native PdfRenderer directly to validate document and get exact pageCount immediately (<10ms)
                var detectedPageCount = 1
                try {
                    val testPfd = if (targetPdfPath.startsWith("content://")) {
                        contentResolver.openFileDescriptor(Uri.parse(targetPdfPath), "r")
                    } else {
                        val f = File(targetPdfPath)
                        if (f.exists()) ParcelFileDescriptor.open(f, ParcelFileDescriptor.MODE_READ_ONLY) else null
                    }

                    if (testPfd != null) {
                        val testRenderer = PdfRenderer(testPfd)
                        detectedPageCount = testRenderer.pageCount.coerceAtLeast(1)
                        testRenderer.close()
                        testPfd.close()
                    } else {
                        throw IllegalStateException("File descriptor could not be acquired")
                    }
                } catch (t: Throwable) {
                    Log.e("ReaderActivity", "Cannot open PDF with PdfRenderer: ${t.message}", t)
                    withContext(Dispatchers.Main) {
                        readerLoadState.value = ReaderLoadState.Error("Corrupted or inaccessible document: ${t.message ?: "Format error"}")
                    }
                    return@launch
                }

                // 4. Initialize thumbnail and search managers
                val tManager = PdfThumbnailManager(applicationContext, activeRenderingPath)
                val sManager = PdfTextSearchManager(applicationContext, activeRenderingPath, detectedPageCount)
                thumbnailManager = tManager
                searchManager = sManager

                // Restore last read page from database only if not launching a specific study session
                if (sessionStartPage <= 0) {
                    try {
                        val db = MulberryDatabase.getInstance(applicationContext)
                        val book = if (bookId != -1) {
                            db.bookDao().getBookById(bookId)
                        } else {
                            db.bookDao().getAllBooksSync().find { it.uriString == bookPath || it.binFilePath == bookPath }
                        }
                        if (book != null && book.lastReadPage > 0) {
                            withContext(Dispatchers.Main) {
                                val zeroIdx = (book.lastReadPage - 1).coerceIn(0, detectedPageCount - 1)
                                currentSinglePage.intValue = zeroIdx
                                singlePageTargetJump.intValue = zeroIdx
                            }
                        }
                    } catch (_: Throwable) {}
                }

                val extIndex = com.example.util.MulberryStorageManager.loadDownloadedExternal(applicationContext)
                val extMatch = extIndex.books.firstOrNull {
                    it.localPath == bookPath || (bookSlug.isNotBlank() && it.id == bookSlug) || (bookTitle.isNotBlank() && it.title.equals(bookTitle, ignoreCase = true))
                }

                // 5. Instantly mark reader ready so page 1 renders without delay!
                withContext(Dispatchers.Main) {
                    if (extMatch != null) {
                        isExternalPdfState.value = true
                        isAlreadyInDeskState.value = true
                        if (bookSlug.isEmpty()) bookSlug = extMatch.id
                    }
                    totalPagesState.intValue = detectedPageCount
                    readerLoadState.value = ReaderLoadState.Ready
                    sessionStartTimeMs = System.currentTimeMillis()
                    initialOpenedPage = currentSinglePage.intValue + 1
                    visitedPages.add(initialOpenedPage)
                    MulberryAnalytics.logBookOpened(
                        bookId = if (bookId != -1) bookId.toString() else (bookTitle.ifEmpty { "unknown_book" }),
                        bookTitle = bookTitle.ifEmpty { "Medical Textbook" },
                        subject = "Medical",
                        page = currentSinglePage.intValue + 1
                    )
                    setupComposeTopBar()
                }

                // 6. Asynchronously extract Table of Contents in background without blocking reader view
                lifecycleScope.launch(Dispatchers.IO) {
                    extractPdfMetadataAndTocSafely()
                }
            } catch (t: Throwable) {
                Log.e("ReaderActivity", "Preparation error: ${t.message}", t)
                withContext(Dispatchers.Main) {
                    readerLoadState.value = ReaderLoadState.Error(t.message ?: "An unexpected error occurred.")
                }
            }
        }
    }

    private fun setupComposeTopBar() {
        val composeView = findViewById<ComposeView>(R.id.top_bar_compose_view)
        composeView.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        composeView.setContent {
            MulberryTheme(themeMode = activeTheme) {
                val readerPrefs by ReaderPreferencesManager.preferences.collectAsState()
                val tManager = thumbnailManager
                val sManager = searchManager
                if (tManager != null && sManager != null && readerLoadState.value is ReaderLoadState.Ready) {
                    AnimatedVisibility(
                        visible = showControlsState.value,
                        enter = slideInVertically(
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioLowBouncy,
                                stiffness = Spring.StiffnessMediumLow
                            ),
                            initialOffsetY = { -it }
                        ) + fadeIn(
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                        ),
                        exit = slideOutVertically(
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                            targetOffsetY = { -it }
                        ) + fadeOut(
                            animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                        )
                    ) {
                        Box(modifier = Modifier.statusBarsPadding()) {
                            ReaderTopBarScreen(
                                bookTitle = bookTitle,
                                bookPath = bookPath,
                                totalPages = totalPagesState.intValue,
                                currentPageIndex = currentSinglePage.intValue,
                                tocItems = tocItemsState.value,
                                isTocLoading = isTocLoadingState.value,
                                thumbnailManager = tManager,
                                searchManager = sManager,
                                onBackClick = {
                                    saveReadingProgress()
                                    finish()
                                },
                                onJumpToPage = { targetPageIdx ->
                                    jumpToPage(targetPageIdx)
                                },
                                onOpenWith = {
                                    openWithExternalApp()
                                },
                                onShare = {
                                    shareDocument()
                                },
                                onFeedback = {
                                    sendFeedbackEmail()
                                },
                                onAddToDesk = if (isExternalPdfState.value && !isAlreadyInDeskState.value) {
                                    { showAddToDeskSheetState.value = true }
                                } else null,
                                onContributeToLibrary = if (isExternalPdfState.value && isAlreadyInDeskState.value) {
                                    val externalIndex = com.example.util.MulberryStorageManager.loadDownloadedExternal(applicationContext)
                                    val externalMeta = externalIndex.books.firstOrNull { it.id == bookSlug || it.localPath == bookPath || it.title.equals(bookTitle, ignoreCase = true) }
                                    val status = externalMeta?.contributedStatus ?: "NONE"
                                    if (status != "PENDING" && status != "SUBMITTED") {
                                        {
                                            val slugToUse = externalMeta?.id ?: bookSlug.ifEmpty { bookTitle.slugify() }
                                            com.example.util.BookContributionManager.enqueueContribution(
                                                context = applicationContext,
                                                slug = slugToUse,
                                                title = bookTitle,
                                                author = externalMeta?.author ?: "Local Import",
                                                prof = externalMeta?.prof ?: "1st Prof",
                                                subject = externalMeta?.subject ?: "General",
                                                type = externalMeta?.type ?: "Standard Textbook",
                                                totalPages = totalPagesState.intValue,
                                                sizeMb = externalMeta?.sizeMb ?: 0.0
                                            )
                                            Toast.makeText(this@ReaderActivity, "Contribution enqueued for library upload!", Toast.LENGTH_SHORT).show()
                                        }
                                    } else null
                                } else null,
                                readerPreferences = readerPrefs,
                                onUpdatePreferences = { transform ->
                                    ReaderPreferencesManager.update(applicationContext, transform)
                                },
                                onMatchesChanged = { matches, currentIdx ->
                                    searchMatchesState.value = matches
                                    activeSearchMatchIndexState.intValue = currentIdx
                                }
                            )
                        }
                    }
                }
            }
        }
    }

    @OptIn(ExperimentalMaterial3Api::class)
    private fun setupSinglePageView() {
        val singlePageComposeView = findViewById<ComposeView>(R.id.single_page_compose_view)
        singlePageComposeView.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        singlePageComposeView.setContent {
            MulberryTheme(themeMode = activeTheme) {
                val colors = LocalMulberryColors.current
                when (val state = readerLoadState.value) {
                    is ReaderLoadState.Loading -> {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = colors.background
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .statusBarsPadding()
                                    .navigationBarsPadding(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(32.dp)
                                ) {
                                    CircularProgressIndicator(
                                        color = colors.primary,
                                        strokeWidth = 3.dp,
                                        modifier = Modifier.size(48.dp)
                                    )
                                    Spacer(modifier = Modifier.height(32.dp))
                                    Text(
                                        text = "Preparing Medical Textbook",
                                        fontFamily = PoppinsFamily,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 18.sp,
                                        color = colors.textPrimary
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = bookTitle.ifEmpty { "Medical Vault Textbook" },
                                        fontFamily = InterFamily,
                                        fontWeight = FontWeight.Normal,
                                        fontSize = 14.sp,
                                        color = colors.textSecondary,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(48.dp))
                                    OutlinedButton(
                                        onClick = {
                                            prepareJob?.cancel()
                                            finish()
                                        },
                                        shape = RoundedCornerShape(20.dp),
                                        border = BorderStroke(1.dp, colors.borderSubtle),
                                        colors = ButtonDefaults.outlinedButtonColors(contentColor = colors.textPrimary)
                                    ) {
                                        Icon(
                                            imageVector = FluentIcons.Dismiss24Regular,
                                            contentDescription = "Cancel",
                                            modifier = Modifier.size(16.dp),
                                            tint = colors.textPrimary
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "Cancel",
                                            fontFamily = InterFamily,
                                            fontSize = 13.sp,
                                            fontWeight = FontWeight.Medium,
                                            color = colors.textPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }
                    is ReaderLoadState.Error -> {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = colors.background
                        ) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .statusBarsPadding()
                                    .navigationBarsPadding(),
                                contentAlignment = Alignment.Center
                            ) {
                                Column(
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    modifier = Modifier.padding(32.dp)
                                ) {
                                    Icon(
                                        imageVector = FluentIcons.Dismiss24Regular,
                                        contentDescription = null,
                                        tint = colors.accentDanger,
                                        modifier = Modifier.size(44.dp)
                                    )
                                    Spacer(modifier = Modifier.height(20.dp))
                                    Text(
                                        text = "Unable to Open Book",
                                        fontFamily = PoppinsFamily,
                                        fontWeight = FontWeight.SemiBold,
                                        fontSize = 18.sp,
                                        color = colors.textPrimary
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Text(
                                        text = state.message,
                                        fontFamily = InterFamily,
                                        fontSize = 14.sp,
                                        color = colors.textSecondary,
                                        textAlign = TextAlign.Center
                                    )
                                    Spacer(modifier = Modifier.height(32.dp))
                                    Button(
                                        onClick = { finish() },
                                        shape = RoundedCornerShape(20.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
                                    ) {
                                        Text(
                                            text = "Close",
                                            fontFamily = InterFamily,
                                            fontSize = 14.sp,
                                            fontWeight = FontWeight.SemiBold,
                                            color = colors.onPrimary
                                        )
                                    }
                                }
                            }
                        }
                    }
                    is ReaderLoadState.Ready -> {
                        val readerPrefs by ReaderPreferencesManager.preferences.collectAsState()
                        val targetPath = activeRenderingPath.ifEmpty { bookPath }
                        val docFile = if (!targetPath.startsWith("content://")) File(targetPath).takeIf { it.exists() } else null
                        val docUri = if (targetPath.startsWith("content://")) Uri.parse(targetPath) else null

                        Box(modifier = Modifier.fillMaxSize()) {
                            SinglePagePdfViewer(
                                pdfFile = docFile,
                                pdfUri = docUri,
                                initialPage = currentSinglePage.intValue,
                                totalPages = totalPagesState.intValue,
                                targetPageJump = singlePageTargetJump.intValue,
                                searchMatches = searchMatchesState.value,
                                activeSearchMatchIndex = activeSearchMatchIndexState.intValue,
                                readerPreferences = readerPrefs,
                                onPageChanged = { pageIndex ->
                                    currentSinglePage.intValue = pageIndex
                                    val pageNum = pageIndex + 1
                                    visitedPages.add(pageNum)
                                    maxPageReached = maxOf(maxPageReached, pageNum)
                                },
                                onSpreadPagesVisited = { leftIdx, rightIdx ->
                                    val leftPageNum = leftIdx + 1
                                    visitedPages.add(leftPageNum)
                                    maxPageReached = maxOf(maxPageReached, leftPageNum)
                                    if (rightIdx != null) {
                                        val rightPageNum = rightIdx + 1
                                        visitedPages.add(rightPageNum)
                                        maxPageReached = maxOf(maxPageReached, rightPageNum)
                                    }
                                },
                                themeMode = activeTheme,
                                onToggleControls = { visible ->
                                    toggleReaderControls(visible)
                                },
                                modifier = Modifier.fillMaxSize()
                            )

                            // Live Contribution Upload Card if active
                            val uploadState by com.example.util.BookContributionManager.uploadState.collectAsState()
                            if (uploadState !is com.example.util.ContributionUploadState.Idle) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.TopCenter)
                                        .statusBarsPadding()
                                        .padding(top = if (showControlsState.value) 60.dp else 12.dp, start = 16.dp, end = 16.dp)
                                ) {
                                    ContributionLiveStatusCard(
                                        state = uploadState,
                                        onCancel = { slug -> com.example.util.BookContributionManager.cancelContribution(applicationContext, slug) },
                                        onDismiss = { com.example.util.BookContributionManager.dismissUploadState() }
                                    )
                                }
                            }

                            // Floating pill card positioned right above the bottom scrubber bar
                            if (showAddToDeskBannerState.value && externalPdfFile != null) {
                                Box(
                                    modifier = Modifier
                                        .align(Alignment.BottomCenter)
                                        .padding(bottom = if (showControlsState.value) 96.dp else 24.dp)
                                ) {
                                    ExternalPdfFloatingBanner(
                                        title = bookTitle,
                                        onAddToDeskClick = {
                                            showAddToDeskSheetState.value = true
                                        },
                                        onDismiss = {
                                            showAddToDeskBannerState.value = false
                                        }
                                    )
                                }
                            }

                            // Add to Desk Bottom Sheet
                            if (showAddToDeskSheetState.value && externalPdfFile != null) {
                                val userProfile = androidx.compose.runtime.remember {
                                    UserProfileRepository.getInstance(applicationContext).userProfile.value
                                }
                                AddToDeskBottomSheet(
                                    tempPdfFile = externalPdfFile!!,
                                    initialFileName = externalPdfFile!!.name,
                                    totalPages = totalPagesState.intValue,
                                    userProfile = userProfile,
                                    onDismissRequest = {
                                        showAddToDeskSheetState.value = false
                                    },
                                    onAddedToDesk = { slug, title ->
                                        bookSlug = slug
                                        bookTitle = title
                                        showAddToDeskSheetState.value = false
                                        showAddToDeskBannerState.value = false
                                        isAlreadyInDeskState.value = true
                                        Toast.makeText(this@ReaderActivity, "Added '$title' to Desk!", Toast.LENGTH_SHORT).show()
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun toggleReaderControls(visible: Boolean) {
        showControlsState.value = visible

        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        if (visible) {
            windowInsetsController.show(WindowInsetsCompat.Type.systemBars())
        } else {
            windowInsetsController.hide(WindowInsetsCompat.Type.systemBars())
            windowInsetsController.systemBarsBehavior =
                WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        }
    }

    fun jumpToPage(targetPageIdx: Int) {
        val safeIndex = targetPageIdx.coerceIn(0, (totalPagesState.intValue - 1).coerceAtLeast(0))
        currentSinglePage.intValue = safeIndex
        singlePageTargetJump.intValue = safeIndex
    }

    private suspend fun extractPdfMetadataAndTocSafely() {
        val targetPath = activeRenderingPath.ifEmpty { bookPath }
        withTimeoutOrNull(5000L) {
            var pdDoc: PDDocument? = null
            try {
                PdfTextSearchManager.initPdfBox(applicationContext)
                pdDoc = withContext(Dispatchers.IO) {
                    if (targetPath.startsWith("content://")) {
                        contentResolver.openInputStream(Uri.parse(targetPath))?.use { inputStream ->
                            PDDocument.load(inputStream)
                        }
                    } else {
                        val file = File(targetPath)
                        if (file.exists()) PDDocument.load(file) else null
                    }
                }

                if (pdDoc != null) {
                    val outline: PDDocumentOutline? = pdDoc.documentCatalog?.documentOutline
                    val parsed = if (outline != null) {
                        val visited = mutableSetOf<String>()
                        parseBookmarksRecursively(pdDoc, outline.children(), visited, 0)
                    } else {
                        emptyList()
                    }
                    withContext(Dispatchers.Main) {
                        tocItemsState.value = parsed
                        isTocLoadingState.value = false
                    }
                } else {
                    withContext(Dispatchers.Main) {
                        isTocLoadingState.value = false
                    }
                }
            } catch (t: Throwable) {
                Log.w("ReaderActivity", "TOC extraction non-fatal warning: ${t.message}")
                withContext(Dispatchers.Main) {
                    isTocLoadingState.value = false
                }
            } finally {
                try {
                    pdDoc?.close()
                } catch (_: Throwable) {}
            }
        } ?: run {
            Log.w("ReaderActivity", "TOC extraction timed out; proceeding without bookmarks")
            withContext(Dispatchers.Main) {
                isTocLoadingState.value = false
            }
        }
    }

    private fun parseBookmarksRecursively(
        doc: PDDocument,
        items: Iterable<PDOutlineItem>?,
        visited: MutableSet<String>,
        depth: Int
    ): List<TocItem> {
        if (items == null || depth > 8) return emptyList()
        val result = mutableListOf<TocItem>()
        for (item in items) {
            val title = item.title?.takeIf { it.isNotBlank() } ?: "Section"
            val destinationPage = try {
                item.findDestinationPage(doc)
            } catch (_: Throwable) {
                null
            }
            val pageIdx = if (destinationPage != null) {
                doc.pages.indexOf(destinationPage).coerceAtLeast(0).toLong()
            } else {
                0L
            }
            val key = "${pageIdx}_$title"
            if (!visited.add(key)) continue

            val children = if (item.hasChildren()) {
                parseBookmarksRecursively(doc, item.children(), visited, depth + 1)
            } else {
                emptyList()
            }
            result.add(
                TocItem(
                    title = title,
                    pageIdx = pageIdx,
                    children = children
                )
            )
        }
        return result
    }

    private fun getDocumentUri(): Uri? {
        val targetPath = activeRenderingPath.ifEmpty { bookPath }
        return if (targetPath.startsWith("content://")) {
            Uri.parse(targetPath)
        } else {
            val file = File(targetPath)
            if (file.exists()) {
                try {
                    FileProvider.getUriForFile(this, "$packageName.provider", file)
                } catch (_: Exception) {
                    Uri.fromFile(file)
                }
            } else {
                null
            }
        }
    }

    private fun openWithExternalApp() {
        try {
            val uri = getDocumentUri() ?: return
            val intent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/pdf")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, "Open With..."))
        } catch (e: Exception) {
            Toast.makeText(this, "Could not open document: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun shareDocument() {
        try {
            val uri = getDocumentUri() ?: return
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/pdf"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            startActivity(Intent.createChooser(intent, "Share Document"))
        } catch (e: Exception) {
            Toast.makeText(this, "Could not share document: ${e.message}", Toast.LENGTH_SHORT).show()
        }
    }

    private fun sendFeedbackEmail() {
        try {
            val body = """
                Hi Mulberry Team,

                [Please type your feedback or thoughts here]

                ---
                Diagnostics:
                • Device: ${android.os.Build.MANUFACTURER} ${android.os.Build.MODEL}
                • Android Version: Android ${android.os.Build.VERSION.RELEASE} (SDK ${android.os.Build.VERSION.SDK_INT})
                • App Version: Mulberry v1.0.0
                • Document: $bookTitle
                • Total Pages: ${totalPagesState.intValue}
            """.trimIndent()

            val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:support@mulberry.app?subject=" + Uri.encode("Feedback on Mulberry PDF Viewer") + "&body=" + Uri.encode(body))
            }
            startActivity(emailIntent)
        } catch (_: Exception) {
            Toast.makeText(this, "Could not launch email app", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveReadingProgress() {
        val totalPages = totalPagesState.intValue.coerceAtLeast(1)
        val currentPage = (currentSinglePage.intValue + 1).coerceIn(1, totalPages)
        val progressPercent = ((currentPage.toFloat() / totalPages.toFloat()) * 100f).coerceIn(0f, 100f)

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val db = MulberryDatabase.getInstance(applicationContext)
                val bookDao = db.bookDao()
                val now = System.currentTimeMillis()

                if (bookPath.isNotBlank()) {
                    bookDao.updateProgress(
                        bookPath = bookPath,
                        lastPage = currentPage,
                        totalPages = totalPages,
                        progressPercent = progressPercent,
                        lastReadTimestamp = now
                    )
                }

                if (bookId != -1) {
                    bookDao.updateReadingProgress(
                        id = bookId,
                        page = currentPage,
                        percent = progressPercent.toInt(),
                        timestamp = now
                    )
                }

                val effectiveSlug = bookSlug.ifEmpty {
                    val b = if (bookId != -1) bookDao.getBookById(bookId) else bookDao.getAllBooksSync().find { it.uriString == bookPath || it.binFilePath == bookPath }
                    b?.slug ?: ""
                }

                if (effectiveSlug.isNotBlank()) {
                    bookDao.updateReadingProgressBySlug(
                        slug = effectiveSlug,
                        page = currentPage,
                        percent = progressPercent.toInt(),
                        timestamp = now
                    )
                }

                // Record truthful telemetry to /sdcard/Mulberry/reading_history.json
                val resolvedBook = if (effectiveSlug.isNotBlank()) bookDao.getBookBySlug(effectiveSlug) else null
                val resolvedTitle = bookTitle.ifEmpty { resolvedBook?.title ?: "Book" }
                val resolvedSubject = resolvedBook?.subject ?: ""
                val actualVisited = visitedPages.size.coerceAtLeast(1)

                if (effectiveSlug.isNotBlank()) {
                    com.example.util.MulberryStorageManager.recordReadingActivity(
                        context = applicationContext,
                        bookSlug = effectiveSlug,
                        bookTitle = resolvedTitle,
                        subject = resolvedSubject,
                        lastReadPage = currentPage,
                        totalPages = totalPages,
                        durationSeconds = accumulatedActiveSeconds,
                        startPage = initialOpenedPage,
                        endPage = currentPage,
                        pagesVisitedCount = actualVisited,
                        sessionId = sessionId
                    )
                }

                // Handle study session progress and weekly progress tracking
                val currentSessionId = sessionId
                val activeMinutes = (accumulatedActiveSeconds / 60).toInt().coerceAtLeast(1)

                val scheduledPagesCount: Int
                val outsidePagesCount: Int

                if (!currentSessionId.isNullOrBlank() && sessionStartPage > 0 && sessionEndPage >= sessionStartPage) {
                    val scheduledRange = sessionStartPage..sessionEndPage
                    val scheduledVisited = visitedPages.filter { it in scheduledRange }.toSet()
                    scheduledPagesCount = scheduledVisited.size
                    outsidePagesCount = visitedPages.count { it !in scheduledRange }
                    val isAllScheduledPagesVisited = scheduledRange.all { it in visitedPages }

                    if (isAllScheduledPagesVisited) {
                        com.example.data.schedule.ScheduleRepository.getInstance(applicationContext)
                            .markCompleted(
                                sessionId = currentSessionId,
                                actualReadPages = scheduledPagesCount,
                                actualDurationMinutes = activeMinutes
                            )
                    } else {
                        com.example.data.schedule.ScheduleRepository.getInstance(applicationContext)
                            .updateIncrementalSessionProgress(
                                sessionId = currentSessionId,
                                actualReadPages = scheduledPagesCount,
                                actualDurationMinutes = activeMinutes
                            )
                    }
                } else {
                    scheduledPagesCount = 0
                    outsidePagesCount = visitedPages.size
                }

                // Credit any pages read outside the scheduled session (or non-session free reading) to weekly progress
                val incrementalOutside = (outsidePagesCount - recordedExtraPagesCount).coerceAtLeast(0)
                if (incrementalOutside > 0) {
                    com.example.data.schedule.ScheduleRepository.getInstance(applicationContext)
                        .recordExtraPagesRead(incrementalOutside)
                    recordedExtraPagesCount = outsidePagesCount
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onPause() {
        if (resumeTimestamp > 0L) {
            val activeSeconds = ((System.currentTimeMillis() - resumeTimestamp) / 1000).coerceAtLeast(0)
            accumulatedActiveSeconds += activeSeconds
            resumeTimestamp = 0L
        }
        saveReadingProgress()
        super.onPause()
    }

    private fun logReadingSessionIfNeeded() {
        if (hasLoggedSessionCompleted || sessionStartTimeMs <= 0L) return
        hasLoggedSessionCompleted = true
        val durationSeconds = ((System.currentTimeMillis() - sessionStartTimeMs) / 1000).coerceAtLeast(0)
        val pagesRead = visitedPages.size.coerceAtLeast(1)
        MulberryAnalytics.logBookSessionCompleted(
            bookId = if (bookId != -1) bookId.toString() else (bookTitle.ifEmpty { "unknown_book" }),
            durationSeconds = durationSeconds,
            pagesRead = pagesRead
        )
    }

    override fun onDestroy() {
        logReadingSessionIfNeeded()
        prepareJob?.cancel()
        thumbnailManager?.clearMemory()
        ephemeralPdfFile?.let {
            com.example.util.VaultCryptoManager.purgeEphemeralSession(it)
        }
        super.onDestroy()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        saveReadingProgress()
        super.onBackPressed()
    }

    override fun finish() {
        super.finish()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            overrideActivityTransition(
                Activity.OVERRIDE_TRANSITION_CLOSE,
                R.anim.book_close_enter,
                R.anim.book_close_exit
            )
        } else {
            @Suppress("DEPRECATION")
            overridePendingTransition(
                R.anim.book_close_enter,
                R.anim.book_close_exit
            )
        }
    }
}
