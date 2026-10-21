package com.example.data.analytics

import android.content.Context
import android.os.Build
import android.util.Log
import com.example.data.analytics.model.StudyMetricsData
import com.example.data.analytics.model.StudySessionRecord
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

class StudyMetricsManager private constructor(private val appContext: Context) {

    companion object {
        private const val TAG = "StudyMetricsManager"
        private const val METRICS_FILE_NAME = "study_analytics_metrics.json"

        @Volatile
        private var instance: StudyMetricsManager? = null

        fun getInstance(context: Context): StudyMetricsManager {
            return instance ?: synchronized(this) {
                instance ?: StudyMetricsManager(context.applicationContext).also { instance = it }
            }
        }
    }

    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()
    private val mutex = Mutex()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val metricsFile: File = File(appContext.filesDir, METRICS_FILE_NAME)

    private val _metrics = MutableStateFlow(createInitialMetrics())
    val metrics: StateFlow<StudyMetricsData> = _metrics.asStateFlow()

    init {
        scope.launch {
            loadMetrics()
        }
    }

    private fun getTodayDateString(): String {
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())
    }

    private fun getYesterdayDateString(): String {
        val cal = Calendar.getInstance()
        cal.add(Calendar.DAY_OF_YEAR, -1)
        return SimpleDateFormat("yyyy-MM-dd", Locale.US).format(cal.time)
    }

    private fun createInitialMetrics(): StudyMetricsData {
        val today = getTodayDateString()
        val cal = Calendar.getInstance()
        val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)

        val daysMap = mutableMapOf<String, Int>()
        // Seed past 7 days with realistic study hours
        val pastSeedMinutes = listOf(150, 110, 190, 120, 170, 0, 0)
        for (i in 0 until 7) {
            val c = Calendar.getInstance()
            c.firstDayOfWeek = Calendar.MONDAY
            c.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            c.add(Calendar.DAY_OF_YEAR, i)
            val dateStr = format.format(c.time)
            daysMap[dateStr] = pastSeedMinutes[i]
        }

        val subjectMap = mapOf(
            "Pathology" to 390,     // 6.5 hrs
            "Pharmacology" to 240,  // 4.0 hrs
            "Microbiology" to 192,  // 3.2 hrs
            "Anatomy" to 150,       // 2.5 hrs
            "Physiology" to 148     // 2.4 hrs
        )

        val recent = listOf(
            StudySessionRecord(
                bookTitle = "Robbins Basic Pathology",
                subject = "Pathology",
                durationMinutes = 45,
                pagesRead = 28,
                timestampEpochMillis = System.currentTimeMillis() - 7200000L
            ),
            StudySessionRecord(
                bookTitle = "KD Tripathi Pharmacology",
                subject = "Pharmacology",
                durationMinutes = 60,
                pagesRead = 34,
                timestampEpochMillis = System.currentTimeMillis() - 86400000L
            ),
            StudySessionRecord(
                bookTitle = "Ananthanarayan Microbiology",
                subject = "Microbiology",
                durationMinutes = 40,
                pagesRead = 22,
                timestampEpochMillis = System.currentTimeMillis() - 172800000L
            )
        )

        return StudyMetricsData(
            version = 1,
            totalStudyMinutes = 1120L, // 18 hrs 40 mins
            currentStreakDays = 5,
            bestStreakDays = 12,
            lastStudyDate = today,
            totalPagesRead = 620,
            readingVelocityMinPerPage = 1.8f,
            dailyMinutes = daysMap,
            subjectMinutes = subjectMap,
            recentSessions = recent
        )
    }

    private suspend fun loadMetrics(): StudyMetricsData = withContext(Dispatchers.IO) {
        mutex.withLock {
            if (metricsFile.exists()) {
                try {
                    val json = metricsFile.readText()
                    if (json.isNotBlank()) {
                        val parsed = gson.fromJson(json, StudyMetricsData::class.java)
                        if (parsed != null) {
                            _metrics.value = parsed
                            return@withLock parsed
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Error reading $METRICS_FILE_NAME, resetting to default", e)
                }
            }

            // If file does not exist, initialize and persist demo baseline
            val initial = createInitialMetrics()
            _metrics.value = initial
            persistToDisk(initial)
            initial
        }
    }

    private fun persistToDisk(data: StudyMetricsData) {
        try {
            val tmp = File(appContext.filesDir, "$METRICS_FILE_NAME.tmp")
            val json = gson.toJson(data)
            FileOutputStream(tmp).use { fos ->
                fos.write(json.toByteArray(Charsets.UTF_8))
                fos.flush()
                fos.fd.sync()
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                try {
                    Files.move(tmp.toPath(), metricsFile.toPath(), StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING)
                } catch (_: Exception) {
                    if (metricsFile.exists()) metricsFile.delete()
                    tmp.renameTo(metricsFile)
                }
            } else {
                if (metricsFile.exists()) metricsFile.delete()
                tmp.renameTo(metricsFile)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed writing metrics to disk", e)
        }
    }

    suspend fun recordStudySession(
        bookTitle: String,
        subject: String,
        durationMinutes: Int,
        pagesRead: Int
    ): Unit = withContext(Dispatchers.IO) {
        mutex.withLock {
            val current = _metrics.value
            val today = getTodayDateString()
            val yesterday = getYesterdayDateString()

            val effectiveDuration = durationMinutes.coerceAtLeast(1)
            val effectivePages = pagesRead.coerceAtLeast(1)

            // Calculate streak
            val newStreak = when (current.lastStudyDate) {
                today -> current.currentStreakDays // Already studied today
                yesterday -> current.currentStreakDays + 1 // Consecutive day streak continued
                else -> 1 // Streak broken or first day
            }
            val newBestStreak = maxOf(current.bestStreakDays, newStreak)

            // Update times and counts
            val newTotalTime = current.totalStudyMinutes + effectiveDuration
            val newTotalPages = current.totalPagesRead + effectivePages
            val newVelocity = (newTotalTime.toFloat() / newTotalPages.toFloat()).coerceIn(0.5f, 10.0f)

            // Update daily map
            val updatedDaily = current.dailyMinutes.toMutableMap()
            updatedDaily[today] = (updatedDaily[today] ?: 0) + effectiveDuration

            // Update subject map
            val effectiveSubject = if (subject.isBlank() || subject.equals("General", ignoreCase = true)) "Medical Core" else subject
            val updatedSubjects = current.subjectMinutes.toMutableMap()
            updatedSubjects[effectiveSubject] = (updatedSubjects[effectiveSubject] ?: 0) + effectiveDuration

            // Prepend session to recent sessions
            val newSession = StudySessionRecord(
                bookTitle = bookTitle,
                subject = effectiveSubject,
                durationMinutes = effectiveDuration,
                pagesRead = effectivePages,
                timestampEpochMillis = System.currentTimeMillis()
            )
            val updatedRecent = (listOf(newSession) + current.recentSessions).take(20)

            val updatedMetrics = current.copy(
                totalStudyMinutes = newTotalTime,
                currentStreakDays = newStreak,
                bestStreakDays = newBestStreak,
                lastStudyDate = today,
                totalPagesRead = newTotalPages,
                readingVelocityMinPerPage = newVelocity,
                dailyMinutes = updatedDaily,
                subjectMinutes = updatedSubjects,
                recentSessions = updatedRecent
            )

            _metrics.value = updatedMetrics
            persistToDisk(updatedMetrics)
            Log.d(TAG, "Logged study session: $bookTitle ($effectiveDuration mins, $effectivePages pages). Streak: $newStreak days")
        }
    }
}
