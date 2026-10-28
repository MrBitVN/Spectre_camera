import SwiftUI

/**
 * Main Camera View matching reference layout:
 *
 * 1. Top Bar:
 *    - Mode label "AUTO 💡", tap opens mode picker menu. In Auto, shows detected mode.
 *    - Active green camera status dot.
 *    - Sun brightness icon with auto-hiding exposure compensation slider.
 *
 * 2. Preview Area (~65% height, rounded corners):
 *    - Live Camera Preview with optional GridOverlay.
 *    - Floating zoom pill at bottom center ("1x", tap cycles supported lenses).
 *
 * 3. Control Row below preview:
 *    - Flip camera button on left.
 *    - Stability indicator [ 📱 ] in center (STABLE green / SHAKY amber).
 *    - Settings gear button on right.
 *
 * 4. Bottom Row:
 *    - Rounded-square thumbnail of last saved photo on left (opens result viewer).
 *    - Two concentric green rings shutter button in center (press & hold, pulse & arc).
 *    - Rotary duration dial in bottom-right corner (3, 5, 9, 15, 30s).
 */
public struct CameraView: View {
    @ObservedObject public var viewModel: CameraViewModel

    public var body: some View {
        ZStack {
            Color.bgObsidian.ignoresSafeArea()

            VStack(spacing: 0) {
                // === 1. TOP BAR ===
                TopBarControls(
                    selectedMode: $viewModel.selectedMode,
                    detectedMode: viewModel.detectedMode,
                    isCameraActive: viewModel.cameraService.isReady,
                    exposureCompensation: $viewModel.exposureCompensation,
                    onSelectMode: { viewModel.selectMode($0) },
                    onExposureChanged: { viewModel.setExposureCompensation($0) }
                )
                .padding(.top, 8)

                // === 2. PREVIEW VIEWPORT (~65% height, rounded corners) ===
                GeometryReader { geo in
                    ZStack {
                        // Live Viewfinder
                        CameraPreviewView(session: viewModel.cameraService.captureSession)
                            .frame(width: geo.size.width, height: geo.size.height)
                            .clipped()

                        // Rule of Thirds Grid
                        if viewModel.settings.showGrid {
                            GridOverlay()
                        }

                        // Floating Zoom Pill at bottom center of preview
                        VStack {
                            Spacer()
                            FloatingZoomPill(
                                currentZoom: viewModel.cameraService.currentZoom,
                                supportedLenses: viewModel.cameraService.supportedLenses,
                                onSelectLens: { viewModel.selectLens($0) }
                            )
                            .padding(.bottom, 14)
                        }
                    }
                    .frame(width: geo.size.width, height: geo.size.height)
                    .clipShape(RoundedRectangle(cornerRadius: 24))
                    .padding(.horizontal, 8)
                }
                .frame(maxHeight: UIScreen.main.bounds.height * 0.65)

                Spacer().frame(height: 12)

                // === 3. CONTROL ROW (Flip | Stability [📱] | Settings Gear) ===
                HStack {
                    // Flip Camera Button
                    Button {
                        viewModel.flipCamera()
                    } label: {
                        Image(systemName: "camera.rotate.fill")
                            .font(.system(size: 20))
                            .foregroundColor(.textPrimary)
                            .frame(width: 44, height: 44)
                            .background(Color.bgSurface.opacity(0.8))
                            .clipShape(Circle())
                    }
                    .accessibilityLabel(NSLocalizedString("cd_flip_camera", comment: ""))

                    Spacer()

                    // Center Stability Indicator [ 📱 ]
                    StabilityIndicator(state: viewModel.stabilityDetector.stabilityState)

                    Spacer()

                    // Settings Gear Button
                    Button {
                        viewModel.showSettings = true
                    } label: {
                        Image(systemName: "gearshape.fill")
                            .font(.system(size: 20))
                            .foregroundColor(.textPrimary)
                            .frame(width: 44, height: 44)
                            .background(Color.bgSurface.opacity(0.8))
                            .clipShape(Circle())
                    }
                    .accessibilityLabel(NSLocalizedString("cd_settings", comment: ""))
                }
                .padding(.horizontal, 28)

                // Shutter Instruction & Hint Pill
                ZStack {
                    if let hint = viewModel.hintMessage {
                        Text(hint)
                            .font(.system(size: 12, weight: .semibold))
                            .foregroundColor(.white)
                            .padding(.horizontal, 14)
                            .padding(.vertical, 5)
                            .background(Color(red: 0.9, green: 0.35, blue: 0.0).opacity(0.9))
                            .cornerRadius(14)
                            .transition(.opacity)
                    } else if viewModel.cameraService.isCapturing {
                        Text(String(format: NSLocalizedString("hint_release_to_capture", comment: ""), viewModel.selectedDuration.rawValue))
                            .font(.system(size: 12, weight: .medium))
                            .foregroundColor(.accentEmerald)
                            .padding(.horizontal, 14)
                            .padding(.vertical, 5)
                            .background(Color.bgSurface.opacity(0.85))
                            .cornerRadius(14)
                            .transition(.opacity)
                    }
                }
                .frame(height: 28)

                Spacer()

                // === 4. BOTTOM CAPTURE STRIP (Thumbnail | Concentric Shutter | Rotary Dial) ===
                ZStack {
                    // Left: Last Photo Thumbnail
                    HStack {
                        Button {
                            viewModel.openResultViewer()
                        } label: {
                            ZStack {
                                RoundedRectangle(cornerRadius: 14)
                                    .fill(Color(white: 0.12))
                                    .frame(width: 56, height: 56)
                                    .overlay(
                                        RoundedRectangle(cornerRadius: 14)
                                            .stroke(Color.white.opacity(0.25), lineWidth: 1.5)
                                    )

                                if let thumb = viewModel.lastSavedThumbnail {
                                    Image(uiImage: thumb)
                                        .resizable()
                                        .aspectRatio(contentMode: .fill)
                                        .frame(width: 56, height: 56)
                                        .clipShape(RoundedRectangle(cornerRadius: 14))
                                } else {
                                    Image(systemName: "photo.on.rectangle")
                                        .font(.system(size: 20))
                                        .foregroundColor(Color(white: 0.45))
                                }
                            }
                        }
                        .padding(.leading, 20)

                        Spacer()
                    }

                    // Center: Dual-Ring Shutter Button
                    RingShutterButton(
                        isCapturing: viewModel.cameraService.isCapturing,
                        progress: viewModel.cameraService.captureProgress,
                        elapsedSeconds: viewModel.cameraService.elapsedSeconds,
                        onPressStart: { viewModel.shutterPressed() },
                        onPressRelease: { viewModel.shutterReleased() },
                        onPressCancel: { viewModel.cancelCapture() }
                    )

                    // Right: Rotary Duration Dial in bottom-right corner
                    HStack {
                        Spacer()
                        RotaryDurationDial(
                            selectedDuration: $viewModel.selectedDuration,
                            isProUnlocked: viewModel.proManager.isProUnlocked,
                            onSelect: { viewModel.selectDuration($0) },
                            onLockedTapped: {
                                viewModel.hintMessage = String(format: NSLocalizedString("hint_pro_required", comment: ""), viewModel.selectedDuration.rawValue)
                            }
                        )
                    }
                }
                .padding(.bottom, 16)
            }
        }
        .sheet(isPresented: $viewModel.showSettings) {
            SettingsView(
                settings: $viewModel.settings,
                onOpenOnboarding: {
                    viewModel.showSettings = false
                    viewModel.showOnboarding = true
                },
                onDismiss: { viewModel.showSettings = false }
            )
        }
        .sheet(isPresented: $viewModel.showOnboarding) {
            TutorialsView(
                onDismiss: {
                    viewModel.showOnboarding = false
                    UserDefaults.standard.set(true, forKey: "has_seen_onboarding")
                }
            )
        }
    }
}
