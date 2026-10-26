import Foundation

public enum LongExposureMode: String, CaseIterable, Identifiable {
    case auto = "auto"
    case motionBlur = "motion_blur"
    case lightTrails = "light_trails"
    case vanish = "vanish"

    public var id: String { rawValue }

    public var titleKey: String {
        switch self {
        case .auto: return "mode_auto"
        case .motionBlur: return "mode_motion_blur"
        case .lightTrails: return "mode_light_trails"
        case .vanish: return "mode_vanish"
        }
    }
}
