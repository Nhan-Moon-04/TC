package com.nhandev.notificationfix.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary              = OrangeGlow,
    onPrimary            = Color.White,
    primaryContainer     = OrangeDim,
    onPrimaryContainer   = OrangeLight,
    secondary            = BlueAccent,
    onSecondary          = BackgroundDark,
    secondaryContainer   = BlueDim,
    onSecondaryContainer = BlueAccent,
    tertiary             = PurpleAccent,
    background           = BackgroundDark,
    onBackground         = TextPrimary,
    surface              = SurfaceDark,
    onSurface            = TextPrimary,
    surfaceVariant       = SurfaceVariant,
    onSurfaceVariant     = TextSecondary,
    surfaceContainer     = CardDark,
    surfaceContainerHigh = CardElevated,
    error                = RedFail,
    onError              = Color.White,
    errorContainer       = RedFailDim,
    outline              = CardBorder,
    outlineVariant       = DrawerBorder,
    inverseSurface       = TextPrimary,
    inverseOnSurface     = BackgroundDark,
)

@Composable
fun NotificationFixTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography  = Typography,
        content     = content
    )
}
