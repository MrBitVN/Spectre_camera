import SwiftUI

/**
 * Stability indicator matching reference UI:
 * - A smartphone icon enclosed inside 4 corner brackets [ 📱 ].
 * - Bright green & "STABLE" when steady.
 * - Dim amber/yellow & "SHAKY" when moving.
 */
public struct StabilityIndicator: View {
    public let state: StabilityState

    public init(state: StabilityState) {
        self.state = state
    }

    public var body: some View {
        let isStable = state == .stable
        let activeColor: Color = isStable ? .accentEmerald : Color(red: 1.0, green: 0.7, blue: 0.0)

        VStack(spacing: 2) {
            ZStack {
                // 4 Corner Brackets
                Canvas { context, size in
                    let strokeWidth: CGFloat = 2.0
                    let bracketLen: CGFloat = 7.0
                    let w = size.width
                    let h = size.height

                    // Top-Left ┌
                    var tl = Path()
                    tl.move(to: CGPoint(x: 0, y: bracketLen))
                    tl.addLine(to: CGPoint(x: 0, y: 0))
                    tl.addLine(to: CGPoint(x: bracketLen, y: 0))
                    context.stroke(tl, with: .color(activeColor), style: StrokeStyle(lineWidth: strokeWidth, lineCap: .round))

                    // Top-Right ┐
                    var tr = Path()
                    tr.move(to: CGPoint(x: w - bracketLen, y: 0))
                    tr.addLine(to: CGPoint(x: w, y: 0))
                    tr.addLine(to: CGPoint(x: w, y: bracketLen))
                    context.stroke(tr, with: .color(activeColor), style: StrokeStyle(lineWidth: strokeWidth, lineCap: .round))

                    // Bottom-Left └
                    var bl = Path()
                    bl.move(to: CGPoint(x: 0, y: h - bracketLen))
                    bl.addLine(to: CGPoint(x: 0, y: h))
                    bl.addLine(to: CGPoint(x: bracketLen, y: h))
                    context.stroke(bl, with: .color(activeColor), style: StrokeStyle(lineWidth: strokeWidth, lineCap: .round))

                    // Bottom-Right ┘
                    var br = Path()
                    br.move(to: CGPoint(x: w - bracketLen, y: h))
                    br.addLine(to: CGPoint(x: w, y: h))
                    br.addLine(to: CGPoint(x: w, y: h - bracketLen))
                    context.stroke(br, with: .color(activeColor), style: StrokeStyle(lineWidth: strokeWidth, lineCap: .round))
                }
                .frame(width: 34, height: 34)

                // Phone Outline
                RoundedRectangle(cornerRadius: 2.5)
                    .stroke(activeColor, lineWidth: 1.5)
                    .frame(width: 13, height: 21)
            }
            .frame(width: 38, height: 38)

            Text(isStable ? NSLocalizedString("stability_stable", comment: "") : NSLocalizedString("stability_shaky", comment: ""))
                .font(.system(size: 10, weight: .bold, design: .monospaced))
                .foregroundColor(activeColor)
                .tracking(0.8)
        }
    }
}
