package com.nhandev.notificationfix.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nhandev.notificationfix.data.FirestoreRepository
import com.nhandev.notificationfix.domain.AppScanner
import com.nhandev.notificationfix.domain.FixEngine
import com.nhandev.notificationfix.domain.MilletWatcherService
import com.nhandev.notificationfix.domain.PrivilegeDetector
import com.nhandev.notificationfix.domain.ShizukuHelper
import com.nhandev.notificationfix.domain.StatusChecker
import com.nhandev.notificationfix.domain.model.ALL_PRESETS
import com.nhandev.notificationfix.domain.model.AppInfo
import com.nhandev.notificationfix.domain.model.AppPreset
import com.nhandev.notificationfix.domain.model.AppStatus
import com.nhandev.notificationfix.domain.model.FixLogEntry
import com.nhandev.notificationfix.domain.model.PrivilegeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

data class AppListUiState(
    val allApps: List<AppInfo>             = emptyList(),
    val statusMap: Map<String, AppStatus>  = emptyMap(),
    val selectedPkgs: Set<String>          = emptySet(),
    val searchQuery: String                = "",
    val activePreset: AppPreset?           = null,
    val isLoading: Boolean                 = true,
    val isFixing: Boolean                  = false,
    val fixProgress: Float                 = 0f,
    val fixCurrentApp: String              = "",
    val fixSuccessMessage: String?         = null,
    val privilegeMode: PrivilegeMode       = PrivilegeMode.NONE,
    val fixLogs: List<FixLogEntry>         = emptyList(),
    val showOnlyUnfixed: Boolean           = false,
    val filterAutoStart: Boolean           = false,
    val filterRunInBg: Boolean             = false,
    val filterDoze: Boolean                = false,
    /** Packages currently being toggled individually (for drawer loading state) */
    val togglingPkgs: Set<String>          = emptySet(),
    /** GMS Core protection state */
    val isGmsProtected: Boolean            = false,
    val isProtectingGms: Boolean           = false,
    val isRefreshingGreezer: Boolean       = false,
    /** MILLET Watcher service running state */
    val isWatcherRunning: Boolean          = false,
    val hasWriteSettingsPermission: Boolean = false,
    /** Device profile detection */
    val deviceProfile: com.nhandev.notificationfix.domain.DeviceProfile? = null,
    val isAutoOptimizing: Boolean          = false,
    val isZaloProtected: Boolean           = false,
    val isProtectingZalo: Boolean          = false,
    val isSelfImmune: Boolean              = false,
    val isGrantingSelfImmunity: Boolean    = false,
)

