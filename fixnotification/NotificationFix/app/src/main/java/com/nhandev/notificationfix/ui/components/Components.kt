package com.nhandev.notificationfix.ui.components

import android.graphics.drawable.Drawable
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.nhandev.notificationfix.domain.model.AppInfo
import com.nhandev.notificationfix.domain.model.AppStatus
import com.nhandev.notificationfix.domain.model.PrivilegeMode
import com.nhandev.notificationfix.ui.theme.*
import com.nhandev.notificationfix.viewmodel.AppListUiState

// ─────────────────────────────────────────────────────────────────────────────
// AppCard — Redesigned with toggle switch + MILLET badge
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun AppCard(
    packageName: String,
    label: String,
    icon: Drawable?,
    status: AppStatus?,
    isSelected: Boolean,
    onToggleSelect: () -> Unit,
    onFixClick: () -> Unit,
    onRestoreClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val isFixed = status?.isFullyFixed == true

    val cardBg by animateColorAsState(
        targetValue = if (isSelected) Color(0xFF1A1A35) else CardDark,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "cardBg"
    )
    val borderColor by animateColorAsState(
        targetValue = when {
            isSelected -> OrangeGlow
            isFixed    -> GreenOK.copy(alpha = 0.4f)
            else       -> CardBorder
        },
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "borderColor"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .border(1.dp, borderColor, RoundedCornerShape(18.dp))
            .background(cardBg)
            .clickable { onToggleSelect() }
            .padding(14.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Checkbox (selection)
            Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isSelected) OrangeGlow else SurfaceVariant)
                    .border(1.dp, if (isSelected) OrangeGlow else CardBorder, RoundedCornerShape(6.dp)),
                contentAlignment = Alignment.Center
            ) {
                if (isSelected) {
                    Icon(
                        Icons.Rounded.Check, null,
                        tint = Color.White,
                        modifier = Modifier.size(14.dp)
                    )
                }
            }

            Spacer(Modifier.width(10.dp))

            // App icon
            AppIcon(icon = icon, packageName = packageName)

            Spacer(Modifier.width(12.dp))

            // Label + package + status badges
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text       = label,
                    style      = MaterialTheme.typography.titleMedium,
                    color      = TextPrimary,
                    maxLines   = 1,
                    overflow   = TextOverflow.Ellipsis,
                )
                Text(
                    text     = packageName,
                    fontSize = 10.sp,
                    color    = TextTertiary,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(Modifier.height(6.dp))
                StatusBadgeRow(status = status)
            }

            Spacer(Modifier.width(10.dp))

            // Toggle switch: ON = Fixed, OFF = Not fixed
            AppFixToggle(
                isFixed    = isFixed,
                onFixClick = onFixClick,
                onRestoreClick = onRestoreClick,
            )
        }
    }
}

