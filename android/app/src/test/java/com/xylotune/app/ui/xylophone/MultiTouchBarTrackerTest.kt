package com.xylotune.app.ui.xylophone

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MultiTouchBarTrackerTest {

    private fun trackerWithEightEqualBars(): MultiTouchBarTracker {
        val tracker = MultiTouchBarTracker(8)
        for (i in 0 until 8) {
            tracker.setBarRect(
                i,
                MultiTouchBarTracker.Rect(
                    left = i * 10f,
                    top = 0f,
                    right = i * 10f + 10f,
                    bottom = 100f,
                ),
            )
        }
        return tracker
    }

    @Test
    fun `press inside a bar's rect returns that bar`() {
        val tracker = trackerWithEightEqualBars()
        assertEquals(3, tracker.onPress(pointerId = 1L, x = 35f, y = 50f))
    }

    @Test
    fun `press outside every rect returns null`() {
        val tracker = trackerWithEightEqualBars()
        assertNull(tracker.onPress(pointerId = 1L, x = 999f, y = 50f))
        assertNull(tracker.onPress(pointerId = 1L, x = 5f, y = -10f))
    }

    @Test
    fun `two pointers pressing two different bars both resolve independently`() {
        val tracker = trackerWithEightEqualBars()
        val first = tracker.onPress(pointerId = 1L, x = 5f, y = 50f)
        val second = tracker.onPress(pointerId = 2L, x = 65f, y = 50f)
        assertEquals(0, first)
        assertEquals(6, second)
    }

    @Test
    fun `two pointers on the same bar both resolve to it`() {
        val tracker = trackerWithEightEqualBars()
        val first = tracker.onPress(pointerId = 1L, x = 22f, y = 10f)
        val second = tracker.onPress(pointerId = 2L, x = 28f, y = 90f)
        assertEquals(2, first)
        assertEquals(2, second)
    }

    @Test
    fun `release does not affect subsequent hit testing`() {
        val tracker = trackerWithEightEqualBars()
        tracker.onPress(pointerId = 1L, x = 5f, y = 50f)
        tracker.onRelease(pointerId = 1L)
        assertEquals(0, tracker.onPress(pointerId = 1L, x = 5f, y = 50f))
    }

    @Test
    fun `rect right and bottom edges are exclusive`() {
        val tracker = MultiTouchBarTracker(1)
        tracker.setBarRect(0, MultiTouchBarTracker.Rect(left = 0f, top = 0f, right = 10f, bottom = 10f))
        assertEquals(0, tracker.onPress(pointerId = 1L, x = 0f, y = 0f))
        assertNull(tracker.onPress(pointerId = 2L, x = 10f, y = 5f))
        assertNull(tracker.onPress(pointerId = 3L, x = 5f, y = 10f))
    }
}
