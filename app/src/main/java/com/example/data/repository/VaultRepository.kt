package com.example.data.repository

import android.content.Context
import com.example.data.local.dao.BookDao
import com.example.data.local.entities.BookEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

class VaultRepository(
    private val context: Context,
    private val bookDao: BookDao,
    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .build()
) {

    val vaultDir: File
        get() = File(context.filesDir, "vault").apply { mkdirs() }

    fun getLocalBinFile(bookId: String): File {
        return File(vaultDir, "$bookId.bin")
    }

    /**
     * Downloads an encrypted .bin asset from the provided URL (e.g. GitHub Releases).
     * Writes strictly into context.filesDir/vault/{id}.bin without creating plaintext files.
     */
    suspend fun downloadVaultPayload(
        book: BookEntity,
        onProgress: (bytesRead: Long, totalBytes: Long) -> Unit = { _, _ -> }
    ): Result<File> = withContext(Dispatchers.IO) {
        val targetFile = getLocalBinFile(book.id)
        val tempDownloadFile = File(vaultDir, "${book.id}.downloading")

        try {
            val downloadUrl = book.downloadUrl.ifBlank {
                "https://github.com/doubtsolved/mulberry-vault-prof1/releases/download/v1-books/${book.id}.bin"
            }

            val request = Request.Builder()
                .url(downloadUrl)
                .header("User-Agent", "Mulberry-Android-Client/2.0")
                .build()

            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                return@withContext Result.failure(Exception("Download failed with HTTP ${response.code}"))
            }

            val body = response.body ?: return@withContext Result.failure(Exception("Empty response body"))
            val contentLength = body.contentLength()

            body.byteStream().use { input ->
                FileOutputStream(tempDownloadFile).use { output ->
                    val buffer = ByteArray(8192)
                    var bytesRead: Int
                    var totalRead = 0L

                    while (input.read(buffer).also { bytesRead = it } != -1) {
                        output.write(buffer, 0, bytesRead)
                        totalRead += bytesRead
                        onProgress(totalRead, contentLength)
                    }
                    output.flush()
                }
            }

            // Rename .downloading to .bin
            if (targetFile.exists()) targetFile.delete()
            tempDownloadFile.renameTo(targetFile)

            // Update Room database
            bookDao.updateDownloadState(book.id, true, targetFile.absolutePath)

            Result.success(targetFile)
        } catch (e: Exception) {
            tempDownloadFile.delete()
            Result.failure(e)
        }
    }

    /**
     * Removes the encrypted .bin file from disk and updates Room state.
     */
    suspend fun deleteDownloadedBook(bookId: String): Boolean = withContext(Dispatchers.IO) {
        val file = getLocalBinFile(bookId)
        val deleted = if (file.exists()) file.delete() else true
        bookDao.removeBookFromShelf(bookId)
        deleted
    }

    /**
     * Calculates total bytes consumed by all encrypted .bin vault files.
     */
    fun getTotalVaultBytes(): Long {
        if (!vaultDir.exists()) return 0L
        return vaultDir.listFiles()?.sumOf { it.length() } ?: 0L
    }
}
