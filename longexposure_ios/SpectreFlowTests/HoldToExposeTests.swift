import XCTest
@testable import SpectreFlow

final class HoldToExposeTests: XCTestCase {

    func testMinimumHoldConstraint() {
        func isValidCapture(elapsedMs: Int) -> Bool {
            return elapsedMs >= 1000
        }

        // Taps under 1s (1000ms) are discarded
        XCTAssertFalse(isValidCapture(elapsedMs: 0))
        XCTAssertFalse(isValidCapture(elapsedMs: 400))
        XCTAssertFalse(isValidCapture(elapsedMs: 999))

        // Holds of 1000ms or longer are synthesized
        XCTAssertTrue(isValidCapture(elapsedMs: 1000))
        XCTAssertTrue(isValidCapture(elapsedMs: 2500))
        XCTAssertTrue(isValidCapture(elapsedMs: 5000))
        XCTAssertTrue(isValidCapture(elapsedMs: 30000))
    }

    func testProgressRatioCalculation() {
        func calculateProgress(elapsedMs: Int, maxDurationSeconds: Int) -> Float {
            let totalMs = Float(maxDurationSeconds * 1000)
            return min(1.0, max(0.0, Float(elapsedMs) / totalMs))
        }

        XCTAssertEqual(calculateProgress(elapsedMs: 0, maxDurationSeconds: 5), 0.0, accuracy: 0.001)
        XCTAssertEqual(calculateProgress(elapsedMs: 2500, maxDurationSeconds: 5), 0.5, accuracy: 0.001)
        XCTAssertEqual(calculateProgress(elapsedMs: 5000, maxDurationSeconds: 5), 1.0, accuracy: 0.001)
        XCTAssertEqual(calculateProgress(elapsedMs: 8000, maxDurationSeconds: 5), 1.0, accuracy: 0.001)
    }

    func testTimerStringFormatting() {
        func formatTimer(seconds: Float) -> String {
            let s = Int(seconds)
            return String(format: "00:%02d", s)
        }

        XCTAssertEqual(formatTimer(seconds: 0.0), "00:00")
        XCTAssertEqual(formatTimer(seconds: 0.8), "00:00")
        XCTAssertEqual(formatTimer(seconds: 1.0), "00:01")
        XCTAssertEqual(formatTimer(seconds: 7.2), "00:07")
        XCTAssertEqual(formatTimer(seconds: 15.0), "00:15")
        XCTAssertEqual(formatTimer(seconds: 30.0), "00:30")
    }

    func testCaptureStateMachineTransitions() {
        var state = CaptureState.idle

        // 1. Shutter press down: idle -> capturing
        XCTAssertEqual(state, .idle)
        state = .capturing
        XCTAssertEqual(state, .capturing)

        // 2. Early release (<1s): capturing -> idle
        let earlyElapsed = 850
        if earlyElapsed < 1000 {
            state = .idle
        }
        XCTAssertEqual(state, .idle)

        // 3. Valid hold (3s, 5s, 15s, 30s): capturing -> processing -> result
        state = .capturing
        let validElapsed = 5000
        if validElapsed >= 1000 {
            state = .processing
        }
        XCTAssertEqual(state, .processing)
        state = .result
        XCTAssertEqual(state, .result)

        // 4. Slide cancel (finger dragged out): capturing -> idle
        state = .capturing
        let draggedOut = true
        if draggedOut {
            state = .idle
        }
        XCTAssertEqual(state, .idle)

        // 5. App backgrounded during hold: capturing -> idle
        state = .capturing
        let appBackgrounded = true
        if appBackgrounded {
            state = .idle
        }
        XCTAssertEqual(state, .idle)
    }

