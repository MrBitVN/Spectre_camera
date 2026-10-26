import XCTest
import CoreGraphics
@testable import SpectreFlow

final class AlignmentMathTests: XCTestCase {

    func testCropBoundsCalculation() {
        let aligner = FrameAligner(baseWidth: 1000, baseHeight: 800)

        XCTAssertEqual(aligner.cropBounds, CGRect(x: 0, y: 0, width: 1000, height: 800))
    }
}
