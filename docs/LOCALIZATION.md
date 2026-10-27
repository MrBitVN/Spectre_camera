# SpectreFlow - 100% English Global Localization Report

**Author:** Senior Mobile Systems Engineer  
**Date:** 2026-10-27  
**Status:** COMPLETED & VERIFIED  

---

## 1. Executive Summary

The entire SpectreFlow application on both **Android** and **iOS** is localized to **100% clean, natural, professional English**:
- **Default System Locale:** English (`en-US` / `en`).
- **Resource Bundles:** `res/values/strings.xml` and `res/values-vi/strings.xml` on Android and `Localizable.strings` (`en.lproj` and `vi.lproj`) on iOS contain purely English definitions.
- **System Permissions:** `Info.plist` (iOS) and runtime permission dialogs (Android & iOS) provide natural English camera and photo library descriptions.
- **Accessibility & Assistive Tech:** All TalkBack (`semantics { contentDescription }`) and VoiceOver (`.accessibilityLabel`) tags are localized into natural English.
- **Settings & Preferences:** The language selector is omitted from Settings, locking the app permanently and consistently to English across all device locales.
- **Typography & Font Rendering:** System fonts (Roboto on Android, San Francisco on iOS) provide clean, crisp typography with optimal layout margins.

---

## 2. Standardized English Terminology Table

| Feature / Concept | Standard English Term | Context & Screen Location |
|---|---|---|
| **Motion Blur** | **Motion Blur** (Short: **Blur**) | Mode Selector, Onboarding, Scene Detector |
| **Light Trails** | **Light Trails** (Short: **Trails**) | Mode Selector, Onboarding, Scene Detector |
| **Vanish** | **Vanish** (Short: **Vanish**) | Mode Selector, Onboarding, Scene Detector |
| **Auto** | **Auto** | Mode Selector (e.g., "Auto (Blur)") |
| **STABLE** | **STABLE** | Top Bar Gyro Stability Badge (Green) |
| **SHAKY** | **SHAKY** | Top Bar Gyro Stability Badge (Amber) |
| **Hold to expose** | **Press & hold to expose** | Bottom Shutter Banner & Shutter Button |
| **Hold longer to capture** | **Hold longer to capture (min 1s)** | Banner when released < 1s |
| **Release to capture** | **Release to capture • Max %ds** | Active Exposure Ring Shutter Hint |
| **Processing…** | **Synthesizing long exposure…** | Loading Animation & Synthesis Screen |
| **Capturing burst…** | **Capturing burst frames…** | Burst Stream Status Indicator |
| **Aligning frames…** | **Aligning frames with AI Vision…** | AI Alignment Phase Indicator |
| **Save** | **Save** | Result Screen Primary Action Button |
| **Share** | **Share** | Result Screen Outlined Action Button |
| **Share Video** | **Share Video** | Result Screen Companion Video Share Button |
| **Wallpaper** | **Wallpaper** | Result Screen Looping Wallpaper Export |
| **Discard** | **Discard** | Result Screen & Processing Cancel Action |
| **Pick as Result** | **Pick as Result** | Filmstrip Scrub Alternative Frame Selector |
| **Long Exposure Composite** | **Long Exposure Composite** | Filmstrip Result Header |
| **Settings** | **Settings** | Top Bar Button & Settings Screen Header |
| **Rule of Thirds Grid** | **Rule of Thirds Grid** | Settings Screen Switch |
| **Haptic Feedback** | **Haptic Feedback** | Settings Screen Switch |
| **Save Companion Video** | **Save Companion Video** | Settings Screen Switch |
| **Output Resolution** | **Output Resolution** | Settings Screen Segment (High / Balanced) |
| **App Icon Theme** | **App Icon Theme** | Settings Screen Icon Picker |
| **Pro Unlocked** | **SpectreFlow Pro Unlocked** | Settings Screen Subscription Status |
| **View Tutorial** | **View Tutorial** | Settings Screen Tutorial Trigger |

---

## 3. String Audit & Conversion Count

Every file across both Android and iOS codebases was audited for user-facing strings, TalkBack/VoiceOver descriptions, toasts, dialogs, and labels.

### 3.1 Android Components
- `longexposure_android/app/src/main/res/values/strings.xml`: 100% English resources.
- `longexposure_android/app/src/main/res/values-vi/strings.xml`: 100% English resources.
- `MainActivity.kt`: Error dialogs, stack trace inspection, copy log toast, share chooser titles, and `applyLocale(Locale("en", "US"))`.
- `MainViewModel.kt`: Capture failure hint and error dialog strings.
- `AppSettings.kt`: `AppLanguage.ENGLISH` and English theme display names ("Obsidian Emerald (Default)", "Cyber Mint (Pro)", "Deep Gold (Pro)").
- `SettingsScreen.kt`: Settings toggles and chip selectors.
- `ResultScreen.kt`: Export, share, wallpaper, and filmstrip scrub actions.

### 3.2 iOS Components
- `longexposure_ios/SpectreFlow/Resources/en.lproj/Localizable.strings`: 100% English dictionary.
- `longexposure_ios/SpectreFlow/Resources/vi.lproj/Localizable.strings`: 100% English dictionary.
- `longexposure_ios/SpectreFlow/Info.plist`: `CFBundleDevelopmentRegion = en`, English camera and photo library descriptions.
- `longexposure_ios/SpectreFlow.xcodeproj/project.pbxproj`: `developmentRegion = en;`.
- `longexposure_ios/SpectreFlow/Model/AppSettings.swift`: `AppLanguage.english` and English theme display names.
- `longexposure_ios/SpectreFlow/ViewModel/CameraViewModel.swift`: `onError("No frames available")`.

---

## 4. Verification

- All non-ASCII characters across both Android and iOS source codebases verified to be exclusively typographical symbols (`…`, `•`).
- Zero hardcoded non-English strings remaining in either repository.
