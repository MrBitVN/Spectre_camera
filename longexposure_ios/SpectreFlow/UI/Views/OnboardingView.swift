import SwiftUI

public struct OnboardingView: View {
    public let onDismiss: () -> Void

    @State private var currentPage: Int = 0

    struct PageItem {
        let title: String
        let subtitle: String
        let systemImage: String
        let bullets: [String]
    }

    private var pages: [PageItem] {
        [
            PageItem(
                title: NSLocalizedString("onboarding_step1_title", comment: ""),
                subtitle: NSLocalizedString("onboarding_step1_subtitle", comment: ""),
                systemImage: "hand.tap.fill",
                bullets: [
                    NSLocalizedString("onboarding_step1_bullet1", comment: ""),
                    NSLocalizedString("onboarding_step1_bullet2", comment: ""),
                    NSLocalizedString("onboarding_step1_bullet3", comment: "")
                ]
            ),
            PageItem(
                title: NSLocalizedString("onboarding_step2_title", comment: ""),
                subtitle: NSLocalizedString("onboarding_step2_subtitle", comment: ""),
                systemImage: "gyroscope",
                bullets: [
                    NSLocalizedString("onboarding_step2_bullet1", comment: ""),
                    NSLocalizedString("onboarding_step2_bullet2", comment: ""),
                    NSLocalizedString("onboarding_step2_bullet3", comment: "")
                ]
            ),
            PageItem(
                title: NSLocalizedString("onboarding_step3_title", comment: ""),
                subtitle: NSLocalizedString("onboarding_step3_subtitle", comment: ""),
                systemImage: "sparkles",
                bullets: [
                    NSLocalizedString("onboarding_step3_bullet1", comment: ""),
                    NSLocalizedString("onboarding_step3_bullet2", comment: ""),
                    NSLocalizedString("onboarding_step3_bullet3", comment: "")
                ]
            )
        ]
    }

    public var body: some View {
        ZStack {
            Color.bgObsidian.ignoresSafeArea()

            VStack(spacing: 0) {
                // Top Skip Button
                HStack {
                    Spacer()
                    if currentPage < pages.count - 1 {
                        Button(NSLocalizedString("action_skip", comment: "")) {
                            onDismiss()
                        }
                        .foregroundColor(.textSecondary)
                        .font(.system(size: 15))
                        .padding(.trailing, 20)
                        .padding(.top, 10)
                    }
                }
                .frame(height: 44)

                Spacer()

                // Center Page
                let current = pages[currentPage]
                VStack(spacing: 24) {
                    ZStack {
                        Circle()
                            .fill(Color.bgSurface)
                            .frame(width: 100, height: 100)

                        Image(systemName: current.systemImage)
                            .font(.system(size: 46))
                            .foregroundColor(.accentEmerald)
                    }

                    VStack(spacing: 8) {
                        Text(current.title)
                            .font(.system(size: 24, weight: .bold))
                            .foregroundColor(.textPrimary)
                            .multilineTextAlignment(.center)

                        Text(current.subtitle)
                            .font(.system(size: 14, weight: .medium))
                            .foregroundColor(.accentMint)
                            .multilineTextAlignment(.center)
                    }

                    VStack(alignment: .leading, spacing: 14) {
                        ForEach(current.bullets, id: \.self) { bullet in
                            HStack(alignment: .top, spacing: 10) {
                                Circle()
                                    .fill(Color.accentEmerald)
                                    .frame(width: 6, height: 6)
                                    .padding(.top, 6)

                                Text(bullet)
                                    .font(.system(size: 14))
                                    .foregroundColor(.textSecondary)
                                    .lineSpacing(4)
                            }
                        }
                    }
                    .padding(20)
                    .background(Color.bgSurface.opacity(0.85))
                    .cornerRadius(16)
                    .padding(.horizontal, 24)
                }

                Spacer()

                // Bottom Page Indicators & Action Button
                VStack(spacing: 20) {
                    HStack(spacing: 8) {
                        ForEach(0..<pages.count, id: \.self) { index in
                            Capsule()
                                .fill(index == currentPage ? Color.accentEmerald : Color.textSecondary.opacity(0.3))
                                .frame(width: index == currentPage ? 24 : 6, height: 6)
                                .animation(.spring(), value: currentPage)
                        }
                    }

                    Button {
                        if currentPage < pages.count - 1 {
                            withAnimation {
                                currentPage += 1
                            }
                        } else {
                            onDismiss()
                        }
                    } label: {
                        Text(currentPage == pages.count - 1 ? NSLocalizedString("onboarding_get_started", comment: "") : NSLocalizedString("action_next", comment: ""))
                            .font(.system(size: 16, weight: .bold))
                            .foregroundColor(.bgObsidian)
                            .frame(maxWidth: .infinity)
                            .frame(height: 52)
                            .background(Color.accentEmerald)
                            .cornerRadius(14)
                    }
                    .padding(.horizontal, 24)
                }
                .padding(.bottom, 24)
            }
        }
    }
}
