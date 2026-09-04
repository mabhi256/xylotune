package com.xylotune.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

// The web app has no light theme, so neither does this port — always the dark palette,
// regardless of system setting.
private val XylotuneColorScheme = darkColorScheme(
    primary = AccentTeal,
    secondary = AccentBlue,
    tertiary = AccentPurple,
    error = AccentDanger,
    background = AppBackground,
    surface = SurfaceElevated,
    surfaceVariant = SurfaceControl,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    onSurfaceVariant = TextSecondary,
    outline = BorderSubtle,
)

@Composable
fun XylotuneTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = XylotuneColorScheme,
        typography = XylotuneTypography,
        content = content,
    )
}
