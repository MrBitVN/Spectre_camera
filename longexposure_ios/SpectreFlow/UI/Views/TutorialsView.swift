import SwiftUI

public struct TutorialItemData: Identifiable {
    public let id: Int
    public let part1: String
    public let boldPart: String
    public let part2: String
    public let gradient: LinearGradient
    public let iconName: String
    public let modeName: String
    public let steps: [String]
}

/**
 * Tutorials Screen matching reference UI (Image 2):
 * - Top bar: circular back button on left, title "Tutorials".
 * - Intro text: "These guides show you the power of the AI-augmented camera:"
 * - 3 rounded dark-grey cards with vector thumbnail on left and bold title phrase on right.
 * - Tapping a card opens a modal detailing 3-4 steps.
 */
public struct TutorialsView: View {
    public let onDismiss: () -> Void
    @State private var activeTutorial: TutorialItemData? = nil

    private let tutorials: [TutorialItemData] = [
        TutorialItemData(
            id: 1,
            part1: NSLocalizedString("tutorial_1_title_part1", comment: ""),
            boldPart: NSLocalizedString("tutorial_1_title_bold", comment: ""),
            part2: "",
            gradient: LinearGradient(colors: [Color(red: 0.85, green: 0.3, blue: 0.0), Color(red: 0.5, green: 0.1, blue: 0.0)], startPoint: .top, endPoint: .bottom),
            iconName: "person.2.slash.fill",
            modeName: NSLocalizedString("mode_vanish", comment: ""),
            steps: [
                NSLocalizedString("tutorial_1_step1", comment: ""),
                NSLocalizedString("tutorial_1_step2", comment: ""),
                NSLocalizedString("tutorial_1_step3", comment: ""),
                NSLocalizedString("tutorial_1_step4", comment: "")
            ]
        ),
        TutorialItemData(
            id: 2,
            part1: NSLocalizedString("tutorial_2_title_part1", comment: ""),
            boldPart: NSLocalizedString("tutorial_2_title_bold", comment: ""),
            part2: "",
            gradient: LinearGradient(colors: [Color(red: 0.05, green: 0.25, blue: 0.7), Color(red: 0.9, green: 0.4, blue: 0.0)], startPoint: .top, endPoint: .bottom),
            iconName: "bolt.fill",
            modeName: NSLocalizedString("mode_light_trails", comment: ""),
            steps: [
                NSLocalizedString("tutorial_2_step1", comment: ""),
                NSLocalizedString("tutorial_2_step2", comment: ""),
                NSLocalizedString("tutorial_2_step3", comment: ""),
                NSLocalizedString("tutorial_2_step4", comment: "")
            ]
        ),
        TutorialItemData(
            id: 3,
            part1: NSLocalizedString("tutorial_3_title_part1", comment: ""),
            boldPart: NSLocalizedString("tutorial_3_title_bold", comment: ""),
            part2: NSLocalizedString("tutorial_3_title_part2", comment: ""),
            gradient: LinearGradient(colors: [Color(red: 0.05, green: 0.45, blue: 0.35), Color(red: 0.0, green: 0.35, blue: 0.4)], startPoint: .top, endPoint: .bottom),
            iconName: "drop.fill",
            modeName: NSLocalizedString("mode_motion_blur", comment: ""),
            steps: [
                NSLocalizedString("tutorial_3_step1", comment: ""),
                NSLocalizedString("tutorial_3_step2", comment: ""),
                NSLocalizedString("tutorial_3_step3", comment: ""),
                NSLocalizedString("tutorial_3_step4", comment: "")
            ]
        )
    ]

    public init(onDismiss: @escaping () -> Void) {
        self.onDismiss = onDismiss
    }

