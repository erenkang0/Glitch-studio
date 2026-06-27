package com.glitchstudio.app.effects

private val C = Categories.COLOR

val colorEffects: List<Effect> = listOf(

    Effect(
        id = "duotone", name = "Duotone", category = C,
        params = listOf(
            EffectParam("Shadow Hue", 0f, 1f, 0.62f),
            EffectParam("Highlight Hue", 0f, 1f, 0.08f)
        ),
        body = """
        vec4 process(vec2 uv) {
            float l = luma(tex(uv));
            vec3 a = hsv2rgb(vec3(p0, 0.7, 0.25));
            vec3 b = hsv2rgb(vec3(p1, 0.6, 1.0));
            return vec4(mix(a, b, l), 1.0);
        }"""
    ),

    Effect(
        id = "gradient_map", name = "Gradient Map", category = C,
        params = listOf(
            EffectParam("Low Hue", 0f, 1f, 0.0f),
            EffectParam("High Hue", 0f, 1f, 0.15f)
        ),
        body = """
        vec4 process(vec2 uv) {
            float l = luma(tex(uv));
            vec3 a = hsv2rgb(vec3(p0, 0.8, 0.1));
            vec3 b = hsv2rgb(vec3(p1, 0.9, 1.0));
            return vec4(mix(a, b, smoothstep(0.0, 1.0, l)), 1.0);
        }"""
    ),

    Effect(
        id = "hue_rotate", name = "Hue Rotate", category = C,
        params = listOf(EffectParam("Shift", 0f, 1f, 0.2f)),
        body = """
        vec4 process(vec2 uv) {
            vec3 h = rgb2hsv(tex(uv));
            h.x = fract(h.x + p0);
            return vec4(hsv2rgb(h), 1.0);
        }"""
    ),

    Effect(
        id = "saturation", name = "Saturation", category = C,
        params = listOf(EffectParam("Amount", 0f, 2f, 1.3f)),
        body = """
        vec4 process(vec2 uv) {
            vec3 c = tex(uv);
            float l = luma(c);
            return vec4(clamp(mix(vec3(l), c, p0), 0.0, 1.0), 1.0);
        }"""
    ),

    Effect(
        id = "contrast", name = "Contrast", category = C,
        params = listOf(
            EffectParam("Contrast", 0f, 2f, 1.2f),
            EffectParam("Brightness", -0.5f, 0.5f, 0.0f)
        ),
        body = """
        vec4 process(vec2 uv) {
            vec3 c = (tex(uv) - 0.5) * p0 + 0.5 + p1;
            return vec4(clamp(c, 0.0, 1.0), 1.0);
        }"""
    ),

    Effect(
        id = "temperature", name = "Temperature", category = C,
        params = listOf(
            EffectParam("Warmth", -1f, 1f, 0.25f),
            EffectParam("Tint", -1f, 1f, 0.0f)
        ),
        body = """
        vec4 process(vec2 uv) {
            vec3 c = tex(uv);
            c.r += p0 * 0.15; c.b -= p0 * 0.15;
            c.g += p1 * 0.15;
            return vec4(clamp(c, 0.0, 1.0), 1.0);
        }"""
    ),

    Effect(
        id = "sepia", name = "Sepia", category = C,
        params = listOf(EffectParam("Amount", 0f, 1f, 1f)),
        body = """
        vec4 process(vec2 uv) {
            vec3 c = tex(uv);
            vec3 s = vec3(dot(c, vec3(0.393, 0.769, 0.189)),
                          dot(c, vec3(0.349, 0.686, 0.168)),
                          dot(c, vec3(0.272, 0.534, 0.131)));
            return vec4(mix(c, clamp(s, 0.0, 1.0), p0), 1.0);
        }"""
    ),

    Effect(
        id = "grayscale", name = "Grayscale", category = C,
        params = listOf(EffectParam("Amount", 0f, 1f, 1f)),
        body = """
        vec4 process(vec2 uv) {
            vec3 c = tex(uv);
            return vec4(mix(c, vec3(luma(c)), p0), 1.0);
        }"""
    ),

    Effect(
        id = "invert", name = "Invert", category = C,
        params = listOf(EffectParam("Amount", 0f, 1f, 1f)),
        body = """
        vec4 process(vec2 uv) {
            vec3 c = tex(uv);
            return vec4(mix(c, 1.0 - c, p0), 1.0);
        }"""
    ),

    Effect(
        id = "solarize", name = "Solarize", category = C,
        params = listOf(EffectParam("Threshold", 0f, 1f, 0.5f)),
        body = """
        vec4 process(vec2 uv) {
            vec3 c = tex(uv);
            return vec4(mix(c, 1.0 - c, step(p0, c)), 1.0);
        }"""
    ),

    Effect(
        id = "thermal", name = "Thermal", category = C,
        params = listOf(EffectParam("Amount", 0f, 1f, 1f)),
        body = """
        vec4 process(vec2 uv) {
            float l = luma(tex(uv));
            vec3 col = hsv2rgb(vec3((1.0 - l) * 0.7, 1.0, 1.0));
            return vec4(mix(tex(uv), col, p0), 1.0);
        }"""
    ),

    Effect(
        id = "night_vision", name = "Night Vision", category = C, animated = true,
        params = listOf(
            EffectParam("Amount", 0f, 1f, 1f),
            EffectParam("Noise", 0f, 1f, 0.3f)
        ),
        body = """
        vec4 process(vec2 uv) {
            float l = luma(tex(uv));
            float n = hash21(uv * uResolution + uTime * 60.0) * p1;
            l = l * 1.4 + n * 0.2;
            l *= 0.85 + 0.15 * sin(uv.y * uResolution.y * 1.5);
            float v = 1.0 - dot(uv - 0.5, uv - 0.5) * 1.2;
            vec3 col = vec3(0.1, l, 0.1) * v;
            return vec4(mix(tex(uv), col, p0), 1.0);
        }"""
    ),

    Effect(
        id = "predator", name = "Predator", category = C,
        params = listOf(EffectParam("Bands", 3f, 12f, 6f)),
        body = """
        vec4 process(vec2 uv) {
            float l = luma(tex(uv));
            float q = floor(l * p0) / p0;
            return vec4(hsv2rgb(vec3((1.0 - q) * 0.75, 1.0, 1.0)), 1.0);
        }"""
    ),

    Effect(
        id = "teal_orange", name = "Teal & Orange", category = C,
        params = listOf(EffectParam("Strength", 0f, 1f, 0.6f)),
        body = """
        vec4 process(vec2 uv) {
            vec3 c = tex(uv);
            float l = luma(c);
            vec3 shadow = vec3(0.0, 0.3, 0.4);
            vec3 high = vec3(1.0, 0.6, 0.2);
            vec3 grade = mix(c, mix(shadow, high, l), p0 * 0.6);
            return vec4(clamp(grade, 0.0, 1.0), 1.0);
        }"""
    ),

    Effect(
        id = "bleach_bypass", name = "Bleach Bypass", category = C,
        params = listOf(EffectParam("Amount", 0f, 1f, 0.7f)),
        body = """
        vec4 process(vec2 uv) {
            vec3 c = tex(uv);
            float l = luma(c);
            vec3 b = mix(c * l * 2.0, 1.0 - 2.0 * (1.0 - c) * (1.0 - l), step(0.5, l));
            return vec4(clamp(mix(c, b, p0), 0.0, 1.0), 1.0);
        }"""
    ),

    Effect(
        id = "cross_process", name = "Cross Process", category = C,
        params = listOf(EffectParam("Amount", 0f, 1f, 0.7f)),
        body = """
        vec4 process(vec2 uv) {
            vec3 c = tex(uv);
            vec3 x = c;
            x.r = smoothstep(0.0, 1.0, x.r);
            x.g = pow(x.g, 0.9);
            x.b = x.b * 0.8 + 0.1;
            return vec4(clamp(mix(c, x, p0), 0.0, 1.0), 1.0);
        }"""
    ),

    Effect(
        id = "technicolor", name = "Technicolor", category = C,
        params = listOf(EffectParam("Amount", 0f, 1f, 0.8f)),
        body = """
        vec4 process(vec2 uv) {
            vec3 c = tex(uv);
            vec3 t;
            t.r = c.r - (c.g + c.b) * 0.3 + 0.3;
            t.g = c.g - (c.r + c.b) * 0.3 + 0.3;
            t.b = c.b - (c.r + c.g) * 0.3 + 0.3;
            return vec4(clamp(mix(c, t, p0), 0.0, 1.0), 1.0);
        }"""
    ),

    Effect(
        id = "infrared", name = "Infrared", category = C,
        params = listOf(EffectParam("Amount", 0f, 1f, 1f)),
        body = """
        vec4 process(vec2 uv) {
            vec3 c = tex(uv);
            vec3 ir = vec3(c.g, c.b, c.r);
            ir.r = 1.0 - ir.r * 0.5;
            return vec4(mix(c, clamp(ir, 0.0, 1.0), p0), 1.0);
        }"""
    )
)
