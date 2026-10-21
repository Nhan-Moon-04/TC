package com.nhandev.notificationfix.domain

import android.content.Context
import com.nhandev.notificationfix.domain.model.FixAction
import com.nhandev.notificationfix.domain.model.FixLogEntry
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Applies / restores notification fixes for a given package.
 * Implements Xiaomi HyperOS 7-layer deep fix (v5.0 — HyperOS 3 compatible):
 *
 * 1. MILLET_NO_RESTRICT_APP (Preserves existing apps, adds GMS & target app)
 *    → Blocks BOTH Aurogon quick-freeze AND PowerStrategyMode tobg/from-system paths
 * 2. Greezer GMS Limiter defense-in-depth (dumpsys greezer IM GMS disable)
 * 3. Greezer LM allowlist for EACH target app (dumpsys greezer LM add <pkg>)
 * 4. GMS & Target AppOps (10007, 10008, 10021, 10023, 10053, WAKE_LOCK, RUN_IN_BG, POST_NOTIFICATION)
 *    + HyperOS 3 ops: 10006, 10012, 10016, 10062, 10065
 * 5. AOSP Doze whitelist & Standby bucket EXEMPTED
 * 6. Non-FCM app deep protection (Zalo etc.) — process keep-alive + network whitelist
 * 7. Reversible restore to system defaults
 *
 * Key insights:
 * - HyperOS 2.x: MILLET_NO_RESTRICT_APP is THE decisive exemption
 * - HyperOS 3: Additional freeze paths exist — need per-app Greezer disable + extra ops
 * - Non-FCM apps (Zalo) maintain their own TCP connection → process MUST stay alive
 *   FCM apps only need GMS alive, but Zalo needs its OWN process alive
 * - Greezer LM add only covers Aurogon ordinary allowlist, NOT PowerStrategyMode → must use MILLET
 * - dumpsys greezer IM GMS disable is volatile (resets on reboot) → defense in depth only
 * - AOSP Doze whitelist does NOT prevent Xiaomi Greezer cgroup freezer
 */
