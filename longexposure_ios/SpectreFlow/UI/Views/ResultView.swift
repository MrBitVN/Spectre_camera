import SwiftUI

public struct ResultView: View {
    public let resultImage: UIImage
    public let keyframes: [UIImage]
    public let hasCompanionVideo: Bool
    public let onSavePhoto: (UIImage) -> Void
    public let onSharePhoto: (UIImage) -> Void
    public let onShareVideo: () -> Void
    public let onExportWallpaper: (() -> Void)?
    public let onDiscard: () -> Void

    @State private var selectedIndex: Int = -1 // -1 = composite
    @State private var activeImage: UIImage

    public init(
        resultImage: UIImage,
        keyframes: [UIImage],
        hasCompanionVideo: Bool,
        onSavePhoto: @escaping (UIImage) -> Void,
        onSharePhoto: @escaping (UIImage) -> Void,
        onShareVideo: @escaping () -> Void,
        onExportWallpaper: (() -> Void)? = nil,
        onDiscard: @escaping () -> Void
    ) {
        self.resultImage = resultImage
        self.keyframes = keyframes
        self.hasCompanionVideo = hasCompanionVideo
        self.onSavePhoto = onSavePhoto
        self.onSharePhoto = onSharePhoto
        self.onShareVideo = onShareVideo
        self.onExportWallpaper = onExportWallpaper
        self.onDiscard = onDiscard
        self._activeImage = State(initialValue: resultImage)
    }

    private var displayedImage: UIImage {
        if selectedIndex == -1 || selectedIndex >= keyframes.count {
            return activeImage
        }
        return keyframes[selectedIndex]
    }

