import Foundation
import CoreGraphics
import CoreVideo
import UIKit

public struct FrameAnalysisStats {
    public let averageLuminance: Float  // 0.0 to 1.0
    public let highlightRatio: Float    // Ratio of pixels with luminance > 0.82
    public let motionScore: Float       // Frame delta metric (0.0 to 1.0)
}

/// Protocol for real-time camera scene analysis.
/// Designed for pluggable Core ML classification model or heuristic analysis.
public protocol SceneDetectorProtocol: AnyObject {
    func analyzePixelBuffer(_ pixelBuffer: CVPixelBuffer) -> FrameAnalysisStats
    func classifyScene(stats: FrameAnalysisStats) -> LongExposureMode
    func reset()
}

public final class HeuristicSceneDetector: SceneDetectorProtocol {
    private var previousLuminance: [Float]?
    private let sampleWidth = 48
    private let sampleHeight = 48
    private var pixelCount: Int { sampleWidth * sampleHeight }

    public init() {}

    public func analyzePixelBuffer(_ pixelBuffer: CVPixelBuffer) -> FrameAnalysisStats {
        CVPixelBufferLockBaseAddress(pixelBuffer, .readOnly)
        defer { CVPixelBufferUnlockBaseAddress(pixelBuffer, .readOnly) }

        guard let baseAddress = CVPixelBufferGetBaseAddress(pixelBuffer) else {
            return FrameAnalysisStats(averageLuminance: 0.5, highlightRatio: 0.0, motionScore: 0.0)
        }

        let width = CVPixelBufferGetWidth(pixelBuffer)
        let height = CVPixelBufferGetHeight(pixelBuffer)
        let bytesPerRow = CVPixelBufferGetBytesPerRow(pixelBuffer)

        let stepX = max(1, width / sampleWidth)
        let stepY = max(1, height / sampleHeight)

        var currentLuma = [Float]()
        currentLuma.reserveCapacity(pixelCount)

        var sumLuma: Float = 0.0
        var highlightCount = 0

        let bufferPtr = baseAddress.assumingMemoryBound(to: UInt8.self)

        for y in stride(from: 0, to: height, by: stepY) {
            let rowOffset = y * bytesPerRow
            for x in stride(from: 0, to: width, by: stepX) {
                let pixelOffset = rowOffset + x * 4
                // BGRA format
                let b = Float(bufferPtr[pixelOffset])
                let g = Float(bufferPtr[pixelOffset + 1])
                let r = Float(bufferPtr[pixelOffset + 2])

                // Rec. 601 Luminance
                let luma = (0.299 * r + 0.587 * g + 0.114 * b) / 255.0
                currentLuma.append(luma)
                sumLuma += luma

                if luma > 0.82 {
                    highlightCount += 1
                }
            }
        }

        let count = Float(max(1, currentLuma.count))
        let avgLuma = sumLuma / count
        let highlightRatio = Float(highlightCount) / count

        var motionScore: Float = 0.0
        if let prev = previousLuminance, prev.count == currentLuma.count {
            var diffSum: Float = 0.0
            for i in 0..<prev.count {
                diffSum += abs(currentLuma[i] - prev[i])
            }
            motionScore = min(1.0, diffSum / count)
        }
        previousLuminance = currentLuma

        return FrameAnalysisStats(
            averageLuminance: avgLuma,
            highlightRatio: highlightRatio,
            motionScore: motionScore
        )
    }

    public func classifyScene(stats: FrameAnalysisStats) -> LongExposureMode {
        // Dark scene with concentrated bright lights & motion -> Light Trails
        if stats.averageLuminance < 0.38 && stats.highlightRatio > 0.005 && stats.motionScore > 0.02 {
            return .lightTrails
        }
        // In daytime scenes with traffic or motion, default to Motion Blur (mean)
        // so cars turn into smooth silky streaks while background remains sharp.
        return .motionBlur
    }

    public func reset() {
        previousLuminance = nil
    }
}
