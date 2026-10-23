package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.schedule.ScheduleRepository
import com.example.data.schedule.model.ReadingSession
import com.example.data.schedule.model.ScheduleVaultFile
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.io.File

@RunWith(RobolectricTestRunner::class)
class ScheduleRepositoryTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var context: Context
    private lateinit var repository: ScheduleRepository
    private lateinit var mockVaultDir: File

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        mockVaultDir = tempFolder.newFolder("TestVault")
        repository = ScheduleRepository.getInstance(context)
        repository.setActiveVaultPath(mockVaultDir.absolutePath)
    }

    @Test
    fun readingSession_metricsCalculatedCorrectly() {
        val session = ReadingSession(
            bookSlug = "guyton_physiology",
            bookTitle = "Guyton Physiology",
            startPage = 214,
            endPage = 230,
            plannedDurationMinutes = 34,
            scheduledEpochMillis = 1700000000000L,
            reminderMinutesBefore = 15,
            microNote = "Aortic valve murmurs"
        )

        assertEquals("guyton_physiology", session.bookSlug)
        assertEquals(17, session.totalPages)
        assertEquals(34, session.estimatedMinutes)
        assertEquals(1700000000000L - (15 * 60 * 1000L), session.targetAlarmEpochMillis)
        assertEquals(session.id.hashCode(), session.alarmRequestCode)
        assertFalse(session.isCompleted)
    }

    @Test
    fun addAndCompleteSession_persistsAtomicallyToDisk() = runBlocking {
        val session = ReadingSession(
            bookSlug = "guyton_physiology",
            bookTitle = "Guyton Physiology",
            startPage = 10,
            endPage = 25,
            plannedDurationMinutes = 30,
            scheduledEpochMillis = System.currentTimeMillis() + 3600000L,
            reminderMinutesBefore = 15
        )

        repository.addSession(session)

        val plannerFile = File(mockVaultDir, "study_planner.json")
        assertTrue("study_planner.json must exist in vault", plannerFile.exists())

        val reloaded = repository.loadSchedule()
        assertEquals(1, reloaded.sessions.size)
        assertEquals("Guyton Physiology", reloaded.sessions[0].bookTitle)
        assertEquals("guyton_physiology", reloaded.sessions[0].bookSlug)

        // Mark completed
        repository.markCompleted(session.id, actualReadPages = 16, actualDurationMinutes = 32)
        val completedState = repository.loadSchedule()
        val completedSession = completedState.sessions.find { it.id == session.id }
        assertNotNull(completedSession)
        assertTrue(completedSession!!.isCompleted)
        assertEquals(16, completedSession.actualPagesRead)
        assertEquals(32, completedSession.actualDurationMinutes)
        assertNotNull(completedSession.completedEpochMillis)

        // Toggle / Reopen
        repository.toggleCompleted(session.id)
        val reopenedState = repository.loadSchedule()
        val reopenedSession = reopenedState.sessions.find { it.id == session.id }!!
        assertFalse(reopenedSession.isCompleted)
        assertNull(reopenedSession.completedEpochMillis)
    }

    @Test
    fun snoozeSession_setsSnoozeWithoutDroppingAlarms() = runBlocking {
        val originalTime = 1700000000000L
        val session = ReadingSession(
            bookSlug = "grays_anatomy",
            bookTitle = "Gray's Anatomy",
            startPage = 1,
            endPage = 20,
            scheduledEpochMillis = originalTime,
            reminderMinutesBefore = 15
        )

        repository.addSession(session)
        val beforeSnooze = System.currentTimeMillis()
        repository.snoozeSession(session.id, snoozeMinutes = 30)

        val updated = repository.loadSchedule()
        val snoozed = updated.sessions.find { it.id == session.id }!!

        // Original scheduledEpochMillis must remain preserved
        assertEquals(originalTime, snoozed.scheduledEpochMillis)
        assertNotNull(snoozed.snoozedUntilEpochMillis)
        assertTrue(snoozed.snoozedUntilEpochMillis!! >= beforeSnooze + (30 * 60_000L))
        // targetAlarmEpochMillis must directly target snoozedUntilEpochMillis
        assertEquals(snoozed.snoozedUntilEpochMillis, snoozed.targetAlarmEpochMillis)
    }

    @Test
    fun bumpSession_delaysBy24Hours() = runBlocking {
        val originalTime = 1700000000000L
        val session = ReadingSession(
            bookSlug = "grays_anatomy",
            bookTitle = "Gray's Anatomy",
            startPage = 1,
            endPage = 20,
            scheduledEpochMillis = originalTime,
            reminderMinutesBefore = 30
        )

        repository.addSession(session)
        repository.bumpSession(session.id, daysToAdd = 1)

        val updated = repository.loadSchedule()
        val bumped = updated.sessions.find { it.id == session.id }!!
        assertEquals(originalTime + (24L * 60L * 60L * 1000L), bumped.scheduledEpochMillis)
        assertFalse(bumped.isCompleted)
    }

    @Test
    fun updateWeeklyGoals_persistsToDisk() = runBlocking {
        repository.updateWeeklyGoal(350)
        repository.updateWeeklyGoalMinutes(540)
        val loaded = repository.loadSchedule()
        assertEquals(350, loaded.weeklyGoalPages)
        assertEquals(540, loaded.weeklyGoalMinutes)
    }

    @Test
    fun corruptedFile_recoversCleanlyWithoutCrashing() = runBlocking {
        val corruptFile = File(mockVaultDir, "study_planner.json")
        corruptFile.writeText("{ this is invalid json syntax ::: broken")

        // Must not throw exception
        val recovered = repository.loadSchedule()
        assertNotNull(recovered)
        assertTrue(recovered.sessions.isEmpty())
        assertEquals(2, recovered.version)
    }
}
