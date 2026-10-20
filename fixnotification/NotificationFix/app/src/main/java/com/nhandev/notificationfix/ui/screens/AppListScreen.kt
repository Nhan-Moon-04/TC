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
import com.nhandev.notificationfix.domain.model.ALL_PRESETS
import com.nhandev.notificationfix.domain.model.PrivilegeMode
import com.nhandev.notificationfix.ui.components.*
import com.nhandev.notificationfix.ui.theme.*
import com.nhandev.notificationfix.viewmodel.AppListViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppListScreen(
    vm: AppListViewModel,
    onBackToHome: () -> Unit = {},
    onOpenDrawer: () -> Unit = {},
) {
    val state    by vm.uiState.collectAsState()
    val filtered by vm.filteredApps.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        // ── Top bar
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(SurfaceDark, BackgroundDark)
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.verticalGradient(
                        listOf(Color.Transparent, CardBorder)
                    ),
                    shape = RoundedCornerShape(0.dp),
                )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onBackToHome) {
                    Icon(
                        imageVector        = Icons.Rounded.ArrowBack,
                        contentDescription = "Quay về Trang chủ",
                        tint               = OrangeGlow,
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = if (state.activePreset != null)
                            "${state.activePreset!!.emoji} ${state.activePreset!!.label}"
                        else "Danh sách ứng dụng",
                        color      = TextPrimary,
                        fontSize   = 18.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    Text(
                        text       = "${filtered.size} ứng dụng · ${state.selectedPkgs.size} đã chọn",
                        color      = TextTertiary,
                        fontSize   = 11.sp,
                    )
                }

                // Filter toggle
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(
                            if (state.showOnlyUnfixed) OrangeDim else SurfaceVariant
                        )
                        .border(
                            1.dp,
                            if (state.showOnlyUnfixed) OrangeGlow.copy(alpha = 0.5f) else CardBorder,
                            RoundedCornerShape(10.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(
                        onClick = { vm.toggleShowOnlyUnfixed() },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (state.showOnlyUnfixed) Icons.Rounded.FilterAlt else Icons.Rounded.FilterAltOff,
                            contentDescription = "Lọc app chưa fix",
                            tint = if (state.showOnlyUnfixed) OrangeGlow else TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(Modifier.width(4.dp))

                // Drawer button
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(SurfaceVariant)
                        .border(1.dp, CardBorder, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(
                        onClick = onOpenDrawer,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            Icons.Rounded.Tune,
                            "Mở App Manager",
                            tint = TextSecondary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                Spacer(Modifier.width(4.dp))

                // Refresh
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(SurfaceVariant)
                        .border(1.dp, CardBorder, RoundedCornerShape(10.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    IconButton(
                        onClick = { vm.load() },
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(Icons.Rounded.Refresh, "Làm mới", tint = TextSecondary, modifier = Modifier.size(18.dp))
                    }
                }
            }
        }

        // ── Search bar
        Box(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
            TextField(
                value         = state.searchQuery,
                onValueChange = { vm.setSearchQuery(it) },
                modifier      = Modifier.fillMaxWidth(),
                placeholder   = {
                    Text("Tìm app hoặc package name...", color = TextTertiary, fontSize = 13.sp)
                },
                leadingIcon   = {
                    Icon(Icons.Rounded.Search, null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                },
                trailingIcon  = {
                    if (state.searchQuery.isNotEmpty()) {
                        IconButton(onClick = { vm.setSearchQuery("") }) {
                            Icon(Icons.Rounded.Close, null, tint = TextSecondary, modifier = Modifier.size(18.dp))
                        }
                    }
                },
                singleLine  = true,
                colors      = TextFieldDefaults.colors(
                    focusedContainerColor   = SurfaceVariant,
                    unfocusedContainerColor = SurfaceElevated,
                    focusedTextColor        = TextPrimary,
                    unfocusedTextColor      = TextPrimary,
                    focusedIndicatorColor   = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor             = OrangeGlow,
                ),
                shape       = RoundedCornerShape(14.dp),
            )
        }

        // ── Preset filter chips
        LazyRow(
            contentPadding        = PaddingValues(horizontal = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            item {
                PillChip(
                    selected = state.activePreset == null,
                    onClick  = { vm.selectAll() },
                    label    = "🔍 Tất cả",
                )
            }
            items(ALL_PRESETS) { preset ->
                PillChip(
                    selected = state.activePreset?.id == preset.id,
                    onClick  = { vm.selectPreset(preset) },
                    label    = "${preset.emoji} ${preset.label}",
                )
            }
        }

        Spacer(Modifier.height(8.dp))

        // ── Status filters
        val hasStatusFilter = state.filterAutoStart || state.filterRunInBg || state.filterDoze
        Column(modifier = Modifier.padding(horizontal = 12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Lọc theo trạng thái:", color = TextTertiary, fontSize = 11.sp)
                if (hasStatusFilter) {
                    TextButton(
                        onClick = { vm.clearStatusFilters() },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                    ) {
                        Text("Xóa lọc", color = YellowWarn, fontSize = 11.sp)
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                StatusFilterChip(
                    selected = state.filterAutoStart,
                    onClick  = { vm.toggleFilterAutoStart() },
                    label    = "🚀 Tự chạy",
                    onColor  = Color(0xFFE65100),
                )
                StatusFilterChip(
                    selected = state.filterRunInBg,
                    onClick  = { vm.toggleFilterRunInBg() },
                    label    = "🔄 Chạy nền",
                    onColor  = Color(0xFF2E7D32),
                )
                StatusFilterChip(
                    selected = state.filterDoze,
                    onClick  = { vm.toggleFilterDoze() },
                    label    = "🔋 Bỏ pin",
                    onColor  = Color(0xFF1565C0),
                )
            }

            if (hasStatusFilter && filtered.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        "${filtered.size} app thỏa mãn",
                        color = OrangeLight,
                        fontSize = 11.sp
                    )
                    TextButton(
                        onClick = { vm.selectFilteredApps(filtered) },
                        contentPadding = PaddingValues(horizontal = 6.dp, vertical = 0.dp)
                    ) {
                        Text("Chọn hết", color = OrangeGlow, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // ── Bulk actions bar
        AnimatedVisibility(
            visible = state.selectedPkgs.isNotEmpty(),
            enter   = expandVertically() + fadeIn(),
            exit    = shrinkVertically() + fadeOut(),
        ) {
            val context = androidx.compose.ui.platform.LocalContext.current
            Box(
                modifier = Modifier
                    .padding(horizontal = 12.dp, vertical = 6.dp)
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(
                        Brush.horizontalGradient(
                            listOf(Color(0xFF1F1000), Color(0xFF1A1520))
                        )
                    )
                    .border(1.dp, OrangeGlow.copy(alpha = 0.3f), RoundedCornerShape(14.dp))
                    .padding(horizontal = 12.dp, vertical = 10.dp),
            ) {
                Row(
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment     = Alignment.CenterVertically,
                    modifier              = Modifier.fillMaxWidth(),
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(24.dp)
                                .clip(CircleShape)
                                .background(OrangeGlow),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                "${state.selectedPkgs.size}",
                                color = Color.White,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        Spacer(Modifier.width(8.dp))
                        Text("app đã chọn", color = OrangeLight, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                    }
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        Button(
                            onClick  = {
                                if (state.privilegeMode == PrivilegeMode.NONE) {
                                    android.widget.Toast.makeText(context, "⚠️ Cần cấp quyền Shizuku!", android.widget.Toast.LENGTH_SHORT).show()
                                    vm.requestShizukuPermission()
                                } else {
                                    vm.fixSelected()
                                }
                            },
                            enabled = !state.isFixing,
                            colors   = ButtonDefaults.buttonColors(containerColor = OrangeGlow),
                            shape    = RoundedCornerShape(10.dp),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        ) {
                            Icon(Icons.Rounded.FlashOn, null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Fix", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                        OutlinedButton(
                            onClick  = {
                                if (state.privilegeMode == PrivilegeMode.NONE) {
                                    android.widget.Toast.makeText(context, "⚠️ Cần cấp quyền Shizuku!", android.widget.Toast.LENGTH_SHORT).show()
                                    vm.requestShizukuPermission()
                                } else {
                                    vm.restoreSelected()
                                }
                            },
                            enabled  = !state.isFixing,
                            shape    = RoundedCornerShape(10.dp),
                            colors   = ButtonDefaults.outlinedButtonColors(contentColor = YellowWarn),
                            border   = androidx.compose.foundation.BorderStroke(1.dp, YellowWarn.copy(alpha = 0.5f)),
                            contentPadding = PaddingValues(horizontal = 12.dp, vertical = 6.dp),
                        ) {
                            Icon(Icons.Rounded.RestartAlt, null, modifier = Modifier.size(14.dp))
                            Spacer(Modifier.width(4.dp))
                            Text("Hoàn tác", fontSize = 13.sp)
                        }
                    }
                }
            }
        }

        // ── App list
        val context = androidx.compose.ui.platform.LocalContext.current
        if (state.isLoading) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    CircularProgressIndicator(color = OrangeGlow, strokeWidth = 3.dp)
                    Spacer(Modifier.height(14.dp))
                    Text("Đang quét ứng dụng...", color = TextSecondary, fontSize = 14.sp)
                }
            }
        } else if (filtered.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(Icons.Rounded.SearchOff, null, tint = TextTertiary, modifier = Modifier.size(52.dp))
                    Spacer(Modifier.height(10.dp))
                    Text("Không tìm thấy app nào", color = TextTertiary, fontSize = 15.sp)
                    Text("Thử thay đổi bộ lọc", color = TextTertiary, fontSize = 12.sp)
                }
            }
        } else {
            LazyColumn(
                contentPadding      = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                items(filtered, key = { it.packageName }) { app ->
                    AppCard(
                        packageName    = app.packageName,
                        label          = app.label,
                        icon           = app.icon,
                        status         = state.statusMap[app.packageName],
                        isSelected     = state.selectedPkgs.contains(app.packageName),
                        onToggleSelect = { vm.toggleSelection(app.packageName) },
                        onFixClick     = {
                            if (state.privilegeMode == PrivilegeMode.NONE) {
                                android.widget.Toast.makeText(context, "⚠️ Cần cấp quyền Shizuku!", android.widget.Toast.LENGTH_SHORT).show()
                                vm.requestShizukuPermission()
                            } else {
                                vm.fixSingle(app.packageName, app.label)
                            }
                        },
                        onRestoreClick = {
                            if (state.privilegeMode == PrivilegeMode.NONE) {
                                android.widget.Toast.makeText(context, "⚠️ Cần cấp quyền Shizuku!", android.widget.Toast.LENGTH_SHORT).show()
                                vm.requestShizukuPermission()
                            } else {
                                vm.restoreSingle(app.packageName, app.label)
                            }
                        },
                    )
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
private fun PillChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
) {
    val bgColor by animateColorAsState(
        targetValue = if (selected) OrangeGlow else SurfaceVariant,
        label = "pillBg"
    )
    val textColor by animateColorAsState(
        targetValue = if (selected) Color.White else TextSecondary,
        label = "pillText"
    )

    Surface(
        onClick = onClick,
        color   = bgColor,
        shape   = RoundedCornerShape(20.dp),
    ) {
        Text(
            text     = label,
            color    = textColor,
            fontSize = 12.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
        )
    }
}

@Composable
private fun StatusFilterChip(
    selected: Boolean,
    onClick: () -> Unit,
    label: String,
    onColor: Color,
) {
    val bgColor by animateColorAsState(
        targetValue = if (selected) onColor else SurfaceVariant,
        label = "statusChipBg"
    )

    Surface(
        onClick = onClick,
        color   = bgColor,
        shape   = RoundedCornerShape(10.dp),
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            if (selected) {
                Icon(Icons.Rounded.Check, null, modifier = Modifier.size(12.dp), tint = Color.White)
                Spacer(Modifier.width(3.dp))
            }
            Text(
                label,
                fontSize = 11.sp,
                color    = if (selected) Color.White else TextSecondary,
                fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Normal,
            )
        }
    }
}
