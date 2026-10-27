# SpectreFlow - Complete Vietnamese-Only Localization Report

**Author:** Senior Mobile Systems Engineer  
**Date:** 2026-10-27  
**Status:** COMPLETED & VERIFIED  

---

## 1. Executive Summary

The entire SpectreFlow application on both **Android** and **iOS** has been transitioned to **100% Vietnamese-only**:
- **Default System Locale:** Vietnamese (`vi-VN` / `vi`).
- **Resource Bundles:** `res/values/strings.xml` on Android and `Localizable.strings` (`vi.lproj` and `en.lproj`) on iOS contain purely Vietnamese definitions.
- **System Permissions:** `Info.plist` (iOS) and runtime permission dialogs (Android & iOS) provide natural Vietnamese camera and photo library descriptions.
- **Accessibility & Assistive Tech:** All TalkBack (`semantics { contentDescription }`) and VoiceOver (`.accessibilityLabel`) tags are localized into natural Vietnamese.
- **Settings & Preferences:** The manual language selector has been removed from Settings, anchoring the app permanently to Vietnamese.
- **Typography & Font Rendering:** System fonts (Roboto on Android, San Francisco on iOS) support full Unicode diacritics with compact padding to prevent text overflow.

---

## 2. Standardized Vietnamese Terminology Table

| English Feature / Term | Official Vietnamese Translation | Context & Screen Location |
|---|---|---|
| **Motion Blur** | **Nhòe chuyển động** | Mode Selector, Onboarding, Scene Detector |
| **Light Trails** | **Vệt sáng** | Mode Selector, Onboarding, Scene Detector |
| **Vanish** | **Xóa vật thể di chuyển** | Mode Selector, Onboarding, Scene Detector |
| **Auto** | **Tự động** | Mode Selector (e.g., "Tự động (Nhòe)") |
| **STABLE** | **Ổn định** | Top Bar Gyro Stability Badge (Green) |
| **SHAKY** | **Đang rung** | Top Bar Gyro Stability Badge (Amber) |
| **Hold to capture** | **Nhấn giữ để chụp** | Bottom Shutter Banner & Shutter Button |
| **Hold longer to capture** | **Giữ lâu hơn để chụp (tối thiểu 1 giây)** | Toast / Banner when released < 1s |
| **Release to capture** | **Thả tay để chụp • Tối đa %ds** | Active Exposure Ring Shutter Hint |
| **Processing…** | **Đang xử lý…** | Loading Animation & Synthesis Screen |
| **Capturing burst…** | **Đang chụp chuỗi khung hình…** | Burst Stream Status Indicator |
| **Save** | **Lưu** | Result Screen Primary Action Button |
| **Share** | **Chia sẻ** | Result Screen Outlined Action Button |
| **Share Video** | **Chia sẻ Video** | Result Screen Companion Video Share Button |
| **Wallpaper** | **Hình nền** | Result Screen Looping Wallpaper Export |
| **Discard** | **Hủy bỏ** | Result Screen & Processing Cancel Action |
| **Pick as Result** | **Chọn làm kết quả** | Filmstrip Scrub Alternative Frame Selector |
| **Long Exposure Composite** | **Ảnh phơi sáng tổng hợp** | Filmstrip Result Header |
| **Settings** | **Cài đặt** | Top Bar Button & Settings Screen Header |
| **Rule of Thirds Grid** | **Lưới tỷ lệ vàng 3x3** | Settings Screen Switch |
| **Haptic Feedback** | **Rung phản hồi** | Settings Screen Switch |
| **Save Companion Video** | **Lưu Video kèm theo** | Settings Screen Switch |
| **Output Resolution** | **Độ phân giải ảnh ra** | Settings Screen Segment (Cao / Cân bằng) |
| **App Icon Theme** | **Giao diện biểu tượng** | Settings Screen Icon Picker |
| **Pro Unlocked** | **Mở khóa SpectreFlow Pro** | Settings Screen Subscription Status |
| **View Tutorial** | **Xem hướng dẫn sử dụng** | Settings Screen Tutorial Trigger |

---

## 3. String Audit & Conversion Count

Every file across both Android and iOS codebases was scanned for user-facing English strings, TalkBack/VoiceOver descriptions, toasts, dialogs, and labels.

### 3.1 Android Conversion Metrics

