import Foundation
import AVFoundation
import CoreVideo
import UIKit

public final class CompanionVideoWriter {
    private var assetWriter: AVAssetWriter?
    private var writerInput: AVAssetWriterInput?
    private var adaptor: AVAssetWriterInputPixelBufferAdaptor?
    private var frameCount: Int64 = 0
    private let fps: Int32 = 15
    private var outputURL: URL?

    public init() {}

    public func startWriting(outputURL: URL, width: Int, height: Int) throws {
        self.outputURL = outputURL
        self.frameCount = 0

        if FileManager.default.fileExists(atPath: outputURL.path) {
            try? FileManager.default.removeItem(at: outputURL)
        }

        let writer = try AVAssetWriter(outputURL: outputURL, fileType: .mov)

        let videoSettings: [String: Any] = [
            AVVideoCodecKey: AVVideoCodecType.h264,
            AVVideoWidthKey: width,
            AVVideoHeightKey: height
        ]

        let input = AVAssetWriterInput(mediaType: .video, outputSettings: videoSettings)
        input.expectsMediaDataInRealTime = true

        let sourcePixelBufferAttributes: [String: Any] = [
            kCVPixelBufferPixelFormatTypeKey as String: Int(kCVPixelFormatType_32BGRA),
            kCVPixelBufferWidthKey as String: width,
            kCVPixelBufferHeightKey as String: height
        ]

        let adaptor = AVAssetWriterInputPixelBufferAdaptor(
            assetWriterInput: input,
            sourcePixelBufferAttributes: sourcePixelBufferAttributes
        )

        if writer.canAdd(input) {
            writer.add(input)
        }

        writer.startWriting()
        writer.startSession(atSourceTime: .zero)

        self.assetWriter = writer
        self.writerInput = input
        self.adaptor = adaptor
    }

    public func appendPixelBuffer(_ pixelBuffer: CVPixelBuffer) {
        guard let writer = assetWriter, writer.status == .writing,
              let input = writerInput, input.isReadyForMoreMediaData,
              let adaptor = adaptor else {
            return
        }

        let presentationTime = CMTime(value: frameCount, timescale: fps)
        adaptor.append(pixelBuffer, withPresentationTime: presentationTime)
        frameCount += 1
    }

    public func finishWriting() async -> URL? {
        guard let writer = assetWriter, let input = writerInput else { return nil }

        input.markAsFinished()
        await writer.finishWriting()

        self.assetWriter = nil
        self.writerInput = nil
        self.adaptor = nil

        return outputURL
    }

    public func cancel() {
        assetWriter?.cancelWriting()
        assetWriter = nil
        writerInput = nil
        adaptor = nil
        if let url = outputURL {
            try? FileManager.default.removeItem(at: url)
        }
        outputURL = nil
    }

    /// Generates a smooth ping-pong (boomerang) looping video suitable for Live Wallpapers and animated sharing.
    public static func createLoopingVideo(from keyframes: [UIImage], outputURL: URL, fps: Int32 = 24) async throws -> URL {
        guard !keyframes.isEmpty else {
            throw NSError(domain: "com.spectreflow", code: 400, userInfo: [NSLocalizedDescriptionKey: "No keyframes to loop"])
        }
        if FileManager.default.fileExists(atPath: outputURL.path) {
            try? FileManager.default.removeItem(at: outputURL)
        }

        guard let firstCG = keyframes.first?.cgImage else {
            throw NSError(domain: "com.spectreflow", code: 400, userInfo: [NSLocalizedDescriptionKey: "Invalid keyframe data"])
        }

        let width = (firstCG.width / 2) * 2
        let height = (firstCG.height / 2) * 2

        let writer = try AVAssetWriter(outputURL: outputURL, fileType: .mov)
        let videoSettings: [String: Any] = [
            AVVideoCodecKey: AVVideoCodecType.h264,
            AVVideoWidthKey: width,
            AVVideoHeightKey: height
        ]

        let input = AVAssetWriterInput(mediaType: .video, outputSettings: videoSettings)
        input.expectsMediaDataInRealTime = false

        let attributes: [String: Any] = [
            kCVPixelBufferPixelFormatTypeKey as String: Int(kCVPixelFormatType_32BGRA),
            kCVPixelBufferWidthKey as String: width,
            kCVPixelBufferHeightKey as String: height
        ]

        let adaptor = AVAssetWriterInputPixelBufferAdaptor(
            assetWriterInput: input,
            sourcePixelBufferAttributes: attributes
        )

        if writer.canAdd(input) {
            writer.add(input)
        }

        writer.startWriting()
        writer.startSession(atSourceTime: .zero)

        // Ping-pong sequence: forward 0..N-1, backward N-2..1
        var sequence: [UIImage] = []
        for i in 0..<keyframes.count {
            sequence.append(keyframes[i])
        }
        if keyframes.count > 2 {
            for i in stride(from: keyframes.count - 2, to: 0, by: -1) {
                sequence.append(keyframes[i])
            }
        }
        // Repeat loop twice for seamless motion
        let fullLoop = sequence + sequence

        var frameIdx: Int64 = 0
        for img in fullLoop {
            while !input.isReadyForMoreMediaData {
                try? await Task.sleep(nanoseconds: 10_000_000)
            }
            if let pixelBuf = createPixelBuffer(from: img, width: width, height: height) {
                let time = CMTime(value: frameIdx, timescale: fps)
                adaptor.append(pixelBuf, withPresentationTime: time)
                frameIdx += 1
            }
        }

        input.markAsFinished()
        await writer.finishWriting()
        return outputURL
    }

    private static func createPixelBuffer(from image: UIImage, width: Int, height: Int) -> CVPixelBuffer? {
        guard let cgImage = image.cgImage else { return nil }

        var pixelBuffer: CVPixelBuffer?
        let attributes: [CFString: Any] = [
            kCVPixelBufferCGImageCompatibilityKey: true,
            kCVPixelBufferCGBitmapContextCompatibilityKey: true
        ]
        let status = CVPixelBufferCreate(
            kCFAllocatorDefault,
            width,
            height,
            kCVPixelFormatType_32BGRA,
            attributes as CFDictionary,
            &pixelBuffer
        )

        guard status == kCVReturnSuccess, let buffer = pixelBuffer else { return nil }

        CVPixelBufferLockBaseAddress(buffer, [])
        defer { CVPixelBufferUnlockBaseAddress(buffer, []) }

        let context = CGContext(
            data: CVPixelBufferGetBaseAddress(buffer),
            width: width,
            height: height,
            bitsPerComponent: 8,
            bytesPerRow: CVPixelBufferGetBytesPerRow(buffer),
            space: CGColorSpaceCreateDeviceRGB(),
            bitmapInfo: CGImageAlphaInfo.premultipliedFirst.rawValue | CGBitmapInfo.byteOrder32Little.rawValue
        )

        context?.draw(cgImage, in: CGRect(x: 0, y: 0, width: width, height: height))
        return buffer
    }
}
