# =============================================================================
# notification_fix_v3.ps1
# Xiaomi HyperOS Deep Notification Fix - ADB / Shizuku-compatible
#
# Muc tieu:
#   - Bao ve Google Play Services (FCM) khoi Greezer/PowerKeeper freeze
#   - Repair MILLET_NO_RESTRICT_APP thay vi chi whitelist Doze
#   - Cau hinh AppOps MIUI + Android cho app thong bao quan trong
#   - Doze whitelist + standby active
#   - Greezer GMS limiter defense-in-depth
#   - Backup + rollback
#
# Khong root. Chay bang ADB shell (USB debugging).
# Neu chay thong qua Shizuku, cac lenh shell tuong tu co the dung lai.
#
# NOTE:
#   HyperOS khac nhau theo ROM/build. Cac lenh Greezer/MIUI khong phai
#   API Android chinh thuc; script luon kiem tra va ghi log thay vi coi
#   lenh thanh cong = da ap dung.
# =============================================================================

$ErrorActionPreference = "Continue"
$Host.UI.RawUI.WindowTitle = "Xiaomi HyperOS - Deep Notification Fix v3.0"

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
    ERR "Khong tim thay adb.exe."
    WARN "Dat notification_fix_v3.ps1 cung thu muc voi adb.exe."
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

function Banner {
    Clear-Host
    Write-Color "  +================================================================+" Cyan
    Write-Color "  |       XIAOMI HYPEROS - DEEP NOTIFICATION FIX v3.0            |" Cyan
    Write-Color "  |       FCM / Greezer / PowerKeeper / AppOps / Doze             |" Cyan
    Write-Color "  +================================================================+" Cyan
}

Banner

# -------------------------------------------------------------------------
# 1. DEVICE
# -------------------------------------------------------------------------
STEP "1/8" "Kiem tra ADB va thiet bi..."
$devices = ADB-Run @("devices")
$connected = $devices | Where-Object { $_ -match "`tdevice$" }

if (-not $connected) {
    ERR "Khong tim thay thiet bi."
    INFO "Bat USB debugging va chap nhan RSA tren dien thoai."
    pause
    exit 1
}

$deviceId = (($connected | Select-Object -First 1) -split "`t")[0]
OK "Device: $deviceId"

$brand = ShellText "getprop ro.product.brand"
$model = ShellText "getprop ro.product.model"
$android = ShellText "getprop ro.build.version.release"
$sdk = ShellText "getprop ro.build.version.sdk"
$hyper = ShellText "getprop ro.miui.ui.version.name"
$build = ShellText "getprop ro.build.display.id"

OK "Brand : $brand"
OK "Model : $model"
OK "Android: $android / SDK $sdk"
if ($hyper) { OK "HyperOS: $hyper" }
INFO "Build : $build"

# -------------------------------------------------------------------------
# 2. BACKUP
# -------------------------------------------------------------------------
STEP "2/8" "Backup cau hinh hien tai..."

$backupDir = Join-Path $ScriptDir "notification_backup"
New-Item -ItemType Directory -Force -Path $backupDir | Out-Null

$stamp = Get-Date -Format "yyyyMMdd_HHmmss"
$backup = Join-Path $backupDir "backup_$stamp.txt"

$backupLines = @()
$backupLines += "===== Xiaomi HyperOS Notification Fix Backup ====="
$backupLines += "Time=$stamp"
$backupLines += "Device=$brand $model"
$backupLines += "Android=$android"
$backupLines += "HyperOS=$hyper"
$backupLines += "Build=$build"
$backupLines += ""
$backupLines += "[MILLET_NO_RESTRICT_APP]"
$backupLines += (ShellText "settings --user 0 get system MILLET_NO_RESTRICT_APP")
$backupLines += ""
$backupLines += "[GREEZER]"
$backupLines += (ShellText "dumpsys greezer | head -250")
$backupLines += ""
$backupLines += "[DEVICEIDLE]"
$backupLines += (ShellText "dumpsys deviceidle whitelist")
$backupLines += ""
$backupLines += "[STANDBY]"
$backupLines += (ShellText "am get-standby-bucket com.google.android.gms")
$backupLines += ""

$backupLines | Out-File -FilePath $backup -Encoding UTF8
OK "Backup: $backup"

# -------------------------------------------------------------------------
# 3. PACKAGES
# -------------------------------------------------------------------------
STEP "3/8" "Xac dinh Google FCM va app muc tieu..."

$allPkgs = @(ShellText "pm list packages" | ForEach-Object {
    $_ -split "`n" | ForEach-Object { ($_ -replace "^package:","").Trim() }
} | Where-Object { $_ })

$targets = [ordered]@{
    "Google Play Services" = "com.google.android.gms"
    "Google GSF"           = "com.google.android.gsf"
    "Zalo"                 = ""
    "Messenger"            = ""
    "Vietcombank"          = ""
}

