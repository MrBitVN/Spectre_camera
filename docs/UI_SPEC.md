# SpectreFlow - Native Mobile UI Specification & Flow Architecture

**Author:** Senior Mobile Systems Architect  
**Platform:** Android (Kotlin, Jetpack Compose, CameraX) & iOS (Swift, SwiftUI, AVFoundation)  
**Document:** `docs/UI_SPEC.md`  
**Status:** APPROVED & SPECIFIED  

---

## 1. Design System & Visual Foundation

- **Color Palette:**
  - Background Canvas: Obsidian `#0A0C0E` / True Dark `#000000`
  - Elevated Cards / Surfaces: Dark Slate `#16191D` / `#1E2228`
  - Primary Accent: Neon Emerald `#1ED98B` (also supported by Mint Glow `#00E676`)
  - Warning / Shaky: Warm Amber `#FFB300` / `#FF9800`
  - Text Primary: `#FFFFFF` (100% white)
  - Text Secondary: `#90A4AE` (Muted silver)
  - Locked / Disabled Accent: `#546E7A` with lock badge
- **Typography:**
  - Monospace Numerals for exposure timers (`00:07`), duration dial numbers, and zoom indicators.
  - Medium/Bold San Francisco (iOS) and Roboto (Android) for UI titles and buttons.
- **Language & Localization:**
  - 100% English UI across all components, strings, dialogs, and accessibility labels.
  - Zero hardcoded UI strings; referenced strictly from `strings.xml` (Android) and `Localizable.strings` (iOS).

---

## 2. Screen Hierarchy & Navigation Map

```
                  ┌──────────────────────────────────────────────┐
                  │              CAMERA SCREEN                   │
                  │   - Top Bar: Mode ("AUTO 💡") + Sun + Dot   │
                  │   - Preview (~65% H, rounded corners, pill)  │
                  │   - Control Row: Flip | [📱] Stability | Gear │
                  │   - Bottom: Thumbnail | Dual-Ring | Dial     │
                  └──────┬───────────────────┬────────────┬──────┘
                         │                   │            │
            Press & Hold │       Tap Setting │            │ Tap Thumb
            (Release)    ▼                   ▼            ▼
               ┌───────────────────┐    ┌─────────────┐ ┌──────────────┐
               │ PROCESSING OVERLAY│    │  SETTINGS   │ │RESULT VIEWER │
               │ "Processing..."   │    │  SCREEN     │ │(Last photo / │
               └─────────┬─────────┘    └──────┬──────┘ │ companion)   │
                         │                     │        └──────────────┘
                         ▼                     │
               ┌───────────────────┐           │ Open
               │   RESULT SCREEN   │           ▼
               │ Save/Share/Discard│    ┌──────────────┐
               │ Filmstrip scrub   │    │  TUTORIALS   │
               └───────────────────┘    │  SCREEN (3)  │
                                        └──────┬───────┘
                                               │ Tap card
                                               ▼
                                        ┌──────────────┐
                                        │TUTORIAL DEEP │
                                        │STEP DETAIL   │
                                        └──────────────┘
```

---

## 3. Screen Specifications

### 3.1 Main Camera Screen (Portrait, Dark Minimalist)

#### A. Top Bar
- **Left - Mode Picker Pill:**
  - Displays `"AUTO 💡"` (or `"MOTION BLUR 💡"`, `"LIGHT TRAILS 💡"`, `"VANISH 💡"`). In Auto mode, when a scene is detected, it dynamically reflects the detected preset (e.g. `AUTO (BLUR) 💡`).
  - Tap interaction: Opens an elegant floating modal / menu displaying the 4 modes with descriptions:
    1. **Auto:** AI heuristic analyzer selects optimal mode based on brightness and motion.
    2. **Motion Blur:** Per-pixel mean blending for silky water, cloud drifts, and ghosted pedestrians.
    3. **Light Trails:** Luminance max/lighten blending for vehicle headlights, star trails, and fireworks.
    4. **Vanish:** Temporal median blending to erase walking people and moving traffic.
- **Center-Right - Active Camera Dot:**
  - A subtle 6dp pulsing neon-emerald dot indicating sensor streaming and hardware camera lock.
