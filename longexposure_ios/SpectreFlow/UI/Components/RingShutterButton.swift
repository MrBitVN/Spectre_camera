import SwiftUI

/**
 * Large dual-ring shutter button matching reference design:
 * - Two concentric green rings: outer ring + inner ring.
 * - Press and hold to expose, release to capture.
 * - While holding: inner ring pulses, outer ring arc fills toward cap, elapsed time "00:07" displayed.
 * - Tactile haptic pulse on press down and release.
 */
public struct RingShutterButton: View {
    public let isCapturing: Bool
    public let progress: Float
    public let elapsedSeconds: Float
    public let onPressStart: () -> Void
    public let onPressRelease: () -> Void
    public let onPressCancel: () -> Void

    @State private var isTouching: Bool = false
    @State private var didCancel: Bool = false
    @State private var isPulsing: Bool = false
    private let impactFeedback = UIImpactFeedbackGenerator(style: .medium)

    public init(
        isCapturing: Bool,
        progress: Float,
        elapsedSeconds: Float,
        onPressStart: @escaping () -> Void,
        onPressRelease: @escaping () -> Void,
        onPressCancel: @escaping () -> Void
    ) {
        self.isCapturing = isCapturing
        self.progress = progress
        self.elapsedSeconds = elapsedSeconds
        self.onPressStart = onPressStart
        self.onPressRelease = onPressRelease
        self.onPressCancel = onPressCancel
    }

    public var body: some View {
        ZStack {
            // 1. Outer Ring
            if isCapturing {
                // Dim background track
                Circle()
                    .stroke(Color.accentEmerald.opacity(0.25), lineWidth: 3.5)
                    .frame(width: 80, height: 80)

                // Active progress arc
                Circle()
                    .trim(from: 0, to: CGFloat(progress))
                    .stroke(
                        Color.accentEmerald,
                        style: StrokeStyle(lineWidth: 4, lineCap: .round)
                    )
                    .rotationEffect(.degrees(-90))
                    .frame(width: 80, height: 80)
                    .animation(.linear(duration: 0.08), value: progress)
            } else {
                Circle()
                    .stroke(Color.accentEmerald, lineWidth: 3.5)
                    .frame(width: 80, height: 80)
            }

            // 2. Inner Ring
            Circle()
                .stroke(Color.accentEmerald, lineWidth: 3.5)
                .frame(width: 58, height: 58)
                .scaleEffect(isCapturing && isPulsing ? 1.06 : (isCapturing ? 0.94 : 1.0))
                .animation(isCapturing ? Animation.easeInOut(duration: 0.65).repeatForever(autoreverses: true) : .default, value: isPulsing)

            // 3. Center Elapsed Time Display
            if isCapturing {
                let secondsInt = Int(elapsedSeconds)
                let formattedTime = String(format: "00:%02d", secondsInt)
                Text(formattedTime)
                    .font(.system(size: 15, weight: .bold, design: .monospaced))
                    .foregroundColor(.white)
                    .tracking(0.5)
            }
        }
        .frame(width: 82, height: 82)
        .contentShape(Circle())
        .onChange(of: isCapturing) { capturing in
            if capturing {
                isPulsing = true
            } else {
                isPulsing = false
            }
        }
        .gesture(
            DragGesture(minimumDistance: 0)
                .onChanged { value in
                    if !isTouching {
                        isTouching = true
                        didCancel = false
                        impactFeedback.impactOccurred()
                        onPressStart()
                    } else if !didCancel {
                        let distance = hypot(value.translation.width, value.translation.height)
                        if distance > 70 {
                            didCancel = true
                            impactFeedback.impactOccurred()
                            onPressCancel()
                        }
                    }
                }
                .onEnded { _ in
                    guard isTouching else { return }
                    isTouching = false
                    impactFeedback.impactOccurred()
                    if didCancel {
                        onPressCancel()
                    } else {
                        onPressRelease()
                    }
                }
        )
        .accessibilityLabel(isCapturing ? NSLocalizedString("cd_shutter_capturing", comment: "") : NSLocalizedString("cd_shutter_idle", comment: ""))
        .accessibilityAddTraits(.isButton)
    }
}
