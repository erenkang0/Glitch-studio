package com.glitchstudio.app.effects

private val C = Categories.ART

val artEffects: List<Effect> = listOf(

    Effect("mosaic_glass", "Mosaic Glass", C,
        params = listOf(EffectParam("Scale", 4f, 40f, 14f)),
        body = """
        vec4 process(vec2 uv) {
            vec2 g = uv * p0; vec2 i = floor(g); vec2 f = fract(g);
            float d1 = 8.0, d2 = 8.0; vec2 mp = vec2(0.0);
            for (int y = -1; y <= 1; y++) for (int x = -1; x <= 1; x++) {
                vec2 o = vec2(float(x), float(y));
                vec2 jc = o + vec2(hash21(i + o), hash21(i + o + 3.7));
                float d = dot(jc - f, jc - f);
                if (d < d1) { d2 = d1; d1 = d; mp = i + jc; } else if (d < d2) d2 = d;
            }
            float border = smoothstep(0.0, 0.06, sqrt(d2) - sqrt(d1));
            return vec4(tex(mp / p0) * mix(0.2, 1.1, border), 1.0);
        }"""),

    Effect("paper_grain", "Paper Grain", C,
        params = listOf(EffectParam("Amount", 0f, 1f, 0.4f)),
        body = """
        vec4 process(vec2 uv) {
            vec3 c = tex(uv);
            float n = fbm(uv * uResolution.y * 0.3);
            return vec4(c * mix(1.0, 0.85 + 0.3 * n, p0), 1.0);
        }"""),

    Effect("canvas_weave", "Canvas", C,
        params = listOf(EffectParam("Amount", 0f, 1f, 0.4f)),
        body = """
        vec4 process(vec2 uv) {
            vec3 c = tex(uv);
            vec2 P = uv * uResolution;
            float weave = (0.5 + 0.5 * sin(P.x * 0.8)) * (0.5 + 0.5 * sin(P.y * 0.8));
            return vec4(c * mix(1.0, 0.7 + 0.6 * weave, p0), 1.0);
        }"""),

    Effect("cracked", "Cracked", C,
        params = listOf(EffectParam("Scale", 6f, 40f, 18f)),
        body = """
        vec4 process(vec2 uv) {
            vec2 g = uv * p0; vec2 i = floor(g); vec2 f = fract(g);
            float d1 = 8.0, d2 = 8.0;
            for (int y = -1; y <= 1; y++) for (int x = -1; x <= 1; x++) {
                vec2 o = vec2(float(x), float(y));
                vec2 jc = o + vec2(hash21(i + o), hash21(i + o + 3.7)) - f;
                float d = dot(jc, jc);
                if (d < d1) { d2 = d1; d1 = d; } else if (d < d2) d2 = d;
            }
            float crack = smoothstep(0.03, 0.0, sqrt(d2) - sqrt(d1));
            return vec4(tex(uv) * (1.0 - crack * 0.8), 1.0);
        }"""),

    Effect("holographic", "Holographic", C, animated = true,
        params = listOf(
            EffectParam("Intensity", 0f, 1f, 0.5f),
            EffectParam("Speed", 0f, 4f, 1f)
        ),
        body = """
        vec4 process(vec2 uv) {
            vec3 c = tex(uv);
            float l = luma(c);
            vec3 rb = hsv2rgb(vec3(fract(l * 3.0 + uTime * p1 * 0.2 + uv.x), 0.8, 1.0));
            return vec4(mix(c, rb, smoothstep(0.6, 1.0, l) * p0), 1.0);
        }"""),

    Effect("oil_smear", "Oil Smear", C,
        params = listOf(EffectParam("Radius", 1f, 5f, 3f)),
        body = """
        vec4 process(vec2 uv) {
            vec2 px = (1.0 / uResolution) * p0;
            vec3 best = tex(uv); float bestv = 1e9;
            for (int i = 0; i < 8; i++) {
                float a = float(i) / 8.0 * TAU;
                vec2 o = vec2(cos(a), sin(a)) * px * 2.0;
                vec3 m = (tex(uv + o) + tex(uv + o * 0.5) + tex(uv)) / 3.0;
                vec3 d = tex(uv + o) - tex(uv);
                float v = dot(d, d);
                if (v < bestv) { bestv = v; best = m; }
            }
            return vec4(best, 1.0);
        }"""),

    Effect("popart", "Pop Art", C,
        params = listOf(EffectParam("Scale", 2f, 16f, 6f)),
        body = """
        vec4 process(vec2 uv) {
            vec3 c = floor(tex(uv) * 3.0) / 2.0;
            float l = luma(tex(uv));
            vec2 g = fract(centered(uv) * (uResolution.y / p0)) - 0.5;
            float dotp = smoothstep((1.0 - l) * 0.6 + 0.05, (1.0 - l) * 0.6 - 0.05, length(g));
            return vec4(clamp(mix(c, c * 0.5, 1.0 - dotp), 0.0, 1.0), 1.0);
        }"""),

    Effect("stained_glass", "Stained Glass", C,
        params = listOf(EffectParam("Scale", 4f, 30f, 12f)),
        body = """
        vec4 process(vec2 uv) {
            vec2 g = uv * p0; vec2 i = floor(g); vec2 f = fract(g);
            float d1 = 8.0, d2 = 8.0; vec2 mp = vec2(0.0);
            for (int y = -1; y <= 1; y++) for (int x = -1; x <= 1; x++) {
                vec2 o = vec2(float(x), float(y));
                vec2 jc = o + vec2(hash21(i + o), hash21(i + o + 3.7));
                float d = dot(jc - f, jc - f);
                if (d < d1) { d2 = d1; d1 = d; mp = i + jc; } else if (d < d2) d2 = d;
            }
            vec3 h = rgb2hsv(tex(mp / p0));
            h.y = min(h.y * 1.5, 1.0);
            float border = smoothstep(0.0, 0.05, sqrt(d2) - sqrt(d1));
            return vec4(hsv2rgb(h) * border, 1.0);
        }""")
)
