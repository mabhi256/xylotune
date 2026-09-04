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
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.xylotune.app.audio.SoundMaterial

// Dumb and pointer-input-free by design (see the plan's Risk Area 1): all touch handling
// lives on the shared listener in XylophoneBoard, never here — a per-Bar pointerInput
// modifier is exactly what breaks independent two-thumb multitouch in Compose.
@Composable
fun Bar(
    label: String,
    color: Color,
    material: SoundMaterial,
    index: Int,
    isTarget: Boolean,
    strikeId: Int,
    heightDp: Dp,
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
    val shape = RoundedCornerShape(10.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(heightDp)
            .scale(scaleAmount)
            .then(if (isTarget) Modifier.border(3.dp, Color.White, shape) else Modifier)
            .clip(shape)
            .background(displayColor)
            .then(if (material == SoundMaterial.Wood) Modifier.woodGrain() else Modifier)
            .semantics {
                contentDescription = if (isTarget) {
                    "Bar ${index + 1}, note $label, practice target — tap this one"
                } else {
                    "Bar ${index + 1}, note $label"
                }
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(text = label, color = Color(0xFF17181A), fontSize = 26.sp, style = MaterialTheme.typography.titleLarge)
    }
}

// Approximates index.html's wood-material CSS: a repeating 13px-tall band of one dark
// line then, 5px later, one light line — a cheap but recognizable wood-grain cue.
private fun Modifier.woodGrain(): Modifier = drawWithContent {
    drawContent()
    val period = 13.dp.toPx()
    val darkLine = Color.Black.copy(alpha = 0.08f)
    val lightLine = Color.White.copy(alpha = 0.05f)
    var y = 0f
    while (y < size.height) {
        drawRect(darkLine, topLeft = Offset(0f, y), size = Size(size.width, 1f))
        val lightY = y + 5f
        if (lightY < size.height) {
            drawRect(lightLine, topLeft = Offset(0f, lightY), size = Size(size.width, 1f))
        }
        y += period
    }
}
