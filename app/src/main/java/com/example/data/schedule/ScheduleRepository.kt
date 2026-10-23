package com.example.data.schedule

import android.content.Context
import android.util.Log
import com.example.data.schedule.model.ReadingSession
import com.example.data.schedule.model.ScheduleVaultFile
import com.example.util.MulberryStorageManager
import com.example.util.SafeFileWriter
import com.example.util.ScheduleAlarmScheduler
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
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
import java.lang.reflect.Type
import java.util.Calendar
import java.util.UUID

class ScheduleRepository private constructor(private val appContext: Context) {

    companion object {
        private const val TAG = "ScheduleRepository"

        @Volatile
        private var instance: ScheduleRepository? = null

        fun getInstance(context: Context): ScheduleRepository {
            return instance ?: synchronized(this) {
                instance ?: ScheduleRepository(context.applicationContext).also { instance = it }
            }
        }
    }

    private class ScheduleVaultFileDeserializer : JsonDeserializer<ScheduleVaultFile> {
        override fun deserialize(
            json: JsonElement?,
            typeOfT: Type?,
            context: JsonDeserializationContext?
        ): ScheduleVaultFile {
            if (json == null || !json.isJsonObject) {
                return ScheduleVaultFile()
            }
            val obj = json.asJsonObject
            val version = obj.get("version")?.let { if (it.isJsonPrimitive) runCatching { it.asInt }.getOrDefault(2) else 2 } ?: 2
            val updatedAt = obj.get("updatedAt")?.let { if (it.isJsonPrimitive) runCatching { it.asLong }.getOrDefault(System.currentTimeMillis()) else System.currentTimeMillis() } ?: System.currentTimeMillis()
            val weeklyGoalMinutes = obj.get("weeklyGoalMinutes")?.let { if (it.isJsonPrimitive) runCatching { it.asInt }.getOrDefault(420) else 420 } ?: 420
            val weeklyGoalPages = obj.get("weeklyGoalPages")?.let { if (it.isJsonPrimitive) runCatching { it.asInt }.getOrDefault(200) else 200 } ?: 200

            val sessionsList = mutableListOf<ReadingSession>()
            val sessionsElem = obj.get("sessions")
            if (sessionsElem != null && sessionsElem.isJsonArray) {
                for (item in sessionsElem.asJsonArray) {
                    if (item != null && item.isJsonObject && context != null) {
                        try {
                            val session: ReadingSession? = context.deserialize(item, ReadingSession::class.java)
                            if (session != null) {
                                sessionsList.add(session)
                            }
                        } catch (e: Exception) {
                            Log.w(TAG, "Skipping invalid session item in study_planner.json", e)
                        }
                    }
                }
            }
            val extraMap = mutableMapOf<String, Int>()
            val extraElem = obj.get("extraWeeklyPages")
            if (extraElem != null && extraElem.isJsonObject) {
                for ((key, value) in extraElem.asJsonObject.entrySet()) {
                    if (value != null && value.isJsonPrimitive) {
                        runCatching { value.asInt }.onSuccess { extraMap[key] = it }
                    }
                }
            }

            return ScheduleVaultFile(
                version = version,
                updatedAt = updatedAt,
                weeklyGoalMinutes = weeklyGoalMinutes,
                weeklyGoalPages = weeklyGoalPages,
                sessions = sessionsList,
                extraWeeklyPages = extraMap
            )
        }
    }

