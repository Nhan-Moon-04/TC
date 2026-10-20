package com.nhandev.notificationfix

import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.compose.*
import com.nhandev.notificationfix.ui.components.*
import com.nhandev.notificationfix.ui.screens.*
import com.nhandev.notificationfix.ui.theme.*
import com.nhandev.notificationfix.viewmodel.AppListViewModel
import kotlinx.coroutines.launch

sealed class Screen(val route: String, val label: String, val icon: ImageVector) {
    object Home : Screen("home", "Trang chủ", Icons.Rounded.Home)
    object Apps : Screen("apps", "Ứng dụng",  Icons.Rounded.Apps)
    object Log  : Screen("log",  "Lịch sử",   Icons.Rounded.History)
}

private val TOP_LEVEL_SCREENS = listOf(Screen.Home, Screen.Apps, Screen.Log)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppNavigation() {
    val navController = rememberNavController()
    val vm: AppListViewModel = hiltViewModel()
    val state by vm.uiState.collectAsState()

    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                vm.refreshPrivilege()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = navBackStackEntry?.destination?.route

    val navigateToHome: () -> Unit = {
        if (currentRoute != Screen.Home.route) {
            navController.navigate(Screen.Home.route) {
                popUpTo(Screen.Home.route) { inclusive = false }
                launchSingleTop = true
            }
        }
    }

    val navigateToApps: () -> Unit = {
        if (currentRoute != Screen.Apps.route) {
            navController.navigate(Screen.Apps.route) {
                popUpTo(Screen.Home.route) { inclusive = false }
                launchSingleTop = true
            }
        }
    }

    // ── Drawer state
    val drawerState   = rememberDrawerState(DrawerValue.Closed)
    val coroutineScope = rememberCoroutineScope()

    val openDrawer  = { coroutineScope.launch { drawerState.open() } }
    val closeDrawer = { coroutineScope.launch { drawerState.close() } }

    // ── Progress + Success dialogs
    if (state.isFixing) {
        FixProgressDialog(
            progress   = state.fixProgress,
            currentApp = state.fixCurrentApp,
        )
    }

    state.fixSuccessMessage?.let { msg ->
        FixSuccessDialog(
            message   = msg,
            onDismiss = { vm.dismissSuccessDialog() },
        )
    }

    ModalNavigationDrawer(
        drawerState   = drawerState,
        gesturesEnabled = true,
        drawerContent = {
            FixedAppsDrawerContent(
                state       = state,
                onToggleApp = { pkg, label -> vm.toggleAppFix(pkg, label) },
                onFixAll    = {
                    closeDrawer()
                    vm.fixSelected()
                },
                onClose     = { closeDrawer() },
            )
        },
        scrimColor = BackgroundDark.copy(alpha = 0.7f),
    ) {
        Scaffold(
            containerColor = BackgroundDark,
            bottomBar = {
                PremiumBottomBar(
                    currentRoute = currentRoute,
                    onNavigate   = { screen ->
                        if (currentRoute != screen.route) {
                            navController.navigate(screen.route) {
                                popUpTo(Screen.Home.route) { inclusive = false }
                                launchSingleTop = true
                            }
                        }
                    }
                )
            }
        ) { innerPadding ->
            NavHost(
                navController    = navController,
                startDestination = Screen.Home.route,
                modifier         = Modifier.padding(innerPadding),
                enterTransition  = { fadeIn(tween(220)) + slideInHorizontally(tween(220)) { it / 8 } },
                exitTransition   = { fadeOut(tween(180)) },
                popEnterTransition  = { fadeIn(tween(220)) + slideInHorizontally(tween(220)) { -it / 8 } },
                popExitTransition   = { fadeOut(tween(180)) },
            ) {
                composable(Screen.Home.route) {
                    HomeScreen(
                        vm               = vm,
                        onNavigateToApps = navigateToApps,
                        onOpenDrawer     = { openDrawer() },
                    )
                }
                composable(Screen.Apps.route) {
                    BackHandler { navigateToHome() }
                    AppListScreen(
                        vm           = vm,
                        onBackToHome = navigateToHome,
                        onOpenDrawer = { openDrawer() },
                    )
                }
                composable(Screen.Log.route) {
                    BackHandler { navigateToHome() }
                    LogScreen(
                        vm           = vm,
                        onBackToHome = navigateToHome,
                    )
                }
            }
        }
    }
}

