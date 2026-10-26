package com.spectreflow.app.alignment

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Alignment result holding the warped aligned Bitmap, translation delta,
 * and updated valid intersection crop bounds.
 */
data class AlignmentResult(
    val alignedBitmap: Bitmap,
    val deltaX: Float,
    val deltaY: Float,
    val cropRect: Rect
)

/**
 * Handheld Tremor Auto-Alignment Engine.
 * Aligns burst frames to reference frame (frame 0) using 2D spatial cross-correlation
 * on downsampled luminance representations, then warps and tracks the border crop rectangle.
 */
class FrameAligner(
    val baseWidth: Int,
    val baseHeight: Int,
    private val maxSearchRadius: Int = 18
) {
    private var referenceLuminance: ByteArray? = null
    private val dsWidth = 160
    private val dsHeight = 120
    private val scaleX = baseWidth.toFloat() / dsWidth
    private val scaleY = baseHeight.toFloat() / dsHeight

    var cropBounds = Rect(0, 0, baseWidth, baseHeight)
        private set

    private var frameIndex = 0

    fun alignFrame(frame: Bitmap, isStableTripod: Boolean): AlignmentResult {
        if (frameIndex == 0 || isStableTripod) {
            if (frameIndex == 0) {
                referenceLuminance = extractDownscaledLuminance(frame)
            }
            frameIndex++
            return AlignmentResult(
                alignedBitmap = frame,
                deltaX = 0f,
                deltaY = 0f,
                cropRect = cropBounds
            )
        }

        val refLum = referenceLuminance
        if (refLum == null) {
            referenceLuminance = extractDownscaledLuminance(frame)
            frameIndex++
            return AlignmentResult(frame, 0f, 0f, cropBounds)
        }

        val currentLum = extractDownscaledLuminance(frame)
        val (shiftX, shiftY) = ShiftEstimator.computeTranslationalShift(
            refLum,
            currentLum,
            dsWidth,
            dsHeight,
            maxSearchRadius
        )

        val fullDeltaX = shiftX * scaleX
        val fullDeltaY = shiftY * scaleY

        val warpedBitmap = Bitmap.createBitmap(frame.width, frame.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(warpedBitmap)
        val matrix = Matrix().apply {
            postTranslate(fullDeltaX, fullDeltaY)
        }
        val paint = Paint(Paint.FILTER_BITMAP_FLAG)
        canvas.drawBitmap(frame, matrix, paint)

        val left = max(cropBounds.left, if (fullDeltaX > 0) fullDeltaX.toInt() else 0)
        val top = max(cropBounds.top, if (fullDeltaY > 0) fullDeltaY.toInt() else 0)
        val right = min(cropBounds.right, if (fullDeltaX < 0) (baseWidth + fullDeltaX).toInt() else baseWidth)
        val bottom = min(cropBounds.bottom, if (fullDeltaY < 0) (baseHeight + fullDeltaY).toInt() else baseHeight)

        cropBounds = Rect(
            left.coerceIn(0, baseWidth / 4),
            top.coerceIn(0, baseHeight / 4),
            right.coerceIn(baseWidth * 3 / 4, baseWidth),
            bottom.coerceIn(baseHeight * 3 / 4, baseHeight)
        )

        frameIndex++
        return AlignmentResult(
            alignedBitmap = warpedBitmap,
            deltaX = fullDeltaX,
            deltaY = fullDeltaY,
            cropRect = cropBounds
        )
    }

    private fun extractDownscaledLuminance(bitmap: Bitmap): ByteArray {
        val scaled = Bitmap.createScaledBitmap(bitmap, dsWidth, dsHeight, true)
        val pixels = IntArray(dsWidth * dsHeight)
        scaled.getPixels(pixels, 0, dsWidth, 0, 0, dsWidth, dsHeight)

        val lum = ByteArray(pixels.size)
        for (i in pixels.indices) {
            val color = pixels[i]
            val r = (color shr 16) and 0xFF
            val g = (color shr 8) and 0xFF
            val b = color and 0xFF
            lum[i] = ((0.299 * r) + (0.587 * g) + (0.114 * b)).toInt().toByte()
        }
        return lum
    }

    fun reset() {
        referenceLuminance = null
        frameIndex = 0
        cropBounds = Rect(0, 0, baseWidth, baseHeight)
    }
}
