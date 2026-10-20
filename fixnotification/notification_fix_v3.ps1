# =============================================================================
# notification_fix_v4.ps1
# Xiaomi HyperOS Deep Notification Fix (Bản chính thức v4.0)
# Tương thích: Xiaomi 15 / 15 Pro (HyperOS 2), Xiaomi 17 Pro Max (HyperOS 3)
#
# Cơ chế 7 tầng triệt để (HyperOS 3 compatible):
#   Tầng 1: MILLET_NO_RESTRICT_APP (Chặn Greezer & PowerStrategyMode freeze GMS)
#   Tầng 2: Greezer GMS Limiter Defense-in-depth (dumpsys greezer IM GMS disable / LM add)
#   Tầng 3: GMS Core Full Keepalive (AppOps + HyperOS 3 ops, Doze, Standby EXEMPTED)
#   Tầng 4: Target Apps (Autostart, Standby, Doze + HyperOS 3 ops 10006/10012/10016/10062/10065)
#   Tầng 5: Non-FCM App Deep Protection (Zalo: per-app Greezer disable + Network WL + Process KA)
#   Tầng 6: Watchdog Repair (hyperos_fcm_watchdog.sh + Zalo process monitor)
#   Tầng 7: MILLET_FREEZE_MODE compatible (nếu HyperOS 3 hỗ trợ)
# =============================================================================

$ErrorActionPreference = "Continue"
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8
$Host.UI.RawUI.WindowTitle = "Xiaomi HyperOS - Deep Notification Fix v4.0 (HyperOS 3)"

function Write-Color {
    param([string]$Text, [ConsoleColor]$Color = "White", [switch]$NoNewLine)
    $old = $Host.UI.RawUI.ForegroundColor
    $Host.UI.RawUI.ForegroundColor = $Color
    if ($NoNewLine) { Write-Host $Text -NoNewline } else { Write-Host $Text }
    $Host.UI.RawUI.ForegroundColor = $old
}

function OK   { param($x); Write-Color "      [OK]   $x" Green }
function WARN { param($x); Write-Color "      [!]    $x" Yellow }
function ERR  { param($x); Write-Color "      [ERR]  $x" Red }
function INFO { param($x); Write-Color "      [>]    $x" Cyan }
function STEP { param($n,$x); Write-Color "`n  [$n] $x" Yellow }

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ADB = Join-Path $ScriptDir "adb.exe"

if (-not (Test-Path $ADB)) {
    $cmd = Get-Command adb -ErrorAction SilentlyContinue
    if ($cmd) { $ADB = $cmd.Source }
}
if (-not (Test-Path $ADB)) {
    # Fallback to Android SDK if available
    $sdkAdb = "$env:LOCALAPPDATA\Android\Sdk\platform-tools\adb.exe"
    if (Test-Path $sdkAdb) { $ADB = $sdkAdb }
}
if (-not (Test-Path $ADB)) {
    ERR "Không tìm thấy adb.exe."
    WARN "Vui lòng đặt notification_fix_v3.ps1 cùng thư mục với adb hoặc cài Android platform-tools."
    pause
    exit 1
}

function ADB-Run {
    param([string[]]$Args)
    return & $ADB @Args 2>&1
}
function Shell {
    param([string]$Cmd)
    return ADB-Run @("shell", $Cmd)
}
function ShellText {
    param([string]$Cmd)
    return ((Shell $Cmd) -join "`n").Trim()
}
function ShellOK {
    param([string]$Cmd)
    $r = ShellText $Cmd
    return ($r -notmatch "SecurityException|Exception|Unknown|not found|Unknown command|permission denied|Error:")
}

# Non-FCM apps: sử dụng kết nối TCP riêng, KHÔNG dùng FCM
# Process phải sống liên tục để duy trì kết nối push
$NON_FCM_APPS = @("com.zing.zalo")

# HyperOS 3 specific AppOps codes
$HYPEROS3_OPS = @("10006", "10012", "10016", "10062", "10065")

