import SwiftUI

public struct CameraView: View {
    @ObservedObject public var viewModel: CameraViewModel

    public var body: some View {
        ZStack {
            Color.bgObsidian.ignoresSafeArea()

            // Live Camera Preview
            CameraPreviewView(session: viewModel.cameraService.captureSession)
                .ignoresSafeArea()

            // Rule of Thirds Grid
            if viewModel.settings.showGrid {
                GridOverlay()
            }

            // Top Bar
            VStack {
                HStack {
                    HStack(spacing: 8) {
                        Text("SpectreFlow")
                            .font(.system(size: 18, weight: .bold))
                            .foregroundColor(.accentEmerald)
                            .tracking(1)

                        StabilityBadge(state: viewModel.stabilityDetector.stabilityState)
                    }

                    Spacer()

                    Button {
                        viewModel.showSettings = true
                    } label: {
                        Image(systemName: "gearshape.fill")
                            .font(.system(size: 18))
                            .foregroundColor(.textPrimary)
                            .padding(10)
                            .background(Color.bgSurface.opacity(0.85))
                            .clipShape(Circle())
                    }
                    .accessibilityLabel("Settings")
                }
                .padding(.horizontal, 20)
                .padding(.top, 10)

                if viewModel.isLimitedAccess {
                    HStack(spacing: 6) {
                        Image(systemName: "info.circle.fill")
                            .foregroundColor(.accentEmerald)
                            .font(.system(size: 12))
                        Text(NSLocalizedString("limited_access_hint", comment: ""))
                            .font(.system(size: 11))
                            .foregroundColor(.textSecondary)
                    }
                    .padding(.horizontal, 12)
                    .padding(.vertical, 5)
                    .background(Color.bgSurface.opacity(0.85))
                    .cornerRadius(10)
                    .padding(.top, 4)
                }

                Spacer()

                // Bottom Controls
                VStack(spacing: 10) {
                    // Lens Selector (Wide, Ultra-wide, Telephoto, Flip)
                    LensSelector(
                        supportedLenses: viewModel.cameraService.supportedLenses,
                        currentZoom: viewModel.cameraService.currentZoom,
                        isFrontCamera: viewModel.cameraService.isFrontCamera,
                        onSelectLens: { viewModel.selectLens($0) },
                        onFlipCamera: { viewModel.flipCamera() }
                    )

                    // Mode Selector (Auto, Motion Blur, Light Trails, Vanish)
                    ModeSelector(
                        selectedMode: $viewModel.selectedMode,
                        detectedMode: viewModel.detectedMode,
                        onSelect: { viewModel.selectMode($0) }
                    )

                    // Duration Selector
                    DurationSelector(
                        selectedDuration: $viewModel.selectedDuration,
                        isProUnlocked: viewModel.proManager.isProUnlocked,
                        onSelect: { viewModel.selectDuration($0) }
                    )

                    // Shutter Instruction & Hint Pill
                    Group {
                        if let hint = viewModel.hintMessage {
                            Text(hint)
                                .font(.system(size: 12, weight: .semibold))
                                .foregroundColor(.white)
                                .padding(.horizontal, 14)
                                .padding(.vertical, 5)
                                .background(Color(red: 0.9, green: 0.35, blue: 0.0).opacity(0.9))
                                .cornerRadius(14)
                        } else if viewModel.cameraService.isCapturing {
                            Text(String(format: NSLocalizedString("hint_release_to_capture", comment: ""), viewModel.selectedDuration.rawValue))
                                .font(.system(size: 12, weight: .medium))
                                .foregroundColor(.accentEmerald)
                                .padding(.horizontal, 14)
                                .padding(.vertical, 5)
                                .background(Color.bgSurface.opacity(0.85))
                                .cornerRadius(14)
                        } else {
                            Text(NSLocalizedString("hint_press_hold", comment: ""))
                                .font(.system(size: 12, weight: .regular))
                                .foregroundColor(.textSecondary)
                        }
                    }
                    .frame(height: 26)

                    // Press-and-Hold Shutter Button
                    RingShutterButton(
                        isCapturing: viewModel.cameraService.isCapturing,
                        progress: viewModel.cameraService.captureProgress,
                        elapsedSeconds: viewModel.cameraService.elapsedSeconds,
                        onPressStart: { viewModel.shutterPressed() },
                        onPressRelease: { viewModel.shutterReleased() },
                        onPressCancel: { viewModel.shutterCanceled() }
                    )
                    .padding(.top, 2)
                }
                .padding(.bottom, 24)
            }
        }
        .fullScreenCover(isPresented: $viewModel.showOnboarding) {
            OnboardingView(onDismiss: {
                viewModel.dismissOnboarding()
            })
        }
    }
}
