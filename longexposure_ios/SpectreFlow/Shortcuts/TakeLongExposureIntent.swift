import Foundation
import AppIntents

@available(iOS 16.0, *)
public struct TakeLongExposureIntent: AppIntent {
    public static var title: LocalizedStringResource = "Take Long Exposure"
    public static var description = IntentDescription("Launches SpectreFlow to capture an AI long exposure photo.")

    public static var openAppWhenRun: Bool = true

    public init() {}

    @MainActor
    public func perform() async throws -> some IntentResult {
        return .result()
    }
}
