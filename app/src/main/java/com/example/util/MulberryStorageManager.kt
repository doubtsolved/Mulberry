package com.example.util

import android.content.Context
import android.os.Environment
import android.util.Log
import com.example.data.dao.BookDao
import com.example.data.model.BookEntity
import com.google.gson.GsonBuilder
import com.google.gson.annotations.SerializedName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileReader
import java.io.FileWriter

/**
 * Metadata record stored in /sdcard/Mulberry/downloaded_books.json
 * Contains complete catalog and file information to track offline textbooks.
 */
data class DownloadedBookMetadata(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String,
    @SerializedName("author") val author: String = "",
    @SerializedName("edition") val edition: String = "",
    @SerializedName("prof") val prof: String = "",
    @SerializedName("year") val year: String = "",
    @SerializedName("subject") val subject: String = "",
    @SerializedName("type") val type: String = "",
    @SerializedName("tags") val tags: List<String> = emptyList(),
    @SerializedName("totalPages") val totalPages: Int = 0,
    @SerializedName("sizeMb") val sizeMb: Double = 0.0,
    @SerializedName("coverUrl") val coverUrl: String = "",
    @SerializedName("fileName") val fileName: String,
    @SerializedName("localPath") val localPath: String,
    @SerializedName("fileSizeBytes") val fileSizeBytes: Long = 0L,
    @SerializedName("downloadedAt") val downloadedAt: Long = System.currentTimeMillis(),
    @SerializedName("format") val format: String = "aes-256-gcm",
    @SerializedName("isEncrypted") val isEncrypted: Boolean = true,
    @SerializedName("uploader") val uploader: String = ""
)

/**
 * Root JSON structure for /sdcard/Mulberry/downloaded_books.json
 */
data class DownloadedBooksIndex(
    @SerializedName("version") val version: Int = 1,
    @SerializedName("updatedAt") val updatedAt: Long = System.currentTimeMillis(),
    @SerializedName("storageDirectory") val storageDirectory: String = "/sdcard/Mulberry",
    @SerializedName("totalBooks") val totalBooks: Int = 0,
    @SerializedName("books") val books: List<DownloadedBookMetadata> = emptyList()
)

/**
 * Manages physical offline storage for Project Mulberry:
 * - Direct storage directory: /sdcard/Mulberry/
 * - Catalog-indexed tracking file: /sdcard/Mulberry/downloaded_books.json
 */
object MulberryStorageManager {
    private const val TAG = "MulberryStorageManager"
    const val TRACKING_FILE_NAME = "downloaded_books.json"

    private val gson by lazy {
        GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create()
    }

    /**
     * Resolves the primary /sdcard/Mulberry directory, creating it if needed.
     */
    fun getMulberryDirectory(context: Context): File {
        // 1. Standard External Storage Directory (/sdcard/Mulberry or /storage/emulated/0/Mulberry)
        val extStorage = Environment.getExternalStorageDirectory()
        val standardDir = File(extStorage, "Mulberry")
        if (standardDir.exists() || standardDir.mkdirs()) {
            return standardDir
        }

        // 2. Direct absolute path fallback
        val directSdcard = File("/sdcard/Mulberry")
        if (directSdcard.exists() || directSdcard.mkdirs()) {
            return directSdcard
        }

        // 3. App-specific external files dir fallback (always guaranteed writable)
        val appExtDir = context.getExternalFilesDir(null)
        val appMulberry = File(appExtDir ?: context.filesDir, "Mulberry")
        if (!appMulberry.exists()) {
            appMulberry.mkdirs()
        }
        return appMulberry
    }

    /**
     * Returns the tracking JSON file located at /sdcard/Mulberry/downloaded_books.json
     */
    fun getTrackingJsonFile(context: Context): File {
        val dir = getMulberryDirectory(context)
        return File(dir, TRACKING_FILE_NAME)
    }

    /**
     * Returns the target .bin file path for a book inside /sdcard/Mulberry/
     */
    fun getBinFileForBook(context: Context, slug: String): File {
        val dir = getMulberryDirectory(context)
        return File(dir, "$slug.bin")
    }

