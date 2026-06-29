package com.glitchstudio.app.export

import android.graphics.Bitmap
import com.glitchstudio.app.effects.ShaderEffect
import com.glitchstudio.app.gl.OffscreenRenderer

/** Renders a single full-resolution still frame of an effect. */
object ImageRenderer {

    fun render(
        source: Bitmap,
        effect: ShaderEffect,
        values: FloatArray,
        maxDimension: Int = Int.MAX_VALUE,
        time: Float = 0f,
    ): Bitmap {
        val renderer = OffscreenRenderer(source, maxDimension)
        return try {
            renderer.renderFrame(effect, values, time)
        } finally {
            renderer.release()
        }
    }
}
