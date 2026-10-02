package com.example.data.model

import com.google.gson.annotations.SerializedName

data class LibraryStateConfig(
    @SerializedName("version") val version: Int = 1,
    @SerializedName("lastUpdated") val lastUpdated: Long = System.currentTimeMillis(),
    @SerializedName("books") val books: List<BookStateItem> = emptyList()
)

data class BookStateItem(
    @SerializedName("relativePath") val relativePath: String, // e.g., "Biochemistry/Satyanarayana.pdf"
    @SerializedName("lastPage") val lastPage: Int = 1,
    @SerializedName("totalPages") val totalPages: Int = 1,
    @SerializedName("progressPercent") val progressPercent: Float = 0f,
    @SerializedName("isFavorite") val isFavorite: Boolean = false,
    @SerializedName("lastReadTimestamp") val lastReadTimestamp: Long = 0L
)

data class StudyTasksConfig(
    @SerializedName("version") val version: Int = 1,
    @SerializedName("lastUpdated") val lastUpdated: Long = System.currentTimeMillis(),
    @SerializedName("tasks") val tasks: List<StudyTaskItem> = emptyList()
)

data class StudyTaskItem(
    @SerializedName("id") val id: String,
    @SerializedName("title") val title: String,
    @SerializedName("isCompleted") val isCompleted: Boolean = false,
    @SerializedName("dueDate") val dueDate: Long? = null,
    @SerializedName("linkedBookRelativePath") val linkedBookRelativePath: String? = null,
    @SerializedName("linkedPage") val linkedPage: Int? = null
)
