package com.example.util

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import com.example.data.schedule.model.ReadingSession
import com.example.receiver.ReadingReminderReceiver

object ScheduleAlarmScheduler {
    private const val TAG = "ScheduleAlarmScheduler"

    fun scheduleAlarm(context: Context, session: ReadingSession) {
        if (session.isCompleted) return

        val triggerAtMillis = session.targetAlarmEpochMillis ?: return
        val now = System.currentTimeMillis()

        if (triggerAtMillis <= now) {
            Log.d(TAG, "Skipping stale alarm for session ${session.id} (trigger $triggerAtMillis <= $now)")
            return
        }

        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return

        // Cancel any existing alarm for this session code first
        cancelAlarmByCode(context, session.alarmRequestCode)

        val intent = Intent(context, ReadingReminderReceiver::class.java).apply {
            action = ReadingReminderReceiver.ACTION_TRIGGER_REMINDER
            putExtra(ReadingReminderReceiver.EXTRA_SESSION_ID, session.id)
            putExtra(ReadingReminderReceiver.EXTRA_BOOK_SLUG, session.bookSlug)
            putExtra(ReadingReminderReceiver.EXTRA_BOOK_TITLE, session.bookTitle)
            putExtra(ReadingReminderReceiver.EXTRA_START_PAGE, session.startPage)
            putExtra(ReadingReminderReceiver.EXTRA_END_PAGE, session.endPage)
            putExtra(ReadingReminderReceiver.EXTRA_MICRO_NOTE, session.microNote)
            putExtra(ReadingReminderReceiver.EXTRA_SCHEDULED_TIME, session.scheduledEpochMillis)
        }

        val pendingIntent = PendingIntent.getBroadcast(
            context,
            session.alarmRequestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (alarmManager.canScheduleExactAlarms()) {
                    alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                } else {
                    alarmManager.setAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerAtMillis,
                        pendingIntent
                    )
                }
            } else {
                alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            }
            Log.d(TAG, "Scheduled alarm for session ${session.id} (slug: ${session.bookSlug}) at $triggerAtMillis")
        } catch (e: SecurityException) {
            Log.w(TAG, "SecurityException scheduling exact alarm, falling back to setAndAllowWhileIdle", e)
            try {
                alarmManager.setAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    triggerAtMillis,
                    pendingIntent
                )
            } catch (ex: Exception) {
                Log.e(TAG, "Failed fallback alarm scheduling", ex)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error scheduling alarm", e)
        }
    }

    fun cancelAlarm(context: Context, session: ReadingSession) {
        cancelAlarmByCode(context, session.alarmRequestCode)
    }

    fun cancelAlarmByCode(context: Context, alarmRequestCode: Int) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, ReadingReminderReceiver::class.java).apply {
            action = ReadingReminderReceiver.ACTION_TRIGGER_REMINDER
        }
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            alarmRequestCode,
            intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
            Log.d(TAG, "Cancelled alarm request code: $alarmRequestCode")
        }
    }

    fun rescheduleAllPendingAlarms(context: Context, sessions: List<ReadingSession>) {
        sessions.forEach { session ->
            if (!session.isCompleted) {
                scheduleAlarm(context, session)
            }
        }
    }
}
