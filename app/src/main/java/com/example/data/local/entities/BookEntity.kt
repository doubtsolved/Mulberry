package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "books")
data class BookEntity(
    @PrimaryKey val id: String,
    val title: String,
    val author: String,
    val edition: String,
    val prof: String,              // e.g., "1st Prof"
    val subject: String,           // e.g., "Physiology"
    val type: String,              // e.g., "Standard Textbook", "Handwritten Notes"
    val tagsCsv: String,           // Comma-separated or JSON list of search tags
    val totalPages: Int,
    val sizeMb: Double,
    val coverUrl: String,          // jsDelivr CDN URL
    val downloadUrl: String,       // GitHub Release .bin URL
    val driveFallbackUrl: String,  // Google Drive fallback
    val telegramDeepLink: String,  // Telegram bot link
    val uploader: String = "Mulberry",

    // Device offline state
    val isDownloaded: Boolean = false,
    val localBinPath: String? = null,    // Points to context.filesDir/vault/{id}.bin
    val lastReadPage: Int = 1,
    val readingProgressPercent: Int = 0,
    val shelfFolder: String = "All Books",
    val lastAccessedTimestamp: Long = 0L
)