    public var body: some View {
        ZStack {
            Color.bgObsidian.ignoresSafeArea()

            // Preview Image
            Image(uiImage: displayedImage)
                .resizable()
                .scaledToFit()
                .ignoresSafeArea()

            // Top Status Badge
            VStack {
                Text(selectedIndex == -1 ? NSLocalizedString("label_composite_result", comment: "") : String(format: NSLocalizedString("label_frame_index", comment: ""), selectedIndex + 1, keyframes.count))
                    .font(.system(size: 13, weight: .semibold))
                    .foregroundColor(.accentEmerald)
                    .padding(.horizontal, 16)
                    .padding(.vertical, 6)
                    .background(Color.bgSurface.opacity(0.85))
                    .cornerRadius(16)
                    .padding(.top, 10)

                Spacer()
            }

            // Bottom Controls: Filmstrip Scrubber & Action Bar
            VStack(spacing: 12) {
                Spacer()

                if !keyframes.isEmpty {
                    VStack(spacing: 8) {
                        HStack {
                            Text(NSLocalizedString("label_scrub_frames", comment: ""))
                                .font(.system(size: 12))
                                .foregroundColor(.textSecondary)

                            Spacer()

                            if selectedIndex != -1 {
                                Button {
                                    activeImage = keyframes[selectedIndex]
                                    selectedIndex = -1
                                } label: {
                                    Text(NSLocalizedString("action_pick_as_cover", comment: ""))
                                        .font(.system(size: 11, weight: .bold))
                                        .foregroundColor(.bgObsidian)
                                        .padding(.horizontal, 10)
                                        .padding(.vertical, 4)
                                        .background(Color.accentMint)
                                        .cornerRadius(8)
                                }
                            } else {
                                Text(NSLocalizedString("label_composite_active", comment: ""))
                                    .font(.system(size: 11, weight: .semibold))
                                    .foregroundColor(.accentEmerald)
                            }
                        }
                        .padding(.horizontal, 16)

                        // Filmstrip Thumbnails
                        ScrollView(.horizontal, showsIndicators: false) {
                            HStack(spacing: 8) {
                                // Composite
                                Image(uiImage: activeImage)
                                    .resizable()
                                    .scaledToFill()
                                    .frame(width: 44, height: 60)
                                    .cornerRadius(6)
                                    .overlay(
                                        RoundedRectangle(cornerRadius: 6)
                                            .stroke(selectedIndex == -1 ? Color.accentEmerald : Color.clear, lineWidth: 2)
                                    )
                                    .onTapGesture {
                                        selectedIndex = -1
                                    }

                                // Keyframes
                                ForEach(0..<keyframes.count, id: \.self) { index in
                                    Image(uiImage: keyframes[index])
                                        .resizable()
                                        .scaledToFill()
                                        .frame(width: 44, height: 60)
                                        .cornerRadius(6)
                                        .overlay(
                                            RoundedRectangle(cornerRadius: 6)
                                                .stroke(selectedIndex == index ? Color.accentEmerald : Color.clear, lineWidth: 2)
                                        )
                                        .onTapGesture {
                                            selectedIndex = index
                                        }
                                }
                            }
                            .padding(.horizontal, 16)
                        }
                    }
                }

                // Action Bar
                HStack(spacing: 8) {
                    Button(action: onDiscard) {
                        Label(NSLocalizedString("action_discard", comment: ""), systemImage: "xmark")
                            .font(.system(size: 12, weight: .medium))
                            .foregroundColor(.textPrimary)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 10)
                            .background(Color.bgCard)
                            .clipShape(RoundedRectangle(cornerRadius: 12))
                            .overlay(RoundedRectangle(cornerRadius: 12).stroke(Color.borderSubtle, lineWidth: 1))
                    }

                    if hasCompanionVideo {
                        Button(action: onShareVideo) {
                            Label(NSLocalizedString("action_share_video", comment: ""), systemImage: "video.fill")
                                .font(.system(size: 11, weight: .semibold))
                                .foregroundColor(.accentMint)
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 10)
                                .background(Color.bgCard)
                                .clipShape(RoundedRectangle(cornerRadius: 12))
                                .overlay(RoundedRectangle(cornerRadius: 12).stroke(Color.accentMint.opacity(0.4), lineWidth: 1))
                        }
                    }

                    if let exportWallpaper = onExportWallpaper, !keyframes.isEmpty {
                        Button(action: exportWallpaper) {
                            Label(NSLocalizedString("action_export_wallpaper", comment: ""), systemImage: "sparkles.tv")
                                .font(.system(size: 11, weight: .semibold))
                                .foregroundColor(.accentMint)
                                .frame(maxWidth: .infinity)
                                .padding(.vertical, 10)
                                .background(Color.bgCard)
                                .clipShape(RoundedRectangle(cornerRadius: 12))
                                .overlay(RoundedRectangle(cornerRadius: 12).stroke(Color.accentMint.opacity(0.4), lineWidth: 1))
                        }
                    }

                    Button {
                        onSharePhoto(displayedImage)
                    } label: {
                        Label(NSLocalizedString("action_share", comment: ""), systemImage: "square.and.arrow.up")
                            .font(.system(size: 12, weight: .medium))
                            .foregroundColor(.textPrimary)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 10)
                            .background(Color.bgCard)
                            .clipShape(RoundedRectangle(cornerRadius: 12))
                            .overlay(RoundedRectangle(cornerRadius: 12).stroke(Color.borderSubtle, lineWidth: 1))
                    }

                    Button {
                        onSavePhoto(displayedImage)
                    } label: {
                        Label(NSLocalizedString("action_save", comment: ""), systemImage: "checkmark")
                            .font(.system(size: 12, weight: .bold))
                            .foregroundColor(.bgObsidian)
                            .frame(maxWidth: .infinity)
                            .padding(.vertical, 10)
                            .background(Color.accentEmerald)
                            .clipShape(RoundedRectangle(cornerRadius: 12))
                    }
                }
                .padding(.horizontal, 14)
                .padding(.bottom, 14)
            }
            .background(Color.bgObsidian.opacity(0.85))
        }
    }
}
