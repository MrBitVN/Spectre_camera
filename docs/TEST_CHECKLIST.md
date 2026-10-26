# SpectreFlow: AI Long Exposure Camera — Verification & Test Checklist

**Test Suite Version:** 1.0.0  
**Target Environments:** Android (API 26+) & iOS (iOS 16.0+)  

---

## 1. Shutter Interaction & Hold-To-Expose Test Matrix

| Test ID | Interaction Case | Step-by-Step Procedure | Expected Result | Pass/Fail |
|---|---|---|---|---|
| `TEST-SHUTTER-01` | **Quick Tap (< 1s)** | Tap shutter button quickly (< 1000ms) without holding | Capture immediately halts; all temporary frames discarded; shows toast/pill *"Hold longer to capture (min 1s)"* / *"Giữ lâu hơn để chụp (tối thiểu 1 giây)"*; returns to camera preview cleanly. | [ ] |
| `TEST-SHUTTER-02` | **Hold 3s & Release** | Set max cap to 5s. Press & hold shutter button for exactly 3 seconds, then release finger | Shutter ring fills up to 60%; center counter displays `00:03`; on release, immediately stops capture, processes all 3s frames into composite, and navigates to Result screen. | [ ] |
| `TEST-SHUTTER-03` | **Hold until Auto-Finish** | Set max cap to 3s (or 5s). Press & hold shutter button continuously without lifting finger | Shutter fills to 100%; upon reaching cap, capture automatically stops and proceeds to processing as if released; does not exceed duration cap. | [ ] |
| `TEST-SHUTTER-04` | **Finger Slides Out** | Press and hold shutter button, then slide finger $> 1.4\times$ outside the button circle | Burst capture is canceled safely; all memory buffers are reset; UI returns to camera idle state without crashing. | [ ] |
| `TEST-SHUTTER-05` | **App Backgrounding During Hold** | Press and hold shutter; while holding, swipe to Home / App Switcher or receive incoming call | Lifecycle observer triggers safe cancellation; camera unlocks AF/AE/AWB; no memory leak, buffer corruption, or crash. | [ ] |
| `TEST-SHUTTER-06` | **Rapid Double Trigger Prevention** | Repeatedly press/tap the shutter in rapid succession | Initial press locks state atomically; subsequent rapid taps while holding or processing are ignored without deadlock or duplicate jobs. | [ ] |

---

## 2. Unit Test Matrix (Automated Blending Math)

| Test Identifier | Component | Verification Criteria | Expected Result |
|---|---|---|---|
| `TEST-UNIT-01` | `MeanBlender` (Android & iOS) | Ingest 3 frames with varying RGB values (e.g., $(10,20,30)$, $(50,60,70)$, $(90,100,110)$) | Output channels strictly equal the arithmetic mean: $R=50, G=60, B=70$ |
| `TEST-UNIT-02` | `LightTrailsBlender` | Ingest background frame (low intensity) followed by burst frames with bright headlight ($250$) and sparkler ($255$) | Output preserves maximum luminance peak per pixel without darkening background |
| `TEST-UNIT-03` | `MedianBlender` (Vanish Mode) | Ingest 5 frames where a transient tourist passes across a pixel in frame 3 (value $220$) over background $45$ | Median sort eliminates the outlier: output pixel channel is strictly $45$ |
| `TEST-UNIT-04` | `FrameAligner` (Translational Shift) | Ingest synthetic frame displaced by $(\Delta x = +2, \Delta y = +3)$ | Aligner cross-correlation computes $(-2, -3)$ inverse shift vector to restore alignment |
| `TEST-UNIT-05` | `FrameAligner` (Identity Shift) | Ingest identical frame | Detected shift is strictly $(0, 0)$, crop bounds remain unconstrained |

---

## 2. Field Scenarios & Photographic Quality Tests

### 2.1 Scenario A: Tripod Stability Test
- **Objective:** Verify that stable mounting activates low-power zero-alignment mode.
- **Setup:** Mount phone on a mini-tripod or rest vertically against a book/wall.
- **Procedure:**
  1. Launch SpectreFlow.
  2. Observe stability indicator badge.
  3. Select **Motion Blur** mode, **15s** or **30s** duration (Pro).
  4. Press shutter.
