import Foundation
import SwiftUI
import Combine
import AVFoundation
import Photos

public enum ScreenState {
    case camera
    case processing
    case result
}

@MainActor
public final class CameraViewModel: ObservableObject {
    public let cameraService = CameraService()
    public let stabilityDetector = MotionStabilityDetector()
    public let proManager = ProSubscriptionManager.shared
    private let blenderEngine = FrameBlenderEngine()
    private let sceneDetector = HeuristicSceneDetector()
    private let videoWriter = CompanionVideoWriter()

    @Published public var screenState: ScreenState = .camera
    @Published public var selectedMode: LongExposureMode = .auto
    @Published public var detectedMode: LongExposureMode = .motionBlur
    @Published public var selectedDuration: CaptureDuration = .sec3
    @Published public var settings: AppSettingsState = AppSettingsState()
    @Published public var showSettings: Bool = false
    @Published public var showOnboarding: Bool = false
    @Published public var showPermissionRationale: Bool = false
    @Published public var isLimitedAccess: Bool = false

    @Published public var processingMessage: String = ""
    @Published public var processingProgress: Float = 0.0
    @Published public var resultImage: UIImage? = nil
    @Published public var burstKeyframes: [UIImage] = []
    @Published public var companionVideoURL: URL? = nil
    @Published public var shareSheetPresented: Bool = false
    @Published public var hintMessage: String? = nil

    private var frameAligner: FrameAligner?
    private var isFirstFrame = true
    private var effectiveModeAtCapture: LongExposureMode = .motionBlur
    private var hapticFeedback = UIImpactFeedbackGenerator(style: .medium)
    private var cancellables = Set<AnyCancellable>()

    public init() {
        setupSubscriptions()
        stabilityDetector.startMonitoring()
        checkCameraPermission()

        let seen = UserDefaults.standard.bool(forKey: "has_seen_onboarding")
        if !seen {
            self.showOnboarding = true
        }
    }

    private func setupSubscriptions() {
        cameraService.onFrameCaptured = { [weak self] image, pixelBuffer in
            Task { @MainActor in
                self?.handleCapturedFrame(image: image, pixelBuffer: pixelBuffer)
            }
        }

        cameraService.onPreviewPixelBuffer = { [weak self] pixelBuffer in
            Task { @MainActor in
                self?.handlePreviewPixelBuffer(pixelBuffer)
            }
        }

        cameraService.onBurstComplete = { [weak self] in
            Task { @MainActor in
                self?.handleBurstCompleted()
            }
        }

        NotificationCenter.default.publisher(for: UIApplication.didEnterBackgroundNotification)
            .sink { [weak self] _ in
                self?.handleAppBackgrounded()
            }
            .store(in: &cancellables)
    }

    public func dismissOnboarding() {
        UserDefaults.standard.set(true, forKey: "has_seen_onboarding")
        showOnboarding = false
    }

    public func openOnboarding() {
        showOnboarding = true
    }

    public func checkCameraPermission() {
        switch AVCaptureDevice.authorizationStatus(for: .video) {
        case .authorized:
            cameraService.configure()
        case .notDetermined:
            AVCaptureDevice.requestAccess(for: .video) { [weak self] granted in
                DispatchQueue.main.async {
                    if granted {
                        self?.cameraService.configure()
                    } else {
                        self?.showPermissionRationale = true
                    }
                }
            }
        case .denied, .restricted:
            showPermissionRationale = true
        @unknown default:
            break
        }

        // Check limited photo access
        let photoStatus = PhotoLibrarySaver.checkAuthorizationStatus()
        self.isLimitedAccess = (photoStatus == .limited)
    }

    private func handlePreviewPixelBuffer(_ pixelBuffer: CVPixelBuffer) {
        let stats = sceneDetector.analyzePixelBuffer(pixelBuffer)
        let mode = sceneDetector.classifyScene(stats: stats)
        if self.detectedMode != mode {
            self.detectedMode = mode
        }
    }

    public func selectLens(_ factor: CGFloat) {
        cameraService.setZoomFactor(factor)
    }

    public func flipCamera() {
        cameraService.flipCamera()
    }

    public func shutterPressed() {
        if cameraService.isCapturing { return }

        if selectedDuration.isProOnly && !proManager.isProUnlocked {
            hintMessage = String(format: NSLocalizedString("hint_pro_required", comment: ""), selectedDuration.rawValue)
            return
        }

        triggerHaptic()
        isFirstFrame = true
        frameAligner = nil
        hintMessage = nil
        burstKeyframes.removeAll()

        effectiveModeAtCapture = (selectedMode == .auto) ? detectedMode : selectedMode

        Task {
            await blenderEngine.reset()
        }

        cameraService.startBurst(maxDurationSeconds: selectedDuration.rawValue)
    }

