package com.example.ui.reader

import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.LruCache
import android.widget.Toast
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.calculatePan
import androidx.compose.foundation.gestures.calculateZoom
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.compose.ui.zIndex
import androidx.core.content.FileProvider
import com.example.ui.components.FluentIcons
import com.example.ui.theme.InterFamily
import com.example.ui.theme.LocalMulberryColors
import com.example.ui.theme.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

private enum class SpreadHalf {
    FULL,
    LEFT,
    RIGHT
}

/**
 * Single-Page & Dual-Page Spread PDF Reader powered by Android's native PdfRenderer and Jetpack Compose HorizontalPager.
 * Features:
 * - Eye Relax GPU ColorMatrix calibration overlay (warmth blue-light filter, software night dimmer, E-Ink grayscale)
 * - Adaptive & manual Dual-Page Book Spread for tablets, foldables, and landscape orientation
 * - Cover-page aware spread indexing with 1.dp book gutter divider and blank opposite page for odd trailing pages
 * - Conditional gesture routing (swipes smoothly at scale <= 1.05f; pans/zooms when scale > 1.05f)
 * - Animated double-tap 2.5x zoom inspecting the tapped page (left or right)
 * - One-handed tap zones: left 20% previous spread, right 20% next spread, middle 60% overlay toggle
 * - Floating Page Scrubber Bar displaying single or dual spread page range (e.g., "Page 142–143 / 1150")
 */
