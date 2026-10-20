package com.nhandev.notificationfix.domain.model

import android.graphics.drawable.Drawable

/**
 * Represents an installed app with its basic info.
 */
data class AppInfo(
    val packageName: String,
    val label: String,
    val icon: Drawable?,
    val isSystemApp: Boolean = false,
    val isSelected: Boolean = false,
)

/**
 * Preset groups of apps to fix together.
 */
data class AppPreset(
    val id: String,
    val label: String,
    val emoji: String,
    val packages: List<String>,
)

val PRESET_GOOGLE_FCM = AppPreset(
    id       = "google_fcm",
    label    = "Google FCM",
    emoji    = "🔔",
    packages = listOf(
        "com.google.android.gms",
        "com.google.android.gsf",
    )
)

val PRESET_MESSAGING = AppPreset(
    id       = "messaging",
    label    = "Nhắn tin",
    emoji    = "💬",
    packages = listOf(
        "com.zing.zalo",
        "com.facebook.orca",
        "org.telegram.messenger",
        "com.whatsapp",
        "com.microsoft.teams",
        "com.discord",
        "com.viber.voip",
        "com.skype.raider",
    )
)

val PRESET_BANKING_VN = AppPreset(
    id       = "banking_vn",
    label    = "Ngân hàng VN",
    emoji    = "🏦",
    packages = listOf(
        "com.VCB",
        "com.vnpay.bidv",
        "com.mb.MBBank",
        "com.techcombank.mb.app",
        "vn.tpb.mobile",
        "com.acb.bank",
        "com.vib.myvib2",
        "vn.momo.client",
        "vn.zalopay.app",
        "vn.vneid.app",
        "com.shopee.vn",
    )
)

val PRESET_MICROG_REVANCED = AppPreset(
    id       = "microg_revanced",
    label    = "MicroG / ReVanced",
    emoji    = "▶️",
    packages = listOf(
        "com.mgoogle.android.gms",
        "app.revanced.android.youtube",
        "app.revanced.android.apps.youtube.music",
        "com.google.android.youtube",
        "com.google.android.apps.youtube.music",
        "org.microg.gms.droidguard",
        "com.vanced.android.youtube",
        "com.vanced.android.apps.youtube.music",
        "app.rvx.android.youtube",
        "app.rvx.android.apps.youtube.music",
    )
)

val PRESET_SOCIAL = AppPreset(
    id       = "social",
    label    = "Mạng xã hội",
    emoji    = "📱",
    packages = listOf(
        "com.instagram.android",
        "com.twitter.android",
        "com.zhiliaoapp.musically",
        "com.facebook.katana",
        "com.snapchat.android",
        "com.linkedin.android",
        "com.reddit.frontpage",
    )
)

val ALL_PRESETS = listOf(
    PRESET_GOOGLE_FCM,
    PRESET_MESSAGING,
    PRESET_BANKING_VN,
    PRESET_MICROG_REVANCED,
    PRESET_SOCIAL,
)
