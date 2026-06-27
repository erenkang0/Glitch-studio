# Glitch Studio

A professional, shader-based photo editing app for Android, inspired by the
creative shaders at [shaders.figma.com](https://shaders.figma.com). It runs a
real OpenGL ES 2.0 pipeline so every effect is a live GPU fragment shader with
its own parameters, plus a layer stack, masks (including freehand brush masks)
and animated GIF export.

## Features

- **80+ creative effects** across 7 categories — Glitch, Retro, Distort,
  Stylize, Color, Light and Pattern. Each effect is a GLSL shader with its own
  set of adjustable parameters.
- **Per-effect controls** — every effect exposes sliders for its parameters
  (amount, frequency, angle, radius, levels, …) plus a per-layer opacity.
- **Layer stack** — stack multiple effects, reorder them, toggle visibility and
  blend each one independently.
- **Masks** — blend an effect locally with a Radial, Linear or freehand
  **Brush** mask. The brush supports adjustable size, hardness and an
  erase mode, plus Fill / Clear / Invert.
- **Before / After** — press and hold on the canvas to compare with the
  original photo.
- **Animated shaders** — many effects animate over time (VHS, ripple, plasma,
  light leak, …). Play/pause the animation in the editor.
- **Export** — save a still as **PNG / JPEG / WEBP**, or export an animated
  **GIF** with a selectable frame count, duration and loop option. Exports go to
  `Pictures/Glitch Studio` in your gallery.
- **Professional, Figma-inspired UI** — a flat dark theme, smooth Compose
  animations and **haptic feedback** throughout. No emojis.

## Tech

- Kotlin + Jetpack Compose (Material 3)
- OpenGL ES 2.0 rendering with ping-pong framebuffers for the layer stack
- AGSL-style GLSL fragment shaders assembled from a shared header
- Self-contained GIF89a encoder (NeuQuant quantisation + LZW), no third-party
  image libraries
- `minSdk 24`, `targetSdk 34`

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
  effects/    Effect model + 80+ GLSL shader definitions, grouped by category
  gl/         OpenGL renderer, layer compositing, FBOs, mask textures
  export/     Gallery saving + GIF89a encoder (NeuQuant + LZW)
  ui/         Compose editor, view-model, theme, haptics, image loading
```