- **Pass Criteria:**
  - [ ] Stability badge displays green **STABLE** (or **ỔN ĐỊNH** in Vietnamese).
  - [ ] Frame alignment algorithm is bypassed (CPU usage remains < 15%).
  - [ ] Zero crop border loss ($0\%$ edge reduction).
  - [ ] Output photo shows tack-sharp static details with silky moving clouds/water.

---

### 2.2 Scenario B: Handheld Tremor Auto-Alignment Test
- **Objective:** Verify that handheld micro-movements are canceled out by registration without tearing.
- **Setup:** Hold phone by two hands, standing naturally without armrests.
- **Procedure:**
  1. Launch SpectreFlow and point at a building facade with pedestrians walking in front.
  2. Observe stability indicator badge.
  3. Select **Motion Blur**, **5s** duration.
  4. Press shutter and hold reasonably still for 5 seconds.
- **Pass Criteria:**
  - [ ] Stability badge displays amber **SHAKY** (or **RUNG LẮC**).
  - [ ] Frame registration detects and counteracts $(\Delta x, \Delta y)$ jitter on each incoming frame.
  - [ ] Outer frame boundaries are cleanly cropped to the valid intersection bounding box.
  - [ ] Static architectural edges (window frames, brickwork) are crisp without double-imaging.

---

### 2.3 Scenario C: Night Traffic & Light Trails Test
- **Objective:** Validate maximum pixel blend on dynamic illumination against dark asphalt.
- **Setup:** Highway overpass or street intersection at dusk/night with passing cars.
- **Procedure:**
  1. Select **Light Trails** mode.
  2. Set duration to **9s** or **15s**.
  3. Focus on road lane and initiate capture.
- **Pass Criteria:**
  - [ ] Sensor locks exposure and white balance at start; no brightness fluttering.
  - [ ] Vehicle headlights produce continuous white streaks; taillights produce red ribbons.
  - [ ] Dark sky and stationary asphalt remain properly exposed without blown highlights.

---

### 2.4 Scenario D: Daytime Crowds & Moving Traffic ("Vanish" Mode)
- **Objective:** Validate temporal median rejection of pedestrians and fleeting objects.
- **Setup:** Public plaza, museum entrance, or city crosswalk with steady pedestrian flow.
- **Procedure:**
  1. Select **Vanish** mode.
  2. Choose **9s** or **15s** duration.
  3. Keep the camera focused on the monument or landmark while people walk through the frame.
- **Pass Criteria:**
  - [ ] Moving tourists traverse the frame and disappear completely from the final image.
  - [ ] Stationary bench, cobblestones, and background buildings remain visible and sharp.
  - [ ] Memory footprint remains strictly bounded (no Out-Of-Memory termination).

---

### 2.5 Scenario E: Low Light & Astrophotography Test
- **Objective:** Check long accumulation noise suppression and contrast fidelity.
- **Setup:** Dark room or night sky landscape with distant stars or faint ambient light.
- **Procedure:**
  1. Select **Motion Blur** or **Light Trails** mode with **30s** duration.
  2. Trigger burst on steady surface.
- **Pass Criteria:**
  - [ ] Random sensor shot noise averages out due to $O(\sqrt{N})$ noise reduction of multi-frame averaging.
  - [ ] Star trails begin to emerge under Light Trails mode.

---

## 3. Resilience, Concurrency & System Stress Tests

### 3.1 Background & Interruption Resilience
- [ ] **Home Button / App Switcher during capture:**
  - Press Home / swipe up to app switcher at $t = 2.5\text{s}$ during a 9s capture.
  - *Result:* App safely cancels burst, releases camera hardware locks, and does not crash or leak memory.
- [ ] **Incoming Phone Call / Notification:**
  - Session pauses safely without audio/video deadlock.
- [ ] **Low Memory Warning (Simulated `didReceiveMemoryWarning`):**
  - Reservoir downsizes resolution gracefully without crash.

### 3.2 Permissions & First-Run Flow
- [ ] **First Launch:** Camera permission rationale dialog displays clear explanation.
- [ ] **Permission Denied:** Friendly guidance to open System Settings; app does not crash.
- [ ] **Permission Granted:** Preview starts smoothly within 400ms.
- [ ] **Photo Library Access:** Result photo saved to system gallery (DCIM / Photos) with valid timestamp and EXIF tags.