@Composable
fun SinglePagePdfViewer(
    pdfFile: File? = null,
    pdfUri: Uri? = null,
    initialPage: Int = 0,
    totalPages: Int = 1,
    targetPageJump: Int = -1,
    searchMatches: List<SearchMatch> = emptyList(),
    activeSearchMatchIndex: Int = -1,
    readerPreferences: ReaderPreferences = ReaderPreferences(),
    onPageChanged: (Int) -> Unit = {},
    onSpreadPagesVisited: (Int, Int?) -> Unit = { _, _ -> },
    themeMode: ThemeMode = ThemeMode.LIGHT,
    onTap: (() -> Unit)? = null,
    onToggleControls: ((Boolean) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = LocalMulberryColors.current
    val configuration = LocalConfiguration.current
    val coroutineScope = rememberCoroutineScope()

    var pdfRenderer by remember { mutableStateOf<PdfRenderer?>(null) }
    var pageCount by remember { mutableIntStateOf(totalPages.coerceAtLeast(1)) }
    var isCurrentPageZoomed by remember { mutableStateOf(false) }
    var showControls by remember { mutableStateOf(true) }

    // Track logical 0-based page index so mode/orientation changes preserve the exact reading spot
    var activeAnchorPageIndex by remember { mutableIntStateOf(initialPage.coerceAtLeast(0)) }

    val hapticFeedback = LocalHapticFeedback.current
    var circleSearchCoords by remember { mutableStateOf<Offset?>(null) }

    // Synchronize access to PdfRenderer (Android allows only one open page at a time)
    val rendererMutex = remember { Mutex() }

    // Memory cache for rendered page bitmaps (retains up to 12 pages for smooth dual-page spreads)
    val pageCache = remember {
        object : LruCache<Int, Bitmap>(12) {}
    }

    // Detect wide screens (tablets, foldables, or landscape orientation)
    val isLandscape = configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
    val isWideScreen = configuration.screenWidthDp >= 600 || isLandscape
    val isDualPageActive = when (readerPreferences.dualPageMode) {
        DualPageMode.AUTO -> isWideScreen && pageCount > 1
        DualPageMode.FORCE_SINGLE -> false
        DualPageMode.FORCE_DUAL -> pageCount > 1
    }

    val spreadCount = remember(pageCount, isDualPageActive, readerPreferences.firstPageIsCover) {
        DualSpreadCalculator.getSpreadCount(
            totalPages = pageCount,
            isDualActive = isDualPageActive,
            firstPageIsCover = readerPreferences.firstPageIsCover
        )
    }

    // Compute Eye Relax ColorFilter without re-rasterizing underlying bitmaps
    val eyeRelaxColorFilter = remember(
        readerPreferences.eyeRelaxEnabled,
        readerPreferences.eyeRelaxWarmth,
        readerPreferences.eyeRelaxDim,
        readerPreferences.eyeRelaxGrayscale
    ) {
        if (readerPreferences.eyeRelaxEnabled) {
            ColorFilter.colorMatrix(readerPreferences.computeColorMatrix())
        } else {
            null
        }
    }

    fun shareHighDefinitionPage(pageIndex: Int) {
        if (pageIndex !in 0 until pageCount) return
        coroutineScope.launch {
            Toast.makeText(context, "Rendering high definition page...", Toast.LENGTH_SHORT).show()
            val highDefBitmap = withContext(Dispatchers.IO) {
                try {
                    rendererMutex.withLock {
                        val renderer = pdfRenderer ?: return@withLock null
                        val page = renderer.openPage(pageIndex)
                        try {
                            val scale = 2.5f
                            val bmpW = (page.width * scale).toInt().coerceAtLeast(1)
                            val bmpH = (page.height * scale).toInt().coerceAtLeast(1)
                            val bmp = Bitmap.createBitmap(bmpW, bmpH, Bitmap.Config.ARGB_8888)
                            val canvas = Canvas(bmp)
                            canvas.drawColor(AndroidColor.WHITE)
                            page.render(bmp, null, null, PdfRenderer.Page.RENDER_MODE_FOR_PRINT)
                            bmp
                        } finally {
                            page.close()
                        }
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    null
                }
            }

            if (highDefBitmap != null) {
                withContext(Dispatchers.IO) {
                    try {
                        val cacheDir = File(context.cacheDir, "shared_pages")
                        if (!cacheDir.exists()) cacheDir.mkdirs()
                        val pageFile = File(cacheDir, "page_${pageIndex + 1}.png")
                        FileOutputStream(pageFile).use { out ->
                            highDefBitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
                        }
                        val pageUri = FileProvider.getUriForFile(
                            context,
                            "${context.packageName}.provider",
                            pageFile
                        )
                        withContext(Dispatchers.Main) {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "image/png"
                                putExtra(Intent.EXTRA_STREAM, pageUri)
                                putExtra(Intent.EXTRA_SUBJECT, "Page ${pageIndex + 1}")
                                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Share Page ${pageIndex + 1} Image"))
                        }
                    } catch (e: Exception) {
                        e.printStackTrace()
                        withContext(Dispatchers.Main) {
                            Toast.makeText(context, "Failed to share page: ${e.message}", Toast.LENGTH_SHORT).show()
                        }
                    }
                }
            } else {
                Toast.makeText(context, "Could not render page image", Toast.LENGTH_SHORT).show()
            }
        }
    }

    fun triggerCircleToSearch(pressOffset: Offset, pageBitmap: Bitmap?, pageIndex: Int) {
        hapticFeedback.performHapticFeedback(HapticFeedbackType.LongPress)
        circleSearchCoords = pressOffset
        coroutineScope.launch {
            CircleToSearchHelper.activateCircleToSearch(
                context = context,
                pageBitmap = pageBitmap,
                pageIndex = pageIndex
            )
        }
    }

    var initErrorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(totalPages) {
        if (totalPages > pageCount) {
            pageCount = totalPages
        }
    }

    // Open native PdfRenderer from File or Content Uri
    DisposableEffect(pdfFile, pdfUri) {
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        try {
            initErrorMessage = null
            pfd = when {
                pdfFile != null && pdfFile.exists() -> {
                    ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
                }
                pdfUri != null -> {
                    context.contentResolver.openFileDescriptor(pdfUri, "r")
                }
                else -> null
            }
            if (pfd != null) {
                renderer = PdfRenderer(pfd)
                pdfRenderer = renderer
                pageCount = renderer.pageCount.coerceAtLeast(1)
            } else {
                initErrorMessage = "PDF file could not be opened on device."
            }
        } catch (t: Throwable) {
            t.printStackTrace()
            initErrorMessage = "Unable to read document: ${t.message ?: "Format error"}"
        }

        onDispose {
            try {
                renderer?.close()
            } catch (_: Exception) {}
            try {
                pfd?.close()
            } catch (_: Exception) {}
            pageCache.evictAll()
        }
    }

    val initialSpreadIndex = remember {
        DualSpreadCalculator.pageIndexToSpreadIndex(
            pageIndex = initialPage.coerceIn(0, (pageCount - 1).coerceAtLeast(0)),
            totalPages = pageCount,
            isDualActive = isDualPageActive,
            firstPageIsCover = readerPreferences.firstPageIsCover
        ).coerceIn(0, (spreadCount - 1).coerceAtLeast(0))
    }

    val pagerState = rememberPagerState(initialPage = initialSpreadIndex) { spreadCount }

    // Keep current reading page anchored when switching between Single and Dual spread modes
    LaunchedEffect(isDualPageActive, readerPreferences.firstPageIsCover, pageCount) {
        val targetSpread = DualSpreadCalculator.pageIndexToSpreadIndex(
            pageIndex = activeAnchorPageIndex.coerceIn(0, (pageCount - 1).coerceAtLeast(0)),
            totalPages = pageCount,
            isDualActive = isDualPageActive,
            firstPageIsCover = readerPreferences.firstPageIsCover
        ).coerceIn(0, (spreadCount - 1).coerceAtLeast(0))

        if (targetSpread != pagerState.currentPage) {
            pagerState.scrollToPage(targetSpread)
        }
    }

    val currentSpread = remember(
        pagerState.currentPage,
        pageCount,
        isDualPageActive,
        readerPreferences.firstPageIsCover
    ) {
        DualSpreadCalculator.getSpreadForIndex(
            spreadIndex = pagerState.currentPage,
            totalPages = pageCount,
            isDualActive = isDualPageActive,
            firstPageIsCover = readerPreferences.firstPageIsCover
        )
    }

    // Report page changes to caller
    LaunchedEffect(currentSpread.leftPageIndex, currentSpread.rightPageIndex) {
        activeAnchorPageIndex = currentSpread.leftPageIndex
        onPageChanged(currentSpread.leftPageIndex)
        onSpreadPagesVisited(currentSpread.leftPageIndex, currentSpread.rightPageIndex)
        // Reset zoom state on spread turn
        isCurrentPageZoomed = false
    }

    // Handle external jump requests (TOC, Search, Thumbnail Grid)
    LaunchedEffect(targetPageJump) {
        if (targetPageJump in 0 until pageCount) {
            activeAnchorPageIndex = targetPageJump
            val targetSpread = DualSpreadCalculator.pageIndexToSpreadIndex(
                pageIndex = targetPageJump,
                totalPages = pageCount,
                isDualActive = isDualPageActive,
                firstPageIsCover = readerPreferences.firstPageIsCover
            ).coerceIn(0, (spreadCount - 1).coerceAtLeast(0))

            if (targetSpread != pagerState.currentPage) {
                coroutineScope.launch {
                    if (kotlin.math.abs(targetSpread - pagerState.currentPage) > 3) {
                        pagerState.scrollToPage(targetSpread)
                    } else {
                        pagerState.animateScrollToPage(targetSpread)
                    }
                }
            }
        }
    }

    fun handleSpreadTapZone(screenXFraction: Float) {
        when {
            // Left 20% of screen: Navigate back 1 spread
            screenXFraction < 0.20f -> {
                coroutineScope.launch {
                    val prev = (pagerState.currentPage - 1).coerceAtLeast(0)
                    pagerState.animateScrollToPage(prev)
                }
            }
            // Right 20% of screen: Navigate forward 1 spread
            screenXFraction > 0.80f -> {
                coroutineScope.launch {
                    val next = (pagerState.currentPage + 1).coerceAtMost(spreadCount - 1)
                    pagerState.animateScrollToPage(next)
                }
            }
            // Middle 60% of screen: Toggle overlays & system bars
            else -> {
                showControls = !showControls
                onToggleControls?.invoke(showControls)
                onTap?.invoke()
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        if (initErrorMessage != null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(28.dp)
                ) {
                    Icon(
                        imageVector = FluentIcons.Dismiss24Regular,
                        contentDescription = null,
                        tint = colors.accentDanger,
                        modifier = Modifier.size(36.dp)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = initErrorMessage ?: "Document Error",
                        fontFamily = InterFamily,
                        fontSize = 14.sp,
                        color = colors.textPrimary,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else if (pdfRenderer == null) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = colors.primary,
                    strokeWidth = 2.5.dp,
                    modifier = Modifier.size(36.dp)
                )
            }
        } else {
            // Horizontal Pager over spreads, with Eye Relax GPU ColorMatrix applied over the viewport
            HorizontalPager(
                state = pagerState,
                userScrollEnabled = !isCurrentPageZoomed,
                beyondViewportPageCount = 1,
                modifier = Modifier
                    .fillMaxSize()
                    .testTag("pdf_spread_pager")
            ) { spreadIndex ->
                val spread = DualSpreadCalculator.getSpreadForIndex(
                    spreadIndex = spreadIndex,
                    totalPages = pageCount,
                    isDualActive = isDualPageActive,
                    firstPageIsCover = readerPreferences.firstPageIsCover
                )
                val isCurrentSpread = spreadIndex == pagerState.currentPage

                if (!isDualPageActive || spread.isSingleCenteredCover) {
                    // Single page or centered Cover Page (Spread 0 when firstPageIsCover == true)
                    SinglePdfPageItem(
                        pageIndex = spread.leftPageIndex,
                        renderer = pdfRenderer,
                        rendererMutex = rendererMutex,
                        pageCache = pageCache,
                        isCurrentPage = isCurrentSpread,
                        spreadHalf = SpreadHalf.FULL,
                        eyeRelaxColorFilter = eyeRelaxColorFilter,
                        searchMatches = searchMatches,
                        activeSearchMatchIndex = activeSearchMatchIndex,
                        onZoomStateChanged = { zoomed ->
                            if (isCurrentSpread) {
                                isCurrentPageZoomed = zoomed
                            }
                        },
                        onTapZone = { xFraction ->
                            handleSpreadTapZone(xFraction)
                        },
                        onLongPressCircleToSearch = { pressOffset, pageBitmap ->
                            triggerCircleToSearch(pressOffset, pageBitmap, spread.leftPageIndex)
                        }
                    )
                } else {
                    // Dual-Page Spread Row with 1.dp book gutter divider
                    var leftZoomed by remember(spreadIndex) { mutableStateOf(false) }
                    var rightZoomed by remember(spreadIndex) { mutableStateOf(false) }

                    LaunchedEffect(leftZoomed, rightZoomed, isCurrentSpread) {
                        if (isCurrentSpread) {
                            isCurrentPageZoomed = leftZoomed || rightZoomed
                        }
                    }

                    Row(
                        modifier = Modifier
                            .fillMaxSize()
                            .testTag("dual_page_spread_row"),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Left Page Half
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .zIndex(if (leftZoomed) 2f else 1f)
                        ) {
                            SinglePdfPageItem(
                                pageIndex = spread.leftPageIndex,
                                renderer = pdfRenderer,
                                rendererMutex = rendererMutex,
                                pageCache = pageCache,
                                isCurrentPage = isCurrentSpread && !rightZoomed,
                                spreadHalf = SpreadHalf.LEFT,
                                eyeRelaxColorFilter = eyeRelaxColorFilter,
                                searchMatches = searchMatches,
                                activeSearchMatchIndex = activeSearchMatchIndex,
                                onZoomStateChanged = { zoomed ->
                                    leftZoomed = zoomed
                                    if (zoomed) rightZoomed = false
                                },
                                onTapZone = { localXFraction ->
                                    // Map left half [0..1] to full screen [0..0.5]
                                    handleSpreadTapZone(localXFraction * 0.5f)
                                },
                                onLongPressCircleToSearch = { pressOffset, pageBitmap ->
                                    triggerCircleToSearch(pressOffset, pageBitmap, spread.leftPageIndex)
                                }
                            )
                        }

                        // Subtle 1.dp book gutter divider (surfaceBorder)
                        Box(
                            modifier = Modifier
                                .width(1.dp)
                                .fillMaxHeight()
                                .background(colors.borderSubtle)
                                .testTag("dual_page_gutter_divider")
                        )

                        // Right Page Half (or blank surface if final odd page)
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .zIndex(if (rightZoomed) 2f else 1f)
                        ) {
                            if (spread.rightPageIndex != null) {
                                SinglePdfPageItem(
                                    pageIndex = spread.rightPageIndex,
                                    renderer = pdfRenderer,
                                    rendererMutex = rendererMutex,
                                    pageCache = pageCache,
                                    isCurrentPage = isCurrentSpread && !leftZoomed,
                                    spreadHalf = SpreadHalf.RIGHT,
                                    eyeRelaxColorFilter = eyeRelaxColorFilter,
                                    searchMatches = searchMatches,
                                    activeSearchMatchIndex = activeSearchMatchIndex,
                                    onZoomStateChanged = { zoomed ->
                                        rightZoomed = zoomed
                                        if (zoomed) leftZoomed = false
                                    },
                                    onTapZone = { localXFraction ->
                                        // Map right half [0..1] to full screen [0.5..1.0]
                                        handleSpreadTapZone(0.5f + localXFraction * 0.5f)
                                    },
                                    onLongPressCircleToSearch = { pressOffset, pageBitmap ->
                                        triggerCircleToSearch(pressOffset, pageBitmap, spread.rightPageIndex)
                                    }
                                )
                            } else {
                                // Final odd page in dual mode: right page space blank with theme surface background
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .background(colors.surface)
                                        .pointerInput(spreadIndex) {
                                            detectTapGestures(
                                                onTap = { tapOffset ->
                                                    val localXFraction = tapOffset.x / size.width.toFloat().coerceAtLeast(1f)
                                                    handleSpreadTapZone(0.5f + localXFraction * 0.5f)
                                                }
                                            )
                                        }
                                        .testTag("dual_page_blank_trailing_slot")
                                )
                            }
                        }
                    }
                }
            }

            // Floating Page Scrubber Bar - sleek spring slide and fade animation
            AnimatedVisibility(
                visible = showControls,
                enter = slideInVertically(
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessMediumLow
                    ),
                    initialOffsetY = { it }
                ) + fadeIn(
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                ),
                exit = slideOutVertically(
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
                    targetOffsetY = { it }
                ) + fadeOut(
                    animationSpec = spring(stiffness = Spring.StiffnessMediumLow)
                ),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(horizontal = 16.dp, vertical = 20.dp)
                    .navigationBarsPadding()
            ) {
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = colors.surface.copy(alpha = 0.94f),
                    shadowElevation = 6.dp,
                    border = BorderStroke(0.5.dp, colors.borderSubtle),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        // Previous Spread / Page IconButton
                        IconButton(
                            onClick = {
                                coroutineScope.launch {
                                    val prev = (pagerState.currentPage - 1).coerceAtLeast(0)
                                    pagerState.animateScrollToPage(prev)
                                }
                            },
                            enabled = pagerState.currentPage > 0,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = FluentIcons.ChevronLeft24Regular,
                                contentDescription = "Previous Page",
                                tint = if (pagerState.currentPage > 0) colors.primary else colors.textMuted,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        // Material 3 Slider for scrubbing across spreads
                        val maxRange = (spreadCount - 1).coerceAtLeast(1).toFloat()
                        Slider(
                            value = pagerState.currentPage.toFloat().coerceIn(0f, maxRange),
                            valueRange = 0f..maxRange,
                            onValueChange = { target ->
                                coroutineScope.launch {
                                    pagerState.scrollToPage(target.toInt().coerceIn(0, spreadCount - 1))
                                }
                            },
                            colors = SliderDefaults.colors(
                                thumbColor = colors.primary,
                                activeTrackColor = colors.primary,
                                inactiveTrackColor = colors.borderSubtle
                            ),
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 6.dp)
                        )

                        // Next Spread / Page IconButton
                        IconButton(
                            onClick = {
                                coroutineScope.launch {
                                    val next = (pagerState.currentPage + 1).coerceAtMost(spreadCount - 1)
                                    pagerState.animateScrollToPage(next)
                                }
                            },
                            enabled = pagerState.currentPage < spreadCount - 1,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = FluentIcons.ChevronRight24Regular,
                                contentDescription = "Next Page",
                                tint = if (pagerState.currentPage < spreadCount - 1) colors.primary else colors.textMuted,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        // Pill Badge: displays "Page 142–143 / 1150" in dual mode or "Page 142 / 1150" in single/cover mode
                        val pageBadgeText = if (isDualPageActive && currentSpread.rightPageIndex != null) {
                            "Page ${currentSpread.leftPageIndex + 1}–${currentSpread.rightPageIndex + 1} / $pageCount"
                        } else {
                            "Page ${currentSpread.leftPageIndex + 1} / $pageCount"
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = colors.primary.copy(alpha = 0.12f),
                            modifier = Modifier
                                .padding(start = 2.dp)
                                .testTag("reader_scrubber_page_badge")
                        ) {
                            Text(
                                text = pageBadgeText,
                                fontFamily = InterFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.sp,
                                color = colors.primary,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }

                        // Subtle vertical separator before share button
                        Spacer(modifier = Modifier.width(4.dp))
                        Box(
                            modifier = Modifier
                                .height(16.dp)
                                .width(1.dp)
                                .background(colors.borderSubtle)
                        )
                        Spacer(modifier = Modifier.width(2.dp))

                        // High-Definition Share Button at the very end of the scroll bar
                        IconButton(
                            onClick = {
                                shareHighDefinitionPage(currentSpread.leftPageIndex)
                            },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = FluentIcons.Share24Regular,
                                contentDescription = "Share High Definition Page",
                                tint = colors.primary,
                                modifier = Modifier.size(19.dp)
                            )
                        }
                    }
                }
            }

            // Circle to Search visual ripple and lens overlay
            CircleToSearchOverlay(
                centerOffset = circleSearchCoords,
                visible = circleSearchCoords != null,
                onDismiss = { circleSearchCoords = null }
            )
        }
    }
}