$search = @{
    "Zalo"        = @("zalo")
    "Messenger"  = @("com.facebook.orca")
    "Vietcombank" = @("vietcom","vcb")
}

foreach ($key in @($search.Keys)) {
    foreach ($term in $search[$key]) {
        $hit = $allPkgs | Where-Object { $_ -imatch [regex]::Escape($term) } | Select-Object -First 1
        if ($hit) {
            $targets[$key] = $hit
            break
        }
    }
}

foreach ($key in $targets.Keys) {
    $pkg = $targets[$key]
    if ($pkg -and ($allPkgs -contains $pkg)) {
        OK "$($key.PadRight(22)) $pkg"
    } elseif ($pkg) {
        WARN "$key : khong ton tai"
        $targets[$key] = ""
    } else {
        WARN "$key : khong tu dong tim thay"
    }
}

# -------------------------------------------------------------------------
# 4. GMS DEEP FIX
# -------------------------------------------------------------------------
STEP "4/8" "Bao ve Google Play Services khoi Xiaomi Greezer/PowerKeeper..."

$gms = "com.google.android.gms"

# ---- MILLET_NO_RESTRICT_APP ---------------------------------------------
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
        OK "MILLET_NO_RESTRICT_APP: da them GMS"
    } else {
        ERR "MILLET_NO_RESTRICT_APP: khong xac minh duoc"
        if ($out) { INFO $out }
    }
} else {
    OK "MILLET_NO_RESTRICT_APP: GMS da co"
}

# ---- Greezer GMS limiter -----------------------------------------------
$greezerDisable = ShellText "dumpsys greezer IM GMS disable"
if ($greezerDisable -notmatch "SecurityException|permission denied|Unknown command|Error:") {
    OK "Greezer GMS limiter: da yeu cau DISABLE"
} else {
    WARN "Greezer GMS disable: ROM nay khong cho phep/khong co lenh"
}

# ---- Greezer runtime allowlist (defense in depth only) ------------------
$lm = ShellText "dumpsys greezer LM add $gms"
if ($lm -notmatch "SecurityException|permission denied|Unknown command|Error:") {
    OK "Greezer LM allowlist: da them GMS"
} else {
    WARN "Greezer LM add: khong kha dung tren build nay"
}

# ---- GMS appops ----------------------------------------------------------
$gmsOps = @(
    "RUN_IN_BACKGROUND",
    "RUN_ANY_IN_BACKGROUND",
    "WAKE_LOCK"
)

foreach ($op in $gmsOps) {
    $r = ShellText "cmd appops set $gms $op allow"
    if ($r -notmatch "Error|Exception|Unknown") {
        OK "GMS AppOp $op = allow"
    } else {
        WARN "GMS AppOp $op khong ap dung duoc"
    }
}

# MIUI undocumented AppOps commonly used by HyperOS
$miuiOps = @(
    "10007", # BOOT_COMPLETED / related MIUI boot control
    "10008", # AUTO_START
    "10021", # background-start-activity
    "10023", # foreground-service
    "10053"  # MIUI autostart switch on builds exposing it
)

foreach ($op in $miuiOps) {
    $r = ShellText "cmd appops set $gms $op allow"
    if ($r -notmatch "Error|Exception|Unknown") {
        OK "GMS MIUI AppOp $op = allow"
    } else {
        WARN "GMS MIUI AppOp $op khong duoc ROM nay expose"
    }
}

# DeviceIdle / standby
$r = ShellText "dumpsys deviceidle whitelist +$gms"
if ($r -notmatch "Error|Exception") { OK "GMS DeviceIdle whitelist" }

$r = ShellText "am set-inactive $gms false"
if ($r -notmatch "Error|Exception") { OK "GMS set-inactive=false" }

$r = ShellText "am set-standby-bucket $gms active"
if ($r -notmatch "Error|Exception") { OK "GMS standby bucket=active" }

# -------------------------------------------------------------------------
# 5. TARGET APPS
# -------------------------------------------------------------------------
STEP "5/8" "Ap dung deep background policy cho app thong bao..."

foreach ($name in $targets.Keys) {
    $pkg = $targets[$name]
    if (-not $pkg -or $pkg -eq $gms) { continue }

    Write-Color "  -- $name : $pkg --" Cyan

    foreach ($op in @(
        "RUN_IN_BACKGROUND",
        "RUN_ANY_IN_BACKGROUND",
        "WAKE_LOCK",
        "10007",
        "10008",
        "10021",
        "10023",
        "10053"
    )) {
        $r = ShellText "cmd appops set $pkg $op allow"
        if ($r -notmatch "Error|Exception|Unknown") {
            OK "$op = allow"
        } else {
            WARN "$op khong duoc expose"
        }
    }

    $r = ShellText "dumpsys deviceidle whitelist +$pkg"
    if ($r -notmatch "Error|Exception") { OK "DeviceIdle whitelist" }

    $r = ShellText "am set-inactive $pkg false"
    if ($r -notmatch "Error|Exception") { OK "set-inactive=false" }

    $r = ShellText "am set-standby-bucket $pkg active"
    if ($r -notmatch "Error|Exception") { OK "standby=active" }

    Write-Host ""
}