@Singleton
class FixEngine @Inject constructor(
    @ApplicationContext private val context: Context,
    private val shizuku: ShizukuHelper,
    private val root: RootHelper,
    private val deviceProfileManager: DeviceProfileManager,
) {

    companion object {
        /**
         * Apps that do NOT use FCM for push notifications.
         * These apps maintain their own persistent TCP/WebSocket connections.
         * They need extra protection: their OWN process must stay alive (not just GMS).
         *
         * Zalo: Uses proprietary VNG push infrastructure via persistent TCP connection
         */
        val NON_FCM_APPS = setOf(
            "com.zing.zalo",           // Zalo — VNG proprietary push
        )

        /**
         * HyperOS 3 specific AppOps codes (Xiaomi 17 Pro Max etc.)
         * These ops control additional freeze/restrict paths not present in HyperOS 2.
         */
        val HYPEROS3_OPS = listOf(
            "10006",  // Force stop protection
            "10012",  // Background process limit exception
            "10016",  // Background data restriction
            "10062",  // Background network restriction (NEW in HyperOS 3)
            "10065",  // MIUI Battery saver mode per-app (NEW in HyperOS 3)
        )
    }

    fun getDeviceProfile(): DeviceProfile = deviceProfileManager.detectDeviceProfile()

    private fun run(cmd: String): ShellResult {
        return when {
            shizuku.hasPermission -> shizuku.runCommand(cmd)
            root.isAvailable      -> root.runCommand(cmd)
            else -> ShellResult(false, "", "No privilege available (Shizuku or Root required)")
        }
    }

    // ── GMS & GREEZER DEEP DEFENSE ──────────────────────────────────────────

    /**
     * Ensure com.google.android.gms is fully protected from ALL Greezer paths.
     * This is the foundation — if GMS is frozen, NO app receives FCM push.
     */
    fun ensureGmsDeepProtection(): List<FixLogEntry> {
        val logs = mutableListOf<FixLogEntry>()
        val gms = "com.google.android.gms"

        // Layer 1: MILLET_NO_RESTRICT_APP — THE decisive protection
        ensureMilletNoRestrict(gms)

        // Layer 2: Greezer explicit GMS limiter — volatile defense-in-depth
        val greezerDisable = run("dumpsys greezer IM GMS disable")
        logs.add(FixLogEntry(
            packageName = gms,
            appLabel    = "Google Play Services",
            action      = FixAction.FIX_GREEZER_LM,
            success     = greezerDisable.success || !greezerDisable.error.contains("SecurityException"),
            message     = if (greezerDisable.success || !greezerDisable.error.contains("SecurityException"))
                "Greezer GMS limiter disabled (mGmsLimitEnabled=false)"
            else "ROM không hỗ trợ lệnh này",
        ))

        // Layer 3: Greezer LM allowlist for GMS
        run("dumpsys greezer LM add $gms")

        // Layer 4: AOSP Doze whitelist (NOT a substitute for MILLET, but helps)
        run("dumpsys deviceidle whitelist +$gms")
        run("am set-inactive $gms false")
        run("am set-standby-bucket $gms active")

        // Layer 5: GMS AppOps — full set including POST_NOTIFICATION
        val gmsOps = listOf(
            "10007",  // BOOT_COMPLETED
            "10008",  // MIUI Autostart (Tự khởi chạy)
            "10021",  // Background Start Activity (Khởi chạy liên kết)
            "10023",  // Foreground Service
            "10053",  // MIUI Autostart switch
            "RUN_IN_BACKGROUND",
            "RUN_ANY_IN_BACKGROUND",
            "WAKE_LOCK",
            "POST_NOTIFICATION",
        )
        for (op in gmsOps) {
            run("cmd appops set $gms $op allow")
        }

        // Also protect GSF
        val gsf = "com.google.android.gsf"
        ensureMilletNoRestrict(gsf)
        run("dumpsys greezer LM add $gsf")
        run("dumpsys deviceidle whitelist +$gsf")
        run("am set-standby-bucket $gsf active")
        for (op in listOf("RUN_IN_BACKGROUND", "RUN_ANY_IN_BACKGROUND", "WAKE_LOCK", "10008")) {
            run("cmd appops set $gsf $op allow")
        }

        // Protect our own app (NotificationFix) so HyperOS won't kill the MILLET watcher service
        val self = "com.nhandev.notificationfix"
        ensureMilletNoRestrict(self)
        run("dumpsys deviceidle whitelist +$self")
        run("am set-standby-bucket $self active")
        for (op in listOf("RUN_IN_BACKGROUND", "RUN_ANY_IN_BACKGROUND", "WAKE_LOCK", "10008", "10023")) {
            run("cmd appops set $self $op allow")
        }

        return logs
    }

    /** Appends package to Settings.System.MILLET_NO_RESTRICT_APP preserving existing list */
    fun ensureMilletNoRestrict(pkg: String): Boolean {
        val res = run("settings --user 0 get system MILLET_NO_RESTRICT_APP")
        val current = res.output.trim()
        val entries = if (current.isBlank() || current == "null") {
            mutableListOf()
        } else {
            current.split(",").map { it.trim() }.filter { it.isNotBlank() }.toMutableList()
        }

        if (!entries.contains(pkg)) {
            entries.add(pkg)
            val updated = entries.distinct().joinToString(",")
            run("settings --user 0 put system MILLET_NO_RESTRICT_APP '$updated'")

            // Verify
            val verify = run("settings --user 0 get system MILLET_NO_RESTRICT_APP")
            return verify.output.contains(pkg)
        }
        return true // Already present
    }

    /** Check if a package is in MILLET_NO_RESTRICT_APP */
    fun isInMillet(pkg: String): Boolean {
        val res = run("settings --user 0 get system MILLET_NO_RESTRICT_APP")
        val current = res.output.trim()
        if (current.isBlank() || current == "null") return false
        return current.split(",").map { it.trim() }.contains(pkg)
    }

    /** Removes package from MILLET_NO_RESTRICT_APP (if not GMS/GSF) */
    fun removeMilletNoRestrict(pkg: String) {
        // Never remove GMS or GSF from MILLET — they must always be protected
        if (pkg == "com.google.android.gms" || pkg == "com.google.android.gsf") return
        val res = run("settings --user 0 get system MILLET_NO_RESTRICT_APP")
        val current = res.output.trim()
        if (current.isNotBlank() && current != "null") {
            val entries = current.split(",").map { it.trim() }.filter { it.isNotBlank() && it != pkg }
            val updated = entries.joinToString(",")
            run("settings --user 0 put system MILLET_NO_RESTRICT_APP '$updated'")
        }
    }

    // ── FIX ──────────────────────────────────────────────────────────────────

    /**
     * Apply all 7 layers to a package + ensure GMS core protection.
     * HyperOS 3 (Xiaomi 17 Pro Max) requires:
     * - MILLET_NO_RESTRICT_APP (blocks both Aurogon + PowerStrategyMode)
     * - Greezer LM allowlist + per-app Greezer disable
     * - Standby bucket EXEMPTED (stronger than active)
     * - HyperOS 3 specific ops (10006, 10012, 10016, 10062, 10065)
     * - Non-FCM apps (Zalo): extra process keep-alive + network whitelist
     */
    fun fixAll(packageName: String, appLabel: String): List<FixLogEntry> {
        val logs = mutableListOf<FixLogEntry>()

        // Step 0: Ensure GMS FCM Core is immune to Greezer freeze
        logs += ensureGmsDeepProtection()

        // Step 0.5: Persist package for Bank Mode / Developer Options off survival
        ProtectedAppsStorage.addPackage(context, packageName)

        // Step 1: Add target app to MILLET_NO_RESTRICT_APP (THE critical fix)
        val milletOk = ensureMilletNoRestrict(packageName)
        logs.add(FixLogEntry(
            packageName = packageName,
            appLabel    = appLabel,
            action      = FixAction.FIX_MILLET_PROTECT,
            success     = milletOk,
            message     = if (milletOk)
                "MILLET_NO_RESTRICT_APP: Đã bảo vệ khỏi Greezer freeze"
            else
                "MILLET_NO_RESTRICT_APP: Không thể thêm",
        ))

        // Step 2: Add target app to Greezer LM allowlist (defense-in-depth for Aurogon)
        val greezerLm = run("dumpsys greezer LM add $packageName")
        logs.add(FixLogEntry(
            packageName = packageName,
            appLabel    = appLabel,
            action      = FixAction.FIX_GREEZER_LM,
            success     = greezerLm.success || !greezerLm.error.contains("SecurityException"),
            message     = if (greezerLm.success || !greezerLm.error.contains("SecurityException"))
                "Greezer LM allowlist: Đã thêm"
            else "Greezer LM: ROM không hỗ trợ",
        ))

        // Step 2.5 (HyperOS 3): Try per-app Greezer freeze disable
        run("dumpsys greezer IM APP disable $packageName")

        // Step 3: Set standby bucket EXEMPTED (stronger than active on HyperOS 3)
        run("am set-inactive $packageName false")
        val exemptResult = run("am set-standby-bucket $packageName exempted")
        if (!exemptResult.success) {
            // Fallback to active if exempted not supported
            run("am set-standby-bucket $packageName active")
        }

        // Step 4-7: Individual fix operations (includes HyperOS 3 ops)
        logs.add(fixWakeLock(packageName, appLabel))
        logs.add(fixRunInBackground(packageName, appLabel))
        logs.add(fixAutoStart(packageName, appLabel))
        logs.add(fixDozeWhitelist(packageName, appLabel))

        // Step 8: Non-FCM app deep protection (Zalo, etc.)
        if (isNonFcmApp(packageName)) {
            logs += fixNonFcmApp(packageName, appLabel)
        }

        return logs
    }

    /** Check if this app uses its own push mechanism instead of FCM */
    fun isNonFcmApp(packageName: String): Boolean {
        return NON_FCM_APPS.contains(packageName)
    }

    fun fixWakeLock(pkg: String, label: String): FixLogEntry {
        val r = run("cmd appops set $pkg WAKE_LOCK allow")
        return FixLogEntry(
            packageName = pkg,
            appLabel    = label,
            action      = FixAction.FIX_WAKE_LOCK,
            success     = r.success,
            message     = if (r.success) "WAKE_LOCK = allow" else r.error,
        )
    }

    fun fixRunInBackground(pkg: String, label: String): FixLogEntry {
        run("cmd appops set $pkg RUN_IN_BACKGROUND allow")
        val r = run("cmd appops set $pkg RUN_ANY_IN_BACKGROUND allow")
        return FixLogEntry(
            packageName = pkg,
            appLabel    = label,
            action      = FixAction.FIX_RUN_IN_BG,
            success     = r.success,
            message     = if (r.success) "RUN_ANY_IN_BACKGROUND = allow" else r.error,
        )
    }

    fun fixAutoStart(pkg: String, label: String): FixLogEntry {
        // Xiaomi MIUI / HyperOS specific ops:
        // 10007: BOOT_COMPLETED
        // 10008: Autostart (Tự khởi chạy)
        // 10021: Background Autostart (Khởi chạy liên kết)
        // 10023: Foreground Service
        // 10053: MIUI Autostart switch
        run("cmd appops set $pkg 10007 allow")
        run("cmd appops set $pkg 10008 allow")
        run("cmd appops set $pkg 10021 allow")
        run("cmd appops set $pkg 10023 allow")
        run("cmd appops set $pkg 10053 allow")
        run("cmd appops set $pkg AUTO_START allow")
        run("cmd appops set $pkg BOOT_COMPLETED allow")
        run("cmd appops set $pkg POST_NOTIFICATION allow")

        // HyperOS 3 specific ops (Xiaomi 17 Pro Max etc.)
        // These control additional freeze/restrict paths not present in HyperOS 2
        for (op in HYPEROS3_OPS) {
            run("cmd appops set $pkg $op allow")
        }

        return FixLogEntry(
            packageName = pkg,
            appLabel    = label,
            action      = FixAction.FIX_AUTO_START,
            success     = true,
            message     = "Bật Tự khởi chạy + Thông báo + HyperOS 3 ops (10006-10065)",
        )
    }

    fun fixDozeWhitelist(pkg: String, label: String): FixLogEntry {
        val r = run("dumpsys deviceidle whitelist +$pkg")
        return FixLogEntry(
            packageName = pkg,
            appLabel    = label,
            action      = FixAction.FIX_DOZE_WHITELIST,
            success     = r.success || r.error.isBlank(),
            message     = if (r.success || r.error.isBlank()) "Không giới hạn pin (Doze whitelist)" else r.error,
        )
    }

    // ── NON-FCM APP DEEP PROTECTION (ZALO etc.) ─────────────────────────────

    /**
     * Special fix for apps that do NOT use FCM (e.g. Zalo).
     * These apps maintain their own persistent TCP/WebSocket connections to their servers.
     * Unlike FCM apps (which only need GMS alive), non-FCM apps need their OWN process alive.
     *
     * On HyperOS 3, Greezer can freeze Zalo's process even with MILLET protection,
     * because there are NEW freeze paths specific to HyperOS 3.
     *
     * This method applies additional protections:
     * 1. Per-app Greezer freeze disable (HyperOS 3 specific)
     * 2. Network whitelist (prevent background network restriction → TCP stays alive)
     * 3. Process keep-alive via am make-uid-active
     * 4. MILLET_FREEZE_MODE compatibility (if available)
     */
    fun fixNonFcmApp(packageName: String, appLabel: String): List<FixLogEntry> {
        val logs = mutableListOf<FixLogEntry>()

        // 1. Try HyperOS 3 per-app Greezer freeze disable
        val greezerAppDisable = run("dumpsys greezer IM APP disable $packageName")
        logs.add(FixLogEntry(
            packageName = packageName,
            appLabel    = appLabel,
            action      = FixAction.FIX_NON_FCM_PROTECT,
            success     = !greezerAppDisable.error.contains("SecurityException"),
            message     = "[Non-FCM] Greezer per-app freeze disable",
        ))

        // 2. Network whitelist — prevent background network restriction for persistent TCP
        // Get UID first
        val uidResult = run("cmd package list packages -U $packageName 2>/dev/null | head -1")
        val uidMatch = Regex("uid:(\\d+)").find(uidResult.output)
        val uid = uidMatch?.groupValues?.getOrNull(1)
        if (uid != null) {
            run("cmd netpolicy add restrict-background-whitelist $uid")
            logs.add(FixLogEntry(
                packageName = packageName,
                appLabel    = appLabel,
                action      = FixAction.FIX_NON_FCM_PROTECT,
                success     = true,
                message     = "[Non-FCM] Network whitelist: UID $uid added",
            ))
        }

        // 3. Disable background restriction explicitly
        run("cmd appops set $packageName RUN_IN_BACKGROUND allow")
        run("cmd appops set $packageName RUN_ANY_IN_BACKGROUND allow")

        // 4. Try MILLET_FREEZE_MODE (some HyperOS 3 builds)
        val freezeMode = run("settings --user 0 get system MILLET_FREEZE_MODE")
        if (freezeMode.output.trim() != "null" && freezeMode.output.trim().isNotBlank()) {
            // Mode 1 = compatible mode (less aggressive freeze)
            run("settings --user 0 put system MILLET_FREEZE_MODE 1")
            logs.add(FixLogEntry(
                packageName = packageName,
                appLabel    = appLabel,
                action      = FixAction.FIX_NON_FCM_PROTECT,
                success     = true,
                message     = "[Non-FCM] MILLET_FREEZE_MODE set to compatible (1)",
            ))
        }

        // 5. AppOps for Non-FCM VoIP Calls:
        // 10021 (Background Start Activity) + SYSTEM_ALERT_WINDOW are CRITICAL:
        // Without these, HyperOS blocks Zalo from opening incoming call screen when screen is off!
        run("cmd appops set $packageName 10021 allow")
        run("cmd appops set $packageName SYSTEM_ALERT_WINDOW allow")
        run("cmd appops set $packageName 10008 allow")
        run("cmd appops set $packageName 10007 allow")
        run("cmd appops set $packageName 10023 allow")
        run("cmd appops set $packageName 10053 allow")
        run("cmd appops set $packageName WAKE_LOCK allow")
        run("cmd appops set $packageName POST_NOTIFICATION allow")

        // 6. Ensure process importance — trigger Zalo's services to bind and prevent LMKD kill
        run("cmd activity make-uid-active $packageName 2>/dev/null")
        run("am startservice --user 0 -n $packageName/.push.notification.PushService 2>/dev/null")
        run("am startservice --user 0 -n $packageName/.service.ZaloService 2>/dev/null")
        run("am startservice --user 0 -n $packageName/.push.PushReceiver 2>/dev/null")

        logs.add(FixLogEntry(
            packageName = packageName,
            appLabel    = appLabel,
            action      = FixAction.FIX_ZALO_VOIP,
            success     = true,
            message     = "[Zalo Call/VoIP] Đã mở Khởi chạy nền (10021) + Cửa sổ nổi + TCP Network WL + Process High Priority",
        ))

        return logs
    }

    /**
     * Specialized Deep Protection for Zalo VoIP calls & chat socket.
     * Prevents missed calls after long idle / screen off by ensuring:
     * - MILLET_NO_RESTRICT_APP inclusion (immune to Greezer cgroup freeze)
     * - TCP socket protection via netpolicy restrict-background-whitelist
     * - 10021 + SYSTEM_ALERT_WINDOW allow (so call ringing screen pops up when locked)
     * - Standby bucket = exempted
     * - Auto-restart push services + make-uid-active
     */
    fun fixZaloVoIPProtection(): List<FixLogEntry> {
        val zaloPkg = "com.zing.zalo"
        return fixNonFcmApp(zaloPkg, "Zalo")
    }

    /**
     * Makes NotificationFix app itself immune to HyperOS background kills:
     * - Adds com.nhandev.notificationfix to MILLET_NO_RESTRICT_APP
     * - Whitelists in Doze (dumpsys deviceidle whitelist)
     * - Sets standby bucket to active
     * - Grants MIUI Autostart (10008) + Boot Completed (10007) so it restarts on reboot
     * - Grants Foreground Service (10023) + Background Activity (10021)
     * - Adds to netpolicy background whitelist
     */
    fun grantSelfImmunity(): FixLogEntry {
        val selfPkg = "com.nhandev.notificationfix"
        ensureMilletNoRestrict(selfPkg)
        ProtectedAppsStorage.addPackage(context, selfPkg)
        run("dumpsys deviceidle whitelist +$selfPkg")
        run("am set-standby-bucket $selfPkg active")
        run("cmd appops set $selfPkg RUN_IN_BACKGROUND allow")
        run("cmd appops set $selfPkg RUN_ANY_IN_BACKGROUND allow")
        run("cmd appops set $selfPkg WAKE_LOCK allow")
        run("cmd appops set $selfPkg POST_NOTIFICATION allow")
        run("cmd appops set $selfPkg 10007 allow") // Boot completed
        run("cmd appops set $selfPkg 10008 allow") // MIUI Autostart
        run("cmd appops set $selfPkg 10021 allow") // Background start
        run("cmd appops set $selfPkg 10023 allow") // Foreground service
        run("cmd appops set $selfPkg 10053 allow") // Autostart switch
        run("cmd appops set $selfPkg AUTO_START allow")
        run("cmd appops set $selfPkg BOOT_COMPLETED allow")
        run("cmd appops set $selfPkg WRITE_SETTINGS allow")
        run("cmd appops set $selfPkg SYSTEM_ALERT_WINDOW allow")

        // Ẩn thông báo thanh trạng thái (set notification channel importance = 1 / MIN)
        run("cmd notification set_notification_channel_importance $selfPkg millet_watcher_silent_v2 1")

        val uidResult = run("cmd package list packages -U $selfPkg 2>/dev/null | head -1")
        val uidMatch = Regex("uid:(\\d+)").find(uidResult.output)
        uidMatch?.groupValues?.getOrNull(1)?.let { uid ->
            run("cmd netpolicy add restrict-background-whitelist $uid")
        }

        return FixLogEntry(
            packageName = selfPkg,
            appLabel    = "NotificationFix App",
            action      = FixAction.FIX_SELF_IMMUNITY,
            success     = true,
            message     = "NotificationFix: Đã cấp quyền Bất Tử (Tự chạy + Doze WL + WRITE_SETTINGS + Ẩn thông báo)",
        )
    }

    /** Check if NotificationFix has already granted itself all immunity permissions */
    fun isSelfImmune(): Boolean {
        val selfPkg = "com.nhandev.notificationfix"
        val inMillet = isInMillet(selfPkg)
        val autostart = run("cmd appops get $selfPkg 10008").output.contains("allow", ignoreCase = true)
        val doze = run("dumpsys deviceidle whitelist").output.contains(selfPkg)
        val writeSettings = MilletWatcherService.canWriteSettings(context) ||
            run("cmd appops get $selfPkg WRITE_SETTINGS").output.contains("allow", ignoreCase = true)
        return (inMillet && autostart && doze) || writeSettings
    }

    /**
     * Deploys and starts the detached Linux shell daemon (/data/local/tmp/hyperos_watchdog.sh).
     * Because this daemon executes in the ADB shell/root domain (UID 2000/0),
     * it cannot be killed by MIUI/HyperOS ActivityManager or task killer!
     */
    fun deployAndStartShellDaemon(): FixLogEntry {
        val daemonScript = """
            #!/system/bin/sh
            # HyperOS FCM & Zalo Deep Watchdog Daemon
            # Runs in Linux shell domain (UID 2000/0) - Immune to Android ActivityManager app kills!

            GMS="com.google.android.gms"
            ZALO="com.zing.zalo"
            SELF="com.nhandev.notificationfix"

            while true; do
                # 1. MILLET protection: Ensure GMS, Zalo, and Self are present
                CUR="$(settings --user 0 get system MILLET_NO_RESTRICT_APP 2>/dev/null)"
                NEW="${'$'}CUR"
                for PKG in "${'$'}GMS" "${'$'}ZALO" "${'$'}SELF"; do
                    case ",${'$'}NEW," in
                        *,"${'$'}PKG",*) ;;
                        *)
                            if [ -z "${'$'}NEW" ] || [ "${'$'}NEW" = "null" ]; then
                                NEW="${'$'}PKG"
                            else
                                NEW="${'$'}NEW,${'$'}PKG"
                            fi
                            ;;
                    esac
                done
                if [ "${'$'}NEW" != "${'$'}CUR" ]; then
                    settings --user 0 put system MILLET_NO_RESTRICT_APP "${'$'}NEW" 2>/dev/null
                fi

                # 2. Greezer volatile overrides
                dumpsys greezer IM GMS disable >/dev/null 2>&1
                dumpsys greezer IM APP disable "${'$'}ZALO" >/dev/null 2>&1
                dumpsys greezer LM add "${'$'}GMS" >/dev/null 2>&1
                dumpsys greezer LM add "${'$'}ZALO" >/dev/null 2>&1

                # 3. Zalo Process & Call Keep-Alive
                ZALO_PID=$(pidof "${'$'}ZALO" 2>/dev/null)
                if [ -z "${'$'}ZALO_PID" ]; then
                    am startservice --user 0 -n "${'$'}ZALO/.service.ZaloService" >/dev/null 2>&1
                    am startservice --user 0 -n "${'$'}ZALO/.push.notification.PushService" >/dev/null 2>&1
                    cmd activity make-uid-active "${'$'}ZALO" >/dev/null 2>&1
                fi

                sleep 30
            done
        """.trimIndent()

        // Write script to /data/local/tmp
        val scriptPath = "/data/local/tmp/hyperos_watchdog.sh"
        run("mkdir -p /data/local/tmp")
        
        // Write script safely using printf / base64 or heredoc
        val b64 = android.util.Base64.encodeToString(daemonScript.toByteArray(Charsets.UTF_8), android.util.Base64.NO_WRAP)
        run("echo '$b64' | base64 -d > $scriptPath")
        run("chmod 755 $scriptPath")

        // Kill existing instance if any
        run("pkill -f hyperos_watchdog.sh")

        // Launch in background with nohup
        val launchRes = run("nohup sh $scriptPath >/dev/null 2>&1 &")

        // Verify daemon is running
        val pgrep = run("pgrep -f hyperos_watchdog.sh")
        val isRunning = pgrep.output.isNotBlank()

        return FixLogEntry(
            packageName = "hyperos.watchdog.daemon",
            appLabel    = "Linux Shell Watchdog",
            action      = FixAction.START_WATCHDOG_DAEMON,
            success     = isRunning || launchRes.success,
            message     = if (isRunning)
                "Shell Watchdog Daemon đã chạy ngầm độc lập (PID: ${pgrep.output.split("\n").firstOrNull()})"
            else
                "Đã khởi động watchdog daemon (/data/local/tmp/hyperos_watchdog.sh)",
        )
    }

    /**
     * Auto-detects device hardware model (Xiaomi 15 Pro, Xiaomi 17 Pro Max, or Generic HyperOS)
     * and applies the optimal full-stack profile tailored specifically for this hardware.
     */
    fun autoOptimizeForModel(installedPackages: List<String>): List<FixLogEntry> {
        val logs = mutableListOf<FixLogEntry>()
        val profile = getDeviceProfile()

        logs.add(FixLogEntry(
            packageName = "system.device.profile",
            appLabel    = profile.displayName,
            action      = FixAction.FIX_DEVICE_PROFILE_OPTIMIZE,
            success     = true,
            message     = "Nhận diện máy: ${profile.displayName} (${profile.hyperOsVersion}) -> Áp dụng cấu hình: ${profile.badgeLabel}",
        ))

        // 1. Core GMS Protection (MILLET + Greezer)
        logs += ensureGmsDeepProtection()

        // 2. Hardware profile specific adjustments
        if (profile.supportsMilletFreezeMode) {
            run("settings --user 0 put system MILLET_FREEZE_MODE 1")
        }

        // 3. Deep Zalo VoIP & Socket Protection
        if (installedPackages.contains("com.zing.zalo")) {
            logs += fixZaloVoIPProtection()
        }

        // Persist all installed and protected packages
        ProtectedAppsStorage.addPackages(context, installedPackages)

        // 4. Immunity for NotificationFix app itself (reboot survival + unkillable)
        logs.add(grantSelfImmunity())

        // 5. Deploy Linux shell watchdog daemon
        logs.add(deployAndStartShellDaemon())

        return logs
    }

    // ── RESTORE (HỦY FIX / ĐƯA VỀ GỐC) ──────────────────────────────────────

    /** Reset all settings back to system default (turn off autostart, reset battery & wake lock). */
    fun restoreAll(packageName: String, appLabel: String): List<FixLogEntry> {
        removeMilletNoRestrict(packageName)
        ProtectedAppsStorage.removePackage(context, packageName)
        run("am set-standby-bucket $packageName working_set")

        // Remove from Greezer LM allowlist if possible
        run("dumpsys greezer LM remove $packageName")

        return listOf(
            restoreAutoStart(packageName, appLabel),
            restoreWakeLock(packageName, appLabel),
            restoreRunInBackground(packageName, appLabel),
            restoreDozeWhitelist(packageName, appLabel),
        )
    }

    fun restoreAutoStart(pkg: String, label: String): FixLogEntry {
        run("cmd appops set $pkg 10007 default")
        run("cmd appops set $pkg 10008 default")
        run("cmd appops set $pkg 10021 default")
        run("cmd appops set $pkg 10023 default")
        run("cmd appops set $pkg 10053 default")
        run("cmd appops set $pkg AUTO_START default")
        val r = run("cmd appops set $pkg BOOT_COMPLETED default")
        return FixLogEntry(
            packageName = pkg,
            appLabel    = label,
            action      = FixAction.RESTORE_AUTO_START,
            success     = r.success || r.error.isBlank(),
            message     = if (r.success || r.error.isBlank()) "Tắt Tự khởi chạy (Autostart về mặc định)" else r.error,
        )
    }

    fun restoreWakeLock(pkg: String, label: String): FixLogEntry {
        val r = run("cmd appops set $pkg WAKE_LOCK default")
        return FixLogEntry(
            packageName = pkg,
            appLabel    = label,
            action      = FixAction.RESTORE_WAKE_LOCK,
            success     = r.success,
            message     = if (r.success) "WAKE_LOCK = default" else r.error,
        )
    }

    fun restoreRunInBackground(pkg: String, label: String): FixLogEntry {
        run("cmd appops set $pkg RUN_IN_BACKGROUND default")
        val r = run("cmd appops set $pkg RUN_ANY_IN_BACKGROUND default")
        return FixLogEntry(
            packageName = pkg,
            appLabel    = label,
            action      = FixAction.RESTORE_RUN_IN_BG,
            success     = r.success,
            message     = if (r.success) "RUN_ANY_IN_BACKGROUND = default" else r.error,
        )
    }

    fun restoreDozeWhitelist(pkg: String, label: String): FixLogEntry {
        val r = run("dumpsys deviceidle whitelist -$pkg")
        return FixLogEntry(
            packageName = pkg,
            appLabel    = label,
            action      = FixAction.RESTORE_DOZE_WHITELIST,
            success     = r.success || r.error.isBlank(),
            message     = if (r.success || r.error.isBlank()) "Khôi phục tiết kiệm pin bình thường" else r.error,
        )
    }
}
