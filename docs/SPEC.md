# SpectreFlow: AI Long Exposure Camera — Technical Specification

**Version:** 1.0.0  
**Target Platforms:** Native Android (Kotlin + Jetpack Compose + CameraX) & Native iOS (Swift + SwiftUI + AVFoundation)  
**Author:** Mobile Engineering Architecture Team  

---

## 1. Executive Summary & Brand Identity

### 1.1 App Overview
**SpectreFlow** is a native, computational photography camera application designed for both Android and iOS. It delivers tripod-quality long exposure photography on handheld mobile devices by combining:
1. Low-latency burst acquisition with sensor locking (AE/AF/AWB).
2. Real-time 6-DoF inertial motion sensing (Gyroscope/Accelerometer) for stability classification.
3. Multi-frame image alignment (phase correlation / homography registration) to cancel handheld tremor.
4. Memory-bounded streaming frame blending (Mean, Light Trails Max, and Temporal Median).
5. Lossless tone preservation and gallery export with EXIF metadata.

### 1.2 Brand Identity & Theme
- **App Name:** `SpectreFlow` (Sub-title: `AI Long Exposure Camera`)
- **Logo Iconography:** A minimalist obsidian aperture ring entwined with a flowing neon emerald streak (`#00E676`), symbolizing the continuous flow of light photons over time.
- **Color Palette (Dark UI with Emerald/Mint accents):**
  - **Background (Deep Obsidian):** `#0C0E10` (Surface dark: `#14171A`, Card/Sheet: `#1D2228`)
  - **Primary Accent (Neon Emerald):** `#00E676` (RGB: 0, 230, 118)
  - **Secondary Glow (Mint Flow):** `#69F0AE` (RGB: 105, 240, 174)
  - **Stability State Green:** `#00E676` ("STABLE")
  - **Stability State Amber:** `#FFAB00` ("SHAKY")
  - **Destructive/Cancel Red:** `#FF5252`
  - **Text Primary:** `#F8FAFC` (Slate 50), **Text Secondary:** `#94A3B8` (Slate 400), **Borders:** `#2E3842`

---

## 2. Shared System Architecture & Data Flow

### 2.1 Shutter Interaction Model: "Press-and-Hold to Expose, Release to Capture"
SpectreFlow uses an intuitive, responsive press-and-hold tactile exposure interaction:
1. **Touch Down / Press Start:**
   - Shutter button (`RingShutterButton`) intercepts pointer down.
   - Triggers light haptic impulse (`UIImpactFeedbackGenerator` / `VibrationEffect`).
   - Locks hardware AF, AE, and AWB parameters at touch instant to ensure constant luminance.
   - Clears prior blenders and starts burst frame streaming.
2. **Holding / Exposure Phase:**
   - Real-time elapsed timer is rendered directly in the center of the shutter (`00:07`).
   - Outer neon emerald ring progressively sweeps clockwise toward the selected maximum duration cap (3s, 5s, 9s, 15s, 30s).
   - Frames are streamed and accumulated in $O(1)$ memory without frame queue backlog.
3. **Touch Up / Shutter Release:**
   - **Minimum Hold Constraint (1.0s):** If the button is released before 1.0 second has elapsed, the burst is discarded with a user notification *"Hold longer to capture (min 1s)"* / *"Giữ lâu hơn để chụp (tối thiểu 1 giây)"*.
   - **Hold $\ge$ 1.0s:** Halts frame streaming immediately, restores continuous camera focus/metering, aligns accumulated frames, and transitions to the Processing Screen to synthesize the composite image.
4. **Maximum Duration Auto-Finish:**
   - The chosen duration (3s, 5s, 9s, 15s Pro, 30s Pro) acts as the hard ceiling cap.
   - When reached, capture automatically finishes as if released, proceeding to alignment and blending.
