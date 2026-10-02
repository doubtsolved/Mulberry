package com.example.ui.reader

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.LruCache
import android.content.Intent
import android.widget.Toast
import androidx.core.content.FileProvider
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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
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

/**
 * Single-Page Mode reader powered by Android's native PdfRenderer and Jetpack Compose HorizontalPager.
 * Features:
 * - Conditional gesture routing (swipes smoothly at scale <= 1.05f; pans/zooms when scale > 1.05f)
 * - Animated double-tap zoom centered on coordinate
 * - One-handed tap zones: left 20% previous page, right 20% next page, middle 60% overlay toggle
 * - Floating Page Scrubber Bar with Slider, Prev/Next buttons, and Page Badge
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
    onPageChanged: (Int) -> Unit = {},
    themeMode: ThemeMode = ThemeMode.LIGHT,
    onTap: (() -> Unit)? = null,
    onToggleControls: ((Boolean) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val colors = LocalMulberryColors.current
    val coroutineScope = rememberCoroutineScope()

    var pdfRenderer by remember { mutableStateOf<PdfRenderer?>(null) }
    var pageCount by remember { mutableIntStateOf(totalPages.coerceAtLeast(1)) }
    var isCurrentPageZoomed by remember { mutableStateOf(false) }
    var showControls by remember { mutableStateOf(true) }

    val hapticFeedback = LocalHapticFeedback.current
    var circleSearchCoords by remember { mutableStateOf<Offset?>(null) }

    // Synchronize access to PdfRenderer (Android allows only one open page at a time)
    val rendererMutex = remember { Mutex() }

    // Memory cache for rendered page bitmaps (retains up to 8 pages)
    val pageCache = remember {
        object : LruCache<Int, Bitmap>(8) {}
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

    // Open native PdfRenderer from File or Content Uri
    DisposableEffect(pdfFile, pdfUri) {
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        try {
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
            }
        } catch (e: Exception) {
            e.printStackTrace()
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

    val safeInitialPage = initialPage.coerceIn(0, (pageCount - 1).coerceAtLeast(0))
    val pagerState = rememberPagerState(initialPage = safeInitialPage) { pageCount }

    // Report page changes to caller
    LaunchedEffect(pagerState.currentPage) {
        onPageChanged(pagerState.currentPage)
        // Reset zoom state on page turn
        isCurrentPageZoomed = false
    }

    // Handle external jump requests (TOC, Search, Thumbnail Grid)
    LaunchedEffect(targetPageJump) {
        if (targetPageJump in 0 until pageCount && targetPageJump != pagerState.currentPage) {
            coroutineScope.launch {
                if (kotlin.math.abs(targetPageJump - pagerState.currentPage) > 3) {
                    pagerState.scrollToPage(targetPageJump)
                } else {
                    pagerState.animateScrollToPage(targetPageJump)
                }
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        if (pdfRenderer == null) {
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
            // Horizontal Pager: user scrolling enabled when not zoomed in
            HorizontalPager(
                state = pagerState,
                userScrollEnabled = !isCurrentPageZoomed,
                beyondViewportPageCount = 1,
                modifier = Modifier.fillMaxSize()
            ) { pageIndex ->
                SinglePdfPageItem(
                    pageIndex = pageIndex,
                    renderer = pdfRenderer,
                    rendererMutex = rendererMutex,
                    pageCache = pageCache,
                    isCurrentPage = pageIndex == pagerState.currentPage,
                    searchMatches = searchMatches,
                    activeSearchMatchIndex = activeSearchMatchIndex,
                    onZoomStateChanged = { zoomed ->
                        if (pageIndex == pagerState.currentPage) {
                            isCurrentPageZoomed = zoomed
                        }
                    },
                    onTapZone = { xFraction ->
                        when {
                            // Left 20% of screen: Navigate to Previous Page
                            xFraction < 0.20f -> {
                                coroutineScope.launch {
                                    val prev = (pagerState.currentPage - 1).coerceAtLeast(0)
                                    pagerState.animateScrollToPage(prev)
                                }
                            }
                            // Right 20% of screen: Navigate to Next Page
                            xFraction > 0.80f -> {
                                coroutineScope.launch {
                                    val next = (pagerState.currentPage + 1).coerceAtMost(pageCount - 1)
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
                    },
                    onLongPressCircleToSearch = { pressOffset, pageBitmap ->
                        triggerCircleToSearch(pressOffset, pageBitmap, pageIndex)
                    }
                )
            }

            // Floating Page Scrubber Bar (Single-Page Mode) - sleek spring slide and fade animation
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
                        // Previous Page IconButton
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

                        // Material 3 Slider for scrubbing
                        val maxRange = (pageCount - 1).coerceAtLeast(1).toFloat()
                        Slider(
                            value = pagerState.currentPage.toFloat().coerceIn(0f, maxRange),
                            valueRange = 0f..maxRange,
                            onValueChange = { target ->
                                coroutineScope.launch {
                                    pagerState.scrollToPage(target.toInt().coerceIn(0, pageCount - 1))
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

                        // Next Page IconButton
                        IconButton(
                            onClick = {
                                coroutineScope.launch {
                                    val next = (pagerState.currentPage + 1).coerceAtMost(pageCount - 1)
                                    pagerState.animateScrollToPage(next)
                                }
                            },
                            enabled = pagerState.currentPage < pageCount - 1,
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = FluentIcons.ChevronRight24Regular,
                                contentDescription = "Next Page",
                                tint = if (pagerState.currentPage < pageCount - 1) colors.primary else colors.textMuted,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(4.dp))

                        // Pill Badge: "${pagerState.currentPage + 1} / $totalPages"
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = colors.primary.copy(alpha = 0.12f),
                            modifier = Modifier.padding(start = 2.dp)
                        ) {
                            Text(
                                text = "${pagerState.currentPage + 1} / $pageCount",
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
                                shareHighDefinitionPage(pagerState.currentPage)
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
 * - Double-tap smoothly animates zoom (1f <-> 2.5f)
 * - Single-tap checks 20% left / 60% center / 20% right zones
 * - Long-press activates Google Circle to Search
 */
