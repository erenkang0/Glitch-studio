package com.glitchstudio.app.effects

private val C = Categories.DISTORT

val distortEffects2: List<Effect> = listOf(

    Effect("barrel", "Barrel", C,
        params = listOf(EffectParam("Amount", -1f, 1f, 0.4f)),
        body = """
        vec4 process(vec2 uv) {
            vec2 p = centered(uv);
            float r2 = dot(p, p);
            p *= 1.0 + p0 * r2 + p0 * 0.5 * r2 * r2;
            return vec4(tex(uncentered(p)), 1.0);
        }"""),

    Effect("shockwave", "Shockwave", C, animated = true,
        params = listOf(
            EffectParam("Amplitude", 0f, 0.1f, 0.04f),
            EffectParam("Speed", 0f, 4f, 1f),
            EffectParam("Width", 0.02f, 0.2f, 0.06f)
        ),
        body = """
        vec4 process(vec2 uv) {
            vec2 p = centered(uv);
            float d = length(p);
            float t = fract(uTime * p1 * 0.3);
            float ring = smoothstep(t - p2, t, d) * smoothstep(t + p2, t, d);
            vec2 dir = d > 0.0 ? p / d : vec2(0.0);
            return vec4(tex(uv + dir * ring * p0), 1.0);
        }"""),

    Effect("stretch", "Stretch", C,
        params = listOf(
            EffectParam("Stretch X", -0.5f, 2f, 0.3f),
            EffectParam("Stretch Y", -0.5f, 2f, 0.0f)
        ),
        body = """
        vec4 process(vec2 uv) {
            vec2 p = uv - 0.5;
            p.x /= (1.0 + p0);
            p.y /= (1.0 + p1);
            return vec4(tex(p + 0.5), 1.0);
        }"""),

    Effect("pixel_melt", "Pixel Melt", C, animated = true,
        params = listOf(
            EffectParam("Amount", 0f, 0.3f, 0.1f),
            EffectParam("Speed", 0f, 4f, 1f)
        ),
        body = """
        vec4 process(vec2 uv) {
            float n = vnoise(vec2(uv.x * 20.0, 0.0));
            uv.y += n * p0 * (0.5 + 0.5 * sin(uTime * p1));
            return vec4(tex(uv), 1.0);
        }"""),

    Effect("wormhole", "Wormhole", C,
        params = listOf(EffectParam("Strength", -2f, 2f, 1f)),
        body = """
        vec4 process(vec2 uv) {
            vec2 p = centered(uv);
            float d = length(p);
            float a = atan(p.y, p.x) + p0 / (d + 0.1);
            p = vec2(cos(a), sin(a)) * d;
            return vec4(tex(uncentered(p)), 1.0);
        }"""),

    Effect("glitch_warp", "Glitch Warp", C, animated = true,
        params = listOf(
            EffectParam("Amount", 0f, 0.15f, 0.05f),
            EffectParam("Scale", 1f, 12f, 4f),
            EffectParam("Speed", 0f, 4f, 1f)
        ),
        body = """
        vec4 process(vec2 uv) {
            float t = uTime * p2;
            vec2 d = vec2(fbm(uv * p1 + t), fbm(uv * p1 + 5.0 - t)) - 0.5;
            return vec4(tex(uv + d * p0), 1.0);
        }"""),

    Effect("fold_mirror", "Fold Mirror", C,
        params = listOf(EffectParam("Count", 1f, 6f, 2f)),
        body = """
        vec4 process(vec2 uv) {
            vec2 p = abs(mod(uv * p0, 2.0) - 1.0);
            return vec4(tex(p), 1.0);
        }"""),

    Effect("squeeze", "Squeeze", C,
        params = listOf(EffectParam("Amount", -1f, 1f, 0.5f)),
        body = """
        vec4 process(vec2 uv) {
            vec2 p = centered(uv);
            p.x *= 1.0 + p0 * abs(p.y) * 2.0;
            return vec4(tex(uncentered(p)), 1.0);
        }"""),

    Effect("water_drop", "Water Drop", C, animated = true,
        params = listOf(
            EffectParam("Amplitude", 0f, 0.1f, 0.03f),
            EffectParam("Frequency", 5f, 60f, 30f),
            EffectParam("Speed", 0f, 8f, 4f)
        ),
        body = """
        vec4 process(vec2 uv) {
            vec2 p = uv - 0.5;
            float d = length(p);
            float w = sin(d * p1 - uTime * p2) * p0 * exp(-d * 3.0);
            vec2 dir = d > 0.0 ? p / d : vec2(0.0);
            return vec4(tex(uv + dir * w), 1.0);
        }"""),

    Effect("tunnel", "Tunnel", C, animated = true,
        params = listOf(
            EffectParam("Depth", 0.05f, 0.5f, 0.2f),
            EffectParam("Speed", 0f, 2f, 0.4f)
        ),
        body = """
        vec4 process(vec2 uv) {
            vec2 p = centered(uv);
            float a = atan(p.y, p.x) / TAU + 0.5;
            float r = p0 / (length(p) + 0.05) + uTime * p1;
            return vec4(tex(fract(vec2(a, r))), 1.0);
        }""")
)
