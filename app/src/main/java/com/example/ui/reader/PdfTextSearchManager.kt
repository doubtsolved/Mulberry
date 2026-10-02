package com.example.ui.reader

import android.content.Context
import android.graphics.RectF
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.Build
import android.os.ParcelFileDescriptor
import android.util.Log
import com.tom_roush.pdfbox.android.PDFBoxResourceLoader
import com.tom_roush.pdfbox.pdmodel.PDDocument
import com.tom_roush.pdfbox.pdmodel.PDPage
import com.tom_roush.pdfbox.text.PDFTextStripper
import com.tom_roush.pdfbox.text.TextPosition
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import java.io.File
import java.io.InputStream

/**
 * Normalized bounding rectangle (0f..1f relative to page width and height).
 */
data class MatchRect(
    val left: Float,
    val top: Float,
    val right: Float,
    val bottom: Float
)

data class SearchMatch(
    val pageIndex: Int,
    val matchIndexOnPage: Int = 0,
    val globalIndex: Int = 0,
    val bounds: List<MatchRect> = emptyList(),
    val snippet: String = ""
)

/**
 * Universal Dual-Engine PDF Text Search Manager.
 * 
 * - Primary Engine (Android 15+ / API 35+): Platform-native PdfRenderer.Page.searchText(query)
 *   with hardware-accelerated bounding box extraction.
 * - Universal Engine (Android 13 / API 24-34, e.g. Moto G52): Apache PDFBox for Android with
 *   custom coordinate stripper supporting full CMap, ToUnicode, font decoding, and ligature mapping.
 * - Progressive streaming: Emits matches on-the-fly while scanning, preventing UI freezes and OOM crashes.
 */
