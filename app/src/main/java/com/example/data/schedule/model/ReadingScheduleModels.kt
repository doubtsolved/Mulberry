package com.example.data.schedule.model

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import java.util.UUID

@Keep
data class ScheduleVaultFile(
    @SerializedName("version") val version: Int = 1,
    @SerializedName("weeklyGoalPages") val weeklyGoalPages: Int = 200,
    @SerializedName("sessions") val sessions: List<ReadingSession> = emptyList()
)

@Keep
data class ReadingSession(
    @SerializedName("id") val id: String = UUID.randomUUID().toString(),
    @SerializedName("vaultPath") val vaultPath: String = "",              // Absolute root path of containing vault
    @SerializedName("bookFileName") val bookFileName: String = "",           // Base file name (e.g., "Guyton.pdf")
    @SerializedName("bookRelativePath") val bookRelativePath: String = "",  // Relative path in vault (e.g., "1st_Prof/Biochem/Satyanarayan.pdf")
    @SerializedName("bookTitle") val bookTitle: String = "",              // Human-readable title
    @SerializedName("startPage") val startPage: Int = 1,                 // 1-indexed start page
    @SerializedName("endPage") val endPage: Int = 1,                   // 1-indexed end page
    @SerializedName("currentPageReached") val currentPageReached: Int = startPage, // 1-indexed live page reached
    @SerializedName("scheduledEpochMillis") val scheduledEpochMillis: Long = 0L,     // Target start timestamp
    @SerializedName("reminderMinutesBefore") val reminderMinutesBefore: Int = 15, // 0, 15, 30, 60 (-1 = disabled)
    @SerializedName("microNote") val microNote: String = "",         // Short focus label (e.g., "Aortic valve murmurs")
    @SerializedName("isCompleted") val isCompleted: Boolean = false,
    @SerializedName("completedEpochMillis") val completedEpochMillis: Long? = null,
    @SerializedName("actualPagesRead") val actualPagesRead: Int = 0
) {
    val totalPages: Int get() = (endPage - startPage + 1).coerceAtLeast(1)
    val estimatedMinutes: Int get() = totalPages * 2 // 2 min/page standard estimate
    val alarmRequestCode: Int get() = id.hashCode()

    val progressFraction: Float
        get() {
            if (isCompleted) return 1f
            if (endPage <= startPage) return 0f
            val current = currentPageReached.coerceIn(startPage, endPage)
            return ((current - startPage).toFloat() / (endPage - startPage).toFloat()).coerceIn(0f, 1f)
        }

    val progressPercent: Int
        get() = (progressFraction * 100f).toInt().coerceIn(0, 100)

    val targetAlarmEpochMillis: Long?
        get() = if (reminderMinutesBefore >= 0 && scheduledEpochMillis > 0L) {
            scheduledEpochMillis - (reminderMinutesBefore * 60 * 1000L)
        } else null
}
