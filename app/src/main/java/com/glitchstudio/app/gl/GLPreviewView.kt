package com.glitchstudio.app.gl

import android.content.Context
import android.graphics.Bitmap
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.GLUtils
import com.glitchstudio.app.effects.ShaderEffect
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/**
 * Live, GPU-accelerated preview of the current image with the selected effect.
 * State is mutated from the UI thread through the public setters, which marshal
 * onto the GL thread via [queueEvent] and request a redraw.
 */
class GLPreviewView(context: Context) : GLSurfaceView(context) {

    private val renderer = PreviewRenderer()

    init {
        setEGLContextClientVersion(2)
        preserveEGLContextOnPause = true
        setRenderer(renderer)
        renderMode = RENDERMODE_WHEN_DIRTY
    }

    fun setImage(bitmap: Bitmap) {
        queueEvent {
            renderer.pendingBitmap = bitmap
            requestRender()
        }
    }

    fun setEffect(effect: ShaderEffect?) {
        queueEvent {
            renderer.effect = effect
            requestRender()
        }
        renderMode = if (effect?.animated == true) RENDERMODE_CONTINUOUSLY else RENDERMODE_WHEN_DIRTY
    }

    fun setValues(values: FloatArray) {
        queueEvent {
            renderer.values = values
            requestRender()
        }
    }

    /** Show the original, unprocessed image (used by a press-and-hold compare). */
    fun setBypass(bypass: Boolean) {
        queueEvent {
            renderer.bypass = bypass
            requestRender()
        }
    }

    private class PreviewRenderer : Renderer {
        @Volatile var pendingBitmap: Bitmap? = null
        @Volatile var effect: ShaderEffect? = null
        @Volatile var values: FloatArray = FloatArray(0)
        @Volatile var bypass: Boolean = false

        private var textureId = 0
        private var viewportW = 1
        private var viewportH = 1
        private val programCache = HashMap<String, ShaderProgram>()
        private var passthrough: ShaderProgram? = null
        private val startNanos = System.nanoTime()

        override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
            GLES20.glClearColor(0.05f, 0.05f, 0.05f, 1f)
            // The EGL context was (re)created: every GL object is gone.
            programCache.clear()
            passthrough = null
            textureId = GlUtil.createTexture()
            // Force re-upload of whatever image is current.
            pendingBitmap?.let { uploadTexture(it) }
        }

        override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
            viewportW = width.coerceAtLeast(1)
            viewportH = height.coerceAtLeast(1)
            GLES20.glViewport(0, 0, viewportW, viewportH)
        }

        override fun onDrawFrame(gl: GL10?) {
            pendingBitmap?.let {
                uploadTexture(it)
                pendingBitmap = null
            }
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
            if (textureId == 0) return

            val time = (System.nanoTime() - startNanos) / 1_000_000_000f
            val current = effect
            val program = if (bypass || current == null) passthrough() else programFor(current)
            val vals = if (bypass || current == null) FloatArray(0) else values
            program.draw(textureId, vals, viewportW, viewportH, time)
        }

        private fun programFor(effect: ShaderEffect): ShaderProgram =
            programCache.getOrPut(effect.id) { ShaderProgram.build(effect) }

        private fun passthrough(): ShaderProgram =
            passthrough ?: ShaderProgram.build(PASSTHROUGH).also { passthrough = it }

        private fun uploadTexture(bitmap: Bitmap) {
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)
        }
    }

    companion object {
        // Minimal effect that just samples the texture; used for bypass/original.
        val PASSTHROUGH = ShaderEffect(
            id = "__passthrough",
            name = "Original",
            category = com.glitchstudio.app.effects.EffectCategory.COLOR,
            description = "",
            params = emptyList(),
            fragmentBody = "void main(){ gl_FragColor = texture2D(u_Texture, v_TexCoord); }",
        )
    }
}
