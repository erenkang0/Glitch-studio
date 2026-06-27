package com.glitchstudio.app.effects

/**
 * A single tunable parameter of an effect. Values are mapped, in order, to the
 * GLSL uniforms p0..p7 declared in [ShaderLib.HEADER].
 */
data class EffectParam(
    val name: String,
    val min: Float,
    val max: Float,
    val default: Float
)

/**
 * A creative effect. [body] is a GLSL ES 1.00 snippet that must define:
 *
 *     vec4 process(vec2 uv) { ... }
 *
 * It may use any helper / uniform declared in [ShaderLib.HEADER] (uTime,
 * uResolution, uAspect, the p0..p7 params, tex(), noise(), hsv helpers, ...).
 */
data class Effect(
    val id: String,
    val name: String,
    val category: String,
    val animated: Boolean = false,
    val params: List<EffectParam> = emptyList(),
    val body: String
) {
    fun fragmentSource(): String = ShaderLib.HEADER + "\n" + body + "\n" + ShaderLib.MAIN
}

/** Categories used to group effects in the browser. */
object Categories {
    const val GLITCH = "Glitch"
    const val RETRO = "Retro"
    const val DISTORT = "Distort"
    const val STYLIZE = "Stylize"
    const val COLOR = "Color"
    const val LIGHT = "Light"
    const val PATTERN = "Pattern"

    val ALL = listOf(GLITCH, RETRO, DISTORT, STYLIZE, COLOR, LIGHT, PATTERN)
}
