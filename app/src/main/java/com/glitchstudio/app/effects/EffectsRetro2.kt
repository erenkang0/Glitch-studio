package com.glitchstudio.app.effects

private val C = Categories.RETRO

val retroEffects2: List<Effect> = listOf(

    Effect("vhs_tracking", "VHS Tracking", C, animated = true,
        params = listOf(
            EffectParam("Amount", 0f, 0.03f, 0.008f),
            EffectParam("Speed", 0f, 8f, 3f)
        ),
        body = """
        vec4 process(vec2 uv) {
            float t = uTime * p1;
            uv.x += (hash21(vec2(floor(uv.y * 200.0), floor(t * 20.0))) - 0.5) * p0;
            vec3 c;
            c.r = tex(uv + vec2(0.004, 0.0)).r;
            c.g = tex(uv).g;
            c.b = tex(uv - vec2(0.004, 0.0)).b;
            float tr = smoothstep(0.0, 0.02, fract(uv.y * 3.0 - t * 0.2));
            c *= 0.8 + 0.2 * tr;
            c *= 0.92 + 0.08 * sin(uv.y * uResolution.y * 2.0);
            return vec4(c, 1.0);
        }"""),

    Effect("cmyk_print", "CMYK Print", C,
        params = listOf(EffectParam("Scale", 2f, 20f, 6f)),
        body = """
        float dotsAt(vec2 uv, float ang, float scale, float v) {
            vec2 p = rot(ang) * (centered(uv) * (uResolution.y / scale));
            vec2 g = fract(p) - 0.5;
            return smoothstep((1.0 - v) * 0.7 + 0.04, (1.0 - v) * 0.7 - 0.04, length(g));
        }
        vec4 process(vec2 uv) {
            vec3 c = tex(uv);
            float k = 1.0 - max(max(c.r, c.g), c.b);
            vec3 cmy = (1.0 - c - k) / max(1.0 - k, 0.001);
            float cy = dotsAt(uv, 0.26, p0, cmy.x);
            float mg = dotsAt(uv, 1.31, p0, cmy.y);
            float ye = dotsAt(uv, 0.0, p0, cmy.z);
            float kk = dotsAt(uv, 0.78, p0, k);
            vec3 res = vec3(1.0);
            res.r -= cy; res.g -= mg; res.b -= ye;
            res -= vec3(kk);
            return vec4(clamp(res, 0.0, 1.0), 1.0);
        }"""),

    Effect("lcd_grid", "LCD Grid", C,
        params = listOf(
            EffectParam("Scale", 2f, 16f, 6f),
            EffectParam("Gap", 0f, 0.3f, 0.1f)
        ),
        body = """
        vec4 process(vec2 uv) {
            vec2 cell = p0 / uResolution;
            vec3 c = tex((floor(uv / cell) + 0.5) * cell);
            vec2 f = fract(uv / cell);
            float col = floor(f.x * 3.0);
            vec3 mask = vec3(0.0);
            if (col < 1.0) mask.r = 1.0; else if (col < 2.0) mask.g = 1.0; else mask.b = 1.0;
            float gap = step(p1, f.y) * step(f.y, 1.0 - p1);
            return vec4(c * mask * gap * 1.6, 1.0);
        }"""),

    Effect("teletext", "Teletext", C,
        params = listOf(EffectParam("Size", 4f, 24f, 10f)),
        body = """
        vec4 process(vec2 uv) {
            vec2 cell = p0 / uResolution;
            vec3 c = tex((floor(uv / cell) + 0.5) * cell);
            return vec4(step(0.5, c), 1.0);
        }"""),

    Effect("dot_matrix", "Dot Matrix", C,
        params = listOf(EffectParam("Size", 4f, 24f, 8f)),
        body = """
        vec4 process(vec2 uv) {
            vec2 cell = p0 / uResolution;
            vec3 c = tex((floor(uv / cell) + 0.5) * cell);
            vec2 f = fract(uv / cell) - 0.5;
            float d = smoothstep(0.45, 0.4, length(f));
            return vec4(c * d, 1.0);
        }"""),

    Effect("newsprint", "Newsprint", C,
        params = listOf(EffectParam("Scale", 2f, 16f, 6f)),
        body = """
        vec4 process(vec2 uv) {
            float l = luma(tex(uv));
            vec2 g = fract(centered(uv) * (uResolution.y / p0)) - 0.5;
            float ink = smoothstep((1.0 - l) * 0.7 + 0.04, (1.0 - l) * 0.7 - 0.04, length(g));
            float paper = 0.92 + 0.08 * hash21(floor(uv * uResolution * 0.5));
            return vec4(vec3(paper * (1.0 - ink)), 1.0);
        }"""),

    Effect("risograph", "Risograph", C,
        params = listOf(
            EffectParam("Ink A", 0f, 1f, 0.0f),
            EffectParam("Ink B", 0f, 1f, 0.55f)
        ),
        body = """
        vec4 process(vec2 uv) {
            float l = luma(tex(uv));
            vec3 a = hsv2rgb(vec3(p0, 0.8, 1.0));
            vec3 b = hsv2rgb(vec3(p1, 0.8, 1.0));
            float d = hash21(floor(uv * uResolution / 2.0)) * 0.15 - 0.075;
            return vec4(mix(a, b, step(0.5, l + d)), 1.0);
        }"""),

    Effect("polaroid", "Polaroid", C,
        params = listOf(EffectParam("Warmth", 0f, 1f, 0.6f)),
        body = """
        vec4 process(vec2 uv) {
            vec3 c = tex(uv);
            c = mix(c, c * vec3(1.1, 1.0, 0.85) + 0.05, p0);
            c = pow(clamp(c, 0.0, 1.0), vec3(0.95));
            float v = smoothstep(0.95, 0.5, length(centered(uv)));
            c *= mix(0.85, 1.0, v);
            float b = step(0.46, abs(uv.x - 0.5)) + step(0.46, abs(uv.y - 0.5));
            return vec4(mix(c, vec3(0.96), clamp(b, 0.0, 1.0)), 1.0);
        }"""),

    Effect("c64", "C64", C,
        params = listOf(EffectParam("Pixel", 2f, 12f, 4f)),
        body = """
        vec4 process(vec2 uv) {
            vec2 cell = p0 / uResolution;
            vec3 c = tex((floor(uv / cell) + 0.5) * cell);
            return vec4(clamp(floor(c * 4.0) / 3.0, 0.0, 1.0), 1.0);
        }"""),

    Effect("cassette", "Cassette", C, animated = true,
        params = listOf(
            EffectParam("Wow", 0f, 0.02f, 0.006f),
            EffectParam("Noise", 0f, 1f, 0.3f)
        ),
        body = """
        vec4 process(vec2 uv) {
            float t = uTime;
            uv.x += sin(uv.y * 10.0 + t * 3.0) * p0;
            vec3 c = tex(uv);
            c = mix(c, vec3(luma(c)), 0.2) * vec3(1.05, 1.0, 0.92);
            c += (hash21(uv * uResolution + floor(t * 20.0)) - 0.5) * p1 * 0.3;
            return vec4(c, 1.0);
        }""")
)
