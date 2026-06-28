package com.glitchstudio.app.effects

private val C = Categories.TEXTURE

val textureEffects: List<Effect> = listOf(

    Effect("grain_heavy", "Heavy Grain", C, animated = true,
        params = listOf(
            EffectParam("Amount", 0f, 1f, 0.5f),
            EffectParam("Size", 0.5f, 4f, 1.5f)
        ),
        body = """
        vec4 process(vec2 uv) {
            vec3 c = tex(uv);
            float n = hash21(floor(uv * uResolution / p1) + floor(uTime * 24.0));
            return vec4(c + (n - 0.5) * p0, 1.0);
        }"""),

    Effect("scratches_tex", "Scratches", C, animated = true,
        params = listOf(EffectParam("Amount", 0f, 1f, 0.4f)),
        body = """
        vec4 process(vec2 uv) {
            float s = step(0.99, hash11(floor(uv.x * uResolution.x * 0.3) + floor(uTime * 8.0) * 71.0)) * p0;
            return vec4(tex(uv) + s * 0.6, 1.0);
        }"""),

    Effect("dust_tex", "Dust", C, animated = true,
        params = listOf(EffectParam("Amount", 0f, 1f, 0.4f)),
        body = """
        vec4 process(vec2 uv) {
            vec3 c = tex(uv);
            float t = floor(uTime * 10.0);
            float spk = smoothstep(0.992, 1.0, hash21(floor(uv * uResolution / 2.0) + t)) * p0;
            float drk = smoothstep(0.992, 1.0, hash21(floor(uv * uResolution / 2.0) + 50.0 + t)) * p0;
            return vec4(c + spk - drk * 0.5, 1.0);
        }"""),

    Effect("fabric", "Fabric", C,
        params = listOf(EffectParam("Amount", 0f, 1f, 0.4f)),
        body = """
        vec4 process(vec2 uv) {
            vec2 P = uv * uResolution;
            float w = sin(P.x * 1.5) * sin(P.y * 1.5);
            return vec4(tex(uv) * mix(1.0, 0.85 + 0.3 * (0.5 + 0.5 * w), p0), 1.0);
        }"""),

    Effect("concrete", "Concrete", C,
        params = listOf(EffectParam("Amount", 0f, 1f, 0.4f)),
        body = """
        vec4 process(vec2 uv) {
            vec3 c = tex(uv);
            return vec4(mix(c, c * (0.6 + 0.8 * fbm(uv * 40.0)), p0), 1.0);
        }"""),

    Effect("noise_overlay", "Noise Overlay", C, animated = true,
        params = listOf(
            EffectParam("Amount", 0f, 1f, 0.4f),
            EffectParam("Scale", 1f, 20f, 6f)
        ),
        body = """
        vec4 process(vec2 uv) {
            vec3 c = tex(uv);
            float n = fbm(uv * p1 + uTime * 0.5);
            return vec4(mix(c, c * (0.5 + n), p0), 1.0);
        }""")
)
