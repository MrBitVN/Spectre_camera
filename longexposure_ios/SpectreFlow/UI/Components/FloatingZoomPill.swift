import SwiftUI

/**
 * Floating zoom pill positioned at the bottom center of the camera preview.
 * Tapping cycles the device's supported lenses (0.5x, 1x, 2x, 3x).
 */
public struct FloatingZoomPill: View {
    public let currentZoom: CGFloat
    public let supportedLenses: [CGFloat]
    public let onSelectLens: (CGFloat) -> Void

    private let haptic = UISelectionFeedbackGenerator()

    public init(
        currentZoom: CGFloat,
        supportedLenses: [CGFloat],
        onSelectLens: @escaping (CGFloat) -> Void
    ) {
        self.currentZoom = currentZoom
        self.supportedLenses = supportedLenses
        self.onSelectLens = onSelectLens
    }

    public var body: some View {
        let zoomText = (currentZoom == floor(currentZoom)) ? "\(Int(currentZoom))x" : String(format: "%.1fx", currentZoom)

        Button {
            haptic.selectionChanged()
            let count = supportedLenses.count
            guard count > 0 else { return }
            let currentIndex = supportedLenses.firstIndex(where: { abs($0 - currentZoom) < 0.1 }) ?? 0
            let nextIndex = (currentIndex + 1) % count
            onSelectLens(supportedLenses[nextIndex])
        } label: {
            Text(zoomText)
                .font(.system(size: 12, weight: .bold, design: .monospaced))
                .foregroundColor(.white)
                .frame(width: 38, height: 38)
                .background(Color.black.opacity(0.55))
                .clipShape(Circle())
                .overlay(
                    Circle()
                        .stroke(Color.white.opacity(0.2), lineWidth: 1)
                )
        }
        .accessibilityLabel(String(format: NSLocalizedString("cd_zoom", comment: ""), zoomText))
    }
}
