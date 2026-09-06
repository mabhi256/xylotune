package com.xylotune.app.ui.composer

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.keyframes
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.xylotune.app.ui.theme.NowRed

// Ported from index.html's .caret — a blinking text-insertion indicator. Defaults to the
// same reading-head red the roll uses, so the sheet's caret and the roll's caret read as
// the same idea (a fixed point where "now" is) rather than two unrelated colours.
@Composable
fun Caret(color: Color = NowRed) {
    val transition = rememberInfiniteTransition(label = "caretBlink")
    val alpha by transition.animateFloat(
        initialValue = 1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 1100
                1f at 0
                1f at 549
                0f at 550
                0f at 1099
            },
            repeatMode = RepeatMode.Restart,
        ),
        label = "caretBlinkAlpha",
    )
    Box(
        modifier = Modifier
            .width(2.dp)
            .height(18.dp)
            .alpha(alpha)
            .background(color),
    )
}
