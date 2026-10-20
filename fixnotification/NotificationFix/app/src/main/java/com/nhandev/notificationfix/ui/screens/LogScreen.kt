package com.nhandev.notificationfix.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
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
import com.nhandev.notificationfix.domain.model.FixAction
import com.nhandev.notificationfix.domain.model.FixLogEntry
import com.nhandev.notificationfix.ui.theme.*
import com.nhandev.notificationfix.viewmodel.AppListViewModel
import java.text.SimpleDateFormat
import java.util.*

@Composable
fun LogScreen(
    vm: AppListViewModel,
    onBackToHome: () -> Unit = {},
) {
    val state by vm.uiState.collectAsState()
    val logs  = state.fixLogs.sortedByDescending { it.timestamp }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(BackgroundDark)
    ) {
        // ── Header
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(listOf(SurfaceDark, BackgroundDark))
                )
                .border(
                    1.dp,
                    Brush.verticalGradient(listOf(Color.Transparent, CardBorder)),
                    RoundedCornerShape(0.dp)
                )
        ) {
            Row(
                modifier          = Modifier
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
                        text       = "Lịch sử thao tác",
                        color      = TextPrimary,
                        fontSize   = 18.sp,
                        fontWeight = FontWeight.Bold,
                    )
                    if (logs.isNotEmpty()) {
                        Text(
                            text     = "${logs.size} bản ghi",
                            color    = TextTertiary,
                            fontSize = 11.sp,
                        )
                    }
                }
                if (logs.isNotEmpty()) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(10.dp))
                            .background(RedFailDim)
                            .border(1.dp, RedFail.copy(alpha = 0.3f), RoundedCornerShape(10.dp))
                    ) {
                        TextButton(
                            onClick = { vm.clearLogs() },
                            contentPadding = PaddingValues(horizontal = 10.dp, vertical = 4.dp),
                        ) {
                            Icon(Icons.Rounded.DeleteSweep, null, modifier = Modifier.size(16.dp), tint = RedFail)
                            Spacer(Modifier.width(4.dp))
                            Text("Xóa log", fontSize = 12.sp, color = RedFail)
                        }
                    }
                    Spacer(Modifier.width(8.dp))
                }
            }
        }

        // ── Summary strip
        if (logs.isNotEmpty()) {
            val successCount = logs.count { it.success }
            val failCount    = logs.size - successCount
            val fixCount     = logs.count { !it.action.name.startsWith("RESTORE") }
            val restoreCount = logs.count { it.action.name.startsWith("RESTORE") }

            Row(
                modifier              = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                LogStat("✅ OK",      "$successCount", GreenOK,    Modifier.weight(1f))
                LogStat("❌ Lỗi",    "$failCount",    RedFail,     Modifier.weight(1f))
                LogStat("⚡ Fix",    "$fixCount",     OrangeGlow,  Modifier.weight(1f))
                LogStat("↩ Restore", "$restoreCount", BlueAccent,  Modifier.weight(1f))
            }

            HorizontalDivider(color = CardBorder, thickness = 1.dp, modifier = Modifier.padding(horizontal = 12.dp))
            Spacer(Modifier.height(4.dp))
        }

        // ── Log list
        if (logs.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(SurfaceVariant),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(Icons.Rounded.History, null, tint = TextTertiary, modifier = Modifier.size(44.dp))
                    }
                    Spacer(Modifier.height(16.dp))
                    Text("Chưa có log nào", color = TextSecondary, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(4.dp))
                    Text("Thực hiện Fix để xem kết quả ở đây", color = TextTertiary, fontSize = 13.sp)
                }
            }
        } else {
            LazyColumn(
                contentPadding      = PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(0.dp),
            ) {
                itemsIndexed(logs, key = { _, it -> "${it.packageName}-${it.action}-${it.timestamp}" }) { index, entry ->
                    TimelineLogEntry(
                        entry    = entry,
                        isLast   = index == logs.lastIndex,
                    )
                }
                item { Spacer(Modifier.height(80.dp)) }
            }
        }
    }
}

@Composable
private fun LogStat(label: String, value: String, color: Color, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(color.copy(alpha = 0.1f))
            .border(1.dp, color.copy(alpha = 0.2f), RoundedCornerShape(10.dp))
            .padding(vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(value, color = color, fontSize = 16.sp, fontWeight = FontWeight.Bold)
            Text(label, color = color.copy(alpha = 0.7f), fontSize = 9.sp)
        }
    }
}

