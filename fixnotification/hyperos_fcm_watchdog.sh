#!/system/bin/sh
# =============================================================================
# Xiaomi HyperOS FCM & Zalo Deep Watchdog Service v4.5
# Compatible: Xiaomi 15 Pro (HyperOS 2) & Xiaomi 17 Pro Max (HyperOS 3)
# Runs in Linux shell domain (UID 2000/0) — Immune to ActivityManager kills!
# =============================================================================

GMS="com.google.android.gms"
GSF="com.google.android.gsf"
ZALO="com.zing.zalo"
SELF="com.nhandev.notificationfix"

while true; do
    # ── 1. MILLET_NO_RESTRICT_APP Protection ──────────────────────────────────
    CURRENT="$(settings --user 0 get system MILLET_NO_RESTRICT_APP 2>/dev/null)"
    UPDATED="$CURRENT"

    for PKG in "$GMS" "$GSF" "$ZALO" "$SELF"; do
        case ",$UPDATED," in
            *,"$PKG",*)
                ;;
            *)
                if [ -z "$UPDATED" ] || [ "$UPDATED" = "null" ]; then
                    UPDATED="$PKG"
                else
                    UPDATED="$UPDATED,$PKG"
                fi
                ;;
        esac
    done

    if [ "$UPDATED" != "$CURRENT" ]; then
        settings --user 0 put system MILLET_NO_RESTRICT_APP "$UPDATED" >/dev/null 2>&1
    fi

    # ── 2. Greezer Limiter Volatile Overrides ──────────────────────────────────
    dumpsys greezer IM GMS disable >/dev/null 2>&1
    dumpsys greezer IM APP disable "$ZALO" >/dev/null 2>&1
    dumpsys greezer LM add "$GMS" >/dev/null 2>&1
    dumpsys greezer LM add "$ZALO" >/dev/null 2>&1

    # ── 3. Zalo VoIP Call & Process Keep-Alive ────────────────────────────────
    # Prevents missed incoming calls by resurrecting socket push services
    ZALO_PID=$(pidof "$ZALO" 2>/dev/null)
    if [ -z "$ZALO_PID" ]; then
        am startservice --user 0 -n "$ZALO/.service.ZaloService" >/dev/null 2>&1
        am startservice --user 0 -n "$ZALO/.push.notification.PushService" >/dev/null 2>&1
        cmd activity make-uid-active "$ZALO" >/dev/null 2>&1
    fi

    # ── 4. Ensure NotificationFix Watcher Service is running ──────────────────
    # If the foreground service was killed, revive it immediately
    SERVICE_CHECK="$(dumpsys activity services com.nhandev.notificationfix 2>/dev/null | grep 'MilletWatcherService')"
    if [ -z "$SERVICE_CHECK" ]; then
        am start-foreground-service -n "$SELF/.domain.MilletWatcherService" >/dev/null 2>&1
    fi

    sleep 30
done
