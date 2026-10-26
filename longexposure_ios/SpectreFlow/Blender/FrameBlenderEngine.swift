import Foundation
import UIKit
import CoreGraphics
import CoreImage
import Metal
import MetalKit

/// Hardware-accelerated Metal Compute Shader Engine for Long Exposure Blending.
/// Executes frame accumulation kernels directly on the device GPU (Apple Silicon / A-series / M-series).
public final class MetalBlenderEngine {
    private let device: MTLDevice
    private let commandQueue: MTLCommandQueue
    private var meanAccumulatePipeline: MTLComputePipelineState?
    private var meanFinalizePipeline: MTLComputePipelineState?
    private var lightTrailsAccumulatePipeline: MTLComputePipelineState?

    private var accumulatorBuffer: MTLBuffer?
    private var lightTrailsTextureA: MTLTexture?
    private var lightTrailsTextureB: MTLTexture?
    private var isUsingTextureAAsInput = true

    public private(set) var frameCount: Int = 0
    public let width: Int
    public let height: Int
    public let mode: LongExposureMode

    public init?(width: Int, height: Int, mode: LongExposureMode) {
        guard let mtlDevice = MTLCreateSystemDefaultDevice(),
              let queue = mtlDevice.makeCommandQueue() else {
            return nil
        }
        self.device = mtlDevice
        self.commandQueue = queue
        self.width = width
        self.height = height
        self.mode = mode

        // Metal compute shader source
        let metalSource = """
        #include <metal_stdlib>
        using namespace metal;

        kernel void meanAccumulateKernel(
            texture2d<float, access::read> currentFrame [[texture(0)]],
            device float4* accumulatorBuffer [[buffer(0)]],
            uint2 gid [[thread_position_in_grid]]
        ) {
            if (gid.x >= currentFrame.get_width() || gid.y >= currentFrame.get_height()) {
                return;
            }
            uint index = gid.y * currentFrame.get_width() + gid.x;
            float4 pixel = currentFrame.read(gid);
            accumulatorBuffer[index] += pixel;
        }

        kernel void meanFinalizeKernel(
            const device float4* accumulatorBuffer [[buffer(0)]],
            texture2d<float, access::write> outputTexture [[texture(0)]],
            constant uint& frameCount [[buffer(1)]],
            uint2 gid [[thread_position_in_grid]]
        ) {
            if (gid.x >= outputTexture.get_width() || gid.y >= outputTexture.get_height()) {
                return;
            }
            uint index = gid.y * outputTexture.get_width() + gid.x;
            float4 sum = accumulatorBuffer[index];
            float4 meanColor = sum / float(frameCount);
            meanColor.a = 1.0;
            outputTexture.write(meanColor, gid);
        }

        kernel void lightTrailsAccumulateKernel(
            texture2d<float, access::read> currentFrame [[texture(0)]],
            texture2d<float, access::read> previousAcc [[texture(1)]],
            texture2d<float, access::write> outputTexture [[texture(2)]],
            uint2 gid [[thread_position_in_grid]]
        ) {
            if (gid.x >= currentFrame.get_width() || gid.y >= currentFrame.get_height()) {
                return;
            }
            float4 current = currentFrame.read(gid);
            float4 prev = previousAcc.read(gid);
            float4 maxColor = float4(
                max(current.r, prev.r),
                max(current.g, prev.g),
                max(current.b, prev.b),
                1.0
            );
            outputTexture.write(maxColor, gid);
        }
        """

        let library: MTLLibrary
        if let defaultLib = mtlDevice.makeDefaultLibrary(),
           defaultLib.makeFunction(name: "meanAccumulateKernel") != nil {
            library = defaultLib
        } else if let compiledLib = try? mtlDevice.makeLibrary(source: metalSource, options: nil) {
            library = compiledLib
        } else {
            return nil
        }

        guard let meanAccFn = library.makeFunction(name: "meanAccumulateKernel"),
              let meanFinFn = library.makeFunction(name: "meanFinalizeKernel"),
              let lightTrailsFn = library.makeFunction(name: "lightTrailsAccumulateKernel") else {
            return nil
        }

        do {
            self.meanAccumulatePipeline = try mtlDevice.makeComputePipelineState(function: meanAccFn)
            self.meanFinalizePipeline = try mtlDevice.makeComputePipelineState(function: meanFinFn)
            self.lightTrailsAccumulatePipeline = try mtlDevice.makeComputePipelineState(function: lightTrailsFn)
        } catch {
            return nil
        }

        if mode == .motionBlur || mode == .auto {
            let bufferSize = width * height * MemoryLayout<SIMD4<Float>>.stride
            guard let buf = mtlDevice.makeBuffer(length: bufferSize, options: .storageModeShared) else {
                return nil
            }
            memset(buf.contents(), 0, bufferSize)
            self.accumulatorBuffer = buf
        } else if mode == .lightTrails {
            let desc = MTLTextureDescriptor.texture2DDescriptor(
                pixelFormat: .rgba8Unorm,
                width: width,
                height: height,
                mipmapped: false
            )
            desc.usage = [.shaderRead, .shaderWrite]
            self.lightTrailsTextureA = mtlDevice.makeTexture(descriptor: desc)
            self.lightTrailsTextureB = mtlDevice.makeTexture(descriptor: desc)
            if self.lightTrailsTextureA == nil || self.lightTrailsTextureB == nil {
                return nil
            }
        }
    }

