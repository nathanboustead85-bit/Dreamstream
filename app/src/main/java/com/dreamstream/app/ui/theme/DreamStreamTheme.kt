package com.dreamstream.app.ui.theme

import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val UltraBlue = Color(0xFF00C8FF)
val UltraPurple = Color(0xFF985BFF)
val UltraBackground = Color(0xFF080B16)
val UltraSurface = Color(0xFF151B30)

private val UltraColors = darkColorScheme(
    primary = UltraBlue,
    onPrimary = Color(0xFF00141E),
    secondary = UltraPurple,
    onSecondary = Color.White,
    background = UltraBackground,
    onBackground = Color.White,
    surface = UltraSurface,
    onSurface = Color.White,
    surfaceVariant = Color(0xFF202942),
    onSurfaceVariant = Color(0xFFCCD5EB),
    error = Color(0xFFFF667A)
)

@Composable
fun DreamStreamTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = UltraColors,
        typography = Typography(),
        content = content
    )
}
