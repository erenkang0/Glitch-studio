package com.glitchstudio.app.effects

private val C = Categories.COLOR

val colorEffects2: List<Effect> = listOf(

    Effect("cinematic", "Cinematic", C,
        params = listOf(EffectParam("Strength", 0f, 1f, 0.6f)),
        body = """
        vec4 process(vec2 uv) {
            vec3 c = tex(uv);
            float l = luma(c);
            vec3 grade = mix(vec3(0.0, 0.25, 0.35), vec3(1.05, 0.7, 0.35), smoothstep(0.0, 1.0, l));
            c = mix(c, c * grade, p0);
            c = (c - 0.5) * 1.1 + 0.5;
            return vec4(clamp(c, 0.0, 1.0), 1.0);
        }"""),

    Effect("split_tone", "Split Tone", C,
        params = listOf(
            EffectParam("Shadow Hue", 0f, 1f, 0.6f),
            EffectParam("Highlight Hue", 0f, 1f, 0.1f),
            EffectParam("Balance", 0f, 1f, 0.5f)
        ),
        body = """
        vec4 process(vec2 uv) {
            vec3 c = tex(uv);
            float l = luma(c);
            vec3 tint = mix(hsv2rgb(vec3(p0, 0.5, 0.5)), hsv2rgb(vec3(p1, 0.5, 1.0)), l);
            return vec4(clamp(mix(c, c * tint * 1.5, p2), 0.0, 1.0), 1.0);
        }"""),

    Effect("vibrance", "Vibrance", C,
        params = listOf(EffectParam("Amount", 0f, 1f, 0.5f)),
        body = """
        vec4 process(vec2 uv) {
            vec3 c = tex(uv);
            float mx = max(max(c.r, c.g), c.b);
            float avg = (c.r + c.g + c.b) / 3.0;
            float amt = (mx - avg) * p0 * 2.0;
            return vec4(clamp(mix(vec3(luma(c)), c, 1.0 + amt), 0.0, 1.0), 1.0);
        }"""),

    Effect("orton", "Orton", C,
        params = listOf(EffectParam("Amount", 0f, 1f, 0.6f)),
        body = """
        vec4 process(vec2 uv) {
            vec3 c = tex(uv);
            vec2 px = 1.0 / uResolution;
            vec3 b = vec3(0.0); float w = 0.0;
            for (int x = -3; x <= 3; x++) for (int y = -3; y <= 3; y++) {
                b += tex(uv + vec2(float(x), float(y)) * px * 3.0); w += 1.0;
            }
            b /= w;
            vec3 res = mix(c, 1.0 - (1.0 - c) * (1.0 - b), p0);
            res = (res - 0.5) * 1.1 + 0.5;
            return vec4(clamp(res, 0.0, 1.0), 1.0);
        }"""),

    Effect("faded_film", "Faded Film", C,
        params = listOf(EffectParam("Amount", 0f, 1f, 0.8f)),
        body = """
        vec4 process(vec2 uv) {
            vec3 c = tex(uv);
            c = mix(c, vec3(luma(c)), 0.2);
            c = c * 0.85 + 0.1;
            c.b += 0.03;
            return vec4(clamp(mix(tex(uv), c, p0), 0.0, 1.0), 1.0);
        }"""),

    Effect("cyberpunk", "Cyberpunk", C,
        params = listOf(EffectParam("Strength", 0f, 1f, 0.6f)),
        body = """
        vec4 process(vec2 uv) {
            vec3 c = tex(uv);
            float l = luma(c);
            vec3 grade = mix(vec3(0.1, 0.0, 0.2), vec3(0.0, 0.9, 1.0), l);
            grade = mix(grade, vec3(1.0, 0.1, 0.7), smoothstep(0.5, 1.0, l));
            return vec4(clamp(mix(c, grade, p0), 0.0, 1.0), 1.0);
        }"""),

    Effect("vaporwave", "Vaporwave", C,
        params = listOf(EffectParam("Strength", 0f, 1f, 0.6f)),
        body = """
        vec4 process(vec2 uv) {
            vec3 c = tex(uv);
            vec3 grade = mix(vec3(0.3, 0.1, 0.5), vec3(1.0, 0.5, 0.9), luma(c));
            return vec4(clamp(mix(c, grade, p0), 0.0, 1.0), 1.0);
        }"""),

    Effect("matrix_green", "Matrix", C,
        params = listOf(EffectParam("Amount", 0f, 1f, 1f)),
        body = """
        vec4 process(vec2 uv) {
            float l = luma(tex(uv));
            vec3 g = vec3(0.0, l * 1.2, 0.0) * 0.9 + vec3(0.0, 0.05, 0.0);
            return vec4(mix(tex(uv), g, p0), 1.0);
        }"""),

    Effect("blueprint", "Blueprint", C,
        params = listOf(EffectParam("Amount", 0f, 1f, 1f)),
        body = """
        vec4 process(vec2 uv) {
            vec2 px = 1.0 / uResolution;
            float gx = luma(tex(uv + vec2(px.x, 0.0))) - luma(tex(uv - vec2(px.x, 0.0)));
            float gy = luma(tex(uv + vec2(0.0, px.y))) - luma(tex(uv - vec2(0.0, px.y)));
            vec3 bp = vec3(0.05, 0.15, 0.4) + vec3(length(vec2(gx, gy)) * 3.0);
            return vec4(mix(tex(uv), clamp(bp, 0.0, 1.0), p0), 1.0);
        }"""),

    Effect("color_pop", "Color Pop", C,
        params = listOf(
            EffectParam("Hue", 0f, 1f, 0.0f),
            EffectParam("Range", 0.02f, 0.5f, 0.12f)
        ),
        body = """
        vec4 process(vec2 uv) {
            vec3 c = tex(uv);
            vec3 h = rgb2hsv(c);
            float dist = abs(h.x - p0);
            dist = min(dist, 1.0 - dist);
            float keep = 1.0 - smoothstep(p1 * 0.5, p1, dist);
            return vec4(mix(vec3(luma(c)), c, keep), 1.0);
        }"""),

    Effect("lomo", "Lomo", C,
        params = listOf(EffectParam("Amount", 0f, 1f, 0.8f)),
        body = """
        vec4 process(vec2 uv) {
            vec3 c = tex(uv);
            c = mix(vec3(luma(c)), c, 1.4);
            c = (c - 0.5) * 1.2 + 0.5;
            float v = smoothstep(0.9, 0.3, length(centered(uv)));
            c *= mix(0.6, 1.0, v);
            return vec4(clamp(mix(tex(uv), c, p0), 0.0, 1.0), 1.0);
        }"""),

    Effect("tritone", "Tritone", C,
        params = listOf(
            EffectParam("Hue A", 0f, 1f, 0.6f),
            EffectParam("Hue B", 0f, 1f, 0.1f)
        ),
        body = """
        vec4 process(vec2 uv) {
            float l = luma(tex(uv));
            vec3 a = hsv2rgb(vec3(p0, 0.8, 0.2));
            vec3 b = hsv2rgb(vec3((p0 + p1) * 0.5, 0.7, 0.7));
            vec3 d = hsv2rgb(vec3(p1, 0.6, 1.0));
            vec3 res = l < 0.5 ? mix(a, b, l * 2.0) : mix(b, d, (l - 0.5) * 2.0);
            return vec4(res, 1.0);
        }""")
)
