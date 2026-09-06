package com.xylotune.app.ui.theme

import androidx.compose.ui.graphics.Color
import kotlin.math.abs

// The paper-roll redesign's bar palette: same hues and order as the original dark-ground
// rainbow, lightness/saturation normalised so nothing reads heavy on the bright paper
// ground, with a per-bar darker edge so the yellow bar doesn't dissolve into the page.
// Dark ink text on every one of these clears 4.5:1 contrast — verified against the exact
// hexes below during design, not re-derived at runtime.
val NoteC = Color(0xFFDE5D54)
val NoteD = Color(0xFFEA943E)
val NoteE = Color(0xFFE6B933)
val NoteF = Color(0xFF51B856)
val NoteG = Color(0xFF37AEA2)
val NoteA = Color(0xFF2FA7DA)
val NoteB = Color(0xFF9671D0)
val NoteC2 = Color(0xFFDC706A)

val NoteCEdge = Color(0xFFCC2A1E)
val NoteDEdge = Color(0xFFD17010)
val NoteEEdge = Color(0xFFC09411)
val NoteFEdge = Color(0xFF328F37)
val NoteGEdge = Color(0xFF217D74)
val NoteAEdge = Color(0xFF177EAB)
val NoteBEdge = Color(0xFF6D37C3)
val NoteC2Edge = Color(0xFFD63129)

// CSS hsl(h, s%, l%) -> Color, kept as a direct formula (not hand-converted hex) so these
// stay verifiable against the wood gradient's own hsl(...) declarations at a glance.
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

// Wood material palette — an oak-to-walnut gradient, lightness/saturation stepped the same
// way across all eight bars as the metal palette above, so switching material never makes
// one particular bar jump in perceived brightness relative to its neighbours.
val WoodC = hsl(32f, 0.42f, 0.60f)
val WoodD = hsl(30.3f, 0.44f, 0.58f)
val WoodE = hsl(28.6f, 0.47f, 0.55f)
val WoodF = hsl(26.9f, 0.49f, 0.53f)
val WoodG = hsl(25.1f, 0.51f, 0.51f)
val WoodA = hsl(23.4f, 0.54f, 0.48f)
val WoodB = hsl(21.7f, 0.56f, 0.46f)
val WoodC2 = hsl(20f, 0.58f, 0.44f)

val WoodCEdge = hsl(32f, 0.48f, 0.46f)
val WoodDEdge = hsl(30.3f, 0.50f, 0.44f)
val WoodEEdge = hsl(28.6f, 0.53f, 0.41f)
val WoodFEdge = hsl(26.9f, 0.55f, 0.39f)
val WoodGEdge = hsl(25.1f, 0.57f, 0.37f)
val WoodAEdge = hsl(23.4f, 0.60f, 0.34f)
val WoodBEdge = hsl(21.7f, 0.62f, 0.32f)
val WoodC2Edge = hsl(20f, 0.64f, 0.30f)

// Paper-and-crayon neutrals. This is a deliberately single, always-light theme — the app
// has no dark-mode variant, matching the original dark-only build's own reasoning, just
// with the polarity flipped: the instrument is a physical toy metaphor, not a themed UI,
// so it looks the same regardless of the device's system theme.
val AppBackground = Color(0xFFFFFDF7)
val SurfaceElevated = Color(0xFFFFFFFF)
val SurfaceControl = Color(0xFFF3EEDF)
val SurfaceControlHover = Color(0xFFEDE6D2)
val BorderSubtle = Color(0xFFE6DDC9)
val TextPrimary = Color(0xFF33302B)
val TextSecondary = Color(0xFF776F62)
val TextMuted = Color(0xFFA89E8D)
// The lyric caption's colour when untagged — distinct from TextMuted so it doesn't
// silently drift if TextMuted's own role changes elsewhere.
val TextLyric = Color(0xFF8A7448)

val AccentTeal = Color(0xFF1F8A7C)
val AccentAmber = Color(0xFFB9691C)
val AccentBlue = Color(0xFF2E86C8)
val AccentPurple = Color(0xFF6A3FAE)
val AccentDanger = Color(0xFFC7452C)

// The reading-head red and the manila paper the roll/sheet are drawn on.
val NowRed = Color(0xFFE2553D)
val PaperFill = Color(0xFFF3E7CB)
val PaperEdge = Color(0xFFE7D8B4)
