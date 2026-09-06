package com.xylotune.app.ui.crayon

import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import kotlin.math.sin
import kotlin.random.Random

/**
 * A rounded-rectangle outline with a hand-drawn wobble — real vector geometry, not a
 * texture, so it stays crisp at any density. The wobble is generated once per [seed]:
 * [createOutline] is a pure function of `size` and `seed`, so the same bar always wobbles
 * the same way and recomposition never makes it shimmer (a genuinely per-frame-random
 * jitter would read as static noise, not a crayon stroke — this is the implementation the
 * paper-roll redesign's mockup approximated with an SVG feTurbulence filter, which has no
 * Android equivalent).
 *
 * Give every use of this shape a distinct, stable [seed] (e.g. a bar's note index) — reusing
 * one seed across differently-sized shapes is fine (only the size differs, per the
 * paragraph above), but reusing it across shapes meant to look independently hand-drawn
 * (e.g. two different bars) will visibly wobble them in lockstep.
 */
class RoughShape(
    private val cornerRadiusPx: Float,
    private val seed: Int,
    private val amplitudePx: Float = 3f,
    private val segments: Int = 5,
) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        val path = Path()
        val r = cornerRadiusPx.coerceAtMost(minOf(size.width, size.height) / 2f)
        val rnd = Random(seed)

        // A smooth, low-frequency wobble (sine) plus a touch of per-point randomness, so
        // neighbouring points move together rather than jumping independently — the
        // difference between a wavering hand-drawn line and visual noise.
        fun jitter(t: Float): Float {
            val phase = rnd.nextFloat() * 6.2832f
            return (sin(t * 3.1f + phase) * 0.6f + (rnd.nextFloat() - 0.5f) * 0.4f) * amplitudePx
        }

        fun edge(from: Offset, to: Offset, normal: Offset): List<Offset> =
            (0..segments).map { i ->
                val t = i / segments.toFloat()
                val base = Offset(from.x + (to.x - from.x) * t, from.y + (to.y - from.y) * t)
                val j = jitter(t)
                Offset(base.x + normal.x * j, base.y + normal.y * j)
            }

        fun addSmooth(pts: List<Offset>) {
            for (i in 1 until pts.size) {
                val prev = pts[i - 1]
                val cur = pts[i]
                val mid = Offset((prev.x + cur.x) / 2f, (prev.y + cur.y) / 2f)
                path.quadraticTo(prev.x, prev.y, mid.x, mid.y)
            }
            path.lineTo(pts.last().x, pts.last().y)
        }

        val topPts = edge(Offset(r, 0f), Offset(size.width - r, 0f), Offset(0f, -1f))
        val rightPts = edge(Offset(size.width, r), Offset(size.width, size.height - r), Offset(1f, 0f))
        val bottomPts = edge(Offset(size.width - r, size.height), Offset(r, size.height), Offset(0f, 1f))
        val leftPts = edge(Offset(0f, size.height - r), Offset(0f, r), Offset(-1f, 0f))

        path.moveTo(topPts.first().x, topPts.first().y)
        addSmooth(topPts)
        path.quadraticTo(size.width, 0f, size.width, r)
        addSmooth(rightPts)
        path.quadraticTo(size.width, size.height, size.width - r, size.height)
        addSmooth(bottomPts)
        path.quadraticTo(0f, size.height, 0f, size.height - r)
        addSmooth(leftPts)
        path.quadraticTo(0f, 0f, r, 0f)
        path.close()

        return Outline.Generic(path)
    }
}
