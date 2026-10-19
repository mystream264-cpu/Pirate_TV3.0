package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val PirateDarkColorScheme = darkColorScheme(
    primary = GoldPrimary,
    onPrimary = BackgroundDark,
    primaryContainer = PanelAltDark,
    onPrimaryContainer = GoldLight,
    secondary = GoldLight,
    onSecondary = BackgroundDark,
    background = BackgroundDark,
    onBackground = TextPrimary,
    surface = PanelDark,
    onSurface = TextPrimary,
    surfaceVariant = PanelAltDark,
    onSurfaceVariant = TextSecondary,
    outline = BorderDark,
    outlineVariant = InputBorderDark
)

@Composable
fun PirateTVTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = PirateDarkColorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = true,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    PirateTVTheme(content = content)
}
