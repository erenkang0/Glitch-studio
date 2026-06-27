package com.glitchstudio.app.effects

private val C = Categories.RETRO

val retroEffects: List<Effect> = listOf(

    Effect(
        id = "pixelate", name = "Pixelate", category = C,
        params = listOf(EffectParam("Pixel Size", 2f, 100f, 16f)),
        body = """
        vec4 process(vec2 uv) {
            vec2 d = vec2(p0) / uResolution;
            uv = (floor(uv / d) + 0.5) * d;
            return vec4(tex(uv), 1.0);
        }"""
    ),

    Effect(
        id = "hex_pixelate", name = "Hex Pixelate", category = C,
        params = listOf(EffectParam("Size", 4f, 80f, 26f)),
        body = """
        vec4 process(vec2 uv) {
            vec2 p = uv * uResolution / p0;
            vec2 r = vec2(1.0, 1.7320508);
            vec2 h = r * 0.5;
            vec2 a = mod(p, r) - h;
            vec2 b = mod(p - h, r) - h;
            vec2 gv = dot(a, a) < dot(b, b) ? a : b;
            vec2 center = p - gv;
            return vec4(tex(center * p0 / uResolution), 1.0);
        }"""
    ),

    Effect(
        id = "crt", name = "CRT Screen", category = C,
        params = listOf(
            EffectParam("Curvature", 0f, 1f, 0.35f),
            EffectParam("Scanline", 0f, 1f, 0.5f),
            EffectParam("Vignette", 0f, 1f, 0.4f),
            EffectParam("Brightness", 0.5f, 2f, 1.25f)
        ),
        body = """
        vec4 process(vec2 uv) {
            vec2 cc = uv * 2.0 - 1.0;
            cc *= 1.0 + p0 * 0.3 * vec2(cc.y * cc.y, cc.x * cc.x);
            vec2 suv = cc * 0.5 + 0.5;
            if (suv.x < 0.0 || suv.x > 1.0 || suv.y < 0.0 || suv.y > 1.0)
                return vec4(0.0, 0.0, 0.0, 1.0);
            vec3 c = tex(suv) * p3;
            float sl = 0.5 + 0.5 * sin(suv.y * uResolution.y * PI);
            c *= 1.0 - p1 * 0.4 * (1.0 - sl);
            c *= 1.0 - p2 * dot(cc, cc) * 0.5;
            float m = mod(floor(suv.x * uResolution.x), 3.0);
            vec3 mask = vec3(0.75);
            if (m < 1.0) mask.r = 1.0;
            else if (m < 2.0) mask.g = 1.0;
            else mask.b = 1.0;
            c *= mask;
            return vec4(c, 1.0);
        }"""
    ),

    Effect(
        id = "halftone", name = "Halftone", category = C,
        params = listOf(
            EffectParam("Scale", 2f, 30f, 8f),
            EffectParam("Angle", 0f, 1.57f, 0.4f)
        ),
        body = """
        vec4 process(vec2 uv) {
            vec3 col = tex(uv);
            float l = luma(col);
            vec2 p = centered(uv) * (uResolution.y / p0);
            p = rot(p1) * p;
            vec2 g = fract(p) - 0.5;
            float d = length(g);
            float r = (1.0 - l) * 0.65;
            float ink = smoothstep(r + 0.04, r - 0.04, d);
            return vec4(mix(vec3(1.0), col, ink), 1.0);
        }"""
    ),

    Effect(
        id = "dot_screen", name = "Dot Screen", category = C,
        params = listOf(EffectParam("Scale", 2f, 30f, 8f)),
        body = """
        vec4 process(vec2 uv) {
            float l = luma(tex(uv));
            vec2 p = centered(uv) * (uResolution.y / p0);
            vec2 g = fract(p) - 0.5;
            float d = length(g);
            float r = (1.0 - l) * 0.7;
            float ink = smoothstep(r + 0.04, r - 0.04, d);
            return vec4(vec3(1.0 - ink), 1.0);
        }"""
    ),

    Effect(
        id = "ascii_blocks", name = "ASCII Blocks", category = C,
        params = listOf(
            EffectParam("Size", 4f, 30f, 10f),
            EffectParam("Contrast", 0f, 2f, 1f)
        ),
        body = """
        vec4 process(vec2 uv) {
            vec2 cell = p0 / uResolution;
            vec2 cuv = (floor(uv / cell) + 0.5) * cell;
            vec3 col = tex(cuv);
            float l = clamp((luma(col) - 0.5) * p1 + 0.5, 0.0, 1.0);
            vec2 f = fract(uv / cell);
            vec2 g = floor(f * 3.0);
            float idx = g.x + g.y * 3.0;
            float on = step(idx, l * 9.0);
            return vec4(col * on, 1.0);
        }"""
    ),

    Effect(
        id = "gameboy", name = "Game Boy", category = C,
        params = listOf(EffectParam("Pixel Size", 2f, 16f, 4f)),
        body = """
        vec4 process(vec2 uv) {
            vec2 cell = p0 / uResolution;
            vec2 cuv = (floor(uv / cell) + 0.5) * cell;
            float l = luma(tex(cuv));
            float dth = hash21(floor(uv * uResolution)) * 0.1 - 0.05;
            l = clamp(l + dth, 0.0, 1.0);
            float q = clamp(floor(l * 4.0) / 3.0, 0.0, 1.0);
            vec3 dark = vec3(0.06, 0.22, 0.06);
            vec3 light = vec3(0.61, 0.74, 0.06);
            return vec4(mix(dark, light, q), 1.0);
        }"""
    ),

    Effect(
        id = "eight_bit", name = "8-Bit", category = C,
        params = listOf(
            EffectParam("Pixel Size", 1f, 12f, 4f),
            EffectParam("Colors", 2f, 6f, 3f)
        ),
        body = """
        vec4 process(vec2 uv) {
            vec2 cell = p0 / uResolution;
            vec2 cuv = (floor(uv / cell) + 0.5) * cell;
            vec3 c = tex(cuv);
            float n = p1;
            c = floor(c * n) / max(n - 1.0, 1.0);
            return vec4(clamp(c, 0.0, 1.0), 1.0);
        }"""
    ),

    Effect(
        id = "bayer_dither", name = "Bayer Dither", category = C,
        params = listOf(
            EffectParam("Scale", 1f, 8f, 2f),
            EffectParam("Levels", 2f, 6f, 2f)
        ),
        body = """
        float Bayer2(vec2 a) { a = floor(a); return fract(a.x * 0.5 + a.y * a.y * 0.75); }
        float Bayer4(vec2 a) { return Bayer2(0.5 * a) * 0.25 + Bayer2(a); }
        vec4 process(vec2 uv) {
            vec2 px = uv * uResolution / p0;
            float th = Bayer4(px) - 0.5;
            float l = luma(tex(uv));
            float n = p1;
            float v = clamp(floor(l * n + th + 0.5) / max(n - 1.0, 1.0), 0.0, 1.0);
            return vec4(vec3(v), 1.0);
        }"""
    ),

    Effect(
        id = "ordered_dither", name = "Ordered Dither", category = C,
        params = listOf(
            EffectParam("Scale", 1f, 8f, 2f),
            EffectParam("Levels", 2f, 6f, 3f)
        ),
        body = """
        float Bayer2(vec2 a) { a = floor(a); return fract(a.x * 0.5 + a.y * a.y * 0.75); }
        float Bayer4(vec2 a) { return Bayer2(0.5 * a) * 0.25 + Bayer2(a); }
        vec4 process(vec2 uv) {
            vec2 px = uv * uResolution / p0;
            float th = Bayer4(px) - 0.5;
            vec3 c = tex(uv);
            float n = p1;
            vec3 d = floor(c * n + th + 0.5) / max(n - 1.0, 1.0);
            return vec4(clamp(d, 0.0, 1.0), 1.0);
        }"""
    ),

    Effect(
        id = "posterize", name = "Posterize", category = C,
        params = listOf(EffectParam("Levels", 2f, 12f, 5f)),
        body = """
        vec4 process(vec2 uv) {
            float n = p0;
            vec3 c = floor(tex(uv) * n) / max(n - 1.0, 1.0);
            return vec4(clamp(c, 0.0, 1.0), 1.0);
        }"""
    ),

    Effect(
        id = "crosshatch", name = "Crosshatch", category = C,
        params = listOf(EffectParam("Spacing", 4f, 20f, 8f)),
        body = """
        vec4 process(vec2 uv) {
            float l = luma(tex(uv));
            vec2 P = uv * uResolution;
            vec3 col = vec3(1.0);
            float sp = p0;
            if (l < 0.85 && mod(P.x + P.y, sp) <= 1.0) col = vec3(0.0);
            if (l < 0.65 && mod(P.x - P.y, sp) <= 1.0) col = vec3(0.0);
            if (l < 0.45 && mod(P.x + P.y, sp * 0.5) <= 1.0) col = vec3(0.0);
            if (l < 0.25 && mod(P.x - P.y, sp * 0.5) <= 1.0) col = vec3(0.0);
            return vec4(col, 1.0);
        }"""
    ),

    Effect(
        id = "mosaic_tiles", name = "Mosaic Tiles", category = C,
        params = listOf(
            EffectParam("Tiles", 8f, 80f, 30f),
            EffectParam("Gap", 0f, 0.3f, 0.08f)
        ),
        body = """
        vec4 process(vec2 uv) {
            float n = p0;
            vec2 cuv = floor(uv * n) / n + 0.5 / n;
            vec2 f = fract(uv * n);
            vec3 c = tex(cuv);
            float g = p1;
            float edge = step(g, f.x) * step(g, f.y) * step(f.x, 1.0 - g) * step(f.y, 1.0 - g);
            return vec4(c * mix(0.3, 1.0, edge), 1.0);
        }"""
    )
)
