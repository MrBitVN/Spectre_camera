import Foundation

public enum CaptureDuration: Int, CaseIterable, Identifiable {
    case sec3 = 3
    case sec5 = 5
    case sec9 = 9
    case sec15 = 15
    case sec30 = 30

    public var id: Int { rawValue }

    public var isProOnly: Bool {
        return rawValue >= 15
    }

    public var label: String {
        return "\(rawValue)s"
    }
}

public enum OutputResolution: String, CaseIterable, Identifiable {
    case high = "high"
    case balanced = "balanced"

    public var id: String { rawValue }
}

public enum AppLanguage: String, CaseIterable, Identifiable {
    case english = "en"

    public var id: String { rawValue }

    public var displayName: String {
        return "English"
    }
}

public enum AppIconTheme: String, CaseIterable, Identifiable {
    case obsidianEmerald = "emerald"
    case cyberMint = "mint"
    case deepGold = "gold"

    public var id: String { rawValue }

    public var displayName: String {
        switch self {
        case .obsidianEmerald: return "Obsidian Emerald (Default)"
        case .cyberMint: return "Cyber Mint (Pro)"
        case .deepGold: return "Deep Gold (Pro)"
        }
    }
}

public struct AppSettingsState {
    public var duration: CaptureDuration = .sec3
    public var showGrid: Bool = true
    public var hapticsEnabled: Bool = true
    public var saveCompanionVideo: Bool = true
    public var resolution: OutputResolution = .high
    public var isProUnlocked: Bool = false
    public var selectedAppIcon: AppIconTheme = .obsidianEmerald
    public var hasSeenOnboarding: Bool = false
    public var language: AppLanguage = .english

    public init() {}
}
