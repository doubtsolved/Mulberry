package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "snips")
data class SnipEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bookId: String,
    val bookTitle: String = "",
    val subject: String,
    val pageNumber: Int,
    val imageLocalPath: String, // e.g. context.filesDir/snips/{id}.webp
    val title: String,          // e.g. "Brachial Plexus Diagram"
    val tagsCsv: String = "",   // e.g. "anatomy, upper limb, high yield"
    val createdAt: Long = System.currentTimeMillis()
)