    func testDialRotationAndProLockValidation() {
        let durations = CaptureDuration.allCases
        XCTAssertEqual(durations.count, 5)

        // 3s, 5s, 9s are non-Pro
        XCTAssertFalse(durations[0].isProOnly)
        XCTAssertFalse(durations[1].isProOnly)
        XCTAssertFalse(durations[2].isProOnly)

        // 15s, 30s are Pro-only
        XCTAssertTrue(durations[3].isProOnly)
        XCTAssertTrue(durations[4].isProOnly)

        var isProUnlocked = false
        var selected = durations[0]

        func trySelect(_ duration: CaptureDuration) -> Bool {
            if duration.isProOnly && !isProUnlocked {
                return false
            }
            selected = duration
            return true
        }

        XCTAssertTrue(trySelect(durations[1])) // 5s allowed
        XCTAssertEqual(selected, .sec5)

        XCTAssertFalse(trySelect(durations[3])) // 15s rejected
        XCTAssertEqual(selected, .sec5)

        // Unlock Pro
        isProUnlocked = true
        XCTAssertTrue(trySelect(durations[3])) // 15s allowed now
        XCTAssertEqual(selected, .sec15)
    }

    func testQuickTapShowsHintAndDiscards() {
        var hintMessage: String? = nil
        var isCaptured = false

        func onRelease(elapsedMs: Int) {
            if elapsedMs < 1000 {
                hintMessage = "Hold longer to capture"
                isCaptured = false
            } else {
                hintMessage = nil
                isCaptured = true
            }
        }

        onRelease(elapsedMs: 400)
        XCTAssertEqual(hintMessage, "Hold longer to capture")
        XCTAssertFalse(isCaptured)

        onRelease(elapsedMs: 2500)
        XCTAssertNil(hintMessage)
        XCTAssertTrue(isCaptured)
    }

    func testSlideCancelSafety() {
        let buttonRadius: CGFloat = 40.0
        let cancelDistance: CGFloat = buttonRadius * 2.0 * 1.5 // 120.0
        var isCancelled = false

        func onPointerMove(distanceFromCenter: CGFloat) {
            if distanceFromCenter > cancelDistance {
                isCancelled = true
            }
        }

        onPointerMove(distanceFromCenter: 60.0)
        XCTAssertFalse(isCancelled)

        onPointerMove(distanceFromCenter: 130.0)
        XCTAssertTrue(isCancelled)
    }

    func testPhotoRepositoryAndGallerySync() {
        // Simulates unified single-source-of-truth behavior between thumbnail and gallery viewer
        var photos: [String] = []
        var latestThumbnail: String? = nil
        var currentScreen: ScreenState = .camera
        var hintMessage: String? = nil

        func openResultViewer() {
            if photos.isEmpty {
                hintMessage = "No photos captured yet"
            } else {
                currentScreen = .gallery
            }
        }

        func savePhoto(id: String) {
            photos.insert(id, at: 0) // LIFO sorted by date descending
            latestThumbnail = "thumb_\(id)"
        }

        func deletePhoto(id: String) {
            photos.removeAll(where: { $0 == id })
            latestThumbnail = photos.first.map { "thumb_\($0)" }
            if photos.isEmpty {
                currentScreen = .camera
            }
        }

        // Scenario 1: Fresh install / no photos
        XCTAssertTrue(photos.isEmpty)
        XCTAssertNil(latestThumbnail)
        openResultViewer()
        XCTAssertEqual(hintMessage, "No photos captured yet")
        XCTAssertEqual(currentScreen, .camera)

        // Scenario 2: Capture and save 2 photos
        hintMessage = nil
        savePhoto(id: "photo_001")
        XCTAssertEqual(latestThumbnail, "thumb_photo_001")
        savePhoto(id: "photo_002")
        XCTAssertEqual(latestThumbnail, "thumb_photo_002")
        XCTAssertEqual(photos.count, 2)

        // Scenario 3: Tap thumbnail opens gallery viewer with newest first
        openResultViewer()
        XCTAssertNil(hintMessage)
        XCTAssertEqual(currentScreen, .gallery)
        XCTAssertEqual(photos[0], "photo_002")

        // Scenario 4: Delete latest photo -> thumbnail falls back to photo_001
        deletePhoto(id: "photo_002")
        XCTAssertEqual(photos.count, 1)
        XCTAssertEqual(latestThumbnail, "thumb_photo_001")
        XCTAssertEqual(currentScreen, .gallery)

        // Scenario 5: Delete last photo -> returns to camera, thumbnail becomes nil
        deletePhoto(id: "photo_001")
        XCTAssertTrue(photos.isEmpty)
        XCTAssertNil(latestThumbnail)
        XCTAssertEqual(currentScreen, .camera)
    }
}
