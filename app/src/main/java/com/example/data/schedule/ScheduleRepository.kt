package com.example.data.schedule

import android.content.Context
import android.os.Build
import android.util.Log
import com.example.data.schedule.model.ReadingSession
import com.example.data.schedule.model.ScheduleVaultFile
import com.example.util.ScheduleAlarmScheduler
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
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
import java.util.Calendar

import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import java.lang.reflect.Type
import java.util.UUID

class ScheduleRepository private constructor(private val appContext: Context) {

    companion object {
        private const val TAG = "ScheduleRepository"
        private const val SCHEDULE_DIR_NAME = ".mulberry"
        private const val SCHEDULE_FILE_NAME = "schedule.json"
        private const val BACKUP_FILE_NAME = "schedule.json.bak"
        private const val TMP_FILE_NAME = "schedule.json.tmp"

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
            val version = obj.get("version")?.let { if (it.isJsonPrimitive) runCatching { it.asInt }.getOrDefault(1) else 1 } ?: 1
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
                            Log.w(TAG, "Skipping invalid session item in schedule.json", e)
                        }
                    }
                }
            }
            return ScheduleVaultFile(
                version = version,
                weeklyGoalPages = weeklyGoalPages,
                sessions = sessionsList
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
            return ReadingSession(
                id = obj.get("id")?.let { runCatching { it.asString }.getOrNull() } ?: UUID.randomUUID().toString(),
                vaultPath = obj.get("vaultPath")?.let { runCatching { it.asString }.getOrNull() } ?: "",
                bookFileName = obj.get("bookFileName")?.let { runCatching { it.asString }.getOrNull() } ?: "",
                bookRelativePath = obj.get("bookRelativePath")?.let { runCatching { it.asString }.getOrNull() } ?: "",
                bookTitle = obj.get("bookTitle")?.let { runCatching { it.asString }.getOrNull() } ?: "",
                startPage = obj.get("startPage")?.let { runCatching { it.asInt }.getOrDefault(1) } ?: 1,
                endPage = obj.get("endPage")?.let { runCatching { it.asInt }.getOrDefault(1) } ?: 1,
                scheduledEpochMillis = obj.get("scheduledEpochMillis")?.let { runCatching { it.asLong }.getOrDefault(0L) } ?: 0L,
                reminderMinutesBefore = obj.get("reminderMinutesBefore")?.let { runCatching { it.asInt }.getOrDefault(15) } ?: 15,
                microNote = obj.get("microNote")?.let { runCatching { it.asString }.getOrNull() } ?: "",
                isCompleted = obj.get("isCompleted")?.let { runCatching { it.asBoolean }.getOrDefault(false) } ?: false,
                completedEpochMillis = obj.get("completedEpochMillis")?.let { if (it.isJsonPrimitive) runCatching { it.asLong }.getOrNull() else null },
                actualPagesRead = obj.get("actualPagesRead")?.let { runCatching { it.asInt }.getOrDefault(0) } ?: 0
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
    private var loadJob: Job? = null

    private var activeVaultPath: String? = null

    private val _scheduleState = MutableStateFlow(ScheduleVaultFile())
    val scheduleState: StateFlow<ScheduleVaultFile> = _scheduleState.asStateFlow()

    init {
        // Auto-discover initial active vault from shared preferences
        val prefs = appContext.getSharedPreferences("mulberry_prefs", Context.MODE_PRIVATE)
        val savedVaultPath = prefs.getString("active_vault_path", null)
        if (!savedVaultPath.isNullOrBlank()) {
            setActiveVaultPath(savedVaultPath)
        }
    }

    fun setActiveVaultPath(vaultPath: String?) {
        if (vaultPath.isNullOrBlank()) return
        if (activeVaultPath == vaultPath) return

        loadJob?.cancel()

        val previousSessions = _scheduleState.value.sessions
        // Deregister alarms belonging to outgoing vault
        previousSessions.filter { !it.isCompleted }.forEach {
            ScheduleAlarmScheduler.cancelAlarm(appContext, it)
        }

        activeVaultPath = vaultPath
        appContext.getSharedPreferences("mulberry_prefs", Context.MODE_PRIVATE)
            .edit()
            .putString("active_vault_path", vaultPath)
            .apply()

        loadJob = scope.launch {
            runCatching { loadSchedule() }
        }
    }

    fun getActiveVaultPath(): String? = activeVaultPath

    suspend fun loadSchedule(): ScheduleVaultFile = withContext(Dispatchers.IO) {
        mutex.withLock {
            val root = activeVaultPath
            if (root.isNullOrBlank()) {
                val empty = ScheduleVaultFile()
                _scheduleState.value = empty
                return@withLock empty
            }

            val scheduleFile = getScheduleFile(root)
            val backupFile = getBackupFile(root)

            var parsed: ScheduleVaultFile? = null

            if (scheduleFile.exists()) {
                try {
                    val json = scheduleFile.readText()
                    if (json.isNotBlank()) {
                        parsed = gson.fromJson(json, ScheduleVaultFile::class.java)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Corrupted schedule.json detected, isolating file and falling back", e)
                    // 1. Rename damaged file to isolate it
                    try {
                        val corruptFile = File(
                            scheduleFile.parentFile,
                            "schedule.json.corrupted_${System.currentTimeMillis()}"
                        )
                        scheduleFile.renameTo(corruptFile)
                    } catch (renEx: Exception) {
                        Log.e(TAG, "Failed renaming corrupted schedule file", renEx)
                    }
                }
            }

            // Fallback to backup if primary failed or was corrupted
            if (parsed == null && backupFile.exists()) {
                try {
                    val json = backupFile.readText()
                    if (json.isNotBlank()) {
                        parsed = gson.fromJson(json, ScheduleVaultFile::class.java)
                        Log.i(TAG, "Successfully restored schedule from .bak file")
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to restore from .bak file", e)
                }
            }

            var hadRawMaps = false
            val cleanSessions = mutableListOf<ReadingSession>()
            val rawList = (parsed?.sessions as? List<*>) ?: emptyList<Any>()
            for (item in rawList) {
                when (item) {
                    is ReadingSession -> cleanSessions.add(item)
                    is Map<*, *> -> {
                        hadRawMaps = true
                        try {
                            val jsonTree = gson.toJsonTree(item)
                            val s = gson.fromJson(jsonTree, ReadingSession::class.java)
                            if (s != null) cleanSessions.add(s)
                        } catch (e: Exception) {
                            Log.w(TAG, "Failed recovering session from map", e)
                        }
                    }
                    else -> {
                        Log.w(TAG, "Skipping unrecognized session type: ${item?.javaClass?.name}")
                    }
                }
            }

            val finalState = ScheduleVaultFile(
                version = parsed?.version ?: 1,
                weeklyGoalPages = parsed?.weeklyGoalPages ?: 200,
                sessions = cleanSessions
            )
            _scheduleState.value = finalState

            // If we recovered with an empty file due to corruption or absence, or cleaned legacy maps, save clean file to disk
            if ((parsed == null && !scheduleFile.exists()) || hadRawMaps) {
                runCatching { persistToDisk(finalState) }
            }

            // Re-arm uncompleted future alarms for this vault
            ScheduleAlarmScheduler.rescheduleAllPendingAlarms(appContext, finalState.sessions)

            finalState
        }
    }

    private suspend fun persistToDisk(data: ScheduleVaultFile) = withContext(Dispatchers.IO) {
        val root = activeVaultPath ?: return@withContext
        val scheduleFile = getScheduleFile(root)
        val backupFile = getBackupFile(root)
        val tmpFile = getTmpFile(root)

        try {
            // Ensure parent directory (.mulberry) exists
            val dir = scheduleFile.parentFile
            if (dir != null && !dir.exists()) {
                dir.mkdirs()
            }

            val json = gson.toJson(data)

            // 1. Write to temporary file with file descriptor sync
            FileOutputStream(tmpFile).use { fos ->
                fos.write(json.toByteArray(Charsets.UTF_8))
                fos.flush()
                fos.fd.sync()
            }

            // 2. Rotate current file to .bak if it exists
            if (scheduleFile.exists()) {
                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        Files.copy(
                            scheduleFile.toPath(),
                            backupFile.toPath(),
                            StandardCopyOption.REPLACE_EXISTING
                        )
                    } else {
                        scheduleFile.copyTo(backupFile, overwrite = true)
                    }
                } catch (e: Exception) {
                    Log.w(TAG, "Failed creating .bak backup copy", e)
                }
            }

            // 3. Atomically move tmp to primary schedule.json
            var moved = false
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                try {
                    Files.move(
                        tmpFile.toPath(),
                        scheduleFile.toPath(),
                        StandardCopyOption.ATOMIC_MOVE,
                        StandardCopyOption.REPLACE_EXISTING
                    )
                    moved = true
                } catch (e: Exception) {
                    Log.w(TAG, "Atomic move failed, falling back to replace copy", e)
                }
            }

            if (!moved) {
                if (scheduleFile.exists()) scheduleFile.delete()
                if (!tmpFile.renameTo(scheduleFile)) {
                    tmpFile.copyTo(scheduleFile, overwrite = true)
                    tmpFile.delete()
                }
            }

            _scheduleState.value = data
            Log.d(TAG, "Saved ${data.sessions.size} reading sessions to ${scheduleFile.absolutePath}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed writing schedule to disk", e)
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

    suspend fun markCompleted(sessionId: String, actualReadPages: Int = 0): Unit = withContext(Dispatchers.IO) {
        mutex.withLock {
            val current = _scheduleState.value
            val target = current.sessions.find { it.id == sessionId } ?: return@withLock
            val now = System.currentTimeMillis()

            val updatedSession = target.copy(
                isCompleted = true,
                completedEpochMillis = now,
                actualPagesRead = if (actualReadPages > 0) actualReadPages else target.totalPages
            )

            val updatedSessions = current.sessions.map {
                if (it.id == sessionId) updatedSession else it
            }

            ScheduleAlarmScheduler.cancelAlarm(appContext, target)
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
                    actualPagesRead = target.totalPages
                )
            } else {
                target.copy(
                    isCompleted = false,
                    completedEpochMillis = null,
                    actualPagesRead = 0
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
            if (target.bookRelativePath == newRelativePath) return@withLock

            val updatedSession = target.copy(bookRelativePath = newRelativePath)
            val updatedSessions = current.sessions.map {
                if (it.id == sessionId) updatedSession else it
            }
            persistToDisk(current.copy(sessions = updatedSessions))
            Log.d(TAG, "Updated bookRelativePath for session $sessionId to $newRelativePath")
        }
    }

    suspend fun updateWeeklyGoal(weeklyGoalPages: Int): Unit = withContext(Dispatchers.IO) {
        mutex.withLock {
            val current = _scheduleState.value
            val updated = current.copy(weeklyGoalPages = weeklyGoalPages.coerceAtLeast(10))
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

    fun getWeeklyCompletedPages(sessions: List<*> = _scheduleState.value.sessions): Int {
        val (startOfWeek, endOfWeek) = getWeekBounds()
        return sessions.filterIsInstance<ReadingSession>()
            .filter { it.isCompleted && (it.completedEpochMillis ?: it.scheduledEpochMillis) in startOfWeek..endOfWeek }
            .sumOf { if (it.actualPagesRead > 0) it.actualPagesRead else it.totalPages }
    }

    fun getWeeklyPlannedPages(sessions: List<*> = _scheduleState.value.sessions): Int {
        val (startOfWeek, endOfWeek) = getWeekBounds()
        return sessions.filterIsInstance<ReadingSession>()
            .filter { it.scheduledEpochMillis in startOfWeek..endOfWeek }
            .sumOf { it.totalPages }
    }

    private fun getScheduleFile(root: String): File {
        val dir = File(root, SCHEDULE_DIR_NAME)
        return File(dir, SCHEDULE_FILE_NAME)
    }

    private fun getBackupFile(root: String): File {
        val dir = File(root, SCHEDULE_DIR_NAME)
        return File(dir, BACKUP_FILE_NAME)
    }

    private fun getTmpFile(root: String): File {
        val dir = File(root, SCHEDULE_DIR_NAME)
        return File(dir, TMP_FILE_NAME)
    }
}