    private class ReadingSessionDeserializer : JsonDeserializer<ReadingSession> {
        override fun deserialize(
            json: JsonElement?,
            typeOfT: Type?,
            context: JsonDeserializationContext?
        ): ReadingSession {
            if (json == null || !json.isJsonObject) {
                return ReadingSession()
            }
            val obj = json.asJsonObject

            // Handle legacy bookFileName/bookRelativePath mapping to bookSlug
            val explicitSlug = obj.get("bookSlug")?.let { runCatching { it.asString }.getOrNull() }
            val fallbackSlug = obj.get("bookFileName")?.let { runCatching { it.asString }.getOrNull() }
                ?.removeSuffix(".bin")?.removeSuffix(".pdf")
            val relativeSlug = obj.get("bookRelativePath")?.let { runCatching { it.asString }.getOrNull() }
                ?.let { File(it).nameWithoutExtension }
            val finalSlug = explicitSlug?.takeIf { it.isNotBlank() }
                ?: fallbackSlug?.takeIf { it.isNotBlank() }
                ?: relativeSlug?.takeIf { it.isNotBlank() }
                ?: ""

            return ReadingSession(
                id = obj.get("id")?.let { runCatching { it.asString }.getOrNull() } ?: UUID.randomUUID().toString(),
                bookSlug = finalSlug,
                bookTitle = obj.get("bookTitle")?.let { runCatching { it.asString }.getOrNull() } ?: "",
                subject = obj.get("subject")?.let { runCatching { it.asString }.getOrNull() } ?: "",
                resourceType = obj.get("resourceType")?.let { runCatching { it.asString }.getOrNull() } ?: "BOOK",
                startPage = obj.get("startPage")?.let { runCatching { it.asInt }.getOrDefault(1) } ?: 1,
                endPage = obj.get("endPage")?.let { runCatching { it.asInt }.getOrDefault(20) } ?: 20,
                plannedDurationMinutes = obj.get("plannedDurationMinutes")?.let { runCatching { it.asInt }.getOrDefault(45) } ?: 45,
                scheduledEpochMillis = obj.get("scheduledEpochMillis")?.let { runCatching { it.asLong }.getOrDefault(0L) } ?: 0L,
                reminderMinutesBefore = obj.get("reminderMinutesBefore")?.let { runCatching { it.asInt }.getOrDefault(15) } ?: 15,
                snoozedUntilEpochMillis = obj.get("snoozedUntilEpochMillis")?.let { if (it.isJsonPrimitive) runCatching { it.asLong }.getOrNull() else null },
                microNote = obj.get("microNote")?.let { runCatching { it.asString }.getOrNull() } ?: "",
                isCompleted = obj.get("isCompleted")?.let { runCatching { it.asBoolean }.getOrDefault(false) } ?: false,
                completedEpochMillis = obj.get("completedEpochMillis")?.let { if (it.isJsonPrimitive) runCatching { it.asLong }.getOrNull() else null },
                actualPagesRead = obj.get("actualPagesRead")?.let { runCatching { it.asInt }.getOrDefault(0) } ?: 0,
                actualDurationMinutes = obj.get("actualDurationMinutes")?.let { runCatching { it.asInt }.getOrDefault(0) } ?: 0
            )
        }
    }

    private val gson: Gson = GsonBuilder()
        .registerTypeAdapter(ScheduleVaultFile::class.java, ScheduleVaultFileDeserializer())
        .registerTypeAdapter(ReadingSession::class.java, ReadingSessionDeserializer())
        .setLenient()
        .setPrettyPrinting()
        .create()

    private val mutex = Mutex()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    private var activeVaultPath: String? = null

    private val _scheduleState = MutableStateFlow(ScheduleVaultFile())
    val scheduleState: StateFlow<ScheduleVaultFile> = _scheduleState.asStateFlow()

    init {
        scope.launch {
            loadSchedule()
        }
    }

    fun setActiveVaultPath(vaultPath: String?) {
        activeVaultPath = vaultPath
        scope.launch {
            loadSchedule()
        }
    }

    fun getActiveVaultPath(): String = activeVaultPath ?: MulberryStorageManager.getMulberryDirectory(appContext).absolutePath

