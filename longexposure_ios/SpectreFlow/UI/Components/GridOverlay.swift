import SwiftUI

public struct GridOverlay: View {
    public var body: some View {
        GeometryReader { proxy in
            let w = proxy.size.width
            let h = proxy.size.height
            let gridColor = Color.white.opacity(0.18)

            Path { path in
                // Vertical lines
                path.move(to: CGPoint(x: w / 3.0, y: 0))
                path.addLine(to: CGPoint(x: w / 3.0, y: h))

                path.move(to: CGPoint(x: w * 2.0 / 3.0, y: 0))
                path.addLine(to: CGPoint(x: w * 2.0 / 3.0, y: h))

                // Horizontal lines
                path.move(to: CGPoint(x: 0, y: h / 3.0))
                path.addLine(to: CGPoint(x: w, y: h / 3.0))

                path.move(to: CGPoint(x: 0, y: h * 2.0 / 3.0))
                path.addLine(to: CGPoint(x: w, y: h * 2.0 / 3.0))
            }
            .stroke(gridColor, lineWidth: 1)
        }
        .allowsHitTesting(false)
    }
}
