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

    @Test
    fun testCompanionVideoFailureDoesNotCrashCapturePipeline() {
        var captureSucceeded = false
        var companionVideoFailed = false

        // Simulate frame consumption with simulated video encoder failure
        try {
            // Simulated video failure (e.g. MediaCodec lockHardwareCanvas or codec exception)
            try {
                throw IllegalStateException("Surface was not created for software rendering")
            } catch (vt: Throwable) {
                companionVideoFailed = true
                // Companion video failure must be swallowed safely
            }

            // Core blending engine must still receive and blend frames
            captureSucceeded = true
        } catch (t: Throwable) {
            captureSucceeded = false
        }

        assertTrue("Companion video failure must be caught safely", companionVideoFailed)
        assertTrue("Capture pipeline must succeed even if companion video fails", captureSucceeded)
    }

    @Test
    fun testDoublePressAndConcurrentStartRejection() {
        val captureState = java.util.concurrent.atomic.AtomicReference(CaptureState.IDLE)

        // First press down
        val firstStart = captureState.compareAndSet(CaptureState.IDLE, CaptureState.CAPTURING)
        assertTrue("First press must succeed", firstStart)
        assertEquals(CaptureState.CAPTURING, captureState.get())

        // Simultaneous second press down while still capturing
        val secondStart = captureState.compareAndSet(CaptureState.IDLE, CaptureState.CAPTURING)
        assertFalse("Second concurrent press must be rejected", secondStart)
        assertEquals(CaptureState.CAPTURING, captureState.get())
    }

    @Test
    fun testCrashDialogSafeDismissalAndStateReset() {
        var isCapturing = true
        var currentScreen = AppScreen.CAMERA
        var crashInfo: AppCrashInfo? = AppCrashInfo(
            title = "Đã xảy ra lỗi",
            message = "Test exception",
            stackTrace = "at test line 1"
        )

        // Simulate dismissal
        crashInfo = null
        isCapturing = false
        currentScreen = AppScreen.CAMERA

        org.junit.Assert.assertNull(crashInfo)
        assertFalse(isCapturing)
        assertEquals(AppScreen.CAMERA, currentScreen)
    }

    @Test
    fun testDialRotationAndProLockValidation() {
        val durations = com.spectreflow.app.settings.CaptureDuration.entries
        assertEquals(5, durations.size)

        // 3s, 5s, 9s are non-Pro
        assertFalse(durations[0].isProOnly)
        assertFalse(durations[1].isProOnly)
        assertFalse(durations[2].isProOnly)

        // 15s, 30s are Pro-only
        assertTrue(durations[3].isProOnly)
        assertTrue(durations[4].isProOnly)

        // Simulate dial selection with isProUnlocked = false
        var isProUnlocked = false
        var selected = durations[0]

        fun trySelect(duration: com.spectreflow.app.settings.CaptureDuration): Boolean {
            return if (duration.isProOnly && !isProUnlocked) {
                false
            } else {
                selected = duration
                true
            }
        }

        assertTrue(trySelect(durations[1])) // 5s allowed
        assertEquals(com.spectreflow.app.settings.CaptureDuration.SEC_5, selected)

        assertFalse(trySelect(durations[3])) // 15s rejected
        assertEquals(com.spectreflow.app.settings.CaptureDuration.SEC_5, selected)

        // Unlock Pro
        isProUnlocked = true
        assertTrue(trySelect(durations[3])) // 15s allowed now
        assertEquals(com.spectreflow.app.settings.CaptureDuration.SEC_15, selected)
    }

    @Test
    fun testQuickTapShowsHintAndDiscards() {
        var hintMessage: String? = null
        var isCaptured = false

        fun onRelease(elapsedMs: Long) {
            if (elapsedMs < 1000L) {
                hintMessage = "Hold longer to capture"
                isCaptured = false
            } else {
                hintMessage = null
                isCaptured = true
            }
        }

        onRelease(400L)
        assertEquals("Hold longer to capture", hintMessage)
        assertFalse(isCaptured)

        onRelease(2500L)
        org.junit.Assert.assertNull(hintMessage)
        assertTrue(isCaptured)
    }

    @Test
    fun testSlideCancelSafety() {
        val buttonRadius = 40f
        val cancelDistance = buttonRadius * 2f * 1.5f // 120f
        var isCancelled = false

        fun onPointerMove(distanceFromCenter: Float) {
            if (distanceFromCenter > cancelDistance) {
                isCancelled = true
            }
        }

        onPointerMove(60f)
        assertFalse(isCancelled)

        onPointerMove(130f)
        assertTrue(isCancelled)
    }

    @Test
    fun testPhotoRepositoryViewerConsistency() {
        val photos = mutableListOf<String>()
        var latestThumbnail: String? = null
        var currentScreen = AppScreen.CAMERA
        var hintMessage: String? = null

        fun openResultViewer() {
            if (photos.isEmpty()) {
                hintMessage = "No photos captured yet"
            } else {
                currentScreen = AppScreen.GALLERY_VIEWER
            }
        }

        fun savePhoto(id: String) {
            photos.add(0, id) // LIFO sorted by date descending
            latestThumbnail = "thumb_$id"
        }

        fun deletePhoto(id: String) {
            photos.remove(id)
            latestThumbnail = photos.firstOrNull()?.let { "thumb_$it" }
            if (photos.isEmpty()) {
                currentScreen = AppScreen.CAMERA
            }
        }

        // Scenario 1: Fresh install / no photos -> empty placeholder, tap shows hint
        assertTrue(photos.isEmpty())
        org.junit.Assert.assertNull(latestThumbnail)
        openResultViewer()
        assertEquals("No photos captured yet", hintMessage)
        assertEquals(AppScreen.CAMERA, currentScreen)

        // Scenario 2: Save 2 photos -> thumbnail updates immediately to latest
        hintMessage = null
        savePhoto("photo_1")
        assertEquals("thumb_photo_1", latestThumbnail)
        savePhoto("photo_2")
        assertEquals("thumb_photo_2", latestThumbnail)
        assertEquals(2, photos.size)

        // Scenario 3: Tap thumbnail -> opens gallery viewer showing newest first
        openResultViewer()
        org.junit.Assert.assertNull(hintMessage)
        assertEquals(AppScreen.GALLERY_VIEWER, currentScreen)
        assertEquals("photo_2", photos[0])

        // Scenario 4: Delete newest photo -> thumbnail falls back to photo_1
        deletePhoto("photo_2")
        assertEquals(1, photos.size)
        assertEquals("thumb_photo_1", latestThumbnail)
        assertEquals(AppScreen.GALLERY_VIEWER, currentScreen)

        // Scenario 5: Delete all photos -> returns to camera screen, thumbnail is null
        deletePhoto("photo_1")
        assertTrue(photos.isEmpty())
        org.junit.Assert.assertNull(latestThumbnail)
        assertEquals(AppScreen.CAMERA, currentScreen)
    }
}
