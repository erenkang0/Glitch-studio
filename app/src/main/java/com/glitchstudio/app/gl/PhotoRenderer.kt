package com.glitchstudio.app.gl

import android.graphics.Bitmap
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.GLUtils
import com.glitchstudio.app.effects.Effect
import com.glitchstudio.app.effects.EffectRegistry
import com.glitchstudio.app.effects.ShaderLib
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.max
import kotlin.math.min

/**
 * Renders the source photo through a stack of effect layers. Each layer is
 * applied in sequence using ping-pong framebuffers; the per-layer shader
 * composites its result over the layer input using a mask and opacity.
 */
class PhotoRenderer : GLSurfaceView.Renderer {

    private class Program(val id: Int) {
        val aPos = GLES20.glGetAttribLocation(id, "aPos")
        val aTex = GLES20.glGetAttribLocation(id, "aTex")
        val uTex = GLES20.glGetUniformLocation(id, "uTex")
        val uMaskTex = GLES20.glGetUniformLocation(id, "uMaskTex")
        val uResolution = GLES20.glGetUniformLocation(id, "uResolution")
        val uAspect = GLES20.glGetUniformLocation(id, "uAspect")
        val uTime = GLES20.glGetUniformLocation(id, "uTime")
        val uOpacity = GLES20.glGetUniformLocation(id, "uOpacity")
        val uMaskType = GLES20.glGetUniformLocation(id, "uMaskType")
        val uMaskCenter = GLES20.glGetUniformLocation(id, "uMaskCenter")
        val uMaskSize = GLES20.glGetUniformLocation(id, "uMaskSize")
        val uMaskFeather = GLES20.glGetUniformLocation(id, "uMaskFeather")
        val uMaskAngle = GLES20.glGetUniformLocation(id, "uMaskAngle")
        val uMaskInvert = GLES20.glGetUniformLocation(id, "uMaskInvert")
        val uVScale = GLES20.glGetUniformLocation(id, "uVScale")
        val uVOffset = GLES20.glGetUniformLocation(id, "uVOffset")
        val pLoc = IntArray(8) { GLES20.glGetUniformLocation(id, "p$it") }
    }

