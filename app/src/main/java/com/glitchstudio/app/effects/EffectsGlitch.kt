package com.glitchstudio.app.effects

private val C = Categories.GLITCH

val glitchEffects: List<Effect> = listOf(

    Effect(
        id = "rgb_split", name = "RGB Split", category = C,
        params = listOf(
            EffectParam("Amount", 0f, 0.1f, 0.02f),
            EffectParam("Angle", 0f, 6.2831f, 0f)
        ),
        body = """
        vec4 process(vec2 uv) {
            vec2 dir = vec2(cos(p1), sin(p1)) * p0;
            float r = tex(uv + dir).r;
            float g = tex(uv).g;
            float b = tex(uv - dir).b;
            return vec4(r, g, b, 1.0);
        }"""
    ),

    Effect(
        id = "digital_glitch", name = "Digital Glitch", category = C, animated = true,
        params = listOf(
            EffectParam("Intensity", 0f, 1f, 0.5f),
            EffectParam("Speed", 0f, 10f, 3f),
            EffectParam("Blocks", 4f, 80f, 24f)
        ),
        body = """
        vec4 process(vec2 uv) {
            float t = floor(uTime * p1 * 5.0);
            float line = floor(uv.y * p2);
            float n = hash21(vec2(line, t));
            float gate = step(0.7, hash21(vec2(line * 1.3, t)));
            float shift = (n - 0.5) * 0.3 * p0 * gate;
            vec2 uv2 = uv; uv2.x += shift;
            float r = tex(uv2 + vec2(0.01 * p0, 0.0)).r;
            float g = tex(uv2).g;
            float b = tex(uv2 - vec2(0.01 * p0, 0.0)).b;
            vec3 c = vec3(r, g, b);
            float blk = step(0.96, hash21(vec2(line * 2.7, t + 1.0)));
            c = mix(c, vec3(hash21(vec2(uv.y * 50.0, t))), blk * p0 * 0.5);
            return vec4(c, 1.0);
        }"""
    ),

    Effect(
        id = "scanline_tear", name = "Scanline Tear", category = C, animated = true,
        params = listOf(
            EffectParam("Strength", 0f, 0.2f, 0.05f),
            EffectParam("Speed", 0f, 10f, 2f)
        ),
        body = """
        vec4 process(vec2 uv) {
            float t = uTime * p1;
            float tear = sin(uv.y * 80.0 + t * 6.0) * step(0.5, fract(uv.y * 4.0 + t));
            uv.x += tear * p0 * 0.1;
            vec3 c = tex(uv);
            c *= 0.9 + 0.1 * sin(uv.y * 700.0);
            return vec4(c, 1.0);
        }"""
    ),

    Effect(
        id = "pixel_streak", name = "Pixel Streak", category = C,
        params = listOf(
            EffectParam("Threshold", 0f, 1f, 0.55f),
            EffectParam("Length", 0f, 0.3f, 0.12f)
        ),
        body = """
        vec4 process(vec2 uv) {
            vec3 c = tex(uv);
            float steps = 24.0;
            for (int i = 1; i < 24; i++) {
                float fi = float(i) / steps;
                vec3 sc = tex(vec2(uv.x - fi * p1, uv.y));
                if (luma(sc) > p0) { c = max(c, sc * (1.0 - fi)); }
            }
            return vec4(c, 1.0);
        }"""
    ),

    Effect(
        id = "datamosh", name = "Datamosh", category = C, animated = true,
        params = listOf(
            EffectParam("Block Size", 4f, 64f, 16f),
            EffectParam("Displace", 0f, 0.3f, 0.08f),
            EffectParam("Seed", 0f, 10f, 1f)
        ),
        body = """
        vec4 process(vec2 uv) {
            vec2 grid = floor(uv * uResolution / max(p0, 1.0));
            float t = floor(uTime * 2.0 + p2);
            vec2 off = (vec2(hash21(grid + t), hash21(grid * 1.7 - t)) - 0.5) * p1;
            float move = step(0.6, hash21(grid * 0.3 + t * 0.7));
            return vec4(tex(uv + off * move), 1.0);
        }"""
    ),

    Effect(
        id = "wave_glitch", name = "Wave Glitch", category = C, animated = true,
        params = listOf(
            EffectParam("Amplitude", 0f, 0.1f, 0.02f),
            EffectParam("Frequency", 1f, 60f, 20f),
            EffectParam("Speed", 0f, 10f, 3f)
        ),
        body = """
        vec4 process(vec2 uv) {
            uv.x += sin(uv.y * p1 + uTime * p2) * p0;
            return vec4(tex(uv), 1.0);
        }"""
    ),

    Effect(
        id = "chromatic", name = "Chromatic Aberration", category = C,
        params = listOf(
            EffectParam("Strength", 0f, 0.05f, 0.012f),
            EffectParam("Falloff", 0f, 3f, 1.5f)
        ),
        body = """
        vec4 process(vec2 uv) {
            vec2 d = uv - 0.5;
            float r2 = dot(d, d);
            float k = p0 * pow(r2, p1 * 0.5) * 8.0;
            vec2 dir = normalize(d + 1e-5);
            vec3 c;
            c.r = tex(uv + dir * k).r;
            c.g = tex(uv).g;
            c.b = tex(uv - dir * k).b;
            return vec4(c, 1.0);
        }"""
    ),

    Effect(
        id = "vhs", name = "VHS", category = C, animated = true,
        params = listOf(
            EffectParam("Noise", 0f, 1f, 0.4f),
            EffectParam("Wobble", 0f, 0.05f, 0.01f),
            EffectParam("Chroma", 0f, 0.03f, 0.008f),
            EffectParam("Scanline", 0f, 1f, 0.5f)
        ),
        body = """
        vec4 process(vec2 uv) {
            float t = uTime;
            uv.x += sin(uv.y * 120.0 + t * 5.0) * p1;
            uv.x += (hash21(vec2(floor(uv.y * 200.0), floor(t * 15.0))) - 0.5) * p1 * 2.0;
            vec3 c;
            c.r = tex(uv + vec2(p2, 0.0)).r;
            c.g = tex(uv).g;
            c.b = tex(uv - vec2(p2, 0.0)).b;
            float n = hash21(uv * uResolution + t * 50.0);
            c += (n - 0.5) * p0 * 0.5;
            c *= 1.0 - p3 * 0.3 * (0.5 + 0.5 * sin(uv.y * uResolution.y * 2.0));
            return vec4(c, 1.0);
        }"""
    ),

    Effect(
        id = "bad_signal", name = "Bad Signal", category = C, animated = true,
        params = listOf(
            EffectParam("Noise", 0f, 1f, 0.5f),
            EffectParam("Tearing", 0f, 0.3f, 0.1f),
            EffectParam("Roll", 0f, 1f, 0.4f)
        ),
        body = """
        vec4 process(vec2 uv) {
            float t = uTime;
            float jump = step(0.985, hash21(vec2(floor(t * 3.0), 1.0)));
            uv.y = fract(uv.y + jump * p2 * hash21(vec2(floor(t * 3.0), 2.0)));
            float line = floor(uv.y * uResolution.y);
            float tear = (hash21(vec2(line, floor(t * 20.0))) - 0.5);
            uv.x += tear * step(0.7, abs(tear) * 2.0) * p1;
            vec3 c = tex(uv);
            float n = hash21(uv * uResolution + t * 99.0);
            c = mix(c, vec3(n), step(0.92, n) * p0);
            return vec4(c, 1.0);
        }"""
    ),

    Effect(
        id = "channel_shift", name = "Channel Shift", category = C,
        params = listOf(
            EffectParam("Red X", -0.05f, 0.05f, 0.012f),
            EffectParam("Green X", -0.05f, 0.05f, 0f),
            EffectParam("Blue X", -0.05f, 0.05f, -0.012f)
        ),
        body = """
        vec4 process(vec2 uv) {
            float r = tex(uv + vec2(p0, 0.0)).r;
            float g = tex(uv + vec2(p1, 0.0)).g;
            float b = tex(uv + vec2(p2, 0.0)).b;
            return vec4(r, g, b, 1.0);
        }"""
    )
)
