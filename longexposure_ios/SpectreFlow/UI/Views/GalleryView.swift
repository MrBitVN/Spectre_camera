import SwiftUI
import Photos

public struct GalleryView: View {
    @ObservedObject var viewModel: CameraViewModel
    @ObservedObject private var repository = PhotoRepository.shared

    @State private var selectedIndex: Int = 0
    @State private var showingDeleteAlert = false
    @State private var showingShareSheet = false
    @State private var shareItems: [Any] = []
    @State private var loadedImages: [String: UIImage] = [:]

    private let dateFormatter: DateFormatter = {
        let df = DateFormatter()
        df.dateStyle = .medium
        df.timeStyle = .short
        return df
    }()

    public init(viewModel: CameraViewModel) {
        self.viewModel = viewModel
    }

    public var body: some View {
        ZStack {
            Color.bgObsidian.ignoresSafeArea()

            if repository.photos.isEmpty {
                VStack(spacing: 16) {
                    Image(systemName: "photo.on.rectangle.angled")
                        .font(.system(size: 54))
                        .foregroundColor(Color.textSecondary.opacity(0.6))

                    Text(NSLocalizedString("no_photos_yet", comment: "No photos captured yet"))
                        .font(.headline)
                        .foregroundColor(Color.textSecondary)

                    Button {
                        viewModel.screenState = .camera
                    } label: {
                        Text(NSLocalizedString("cd_back", comment: "Back"))
                            .font(.system(size: 15, weight: .semibold))
                            .foregroundColor(.black)
                            .padding(.horizontal, 24)
                            .padding(.vertical, 12)
                            .background(Color.accentEmerald)
                            .clipShape(Capsule())
                    }
                    .padding(.top, 8)
                }
            } else {
                VStack(spacing: 0) {
                    // Top Bar
                    topBar

                    // Swipeable Photo Pager
                    TabView(selection: $selectedIndex) {
                        ForEach(0..<repository.photos.count, id: \.self) { index in
                            let asset = repository.photos[index]
                            ZStack {
                                if let img = loadedImages[asset.localIdentifier] {
                                    Image(uiImage: img)
                                        .resizable()
                                        .aspectRatio(contentMode: .fit)
                                } else {
                                    ProgressView()
                                        .tint(Color.accentEmerald)
                                        .onAppear {
                                            loadImage(for: asset)
                                        }
                                }
                            }
                            .frame(maxWidth: .infinity, maxHeight: .infinity)
                            .tag(index)
                        }
                    }
                    .tabViewStyle(PageTabViewStyle(indexDisplayMode: .never))

                    // Bottom Bar
                    bottomBar
                }
            }
        }
        .sheet(isPresented: $showingShareSheet) {
            ActivityView(activityItems: shareItems)
        }
        .alert(isPresented: $showingDeleteAlert) {
            Alert(
                title: Text(NSLocalizedString("gallery_delete_confirm_title", comment: "")),
                message: Text(NSLocalizedString("gallery_delete_confirm_message", comment: "")),
                primaryButton: .destructive(Text(NSLocalizedString("action_delete", comment: ""))) {
                    deleteCurrentPhoto()
                },
                secondaryButton: .cancel(Text(NSLocalizedString("action_cancel", comment: "")))
            )
        }
        .onAppear {
            repository.fetchPhotos()
            selectedIndex = 0
        }
    }

    private var topBar: some View {
        HStack {
            Button {
                viewModel.screenState = .camera
            } label: {
                HStack(spacing: 6) {
                    Image(systemName: "chevron.left")
                        .font(.system(size: 16, weight: .semibold))
                    Text(NSLocalizedString("cd_back", comment: "Back"))
                        .font(.system(size: 15, weight: .medium))
                }
                .foregroundColor(Color.accentEmerald)
            }

            Spacer()

            if !repository.photos.isEmpty && selectedIndex < repository.photos.count {
                let asset = repository.photos[selectedIndex]
                VStack(spacing: 2) {
                    Text("\(selectedIndex + 1) of \(repository.photos.count)")
                        .font(.system(size: 14, weight: .semibold))
                        .foregroundColor(Color.textPrimary)

                    if let creationDate = asset.creationDate {
                        Text(dateFormatter.string(from: creationDate))
                            .font(.system(size: 11))
                            .foregroundColor(Color.textSecondary)
                    }
                }
            }

            Spacer()

            // Balance spacer for centering
            Color.clear
                .frame(width: 50, height: 20)
        }
        .padding(.horizontal, 16)
        .padding(.top, 10)
        .padding(.bottom, 8)
        .background(Color.bgSurface.opacity(0.85))
    }

    private var bottomBar: some View {
        HStack {
            // Share Button
            Button {
                shareCurrentPhoto()
            } label: {
                HStack(spacing: 6) {
                    Image(systemName: "square.and.arrow.up")
                        .font(.system(size: 17))
                    Text(NSLocalizedString("action_share", comment: "Share"))
                        .font(.system(size: 14, weight: .medium))
                }
                .foregroundColor(Color.textPrimary)
                .padding(.horizontal, 20)
                .padding(.vertical, 10)
                .background(Color.bgCard)
                .clipShape(Capsule())
            }

            Spacer()

            // Delete Button
            Button {
                showingDeleteAlert = true
            } label: {
                HStack(spacing: 6) {
                    Image(systemName: "trash")
                        .font(.system(size: 17))
                    Text(NSLocalizedString("action_delete", comment: "Delete"))
                        .font(.system(size: 14, weight: .medium))
                }
                .foregroundColor(Color.statusError)
                .padding(.horizontal, 20)
                .padding(.vertical, 10)
                .background(Color.bgCard)
                .clipShape(Capsule())
            }
        }
        .padding(.horizontal, 24)
        .padding(.vertical, 16)
        .background(Color.bgSurface.opacity(0.9))
    }

    private func loadImage(for asset: PHAsset) {
        if loadedImages[asset.localIdentifier] != nil { return }
        repository.loadFullImage(for: asset) { image in
            if let image = image {
                loadedImages[asset.localIdentifier] = image
            }
        }
    }

    private func shareCurrentPhoto() {
        guard selectedIndex < repository.photos.count else { return }
        let asset = repository.photos[selectedIndex]
        if let image = loadedImages[asset.localIdentifier] {
            shareItems = [image]
            showingShareSheet = true
        } else {
            repository.loadFullImage(for: asset) { image in
                if let image = image {
                    loadedImages[asset.localIdentifier] = image
                    shareItems = [image]
                    showingShareSheet = true
                }
            }
        }
    }

    private func deleteCurrentPhoto() {
        guard selectedIndex < repository.photos.count else { return }
        let asset = repository.photos[selectedIndex]
        repository.deletePhoto(asset) { success in
            if success {
                loadedImages.removeValue(forKey: asset.localIdentifier)
                if repository.photos.isEmpty {
                    viewModel.screenState = .camera
                } else if selectedIndex >= repository.photos.count {
                    selectedIndex = max(0, repository.photos.count - 1)
                }
            }
        }
    }
}
