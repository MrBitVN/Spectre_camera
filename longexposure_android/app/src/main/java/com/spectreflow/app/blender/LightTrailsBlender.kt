package com.spectreflow.app.blender

import kotlin.math.max

/**
 * Streaming Light Trails (Max/Lighten) Blender.
 * Performs per-pixel maximum blending across burst frames with O(1) memory.
 * Preserves high-intensity dynamic elements (vehicle headlights, sparklers, stars)
 * over dark, stable backgrounds.
 */
class LightTrailsBlender(val width: Int, val height: Int) {
    private val pixelCount = width * height
    private val outputPixels = IntArray(pixelCount)

    var frameCount: Int = 0
        private set

    fun addFrame(pixels: IntArray) {
        require(pixels.size == pixelCount) {
            "Frame size (${pixels.size}) does not match blender dimensions ($pixelCount)"
        }
        if (frameCount == 0) {
            System.arraycopy(pixels, 0, outputPixels, 0, pixelCount)
            frameCount = 1
            return
        }

        for (i in 0 until pixelCount) {
            val currColor = outputPixels[i]
            val currR = (currColor shr 16) and 0xFF
            val currG = (currColor shr 8) and 0xFF
            val currB = currColor and 0xFF

            val newColor = pixels[i]
            val newR = (newColor shr 16) and 0xFF
            val newG = (newColor shr 8) and 0xFF
            val newB = newColor and 0xFF

            // Per-pixel channel maximum blend
            val maxR = max(currR, newR)
            val maxG = max(currG, newG)
            val maxB = max(currB, newB)

            outputPixels[i] = (0xFF shl 24) or (maxR shl 16) or (maxG shl 8) or maxB
        }
        frameCount++
    }

    fun finalizeOutput(): IntArray {
        val result = IntArray(pixelCount)
        System.arraycopy(outputPixels, 0, result, 0, pixelCount)
        return result
    }

    fun reset() {
        outputPixels.fill(0)
        frameCount = 0
    }
}