@Composable
private fun AppFixToggle(
    isFixed: Boolean,
    onFixClick: () -> Unit,
    onRestoreClick: () -> Unit,
) {
    val thumbScale by animateFloatAsState(
        targetValue = if (isFixed) 1f else 0.85f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
        label = "thumbScale"
    )
    val trackColor by animateColorAsState(
        targetValue = if (isFixed) GreenOK else CardBorder,
        animationSpec = tween(200),
        label = "trackColor"
    )
    val thumbColor by animateColorAsState(
        targetValue = if (isFixed) Color.White else TextTertiary,
        animationSpec = tween(200),
        label = "thumbColor"
    )

    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Custom toggle
        Box(
            modifier = Modifier
                .width(52.dp)
                .height(28.dp)
                .clip(RoundedCornerShape(14.dp))
                .background(trackColor.copy(alpha = 0.3f))
                .border(1.5.dp, trackColor, RoundedCornerShape(14.dp))
                .clickable {
                    if (isFixed) onRestoreClick() else onFixClick()
                },
            contentAlignment = if (isFixed) Alignment.CenterEnd else Alignment.CenterStart
        ) {
            Box(
                modifier = Modifier
                    .padding(3.dp)
                    .size(22.dp)
                    .scale(thumbScale)
                    .clip(CircleShape)
                    .background(thumbColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (isFixed) Icons.Rounded.FlashOn else Icons.Rounded.NotificationsOff,
                    contentDescription = null,
                    tint = if (isFixed) GreenOK else CardBorder,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
        Text(
            text = if (isFixed) "Fixed" else "Off",
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            color = if (isFixed) GreenOK else TextTertiary
        )
    }
}

@Composable
fun AppIcon(icon: Drawable?, packageName: String) {
    Box(
        modifier = Modifier
            .size(46.dp)
            .clip(RoundedCornerShape(13.dp))
            .background(SurfaceVariant),
        contentAlignment = Alignment.Center,
    ) {
        if (icon != null) {
            AsyncImage(
                model              = icon,
                contentDescription = packageName,
                modifier           = Modifier.size(46.dp).clip(RoundedCornerShape(13.dp)),
            )
        } else {
            Icon(
                imageVector        = Icons.Rounded.Android,
                contentDescription = null,
                tint               = TextSecondary,
                modifier           = Modifier.size(28.dp),
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Status badges — now with MILLET protection badge
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun StatusBadgeRow(status: AppStatus?, modifier: Modifier = Modifier) {
    // First row: main status badges
    Row(
        modifier              = modifier,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        StatusBadge(label = "Tự chạy", ok = status?.autoStartAllowed)
        StatusBadge(label = "Nền",     ok = status?.runInBackgroundAllowed)
        StatusBadge(label = "Pin",     ok = status?.dozeWhitelisted)
    }
    // Second row: HyperOS-specific badges
    if (status != null) {
        Spacer(modifier = Modifier.height(3.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            MilletBadge(ok = status.milletProtected)
        }
    }
}

@Composable
fun StatusBadge(label: String, ok: Boolean?) {
    val bg   = when (ok) {
        true  -> GreenOKDim
        false -> RedFailDim
        null  -> SurfaceVariant
    }
    val textColor = when (ok) {
        true  -> GreenOK
        false -> RedFail
        null  -> TextTertiary
    }
    val icon = when (ok) {
        true  -> Icons.Rounded.Check
        false -> Icons.Rounded.Close
        null  -> null
    }
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .padding(horizontal = 5.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        if (icon != null) {
            Icon(icon, null, tint = textColor, modifier = Modifier.size(9.dp))
        }
        Text(
            text       = label,
            fontSize   = 9.sp,
            fontWeight = FontWeight.SemiBold,
            color      = textColor,
        )
    }
}

/**
 * Special badge for MILLET_NO_RESTRICT_APP protection status.
 * This is the most critical setting for Xiaomi HyperOS 2.x.
 */
@Composable
fun MilletBadge(ok: Boolean) {
    val bg = if (ok) Color(0xFF0A1E3D) else RedFailDim
    val textColor = if (ok) CyanAccent else RedFail
    val icon = if (ok) Icons.Rounded.Shield else Icons.Rounded.ShieldMoon

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(bg)
            .border(
                width = 0.5.dp,
                color = if (ok) CyanAccent.copy(alpha = 0.3f) else RedFail.copy(alpha = 0.3f),
                shape = RoundedCornerShape(6.dp)
            )
            .padding(horizontal = 5.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Icon(icon, null, tint = textColor, modifier = Modifier.size(9.dp))
        Text(
            text       = if (ok) "MILLET ✓" else "MILLET ✗",
            fontSize   = 9.sp,
            fontWeight = FontWeight.Bold,
            color      = textColor,
        )
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// GradientButton with shimmer effect
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun GradientButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    isLoading: Boolean = false,
    gradient: Brush = Brush.horizontalGradient(listOf(GradientOrange1, GradientOrange2)),
) {
    val shimmerOffset by rememberInfiniteTransition(label = "shimmer").animateFloat(
        initialValue = -1f,
        targetValue  = 2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "shimmerOffset"
    )

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (enabled)
                    gradient
                else
                    Brush.horizontalGradient(listOf(CardBorder, SurfaceVariant))
            )
            .clickable(enabled = enabled && !isLoading) { onClick() }
            .padding(vertical = 17.dp),
        contentAlignment = Alignment.Center,
    ) {
        if (isLoading) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                CircularProgressIndicator(
                    color    = Color.White,
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                )
                Text("Đang xử lý...", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
        } else {
            Text(
                text       = text,
                color      = Color.White,
                fontSize   = 15.sp,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// GMS Protection Button — dedicated 1-tap GMS shield
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun GmsProtectButton(
    onClick: () -> Unit,
    isProtected: Boolean,
    isLoading: Boolean = false,
    modifier: Modifier = Modifier,
) {
    val bgGradient = if (isProtected)
        Brush.horizontalGradient(listOf(Color(0xFF003D20), Color(0xFF0A2E1A)))
    else
        Brush.horizontalGradient(listOf(Color(0xFF1A0A2E), Color(0xFF2E1A3D)))

    val borderColor by animateColorAsState(
        targetValue = if (isProtected) GreenOK.copy(alpha = 0.5f) else CyanAccent.copy(alpha = 0.5f),
        label = "gmsBorder"
    )
    val accent = if (isProtected) GreenOK else CyanAccent

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .background(bgGradient)
            .clickable(enabled = !isLoading) { onClick() }
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            // Shield icon
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(accent.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(22.dp),
                        color = accent,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Icon(
                        imageVector = if (isProtected) Icons.Rounded.VerifiedUser else Icons.Rounded.Shield,
                        contentDescription = null,
                        tint = accent,
                        modifier = Modifier.size(22.dp),
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (isProtected)
                        "🛡️ GMS Core đã được bảo vệ"
                    else
                        "🛡️ Bảo vệ GMS Core (FCM)",
                    color = accent,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                )
                Text(
                    text = if (isProtected)
                        "MILLET + Greezer + Doze — Đang hoạt động"
                    else
                        "Chống Greezer freeze — Fix thông báo gốc",
                    color = TextSecondary,
                    fontSize = 11.sp,
                )
            }

            if (!isProtected && !isLoading) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(10.dp))
                        .background(accent)
                        .padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Text(
                        "Bật",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                    )
                }
            }

            if (isProtected) {
                Box(
                    modifier = Modifier
                        .size(28.dp)
                        .clip(CircleShape)
                        .background(GreenOK.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Rounded.Check, null,
                        tint = GreenOK,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Greezer Refresh Button
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun GreezerRefreshButton(
    onClick: () -> Unit,
    isLoading: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, PurpleAccent.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
            .background(Color(0xFF1A0A2E).copy(alpha = 0.6f))
            .clickable(enabled = !isLoading) { onClick() }
            .padding(horizontal = 16.dp, vertical = 12.dp),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(PurpleAccent.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = PurpleAccent,
                        strokeWidth = 2.dp,
                    )
                } else {
                    Icon(
                        Icons.Rounded.Refresh,
                        null,
                        tint = PurpleAccent,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            Spacer(Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    "🔄 Refresh Greezer Limiter",
                    color = PurpleAccent,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 13.sp,
                )
                Text(
                    "Tái vô hiệu Greezer GMS limiter (resets on reboot)",
                    color = TextTertiary,
                    fontSize = 10.sp,
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// PrivilegeCard
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun PrivilegeCard(
    mode: com.nhandev.notificationfix.domain.model.PrivilegeMode,
    onRequestShizuku: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val (icon, title, subtitle, bg, accent) = when (mode) {
        com.nhandev.notificationfix.domain.model.PrivilegeMode.SHIZUKU -> Quintuple(
            Icons.Rounded.Verified,
            "Shizuku Connected",
            "Đầy đủ quyền — sẵn sàng fix",
            Brush.linearGradient(listOf(Color(0xFF0A1E0A), Color(0xFF061408))),
            GreenOK
        )
        com.nhandev.notificationfix.domain.model.PrivilegeMode.ROOT -> Quintuple(
            Icons.Rounded.AdminPanelSettings,
            "Root Available",
            "Magisk/KSU — quyền tối cao",
            Brush.linearGradient(listOf(Color(0xFF060E1E), Color(0xFF0A1228))),
            BlueAccent
        )
        com.nhandev.notificationfix.domain.model.PrivilegeMode.NONE -> Quintuple(
            Icons.Rounded.Warning,
            "Chưa cấp quyền",
            "Cần Shizuku hoặc Root để thực hiện fix",
            Brush.linearGradient(listOf(Color(0xFF1A1000), Color(0xFF120C00))),
            YellowWarn
        )
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .border(1.dp, accent.copy(alpha = 0.5f), RoundedCornerShape(18.dp))
            .background(bg)
            .padding(16.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Pulsing icon for NONE state
            if (mode == com.nhandev.notificationfix.domain.model.PrivilegeMode.NONE) {
                val pulse by rememberInfiniteTransition(label = "pulse").animateFloat(
                    initialValue = 0.8f,
                    targetValue  = 1.1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(800, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "pulseScale"
                )
                Box(modifier = Modifier.scale(pulse)) {
                    Icon(imageVector = icon, contentDescription = null, tint = accent, modifier = Modifier.size(38.dp))
                }
            } else {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(imageVector = icon, contentDescription = null, tint = accent, modifier = Modifier.size(26.dp))
                }
            }

            Spacer(Modifier.width(14.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, fontWeight = FontWeight.Bold, fontSize = 15.sp, color = accent)
                Text(text = subtitle, fontSize = 12.sp, color = TextSecondary)
            }

            if (mode == com.nhandev.notificationfix.domain.model.PrivilegeMode.NONE) {
                Button(
                    onClick = onRequestShizuku,
                    colors  = ButtonDefaults.buttonColors(containerColor = YellowWarn, contentColor = Color(0xFF1A1000)),
                    shape   = RoundedCornerShape(10.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                ) {
                    Text("Bật Shizuku", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Fixed Apps Drawer Content
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun FixedAppsDrawerContent(
    state: AppListUiState,
    onToggleApp: (packageName: String, label: String) -> Unit,
    onFixAll: () -> Unit,
    onClose: () -> Unit,
) {
    val appsInDrawer = state.allApps  // show all scanned apps

    Column(
        modifier = Modifier
            .fillMaxHeight()
            .width(300.dp)
            .background(
                Brush.verticalGradient(listOf(DrawerBg, Color(0xFF0A0A1E)))
            )
    ) {
        // ── Drawer Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.horizontalGradient(listOf(GradientOrange1.copy(alpha = 0.2f), Color.Transparent))
                )
                .padding(top = 52.dp, start = 20.dp, end = 16.dp, bottom = 16.dp)
        ) {
            Column {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(OrangeGlow.copy(alpha = 0.2f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Rounded.Tune,
                                null,
                                tint = OrangeGlow,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                "App Manager",
                                color = TextPrimary,
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            )
                            Text(
                                "${state.allApps.size} ứng dụng đã scan",
                                color = TextTertiary,
                                fontSize = 11.sp
                            )
                        }
                    }
                    IconButton(onClick = onClose, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Rounded.Close, null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                    }
                }

                Spacer(Modifier.height(12.dp))

                // Stats row
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    val fixedCount   = state.statusMap.values.count { it.isFullyFixed }
                    val unfixedCount = state.allApps.size - fixedCount
                    val milletCount  = state.statusMap.values.count { it.milletProtected }

                    DrawerStatChip(
                        label = "$fixedCount Fixed",
                        color = GreenOK,
                        modifier = Modifier.weight(1f)
                    )
                    DrawerStatChip(
                        label = "$milletCount MILLET",
                        color = CyanAccent,
                        modifier = Modifier.weight(1f)
                    )
                    DrawerStatChip(
                        label = "$unfixedCount Off",
                        color = YellowWarn,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        HorizontalDivider(color = DrawerBorder, thickness = 1.dp)

        // ── App List
        if (appsInDrawer.isEmpty()) {
            Box(
                modifier = Modifier.weight(1f).fillMaxWidth(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Rounded.Apps, null, tint = TextTertiary, modifier = Modifier.size(44.dp))
                    Spacer(Modifier.height(8.dp))
                    Text("Chưa có app nào", color = TextTertiary, fontSize = 13.sp)
                    Text("Vào tab Ứng dụng để scan", color = TextTertiary, fontSize = 11.sp)
                }
            }
        } else {
            LazyColumn(
                modifier = Modifier.weight(1f),
                contentPadding = PaddingValues(vertical = 8.dp),
            ) {
                items(appsInDrawer, key = { it.packageName }) { app ->
                    val isFixed    = state.statusMap[app.packageName]?.isFullyFixed == true
                    val isMillet   = state.statusMap[app.packageName]?.milletProtected == true
                    val isToggling = state.togglingPkgs.contains(app.packageName)
                    DrawerAppRow(
                        app        = app,
                        isFixed    = isFixed,
                        isMillet   = isMillet,
                        isToggling = isToggling,
                        onToggle   = { onToggleApp(app.packageName, app.label) }
                    )
                }
            }
        }

        // ── Bottom actions
        HorizontalDivider(color = DrawerBorder, thickness = 1.dp)
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(
                onClick = onFixAll,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = OrangeGlow),
                shape = RoundedCornerShape(12.dp),
            ) {
                Icon(Icons.Rounded.FlashOn, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text("⚡ Fix tất cả app", fontWeight = FontWeight.Bold, fontSize = 14.sp)
            }
        }
    }
}

@Composable
private fun DrawerStatChip(label: String, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(8.dp))
            .background(color.copy(alpha = 0.15f))
            .border(1.dp, color.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
            .padding(horizontal = 6.dp, vertical = 4.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(label, color = color, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun DrawerAppRow(
    app: AppInfo,
    isFixed: Boolean,
    isMillet: Boolean,
    isToggling: Boolean,
    onToggle: () -> Unit,
) {
    val bgColor by animateColorAsState(
        targetValue = when {
            isFixed -> GreenOKDim.copy(alpha = 0.3f)
            isMillet -> Color(0xFF0A1E3D).copy(alpha = 0.3f)
            else -> Color.Transparent
        },
        animationSpec = tween(300),
        label = "drawerRowBg"
    )

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(bgColor)
            .clickable(enabled = !isToggling) { onToggle() }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        // App icon (small)
        Box(
            modifier = Modifier
                .size(38.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(SurfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            if (app.icon != null) {
                AsyncImage(
                    model              = app.icon,
                    contentDescription = app.packageName,
                    modifier           = Modifier.size(38.dp).clip(RoundedCornerShape(10.dp)),
                )
            } else {
                Icon(Icons.Rounded.Android, null, tint = TextSecondary, modifier = Modifier.size(22.dp))
            }
        }

        Spacer(Modifier.width(10.dp))

        Column(modifier = Modifier.weight(1f)) {
            Text(
                text     = app.label,
                color    = if (isFixed) TextPrimary else TextSecondary,
                fontSize = 13.sp,
                fontWeight = if (isFixed) FontWeight.SemiBold else FontWeight.Normal,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(
                    text     = if (isFixed) "✓ Fixed" else "Chưa fix",
                    color    = if (isFixed) GreenOK else TextTertiary,
                    fontSize = 10.sp,
                )
                if (isMillet && !isFixed) {
                    Text(
                        text = "· MILLET ✓",
                        color = CyanAccent,
                        fontSize = 10.sp,
                    )
                }
            }
        }

        Spacer(Modifier.width(8.dp))

        if (isToggling) {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                color = OrangeGlow,
                strokeWidth = 2.dp
            )
        } else {
            // Mini toggle switch
            Box(
                modifier = Modifier
                    .width(44.dp)
                    .height(24.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(if (isFixed) GreenOK.copy(alpha = 0.25f) else SurfaceVariant)
                    .border(
                        1.dp,
                        if (isFixed) GreenOK.copy(alpha = 0.6f) else CardBorder,
                        RoundedCornerShape(12.dp)
                    ),
                contentAlignment = if (isFixed) Alignment.CenterEnd else Alignment.CenterStart
            ) {
                Box(
                    modifier = Modifier
                        .padding(2.dp)
                        .size(20.dp)
                        .clip(CircleShape)
                        .background(if (isFixed) GreenOK else TextTertiary)
                )
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Helpers
// ─────────────────────────────────────────────────────────────────────────────

private data class Quintuple<A, B, C, D, E>(val a: A, val b: B, val c: C, val d: D, val e: E)
private operator fun <A, B, C, D, E> Quintuple<A, B, C, D, E>.component1() = a
private operator fun <A, B, C, D, E> Quintuple<A, B, C, D, E>.component2() = b
private operator fun <A, B, C, D, E> Quintuple<A, B, C, D, E>.component3() = c
private operator fun <A, B, C, D, E> Quintuple<A, B, C, D, E>.component4() = d
private operator fun <A, B, C, D, E> Quintuple<A, B, C, D, E>.component5() = e