- **Right - Sun / Brightness Exposure Slider:**
  - A minimalist sun icon. Tapping reveals a sleek horizontal exposure compensation slider (`-2.0 EV` to `+2.0 EV`).
  - Auto-hides after 3 seconds of inactivity to keep the preview uncluttered.

#### B. Preview Area (~65% Screen Height)
- Positioned centrally with prominent rounded top corners (`24dp` radius) and rounded bottom corners.
- Displays full-fidelity live camera viewfinder.
- **Optional Rule-of-Thirds Grid:** High-precision subtle grid lines toggled via Settings.
- **Floating Lens Zoom Pill:**
  - Centered at the bottom edge of the preview viewport (`16dp` above bottom edge).
  - Semi-transparent circular pill (`#26000000` with blur) displaying `"1x"` (or `"0.5x"`, `"2x"`, `"3x"`).
  - Tapping cycles through hardware-supported optical/digital lenses smoothly.

#### C. Control Row (Directly Below Preview Area)
- **Left - Flip Camera Button:**
  - Circular dark button with dual-arrow rotating icon. Flips between rear multi-lens array and front TrueDepth/selfie camera.
- **Center - Stability Indicator `[ 📱 ]`:**
  - Iconography: A vertical smartphone glyph enclosed inside 4 precision corner brackets.
  - **STABLE State (Tripod detected via 6-axis gyro & accelerometer):**
    - Vibrant Neon Emerald `#1ED98B`.
    - Text badge: `STABLE`.
    - System bypasses frame alignment calculations to optimize battery and memory bandwidth.
  - **SHAKY State (Handheld micro-tremors detected):**
    - Amber `#FFB300`.
    - Text badge: `SHAKY`.
    - Active AI homography / feature alignment executes on every incoming frame.
- **Right - Settings Gear Button:**
  - Minimalist gear icon navigating to `SettingsScreen`.

#### D. Bottom Row (Ergonomic Thumb-Reachable Capture Strip)
- **Left - Recent Photo Thumbnail:**
  - Squircle/rounded-square thumbnail (`54x54dp`) displaying the last saved long-exposure composite.
  - Tapping opens the full-screen Result Viewer.
- **Center - Dual-Ring Shutter Button:**
  - Outer Ring: Emerald stroke (`76dp` diameter).
  - Inner Ring: Concentric emerald filled circle / ring (`58dp` diameter).
  - **Interaction Model:**
    - **Press and Hold:** Instantly locks 3A sensors (Exposure, ISO, Focus, White Balance), triggers a crisp haptic tap, and begins streaming burst frames at ~30 FPS into a low-memory float accumulator.
    - **During Hold:** Inner ring pulses softly; an outer arc smoothly traces clockwise proportional to elapsed time vs. maximum cap; a glowing digital readout displays elapsed seconds (`00:07`).
    - **Release:** Triggers a second haptic pulse, stops burst streaming, triggers AI alignment & synthesis, and transitions immediately to the Processing Screen.
    - **Minimum Hold Rule (< 1.0s):** If released before 1 second, the capture aborts with no crash, buffers are freed, and a floating toast displays: `"Hold longer to capture"`.
    - **Safety Boundaries:** Sliding the finger far off the shutter button, taking an incoming call, or app backgrounding safely terminates the burst without memory leaks.
- **Right - Rotary Duration Dial (Quarter-Circle Arc):**
  - Quarter-circle radial arc anchored in the bottom-right corner with radial tick marks.
  - Exposure Caps: `3s`, `5s`, `9s`, `15s`, `30s`.
  - Selected value is rendered in large Emerald numerals (`22sp`, bold).
  - Pro values (`15s`, `30s`) display a padlock icon and muted silver tone if Pro is locked.
  - Swipe along the arc or tap any numeral to switch caps, accompanied by tactile haptic tick feedback.

---

### 3.2 Capture & Processing State Machine

