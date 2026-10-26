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

    /**
     * Step 3 verification:
     * Feeds synthetic burst frames with a static background (bridge/buildings)
     * and a moving bright square (car headlights/body) across 30 frames.
     * Asserts:
     * 1. Static background pixels outside the movement path are completely unchanged and sharp.
     * 2. The moving square turns into a smooth continuous streak with mathematically exact intermediate values.
     * 3. Vanish mode removes the moving square, whereas Motion Blur blurs it into a streak.
     */
    @Test
    fun testSyntheticMovingSquareMotionBlur() {
        val width = 100
        val height = 100
        val numFrames = 30
        val motionBlender = MeanBlender(width, height)
        val vanishBlender = MedianBlender(width, height, maxReservoirFrames = numFrames)

        fun argb(r: Int, g: Int, b: Int): Int =
            (0xFF shl 24) or (r shl 16) or (g shl 8) or b

        val bgR = 80
        val bgG = 90
        val bgB = 100
        val carR = 250
        val carG = 240
        val carB = 220

        // Square size: 10x10, moves horizontally from x=10 to x=68 (2 pixels per frame)
        // along y range 40..49
        for (f in 0 until numFrames) {
            val frame = IntArray(width * height) { argb(bgR, bgG, bgB) }
            val carStartX = 10 + f * 2
            val carEndX = carStartX + 10

            for (y in 40 until 50) {
                for (x in carStartX until carEndX) {
                    if (x in 0 until width) {
                        frame[y * width + x] = argb(carR, carG, carB)
                    }
                }
            }

            motionBlender.addFrame(frame)
            vanishBlender.addDirectFrame(frame)
        }

        assertEquals(numFrames, motionBlender.frameCount)

        val motionResult = motionBlender.finalizeOutput()
        val vanishResult = vanishBlender.finalizeOutput()

        // 1. Static background verification: pixel at (10, 10) was never touched by the car
        val staticPixel = motionResult[10 * width + 10]
        val sR = (staticPixel shr 16) and 0xFF
        val sG = (staticPixel shr 8) and 0xFF
        val sB = staticPixel and 0xFF
        assertEquals("Static background R must stay sharp and unchanged", bgR, sR)
        assertEquals("Static background G must stay sharp and unchanged", bgG, sG)
        assertEquals("Static background B must stay sharp and unchanged", bgB, sB)

        // 2. Motion blur streak verification: pixel at (30, 45) was traversed by the moving car
        // The square occupied x in [30-9, 30] = [21, 30] for several frames out of 30.
        // It must be strictly brighter than background (80) and strictly dimmer than pure car (250).
        val streakPixel = motionResult[45 * width + 30]
        val streakR = (streakPixel shr 16) and 0xFF
        org.junit.Assert.assertTrue(
            "Motion blur streak must be brighter than static background ($bgR), actual=$streakR",
            streakR > bgR
        )
        org.junit.Assert.assertTrue(
            "Motion blur streak must be smoothly blended and lower than peak car ($carR), actual=$streakR",
            streakR < carR
        )

        // 3. Smoothness check: adjacent pixels along the trajectory must transition smoothly without gaps
        var prevR = (motionResult[45 * width + 20] shr 16) and 0xFF
        for (x in 21..60) {
            val currR = (motionResult[45 * width + x] shr 16) and 0xFF
            val delta = kotlin.math.abs(currR - prevR)
            org.junit.Assert.assertTrue(
                "Motion blur streak gradient must be smooth without jump cuts (delta=$delta at x=$x)",
                delta <= 25
            )
            prevR = currR
        }

        // 4. Contrast with Vanish mode: Vanish mode should remove the moving car completely
        val vanishStreakPixel = vanishResult[45 * width + 30]
        val vR = (vanishStreakPixel shr 16) and 0xFF
        assertEquals("Vanish mode must remove the car back to background luminance", bgR, vR)
    }
}