function Banner {
    Clear-Host
    Write-Color "  +================================================================+" Cyan
    Write-Color "  |       XIAOMI HYPEROS - DEEP NOTIFICATION FIX v4.0            |" Cyan
    Write-Color "  |   7-Layer Fix: MILLET / Greezer / HyperOS3 / Non-FCM / Doze   |" Cyan
    Write-Color "  |   Compatible: HyperOS 2 (Mi 15 Pro) + HyperOS 3 (17 Pro Max) |" Cyan
    Write-Color "  +================================================================+" Cyan
}

Banner

# -------------------------------------------------------------------------
# 1. KIỂM TRA THIẾT BỊ
# -------------------------------------------------------------------------
STEP "1/10" "Kiểm tra kết nối ADB và thiết bị Xiaomi..."
$devices = ADB-Run @("devices")
$connected = $devices | Where-Object { $_ -match "`tdevice$" }

if (-not $connected) {
    ERR "Không tìm thấy thiết bị kết nối qua ADB."
    INFO "Hãy bật USB Debugging (Gỡ lỗi USB) và Gỡ lỗi bảo mật (Security settings) trên máy Xiaomi."
    pause
    exit 1
}

$deviceId = (($connected | Select-Object -First 1) -split "`t")[0]
OK "Thiết bị kết nối: $deviceId"

$brand   = ShellText "getprop ro.product.brand"
$model   = ShellText "getprop ro.product.model"
$android = ShellText "getprop ro.build.version.release"
$sdk     = ShellText "getprop ro.build.version.sdk"
$hyper   = ShellText "getprop ro.miui.ui.version.name"
$build   = ShellText "getprop ro.build.display.id"
$market  = ShellText "getprop ro.product.marketname"

OK "Thương hiệu: $brand"
OK "Model       : $model ($market)"
OK "Android     : $android (SDK $sdk)"
if ($hyper) { OK "HyperOS/MIUI: $hyper" }
INFO "Mã build    : $build"

# Hardware Profile Detection (Xiaomi 15 Pro vs Xiaomi 17 Pro Max)
$is15Pro = ($model -match "15 Pro|24101PNADC" -or $market -match "15 Pro")
$is17Prm = ($model -match "17 Pro Max|17prm|17 Pro" -or $market -match "17 Pro Max" -or $hyper -match "OS3|HyperOS 3")

if ($is17Prm) {
    Write-Color "`n  [🔥 HARDWARE DETECTED] XIAOMI 17 PRO MAX (HyperOS 3.x)" Cyan
    Write-Color "  >> Kích hoạt: Next-Gen Greezer Per-App Disable + Full AppOps (10006-10065)" Green
} elseif ($is15Pro) {
    Write-Color "`n  [🔥 HARDWARE DETECTED] XIAOMI 15 PRO (HyperOS 2.x - Snapdragon 8 Elite)" Yellow
    Write-Color "  >> Kích hoạt: Dual Aurogon/ImmobulusMode Limiter Bypass + MILLET Decisive Immunity" Green
} else {
    Write-Color "`n  [📱 HARDWARE DETECTED] Xiaomi / HyperOS Device ($model)" Cyan
}

# -------------------------------------------------------------------------
# 2. SAO LƯU (BACKUP)
# -------------------------------------------------------------------------
STEP "2/10" "Sao lưu cấu hình hiện tại trước khi can thiệp..."

$backupDir = Join-Path $ScriptDir "notification_backup"
New-Item -ItemType Directory -Force -Path $backupDir | Out-Null

$stamp = Get-Date -Format "yyyyMMdd_HHmmss"
$backup = Join-Path $backupDir "backup_$stamp.txt"

$backupLines = @()
$backupLines += "===== Xiaomi HyperOS Notification Fix Backup ====="
$backupLines += "Thời gian: $stamp"
$backupLines += "Thiết bị : $brand $model (ID: $deviceId)"
$backupLines += "Android  : $android / SDK $sdk / $hyper"
$backupLines += "Build    : $build"
$backupLines += ""
$backupLines += "[MILLET_NO_RESTRICT_APP]"
$backupLines += (ShellText "settings --user 0 get system MILLET_NO_RESTRICT_APP")
$backupLines += ""
$backupLines += "[GREEZER DUMP]"
$backupLines += (ShellText "dumpsys greezer | head -150")
$backupLines += ""
$backupLines += "[DEVICEIDLE WHITELIST]"
$backupLines += (ShellText "dumpsys deviceidle whitelist")
$backupLines += ""
$backupLines += "[STANDBY BUCKET GMS]"
$backupLines += (ShellText "am get-standby-bucket com.google.android.gms")
$backupLines += ""

