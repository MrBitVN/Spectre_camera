import Foundation

public enum StabilityState: String {
    case stable = "STABLE"
    case shaky = "SHAKY"

    public var titleKey: String {
        switch self {
        case .stable: return "stability_stable"
        case .shaky: return "stability_shaky"
        }
    }
}