@Composable
private fun TimelineLogEntry(
    entry: FixLogEntry,
    isLast: Boolean,
) {
    val isRestore = entry.action.name.startsWith("RESTORE")
    val accent    = when {
        !entry.success -> RedFail
        isRestore      -> BlueAccent
        else           -> GreenOK
    }
    val bgColor   = when {
        !entry.success -> RedFailDim.copy(alpha = 0.5f)
        isRestore      -> BlueDim.copy(alpha = 0.5f)
        else           -> GreenOKDim.copy(alpha = 0.4f)
    }
    val timeStr = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date(entry.timestamp))
    val dateStr = SimpleDateFormat("dd/MM", Locale.getDefault()).format(Date(entry.timestamp))

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp),
    ) {
        // Timeline column
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier            = Modifier.width(24.dp)
        ) {
            // Circle dot
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(accent.copy(alpha = 0.2f))
                    .border(2.dp, accent, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(5.dp)
                        .clip(CircleShape)
                        .background(accent)
                )
            }
            // Vertical line
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(60.dp)
                        .background(
                            Brush.verticalGradient(
                                listOf(accent.copy(alpha = 0.3f), Color.Transparent)
                            )
                        )
                )
            }
        }

        Spacer(Modifier.width(10.dp))

        // Card
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = if (isLast) 0.dp else 6.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(bgColor)
                .border(1.dp, accent.copy(alpha = 0.25f), RoundedCornerShape(12.dp))
                .padding(10.dp)
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text       = entry.appLabel,
                            color      = TextPrimary,
                            fontWeight = FontWeight.SemiBold,
                            fontSize   = 13.sp,
                        )
                        Column(horizontalAlignment = Alignment.End) {
                            Text(timeStr, color = TextTertiary, fontSize = 10.sp)
                            Text(dateStr, color = TextTertiary, fontSize = 9.sp)
                        }
                    }

                    Text(
                        text     = entry.packageName,
                        color    = TextTertiary,
                        fontSize = 9.sp,
                    )

                    Spacer(Modifier.height(4.dp))

                    // Action badge
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(5.dp))
                            .background(accent.copy(alpha = 0.15f))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text       = formatAction(entry.action),
                            color      = accent,
                            fontSize   = 10.sp,
                            fontWeight = FontWeight.Medium,
                        )
                    }

                    if (entry.message.isNotBlank()) {
                        Spacer(Modifier.height(3.dp))
                        Text(text = entry.message, color = TextSecondary, fontSize = 10.sp)
                    }
                }
            }
        }

        Spacer(Modifier.width(12.dp))
    }
}

private fun formatAction(action: FixAction): String = when (action) {
    FixAction.FIX_WAKE_LOCK                 -> "⚡ WAKE_LOCK → allow"
    FixAction.FIX_RUN_IN_BG                 -> "⚡ RUN_IN_BACKGROUND → allow"
    FixAction.FIX_AUTO_START                -> "⚡ AUTO_START → allow"
    FixAction.FIX_DOZE_WHITELIST            -> "⚡ Doze whitelist → added"
    FixAction.FIX_MILLET_PROTECT            -> "🛡️ MILLET_NO_RESTRICT → added"
    FixAction.FIX_GREEZER_LM                -> "🔧 Greezer LM → disabled"
    FixAction.FIX_NON_FCM_PROTECT           -> "★ Non-FCM Deep Protect"
    FixAction.FIX_DEVICE_PROFILE_OPTIMIZE   -> "🚀 Tối ưu Model phần cứng"
    FixAction.FIX_ZALO_VOIP                 -> "📞 Zalo VoIP / Cuộc gọi nền"
    FixAction.FIX_SELF_IMMUNITY             -> "🛡️ Cấp quyền Bất Tử App"
    FixAction.START_WATCHDOG_DAEMON         -> "⚙️ Khởi động Shell Daemon"
    FixAction.RESTORE_WAKE_LOCK             -> "↩ WAKE_LOCK → default"
    FixAction.RESTORE_RUN_IN_BG             -> "↩ RUN_IN_BACKGROUND → default"
    FixAction.RESTORE_AUTO_START            -> "↩ AUTO_START → off"
    FixAction.RESTORE_DOZE_WHITELIST        -> "↩ Doze whitelist → removed"
    FixAction.FIX_ALL                       -> "⚡ Fix All"
    FixAction.RESTORE_ALL                   -> "↩ Restore All"
    else                                    -> "⚡ ${action.name}"
}