/**
 * Individual Page Item with conditional gesture routing:
 * - Only consumes drag/pan when scale > 1.05f
 * - When scale <= 1.05f, touch events pass through unconsumed to HorizontalPager
 * - Double-tap smoothly animates zoom (1f <-> 2.5f) inspecting the tapped page (left, right, or single)
 * - Single-tap checks 20% left / 60% center / 20% right zones across the spread
 * - Long-press activates Google Circle to Search
 */
@Composable
private fun SinglePdfPageItem(
    pageIndex: Int,
    renderer: PdfRenderer?,
    rendererMutex: Mutex,
    pageCache: LruCache<Int, Bitmap>,
    isCurrentPage: Boolean,
    spreadHalf: SpreadHalf = SpreadHalf.FULL,
    eyeRelaxColorFilter: ColorFilter? = null,
    searchMatches: List<SearchMatch> = emptyList(),
    activeSearchMatchIndex: Int = -1,
    onZoomStateChanged: (Boolean) -> Unit,
    onTapZone: (Float) -> Unit,
    onLongPressCircleToSearch: (Offset, Bitmap?) -> Unit = { _, _ -> }
) {
    val colors = LocalMulberryColors.current
    val coroutineScope = rememberCoroutineScope()
    var bitmap by remember(pageIndex) { mutableStateOf<Bitmap?>(pageCache.get(pageIndex)) }
    var isLoading by remember(pageIndex) { mutableStateOf(bitmap == null) }

    val pageMatches = remember(searchMatches, pageIndex) {
        searchMatches.filter { it.pageIndex == pageIndex }
    }

    // Conditional Transform State
    var scale by remember(pageIndex) { mutableFloatStateOf(1f) }
    var offset by remember(pageIndex) { mutableStateOf(Offset.Zero) }

    // Reset zoom when page/spread is no longer active
    LaunchedEffect(isCurrentPage) {
        if (!isCurrentPage && scale != 1f) {
            scale = 1f
            offset = Offset.Zero
            onZoomStateChanged(false)
        }
    }

    val contentAlignment = when (spreadHalf) {
        SpreadHalf.LEFT -> Alignment.CenterEnd
        SpreadHalf.RIGHT -> Alignment.CenterStart
        SpreadHalf.FULL -> Alignment.Center
    }

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = contentAlignment
    ) {
        val density = LocalDensity.current
        val containerWidthPx = with(density) { maxWidth.toPx() }.toInt().coerceAtLeast(1)
        val containerHeightPx = with(density) { maxHeight.toPx() }.toInt().coerceAtLeast(1)

        var renderError by remember(pageIndex) { mutableStateOf<String?>(null) }
        var retryTrigger by remember(pageIndex) { mutableIntStateOf(0) }

        // Asynchronously render page bitmap on Dispatchers.IO
        LaunchedEffect(pageIndex, renderer, retryTrigger) {
            if (renderer == null) return@LaunchedEffect
            val cached = pageCache.get(pageIndex)
            if (cached != null) {
                bitmap = cached
                isLoading = false
                renderError = null
                return@LaunchedEffect
            }

            isLoading = true
            renderError = null
            withContext(Dispatchers.IO) {
                try {
                    val renderedBitmap = rendererMutex.withLock {
                        val page = renderer.openPage(pageIndex)
                        try {
                            // Scale factor to fit container while maintaining crisp rendering (1.5x supersampling)
                            val targetScale = minOf(
                                (containerWidthPx * 1.5f) / page.width.toFloat(),
                                (containerHeightPx * 1.5f) / page.height.toFloat()
                            ).coerceIn(1f, 3.5f)

                            val bmpW = (page.width * targetScale).toInt().coerceAtLeast(1)
                            val bmpH = (page.height * targetScale).toInt().coerceAtLeast(1)

                            val newBitmap = Bitmap.createBitmap(bmpW, bmpH, Bitmap.Config.ARGB_8888)
                            val canvas = Canvas(newBitmap)
                            canvas.drawColor(AndroidColor.WHITE)
                            page.render(newBitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                            newBitmap
                        } finally {
                            page.close()
                        }
                    }

                    pageCache.put(pageIndex, renderedBitmap)
                    withContext(Dispatchers.Main) {
                        bitmap = renderedBitmap
                        isLoading = false
                        renderError = null
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    withContext(Dispatchers.Main) {
                        isLoading = false
                        renderError = e.message ?: "Failed to render page"
                    }
                }
            }
        }

        if (bitmap != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    // 1. Double-tap (2.5x zoom on tapped page) and tap zone detection
                    .pointerInput(pageIndex, spreadHalf) {
                        detectTapGestures(
                            onDoubleTap = { tapOffset ->
                                coroutineScope.launch {
                                    if (scale > 1.05f) {
                                        // Animate back to 1f and Offset.Zero
                                        val startScale = scale
                                        val startOffset = offset
                                        animate(0f, 1f, animationSpec = tween(220)) { value, _ ->
                                            scale = lerp(startScale, 1f, value)
                                            offset = lerp(startOffset, Offset.Zero, value)
                                        }
                                        scale = 1f
                                        offset = Offset.Zero
                                        onZoomStateChanged(false)
                                    } else {
                                        // Animate zoom to 2.5f centered on tapped coordinate of this page
                                        val targetScale = 2.5f
                                        val centerX = size.width / 2f
                                        val centerY = size.height / 2f
                                        val maxX = (size.width * (targetScale - 1f)) / 2f
                                        val maxY = (size.height * (targetScale - 1f)) / 2f
                                        val targetOffset = Offset(
                                            ((centerX - tapOffset.x) * (targetScale - 1f)).coerceIn(-maxX, maxX),
                                            ((centerY - tapOffset.y) * (targetScale - 1f)).coerceIn(-maxY, maxY)
                                        )
                                        val startScale = scale
                                        val startOffset = offset
                                        animate(0f, 1f, animationSpec = tween(220)) { value, _ ->
                                            scale = lerp(startScale, targetScale, value)
                                            offset = lerp(startOffset, targetOffset, value)
                                        }
                                        scale = targetScale
                                        offset = targetOffset
                                        onZoomStateChanged(true)
                                    }
                                }
                            },
                            onTap = { tapOffset ->
                                // Tap zones active when not zoomed in
                                if (scale <= 1.05f) {
                                    val xFraction = tapOffset.x / size.width.toFloat().coerceAtLeast(1f)
                                    onTapZone(xFraction)
                                }
                            },
                            onLongPress = { pressOffset ->
                                onLongPressCircleToSearch(pressOffset, bitmap)
                            }
                        )
                    }
                    // 2. Conditional Transform / Drag Detection
                    .pointerInput(pageIndex) {
                        awaitEachGesture {
                            awaitFirstDown(requireUnconsumed = false)
                            do {
                                val event = awaitPointerEvent()
                                val activePointers = event.changes.filter { it.pressed }

                                if (activePointers.size >= 2) {
                                    // Pinch zoom gesture
                                    val zoomChange = event.calculateZoom()
                                    val panChange = event.calculatePan()

                                    val newScale = (scale * zoomChange).coerceIn(1f, 5f)
                                    scale = newScale
                                    val maxX = (size.width * (scale - 1f)) / 2f
                                    val maxY = (size.height * (scale - 1f)) / 2f
                                    val newX = (offset.x + panChange.x).coerceIn(-maxX, maxX)
                                    val newY = (offset.y + panChange.y).coerceIn(-maxY, maxY)
                                    offset = Offset(newX, newY)

                                    val isZoomed = scale > 1.05f
                                    onZoomStateChanged(isZoomed)

                                    // Consume events during multi-touch transform
                                    event.changes.forEach { it.consume() }
                                } else if (activePointers.size == 1 && scale > 1.05f) {
                                    // Pan gesture on zoomed document
                                    val panChange = event.calculatePan()
                                    val maxX = (size.width * (scale - 1f)) / 2f
                                    val maxY = (size.height * (scale - 1f)) / 2f
                                    val newX = (offset.x + panChange.x).coerceIn(-maxX, maxX)
                                    val newY = (offset.y + panChange.y).coerceIn(-maxY, maxY)
                                    offset = Offset(newX, newY)

                                    // Consume events so HorizontalPager doesn't change pages while panning
                                    event.changes.forEach { it.consume() }
                                } else {
                                    // scale <= 1.05f and single finger:
                                    // DO NOT CONSUME! Allow horizontal swipe to propagate to HorizontalPager smoothly
                                }
                            } while (event.changes.any { it.pressed })

                            // If released below threshold, reset to 1f
                            if (scale <= 1.05f && (scale != 1f || offset != Offset.Zero)) {
                                scale = 1f
                                offset = Offset.Zero
                                onZoomStateChanged(false)
                            }
                        }
                    },
                contentAlignment = contentAlignment
            ) {
                Box(
                    modifier = Modifier
                        .graphicsLayer {
                            scaleX = scale
                            scaleY = scale
                            translationX = offset.x
                            translationY = offset.y
                        }
                ) {
                    Image(
                        bitmap = bitmap!!.asImageBitmap(),
                        contentDescription = "Page ${pageIndex + 1}",
                        colorFilter = eyeRelaxColorFilter
                    )

                    // On-Page Visual Highlighting for search matches
                    if (pageMatches.isNotEmpty()) {
                        androidx.compose.foundation.Canvas(modifier = Modifier.matchParentSize()) {
                            for (match in pageMatches) {
                                val isActive = match.globalIndex == activeSearchMatchIndex
                                val fillColor = if (isActive) {
                                    androidx.compose.ui.graphics.Color(0xFFFFB300).copy(alpha = 0.65f)
                                } else {
                                    androidx.compose.ui.graphics.Color(0xFFFFD54F).copy(alpha = 0.38f)
                                }
                                val strokeColor = if (isActive) {
                                    androidx.compose.ui.graphics.Color(0xFFE65100)
                                } else null

                                for (rect in match.bounds) {
                                    val leftPx = rect.left * size.width
                                    val topPx = rect.top * size.height
                                    val widthPx = ((rect.right - rect.left) * size.width).coerceAtLeast(4f)
                                    val heightPx = ((rect.bottom - rect.top) * size.height).coerceAtLeast(4f)
                                    val cornerRadius = androidx.compose.ui.geometry.CornerRadius(3.dp.toPx(), 3.dp.toPx())

                                    drawRoundRect(
                                        color = fillColor,
                                        topLeft = androidx.compose.ui.geometry.Offset(leftPx, topPx),
                                        size = androidx.compose.ui.geometry.Size(widthPx, heightPx),
                                        cornerRadius = cornerRadius
                                    )

                                    if (strokeColor != null) {
                                        drawRoundRect(
                                            color = strokeColor,
                                            topLeft = androidx.compose.ui.geometry.Offset(leftPx, topPx),
                                            size = androidx.compose.ui.geometry.Size(widthPx, heightPx),
                                            cornerRadius = cornerRadius,
                                            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = isLoading,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    color = colors.primary,
                    strokeWidth = 2.dp,
                    modifier = Modifier.size(32.dp)
                )
            }
        }

        if (renderError != null && bitmap == null && !isLoading) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(24.dp)
                ) {
                    Icon(
                        imageVector = FluentIcons.Dismiss24Regular,
                        contentDescription = null,
                        tint = colors.accentDanger,
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        text = "Page ${pageIndex + 1} could not be rendered",
                        fontFamily = InterFamily,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = colors.textPrimary
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = renderError ?: "Error processing page",
                        fontFamily = InterFamily,
                        fontSize = 12.sp,
                        color = colors.textSecondary
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = { retryTrigger++ },
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = colors.primary)
                    ) {
                        Text("Retry Page", color = colors.onPrimary, fontSize = 12.sp)
                    }
                }
            }
        }
    }
}
