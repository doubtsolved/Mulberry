package com.example.util

import android.content.Context
import android.graphics.Bitmap
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.Locale

/**
 * Cleans and formats raw PDF filenames into human-readable book titles.
 * Strips ".pdf", removes "copy of", "copy", numbering noise like "(1)",
 * replaces dashes/underscores/dots with clean spaces, and applies title casing.
 */
fun cleanBookTitle(name: String): String {
    var clean = name.trim()
    while (clean.endsWith(".pdf", ignoreCase = true)) {
        clean = clean.dropLast(4).trim()
    }
    // Remove typical duplicate copy markers
    clean = clean.replace(Regex("(?i)\\bcopy\\s+of\\b"), "")
    clean = clean.replace(Regex("(?i)\\bcopy\\b"), "")
    clean = clean.replace(Regex("\\(\\d+\\)"), "")
    clean = clean.replace(Regex("\\[\\d+\\]"), "")

    // Replace _, -, . with spaces
    clean = clean.replace('_', ' ').replace('-', ' ').replace('.', ' ')
    clean = clean.trim().replace(Regex("\\s+"), " ")

    if (clean.isBlank()) return name

    return clean.split(" ").filter { it.isNotBlank() }.joinToString(" ") { word ->
        // Keep acronyms like MBBS, HCV, DNA, USA in uppercase
        if (word.all { it.isUpperCase() } && word.length in 2..5) {
            word
        } else {
            word.lowercase(Locale.ROOT).replaceFirstChar {
                if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString()
            }
        }
    }.ifBlank { name }
}

fun String.toCleanBookTitle(): String = cleanBookTitle(this)

/**
 * Helper to generate and retrieve cached first-page JPEG thumbnails for PDF books.
 */
object ThumbnailManager {
    fun getThumbnailFile(context: Context, bookId: Int, uriString: String): File {
        val dir = File(context.cacheDir, "thumbs").apply { if (!exists()) mkdirs() }
        val filename = if (bookId > 0) "thumb_$bookId.jpg" else "thumb_${uriString.hashCode()}.jpg"
        return File(dir, filename)
    }

    suspend fun getOrGenerateThumbnail(
        context: Context,
        bookId: Int,
        uriString: String
    ): File? = withContext(Dispatchers.IO) {
        val file = getThumbnailFile(context, bookId, uriString)
        if (file.exists() && file.length() > 0) {
            return@withContext file
        }

        try {
            val pfd: android.os.ParcelFileDescriptor? = if (uriString.startsWith("content://")) {
                context.contentResolver.openFileDescriptor(Uri.parse(uriString), "r")
            } else {
                val rawFile = File(uriString)
                if (rawFile.exists()) android.os.ParcelFileDescriptor.open(rawFile, android.os.ParcelFileDescriptor.MODE_READ_ONLY) else null
            }

            pfd?.use { descriptor ->
                PdfRenderer(descriptor).use { renderer ->
                    if (renderer.pageCount > 0) {
                        renderer.openPage(0).use { page ->
                            val scale = (360f / page.width.toFloat()).coerceAtMost(1f)
                            val w = (page.width * scale).toInt().coerceAtLeast(1)
                            val h = (page.height * scale).toInt().coerceAtLeast(1)
                            val bitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
                            val canvas = android.graphics.Canvas(bitmap)
                            canvas.drawColor(android.graphics.Color.WHITE)
                            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                            FileOutputStream(file).use { out ->
                                bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
                            }
                            bitmap.recycle()
                        }
                    }
                }
            }
        } catch (e: Exception) {
            // Ignore preview generation failure
        }

        if (file.exists() && file.length() > 0) file else null
    }
}
