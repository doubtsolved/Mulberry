package com.example.data.schedule.model

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import java.util.UUID

@Keep
data class ScheduleVaultFile(
    @SerializedName("version") val version: Int = 2,
    @SerializedName("updatedAt") val updatedAt: Long = System.currentTimeMillis(),
    @SerializedName("weeklyGoalMinutes") val weeklyGoalMinutes: Int = 420, // 7 hours default
    @SerializedName("weeklyGoalPages") val weeklyGoalPages: Int = 200,
    @SerializedName("sessions") val sessions: List<ReadingSession> = emptyList(),
    @SerializedName("extraWeeklyPages") val extraWeeklyPages: Map<String, Int> = emptyMap()
)

@Keep
data class ReadingSession(
    @SerializedName("id") val id: String = UUID.randomUUID().toString(),
    @SerializedName("bookSlug") val bookSlug: String = "",
    @SerializedName("bookTitle") val bookTitle: String = "",
    @SerializedName("subject") val subject: String = "",
    @SerializedName("resourceType") val resourceType: String = "BOOK", // BOOK, NOTES, PYQ
    @SerializedName("startPage") val startPage: Int = 1,
    @SerializedName("endPage") val endPage: Int = 20,
    @SerializedName("plannedDurationMinutes") val plannedDurationMinutes: Int = 45,
    @SerializedName("scheduledEpochMillis") val scheduledEpochMillis: Long = 0L,
    @SerializedName("reminderMinutesBefore") val reminderMinutesBefore: Int = 15, // -1 = disabled
    @SerializedName("snoozedUntilEpochMillis") val snoozedUntilEpochMillis: Long? = null,
    @SerializedName("microNote") val microNote: String = "",
    @SerializedName("isCompleted") val isCompleted: Boolean = false,
    @SerializedName("completedEpochMillis") val completedEpochMillis: Long? = null,
    @SerializedName("actualPagesRead") val actualPagesRead: Int = 0,
    @SerializedName("actualDurationMinutes") val actualDurationMinutes: Int = 0
) {
    val totalPages: Int
        get() = (endPage - startPage + 1).coerceAtLeast(1)

    val alarmRequestCode: Int
        get() = id.hashCode()

    val targetAlarmEpochMillis: Long?
        get() {
            if (isCompleted) return null
            // Priority 1: If explicitly snoozed, fire at snoozed timestamp
            if (snoozedUntilEpochMillis != null) return snoozedUntilEpochMillis
            // Priority 2: Standard pre-session reminder
            if (reminderMinutesBefore < 0) return null
            return scheduledEpochMillis - (reminderMinutesBefore * 60_000L)
        }

    // Backwards compatibility helpers
    val estimatedMinutes: Int
        get() = if (plannedDurationMinutes > 0) plannedDurationMinutes else totalPages * 2

    val bookFileName: String
        get() = if (bookSlug.isNotBlank()) "$bookSlug.bin" else ""

    val vaultPath: String
        get() = "/sdcard/Mulberry"

    val bookRelativePath: String
        get() = if (bookSlug.isNotBlank()) "$bookSlug.bin" else ""
}

@Keep
data class ReadingHistoryIndex(
    @SerializedName("version") val version: Int = 2,
    @SerializedName("updatedAt") val updatedAt: Long = System.currentTimeMillis(),
    @SerializedName("books") val books: List<BookReadingState> = emptyList(),
    @SerializedName("logs") val logs: List<ReadingActivityLog> = emptyList()
)

@Keep
data class BookReadingState(
    @SerializedName("bookSlug") val bookSlug: String,
    @SerializedName("bookTitle") val bookTitle: String = "",
    @SerializedName("subject") val subject: String = "",
    @SerializedName("lastReadPage") val lastReadPage: Int = 1,
    @SerializedName("totalPages") val totalPages: Int = 1,
    @SerializedName("progressPercent") val progressPercent: Int = 0,
    @SerializedName("lastReadEpochMillis") val lastReadEpochMillis: Long = System.currentTimeMillis(),
    @SerializedName("totalTimeSeconds") val totalTimeSeconds: Long = 0L
)

@Keep
data class ReadingActivityLog(
    @SerializedName("logId") val logId: String = UUID.randomUUID().toString(),
    @SerializedName("sessionId") val sessionId: String? = null,
    @SerializedName("bookSlug") val bookSlug: String,
    @SerializedName("bookTitle") val bookTitle: String = "",
    @SerializedName("startedAtEpochMillis") val startedAtEpochMillis: Long = System.currentTimeMillis(),
    @SerializedName("endedAtEpochMillis") val endedAtEpochMillis: Long = System.currentTimeMillis(),
    @SerializedName("durationSeconds") val durationSeconds: Long = 0L,
    @SerializedName("startPage") val startPage: Int = 1,
    @SerializedName("endPage") val endPage: Int = 1,
    @SerializedName("pagesVisitedCount") val pagesVisitedCount: Int = 1
)
