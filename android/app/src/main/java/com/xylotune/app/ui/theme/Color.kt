package com.xylotune.app.ui.theme

import androidx.compose.ui.graphics.Color
import kotlin.math.abs

// Rainbow bar palette, C5-C6 — ported verbatim from index.html's --c1..--c8 custom properties.
val NoteC = Color(0xFFD63C30)
val NoteD = Color(0xFFEE9421)
val NoteE = Color(0xFFE6BD1C)
val NoteF = Color(0xFF4BA54F)
val NoteG = Color(0xFF12897E)
val NoteA = Color(0xFF189AD3)
val NoteB = Color(0xFF6A3FAE)
val NoteC2 = Color(0xFFD84941)

// CSS hsl(h, s%, l%) -> Color, kept as a direct formula (not hand-converted hex) so these
// stay verifiable against index.html's own hsl(...) declarations at a glance.
private fun hsl(hueDeg: Float, saturation: Float, lightness: Float): Color {
    val c = (1f - abs(2f * lightness - 1f)) * saturation
    val hPrime = hueDeg / 60f
    val x = c * (1f - abs(hPrime % 2f - 1f))
    val (r1, g1, b1) = when {
        hPrime < 1f -> Triple(c, x, 0f)
        hPrime < 2f -> Triple(x, c, 0f)
        hPrime < 3f -> Triple(0f, c, x)
        hPrime < 4f -> Triple(0f, x, c)
        hPrime < 5f -> Triple(x, 0f, c)
        else -> Triple(c, 0f, x)
    }
    val m = lightness - c / 2f
    return Color(red = r1 + m, green = g1 + m, blue = b1 + m)
}

// Wood material palette — ported verbatim from index.html's
// body[data-material="wood"] { --c1..--c8: hsl(...) } oak-to-walnut gradient.
val WoodC = hsl(32f, 0.42f, 0.62f)
val WoodD = hsl(31f, 0.42f, 0.59f)
val WoodE = hsl(30f, 0.43f, 0.55f)
val WoodF = hsl(28f, 0.43f, 0.52f)
val WoodG = hsl(27f, 0.44f, 0.48f)
val WoodA = hsl(25f, 0.45f, 0.45f)
val WoodB = hsl(24f, 0.46f, 0.41f)
val WoodC2 = hsl(22f, 0.47f, 0.38f)

val AppBackground = Color(0xFF212326)
val SurfaceElevated = Color(0xFF2A2D31)
val SurfaceControl = Color(0xFF35383D)
val SurfaceControlHover = Color(0xFF3F434A)
val BorderSubtle = Color(0xFF3A3D42)
val TextPrimary = Color(0xFFE8EAED)
val TextSecondary = Color(0xFF9AA0A6)
val TextMuted = Color(0xFF6F7479)

val AccentTeal = Color(0xFF12897E)
val AccentAmber = Color(0xFFB9691C)
val AccentBlue = Color(0xFF189AD3)
val AccentPurple = Color(0xFF6A3FAE)
val AccentDanger = Color(0xFFA83A32)
