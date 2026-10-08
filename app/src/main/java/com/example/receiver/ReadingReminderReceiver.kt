package com.example.receiver

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.R
import com.example.data.schedule.ScheduleRepository
import com.example.ui.reader.ReaderActivity
import com.example.util.ScheduleAlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class ReadingReminderReceiver : BroadcastReceiver() {

    companion object {
        const val CHANNEL_ID = "mulberry_reading_reminders"
        const val CHANNEL_NAME = "Reading Reminders"

        const val ACTION_TRIGGER_REMINDER = "com.example.ACTION_TRIGGER_REMINDER"
        const val ACTION_SNOOZE_SESSION = "com.example.ACTION_SNOOZE_SESSION"

        const val EXTRA_SESSION_ID = "EXTRA_SESSION_ID"
        const val EXTRA_VAULT_PATH = "EXTRA_VAULT_PATH"
        const val EXTRA_BOOK_FILE_NAME = "EXTRA_BOOK_FILE_NAME"
        const val EXTRA_BOOK_TITLE = "EXTRA_BOOK_TITLE"
        const val EXTRA_START_PAGE = "EXTRA_START_PAGE"
        const val EXTRA_END_PAGE = "EXTRA_END_PAGE"
        const val EXTRA_MICRO_NOTE = "EXTRA_MICRO_NOTE"
        const val EXTRA_SCHEDULED_TIME = "EXTRA_SCHEDULED_TIME"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        val sessionId = intent.getStringExtra(EXTRA_SESSION_ID) ?: return
        val vaultPath = intent.getStringExtra(EXTRA_VAULT_PATH) ?: ""
        val bookFileName = intent.getStringExtra(EXTRA_BOOK_FILE_NAME) ?: ""
        val bookTitle = intent.getStringExtra(EXTRA_BOOK_TITLE) ?: "Reading Session"
        val startPage = intent.getIntExtra(EXTRA_START_PAGE, 1)
        val endPage = intent.getIntExtra(EXTRA_END_PAGE, 1)
        val microNote = intent.getStringExtra(EXTRA_MICRO_NOTE) ?: ""
        val scheduledTime = intent.getLongExtra(EXTRA_SCHEDULED_TIME, System.currentTimeMillis())

        when (action) {
            ACTION_TRIGGER_REMINDER -> {
                showReminderNotification(
                    context = context,
                    sessionId = sessionId,
                    vaultPath = vaultPath,
                    bookFileName = bookFileName,
                    bookTitle = bookTitle,
                    startPage = startPage,
                    endPage = endPage,
                    microNote = microNote
                )
            }
            ACTION_SNOOZE_SESSION -> {
                // Cancel active notification
                val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
                notificationManager?.cancel(sessionId.hashCode())

                // Postpone session in repository & reschedule alarm in 30 minutes
                val repo = ScheduleRepository.getInstance(context)
                if (vaultPath.isNotBlank()) {
                    repo.setActiveVaultPath(vaultPath)
                }
                CoroutineScope(Dispatchers.IO).launch {
                    val snoozedTime = System.currentTimeMillis() + (30 * 60 * 1000L)
                    repo.postponeSessionTo(sessionId, snoozedTime)
                }
            }
        }
    }

    private fun showReminderNotification(
        context: Context,
        sessionId: String,
        vaultPath: String,
        bookFileName: String,
        bookTitle: String,
        startPage: Int,
        endPage: Int,
        microNote: String
    ) {
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            ?: return

        // Create High Importance channel if needed
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for scheduled reading and study sessions"
                enableVibration(true)
                setShowBadge(true)
            }
            notificationManager.createNotificationChannel(channel)
        }

        // Resolve absolute book path
        val fullBookPath = if (bookFileName.startsWith("/")) {
            bookFileName
        } else if (vaultPath.isNotBlank()) {
            File(vaultPath, bookFileName).absolutePath
        } else {
            bookFileName
        }

        // Direct Action 1: Read Now PendingIntent
        val readIntent = Intent(context, ReaderActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra(ReaderActivity.EXTRA_BOOK_PATH, fullBookPath)
            putExtra(ReaderActivity.EXTRA_BOOK_TITLE, bookTitle)
            putExtra(ReaderActivity.EXTRA_START_PAGE, startPage)
            putExtra(ReaderActivity.EXTRA_SESSION_ID, sessionId)
        }
        val readPendingIntent = PendingIntent.getActivity(
            context,
            sessionId.hashCode(),
            readIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // Direct Action 2: Snooze 30 Min PendingIntent
        val snoozeIntent = Intent(context, ReadingReminderReceiver::class.java).apply {
            action = ACTION_SNOOZE_SESSION
            putExtra(EXTRA_SESSION_ID, sessionId)
            putExtra(EXTRA_VAULT_PATH, vaultPath)
            putExtra(EXTRA_BOOK_FILE_NAME, bookFileName)
            putExtra(EXTRA_BOOK_TITLE, bookTitle)
            putExtra(EXTRA_START_PAGE, startPage)
            putExtra(EXTRA_END_PAGE, endPage)
            putExtra(EXTRA_MICRO_NOTE, microNote)
        }
        val snoozePendingIntent = PendingIntent.getBroadcast(
            context,
            sessionId.hashCode() + 1,
            snoozeIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val pageSpan = "p. $startPage – $endPage"
        val subtitle = if (microNote.isNotBlank()) {
            "$pageSpan • \"$microNote\""
        } else {
            pageSpan
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setContentTitle(bookTitle)
            .setContentText("Target: $subtitle")
            .setStyle(NotificationCompat.BigTextStyle().bigText("Target: $subtitle\nTime for your scheduled study session."))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(readPendingIntent)
            .addAction(
                R.mipmap.ic_launcher,
                "Read Now (p. $startPage)",
                readPendingIntent
            )
            .addAction(
                R.mipmap.ic_launcher,
                "Snooze 30 Min",
                snoozePendingIntent
            )
            .build()

        notificationManager.notify(sessionId.hashCode(), notification)
    }
}
