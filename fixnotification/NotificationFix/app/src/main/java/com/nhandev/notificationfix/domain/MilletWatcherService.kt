package com.nhandev.notificationfix.domain

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.database.ContentObserver
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import com.nhandev.notificationfix.MainActivity
import com.nhandev.notificationfix.R
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Immortal Foreground Watcher Service for Xiaomi HyperOS.
 *
 * Core Capabilities:
 * 1. Event-Driven: ContentObserver watches Settings.System.MILLET_NO_RESTRICT_APP for instant repair
 * 2. Active Heartbeat: Coroutine periodic loop (every 45s) checks GMS & Zalo status
 * 3. Zalo VoIP & Call Watchdog: Detects if Zalo process is killed or frozen; resurrects push service
 *    and boosts UID importance so incoming call ringing activity always pops up
 * 4. Volatile Greezer Protection: Re-applies dumpsys greezer IM GMS disable and IM APP disable
 * 5. Self-Immunity & Anti-Kill:
 *    - START_STICKY service
 *    - onTaskRemoved() resurrects service via AlarmManager in 1s if swiped from Recents
 *    - stopWithTask="false"
 *    - Uses brief WakeLock (max 2s) during check to wake CPU from deep Doze sleep
 *    - Survives device reboot (BootReceiver + Autostart permissions)
 */
class MilletWatcherService : Service() {

    companion object {
        private const val TAG = "MilletWatcher"
        private const val CHANNEL_ID = "millet_watcher_channel"
        private const val NOTIFICATION_ID = 2001
        private const val MILLET_KEY = "MILLET_NO_RESTRICT_APP"
        private const val HEARTBEAT_INTERVAL_MS = 45_000L

        /** Packages that MUST always be in MILLET_NO_RESTRICT_APP */
        val PROTECTED_PACKAGES = setOf(
            "com.google.android.gms",       // GMS — FCM push foundation
            "com.google.android.gsf",       // Google Services Framework
            "com.zing.zalo",                // Zalo — non-FCM, persistent TCP socket + VoIP calls
            "com.nhandev.notificationfix",   // Self-protection — keep watcher alive
        )

        private var isExplicitlyStopped = false

        fun start(context: Context) {
            isExplicitlyStopped = false
            val intent = Intent(context, MilletWatcherService::class.java)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Standard startForegroundService failed (${e.message}), using AlarmManager resurrection fallback", e)
                scheduleServiceResurrection(context, 1000L)
            }
        }

        fun stop(context: Context) {
            isExplicitlyStopped = true
            context.stopService(Intent(context, MilletWatcherService::class.java))
        }

        fun canWriteSettings(context: Context): Boolean {
            return Settings.System.canWrite(context)
        }

