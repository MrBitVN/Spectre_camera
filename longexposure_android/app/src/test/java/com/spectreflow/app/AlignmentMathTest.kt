package com.spectreflow.app

import com.spectreflow.app.alignment.ShiftEstimator
import org.junit.Assert.assertEquals
import org.junit.Test

class AlignmentMathTest {

    @Test
    fun testAlignmentTranslationalShiftDetection() {
        val ref = ByteArray(160 * 120)
        for (y in 40 until 50) {
            for (x in 50 until 60) {
                ref[y * 160 + x] = 200.toByte()
            }
        }

        val curr = ByteArray(160 * 120)
        for (y in 43 until 53) {
            for (x in 52 until 62) {
                curr[y * 160 + x] = 200.toByte()
            }
        }

        val (shiftX, shiftY) = ShiftEstimator.computeTranslationalShift(ref, curr, 160, 120)

        // The shift should be (-2, -3) to pull the shifted frame back into alignment with ref
        assertEquals(-2, shiftX)
        assertEquals(-3, shiftY)
    }

    @Test
    fun testAlignmentZeroShift() {
        val ref = ByteArray(160 * 120)
        for (y in 30 until 40) {
            for (x in 30 until 40) {
                ref[y * 160 + x] = 180.toByte()
            }
        }

        val (shiftX, shiftY) = ShiftEstimator.computeTranslationalShift(ref, ref, 160, 120)
        assertEquals(0, shiftX)
        assertEquals(0, shiftY)
    }
}
