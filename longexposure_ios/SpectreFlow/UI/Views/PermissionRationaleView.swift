import SwiftUI

public struct PermissionRationaleView: View {
    public let onGrant: () -> Void

    public var body: some View {
        ZStack {
            Color.bgObsidian.opacity(0.85).ignoresSafeArea()

            VStack(spacing: 20) {
                Image(systemName: "camera.fill")
                    .font(.system(size: 48))
                    .foregroundColor(.accentEmerald)

                Text(NSLocalizedString("permission_camera_title", comment: ""))
                    .font(.system(size: 18, weight: .bold))
                    .foregroundColor(.textPrimary)
                    .multilineTextAlignment(.center)

                Text(NSLocalizedString("permission_camera_desc", comment: ""))
                    .font(.system(size: 14))
                    .foregroundColor(.textSecondary)
                    .multilineTextAlignment(.center)
                    .padding(.horizontal, 16)

                Button(action: onGrant) {
                    Text(NSLocalizedString("permission_grant", comment: ""))
                        .font(.system(size: 15, weight: .bold))
                        .foregroundColor(.bgObsidian)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 12)
                        .background(Color.accentEmerald)
                        .clipShape(RoundedRectangle(cornerRadius: 14))
                }
                .padding(.top, 12)
            }
            .padding(24)
            .background(Color.bgCard)
            .clipShape(RoundedRectangle(cornerRadius: 24))
            .padding(.horizontal, 32)
        }
    }
}
