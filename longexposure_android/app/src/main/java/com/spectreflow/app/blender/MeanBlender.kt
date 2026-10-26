package com.spectreflow.app.blender

import kotlin.math.roundToInt

/**
 * High-performance streaming Mean (Average) Blender.
 * Accumulates pixel color values across consecutive frames with O(1) memory overhead.
 * Blends turbulent moving elements (waterfalls, oceans, clouds, pedestrians) into smooth silk.
 */
class MeanBlender(val width: Int, val height: Int) {
    private val pixelCount = width * height
    private val redAccumulator = LongArray(pixelCount)
    private val greenAccumulator = LongArray(pixelCount)
    private val blueAccumulator = LongArray(pixelCount)

    var frameCount: Int = 0
        private set

    fun addFrame(pixels: IntArray) {
        require(pixels.size == pixelCount) {
            "Frame size (${pixels.size}) does not match blender dimensions ($pixelCount)"
        }
        for (i in 0 until pixelCount) {
            val color = pixels[i]
            val r = (color shr 16) and 0xFF
            val g = (color shr 8) and 0xFF
            val b = color and 0xFF

            redAccumulator[i] += r.toLong()
            greenAccumulator[i] += g.toLong()
            blueAccumulator[i] += b.toLong()
        }
        frameCount++
    }

    fun finalizeOutput(): IntArray {
        val output = IntArray(pixelCount)
        if (frameCount == 0) return output

        val count = frameCount.toDouble()
        for (i in 0 until pixelCount) {
            val r = (redAccumulator[i] / count).roundToInt().coerceIn(0, 255)
            val g = (greenAccumulator[i] / count).roundToInt().coerceIn(0, 255)
            val b = (blueAccumulator[i] / count).roundToInt().coerceIn(0, 255)
            output[i] = (0xFF shl 24) or (r shl 16) or (g shl 8) or b
        }
        return output
    }

    fun reset() {
        redAccumulator.fill(0L)
        greenAccumulator.fill(0L)
        blueAccumulator.fill(0L)
        frameCount = 0
    }
}
