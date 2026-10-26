package com.spectreflow.app.blender

/**
 * Memory-bounded Temporal Median Blender for "Vanish" Mode.
 * Removes transient elements (walking tourists, driving vehicles, fleeting pedestrians)
 * from stationary architecture/scenery by taking the median pixel value over time.
 *
 * To avoid Out-Of-Memory crashes, retains an evenly spaced reservoir of keyframes
 * (default max 15 frames) across the exposure duration.
 */
class MedianBlender(
    val width: Int,
    val height: Int,
    private val maxReservoirFrames: Int = 11
) {
    private val pixelCount = width * height
    private val frameReservoir = ArrayList<IntArray>()
    var totalObservedFrames: Int = 0
        private set

    /**
     * Ingests a new aligned frame. Uses reservoir subsampling if frame count exceeds capacity.
     */
    fun addFrame(pixels: IntArray) {
        require(pixels.size == pixelCount) {
            "Frame size (${pixels.size}) does not match blender dimensions ($pixelCount)"
        }
        totalObservedFrames++

        if (frameReservoir.size < maxReservoirFrames) {
            val copy = IntArray(pixelCount)
            System.arraycopy(pixels, 0, copy, 0, pixelCount)
            frameReservoir.add(copy)
        } else {
            // Uniform reservoir replacement to evenly span the capture window
            val replaceIndex = (0 until frameReservoir.size).random()
            if (totalObservedFrames % (totalObservedFrames / maxReservoirFrames + 1) == 0) {
                System.arraycopy(pixels, 0, frameReservoir[replaceIndex], 0, pixelCount)
            }
        }
    }

    /**
     * Ingests frame directly for deterministic unit testing.
     */
    fun addDirectFrame(pixels: IntArray) {
        val copy = IntArray(pixelCount)
        System.arraycopy(pixels, 0, copy, 0, pixelCount)
        frameReservoir.add(copy)
        totalObservedFrames++
    }

    /**
     * Computes the per-pixel channel median across all frames in the reservoir.
     */
    fun finalizeOutput(): IntArray {
        val output = IntArray(pixelCount)
        val k = frameReservoir.size
        if (k == 0) return output
        if (k == 1) {
            System.arraycopy(frameReservoir[0], 0, output, 0, pixelCount)
            return output
        }

        val rValues = IntArray(k)
        val gValues = IntArray(k)
        val bValues = IntArray(k)
        val medianIdx = k / 2

        for (p in 0 until pixelCount) {
            for (f in 0 until k) {
                val color = frameReservoir[f][p]
                rValues[f] = (color shr 16) and 0xFF
                gValues[f] = (color shr 8) and 0xFF
                bValues[f] = color and 0xFF
            }

            // In-place sort of the small array (k <= 15 is extremely fast with insertion sort)
            val medR = computeMedian(rValues, medianIdx)
            val medG = computeMedian(gValues, medianIdx)
            val medB = computeMedian(bValues, medianIdx)

            output[p] = (0xFF shl 24) or (medR shl 16) or (medG shl 8) or medB
        }

        return output
    }

    /**
     * Computes median using insertion sort for small arrays.
     */
    internal fun computeMedian(values: IntArray, targetIndex: Int): Int {
        for (i in 1 until values.size) {
            val key = values[i]
            var j = i - 1
            while (j >= 0 && values[j] > key) {
                values[j + 1] = values[j]
                j--
            }
            values[j + 1] = key
        }
        return values[targetIndex]
    }

    fun reset() {
        frameReservoir.clear()
        totalObservedFrames = 0
    }
}
