import Foundation
import Photos
import UIKit

/// High-resolution Photo Library Exporter using PHPhotoLibrary.
/// Saves synthesized long exposure images and companion video / Live Photos to the Camera Roll and SpectreFlow album.
public final class PhotoLibrarySaver {

    public static let albumName = "SpectreFlow"

    public static func checkAuthorizationStatus() -> PHAuthorizationStatus {
        return PHPhotoLibrary.authorizationStatus(for: .readWrite)
    }

    public static func getOrCreateAlbum() -> PHAssetCollection? {
        let fetchOptions = PHFetchOptions()
        fetchOptions.predicate = NSPredicate(format: "title = %@", albumName)
        let collection = PHAssetCollection.fetchAssetCollections(with: .album, subtype: .albumRegular, options: fetchOptions)
        if let first = collection.firstObject {
            return first
        }

        var placeholder: PHObjectPlaceholder?
        do {
            try PHPhotoLibrary.shared().performChangesAndWait {
                let createAlbum = PHAssetCollectionChangeRequest.creationRequestForAssetCollection(withTitle: albumName)
                placeholder = createAlbum.placeholderForCreatedAssetCollection
            }
            if let placeholder = placeholder {
                let result = PHAssetCollection.fetchAssetCollections(withLocalIdentifiers: [placeholder.localIdentifier], options: nil)
                return result.firstObject
            }
        } catch {
            print("Failed to create album: \(error)")
        }
        return nil
    }

    public static func saveImage(_ image: UIImage, pairedVideoURL: URL? = nil) async throws {
        let status = await PHPhotoLibrary.requestAuthorization(for: .readWrite)
        guard status == .authorized || status == .limited else {
            throw NSError(domain: "com.spectreflow", code: 403, userInfo: [NSLocalizedDescriptionKey: "Photo library access denied"])
        }

        let album = getOrCreateAlbum()

        try await PHPhotoLibrary.shared().performChanges {
            var assetPlaceholder: PHObjectPlaceholder?

            if let videoURL = pairedVideoURL, FileManager.default.fileExists(atPath: videoURL.path),
               let jpegData = image.jpegData(compressionQuality: 0.95) {
                // Save as paired Live Photo asset
                let creationRequest = PHAssetCreationRequest.forAsset()
                let photoOptions = PHAssetResourceCreationOptions()
                creationRequest.addResource(with: .photo, data: jpegData, options: photoOptions)

                let videoOptions = PHAssetResourceCreationOptions()
                videoOptions.shouldMoveFile = false
                creationRequest.addResource(with: .pairedVideo, fileURL: videoURL, options: videoOptions)
                assetPlaceholder = creationRequest.placeholderForCreatedAsset
            } else {
                let request = PHAssetChangeRequest.creationRequestForAsset(from: image)
                request.creationDate = Date()
                assetPlaceholder = request.placeholderForCreatedAsset
            }

            if let album = album, let placeholder = assetPlaceholder {
                if let albumChangeRequest = PHAssetCollectionChangeRequest(for: album) {
                    albumChangeRequest.addAssets([placeholder] as NSArray)
                }
            }
        }
    }

    public static func saveVideo(_ videoURL: URL) async throws {
        let status = await PHPhotoLibrary.requestAuthorization(for: .readWrite)
        guard status == .authorized || status == .limited else {
            throw NSError(domain: "com.spectreflow", code: 403, userInfo: [NSLocalizedDescriptionKey: "Photo library access denied"])
        }

        let album = getOrCreateAlbum()

        try await PHPhotoLibrary.shared().performChanges {
            let request = PHAssetChangeRequest.creationRequestForAssetFromVideo(atFileURL: videoURL)
            if let album = album, let placeholder = request?.placeholderForCreatedAsset {
                if let albumChangeRequest = PHAssetCollectionChangeRequest(for: album) {
                    albumChangeRequest.addAssets([placeholder] as NSArray)
                }
            }
        }
    }
}
