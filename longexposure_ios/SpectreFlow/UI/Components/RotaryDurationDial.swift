import SwiftUI

/**
 * Rotary Duration Dial anchored in the bottom-right corner.
 *
 * Renders a quarter-circle arc with radial ticks for exposure caps (3, 5, 9, 15, 30s).
 * Selected value is rendered large in neon emerald. Locked values display a lock indicator.
 * Supports swiping along the dial and tapping with tactile haptic feedback.
 */
public struct RotaryDurationDial: View {
    @Binding public var selectedDuration: CaptureDuration
    public let isProUnlocked: Bool
    public let onSelect: (CaptureDuration) -> Void
    public let onLockedTapped: () -> Void

    private let durations: [CaptureDuration] = [.sec3, .sec5, .sec9, .sec15, .sec30]
    // Angles for the 5 points along the quarter circle arc (190° to 262°)
    private let angles: [Double] = [190.0, 208.0, 226.0, 244.0, 262.0]
    private let haptic = UISelectionFeedbackGenerator()

    public init(
        selectedDuration: Binding<CaptureDuration>,
        isProUnlocked: Bool,
        onSelect: @escaping (CaptureDuration) -> Void,
        onLockedTapped: @escaping () -> Void
    ) {
        self._selectedDuration = selectedDuration
        self.isProUnlocked = isProUnlocked
        self.onSelect = onSelect
        self.onLockedTapped = onLockedTapped
    }

    private func handleAngle(_ angleDeg: Double) {
        var minDiff = Double.greatestFiniteMagnitude
        var bestIndex = 0
        for i in angles.indices {
            let diff = abs(angleDeg - angles[i])
            if diff < minDiff {
                minDiff = diff
                bestIndex = i
            }
        }
        let target = durations[bestIndex]
        if target != selectedDuration {
            if target.isProOnly && !isProUnlocked {
                onLockedTapped()
            } else {
                haptic.selectionChanged()
                selectedDuration = target
                onSelect(target)
            }
        }
    }

    public var body: some View {
        ZStack {
            // Radial Guide Track & Ticks Canvas
            Canvas { context, size in
                let w = size.width
                let h = size.height
                let center = CGPoint(x: w, y: h)
                let arcRadius = w - 16

                // Quarter-circle arc path
                var path = Path()
                path.addArc(
                    center: center,
                    radius: arcRadius,
                    startAngle: .degrees(180),
                    endAngle: .degrees(270),
                    clockwise: false
                )
                context.stroke(path, with: .color(Color(white: 0.25).opacity(0.35)), lineWidth: 1.5)

                // Draw Ticks for each duration
                for (index, duration) in durations.enumerated() {
                    let angleDeg = angles[index]
                    let angleRad = angleDeg * .pi / 180.0
                    let isSelected = duration == selectedDuration
                    let isLocked = duration.isProOnly && !isProUnlocked

                    let innerR = arcRadius - (isSelected ? 10.0 : 6.0)
                    let outerR = arcRadius + (isSelected ? 4.0 : 2.0)

                    let startPoint = CGPoint(
                        x: center.x + innerR * cos(angleRad),
                        y: center.y + innerR * sin(angleRad)
                    )
                    let endPoint = CGPoint(
                        x: center.x + outerR * cos(angleRad),
                        y: center.y + outerR * sin(angleRad)
                    )

                    var tickPath = Path()
                    tickPath.move(to: startPoint)
                    tickPath.addLine(to: endPoint)

                    let tickColor: Color = isSelected ? .accentEmerald : (isLocked ? Color(white: 0.35) : Color(white: 0.55))
                    context.stroke(tickPath, with: .color(tickColor), style: StrokeStyle(lineWidth: isSelected ? 3.0 : 1.5, lineCap: .round))
                }
            }
            .frame(width: 130, height: 130)

            // Numerals & Lock Glyphs
            ForEach(0..<durations.count, id: \.self) { index in
                let duration = durations[index]
                let angleDeg = angles[index]
                let angleRad = angleDeg * .pi / 180.0
                let isSelected = duration == selectedDuration
                let isLocked = duration.isProOnly && !isProUnlocked

                let textDist = 130.0 - (isSelected ? 34.0 : 30.0)
                let xPos = 130.0 + textDist * cos(angleRad)
                let yPos = 130.0 + textDist * sin(angleRad)

                HStack(spacing: 2) {
                    if isLocked {
                        Image(systemName: "lock.fill")
                            .font(.system(size: isSelected ? 10 : 8))
                            .foregroundColor(Color(white: 0.45))
                    }
                    Text("\(duration.rawValue)")
                        .font(.system(size: isSelected ? 20 : 13, weight: isSelected ? .bold : .medium, design: .monospaced))
                        .foregroundColor(isSelected ? .accentEmerald : (isLocked ? Color(white: 0.35) : .textSecondary))
                }
                .position(x: xPos, y: yPos)
            }
        }
        .frame(width: 130, height: 130)
        .contentShape(Rectangle())
        .gesture(
            DragGesture(minimumDistance: 0)
                .onChanged { value in
                    let dx = value.location.x - 130.0
                    let dy = value.location.y - 130.0
                    var angle = atan2(dy, dx) * 180.0 / .pi
                    if angle < 0 { angle += 360.0 }
                    handleAngle(angle)
                }
        )
        .accessibilityLabel(NSLocalizedString("cd_duration_dial", comment: ""))
    }
}
