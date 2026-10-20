# =============================================================================
#  notification_restore.ps1
#  Hoan tac fix push notification da thuc hien boi notification_fix.ps1
#  Tac gia: Antigravity
#  Phien ban: 2.0
#  Cach dung: .\notification_restore.ps1
# =============================================================================

$Host.UI.RawUI.WindowTitle = "Xiaomi HyperOS - Notification RESTORE Tool"

function Write-Color {
    param([string]$Text, [ConsoleColor]$Color = "White", [switch]$NoNewLine)
    $prev = $Host.UI.RawUI.ForegroundColor
    $Host.UI.RawUI.ForegroundColor = $Color
    if ($NoNewLine) { Write-Host $Text -NoNewline } else { Write-Host $Text }
    $Host.UI.RawUI.ForegroundColor = $prev
}

function Write-Step { param([string]$N, [string]$M)
    Write-Color "  [$N] " Yellow -NoNewLine; Write-Color $M White }
function Write-OK   { param([string]$M); Write-Color "      OK  $M" Green }
function Write-FAIL { param([string]$M); Write-Color "      ERR $M" Red }
function Write-INFO { param([string]$M); Write-Color "      --> $M" DarkCyan }
function Write-WARN { param([string]$M); Write-Color "      !   $M" Yellow }

$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$ADB = Join-Path $ScriptDir "adb.exe"
if (-not (Test-Path $ADB)) {
    $adbCmd = Get-Command adb -ErrorAction SilentlyContinue
    if ($adbCmd) { $ADB = $adbCmd.Source }
    else { Write-Color "`n  [LOI] Khong tim thay adb.exe" Red; pause; exit 1 }
}

function ADB-Shell { param([string]$Cmd); return & $ADB shell $Cmd 2>&1 }

# ==============================================================================
Clear-Host
Write-Color "  +==============================================================+" Red
Write-Color "  |       XIAOMI HYPEROS - NOTIFICATION RESTORE TOOL v2.0       |" Red
Write-Color "  |       Hoan tac tat ca thay doi cua notification_fix.ps1      |" Red
Write-Color "  +==============================================================+" Red
Write-Host ""

# ---- Kiem tra thiet bi -------------------------------------------------------
Write-Step "1/4" "Kiem tra ket noi thiet bi..."
Write-Host ""

$devices   = & $ADB devices 2>&1
$connected = $devices | Where-Object { $_ -match "`tdevice$" }
if (-not $connected) {
    Write-FAIL "Khong tim thay thiet bi! Hay kiem tra cap USB + USB Debugging."
    pause; exit 1
}
$deviceId = (($connected | Select-Object -First 1) -split "`t")[0]
Write-OK "Da ket noi: $deviceId"
Write-Host ""

# ---- Danh sach app can hoan tac ----------------------------------------------
Write-Step "2/4" "Doc log fix truoc do..."
Write-Host ""

$logPath = Join-Path $ScriptDir "notification_fix_log.txt"
$restoredPkgs = @()

if (Test-Path $logPath) {
    $logContent = Get-Content $logPath
    foreach ($line in $logContent) {
        if ($line -match "^\s+\S+ -- ([\w\.]+)$") {
            $restoredPkgs += $Matches[1]
        }
    }
    if ($restoredPkgs.Count -gt 0) {
        Write-OK "Tim thay $($restoredPkgs.Count) app da fix trong log:"
        foreach ($pkg in $restoredPkgs) { Write-INFO $pkg }
    } else {
        Write-WARN "Khong doc duoc package nao tu log. Dung danh sach mac dinh."
    }
} else {
    Write-WARN "Khong tim thay log file. Dung danh sach mac dinh."
}

# Neu khong co log, dung danh sach mac dinh
if ($restoredPkgs.Count -eq 0) {
    $restoredPkgs = @(
        "com.google.android.gms"
        "com.google.android.gsf"
        "com.zing.zalo"
        "com.facebook.orca"
    )
    Write-WARN "Su dung danh sach mac dinh:"
    foreach ($pkg in $restoredPkgs) { Write-INFO $pkg }
}

Write-Host ""

# ---- Xac nhan ----------------------------------------------------------------
Write-Color "  CANH BAO: Thao tac nay se XOA cac fix notification da thuc hien." Yellow
Write-Color "  App co the bi tre thong bao tro lai neu HyperOS han che nen." Yellow
Write-Host ""
Write-Color "  Tiep tuc? Nhap Y de dong y, bat ky phim khac de huy: " Yellow -NoNewLine
$confirm = Read-Host
if ($confirm -notmatch "^[Yy]$") {
    Write-WARN "Da huy. Khong co thay doi nao."
    pause; exit 0
}
Write-Host ""

# ---- Hoan tac ----------------------------------------------------------------
Write-Step "3/4" "Dang hoan tac..."
Write-Host ""

foreach ($pkg in $restoredPkgs) {
    Write-Color "  -- $pkg --" Cyan

    # Reset WAKE_LOCK ve mac dinh
    $null = ADB-Shell "cmd appops set $pkg WAKE_LOCK default"
    Write-OK "WAKE_LOCK reset -> default"

    # Reset RUN_ANY_IN_BACKGROUND ve mac dinh
    $null = ADB-Shell "cmd appops set $pkg RUN_ANY_IN_BACKGROUND default"
    Write-OK "RUN_ANY_IN_BACKGROUND reset -> default"

    # Xoa khoi DeviceIdle whitelist
    $r = ADB-Shell "dumpsys deviceidle whitelist -$pkg"
    if (($r -join "") -notmatch "Error|Exception") {
        Write-OK "DeviceIdle whitelist: da xoa"
    } else {
        Write-WARN "DeviceIdle whitelist: $( ($r -join '') )"
    }
    Write-Host ""
}

# ---- Ket qua -----------------------------------------------------------------
Write-Step "4/4" "Hoan thanh hoan tac!"
Write-Host ""
Write-Color "  Tat ca cac fix da duoc hoat tac." Green
Write-Color "  Doze va background restrictions da ve trang thai mac dinh." Green
Write-Host ""

# Ghi log restore
$timestamp = Get-Date -Format "yyyy-MM-dd HH:mm:ss"
$logLines = @(
    "===== notification_restore.ps1 ====="
    "Thoi gian hoan tac: $timestamp"
    "Package da hoan tac:"
)
foreach ($pkg in $restoredPkgs) { $logLines += "  - $pkg" }
$logLines += ""
$logLines | Out-File -FilePath $logPath -Encoding UTF8 -Append
Write-Color "      --> Log hoan tac da luu tai: $logPath" DarkCyan
Write-Host ""
pause
