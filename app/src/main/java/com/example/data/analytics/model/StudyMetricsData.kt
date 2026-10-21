package com.example.data.analytics.model

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName
import java.util.UUID

@Keep
data class StudySessionRecord(
    @SerializedName("id") val id: String = UUID.randomUUID().toString(),
    @SerializedName("bookTitle") val bookTitle: String = "",
    @SerializedName("subject") val subject: String = "General Medical",
    @SerializedName("durationMinutes") val durationMinutes: Int = 0,
    @SerializedName("pagesRead") val pagesRead: Int = 0,
    @SerializedName("timestampEpochMillis") val timestampEpochMillis: Long = System.currentTimeMillis()
)

@Keep
data class StudyMetricsData(
    @SerializedName("version") val version: Int = 1,
    @SerializedName("totalStudyMinutes") val totalStudyMinutes: Long = 1120L, // Default demo seed ~18h 40m
    @SerializedName("currentStreakDays") val currentStreakDays: Int = 5,
    @SerializedName("bestStreakDays") val bestStreakDays: Int = 12,
    @SerializedName("lastStudyDate") val lastStudyDate: String = "",
    @SerializedName("totalPagesRead") val totalPagesRead: Int = 620,
    @SerializedName("readingVelocityMinPerPage") val readingVelocityMinPerPage: Float = 1.8f,
    @SerializedName("dailyMinutes") val dailyMinutes: Map<String, Int> = emptyMap(),
    @SerializedName("subjectMinutes") val subjectMinutes: Map<String, Int> = emptyMap(),
    @SerializedName("recentSessions") val recentSessions: List<StudySessionRecord> = emptyList()
) {
    val totalHours: Int get() = (totalStudyMinutes / 60).toInt()
    val remainingMinutes: Int get() = (totalStudyMinutes % 60).toInt()

    val formattedTotalTime: String
        get() = if (totalHours > 0) "${totalHours} hrs ${remainingMinutes} min" else "${remainingMinutes} min"

    val formattedVelocity: String
        get() = String.format("%.1f min/page", if (readingVelocityMinPerPage <= 0f) 1.8f else readingVelocityMinPerPage)

    val topSubject: Pair<String, Int>?
        get() = subjectMinutes.maxByOrNull { it.value }?.toPair()
}