    public func addFrame(cgImage: CGImage) -> Int {
        guard let commandBuffer = commandQueue.makeCommandBuffer(),
              let encoder = commandBuffer.makeComputeCommandEncoder() else {
            return frameCount
        }

        let desc = MTLTextureDescriptor.texture2DDescriptor(
            pixelFormat: .rgba8Unorm,
            width: width,
            height: height,
            mipmapped: false
        )
        desc.usage = [.shaderRead]
        guard let inputTexture = device.makeTexture(descriptor: desc) else {
            return frameCount
        }

        var rawBytes = [UInt32](repeating: 0, count: width * height)
        let colorSpace = CGColorSpace(name: CGColorSpace.displayP3) ?? CGColorSpaceCreateDeviceRGB()
        let bitmapInfo = CGBitmapInfo.byteOrder32Little.rawValue | CGImageAlphaInfo.premultipliedFirst.rawValue
        guard let ctx = CGContext(
            data: &rawBytes,
            width: width,
            height: height,
            bitsPerComponent: 8,
            bytesPerRow: width * 4,
            space: colorSpace,
            bitmapInfo: bitmapInfo
        ) else { return frameCount }
        ctx.draw(cgImage, in: CGRect(x: 0, y: 0, width: width, height: height))

        inputTexture.replace(
            region: MTLRegionMake2D(0, 0, width, height),
            mipmapLevel: 0,
            withBytes: rawBytes,
            bytesPerRow: width * 4
        )

        let threadgroupSize = MTLSize(width: 16, height: 16, depth: 1)
        let threadgroups = MTLSize(
            width: (width + 15) / 16,
            height: (height + 15) / 16,
            depth: 1
        )

        if mode == .motionBlur || mode == .auto {
            guard let pipeline = meanAccumulatePipeline, let accBuf = accumulatorBuffer else { return frameCount }
            encoder.setComputePipelineState(pipeline)
            encoder.setTexture(inputTexture, index: 0)
            encoder.setBuffer(accBuf, offset: 0, index: 0)
            encoder.dispatchThreadgroups(threadgroups, threadsPerThreadgroup: threadgroupSize)
            encoder.endEncoding()
            commandBuffer.commit()
            commandBuffer.waitUntilCompleted()
            frameCount += 1
            return frameCount
        } else if mode == .lightTrails {
            guard let pipeline = lightTrailsAccumulatePipeline,
                  let texA = lightTrailsTextureA,
                  let texB = lightTrailsTextureB else { return frameCount }

            if frameCount == 0 {
                guard let blitEncoder = commandBuffer.makeBlitCommandEncoder() else { return frameCount }
                blitEncoder.copy(
                    from: inputTexture, sourceSlice: 0, sourceLevel: 0, sourceOrigin: MTLOriginMake(0, 0, 0), sourceSize: MTLSizeMake(width, height, 1),
                    to: texA, destinationSlice: 0, destinationLevel: 0, destinationOrigin: MTLOriginMake(0, 0, 0)
                )
                blitEncoder.endEncoding()
                commandBuffer.commit()
                commandBuffer.waitUntilCompleted()
                isUsingTextureAAsInput = true
                frameCount += 1
                return frameCount
            }

            let sourceTex = isUsingTextureAAsInput ? texA : texB
            let destTex = isUsingTextureAAsInput ? texB : texA

            encoder.setComputePipelineState(pipeline)
            encoder.setTexture(inputTexture, index: 0)
            encoder.setTexture(sourceTex, index: 1)
            encoder.setTexture(destTex, index: 2)
            encoder.dispatchThreadgroups(threadgroups, threadsPerThreadgroup: threadgroupSize)
            encoder.endEncoding()
            commandBuffer.commit()
            commandBuffer.waitUntilCompleted()

            isUsingTextureAAsInput.toggle()
            frameCount += 1
            return frameCount
        }

        return frameCount
    }