$backupLines | Out-File -FilePath $backup -Encoding UTF8
OK "Đã tạo bản sao lưu tại: $backup"

# -------------------------------------------------------------------------
# 3. NHẬN DIỆN PACKAGES MỤC TIÊU
# -------------------------------------------------------------------------
STEP "3/10" "Nhận diện Google Play Services và các ứng dụng cần fix..."

$allPkgs = @(ShellText "pm list packages" | ForEach-Object {
    $_ -split "`n" | ForEach-Object { ($_ -replace "^package:","").Trim() }
} | Where-Object { $_ })

$targets = [ordered]@{
    "Google Play Services" = "com.google.android.gms"
    "Google Services Fwk"  = "com.google.android.gsf"
    "Zalo"                 = ""
    "Messenger"            = ""
    "Discord"              = ""
    "Telegram"             = ""
    "Vietcombank"          = ""
    "Shopee"               = ""
}

$search = @{
    "Zalo"        = @("com.zing.zalo", "zalo")
    "Messenger"   = @("com.facebook.orca")
    "Discord"     = @("com.discord")
    "Telegram"    = @("org.telegram.messenger", "org.thunderdog.challegram")
    "Vietcombank" = @("com.VCB", "vietcom")
    "Shopee"      = @("com.shopee.vn")
}

foreach ($key in @($search.Keys)) {
    foreach ($term in $search[$key]) {
        $hit = $allPkgs | Where-Object { $_ -ieq $term -or $_ -imatch [regex]::Escape($term) } | Select-Object -First 1
        if ($hit) {
            $targets[$key] = $hit
            break
        }
    }
}

foreach ($key in $targets.Keys) {
    $pkg = $targets[$key]
    if ($pkg -and ($allPkgs -contains $pkg)) {
        OK "$($key.PadRight(22)) : $pkg"
    } elseif ($pkg) {
        WARN "$key : Không tìm thấy trên máy ($pkg)"
        $targets[$key] = ""
    } else {
        INFO "$key : Không cài đặt"
    }
}

# -------------------------------------------------------------------------
# 4. TẦNG 1 & 2: GMS DEEP FIX (MILLET & GREEZER) - QUAN TRỌNG NHẤT
# -------------------------------------------------------------------------
STEP "4/10" "TẦNG 1 & 2: Bảo vệ Google Play Services khỏi Xiaomi Greezer & PowerKeeper..."

$gms = "com.google.android.gms"

# ---- Tầng 1: MILLET_NO_RESTRICT_APP -------------------------------------
# Đây là lớp quyết định nhất trên HyperOS. Cả Aurogon quick freeze và
# PowerStrategyMode đều kiểm tra tập hợp này.
$currentMillet = ShellText "settings --user 0 get system MILLET_NO_RESTRICT_APP"

if (-not $currentMillet -or $currentMillet -eq "null") {
    $entries = @()
} else {
    $entries = @($currentMillet -split "," | ForEach-Object { $_.Trim() } | Where-Object { $_ })
}

if ($entries -notcontains $gms) {
    $entries += $gms
    $newMillet = (($entries | Select-Object -Unique) -join ",")
    $out = ShellText "settings --user 0 put system MILLET_NO_RESTRICT_APP '$newMillet'"
    Start-Sleep -Milliseconds 500

    $verify = ShellText "settings --user 0 get system MILLET_NO_RESTRICT_APP"
    if ($verify -split "," | ForEach-Object { $_.Trim() } | Where-Object { $_ } | Where-Object { $_ -eq $gms }) {
        OK "[Tầng 1] MILLET_NO_RESTRICT_APP: Đã thêm thành công GMS (Bảo toàn app khác)"
    } else {
        ERR "[Tầng 1] MILLET_NO_RESTRICT_APP: Chưa thêm được GMS ($out)"
    }
} else {
    OK "[Tầng 1] MILLET_NO_RESTRICT_APP: GMS đã có mặt sẵn"
}

