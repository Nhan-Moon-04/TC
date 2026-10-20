package com.nhandev.notificationfix.domain

import android.os.Build
import android.util.Log
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Identifies the exact Xiaomi device model and HyperOS generation,
 * generating a hardware-tailored optimization profile specifically for:
 * - Xiaomi 15 Pro (HyperOS 2.x - Snapdragon 8 Elite)
 * - Xiaomi 17 Pro Max (HyperOS 3.x - Next-Gen Greezer Architecture)
 * - Generic Xiaomi / HyperOS devices (Adaptive Fallback)
 */
@Singleton
class DeviceProfileManager @Inject constructor(
    private val shizuku: ShizukuHelper,
    private val root: RootHelper,
) {

    companion object {
        private const val TAG = "DeviceProfile"

        // Xiaomi 15 Pro codenames & model numbers
        val XIAOMI_15_PRO_MODELS = setOf(
            "24101PNADC",       // Xiaomi 15 Pro (China)
            "XIAOMI 15 PRO",
            "MI 15 PRO",
            "15 PRO",
        )

        // Xiaomi 17 Pro Max codenames & model patterns
        val XIAOMI_17_PRM_MODELS = setOf(
            "17 PRO MAX",
            "17PRM",
            "XIAOMI 17 PRO MAX",
            "MI 17 PRO MAX",
            "17 PRO",
        )
    }

    private var cachedProfile: DeviceProfile? = null

    private fun run(cmd: String): String {
        return when {
            shizuku.hasPermission -> shizuku.runCommand(cmd).output.trim()
            root.isAvailable      -> root.runCommand(cmd).output.trim()
            else -> ""
        }
    }

    /**
     * Reads system properties via ADB shell / getprop / Android Build APIs
     * and constructs the active DeviceProfile.
     */
    fun detectDeviceProfile(forceRefresh: Boolean = false): DeviceProfile {
        if (!forceRefresh && cachedProfile != null) {
            return cachedProfile!!
        }

        val brand = (run("getprop ro.product.brand").takeIf { it.isNotBlank() } ?: Build.BRAND).uppercase()
        val manufacturer = (run("getprop ro.product.manufacturer").takeIf { it.isNotBlank() } ?: Build.MANUFACTURER).uppercase()
        val modelRaw = run("getprop ro.product.model").takeIf { it.isNotBlank() } ?: Build.MODEL
        val marketNameRaw = run("getprop ro.product.marketname").takeIf { it.isNotBlank() } ?: ""
        val deviceCode = run("getprop ro.product.device").takeIf { it.isNotBlank() } ?: Build.DEVICE
        val hyperOsVersionRaw = run("getprop ro.miui.ui.version.name").takeIf { it.isNotBlank() } ?: ""
        val buildId = run("getprop ro.build.display.id").takeIf { it.isNotBlank() } ?: Build.DISPLAY
        val socModel = run("getprop ro.soc.model").takeIf { it.isNotBlank() } ?: ""
        val androidRelease = run("getprop ro.build.version.release").takeIf { it.isNotBlank() } ?: Build.VERSION.RELEASE
        val sdkInt = Build.VERSION.SDK_INT

        val isXiaomi = brand.contains("XIAOMI") || brand.contains("REDMI") || brand.contains("POCO") ||
                manufacturer.contains("XIAOMI")

        val modelUpper = modelRaw.uppercase()
        val marketUpper = marketNameRaw.uppercase()

        // Match Xiaomi 15 Pro
        val is15Pro = isXiaomi && (
                XIAOMI_15_PRO_MODELS.any { modelUpper.contains(it) || marketUpper.contains(it) } ||
                modelUpper == "24101PNADC" ||
                (modelUpper.contains("15") && modelUpper.contains("PRO") && !modelUpper.contains("REDMI"))
        )

        // Match Xiaomi 17 Pro Max
        val is17Prm = isXiaomi && (
                XIAOMI_17_PRM_MODELS.any { modelUpper.contains(it) || marketUpper.contains(it) } ||
                (modelUpper.contains("17") && (modelUpper.contains("PRO MAX") || modelUpper.contains("PRM") || modelUpper.contains("PRO"))) ||
                (hyperOsVersionRaw.startsWith("OS3", ignoreCase = true) || hyperOsVersionRaw.contains("HyperOS 3", ignoreCase = true))
        )

        val modelType = when {
            is17Prm -> DeviceModelType.XIAOMI_17_PRO_MAX
            is15Pro -> DeviceModelType.XIAOMI_15_PRO
            isXiaomi -> DeviceModelType.GENERIC_HYPEROS
            else -> DeviceModelType.OTHER_ANDROID
        }

        val displayName = when (modelType) {
            DeviceModelType.XIAOMI_17_PRO_MAX -> "Xiaomi 17 Pro Max"
            DeviceModelType.XIAOMI_15_PRO     -> "Xiaomi 15 Pro"
            DeviceModelType.GENERIC_HYPEROS   -> if (marketNameRaw.isNotBlank()) marketNameRaw else modelRaw
            DeviceModelType.OTHER_ANDROID     -> modelRaw
        }

        val profile = when (modelType) {
            DeviceModelType.XIAOMI_17_PRO_MAX -> DeviceProfile(
                modelType = modelType,
                displayName = displayName,
                modelCode = modelRaw,
                marketName = if (marketNameRaw.isNotBlank()) marketNameRaw else "Xiaomi 17 Pro Max",
                hyperOsVersion = if (hyperOsVersionRaw.isNotBlank()) hyperOsVersionRaw else "HyperOS 3.0",
                androidRelease = androidRelease,
                sdkVersion = sdkInt,
                socModel = if (socModel.isNotBlank()) socModel else "Next-Gen Snapdragon",
                buildId = buildId,
                badgeLabel = "HyperOS 3 · Flagship",
                architectureSummary = "HyperOS 3 Next-Gen Greezer · Per-App Freeze Immunity · 7-Tier AppOps",
                supportsHyperOs3Ops = true,
                supportsPerAppGreezer = true,
                supportsMilletFreezeMode = true,
                preferredStandbyBucket = "exempted",
                requiredAppOps = listOf(
                    "RUN_IN_BACKGROUND",
                    "RUN_ANY_IN_BACKGROUND",
                    "WAKE_LOCK",
                    "POST_NOTIFICATION",
                    "SYSTEM_ALERT_WINDOW",
                    "10007", // BOOT_COMPLETED
                    "10008", // MIUI Autostart
                    "10021", // Background Activity Start (pop-up window / incoming call ring screen)
                    "10023", // Foreground Service
                    "10053", // Autostart Switch
                    "10006", // HyperOS 3: Force stop protection
                    "10012", // HyperOS 3: Background process limit exception
                    "10016", // HyperOS 3: Background data restriction bypass
                    "10062", // HyperOS 3: Background network restriction bypass
                    "10065", // HyperOS 3: Battery saver per-app immunity
                ),
            )

            DeviceModelType.XIAOMI_15_PRO -> DeviceProfile(
                modelType = modelType,
                displayName = displayName,
                modelCode = modelRaw,
                marketName = if (marketNameRaw.isNotBlank()) marketNameRaw else "Xiaomi 15 Pro",
                hyperOsVersion = if (hyperOsVersionRaw.isNotBlank()) hyperOsVersionRaw else "HyperOS 2.0 (V816)",
                androidRelease = androidRelease,
                sdkVersion = sdkInt,
                socModel = if (socModel.isNotBlank()) socModel else "Snapdragon 8 Elite",
                buildId = buildId,
                badgeLabel = "HyperOS 2 · SD 8 Elite",
                architectureSummary = "HyperOS 2 Aurogon + ImmobulusMode Bypass · MILLET Decisive Immunity",
                supportsHyperOs3Ops = false,
                supportsPerAppGreezer = false,
                supportsMilletFreezeMode = false,
                preferredStandbyBucket = "exempted",
                requiredAppOps = listOf(
                    "RUN_IN_BACKGROUND",
                    "RUN_ANY_IN_BACKGROUND",
                    "WAKE_LOCK",
                    "POST_NOTIFICATION",
                    "SYSTEM_ALERT_WINDOW",
                    "10007", // BOOT_COMPLETED
                    "10008", // MIUI Autostart
                    "10021", // Background Activity Start (critical for Zalo call ring activity)
                    "10023", // Foreground Service
                    "10053", // Autostart Switch
                ),
            )

            DeviceModelType.GENERIC_HYPEROS -> DeviceProfile(
                modelType = modelType,
                displayName = displayName,
                modelCode = modelRaw,
                marketName = marketNameRaw,
                hyperOsVersion = hyperOsVersionRaw,
                androidRelease = androidRelease,
                sdkVersion = sdkInt,
                socModel = socModel,
                buildId = buildId,
                badgeLabel = "HyperOS Adaptive",
                architectureSummary = "Adaptive Xiaomi Greezer & MILLET Optimization",
                supportsHyperOs3Ops = hyperOsVersionRaw.startsWith("OS3", ignoreCase = true) || sdkInt >= 36,
                supportsPerAppGreezer = true,
                supportsMilletFreezeMode = false,
                preferredStandbyBucket = "exempted",
                requiredAppOps = listOf(
                    "RUN_IN_BACKGROUND",
                    "RUN_ANY_IN_BACKGROUND",
                    "WAKE_LOCK",
                    "POST_NOTIFICATION",
                    "SYSTEM_ALERT_WINDOW",
                    "10007",
                    "10008",
                    "10021",
                    "10023",
                    "10053",
                ),
            )

            DeviceModelType.OTHER_ANDROID -> DeviceProfile(
                modelType = modelType,
                displayName = displayName,
                modelCode = modelRaw,
                marketName = marketNameRaw,
                hyperOsVersion = "",
                androidRelease = androidRelease,
                sdkVersion = sdkInt,
                socModel = socModel,
                buildId = buildId,
                badgeLabel = "AOSP Standard",
                architectureSummary = "Standard Android Doze & Standby Optimization",
                supportsHyperOs3Ops = false,
                supportsPerAppGreezer = false,
                supportsMilletFreezeMode = false,
                preferredStandbyBucket = "active",
                requiredAppOps = listOf(
                    "RUN_IN_BACKGROUND",
                    "RUN_ANY_IN_BACKGROUND",
                    "WAKE_LOCK",
                    "POST_NOTIFICATION",
                ),
            )
        }

        cachedProfile = profile
        Log.i(TAG, "Device detected: ${profile.displayName} | Type: ${profile.modelType} | HyperOS: ${profile.hyperOsVersion}")
        return profile
    }
}

enum class DeviceModelType {
    XIAOMI_15_PRO,
    XIAOMI_17_PRO_MAX,
    GENERIC_HYPEROS,
    OTHER_ANDROID,
}

data class DeviceProfile(
    val modelType: DeviceModelType,
    val displayName: String,
    val modelCode: String,
    val marketName: String,
    val hyperOsVersion: String,
    val androidRelease: String,
    val sdkVersion: Int,
    val socModel: String,
    val buildId: String,
    val badgeLabel: String,
    val architectureSummary: String,
    val supportsHyperOs3Ops: Boolean,
    val supportsPerAppGreezer: Boolean,
    val supportsMilletFreezeMode: Boolean,
    val preferredStandbyBucket: String,
    val requiredAppOps: List<String>,
)
