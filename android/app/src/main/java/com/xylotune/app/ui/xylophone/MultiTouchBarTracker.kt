package com.xylotune.app.ui.xylophone

// Compose-free (no Compose/Android imports at all) so it can be unit-tested on the JVM
// without a device/emulator — see the plan's Risk Area 1. Owns exactly the two things
// true two-thumb multitouch needs: which rect each bar currently occupies in its parent's
// coordinate space, and which bar (if any) each currently-down pointer landed on.
//
// Callers drive it from a single shared pointer-event loop on the bars' parent container
// (never per-bar pointerInput — see the plan for why that fails under multitouch):
// onPress() on each pointer's edge-triggered down, onRelease() on its edge-triggered up.
// Move events are intentionally never fed in here — there is no drag-to-glissando in the
// source app, so a finger sliding off a bar without lifting must not re-trigger a strike.
class MultiTouchBarTracker(private val barCount: Int) {
    data class Rect(val left: Float, val top: Float, val right: Float, val bottom: Float) {
        fun contains(x: Float, y: Float) = x >= left && x < right && y >= top && y < bottom
    }

    private val rects = arrayOfNulls<Rect>(barCount)
    private val pointerToBar = HashMap<Long, Int>()

    fun setBarRect(index: Int, rect: Rect) {
        rects[index] = rect
    }

    /** Returns the bar index to strike, or null if this press didn't land on any bar. */
    fun onPress(pointerId: Long, x: Float, y: Float): Int? {
        val bar = hitTest(x, y) ?: return null
        pointerToBar[pointerId] = bar
        return bar
    }

    fun onRelease(pointerId: Long) {
        pointerToBar.remove(pointerId)
    }

    private fun hitTest(x: Float, y: Float): Int? {
        for (i in 0 until barCount) {
            if (rects[i]?.contains(x, y) == true) return i
        }
        return null
    }
}
