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
}
