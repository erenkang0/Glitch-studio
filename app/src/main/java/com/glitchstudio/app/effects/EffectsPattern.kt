package com.glitchstudio.app.effects

private val C = Categories.PATTERN

val patternEffects: List<Effect> = listOf(

    Effect(
        id = "plasma", name = "Plasma", category = C, animated = true,
        params = listOf(
            EffectParam("Speed", 0f, 4f, 1f),
            EffectParam("Scale", 1f, 10f, 4f),
            EffectParam("Blend", 0f, 1f, 0.5f)
        ),
        body = """
        vec4 process(vec2 uv) {
            float t = uTime * p0;
            vec2 p = uv * p1;
            float v = sin(p.x + t) + sin(p.y + t) +
                      sin((p.x + p.y) * 0.5 + t) + sin(length(p - p1 * 0.5) + t);
            vec3 col = hsv2rgb(vec3(0.5 + 0.5 * sin(v), 0.7, 1.0));
            return vec4(mix(tex(uv), col, p2), 1.0);
        }"""
    ),

    Effect(
        id = "interference", name = "Interference", category = C, animated = true,
        params = listOf(
            EffectParam("Frequency", 10f, 200f, 80f),
            EffectParam("Angle", 0f, 3.14f, 0.3f),
            EffectParam("Speed", 0f, 6f, 2f)
        ),
        body = """
        vec4 process(vec2 uv) {
            vec2 p = rot(p1) * centered(uv);
            float m = sin(p.x * p0 + uTime * p2) * sin(p.y * p0 - uTime * p2);
            return vec4(tex(uv) * (0.7 + 0.3 * m), 1.0);
        }"""
    ),

    Effect(
        id = "color_cycle", name = "Color Cycle", category = C, animated = true,
        params = listOf(EffectParam("Speed", 0f, 4f, 1f)),
        body = """
        vec4 process(vec2 uv) {
            vec3 h = rgb2hsv(tex(uv));
            h.x = fract(h.x + uTime * p0 * 0.1);
            return vec4(hsv2rgb(h), 1.0);
        }"""
    ),

    Effect(
        id = "trippy_warp", name = "Trippy Warp", category = C, animated = true,
        params = listOf(
            EffectParam("Amount", 0f, 0.1f, 0.04f),
            EffectParam("Speed", 0f, 4f, 1.5f),
            EffectParam("Scale", 1f, 10f, 4f)
        ),
        body = """
        vec4 process(vec2 uv) {
            float t = uTime * p1;
            vec2 q = uv;
            q.x += sin(uv.y * p2 + t) * p0;
            q.y += cos(uv.x * p2 + t) * p0;
            vec3 h = rgb2hsv(tex(q));
            h.x = fract(h.x + sin(t) * 0.1);
            return vec4(hsv2rgb(h), 1.0);
        }"""
    ),

    Effect(
        id = "scanlines", name = "Scanlines", category = C,
        params = listOf(
            EffectParam("Count", 100f, 1000f, 400f),
            EffectParam("Intensity", 0f, 1f, 0.5f)
        ),
        body = """
        vec4 process(vec2 uv) {
            vec3 c = tex(uv);
            float s = 0.5 + 0.5 * sin(uv.y * p0 * PI);
            c *= 1.0 - p1 * (1.0 - s) * 0.5;
            return vec4(c, 1.0);
        }"""
    )
)
