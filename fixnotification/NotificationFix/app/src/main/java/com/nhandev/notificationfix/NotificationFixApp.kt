package com.nhandev.notificationfix

import android.app.Application
import com.topjohnwu.superuser.Shell
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class NotificationFixApp : Application() {

    override fun onCreate() {
        super.onCreate()

        // Init libsu root shell — non-blocking, will succeed only if device is rooted
        Shell.enableVerboseLogging = false
        Shell.setDefaultBuilder(
            Shell.Builder.create()
                .setFlags(Shell.FLAG_REDIRECT_STDERR)
                .setTimeout(10)
        )

        // Auto-start immortal Watcher service and schedule backup alarm
        try {
            com.nhandev.notificationfix.domain.MilletWatcherService.start(this)
            com.nhandev.notificationfix.domain.WatchdogAlarmReceiver.schedulePeriodicCheck(this)
        } catch (e: Exception) {
            // Log silently
        }
    }
}
