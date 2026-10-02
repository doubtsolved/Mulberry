package com.example.ui.reader

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color as AndroidColor
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.LruCache
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.security.MessageDigest

/**
 * High-performance thumbnail manager with 2-tier caching:
 * 1. In-memory LRU cache (retaining up to 60 decoded bitmaps)
 * 2. Persistent disk cache in cacheDir/pdf_thumbnails/{docHash}/page_{index}.jpg
 *
 * Prevents main-thread stuttering and enables instant reopening of large documents.
 */
class PdfThumbnailManager(
    private val context: Context,
    private val bookPath: String
) {
    private val memoryCache = object : LruCache<Int, Bitmap>(60) {}
    private val renderMutex = Mutex()

    private val cacheDir: File by lazy {
        val hash = hashPath(bookPath)
        val dir = File(context.cacheDir, "pdf_thumbnails/$hash")
        if (!dir.exists()) {
            dir.mkdirs()
        }
        dir
    }

    private fun hashPath(path: String): String {
        return try {
            val md = MessageDigest.getInstance("MD5")
            val digest = md.digest(path.toByteArray())
            digest.joinToString("") { "%02x".format(it) }
        } catch (_: Exception) {
            path.hashCode().toString()
        }
    }

    fun getFromMemory(pageIndex: Int): Bitmap? {
        return memoryCache.get(pageIndex)
    }

    suspend fun getThumbnail(pageIndex: Int, targetWidth: Int = 220): Bitmap? = withContext(Dispatchers.IO) {
        // 1. Check in-memory LRU cache
        memoryCache.get(pageIndex)?.let { return@withContext it }

        // 2. Check disk cache
        val diskFile = File(cacheDir, "page_${pageIndex}.jpg")
        if (diskFile.exists() && diskFile.length() > 0) {
            try {
                val bitmap = BitmapFactory.decodeFile(diskFile.absolutePath)
                if (bitmap != null) {
                    memoryCache.put(pageIndex, bitmap)
                    return@withContext bitmap
                }
            } catch (e: Exception) {
                diskFile.delete()
            }
        }

        // 3. Render via PdfRenderer with synchronization
        renderMutex.withLock {
            // Re-check after acquiring lock
            memoryCache.get(pageIndex)?.let { return@withContext it }

            var pfd: ParcelFileDescriptor? = null
            var renderer: PdfRenderer? = null
            var page: PdfRenderer.Page? = null

            try {
                pfd = when {
                    bookPath.startsWith("content://") -> {
                        context.contentResolver.openFileDescriptor(Uri.parse(bookPath), "r")
                    }
                    else -> {
                        val file = File(bookPath)
                        if (file.exists()) ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY) else null
                    }
                }

                if (pfd != null) {
                    renderer = PdfRenderer(pfd)
                    if (pageIndex in 0 until renderer.pageCount) {
                        page = renderer.openPage(pageIndex)
                        val pageWidth = page.width
                        val pageHeight = page.height

                        val width = targetWidth.coerceAtLeast(100)
                        val height = ((width.toFloat() / pageWidth.toFloat()) * pageHeight).toInt().coerceAtLeast(100)

                        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                        val canvas = Canvas(bitmap)
                        canvas.drawColor(AndroidColor.WHITE)

                        page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                        // Save to disk cache
                        try {
                            FileOutputStream(diskFile).use { out ->
                                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
                            }
                        } catch (e: Exception) {
                            e.printStackTrace()
                        }

                        memoryCache.put(pageIndex, bitmap)
                        return@withContext bitmap
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            } finally {
                try { page?.close() } catch (_: Exception) {}
                try { renderer?.close() } catch (_: Exception) {}
                try { pfd?.close() } catch (_: Exception) {}
            }
        }

        null
    }

    fun clearMemory() {
        memoryCache.evictAll()
    }
}

@Composable
fun AsyncPageThumbnail(
    pageIndex: Int,
    thumbnailManager: PdfThumbnailManager,
    modifier: Modifier = Modifier
) {
    var bitmap by remember(pageIndex) {
        mutableStateOf(thumbnailManager.getFromMemory(pageIndex))
    }

    LaunchedEffect(pageIndex) {
        if (bitmap == null) {
            val loaded = thumbnailManager.getThumbnail(pageIndex)
            if (loaded != null) {
                bitmap = loaded
            }
        }
    }

    Box(
        modifier = modifier.background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        val currentBitmap = bitmap
        if (currentBitmap != null) {
            Image(
                bitmap = currentBitmap.asImageBitmap(),
                contentDescription = "Page ${pageIndex + 1} thumbnail",
                contentScale = ContentScale.FillWidth,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color(0xFFF3F3F3)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(
                    strokeWidth = 2.dp,
                    color = Color(0xFF888888),
                    modifier = Modifier.fillMaxSize(0.25f)
                )
            }
        }
    }
}
