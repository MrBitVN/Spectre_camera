package com.spectreflow.app.blender

import android.graphics.Bitmap
import android.graphics.ColorSpace
import android.graphics.Rect
import android.os.Build
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * High-performance Long Exposure Accumulation & Blending Engine.
 * Accumulates frames progressively in 64-bit sum buffers (O(1) memory overhead),
 * executes Motion Blur (exact mathematical running mean), Light Trails (per-pixel maximum),
 * and Vanish (temporal median), outputs in Display P3 wide color gamut,
 * and crops aligned border edges.
 */
class FrameBlenderEngine {
    private var meanBlender: MeanBlender? = null
    private var lightTrailsBlender: LightTrailsBlender? = null
    private var medianBlender: MedianBlender? = null

    private var currentMode: LongExposureMode = LongExposureMode.MOTION_BLUR
    private var targetWidth: Int = 0
    private var targetHeight: Int = 0

    val isRunning: Boolean
        get() = meanBlender != null || lightTrailsBlender != null || medianBlender != null

    suspend fun start(mode: LongExposureMode, width: Int, height: Int) = withContext(Dispatchers.Default) {
        currentMode = mode
        targetWidth = width
        targetHeight = height

        when (mode) {
            LongExposureMode.AUTO,
            LongExposureMode.MOTION_BLUR -> {
                meanBlender = MeanBlender(width, height)
                lightTrailsBlender = null
                medianBlender = null
            }
            LongExposureMode.LIGHT_TRAILS -> {
                lightTrailsBlender = LightTrailsBlender(width, height)
                meanBlender = null
                medianBlender = null
            }
            LongExposureMode.VANISH -> {
                medianBlender = MedianBlender(width, height)
                meanBlender = null
                lightTrailsBlender = null
            }
        }
    }

    suspend fun addFrame(bitmap: Bitmap): Int = withContext(Dispatchers.Default) {
        val width = bitmap.width
        val height = bitmap.height
        val pixels = IntArray(width * height)
        bitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        when (currentMode) {
            LongExposureMode.AUTO,
            LongExposureMode.MOTION_BLUR -> {
                meanBlender?.addFrame(pixels)
                meanBlender?.frameCount ?: 0
            }
            LongExposureMode.LIGHT_TRAILS -> {
                lightTrailsBlender?.addFrame(pixels)
                lightTrailsBlender?.frameCount ?: 0
            }
            LongExposureMode.VANISH -> {
                medianBlender?.addFrame(pixels)
                medianBlender?.totalObservedFrames ?: 0
            }
        }
    }

    suspend fun finalizeResult(cropBounds: Rect? = null): Bitmap = withContext(Dispatchers.Default) {
        val outputPixels = when (currentMode) {
            LongExposureMode.AUTO,
            LongExposureMode.MOTION_BLUR -> meanBlender?.finalizeOutput() ?: IntArray(0)
            LongExposureMode.LIGHT_TRAILS -> lightTrailsBlender?.finalizeOutput() ?: IntArray(0)
            LongExposureMode.VANISH -> medianBlender?.finalizeOutput() ?: IntArray(0)
        }

        if (outputPixels.isEmpty() || targetWidth <= 0 || targetHeight <= 0) {
            return@withContext Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
        }

        val rawBitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val p3 = ColorSpace.get(ColorSpace.Named.DISPLAY_P3)
            Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888, true, p3).apply {
                setPixels(outputPixels, 0, targetWidth, 0, 0, targetWidth, targetHeight)
            }
        } else {
            Bitmap.createBitmap(outputPixels, targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
        }

        // Crop borders if alignment caused offset shifts
        if (cropBounds != null && cropBounds.width() > 10 && cropBounds.height() > 10) {
            val safeLeft = cropBounds.left.coerceIn(0, targetWidth - 1)
            val safeTop = cropBounds.top.coerceIn(0, targetHeight - 1)
            val safeWidth = cropBounds.width().coerceAtMost(targetWidth - safeLeft)
            val safeHeight = cropBounds.height().coerceAtMost(targetHeight - safeTop)

            try {
                return@withContext Bitmap.createBitmap(rawBitmap, safeLeft, safeTop, safeWidth, safeHeight)
            } catch (e: Exception) {
                return@withContext rawBitmap
            }
        }

        rawBitmap
    }

    fun reset() {
        meanBlender?.reset()
        lightTrailsBlender?.reset()
        medianBlender?.reset()
        meanBlender = null
        lightTrailsBlender = null
        medianBlender = null
    }
}
