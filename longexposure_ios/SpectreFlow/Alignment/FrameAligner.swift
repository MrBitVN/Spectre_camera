import Foundation
import UIKit
import Vision
import CoreImage

public struct AlignmentResult {
    public let alignedImage: UIImage
    public let deltaX: CGFloat
    public let deltaY: CGFloat
    public let cropRect: CGRect
}

/// Vision-based Handheld Tremor Auto-Alignment Engine.
/// Computes image registration translation vectors relative to the base frame,
/// warps incoming frames into spatial alignment, and tracks the cumulative crop bounding box.
public final class FrameAligner {
    public let baseWidth: CGFloat
    public let baseHeight: CGFloat

    private var referenceCVPixelBuffer: CVPixelBuffer?
    public private(set) var cropBounds: CGRect
    private var frameIndex = 0

    private let ciContext = CIContext(options: [.useSoftwareRenderer: false])

    public init(baseWidth: CGFloat, baseHeight: CGFloat) {
        self.baseWidth = baseWidth
        self.baseHeight = baseHeight
        self.cropBounds = CGRect(x: 0, y: 0, width: baseWidth, height: baseHeight)
    }

    /// Aligns incoming frame to reference frame 0.
    /// If isStableTripod is true, bypasses registration to save CPU.
    public func align(image: UIImage, pixelBuffer: CVPixelBuffer?, isStableTripod: Bool) -> AlignmentResult {
        if frameIndex == 0 || isStableTripod {
            if frameIndex == 0 {
                self.referenceCVPixelBuffer = pixelBuffer
            }
            frameIndex += 1
            return AlignmentResult(
                alignedImage: image,
                deltaX: 0,
                deltaY: 0,
                cropRect: cropBounds
            )
        }

        guard let refBuffer = referenceCVPixelBuffer, let targetBuffer = pixelBuffer else {
            frameIndex += 1
            return AlignmentResult(alignedImage: image, deltaX: 0, deltaY: 0, cropRect: cropBounds)
        }

        // Vision Image Registration
        var deltaX: CGFloat = 0
        var deltaY: CGFloat = 0

        let registrationRequest = VNTranslationalImageRegistrationRequest(targetedCVPixelBuffer: targetBuffer)
        let handler = VNSequenceRequestHandler()

        do {
            try handler.perform([registrationRequest], on: refBuffer)
            if let observation = registrationRequest.results?.first as? VNImageTranslationObservation {
                let transform = observation.alignmentTransform
                deltaX = transform.tx
                deltaY = transform.ty
            }
        } catch {
            print("Vision registration error: \(error)")
        }

        // Warp image with translation transform
        let alignedImage: UIImage
        if abs(deltaX) > 0.1 || abs(deltaY) > 0.1, let cgImg = image.cgImage {
            let ciImage = CIImage(cgImage: cgImg).transformed(by: CGAffineTransform(translationX: deltaX, y: deltaY))
            let extent = CGRect(x: 0, y: 0, width: baseWidth, height: baseHeight)
            if let renderedCG = ciContext.createCGImage(ciImage, from: extent) {
                alignedImage = UIImage(cgImage: renderedCG)
            } else {
                alignedImage = image
            }
        } else {
            alignedImage = image
        }

        // Update crop rectangle intersection
        let minX = max(cropBounds.origin.x, deltaX > 0 ? deltaX : 0)
        let minY = max(cropBounds.origin.y, deltaY > 0 ? deltaY : 0)
        let maxX = min(cropBounds.maxX, deltaX < 0 ? baseWidth + deltaX : baseWidth)
        let maxY = min(cropBounds.maxY, deltaY < 0 ? baseHeight + deltaY : baseHeight)

        cropBounds = CGRect(
            x: minX,
            y: minY,
            width: max(20, maxX - minX),
            height: max(20, maxY - minY)
        )

        frameIndex += 1
        return AlignmentResult(
            alignedImage: alignedImage,
            deltaX: deltaX,
            deltaY: deltaY,
            cropRect: cropBounds
        )
    }

    public func reset() {
        referenceCVPixelBuffer = nil
        cropBounds = CGRect(x: 0, y: 0, width: baseWidth, height: baseHeight)
        frameIndex = 0
    }
}