### 3.3 Localization & UI Polish
- [ ] **English (`en`):** All labels, dialogs, duration chips, mode names, and settings render correctly without truncation.
- [ ] **Vietnamese (`vi`):** "Làm mờ chuyển động", "Vệt sáng", "Biến mất", "ỔN ĐỊNH", "RUNG LẮC" display with proper diacritics and correct font sizing.
- [ ] **Haptic Feedback:** Crisp impact haptic on shutter tap (and toggleable in Settings).
- [ ] **3x3 Grid Overlay:** Rule of thirds grid overlays cleanly on camera preview without interfering with touch targets.
- [ ] **Pro Duration Gating:** 15s and 30s chips indicate "PRO"; toggleable in Settings.

---

## 4. Advanced Features Test Matrix (v1.1)

### 4.1 Auto Scene Detection
- [ ] **Dark Traffic Test:** Point at night street with moving headlights; verify mode chip displays `AUTO (Trails)` and applies Light Trails blend.
- [ ] **Daylight Crowd Test:** Point at sunny plaza with walking tourists; verify mode chip displays `AUTO (Vanish)` and applies temporal median blend.
- [ ] **Water / Cloud Test:** Point at waterfall, river, or clouds; verify mode chip displays `AUTO (Blur)`.
- [ ] **Manual Override:** Tap any specific mode chip (Motion Blur, Light Trails, Vanish); verify auto-detection is overridden and the selected mode remains locked.

### 4.2 Companion Video & Frame Scrubber
- [ ] **Companion Video Recording:** Capture a 5-second long exposure with "Save Companion Video" ON. Verify an MP4 (Android) or Live Photo (iOS) is created in gallery.
- [ ] **Toggle Off:** Turn toggle OFF in Settings. Capture long exposure; verify no auxiliary video is created.
- [ ] **Frame Scrubbing:** In Result screen, scrub across filmstrip thumbnails; verify preview updates smoothly to the scrubbed burst frame.
- [ ] **Pick as Result:** Select Frame 4, tap "Pick as Result"; verify Frame 4 becomes the active photo and saves/shares properly.
- [ ] **Share Video Button:** Tap "Share Video"; verify system share sheet presents the MP4/MOV file.

### 4.3 Lens Switching & Camera Flip
- [ ] **Hardware Lens Detection:** Only lenses supported by the phone are displayed (e.g. 0.5x, 1x, 2x/3x).
- [ ] **Lens Tap:** Tapping 0.5x / 1x / 2x smoothly switches zoom level without preview stutter.
- [ ] **Camera Flip:** Tap flip button; camera switches to front selfie camera; flip again to return to back camera.

### 4.4 Onboarding Tutorial Flow
- [ ] **First Launch:** On clean install, 3-screen interactive tutorial displays automatically.
- [ ] **Dot Progression:** Swiping or tapping "Next" advances dots and pages.
- [ ] **Completion Persistence:** Tap "Get Started"; user lands on Camera preview; subsequent launches skip onboarding.
- [ ] **Re-launch from Settings:** Tap "View Tutorial" in Settings; onboarding opens cleanly and returns on completion.

### 4.5 Color & Display P3
- [ ] **Wide Gamut Verification:** Verify that photos captured on P3-capable screens preserve rich emeralds and saturated tones.

### 4.6 Accessibility (TalkBack & VoiceOver)
- [ ] **Screen Reader Audit:** Enable TalkBack (Android) or VoiceOver (iOS):
  - Shutter button announces state ("Shutter button, press and hold to expose, release to capture" / "Exposing, hold to continue, release to capture").
  - Mode selector chips announce selection state and mode names.
  - Duration chips announce seconds and Pro requirement if locked.
  - Lens buttons announce zoom level and flip action.

### 4.7 Shortcuts App Intent (iOS)
- [ ] **Siri Shortcuts Integration:** Add "Take Long Exposure" shortcut in Shortcuts app; run shortcut; verify SpectreFlow launches directly into camera capture state.

---
**End of Test Checklist**
