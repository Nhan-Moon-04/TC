package com.nhandev.notificationfix.domain.model

/**
 * Real-time permission/battery status of one app.
 * Includes HyperOS-specific MILLET and Greezer status.
 */
data class AppStatus(
    val packageName: String,
    // appops
    val wakeLockAllowed: Boolean        = false,
    val runInBackgroundAllowed: Boolean = false,
    val autoStartAllowed: Boolean       = false,
    // Doze whitelist
    val dozeWhitelisted: Boolean        = false,
    // Battery restriction
    val batteryUnrestricted: Boolean    = false,
    // Notification channel enabled
    val notificationsEnabled: Boolean   = false,
    // HyperOS MILLET_NO_RESTRICT_APP membership (critical for Greezer bypass)
    val milletProtected: Boolean        = false,
    // Standby bucket = active
    val standbyActive: Boolean          = false,
) {
    /** True when all critical settings are applied (including MILLET + Autostart + Standby). */
    val isFullyFixed: Boolean
        get() = wakeLockAllowed && runInBackgroundAllowed && dozeWhitelisted
                && autoStartAllowed && milletProtected

    val fixedCount: Int
        get() = listOf(
            wakeLockAllowed,
            runInBackgroundAllowed,
            dozeWhitelisted,
            autoStartAllowed,
            milletProtected,
        ).count { it }

    val totalCount: Int = 5
}

/** Represents a single fix/restore action in the log. */
data class FixLogEntry(
    val id: Long = 0,
    val packageName: String,
    val appLabel: String,
    val action: FixAction,
    val success: Boolean,
    val message: String = "",
    val timestamp: Long = System.currentTimeMillis(),
)

enum class FixAction {
    FIX_WAKE_LOCK,
    FIX_RUN_IN_BG,
    FIX_AUTO_START,
    FIX_DOZE_WHITELIST,
    FIX_MILLET_PROTECT,
    FIX_GREEZER_LM,
    FIX_NON_FCM_PROTECT,
    FIX_DEVICE_PROFILE_OPTIMIZE,
    FIX_ZALO_VOIP,
    FIX_SELF_IMMUNITY,
    START_WATCHDOG_DAEMON,
    RESTORE_WAKE_LOCK,
    RESTORE_RUN_IN_BG,
    RESTORE_AUTO_START,
    RESTORE_DOZE_WHITELIST,
    FIX_ALL,
    RESTORE_ALL,
}

/** Overall privilege mode available on the device. */
enum class PrivilegeMode {
    NONE,       // nothing — ADB-only guide will show
    SHIZUKU,    // Shizuku connected
    ROOT,       // Root (Magisk/KSU) available
}
