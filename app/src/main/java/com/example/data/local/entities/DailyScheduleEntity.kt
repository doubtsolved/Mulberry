package com.example.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Deprecated("Legacy scheduling entity. Replaced by com.example.data.schedule.model.ReadingSession and /sdcard/Mulberry/study_planner.json")
@Entity(tableName = "daily_schedules")
data class DailyScheduleEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val bookId: String,
    val bookTitle: String = "",
    val targetDateEpochDay: Long, // Epoch day for "Today" filtering (e.g. System.currentTimeMillis() / 86400000)
    val targetChapter: String,     // e.g. "Cardiovascular System"
    val startPage: Int,
    val endPage: Int,
    val isCompleted: Boolean = false
) {
    val totalPages: Int get() = (endPage - startPage + 1).coerceAtLeast(1)
    val estimatedMinutes: Int get() = totalPages * 2
}
