package com.example.ui.screens.viewer

import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.local.entities.BookEntity
import com.example.ui.components.FluentIcons
import com.example.ui.theme.LocalMulberryColors
import com.example.viewmodel.EphemeralMulberryViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

@Composable
fun PdfReaderScreen(
    book: BookEntity,
    tempPdfFile: File,
    initialPage: Int,
    viewModel: EphemeralMulberryViewModel,
    onClose: () -> Unit
) {
    val colors = LocalMulberryColors.current
    val coroutineScope = rememberCoroutineScope()

    var pdfRenderer by remember { mutableStateOf<PdfRenderer?>(null) }
    var fileDescriptor by remember { mutableStateOf<ParcelFileDescriptor?>(null) }
    var totalPages by remember { mutableIntStateOf(book.totalPages.coerceAtLeast(1)) }
    var currentPageIndex by remember { mutableIntStateOf((initialPage - 1).coerceAtLeast(0)) }
    var renderedBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var isLoadingPage by remember { mutableStateOf(true) }

    // Transform states for zoom & pan
    var scale by remember { mutableFloatStateOf(1f) }
    var offset by remember { mutableStateOf(Offset.Zero) }

    // Snip mode state
    var isSnipModeActive by remember { mutableStateOf(false) }

    // Back handler closes reader
    BackHandler {
        onClose()
    }

    // Lifecycle cleanup: guarantees ephemeral cache purge on dispose
    DisposableEffect(tempPdfFile) {
        onDispose {
            viewModel.updateReadingProgress(book.id, currentPageIndex + 1, totalPages)
            renderedBitmap?.recycle()
            try {
                pdfRenderer?.close()
                fileDescriptor?.close()
            } catch (_: Exception) {}
            viewModel.closeReader()
        }
    }

    // Initialize PdfRenderer from the decrypted temp PDF file
    LaunchedEffect(tempPdfFile) {
        withContext(Dispatchers.IO) {
            try {
                val pfd = ParcelFileDescriptor.open(tempPdfFile, ParcelFileDescriptor.MODE_READ_ONLY)
                val renderer = PdfRenderer(pfd)
                fileDescriptor = pfd
                pdfRenderer = renderer
                totalPages = renderer.pageCount.coerceAtLeast(1)
            } catch (_: Exception) {}
        }
    }

    // Render active page bitmap whenever currentPageIndex changes
    LaunchedEffect(currentPageIndex, pdfRenderer) {
        val renderer = pdfRenderer ?: return@LaunchedEffect
        isLoadingPage = true
        withContext(Dispatchers.IO) {
            try {
                val page = renderer.openPage(currentPageIndex.coerceIn(0, totalPages - 1))
                // High-resolution bitmap (approx 1.5x - 2x for sharp medical reading)
                val targetWidth = (page.width * 1.5f).toInt().coerceAtLeast(600)
                val targetHeight = (page.height * 1.5f).toInt().coerceAtLeast(800)
                val bitmap = Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
                page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
                page.close()

                withContext(Dispatchers.Main) {
                    renderedBitmap?.recycle()
                    renderedBitmap = bitmap
                    isLoadingPage = false
                    scale = 1f
                    offset = Offset.Zero
                    viewModel.updateReadingProgress(book.id, currentPageIndex + 1, totalPages)
                }
            } catch (_: Exception) {
                isLoadingPage = false
            }
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.background)
    ) {
        // Main PDF Canvas View
        Box(
            modifier = Modifier
                .fillMaxSize()
                .pointerInput(isSnipModeActive) {
                    if (!isSnipModeActive) {
                        detectTransformGestures { _, pan, zoom, _ ->
                            scale = (scale * zoom).coerceIn(1f, 4f)
                            if (scale > 1f) {
                                offset += pan
                            } else {
                                offset = Offset.Zero
                            }
                        }
                    }
                },
            contentAlignment = Alignment.Center
        ) {
            renderedBitmap?.let { bmp ->
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = "PDF Page ${currentPageIndex + 1}",
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer(
                            scaleX = scale,
                            scaleY = scale,
                            translationX = offset.x,
                            translationY = offset.y
                        )
                )
            }

            if (isLoadingPage) {
                CircularProgressIndicator(
                    color = colors.primary,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(44.dp)
                )
            }
        }

        // Snip Bounding-Box Overlay (when Snip mode is engaged)
        if (isSnipModeActive) {
            SnipOverlayView(
                pageBitmap = renderedBitmap,
                pageNumber = currentPageIndex + 1,
                onSnipConfirmed = { croppedBmp, title, tags ->
                    viewModel.saveSnip(
                        bookId = book.id,
                        bookTitle = book.title,
                        subject = book.subject,
                        pageNumber = currentPageIndex + 1,
                        bitmap = croppedBmp,
                        title = title,
                        tags = tags
                    )
                    isSnipModeActive = false
                },
                onCancel = { isSnipModeActive = false }
            )
        }

        // Top Chrome Bar (only visible when not snipping)
        if (!isSnipModeActive) {
            Surface(
                color = colors.surface.copy(alpha = 0.94f),
                border = androidx.compose.foundation.BorderStroke(0.5.dp, colors.borderSubtle),
                modifier = Modifier
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 14.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onClose,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = FluentIcons.ArrowLeft24Regular,
                            contentDescription = "Back",
                            tint = colors.textPrimary,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = book.title,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${book.subject} • Page ${currentPageIndex + 1} of $totalPages",
                            fontSize = 11.sp,
                            color = colors.textSecondary
                        )
                    }

                    // Snip CTA in top bar
                    IconButton(
                        onClick = { isSnipModeActive = true },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(colors.surfaceTint.copy(alpha = 0.6f))
                    ) {
                        Icon(
                            imageVector = FluentIcons.Scissors24Regular,
                            contentDescription = "Snip Tool",
                            tint = colors.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        // Bottom Page Scrubber Controls (only visible when not snipping)
        if (!isSnipModeActive) {
            Surface(
                shape = RoundedCornerShape(24.dp),
                color = colors.surface.copy(alpha = 0.94f),
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.borderSubtle),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding()
                    .padding(horizontal = 20.dp, vertical = 14.dp)
                    .fillMaxWidth(0.92f)
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Prev Page
                    IconButton(
                        onClick = {
                            if (currentPageIndex > 0) currentPageIndex--
                        },
                        enabled = currentPageIndex > 0,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = FluentIcons.ChevronLeft24Regular,
                            contentDescription = "Previous Page",
                            tint = if (currentPageIndex > 0) colors.textPrimary else colors.textMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Slider
                    Slider(
                        value = (currentPageIndex + 1).toFloat(),
                        onValueChange = { targetVal ->
                            currentPageIndex = (targetVal.toInt() - 1).coerceIn(0, totalPages - 1)
                        },
                        valueRange = 1f..totalPages.toFloat(),
                        colors = SliderDefaults.colors(
                            thumbColor = colors.primary,
                            activeTrackColor = colors.primary,
                            inactiveTrackColor = colors.borderSubtle
                        ),
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 8.dp)
                    )

                    // Next Page
                    IconButton(
                        onClick = {
                            if (currentPageIndex < totalPages - 1) currentPageIndex++
                        },
                        enabled = currentPageIndex < totalPages - 1,
                        modifier = Modifier.size(34.dp)
                    ) {
                        Icon(
                            imageVector = FluentIcons.ChevronRight24Regular,
                            contentDescription = "Next Page",
                            tint = if (currentPageIndex < totalPages - 1) colors.textPrimary else colors.textMuted,
                            modifier = Modifier.size(18.dp)
                        )
                    }

                    // Page Badge
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = colors.surfaceTint,
                        modifier = Modifier.padding(start = 6.dp)
                    ) {
                        Text(
                            text = "${currentPageIndex + 1} / $totalPages",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = colors.primary,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }
        }
    }
}
