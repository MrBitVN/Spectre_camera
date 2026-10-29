# SpectreFlow (Spectre_camera)

> **Next-Generation Computational Long Exposure Camera for Android & iOS**  
> *100% Native Kotlin (Jetpack Compose + CameraX) & Native Swift (SwiftUI + AVFoundation)*

---

## 🌟 Overview

**SpectreFlow** brings DSLR-quality computational long exposure photography to mobile devices without requiring tripods or neutral density (ND) filters. By fusing low-latency sensor burst acquisition, 6-DoF inertial gyro motion detection, computer vision homography alignment, and streaming hardware-accelerated accumulators, SpectreFlow turns busy crowds invisible, smooths flowing water into silky fog, and weaves vehicle headlights into luminous light ribbons.

---

## 🚀 Key Features

### 1. Tactile "Press-and-Hold to Expose, Release to Capture" Interaction
- **Press & Hold:** Instantly locks hardware Auto-Exposure (AE), Auto-Focus (AF), and Auto-White Balance (AWB). Light haptic impulse confirms burst start.
- **Dynamic Shutter Ring:** Displays a real-time elapsed counter (`00:07`) with a neon emerald progress ring sweeping toward the selected exposure cap.
- **Release to Capture:** Captures are stopped immediately upon finger release. Minimum hold constraint is **1 second** (shorter taps display a non-blocking toast *"Hold longer to capture"*).
- **Auto-Finish Cap:** Automatically completes and synthesizes when the maximum cap is reached (3s, 5s, 9s, 15s Pro, 30s Pro).
- **Safety Cancellation:** Dragging the finger $>1.5\times$ outside the button radius, incoming phone calls, or app backgrounding safely cancels capture and frees all frame buffers without crashes.

### 2. Computational Blending Modes
- **Motion Blur (Mean Blend):** Averages luminance over time to turn ocean surf, waterfalls, and drifting clouds into silky mist.
- **Light Trails (Lighten / Max Blend):** Preserves peak luminance across frames to capture car headlights/taillights, firework bursts, and star trails.
- **Vanish (Temporal Median):** Calculates temporal median across sampled keyframes to erase walking pedestrians and passing vehicles from static architectural landmarks.
- **Auto Scene Detection:** Heuristic analyzer processes live preview luminance, histogram contrast, and inter-frame optical flow to automatically recommend the optimal mode.

### 3. AI Gyro Stabilization & Tripod Detection
- Real-time 6-axis gyroscope and accelerometer tracking.
- **STABLE (Green):** Indicates tripod or resting placement; bypasses CV alignment to optimize battery and speed.
- **SHAKY (Amber):** Activates multi-frame homography alignment / phase correlation to cancel hand tremor.

### 4. Companion Video & Live Photos
- **iOS:** Exports long exposures as paired Apple Live Photos (`PHAssetCreationRequest`) or companion MOV videos.
- **Android:** Records burst frames into high-efficiency H.264 MP4 companion videos for scrubbing and keyframe selection.
- **Cover Photo Selection:** Scrub individual burst frames on the result screen and pick any frame as the primary still cover.

### 5. Unified PhotoRepository & Full-Screen Gallery Viewer
- **Single Source of Truth:** Android uses `MediaStore` (`Pictures/SpectreFlow`, atomic `IS_PENDING`), while iOS uses `PHPhotoLibrary` custom album (`SpectreFlow`).
- **Live Sync:** Observes storage changes in real time via `ContentObserver` (Android) and `PHPhotoLibraryChangeObserver` (iOS).
- **Full-Screen Viewer:** Tapping the bottom-left camera thumbnail opens a swipeable horizontal pager (`HorizontalPager` / `TabView`) displaying newest photos first, with counter (`X of Y`), timestamp, Share sheet, and confirmation-guarded Delete.

### 6. Reference UI & Dark Obsidian Aesthetic
- **Color Palette:** Deep Obsidian (`#0C0E10`), Card Surface (`#1D2228`), Neon Emerald (`#00E676`), and Mint Flow (`#69F0AE`).
- **Thumb-Reachable Controls:** Rotary duration dial, floating zoom pill (`0.5x`, `1x`, `2x`, `3x`), and top-bar EV compensation slider (-2.0 EV to +2.0 EV).
- **Interactive Tutorials:** Built-in modal cards walking users through capturing Vanish, Light Trails, and Waterfall long exposures.