@Composable
private fun PremiumBottomBar(
    currentRoute: String?,
    onNavigate: (Screen) -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(Color.Transparent)
    ) {
        // Glass background
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, SurfaceDark.copy(alpha = 0.98f))
                    )
                )
                .border(
                    width = 1.dp,
                    brush = Brush.horizontalGradient(
                        listOf(Color.Transparent, GlassBorder, Color.Transparent)
                    ),
                    shape = RoundedCornerShape(topStart = 0.dp, topEnd = 0.dp),
                )
        ) {
            NavigationBar(
                containerColor = Color.Transparent,
                tonalElevation = 0.dp,
            ) {
                TOP_LEVEL_SCREENS.forEach { screen ->
                    val selected = currentRoute == screen.route
                    NavigationBarItem(
                        selected = selected,
                        onClick  = { onNavigate(screen) },
                        icon = {
                            Box(contentAlignment = Alignment.Center) {
                                // Glow behind selected icon
                                if (selected) {
                                    Box(
                                        modifier = Modifier
                                            .size(40.dp)
                                            .clip(CircleShape)
                                            .background(OrangeGlow.copy(alpha = 0.15f))
                                    )
                                }
                                Icon(
                                    imageVector        = screen.icon,
                                    contentDescription = screen.label,
                                    modifier           = Modifier.size(if (selected) 26.dp else 22.dp),
                                    tint               = if (selected) OrangeGlow else TextTertiary,
                                )
                            }
                        },
                        label = {
                            Text(
                                text       = screen.label,
                                color      = if (selected) OrangeGlow else TextTertiary,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                fontSize   = 11.sp,
                            )
                        },
                        colors = NavigationBarItemDefaults.colors(
                            indicatorColor      = Color.Transparent,
                            selectedIconColor   = OrangeGlow,
                            unselectedIconColor = TextTertiary,
                            selectedTextColor   = OrangeGlow,
                            unselectedTextColor = TextTertiary,
                        ),
                    )
                }
            }
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// Fix Progress Dialog
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun FixProgressDialog(
    progress: Float,
    currentApp: String,
) {
    val percent = (progress.coerceIn(0f, 1f) * 100).toInt()
    val animProgress by animateFloatAsState(
        targetValue = progress.coerceIn(0.02f, 1f),
        animationSpec = tween(400),
        label = "progress"
    )

    AlertDialog(
        onDismissRequest = {},
        properties       = DialogProperties(dismissOnBackPress = false, dismissOnClickOutside = false),
        containerColor   = CardDark,
        shape            = RoundedCornerShape(24.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(OrangeGlow.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector        = Icons.Rounded.FlashOn,
                        contentDescription = null,
                        tint               = OrangeGlow,
                        modifier           = Modifier.size(24.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text       = "Đang tối ưu...",
                        fontWeight = FontWeight.Bold,
                        fontSize   = 17.sp,
                        color      = TextPrimary,
                    )
                    Text("$percent% hoàn thành", color = OrangeLight, fontSize = 12.sp)
                }
            }
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text     = if (currentApp.isNotBlank()) currentApp else "Đang khởi tạo...",
                    color    = TextSecondary,
                    fontSize = 13.sp,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(16.dp))
                // Segmented progress bar
                LinearProgressIndicator(
                    progress        = { animProgress },
                    modifier        = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .clip(RoundedCornerShape(5.dp)),
                    color           = OrangeGlow,
                    trackColor      = SurfaceVariant,
                    strokeCap       = androidx.compose.ui.graphics.StrokeCap.Round,
                )
                Spacer(Modifier.height(10.dp))
                Row(
                    modifier              = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Tiến trình", color = TextTertiary, fontSize = 12.sp)
                    Text(
                        text       = "$percent%",
                        color      = OrangeLight,
                        fontWeight = FontWeight.Bold,
                        fontSize   = 14.sp
                    )
                }
            }
        },
        confirmButton = {}
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// Fix Success Dialog
// ─────────────────────────────────────────────────────────────────────────────

@Composable
fun FixSuccessDialog(
    message: String,
    onDismiss: () -> Unit,
) {
    val isRestore = message.contains("hoàn tác", ignoreCase = true) || message.contains("gốc", ignoreCase = true)
    val accent    = if (isRestore) YellowWarn else GreenOK

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor   = CardDark,
        shape            = RoundedCornerShape(24.dp),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(accent.copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector        = if (isRestore) Icons.Rounded.RestartAlt else Icons.Rounded.CheckCircle,
                        contentDescription = null,
                        tint               = accent,
                        modifier           = Modifier.size(26.dp)
                    )
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        text       = if (isRestore) "Đã hoàn tác!" else "Hoàn tất! 🎉",
                        fontWeight = FontWeight.Bold,
                        fontSize   = 18.sp,
                        color      = accent
                    )
                    Text(text = message, color = TextSecondary, fontSize = 12.sp)
                }
            }
        },
        text = {
            Surface(
                shape    = RoundedCornerShape(16.dp),
                color    = SurfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier            = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    if (isRestore) {
                        SuccessItem("Tự khởi chạy (Autostart): Đã TẮT về mặc định", YellowWarn)
                        SuccessItem("Chạy ngầm: Đã tắt quyền chạy không giới hạn", YellowWarn)
                        SuccessItem("Pin Xiaomi: Đã đưa về tiết kiệm pin thông thường", YellowWarn)
                        SuccessItem("MILLET: Đã xóa khỏi danh sách bảo vệ", YellowWarn)
                    } else {
                        SuccessItem("MILLET_NO_RESTRICT: Bảo vệ khỏi Greezer freeze", GreenOK)
                        SuccessItem("Greezer LM: Đã thêm vào allowlist", GreenOK)
                        SuccessItem("Tự khởi chạy (Autostart Xiaomi): ĐÃ BẬT", GreenOK)
                        SuccessItem("Chạy ngầm (RUN_IN_BG): Cho phép không giới hạn", GreenOK)
                        SuccessItem("Tiết kiệm pin Xiaomi: Đã loại trừ hoàn toàn", GreenOK)
                        SuccessItem("POST_NOTIFICATION: Cho phép thông báo", GreenOK)
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick  = onDismiss,
                colors   = ButtonDefaults.buttonColors(containerColor = accent),
                shape    = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text       = "Đã hiểu & Đóng",
                    fontWeight = FontWeight.Bold,
                    color      = if (isRestore) Color(0xFF1A1000) else Color.White,
                    fontSize   = 15.sp
                )
            }
        }
    )
}

@Composable
private fun SuccessItem(text: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(16.dp)
                .clip(CircleShape)
                .background(color.copy(alpha = 0.2f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(Icons.Rounded.Check, null, tint = color, modifier = Modifier.size(10.dp))
        }
        Spacer(Modifier.width(8.dp))
        Text(text, fontSize = 12.sp, color = TextPrimary)
    }
}
