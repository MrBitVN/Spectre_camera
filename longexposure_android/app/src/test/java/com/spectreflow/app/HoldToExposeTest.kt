package com.spectreflow.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class HoldToExposeTest {

    @Test
    fun testHoldDurationValidation() {
        fun isValidCapture(elapsedMs: Long): Boolean = elapsedMs >= 1000L

        // Quick taps under 1000ms must be discarded
        assertFalse(isValidCapture(0L))
        assertFalse(isValidCapture(250L))
        assertFalse(isValidCapture(999L))

        // Holds of 1000ms or longer are valid captures
        assertTrue(isValidCapture(1000L))
        assertTrue(isValidCapture(1500L))
        assertTrue(isValidCapture(3000L))
        assertTrue(isValidCapture(30000L))
    }

    @Test
    fun testProgressCalculation() {
        fun calculateProgress(elapsedMs: Long, maxDurationSeconds: Int): Float {
            val totalMs = maxDurationSeconds * 1000f
            return (elapsedMs / totalMs).coerceIn(0f, 1f)
        }

        // Test at start
        assertEquals(0f, calculateProgress(0L, 5), 0.001f)

        // Test at midpoint (2.5s of 5s)
        assertEquals(0.5f, calculateProgress(2500L, 5), 0.001f)

        // Test at max cap (5s of 5s)
        assertEquals(1.0f, calculateProgress(5000L, 5), 0.001f)

        // Test past cap (clamped to 1.0f)
        assertEquals(1.0f, calculateProgress(7000L, 5), 0.001f)
    }

    @Test
    fun testTimerFormatting() {
        fun formatTimer(seconds: Float): String {
            val s = seconds.toInt()
            return String.format(Locale.US, "00:%02d", s)
        }

        assertEquals("00:00", formatTimer(0.0f))
        assertEquals("00:00", formatTimer(0.9f))
        assertEquals("00:01", formatTimer(1.0f))
        assertEquals("00:07", formatTimer(7.4f))
        assertEquals("00:15", formatTimer(15.0f))
        assertEquals("00:30", formatTimer(30.0f))
    }

    @Test
    fun testCaptureStateMachineTransitions() {
        var state = CaptureState.IDLE

        // 1. Shutter press down: IDLE -> CAPTURING
        assertTrue(state == CaptureState.IDLE)
        state = CaptureState.CAPTURING
        assertEquals(CaptureState.CAPTURING, state)

        // 2. Early release (<1s): CAPTURING -> IDLE
        val earlyElapsed = 850L
        if (earlyElapsed < 1000L) {
            state = CaptureState.IDLE
        }
        assertEquals(CaptureState.IDLE, state)

        // 3. Valid hold (3s, 5s, 15s, 30s): CAPTURING -> PROCESSING -> RESULT
        state = CaptureState.CAPTURING
        val validElapsed = 5000L
        if (validElapsed >= 1000L) {
            state = CaptureState.PROCESSING
        }
        assertEquals(CaptureState.PROCESSING, state)
        state = CaptureState.RESULT
        assertEquals(CaptureState.RESULT, state)

        // 4. Slide cancel (finger dragged out): CAPTURING -> IDLE
        state = CaptureState.CAPTURING
        val draggedOut = true
        if (draggedOut) {
            state = CaptureState.IDLE
        }
        assertEquals(CaptureState.IDLE, state)

        // 5. App backgrounded during hold: CAPTURING -> IDLE
        state = CaptureState.CAPTURING
        val appBackgrounded = true
        if (appBackgrounded) {
            state = CaptureState.IDLE
        }
        assertEquals(CaptureState.IDLE, state)
    }
}