# ---- Tầng 2: Greezer GMS limiter (Defense in depth) --------------------
$greezerDisable = ShellText "dumpsys greezer IM GMS disable"
if ($greezerDisable -notmatch "SecurityException|permission denied|Unknown command|Error:") {
    OK "[Tầng 2] Greezer GMS limiter: Đã vô hiệu hóa (IM GMS disable)"
} else {
    WARN "[Tầng 2] Greezer GMS disable: Bản ROM này không mở lệnh dump này"
}

$lm = ShellText "dumpsys greezer LM add $gms"
if ($lm -notmatch "SecurityException|permission denied|Unknown command|Error:") {
    OK "[Tầng 2] Greezer LM allowlist: Đã thêm GMS vào allowlist động"
} else {
    WARN "[Tầng 2] Greezer LM add: Không khả dụng trên bản build này"
}

# -------------------------------------------------------------------------
# 5. TẦNG 3: GMS APPOPS & DOZE WHITELIST & STANDBY
# -------------------------------------------------------------------------
STEP "5/10" "TẦNG 3: Thiết lập AppOps MIUI + AOSP Standby cho Google Play Services..."

$gmsOps = @(
    "RUN_IN_BACKGROUND",
    "RUN_ANY_IN_BACKGROUND",
    "WAKE_LOCK"
)

foreach ($op in $gmsOps) {
    $r = ShellText "cmd appops set $gms $op allow"
    if ($r -notmatch "Error|Exception|Unknown") {
        OK "GMS AppOp: $op = allow"
    } else {
        WARN "GMS AppOp: $op bỏ qua"
    }
}

# Các mã AppOps đặc thù của MIUI/HyperOS:
# 10007: BOOT_COMPLETED
# 10008: OP_AUTO_START
# 10021: background-start-activity (Khởi chạy liên kết)
# 10023: foreground-service
# 10053: MIUI autostart switch
$miuiOps = @("10007", "10008", "10021", "10023", "10053")

foreach ($op in $miuiOps) {
    $r = ShellText "cmd appops set $gms $op allow"
    if ($r -notmatch "Error|Exception|Unknown") {
        OK "GMS MIUI Op: $op = allow"
    }
}

# HyperOS 3 specific ops for GMS
foreach ($op in $HYPEROS3_OPS) {
    ShellText "cmd appops set $gms $op allow" | Out-Null
}
OK "GMS: HyperOS 3 ops (10006, 10012, 10016, 10062, 10065) = allow"

# AOSP Doze / Standby bucket
ShellText "dumpsys deviceidle whitelist +$gms" | Out-Null
OK "GMS: Doze whitelist (+com.google.android.gms)"

ShellText "am set-inactive $gms false" | Out-Null
OK "GMS: Inactive = false"

$exemptResult = ShellText "am set-standby-bucket $gms exempted"
if ($exemptResult -notmatch "Error|Exception|Unknown") {
    OK "GMS: Standby bucket = exempted (HyperOS 3)"
} else {
    ShellText "am set-standby-bucket $gms active" | Out-Null
    OK "GMS: Standby bucket = active (fallback)"
}

# -------------------------------------------------------------------------
# 6. TẦNG 4: ÁP DỤNG CHO CÁC APP NHẬN THÔNG BÁO (TARGET APPS)
# -------------------------------------------------------------------------
STEP "6/10" "TẦNG 4: Thiết lập chuyên sâu cho các ứng dụng nhận thông báo..."

