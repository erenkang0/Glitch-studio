package com.glitchstudio.app.effects

private val C = Categories.STYLIZE

val stylizeEffects2: List<Effect> = listOf(

    Effect("voronoi_crystals", "Voronoi Crystals", C,
        params = listOf(EffectParam("Scale", 4f, 60f, 20f)),
        body = """
        vec4 process(vec2 uv) {
            vec2 g = uv * p0;
            vec2 i = floor(g); vec2 f = fract(g);
            float md = 8.0; vec2 mp = vec2(0.0);
            for (int y = -1; y <= 1; y++) for (int x = -1; x <= 1; x++) {
                vec2 o = vec2(float(x), float(y));
                vec2 jc = o + vec2(hash21(i + o), hash21(i + o + 3.7));
                float d = dot(jc - f, jc - f);
                if (d < md) { md = d; mp = i + jc; }
            }
            return vec4(tex(mp / p0), 1.0);
        }"""),

    Effect("hatching", "Hatching", C,
        params = listOf(EffectParam("Spacing", 4f, 16f, 7f)),
        body = """
        vec4 process(vec2 uv) {
            float l = luma(tex(uv));
            vec2 P = uv * uResolution;
            float c = 1.0;
            if (l < 0.8 && mod(P.x + P.y, p0) < 1.5) c = 0.0;
            if (l < 0.55 && mod(P.x - P.y, p0) < 1.5) c = 0.0;
            if (l < 0.3 && mod(P.x, p0 * 0.6) < 1.5) c = 0.0;
            return vec4(vec3(c), 1.0);
        }"""),

    Effect("posterized_edge", "Posterized Edge", C,
        params = listOf(
            EffectParam("Levels", 2f, 8f, 4f),
            EffectParam("Edge", 0f, 1f, 0.7f)
        ),
        body = """
        vec4 process(vec2 uv) {
            vec3 q = clamp(floor(tex(uv) * p0) / max(p0 - 1.0, 1.0), 0.0, 1.0);
            vec2 px = 1.0 / uResolution;
            float e = length(vec2(
                luma(tex(uv + vec2(px.x, 0.0))) - luma(tex(uv - vec2(px.x, 0.0))),
                luma(tex(uv + vec2(0.0, px.y))) - luma(tex(uv - vec2(0.0, px.y)))));
            return vec4(q * (1.0 - smoothstep(0.1, 0.3, e) * p1), 1.0);
        }"""),

    Effect("color_sketch", "Color Sketch", C,
        params = listOf(EffectParam("Strength", 0f, 2f, 1f)),
        body = """
        vec4 process(vec2 uv) {
            vec2 px = 1.0 / uResolution;
            vec3 c = tex(uv);
            float gx = luma(tex(uv + vec2(px.x, 0.0))) - luma(tex(uv - vec2(px.x, 0.0)));
            float gy = luma(tex(uv + vec2(0.0, px.y))) - luma(tex(uv - vec2(0.0, px.y)));
            float e = length(vec2(gx, gy)) * p0 * 4.0;
            return vec4(mix(c * 1.2, vec3(0.1), clamp(e, 0.0, 1.0)), 1.0);
        }"""),

    Effect("low_poly", "Low Poly", C,
        params = listOf(EffectParam("Scale", 6f, 50f, 20f)),
        body = """
        vec4 process(vec2 uv) {
            vec2 g = uv * p0;
            vec2 i = floor(g); vec2 f = fract(g);
            float tri = step(f.x, f.y);
            vec2 c = (i + vec2(0.33 + tri * 0.34, 0.66 - tri * 0.34)) / p0;
            return vec4(tex(c), 1.0);
        }"""),

    Effect("stippling", "Stippling", C,
        params = listOf(EffectParam("Scale", 2f, 14f, 5f)),
        body = """
        vec4 process(vec2 uv) {
            float l = luma(tex(uv));
            vec2 p = uv * uResolution / p0;
            vec2 g = fract(p) - 0.5;
            vec2 j = (vec2(hash21(floor(p)), hash21(floor(p) + 1.3)) - 0.5) * 0.4;
            float r = (1.0 - l) * 0.5;
            float ink = smoothstep(r + 0.05, r - 0.05, length(g - j));
            return vec4(vec3(1.0 - ink), 1.0);
        }"""),

    Effect("woodcut", "Woodcut", C,
        params = listOf(EffectParam("Threshold", 0f, 1f, 0.5f)),
        body = """
        vec4 process(vec2 uv) {
            float l = luma(tex(uv));
            vec2 px = 1.0 / uResolution;
            float gx = luma(tex(uv + vec2(px.x, 0.0))) - luma(tex(uv - vec2(px.x, 0.0)));
            float gy = luma(tex(uv + vec2(0.0, px.y))) - luma(tex(uv - vec2(0.0, px.y)));
            float c = min(step(p0, l), 1.0 - step(0.15, length(vec2(gx, gy))));
            return vec4(vec3(c), 1.0);
        }"""),

    Effect("watercolor", "Watercolor", C,
        params = listOf(EffectParam("Strength", 0f, 1f, 0.6f)),
        body = """
        vec4 process(vec2 uv) {
            vec2 px = 1.0 / uResolution;
            vec3 c = vec3(0.0); float w = 0.0;
            for (int x = -2; x <= 2; x++) for (int y = -2; y <= 2; y++) {
                c += tex(uv + vec2(float(x), float(y)) * px * 2.0); w += 1.0;
            }
            c = clamp(floor((c / w) * 6.0) / 5.0, 0.0, 1.0);
            float gx = luma(tex(uv + vec2(px.x, 0.0))) - luma(tex(uv - vec2(px.x, 0.0)));
            float gy = luma(tex(uv + vec2(0.0, px.y))) - luma(tex(uv - vec2(0.0, px.y)));
            float paper = 0.96 + 0.04 * vnoise(uv * 200.0);
            return vec4(mix(c * paper, vec3(0.2), smoothstep(0.15, 0.4, length(vec2(gx, gy))) * p0), 1.0);
        }"""),

    Effect("charcoal", "Charcoal", C,
        params = listOf(EffectParam("Strength", 0f, 2f, 1f)),
        body = """
        vec4 process(vec2 uv) {
            vec2 px = 1.0 / uResolution;
            float gx = luma(tex(uv + vec2(px.x, 0.0))) - luma(tex(uv - vec2(px.x, 0.0)));
            float gy = luma(tex(uv + vec2(0.0, px.y))) - luma(tex(uv - vec2(0.0, px.y)));
            float e = length(vec2(gx, gy)) * p0 * 4.0;
            float l = luma(tex(uv));
            float paper = hash21(floor(uv * uResolution * 0.7)) * 0.15;
            float v = 1.0 - e - (1.0 - l) * 0.3 - paper;
            return vec4(vec3(clamp(v, 0.0, 1.0)), 1.0);
        }"""),

    Effect("engraving", "Engraving", C,
        params = listOf(EffectParam("Density", 50f, 600f, 200f)),
        body = """
        vec4 process(vec2 uv) {
            float l = luma(tex(uv));
            vec2 P = uv * uResolution;
            float line = sin((P.x + P.y) * (p0 / uResolution.y) * 6.2831);
            return vec4(vec3(step(l, 0.5 + 0.5 * line)), 1.0);
        }""")
)