    public func finalizeResult() -> CGImage? {
        guard frameCount > 0 else { return nil }

        let colorSpace = CGColorSpace(name: CGColorSpace.displayP3) ?? CGColorSpaceCreateDeviceRGB()
        let bitmapInfo = CGBitmapInfo.byteOrder32Little.rawValue | CGImageAlphaInfo.premultipliedFirst.rawValue

        if mode == .motionBlur || mode == .auto {
            guard let commandBuffer = commandQueue.makeCommandBuffer(),
                  let encoder = commandBuffer.makeComputeCommandEncoder(),
                  let pipeline = meanFinalizePipeline,
                  let accBuf = accumulatorBuffer else { return nil }

            let desc = MTLTextureDescriptor.texture2DDescriptor(
                pixelFormat: .rgba8Unorm,
                width: width,
                height: height,
                mipmapped: false
            )
            desc.usage = [.shaderRead, .shaderWrite]
            guard let outputTex = device.makeTexture(descriptor: desc) else { return nil }

            var count = UInt32(frameCount)
            encoder.setComputePipelineState(pipeline)
            encoder.setBuffer(accBuf, offset: 0, index: 0)
            encoder.setTexture(outputTex, index: 0)
            encoder.setBytes(&count, length: MemoryLayout<UInt32>.size, index: 1)

            let threadgroupSize = MTLSize(width: 16, height: 16, depth: 1)
            let threadgroups = MTLSize(
                width: (width + 15) / 16,
                height: (height + 15) / 16,
                depth: 1
            )
            encoder.dispatchThreadgroups(threadgroups, threadsPerThreadgroup: threadgroupSize)
            encoder.endEncoding()
            commandBuffer.commit()
            commandBuffer.waitUntilCompleted()

            var outputBytes = [UInt32](repeating: 0, count: width * height)
            outputTex.getBytes(
                &outputBytes,
                bytesPerRow: width * 4,
                from: MTLRegionMake2D(0, 0, width, height),
                mipmapLevel: 0
            )

            guard let ctx = CGContext(
                data: &outputBytes,
                width: width,
                height: height,
                bitsPerComponent: 8,
                bytesPerRow: width * 4,
                space: colorSpace,
                bitmapInfo: bitmapInfo
            ) else { return nil }
            return ctx.makeImage()
        } else if mode == .lightTrails {
            let finalTex = isUsingTextureAAsInput ? lightTrailsTextureA : lightTrailsTextureB
            guard let tex = finalTex else { return nil }

            var outputBytes = [UInt32](repeating: 0, count: width * height)
            tex.getBytes(
                &outputBytes,
                bytesPerRow: width * 4,
                from: MTLRegionMake2D(0, 0, width, height),
                mipmapLevel: 0
            )

            guard let ctx = CGContext(
                data: &outputBytes,
                width: width,
                height: height,
                bitsPerComponent: 8,
                bytesPerRow: width * 4,
                space: colorSpace,
                bitmapInfo: bitmapInfo
            ) else { return nil }
            return ctx.makeImage()
        }

        return nil
    }

    public func reset() {
        accumulatorBuffer = nil
        lightTrailsTextureA = nil
        lightTrailsTextureB = nil
        frameCount = 0
    }
}

