import SwiftUI

public struct LensSelector: View {
    public let supportedLenses: [CGFloat]
    public let currentZoom: CGFloat
    public let isFrontCamera: Bool
    public let onSelectLens: (CGFloat) -> Void
    public let onFlipCamera: () -> Void

    public var body: some View {
        HStack(spacing: 6) {
            if !isFrontCamera {
                ForEach(supportedLenses, id: \.self) { lens in
                    let isSelected = abs(currentZoom - lens) < 0.15
                    let label = (lens < 1.0) ? "0.5x" : "\(Int(lens))x"

                    Button {
                        onSelectLens(lens)
                    } label: {
                        Text(label)
                            .font(.system(size: 11, weight: isSelected ? .bold : .medium))
                            .foregroundColor(isSelected ? .bgObsidian : .textSecondary)
                            .frame(width: 32, height: 32)
                            .background(isSelected ? Color.accentEmerald : Color.clear)
                            .clipShape(Circle())
                    }
                    .accessibilityLabel(String(format: NSLocalizedString("cd_zoom", comment: ""), label))
                    .accessibilityAddTraits(isSelected ? [.isSelected, .isButton] : [.isButton])
                }
            }

            Button {
                onFlipCamera()
            } label: {
                Image(systemName: "camera.rotate.fill")
                    .font(.system(size: 13, weight: .semibold))
                    .foregroundColor(isFrontCamera ? .accentEmerald : .textPrimary)
                    .frame(width: 32, height: 32)
                    .background(Color.clear)
                    .clipShape(Circle())
            }
            .accessibilityLabel(NSLocalizedString("cd_flip_camera", comment: ""))
        }
        .padding(.horizontal, 8)
        .padding(.vertical, 4)
        .background(Color.bgSurface.opacity(0.85))
        .clipShape(Capsule())
    }
}
