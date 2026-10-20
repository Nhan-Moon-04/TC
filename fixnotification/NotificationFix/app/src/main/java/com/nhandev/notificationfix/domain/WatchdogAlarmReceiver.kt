package com.nhandev.notificationfix.domain

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import android.util.Log

/**
 * Backup Watchdog Alarm Receiver.
 * Responds to periodic wakeups (every 15 minutes) and onTaskRemoved resurrection intents.
 * Ensures MilletWatcherService is continuously revived even if Android's Out-Of-Memory killer
 * or Xiaomi's aggressive PowerKeeper terminates the process during deep sleep.
 */
class WatchdogAlarmReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "WatchdogAlarm"
        const val ACTION_RESTART_SERVICE = "com.nhandev.notificationfix.ACTION_RESTART_SERVICE"
        const val ACTION_PERIODIC_CHECK  = "com.nhandev.notificationfix.ACTION_PERIODIC_CHECK"
        private const val PERIODIC_INTERVAL_MS = 15 * 60 * 1000L // 15 minutes

        /** Schedule periodic backup check via AlarmManager */
        fun schedulePeriodicCheck(context: Context) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val intent = Intent(context, WatchdogAlarmReceiver::class.java).apply {
                action = ACTION_PERIODIC_CHECK
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                1003,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val triggerAt = SystemClock.elapsedRealtime() + PERIODIC_INTERVAL_MS

            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAt, pendingIntent)
                } else {
                    alarmManager.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAt, pendingIntent)
                }
                Log.d(TAG, "Scheduled next periodic watchdog check in 15 mins")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to schedule periodic check: ${e.message}")
            }
        }
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action
        Log.i(TAG, "WatchdogAlarmReceiver fired: $action — verifying MilletWatcherService...")

        // Always restart/ensure service is alive
        MilletWatcherService.start(context)

        // Reschedule next backup check
        schedulePeriodicCheck(context)
    }
}
