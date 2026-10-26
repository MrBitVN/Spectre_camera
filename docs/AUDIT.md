# SpectreFlow: AI Long Exposure Camera — Technical Audit & Known Limitations

**Audit Date:** October 26, 2026  
**Audited Components:** Native Android (Kotlin + Jetpack Compose + CameraX) & Native iOS (Swift + SwiftUI + AVFoundation)  
**Version:** 1.1.0  

---

## 1. Architectural Strengths & Compliance
- **Zero Cross-Platform Overhead:** Both applications are 100% native (Jetpack Compose + CameraX on Android; SwiftUI + AVFoundation + Metal on iOS).
- **Tactile Shutter Ergonomics:** "Press and hold to expose, release to capture" is fully non-blocking, asynchronous, and enforces the 1.0s minimum threshold and auto-finish ceiling.
- **Memory-Bounded Streaming Math:** Mean and Light Trails accumulate in $O(1)$ RAM buffers ($W \times H \times 4$), preventing Out-Of-Memory termination even during continuous 30s exposures.
- **Hardware Sensor Lock:** AF/AE/AWB locks prevent illumination flickering during multi-second frame bursts.
- **Hardware-Accelerated Companion Video:** Surface-input MediaCodec on Android and AVAssetWriter on iOS record companion video with negligible overhead.

---

## 2. Known Limitations & Technical Trade-offs

### 2.1 Android Companion Video: Separate MP4 vs. Embedded Motion Photo
- **Status:** Android saves long-exposure composites as JPEG + companion video as a separate MP4 in `Movies/SpectreFlow/`.
- **Reasoning:** Google Motion Photo format is not a formal ISO standard; it requires injecting Google Camera XMP tags (`GCamera:MicroVideo = 1`, `GCamera:MicroVideoOffset`) and concatenating the raw MP4 binary to the tail of the JPEG. This format is frequently stripped by non-Google cloud services (OneDrive, Dropbox) or unsupported by third-party Android gallery apps (Xiaomi Gallery, Vivo Albums).
- **Mitigation:** Saving a companion MP4 ensures 100% compatibility across all platforms and social media share sheets. An embedded XMP motion photo packer can be added as an optional export setting in a future update.

### 2.2 iOS Live Photo Cross-Platform Export
- **Status:** Live Photos are written via `PHAssetCreationRequest.forAsset()` with paired `.mov` resources.
- **Limitation:** When exported outside of the Apple ecosystem (e.g. AirDrop to a PC, cloud upload, or third-party messengers), the photo and video are treated as two separate files rather than an interactive Live Photo.

### 2.3 Handheld Tremor & Boundary Cropping
- **Status:** Translational image registration counteracts $(x, y)$ micro-tremors and small yaw/pitch vibrations.
- **Limitation:** If the user moves aggressively (e.g. walks or turns around during a 15s or 30s capture), the cumulative warp offset shrinks the intersection bounding box excessively.
- **Guidance:** The UI displays real-time gyroscope stability (`STABLE` vs `SHAKY`). Users are guided by the onboarding tutorial to hold reasonably steady or mount to a tripod for captures exceeding 9 seconds.

### 2.4 Temporal Median Subsampling (Vanish Mode)
- **Status:** True pixel median sorting across 300+ full-resolution 4K frames would require $> 2.5\text{ GB}$ of RAM, crashing 3GB and 4GB Android devices.
- **Trade-off:** Vanish mode uses a chunked reservoir (16-30 subsampled temporal keyframes). Fleeting pedestrians and cars are cleanly removed, but objects lingering in one position for $> 40\%$ of the exposure duration may leave faint translucent ghosting.

### 2.5 Camera2 Hardware Support Levels on Legacy Android Devices
- **Status:** CameraX locks 3A parameters using `FocusMeteringAction.disableAutoCancel()`.
- **Limitation:** On rare devices with `INFO_SUPPORTED_HARDWARE_LEVEL_LEGACY`, vendor HAL implementations may occasionally auto-adjust exposure if dramatic lighting transitions occur during capture. Devices with `LIMITED`, `FULL`, or `LEVEL_3` honor locks completely.

### 2.6 Dynamic App Icon Switching (Pro Tier)
- **Status:** Pro tier icon theme selection is modeled and stubbed in settings.
- **Limitation:** On Android, changing launcher icons dynamically requires toggling `<activity-alias>` components via `PackageManager.setComponentEnabledSetting()`, which can briefly relaunch the launcher on some OEM skins (MIUI/ColorOS). On iOS, `UIApplication.shared.setAlternateIconName` requires all alternate icon assets to be declared in `CFBundleIcons` in `Info.plist`.

### 2.7 In-App Billing Stubbing
- **Status:** Both platforms implement clean abstraction layers (`BillingService` on Android, `ProSubscriptionProtocol` on iOS).
- **Current Behavior:** Toggling the Pro switch unlocks 15s/30s exposure and Pro assets immediately.
- **Future Integration:** Direct drop-in points are established for Google Play Billing KTX (`com.android.billingclient:billing-ktx`) and Apple StoreKit 2 (`StoreKit.Product`).

---

## 3. Recommended Roadmap for v1.2
1. **Core ML / TFLite Neural Scene Classifier:** Train a lightweight MobileNetV4 classifier on scene datasets (night traffic, waterfalls, crowds) to supersede the current heuristic luminance/motion analyzer.
2. **Homographic Perspective Warping via Metal Performance Shaders (MPS):** Add 3x3 homography matrix estimation to cancel perspective keystoning during heavy handheld tilt.
3. **Google Motion Photo XMP Concatenator:** Add toggle to embed companion MP4 into the JPEG binary trailer.
4. **Custom Long Exposure Durations (Freeform Slider):** Allow Pro users to set custom exposure times (e.g. 1s to 60s).

---
**End of Audit Report**
