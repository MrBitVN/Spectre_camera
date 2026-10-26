import SwiftUI

/// Press-and-Hold Shutter Button for Long Exposure (SwiftUI).
/// - Press down: triggers burst capture and haptic
/// - Hold: fills circular progress track toward max duration cap, displays live 00:xx timer
/// - Release: triggers exposure synthesis (if hold >= 1s)
/// - Drag far outside (> 75pt): safely cancels capture
public struct RingShutterButton: View {
    public let isCapturing: Bool
    public let progress: Float
    public let elapsedSeconds: Float
    public let onPressStart: () -> Void
    public let onPressRelease: () -> Void
    public let onPressCancel: () -> Void

    @State private var isTouching: Bool = false
    @State private var didCancel: Bool = false

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
            // Background Track
            Circle()
                .stroke(Color.accentEmerald.opacity(0.25), lineWidth: 5)
                .frame(width: 86, height: 86)

            // Active Progress Arc
            if isCapturing {
                Circle()
                    .trim(from: 0, to: CGFloat(progress))
                    .stroke(
                        Color.accentEmerald,
                        style: StrokeStyle(lineWidth: 5, lineCap: .round)
                    )
                    .rotationEffect(.degrees(-90))
                    .frame(width: 86, height: 86)
                    .animation(.linear(duration: 0.08), value: progress)
            } else {
                Circle()
                    .stroke(Color.accentEmerald, lineWidth: 5)
                    .frame(width: 86, height: 86)
            }

            // Inner Core
            if isCapturing {
                ZStack {
                    Circle()
                        .fill(Color.accentEmerald.opacity(0.22))
                        .frame(width: 64, height: 64)

                    let secondsInt = Int(elapsedSeconds)
                    let formattedTime = String(format: "00:%02d", secondsInt)
                    Text(formattedTime)
                        .font(.system(size: 16, weight: .bold, design: .monospaced))
                        .foregroundColor(.white)
                }
                .transition(.scale)
            } else {
                Circle()
                    .fill(Color.accentMint)
                    .frame(width: 58, height: 58)
                    .transition(.scale)
            }
        }
        .contentShape(Circle())
        .accessibilityLabel(isCapturing ? "Exposing long exposure photo. Hold to continue, release to capture" : "Shutter button. Press and hold to expose, release to capture")
        .accessibilityAddTraits(.isButton)
        .gesture(
            DragGesture(minimumDistance: 0)
                .onChanged { value in
                    if !isTouching {
                        isTouching = true
                        didCancel = false
                        onPressStart()
                    } else if !didCancel {
                        let distance = hypot(value.translation.width, value.translation.height)
                        if distance > 75 {
                            didCancel = true
                            onPressCancel()
                        }
                    }
                }
                .onEnded { value in
                    guard isTouching else { return }
                    isTouching = false
                    if didCancel {
                        didCancel = false
                    } else {
                        let distance = hypot(value.translation.width, value.translation.height)
                        if distance > 75 {
                            onPressCancel()
                        } else {
                            onPressRelease()
                        }
                    }
                }
        )
    }
}
