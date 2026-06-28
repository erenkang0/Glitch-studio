package com.glitchstudio.app.effects

private val C = Categories.GLITCH

val glitchEffects2: List<Effect> = listOf(

    Effect("slice_shift", "Slice Shift", C, animated = true,
        params = listOf(
            EffectParam("Intensity", 0f, 0.5f, 0.15f),
            EffectParam("Rows", 4f, 120f, 40f),
            EffectParam("Speed", 0f, 10f, 4f)
        ),
        body = """
        vec4 process(vec2 uv) {
            float r = floor(uv.y * p1);
            float t = floor(uTime * p2 * 3.0);
            float sh = (hash21(vec2(r, t)) - 0.5) * p0 * step(0.5, hash21(vec2(r * 1.7, t)));
            uv.x = fract(uv.x + sh);
            return vec4(tex(uv), 1.0);
        }"""),

    Effect("channel_glitch", "Channel Glitch", C, animated = true,
        params = listOf(
            EffectParam("Amount", 0f, 0.1f, 0.03f),
            EffectParam("Speed", 0f, 10f, 4f)
        ),
        body = """
        vec4 process(vec2 uv) {
            float t = floor(uTime * p1 * 4.0);
            float blk = floor(uv.y * 30.0);
            vec2 o = vec2((hash21(vec2(blk, t)) - 0.5) * p0, 0.0);
            float r = tex(uv + o).r;
            float g = tex(uv - o).g;
            float b = tex(uv + o.yx).b;
            return vec4(r, g, b, 1.0);
        }"""),

    Effect("static_noise", "Static", C, animated = true,
        params = listOf(
            EffectParam("Amount", 0f, 1f, 0.4f),
            EffectParam("Speed", 0f, 6f, 3f)
        ),
        body = """
        vec4 process(vec2 uv) {
            vec3 c = tex(uv);
            float n = hash21(uv * uResolution + floor(uTime * p1 * 30.0));
            return vec4(mix(c, vec3(n), p0 * 0.6), 1.0);
        }"""),

    Effect("pixel_drift", "Pixel Drift", C, animated = true,
        params = listOf(
            EffectParam("Amount", 0f, 0.1f, 0.03f),
            EffectParam("Scale", 1f, 30f, 8f),
            EffectParam("Speed", 0f, 4f, 1f)
        ),
        body = """
        vec4 process(vec2 uv) {
            vec2 d = vec2(vnoise(uv * p1 + uTime * p2), vnoise(uv * p1 + 10.0 + uTime * p2)) - 0.5;
            return vec4(tex(uv + d * p0), 1.0);
        }"""),

    Effect("sync_loss", "Sync Loss", C, animated = true,
        params = listOf(
            EffectParam("Amount", 0f, 0.5f, 0.2f),
            EffectParam("Speed", 0f, 6f, 2f)
        ),
        body = """
        vec4 process(vec2 uv) {
            float band = fract(uv.y * 2.0 - uTime * p1 * 0.3);
            float tear = smoothstep(0.0, 0.05, band) * smoothstep(0.2, 0.15, band);
            uv.x = fract(uv.x + tear * p0);
            return vec4(tex(uv), 1.0);
        }"""),

    Effect("block_corrupt", "Block Corrupt", C, animated = true,
        params = listOf(
            EffectParam("Block Size", 4f, 64f, 18f),
            EffectParam("Amount", 0f, 0.4f, 0.15f),
            EffectParam("Speed", 0f, 6f, 3f)
        ),
        body = """
        vec4 process(vec2 uv) {
            vec2 grid = floor(uv * uResolution / max(p0, 1.0));
            float t = floor(uTime * p2 * 2.0);
            float k = hash21(grid + t);
            vec2 off = (vec2(hash21(grid + t + 1.0), hash21(grid - t)) - 0.5) * p1 * step(0.7, k);
            vec3 c = tex(uv + off);
            if (k > 0.95) c = vec3(hash21(grid * 1.3 + t));
            return vec4(c, 1.0);
        }"""),

    Effect("color_bleed", "Color Bleed", C,
        params = listOf(EffectParam("Amount", 0f, 0.2f, 0.08f)),
        body = """
        vec4 process(vec2 uv) {
            vec3 acc = tex(uv); float w = 1.0;
            for (int i = 1; i < 12; i++) {
                float fi = float(i) / 12.0;
                acc += tex(uv - vec2(fi * p0, 0.0)) * (1.0 - fi);
                w += (1.0 - fi);
            }
            return vec4(acc / w, 1.0);
        }"""),

    Effect("ghosting", "Ghosting", C,
        params = listOf(
            EffectParam("Offset", 0f, 0.08f, 0.025f),
            EffectParam("Decay", 0.1f, 0.95f, 0.6f)
        ),
        body = """
        vec4 process(vec2 uv) {
            vec3 c = vec3(0.0); float w = 0.0;
            for (int i = 0; i < 5; i++) {
                float fi = float(i);
                float a = pow(p1, fi);
                c += tex(uv - vec2(p0 * fi, 0.0)) * a;
                w += a;
            }
            return vec4(c / w, 1.0);
        }"""),

    Effect("signal_roll", "Signal Roll", C, animated = true,
        params = listOf(
            EffectParam("Speed", 0f, 3f, 0.5f),
            EffectParam("Width", 0.02f, 0.3f, 0.1f)
        ),
        body = """
        vec4 process(vec2 uv) {
            float bar = fract(uv.y + uTime * p0);
            float b = smoothstep(0.0, p1, bar) * smoothstep(p1 * 2.0, p1, bar);
            vec3 c = tex(uv) + b * 0.3;
            c -= smoothstep(p1 * 2.0, p1 * 3.0, bar) * 0.1;
            return vec4(c, 1.0);
        }"""),

    Effect("compression", "Compression", C,
        params = listOf(
            EffectParam("Block", 2f, 24f, 8f),
            EffectParam("Strength", 0f, 1f, 0.6f)
        ),
        body = """
        vec4 process(vec2 uv) {
            vec2 grid = floor(uv * uResolution / p0) * p0 / uResolution;
            vec3 blk = tex(grid + (p0 * 0.5) / uResolution);
            return vec4(mix(tex(uv), blk, p1), 1.0);
        }""")
)