5. **Safety & Cancellation Handlers:**
   - **Slide Out:** If the user drags their finger $> 1.4\times$ outside the button radius, capture safely cancels, all buffers are freed, and state resets.
   - **App Backgrounding & Interruptions:** `Lifecycle.Event.ON_PAUSE`/`ON_STOP` (Android) and `UIApplication.didEnterBackgroundNotification` (iOS) safely abort burst capture and unlock camera hardware with zero memory leaks or crashes.
   - **Double-Trigger Prevention:** Touch inputs are serialized with atomic capture flags preventing duplicate bursts.

```
                      +-----------------------------------+
                      |   SpectreFlow Camera UI Layer     |
                      |  (Compose View / SwiftUI Canvas)  |
                      +-----------------+-----------------+
                                        |
               +------------------------+------------------------+
               |                                                 |
               v                                                 v
   +-----------------------+                         +-----------------------+
   |   Camera Subsystem    |                         |   Inertial Motion     |
   | (CameraX / AVFound.)  |                         |  (Sensor / CoreMotion)|
   +-----------+-----------+                         +-----------+-----------+
               | Fixed ISO/Shutter/AWB                           | 60Hz Gyro & Accel
               | Raw YUV/RGBA Frames                             v
               |                             +-----------------------------------+
               |                             | Stability Classifier (Tripod/Hand)|
               |                             +-----------------+-----------------+
               v                                               | Skip if Tripod
   +-----------------------------------------------------------v-----------------+
   |                     Frame Alignment Engine                                  |
   |   (Android: Phase Correlation/Homography | iOS: Vision Image Registration)  |
   +-----------------------------------+-----------------------------------------+
                                       | Aligned & Warped Frame
                                       v
   +-----------------------------------------------------------------------------+
   |                   Streaming Accumulation Blender                            |
   |   - Motion Blur: Progressive Welford/Mean Accumulator (O(1) Memory)         |
   |   - Light Trails: Per-Pixel Max Luminance Blend (O(1) Memory)               |
   |   - Vanish (Median): Chunked Temporal Subsampled Median Buffer              |
   +-----------------------------------+-----------------------------------------+
                                       | Composite Result Matrix
                                       v
   +-----------------------------------------------------------------------------+
   |                       Post-Processing & Output                              |
   |         - Border Crop to Valid Intersection Bounding Box                    |
   |         - Contrast & Color Enhancement (Vibrance & Gamma)                   |
   |         - MediaStore / PHPhotoLibrary Export with EXIF                      |
   +-----------------------------------------------------------------------------+
```

---

## 3. Mathematical Foundations & Blending Algorithms

### 3.1 Motion Blur Mode (Temporal Mean)
Produces silky water, flowing fog, wispy clouds, and ghosted human trajectories.
- **Streaming Formulation:** Rather than buffering all $N$ frames, we accumulate directly in high-precision float buffers:
  $$\mu_k(x, y) = \mu_{k-1}(x, y) + \frac{1}{k} \Big( I_k(x, y) - \mu_{k-1}(x, y) \Big)$$
  or via sum accumulation:
  $$S(x, y) = \sum_{k=1}^N I_k(x, y), \quad I_{final}(x, y) = \frac{1}{N} S(x, y)$$
- **Memory Footprint:** Exactly $W \times H \times 4$ floats (or $3 \times \text{float32}$ for RGB). $O(1)$ memory relative to time.

### 3.2 Light Trails Mode (Per-Pixel Maxima)
Preserves dynamic light streaks (fireworks, stars, vehicle taillights, sparklers) over dark backgrounds without over-exposing stationary surroundings.
- **Blending Formula:**
  $$I_{out}(x, y, c) = \max \left( I_{acc}(x, y, c), \; I_k(x, y, c) \right) \quad \forall c \in \{R, G, B\}$$
  To preserve color fidelity under non-uniform illumination, luminance gating can optionally be evaluated:
  $$L(p) = 0.2126 R + 0.7152 G + 0.0722 B$$
  $$I_{acc}(x, y) = \begin{cases} I_k(x, y) & \text{if } L(I_k(x, y)) > L(I_{acc}(x, y)) \\ I_{acc}(x, y) & \text{otherwise} \end{cases}$$
- **Memory Footprint:** Exactly 1 output frame buffer ($W \times H \times 4$ bytes RGBA8888 or Float32). $O(1)$ memory.

