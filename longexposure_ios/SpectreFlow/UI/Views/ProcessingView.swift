import SwiftUI

public struct ProcessingView: View {
    public let message: String
    public let progress: Float
    public let onCancel: () -> Void

    public var body: some View {
        ZStack {
            Color.bgObsidian.ignoresSafeArea()

            VStack(spacing: 24) {
                ProgressView()
                    .progressViewStyle(CircularProgressViewStyle(tint: .accentEmerald))
                    .scaleEffect(1.8)

                Text(message)
                    .font(.system(size: 16, weight: .semibold))
                    .foregroundColor(.textPrimary)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 24)

                // Progress Bar
                ProgressView(value: progress, total: 1.0)
                    .accentColor(.accentEmerald)
                    .frame(width: 220)

                Text("\(Int(progress * 100))%")
                    .font(.system(size: 13))
                    .foregroundColor(.textSecondary)

                Button(action: onCancel) {
                    Text(NSLocalizedString("action_discard", comment: ""))
                        .font(.system(size: 14, weight: .bold))
                        .foregroundColor(.statusError)
                        .padding(.horizontal, 24)
                        .padding(.vertical, 10)
                        .background(Color.statusError.opacity(0.15))
                        .clipShape(Capsule())
                }
                .padding(.top, 16)
            }
        }
    }
}
