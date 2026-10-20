package com.nhandev.notificationfix.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.nhandev.notificationfix.domain.DeviceModelType
import com.nhandev.notificationfix.domain.DeviceProfile
import com.nhandev.notificationfix.domain.model.ALL_PRESETS
import com.nhandev.notificationfix.domain.model.AppPreset
import com.nhandev.notificationfix.domain.model.PrivilegeMode
import com.nhandev.notificationfix.ui.components.*
import com.nhandev.notificationfix.ui.theme.*
import com.nhandev.notificationfix.viewmodel.AppListUiState
import com.nhandev.notificationfix.viewmodel.AppListViewModel

@Composable
fun HomeScreen(
    vm: AppListViewModel,
    onNavigateToApps: () -> Unit,
    onOpenDrawer: () -> Unit = {},
) {
    val state by vm.uiState.collectAsState()

    LazyColumn(
        modifier            = Modifier
            .fillMaxSize()
            .background(BackgroundDark),
        contentPadding      = PaddingValues(bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(0.dp),
    ) {
        // ── Hero header
        item {
            HomeHeroHeader(
                state        = state,
                onOpenDrawer = onOpenDrawer,
            )
        }

        // ── Privilege status card
        item {
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                PrivilegeCard(
                    mode             = state.privilegeMode,
                    onRequestShizuku = { vm.requestShizukuPermission() },
                )
            }
        }

        item { Spacer(Modifier.height(12.dp)) }

        // ── Device Model Tailored Optimization (Xiaomi 15 Pro / 17 Pro Max)
        item {
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                DeviceModelCard(
                    profile      = state.deviceProfile,
                    isOptimizing = state.isAutoOptimizing,
                    onOptimize   = { vm.autoOptimizeForModel() }
                )
            }
        }

        item { Spacer(Modifier.height(12.dp)) }

        // ── Zalo VoIP Call & Socket Protection Card
        item {
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                ZaloVoIpCard(
                    isProtected  = state.isZaloProtected,
                    isProtecting = state.isProtectingZalo,
                    onFix        = { vm.fixZaloVoIP() }
                )
            }
        }

        item { Spacer(Modifier.height(12.dp)) }

        // ── Permanent Run & Reboot Immunity Card
        item {
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                PermanentKeepAliveCard(
                    isWatcherRunning = state.isWatcherRunning,
                    onEnable         = { vm.enablePermanentRunAndReboot() }
                )
            }
        }

        item { Spacer(Modifier.height(12.dp)) }

        // ── GMS Core Protection (1-tap)
        item {
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                GmsProtectButton(
                    onClick     = { vm.protectGmsCore() },
                    isProtected = state.isGmsProtected,
                    isLoading   = state.isProtectingGms,
                )
            }
        }

        item { Spacer(Modifier.height(12.dp)) }

        // ── Greezer Refresh button
        item {
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                GreezerRefreshButton(
                    onClick   = { vm.refreshGreezer() },
                    isLoading = state.isRefreshingGreezer,
                )
            }
        }

        item { Spacer(Modifier.height(16.dp)) }

        // ── Summary stats
        item {
            Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                SummaryStats(state = state)
            }
        }

        item { Spacer(Modifier.height(20.dp)) }

        // ── Quick Fix Presets
        item {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .height(18.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(OrangeGlow)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text       = "Quick Fix Presets",
                        color      = TextPrimary,
                        fontSize   = 15.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }

        item { Spacer(Modifier.height(10.dp)) }

        item {
            LazyRow(
                contentPadding        = PaddingValues(horizontal = 16.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                items(ALL_PRESETS) { preset ->
                    PresetCard(
                        preset   = preset,
                        isActive = state.activePreset?.id == preset.id,
                        onClick  = {
                            vm.selectPreset(preset)
                            onNavigateToApps()
                        }
                    )
                }
            }
        }

        item { Spacer(Modifier.height(20.dp)) }

        // ── Fix Actions
        item {
            val context = androidx.compose.ui.platform.LocalContext.current
            Column(modifier = Modifier.padding(horizontal = 16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .width(3.dp)
                            .height(18.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(OrangeGlow)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text       = "Hành động",
                        color      = TextPrimary,
                        fontSize   = 15.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }

                Spacer(Modifier.height(4.dp))

                val hasSelected = state.selectedPkgs.isNotEmpty()
                GradientButton(
                    text      = if (hasSelected)
                        "⚡  Fix ${state.selectedPkgs.size} app đã chọn"
                    else
                        "⚡  Fix Tất Cả (MILLET + Greezer + Doze)",
                    onClick   = {
                        if (state.privilegeMode == PrivilegeMode.NONE) {
                            android.widget.Toast.makeText(context, "⚠️ Cần cấp quyền Shizuku!", android.widget.Toast.LENGTH_SHORT).show()
                            vm.requestShizukuPermission()
                        } else {
                            if (!hasSelected) vm.selectPreset(ALL_PRESETS[0])
                            vm.fixSelected()
                        }
                    },
                    enabled   = !state.isFixing,
                    isLoading = state.isFixing,
                )

                // Restore button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .border(1.dp, YellowWarn.copy(alpha = 0.4f), RoundedCornerShape(14.dp))
                        .background(YellowWarnDim.copy(alpha = 0.3f))
                ) {
                    OutlinedButton(
                        onClick  = {
                            if (state.privilegeMode == PrivilegeMode.NONE) {
                                android.widget.Toast.makeText(context, "⚠️ Cần cấp quyền Shizuku!", android.widget.Toast.LENGTH_SHORT).show()
                                vm.requestShizukuPermission()
                            } else {
                                vm.restoreSelected()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape    = RoundedCornerShape(14.dp),
                        colors   = ButtonDefaults.outlinedButtonColors(contentColor = YellowWarn),
                        border   = null,
                        enabled  = !state.isFixing,
                    ) {
                        Icon(Icons.Rounded.RestartAlt, null, modifier = Modifier.size(18.dp), tint = YellowWarn)
                        Spacer(Modifier.width(8.dp))
                        Text("↩  Hủy Fix / Đưa về gốc", fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }
                }
            }
        }

        // ── Manual guide if no privilege
        if (state.privilegeMode == PrivilegeMode.NONE) {
            item {
                Spacer(Modifier.height(16.dp))
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    ManualGuideCard()
                }
            }
        }

        item { Spacer(Modifier.height(16.dp)) }
    }
}

@Composable
private fun HomeHeroHeader(
    state: AppListUiState,
    onOpenDrawer: () -> Unit,
) {
    val fixedCount = state.statusMap.values.count { it.isFullyFixed }
    val milletCount = state.statusMap.values.count { it.milletProtected }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    listOf(
                        Color(0xFF1A0A00),
                        Color(0xFF120820),
                        BackgroundDark
                    )
                )
            )
            .padding(top = 16.dp, bottom = 20.dp)
    ) {
        Column(modifier = Modifier.padding(horizontal = 16.dp)) {
            // Top row: menu button + badge
            Row(
                modifier              = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment     = Alignment.CenterVertically,
            ) {
                // Drawer toggle button
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(SurfaceVariant)
                        .border(1.dp, CardBorder, RoundedCornerShape(12.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(onClick = onOpenDrawer, modifier = Modifier.size(40.dp)) {
                        Icon(
                            Icons.Rounded.Menu,
                            contentDescription = "Mở App Manager",
                            tint   = TextSecondary,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }

                // Status badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            if (state.privilegeMode != PrivilegeMode.NONE)
                                GreenOKDim
                            else
                                YellowWarnDim
                        )
                        .border(
                            1.dp,
                            if (state.privilegeMode != PrivilegeMode.NONE) GreenOK.copy(0.3f) else YellowWarn.copy(0.3f),
                            RoundedCornerShape(20.dp)
                        )
                        .padding(horizontal = 12.dp, vertical = 5.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(
                                    if (state.privilegeMode != PrivilegeMode.NONE) GreenOK else YellowWarn
                                )
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(
                            text = when (state.privilegeMode) {
                                PrivilegeMode.SHIZUKU -> "Shizuku"
                                PrivilegeMode.ROOT    -> "Root"
                                PrivilegeMode.NONE    -> "Chưa có quyền"
                            },
                            color    = if (state.privilegeMode != PrivilegeMode.NONE) GreenOK else YellowWarn,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                        )
                    }
                }
            }

            Spacer(Modifier.height(18.dp))

            // App name + subtitle
            Row(verticalAlignment = Alignment.Top) {
                Box(
                    modifier = Modifier
                        .size(52.dp)
                        .clip(RoundedCornerShape(15.dp))
                        .background(
                            Brush.linearGradient(listOf(GradientOrange1, GradientOrange2))
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector        = Icons.Rounded.NotificationsActive,
                        contentDescription = null,
                        tint               = Color.White,
                        modifier           = Modifier.size(30.dp),
                    )
                }
                Spacer(Modifier.width(14.dp))
                Column {
                    Text(
                        text       = "NotificationFix",
                        color      = TextPrimary,
                        fontSize   = 24.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text   = "Xiaomi HyperOS · Push Optimizer v4",
                        color  = TextSecondary,
                        fontSize = 12.sp,
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        if (!state.isLoading && fixedCount > 0) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(OrangeDim)
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    "$fixedCount app đang Fix",
                                    color    = OrangeLight,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                        if (!state.isLoading && milletCount > 0) {
                            Box(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(Color(0xFF0A1E3D))
                                    .border(0.5.dp, CyanAccent.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 3.dp)
                            ) {
                                Text(
                                    "$milletCount MILLET",
                                    color    = CyanAccent,
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryStats(state: AppListUiState) {
    val total   = state.allApps.size
    val fixed   = state.statusMap.values.count { it.isFullyFixed }
    val unfixed = total - fixed
    val millet  = state.statusMap.values.count { it.milletProtected }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            StatCard(
                label   = "Tổng app",
                value   = "$total",
                icon    = Icons.Rounded.Apps,
                accent  = BlueAccent,
                bgColor = BlueDim,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                label   = "Đã Fix",
                value   = "$fixed",
                icon    = Icons.Rounded.CheckCircle,
                accent  = GreenOK,
                bgColor = GreenOKDim,
                modifier = Modifier.weight(1f)
            )
            StatCard(
                label   = "Chưa Fix",
                value   = "$unfixed",
                icon    = Icons.Rounded.WarningAmber,
                accent  = YellowWarn,
                bgColor = YellowWarnDim,
                modifier = Modifier.weight(1f)
            )
        }
        Row(
            modifier              = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            StatCard(
                label    = "MILLET",
                value    = "$millet",
                icon     = Icons.Rounded.Shield,
                accent   = CyanAccent,
                bgColor  = Color(0xFF0A1E3D),
                modifier = Modifier.weight(1f)
            )
            StatCard(
                label    = "GMS Core",
                value    = if (state.isGmsProtected) "✓" else "✗",
                icon     = if (state.isGmsProtected) Icons.Rounded.VerifiedUser else Icons.Rounded.ShieldMoon,
                accent   = if (state.isGmsProtected) GreenOK else RedFail,
                bgColor  = if (state.isGmsProtected) GreenOKDim else RedFailDim,
                modifier = Modifier.weight(1f)
            )
            // Placeholder for alignment
            Box(modifier = Modifier.weight(1f))
        }
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    accent: Color,
    bgColor: Color,
    modifier: Modifier = Modifier,
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(16.dp))
            .background(CardDark)
            .border(1.dp, accent.copy(alpha = 0.2f), RoundedCornerShape(16.dp))
            .padding(14.dp),
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Box(
                modifier = Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(bgColor),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, null, tint = accent, modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.height(8.dp))
            Text(text = value, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = TextPrimary)
            Text(text = label, fontSize = 10.sp, color = TextSecondary, fontWeight = FontWeight.Medium)
        }
    }
}

@Composable
private fun PresetCard(
    preset: AppPreset,
    isActive: Boolean,
    onClick: () -> Unit,
) {
    val borderColor by animateColorAsState(
        targetValue = if (isActive) OrangeGlow else CardBorder,
        label = "presetBorder"
    )
    val bgColor by animateColorAsState(
        targetValue = if (isActive) OrangeDim else CardDark,
        label = "presetBg"
    )

    Box(
        modifier = Modifier
            .width(140.dp)
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, borderColor, RoundedCornerShape(16.dp))
            .background(bgColor),
    ) {
        Surface(
            onClick  = onClick,
            color    = Color.Transparent,
            modifier = Modifier.fillMaxWidth(),
        ) {
            Column(
                modifier            = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text(text = preset.emoji, fontSize = 24.sp)
                Text(
                    text       = preset.label,
                    color      = if (isActive) OrangeLight else TextPrimary,
                    fontSize   = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text     = "${preset.packages.size} apps",
                    color    = TextTertiary,
                    fontSize = 10.sp,
                )
                if (isActive) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(4.dp))
                            .background(OrangeGlow)
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text("Active", color = Color.White, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

@Composable
private fun ManualGuideCard() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.linearGradient(listOf(Color(0xFF1A1400), Color(0xFF0F0E00)))
            )
            .border(1.dp, YellowWarn.copy(alpha = 0.25f), RoundedCornerShape(18.dp))
            .padding(16.dp),
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(32.dp)
                        .clip(CircleShape)
                        .background(YellowWarnDim),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Rounded.Info, null, tint = YellowWarn, modifier = Modifier.size(18.dp))
                }
                Spacer(Modifier.width(10.dp))
                Text(
                    "Hướng dẫn không có Shizuku/Root",
                    color      = YellowWarn,
                    fontWeight = FontWeight.Bold,
                    fontSize   = 13.sp
                )
            }
            Spacer(Modifier.height(10.dp))
            val steps = listOf(
                "Settings → Apps → Manage Apps → [App]",
                "Battery → No restrictions",
                "Background autostart → BẬT ON",
                "Notifications → BẬT ON",
                "Kết nối ADB từ PC và chạy notification_fix.ps1",
            )
            steps.forEachIndexed { i, step ->
                Row(
                    modifier = Modifier.padding(vertical = 3.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .clip(CircleShape)
                            .background(SurfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("${i+1}", color = YellowWarn, fontSize = 9.sp, fontWeight = FontWeight.Bold)
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(step, color = TextSecondary, fontSize = 12.sp)
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Hardware Tailored Model Card (Xiaomi 15 Pro & 17 Pro Max)
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun DeviceModelCard(
    profile: DeviceProfile?,
    isOptimizing: Boolean,
    onOptimize: () -> Unit,
) {
    val is17Prm = profile?.modelType == DeviceModelType.XIAOMI_17_PRO_MAX
    val is15Pro = profile?.modelType == DeviceModelType.XIAOMI_15_PRO

    val accentColor = when {
        is17Prm -> CyanAccent
        is15Pro -> OrangeGlow
        else    -> BlueAccent
    }

    val gradientBg = when {
        is17Prm -> Brush.linearGradient(listOf(Color(0xFF04182B), Color(0xFF0A0F1D)))
        is15Pro -> Brush.linearGradient(listOf(Color(0xFF261005), Color(0xFF14081E)))
        else    -> Brush.linearGradient(listOf(Color(0xFF0E1428), Color(0xFF0A0D18)))
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(gradientBg)
            .border(1.2.dp, accentColor.copy(alpha = 0.5f), RoundedCornerShape(20.dp))
            .padding(16.dp),
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(accentColor.copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = if (is17Prm) Icons.Rounded.RocketLaunch else Icons.Rounded.Memory,
                            contentDescription = null,
                            tint = accentColor,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            text = profile?.displayName ?: "Thiết bị Xiaomi",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                        )
                        Text(
                            text = profile?.socModel ?: "Snapdragon Flagship",
                            color = TextSecondary,
                            fontSize = 11.sp,
                        )
                    }
                }

                // Badge
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(accentColor.copy(alpha = 0.15f))
                        .border(0.8.dp, accentColor.copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text(
                        text = profile?.badgeLabel ?: "HyperOS",
                        color = accentColor,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            Text(
                text = profile?.architectureSummary ?: "Tối ưu hóa đa tầng Greezer & PowerKeeper",
                color = TextSecondary,
                fontSize = 12.sp,
                lineHeight = 16.sp,
            )

            Spacer(Modifier.height(12.dp))

            // Specs chips
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                SpecChip(label = profile?.hyperOsVersion ?: "HyperOS", color = accentColor)
                SpecChip(label = "Android ${profile?.androidRelease ?: "15"}", color = TextSecondary)
                if (is17Prm) {
                    SpecChip(label = "HyperOS 3 Ops", color = GreenOK)
                } else if (is15Pro) {
                    SpecChip(label = "Dual Aurogon", color = OrangeLight)
                }
            }

            Spacer(Modifier.height(14.dp))

            // 1-Tap Optimize button
            Button(
                onClick = onOptimize,
                enabled = !isOptimizing,
                modifier = Modifier.fillMaxWidth().height(46.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = accentColor,
                    contentColor = if (is17Prm) Color.Black else Color.White,
                ),
            ) {
                if (isOptimizing) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(18.dp),
                        color = Color.White,
                        strokeWidth = 2.dp,
                    )
                    Spacer(Modifier.width(8.dp))
                    Text("Đang tối ưu phần cứng...", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                } else {
                    Icon(Icons.Rounded.Bolt, null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = "🚀  Tối Ưu Tự Động Cho ${profile?.displayName ?: "Máy Này"}",
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                    )
                }
            }
        }
    }
}

@Composable
private fun SpecChip(label: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(SurfaceVariant)
            .padding(horizontal = 7.dp, vertical = 3.dp)
    ) {
        Text(text = label, color = color, fontSize = 10.sp, fontWeight = FontWeight.Medium)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Zalo VoIP Call & Anti-Freeze Card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun ZaloVoIpCard(
    isProtected: Boolean,
    isProtecting: Boolean,
    onFix: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.linearGradient(listOf(Color(0xFF041F1A), Color(0xFF071418)))
            )
            .border(1.dp, GreenOK.copy(alpha = 0.35f), RoundedCornerShape(18.dp))
            .padding(16.dp),
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(GreenOKDim),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Rounded.PhoneInTalk, null, tint = GreenOK, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Bảo Vệ Cuộc Gọi Zalo (VoIP)",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                        )
                        Text(
                            text = "Chống lỡ cuộc gọi khi khóa máy / treo lâu",
                            color = TextSecondary,
                            fontSize = 11.sp,
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isProtected) GreenOKDim else YellowWarnDim)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = if (isProtected) "Đã Bảo Vệ" else "Cần Bật",
                        color = if (isProtected) GreenOK else YellowWarn,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            val points = listOf(
                "Mở Cửa sổ nổi (10021): Hiện chuông gọi đến khi màn hình tắt",
                "TCP Network Whitelist: Duy trì socket không bị router ngắt",
                "Miễn trừ Greezer freeze & LMKD process killer",
            )
            points.forEach { pt ->
                Row(modifier = Modifier.padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.CheckCircle, null, tint = GreenOK, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(pt, color = TextSecondary, fontSize = 11.sp)
                }
            }

            Spacer(Modifier.height(12.dp))

            OutlinedButton(
                onClick = onFix,
                enabled = !isProtecting,
                modifier = Modifier.fillMaxWidth().height(42.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = GreenOK,
                    containerColor = GreenOKDim.copy(alpha = 0.3f),
                ),
                border = androidx.compose.foundation.BorderStroke(1.dp, GreenOK.copy(alpha = 0.6f)),
            ) {
                if (isProtecting) {
                    CircularProgressIndicator(modifier = Modifier.size(16.dp), color = GreenOK, strokeWidth = 2.dp)
                    Spacer(Modifier.width(8.dp))
                    Text("Đang thiết lập Zalo VoIP...", fontSize = 12.sp)
                } else {
                    Icon(Icons.Rounded.Check, null, modifier = Modifier.size(16.dp), tint = GreenOK)
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = if (isProtected) "✓ Cập Nhật Lại Bảo Vệ Zalo VoIP" else "📞 Bật Bảo Vệ Cuộc Gọi Zalo Ngay",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Permanent Immortality & Reboot Survival Card
// ─────────────────────────────────────────────────────────────────────────────

@Composable
private fun PermanentKeepAliveCard(
    isWatcherRunning: Boolean,
    onEnable: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(
                Brush.linearGradient(listOf(Color(0xFF10132A), Color(0xFF090A17)))
            )
            .border(1.dp, BlueAccent.copy(alpha = 0.35f), RoundedCornerShape(18.dp))
            .padding(16.dp),
    ) {
        Column {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(BlueDim),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(Icons.Rounded.AllInclusive, null, tint = BlueAccent, modifier = Modifier.size(20.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(
                            text = "Chạy Vĩnh Viễn & Chống Tắt App",
                            color = TextPrimary,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                        )
                        Text(
                            text = "Tự khởi động khi reboot · Tự hồi sinh khi bị kill",
                            color = TextSecondary,
                            fontSize = 11.sp,
                        )
                    }
                }

                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isWatcherRunning) GreenOKDim else BlueDim)
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        text = if (isWatcherRunning) "Đang Giữ App" else "Khả Dụng",
                        color = if (isWatcherRunning) GreenOK else BlueAccent,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
            }

            Spacer(Modifier.height(10.dp))

            val mechanisms = listOf(
                "Tự khởi động lại khi reboot (BootReceiver + Autostart OP 10008)",
                "Immortal Service: AlarmManager tự hồi sinh trong 1s nếu bị xóa khỏi Đa nhiệm",
                "Linux Shell Daemon chạy ngầm độc lập (/data/local/tmp)",
            )
            mechanisms.forEach { m ->
                Row(modifier = Modifier.padding(vertical = 2.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Rounded.Security, null, tint = BlueAccent, modifier = Modifier.size(13.dp))
                    Spacer(Modifier.width(6.dp))
                    Text(m, color = TextSecondary, fontSize = 11.sp)
                }
            }

            Spacer(Modifier.height(12.dp))

            Button(
                onClick = onEnable,
                modifier = Modifier.fillMaxWidth().height(42.dp),
                shape = RoundedCornerShape(10.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = BlueAccent,
                    contentColor = Color.White,
                ),
            ) {
                Icon(Icons.Rounded.Shield, null, modifier = Modifier.size(16.dp))
                Spacer(Modifier.width(6.dp))
                Text(
                    text = "🛡️  Kích Hoạt Chạy Vĩnh Viễn & Auto Reboot",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                )
            }
        }
    }
}

