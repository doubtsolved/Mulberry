package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "vaults")
data class VaultEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val name: String,
    val uriString: String,
    val pathDisplay: String,
    val bookCount: Int = 0,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "books")
data class BookEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val vaultId: Int,
    val title: String,
    val author: String = "",
    val fileName: String,
    val uriString: String,
    val pageCount: Int = 1,
    val fileSizeBytes: Long = 0L,
    val isFavorite: Boolean = false,
    val lastReadPage: Int = 1,
    val progressPercent: Int = 0,
    val lastReadTimestamp: Long = 0L,
    val parentFolder: String = "Vault"
)

val BookEntity.path: String get() = uriString
val BookEntity.fileSize: Long get() = fileSizeBytes
val BookEntity.rawFileName: String get() = fileName
val BookEntity.lastPage: Int get() = lastReadPage
val BookEntity.totalPages: Int get() = pageCount

@Entity(tableName = "agenda_tasks")
data class AgendaTaskEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val isCompleted: Boolean = false,
    val dateString: String,
    val linkedBookId: Int? = null,
    val linkedBookTitle: String? = null,
    val linkedChapterPage: Int? = null,
    val linkedLectureUrl: String? = null,
    val linkedLectureTitle: String? = null,
    val orderIndex: Int = 0
)

@Entity(tableName = "exams")
data class ExamEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val examDateMillis: Long,
    val totalChaptersCount: Int = 10,
    val remainingChaptersCount: Int = 8,
    val syllabusJson: String = "[]" // JSON array of items: [{"title":"Ch. 1 Cell Physiology", "completed": true}, ...]
)

@Entity(tableName = "annotations")
data class AnnotationEntity(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val bookId: Int,
    val pageIndex: Int,
    val strokePointsJson: String, // JSON array of x,y coordinates
    val colorHex: String,
    val strokeWidth: Float,
    val type: String, // PEN, HIGHLIGHTER, NOTE
    val timestamp: Long = System.currentTimeMillis()
)
