package com.dreamstream.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val TeslaColorScheme = darkColorScheme(

    primary = TeslaRed,

    secondary = TeslaRed,

    background = TeslaBlack,

    surface = TeslaSurface,

    onPrimary = TeslaWhite,

    onSecondary = TeslaWhite,

    onBackground = TeslaWhite,

    onSurface = TeslaWhite
)

@Composable
fun DreamStreamTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = TeslaColorScheme,
        typography = AppTypography,
        content = content
    )
}