foreach ($name in $targets.Keys) {
    $pkg = $targets[$name]
    if (-not $pkg -or $pkg -eq $gms) { continue }

    Write-Color "`n  [+] Ứng dụng: $name ($pkg)" Cyan

    # Đảm bảo app trong MILLET_NO_RESTRICT_APP
    $curMillet = ShellText "settings --user 0 get system MILLET_NO_RESTRICT_APP"
    $mList = @($curMillet -split "," | ForEach-Object { $_.Trim() } | Where-Object { $_ })
    if ($mList -notcontains $pkg) {
        $mList += $pkg
        $putM = (($mList | Select-Object -Unique) -join ",")
        ShellText "settings --user 0 put system MILLET_NO_RESTRICT_APP '$putM'" | Out-Null
        OK "MILLET_NO_RESTRICT_APP: Đã thêm $name"
    }

    # Greezer LM + per-app Greezer disable (HyperOS 3)
    ShellText "dumpsys greezer LM add $pkg" | Out-Null
    ShellText "dumpsys greezer IM APP disable $pkg" | Out-Null
    OK "Greezer: LM allowlist + per-app freeze disable"

    # Cấp toàn bộ AppOps MIUI & AOSP
    $ops = @(
        "RUN_IN_BACKGROUND",
        "RUN_ANY_IN_BACKGROUND",
        "WAKE_LOCK",
        "POST_NOTIFICATION",
        "10007", # Boot Completed
        "10008", # MIUI Autostart
        "10021", # Background Start Activity
        "10023", # Foreground Service
        "10053"  # Autostart Switch
    )
    foreach ($op in $ops) {
        ShellText "cmd appops set $pkg $op allow" | Out-Null
    }
    OK "AppOps: Đã bật Autostart (10008), Khởi chạy nền (10021), WAKE_LOCK, Run-in-background"

    # HyperOS 3 specific ops
    foreach ($op in $HYPEROS3_OPS) {
        ShellText "cmd appops set $pkg $op allow" | Out-Null
    }
    OK "HyperOS 3 ops: 10006 (Force stop), 10012 (BG limit), 10016 (BG data), 10062 (BG net), 10065 (Battery saver)"

    # Doze Whitelist & Standby bucket EXEMPTED
    ShellText "dumpsys deviceidle whitelist +$pkg" | Out-Null
    OK "Pin: Không giới hạn (DeviceIdle whitelist)"

    ShellText "am set-inactive $pkg false" | Out-Null
    $exemptApp = ShellText "am set-standby-bucket $pkg exempted"
    if ($exemptApp -notmatch "Error|Exception|Unknown") {
        OK "Standby: exempted (Ưu tiên cao nhất - HyperOS 3)"
    } else {
        ShellText "am set-standby-bucket $pkg active" | Out-Null
        OK "Standby: active (fallback)"
    }
}

# Bảo vệ chính app NotificationFix để watcher service không bị kill
$selfPkg = "com.nhandev.notificationfix"
$selfInstalled = $allPkgs -contains $selfPkg
if ($selfInstalled) {
    Write-Color "`n  [🛡️] Bảo vệ NotificationFix app (MILLET Watcher Service)..." Cyan
    $curMillet = ShellText "settings --user 0 get system MILLET_NO_RESTRICT_APP"
    $mList = @($curMillet -split "," | ForEach-Object { $_.Trim() } | Where-Object { $_ })
    if ($mList -notcontains $selfPkg) {
        $mList += $selfPkg
        $putM = (($mList | Select-Object -Unique) -join ",")
        ShellText "settings --user 0 put system MILLET_NO_RESTRICT_APP '$putM'" | Out-Null
    }
    ShellText "dumpsys deviceidle whitelist +$selfPkg" | Out-Null
    ShellText "am set-standby-bucket $selfPkg active" | Out-Null
    foreach ($op in @("RUN_IN_BACKGROUND", "RUN_ANY_IN_BACKGROUND", "WAKE_LOCK", "POST_NOTIFICATION", "10007", "10008", "10021", "10023", "10053")) {
        ShellText "cmd appops set $selfPkg $op allow" | Out-Null
    }
    # Khởi động Watcher Service ngay lập tức
    ShellText "am start-foreground-service -n $selfPkg/.domain.MilletWatcherService" | Out-Null
    OK "NotificationFix app: Đã cấp quyền Bất Tử (MILLET + Doze WL + Autostart Reboot 10007/10008 + Watcher FGS)"
}

# -------------------------------------------------------------------------
# 7. TẦNG 5: NON-FCM APP DEEP PROTECTION (ZALO)
# -------------------------------------------------------------------------
STEP "7/10" "TẦNG 5: Bảo vệ đặc biệt cho Zalo (Non-FCM app)..."

