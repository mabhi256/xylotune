package com.xylotune.app.ui.xylophone

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xylotune.app.audio.SoundMaterial
import com.xylotune.app.ui.crayon.RoughShape

// Dumb and pointer-input-free by design (see the plan's Risk Area 1): all touch handling
// lives on the shared listener in XylophoneBoard, never here — a per-Bar pointerInput
// modifier is exactly what breaks independent two-thumb multitouch in Compose.
//
// Height comes from `fillMaxHeight()`, not a fixed dp: XylophoneBoard gives the bars row a
// `weight(1f)` slot, so the bars always claim whatever vertical space the screen has left
// after its own header/drawer chrome, on any device, instead of a constant that could
// overflow a short landscape viewport.
@Composable
fun Bar(
    label: String,
    color: Color,
    edgeColor: Color,
    material: SoundMaterial,
    index: Int,
    isTarget: Boolean,
    strikeId: Int,
    modifier: Modifier = Modifier,
) {
    // 0 = normal (bar's own color, scale 1); 1 = freshly struck (brighter mix, scale
    // 0.92) — mirrors the web's @keyframes barStrike running from the flashed state to
    // normal over 180ms.
    val flash = remember { Animatable(0f) }
    LaunchedEffect(strikeId) {
        if (strikeId > 0) {
            flash.snapTo(1f)
            flash.animateTo(0f, tween(180, easing = CubicBezierEasing(0.2f, 0.9f, 0.3f, 1f)))
        }
    }

    val targetPulse = rememberInfiniteTransition(label = "barTargetPulse")
    val pulseBrightness by targetPulse.animateFloat(
        initialValue = 1f,
        targetValue = if (isTarget) 1.35f else 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(550, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse,
        ),
        label = "barTargetPulseBrightness",
    )

    val flashColor = lerp(color, Color.White, 0.55f * flash.value)
    val brightness = (1f + 0.25f * flash.value) * pulseBrightness
    val scaleAmount = 1f - 0.08f * flash.value
    val displayColor = Color(
        red = (flashColor.red * brightness).coerceIn(0f, 1f),
        green = (flashColor.green * brightness).coerceIn(0f, 1f),
        blue = (flashColor.blue * brightness).coerceIn(0f, 1f),
        alpha = 1f,
    )
    // Seeded by index alone (not material/strikeId/etc): a bar's hand-drawn wobble is a
    // fixed trait of that key, not something that should reshuffle when it's struck or the
    // material toggles.
    val shape = remember(index) { RoughShape(cornerRadiusPx = 15f, seed = index) }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .scale(scaleAmount)
            .clip(shape)
            .background(displayColor)
            .border(2.5.dp, edgeColor, shape)
            .crayonTexture(material)
            .then(if (isTarget) Modifier.dashedTargetRing() else Modifier)
            .semantics {
                contentDescription = if (isTarget) {
                    "Bar ${index + 1}, note $label, practice target — tap this one"
                } else {
                    "Bar ${index + 1}, note $label"
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(text = label, color = Color(0xFF2E2A22), fontSize = 26.sp, style = MaterialTheme.typography.titleLarge)
            Text(
                text = "${index + 1}",
                color = Color(0xFF2E2A22).copy(alpha = 0.62f),
                fontSize = 13.sp,
                style = MaterialTheme.typography.labelMedium,
            )
        }
    }
}

// A crayon's waxy sheen: a faint diagonal hatch over the fill, always present. Wood keeps
// its own horizontal grain lines layered on top of this, matching the material's real
// texture rather than only the generic waxy one.
private fun Modifier.crayonTexture(material: SoundMaterial): Modifier = drawWithContent {
    drawContent()
    val period = 7.dp.toPx()
    val diagLine = Color.White.copy(alpha = 0.16f)
    var d = -size.height
    while (d < size.width) {
        drawLine(diagLine, Offset(d, size.height), Offset(d + size.height, 0f), strokeWidth = 1.4f)
        d += period
    }
    if (material == SoundMaterial.Wood) {
        val grainPeriod = 13.dp.toPx()
        val darkLine = Color.Black.copy(alpha = 0.08f)
        val lightLine = Color.White.copy(alpha = 0.05f)
        var y = 0f
        while (y < size.height) {
            drawRect(darkLine, topLeft = Offset(0f, y), size = Size(size.width, 1f))
            val lightY = y + 5f
            if (lightY < size.height) drawRect(lightLine, topLeft = Offset(0f, lightY), size = Size(size.width, 1f))
            y += grainPeriod
        }
    }
}

// The practice target's dashed ring, inset from the bar's own edge so it stays inside the
// already-clipped rough shape rather than needing to draw (and get cut off) outside it.
private fun Modifier.dashedTargetRing(inset: Dp = 5.dp): Modifier = drawWithContent {
    drawContent()
    val insetPx = inset.toPx()
    val stroke = Stroke(width = 3.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 7f)))
    drawRoundRect(
        color = Color.White,
        topLeft = Offset(insetPx, insetPx),
        size = Size(size.width - insetPx * 2, size.height - insetPx * 2),
        cornerRadius = CornerRadius(11f),
        style = stroke,
    )
}
