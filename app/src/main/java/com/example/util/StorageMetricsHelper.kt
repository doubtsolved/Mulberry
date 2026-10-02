package com.example.util

import android.content.Context
import android.text.format.Formatter
import java.io.File

object StorageMetricsHelper {

    fun getCacheSizeBytes(context: Context): Long {
        val internalCache = context.cacheDir.walkTopDown().filter { it.isFile }.sumOf { it.length() }
        val externalCache = context.externalCacheDir?.walkTopDown()?.filter { it.isFile }?.sumOf { it.length() } ?: 0L
        return internalCache + externalCache
    }

    fun getDatabaseSizeBytes(context: Context): Long {
        val dbFile1 = context.getDatabasePath("mulberry_database.db")
        val dbFile2 = context.getDatabasePath("mulberry_database")
        val db = if (dbFile1.exists()) dbFile1 else dbFile2
        val wal = File(db.path + "-wal")
        val shm = File(db.path + "-shm")
        return (if (db.exists()) db.length() else 0L) +
                (if (wal.exists()) wal.length() else 0L) +
                (if (shm.exists()) shm.length() else 0L)
    }

    fun formatFileSize(context: Context, bytes: Long): String {
        return Formatter.formatFileSize(context, bytes)
    }

    fun clearCache(context: Context) {
        try {
            context.cacheDir.listFiles()?.forEach { file ->
                file.deleteRecursively()
            }
            context.externalCacheDir?.listFiles()?.forEach { file ->
                file.deleteRecursively()
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
