package com.example.ui.reader

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.ParcelFileDescriptor
import android.view.View
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.compose.ui.graphics.toArgb
import androidx.core.content.FileProvider
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.example.R
import com.example.data.MulberryDatabase
import com.example.data.repository.VaultMetadataManager
import com.example.ui.theme.MulberryTheme
import com.example.ui.theme.ThemeMode
import com.shockwave.pdfium.PdfDocument
import com.shockwave.pdfium.PdfiumCore
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class ReaderActivity : AppCompatActivity() {

    companion object {
        const val EXTRA_BOOK_PATH = "BOOK_PATH"
        const val EXTRA_BOOK_TITLE = "BOOK_TITLE"
        const val EXTRA_BOOK_ID = "BOOK_ID"

        fun launch(context: Context, bookPath: String, bookTitle: String, bookId: Int = -1) {
            val intent = Intent(context, ReaderActivity::class.java).apply {
                putExtra(EXTRA_BOOK_PATH, bookPath)
                putExtra(EXTRA_BOOK_TITLE, bookTitle)
                putExtra(EXTRA_BOOK_ID, bookId)
            }
            context.startActivity(intent)
        }
    }

    private var bookPath: String = ""
    private var bookTitle: String = ""
    private var bookId: Int = -1

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

    private lateinit var thumbnailManager: PdfThumbnailManager
    private lateinit var searchManager: PdfTextSearchManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Enable edge-to-edge layout for sleek immersive bar transitions
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Read active theme from SharedPreferences
        val prefs = getSharedPreferences("mulberry_prefs", Context.MODE_PRIVATE)
        val savedTheme = prefs.getString("theme_mode", ThemeMode.PAPER.name)
        activeTheme = ThemeMode.from(savedTheme)

        setContentView(R.layout.activity_reader)

        // Set root container background to match the active theme palette
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

        thumbnailManager = PdfThumbnailManager(applicationContext, bookPath)
        searchManager = PdfTextSearchManager(applicationContext, bookPath, totalPagesState.intValue)

        // Restore last read page from database for seamless cold start
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val db = MulberryDatabase.getInstance(applicationContext)
                val book = if (bookId != -1) {
                    db.bookDao().getBookById(bookId)
                } else {
                    db.bookDao().getAllBooksSync().find { it.uriString == bookPath }
                }
                if (book != null && book.lastReadPage > 0) {
                    withContext(Dispatchers.Main) {
                        val zeroIdx = (book.lastReadPage - 1).coerceAtLeast(0)
                        currentSinglePage.intValue = zeroIdx
                        singlePageTargetJump.intValue = zeroIdx
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        // Setup unified Compose TopAppBar, thumbnail broad view, and floating search
        setupComposeTopBar()

        // Hook system back gesture
        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                saveReadingProgress()
                finish()
            }
        })

        // Extract true total page count & TOC via background PdfiumCore
        extractPdfMetadataAndToc()

        // Setup Compose Single-Page Pager Viewport
        setupSinglePageView()
    }

    private fun setupComposeTopBar() {
        val composeView = findViewById<ComposeView>(R.id.top_bar_compose_view)
        composeView.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        composeView.setContent {
            MulberryTheme(themeMode = activeTheme) {
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
                            thumbnailManager = thumbnailManager,
                            searchManager = searchManager,
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

    private fun setupSinglePageView() {
        val singlePageComposeView = findViewById<ComposeView>(R.id.single_page_compose_view)
        singlePageComposeView.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
        singlePageComposeView.setContent {
            MulberryTheme(themeMode = activeTheme) {
                val docFile = if (!bookPath.startsWith("content://")) File(bookPath).takeIf { it.exists() } else null
                val docUri = if (bookPath.startsWith("content://")) Uri.parse(bookPath) else null

                SinglePagePdfViewer(
                    pdfFile = docFile,
                    pdfUri = docUri,
                    initialPage = currentSinglePage.intValue,
                    totalPages = totalPagesState.intValue,
                    targetPageJump = singlePageTargetJump.intValue,
                    searchMatches = searchMatchesState.value,
                    activeSearchMatchIndex = activeSearchMatchIndexState.intValue,
                    onPageChanged = { pageIndex ->
                        currentSinglePage.intValue = pageIndex
                    },
                    themeMode = activeTheme,
                    onToggleControls = { visible ->
                        toggleReaderControls(visible)
                    },
                    modifier = Modifier.fillMaxSize()
                )
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

    private fun extractPdfMetadataAndToc() {
        CoroutineScope(Dispatchers.IO).launch {
            val pfd: ParcelFileDescriptor? = try {
                if (bookPath.startsWith("content://")) {
                    contentResolver.openFileDescriptor(Uri.parse(bookPath), "r")
                } else {
                    val file = File(bookPath)
                    if (file.exists()) ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY) else null
                }
            } catch (e: Exception) {
                e.printStackTrace()
                null
            }

            if (pfd != null) {
                var pdfiumCore: PdfiumCore? = null
                var pdfDoc: PdfDocument? = null
                try {
                    pdfiumCore = PdfiumCore(applicationContext)
                    pdfDoc = pdfiumCore.newDocument(pfd)

                    val realPageCount = pdfiumCore.getPageCount(pdfDoc)
                    if (realPageCount > 0) {
                        withContext(Dispatchers.Main) {
                            totalPagesState.intValue = realPageCount
                            searchManager = PdfTextSearchManager(applicationContext, bookPath, realPageCount)
                        }
                    }

                    // Extract Table of Contents
                    val bookmarks = pdfiumCore.getTableOfContents(pdfDoc)
                    val parsed = parseBookmarksRecursively(bookmarks)
                    withContext(Dispatchers.Main) {
                        tocItemsState.value = parsed
                        isTocLoadingState.value = false
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    withContext(Dispatchers.Main) {
                        isTocLoadingState.value = false
                    }
                } finally {
                    try {
                        if (pdfDoc != null && pdfiumCore != null) {
                            pdfiumCore.closeDocument(pdfDoc)
                        }
                    } catch (_: Exception) {}
                    try {
                        pfd.close()
                    } catch (_: Exception) {}
                }
            } else {
                withContext(Dispatchers.Main) {
                    isTocLoadingState.value = false
                }
            }
        }
    }

    private fun parseBookmarksRecursively(list: List<PdfDocument.Bookmark>?): List<TocItem> {
        if (list.isNullOrEmpty()) return emptyList()
        return list.map { b ->
            val title = b.title?.takeIf { it.isNotBlank() } ?: "Untitled Section"
            val children = if (b.hasChildren() && b.children != null) {
                parseBookmarksRecursively(b.children)
            } else {
                emptyList()
            }
            TocItem(
                title = title,
                pageIdx = b.pageIdx,
                children = children
            )
        }
    }

    private fun getDocumentUri(): Uri? {
        return if (bookPath.startsWith("content://")) {
            Uri.parse(bookPath)
        } else {
            val file = File(bookPath)
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

        CoroutineScope(Dispatchers.IO).launch {
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

                // Persist updated metadata to .mulberry folder
                try {
                    val book = if (bookId != -1) bookDao.getBookById(bookId) else bookDao.getAllBooksSync().find { it.uriString == bookPath }
                    if (book != null) {
                        val vault = db.vaultDao().getAllVaultsSync().find { it.id == book.vaultId }
                        val vaultRoot = vault?.pathDisplay?.ifEmpty { vault.uriString }
                            ?: java.io.File(bookPath).parentFile?.parentFile?.absolutePath
                            ?: java.io.File(bookPath).parentFile?.absolutePath
                        if (!vaultRoot.isNullOrBlank()) {
                            VaultMetadataManager.persistVaultMetadata(
                                vaultRootPath = vaultRoot,
                                vaultId = book.vaultId.toString(),
                                context = applicationContext
                            )
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    override fun onPause() {
        saveReadingProgress()
        super.onPause()
    }

    override fun onDestroy() {
        thumbnailManager.clearMemory()
        super.onDestroy()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        saveReadingProgress()
        super.onBackPressed()
    }
}
