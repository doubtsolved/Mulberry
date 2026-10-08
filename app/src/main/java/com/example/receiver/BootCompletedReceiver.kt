package com.example.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.schedule.ScheduleRepository
import com.example.util.ScheduleAlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class BootCompletedReceiver : BroadcastReceiver() {
    companion object {
        private const val TAG = "BootCompletedReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        if (action == Intent.ACTION_BOOT_COMPLETED || action == Intent.ACTION_MY_PACKAGE_REPLACED) {
            Log.d(TAG, "Device rebooted or package replaced, rescheduling reading session alarms...")
            val repo = ScheduleRepository.getInstance(context)
            CoroutineScope(Dispatchers.IO).launch {
                try {
                    val schedule = repo.loadSchedule()
                    val pendingSessions = schedule.sessions.filterIsInstance<com.example.data.schedule.model.ReadingSession>().filter { !it.isCompleted }
                    ScheduleAlarmScheduler.rescheduleAllPendingAlarms(context, pendingSessions)
                    Log.d(TAG, "Successfully rescheduled ${pendingSessions.size} alarms after boot.")
                } catch (e: Exception) {
                    Log.e(TAG, "Error rescheduling alarms after boot", e)
                }
            }
        }
    }
}
