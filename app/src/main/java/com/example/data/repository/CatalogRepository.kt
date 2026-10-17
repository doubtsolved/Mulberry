package com.example.data.repository

import android.content.Context
import com.example.data.local.dao.BookDao
import com.example.data.local.entities.BookEntity
import com.example.data.remote.dto.CatalogResponseDto
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.io.File
import java.util.concurrent.TimeUnit

class CatalogRepository(
    private val context: Context,
    private val bookDao: BookDao,
    private val gson: Gson = Gson(),
    private val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .build()
) {

    companion object {
        const val PRIMARY_CDN_URL = "https://cdn.jsdelivr.net/gh/doubtsolved/mulberry-catalog@main/catalog.json"
        const val FALLBACK_RAW_URL = "https://raw.githubusercontent.com/doubtsolved/mulberry-catalog/main/catalog.json"
    }

    /**
     * Fetches catalog from primary jsDelivr edge CDN, falling back to raw GitHub on failure.
     * Upserts into Room while preserving local device fields (isDownloaded, localBinPath, progress).
     */
    suspend fun syncCatalog(): Result<Int> = withContext(Dispatchers.IO) {
        val jsonString = fetchCatalogJson()
        if (jsonString.isNullOrBlank()) {
            return@withContext Result.failure(Exception("Could not retrieve catalog from CDN or origin fallback."))
        }

        try {
            val catalogResponse = gson.fromJson(jsonString, CatalogResponseDto::class.java)
            if (catalogResponse == null || catalogResponse.books.isEmpty()) {
                return@withContext Result.failure(Exception("Catalog response was empty or malformed."))
            }

            // Map DTOs to Room Entities preserving existing local shelf state
            val entities = catalogResponse.books.map { dto ->
                val primaryMirror = dto.download.mirrors.firstOrNull { it.provider == "github" }?.url
                    ?: dto.download.mirrors.firstOrNull()?.url
                    ?: ""
                val driveMirror = dto.download.mirrors.firstOrNull { it.provider == "drive" }?.url ?: ""
                val telegramLink = dto.download.telegram?.deepLink ?: ""

                // Check if file is already downloaded in context.filesDir/vault/{id}.bin
                val expectedBinFile = File(File(context.filesDir, "vault"), "${dto.id}.bin")
                val isDownloadedOnDisk = expectedBinFile.exists() && expectedBinFile.length() > 12

                // Check if existing record in Room has reading progress
                val existing = bookDao.getBookById(dto.id)

                BookEntity(
                    id = dto.id,
                    title = dto.title,
                    author = dto.author,
                    edition = dto.edition,
                    prof = dto.prof,
                    subject = dto.subject,
                    type = dto.type,
                    tagsCsv = dto.tags.joinToString(", "),
                    totalPages = dto.totalPages.coerceAtLeast(1),
                    sizeMb = dto.sizeMb,
                    coverUrl = dto.coverUrl.ifBlank {
                        "https://cdn.jsdelivr.net/gh/doubtsolved/mulberry-catalog@main/assets/covers/${dto.id}.webp"
                    },
                    downloadUrl = primaryMirror,
                    driveFallbackUrl = driveMirror,
                    telegramDeepLink = telegramLink,
                    uploader = dto.uploader,
                    isDownloaded = existing?.isDownloaded == true || isDownloadedOnDisk,
                    localBinPath = if (isDownloadedOnDisk) expectedBinFile.absolutePath else existing?.localBinPath,
                    lastReadPage = existing?.lastReadPage ?: 1,
                    readingProgressPercent = existing?.readingProgressPercent ?: 0,
                    shelfFolder = existing?.shelfFolder ?: "${dto.prof} Core",
                    lastAccessedTimestamp = existing?.lastAccessedTimestamp ?: 0L
                )
            }

            bookDao.upsertBooks(entities)
            Result.success(entities.size)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun fetchCatalogJson(): String? {
        // Try Primary CDN first
        try {
            val request = Request.Builder()
                .url("$PRIMARY_CDN_URL?t=${System.currentTimeMillis()}")
                .header("User-Agent", "Mulberry-Android-Client/2.0")
                .header("Cache-Control", "no-cache, no-store")
                .build()
            val response = okHttpClient.newCall(request).execute()
            if (response.isSuccessful) {
                val body = response.body?.string()
                if (!body.isNullOrBlank()) return body
            }
        } catch (_: Exception) {}

        // Fallback to Raw GitHub
        try {
            val fallbackRequest = Request.Builder()
                .url("$FALLBACK_RAW_URL?t=${System.currentTimeMillis()}")
                .header("User-Agent", "Mulberry-Android-Client/2.0")
                .header("Cache-Control", "no-cache, no-store")
                .build()
            val fallbackResponse = okHttpClient.newCall(fallbackRequest).execute()
            if (fallbackResponse.isSuccessful) {
                val body = fallbackResponse.body?.string()
                if (!body.isNullOrBlank()) return body
            }
        } catch (_: Exception) {}

        return null
    }
}