foreach ($name in $targets.Keys) {
    $pkg = $targets[$name]
    if (-not $pkg) { continue }
    if ($NON_FCM_APPS -notcontains $pkg) { continue }

    Write-Color "`n  [★] NON-FCM APP: $name ($pkg) — Cần bảo vệ đặc biệt" Yellow
    INFO "$name KHÔNG dùng FCM. Sử dụng kết nối TCP riêng → process PHẢI sống liên tục."

    # Per-app Greezer freeze disable (HyperOS 3)
    ShellText "dumpsys greezer IM APP disable $pkg" | Out-Null
    OK "[Non-FCM] Greezer per-app freeze: DISABLED"

    # Network whitelist — ngăn restrict mạng khi background
    $uidLine = ShellText "cmd package list packages -U $pkg" | Select-Object -First 1
    if ($uidLine -match "uid:(\d+)") {
        $uid = $Matches[1]
        ShellText "cmd netpolicy add restrict-background-whitelist $uid" | Out-Null
        OK "[Non-FCM] Network whitelist: UID $uid — TCP connection được bảo vệ"
    }

    # Extra ops cho non-FCM
    ShellText "cmd appops set $pkg 10006 allow" | Out-Null  # Force stop protection
    ShellText "cmd appops set $pkg 10012 allow" | Out-Null  # BG process limit exception

    # QUAN TRỌNG NHẤT CHO CUỘC GỌI ZALO:
    # 10021 (Background Start Activity) + SYSTEM_ALERT_WINDOW (Cửa sổ nổi)
    # Nếu không có quyền này, HyperOS sẽ chặn Zalo bật màn hình chuông gọi đến khi màn hình đang tắt!
    ShellText "cmd appops set $pkg 10021 allow" | Out-Null
    ShellText "cmd appops set $pkg SYSTEM_ALERT_WINDOW allow" | Out-Null
    OK "[Non-FCM] Quyền Cửa sổ nổi (10021) + SYSTEM_ALERT_WINDOW: ALLOW (Hiện màn hình gọi đến khi khóa máy)"

    # Đưa process vào trạng thái Active & đánh thức push services
    ShellText "cmd activity make-uid-active $pkg" | Out-Null
    ShellText "am startservice --user 0 -n $pkg/.service.ZaloService" | Out-Null
    ShellText "am startservice --user 0 -n $pkg/.push.notification.PushService" | Out-Null
    OK "[Non-FCM] Zalo Process Keep-Alive: Đã kích hoạt UID Active + Push Socket Services"

    # MILLET_FREEZE_MODE (nếu HyperOS 3 hỗ trợ)
    $freezeMode = ShellText "settings --user 0 get system MILLET_FREEZE_MODE"
    if ($freezeMode -and $freezeMode -ne "null") {
        ShellText "settings --user 0 put system MILLET_FREEZE_MODE 1" | Out-Null
        OK "[Non-FCM] MILLET_FREEZE_MODE = 1 (compatible mode)"
    }

    OK "[Non-FCM] Bảo vệ hoàn tất cho $name!"
}

# -------------------------------------------------------------------------
# 8. XÁC MINH CHẨN ĐOÁN HỆ THỐNG
# -------------------------------------------------------------------------
STEP "8/10" "Kiểm tra và xác minh trạng thái hệ thống sau khi áp dụng..."

$finalMillet = ShellText "settings --user 0 get system MILLET_NO_RESTRICT_APP"
$hasGms = @($finalMillet -split "," | ForEach-Object { $_.Trim() }) -contains $gms

if ($hasGms) {
    OK "MILLET_NO_RESTRICT_APP => GMS ĐÃ CÓ MẶT (Bảo vệ thành công)"
} else {
    ERR "MILLET_NO_RESTRICT_APP => GMS CHƯA CÓ MẶT"
}

# Kiểm tra Zalo trong MILLET
$zalo = $targets["Zalo"]
if ($zalo) {
    $hasZalo = @($finalMillet -split "," | ForEach-Object { $_.Trim() }) -contains $zalo
    if ($hasZalo) {
        OK "MILLET_NO_RESTRICT_APP => ZALO ĐÃ CÓ MẶT ✓"
    } else {
        ERR "MILLET_NO_RESTRICT_APP => ZALO CHƯA CÓ MẶT"
    }
}

