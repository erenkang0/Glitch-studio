package com.glitchstudio.app.gl

import android.content.Context
import android.graphics.Bitmap
import android.opengl.GLSurfaceView
import com.glitchstudio.app.effects.Effect

/**
 * A [GLSurfaceView] that hosts the [PhotoRenderer] and exposes a small,
 * thread-safe API for the Compose layer.
 */
class GlPhotoView(context: Context) : GLSurfaceView(context) {

    val renderer = PhotoRenderer()

    init {
        setEGLContextClientVersion(2)
        setEGLConfigChooser(8, 8, 8, 8, 16, 0)
        preserveEGLContextOnPause = true
        setRenderer(renderer)
        renderMode = RENDERMODE_WHEN_DIRTY
    }

    fun setImage(bitmap: Bitmap) {
        renderer.setImage(bitmap)
        requestRender()
    }

    fun setLayers(layers: List<RenderLayer>) {
        renderer.layers = layers
        requestRender()
    }

    fun setContinuous(continuous: Boolean) {
        renderer.animating = continuous
        renderMode = if (continuous) RENDERMODE_CONTINUOUSLY else RENDERMODE_WHEN_DIRTY
        if (!continuous) requestRender()
    }

    fun setBypass(bypass: Boolean) {
        renderer.bypass = bypass
        requestRender()
    }

    fun setViewTransform(scale: Float, panX: Float, panY: Float) {
        renderer.viewScale = scale
        renderer.viewPanX = panX
        renderer.viewPanY = panY
        requestRender()
    }

    /** Renders a small preview of each effect (default params) on the GL thread. */
    fun renderThumbnails(effects: List<Effect>, cap: Int, onEach: (String, Bitmap?) -> Unit) {
        queueEvent {
            for (effect in effects) {
                val bmp = renderer.renderEffectThumbnail(effect, cap)
                onEach(effect.id, bmp)
            }
            requestRender()
        }
    }

    /** Capture a single still of the current effect at full output resolution. */
    fun captureStill(cap: Int, onResult: (Bitmap?) -> Unit) {
        queueEvent {
            val size = renderer.outputSizeFor(cap)
            val bmp = renderer.renderToBitmap(size[0], size[1], renderer.lastFrameTime)
            onResult(bmp)
            requestRender()
        }
    }

    /** Capture a sequence of frames across one animation loop, for GIF export. */
    fun captureFrames(cap: Int, frameCount: Int, durationSec: Float, onResult: (List<Bitmap>) -> Unit) {
        queueEvent {
            val size = renderer.outputSizeFor(cap)
            val frames = ArrayList<Bitmap>(frameCount)
            for (f in 0 until frameCount) {
                val t = if (frameCount > 0) f.toFloat() / frameCount * durationSec else 0f
                renderer.renderToBitmap(size[0], size[1], t)?.let { frames.add(it) }
            }
            onResult(frames)
            requestRender()
        }
    }
}
