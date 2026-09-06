package com.xylotune.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

// Always the paper palette, regardless of system theme — see Color.kt's note on why this
// stays a single committed look rather than adapting to light/dark.
private val XylotuneColorScheme = lightColorScheme(
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
