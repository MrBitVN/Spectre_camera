import SwiftUI

public struct ModeSelector: View {
    @Binding public var selectedMode: LongExposureMode
    public let detectedMode: LongExposureMode
    public let onSelect: (LongExposureMode) -> Void

    public var body: some View {
        HStack(spacing: 4) {
            ForEach(LongExposureMode.allCases) { mode in
                let isSelected = mode == selectedMode
                let titleText: String = {
                    if mode == .auto {
                        let detectedShort: String
                        switch detectedMode {
                        case .lightTrails: detectedShort = NSLocalizedString("mode_short_light_trails", comment: "")
                        case .vanish: detectedShort = NSLocalizedString("mode_short_vanish", comment: "")
                        default: detectedShort = NSLocalizedString("mode_short_motion_blur", comment: "")
                        }
                        return isSelected ? "\(NSLocalizedString("mode_auto", comment: "")) (\(detectedShort))" : NSLocalizedString("mode_auto", comment: "")
                    } else {
                        return NSLocalizedString(mode.titleKey, comment: "")
                    }
                }()

                Button {
                    onSelect(mode)
                } label: {
                    Text(titleText)
                        .font(.system(size: 12, weight: isSelected ? .bold : .medium))
                        .foregroundColor(isSelected ? .accentEmerald : .textSecondary)
                        .padding(.horizontal, 10)
                        .padding(.vertical, 6)
                        .background(isSelected ? Color.accentEmerald.opacity(0.2) : Color.clear)
                        .clipShape(Capsule())
                }
                .buttonStyle(.plain)
                .accessibilityLabel(String(format: NSLocalizedString("cd_capture_mode", comment: ""), titleText))
                .accessibilityAddTraits(isSelected ? [.isSelected, .isButton] : [.isButton])
            }
        }
        .padding(4)
        .background(Color.bgSurface.opacity(0.85))
        .clipShape(Capsule())
    }
}
