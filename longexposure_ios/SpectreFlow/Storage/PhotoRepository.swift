import Foundation
import Photos
import UIKit
import Combine

/// Unified Photo Repository for SpectreFlow on iOS.
/// Manages the single source of truth for photos saved to the "SpectreFlow" album.
/// Observes changes using `PHPhotoLibraryChangeObserver` so the thumbnail and gallery viewer
/// are always in sync.
public final class PhotoRepository: NSObject, ObservableObject, PHPhotoLibraryChangeObserver {

    public static let shared = PhotoRepository()

    @Published public private(set) var photos: [PHAsset] = []
    @Published public private(set) var latestThumbnail: UIImage? = nil

    private let imageManager = PHCachingImageManager.default()
    private var isObserving = false

    public override init() {
        super.init()
        startObserving()
        fetchPhotos()
    }

    deinit {
        stopObserving()
    }

    public func startObserving() {
        guard !isObserving else { return }
        PHPhotoLibrary.shared().register(self)
        isObserving = true
    }

    public func stopObserving() {
        guard isObserving else { return }
        PHPhotoLibrary.shared().unregisterChangeObserver(self)
        isObserving = false
    }

    public func fetchPhotos() {
        let auth = PHPhotoLibrary.authorizationStatus(for: .readWrite)
        guard auth == .authorized || auth == .limited else {
            DispatchQueue.main.async {
                self.photos = []
                self.latestThumbnail = nil
            }
            return
        }

        let fetchOptions = PHFetchOptions()
        fetchOptions.predicate = NSPredicate(format: "title = %@", PhotoLibrarySaver.albumName)
        let albums = PHAssetCollection.fetchAssetCollections(with: .album, subtype: .albumRegular, options: fetchOptions)

        var newPhotos: [PHAsset] = []
        if let album = albums.firstObject {
            let assetOptions = PHFetchOptions()
            assetOptions.sortDescriptors = [NSSortDescriptor(key: "creationDate", ascending: false)]
            let result = PHAsset.fetchAssets(in: album, options: assetOptions)
            result.enumerateObjects { asset, _, _ in
                newPhotos.append(asset)
            }
        }

        DispatchQueue.main.async {
            self.photos = newPhotos
            if let firstAsset = newPhotos.first {
                self.loadThumbnail(for: firstAsset)
            } else {
                self.latestThumbnail = nil
            }
        }
    }

    private func loadThumbnail(for asset: PHAsset) {
        let options = PHImageRequestOptions()
        options.isNetworkAccessAllowed = true
        options.deliveryMode = .highQualityFormat
        options.resizeMode = .fast

        let size = CGSize(width: 140, height: 140)
        imageManager.requestImage(
            for: asset,
            targetSize: size,
            contentMode: .aspectFill,
            options: options
        ) { [weak self] image, _ in
            DispatchQueue.main.async {
                self?.latestThumbnail = image
            }
        }
    }

    public func loadFullImage(for asset: PHAsset, completion: @escaping (UIImage?) -> Void) {
        let options = PHImageRequestOptions()
        options.isNetworkAccessAllowed = true
        options.deliveryMode = .highQualityFormat
        options.isSynchronous = false

        imageManager.requestImage(
            for: asset,
            targetSize: PHImageManagerMaximumSize,
            contentMode: .aspectFit,
            options: options
        ) { image, _ in
            DispatchQueue.main.async {
                completion(image)
            }
        }
    }

    public func deletePhoto(_ asset: PHAsset, completion: @escaping (Bool) -> Void) {
        PHPhotoLibrary.shared().performChanges({
            PHAssetChangeRequest.deleteAssets([asset] as NSArray)
        }) { [weak self] success, error in
            DispatchQueue.main.async {
                if success {
                    self?.fetchPhotos()
                }
                completion(success)
            }
        }
    }

    // MARK: - PHPhotoLibraryChangeObserver
    public func photoLibraryDidChange(_ changeInstance: PHChange) {
        DispatchQueue.main.async { [weak self] in
            self?.fetchPhotos()
        }
    }
}
