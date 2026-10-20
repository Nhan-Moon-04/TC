# =============================================================================
#  notification_fix.ps1
#  Fix push notification delay tren Xiaomi 15 Pro / HyperOS
#  Danh muc: Google FCM + Zalo + Messenger + Vietcombank
#  Tac gia: Antigravity
#  Phien ban: 2.0
#  Cach dung: Dat file nay vao D:\platform-tools, ket noi dien thoai, chay
#             PowerShell va nhap: .\notification_fix.ps1
# =============================================================================

$Host.UI.RawUI.WindowTitle = "Xiaomi HyperOS - Notification Fix Tool"

# ---- Mau sac trong terminal ---------------------------------------------------
function Write-Color {
    param([string]$Text, [ConsoleColor]$Color = "White", [switch]$NoNewLine)
    $prev = $Host.UI.RawUI.ForegroundColor
    $Host.UI.RawUI.ForegroundColor = $Color
    if ($NoNewLine) { Write-Host $Text -NoNewline } else { Write-Host $Text }
    $Host.UI.RawUI.ForegroundColor = $prev
}

function Write-Banner {
    Clear-Host
    Write-Color "  +==============================================================+" Cyan
    Write-Color "  |       XIAOMI HYPEROS - NOTIFICATION FIX TOOL v2.0           |" Cyan
    Write-Color "  |       Fix push notification delay | Zalo Messenger VCB       |" Cyan
    Write-Color "  +==============================================================+" Cyan
    Write-Host ""
}

function Write-Step {
    param([string]$Num, [string]$Msg)
    Write-Color "  [$Num] " Yellow -NoNewLine
    Write-Color $Msg White
}

function Write-OK   { param([string]$Msg); Write-Color "      OK  $Msg" Green }
function Write-FAIL { param([string]$Msg); Write-Color "      ERR $Msg" Red }
function Write-INFO { param([string]$Msg); Write-Color "      --> $Msg" DarkCyan }
function Write-WARN { param([string]$Msg); Write-Color "      !   $Msg" Yellow }

# ---- Duong dan ADB -----------------------------------------------------------
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ADB = Join-Path $ScriptDir "adb.exe"
if (-not (Test-Path $ADB)) {
    $adbCmd = Get-Command adb -ErrorAction SilentlyContinue
    if ($adbCmd) { $ADB = $adbCmd.Source }
    else {
        Write-Color "`n  [LOI] Khong tim thay adb.exe trong: $ScriptDir" Red
        Write-Color "  Hay dat file nay cung thu muc voi adb.exe" Yellow
        pause; exit 1
    }
}

# ---- Ham ADB -----------------------------------------------------------------
function ADB-Shell {
    param([string]$Cmd)
    return & $ADB shell $Cmd 2>&1
}

# ==============================================================================
Write-Banner

# ---- BUOC 1: Kiem tra thiet bi -----------------------------------------------
Write-Step "1/7" "Kiem tra ket noi thiet bi..."
Write-Host ""

$devices = & $ADB devices 2>&1
$connected = $devices | Where-Object { $_ -match "`tdevice$" }

if (-not $connected) {
    Write-FAIL "Khong tim thay thiet bi nao!"
    Write-WARN "Hay kiem tra:"
    Write-INFO "- Dien thoai da bat Developer Options + USB Debugging"
    Write-INFO "- Cap USB da cam"
    Write-INFO "- Giao thuc USB: File Transfer (MTP)"
    Write-INFO "- Da chap nhan RSA fingerprint tren man hinh dien thoai"
    Write-Host ""
    pause; exit 1
}

$deviceId = (($connected | Select-Object -First 1) -split "`t")[0]
Write-OK "Da ket noi: $deviceId"

$brand  = (ADB-Shell "getprop ro.product.brand") -join "" | ForEach-Object { $_.Trim() }
$model  = (ADB-Shell "getprop ro.product.model") -join "" | ForEach-Object { $_.Trim() }
$osVer  = (ADB-Shell "getprop ro.build.version.release") -join ""
$hyper  = (ADB-Shell "getprop ro.miui.ui.version.name") -join ""
$sdk    = (ADB-Shell "getprop ro.build.version.sdk") -join ""

