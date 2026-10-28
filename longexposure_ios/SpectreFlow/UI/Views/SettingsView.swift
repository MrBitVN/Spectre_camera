import SwiftUI

public struct SettingsView: View {
    @Binding public var settings: AppSettingsState
    public let onOpenOnboarding: () -> Void
    public let onDismiss: () -> Void

    public var body: some View {
        NavigationStack {
            ZStack {
                Color.bgObsidian.ignoresSafeArea()

                ScrollView {
                    VStack(alignment: .leading, spacing: 18) {
                        // 1. Tutorials Row
                        Button {
                            onOpenOnboarding()
                        } label: {
                            HStack {
                                Image(systemName: "questionmark.circle.fill")
                                    .foregroundColor(.accentEmerald)
                                    .font(.system(size: 20))

                                Text(NSLocalizedString("tutorials_title", comment: ""))
                                    .font(.system(size: 15, weight: .semibold))
                                    .foregroundColor(.textPrimary)

                                Spacer()

                                Image(systemName: "chevron.right")
                                    .foregroundColor(.textSecondary)
                                    .font(.system(size: 13, weight: .semibold))
                            }
                            .padding(14)
                            .background(Color.bgSurface.opacity(0.85))
                            .cornerRadius(14)
                        }

                        Divider().background(Color.borderSubtle)

                        // 2. Grid Toggle
                        Toggle(isOn: $settings.showGrid) {
                            Text(NSLocalizedString("setting_grid", comment: ""))
                                .foregroundColor(.textPrimary)
                        }
                        .tint(.accentEmerald)

                        Divider().background(Color.borderSubtle)

                        // 3. Haptics Toggle
                        Toggle(isOn: $settings.hapticsEnabled) {
                            Text(NSLocalizedString("setting_haptics", comment: ""))
                                .foregroundColor(.textPrimary)
                        }
                        .tint(.accentEmerald)

                        Divider().background(Color.borderSubtle)

                        // 4. Companion Video / Live Photo Toggle
                        Toggle(isOn: $settings.saveCompanionVideo) {
                            VStack(alignment: .leading, spacing: 4) {
                                Text(NSLocalizedString("setting_companion_video", comment: ""))
                                    .foregroundColor(.textPrimary)
                                Text(NSLocalizedString("setting_companion_video_desc", comment: ""))
                                    .font(.system(size: 12))
                                    .foregroundColor(.textSecondary)
                            }
                        }
                        .tint(.accentEmerald)

                        Divider().background(Color.borderSubtle)

                        // 5. Resolution / Photo Quality Setting
                        VStack(alignment: .leading, spacing: 8) {
                            Text(NSLocalizedString("setting_resolution", comment: ""))
                                .font(.system(size: 15, weight: .medium))
                                .foregroundColor(.textPrimary)

                            HStack(spacing: 12) {
                                ForEach(OutputResolution.allCases) { res in
                                    let isSelected = settings.resolution == res
                                    Button {
                                        settings.resolution = res
                                    } label: {
                                        Text(res == .high ? NSLocalizedString("resolution_high", comment: "") : NSLocalizedString("resolution_balanced", comment: ""))
                                            .font(.system(size: 13, weight: isSelected ? .bold : .normal))
                                            .foregroundColor(isSelected ? .accentEmerald : .textSecondary)
                                            .padding(.horizontal, 14)
                                            .padding(.vertical, 8)
                                            .background(isSelected ? Color.accentEmerald.opacity(0.18) : Color.bgCard)
                                            .clipShape(RoundedRectangle(cornerRadius: 10))
                                            .overlay(
                                                RoundedRectangle(cornerRadius: 10)
                                                    .stroke(isSelected ? Color.accentEmerald : Color.borderSubtle, lineWidth: 1)
                                            )
                                    }
                                }
                            }
                        }

                        Divider().background(Color.borderSubtle)

                        // 6. Custom App Icon Themes
                        VStack(alignment: .leading, spacing: 8) {
                            Text(NSLocalizedString("setting_app_icon", comment: ""))
                                .font(.system(size: 15, weight: .medium))
                                .foregroundColor(.textPrimary)

                            HStack(spacing: 10) {
                                ForEach(AppIconTheme.allCases) { iconTheme in
                                    let isSelected = settings.selectedAppIcon == iconTheme
                                    let isLocked = iconTheme != .obsidianEmerald && !settings.isProUnlocked

                                    Button {
                                        if !isLocked {
                                            settings.selectedAppIcon = iconTheme
                                        }
                                    } label: {
                                        Text(iconTheme.displayName)
                                            .font(.system(size: 12, weight: isSelected ? .bold : .normal))
                                            .foregroundColor(isSelected ? .accentEmerald : (isLocked ? .textSecondary.opacity(0.5) : .textSecondary))
                                            .padding(.horizontal, 12)
                                            .padding(.vertical, 8)
                                            .background(isSelected ? Color.accentEmerald.opacity(0.18) : Color.bgCard)
                                            .clipShape(RoundedRectangle(cornerRadius: 10))
                                            .overlay(
                                                RoundedRectangle(cornerRadius: 10)
                                                    .stroke(isSelected ? Color.accentEmerald : Color.borderSubtle, lineWidth: 1)
                                            )
                                    }
                                }
                            }
                        }

                        Divider().background(Color.borderSubtle)

                        // 7. Pro Unlock Toggle (Stubbed for StoreKit 2)
                        Toggle(isOn: $settings.isProUnlocked) {
                            VStack(alignment: .leading, spacing: 4) {
                                Text(NSLocalizedString("setting_pro_unlocked", comment: ""))
                                    .foregroundColor(.textPrimary)
                                Text(NSLocalizedString("setting_pro_desc", comment: ""))
                                    .font(.system(size: 12))
                                    .foregroundColor(.textSecondary)
                            }
                        }
                        .tint(.accentEmerald)

                        Divider().background(Color.borderSubtle)

                        // 8. About SpectreFlow
                        HStack(alignment: .top, spacing: 12) {
                            Image(systemName: "info.circle.fill")
                                .foregroundColor(.accentEmerald)
                                .font(.system(size: 18))
                                .padding(.top, 2)

                            VStack(alignment: .leading, spacing: 4) {
                                Text(NSLocalizedString("setting_about", comment: "") + " v1.0.0")
                                    .font(.system(size: 14, weight: .semibold))
                                    .foregroundColor(.textPrimary)
                                Text(NSLocalizedString("setting_about_desc", comment: ""))
                                    .font(.system(size: 12))
                                    .foregroundColor(.textSecondary)
                                    .lineSpacing(3)
                            }
                        }
                        .padding(14)
                        .background(Color.bgSurface.opacity(0.85))
                        .cornerRadius(14)
                    }
                    .padding(20)
                }
            }
            .navigationTitle(NSLocalizedString("settings_title", comment: ""))
            .navigationBarTitleDisplayMode(.inline)
            .toolbar {
                ToolbarItem(placement: .navigationBarTrailing) {
                    Button(action: onDismiss) {
                        Image(systemName: "xmark.circle.fill")
                            .foregroundColor(.textSecondary)
                    }
                }
            }
        }
    }
}