    public func shutterReleased() {
        guard cameraService.isCapturing else { return }

        let elapsedMs = cameraService.stopBurst()
        if elapsedMs < 1000 {
            // Held less than minimum 1 second -> discard
            videoWriter.cancel()
            burstKeyframes.removeAll()
            Task {
                await blenderEngine.reset()
            }
            frameAligner?.reset()
            frameAligner = nil
            isFirstFrame = true
            hintMessage = NSLocalizedString("hint_hold_longer", comment: "")
        } else {
            handleBurstCompleted()
        }
    }

    public func shutterCanceled() {
        cancelCapture()
    }

    public func cancelCapture() {
        cameraService.cancelBurst()
        videoWriter.cancel()
        burstKeyframes.removeAll()
        Task {
            await blenderEngine.reset()
        }
        frameAligner?.reset()
        frameAligner = nil
        screenState = .camera
    }

    public func clearHintMessage() {
        hintMessage = nil
    }

    private func handleCapturedFrame(image: UIImage, pixelBuffer: CVPixelBuffer) {
        let width = image.size.width
        let height = image.size.height

        Task {
            if isFirstFrame {
                await blenderEngine.start(mode: effectiveModeAtCapture, width: Int(width), height: Int(height))
                frameAligner = FrameAligner(baseWidth: width, baseHeight: height)

                if settings.saveCompanionVideo {
                    let tempURL = FileManager.default.temporaryDirectory
                        .appendingPathComponent("spectreflow_\(Int(Date().timeIntervalSince1970)).mov")
                    try? videoWriter.startWriting(outputURL: tempURL, width: Int(width), height: Int(height))
                }
                isFirstFrame = false
            }

            let isTripod = stabilityDetector.stabilityState == .stable
            let alignment = frameAligner?.align(image: image, pixelBuffer: pixelBuffer, isStableTripod: isTripod)
            let frameToBlend = alignment?.alignedImage ?? image

            _ = await blenderEngine.addFrame(image: frameToBlend)

            if settings.saveCompanionVideo {
                videoWriter.appendPixelBuffer(pixelBuffer)
            }

            if burstKeyframes.count < 16 {
                burstKeyframes.append(frameToBlend)
            }
        }
    }

    private func handleBurstCompleted() {
        triggerHaptic()
        screenState = .processing
        processingMessage = NSLocalizedString("blending_frames", comment: "")
        processingProgress = 0.5

        Task {
            let crop = frameAligner?.cropBounds
            let finalImage = await blenderEngine.finalizeResult(cropBounds: crop)

            var videoURL: URL? = nil
            if settings.saveCompanionVideo {
                videoURL = await videoWriter.finishWriting()
            }

            self.companionVideoURL = videoURL
            self.resultImage = finalImage
            self.processingProgress = 1.0
            self.screenState = .result
        }
    }

    public func saveResult(image: UIImage, onSuccess: @escaping () -> Void, onError: @escaping (String) -> Void) {
        Task {
            do {
                try await PhotoLibrarySaver.saveImage(image, pairedVideoURL: companionVideoURL)
                onSuccess()
            } catch {
                onError(error.localizedDescription)
            }
        }
    }

    public func saveVideoOnly(onSuccess: @escaping () -> Void, onError: @escaping (String) -> Void) {
        guard let url = companionVideoURL else { return }
        Task {
            do {
                try await PhotoLibrarySaver.saveVideo(url)
                onSuccess()
            } catch {
                onError(error.localizedDescription)
            }
        }
    }

    public func exportLoopingWallpaper(onSuccess: @escaping () -> Void, onError: @escaping (String) -> Void) {
        guard !burstKeyframes.isEmpty else {
            onError("No frames available")
            return
        }
        triggerHaptic()
        let frames = burstKeyframes
        Task {
            do {
                let loopURL = FileManager.default.temporaryDirectory
                    .appendingPathComponent("spectreflow_loop_\(Int(Date().timeIntervalSince1970)).mov")
                let generatedURL = try await CompanionVideoWriter.createLoopingVideo(from: frames, outputURL: loopURL)
                try await PhotoLibrarySaver.saveVideo(generatedURL)
                await MainActor.run {
                    onSuccess()
                }
            } catch {
                await MainActor.run {
                    onError(error.localizedDescription)
                }
            }
        }
    }

    public func discardResult() {
        Task {
            await blenderEngine.reset()
        }
        resultImage = nil
        burstKeyframes.removeAll()
        companionVideoURL = nil
        screenState = .camera
    }

    public func selectMode(_ mode: LongExposureMode) {
        guard !cameraService.isCapturing else { return }
        triggerHaptic()
        selectedMode = mode
    }

    public func selectDuration(_ duration: CaptureDuration) {
        guard !cameraService.isCapturing else { return }
        triggerHaptic()
        selectedDuration = duration
    }

    public func triggerHaptic() {
        guard settings.hapticsEnabled else { return }
        hapticFeedback.impactOccurred()
    }

    private func handleAppBackgrounded() {
        if cameraService.isCapturing {
            cancelCapture()
        }
    }
}
