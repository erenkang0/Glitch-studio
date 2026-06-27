package com.glitchstudio.app.effects

private val C = Categories.STYLIZE

val stylizeEffects: List<Effect> = listOf(

    Effect(
        id = "gaussian_blur", name = "Gaussian Blur", category = C,
        params = listOf(EffectParam("Radius", 0f, 4f, 1.5f)),
        body = """
        vec4 process(vec2 uv) {
            vec2 px = (1.0 / uResolution) * p0;
            vec3 sum = vec3(0.0);
            float wsum = 0.0;
            for (int x = -3; x <= 3; x++) {
                for (int y = -3; y <= 3; y++) {
                    float w = exp(-float(x * x + y * y) / 8.0);
                    sum += tex(uv + vec2(float(x), float(y)) * px) * w;
                    wsum += w;
                }
            }
            return vec4(sum / wsum, 1.0);
        }"""
    ),

    Effect(
        id = "sharpen", name = "Sharpen", category = C,
        params = listOf(EffectParam("Amount", 0f, 3f, 1f)),
        body = """
        vec4 process(vec2 uv) {
            vec2 px = 1.0 / uResolution;
            vec3 c = tex(uv);
            vec3 blur = (tex(uv + vec2(px.x, 0.0)) + tex(uv - vec2(px.x, 0.0)) +
                         tex(uv + vec2(0.0, px.y)) + tex(uv - vec2(0.0, px.y))) * 0.25;
            return vec4(clamp(c + (c - blur) * p0 * 2.0, 0.0, 1.0), 1.0);
        }"""
    ),

    Effect(
        id = "edge_sobel", name = "Edge Detect", category = C,
        params = listOf(EffectParam("Intensity", 0f, 4f, 1.5f)),
        body = """
        vec4 process(vec2 uv) {
            vec2 px = 1.0 / uResolution;
            float s00 = luma(tex(uv + px * vec2(-1.0, -1.0)));
            float s01 = luma(tex(uv + px * vec2( 0.0, -1.0)));
            float s02 = luma(tex(uv + px * vec2( 1.0, -1.0)));
            float s10 = luma(tex(uv + px * vec2(-1.0,  0.0)));
            float s12 = luma(tex(uv + px * vec2( 1.0,  0.0)));
            float s20 = luma(tex(uv + px * vec2(-1.0,  1.0)));
            float s21 = luma(tex(uv + px * vec2( 0.0,  1.0)));
            float s22 = luma(tex(uv + px * vec2( 1.0,  1.0)));
            float gx = (s02 + 2.0 * s12 + s22) - (s00 + 2.0 * s10 + s20);
            float gy = (s20 + 2.0 * s21 + s22) - (s00 + 2.0 * s01 + s02);
            float g = length(vec2(gx, gy)) * p0;
            return vec4(vec3(g), 1.0);
        }"""
    ),

    Effect(
        id = "emboss", name = "Emboss", category = C,
        params = listOf(EffectParam("Strength", 0f, 3f, 1f)),
        body = """
        vec4 process(vec2 uv) {
            vec2 px = 1.0 / uResolution;
            vec3 a = tex(uv - px);
            vec3 b = tex(uv + px);
            float g = luma(vec3(0.5) + (a - b) * p0);
            return vec4(vec3(g), 1.0);
        }"""
    ),

    Effect(
        id = "oil_paint", name = "Oil Paint", category = C,
        params = listOf(EffectParam("Radius", 1f, 5f, 3f)),
        body = """
        vec4 process(vec2 uv) {
            vec2 px = (1.0 / uResolution) * p0;
            vec3 m0 = vec3(0.0), m1 = vec3(0.0), m2 = vec3(0.0), m3 = vec3(0.0);
            vec3 s0 = vec3(0.0), s1 = vec3(0.0), s2 = vec3(0.0), s3 = vec3(0.0);
            for (int j = -3; j <= 0; j++) for (int i = -3; i <= 0; i++) {
                vec3 c = tex(uv + vec2(float(i), float(j)) * px); m0 += c; s0 += c * c; }
            for (int j = -3; j <= 0; j++) for (int i = 0; i <= 3; i++) {
                vec3 c = tex(uv + vec2(float(i), float(j)) * px); m1 += c; s1 += c * c; }
            for (int j = 0; j <= 3; j++) for (int i = -3; i <= 0; i++) {
                vec3 c = tex(uv + vec2(float(i), float(j)) * px); m2 += c; s2 += c * c; }
            for (int j = 0; j <= 3; j++) for (int i = 0; i <= 3; i++) {
                vec3 c = tex(uv + vec2(float(i), float(j)) * px); m3 += c; s3 += c * c; }
            float n = 16.0;
            m0 /= n; m1 /= n; m2 /= n; m3 /= n;
            float v0 = dot(s0 / n - m0 * m0, vec3(1.0));
            float v1 = dot(s1 / n - m1 * m1, vec3(1.0));
            float v2 = dot(s2 / n - m2 * m2, vec3(1.0));
            float v3 = dot(s3 / n - m3 * m3, vec3(1.0));
            vec3 res = m0; float mv = v0;
            if (v1 < mv) { mv = v1; res = m1; }
            if (v2 < mv) { mv = v2; res = m2; }
            if (v3 < mv) { mv = v3; res = m3; }
            return vec4(res, 1.0);
        }"""
    ),

    Effect(
        id = "toon", name = "Toon", category = C,
        params = listOf(
            EffectParam("Levels", 2f, 8f, 4f),
            EffectParam("Edge", 0f, 1f, 0.5f)
        ),
        body = """
        vec4 process(vec2 uv) {
            vec3 c = tex(uv);
            float n = p0;
            vec3 q = clamp(floor(c * n) / max(n - 1.0, 1.0), 0.0, 1.0);
            vec2 px = 1.0 / uResolution;
            float e = length(vec2(
                luma(tex(uv + vec2(px.x, 0.0))) - luma(tex(uv - vec2(px.x, 0.0))),
                luma(tex(uv + vec2(0.0, px.y))) - luma(tex(uv - vec2(0.0, px.y)))));
            float edge = 1.0 - smoothstep(0.0, 0.2, e) * p1;
            return vec4(q * edge, 1.0);
        }"""
    ),

    Effect(
        id = "pencil_sketch", name = "Pencil Sketch", category = C,
        params = listOf(EffectParam("Strength", 0f, 2f, 1f)),
        body = """
        vec4 process(vec2 uv) {
            vec2 px = 1.0 / uResolution;
            float l = luma(tex(uv));
            float gx = luma(tex(uv + vec2(px.x, 0.0))) - luma(tex(uv - vec2(px.x, 0.0)));
            float gy = luma(tex(uv + vec2(0.0, px.y))) - luma(tex(uv - vec2(0.0, px.y)));
            float g = length(vec2(gx, gy)) * p0 * 4.0;
            float sketch = (1.0 - g) * mix(1.0, l * 1.2 + 0.2, 0.3);
            return vec4(vec3(clamp(sketch, 0.0, 1.0)), 1.0);
        }"""
    ),

    Effect(
        id = "comic", name = "Comic", category = C,
        params = listOf(
            EffectParam("Edge", 0f, 1f, 0.6f),
            EffectParam("Posterize", 2f, 8f, 4f)
        ),
        body = """
        vec4 process(vec2 uv) {
            vec3 c = tex(uv);
            float n = p1;
            vec3 q = clamp(floor(c * n) / max(n - 1.0, 1.0), 0.0, 1.0);
            vec2 px = 1.0 / uResolution;
            float gx = luma(tex(uv + vec2(px.x, 0.0))) - luma(tex(uv - vec2(px.x, 0.0)));
            float gy = luma(tex(uv + vec2(0.0, px.y))) - luma(tex(uv - vec2(0.0, px.y)));
            float e = step(0.15, length(vec2(gx, gy))) * p0;
            return vec4(mix(q, vec3(0.0), e), 1.0);
        }"""
    ),

    Effect(
        id = "bloom", name = "Bloom", category = C,
        params = listOf(
            EffectParam("Threshold", 0f, 1f, 0.6f),
            EffectParam("Intensity", 0f, 2f, 0.8f)
        ),
        body = """
        vec4 process(vec2 uv) {
            vec3 c = tex(uv);
            vec2 px = 1.0 / uResolution;
            vec3 b = vec3(0.0); float w = 0.0;
            for (int x = -3; x <= 3; x++) for (int y = -3; y <= 3; y++) {
                vec3 s = max(tex(uv + vec2(float(x), float(y)) * px * 2.0) - p0, 0.0);
                float ww = exp(-float(x * x + y * y) / 8.0);
                b += s * ww; w += ww;
            }
            return vec4(c + (b / w) * p1, 1.0);
        }"""
    ),

    Effect(
        id = "neon_edges", name = "Neon Edges", category = C,
        params = listOf(
            EffectParam("Intensity", 0f, 3f, 1.5f),
            EffectParam("Hue", 0f, 1f, 0.5f)
        ),
        body = """
        vec4 process(vec2 uv) {
            vec2 px = 1.0 / uResolution;
            float gx = luma(tex(uv + vec2(px.x, 0.0))) - luma(tex(uv - vec2(px.x, 0.0)));
            float gy = luma(tex(uv + vec2(0.0, px.y))) - luma(tex(uv - vec2(0.0, px.y)));
            float e = length(vec2(gx, gy)) * p0;
            vec3 col = hsv2rgb(vec3(p1 + e * 0.2, 1.0, 1.0));
            return vec4(col * e, 1.0);
        }"""
    )
)
