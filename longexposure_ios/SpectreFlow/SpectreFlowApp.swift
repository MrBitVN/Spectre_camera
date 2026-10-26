import SwiftUI

@main
struct SpectreFlowApp: App {
    @StateObject private var viewModel = CameraViewModel()
    @State private var showingToast = false
    @State private var toastMessage = ""
    @State private var shareItems: [Any] = []

    var body: some Scene {
        WindowGroup {
            ZStack {
                Color.bgObsidian.ignoresSafeArea()

                switch viewModel.screenState {
                case .camera:
                    CameraView(viewModel: viewModel)
                case .processing:
                    ProcessingView(
                        message: viewModel.processingMessage,
                        progress: viewModel.processingProgress,
                        onCancel: { viewModel.cancelCapture() }
                    )
                case .result:
                    if let image = viewModel.resultImage {
                        ResultView(
                            resultImage: image,
                            keyframes: viewModel.burstKeyframes,
                            hasCompanionVideo: viewModel.companionVideoURL != nil,
                            onSavePhoto: { pickedImage in
                                viewModel.saveResult(image: pickedImage) {
                                    showToast(NSLocalizedString("save_success", comment: ""))
                                } onError: { err in
                                    showToast("\(NSLocalizedString("save_failed", comment: "")) \(err)")
                                }
                            },
                            onSharePhoto: { pickedImage in
                                shareItems = [pickedImage]
                                viewModel.shareSheetPresented = true
                            },
                            onShareVideo: {
                                if let videoURL = viewModel.companionVideoURL {
                                    shareItems = [videoURL]
                                    viewModel.shareSheetPresented = true
                                }
                            },
                            onExportWallpaper: {
                                viewModel.exportLoopingWallpaper {
                                    showToast(NSLocalizedString("hint_wallpaper_saved", comment: ""))
                                } onError: { err in
                                    showToast(err)
                                }
                            },
                            onDiscard: {
                                viewModel.discardResult()
                            }
                        )
                        .sheet(isPresented: $viewModel.shareSheetPresented) {
                            ActivityView(activityItems: shareItems)
                        }
                    }
                }

                if viewModel.showPermissionRationale {
                    PermissionRationaleView {
                        if let url = URL(string: UIApplication.openSettingsURLString) {
                            UIApplication.shared.open(url)
                        }
                    }
                }

                // Toast Banner
                if showingToast {
                    VStack {
                        Spacer()
                        Text(toastMessage)
                            .font(.system(size: 13, weight: .semibold))
                            .foregroundColor(.bgObsidian)
                            .padding(.horizontal, 16)
                            .padding(.vertical, 10)
                            .background(Color.accentEmerald)
                            .clipShape(Capsule())
                            .shadow(radius: 6)
                            .padding(.bottom, 36)
                    }
                    .transition(.move(edge: .bottom).combined(with: .opacity))
                }
            }
            .sheet(isPresented: $viewModel.showSettings) {
                SettingsView(
                    settings: $viewModel.settings,
                    onOpenOnboarding: {
                        viewModel.showSettings = false
                        viewModel.openOnboarding()
                    },
                    onDismiss: {
                        viewModel.showSettings = false
                    }
                )
            }
            .preferredColorScheme(.dark)
        }
    }

    private func showToast(_ message: String) {
        toastMessage = message
        withAnimation { showingToast = true }
        DispatchQueue.main.asyncAfter(deadline: .now() + 2.5) {
            withAnimation { showingToast = false }
        }
    }
}

struct ActivityView: UIViewControllerRepresentable {
    let activityItems: [Any]

    func makeUIViewController(context: Context) -> UIActivityViewController {
        UIActivityViewController(activityItems: activityItems, applicationActivities: nil)
    }

    func updateUIViewController(_ uiViewController: UIActivityViewController, context: Context) {}
}
