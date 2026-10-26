import Foundation

/// Memory-bounded Temporal Median Blender for "Vanish" Mode.
/// Removes transient people and traffic from stationary scenery.
/// Uses keyframe reservoir sampling to strictly bound iOS heap memory consumption.
public final class MedianBlender {
    public let width: Int
    public let height: Int
    private let pixelCount: Int
    private let maxReservoirFrames: Int

    private var frameReservoir: [[UInt32]] = []
    public private(set) var totalObservedFrames: Int = 0

    public init(width: Int, height: Int, maxReservoirFrames: Int = 11) {
        self.width = width
        self.height = height
        self.pixelCount = width * height
        self.maxReservoirFrames = maxReservoirFrames
    }

    public func addFrame(rgbaPixels: [UInt32]) {
        precondition(rgbaPixels.count == pixelCount, "Pixel buffer size does not match dimensions")
        totalObservedFrames += 1

        if frameReservoir.count < maxReservoirFrames {
            frameReservoir.append(rgbaPixels)
        } else {
            // Uniform keyframe replacement across the temporal span
            let stride = totalObservedFrames / maxReservoirFrames + 1
            if totalObservedFrames % stride == 0 {
                let randomIndex = Int.random(in: 0..<frameReservoir.count)
                frameReservoir[randomIndex] = rgbaPixels
            }
        }
    }

    /// Direct frame ingestion for deterministic unit testing
    public func addDirectFrame(rgbaPixels: [UInt32]) {
        frameReservoir.append(rgbaPixels)
        totalObservedFrames += 1
    }

    public func finalizeOutput() -> [UInt32] {
        let k = frameReservoir.count
        guard k > 0 else {
            return [UInt32](repeating: 0xFF000000, count: pixelCount)
        }
        if k == 1 {
            return frameReservoir[0]
        }

        var output = [UInt32](repeating: 0, count: pixelCount)
        let medianIndex = k / 2
        var rChannel = [Int](repeating: 0, count: k)
        var gChannel = [Int](repeating: 0, count: k)
        var bChannel = [Int](repeating: 0, count: k)

        for p in 0..<pixelCount {
            for f in 0..<k {
                let pixel = frameReservoir[f][p]
                rChannel[f] = Int(pixel & 0xFF)
                gChannel[f] = Int((pixel >> 8) & 0xFF)
                bChannel[f] = Int((pixel >> 16) & 0xFF)
            }

            let medR = UInt32(insertionSortMedian(&rChannel, targetIndex: medianIndex))
            let medG = UInt32(insertionSortMedian(&gChannel, targetIndex: medianIndex))
            let medB = UInt32(insertionSortMedian(&bChannel, targetIndex: medianIndex))
            let a: UInt32 = 0xFF

            output[p] = (a << 24) | (medB << 16) | (medG << 8) | medR
        }

        return output
    }

    /// Fast in-place insertion sort to locate median index for small K <= 15
    internal func insertionSortMedian(_ array: inout [Int], targetIndex: Int) -> Int {
        for i in 1..<array.count {
            let key = array[i]
            var j = i - 1
            while j >= 0 && array[j] > key {
                array[j + 1] = array[j]
                j -= 1
            }
            array[j + 1] = key
        }
        return array[targetIndex]
    }

    public func reset() {
        frameReservoir.removeAll(keepingCapacity: false)
        totalObservedFrames = 0
    }
}
