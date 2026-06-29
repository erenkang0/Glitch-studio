package com.glitchstudio.app.gl

import android.graphics.Bitmap
import android.graphics.Matrix
import android.opengl.EGL14
import android.opengl.EGLConfig
import android.opengl.EGLContext
import android.opengl.EGLDisplay
import android.opengl.EGLSurface
import android.opengl.GLES20
import android.opengl.GLUtils
import com.glitchstudio.app.effects.ShaderEffect
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.max
import kotlin.math.min

/**
 * Headless renderer used for export. Creates its own EGL pbuffer context,
 * renders the effect into an off-screen framebuffer at full (clamped) resolution
 * and reads it back into a [Bitmap]. Reusable across frames for GIF export.
 *
 * Must be constructed and used on a single background thread, then [release]d.
 */
class OffscreenRenderer(source: Bitmap, maxDimension: Int = Int.MAX_VALUE) {

    val width: Int
    val height: Int

    private val eglDisplay: EGLDisplay
    private val eglContext: EGLContext
    private val eglSurface: EGLSurface

    private var sourceTex = 0
    private var fbo = 0
    private var fboTex = 0
    private val programCache = HashMap<String, ShaderProgram>()
    private val readBuffer: ByteBuffer

    init {
        eglDisplay = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
        val version = IntArray(2)
        EGL14.eglInitialize(eglDisplay, version, 0, version, 1)

        val configAttribs = intArrayOf(
            EGL14.EGL_RED_SIZE, 8,
            EGL14.EGL_GREEN_SIZE, 8,
            EGL14.EGL_BLUE_SIZE, 8,
            EGL14.EGL_ALPHA_SIZE, 8,
            EGL14.EGL_RENDERABLE_TYPE, EGL14.EGL_OPENGL_ES2_BIT,
            EGL14.EGL_SURFACE_TYPE, EGL14.EGL_PBUFFER_BIT,
            EGL14.EGL_NONE,
        )
        val configs = arrayOfNulls<EGLConfig>(1)
        val numConfig = IntArray(1)
        EGL14.eglChooseConfig(eglDisplay, configAttribs, 0, configs, 0, 1, numConfig, 0)
        val config = configs[0] ?: throw RuntimeException("No EGL config for offscreen render")

        val contextAttribs = intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 2, EGL14.EGL_NONE)
        eglContext = EGL14.eglCreateContext(
            eglDisplay, config, EGL14.EGL_NO_CONTEXT, contextAttribs, 0
        )

        val surfaceAttribs = intArrayOf(EGL14.EGL_WIDTH, 1, EGL14.EGL_HEIGHT, 1, EGL14.EGL_NONE)
        eglSurface = EGL14.eglCreatePbufferSurface(eglDisplay, config, surfaceAttribs, 0)
        EGL14.eglMakeCurrent(eglDisplay, eglSurface, eglSurface, eglContext)

        // Clamp the export size to what this GPU can render to.
        val maxTex = IntArray(1)
        GLES20.glGetIntegerv(GLES20.GL_MAX_TEXTURE_SIZE, maxTex, 0)
        val cap = max(64, min(maxTex[0], maxDimension))
        val (tw, th) = fitWithin(source.width, source.height, cap)
        width = tw
        height = th

        val scaled = if (source.width == width && source.height == height) source
        else Bitmap.createScaledBitmap(source, width, height, true)
        sourceTex = GlUtil.createTexture()
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, sourceTex)
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, scaled, 0)
        if (scaled !== source) scaled.recycle()

        setupFbo()
        readBuffer = ByteBuffer.allocateDirect(width * height * 4).order(ByteOrder.nativeOrder())
    }

    private fun setupFbo() {
        val ids = IntArray(1)
        GLES20.glGenFramebuffers(1, ids, 0)
        fbo = ids[0]
        fboTex = GlUtil.createTexture()
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, fboTex)
        GLES20.glTexImage2D(
            GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, width, height, 0,
            GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null
        )
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fbo)
        GLES20.glFramebufferTexture2D(
            GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
            GLES20.GL_TEXTURE_2D, fboTex, 0
        )
        val status = GLES20.glCheckFramebufferStatus(GLES20.GL_FRAMEBUFFER)
        if (status != GLES20.GL_FRAMEBUFFER_COMPLETE) {
            throw RuntimeException("Offscreen framebuffer incomplete: $status")
        }
    }

    /** Render one frame and read it back as an upright ARGB_8888 bitmap. */
    fun renderFrame(effect: ShaderEffect, values: FloatArray, time: Float): Bitmap {
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, fbo)
        GLES20.glViewport(0, 0, width, height)
        GLES20.glClearColor(0f, 0f, 0f, 1f)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)

        val program = programCache.getOrPut(effect.id) { ShaderProgram.build(effect) }
        program.draw(sourceTex, values, width, height, time)

        readBuffer.rewind()
        GLES20.glReadPixels(
            0, 0, width, height, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, readBuffer
        )

        val raw = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        readBuffer.rewind()
        raw.copyPixelsFromBuffer(readBuffer)
        // glReadPixels is bottom-up; flip to image orientation.
        val flip = Matrix().apply { postScale(1f, -1f, width / 2f, height / 2f) }
        val out = Bitmap.createBitmap(raw, 0, 0, width, height, flip, true)
        if (out !== raw) raw.recycle()
        return out
    }

    fun release() {
        programCache.values.forEach { it.release() }
        programCache.clear()
        if (fbo != 0) GLES20.glDeleteFramebuffers(1, intArrayOf(fbo), 0)
        if (fboTex != 0) GLES20.glDeleteTextures(1, intArrayOf(fboTex), 0)
        if (sourceTex != 0) GLES20.glDeleteTextures(1, intArrayOf(sourceTex), 0)
        EGL14.eglMakeCurrent(
            eglDisplay, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT
        )
        EGL14.eglDestroySurface(eglDisplay, eglSurface)
        EGL14.eglDestroyContext(eglDisplay, eglContext)
        EGL14.eglTerminate(eglDisplay)
    }

    companion object {
        private fun fitWithin(w: Int, h: Int, cap: Int): Pair<Int, Int> {
            if (w <= cap && h <= cap) return w to h
            val scale = cap.toFloat() / max(w, h)
            return max(1, (w * scale).toInt()) to max(1, (h * scale).toInt())
        }
    }
}