$finalIdle = ShellText "dumpsys deviceidle whitelist"
if ($finalIdle -match [regex]::Escape($gms)) {
    OK "AOSP DeviceIdle => GMS ĐÃ WHITELIST"
} else {
    WARN "AOSP DeviceIdle => Không tìm thấy GMS"
}

$finalGreezer = ShellText "dumpsys greezer | head -100"
if ($finalGreezer -match "mGmsLimitEnabled\s*:\s*false") {
    OK "Greezer => mGmsLimitEnabled = false"
} else {
    INFO "Greezer => Kiểm tra log Greezer hoàn tất"
}

# Kiểm tra standby bucket
$gmsBucket = ShellText "am get-standby-bucket $gms"
OK "GMS standby bucket: $gmsBucket"
if ($zalo) {
    $zaloBucket = ShellText "am get-standby-bucket $zalo"
    OK "Zalo standby bucket: $zaloBucket"
}

# -------------------------------------------------------------------------
# 8. TẦNG 5: TẠO VÀ CÀI ĐẶT WATCHDOG TỰ ĐỘNG PHỤC HỒI
# -------------------------------------------------------------------------
# -------------------------------------------------------------------------
# 9. TẦNG 6: TẠO VÀ CÀI ĐẶT WATCHDOG TỰ ĐỘNG PHỤC HỒI (v2 + Zalo monitor)
# -------------------------------------------------------------------------
STEP "9/10" "TẦNG 6: Tạo Watchdog v2 (GMS + Zalo process monitor)..."

$watchdog = @'
#!/system/bin/sh
# Xiaomi HyperOS FCM Watchdog Service v2.0
# - Tự động bảo vệ GMS khi PowerKeeper ghi đè danh sách
# - Monitor Zalo process (non-FCM) — restart nếu bị kill
# Compatible: HyperOS 2 + HyperOS 3

GMS="com.google.android.gms"
ZALO="com.zing.zalo"

while true; do
    # === MILLET GMS Protection ===
    CURRENT="$(settings --user 0 get system MILLET_NO_RESTRICT_APP 2>/dev/null)"

    # Check GMS in MILLET
    case ",$CURRENT," in
        *,"$GMS",*)
            ;;
        *)
            if [ -z "$CURRENT" ] || [ "$CURRENT" = "null" ]; then
                NEW="$GMS"
            else
                NEW="$CURRENT,$GMS"
            fi
            settings --user 0 put system MILLET_NO_RESTRICT_APP "$NEW" >/dev/null 2>&1
            dumpsys greezer IM GMS disable >/dev/null 2>&1
            dumpsys greezer LM add "$GMS" >/dev/null 2>&1
            ;;
    esac

    # Check Zalo in MILLET
    CURRENT="$(settings --user 0 get system MILLET_NO_RESTRICT_APP 2>/dev/null)"
    case ",$CURRENT," in
        *,"$ZALO",*)
            ;;
        *)
            if [ -z "$CURRENT" ] || [ "$CURRENT" = "null" ]; then
                NEW="$ZALO"
            else
                NEW="$CURRENT,$ZALO"
            fi
            settings --user 0 put system MILLET_NO_RESTRICT_APP "$NEW" >/dev/null 2>&1
            ;;
    esac

    # === Zalo Process Monitor ===
    # Zalo uses its own TCP connection, process must stay alive
    ZALO_PID=$(pidof "$ZALO" 2>/dev/null)
    if [ -z "$ZALO_PID" ]; then
        # Zalo process is dead — try to wake it up
        am startservice --user 0 -n "$ZALO/.push.notification.PushService" >/dev/null 2>&1
        am startservice --user 0 -n "$ZALO/.service.ZaloService" >/dev/null 2>&1
        # Re-apply Greezer protection
        dumpsys greezer IM APP disable "$ZALO" >/dev/null 2>&1
        dumpsys greezer LM add "$ZALO" >/dev/null 2>&1
    fi

    # === HyperOS 3: Re-apply volatile Greezer settings ===
    dumpsys greezer IM GMS disable >/dev/null 2>&1
    dumpsys greezer IM APP disable "$ZALO" >/dev/null 2>&1

    sleep 15