    /**
     * Resolves the primary study_planner.json file at /sdcard/Mulberry/study_planner.json
     * with automatic migration from legacy .schedule/schedule.json or .mulberry/schedule.json.
     */
    private fun getStudyPlannerFile(): File {
        val customRoot = activeVaultPath
        val baseDir = if (!customRoot.isNullOrBlank() && File(customRoot).exists()) {
            File(customRoot)
        } else {
            MulberryStorageManager.getMulberryDirectory(appContext)
        }
        val plannerFile = File(baseDir, MulberryStorageManager.STUDY_PLANNER_FILE_NAME)
        if (!plannerFile.exists()) {
            // Check legacy paths for automatic migration
            val legacyScheduleDir = File(baseDir, ".schedule/schedule.json")
            val legacyMulberryDir = File(baseDir, ".mulberry/schedule.json")
            val sourceFile = when {
                legacyScheduleDir.exists() -> legacyScheduleDir
                legacyMulberryDir.exists() -> legacyMulberryDir
                else -> null
            }
            if (sourceFile != null && sourceFile.length() > 0L) {
                try {
                    val rawJson = sourceFile.readText()
                    if (rawJson.isNotBlank()) {
                        val parsed = gson.fromJson(rawJson, ScheduleVaultFile::class.java)
                        if (parsed != null) {
                            SafeFileWriter.writeAtomically(plannerFile, gson.toJson(parsed.copy(version = 2)))
                            Log.i(TAG, "Migrated legacy schedule from ${sourceFile.absolutePath} to ${plannerFile.absolutePath}")
                        }
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed migrating legacy schedule file", e)
                }
            }
        }
        return plannerFile
    }

    suspend fun loadSchedule(): ScheduleVaultFile = withContext(Dispatchers.IO) {
        mutex.withLock {
            val plannerFile = getStudyPlannerFile()
            var parsed: ScheduleVaultFile? = null

            if (plannerFile.exists() && plannerFile.length() > 0L) {
                try {
                    val content = SafeFileWriter.readSafely(plannerFile)
                    if (!content.isNullOrBlank()) {
                        parsed = gson.fromJson(content, ScheduleVaultFile::class.java)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Corrupted study_planner.json detected, quarantining file and recovering", e)
                    try {
                        val corruptFile = File(
                            plannerFile.parentFile,
                            "study_planner.json.corrupted_${System.currentTimeMillis()}"
                        )
                        plannerFile.renameTo(corruptFile)
                    } catch (renEx: Exception) {
                        Log.e(TAG, "Failed renaming corrupted study_planner file", renEx)
                    }
                }
            }

            val finalState = ScheduleVaultFile(
                version = 2,
                updatedAt = parsed?.updatedAt ?: System.currentTimeMillis(),
                weeklyGoalMinutes = parsed?.weeklyGoalMinutes ?: 420,
                weeklyGoalPages = parsed?.weeklyGoalPages ?: 200,
                sessions = parsed?.sessions ?: emptyList(),
                extraWeeklyPages = parsed?.extraWeeklyPages ?: emptyMap()
            )

            _scheduleState.value = finalState

            if (parsed == null && !plannerFile.exists()) {
                runCatching { persistToDisk(finalState) }
            }

            // Re-arm uncompleted future alarms
            ScheduleAlarmScheduler.rescheduleAllPendingAlarms(appContext, finalState.sessions)

            finalState
        }
    }

    private suspend fun persistToDisk(data: ScheduleVaultFile) = withContext(Dispatchers.IO) {
        try {
            val plannerFile = getStudyPlannerFile()
            val updated = data.copy(updatedAt = System.currentTimeMillis())
            val json = gson.toJson(updated)
            val success = SafeFileWriter.writeAtomically(plannerFile, json)
            if (success) {
                _scheduleState.value = updated
                Log.d(TAG, "Atomically saved ${updated.sessions.size} study sessions to ${plannerFile.absolutePath}")
            } else {
                Log.e(TAG, "SafeFileWriter failed writing study_planner.json to ${plannerFile.absolutePath}")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed writing study planner to disk", e)
        }
    }

    suspend fun addSession(session: ReadingSession): Unit = withContext(Dispatchers.IO) {
        mutex.withLock {
            val current = _scheduleState.value
            val updatedSessions = current.sessions + session
            val updatedFile = current.copy(sessions = updatedSessions)
            persistToDisk(updatedFile)
            ScheduleAlarmScheduler.scheduleAlarm(appContext, session)
        }
    }

    suspend fun updateSession(session: ReadingSession): Unit = withContext(Dispatchers.IO) {
        mutex.withLock {
            val current = _scheduleState.value
            val existing = current.sessions.find { it.id == session.id }
            if (existing != null) {
                ScheduleAlarmScheduler.cancelAlarm(appContext, existing)
            }
            val updatedSessions = current.sessions.map {
                if (it.id == session.id) session else it
            }
            persistToDisk(current.copy(sessions = updatedSessions))
            if (!session.isCompleted) {
                ScheduleAlarmScheduler.scheduleAlarm(appContext, session)
            }
        }
    }

    suspend fun snoozeSession(sessionId: String, snoozeMinutes: Int = 30): Unit = withContext(Dispatchers.IO) {
        mutex.withLock {
            val current = _scheduleState.value
            val target = current.sessions.find { it.id == sessionId } ?: return@withLock
            val snoozeTimestamp = System.currentTimeMillis() + (snoozeMinutes * 60_000L)

            val updatedSession = target.copy(
                snoozedUntilEpochMillis = snoozeTimestamp,
                isCompleted = false
            )

            val updatedSessions = current.sessions.map {
                if (it.id == sessionId) updatedSession else it
            }

            ScheduleAlarmScheduler.cancelAlarm(appContext, target)
            persistToDisk(current.copy(sessions = updatedSessions))
            ScheduleAlarmScheduler.scheduleAlarm(appContext, updatedSession)
            Log.d(TAG, "Snoozed session $sessionId for $snoozeMinutes min until $snoozeTimestamp")
        }
    }

    suspend fun markCompleted(sessionId: String, actualReadPages: Int = 0, actualDurationMinutes: Int = 0): Unit = withContext(Dispatchers.IO) {
        mutex.withLock {
            val current = _scheduleState.value
            val target = current.sessions.find { it.id == sessionId } ?: return@withLock
            val now = System.currentTimeMillis()

            val updatedSession = target.copy(
                isCompleted = true,
                completedEpochMillis = now,
                snoozedUntilEpochMillis = null,
                actualPagesRead = if (actualReadPages > 0) actualReadPages else target.totalPages,
                actualDurationMinutes = if (actualDurationMinutes > 0) actualDurationMinutes else target.plannedDurationMinutes
            )

            val updatedSessions = current.sessions.map {
                if (it.id == sessionId) updatedSession else it
            }

            ScheduleAlarmScheduler.cancelAlarm(appContext, target)
            persistToDisk(current.copy(sessions = updatedSessions))
            Log.d(TAG, "Marked session $sessionId completed: read $actualReadPages pages in $actualDurationMinutes min")
        }
    }

    suspend fun updateIncrementalSessionProgress(sessionId: String, actualReadPages: Int, actualDurationMinutes: Int): Unit = withContext(Dispatchers.IO) {
        mutex.withLock {
            val current = _scheduleState.value
            val target = current.sessions.find { it.id == sessionId } ?: return@withLock
            if (target.isCompleted) return@withLock

            val updatedSession = target.copy(
                actualPagesRead = maxOf(target.actualPagesRead, actualReadPages),
                actualDurationMinutes = maxOf(target.actualDurationMinutes, actualDurationMinutes)
            )

            val updatedSessions = current.sessions.map {
                if (it.id == sessionId) updatedSession else it
            }
            persistToDisk(current.copy(sessions = updatedSessions))
        }
    }

    suspend fun toggleCompleted(sessionId: String): Unit = withContext(Dispatchers.IO) {
        mutex.withLock {
            val current = _scheduleState.value
            val target = current.sessions.find { it.id == sessionId } ?: return@withLock
            val newCompleted = !target.isCompleted
            val now = System.currentTimeMillis()

            val updatedSession = if (newCompleted) {
                target.copy(
                    isCompleted = true,
                    completedEpochMillis = now,
                    snoozedUntilEpochMillis = null,
                    actualPagesRead = target.totalPages,
                    actualDurationMinutes = target.plannedDurationMinutes
                )
            } else {
                target.copy(
                    isCompleted = false,
                    completedEpochMillis = null,
                    snoozedUntilEpochMillis = null,
                    actualPagesRead = 0,
                    actualDurationMinutes = 0
                )
            }

            val updatedSessions = current.sessions.map {
                if (it.id == sessionId) updatedSession else it
            }

            if (newCompleted) {
                ScheduleAlarmScheduler.cancelAlarm(appContext, target)
            } else {
                ScheduleAlarmScheduler.scheduleAlarm(appContext, updatedSession)
            }

            persistToDisk(current.copy(sessions = updatedSessions))
        }
    }

    suspend fun bumpSession(sessionId: String, daysToAdd: Int = 1): Unit = withContext(Dispatchers.IO) {
        mutex.withLock {
            val current = _scheduleState.value
            val target = current.sessions.find { it.id == sessionId } ?: return@withLock
            val additionalMillis = daysToAdd * 24L * 60L * 60L * 1000L
            val newScheduledTime = target.scheduledEpochMillis + additionalMillis

            val updatedSession = target.copy(
                scheduledEpochMillis = newScheduledTime,
                snoozedUntilEpochMillis = null,
                isCompleted = false,
                completedEpochMillis = null
            )

            val updatedSessions = current.sessions.map {
                if (it.id == sessionId) updatedSession else it
            }

            ScheduleAlarmScheduler.cancelAlarm(appContext, target)
            persistToDisk(current.copy(sessions = updatedSessions))
            ScheduleAlarmScheduler.scheduleAlarm(appContext, updatedSession)
        }
    }

    suspend fun postponeSessionTo(sessionId: String, newEpochMillis: Long): Unit = withContext(Dispatchers.IO) {
        mutex.withLock {
            val current = _scheduleState.value
            val target = current.sessions.find { it.id == sessionId } ?: return@withLock

            val updatedSession = target.copy(
                scheduledEpochMillis = newEpochMillis,
                snoozedUntilEpochMillis = null,
                isCompleted = false
            )

            val updatedSessions = current.sessions.map {
                if (it.id == sessionId) updatedSession else it
            }

            ScheduleAlarmScheduler.cancelAlarm(appContext, target)
            persistToDisk(current.copy(sessions = updatedSessions))
            ScheduleAlarmScheduler.scheduleAlarm(appContext, updatedSession)
        }
    }

    suspend fun updateBookRelativePath(sessionId: String, newRelativePath: String): Unit = withContext(Dispatchers.IO) {
        mutex.withLock {
            val current = _scheduleState.value
            val target = current.sessions.find { it.id == sessionId } ?: return@withLock
            val slug = File(newRelativePath).nameWithoutExtension
            if (target.bookSlug == slug) return@withLock

            val updatedSession = target.copy(bookSlug = slug)
            val updatedSessions = current.sessions.map {
                if (it.id == sessionId) updatedSession else it
            }
            persistToDisk(current.copy(sessions = updatedSessions))
        }
    }

    suspend fun updateWeeklyGoal(weeklyGoalPages: Int): Unit = withContext(Dispatchers.IO) {
        mutex.withLock {
            val current = _scheduleState.value
            val updated = current.copy(weeklyGoalPages = weeklyGoalPages.coerceAtLeast(10))
            persistToDisk(updated)
        }
    }

    suspend fun updateWeeklyGoalMinutes(weeklyGoalMinutes: Int): Unit = withContext(Dispatchers.IO) {
        mutex.withLock {
            val current = _scheduleState.value
            val updated = current.copy(weeklyGoalMinutes = weeklyGoalMinutes.coerceAtLeast(30))
            persistToDisk(updated)
        }
    }

    suspend fun deleteSession(sessionId: String): Unit = withContext(Dispatchers.IO) {
        mutex.withLock {
            val current = _scheduleState.value
            val target = current.sessions.find { it.id == sessionId }
            if (target != null) {
                ScheduleAlarmScheduler.cancelAlarm(appContext, target)
            }
            val updatedSessions = current.sessions.filter { it.id != sessionId }
            persistToDisk(current.copy(sessions = updatedSessions))
        }
    }

    // Weekly Pacing Utilities (Current week Monday 00:00 to Sunday 23:59:59)
    fun getWeekBounds(): Pair<Long, Long> {
        val cal = Calendar.getInstance().apply {
            firstDayOfWeek = Calendar.MONDAY
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }
        val startOfWeek = cal.timeInMillis

        cal.add(Calendar.DAY_OF_WEEK, 6)
        cal.set(Calendar.HOUR_OF_DAY, 23)
        cal.set(Calendar.MINUTE, 59)
        cal.set(Calendar.SECOND, 59)
        cal.set(Calendar.MILLISECOND, 999)
        val endOfWeek = cal.timeInMillis

        return Pair(startOfWeek, endOfWeek)
    }

    fun getCurrentWeekKey(): String {
        val cal = Calendar.getInstance().apply {
            firstDayOfWeek = Calendar.MONDAY
        }
        val year = cal.get(Calendar.YEAR)
        val week = cal.get(Calendar.WEEK_OF_YEAR)
        return "${year}_W${week}"
    }

    suspend fun recordExtraPagesRead(pages: Int): Unit = withContext(Dispatchers.IO) {
        if (pages <= 0) return@withContext
        mutex.withLock {
            val current = _scheduleState.value
            val weekKey = getCurrentWeekKey()
            val currentExtra = current.extraWeeklyPages[weekKey] ?: 0
            val updatedMap = current.extraWeeklyPages.toMutableMap().apply {
                put(weekKey, currentExtra + pages)
            }
            val updated = current.copy(
                updatedAt = System.currentTimeMillis(),
                extraWeeklyPages = updatedMap
            )
            persistToDisk(updated)
            Log.d(TAG, "Recorded $pages extra pages read this week ($weekKey), new total: ${currentExtra + pages}")
        }
    }

    fun getWeeklyCompletedPages(
        sessions: List<ReadingSession> = _scheduleState.value.sessions,
        extraPages: Int? = null
    ): Int {
        val (startOfWeek, endOfWeek) = getWeekBounds()
        val sessionPages = sessions
            .filter { (it.completedEpochMillis ?: it.scheduledEpochMillis) in startOfWeek..endOfWeek }
            .sumOf {
                if (it.isCompleted) {
                    if (it.actualPagesRead > 0) it.actualPagesRead else it.totalPages
                } else {
                    it.actualPagesRead
                }
            }
        val weekKey = getCurrentWeekKey()
        val extra = extraPages ?: (_scheduleState.value.extraWeeklyPages[weekKey] ?: 0)
        return sessionPages + extra
    }

    fun getWeeklyPlannedPages(sessions: List<ReadingSession> = _scheduleState.value.sessions): Int {
        val (startOfWeek, endOfWeek) = getWeekBounds()
        return sessions
            .filter { it.scheduledEpochMillis in startOfWeek..endOfWeek }
            .sumOf { it.totalPages }
    }

    fun getWeeklyCompletedMinutes(sessions: List<ReadingSession> = _scheduleState.value.sessions): Int {
        val (startOfWeek, endOfWeek) = getWeekBounds()
        return sessions
            .filter { it.isCompleted && (it.completedEpochMillis ?: it.scheduledEpochMillis) in startOfWeek..endOfWeek }
            .sumOf { if (it.actualDurationMinutes > 0) it.actualDurationMinutes else it.plannedDurationMinutes }
    }

    fun getWeeklyPlannedMinutes(sessions: List<ReadingSession> = _scheduleState.value.sessions): Int {
        val (startOfWeek, endOfWeek) = getWeekBounds()
        return sessions
            .filter { it.scheduledEpochMillis in startOfWeek..endOfWeek }
            .sumOf { it.plannedDurationMinutes }
    }
}
