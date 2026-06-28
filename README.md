# Glitch Studio

A professional, shader-based photo editing app for Android with a
**Lightroom-style layout**, built for **Android 16**. It runs a real OpenGL
ES 2.0 pipeline so every effect is a live GPU fragment shader with its own
parameters, on top of a layer stack, masks (including freehand brush), zoom/pan,
live effect previews and animated GIF export.

## Features

- **170 creative effects** across 10 categories — Glitch, Retro, Distort,
  Stylize, Color, Light, Pattern, Art, Texture and Sci-Fi. Each effect is a GLSL
  shader with its own adjustable parameters.
- **Live effect previews** — every effect tile renders a thumbnail on your
  actual photo, plus a **None** tile to clear the effect in one tap.
- **Lightroom-style UI** — a slim top bar, a big full-bleed canvas, and a thin
  bottom category rail that slides tool panels up over the photo.
- **Zoom & pan** — pinch to zoom (up to 8x) and drag to pan; double-tap resets.
- **Per-effect controls** — sliders for each effect's parameters plus per-layer
  opacity.
- **Layer stack** — stack/reorder/toggle effects and blend each independently.
- **Masks** — Radial, Linear or freehand **Brush** mask (size, hardness, erase,
  Fill / Clear / Invert).
- **Before / After** — hold on the canvas to compare with the original.
- **Animated shaders** with play/pause.
- **Export** — PNG / JPEG / WEBP, or a looping **GIF** with selectable frame
  count and duration, saved to `Pictures/Glitch Studio`.
- Dark, expressive UI with smooth spring animations and **haptic feedback**.
  No emojis.

## Tech

- Kotlin + Jetpack Compose (Material 3, expressive shapes & motion)
- OpenGL ES 2.0 rendering with ping-pong framebuffers for the layer stack; the
  chain preserves orientation and the screen blit applies zoom/pan, so preview
  and export are always upright
- GLSL fragment shaders assembled from a shared header
- Self-contained GIF89a encoder (NeuQuant quantisation + LZW), no third-party
  image libraries
- `minSdk 24`, `compileSdk 36`, `targetSdk 36` (Android 16)

> Note: the literal Material 3 Expressive library (`material3 1.5.0-alpha` →
> Compose 1.12) requires Android API 37, whose SDK platform is not yet
> downloadable in CI, so the UI uses stable Material 3 styled in the expressive
> language (large rounded shapes, vibrant accents, springy motion).

## Building

The project builds with Gradle and the Android Gradle Plugin:

```bash
./gradlew assembleRelease   # app/build/outputs/apk/release/app-release.apk
./gradlew assembleDebug     # app/build/outputs/apk/debug/app-debug.apk
```

The release build is signed with the debug keystore so the APK is directly
installable without extra signing configuration.

## Downloading the APK

Every push to the development branch triggers the **Android Build** GitHub
Actions workflow (`.github/workflows/android.yml`), which builds the APKs and
publishes them as:

- a **GitHub Release** (`build-<run number>`) with `app-release.apk` attached, and
- a **workflow artifact** (`glitch-studio-apks`).

To install on a device: download `app-release.apk`, allow "Install unknown
apps" for your browser/file manager, then open the file.

## Project layout

```
app/src/main/java/com/glitchstudio/app/
  effects/    Effect model + 170 GLSL shader definitions, grouped by category
  gl/         OpenGL renderer, layer compositing, FBOs, mask textures
  export/     Gallery saving + GIF89a encoder (NeuQuant + LZW)
  ui/         Compose editor, view-model, theme, haptics, image loading
```
