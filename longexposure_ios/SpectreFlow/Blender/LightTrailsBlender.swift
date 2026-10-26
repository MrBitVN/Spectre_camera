import Foundation
import CoreGraphics

/// Streaming Light Trails (Max/Lighten) Blender.
/// Applies per-pixel channel maximum blending across consecutive frames in O(1) space.
/// Captures vehicle headlight streaks, fireworks, and stellar motion.
public final class LightTrailsBlender {
    public let width: Int
    public let height: Int
    private let pixelCount: Int

    private var outputPixels: [UInt32]
    public private(set) var frameCount: Int = 0

    public init(width: Int, height: Int) {
        self.width = width
        self.height = height
        self.pixelCount = width * height
        self.outputPixels = [UInt32](repeating: 0, count: pixelCount)
    }

    public func addFrame(rgbaPixels: [UInt32]) {
        precondition(rgbaPixels.count == pixelCount, "Pixel buffer size does not match dimensions")

        if frameCount == 0 {
            outputPixels = rgbaPixels
            frameCount = 1
            return
        }

        rgbaPixels.withUnsafeBufferPointer { inBuffer in
            guard let inBase = inBuffer.baseAddress else { return }
            outputPixels.withUnsafeMutableBufferPointer { outBuffer in
                guard let outBase = outBuffer.baseAddress else { return }

                for i in 0..<pixelCount {
                    let current = outBase[i]
                    let curR = current & 0xFF
                    let curG = (current >> 8) & 0xFF
                    let curB = (current >> 16) & 0xFF

                    let incoming = inBase[i]
                    let inR = incoming & 0xFF
                    let inG = (incoming >> 8) & 0xFF
                    let inB = (incoming >> 16) & 0xFF

                    let maxR = max(curR, inR)
                    let maxG = max(curG, inG)
                    let maxB = max(curB, inB)
                    let a: UInt32 = 0xFF

                    outBase[i] = (a << 24) | (maxB << 16) | (maxG << 8) | maxR
                }
            }
        }
        frameCount += 1
    }

    public func finalizeOutput() -> [UInt32] {
        return outputPixels
    }

    public func reset() {
        outputPixels = [UInt32](repeating: 0, count: pixelCount)
        frameCount = 0
    }
}