@Composable
private fun SinglePdfPageItem(
    pageIndex: Int,
    renderer: PdfRenderer?,
    rendererMutex: Mutex,
    pageCache: LruCache<Int, Bitmap>,
    isCurrentPage: Boolean,
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
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    // Reset zoom when page is not the active page
    LaunchedEffect(isCurrentPage) {
        if (!isCurrentPage && scale != 1f) {
            scale = 1f
            offset = Offset.Zero
            onZoomStateChanged(false)
        }
    }

    BoxWithConstraints(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        val density = LocalDensity.current
        val containerWidthPx = with(density) { maxWidth.toPx() }.toInt().coerceAtLeast(1)
        val containerHeightPx = with(density) { maxHeight.toPx() }.toInt().coerceAtLeast(1)

        // Asynchronously render page bitmap on Dispatchers.IO
        LaunchedEffect(pageIndex, renderer) {
            if (renderer == null) return@LaunchedEffect
            val cached = pageCache.get(pageIndex)
            if (cached != null) {
                bitmap = cached
                isLoading = false
                return@LaunchedEffect
            }

            isLoading = true
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
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                    withContext(Dispatchers.Main) {
                        isLoading = false
                    }
                }
            }
        }

        if (bitmap != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    // 1. Double-tap and tap zone detection
                    .pointerInput(pageIndex) {
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
                                        // Animate zoom to 2.5f centered on tapped coordinate
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
                                    val xFraction = tapOffset.x / size.width.toFloat()
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
                contentAlignment = Alignment.Center
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
                        contentDescription = "Page ${pageIndex + 1}"
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
    }
}
