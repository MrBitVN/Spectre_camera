package com.spectreflow.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = AccentEmerald,
    secondary = AccentMint,
    background = BgObsidian,
    surface = BgSurface,
    onPrimary = BgObsidian,
    onSecondary = BgObsidian,
    onBackground = TextPrimary,
    onSurface = TextPrimary
)

@Composable
fun SpectreFlowTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
