package com.xylotune.app.ui.icons

import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import kotlin.math.cos
import kotlin.math.sin

/** The six toolbar actions plus the four things the lyrics button cycles through. */
enum class IconKind { Settings, Music, Roll, Sheet, Link, Keys, Reset, Play, Pause, Material }

/**
 * Line-art icons drawn directly on a [Canvas], not vector assets — the toolbar's crayon
 * identity comes from a warm ink stroke and a hand-drawn weight, neither of which a stock
 * Material icon set provides. Every icon is drawn against a 24x24 virtual grid, scaled to
 * whatever size the caller gives it.
 */
@Composable
fun AppIcon(kind: IconKind, tint: Color, modifier: Modifier = Modifier, strokeWidthPx: Float = 2.1f) {
    Canvas(modifier = modifier) {
        val s = size.width / 24f
        val stroke = Stroke(width = strokeWidthPx * s, cap = StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round)
        fun p(x: Float, y: Float) = Offset(x * s, y * s)
        when (kind) {
            IconKind.Settings -> drawSettings(tint, stroke, s, ::p)
            IconKind.Music -> drawMusic(tint, stroke, ::p)
            IconKind.Roll -> drawRoll(tint, stroke, ::p)
            IconKind.Sheet -> drawSheet(tint, stroke, ::p)
            IconKind.Link -> drawLink(tint, stroke, ::p)
            IconKind.Keys -> drawKeys(tint, ::p)
            IconKind.Reset -> drawReset(tint, stroke, s, ::p)
            IconKind.Play -> drawPlay(tint, ::p)
            IconKind.Pause -> drawPause(tint, stroke, ::p)
            IconKind.Material -> drawMaterial(tint, stroke, ::p)
        }
    }
}

private fun DrawScope.drawSettings(tint: Color, stroke: Stroke, s: Float, p: (Float, Float) -> Offset) {
    val center = p(12f, 12f)
    drawCircle(tint, radius = 7.1f * s, center = center, style = stroke)
    drawCircle(tint, radius = 2.7f * s, center = center, style = stroke)
    for (k in 0 until 6) {
        val a = k * Math.PI.toFloat() / 3f
        val from = Offset(center.x + cos(a) * 9.2f * s, center.y + sin(a) * 9.2f * s)
        val to = Offset(center.x + cos(a) * 11.4f * s, center.y + sin(a) * 11.4f * s)
        drawLine(tint, from, to, strokeWidth = stroke.width, cap = StrokeCap.Round)
    }
}

private fun DrawScope.drawMusic(tint: Color, stroke: Stroke, p: (Float, Float) -> Offset) {
    drawCircle(tint, radius = 3.1f * (size.width / 24f), center = p(7f, 17.3f), style = stroke)
    drawCircle(tint, radius = 3.1f * (size.width / 24f), center = p(17.4f, 15f), style = stroke)
    drawLine(tint, p(10.1f, 17.3f), p(10.1f, 5.2f), strokeWidth = stroke.width, cap = StrokeCap.Round)
    drawLine(tint, p(20.5f, 15f), p(20.5f, 2.8f), strokeWidth = stroke.width, cap = StrokeCap.Round)
    drawLine(tint, p(10.1f, 5.2f), p(20.5f, 2.8f), strokeWidth = stroke.width, cap = StrokeCap.Round)
    drawLine(tint, p(10.1f, 8.7f), p(20.5f, 6.3f), strokeWidth = stroke.width, cap = StrokeCap.Round)
}

private fun DrawScope.drawRoll(tint: Color, stroke: Stroke, p: (Float, Float) -> Offset) {
    val topLeft = p(2.4f, 4.6f)
    drawRoundRect(
        tint,
        topLeft = topLeft,
        size = androidx.compose.ui.geometry.Size((19.2f) * (size.width / 24f), (14.8f) * (size.width / 24f)),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(2.2f * (size.width / 24f)),
        style = stroke,
    )
    val dotR = 1f * (size.width / 24f)
    listOf(5.8f, 12f, 18.2f).forEach { x ->
        drawCircle(tint, radius = dotR, center = p(x, 7.6f))
        drawCircle(tint, radius = dotR, center = p(x, 16.4f))
    }
    // two "pegs" mid-strip
    val s = size.width / 24f
    drawRoundRect(tint, topLeft = p(5.4f, 10.6f), size = androidx.compose.ui.geometry.Size(4.6f * s, 2.8f * s), cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.1f * s))
    drawRoundRect(tint, topLeft = p(13.4f, 10.6f), size = androidx.compose.ui.geometry.Size(5.2f * s, 2.8f * s), cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.1f * s))
}

private fun DrawScope.drawSheet(tint: Color, stroke: Stroke, p: (Float, Float) -> Offset) {
    val faint = tint.copy(alpha = 0.42f)
    val faintStroke = Stroke(width = stroke.width * 0.85f, cap = StrokeCap.Round)
    drawLine(faint, p(3.4f, 6.4f), p(14.4f, 6.4f), strokeWidth = faintStroke.width, cap = StrokeCap.Round)
    drawLine(faint, p(3.4f, 11.2f), p(14.4f, 11.2f), strokeWidth = faintStroke.width, cap = StrokeCap.Round)
    drawLine(faint, p(3.4f, 16f), p(9.8f, 16f), strokeWidth = faintStroke.width, cap = StrokeCap.Round)
    val pencil = androidx.compose.ui.graphics.Path().apply {
        moveTo(p(18.4f, 5.6f).x, p(18.4f, 5.6f).y)
        lineTo(p(21.4f, 8.6f).x, p(21.4f, 8.6f).y)
        lineTo(p(13.6f, 16.4f).x, p(13.6f, 16.4f).y)
        lineTo(p(9.6f, 17.4f).x, p(9.6f, 17.4f).y)
        lineTo(p(10.6f, 13.4f).x, p(10.6f, 13.4f).y)
        close()
    }
    drawPath(pencil, tint, style = stroke)
}