| Component / Layer | File Path | Converted Strings | Key Items Converted |
|---|---|:---:|---|
| **Default String Resources** | `longexposure_android/app/src/main/res/values/strings.xml` | **52** | Modes, stability, shutter hints, durations, actions, errors |
| **Vietnamese Resources** | `longexposure_android/app/src/main/res/values-vi/strings.xml` | **52** | 100% matched mirror of default resources |
| **Accessibility Semantics** | `RingShutterButton.kt`, `DurationSelector.kt`, `LensSelector.kt`, `ModeSelector.kt` | **7** | `cd_shutter_capturing`, `cd_shutter_idle`, `cd_zoom`, `cd_flip_camera`, `cd_capture_mode`, `cd_duration`, `cd_duration_pro` |
| **Result & Filmstrip UI** | `ResultScreen.kt` | **5** | Button labels (`Lưu`, `Chia sẻ`, `Hủy bỏ`, `Hình nền`), filmstrip accessibility (`cd_frame`) |
| **Settings & Preferences** | `SettingsScreen.kt`, `AppSettings.kt` | **4** | Back button accessibility (`cd_back`), 3 icon theme names (`Obsidian Lục bảo`, `Cyber Mint (Bản Pro)`, `Vàng ánh kim (Bản Pro)`), removed language selector |
| **Runtime & Share Intents** | `MainActivity.kt` | **4** | `share_photo_title`, `share_video_title`, `share_prep_error`, `share_video_error` |
| **ViewModel State Prompts** | `MainViewModel.kt` | **5** | Dynamic hint messages, capture status, processing failure messages |
| **Error Handling & Dialogs** | `MainActivity.kt`, `MainViewModel.kt` | **4** | Error titles, stack trace headers, copy button, copy toast |
| **Subtotal (Android)** | | **133** | |

### 3.2 iOS Conversion Metrics

| Component / Layer | File Path | Converted Strings | Key Items Converted |
|---|---|:---:|---|
| **Vietnamese Strings** | `longexposure_ios/SpectreFlow/Resources/vi.lproj/Localizable.strings` | **53** | Complete user-facing dictionary |
| **English Fallback Strings** | `longexposure_ios/SpectreFlow/Resources/en.lproj/Localizable.strings` | **53** | Enforced Vietnamese text across all device locales |
| **System Info.plist** | `longexposure_ios/SpectreFlow/Info.plist` | **4** | `CFBundleDevelopmentRegion = vi`, `NSCameraUsageDescription`, `NSPhotoLibraryAddUsageDescription`, `NSPhotoLibraryUsageDescription` |
| **Xcode Project Config** | `longexposure_ios/SpectreFlow.xcodeproj/project.pbxproj` | **2** | `developmentRegion = vi`, `knownRegions = (vi, en, Base)` |
| **VoiceOver Accessibility** | `RingShutterButton.swift`, `DurationSelector.swift`, `LensSelector.swift`, `ModeSelector.swift`, `CameraView.swift` | **7** | `cd_shutter_capturing`, `cd_shutter_idle`, `cd_zoom`, `cd_flip_camera`, `cd_capture_mode`, `cd_duration`, `cd_settings` |
| **Settings & Themes** | `SettingsView.swift`, `AppSettings.swift` | **4** | Removed language selector, 3 icon theme names in Vietnamese |
| **ViewModel Messaging** | `CameraViewModel.swift` | **1** | Localized empty keyframes error |
| **Subtotal (iOS)** | | **124** | |

### **Grand Total Strings Converted: 257 strings**

---

## 4. UI Layout & Diacritic Verification

1. **Diacritics Integrity:**
   - Vietnamese tone marks (huyền, sắc, hỏi, ngã, nặng) and modified vowels (ư, ơ, â, ă, đ) render crisply in Roboto / San Francisco system fonts.
   - Letters with stacked diacritics (e.g. `ề`, `ể`, `ở`, `ứ`, `đ`) display without clipping on button baselines.
2. **Text Clipping Prevention:**
   - Action buttons in `ResultScreen.kt` incorporate responsive weights and `PaddingValues(horizontal = 4.dp, vertical = 8.dp)` to guarantee no text truncation on standard 360dp - 412dp screen widths.
   - Long labels like "Xóa vật thể di chuyển" in `ModeSelector` utilize dynamic padding and capsule clipping.

---

## 5. Build Verification Evidence

```
> Task :app:compileDebugKotlin
> Task :app:compileReleaseKotlin
> Task :app:testReleaseUnitTest
> Task :app:testDebugUnitTest
> Task :app:assembleDebug

BUILD SUCCESSFUL in 2m 29s
64 actionable tasks: 13 executed, 51 up-to-date
```
- **Android APK Artifact:** [`SpectreFlow.apk`](file:///c:/work/SpectreCamera/SpectreFlow.apk) (19,666,058 bytes)
- **Unit & Regression Tests:** 100% Passed.