    /**
     * Reads and parses /sdcard/Mulberry/downloaded_books.json.
     */
    fun loadTrackingIndex(context: Context): DownloadedBooksIndex {
        val jsonFile = getTrackingJsonFile(context)
        if (!jsonFile.exists() || jsonFile.length() == 0L) {
            return DownloadedBooksIndex(
                storageDirectory = getMulberryDirectory(context).absolutePath,
                totalBooks = 0,
                books = emptyList()
            )
        }

        return try {
            FileReader(jsonFile).use { reader ->
                val index = gson.fromJson(reader, DownloadedBooksIndex::class.java)
                index ?: DownloadedBooksIndex(
                    storageDirectory = getMulberryDirectory(context).absolutePath,
                    totalBooks = 0,
                    books = emptyList()
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse $TRACKING_FILE_NAME: ${e.message}", e)
            DownloadedBooksIndex(
                storageDirectory = getMulberryDirectory(context).absolutePath,
                totalBooks = 0,
                books = emptyList()
            )
        }
    }

    /**
     * Atomically writes updated index to /sdcard/Mulberry/downloaded_books.json.
     */
    fun saveTrackingIndex(context: Context, index: DownloadedBooksIndex) {
        val dir = getMulberryDirectory(context)
        if (!dir.exists()) dir.mkdirs()

        val jsonFile = File(dir, TRACKING_FILE_NAME)
        val tempFile = File(dir, "$TRACKING_FILE_NAME.tmp")

        try {
            FileWriter(tempFile).use { writer ->
                gson.toJson(index, writer)
                writer.flush()
            }
            if (jsonFile.exists()) jsonFile.delete()
            if (!tempFile.renameTo(jsonFile)) {
                tempFile.copyTo(jsonFile, overwrite = true)
                tempFile.delete()
            }
            Log.d(TAG, "Saved ${index.books.size} books to ${jsonFile.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to write $TRACKING_FILE_NAME: ${e.message}", e)
            if (tempFile.exists()) tempFile.delete()
        }
    }

    /**
     * Records or updates a downloaded book in the local tracking JSON.
     */
    fun recordDownloadedBook(
        context: Context,
        book: BookEntity,
        binFile: File
    ) {
        val currentIndex = loadTrackingIndex(context)
        val currentBooks = currentIndex.books.filterNot { it.id == book.slug || it.id == "book_${book.id}" }.toMutableList()

        val tagsList = if (book.tagsCsv.isNotBlank()) {
            book.tagsCsv.split(",").map { it.trim() }.filter { it.isNotEmpty() }
        } else emptyList()

        val record = DownloadedBookMetadata(
            id = book.slug.ifEmpty { "book_${book.id}" },
            title = book.title,
            author = book.author,
            edition = book.edition,
            prof = book.prof,
            year = book.year,
            subject = book.subject,
            type = book.type,
            tags = tagsList,
            totalPages = book.pageCount,
            sizeMb = if (book.sizeMb > 0.0) book.sizeMb else (binFile.length() / (1024.0 * 1024.0)),
            coverUrl = book.coverUrl,
            fileName = binFile.name,
            localPath = binFile.absolutePath,
            fileSizeBytes = binFile.length(),
            downloadedAt = System.currentTimeMillis(),
            format = "aes-256-gcm",
            isEncrypted = true,
            uploader = book.uploader
        )

        currentBooks.add(record)
        // Sort by title for neat organization
        currentBooks.sortBy { it.title }

        val newIndex = currentIndex.copy(
            updatedAt = System.currentTimeMillis(),
            storageDirectory = getMulberryDirectory(context).absolutePath,
            totalBooks = currentBooks.size,
            books = currentBooks
        )
        saveTrackingIndex(context, newIndex)
    }

    /**
     * Removes a downloaded book from the local tracking JSON.
     */
    fun removeDownloadedBook(context: Context, slugOrId: String) {
        val currentIndex = loadTrackingIndex(context)
        val filtered = currentIndex.books.filterNot { it.id == slugOrId || it.fileName.contains(slugOrId) }

        val newIndex = currentIndex.copy(
            updatedAt = System.currentTimeMillis(),
            totalBooks = filtered.size,
            books = filtered
        )
        saveTrackingIndex(context, newIndex)
    }

    /**
     * Scans /sdcard/Mulberry/ and synchronizes database state with physical .bin files and JSON index.
     */
    suspend fun syncDiskWithDatabase(context: Context, bookDao: BookDao) = withContext(Dispatchers.IO) {
        try {
            val dir = getMulberryDirectory(context)
            val index = loadTrackingIndex(context)
            val allBooks = bookDao.getAllBooksSync()
            val bookMap = allBooks.associateBy { it.slug.ifEmpty { "book_${it.id}" } }

            val foundBinSlugs = mutableSetOf<String>()

            // 1. Check all index entries
            for (meta in index.books) {
                val binFile = File(meta.localPath)
                if (binFile.exists() && binFile.length() > 12) {
                    foundBinSlugs.add(meta.id)
                    val book = bookMap[meta.id]
                    if (book != null && (!book.isDownloaded || book.binFilePath != binFile.absolutePath)) {
                        bookDao.updateDownloadStateBySlug(
                            slug = meta.id,
                            isDownloaded = true,
                            binFilePath = binFile.absolutePath,
                            fileSizeBytes = binFile.length()
                        )
                    }
                }
            }

            // 2. Scan directory directly for any .bin files not yet in index
            val binFiles = dir.listFiles { f -> f.isFile && f.name.endsWith(".bin") } ?: emptyArray()
            for (bin in binFiles) {
                val slug = bin.nameWithoutExtension
                foundBinSlugs.add(slug)
                val book = bookMap[slug]
                if (book != null && (!book.isDownloaded || book.binFilePath != bin.absolutePath)) {
                    bookDao.updateDownloadStateBySlug(
                        slug = slug,
                        isDownloaded = true,
                        binFilePath = bin.absolutePath,
                        fileSizeBytes = bin.length()
                    )
                    // Also record to index
                    recordDownloadedBook(context, book, bin)
                }
            }

            // 3. Mark removed downloads in DB if .bin file no longer exists
            for (book in allBooks) {
                val slug = book.slug.ifEmpty { "book_${book.id}" }
                if (book.isDownloaded && !foundBinSlugs.contains(slug)) {
                    val file = File(book.binFilePath)
                    if (!file.exists()) {
                        bookDao.updateDownloadStateBySlug(
                            slug = slug,
                            isDownloaded = false,
                            binFilePath = "",
                            fileSizeBytes = 0L
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error synchronizing disk with database: ${e.message}", e)
        }
    }
}
