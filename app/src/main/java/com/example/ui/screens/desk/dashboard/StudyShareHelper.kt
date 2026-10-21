package com.example.ui.screens.desk.dashboard

import android.content.Context
import android.content.Intent
import android.widget.Toast
import com.example.data.analytics.model.StudyMetricsData
import com.example.data.model.UserProfile

object StudyShareHelper {

    fun shareMetrics(context: Context, metrics: StudyMetricsData, userProfile: UserProfile) {
        try {
            val studentName = userProfile.displayName.ifBlank { "Sahabul" }
            val doctorTitle = if (studentName.startsWith("Dr.", ignoreCase = true)) studentName else "Dr. $studentName"

            val topSubjectInfo = metrics.topSubject?.let { (subject, mins) ->
                val hrs = mins / 60
                val remMins = mins % 60
                val timeStr = if (hrs > 0) "${hrs}.${(remMins * 10) / 60} hrs" else "${mins} mins"
                "$subject ($timeStr)"
            } ?: "Pathology (6.5 hrs)"

            val shareText = buildString {
                appendLine("🎓 Mulberry Study Card • $doctorTitle")
                appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                appendLine("🔥 Current Streak: ${metrics.currentStreakDays} Days (Best: ${metrics.bestStreakDays})")
                appendLine("⏱ Total Study Focus: ${metrics.formattedTotalTime}")
                appendLine("📚 Top Subject: $topSubjectInfo")
                appendLine("📑 Pages Mastered: ${metrics.totalPagesRead} pages")
                appendLine("⚡ Reading Velocity: ${metrics.formattedVelocity}")
                appendLine("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━")
                appendLine("Studying smarter with Mulberry MBBS")
            }

            val sendIntent = Intent().apply {
                action = Intent.ACTION_SEND
                putExtra(Intent.EXTRA_TEXT, shareText)
                putExtra(Intent.EXTRA_SUBJECT, "$doctorTitle's Mulberry MBBS Study Snapshot")
                type = "text/plain"
            }

            val chooser = Intent.createChooser(sendIntent, "Share Study Metrics")
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(chooser)
        } catch (e: Exception) {
            Toast.makeText(context, "Could not open share sheet", Toast.LENGTH_SHORT).show()
        }
    }
}
