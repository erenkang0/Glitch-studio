package com.glitchstudio.app.export

import android.graphics.Bitmap
import com.glitchstudio.app.effects.ShaderEffect
import com.glitchstudio.app.gl.OffscreenRenderer
import java.io.ByteArrayOutputStream

/** Renders an animated effect to an in-memory GIF89a byte array. */
object GifRenderer {

    /**
     * @param frames number of frames to render (clamped to 1 for static effects)
     * @param fps playback rate; also sets per-frame delay
     * @param maxDimension export size cap on the longer edge (GIFs stay small)
     */
    fun render(
        source: Bitmap,
        effect: ShaderEffect,
        values: FloatArray,
        frames: Int,
        fps: Int,
        maxDimension: Int = 480,
        onProgress: ((Float) -> Unit)? = null,
    ): ByteArray {
        val frameCount = if (effect.animated) frames.coerceIn(2, 120) else 1
        val renderer = OffscreenRenderer(source, maxDimension)
        try {
            val out = ByteArrayOutputStream()
            val encoder = GifEncoder()
            encoder.setRepeat(0)
            encoder.setDelay(1000 / fps.coerceIn(1, 50))
            encoder.setQuality(12)
            encoder.start(out)
            for (i in 0 until frameCount) {
                val time = i.toFloat() / fps
                val frame = renderer.renderFrame(effect, values, time)
                encoder.addFrame(frame)
                frame.recycle()
                onProgress?.invoke((i + 1).toFloat() / frameCount)
            }
            encoder.finish()
            return out.toByteArray()
        } finally {
            renderer.release()
        }
    }
}
