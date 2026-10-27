import SwiftUI

public struct DurationSelector: View {
    @Binding public var selectedDuration: CaptureDuration
    public let isProUnlocked: Bool
    public let onSelect: (CaptureDuration) -> Void

    public var body: some View {
        HStack(spacing: 4) {
            ForEach(CaptureDuration.allCases) { duration in
                let isSelected = duration == selectedDuration
                let isLocked = duration.isProOnly && !isProUnlocked

                Button {
                    onSelect(duration)
                } label: {
                    HStack(spacing: 2) {
                        Text(duration.label)
                            .font(.system(size: 13, weight: isSelected ? .bold : .medium))
                            .foregroundColor(
                                isSelected ? Color.bgObsidian : (isLocked ? Color.textSecondary.opacity(0.5) : Color.textPrimary)
                            )

                        if isLocked {
                            Text("PRO")
                                .font(.system(size: 8, weight: .bold))
                                .foregroundColor(.accentAmber)
                                .padding(.horizontal, 3)
                                .padding(.vertical, 1)
                                .background(Color.accentAmber.opacity(0.2))
                                .clipShape(RoundedRectangle(cornerRadius: 3))
                        }
                    }
                    .padding(.horizontal, 10)
                    .padding(.vertical, 6)
                    .background(isSelected ? Color.accentEmerald : Color.clear)
                    .clipShape(Capsule())
                }
                .buttonStyle(.plain)
                .accessibilityLabel(isLocked ? String(format: NSLocalizedString("cd_duration_pro", comment: ""), duration.label) : String(format: NSLocalizedString("cd_duration", comment: ""), duration.label))
                .accessibilityAddTraits(isSelected ? [.isSelected, .isButton] : [.isButton])
            }
        }
        .padding(4)
        .background(Color.bgSurface.opacity(0.85))
        .clipShape(Capsule())
        .overlay(
            Capsule()
                .stroke(Color.borderSubtle, lineWidth: 1)
        )
    }
}