### 3.3 Vanish Mode (Temporal Median)
Removes transient objects (walking tourists, crossing cars, birds) leaving static architecture and landscapes unobstructed.
- **Problem:** True median requires sorting all pixel observations across $N$ frames:
  $$I_{median}(x, y) = \text{median} \left( \{ I_1(x, y), I_2(x, y), \dots, I_N(x, y) \} \right)$$
  For 30s at 15 fps = 450 frames. Storing 450 full 12MP frames requires:
  $$450 \times 12{,}000{,}000 \times 4 \approx 21.6 \text{ GB RAM (Instant Out-Of-Memory Crash!)}$$
- **Engineered Memory-Bounded Solution:**
  1. **Temporal Subsampling:** Sample frames at interval $\Delta t = \max(0.5s, \frac{T}{K})$, where $K \in [7, 15]$ frames. A tourist takes 2-4 seconds to traverse a field of view; 10-15 evenly spaced frames provide mathematically robust temporal rejection.
  2. **Tile-Chunked Sorting:** For high-resolution generation, memory is partitioned into horizontal scanline chunks or downscaled guide estimation with selective tile sorting, maintaining total heap allocation under 80 MB.
  3. **Sorting Algorithm:** Quickselect / partial insertion sort per pixel vector $\mathbf{v}_{x,y} \in \mathbb{R}^K$. Median index $m = \lfloor K/2 \rfloor$.

---

## 4. Inertial Stability Classification & Alignment Strategy

### 4.1 Inertial Motion Detection
- **Sensors:** 3-axis Accelerometer + 3-axis Gyroscope sampled at 60 Hz.
- **Metric Formulation:**
  - Angular velocity magnitude: $\omega = \sqrt{\omega_x^2 + \omega_y^2 + \omega_z^2}$ (rad/s)
  - Linear acceleration fluctuation: $a_{var} = \text{Var}(\|a_t - g\|)$ over a 500ms sliding window.
- **Classification Thresholds:**
  - $\omega < 0.035 \text{ rad/s}$ AND $a_{var} < 0.015 \text{ m/s}^2$ $\rightarrow$ **STABLE** (Tripod / resting surface).
  - Otherwise $\rightarrow$ **SHAKY** (Handheld tremor).
- **Optimization:** If classified as **STABLE**, the computationally intensive homography warp step is bypassed, saving up to 70% processing time and thermal throttling.

### 4.2 Auto-Alignment Pipeline
- **Reference Frame:** The first stable frame $I_0$ acts as anchor.
- **Transformation Model:**
  - Handheld micro-tremors are predominantly 2D translational jitter with slight in-plane roll: affine/projective homography $\mathbf{H}_k \in \mathbb{R}^{3 \times 3}$.
- **Platform Implementations:**
  - **Android:** Downscaled luminance phase correlation or FAST/ORB keypoints with RANSAC homography estimation, followed by affine image warp.
  - **iOS:** Apple Vision `VNTranslationalImageRegistrationRequest` or `VNHomographicImageRegistrationRequest` to compute registration matrices $\mathbf{M}_k$, mapped to `CIImage` transform or Metal vertex grid.
- **Border Cropping:**
  Each alignment transform shifts frame boundaries. The cumulative valid bounding box $[x_{min}, y_{min}, x_{max}, y_{max}]$ is tracked:
  $$x_{min} = \max_{k}(\Delta x_k, 0), \quad x_{max} = W - \max_{k}(-\Delta x_k, 0)$$
  After final accumulation, the composite is cropped to this region to eliminate black boundary wedges.

---

## 5. Memory & Performance Budget

| Parameter | Handheld (Fast) | High Quality | Limit / Constraint |
|---|---|---|---|
| Frame Resolution | 1080p ($1920 \times 1080$) | Full Sensor ($3840 \times 2160$+) | OOM ceiling: 150 MB Android Heap |
| Sampling Rate | 10 - 15 fps | 5 - 10 fps | Thermal throttling safety |
| Buffer Model | Streaming Accumulator | Streaming Accumulator | Maximum 2 full buffers in RAM |
| Exposure Durations | 3s, 5s, 9s | 15s (Pro), 30s (Pro) | Non-Pro capped at 9s |
| Alignment Latency | < 25ms / frame | < 60ms / frame | Must keep up with burst queue |

