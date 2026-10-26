package com.spectreflow.app

import com.spectreflow.app.blender.LightTrailsBlender
import com.spectreflow.app.blender.MeanBlender
import com.spectreflow.app.blender.MedianBlender
import org.junit.Assert.assertEquals
import org.junit.Test

class BlendingMathTest {

    @Test
    fun testMeanBlenderMath() {
        val width = 2
        val height = 2
        val blender = MeanBlender(width, height)

        // Helper: pack ARGB
        fun argb(r: Int, g: Int, b: Int): Int =
            (0xFF shl 24) or (r shl 16) or (g shl 8) or b

        // Frame 1: pixel values (10, 20, 30) for all pixels
        val frame1 = IntArray(4) { argb(10, 20, 30) }
        // Frame 2: pixel values (50, 60, 70)
        val frame2 = IntArray(4) { argb(50, 60, 70) }
        // Frame 3: pixel values (90, 100, 110)
        val frame3 = IntArray(4) { argb(90, 100, 110) }

        blender.addFrame(frame1)
        blender.addFrame(frame2)
        blender.addFrame(frame3)

        assertEquals(3, blender.frameCount)

        val output = blender.finalizeOutput()
        // Expected mean:
        // R: (10 + 50 + 90) / 3 = 50
        // G: (20 + 60 + 100) / 3 = 60
        // B: (30 + 70 + 110) / 3 = 70
        for (i in 0 until 4) {
            val pixel = output[i]
            val r = (pixel shr 16) and 0xFF
            val g = (pixel shr 8) and 0xFF
            val b = pixel and 0xFF

            assertEquals("Red mean mismatch", 50, r)
            assertEquals("Green mean mismatch", 60, g)
            assertEquals("Blue mean mismatch", 70, b)
        }
    }

    @Test
    fun testLightTrailsBlenderMath() {
        val width = 2
        val height = 2
        val blender = LightTrailsBlender(width, height)

        fun argb(r: Int, g: Int, b: Int): Int =
            (0xFF shl 24) or (r shl 16) or (g shl 8) or b

        // Frame 1: dark background
        val frame1 = IntArray(4) { argb(20, 25, 30) }
        // Frame 2: car headlight on pixel 0
        val frame2 = IntArray(4) { argb(20, 25, 30) }
        frame2[0] = argb(240, 235, 180)
        // Frame 3: firework spark on pixel 3
        val frame3 = IntArray(4) { argb(20, 25, 30) }
        frame3[3] = argb(255, 120, 50)

        blender.addFrame(frame1)
        blender.addFrame(frame2)
        blender.addFrame(frame3)

        val output = blender.finalizeOutput()

        // Pixel 0 should preserve headlight max
        val p0 = output[0]
        assertEquals("Headlight R", 240, (p0 shr 16) and 0xFF)
        assertEquals("Headlight G", 235, (p0 shr 8) and 0xFF)
        assertEquals("Headlight B", 180, p0 and 0xFF)

        // Pixel 1 should remain background
        val p1 = output[1]
        assertEquals("Background R", 20, (p1 shr 16) and 0xFF)
        assertEquals("Background G", 25, (p1 shr 8) and 0xFF)
        assertEquals("Background B", 30, p1 and 0xFF)

        // Pixel 3 should preserve firework
        val p3 = output[3]
        assertEquals("Firework R", 255, (p3 shr 16) and 0xFF)
        assertEquals("Firework G", 120, (p3 shr 8) and 0xFF)
        assertEquals("Firework B", 50, p3 and 0xFF)
    }

    @Test
    fun testVanishMedianBlenderMath() {
        val width = 2
        val height = 2
        val blender = MedianBlender(width, height, maxReservoirFrames = 5)

        fun argb(r: Int, g: Int, b: Int): Int =
            (0xFF shl 24) or (r shl 16) or (g shl 8) or b

        // Background is 40 across all frames
        // In frame 3 (middle), a tourist walks through pixel 1 (color 230)
        val f1 = IntArray(4) { argb(40, 40, 40) }
        val f2 = IntArray(4) { argb(40, 40, 40) }
        val f3 = IntArray(4) { argb(40, 40, 40) }.apply { this[1] = argb(230, 220, 210) }
        val f4 = IntArray(4) { argb(40, 40, 40) }
        val f5 = IntArray(4) { argb(40, 40, 40) }

        blender.addDirectFrame(f1)
        blender.addDirectFrame(f2)
        blender.addDirectFrame(f3)
        blender.addDirectFrame(f4)
        blender.addDirectFrame(f5)

        val output = blender.finalizeOutput()

        // Pixel 1 should completely vanish the tourist (median of [40, 40, 230, 40, 40] is 40!)
        val p1 = output[1]
        val r = (p1 shr 16) and 0xFF
        val g = (p1 shr 8) and 0xFF
        val b = p1 and 0xFF

        assertEquals("Tourist in R should vanish", 40, r)
        assertEquals("Tourist in G should vanish", 40, g)
        assertEquals("Tourist in B should vanish", 40, b)
    }
}
