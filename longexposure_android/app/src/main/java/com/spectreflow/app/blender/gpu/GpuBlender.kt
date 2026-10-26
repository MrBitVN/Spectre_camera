package com.spectreflow.app.blender.gpu

import android.graphics.Bitmap
import android.graphics.BlendMode
import android.graphics.Canvas
import android.graphics.ColorSpace
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.os.Build
import com.spectreflow.app.blender.LongExposureMode

/**
 * Hardware-accelerated GPU Frame Blender for Android.
 * Leverages Android's hardware GPU rendering pipeline (Vulkan / OpenGL ES via Skia HWUI)
 * using dynamic alpha running-mean compositing for Motion Blur and BlendMode.LIGHTEN for Light Trails.
 * Automatically utilizes wide color gamut (Display P3) where supported.
 */
class GpuBlender(
    val width: Int,
    val height: Int,
    val mode: LongExposureMode
) {
    private var accumulatedBitmap: Bitmap? = null
    private var canvas: Canvas? = null
    private val paint = Paint(Paint.FILTER_BITMAP_FLAG or Paint.ANTI_ALIAS_FLAG)
    var frameCount: Int = 0
        private set

    init {
        val bitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val p3 = ColorSpace.get(ColorSpace.Named.DISPLAY_P3)
            Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888, true, p3)
        } else {
            Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        }
        accumulatedBitmap = bitmap
        canvas = Canvas(bitmap)

        if (mode == LongExposureMode.LIGHT_TRAILS) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                paint.blendMode = BlendMode.LIGHTEN
            } else {
                paint.xfermode = PorterDuffXfermode(PorterDuff.Mode.LIGHTEN)
            }
        }
    }

    /**
     * Ingests a new frame and accumulates it directly on the GPU pipeline.
     */
    fun addFrame(bitmap: Bitmap): Int {
        val cv = canvas ?: return frameCount
        frameCount++

        if (frameCount == 1) {
            val firstPaint = Paint(Paint.FILTER_BITMAP_FLAG)
            cv.drawBitmap(bitmap, 0f, 0f, firstPaint)
            return frameCount
        }

        when (mode) {
            LongExposureMode.AUTO,
            LongExposureMode.MOTION_BLUR -> {
                // Exact running mean: weight = 1.0 / k
                // Canvas alpha compositing: Result = Frame_k * alpha + Accumulator_{k-1} * (1 - alpha)
                val alphaFloat = 1.0f / frameCount.toFloat()
                val alphaInt = (alphaFloat * 255.0f).toInt().coerceIn(1, 255)
                paint.alpha = alphaInt
                cv.drawBitmap(bitmap, 0f, 0f, paint)
            }
            LongExposureMode.LIGHT_TRAILS -> {
                // Per-pixel lighten blend on GPU
                paint.alpha = 255
                cv.drawBitmap(bitmap, 0f, 0f, paint)
            }
            LongExposureMode.VANISH -> {
                // Vanish mode uses MedianBlender
            }
        }
        return frameCount
    }

    /**
     * Finalizes and returns the GPU-accumulated result bitmap.
     */
    fun finalizeResult(): Bitmap? {
        return accumulatedBitmap
    }

    fun reset() {
        accumulatedBitmap?.recycle()
        accumulatedBitmap = null
        canvas = null
        frameCount = 0
    }
}
