# DARKSOULSED® AI CAMERA

**Professional AI Photography & 4K Video Camera for Android**  
*Made with love by Darksouled*

---

## 📸 Overview

**DARKSOULSED® AI CAMERA** is a flagship-grade photography and 4K cinema video camera available both as a **Native Android Application** (Kotlin, Jetpack Compose, CameraX) and as an **Instant Web Application** deployable to **Vercel** with a single click from GitHub (React 18, Vite, WebRTC, HTML5 Canvas AI pipeline).

---

## ⚡ Deploy Directly to Vercel (1-Click from GitHub)

You can deploy the entire camera application to **Vercel** directly from your phone in under 60 seconds:

1. Push or export this repository to your **GitHub** account.
2. Open **[vercel.com](https://vercel.com)** in your mobile browser and log in with GitHub.
3. Tap **"Add New..."** → **"Project"**.
4. Select your `darksouled-ai-camera` repository from the list.
5. Vercel automatically detects the pre-configured `vercel.json` and Vite configuration (`npm run build` → `dist`).
6. Tap **"Deploy"**.
7. In ~30 seconds, your camera app is live at `https://your-project.vercel.app`!
8. **Install on Phone**: Open the link in Chrome on Android or Safari on iOS, tap the menu (or share icon), and tap **"Add to Home Screen"** to use it as a full-screen standalone camera app.

---

## ✨ Key Features

### 1. 🧠 Computational AI Photography Engine
- **Bilateral Spatial Denoising**: Smooths low-light grain and sensor noise without blurring high-frequency edges or skin pores.
- **Laplacian Micro-Contrast Recovery**: Reconstructs fine optical edge details, eyelashes, and fabric textures.
- **Pro Max Color Science & HDR Tone Mapping**: Lifts deep shadows with an organic S-curve while preserving highlights from blowout.
- **Melanin-Safe Skin Tone Preservation**: Detects and protects warm melanin/carotenoid skin tones without artificial redness or plastic smoothing.
- **Interactive Before / After Split Slider**: Real-time interactive comparison viewport with draggable divider.
- **Granular Intensity Tuning**: Adjust enhancement strength (20% to 100%) with live metrics (detail boost, noise reduction, dynamic range in EV).

### 2. 🎥 4K Cinema Video System
- Native high-bitrate video recording with hardware encoder profile negotiation.
- Supported capture modes: 4K UHD, 1080p FHD, 720p HD at up to 60 FPS.
- Live recording indicator with elapsed duration timer and audio capture.
- **AI 4K Video Lab**: Temporal denoising, AI spatial upscaling, and digital frame stabilization with honest distinction between native hardware 4K capture and AI-upscaled exports.

### 3. 🎛️ Camera Controls & Photography Modes
- **Modes**: `PHOTO`, `VIDEO` (4K UHD), `PORTRAIT` (bokeh), `NIGHT` (low-light multi-frame), `HDR` (high dynamic range), and `PRO` (manual exposure compensation index, ISO, and white balance).
- **Controls**:
  - Flash cycling: `Off`, `On`, `Auto`, `Torch`
  - Quick Zoom Selector: `1x`, `2x`, `5x`
  - Aspect Ratio: `4:3`, `16:9`, `1:1`
  - Countdown Timer: `Off`, `3s`, `10s`
  - Rule-of-thirds composition grid
  - Tap-to-focus with pulsing gold reticle

### 4. 📂 Scoped Storage & Built-In Gallery
- Fully compliant with modern Android Scoped Storage (MediaStore API).
- Raw and enhanced photographs saved to `Pictures/DARKSOULSED`.
- 4K videos saved to `Movies/DARKSOULSED`.
- Built-in gallery with category filtering (`All Media`, `AI Enhanced`, `Photos`, `4K Videos`), full-screen media viewer, share intents, and deletion management.

### 5. 💎 AMOLED Dark Design
- Pure `#080808` obsidian dark theme with silver accents and gold indicators.
- Custom adaptive launcher icon with metallic lens emblem.
- Edge-to-edge support with safe area insets handling.

---

## 🛠️ Tech Stack & Architecture

- **Language**: Kotlin 2.2
- **UI Toolkit**: Jetpack Compose (Material Design 3)
- **Camera Core**: AndroidX CameraX (1.5.0) & Camera2 Interop
- **Concurrency**: Kotlin Coroutines & Flow
- **Image Loading**: Coil Compose (2.7.0)
- **Storage**: Android MediaStore (Android 10+ Scoped Storage & backwards compatibility)
- **Architecture**: Single-Activity, Model-View-ViewModel (MVVM) with StateFlow

---

## 🚀 Building & Running

### Prerequisites
- Android Studio Iguana / Ladybug or newer
- JDK 17
- Android SDK 34 (API Level 34 minimum compileSdk 36)
- Physical Android device or emulator with Camera2 support

### Build Commands
```bash
# Clone the repository
git clone https://github.com/YOUR_USERNAME/darksouled-ai-camera.git
cd darksouled-ai-camera

# Build Debug APK
./gradlew assembleDebug

# Run Unit Tests
./gradlew :app:testDebugUnitTest

# Build Release APK
./gradlew assembleRelease
```
Compiled APKs will be located in: `app/build/outputs/apk/`

---

## 📱 Mobile-First GitHub Releases (CI/CD)

The repository includes a ready-to-run GitHub Actions workflow (`.github/workflows/release.yml`) so you can build and download your APK directly from your phone without needing a computer:

1. Push or export the project to your GitHub repository.
2. In your mobile browser or GitHub app, navigate to **Actions** > **"Build & Release DARKSOULSED® AI Camera"**.
3. Tap **"Run workflow"** and specify your version tag (e.g. `v1.0.0`).
4. GitHub Actions will build and attach the following ready-to-download assets to your release:
   - `DARKSOULSED-AI-Camera-v1.0.0.apk` (Direct install on Android)
   - `DARKSOULSED-AI-Camera-v1.0.0.app` / `.aab` (Android App Bundle)
   - `DARKSOULSED-AI-Camera-v1.0.0.zip` (Zip bundle)
   - `DARKSOULSED-AI-Camera-v1.0.0.tar.gz` (Tarball archive)

---

## 📄 License & Credits

**Brand**: DARKSOULSED®  
**Developer**: Made with love by Darksouled  
Licensed under the Apache License, Version 2.0.
