package com.glitchstudio.app.effects

private val C = Categories.LIGHT

val lightEffects: List<Effect> = listOf(

    Effect(
        id = "vignette", name = "Vignette", category = C,
        params = listOf(
            EffectParam("Amount", 0f, 1f, 0.6f),
            EffectParam("Softness", 0.1f, 1f, 0.5f),
            EffectParam("Roundness", 0f, 1f, 1f)
        ),
        body = """
        vec4 process(vec2 uv) {
            vec2 p = uv - 0.5;
            p.x *= mix(1.0, uAspect, p2);
            float d = length(p);
            float v = smoothstep(0.8, 0.8 - p1, d);
            return vec4(tex(uv) * mix(1.0, v, p0), 1.0);
        }"""
    ),

    Effect(
        id = "film_grain", name = "Film Grain", category = C, animated = true,
        params = listOf(
            EffectParam("Amount", 0f, 1f, 0.4f),
            EffectParam("Size", 0.5f, 4f, 1.5f)
        ),
        body = """
        vec4 process(vec2 uv) {
            vec3 c = tex(uv);
            float n = hash21(floor(uv * uResolution / p1) + floor(uTime * 24.0));
            c += (n - 0.5) * p0 * 0.5;
            return vec4(c, 1.0);
        }"""
    ),

    Effect(
        id = "light_leak", name = "Light Leak", category = C, animated = true,
        params = listOf(
            EffectParam("Intensity", 0f, 1f, 0.6f),
            EffectParam("Position", 0f, 1f, 0.2f),
            EffectParam("Hue", 0f, 1f, 0.05f)
        ),
        body = """
        vec4 process(vec2 uv) {
            vec3 c = tex(uv);
            float t = uTime * 0.3;
            vec2 lp = vec2(p1 + sin(t) * 0.1, 0.5 + cos(t * 0.7) * 0.2);
            float leak = smoothstep(0.7, 0.0, distance(uv, lp)) * p0;
            vec3 lc = hsv2rgb(vec3(p2, 0.8, 1.0));
            return vec4(c + lc * leak, 1.0);
        }"""
    ),

    Effect(
        id = "lens_flare", name = "Lens Flare", category = C, animated = true,
        params = listOf(
            EffectParam("Intensity", 0f, 1f, 0.6f),
            EffectParam("Pos X", 0f, 1f, 0.7f),
            EffectParam("Pos Y", 0f, 1f, 0.3f)
        ),
        body = """
        vec4 process(vec2 uv) {
            vec3 c = tex(uv);
            vec2 sp = vec2(p1, p2);
            float core = smoothstep(0.3, 0.0, length(uv - sp));
            float ghost = 0.0;
            for (int i = 1; i <= 5; i++) {
                vec2 gp = mix(sp, vec2(0.5), float(i) * 0.18);
                ghost += smoothstep(0.08, 0.0, distance(uv, gp)) * 0.4;
            }
            float halo = smoothstep(0.02, 0.0, abs(length(uv - 0.5) - 0.3)) * 0.3;
            vec3 add = vec3(1.0, 0.9, 0.7) * (core + ghost + halo) * p0;
            return vec4(c + add, 1.0);
        }"""
    ),

    Effect(
        id = "old_film", name = "Old Film", category = C, animated = true,
        params = listOf(
            EffectParam("Grain", 0f, 1f, 0.5f),
            EffectParam("Scratches", 0f, 1f, 0.4f)
        ),
        body = """
        vec4 process(vec2 uv) {
            vec3 c = tex(uv);
            vec3 s = vec3(dot(c, vec3(0.393, 0.769, 0.189)),
                          dot(c, vec3(0.349, 0.686, 0.168)),
                          dot(c, vec3(0.272, 0.534, 0.131)));
            c = mix(c, clamp(s, 0.0, 1.0), 0.6);
            float n = hash21(uv * uResolution + floor(uTime * 20.0));
            c += (n - 0.5) * p0 * 0.4;
            float sc = step(0.985, hash11(floor(uv.x * uResolution.x * 0.5) + floor(uTime * 12.0) * 53.0)) * p1;
            c += sc * 0.5;
            c *= 0.9 + 0.1 * hash11(floor(uTime * 16.0));
            float v = smoothstep(0.9, 0.3, length(uv - 0.5));
            c *= mix(1.0, v, 0.6);
            return vec4(c, 1.0);
        }"""
    ),

    Effect(
        id = "dreamy_glow", name = "Dreamy Glow", category = C,
        params = listOf(EffectParam("Amount", 0f, 1f, 0.6f)),
        body = """
        vec4 process(vec2 uv) {
            vec3 c = tex(uv);
            vec2 px = 1.0 / uResolution;
            vec3 b = vec3(0.0); float w = 0.0;
            for (int x = -3; x <= 3; x++) for (int y = -3; y <= 3; y++) {
                vec3 s = tex(uv + vec2(float(x), float(y)) * px * 3.0);
                b += max(s, c); w += 1.0;
            }
            return vec4(mix(c, b / w, p0), 1.0);
        }"""
    )
)