    public var body: some View {
        ZStack {
            Color.bgObsidian.ignoresSafeArea()

            VStack(alignment: .leading, spacing: 0) {
                // Top Bar
                HStack {
                    Button(action: onDismiss) {
                        Image(systemName: "chevron.left")
                            .font(.system(size: 16, weight: .bold))
                            .foregroundColor(.textPrimary)
                            .frame(width: 38, height: 38)
                            .background(Color.bgSurface.opacity(0.85))
                            .clipShape(Circle())
                    }

                    Spacer()

                    Text(NSLocalizedString("tutorials_title", comment: ""))
                        .font(.system(size: 18, weight: .bold))
                        .foregroundColor(.textPrimary)

                    Spacer()

                    // Balance layout
                    Color.clear.frame(width: 38, height: 38)
                }
                .padding(.horizontal, 20)
                .padding(.top, 16)

                ScrollView {
                    VStack(alignment: .leading, spacing: 22) {
                        // Intro Text
                        Text(NSLocalizedString("tutorials_intro", comment: ""))
                            .font(.system(size: 15))
                            .foregroundColor(.textSecondary)
                            .lineSpacing(4)
                            .padding(.top, 18)
                            .padding(.horizontal, 4)

                        // 3 Rounded Dark Cards
                        ForEach(tutorials) { item in
                            Button {
                                activeTutorial = item
                            } label: {
                                HStack(spacing: 16) {
                                    // Thumbnail Artwork
                                    ZStack {
                                        RoundedRectangle(cornerRadius: 10)
                                            .fill(item.gradient)
                                            .frame(width: 68, height: 94)

                                        Image(systemName: item.iconName)
                                            .font(.system(size: 28))
                                            .foregroundColor(.white.opacity(0.85))
                                    }

                                    // Title with bold phrase
                                    (Text(item.part1)
                                        .foregroundColor(Color(white: 0.85))
                                    + Text(item.boldPart)
                                        .bold()
                                        .foregroundColor(.white)
                                    + Text(item.part2)
                                        .foregroundColor(Color(white: 0.85)))
                                    .font(.system(size: 15))
                                    .lineSpacing(3)
                                    .multilineTextAlignment(.leading)

                                    Spacer()
                                }
                                .padding(14)
                                .background(Color.bgSurface.opacity(0.85))
                                .cornerRadius(16)
                                .overlay(
                                    RoundedRectangle(cornerRadius: 16)
                                        .stroke(Color(white: 0.2), lineWidth: 1)
                                )
                            }
                            .buttonStyle(PlainButtonStyle())
                        }
                    }
                    .padding(.horizontal, 20)
                    .padding(.bottom, 36)
                }
            }
        }
        .sheet(item: $activeTutorial) { tutorial in
            TutorialDetailSheet(tutorial: tutorial)
        }
    }
}

public struct TutorialDetailSheet: View {
    public let tutorial: TutorialItemData
    @Environment(\.dismiss) private var dismiss

    public var body: some View {
        ZStack {
            Color.bgObsidian.ignoresSafeArea()

            VStack(spacing: 20) {
                HStack {
                    Text(tutorial.modeName)
                        .font(.system(size: 20, weight: .bold))
                        .foregroundColor(.accentEmerald)

                    Spacer()

                    Button {
                        dismiss()
                    } label: {
                        Image(systemName: "xmark.circle.fill")
                            .font(.system(size: 24))
                            .foregroundColor(.textSecondary)
                    }
                }
                .padding(.top, 24)
                .padding(.horizontal, 22)

                VStack(alignment: .leading, spacing: 14) {
                    ForEach(0..<tutorial.steps.count, id: \.self) { idx in
                        HStack(alignment: .top, spacing: 12) {
                            Text("\(idx + 1)")
                                .font(.system(size: 11, weight: .bold))
                                .foregroundColor(.accentEmerald)
                                .frame(width: 22, height: 22)
                                .background(Color.accentEmerald.opacity(0.2))
                                .clipShape(Circle())

                            Text(tutorial.steps[idx])
                                .font(.system(size: 14))
                                .foregroundColor(.textPrimary)
                                .lineSpacing(3)
                        }
                    }
                }
                .padding(.horizontal, 22)
                .frame(maxWidth: .infinity, alignment: .leading)

                Spacer()

                Button {
                    dismiss()
                } label: {
                    Text(NSLocalizedString("action_start_shooting", comment: ""))
                        .font(.system(size: 16, weight: .bold))
                        .foregroundColor(.bgObsidian)
                        .frame(maxWidth: .infinity)
                        .padding(.vertical, 14)
                        .background(Color.accentEmerald)
                        .cornerRadius(12)
                }
                .padding(.horizontal, 22)
                .padding(.bottom, 24)
            }
        }
        .presentationDetents([.medium, .large])
    }
}
