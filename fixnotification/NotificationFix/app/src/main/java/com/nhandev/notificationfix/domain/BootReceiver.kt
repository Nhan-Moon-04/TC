package com.nhandev.notificationfix.domain

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

/**
 * Starts MilletWatcherService on device boot and user unlock.
 * 
 * Flow after reboot:
 * 1. Android sends BOOT_COMPLETED / LOCKED_BOOT_COMPLETED broadcast
 * 2. BootReceiver fires → starts MilletWatcherService immediately
 * 3. Schedules periodic AlarmManager watchdog checks
 * 4. Ensures GMS + Zalo are protected from the moment the device powers on
 */
class BootReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        val action = intent.action ?: return
        Log.i(TAG, "BootReceiver triggered with action: $action")

        when (action) {
            Intent.ACTION_BOOT_COMPLETED,
            Intent.ACTION_LOCKED_BOOT_COMPLETED,
            "android.intent.action.QUICKBOOT_POWERON",
            "com.htc.intent.action.QUICKBOOT_POWERON",
            Intent.ACTION_MY_PACKAGE_REPLACED,
            Intent.ACTION_USER_PRESENT -> {
                Log.i(TAG, "Device booted/unlocked — starting MilletWatcherService and scheduling backup alarm")
                MilletWatcherService.start(context)
                WatchdogAlarmReceiver.schedulePeriodicCheck(context)
            }
        }
    }
}
