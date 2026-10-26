package com.spectreflow.app.ai

import android.graphics.Bitmap
import com.spectreflow.app.blender.LongExposureMode

data class FrameStats(
    val averageLuminance: Float, // 0.0 (pitch black) to 1.0 (pure white)
    val highlightRatio: Float,   // Ratio of pixels above 0.85 luminance threshold
    val motionScore: Float       // Frame-to-frame pixel change metric (0.0 to 1.0)
)

/**
 * Interface for live camera preview scene analysis.
 * Can be implemented by heuristic detectors or hardware-accelerated ML models (e.g. TFLite / NPU).
 */
interface SceneDetector {
    fun analyzeFrame(bitmap: Bitmap): FrameStats
    fun classifyScene(stats: FrameStats): LongExposureMode
    fun reset()
}

/**
 * Heuristic-based real-time scene classifier.
 * Analyzes subsampled 64x64 luminance maps to maintain 60fps throughput (<2ms computation).
 */
class HeuristicSceneDetector : SceneDetector {
    private var previousLuminance: FloatArray? = null
    private val sampleDim = 64
    private val pixelCount = sampleDim * sampleDim

    override fun analyzeFrame(bitmap: Bitmap): FrameStats {
        val scaled = Bitmap.createScaledBitmap(bitmap, sampleDim, sampleDim, false)
        val pixels = IntArray(pixelCount)
        scaled.getPixels(pixels, 0, sampleDim, 0, 0, sampleDim, sampleDim)
        if (scaled != bitmap) {
            scaled.recycle()
        }

        val currentLuma = FloatArray(pixelCount)
        var sumLuma = 0f
        var highlightCount = 0

        for (i in 0 until pixelCount) {
            val p = pixels[i]
            val r = (p shr 16) and 0xFF
            val g = (p shr 8) and 0xFF
            val b = p and 0xFF
            // Rec. 601 Luma
            val luma = (0.299f * r + 0.587f * g + 0.114f * b) / 255f
            currentLuma[i] = luma
            sumLuma += luma

            if (luma > 0.82f) {
                highlightCount++
            }
        }

        val avgLuma = sumLuma / pixelCount
        val highlightRatio = highlightCount.toFloat() / pixelCount

        var motionScore = 0f
        previousLuminance?.let { prev ->
            var diffSum = 0f
            for (i in 0 until pixelCount) {
                val diff = kotlin.math.abs(currentLuma[i] - prev[i])
                diffSum += diff
            }
            motionScore = (diffSum / pixelCount).coerceIn(0f, 1f)
        }
        previousLuminance = currentLuma

        return FrameStats(
            averageLuminance = avgLuma,
            highlightRatio = highlightRatio,
            motionScore = motionScore
        )
    }

    override fun classifyScene(stats: FrameStats): LongExposureMode {
        return when {
            // Dark scene with concentrated bright streaks and motion -> Light Trails
            stats.averageLuminance < 0.38f && stats.highlightRatio > 0.005f && stats.motionScore > 0.02f ->
                LongExposureMode.LIGHT_TRAILS

            // Bright daytime scene with significant motion (pedestrians, moving cars) -> Vanish
            stats.averageLuminance >= 0.35f && stats.motionScore > 0.08f ->
                LongExposureMode.VANISH

            // Low or fluid motion (water, clouds, mist) or generic scene -> Motion Blur
            else ->
                LongExposureMode.MOTION_BLUR
        }
    }

    override fun reset() {
        previousLuminance = null
    }
}