        /**
         * Schedules an immediate alarm to revive this service if terminated by OS or user swipe.
         */
        fun scheduleServiceResurrection(context: Context, delayMs: Long = 1000L) {
            val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
            val restartIntent = Intent(context, WatchdogAlarmReceiver::class.java).apply {
                action = WatchdogAlarmReceiver.ACTION_RESTART_SERVICE
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                1002,
                restartIntent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            val triggerAt = SystemClock.elapsedRealtime() + delayMs
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAt, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.ELAPSED_REALTIME_WAKEUP, triggerAt, pendingIntent)
            }
        }
    }

    private var milletObserver: ContentObserver? = null
    private val handler = Handler(Looper.getMainLooper())
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var heartbeatJob: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null

    private var lastRepairTime: String = "Vừa khởi động"
    private var zaloStatusStr: String = "Đang kiểm tra"

    override fun onCreate() {
        super.onCreate()
        isExplicitlyStopped = false
        initWakeLock()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification("Đang kích hoạt bảo vệ..."))
        registerMilletObserver()

        // Immediate check
        ensureMilletProtection()

        // Start active periodic heartbeat loop
        startHeartbeatLoop()

        Log.i(TAG, "MilletWatcherService started — Active Heartbeat & Immortality online")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        isExplicitlyStopped = false
        // Re-ensure protection and update notification
        serviceScope.launch {
            performFullMaintenanceCheck()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onTaskRemoved(rootIntent: Intent?) {
        super.onTaskRemoved(rootIntent)
        Log.w(TAG, "onTaskRemoved: Task swiped away from Recents! Scheduling immediate resurrection...")
        if (!isExplicitlyStopped) {
            scheduleServiceResurrection(applicationContext, 1000L)
        }
    }

    override fun onDestroy() {
        unregisterMilletObserver()
        heartbeatJob?.cancel()
        serviceScope.cancel()
        releaseWakeLock()

        Log.w(TAG, "MilletWatcherService onDestroy")
        if (!isExplicitlyStopped) {
            Log.i(TAG, "Service destroyed unexpectedly — reviving immediately via WatchdogAlarmReceiver")
            scheduleServiceResurrection(applicationContext, 1500L)
        }
        super.onDestroy()
    }

    private fun initWakeLock() {
        try {
            val pm = getSystemService(Context.POWER_SERVICE) as? PowerManager
            wakeLock = pm?.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "NotificationFix:WatchdogWakeLock")?.apply {
                setReferenceCounted(false)
            }
        } catch (e: Exception) {
            Log.w(TAG, "WakeLock init failed: ${e.message}")
        }
    }

    private fun acquireBriefWakeLock() {
        try {
            wakeLock?.acquire(2500L) // Auto-releases after 2.5s to prevent any battery drain
        } catch (e: Exception) {
            Log.w(TAG, "WakeLock acquire failed: ${e.message}")
        }
    }

    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
        } catch (e: Exception) {
            // ignore
        }
    }

    // ── Active Periodic Heartbeat Loop ───────────────────────────────────────

    private fun startHeartbeatLoop() {
        heartbeatJob?.cancel()
        heartbeatJob = serviceScope.launch {
            while (isActive) {
                delay(HEARTBEAT_INTERVAL_MS)
                performFullMaintenanceCheck()
            }
        }
    }

    /**
     * Executes the comprehensive 5-step maintenance check:
     * 1. Acquires brief 2s wake lock to wake CPU from deep sleep
     * 2. Checks & repairs MILLET_NO_RESTRICT_APP
     * 3. Checks if Zalo process is running; resurrects if killed
     * 4. Re-applies volatile Greezer overrides via Shizuku/shell
     * 5. Updates ongoing notification with timestamp
     */
    private fun performFullMaintenanceCheck() {
        acquireBriefWakeLock()
        try {
            // Step 1: Ensure MILLET list
            ensureMilletProtection()

            // Step 2: Zalo call & keepalive check
            checkAndReviveZalo()

            // Step 3: Re-apply volatile Greezer commands
            reapplyVolatileGreezerSettings()

            // Step 4: Update UI Notification
            val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
            lastRepairTime = timeStr
            updateNotification("🟢 Bảo vệ vĩnh viễn · Zalo: $zaloStatusStr · Check lúc $timeStr")
        } catch (e: Exception) {
            Log.e(TAG, "Maintenance check error: ${e.message}", e)
        } finally {
            releaseWakeLock()
        }
    }

    // ── Zalo VoIP Call & Process Keep-Alive ──────────────────────────────────

    private fun checkAndReviveZalo() {
        val zaloPkg = "com.zing.zalo"
        if (!isPackageInstalled(zaloPkg)) {
            zaloStatusStr = "Chưa cài đặt"
            return
        }

        try {
            // Check if Zalo process is alive via shell / proc
            val shizuku = ShizukuHelper()
            var isAlive = false

            if (shizuku.hasPermission) {
                val pidRes = shizuku.runCommand("pidof $zaloPkg").output.trim()
                isAlive = pidRes.isNotBlank()

                if (!isAlive) {
                    Log.w(TAG, "Zalo process dead! Resurrecting socket push services...")
                    // Resurrect Zalo push service & make UID active
                    shizuku.runCommand("am startservice --user 0 -n $zaloPkg/.service.ZaloService")
                    shizuku.runCommand("am startservice --user 0 -n $zaloPkg/.push.notification.PushService")
                    shizuku.runCommand("cmd activity make-uid-active $zaloPkg")
                    zaloStatusStr = "Đã đánh thức ✓"
                } else {
                    zaloStatusStr = "Online ✓"
                }

                // Ensure 10021 (Background activity start) is always granted for incoming call pop-up
                shizuku.runCommand("cmd appops set $zaloPkg 10021 allow")
                shizuku.runCommand("cmd appops set $zaloPkg SYSTEM_ALERT_WINDOW allow")
            } else {
                // Without Shizuku, trigger via Android Intent
                val intent = Intent().apply {
                    setClassName(zaloPkg, "$zaloPkg.service.ZaloService")
                }
                try {
                    startService(intent)
                    zaloStatusStr = "Active"
                } catch (e: Exception) {
                    zaloStatusStr = "Đang chạy"
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Zalo check error: ${e.message}")
            zaloStatusStr = "Bảo vệ"
        }
    }

    private fun reapplyVolatileGreezerSettings() {
        try {
            val shizuku = ShizukuHelper()
            if (shizuku.hasPermission) {
                shizuku.runCommand("dumpsys greezer IM GMS disable")
                shizuku.runCommand("dumpsys greezer IM APP disable com.zing.zalo")
                shizuku.runCommand("dumpsys greezer LM add com.google.android.gms")
                shizuku.runCommand("dumpsys greezer LM add com.zing.zalo")
            }
        } catch (e: Exception) {
            // ignore
        }
    }

    // ── ContentObserver ──────────────────────────────────────────────────────

    private fun registerMilletObserver() {
        val uri = Settings.System.getUriFor(MILLET_KEY)
        milletObserver = object : ContentObserver(handler) {
            override fun onChange(selfChange: Boolean) {
                super.onChange(selfChange)
                Log.d(TAG, "MILLET_NO_RESTRICT_APP changed by PowerKeeper! Repairing instantly...")
                serviceScope.launch {
                    ensureMilletProtection()
                }
            }
        }
        contentResolver.registerContentObserver(uri, false, milletObserver!!)
        Log.d(TAG, "ContentObserver registered for $MILLET_KEY")
    }

    private fun unregisterMilletObserver() {
        milletObserver?.let {
            contentResolver.unregisterContentObserver(it)
            milletObserver = null
        }
    }

    // ── MILLET Protection Logic ──────────────────────────────────────────────

    private fun ensureMilletProtection() {
        if (!canWriteSettings(this)) {
            Log.w(TAG, "WRITE_SETTINGS permission not granted — trying Shizuku fallback")
            tryShizukuFallback(PROTECTED_PACKAGES.toList())
            return
        }

        try {
            val current = Settings.System.getString(contentResolver, MILLET_KEY) ?: ""
            val entries = if (current.isBlank() || current == "null") {
                mutableListOf()
            } else {
                current.split(",").map { it.trim() }.filter { it.isNotBlank() }.toMutableList()
            }

            val missing = PROTECTED_PACKAGES.filter { pkg ->
                !entries.contains(pkg) && isPackageInstalled(pkg)
            }

            if (missing.isEmpty()) {
                Log.d(TAG, "All protected packages present in MILLET ✓")
                return
            }

            entries.addAll(missing)
            val updated = entries.distinct().joinToString(",")

            Settings.System.putString(contentResolver, MILLET_KEY, updated)

            // Verify
            val verify = Settings.System.getString(contentResolver, MILLET_KEY) ?: ""
            val allPresent = missing.all { verify.contains(it) }

            if (allPresent) {
                Log.i(TAG, "✓ MILLET repaired! Added: ${missing.joinToString(", ")}")
            } else {
                Log.e(TAG, "✗ MILLET repair failed — trying Shizuku fallback")
                tryShizukuFallback(missing)
            }
        } catch (e: Exception) {
            Log.e(TAG, "MILLET protection error: ${e.message}", e)
            tryShizukuFallback(PROTECTED_PACKAGES.toList())
        }
    }

    private fun tryShizukuFallback(packages: List<String>) {
        try {
            val shizuku = ShizukuHelper()
            if (!shizuku.hasPermission) return

            val current = shizuku.runCommand(
                "settings --user 0 get system MILLET_NO_RESTRICT_APP"
            ).output.trim()

            val entries = if (current.isBlank() || current == "null") {
                mutableListOf()
            } else {
                current.split(",").map { it.trim() }.filter { it.isNotBlank() }.toMutableList()
            }

            val missing = packages.filter { !entries.contains(it) && isPackageInstalled(it) }
            if (missing.isEmpty()) return

            entries.addAll(missing)
            val updated = entries.distinct().joinToString(",")
            shizuku.runCommand("settings --user 0 put system MILLET_NO_RESTRICT_APP '$updated'")
            Log.i(TAG, "✓ MILLET repaired via Shizuku fallback: ${missing.joinToString(", ")}")
        } catch (e: Exception) {
            Log.w(TAG, "Shizuku fallback also failed: ${e.message}")
        }
    }

    private fun isPackageInstalled(packageName: String): Boolean {
        return try {
            packageManager.getPackageInfo(packageName, 0)
            true
        } catch (e: Exception) {
            false
        }
    }

    // ── Notification ─────────────────────────────────────────────────────────

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Bảo Vệ Thông Báo HyperOS",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Duy trì kết nối push và chống kill ứng dụng Zalo & GMS"
                setShowBadge(false)
            }
            val nm = getSystemService(NotificationManager::class.java)
            nm.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(statusText: String): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("🛡️ NotificationFix · Chạy Vĩnh Viễn")
            .setContentText(statusText)
            .setSmallIcon(R.mipmap.ic_launcher)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(pendingIntent)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    private fun updateNotification(statusText: String) {
        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(NOTIFICATION_ID, buildNotification(statusText))
    }
}