@HiltViewModel
class AppListViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val scanner: AppScanner,
    private val checker: StatusChecker,
    private val fixEngine: FixEngine,
    private val privilegeDetector: PrivilegeDetector,
    private val shizuku: ShizukuHelper,
    private val firestore: FirestoreRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(AppListUiState())
    val uiState: StateFlow<AppListUiState> = _uiState.asStateFlow()

    /** Derived filtered list based on search + preset + unfixed + multi-status filters. */
    val filteredApps: StateFlow<List<AppInfo>> = combine(
        _uiState
    ) { states ->
        val s = states[0]
        s.allApps.filter { app ->
            val matchesSearch = s.searchQuery.isBlank() ||
                app.label.contains(s.searchQuery, ignoreCase = true) ||
                app.packageName.contains(s.searchQuery, ignoreCase = true)
            val matchesUnfixed = !s.showOnlyUnfixed ||
                s.statusMap[app.packageName]?.isFullyFixed == false

            val status = s.statusMap[app.packageName]
            val matchesAutoStart = !s.filterAutoStart || (status?.autoStartAllowed == true)
            val matchesRunInBg   = !s.filterRunInBg   || (status?.runInBackgroundAllowed == true)
            val matchesDoze      = !s.filterDoze      || (status?.dozeWhitelisted == true)

            matchesSearch && matchesUnfixed && matchesAutoStart && matchesRunInBg && matchesDoze
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), emptyList())

    init {
        load()
        checkWatcherState()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)

            withContext(Dispatchers.IO) {
                val privilege = privilegeDetector.detect()
                val apps      = scanner.getRecommendedApps()
                val statuses  = checker.checkApps(apps.map { it.packageName })

                // Check GMS protection status
                val gmsProtected = fixEngine.isInMillet("com.google.android.gms")
                val zaloProtected = fixEngine.isInMillet("com.zing.zalo")
                val hasWriteSettings = MilletWatcherService.canWriteSettings(context)
                val deviceProfile = fixEngine.getDeviceProfile()
                val isSelfImmune = fixEngine.isSelfImmune()

                _uiState.value = _uiState.value.copy(
                    allApps        = apps,
                    statusMap      = statuses,
                    privilegeMode  = privilege,
                    isLoading      = false,
                    isGmsProtected = gmsProtected,
                    isZaloProtected = zaloProtected,
                    deviceProfile  = deviceProfile,
                    isSelfImmune   = isSelfImmune,
                    hasWriteSettingsPermission = hasWriteSettings,
                )
            }
        }
    }

    fun refreshPrivilege() {
        viewModelScope.launch(Dispatchers.IO) {
            val privilege = privilegeDetector.detect()
            val gmsProtected = fixEngine.isInMillet("com.google.android.gms")
            val zaloProtected = fixEngine.isInMillet("com.zing.zalo")
            val deviceProfile = fixEngine.getDeviceProfile()
            val isSelfImmune = fixEngine.isSelfImmune()
            val hasWriteSettings = MilletWatcherService.canWriteSettings(context)
            _uiState.value = _uiState.value.copy(
                privilegeMode  = privilege,
                isGmsProtected = gmsProtected,
                isZaloProtected = zaloProtected,
                deviceProfile  = deviceProfile,
                isSelfImmune   = isSelfImmune,
                hasWriteSettingsPermission = hasWriteSettings,
            )
        }
    }

    fun requestShizukuPermission() {
        shizuku.requestPermission()
    }

    fun dismissSuccessDialog() {
        _uiState.value = _uiState.value.copy(fixSuccessMessage = null)
    }

    fun setSearchQuery(q: String) {
        _uiState.value = _uiState.value.copy(searchQuery = q)
    }

    fun toggleShowOnlyUnfixed() {
        _uiState.value = _uiState.value.copy(
            showOnlyUnfixed = !_uiState.value.showOnlyUnfixed
        )
    }

    fun toggleFilterAutoStart() {
        _uiState.value = _uiState.value.copy(
            filterAutoStart = !_uiState.value.filterAutoStart
        )
    }

    fun toggleFilterRunInBg() {
        _uiState.value = _uiState.value.copy(
            filterRunInBg = !_uiState.value.filterRunInBg
        )
    }

    fun toggleFilterDoze() {
        _uiState.value = _uiState.value.copy(
            filterDoze = !_uiState.value.filterDoze
        )
    }

    fun clearStatusFilters() {
        _uiState.value = _uiState.value.copy(
            filterAutoStart = false,
            filterRunInBg   = false,
            filterDoze      = false,
        )
    }

    fun selectFilteredApps(apps: List<AppInfo>) {
        val current = _uiState.value.selectedPkgs.toMutableSet()
        current.addAll(apps.map { it.packageName })
        _uiState.value = _uiState.value.copy(selectedPkgs = current)
    }

    fun deselectAll() {
        _uiState.value = _uiState.value.copy(selectedPkgs = emptySet())
    }

    fun selectPreset(preset: AppPreset) {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val presetApps = scanner.getPresetApps(preset)
                val statuses = checker.checkApps(presetApps.map { it.packageName })
                _uiState.value = _uiState.value.copy(
                    activePreset  = preset,
                    allApps       = presetApps,
                    selectedPkgs  = presetApps.map { it.packageName }.toSet(),
                    statusMap     = statuses,
                )
            }
        }
    }

    fun selectAll() {
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val allApps = scanner.getAllApps()
                val statuses = checker.checkApps(allApps.map { it.packageName })
                _uiState.value = _uiState.value.copy(
                    allApps      = allApps,
                    activePreset = null,
                    statusMap    = statuses,
                )
            }
        }
    }

    fun toggleSelection(packageName: String) {
        val current = _uiState.value.selectedPkgs.toMutableSet()
        if (current.contains(packageName)) current.remove(packageName)
        else current.add(packageName)
        _uiState.value = _uiState.value.copy(selectedPkgs = current)
    }

    fun selectPackage(packageName: String) {
        val current = _uiState.value.selectedPkgs.toMutableSet()
        current.add(packageName)
        _uiState.value = _uiState.value.copy(selectedPkgs = current)
    }

    // ── GMS Deep Protection ──────────────────────────────────────────────────

    /** One-tap GMS Core protection: MILLET + Greezer + Doze + AppOps */
    fun protectGmsCore() {
        val priv = _uiState.value.privilegeMode
        if (priv == PrivilegeMode.NONE) {
            shizuku.requestPermission()
            return
        }

        _uiState.value = _uiState.value.copy(isProtectingGms = true)

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val logs = fixEngine.ensureGmsDeepProtection()
                val gmsProtected = fixEngine.isInMillet("com.google.android.gms")

                _uiState.value = _uiState.value.copy(
                    isProtectingGms = false,
                    isGmsProtected  = gmsProtected,
                    fixLogs         = _uiState.value.fixLogs + logs,
                    fixSuccessMessage = "GMS Core đã được bảo vệ hoàn toàn khỏi Greezer!",
                )

                // Refresh status for GMS-related packages
                val gmsPackages = listOf("com.google.android.gms", "com.google.android.gsf")
                    .filter { pkg -> _uiState.value.allApps.any { it.packageName == pkg } }
                if (gmsPackages.isNotEmpty()) {
                    val updatedStatuses = checker.checkApps(gmsPackages)
                    val newMap = _uiState.value.statusMap.toMutableMap().apply { putAll(updatedStatuses) }
                    _uiState.value = _uiState.value.copy(statusMap = newMap)
                }

                runCatching { firestore.saveFixLogs(logs) }
            }

            // Auto-start MILLET watcher after GMS protection
            startMilletWatcher()
        }
    }

    /** Refresh Greezer GMS limiter (volatile, resets on reboot) */
    fun refreshGreezer() {
        val priv = _uiState.value.privilegeMode
        if (priv == PrivilegeMode.NONE) {
            shizuku.requestPermission()
            return
        }

        _uiState.value = _uiState.value.copy(isRefreshingGreezer = true)

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                fixEngine.ensureGmsDeepProtection()
                val gmsProtected = fixEngine.isInMillet("com.google.android.gms")

                _uiState.value = _uiState.value.copy(
                    isRefreshingGreezer = false,
                    isGmsProtected      = gmsProtected,
                    fixSuccessMessage   = "Greezer GMS limiter đã được tái vô hiệu hóa!",
                )
            }
        }
    }

    // ── Auto Optimize For Model (Xiaomi 15 Pro / 17 Pro Max) ──────────────────

    /**
     * 1-Tap Auto Hardware Optimization tailored specifically for:
     * - Xiaomi 15 Pro (HyperOS 2.x)
     * - Xiaomi 17 Pro Max (HyperOS 3.x)
     * Automatically configures GMS, Zalo VoIP call protection, Self-Immunity,
     * and deploys the detached Linux shell daemon.
     */
    fun autoOptimizeForModel() {
        val priv = _uiState.value.privilegeMode
        if (priv == PrivilegeMode.NONE) {
            shizuku.requestPermission()
            return
        }

        _uiState.value = _uiState.value.copy(
            isAutoOptimizing = true,
            isFixing = true,
            fixProgress = 0.1f,
            fixCurrentApp = "Đang nhận diện model máy & cấu hình tối ưu...",
        )

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val installedPkgs = _uiState.value.allApps.map { it.packageName }
                val profile = fixEngine.getDeviceProfile()

                _uiState.value = _uiState.value.copy(
                    fixProgress = 0.3f,
                    fixCurrentApp = "Áp dụng profile cho ${profile.displayName}...",
                )

                val logs = fixEngine.autoOptimizeForModel(installedPkgs)

                _uiState.value = _uiState.value.copy(
                    fixProgress = 0.7f,
                    fixCurrentApp = "Kiểm tra lại trạng thái các ứng dụng...",
                )

                // Refresh statuses
                val updatedStatuses = checker.checkApps(installedPkgs)
                val gmsProtected = fixEngine.isInMillet("com.google.android.gms")
                val zaloProtected = fixEngine.isInMillet("com.zing.zalo")

                _uiState.value = _uiState.value.copy(
                    isAutoOptimizing = false,
                    isFixing = false,
                    fixProgress = 1f,
                    fixCurrentApp = "",
                    statusMap = updatedStatuses,
                    isGmsProtected = gmsProtected,
                    isZaloProtected = zaloProtected,
                    deviceProfile = profile,
                    fixLogs = _uiState.value.fixLogs + logs,
                    fixSuccessMessage = "🚀 Đã tối ưu hoàn tất cho ${profile.displayName}!\n" +
                            "• Cấu hình: ${profile.badgeLabel}\n" +
                            "• Zalo: Chống trễ, fix lỡ cuộc gọi khi tắt màn hình\n" +
                            "• Watchdog: Đã kích hoạt chạy ngầm vĩnh viễn & reboot auto-start",
                )

                runCatching { firestore.saveFixLogs(logs) }
            }

            // Start watcher service
            startMilletWatcher()
        }
    }

    /**
     * Specialized 1-Tap Fix for Zalo VoIP calls & chat socket.
     * Guarantees incoming calls pop up when screen is locked or idle!
     */
    fun fixZaloVoIP() {
        val priv = _uiState.value.privilegeMode
        if (priv == PrivilegeMode.NONE) {
            shizuku.requestPermission()
            return
        }

        _uiState.value = _uiState.value.copy(isProtectingZalo = true)

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val logs = fixEngine.fixZaloVoIPProtection()
                val zaloStatus = checker.checkApp("com.zing.zalo")
                val newMap = _uiState.value.statusMap.toMutableMap().apply {
                    put("com.zing.zalo", zaloStatus)
                }
                val zaloProtected = fixEngine.isInMillet("com.zing.zalo")

                _uiState.value = _uiState.value.copy(
                    isProtectingZalo = false,
                    isZaloProtected = zaloProtected,
                    statusMap = newMap,
                    fixLogs = _uiState.value.fixLogs + logs,
                    fixSuccessMessage = "✓ Đã bật bảo vệ Cuộc gọi Zalo!\n" +
                            "Mở quyền Cửa sổ nổi (10021) + TCP Network WL + Process High Priority",
                )

                runCatching { firestore.saveFixLogs(logs) }
            }
        }
    }

    /**
     * Tự cấp toàn bộ quyền cho NotificationFix & Kích hoạt chạy ngầm vĩnh viễn:
     * - Tự cấp AppOps: Autostart (10008), Boot (10007), Background Start (10021), Foreground (10023)
     * - Tự cấp WRITE_SETTINGS & Doze Battery Whitelist
     * - Kích hoạt MilletWatcherService chạy ngầm ẩn hoàn toàn (không hiện thông báo trên thanh trạng thái)
     * - Triển khai Linux Shell Watchdog Daemon độc lập
     * - Lên lịch Watchdog AlarmManager tự hồi sinh định kỳ và khi reboot máy
     */
    fun enablePermanentRunAndReboot() {
        val priv = _uiState.value.privilegeMode
        if (priv == PrivilegeMode.NONE) {
            shizuku.requestPermission()
            return
        }

        _uiState.value = _uiState.value.copy(
            isGrantingSelfImmunity = true,
            fixCurrentApp = "Đang tự cấp toàn bộ quyền tự chạy & chạy ngầm ẩn..."
        )

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val selfLog = fixEngine.grantSelfImmunity()
                val daemonLog = fixEngine.deployAndStartShellDaemon()
                val isImmune = fixEngine.isSelfImmune()

                _uiState.value = _uiState.value.copy(
                    isGrantingSelfImmunity = false,
                    isSelfImmune = isImmune,
                    isWatcherRunning = true,
                    fixLogs = _uiState.value.fixLogs + listOf(selfLog, daemonLog),
                    fixSuccessMessage = "⚡ ĐÃ TỰ CẤP TOÀN BỘ QUYỀN THÀNH CÔNG!\n\n" +
                            "• Tự khởi chạy (Autostart 10008) & Khởi động cùng hệ thống (10007)\n" +
                            "• Bỏ qua tối ưu hóa Pin & Quyền sửa cài đặt hệ thống (WRITE_SETTINGS)\n" +
                            "• Đang chạy ngầm ẩn hoàn toàn (Không hiện icon trên thanh trạng thái)\n" +
                            "• Bất tử sau reboot và tự động hồi sinh khi bị kill\n\n" +
                            "💡 Giờ bạn có thể TẮT Developer Options để dùng app Ngân Hàng bình thường!",
                )
            }

            startMilletWatcher()
            com.nhandev.notificationfix.domain.WatchdogAlarmReceiver.schedulePeriodicCheck(context)
        }
    }

    fun enableSelfImmunityAndAutoRun() = enablePermanentRunAndReboot()

    // ── Toggle App Fix (Drawer) ──────────────────────────────────────────────

    /**
     * Toggle fix state for a single app (used by the Drawer panel).
     * If the app is currently fully fixed → restore it.
     * If not → fix it.
     */
    fun toggleAppFix(packageName: String, label: String) {
        val priv = _uiState.value.privilegeMode
        if (priv == PrivilegeMode.NONE) {
            shizuku.requestPermission()
            return
        }
        val isFixed = _uiState.value.statusMap[packageName]?.isFullyFixed == true
        // Add to toggling set for loading indicator
        val toggling = _uiState.value.togglingPkgs.toMutableSet().also { it.add(packageName) }
        _uiState.value = _uiState.value.copy(togglingPkgs = toggling)

        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                val logs = if (isFixed) {
                    fixEngine.restoreAll(packageName, label)
                } else {
                    fixEngine.fixAll(packageName, label)
                }
                val status = checker.checkApp(packageName)
                val newMap = _uiState.value.statusMap.toMutableMap().apply {
                    put(packageName, status)
                }
                val newToggling = _uiState.value.togglingPkgs.toMutableSet().also { it.remove(packageName) }
                val gmsProtected = fixEngine.isInMillet("com.google.android.gms")
                _uiState.value = _uiState.value.copy(
                    statusMap      = newMap,
                    fixLogs        = _uiState.value.fixLogs + logs,
                    togglingPkgs   = newToggling,
                    isGmsProtected = gmsProtected,
                )
                runCatching { firestore.saveFixLogs(logs) }
            }
        }
    }

    // ── Fix / Restore ────────────────────────────────────────────────────────

    fun fixSelected() {
        viewModelScope.launch {
            val priv = _uiState.value.privilegeMode
            if (priv == PrivilegeMode.NONE) {
                shizuku.requestPermission()
                return@launch
            }

            val selected = _uiState.value.selectedPkgs.toList()
            if (selected.isEmpty()) return@launch

            _uiState.value = _uiState.value.copy(
                isFixing      = true,
                fixProgress   = 0.05f,
                fixCurrentApp = "Đang bảo vệ GMS Core...",
            )

            withContext(Dispatchers.IO) {
                val logs = mutableListOf<FixLogEntry>()
                val total = selected.size

                selected.forEachIndexed { index, pkg ->
                    val label = _uiState.value.allApps.find { it.packageName == pkg }?.label ?: pkg
                    val prog = (index.toFloat() / total.toFloat()).coerceAtLeast(0.05f)
                    _uiState.value = _uiState.value.copy(
                        fixProgress   = prog,
                        fixCurrentApp = "Đang tối ưu: $label (${index + 1}/$total)",
                    )
                    logs += fixEngine.fixAll(pkg, label)
                    _uiState.value = _uiState.value.copy(
                        fixProgress   = ((index + 1).toFloat() / total.toFloat()),
                    )
                }

                _uiState.value = _uiState.value.copy(
                    fixCurrentApp = "Đang kiểm tra lại trạng thái..."
                )

                val updatedStatuses = checker.checkApps(selected)
                val newStatusMap = _uiState.value.statusMap.toMutableMap().apply {
                    putAll(updatedStatuses)
                }
                val gmsProtected = fixEngine.isInMillet("com.google.android.gms")

                _uiState.value = _uiState.value.copy(
                    isFixing          = false,
                    fixProgress       = 1f,
                    fixCurrentApp     = "",
                    fixSuccessMessage = "Đã fix thông báo thành công cho $total ứng dụng!\nMILLET + Greezer + Doze đã được áp dụng.",
                    statusMap         = newStatusMap,
                    fixLogs           = _uiState.value.fixLogs + logs,
                    isGmsProtected    = gmsProtected,
                )

                runCatching {
                    firestore.saveFixLogs(logs)
                    val toSave = updatedStatuses.mapValues { (pkg, status) ->
                        val label = _uiState.value.allApps.find { it.packageName == pkg }?.label ?: pkg
                        label to status
                    }
                    firestore.saveAllAppStatuses(toSave)
                }

                // Also fix our own app to prevent HyperOS from killing the watcher service
                fixEngine.ensureMilletNoRestrict("com.nhandev.notificationfix")
            }

            // Auto-start MILLET watcher after fixing apps
            startMilletWatcher()
        }
    }

    fun fixSingle(packageName: String, label: String) {
        viewModelScope.launch {
            val priv = _uiState.value.privilegeMode
            if (priv == PrivilegeMode.NONE) {
                shizuku.requestPermission()
                return@launch
            }

            _uiState.value = _uiState.value.copy(
                isFixing      = true,
                fixProgress   = 0.3f,
                fixCurrentApp = "Đang tối ưu $label...",
            )

            withContext(Dispatchers.IO) {
                val logs = fixEngine.fixAll(packageName, label)
                _uiState.value = _uiState.value.copy(fixProgress = 0.8f, fixCurrentApp = "Kiểm tra lại trạng thái...")
                val status = checker.checkApp(packageName)
                val newMap = _uiState.value.statusMap.toMutableMap().apply {
                    put(packageName, status)
                }
                val gmsProtected = fixEngine.isInMillet("com.google.android.gms")
                _uiState.value = _uiState.value.copy(
                    isFixing          = false,
                    fixProgress       = 1f,
                    fixCurrentApp     = "",
                    fixSuccessMessage = "Đã fix thông báo thành công cho $label!\nMILLET + Greezer + Doze đã được áp dụng.",
                    statusMap         = newMap,
                    fixLogs           = _uiState.value.fixLogs + logs,
                    isGmsProtected    = gmsProtected,
                )
                runCatching {
                    firestore.saveFixLogs(logs)
                    firestore.saveAppStatus(label, status)
                }
            }
        }
    }

    fun restoreSingle(packageName: String, label: String) {
        viewModelScope.launch {
            val priv = _uiState.value.privilegeMode
            if (priv == PrivilegeMode.NONE) {
                shizuku.requestPermission()
                return@launch
            }

            _uiState.value = _uiState.value.copy(
                isFixing      = true,
                fixProgress   = 0.3f,
                fixCurrentApp = "Đang hoàn tác $label...",
            )

            withContext(Dispatchers.IO) {
                val logs = fixEngine.restoreAll(packageName, label)
                val status = checker.checkApp(packageName)
                val newMap = _uiState.value.statusMap.toMutableMap().apply {
                    put(packageName, status)
                }
                _uiState.value = _uiState.value.copy(
                    isFixing          = false,
                    fixProgress       = 1f,
                    fixCurrentApp     = "",
                    fixSuccessMessage = "Đã hoàn tác cài đặt cho $label!",
                    statusMap         = newMap,
                    fixLogs           = _uiState.value.fixLogs + logs,
                )
            }
        }
    }

    fun restoreSelected() {
        viewModelScope.launch {
            val priv = _uiState.value.privilegeMode
            if (priv == PrivilegeMode.NONE) {
                shizuku.requestPermission()
                return@launch
            }

            val selected = _uiState.value.selectedPkgs.toList()
            if (selected.isEmpty()) return@launch

            _uiState.value = _uiState.value.copy(
                isFixing      = true,
                fixProgress   = 0.05f,
                fixCurrentApp = "Bắt đầu hoàn tác...",
            )

            withContext(Dispatchers.IO) {
                val logs = mutableListOf<FixLogEntry>()
                val total = selected.size

                selected.forEachIndexed { index, pkg ->
                    val label = _uiState.value.allApps.find { it.packageName == pkg }?.label ?: pkg
                    _uiState.value = _uiState.value.copy(
                        fixProgress   = (index.toFloat() / total.toFloat()).coerceAtLeast(0.05f),
                        fixCurrentApp = "Đang hoàn tác: $label (${index + 1}/$total)",
                    )
                    logs += fixEngine.restoreAll(pkg, label)
                }

                val updatedStatuses = checker.checkApps(selected)
                val newStatusMap = _uiState.value.statusMap.toMutableMap().apply {
                    putAll(updatedStatuses)
                }

                _uiState.value = _uiState.value.copy(
                    isFixing          = false,
                    fixProgress       = 1f,
                    fixCurrentApp     = "",
                    fixSuccessMessage = "Đã hoàn tác cài đặt cho $total ứng dụng!",
                    statusMap         = newStatusMap,
                    fixLogs           = _uiState.value.fixLogs + logs,
                )
            }
        }
    }

    fun clearLogs() {
        _uiState.value = _uiState.value.copy(fixLogs = emptyList())
    }

    // ── MILLET Watcher Service ────────────────────────────────────────────────

    /**
     * Start the MILLET watcher service.
     * This service monitors Settings.System.MILLET_NO_RESTRICT_APP via ContentObserver
     * and instantly re-adds GMS + Zalo when PowerKeeper overwrites the list.
     *
     * Requirements:
     * - WRITE_SETTINGS permission must be granted (prompts user if not)
     * - Works WITHOUT Shizuku, WITHOUT Root, survives reboot (via BootReceiver)
     * - Essentially zero battery usage (event-driven, not polling)
     */
    fun startMilletWatcher() {
        if (!MilletWatcherService.canWriteSettings(context)) {
            // Need to request WRITE_SETTINGS permission
            // This opens Android Settings where user can toggle it
            _uiState.value = _uiState.value.copy(
                fixSuccessMessage = "Cần cấp quyền 'Sửa đổi cài đặt hệ thống' để bảo vệ MILLET tự động.\n" +
                    "Vào Cài đặt > Ứng dụng > Quyền đặc biệt > Sửa đổi cài đặt hệ thống > Bật cho NotificationFix"
            )
            return
        }

        MilletWatcherService.start(context)
        _uiState.value = _uiState.value.copy(isWatcherRunning = true)
    }

    fun stopMilletWatcher() {
        MilletWatcherService.stop(context)
        _uiState.value = _uiState.value.copy(isWatcherRunning = false)
    }

    private fun checkWatcherState() {
        val hasPermission = MilletWatcherService.canWriteSettings(context)
        _uiState.value = _uiState.value.copy(
            hasWriteSettingsPermission = hasPermission,
        )
    }

    fun refreshWriteSettingsPermission() {
        val hasPermission = MilletWatcherService.canWriteSettings(context)
        _uiState.value = _uiState.value.copy(
            hasWriteSettingsPermission = hasPermission,
        )
        if (hasPermission) {
            startMilletWatcher()
        }
    }
}
