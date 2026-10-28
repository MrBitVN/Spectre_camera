import SwiftUI

/**
 * Top Bar Controls matching reference layout:
 * - Left: Mode label "AUTO 💡" (or detected mode in Auto). Tap opens mode picker menu.
 * - Center-Right: Tiny green status dot when camera is active.
 * - Right: Small sun icon with auto-hiding exposure compensation slider (3s).
 */
public struct TopBarControls: View {
    @Binding public var selectedMode: LongExposureMode
    public let detectedMode: LongExposureMode
    public let isCameraActive: Bool
    @Binding public var exposureCompensation: Float
    public let onSelectMode: (LongExposureMode) -> Void
    public let onExposureChanged: (Float) -> Void

    @State private var showExposureSlider: Bool = false
    @State private var sliderDismissTask: Task<Void, Never>? = nil

    public init(
        selectedMode: Binding<LongExposureMode>,
        detectedMode: LongExposureMode,
        isCameraActive: Bool,
        exposureCompensation: Binding<Float>,
        onSelectMode: @escaping (LongExposureMode) -> Void,
        onExposureChanged: @escaping (Float) -> Void
    ) {
        self._selectedMode = selectedMode
        self.detectedMode = detectedMode
        self.isCameraActive = isCameraActive
        self._exposureCompensation = exposureCompensation
        self.onSelectMode = onSelectMode
        self.onExposureChanged = onExposureChanged
    }

    private func resetSliderTimer() {
        sliderDismissTask?.cancel()
        sliderDismissTask = Task {
            try? await Task.sleep(nanoseconds: 3_000_000_000)
            await MainActor.run {
                withAnimation {
                    showExposureSlider = false
                }
            }
        }
    }

    public var body: some View {
        VStack(spacing: 8) {
            HStack {
                // Mode Label & Lightbulb Menu: "AUTO 💡"
                Menu {
                    ForEach(LongExposureMode.allCases) { mode in
                        Button {
                            selectedMode = mode
                            onSelectMode(mode)
                        } label: {
                            HStack {
                                Text(mode.displayName)
                                if mode == selectedMode {
                                    Image(systemName: "checkmark")
                                }
                            }
                        }
                    }
                } label: {
                    let modeLabel = (selectedMode == .auto) ? "AUTO" : selectedMode.displayName.uppercased()
                    HStack(spacing: 5) {
                        Text(modeLabel)
                            .font(.system(size: 16, weight: .bold, design: .monospaced))
                            .foregroundColor(.accentEmerald)
                            .tracking(0.5)

                        Image(systemName: "lightbulb.fill")
                            .font(.system(size: 15))
                            .foregroundColor(.accentEmerald)
                    }
                    .padding(.horizontal, 6)
                    .padding(.vertical, 4)
                }

                Spacer()

                // Active Camera Dot
                if isCameraActive {
                    Circle()
                        .fill(Color.accentEmerald)
                        .frame(width: 7, height: 7)
                        .padding(.trailing, 12)
                }

                // Sun Icon (Exposure Compensation)
                Button {
                    withAnimation {
                        showExposureSlider.toggle()
                        if showExposureSlider {
                            resetSliderTimer()
                        }
                    }
                } label: {
                    Image(systemName: "sun.max.fill")
                        .font(.system(size: 16))
                        .foregroundColor(showExposureSlider ? .accentEmerald : .textSecondary)
                        .frame(width: 32, height: 32)
                }
                .accessibilityLabel(NSLocalizedString("cd_exposure_icon", comment: ""))
            }
            .padding(.horizontal, 20)

            // Auto-hiding Exposure Slider
            if showExposureSlider {
                HStack(spacing: 12) {
                    Text(String(format: "%+.1f EV", exposureCompensation))
                        .font(.system(size: 12, weight: .bold, design: .monospaced))
                        .foregroundColor(.accentEmerald)
                        .frame(width: 55, alignment: .leading)

                    Slider(
                        value: Binding(
                            get: { exposureCompensation },
                            set: { newVal in
                                exposureCompensation = newVal
                                onExposureChanged(newVal)
                                resetSliderTimer()
                            }
                        ),
                        in: -2.0...2.0,
                        step: 0.5
                    )
                    .accentColor(.accentEmerald)
                }
                .padding(.horizontal, 14)
                .padding(.vertical, 6)
                .background(Color.bgSurface.opacity(0.85))
                .cornerRadius(12)
                .padding(.horizontal, 20)
                .transition(.opacity)
            }
        }
    }
}
