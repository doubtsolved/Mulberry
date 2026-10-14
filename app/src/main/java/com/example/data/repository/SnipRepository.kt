package com.example.data.repository

import android.content.Context
import android.graphics.Bitmap
import com.example.data.local.dao.SnipDao
import com.example.data.local.entities.SnipEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class SnipRepository(
    private val context: Context,
    private val snipDao: SnipDao
) {

    val snipsDir: File
        get() = File(context.filesDir, "snips").apply { mkdirs() }

    fun getAllSnips(): Flow<List<SnipEntity>> = snipDao.getAllSnips()

    fun getSnipsBySubject(subject: String): Flow<List<SnipEntity>> =
        snipDao.getSnipsBySubject(subject)

    suspend fun saveSnip(
        bookId: String,
        bookTitle: String,
        subject: String,
        pageNumber: Int,
        croppedBitmap: Bitmap,
        title: String,
        tagsCsv: String
    ): Result<Long> = withContext(Dispatchers.IO) {
        try {
            val fileName = "snip_${System.currentTimeMillis()}.webp"
            val file = File(snipsDir, fileName)

            FileOutputStream(file).use { fos ->
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.R) {
                    croppedBitmap.compress(Bitmap.CompressFormat.WEBP_LOSSY, 90, fos)
                } else {
                    @Suppress("DEPRECATION")
                    croppedBitmap.compress(Bitmap.CompressFormat.WEBP, 90, fos)
                }
                fos.flush()
            }

            val entity = SnipEntity(
                bookId = bookId,
                bookTitle = bookTitle,
                subject = subject,
                pageNumber = pageNumber,
                imageLocalPath = file.absolutePath,
                title = title.ifBlank { "Page $pageNumber Diagram" },
                tagsCsv = tagsCsv
            )

            val id = snipDao.insertSnip(entity)
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteSnip(snip: SnipEntity): Boolean = withContext(Dispatchers.IO) {
        val file = File(snip.imageLocalPath)
        if (file.exists()) file.delete()
        snipDao.deleteSnip(snip.id)
        true
    }

    fun getTotalSnipsBytes(): Long {
        if (!snipsDir.exists()) return 0L
        return snipsDir.listFiles()?.sumOf { it.length() } ?: 0L
    }
}