    /** A reusable colour framebuffer. */
    private class Fbo {
        var fbo = 0; var tex = 0; var w = 0; var h = 0
        fun ensure(nw: Int, nh: Int) {
            if (fbo != 0 && w == nw && h == nh) return
            release()
            val t = IntArray(1); val f = IntArray(1)
            GLES20.glGenTextures(1, t, 0)
            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, t[0])
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
            GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, nw, nh, 0,
                GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, null)
            GLES20.glGenFramebuffers(1, f, 0)
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, f[0])
            GLES20.glFramebufferTexture2D(GLES20.GL_FRAMEBUFFER, GLES20.GL_COLOR_ATTACHMENT0,
                GLES20.GL_TEXTURE_2D, t[0], 0)
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
            tex = t[0]; fbo = f[0]; w = nw; h = nh
        }
        fun release() {
            if (fbo != 0) GLES20.glDeleteFramebuffers(1, intArrayOf(fbo), 0)
            if (tex != 0) GLES20.glDeleteTextures(1, intArrayOf(tex), 0)
            fbo = 0; tex = 0; w = 0; h = 0
        }
        fun reset() { fbo = 0; tex = 0; w = 0; h = 0 }
    }

    // --- shared state -------------------------------------------------------
    @Volatile var layers: List<RenderLayer> = listOf(RenderLayer.original())
    @Volatile var animating: Boolean = false

    /** When true the original photo is shown unmodified (before/after compare). */
    @Volatile var bypass: Boolean = false

    /** On-screen zoom/pan (preview only; export is always full-frame). */
    @Volatile var viewScale: Float = 1f
    @Volatile var viewPanX: Float = 0f
    @Volatile var viewPanY: Float = 0f

    @Volatile var lastFrameTime: Float = 0f
        private set

    private var sourceBitmap: Bitmap? = null
    @Volatile private var pendingBitmap: Bitmap? = null

    private var textureId = 0
    private var imageW = 0
    private var imageH = 0
    private var surfaceW = 0
    private var surfaceH = 0

    private val programCache = HashMap<String, Program>()
    private var fallbackProgram: Program? = null

    // Painted-mask textures, keyed by layer id -> [textureId, uploadedRevision].
    private val maskTextures = HashMap<Long, IntArray>()
    private var whiteTex = 0

    private val previewA = Fbo()
    private val previewB = Fbo()
    private val exportA = Fbo()
    private val exportB = Fbo()
    private val thumbA = Fbo()
    private val thumbB = Fbo()

    // Orientation-preserving quad (off-screen chain passes).
    private val quadNormalBuffer: FloatBuffer
    // V-flipped quad (final on-screen blit only), so on-screen top = image top.
    private val quadFlipBuffer: FloatBuffer

    private var animBaseNanos = System.nanoTime()
    private var pausedTime = 0f
    private var wasAnimating = false

    init {
        // x, y, u, v
        val normal = floatArrayOf(
            -1f, -1f, 0f, 0f,
            1f, -1f, 1f, 0f,
            -1f, 1f, 0f, 1f,
            1f, 1f, 1f, 1f
        )
        val flip = floatArrayOf(
            -1f, -1f, 0f, 1f,
            1f, -1f, 1f, 1f,
            -1f, 1f, 0f, 0f,
            1f, 1f, 1f, 0f
        )
        quadNormalBuffer = ByteBuffer.allocateDirect(normal.size * 4)
            .order(ByteOrder.nativeOrder()).asFloatBuffer().apply { put(normal).position(0) }
        quadFlipBuffer = ByteBuffer.allocateDirect(flip.size * 4)
            .order(ByteOrder.nativeOrder()).asFloatBuffer().apply { put(flip).position(0) }
    }

    // --- public API (UI thread) ---------------------------------------------

    fun setImage(bitmap: Bitmap) {
        sourceBitmap = bitmap
        pendingBitmap = bitmap
    }

    fun hasImage(): Boolean = sourceBitmap != null

    // --- Renderer -----------------------------------------------------------

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0.043f, 0.043f, 0.063f, 1f)
        GLES20.glDisable(GLES20.GL_DEPTH_TEST)
        programCache.clear()
        fallbackProgram = null
        textureId = 0
        whiteTex = 0
        maskTextures.clear()
        previewA.reset(); previewB.reset(); exportA.reset(); exportB.reset()
        thumbA.reset(); thumbB.reset()
        sourceBitmap?.let { pendingBitmap = it }
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        surfaceW = width
        surfaceH = height
    }

    override fun onDrawFrame(gl: GL10?) {
        uploadPendingIfNeeded()
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
        GLES20.glViewport(0, 0, surfaceW, surfaceH)
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
        if (textureId == 0) return

        val rect = fitRect(surfaceW, surfaceH, imageW, imageH)
        val w = rect[2]; val h = rect[3]
        val t = currentTime()
        lastFrameTime = t

        if (bypass) {
            // Before/after: show the untouched source.
            GLES20.glViewport(rect[0], rect[1], w, h)
            drawPassthrough(textureId, w, h, viewScale, viewPanX, viewPanY)
            return
        }

        previewA.ensure(w, h); previewB.ensure(w, h)
        val result = renderChain(layers, previewA, previewB, w, h, t)

        // Blit the final result to the screen, letter-boxed, with zoom/pan applied.
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
        GLES20.glViewport(rect[0], rect[1], w, h)
        drawPassthrough(result.tex, w, h, viewScale, viewPanX, viewPanY)
    }

    // --- internals ----------------------------------------------------------

    private fun currentTime(): Float {
        val anim = animating
        if (anim && !wasAnimating) animBaseNanos = System.nanoTime()
        else if (!anim && wasAnimating) pausedTime += (System.nanoTime() - animBaseNanos) / 1_000_000_000f
        wasAnimating = anim
        return if (anim) pausedTime + (System.nanoTime() - animBaseNanos) / 1_000_000_000f else pausedTime
    }

    private fun uploadPendingIfNeeded() {
        val bmp = pendingBitmap ?: return
        pendingBitmap = null
        if (textureId == 0) {
            val ids = IntArray(1)
            GLES20.glGenTextures(1, ids, 0)
            textureId = ids[0]
        }
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bmp, 0)
        imageW = bmp.width
        imageH = bmp.height
    }

    private fun programFor(effect: Effect): Program {
        programCache[effect.id]?.let { return it }
        val id = GlUtil.buildProgram(ShaderLib.VERTEX, effect.fragmentSource())
        if (id == 0) return ensureFallback()
        val program = Program(id)
        programCache[effect.id] = program
        return program
    }

    private fun ensureFallback(): Program {
        fallbackProgram?.let { return it }
        val program = Program(GlUtil.buildProgram(ShaderLib.VERTEX, EffectRegistry.original.fragmentSource()))
        fallbackProgram = program
        return program
    }

    private fun whiteTexId(): Int {
        if (whiteTex != 0) return whiteTex
        val ids = IntArray(1)
        GLES20.glGenTextures(1, ids, 0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, ids[0])
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_NEAREST)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_NEAREST)
        val white = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder())
        white.put(byteArrayOf(255.toByte(), 255.toByte(), 255.toByte(), 255.toByte())); white.position(0)
        GLES20.glTexImage2D(GLES20.GL_TEXTURE_2D, 0, GLES20.GL_RGBA, 1, 1, 0,
            GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, white)
        whiteTex = ids[0]
        return whiteTex
    }

    private fun ensureMaskTexture(layer: RenderLayer): Int {
        val bmp = layer.maskBitmap
        if (layer.maskType != MaskType.BRUSH || bmp == null || bmp.isRecycled) return whiteTexId()
        val entry = maskTextures[layer.id]
        if (entry != null && entry[1] == layer.maskRevision) return entry[0]
        val texId = if (entry != null && entry[0] != 0) entry[0] else {
            val ids = IntArray(1); GLES20.glGenTextures(1, ids, 0); ids[0]
        }
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, texId)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_S, GLES20.GL_CLAMP_TO_EDGE)
        GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_WRAP_T, GLES20.GL_CLAMP_TO_EDGE)
        GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bmp, 0)
        maskTextures[layer.id] = intArrayOf(texId, layer.maskRevision)
        return texId
    }

    /** Runs the given layer stack into ping-pong FBOs, returning the result FBO. */
    private fun renderChain(layersList: List<RenderLayer>, a: Fbo, b: Fbo, w: Int, h: Int, time: Float): Fbo {
        val active = layersList.filter { it.enabled }.ifEmpty { listOf(RenderLayer.original()) }
        var inputTex = textureId
        var dst = a
        var other = b
        var lastRendered = a
        for (layer in active) {
            GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, dst.fbo)
            GLES20.glViewport(0, 0, w, h)
            GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT)
            drawLayer(layer, inputTex, w, h, time)
            inputTex = dst.tex
            lastRendered = dst
            val tmp = dst; dst = other; other = tmp
        }
        return lastRendered
    }

    private fun bindQuad(p: Program, flip: Boolean) {
        val buf = if (flip) quadFlipBuffer else quadNormalBuffer
        buf.position(0)
        GLES20.glEnableVertexAttribArray(p.aPos)
        GLES20.glVertexAttribPointer(p.aPos, 2, GLES20.GL_FLOAT, false, 16, buf)
        buf.position(2)
        GLES20.glEnableVertexAttribArray(p.aTex)
        GLES20.glVertexAttribPointer(p.aTex, 2, GLES20.GL_FLOAT, false, 16, buf)
    }

    private fun drawLayer(
        layer: RenderLayer, inputTex: Int, w: Int, h: Int, time: Float,
        flip: Boolean = false, sx: Float = 1f, sy: Float = 1f, ox: Float = 0f, oy: Float = 0f
    ) {
        val p = programFor(layer.effect)
        GLES20.glUseProgram(p.id)
        bindQuad(p, flip)
        if (p.uVScale >= 0) GLES20.glUniform2f(p.uVScale, sx, sy)
        if (p.uVOffset >= 0) GLES20.glUniform2f(p.uVOffset, ox, oy)

        // Resolve the mask texture first (may bind/upload on the active unit).
        val maskTex = ensureMaskTexture(layer)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, inputTex)
        if (p.uTex >= 0) GLES20.glUniform1i(p.uTex, 0)
        GLES20.glActiveTexture(GLES20.GL_TEXTURE1)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, maskTex)
        if (p.uMaskTex >= 0) GLES20.glUniform1i(p.uMaskTex, 1)
        if (p.uResolution >= 0) GLES20.glUniform2f(p.uResolution, w.toFloat(), h.toFloat())
        if (p.uAspect >= 0) GLES20.glUniform1f(p.uAspect, w.toFloat() / max(h, 1).toFloat())
        if (p.uTime >= 0) GLES20.glUniform1f(p.uTime, time)
        if (p.uOpacity >= 0) GLES20.glUniform1f(p.uOpacity, layer.opacity)
        if (p.uMaskType >= 0) GLES20.glUniform1f(p.uMaskType, layer.maskType.ordinal.toFloat())
        if (p.uMaskCenter >= 0) GLES20.glUniform2f(p.uMaskCenter, layer.maskCx, layer.maskCy)
        if (p.uMaskSize >= 0) GLES20.glUniform1f(p.uMaskSize, layer.maskSize)
        if (p.uMaskFeather >= 0) GLES20.glUniform1f(p.uMaskFeather, layer.maskFeather)
        if (p.uMaskAngle >= 0) GLES20.glUniform1f(p.uMaskAngle, layer.maskAngle)
        if (p.uMaskInvert >= 0) GLES20.glUniform1f(p.uMaskInvert, if (layer.maskInvert) 1f else 0f)
        val pr = layer.params
        for (i in 0 until 8) if (p.pLoc[i] >= 0) {
            GLES20.glUniform1f(p.pLoc[i], if (i < pr.size) pr[i] else 0f)
        }

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)
        GLES20.glDisableVertexAttribArray(p.aPos)
        GLES20.glDisableVertexAttribArray(p.aTex)
    }

    private fun drawPassthrough(inputTex: Int, w: Int, h: Int, scale: Float, panX: Float, panY: Float) {
        drawLayer(RenderLayer.original(), inputTex, w, h, 0f,
            flip = true, sx = scale, sy = scale, ox = panX, oy = panY)
    }

    /** Render the full stack off-screen at [targetW] x [targetH] and read it back. */
    fun renderToBitmap(targetW: Int, targetH: Int, time: Float): Bitmap? {
        if (textureId == 0) return null
        val w = targetW.coerceAtLeast(1)
        val h = targetH.coerceAtLeast(1)
        exportA.ensure(w, h); exportB.ensure(w, h)
        val result = renderChain(layers, exportA, exportB, w, h, time)
        return readFbo(result, w, h)
    }

    /** Render a single effect (default params) on the source into a small preview bitmap. */
    fun renderEffectThumbnail(effect: Effect, cap: Int): Bitmap? {
        if (textureId == 0) return null
        val size = outputSizeFor(cap)
        val w = size[0]; val h = size[1]
        thumbA.ensure(w, h); thumbB.ensure(w, h)
        val layer = RenderLayer(0L, effect, FloatArray(8) { i -> effect.params.getOrNull(i)?.default ?: 0f })
        val result = renderChain(listOf(layer), thumbA, thumbB, w, h, 0f)
        return readFbo(result, w, h)
    }

    private fun readFbo(result: Fbo, w: Int, h: Int): Bitmap {
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, result.fbo)
        val buffer = ByteBuffer.allocateDirect(w * h * 4).order(ByteOrder.nativeOrder())
        GLES20.glReadPixels(0, 0, w, h, GLES20.GL_RGBA, GLES20.GL_UNSIGNED_BYTE, buffer)
        GLES20.glBindFramebuffer(GLES20.GL_FRAMEBUFFER, 0)
        return bufferToBitmap(buffer, w, h)
    }

    fun outputSizeFor(cap: Int): IntArray {
        val limit = min(cap, GlUtil.maxTextureSize())
        var w = imageW
        var h = imageH
        if (w <= 0 || h <= 0) return intArrayOf(limit, limit)
        if (w > limit || h > limit) {
            val s = limit.toFloat() / max(w, h)
            w = max(1, (w * s).toInt())
            h = max(1, (h * s).toInt())
        }
        return intArrayOf(w, h)
    }

    private fun bufferToBitmap(buffer: ByteBuffer, w: Int, h: Int): Bitmap {
        buffer.rewind()
        val bytes = ByteArray(w * h * 4)
        buffer.get(bytes)
        val ints = IntArray(w * h)
        // Chain passes preserve orientation (image top stored at t=0), and
        // glReadPixels row 0 == t=0, so rows map straight through (no flip).
        for (y in 0 until h) {
            val srcRow = y * w * 4
            val dstRow = y * w
            for (x in 0 until w) {
                val i = srcRow + x * 4
                val r = bytes[i].toInt() and 0xFF
                val g = bytes[i + 1].toInt() and 0xFF
                val b = bytes[i + 2].toInt() and 0xFF
                val a = bytes[i + 3].toInt() and 0xFF
                ints[dstRow + x] = (a shl 24) or (r shl 16) or (g shl 8) or b
            }
        }
        return Bitmap.createBitmap(ints, w, h, Bitmap.Config.ARGB_8888)
    }

    companion object {
        fun fitRect(vw: Int, vh: Int, iw: Int, ih: Int): IntArray {
            if (iw <= 0 || ih <= 0 || vw <= 0 || vh <= 0) return intArrayOf(0, 0, vw, vh)
            val viewAspect = vw.toFloat() / vh
            val imgAspect = iw.toFloat() / ih
            return if (imgAspect > viewAspect) {
                val newH = (vw / imgAspect).toInt()
                intArrayOf(0, (vh - newH) / 2, vw, newH)
            } else {
                val newW = (vh * imgAspect).toInt()
                intArrayOf((vw - newW) / 2, 0, newW, vh)
            }
        }
    }
}
