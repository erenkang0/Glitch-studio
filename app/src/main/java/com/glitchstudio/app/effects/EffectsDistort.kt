package com.glitchstudio.app.effects

private val C = Categories.DISTORT

val distortEffects: List<Effect> = listOf(

    Effect(
        id = "swirl", name = "Swirl", category = C,
        params = listOf(
            EffectParam("Angle", -6.28f, 6.28f, 3f),
            EffectParam("Radius", 0.1f, 1.5f, 0.6f)
        ),
        body = """
        vec4 process(vec2 uv) {
            vec2 p = centered(uv);
            float d = length(p);
            float a = p0 * smoothstep(p1, 0.0, d);
            p = rot(a) * p;
            return vec4(tex(uncentered(p)), 1.0);
        }"""
    ),

    Effect(
        id = "fisheye", name = "Fisheye", category = C,
        params = listOf(EffectParam("Strength", -1f, 1f, 0.5f)),
        body = """
        vec4 process(vec2 uv) {
            vec2 p = centered(uv);
            float r = length(p);
            float rn = r * (1.0 + p0 * r * r);
            p = r > 0.0 ? p / r * rn : p;
            return vec4(tex(uncentered(p)), 1.0);
        }"""
    ),

    Effect(
        id = "bulge", name = "Bulge", category = C,
        params = listOf(
            EffectParam("Strength", 0f, 1f, 0.5f),
            EffectParam("Radius", 0.1f, 1.2f, 0.6f)
        ),
        body = """
        vec4 process(vec2 uv) {
            vec2 p = centered(uv);
            float d = length(p);
            float pct = smoothstep(p1, 0.0, d);
            p *= mix(1.0, 1.0 - p0 * 0.6, pct);
            return vec4(tex(uncentered(p)), 1.0);
        }"""
    ),

    Effect(
        id = "pinch", name = "Pinch", category = C,
        params = listOf(
            EffectParam("Strength", 0f, 1f, 0.5f),
            EffectParam("Radius", 0.1f, 1.2f, 0.6f)
        ),
        body = """
        vec4 process(vec2 uv) {
            vec2 p = centered(uv);
            float d = length(p);
            float pct = smoothstep(p1, 0.0, d);
            p *= mix(1.0, 1.0 + p0 * 1.2, pct);
            return vec4(tex(uncentered(p)), 1.0);
        }"""
    ),

    Effect(
        id = "ripple", name = "Ripple", category = C, animated = true,
        params = listOf(
            EffectParam("Amplitude", 0f, 0.1f, 0.03f),
            EffectParam("Frequency", 5f, 60f, 25f),
            EffectParam("Speed", 0f, 10f, 3f)
        ),
        body = """
        vec4 process(vec2 uv) {
            vec2 p = centered(uv);
            float d = length(p);
            float w = sin(d * p1 - uTime * p2) * p0;
            vec2 dir = d > 0.0 ? p / d : vec2(0.0);
            return vec4(tex(uv + dir * w), 1.0);
        }"""
    ),

    Effect(
        id = "wave_distort", name = "Wave", category = C, animated = true,
        params = listOf(
            EffectParam("Amplitude", 0f, 0.1f, 0.03f),
            EffectParam("Frequency", 1f, 40f, 12f),
            EffectParam("Speed", 0f, 8f, 2f)
        ),
        body = """
        vec4 process(vec2 uv) {
            uv.x += sin(uv.y * p1 + uTime * p2) * p0;
            uv.y += cos(uv.x * p1 + uTime * p2) * p0;
            return vec4(tex(uv), 1.0);
        }"""
    ),

    Effect(
        id = "kaleidoscope", name = "Kaleidoscope", category = C,
        params = listOf(
            EffectParam("Segments", 2f, 16f, 6f),
            EffectParam("Rotation", 0f, 6.28f, 0f)
        ),
        body = """
        vec4 process(vec2 uv) {
            vec2 p = centered(uv);
            float a = atan(p.y, p.x) + p1;
            float r = length(p);
            float seg = TAU / p0;
            a = mod(a, seg);
            a = abs(a - seg * 0.5);
            p = vec2(cos(a), sin(a)) * r;
            return vec4(tex(uncentered(p)), 1.0);
        }"""
    ),

    Effect(
        id = "polar", name = "Polar Coords", category = C,
        params = listOf(EffectParam("Amount", 0f, 1f, 1f)),
        body = """
        vec4 process(vec2 uv) {
            vec2 p = centered(uv);
            float r = length(p) * 2.0;
            float a = atan(p.y, p.x) / TAU + 0.5;
            return vec4(tex(mix(uv, vec2(a, r), p0)), 1.0);
        }"""
    ),

    Effect(
        id = "mirror", name = "Mirror", category = C,
        params = listOf(EffectParam("Position", 0f, 1f, 0.5f)),
        body = """
        vec4 process(vec2 uv) {
            if (uv.x > p0) uv.x = 2.0 * p0 - uv.x;
            return vec4(tex(uv), 1.0);
        }"""
    ),

    Effect(
        id = "quad_mirror", name = "Quad Mirror", category = C,
        params = listOf(EffectParam("Zoom", 0.5f, 2f, 1f)),
        body = """
        vec4 process(vec2 uv) {
            vec2 p = abs(uv - 0.5) * p0;
            return vec4(tex(clamp(p, 0.0, 1.0)), 1.0);
        }"""
    ),

    Effect(
        id = "frosted_glass", name = "Frosted Glass", category = C,
        params = listOf(
            EffectParam("Strength", 0f, 0.05f, 0.015f),
            EffectParam("Scale", 1f, 20f, 8f)
        ),
        body = """
        vec4 process(vec2 uv) {
            vec2 o = vec2(vnoise(uv * p1 * 50.0), vnoise(uv * p1 * 50.0 + 13.0)) - 0.5;
            return vec4(tex(uv + o * p0), 1.0);
        }"""
    ),

    Effect(
        id = "glass_tiles", name = "Glass Tiles", category = C,
        params = listOf(
            EffectParam("Tiles", 8f, 60f, 24f),
            EffectParam("Refraction", 0f, 0.06f, 0.02f)
        ),
        body = """
        vec4 process(vec2 uv) {
            float n = p0;
            vec2 f = fract(uv * n) - 0.5;
            return vec4(tex(uv - f * p1), 1.0);
        }"""
    ),

    Effect(
        id = "wobble", name = "Wobble", category = C, animated = true,
        params = listOf(
            EffectParam("Amount", 0f, 0.05f, 0.02f),
            EffectParam("Speed", 0f, 8f, 3f),
            EffectParam("Scale", 1f, 20f, 6f)
        ),
        body = """
        vec4 process(vec2 uv) {
            float t = uTime * p1;
            uv.x += sin(uv.y * p2 + t) * p0;
            uv.y += cos(uv.x * p2 + t * 1.3) * p0;
            return vec4(tex(uv), 1.0);
        }"""
    ),

    Effect(
        id = "lens_distortion", name = "Lens Distortion", category = C,
        params = listOf(EffectParam("Amount", -0.6f, 0.6f, 0.3f)),
        body = """
        vec4 process(vec2 uv) {
            vec2 p = centered(uv);
            float r2 = dot(p, p);
            p *= 1.0 + p0 * r2;
            return vec4(tex(uncentered(p)), 1.0);
        }"""
    ),

    Effect(
        id = "zoom_blur", name = "Zoom Blur", category = C,
        params = listOf(
            EffectParam("Strength", 0f, 1f, 0.4f),
            EffectParam("Center X", 0f, 1f, 0.5f),
            EffectParam("Center Y", 0f, 1f, 0.5f)
        ),
        body = """
        vec4 process(vec2 uv) {
            vec2 c = vec2(p1, p2);
            vec3 sum = vec3(0.0);
            for (int i = 0; i < 16; i++) {
                float s = 1.0 - p0 * 0.3 * float(i) / 16.0;
                sum += tex((uv - c) * s + c);
            }
            return vec4(sum / 16.0, 1.0);
        }"""
    ),

    Effect(
        id = "spin_blur", name = "Spin Blur", category = C,
        params = listOf(EffectParam("Strength", 0f, 1f, 0.4f)),
        body = """
        vec4 process(vec2 uv) {
            vec2 p = centered(uv);
            float r = length(p);
            float a = atan(p.y, p.x);
            vec3 sum = vec3(0.0);
            for (int i = 0; i < 16; i++) {
                float da = (float(i) / 16.0 - 0.5) * p0 * 0.6;
                vec2 q = vec2(cos(a + da), sin(a + da)) * r;
                sum += tex(uncentered(q));
            }
            return vec4(sum / 16.0, 1.0);
        }"""
    ),

    Effect(
        id = "motion_blur", name = "Motion Blur", category = C,
        params = listOf(
            EffectParam("Angle", 0f, 6.28f, 0f),
            EffectParam("Length", 0f, 0.1f, 0.03f)
        ),
        body = """
        vec4 process(vec2 uv) {
            vec2 dir = vec2(cos(p0), sin(p0)) * p1;
            vec3 sum = vec3(0.0);
            for (int i = 0; i < 12; i++) {
                float t = float(i) / 11.0 - 0.5;
                sum += tex(uv + dir * t);
            }
            return vec4(sum / 12.0, 1.0);
        }"""
    ),

    Effect(
        id = "double_vision", name = "Double Vision", category = C,
        params = listOf(EffectParam("Offset", 0f, 0.05f, 0.015f)),
        body = """
        vec4 process(vec2 uv) {
            vec3 a = tex(uv + vec2(p0, 0.0));
            vec3 b = tex(uv - vec2(p0, 0.0));
            return vec4(mix(a, b, 0.5), 1.0);
        }"""
    )
)