Write-OK "Thiet bi : $brand $model"
Write-OK "Android  : $osVer  (SDK $sdk)"
if ($hyper -and $hyper -ne "") { Write-OK "HyperOS  : $hyper" }
Write-Host ""

if ($brand -notmatch "Xiaomi|POCO|Redmi") {
    Write-WARN "Thiet bi khong phai Xiaomi -- mot so toi uu co the khong can thiet"
    Write-Host ""
}

# ==============================================================================
# ---- BUOC 2: Kiem tra Google Play Services -----------------------------------
Write-Step "2/7" "Kiem tra Google Play Services & GSF..."
Write-Host ""

$gmsVer = ADB-Shell "dumpsys package com.google.android.gms 2>/dev/null | grep versionName | head -1"
$gsfVer = ADB-Shell "dumpsys package com.google.android.gsf 2>/dev/null | grep versionName | head -1"

foreach ($line in $gmsVer) {
    if ($line -match "versionName=(\S+)") {
        Write-OK "Google Play Services : $($Matches[1])"
    }
}
foreach ($line in $gsfVer) {
    if ($line -match "versionName=(\S+)") {
        Write-OK "Google Services Framework : $($Matches[1])"
    }
}

$idleList   = ADB-Shell "dumpsys deviceidle whitelist 2>/dev/null"
$gmsInList  = $idleList | Where-Object { $_ -match "com.google.android.gms" }
if ($gmsInList) {
    Write-OK "GMS da co trong Doze whitelist"
} else {
    Write-WARN "GMS CHUA co trong Doze whitelist -- se duoc them o buoc tiep theo"
}
Write-Host ""

# ==============================================================================
# ---- BUOC 3: Xac dinh package name cac app -----------------------------------
Write-Step "3/7" "Quet package name cac app tren may..."
Write-Host ""

# Dung class de luu thong tin app
class AppInfo {
    [string]$Name
    [string]$Search
    [string]$Package
}

$AppTargets = @(
    [AppInfo]@{ Name = "Google GMS";  Search = "gms";        Package = "com.google.android.gms" }
    [AppInfo]@{ Name = "Google GSF";  Search = "gsf";        Package = "com.google.android.gsf" }
    [AppInfo]@{ Name = "Zalo";        Search = "zalo";       Package = "" }
    [AppInfo]@{ Name = "Messenger";   Search = "orca";       Package = "" }
    [AppInfo]@{ Name = "Vietcombank"; Search = "vcb|vietcom";Package = "" }
)

$allPkgs = ADB-Shell "pm list packages 2>/dev/null" |
           ForEach-Object { ($_ -replace "^package:", "").Trim() } |
           Where-Object { $_ -ne "" }

foreach ($app in $AppTargets) {
    if ($app.Package -ne "") {
        $found = $allPkgs | Where-Object { $_ -eq $app.Package }
        if ($found) {
            Write-OK "$($app.Name.PadRight(20)) $($app.Package)"
        } else {
            Write-WARN "$($app.Name.PadRight(20)) KHONG CO TREN MAY: $($app.Package)"
            $app.Package = ""
        }
    } else {
        $terms = $app.Search -split "\|"
        $found = $null
        foreach ($pkg in $allPkgs) {
            foreach ($term in $terms) {
                if ($pkg -imatch $term) { $found = $pkg; break }
            }
            if ($found) { break }
        }
        if ($found) {
            $app.Package = $found
            Write-OK "$($app.Name.PadRight(20)) $found"
        } else {
            Write-WARN "$($app.Name.PadRight(20)) KHONG TIM THAY (keyword: $($app.Search))"
        }
    }
}
Write-Host ""

$AppsToFix = $AppTargets | Where-Object { $_.Package -ne "" }

if ($AppsToFix.Count -eq 0) {
    Write-FAIL "Khong tim thay bat ky app nao de fix!"
    pause; exit 1
}

