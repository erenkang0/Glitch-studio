# Glitch Studio

A GPU-powered photo editor for Android, inspired by the live shader playground at
[shaders.figma.com](https://shaders.figma.com/) but built around a focused,
**Adobe Lightroom–style** editing experience. Open a photo, sweep through **83
creative effects**, fine-tune every one with its own controls, and export to
**PNG, JPEG, WebP, or animated GIF**.

> No accounts, no emoji, no clutter — just the image and the controls.

## Highlights

- **83 creative effects** across 8 categories: Color, Light, Stylize, Distort,
  Glitch, Blur, Texture, and Retro.
- **Every effect is adjustable.** Each one exposes its own parameters as
  Lightroom-style sliders — double-tap a slider to reset it.
- **Advanced color & direction controls.** Hue parameters render a live colour
  spectrum; angle parameters read out in degrees.
- **On-image finger control.** Drag directly on the photo to steer an effect:
  a draggable handle for position-based effects (Lens Flare, Spotlight), a
  rotation handle for direction-based effects, and horizontal/vertical drag to
  scrub the main parameters of everything else.
- **Real-time GPU preview** via OpenGL ES 2.0 — animated effects (VHS, glitch,
  ripples, etc.) play live and loop in exported GIFs.
- **Press-and-hold to compare** with the untouched original.
- **Full-resolution export** rendered off-screen, saved straight to your gallery
  (`Pictures/GlitchStudio`) or shared to any app.

## Effect categories

| Category | Examples |
| --- | --- |
| Color | Exposure, Contrast, Vibrance, Hue Rotate, Duotone, Split Tone, Curves |
| Light | Vignette, Bloom, Soft Glow, Light Leak, Lens Flare, Spotlight |
| Stylize | Edge Detect, Halftone, Oil Paint, Cartoon, Crosshatch, Dither, Thermal |
| Distort | Wave, Ripple, Swirl, Bulge, Fisheye, Kaleidoscope, Shockwave |
| Glitch | RGB Split, Chromatic Aberration, VHS, Data Mosh, Slice Shift, Scanlines |
| Blur | Gaussian, Box, Zoom, Spin, Motion, Tilt Shift |
| Texture | Film Grain, Color Noise, Old Photo, Scratches, Paper |
| Retro | CRT, Game Boy, 8-Bit, Polaroid, Technicolor, VHS Tracking |

## Getting the APK

Every push to the development branch builds a signed release APK in CI:

1. Open the repository's **Actions** tab → the latest **Android Release APK** run
   → download the `glitch-studio-release` artifact, **or**
2. Grab it from the rolling **`android-latest`** prerelease under **Releases**.

The APK is signed with a debug key for sideloading. Enable *Install unknown apps*
for your browser/file manager and open the APK to install.

## Building locally

Requires JDK 17 and the Android SDK (compileSdk 34).

```bash
./gradlew :app:assembleRelease
# output: app/build/outputs/apk/release/app-release.apk
```

To sign with your own key, provide these as environment variables or
`-P` properties at build time (otherwise the debug key is used):

```
RELEASE_STORE_FILE, RELEASE_STORE_PASSWORD, RELEASE_KEY_ALIAS, RELEASE_KEY_PASSWORD
```

## Architecture

```
app/src/main/java/com/glitchstudio/app/
├── effects/      ShaderEffect model + the 83 GLSL effects (per-category files)
├── gl/           OpenGL ES 2.0 engine: ShaderProgram, live preview, offscreen renderer
├── export/       PNG/JPEG/WebP via MediaStore + pure-Kotlin animated GIF encoder
├── ui/           Compose UI (Lightroom-style), EditorViewModel, theme, components
└── util/         Bitmap loading/scaling helpers
```

- **Rendering.** Each effect is a GLSL fragment shader with auto-wired `float`
  uniforms (`u_p0`, `u_p1`, …) for its parameters, plus shared `u_Texture`,
  `u_Resolution`, and `u_Time`. The live preview uses a `GLSurfaceView`; export
  uses a headless EGL pbuffer + FBO so full-resolution and GIF frames render off
  the UI thread.
- **GIF.** A self-contained Kotlin port of the classic NeuQuant + LZW
  `AnimatedGifEncoder`, so animated GIF export needs no third-party dependency.
- **Minimum Android.** API 26 (Android 8.0) and up.

## Tech

Kotlin · Jetpack Compose (Material 3) · OpenGL ES 2.0 · AGP 8.5 · Gradle 8.9
