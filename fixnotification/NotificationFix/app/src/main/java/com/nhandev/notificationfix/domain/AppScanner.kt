package com.nhandev.notificationfix.domain

import android.content.Context
import android.content.pm.ApplicationInfo
import android.content.pm.PackageManager
import com.nhandev.notificationfix.domain.model.ALL_PRESETS
import com.nhandev.notificationfix.domain.model.AppInfo
import com.nhandev.notificationfix.domain.model.AppPreset
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppScanner @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private val pm: PackageManager = context.packageManager

    /**
     * Returns all user-installed apps (non-system) sorted by label.
     */
    suspend fun getUserApps(): List<AppInfo> = withContext(Dispatchers.IO) {
        pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .filter { it.flags and ApplicationInfo.FLAG_SYSTEM == 0 }
            .map { ai ->
                AppInfo(
                    packageName = ai.packageName,
                    label       = pm.getApplicationLabel(ai).toString(),
                    icon        = pm.getApplicationIcon(ai),
                    isSystemApp = false,
                )
            }
            .sortedBy { it.label.lowercase() }
    }

    /**
     * Returns all apps (user + system) sorted by label.
     */
    suspend fun getAllApps(): List<AppInfo> = withContext(Dispatchers.IO) {
        pm.getInstalledApplications(PackageManager.GET_META_DATA)
            .map { ai ->
                AppInfo(
                    packageName = ai.packageName,
                    label       = pm.getApplicationLabel(ai).toString(),
                    icon        = pm.getApplicationIcon(ai),
                    isSystemApp = ai.flags and ApplicationInfo.FLAG_SYSTEM != 0,
                )
            }
            .sortedBy { it.label.lowercase() }
    }

    /**
     * Returns apps that match the given preset, filtered to only those installed on device.
     */
    suspend fun getPresetApps(preset: AppPreset): List<AppInfo> = withContext(Dispatchers.IO) {
        preset.packages.mapNotNull { pkg ->
            try {
                val ai = pm.getApplicationInfo(pkg, 0)
                AppInfo(
                    packageName = pkg,
                    label       = pm.getApplicationLabel(ai).toString(),
                    icon        = pm.getApplicationIcon(ai),
                    isSystemApp = ai.flags and ApplicationInfo.FLAG_SYSTEM != 0,
                )
            } catch (e: PackageManager.NameNotFoundException) {
                null // app not installed — skip
            }
        }
    }

    /**
     * Returns apps from ALL presets, deduped, installed on device.
     */
    suspend fun getRecommendedApps(): List<AppInfo> = withContext(Dispatchers.IO) {
        ALL_PRESETS
            .flatMap { preset ->
                preset.packages.mapNotNull { pkg ->
                    try {
                        val ai = pm.getApplicationInfo(pkg, 0)
                        AppInfo(
                            packageName = pkg,
                            label       = pm.getApplicationLabel(ai).toString(),
                            icon        = pm.getApplicationIcon(ai),
                        )
                    } catch (e: PackageManager.NameNotFoundException) { null }
                }
            }
            .distinctBy { it.packageName }
            .sortedBy { it.label.lowercase() }
    }

    /** Get AppInfo for a single package, or null if not installed. */
    fun getAppInfo(packageName: String): AppInfo? {
        return try {
            val ai = pm.getApplicationInfo(packageName, 0)
            AppInfo(
                packageName = packageName,
                label       = pm.getApplicationLabel(ai).toString(),
                icon        = pm.getApplicationIcon(ai),
            )
        } catch (e: PackageManager.NameNotFoundException) { null }
    }
}
