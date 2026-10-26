import Foundation
import AVFoundation
import CoreImage
import UIKit

public final class CameraService: NSObject, ObservableObject, AVCaptureVideoDataOutputSampleBufferDelegate {
    public let captureSession = AVCaptureSession()
    private let videoDataOutput = AVCaptureVideoDataOutput()
    private let sessionQueue = DispatchQueue(label: "com.spectreflow.sessionQueue")
    private let videoQueue = DispatchQueue(label: "com.spectreflow.videoQueue")

    private var videoDevice: AVCaptureDevice?
    private var videoInput: AVCaptureDeviceInput?

    @Published public var isReady: Bool = false
    @Published public var isCapturing: Bool = false
    @Published public var captureProgress: Float = 0.0
    @Published public var elapsedSeconds: Float = 0.0
    @Published public var supportedLenses: [CGFloat] = [1.0]
    @Published public var currentZoom: CGFloat = 1.0
    @Published public var isFrontCamera: Bool = false

    public var onFrameCaptured: ((UIImage, CVPixelBuffer) -> Void)?
    public var onPreviewPixelBuffer: ((CVPixelBuffer) -> Void)?
    public var onBurstComplete: (() -> Void)?

    private var burstTimer: Timer?
    private var targetDurationSeconds: Int = 3
    private var elapsedMilliseconds: Int = 0
    private var lastPreviewAnalysisTime: TimeInterval = 0

    // Hardware accelerated CIContext with Display P3 wide color gamut
    private let ciContext: CIContext = {
        let p3 = CGColorSpace(name: CGColorSpace.displayP3) ?? CGColorSpaceCreateDeviceRGB()
        return CIContext(options: [
            .workingColorSpace: p3,
            .outputColorSpace: p3
        ])
    }()

    public override init() {
        super.init()
    }

    public func configure() {
        sessionQueue.async { [weak self] in
            guard let self = self else { return }
            self.setupCamera(position: .back)
        }
    }

    public func flipCamera() {
        guard !isCapturing else { return }
        sessionQueue.async { [weak self] in
            guard let self = self else { return }
            let newPosition: AVCaptureDevice.Position = self.isFrontCamera ? .back : .front
            self.setupCamera(position: newPosition)
        }
    }

    public func setZoomFactor(_ factor: CGFloat) {
        guard let device = videoDevice else { return }
        do {
            try device.lockForConfiguration()
            let clamped = max(device.minAvailableVideoZoomFactor, min(factor, device.maxAvailableVideoZoomFactor))
            device.videoZoomFactor = clamped
            device.unlockForConfiguration()
            DispatchQueue.main.async {
                self.currentZoom = clamped
            }
        } catch {
            print("Failed to set zoom factor: \(error)")
        }
    }

    private func setupCamera(position: AVCaptureDevice.Position) {
        captureSession.beginConfiguration()
        captureSession.sessionPreset = .photo

        // Remove existing input
        if let currentInput = videoInput {
            captureSession.removeInput(currentInput)
        }

        // Setup camera device: Try triple or dual wide camera, fallback to wide angle
        var device: AVCaptureDevice?
        if position == .back {
            let discovery = AVCaptureDevice.DiscoverySession(
                deviceTypes: [.builtInTripleCamera, .builtInDualWideCamera, .builtInWideAngleCamera],
                mediaType: .video,
                position: .back
            )
            device = discovery.devices.first
        } else {
            device = AVCaptureDevice.default(.builtInWideAngleCamera, for: .video, position: .front)
        }

        guard let camera = device, let input = try? AVCaptureDeviceInput(device: camera) else {
            captureSession.commitConfiguration()
            return
        }

        self.videoDevice = camera
        self.videoInput = input

        if captureSession.canAddInput(input) {
            captureSession.addInput(input)
        }

        // Enable Display P3 Wide Color if supported
        do {
            try camera.lockForConfiguration()
            if camera.activeFormat.supportedColorSpaces.contains(.P3_D65) {
                camera.activeColorSpace = .P3_D65
            }
            camera.unlockForConfiguration()
        } catch {
            print("Could not set P3 color space: \(error)")
        }

        // Setup VideoDataOutput
        if captureSession.outputs.isEmpty {
            videoDataOutput.videoSettings = [
                kCVPixelBufferPixelFormatTypeKey as String: Int(kCVPixelFormatType_32BGRA)
            ]
            videoDataOutput.alwaysDiscardsLateVideoFrames = true
            videoDataOutput.setSampleBufferDelegate(self, queue: videoQueue)

            if captureSession.canAddOutput(videoDataOutput) {
                captureSession.addOutput(videoDataOutput)
            }
        }

        if let connection = videoDataOutput.connection(with: .video) {
            if connection.isVideoOrientationSupported {
                connection.videoOrientation = .portrait
            }
        }

        captureSession.commitConfiguration()

        if !captureSession.isRunning {
            captureSession.startRunning()
        }

        // Determine supported zoom levels
        var lenses: [CGFloat] = [1.0]
        if position == .back {
            let minZoom = camera.minAvailableVideoZoomFactor
            let maxZoom = camera.maxAvailableVideoZoomFactor
            let candidates: [CGFloat] = [0.5, 1.0, 2.0, 3.0]
            lenses = candidates.filter { $0 >= minZoom && $0 <= minZoom * 4.0 && $0 <= maxZoom }
            if lenses.isEmpty { lenses = [1.0] }
        }

        DispatchQueue.main.async {
            self.isReady = true
            self.isFrontCamera = (position == .front)
            self.supportedLenses = lenses
            self.currentZoom = camera.videoZoomFactor
        }
    }

