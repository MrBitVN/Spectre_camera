import Foundation
import CoreGraphics
import UIKit

/// Streaming Mean (Average) Image Blender.
/// Accumulates color values incrementally per pixel with strictly O(1) memory footprint.
/// Blends turbulent waterfalls, ocean surfs, and skies into a silky, smooth aesthetic.
public final class MeanBlender {
    public let width: Int
    public let height: Int
    private let pixelCount: Int

    private var redAccumulator: [Int64]
    private var greenAccumulator: [Int64]
    private var blueAccumulator: [Int64]

    public private(set) var frameCount: Int = 0

    public init(width: Int, height: Int) {
        self.width = width
        self.height = height
        self.pixelCount = width * height
        self.redAccumulator = [Int64](repeating: 0, count: pixelCount)
        self.greenAccumulator = [Int64](repeating: 0, count: pixelCount)
        self.blueAccumulator = [Int64](repeating: 0, count: pixelCount)
    }

    /// Ingests a 32-bit RGBA pixel array.
    public func addFrame(rgbaPixels: [UInt32]) {
        precondition(rgbaPixels.count == pixelCount, "Pixel buffer size does not match dimensions")

        rgbaPixels.withUnsafeBufferPointer { buffer in
            guard let baseAddress = buffer.baseAddress else { return }
            for i in 0..<pixelCount {
                let pixel = baseAddress[i]
                let r = Int64(pixel & 0xFF)
                let g = Int64((pixel >> 8) & 0xFF)
                let b = Int64((pixel >> 16) & 0xFF)

                redAccumulator[i] += r
                greenAccumulator[i] += g
                blueAccumulator[i] += b
            }
        }
        frameCount += 1
    }

    /// Computes the final mean pixel matrix and packs into a 32-bit RGBA image.
    public func finalizeOutput() -> [UInt32] {
        guard frameCount > 0 else {
            return [UInt32](repeating: 0xFF000000, count: pixelCount)
        }

        var output = [UInt32](repeating: 0, count: pixelCount)
        let divisor = Double(frameCount)

        output.withUnsafeMutableBufferPointer { outBuffer in
            guard let outBase = outBuffer.baseAddress else { return }
            for i in 0..<pixelCount {
                let r = UInt32(min(max(Int((Double(redAccumulator[i]) / divisor).rounded()), 0), 255))
                let g = UInt32(min(max(Int((Double(greenAccumulator[i]) / divisor).rounded()), 0), 255))
                let b = UInt32(min(max(Int((Double(blueAccumulator[i]) / divisor).rounded()), 0), 255))
                let a: UInt32 = 0xFF

                outBase[i] = (a << 24) | (b << 16) | (g << 8) | r
            }
        }

        return output
    }

    public func reset() {
        redAccumulator = [Int64](repeating: 0, count: pixelCount)
        greenAccumulator = [Int64](repeating: 0, count: pixelCount)
        blueAccumulator = [Int64](repeating: 0, count: pixelCount)
        frameCount = 0
    }
}
