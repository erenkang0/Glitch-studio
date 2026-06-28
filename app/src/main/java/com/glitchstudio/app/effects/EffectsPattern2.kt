package com.glitchstudio.app.effects

private val C = Categories.PATTERN

val patternEffects2: List<Effect> = listOf(

    Effect("truchet", "Truchet", C,
        params = listOf(EffectParam("Scale", 4f, 40f, 12f)),
        body = """
        vec4 process(vec2 uv) {
            vec2 g = uv * p0;
            vec2 i = floor(g); vec2 f = fract(g);
            if (hash21(i) < 0.5) f.x = 1.0 - f.x;
            float d = abs(min(length(f - vec2(0.0, 1.0)), length(f - vec2(1.0, 0.0))) - 0.5);
            float line = smoothstep(0.08, 0.05, d);
            vec3 c = tex(uv);
            return vec4(mix(c, vec3(1.0) - c, line), 1.0);
        }"""),

    Effect("hex_grid", "Hex Grid", C,
        params = listOf(EffectParam("Scale", 4f, 40f, 16f)),
        body = """
        vec4 process(vec2 uv) {
            vec2 p = uv * uResolution / p0;
            vec2 r = vec2(1.0, 1.7320508);
            vec2 a = mod(p, r) - r * 0.5;
            vec2 b = mod(p - r * 0.5, r) - r * 0.5;
            vec2 gv = dot(a, a) < dot(b, b) ? a : b;
            float line = smoothstep(0.42, 0.5, length(gv));
            return vec4(mix(tex(uv), tex(uv) * 0.4, line), 1.0);
        }"""),

    Effect("warp_tunnel", "Warp Tunnel", C, animated = true,
        params = listOf(EffectParam("Speed", 0f, 4f, 1f)),
        body = """
        vec4 process(vec2 uv) {
            vec2 p = centered(uv);
            float r = length(p);
            vec2 t = vec2(atan(p.y, p.x) / PI, 0.2 / r + uTime * p0 * 0.2);
            return vec4(tex(fract(t)) * smoothstep(1.2, 0.0, r), 1.0);
        }"""),

    Effect("moire2", "Moire", C,
        params = listOf(EffectParam("Frequency", 20f, 300f, 120f)),
        body = """
        vec4 process(vec2 uv) {
            vec2 p = centered(uv);
            float m = sin(length(p) * p0) * sin(length(p - vec2(0.05, 0.0)) * p0);
            return vec4(tex(uv) * (0.6 + 0.4 * m), 1.0);
        }"""),

    Effect("tiles3d", "Tiles", C,
        params = listOf(EffectParam("Count", 6f, 60f, 24f)),
        body = """
        vec4 process(vec2 uv) {
            vec2 f = fract(uv * p0);
            vec3 c = tex((floor(uv * p0) + 0.5) / p0);
            float bevel = min(min(f.x, 1.0 - f.x), min(f.y, 1.0 - f.y));
            return vec4(c * mix(0.6, 1.1, smoothstep(0.0, 0.1, bevel)), 1.0);
        }"""),

    Effect("voronoi_cells", "Voronoi Cells", C,
        params = listOf(EffectParam("Scale", 4f, 40f, 16f)),
        body = """
        vec4 process(vec2 uv) {
            vec2 g = uv * p0; vec2 i = floor(g); vec2 f = fract(g);
            float d1 = 8.0, d2 = 8.0;
            for (int y = -1; y <= 1; y++) for (int x = -1; x <= 1; x++) {
                vec2 o = vec2(float(x), float(y));
                vec2 jc = o + vec2(hash21(i + o), hash21(i + o + 3.7)) - f;
                float d = dot(jc, jc);
                if (d < d1) { d2 = d1; d1 = d; } else if (d < d2) { d2 = d; }
            }
            float edge = smoothstep(0.0, 0.05, sqrt(d2) - sqrt(d1));
            return vec4(tex(uv) * mix(0.3, 1.0, edge), 1.0);
        }"""),

    Effect("circuit", "Circuit", C,
        params = listOf(EffectParam("Scale", 6f, 50f, 20f)),
        body = """
        vec4 process(vec2 uv) {
            vec2 g = uv * p0; vec2 i = floor(g); vec2 f = fract(g);
            float line = (hash21(i) < 0.5)
                ? smoothstep(0.06, 0.04, abs(f.x - 0.5))
                : smoothstep(0.06, 0.04, abs(f.y - 0.5));
            float node = smoothstep(0.12, 0.1, length(f - 0.5)) * step(0.8, hash21(i + 1.0));
            vec3 glow = vec3(0.2, 1.0, 0.6) * (line * 0.5 + node);
            return vec4(tex(uv) * 0.7 + glow * 0.6, 1.0);
        }"""),

    Effect("waves_overlay", "Waves", C, animated = true,
        params = listOf(
            EffectParam("Frequency", 5f, 80f, 30f),
            EffectParam("Speed", 0f, 6f, 1f)
        ),
        body = """
        vec4 process(vec2 uv) {
            float w = sin(uv.x * p0 + uTime * p1) * 0.5 + 0.5;
            return vec4(tex(uv) * (0.7 + 0.3 * w), 1.0);
        }"""),

    Effect("stripes_warp", "Stripes Warp", C, animated = true,
        params = listOf(
            EffectParam("Count", 4f, 60f, 20f),
            EffectParam("Speed", 0f, 4f, 1f)
        ),
        body = """
        vec4 process(vec2 uv) {
            float s = sin((uv.x + sin(uv.y * 6.0 + uTime * p1) * 0.1) * p0 * PI);
            return vec4(tex(uv) * (0.6 + 0.4 * step(0.0, s)), 1.0);
        }"""),

    Effect("mandala", "Mandala", C,
        params = listOf(EffectParam("Segments", 3f, 24f, 8f)),
        body = """
        vec4 process(vec2 uv) {
            vec2 p = centered(uv);
            float a = atan(p.y, p.x);
            float seg = TAU / p0;
            a = abs(mod(a, seg) - seg * 0.5);
            vec2 q = vec2(cos(a), sin(a)) * length(p);
            return vec4(tex(uncentered(q)), 1.0);
        }""")
)