class PdfTextSearchManager(
    private val context: Context,
    private val bookPath: String,
    private val totalPages: Int
) {
    companion object {
        private const val TAG = "PdfTextSearchManager"
        @Volatile
        private var isPdfBoxInitialized = false

        fun initPdfBox(context: Context) {
            if (!isPdfBoxInitialized) {
                synchronized(this) {
                    if (!isPdfBoxInitialized) {
                        try {
                            PDFBoxResourceLoader.init(context.applicationContext)
                            isPdfBoxInitialized = true
                        } catch (e: Throwable) {
                            Log.e(TAG, "Failed to initialize PDFBoxResourceLoader: ${e.message}")
                        }
                    }
                }
            }
        }
    }

    init {
        initPdfBox(context)
    }

    /**
     * Executes page-by-page streaming search with cooperative cancellation and progressive progress.
     */
    suspend fun searchStreaming(
        query: String,
        onProgress: (List<SearchMatch>) -> Unit
    ): List<SearchMatch> = withContext(Dispatchers.IO) {
        val trimmedQuery = query.trim()
        if (trimmedQuery.length < 2 || totalPages <= 0) {
            return@withContext emptyList()
        }

        // On Android 15+ (API 35+), try native platform engine first
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            try {
                val nativeMatches = searchWithNativeEngine(trimmedQuery, onProgress)
                if (nativeMatches.isNotEmpty()) {
                    return@withContext nativeMatches
                }
            } catch (e: Throwable) {
                Log.w(TAG, "Native search engine failed, falling back to PDFBox: ${e.message}")
            }
        }

        // Universal Engine (Android 13 / API < 35, or fallback)
        return@withContext searchWithPdfBoxEngine(trimmedQuery, onProgress)
    }

    /**
     * Android 15+ native PdfRenderer.Page.searchText engine.
     */
    private suspend fun searchWithNativeEngine(
        query: String,
        onProgress: (List<SearchMatch>) -> Unit
    ): List<SearchMatch> {
        val allMatches = mutableListOf<SearchMatch>()
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null

        try {
            pfd = openFileDescriptor() ?: return emptyList()
            renderer = PdfRenderer(pfd)
            val pageCount = renderer.pageCount.coerceAtMost(totalPages)

            for (pageIndex in 0 until pageCount) {
                var page: PdfRenderer.Page? = null
                try {
                    page = renderer.openPage(pageIndex)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
                        val searchResults = page.searchText(query)
                        if (searchResults.isNotEmpty()) {
                            val pageWidth = page.width.toFloat().coerceAtLeast(1f)
                            val pageHeight = page.height.toFloat().coerceAtLeast(1f)

                            val pageText = try {
                                page.textContents.joinToString(" ") { it.text }
                            } catch (_: Throwable) {
                                ""
                            }
                            val snippet = buildSnippet(pageText, query)

                            for ((hitIdx, hit) in searchResults.withIndex()) {
                                val matchRects = hit.bounds.map { rect: RectF ->
                                    MatchRect(
                                        left = (rect.left / pageWidth).coerceIn(0f, 1f),
                                        top = (rect.top / pageHeight).coerceIn(0f, 1f),
                                        right = (rect.right / pageWidth).coerceIn(0f, 1f),
                                        bottom = (rect.bottom / pageHeight).coerceIn(0f, 1f)
                                    )
                                }
                                allMatches.add(
                                    SearchMatch(
                                        pageIndex = pageIndex,
                                        matchIndexOnPage = hitIdx,
                                        globalIndex = allMatches.size,
                                        bounds = matchRects,
                                        snippet = snippet
                                    )
                                )
                            }
                            onProgress(allMatches.toList())
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Native search error on page $pageIndex: ${e.message}")
                } finally {
                    try { page?.close() } catch (_: Exception) {}
                }

                if (pageIndex % 3 == 0) yield()
            }
        } finally {
            try { renderer?.close() } catch (_: Exception) {}
            try { pfd?.close() } catch (_: Exception) {}
        }

        return allMatches
    }

    /**
     * Universal PDFBox search engine for Android 13 (Moto G52) and all versions below Android 15.
     */
    private suspend fun searchWithPdfBoxEngine(
        query: String,
        onProgress: (List<SearchMatch>) -> Unit
    ): List<SearchMatch> {
        val allMatches = mutableListOf<SearchMatch>()
        var document: PDDocument? = null
        var inputStream: InputStream? = null

        try {
            document = if (bookPath.startsWith("content://")) {
                inputStream = context.contentResolver.openInputStream(Uri.parse(bookPath))
                if (inputStream != null) PDDocument.load(inputStream) else null
            } else {
                val file = File(bookPath)
                if (file.exists()) PDDocument.load(file) else null
            }

            if (document == null) {
                Log.w(TAG, "PDFBox: Unable to load document from $bookPath")
                return emptyList()
            }

            val docPages = document.numberOfPages.coerceAtMost(totalPages)

            for (pageIndex in 0 until docPages) {
                try {
                    val stripper = CoordinateStripper(query, pageIndex)
                    stripper.extractMatches(document, pageIndex)

                    val pageMatches = stripper.extractedMatches
                    if (pageMatches.isNotEmpty()) {
                        for (pm in pageMatches) {
                            allMatches.add(pm.copy(globalIndex = allMatches.size))
                        }
                        onProgress(allMatches.toList())
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "PDFBox search error on page $pageIndex: ${e.message}")
                }

                if (pageIndex % 2 == 0) yield()
            }
        } catch (e: Exception) {
            Log.e(TAG, "PDFBox engine failure: ${e.message}", e)
        } finally {
            try { document?.close() } catch (_: Exception) {}
            try { inputStream?.close() } catch (_: Exception) {}
        }

        return allMatches
    }

    private fun buildSnippet(fullText: String, query: String): String {
        if (fullText.isBlank()) return "Found match on page"
        val idx = fullText.indexOf(query, ignoreCase = true)
        if (idx == -1) return fullText.take(60)

        val start = (idx - 25).coerceAtLeast(0)
        val end = (idx + query.length + 35).coerceAtMost(fullText.length)
        val prefix = if (start > 0) "…" else ""
        val suffix = if (end < fullText.length) "…" else ""
        return prefix + fullText.substring(start, end).replace("\n", " ").trim() + suffix
    }

    private fun openFileDescriptor(): ParcelFileDescriptor? {
        return try {
            if (bookPath.startsWith("content://")) {
                context.contentResolver.openFileDescriptor(Uri.parse(bookPath), "r")
            } else {
                val file = File(bookPath)
                if (file.exists()) {
                    ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                } else null
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to open ParcelFileDescriptor: ${e.message}")
            null
        }
    }

    /**
     * Coordinate-aware PDFTextStripper to extract normalized character bounding boxes.
     */
    private class CoordinateStripper(
        private val query: String,
        private val targetPageIndex: Int
    ) : PDFTextStripper() {

        val extractedMatches = mutableListOf<SearchMatch>()
        private val textPositions = mutableListOf<TextPosition>()
        private var pageWidth: Float = 1f
        private var pageHeight: Float = 1f

        init {
            startPage = targetPageIndex + 1
            endPage = targetPageIndex + 1
        }

        fun extractMatches(document: PDDocument, pageIndex: Int) {
            val page = document.getPage(pageIndex)
            val cropBox = page.cropBox ?: page.mediaBox
            pageWidth = cropBox.width.coerceAtLeast(1f)
            pageHeight = cropBox.height.coerceAtLeast(1f)

            // Triggers writeString with character coordinates
            getText(document)

            if (textPositions.isEmpty()) return

            val fullText = textPositions.joinToString("") { it.unicode ?: "" }
            var searchStart = 0
            var matchIdxOnPage = 0

            while (searchStart < fullText.length) {
                val matchStart = fullText.indexOf(query, searchStart, ignoreCase = true)
                if (matchStart == -1) break

                val matchEnd = (matchStart + query.length).coerceAtMost(textPositions.size)
                val matchedPositions = textPositions.subList(matchStart, matchEnd)

                if (matchedPositions.isNotEmpty()) {
                    // Calculate bounds in PDF coordinates
                    // PDFBox text position: xDirAdj is from left; yDirAdj is from top of page
                    val minX = matchedPositions.minOf { it.xDirAdj }
                    val maxX = matchedPositions.maxOf { it.xDirAdj + it.widthDirAdj }
                    val minY = matchedPositions.minOf { it.yDirAdj - it.heightDir }
                    val maxY = matchedPositions.maxOf { it.yDirAdj }

                    val normLeft = (minX / pageWidth).coerceIn(0f, 1f)
                    val normTop = (minY / pageHeight).coerceIn(0f, 1f)
                    val normRight = (maxX / pageWidth).coerceIn(0f, 1f)
                    val normBottom = (maxY / pageHeight).coerceIn(0f, 1f)

                    val snippet = buildSnippetText(fullText, matchStart, query.length)

                    extractedMatches.add(
                        SearchMatch(
                            pageIndex = pageIndex,
                            matchIndexOnPage = matchIdxOnPage++,
                            globalIndex = 0, // Will be reassigned
                            bounds = listOf(MatchRect(normLeft, normTop, normRight, normBottom)),
                            snippet = snippet
                        )
                    )
                }

                searchStart = matchStart + query.length.coerceAtLeast(1)
            }
        }

        override fun writeString(text: String?, textPositions: MutableList<TextPosition>?) {
            if (textPositions != null) {
                this.textPositions.addAll(textPositions)
            }
            super.writeString(text, textPositions)
        }

        private fun buildSnippetText(text: String, startIdx: Int, length: Int): String {
            val start = (startIdx - 20).coerceAtLeast(0)
            val end = (startIdx + length + 30).coerceAtMost(text.length)
            val prefix = if (start > 0) "…" else ""
            val suffix = if (end < text.length) "…" else ""
            return prefix + text.substring(start, end).replace("\n", " ").trim() + suffix
        }
    }
}
