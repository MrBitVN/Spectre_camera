package com.spectreflow.app.alignment

import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Mathematical 2D cross-correlation shift estimator.
 * Free from Android UI framework dependencies for unit testability.
 */
object ShiftEstimator {
    fun computeTranslationalShift(
        ref: ByteArray,
        curr: ByteArray,
        dsWidth: Int = 160,
        dsHeight: Int = 120,
        maxSearchRadius: Int = 8
    ): Pair<Int, Int> {
        val radius = min(maxSearchRadius, 8)
        var minSad = Long.MAX_VALUE
        var bestDx = 0
        var bestDy = 0

        val cropStartX = radius
        val cropEndX = dsWidth - radius
        val cropStartY = radius
        val cropEndY = dsHeight - radius

        for (dy in -radius..radius) {
            for (dx in -radius..radius) {
                var sad = 0L

                var y = cropStartY
                while (y < cropEndY) {
                    var x = cropStartX
                    while (x < cropEndX) {
                        val refVal = ref[y * dsWidth + x].toInt() and 0xFF
                        val currVal = curr[(y + dy) * dsWidth + (x + dx)].toInt() and 0xFF
                        sad += abs(refVal - currVal)
                        x += 2
                    }
                    y += 2
                }

                if (sad < minSad) {
                    minSad = sad
                    bestDx = dx
                    bestDy = dy
                }
            }
        }
        return Pair(-bestDx, -bestDy)
    }
}