---

## 6. Android Implementation Architecture (`/longexposure_android`)
- **Language:** Kotlin 2.0+
- **UI Framework:** Jetpack Compose (Material3 Dark Theme, Emerald Accent)
- **Architecture:** Clean MVVM + Repository Pattern + Kotlin Coroutines & StateFlow
- **Camera Stack:** CameraX `1.4.x` with `ImageAnalysis` (YUV_420_888 / RGBA_8888)
- **Lock Controls:** `CameraControl.startFocusAndMetering` to lock AF/AE during burst.
- **Processing Engine:** Kotlin Coroutines Dispatchers.Default + Native Render/Bitmap Array Blending Engine.
- **Storage:** `MediaStore.Images.Media` via Scoped Storage API (`IS_PENDING` workflow).

---

## 7. iOS Implementation Architecture (`/longexposure_ios`)
- **Language:** Swift 5.9+ / Swift 6
- **UI Framework:** SwiftUI (iOS 16+ NavigationStack, Dark Scheme)
- **Architecture:** MVVM with `@Observable` / `ObservableObject`, async/await concurrency
- **Camera Stack:** AVFoundation `AVCaptureSession` + `AVCaptureVideoDataOutput`
- **Lock Controls:** `AVCaptureDevice.lockForConfiguration()`:
  - `focusMode = .locked`
  - `exposureMode = .locked`
  - `whiteBalanceMode = .locked`
- **Sensors:** `CMMotionManager` device motion updates.
- **Processing Engine:** Vision (`VNImageRegistrationRequest`) + Core Image `CIContext` / vImage.
- **Storage:** `PHPhotoLibrary` with metadata creation request.

---

## 8. Localization & Feature Matrix

### 8.1 Dual Language Localization
- English (`en`): Default
- Vietnamese (`vi`): Complete strings for titles, capture modes ("Làm mờ chuyển động", "Vệt sáng", "Biến mất"), stability indicators ("ỔN ĐỊNH", "RUNG LẮC"), permissions, and settings.

### 8.2 Settings & Pro Tier Toggle
- Settings screen includes:
  - Shutter sound & Haptic feedback toggle
  - 3x3 Composition grid overlay toggle
  - Output Resolution: Standard (1080p), High (4K / Full)
  - Language selector: English / Tiếng Việt
  - Pro status toggle (`isProUnlocked: Boolean`) to unlock 15s and 30s durations.
  - Save Companion Video toggle (default ON).
  - Custom App Icon selection (Obsidian Emerald, Cyber Mint, Deep Gold).
  - View Interactive Tutorial button.

---

## 9. Advanced Capabilities (v1.1 Upgrades)

### 9.1 Auto Scene Detection Architecture
SpectreFlow introduces real-time scene understanding:
- **Sampling Engine:** Subsamples preview frames at ~2-3 Hz to $64 \times 64$ (Android) or $48 \times 48$ (iOS) luminance grids in < 2ms without impacting 60fps preview.
- **Metrics Extracted:**
  - $\bar{Y}$: Average scene luminance ($0.0 \dots 1.0$).
  - $R_{high}$: Percentage of saturated highlight points ($Y > 0.82$).
  - $M_{score}$: Frame-to-frame absolute delta metric indicating scene dynamics.
- **Classification Rules:**
  - Dark scene ($\bar{Y} < 0.38$) with bright spots ($R_{high} > 0.5\%$) and motion ($M_{score} > 0.02$) $\rightarrow$ **Light Trails**.
  - Bright daytime scene ($\bar{Y} \ge 0.35$) with heavy transit ($M_{score} > 0.08$) $\rightarrow$ **Vanish** (temporal median).
  - Subtle motion or generic daylight/landscape $\rightarrow$ **Motion Blur**.
