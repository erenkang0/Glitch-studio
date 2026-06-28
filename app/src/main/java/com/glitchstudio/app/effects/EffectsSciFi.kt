package com.glitchstudio.app.effects

private val C = Categories.SCIFI

val sciFiEffects: List<Effect> = listOf(

    Effect("scanner", "Scanner", C, animated = true,
        params = listOf(EffectParam("Speed", 0f, 4f, 1f)),
        body = """
        vec4 process(vec2 uv) {
            vec3 c = tex(uv);
            float line = smoothstep(0.03, 0.0, abs(uv.y - fract(uTime * p0 * 0.2)));
            c += vec3(0.0, 0.5, 0.8) * line;
            c *= 0.9 + 0.1 * sin(uv.y * uResolution.y * 1.5);
            return vec4(c, 1.0);
        }"""),

    Effect("hologram_grid", "Hologram Grid", C, animated = true,
        params = listOf(
            EffectParam("Intensity", 0f, 1f, 0.6f),
            EffectParam("Speed", 0f, 4f, 1f)
        ),
        body = """
        vec4 process(vec2 uv) {
            vec3 c = tex(uv) * vec3(0.4, 0.9, 1.0);
            vec2 P = uv * uResolution;
            float grid = step(0.95, fract(P.x / 8.0)) + step(0.95, fract(P.y / 8.0));
            c += vec3(0.0, 0.6, 0.8) * clamp(grid, 0.0, 1.0) * 0.5;
            c *= 0.8 + 0.2 * sin(uTime * p1 * 4.0 + uv.y * 20.0);
            return vec4(mix(tex(uv), c, p0), 1.0);
        }"""),

    Effect("thermal_hud", "Thermal HUD", C,
        params = listOf(EffectParam("Amount", 0f, 1f, 1f)),
        body = """
        vec4 process(vec2 uv) {
            float l = luma(tex(uv));
            vec3 col = hsv2rgb(vec3((1.0 - l) * 0.7, 1.0, 1.0));
            col *= 0.85 + 0.15 * sin(uv.y * uResolution.y);
            float v = 1.0 - dot(centered(uv), centered(uv)) * 0.8;
            return vec4(mix(tex(uv), col * v, p0), 1.0);
        }"""),

    Effect("wireframe", "Wireframe", C,
        params = listOf(EffectParam("Intensity", 0f, 2f, 1f)),
        body = """
        vec4 process(vec2 uv) {
            vec2 px = 1.0 / uResolution;
            float gx = luma(tex(uv + vec2(px.x, 0.0))) - luma(tex(uv - vec2(px.x, 0.0)));
            float gy = luma(tex(uv + vec2(0.0, px.y))) - luma(tex(uv - vec2(0.0, px.y)));
            float e = length(vec2(gx, gy)) * p0 * 3.0;
            return vec4(vec3(0.0, 1.0, 0.7) * clamp(e, 0.0, 1.0), 1.0);
        }"""),

    Effect("datastream", "Datastream", C, animated = true,
        params = listOf(
            EffectParam("Amount", 0f, 1f, 0.6f),
            EffectParam("Speed", 0f, 4f, 1.5f)
        ),
        body = """
        vec4 process(vec2 uv) {
            vec3 c = tex(uv) * vec3(0.2, 0.6, 0.3);
            float col = floor(uv.x * 40.0);
            float speed = 0.5 + hash11(col);
            float y = fract(uv.y + uTime * p1 * speed * 0.3);
            float ch = hash21(vec2(col, floor(uv.y * 30.0 - uTime * p1 * speed * 9.0)));
            float bright = step(0.5, ch) * smoothstep(0.0, 0.3, y) * smoothstep(1.0, 0.7, y);
            return vec4(c + vec3(0.2, 1.0, 0.4) * bright * p0, 1.0);
        }"""),

    Effect("neon_wire", "Neon Wire", C, animated = true,
        params = listOf(
            EffectParam("Intensity", 0f, 1f, 0.6f),
            EffectParam("Speed", 0f, 4f, 1f)
        ),
        body = """
        vec4 process(vec2 uv) {
            vec2 px = 1.0 / uResolution;
            float gx = luma(tex(uv + vec2(px.x, 0.0))) - luma(tex(uv - vec2(px.x, 0.0)));
            float gy = luma(tex(uv + vec2(0.0, px.y))) - luma(tex(uv - vec2(0.0, px.y)));
            float e = length(vec2(gx, gy));
            float pulse = 0.6 + 0.4 * sin(uTime * p1 * 3.0);
            vec3 col = hsv2rgb(vec3(fract(0.6 + e), 1.0, 1.0));
            return vec4(col * e * p0 * 3.0 * pulse, 1.0);
        }""")
)