# ==============================================================================
# ---- BUOC 4: Xac nhan voi nguoi dung ----------------------------------------
Write-Step "4/7" "Xac nhan truoc khi chay fix..."
Write-Host ""
Write-Color "  Cac app se duoc xu ly:" Cyan
foreach ($app in $AppsToFix) {
    Write-Color "    * $($app.Name.PadRight(20)) $($app.Package)" White
}
Write-Host ""
Write-Color "  Thao tac se thuc hien cho tung app:" Cyan
Write-INFO "WAKE_LOCK = allow"
Write-INFO "RUN_ANY_IN_BACKGROUND = allow"
Write-INFO "DeviceIdle whitelist = them vao"
Write-Host ""

Write-Color "  Tiep tuc? Nhap Y de dong y, bat ky phim khac de huy: " Yellow -NoNewLine
$confirm = Read-Host
if ($confirm -notmatch "^[Yy]$") {
    Write-WARN "Da huy. Khong co thay doi nao."
    pause; exit 0
}
Write-Host ""

# ==============================================================================
# ---- BUOC 5: Ap dung fix cho tung app ----------------------------------------
Write-Step "5/7" "Dang ap dung fix..."
Write-Host ""

foreach ($app in $AppsToFix) {
    $pkg  = $app.Package
    $name = $app.Name
    Write-Color "  -- $name ($pkg) --" Cyan

    # WAKE_LOCK
    $null = ADB-Shell "cmd appops set $pkg WAKE_LOCK allow"
    Write-OK "WAKE_LOCK allow"

    # RUN_ANY_IN_BACKGROUND
    $null = ADB-Shell "cmd appops set $pkg RUN_ANY_IN_BACKGROUND allow"
    Write-OK "RUN_ANY_IN_BACKGROUND allow"

    # DeviceIdle whitelist
    $r3 = ADB-Shell "dumpsys deviceidle whitelist +$pkg"
    if (($r3 -join "") -notmatch "Error|Exception") {
        Write-OK "DeviceIdle whitelist: da them"
    } else {
        Write-FAIL "DeviceIdle whitelist: $(($r3 -join '') )"
    }

    Write-Host ""
}

# ==============================================================================
# ---- BUOC 6: Kiem tra Doze whitelist sau fix ---------------------------------
Write-Step "6/7" "Kiem tra Doze whitelist sau khi fix..."
Write-Host ""

$finalList = ADB-Shell "dumpsys deviceidle whitelist 2>/dev/null"

foreach ($app in $AppsToFix) {
    $inList = $finalList | Where-Object { $_ -match [regex]::Escape($app.Package) }
    if ($inList) {
        Write-OK "$($app.Name.PadRight(20)) co trong whitelist"
    } else {
        Write-WARN "$($app.Name.PadRight(20)) CHUA trong whitelist"
    }
}
Write-Host ""

# ==============================================================================
# ---- BUOC 7: Hoan thanh ------------------------------------------------------
Write-Step "7/7" "Hoan thanh!"
Write-Host ""
Write-Color "  +==============================================================+" Green
Write-Color "  |                    FIX HOAN THANH                          |" Green
Write-Color "  +==============================================================+" Green
Write-Host ""
Write-Color "  Cac fix da duoc ap dung qua ADB." Green
Write-Host ""
Write-Color "  PHAI LAM THEM THU CONG tren may (quan trong):" Yellow
Write-INFO "Settings > Apps > Manage Apps > [Ten app] >"
Write-INFO "  Notifications       : BAT ON"
Write-INFO "  Battery > No restrictions"
Write-INFO "  Background autostart: BAT ON"
Write-INFO "  Lock app (neu co)   : BAT ON"
Write-Host ""
Write-Color "  De hoan tac: chay .\notification_restore.ps1" DarkCyan
Write-Host ""

# Ghi log
$logPath  = Join-Path $ScriptDir "notification_fix_log.txt"
$timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
$logLines  = @(
    "===== notification_fix.ps1 ====="
    "Thoi gian : $timestamp"
    "Thiet bi  : $brand $model"
    "Android   : $osVer"
    "HyperOS   : $hyper"
    ""
    "App da fix:"
)
foreach ($app in $AppsToFix) {
    $logLines += "  $($app.Name) -- $($app.Package)"
}
$logLines += ""
$logLines | Out-File -FilePath $logPath -Encoding UTF8 -Append
Write-INFO "Log da luu tai: $logPath"
Write-Host ""

pause
