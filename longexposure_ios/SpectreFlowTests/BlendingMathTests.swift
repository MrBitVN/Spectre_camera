import XCTest
@testable import SpectreFlow

final class BlendingMathTests: XCTestCase {

    private func makeRGBA(r: UInt8, g: UInt8, b: UInt8, a: UInt8 = 0xFF) -> UInt32 {
        return (UInt32(a) << 24) | (UInt32(b) << 16) | (UInt32(g) << 8) | UInt32(r)
    }

    func testMeanBlenderMath() {
        let width = 2
        let height = 2
        let blender = MeanBlender(width: width, height: height)

        let frame1 = [UInt32](repeating: makeRGBA(r: 10, g: 20, b: 30), count: 4)
        let frame2 = [UInt32](repeating: makeRGBA(r: 50, g: 60, b: 70), count: 4)
        let frame3 = [UInt32](repeating: makeRGBA(r: 90, g: 100, b: 110), count: 4)

        blender.addFrame(rgbaPixels: frame1)
        blender.addFrame(rgbaPixels: frame2)
        blender.addFrame(rgbaPixels: frame3)

        XCTAssertEqual(blender.frameCount, 3)

        let output = blender.finalizeOutput()

        // Mean expected: R=(10+50+90)/3=50, G=(20+60+100)/3=60, B=(30+70+110)/3=70
        for pixel in output {
            let r = pixel & 0xFF
            let g = (pixel >> 8) & 0xFF
            let b = (pixel >> 16) & 0xFF

            XCTAssertEqual(r, 50)
            XCTAssertEqual(g, 60)
            XCTAssertEqual(b, 70)
        }
    }

    func testLightTrailsBlenderMath() {
        let width = 2
        let height = 2
        let blender = LightTrailsBlender(width: width, height: height)

        var f1 = [UInt32](repeating: makeRGBA(r: 30, g: 30, b: 30), count: 4)
        var f2 = [UInt32](repeating: makeRGBA(r: 30, g: 30, b: 30), count: 4)
        f2[0] = makeRGBA(r: 250, g: 240, b: 190) // Car headlight

        var f3 = [UInt32](repeating: makeRGBA(r: 30, g: 30, b: 30), count: 4)
        f3[3] = makeRGBA(r: 255, g: 100, b: 40)  // Sparkler

        blender.addFrame(rgbaPixels: f1)
        blender.addFrame(rgbaPixels: f2)
        blender.addFrame(rgbaPixels: f3)

        let output = blender.finalizeOutput()

        // Headlight preserved on pixel 0
        let p0 = output[0]
        XCTAssertEqual(p0 & 0xFF, 250)
        XCTAssertEqual((p0 >> 8) & 0xFF, 240)
        XCTAssertEqual((p0 >> 16) & 0xFF, 190)

        // Pixel 1 remains background
        let p1 = output[1]
        XCTAssertEqual(p1 & 0xFF, 30)
        XCTAssertEqual((p1 >> 8) & 0xFF, 30)
        XCTAssertEqual((p1 >> 16) & 0xFF, 30)

        // Pixel 3 preserves sparkler
        let p3 = output[3]
        XCTAssertEqual(p3 & 0xFF, 255)
        XCTAssertEqual((p3 >> 8) & 0xFF, 100)
        XCTAssertEqual((p3 >> 16) & 0xFF, 40)
    }

    func testVanishMedianBlenderMath() {
        let width = 2
        let height = 2
        let blender = MedianBlender(width: width, height: height, maxReservoirFrames: 5)

        // Wall background is 45 across 5 frames
        // Tourist passes across pixel 2 in frame 3 (color 220)
        let f1 = [UInt32](repeating: makeRGBA(r: 45, g: 45, b: 45), count: 4)
        let f2 = [UInt32](repeating: makeRGBA(r: 45, g: 45, b: 45), count: 4)
        var f3 = [UInt32](repeating: makeRGBA(r: 45, g: 45, b: 45), count: 4)
        f3[2] = makeRGBA(r: 220, g: 210, b: 200)
        let f4 = [UInt32](repeating: makeRGBA(r: 45, g: 45, b: 45), count: 4)
        let f5 = [UInt32](repeating: makeRGBA(r: 45, g: 45, b: 45), count: 4)

        blender.addDirectFrame(rgbaPixels: f1)
        blender.addDirectFrame(rgbaPixels: f2)
        blender.addDirectFrame(rgbaPixels: f3)
        blender.addDirectFrame(rgbaPixels: f4)
        blender.addDirectFrame(rgbaPixels: f5)

        let output = blender.finalizeOutput()

        // Median of [45, 45, 220, 45, 45] is 45! Tourist is removed.
        let p2 = output[2]
        XCTAssertEqual(p2 & 0xFF, 45)
        XCTAssertEqual((p2 >> 8) & 0xFF, 45)
        XCTAssertEqual((p2 >> 16) & 0xFF, 45)
    }

    /// Step 3 verification:
    /// Feeds synthetic burst frames with static background (bridge/buildings)
    /// and a moving bright square (moving car) across 30 frames.
    /// Asserts static background is completely unchanged and moving square blurs into a smooth streak.
    func testSyntheticMovingSquareMotionBlur() {
        let width = 100
        let height = 100
        let numFrames = 30
        let motionBlender = MeanBlender(width: width, height: height)
        let vanishBlender = MedianBlender(width: width, height: height, maxReservoirFrames: numFrames)

        let bgR: UInt8 = 80
        let bgG: UInt8 = 90
        let bgB: UInt8 = 100
        let carR: UInt8 = 250
        let carG: UInt8 = 240
        let carB: UInt8 = 220

        for f in 0..<numFrames {
            var frame = [UInt32](repeating: makeRGBA(r: bgR, g: bgG, b: bgB), count: width * height)
            let carStartX = 10 + f * 2
            let carEndX = carStartX + 10

            for y in 40..<50 {
                for x in carStartX..<carEndX {
                    if x < width {
                        frame[y * width + x] = makeRGBA(r: carR, g: carG, b: carB)
                    }
                }
            }

            motionBlender.addFrame(rgbaPixels: frame)
            vanishBlender.addDirectFrame(rgbaPixels: frame)
        }

        XCTAssertEqual(motionBlender.frameCount, numFrames)

        let motionResult = motionBlender.finalizeOutput()
        let vanishResult = vanishBlender.finalizeOutput()

        // 1. Static background: pixel at (10, 10) untouched by car
        let staticPixel = motionResult[10 * width + 10]
        XCTAssertEqual(staticPixel & 0xFF, bgR)
        XCTAssertEqual((staticPixel >> 8) & 0xFF, bgG)
        XCTAssertEqual((staticPixel >> 16) & 0xFF, bgB)

        // 2. Motion blur streak: pixel at (30, 45) traversed by moving car
        let streakPixel = motionResult[45 * width + 30]
        let streakR = streakPixel & 0xFF
        XCTAssertTrue(streakR > bgR, "Streak must be brighter than static background")
        XCTAssertTrue(streakR < carR, "Streak must be smooth and dimmer than peak car")

        // 3. Vanish mode removes car completely back to background
        let vanishPixel = vanishResult[45 * width + 30]
        XCTAssertEqual(vanishPixel & 0xFF, bgR)
    }
}