- **Pluggable ML Interface:** Structured as `SceneDetector` (Kotlin) and `SceneDetectorProtocol` (Swift) allowing drop-in TensorFlow Lite or Core ML model replacements.
- **Default Mode:** `AUTO` is the default capture mode; active detected mode is displayed on the mode chip (e.g., `AUTO (Trails)`) and can be overridden by the user.

### 9.2 Companion Video & Live Photo Architecture
While holding to expose, frames are captured simultaneously for composite blending and recorded into video format:
- **iOS Implementation (Live Photo):**
  - Frames are ingested into an `AVAssetWriter` (H.264/HEVC) writing an auxiliary `.mov` video asset.
  - Exported to Photos via `PHAssetCreationRequest.forAsset()` with paired video resource (`.pairedVideo`), generating a native Apple Live Photo with long-exposure scrubbing.
- **Android Implementation (Photo + Separate MP4):**
  - **Design Decision:** Android is implemented as a **High-Resolution Photo + Separate High-Bitrate MP4 companion video** saved to `Movies/SpectreFlow/`. This provides 100% universal compatibility across Google Photos, Samsung Gallery, VLC, and third-party cloud backends without relying on proprietary OEM-specific motion photo container headers (like Samsung SEF or Pixel GCamera XMP micro-video trailers).
  - Uses hardware-accelerated `MediaCodec` (H.264 `video/avc`) with Surface input (`lockHardwareCanvas()`) and `MediaMuxer`.
- **Settings Toggle:** Both platforms provide a "Save companion video" switch (default ON).

### 9.3 Result Review Screen with Frame Scrubber
- Bounded keyframe cache (up to 16 representative burst frames) stored in memory.
- User can scrub horizontally through thumbnails or a continuous slider.
- **"Pick as Result" / "Chọn làm kết quả":** Allows selecting any individual frame from the burst as the primary cover photo to save or share.
- Action bar provides: **Save Photo**, **Share Photo**, **Share Video** (if companion video exists), and **Discard**.

### 9.4 Multi-Lens Switching & Camera Flip
- **Camera Flip:** Front/back toggle button flips sensor orientation smoothly.
- **Optical Zoom & Lens Switching:**
  - Dynamic discovery queries device capabilities (Ultra-wide 0.5x, Wide 1x, Telephoto 2x/3x).
  - Only lenses supported by hardware are displayed.
  - CameraX `setZoomRatio()` and AVFoundation `videoZoomFactor` handle seamless multi-camera logical switching.

### 9.5 Wide Color Gamut (Display P3)
- **iOS:** CIContext and PHPhotoLibrary configured with `CGColorSpace.displayP3` wide color profile.
- **Android:** Activity window configured with `ActivityInfo.COLOR_MODE_WIDE_COLOR_GAMUT` on Android 8+ (API 26+) and `ColorSpace.get(ColorSpace.Named.DISPLAY_P3)` when producing final bitmaps.

### 9.6 Pro Tier & In-App Purchase Architecture
- Structured around `BillingService` (Android) and `ProSubscriptionProtocol` (iOS).
- Decoupled from UI so Google Play Billing KTX and Apple StoreKit 2 can be plugged in without refactoring.
- Entitlements: Unlocks 15s/30s exposure caps and Custom App Icon themes.

### 9.7 Limited Photo Library Access Handling
- **Android 14 (API 34):** Scoped Storage uses `MediaStore` for direct writes without requiring broad read access. When limited access is detected, a banner explains that photos and companion videos save directly to the system gallery.
- **iOS 14+:** Handles `PHAuthorizationStatus.limited` gracefully with informational banner.

### 9.8 Accessibility (TalkBack & VoiceOver)
- Full accessibility semantics and content descriptions added for shutter button (stateful: idle vs holding elapsed time), mode chips, duration chips, and lens controls.

### 9.9 Shortcuts & App Intent (iOS)
- Implemented `TakeLongExposureIntent` conforming to Apple's `AppIntent` framework for Siri Shortcuts automation.

---
**End of Specification**