---

## 📁 Repository Structure

```
Spectre_camera/
├── SpectreFlow.apk                     # Prebuilt signed debug APK (ready to install)
├── docs/                               # Engineering documentation & assets
│   ├── SPEC.md                         # Architecture & data flow specification
│   ├── UI_SPEC.md                      # UI/UX design and interaction specs
│   ├── BUGFIX.md                       # Comprehensive bug fix and crash log analysis
│   ├── TEST_REPORT.md                  # Verification matrix & automated test reports
│   ├── AUDIT.md / FEATURE_AUDIT.md     # Feature parity checklist
│   └── logo.png / logo_squircle.png    # Original brand logos & iconography
│
├── longexposure_android/               # 100% Native Android Project
│   ├── app/
│   │   ├── src/main/java/com/spectreflow/app/
│   │   │   ├── MainActivity.kt         # Entry point & edge-to-edge Compose scaffold
│   │   │   ├── MainViewModel.kt        # State machine, capture loop & repository link
│   │   │   ├── camera/                 # CameraX streaming, zoom & AE/AF locks
│   │   │   ├── blender/                # Mean, Light Trails & Median blenders
│   │   │   ├── alignment/              # OpenCV / native image alignment
│   │   │   ├── sensor/                 # Gyroscope stability detector
│   │   │   ├── storage/                # PhotoRepository (MediaStore scoped storage)
│   │   │   └── ui/                     # Jetpack Compose screens & custom components
│   │   └── src/test/java/              # Unit test suite (HoldToExposeTest, etc.)
│   └── build.gradle.kts
│
└── longexposure_ios/                   # 100% Native iOS Project
    ├── SpectreFlow/
    │   ├── SpectreFlowApp.swift        # SwiftUI App entry point
    │   ├── ViewModel/CameraViewModel.swift
    │   ├── Camera/CameraService.swift  # AVFoundation burst streaming & device control
    │   ├── Blender/                    # Metal shaders & frame blender engine
    │   ├── Alignment/FrameAligner.swift
    │   ├── Sensor/MotionStabilityDetector.swift
    │   ├── Storage/                    # PhotoRepository & PhotoLibrarySaver (PHAsset)
    │   ├── UI/                         # SwiftUI views, rotary dial & gallery viewer
    │   └── Resources/                  # Localizable.strings (EN & VI)
    ├── SpectreFlowTests/               # XCTest suite
    └── SpectreFlow.xcodeproj           # Xcode project bundle
```

---

## 🛠️ Building & Running

### Android
- **Requirements:** Android Studio Hedgehog+ / JDK 17 / Android SDK 34 (minSdk 26).
- **CLI Build & Test:**
  ```powershell
  cd longexposure_android
  ./gradlew test assembleDebug
  ```
- **Run on Device:**
  ```powershell
  adb install -r app/build/outputs/apk/debug/app-debug.apk
  ```

### iOS
- **Requirements:** macOS with Xcode 15+ / iOS 16.0+ deployment target.
- **Build & Run:**
  1. Open `longexposure_ios/SpectreFlow.xcodeproj` in Xcode.
  2. Select your signing team under **Signing & Capabilities**.
  3. Choose an iOS device or simulator and press **Cmd + R**.

---

## 🧪 Automated Test Suite

Both platforms include unit tests validating the press-and-hold interaction, state machine transitions, rotary duration constraints, and unified storage synchronization:

- **Android:** Run `./gradlew testDebugUnitTest`
  - `testHoldDurationValidation`
  - `testProgressCalculation`
  - `testTimerFormatting`
  - `testCaptureStateMachineTransitions`
  - `testDialRotationAndProLockValidation`
  - `testSlideCancelSafety`
  - `testPhotoRepositoryViewerConsistency`
- **iOS:** Run Xcode Test (`Cmd + U`)
  - `HoldToExposeTests.swift`
  - `BlendingMathTests.swift`
  - `AlignmentMathTests.swift`

---

## 📄 License

Proprietary engineering codebase developed for the SpectreFlow camera project. Original branding, logos, and architecture.