/// High-level Swift Long Exposure Frame Blending Engine.
/// Coordinates multi-frame ingestion, executes Metal GPU compute shaders (with CPU fallback),
/// and applies border cropping after homography/translational shifts.
public actor FrameBlenderEngine {
    private var metalEngine: MetalBlenderEngine?
    private var meanBlender: MeanBlender?
    private var lightTrailsBlender: LightTrailsBlender?
    private var medianBlender: MedianBlender?

    private var currentMode: LongExposureMode = .motionBlur
    private var targetWidth: Int = 0
    private var targetHeight: Int = 0

    public init() {}

    public func start(mode: LongExposureMode, width: Int, height: Int) {
        self.currentMode = mode
        self.targetWidth = width
        self.targetHeight = height

        // First attempt hardware-accelerated Metal compute shaders for Motion Blur and Light Trails
        if mode != .vanish {
            self.metalEngine = MetalBlenderEngine(width: width, height: height, mode: mode)
        } else {
            self.metalEngine = nil
        }

        // If Metal is initialized, CPU blenders are kept as backup; if Vanish or Metal unavailable, use CPU
        if metalEngine == nil {
            switch mode {
            case .auto, .motionBlur:
                self.meanBlender = MeanBlender(width: width, height: height)
                self.lightTrailsBlender = nil
                self.medianBlender = nil
            case .lightTrails:
                self.lightTrailsBlender = LightTrailsBlender(width: width, height: height)
                self.meanBlender = nil
                self.medianBlender = nil
            case .vanish:
                self.medianBlender = MedianBlender(width: width, height: height)
                self.meanBlender = nil
                self.lightTrailsBlender = nil
            }
        } else {
            self.meanBlender = nil
            self.lightTrailsBlender = nil
            self.medianBlender = nil
        }
    }

    public func addFrame(image: UIImage) -> Int {
        guard let cgImage = image.cgImage else { return 0 }

        if let metal = metalEngine {
            return metal.addFrame(cgImage: cgImage)
        }

        guard let pixels = extractRGBA(from: cgImage) else { return 0 }

        switch currentMode {
        case .auto, .motionBlur:
            meanBlender?.addFrame(rgbaPixels: pixels)
            return meanBlender?.frameCount ?? 0
        case .lightTrails:
            lightTrailsBlender?.addFrame(rgbaPixels: pixels)
            return lightTrailsBlender?.frameCount ?? 0
        case .vanish:
            medianBlender?.addFrame(rgbaPixels: pixels)
            return medianBlender?.totalObservedFrames ?? 0
        }
    }

    public func finalizeResult(cropBounds: CGRect? = nil) -> UIImage? {
        let rawCGImage: CGImage?

        if let metal = metalEngine {
            rawCGImage = metal.finalizeResult()
        } else {
            let pixels: [UInt32]
            switch currentMode {
            case .auto, .motionBlur:
                pixels = meanBlender?.finalizeOutput() ?? []
            case .lightTrails:
                pixels = lightTrailsBlender?.finalizeOutput() ?? []
            case .vanish:
                pixels = medianBlender?.finalizeOutput() ?? []
            }

            guard !pixels.isEmpty, targetWidth > 0, targetHeight > 0 else { return nil }
            rawCGImage = createCGImage(from: pixels, width: targetWidth, height: targetHeight)
        }

        guard let finalCG = rawCGImage else { return nil }

        if let crop = cropBounds, crop.width > 20, crop.height > 20 {
            let safeX = max(0, min(crop.origin.x, CGFloat(targetWidth - 1)))
            let safeY = max(0, min(crop.origin.y, CGFloat(targetHeight - 1)))
            let safeW = min(crop.width, CGFloat(targetWidth) - safeX)
            let safeH = min(crop.height, CGFloat(targetHeight) - safeY)
            let validCrop = CGRect(x: safeX, y: safeY, width: safeW, height: safeH)

            if let cropped = finalCG.cropping(to: validCrop) {
                return UIImage(cgImage: cropped)
            }
        }

        return UIImage(cgImage: finalCG)
    }

    public func reset() {
        metalEngine?.reset()
        metalEngine = nil
        meanBlender?.reset()
        lightTrailsBlender?.reset()
        medianBlender?.reset()
        meanBlender = nil
        lightTrailsBlender = nil
        medianBlender = nil
    }

    // MARK: - Pixel Buffer Utilities
    private func extractRGBA(from cgImage: CGImage) -> [UInt32]? {
        let width = cgImage.width
        let height = cgImage.height
        var pixels = [UInt32](repeating: 0, count: width * height)
        let colorSpace = CGColorSpace(name: CGColorSpace.displayP3) ?? CGColorSpaceCreateDeviceRGB()
        let bitmapInfo = CGBitmapInfo.byteOrder32Little.rawValue | CGImageAlphaInfo.premultipliedFirst.rawValue

        guard let context = CGContext(
            data: &pixels,
            width: width,
            height: height,
            bitsPerComponent: 8,
            bytesPerRow: width * 4,
            space: colorSpace,
            bitmapInfo: bitmapInfo
        ) else { return nil }

        context.draw(cgImage, in: CGRect(x: 0, y: 0, width: width, height: height))
        return pixels
    }

    private func createCGImage(from pixels: [UInt32], width: Int, height: Int) -> CGImage? {
        var mutablePixels = pixels
        let colorSpace = CGColorSpace(name: CGColorSpace.displayP3) ?? CGColorSpaceCreateDeviceRGB()
        let bitmapInfo = CGBitmapInfo.byteOrder32Little.rawValue | CGImageAlphaInfo.premultipliedFirst.rawValue

        guard let context = CGContext(
            data: &mutablePixels,
            width: width,
            height: height,
            bitsPerComponent: 8,
            bytesPerRow: width * 4,
            space: colorSpace,
            bitmapInfo: bitmapInfo
        ) else { return nil }

        return context.makeImage()
    }
}