    public func startBurst(maxDurationSeconds: Int) {
        guard !isCapturing else { return }

        lockCameraParameters()

        self.targetDurationSeconds = maxDurationSeconds
        self.elapsedMilliseconds = 0
        self.isCapturing = true
        self.captureProgress = 0.0
        self.elapsedSeconds = 0.0

        let startTime = CACurrentMediaTime()

        DispatchQueue.main.async {
            self.burstTimer = Timer.scheduledTimer(withTimeInterval: 0.05, repeats: true) { [weak self] timer in
                guard let self = self, self.isCapturing else { return }
                let elapsedSec = Float(CACurrentMediaTime() - startTime)
                self.elapsedMilliseconds = Int(elapsedSec * 1000)
                self.elapsedSeconds = elapsedSec

                let totalMs = Float(self.targetDurationSeconds * 1000)
                self.captureProgress = min(1.0, Float(self.elapsedMilliseconds) / totalMs)

                if self.elapsedMilliseconds >= self.targetDurationSeconds * 1000 {
                    _ = self.stopBurst()
                    self.onBurstComplete?()
                }
            }
        }
    }

    @discardableResult
    public func stopBurst() -> Int {
        guard isCapturing else { return 0 }
        isCapturing = false

        burstTimer?.invalidate()
        burstTimer = nil
        unlockCameraParameters()

        let elapsed = elapsedMilliseconds
        return elapsed
    }

    public func cancelBurst() {
        guard isCapturing else { return }
        isCapturing = false

        burstTimer?.invalidate()
        burstTimer = nil
        unlockCameraParameters()
        elapsedMilliseconds = 0
        elapsedSeconds = 0.0
        captureProgress = 0.0
    }

    // MARK: - Hardware Sensor Locking
    private func lockCameraParameters() {
        guard let device = videoDevice else { return }
        do {
            try device.lockForConfiguration()

            if device.isFocusModeSupported(.locked) {
                device.focusMode = .locked
            }
            if device.isExposureModeSupported(.locked) {
                device.exposureMode = .locked
            }
            if device.isWhiteBalanceModeSupported(.locked) {
                device.whiteBalanceMode = .locked
            }

            device.unlockForConfiguration()
        } catch {
            print("Failed to lock camera configuration: \(error)")
        }
    }

    private func unlockCameraParameters() {
        guard let device = videoDevice else { return }
        do {
            try device.lockForConfiguration()

            if device.isFocusModeSupported(.continuousAutoFocus) {
                device.focusMode = .continuousAutoFocus
            }
            if device.isExposureModeSupported(.continuousAutoExposure) {
                device.exposureMode = .continuousAutoExposure
            }
            if device.isWhiteBalanceModeSupported(.continuousAutoWhiteBalance) {
                device.whiteBalanceMode = .continuousAutoWhiteBalance
            }

            device.unlockForConfiguration()
        } catch {
            print("Failed to restore camera configuration: \(error)")
        }
    }

    // MARK: - AVCaptureVideoDataOutputSampleBufferDelegate
    public func captureOutput(
        _ output: AVCaptureOutput,
        didOutput sampleBuffer: CMSampleBuffer,
        from connection: AVCaptureConnection
    ) {
        guard let pixelBuffer = CMSampleBufferGetImageBuffer(sampleBuffer) else { return }

        // Send preview buffer to auto scene detector if idle
        if !isCapturing {
            let now = CACurrentMediaTime()
            if now - lastPreviewAnalysisTime > 0.35 {
                lastPreviewAnalysisTime = now
                onPreviewPixelBuffer?(pixelBuffer)
            }
            return
        }

        let ciImage = CIImage(cvPixelBuffer: pixelBuffer)
        guard let cgImage = ciContext.createCGImage(ciImage, from: ciImage.extent) else { return }

        let image = UIImage(cgImage: cgImage)
        onFrameCaptured?(image, pixelBuffer)
    }

    public func stopSession() {
        cancelBurst()
        sessionQueue.async { [weak self] in
            if self?.captureSession.isRunning == true {
                self?.captureSession.stopRunning()
            }
        }
    }
}
