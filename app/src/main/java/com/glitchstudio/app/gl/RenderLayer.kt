package com.glitchstudio.app.gl

import android.graphics.Bitmap
import com.glitchstudio.app.effects.Effect
import com.glitchstudio.app.effects.EffectRegistry

/** Mask shapes available per layer. NONE/RADIAL/LINEAR are procedural; BRUSH is painted. */
enum class MaskType { NONE, RADIAL, LINEAR, BRUSH }

/**
 * An immutable, GL-thread-facing snapshot of one effect layer in the stack.
 * The UI builds a fresh list of these whenever the editor state changes.
 */
data class RenderLayer(
    val id: Long,
    val effect: Effect,
    val params: FloatArray,
    val opacity: Float = 1f,
    val enabled: Boolean = true,
    val maskType: MaskType = MaskType.NONE,
    val maskCx: Float = 0.5f,
    val maskCy: Float = 0.5f,
    val maskSize: Float = 0.4f,
    val maskFeather: Float = 0.15f,
    val maskAngle: Float = 0f,
    val maskInvert: Boolean = false,
    val maskBitmap: Bitmap? = null,
    val maskRevision: Int = 0
) {
    // Identity contract isn't needed; suppress the data-class array warnings.
    override fun equals(other: Any?) = this === other
    override fun hashCode() = System.identityHashCode(this)

    companion object {
        fun original() = RenderLayer(0L, EffectRegistry.original, FloatArray(8))
    }
}
