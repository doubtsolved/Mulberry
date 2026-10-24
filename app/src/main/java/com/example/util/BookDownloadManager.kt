package com.example.util

import android.content.Context
import android.util.Log
import com.example.data.dao.BookDao
import com.example.data.model.BookEntity
import com.mulberry.app.analytics.MulberryAnalytics
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

sealed class DownloadProgressState {
    object Idle : DownloadProgressState()
    data class Downloading(val bookSlug: String, val progressPercent: Int, val bytesRead: Long, val totalBytes: Long) : DownloadProgressState()
    data class Completed(val bookSlug: String, val filePath: String) : DownloadProgressState()
    data class Error(val bookSlug: String, val message: String) : DownloadProgressState()
}

class BookDownloadManager private constructor(
    private val context: Context,
    private val bookDao: BookDao
) {
    companion object {
        private const val TAG = "BookDownloadManager"

        @Volatile
        private var INSTANCE: BookDownloadManager? = null

        fun getInstance(context: Context, bookDao: BookDao): BookDownloadManager {
            return INSTANCE ?: synchronized(this) {
                val instance = BookDownloadManager(context.applicationContext, bookDao)
                INSTANCE = instance
                instance
            }
        }
    }

    // App-level dedicated scope so downloads survive screen navigation and ViewModel clearances
    private val downloadScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val activeJobs = ConcurrentHashMap<String, Job>()

    private val httpClient = OkHttpClient.Builder()
        .dns(BookContributionManager.appDns)
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()

    private val _downloadState = MutableStateFlow<DownloadProgressState>(DownloadProgressState.Idle)
    val downloadState: StateFlow<DownloadProgressState> = _downloadState.asStateFlow()

    /**
     * Launch book download on the persistent application-scoped downloadScope.
     * Survives Activity recreation and ViewModel disposal.
     */
    fun downloadBook(book: BookEntity): Job {
        val targetSlug = book.slug.ifEmpty { "book_${book.id}" }
        val existing = activeJobs[targetSlug]
        if (existing != null && existing.isActive) {
            return existing
        }

        val job = downloadScope.launch {
            executeDownload(book)
        }
        activeJobs[targetSlug] = job
        job.invokeOnCompletion {
            activeJobs.remove(targetSlug)
        }
        return job
    }

    suspend fun downloadBookSuspend(book: BookEntity): Boolean {
        val job = downloadBook(book)
        job.join()
        return _downloadState.value is DownloadProgressState.Completed
    }

    fun cancelDownload(bookSlug: String) {
        val job = activeJobs[bookSlug]
        if (job != null && job.isActive) {
            job.cancel()
        }
        activeJobs.remove(bookSlug)
        val mulberryDir = MulberryStorageManager.getMulberryDirectory(context)
        val tempBinFile = File(mulberryDir, "$bookSlug.tmp")
        if (tempBinFile.exists()) {
            try { tempBinFile.delete() } catch (_: Exception) {}
        }
        _downloadState.value = DownloadProgressState.Idle
    }

    private suspend fun executeDownload(book: BookEntity): Boolean = withContext(Dispatchers.IO) {
        val targetSlug = book.slug.ifEmpty { "book_${book.id}" }
        val targetUrl = book.downloadUrl.ifEmpty { book.driveFallbackUrl }

        if (targetUrl.isBlank()) {
            _downloadState.value = DownloadProgressState.Error(targetSlug, "No download mirror available")
            return@withContext false
        }

        val mulberryDir = MulberryStorageManager.getMulberryDirectory(context)
        if (!mulberryDir.exists()) mulberryDir.mkdirs()

        val targetBinFile = MulberryStorageManager.getBinFileForBook(context, targetSlug)
        val tempBinFile = File(mulberryDir, "$targetSlug.tmp")

        try {
            _downloadState.value = DownloadProgressState.Downloading(targetSlug, 0, 0, 0)

            val request = Request.Builder()
                .url(targetUrl)
                .header("User-Agent", "Mulberry-Android-Applet")
                .build()

            val response = httpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                _downloadState.value = DownloadProgressState.Error(targetSlug, "Server returned HTTP ${response.code}")
                return@withContext false
            }

            val body = response.body ?: throw IOException("Empty response body")
            val contentLength = body.contentLength()
            val sizeMb = if (contentLength > 0) (contentLength / (1024.0 * 1024.0)) else 0.0
            MulberryAnalytics.logBookDownloadStarted(
                bookId = targetSlug,
                bookTitle = book.title,
                subject = book.subject,
                sizeMb = sizeMb
            )

            body.byteStream().use { input ->
                FileOutputStream(tempBinFile).use { output ->
                    val buffer = ByteArray(32768)
                    var bytesReadTotal = 0L
                    var read: Int
                    var lastReportTime = 0L

                    while (input.read(buffer).also { read = it } != -1) {
                        output.write(buffer, 0, read)
                        bytesReadTotal += read

                        val now = System.currentTimeMillis()
                        if (now - lastReportTime > 200 || bytesReadTotal == contentLength) {
                            lastReportTime = now
                            val progress = if (contentLength > 0) {
                                ((bytesReadTotal * 100) / contentLength).toInt().coerceIn(0, 100)
                            } else {
                                50
                            }
                            _downloadState.value = DownloadProgressState.Downloading(
                                targetSlug,
                                progress,
                                bytesReadTotal,
                                contentLength
                            )
                        }
                    }
                    output.flush()
                }
            }

            // Successfully downloaded, atomically rename with fallback
            if (targetBinFile.exists()) targetBinFile.delete()
            if (!tempBinFile.renameTo(targetBinFile)) {
                tempBinFile.copyTo(targetBinFile, overwrite = true)
                tempBinFile.delete()
            }

            // Record into /sdcard/Mulberry/downloaded_books.json tracking index
            MulberryStorageManager.recordDownloadedBook(
                context = context,
                book = book,
                binFile = targetBinFile
            )

            // Update local Room database
            bookDao.updateDownloadStateBySlug(
                slug = targetSlug,
                isDownloaded = true,
                binFilePath = targetBinFile.absolutePath,
                fileSizeBytes = targetBinFile.length()
            )

            _downloadState.value = DownloadProgressState.Completed(targetSlug, targetBinFile.absolutePath)
            MulberryAnalytics.logBookDownloadCompleted(
                bookId = targetSlug,
                bookTitle = book.title,
                subject = book.subject
            )
            true
        } catch (e: CancellationException) {
            Log.d(TAG, "Download job gracefully cancelled for $targetSlug")
            if (tempBinFile.exists()) {
                try { tempBinFile.delete() } catch (_: Exception) {}
            }
            _downloadState.value = DownloadProgressState.Idle
            false
        } catch (e: Exception) {
            Log.e(TAG, "Download failed for $targetSlug: ${e.message}", e)
            if (tempBinFile.exists()) {
                try { tempBinFile.delete() } catch (_: Exception) {}
            }
            _downloadState.value = DownloadProgressState.Error(targetSlug, e.message ?: "Network error")
            false
        }
    }

    suspend fun deleteDownload(book: BookEntity): Boolean = withContext(Dispatchers.IO) {
        val targetSlug = book.slug.ifEmpty { "book_${book.id}" }
        val targetBinFile = if (book.binFilePath.isNotBlank()) {
            File(book.binFilePath)
        } else {
            MulberryStorageManager.getBinFileForBook(context, targetSlug)
        }

        if (targetBinFile.exists()) {
            targetBinFile.delete()
        }

        // Also remove legacy vault copy if exists
        val legacyVaultFile = File(File(context.filesDir, "vault"), "$targetSlug.bin")
        if (legacyVaultFile.exists()) {
            try { legacyVaultFile.delete() } catch (_: Exception) {}
        }

        // Update tracking index
        MulberryStorageManager.removeDownloadedBook(context, targetSlug)

        bookDao.updateDownloadStateBySlug(
            slug = targetSlug,
            isDownloaded = false,
            binFilePath = "",
            fileSizeBytes = 0L
        )
        true
    }

    fun getTotalVaultStorageBytes(): Long {
        val mulberryDir = MulberryStorageManager.getMulberryDirectory(context)
        if (!mulberryDir.exists() || !mulberryDir.isDirectory) return 0L
        return mulberryDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
    }
}
