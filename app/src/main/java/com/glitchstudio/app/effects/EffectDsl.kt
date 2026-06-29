package com.glitchstudio.app.effects

/** Lightweight param spec; the [ShaderParam.uniform] is auto-assigned u_p0, u_p1, ... */
internal data class P(
    val name: String,
    val min: Float,
    val max: Float,
    val default: Float,
    val bipolar: Boolean = false,
)

/** Builds a [ShaderEffect], wiring param uniforms by index so bodies use u_p0.. */
internal fun fx(
    id: String,
    name: String,
    category: EffectCategory,
    description: String,
    params: List<P>,
    body: String,
    animated: Boolean = false,
): ShaderEffect {
    val sp = params.mapIndexed { i, p ->
        ShaderParam(
            name = p.name,
            uniform = "u_p$i",
            min = p.min,
            max = p.max,
            default = p.default,
            bipolar = p.bipolar,
            kind = inferKind(p.name),
            role = inferRole(p.name),
        )
    }
    return ShaderEffect(id, name, category, description, sp, body, animated)
}

private fun inferKind(name: String): ParamKind = when {
    name.contains("hue", true) -> ParamKind.HUE
    name.contains("angle", true) || name.contains("rotation", true) ||
        name.contains("direction", true) -> ParamKind.ANGLE
    else -> ParamKind.LINEAR
}

private fun inferRole(name: String): ParamRole = when {
    name.equals("pos x", true) || name.equals("position x", true) -> ParamRole.POS_X
    name.equals("pos y", true) || name.equals("position y", true) -> ParamRole.POS_Y
    name.contains("angle", true) || name.contains("rotation", true) ||
        name.contains("direction", true) -> ParamRole.ANGLE
    else -> ParamRole.NONE
}

// --- Reusable GLSL snippets prepended to effect bodies that need them ----------

internal const val LIB_HASH = """
float hash21(vec2 p){ return fract(sin(dot(p, vec2(127.1,311.7))) * 43758.5453123); }
float hash11(float p){ return fract(sin(p * 78.233) * 43758.5453123); }
"""

internal const val LIB_NOISE = LIB_HASH + """
float vnoise(vec2 p){
    vec2 i = floor(p);
    vec2 f = fract(p);
    float a = hash21(i);
    float b = hash21(i + vec2(1.0, 0.0));
    float c = hash21(i + vec2(0.0, 1.0));
    float d = hash21(i + vec2(1.0, 1.0));
    vec2 u = f * f * (3.0 - 2.0 * f);
    return mix(a, b, u.x) + (c - a) * u.y * (1.0 - u.x) + (d - b) * u.x * u.y;
}
"""

internal const val LIB_HSV = """
vec3 rgb2hsv(vec3 c){
    vec4 K = vec4(0.0, -1.0/3.0, 2.0/3.0, -1.0);
    vec4 p = mix(vec4(c.bg, K.wz), vec4(c.gb, K.xy), step(c.b, c.g));
    vec4 q = mix(vec4(p.xyw, c.r), vec4(c.r, p.yzx), step(p.x, c.r));
    float d = q.x - min(q.w, q.y);
    float e = 1.0e-10;
    return vec3(abs(q.z + (q.w - q.y) / (6.0 * d + e)), d / (q.x + e), q.x);
}
vec3 hsv2rgb(vec3 c){
    vec4 K = vec4(1.0, 2.0/3.0, 1.0/3.0, 3.0);
    vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);
    return c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y);
}
"""

internal const val LIB_LUMA = """
float luma(vec3 c){ return dot(c, vec3(0.299, 0.587, 0.114)); }
"""
