import Foundation
import Photos
import UIKit

/// High-resolution Photo Library Exporter using PHPhotoLibrary.
/// Saves synthesized long exposure images and companion video / Live Photos to the Camera Roll.
public final class PhotoLibrarySaver {

    public static func checkAuthorizationStatus() -> PHAuthorizationStatus {
        return PHPhotoLibrary.authorizationStatus(for: .readWrite)
    }

    public static func saveImage(_ image: UIImage, pairedVideoURL: URL? = nil) async throws {
        let status = await PHPhotoLibrary.requestAuthorization(for: .readWrite)
        guard status == .authorized || status == .limited else {
            throw NSError(domain: "com.spectreflow", code: 403, userInfo: [NSLocalizedDescriptionKey: "Photo library access denied"])
        }

        try await PHPhotoLibrary.shared().performChanges {
            if let videoURL = pairedVideoURL, FileManager.default.fileExists(atPath: videoURL.path),
               let jpegData = image.jpegData(compressionQuality: 0.95) {
                // Save as paired Live Photo asset
                let creationRequest = PHAssetCreationRequest.forAsset()
                let photoOptions = PHAssetResourceCreationOptions()
                creationRequest.addResource(with: .photo, data: jpegData, options: photoOptions)

                let videoOptions = PHAssetResourceCreationOptions()
                videoOptions.shouldMoveFile = false
                creationRequest.addResource(with: .pairedVideo, fileURL: videoURL, options: videoOptions)
            } else {
                let request = PHAssetChangeRequest.creationRequestForAsset(from: image)
                request.creationDate = Date()
            }
        }
    }

    public static func saveVideo(_ videoURL: URL) async throws {
        let status = await PHPhotoLibrary.requestAuthorization(for: .readWrite)
        guard status == .authorized || status == .limited else {
            throw NSError(domain: "com.spectreflow", code: 403, userInfo: [NSLocalizedDescriptionKey: "Photo library access denied"])
        }

        try await PHPhotoLibrary.shared().performChanges {
            PHAssetChangeRequest.creationRequestForAssetFromVideo(atFileURL: videoURL)
        }
    }
}
