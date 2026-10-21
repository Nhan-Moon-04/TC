package com.nhandev.notificationfix.domain

import android.content.Context
import android.content.SharedPreferences

/**
 * Persistent local storage for user-selected protected packages.
 * 
 * Crucial for Bank Mode / Developer Options OFF:
 * When Developer Options and Shizuku are disabled, MilletWatcherService
 * reads this persistent storage and continuously maintains ALL protected apps
 * inside Settings.System.MILLET_NO_RESTRICT_APP via standard android.permission.WRITE_SETTINGS.
 */
object ProtectedAppsStorage {
    private const val PREFS_NAME = "notification_fix_protected_apps"
    private const val KEY_PROTECTED_PKGS = "protected_packages_set"

    val DEFAULT_SYSTEM_PACKAGES = setOf(
        "com.google.android.gms",        // Google Play Services (FCM Push)
        "com.google.android.gsf",        // Google Services Framework
        "com.zing.zalo",                 // Zalo (Persistent TCP Socket & VoIP Calls)
        "com.nhandev.notificationfix",   // NotificationFix itself (Immortality)
    )

    private fun getPrefs(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Returns the combined set of default system packages and user-selected packages.
     */
    fun getProtectedPackages(context: Context): Set<String> {
        val saved = getPrefs(context).getStringSet(KEY_PROTECTED_PKGS, emptySet()) ?: emptySet()
        return DEFAULT_SYSTEM_PACKAGES + saved
    }

    /**
     * Adds a single package to the persistent protection list.
     */
    fun addPackage(context: Context, packageName: String) {
        val current = getPrefs(context).getStringSet(KEY_PROTECTED_PKGS, emptySet())?.toMutableSet() ?: mutableSetOf()
        current.add(packageName)
        getPrefs(context).edit().putStringSet(KEY_PROTECTED_PKGS, current).apply()
    }

    /**
     * Adds multiple packages to the persistent protection list.
     */
    fun addPackages(context: Context, packages: Collection<String>) {
        val current = getPrefs(context).getStringSet(KEY_PROTECTED_PKGS, emptySet())?.toMutableSet() ?: mutableSetOf()
        current.addAll(packages)
        getPrefs(context).edit().putStringSet(KEY_PROTECTED_PKGS, current).apply()
    }

    /**
     * Removes a package from the custom protection list (cannot remove default system packages).
     */
    fun removePackage(context: Context, packageName: String) {
        val current = getPrefs(context).getStringSet(KEY_PROTECTED_PKGS, emptySet())?.toMutableSet() ?: mutableSetOf()
        current.remove(packageName)
        getPrefs(context).edit().putStringSet(KEY_PROTECTED_PKGS, current).apply()
    }

    /**
     * Gets total count of protected apps.
     */
    fun getProtectedCount(context: Context): Int {
        return getProtectedPackages(context).size
    }
}