private fun DrawScope.drawLink(tint: Color, stroke: Stroke, p: (Float, Float) -> Offset) {
    drawLine(tint, p(9.3f, 14.7f), p(14.7f, 9.3f), strokeWidth = stroke.width, cap = StrokeCap.Round)
    val hook1 = androidx.compose.ui.graphics.Path().apply {
        moveTo(p(12.4f, 6f).x, p(12.4f, 6f).y)
        lineTo(p(14.3f, 4.2f).x, p(14.3f, 4.2f).y)
        cubicTo(
            p(16.6f, 2f).x, p(16.6f, 2f).y,
            p(20f, 5.6f).x, p(20f, 5.6f).y,
            p(19.6f, 8f).x, p(19.6f, 8f).y,
        )
        lineTo(p(17.8f, 9.9f).x, p(17.8f, 9.9f).y)
    }
    drawPath(hook1, tint, style = stroke)
    val hook2 = androidx.compose.ui.graphics.Path().apply {
        moveTo(p(11.6f, 18f).x, p(11.6f, 18f).y)
        lineTo(p(9.7f, 19.8f).x, p(9.7f, 19.8f).y)
        cubicTo(
            p(7.4f, 22f).x, p(7.4f, 22f).y,
            p(4f, 18.4f).x, p(4f, 18.4f).y,
            p(4.4f, 16f).x, p(4.4f, 16f).y,
        )
        lineTo(p(6.2f, 14.1f).x, p(6.2f, 14.1f).y)
    }
    drawPath(hook2, tint, style = stroke)
}

private fun DrawScope.drawKeys(tint: Color, p: (Float, Float) -> Offset) {
    val s = size.width / 24f
    val w = 3.4f * s
    listOf(2.6f, 7.4f, 12.2f, 17f).forEach { x ->
        drawRoundRect(tint, topLeft = p(x, 4.4f), size = androidx.compose.ui.geometry.Size(w, 15.2f * s), cornerRadius = androidx.compose.ui.geometry.CornerRadius(1.5f * s))
    }
}

private fun DrawScope.drawReset(tint: Color, stroke: Stroke, s: Float, p: (Float, Float) -> Offset) {
    val center = p(12f, 12.5f)
    drawArc(
        tint,
        startAngle = -40f,
        sweepAngle = 260f,
        useCenter = false,
        topLeft = Offset(center.x - 8.2f * s, center.y - 8.2f * s),
        size = androidx.compose.ui.geometry.Size(16.4f * s, 16.4f * s),
        style = stroke,
    )
    // arrowhead at the end of the sweep (roughly upper-left)
    val tip = p(4.6f, 6.2f)
    val arrow = androidx.compose.ui.graphics.Path().apply {
        moveTo(tip.x, tip.y)
        lineTo(p(3.6f, 11f).x, p(3.6f, 11f).y)
        moveTo(tip.x, tip.y)
        lineTo(p(9.2f, 9.4f).x, p(9.2f, 9.4f).y)
    }
    drawPath(arrow, tint, style = stroke)
}

private fun DrawScope.drawPlay(tint: Color, p: (Float, Float) -> Offset) {
    val path = androidx.compose.ui.graphics.Path().apply {
        moveTo(p(8f, 4.6f).x, p(8f, 4.6f).y)
        lineTo(p(19.4f, 12f).x, p(19.4f, 12f).y)
        lineTo(p(8f, 19.4f).x, p(8f, 19.4f).y)
        close()
    }
    drawPath(path, tint)
}

private fun DrawScope.drawPause(tint: Color, stroke: Stroke, p: (Float, Float) -> Offset) {
    drawLine(tint, p(8.4f, 4.4f), p(8.4f, 19.6f), strokeWidth = stroke.width * 1.6f, cap = StrokeCap.Round)
    drawLine(tint, p(15.6f, 4.4f), p(15.6f, 19.6f), strokeWidth = stroke.width * 1.6f, cap = StrokeCap.Round)
}

private fun DrawScope.drawMaterial(tint: Color, stroke: Stroke, p: (Float, Float) -> Offset) {
    val tops = listOf(5.6f, 5f, 5.6f, 5f)
    val xs = listOf(3.2f, 6.8f, 10.4f, 14f)
    xs.forEachIndexed { i, x -> drawLine(tint, p(x, tops[i]), p(x, 19.2f), strokeWidth = stroke.width, cap = StrokeCap.Round) }
    // mallet
    drawLine(tint, p(17.4f, 18.6f), p(20f, 9.8f), strokeWidth = stroke.width, cap = StrokeCap.Round)
    drawCircle(tint, radius = 2.4f * (size.width / 24f), center = p(20.6f, 7.2f))
}

/** A small "unread view" dot, drawn over an icon's top-right corner. */
@Composable
fun BadgeDot(tint: Color, modifier: Modifier = Modifier) {
    Canvas(modifier = modifier) {
        drawCircle(tint, radius = size.minDimension / 2f)
    }
}