```
      ┌────────────┐
      │    IDLE    │◄──────────────────────────────────┐
      └─────┬──────┘                                   │
            │ Press & Hold (Shutter)                   │
            ▼                                          │
      ┌────────────┐ Release < 1s                     │
      │ CAPTURING  ├───────────────────────────────────┤
      └─────┬──────┘ (Show "Hold longer to capture")   │
            │ Release >= 1s OR Reached Duration Cap    │
            ▼                                          │
      ┌────────────┐                                   │
      │ PROCESSING │ (Progress ring + "Processing...") │
      └─────┬──────┘                                   │
            │ Blend & Alignment complete               │
            ▼                                          │
      ┌────────────┐ Discard                           │
      │   RESULT   ├───────────────────────────────────┘
      └────────────┘ (Save / Share / Filmstrip Scrub)
```

---

### 3.3 Result Screen
- Immersive edge-to-edge view of the synthesized long-exposure photograph.
- Top Action Bar: Discard (Trash icon / Close) and Mode/Duration metadata badge.
- Bottom Filmstrip Scrubber:
  - When companion video is captured, users can scrub frame-by-frame through keyframes.
  - "Pick as Result" button sets the scrubbed frame as the new composite cover.
- Bottom Action Bar:
  - **Save:** Writes 100% full-resolution JPEG/Display-P3 image to gallery + Motion Photo / Live Photo.
  - **Share:** Opens native system share sheet (Photo + Companion MP4/MOV).
  - **Discard:** Frees buffers and returns to camera.

---

### 3.4 Tutorials Screen (Guide Collection)
- **Header:** Circular Back Button + Centered Title `"Tutorials"`.
- **Introductory Subtitle:** `"These guides show you the power of the AI-augmented camera:"`
- **3 Rounded Dark Cards (`#1E2228`, 16dp corner radius):**
  1. **Card 1 (Vanish):**
     - Thumbnail: Abstract crowd blur gradient with vanishing figure icon.
     - Title: `"Take a shot in a crowded place and make people disappear"` (key words in bold).
     - Target Mode: Vanish (`temporal median`).
  2. **Card 2 (Light Trails):**
     - Thumbnail: Abstract night highway light streak illustration.
     - Title: `"Turn a highway into a dazzling river of light"` (key words in bold).
     - Target Mode: Light Trails (`luminance max`).
  3. **Card 3 (Motion Blur):**
     - Thumbnail: Silky cascading waterfall / misty rapids illustration.
     - Title: `"Get a beautiful soft water exposure of a waterfall"` (key words in bold).
     - Target Mode: Motion Blur (`per-pixel mean`).
- **Detail Step View:**
  - Tapping any card opens a dedicated 4-step tutorial sheet: Recommended Duration, Tripod recommendation, Sensor lock instructions, and Press-and-Hold tips.

---

### 3.5 Settings Screen
- **Tutorials:** Navigation row to open the Tutorials screen.
- **Rule of Thirds Grid:** Switch toggle (`true`/`false`).
- **Haptic Feedback:** Switch toggle for tactile shutter & dial feedback.
- **Save Companion Video:** Switch toggle (saves paired MP4/MOV for scrubbing).
- **Photo Quality:** Segmented chip selector (`High / Full Sensor` vs `Balanced / 1080p`).
- **Unlock Pro:** Stub toggle unlocking 15s and 30s exposures.
- **About:** Displays SpectreFlow version, computational photography engine notes, and privacy statement.

---

## 4. Verification & Testing Matrix

| Test Case | Interaction / Trigger | Expected Behavior |
|---|---|---|
| **Quick Tap** | Tap shutter < 1s | No capture, toast: `"Hold longer to capture"`, 0 leaked buffers |
| **Exposure 3s/5s/9s** | Hold until cap | Arc fills, auto-finishes at cap, blends correctly |
| **Early Release** | Hold 2.5s on 9s cap | Stops immediately, blends 2.5s accumulated frames |
| **Touch Slide Off** | Slide finger 80dp away | Safely cancels capture, frees accumulator |
| **Dial Switch** | Swipe bottom-right arc | Changes max cap with haptic tick, locks 15/30 if not Pro |
| **Lens Cycle** | Tap 1x floating pill | Cycles 0.5x -> 1x -> 2x -> 3x with zoom preview update |
| **Tutorial Open** | Tap Settings -> Tutorials | Opens 3 guide cards; tapping card opens detailed steps |