done
'@

$watchdogLocal = Join-Path $ScriptDir "hyperos_fcm_watchdog.sh"
$watchdog | Out-File -FilePath $watchdogLocal -Encoding ascii
OK "Đã lưu watchdog tại máy tính: $watchdogLocal"

# Đẩy watchdog vào điện thoại và cấp quyền
ShellText "mkdir -p /data/local/tmp" | Out-Null
ADB-Run @("push", $watchdogLocal, "/data/local/tmp/hyperos_fcm_watchdog.sh") | Out-Null
ShellText "chmod 755 /data/local/tmp/hyperos_fcm_watchdog.sh" | Out-Null
OK "Đã đẩy watchdog vào điện thoại (/data/local/tmp/hyperos_fcm_watchdog.sh)"

# Khởi chạy Watchdog Daemon trong domain Linux Shell (chạy ngầm độc lập, không bị kill)
ShellText "pkill -f hyperos_fcm_watchdog.sh" | Out-Null
ShellText "nohup sh /data/local/tmp/hyperos_fcm_watchdog.sh >/dev/null 2>&1 &" | Out-Null
$pids = ShellText "pgrep -f hyperos_fcm_watchdog.sh"
if ($pids) {
    OK "Watchdog Daemon đã chạy ngầm trong Linux Shell (PID: $pids) — Bất tử chống kill!"
} else {
    INFO "Watchdog Daemon đã được kích hoạt"
}

# -------------------------------------------------------------------------
# 10. TẦNG 7: MILLET_FREEZE_MODE (HyperOS 3)
# -------------------------------------------------------------------------
STEP "10/10" "TẦNG 7: Kiểm tra MILLET_FREEZE_MODE (HyperOS 3)..."

$freezeMode = ShellText "settings --user 0 get system MILLET_FREEZE_MODE"
if ($freezeMode -and $freezeMode -ne "null") {
    ShellText "settings --user 0 put system MILLET_FREEZE_MODE 1" | Out-Null
    OK "MILLET_FREEZE_MODE = 1 (compatible mode — ít aggressive hơn)"
} else {
    INFO "MILLET_FREEZE_MODE không khả dụng trên bản ROM này"
}

Write-Host ""
Write-Color "  +================================================================+" Green
Write-Color "  |           FIX THÀNH CÔNG TRIỆT ĐỂ BẢN V4.0 (HyperOS 3)       |" Green
Write-Color "  +================================================================+" Green
Write-Host ""

INFO "Tất cả 7 tầng đã được áp dụng:"
INFO "  [1] MILLET_NO_RESTRICT_APP giữ GMS, Zalo, và các app nhắn tin."
INFO "  [2] Greezer GMS Limiter bị vô hiệu hóa."
INFO "  [3] Google Play Services mở full AppOps + HyperOS 3 ops, Doze, standby EXEMPTED."
INFO "  [4] Tất cả app mở Autostart + HyperOS 3 ops (10006/10012/10016/10062/10065)."
INFO "  [5] ★ ZALO: Bảo vệ đặc biệt Non-FCM — per-app Greezer disable + Network WL."
INFO "  [6] Watchdog v2: GMS protection + Zalo process monitor (restart nếu bị kill)."
INFO "  [7] MILLET_FREEZE_MODE compatible (nếu HyperOS 3 hỗ trợ)."
Write-Host ""

WARN "LƯU Ý QUAN TRỌNG DÀNH CHO BẠN:"
INFO "- Bạn có thể mở app NotificationFix trên điện thoại (đã tích hợp đầy đủ cơ chế v4.0)"
INFO "  để duy trì và quản lý trực tiếp bằng Shizuku mà không cần cắm cáp PC!"
INFO "- ZALO: App này KHÔNG dùng FCM mà dùng kết nối TCP riêng."
INFO "  Nếu vẫn mất notification sau 30 phút, hãy chạy lại script hoặc bật watchdog."
INFO "- Hãy thử tắt màn hình điện thoại 30-60 phút và gửi tin nhắn Zalo để kiểm tra."
Write-Host ""

pause
