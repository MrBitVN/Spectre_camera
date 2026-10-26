import SwiftUI

public struct StabilityBadge: View {
    public let state: StabilityState

    private var isStable: Bool {
        state == .stable
    }

    private var badgeColor: Color {
        isStable ? .accentEmerald : .accentAmber
    }

    public var body: some View {
        HStack(spacing: 6) {
            Circle()
                .fill(badgeColor)
                .frame(width: 8, height: 8)

            Text(NSLocalizedString(state.titleKey, comment: ""))
                .font(.system(size: 11, weight: .bold))
                .foregroundColor(badgeColor)
                .tracking(0.5)
        }
        .padding(.horizontal, 10)
        .padding(.vertical, 5)
        .background(Color.bgSurface.opacity(0.85))
        .clipShape(Capsule())
        .overlay(
            Capsule()
                .stroke(badgeColor.opacity(0.4), lineWidth: 1)
        )
        .animation(.easeInOut(duration: 0.25), value: state)
    }
}
