package com.nhandev.notificationfix.domain

import android.app.NotificationManager
import android.content.Context
import com.nhandev.notificationfix.domain.model.AppStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Reads the current permission/battery/notification status of an app.
 * Uses Shizuku or Root to run privileged shell commands.
 *
 * Now includes HyperOS-specific checks:
 * - MILLET_NO_RESTRICT_APP membership (critical for Greezer bypass)
 * - Standby bucket = active
 */
@Singleton
class StatusChecker @Inject constructor(
    @ApplicationContext private val context: Context,
    private val shizuku: ShizukuHelper,
    private val root: RootHelper,
) {

    private fun run(cmd: String): String {
        return when {
            shizuku.hasPermission -> shizuku.runCommand(cmd).output
            root.isAvailable      -> root.runCommand(cmd).output
            else -> ""
        }
    }

    /**
     * Check status of a single package.
     * Returns AppStatus with all fields filled including HyperOS-specific MILLET status.
     */
    suspend fun checkApp(packageName: String): AppStatus {
        val wakeLock  = checkOps(packageName, "WAKE_LOCK")
        val runInBg   = checkOps(packageName, "RUN_ANY_IN_BACKGROUND")
        val autoStart = checkOps(packageName, "10008") || checkOps(packageName, "AUTO_START")
        val doze      = checkDozeWhitelist(packageName)
        val notif     = checkNotificationsEnabled(packageName)
        val millet    = checkMilletProtected(packageName)
        val standby   = checkStandbyActive(packageName)

        return AppStatus(
            packageName             = packageName,
            wakeLockAllowed         = wakeLock,
            runInBackgroundAllowed  = runInBg,
            autoStartAllowed        = autoStart,
            dozeWhitelisted         = doze,
            notificationsEnabled    = notif,
            milletProtected         = millet,
            standbyActive           = standby,
        )
    }

    /**
     * Check multiple packages at once.
     * Caches shared queries (whitelist, MILLET list) for efficiency.
     */
    suspend fun checkApps(packages: List<String>): Map<String, AppStatus> {
        // Cache shared queries once for efficiency
        val whitelistOutput = run("dumpsys deviceidle whitelist 2>/dev/null")
        val milletOutput    = run("settings --user 0 get system MILLET_NO_RESTRICT_APP 2>/dev/null")
        val milletEntries   = parseMilletEntries(milletOutput)

        return packages.associateWith { pkg ->
            val wakeLock  = checkOps(pkg, "WAKE_LOCK")
            val runInBg   = checkOps(pkg, "RUN_ANY_IN_BACKGROUND")
            val autoStart = checkOps(pkg, "10008") || checkOps(pkg, "AUTO_START")
            val doze      = whitelistOutput.contains(pkg)
            val notif     = checkNotificationsEnabled(pkg)
            val millet    = milletEntries.contains(pkg)
            val standby   = checkStandbyActive(pkg)
            AppStatus(
                packageName             = pkg,
                wakeLockAllowed         = wakeLock,
                runInBackgroundAllowed  = runInBg,
                autoStartAllowed        = autoStart,
                dozeWhitelisted         = doze,
                notificationsEnabled    = notif,
                milletProtected         = millet,
                standbyActive           = standby,
            )
        }
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    /** Returns true if appops for the given op is ALLOW. */
    private fun checkOps(pkg: String, op: String): Boolean {
        val out = run("cmd appops get $pkg $op 2>/dev/null")
        return out.contains("allow", ignoreCase = true)
    }

    private fun checkDozeWhitelist(pkg: String): Boolean {
        val out = run("dumpsys deviceidle whitelist 2>/dev/null")
        return out.contains(pkg)
    }

    /** Check if package is in Settings.System.MILLET_NO_RESTRICT_APP */
    private fun checkMilletProtected(pkg: String): Boolean {
        val out = run("settings --user 0 get system MILLET_NO_RESTRICT_APP 2>/dev/null")
        return parseMilletEntries(out).contains(pkg)
    }

    /** Parse MILLET_NO_RESTRICT_APP comma-separated list exactly as Greezer does */
    private fun parseMilletEntries(raw: String): Set<String> {
        val trimmed = raw.trim()
        if (trimmed.isBlank() || trimmed == "null") return emptySet()
        return trimmed.split(",").map { it.trim() }.filter { it.isNotBlank() }.toSet()
    }

    /** Check if standby bucket is active (highest priority) */
    private fun checkStandbyActive(pkg: String): Boolean {
        val out = run("am get-standby-bucket $pkg 2>/dev/null")
        // "active" = 5, which is the highest priority bucket
        return out.trim().equals("active", ignoreCase = true) ||
               out.trim() == "5"
    }

    private fun checkNotificationsEnabled(pkg: String): Boolean {
        return try {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            nm.areNotificationsEnabledForPackage(pkg, 0)
        } catch (e: Exception) {
            // Fallback: assume enabled if API not available
            true
        }
    }
}

// Extension to call areNotificationsEnabledForPackage safely
private fun NotificationManager.areNotificationsEnabledForPackage(
    pkg: String,
    uid: Int
): Boolean {
    return try {
        val method = NotificationManager::class.java.getDeclaredMethod(
            "areNotificationsEnabledForPackage", String::class.java, Int::class.java
        )
        method.isAccessible = true
        method.invoke(this, pkg, uid) as? Boolean ?: true
    } catch (e: Exception) {
        true
    }
}