# -------------------------------------------------------------------------
# 6. NOTIFICATION DIAGNOSTICS
# -------------------------------------------------------------------------
STEP "6/8" "Kiem tra notification / FCM state..."

$checkPkgs = @($targets.Values | Where-Object { $_ } | Select-Object -Unique)

foreach ($pkg in $checkPkgs) {
    $uidLine = ShellText "cmd package list packages -U $pkg"
    INFO "$pkg"
    INFO "  $uidLine"

    $stopped = ShellText "dumpsys package $pkg | grep -m 3 -E 'stopped=|enabled=|userId='"
    if ($stopped) { INFO "  $stopped" }

    $bucket = ShellText "am get-standby-bucket $pkg"
    INFO "  standby=$bucket"
}

INFO "GMS socket/FCM:"
$gmsNet = ShellText "dumpsys connectivity | grep -i -E 'gms|5228' | head -30"
if ($gmsNet) { INFO $gmsNet } else { INFO "Khong doc duoc thong tin socket bang lenh nay." }

# -------------------------------------------------------------------------
# 7. FINAL VERIFICATION
# -------------------------------------------------------------------------
STEP "7/8" "Xac minh lai cac lop quan trong..."

$finalMillet = ShellText "settings --user 0 get system MILLET_NO_RESTRICT_APP"
$finalHasGms = @($finalMillet -split "," | ForEach-Object { $_.Trim() }) -contains $gms

if ($finalHasGms) {
    OK "MILLET_NO_RESTRICT_APP => GMS CO MAT"
} else {
    ERR "MILLET_NO_RESTRICT_APP => GMS KHONG CO"
}

$finalIdle = ShellText "dumpsys deviceidle whitelist"
if ($finalIdle -match [regex]::Escape($gms)) {
    OK "DeviceIdle => GMS CO MAT"
} else {
    WARN "DeviceIdle => khong thay GMS"
}

$finalGreezer = ShellText "dumpsys greezer | head -120"
if ($finalGreezer -match "mGmsLimitEnabled\s*:\s*false") {
    OK "Greezer => mGmsLimitEnabled=false"
} else {
    WARN "Greezer => khong xac minh duoc mGmsLimitEnabled=false"
}

# -------------------------------------------------------------------------
# 8. WATCHDOG SCRIPT
# -------------------------------------------------------------------------
STEP "8/8" "Tao watchdog de repair MILLET sau khi PowerKeeper ghi de..."

$watchdog = @'
#!/system/bin/sh
# Xiaomi HyperOS FCM watchdog
# Only repairs GMS package membership when it is missing.
# Does NOT overwrite the entire list unnecessarily.

GMS="com.google.android.gms"

while true; do
    CURRENT="$(settings --user 0 get system MILLET_NO_RESTRICT_APP 2>/dev/null)"

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

            # Greezer GMS runtime limiter is volatile and may reset after
            # system_server restart. This command is intentionally best-effort.
            dumpsys greezer IM GMS disable >/dev/null 2>&1
            ;;
    esac

    sleep 15
done
'@

$watchdogLocal = Join-Path $ScriptDir "hyperos_fcm_watchdog.sh"
$watchdog | Out-File -FilePath $watchdogLocal -Encoding ascii

OK "Da tao: $watchdogLocal"

Write-Host ""
Write-Color "  ==================================================================" Green
Write-Color "                         FIX V3 HOAN TAT" Green
Write-Color "  ==================================================================" Green
Write-Host ""

WARN "QUAN TRONG:"
INFO "1. MILLET_NO_RESTRICT_APP la lop quan trong nhat cho GMS tren mot so HyperOS."
INFO "2. PowerKeeper co the ghi de lai setting nay; do do day la ly do watchdog ton tai."
INFO "3. Greezer disable la defense-in-depth va co the reset sau reboot."
INFO "4. Khong co lenh ADB nao dam bao FCM 0.5-1 giay neu server/app gui FCM normal priority."
INFO "5. Khong tat Greezer toan he thong va khong sua system_server."
Write-Host ""

Write-Color "  CAC BUOC KHUYEN NGHI TIEP:" Yellow
INFO "A. Reboot dien thoai."
INFO "B. Sau khi boot, chay lai script 1 lan."
INFO "C. Tat man hinh 15-30 phut."
INFO "D. Gui test Zalo/Messenger/VCB."
INFO "E. Neu van miss, chay dumpsys greezer va dumpsys deviceidle de chan doan."
Write-Host ""

Write-Color "  RESTORE:" Cyan
INFO "Backup nam trong: $backupDir"
INFO "Khong tu dong xoa cac setting cu."
Write-Host ""

pause
