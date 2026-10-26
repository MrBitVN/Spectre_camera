package com.spectreflow.app.blender

import android.graphics.Bitmap
import android.graphics.Rect
import com.spectreflow.app.blender.gpu.GpuBlender
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * High-level orchestration engine for long exposure accumulation and blending.
 * Dispatches frame operations on background workers, leverages GPU hardware rendering (GpuBlender)
 * for Motion Blur and Light Trails with Display P3 wide gamut, uses MedianBlender for Vanish,
 * and applies final intersection cropping.
 */
class FrameBlenderEngine(
    private val useGpuAcceleration: Boolean = true
) {
    private var gpuBlender: GpuBlender? = null
    private var meanBlender: MeanBlender? = null
    private var lightTrailsBlender: LightTrailsBlender? = null
    private var medianBlender: MedianBlender? = null

    private var currentMode: LongExposureMode = LongExposureMode.MOTION_BLUR
    private var targetWidth: Int = 0
    private var targetHeight: Int = 0

    val isRunning: Boolean
        get() = gpuBlender != null || meanBlender != null || lightTrailsBlender != null || medianBlender != null

    suspend fun start(mode: LongExposureMode, width: Int, height: Int) = withContext(Dispatchers.Default) {
        currentMode = mode
        targetWidth = width
        targetHeight = height

        if (useGpuAcceleration && mode != LongExposureMode.VANISH) {
            try {
                gpuBlender = GpuBlender(width, height, mode)
                meanBlender = null
                lightTrailsBlender = null
                medianBlender = null
                return@withContext
            } catch (e: Exception) {
                gpuBlender = null
            }
        }

        // Fallback or Vanish mode
        when (mode) {
            LongExposureMode.AUTO,
            LongExposureMode.MOTION_BLUR -> {
                meanBlender = MeanBlender(width, height)
                lightTrailsBlender = null
                medianBlender = null
                gpuBlender = null
            }
            LongExposureMode.LIGHT_TRAILS -> {
                lightTrailsBlender = LightTrailsBlender(width, height)
                meanBlender = null
                medianBlender = null
                gpuBlender = null
            }
            LongExposureMode.VANISH -> {
                medianBlender = MedianBlender(width, height)
                meanBlender = null
                lightTrailsBlender = null
                gpuBlender = null
            }
        }
    }

    suspend fun addFrame(bitmap: Bitmap): Int = withContext(Dispatchers.Default) {
        val gpu = gpuBlender
        if (gpu != null) {
            return@withContext gpu.addFrame(bitmap)
        }

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
        val gpu = gpuBlender
        val rawBitmap = if (gpu != null) {
            gpu.finalizeResult() ?: Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
        } else {
            val outputPixels = when (currentMode) {
                LongExposureMode.AUTO,
                LongExposureMode.MOTION_BLUR -> meanBlender?.finalizeOutput() ?: IntArray(0)
                LongExposureMode.LIGHT_TRAILS -> lightTrailsBlender?.finalizeOutput() ?: IntArray(0)
                LongExposureMode.VANISH -> medianBlender?.finalizeOutput() ?: IntArray(0)
            }

            if (outputPixels.isEmpty() || targetWidth <= 0 || targetHeight <= 0) {
                return@withContext Bitmap.createBitmap(1, 1, Bitmap.Config.ARGB_8888)
            }

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                val p3 = android.graphics.ColorSpace.get(android.graphics.ColorSpace.Named.DISPLAY_P3)
                Bitmap.createBitmap(targetWidth, targetHeight, Bitmap.Config.ARGB_8888, true, p3).apply {
                    setPixels(outputPixels, 0, targetWidth, 0, 0, targetWidth, targetHeight)
                }
            } else {
                Bitmap.createBitmap(outputPixels, targetWidth, targetHeight, Bitmap.Config.ARGB_8888)
            }
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
        gpuBlender?.reset()
        gpuBlender = null
        meanBlender?.reset()
        lightTrailsBlender?.reset()
        medianBlender?.reset()
        meanBlender = null
        lightTrailsBlender = null
        medianBlender = null
    }
}
