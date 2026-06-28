package com.glitchstudio.app.effects

private val C = Categories.LIGHT

val lightEffects2: List<Effect> = listOf(

    Effect("god_rays", "God Rays", C,
        params = listOf(
            EffectParam("Intensity", 0f, 1f, 0.5f),
            EffectParam("Pos X", 0f, 1f, 0.5f),
            EffectParam("Pos Y", 0f, 1f, 0.3f)
        ),
        body = """
        vec4 process(vec2 uv) {
            vec2 lp = vec2(p1, p2);
            vec2 dir = uv - lp;
            vec3 sum = vec3(0.0);
            for (int i = 0; i < 16; i++) {
                float t = float(i) / 16.0;
                vec3 sc = tex(uv - dir * t * 0.4);
                sum += sc * smoothstep(0.6, 1.0, luma(sc)) * (1.0 - t);
            }
            return vec4(tex(uv) + (sum / 16.0) * p0 * 3.0, 1.0);
        }"""),

    Effect("aurora", "Aurora", C, animated = true,
        params = listOf(
            EffectParam("Intensity", 0f, 1f, 0.5f),
            EffectParam("Speed", 0f, 3f, 1f)
        ),
        body = """
        vec4 process(vec2 uv) {
            float wave = fbm(vec2(uv.x * 3.0, uv.y * 2.0 - uTime * p1 * 0.3));
            float band = smoothstep(0.4, 0.7, wave) * smoothstep(1.0, 0.4, uv.y);
            vec3 col = hsv2rgb(vec3(0.4 + wave * 0.3, 0.7, 1.0));
            return vec4(tex(uv) + col * band * p0, 1.0);
        }"""),

    Effect("star_glow", "Star Glow", C,
        params = listOf(EffectParam("Intensity", 0f, 1f, 0.5f)),
        body = """
        vec4 process(vec2 uv) {
            vec2 px = 1.0 / uResolution;
            vec3 g = vec3(0.0);
            for (int i = -8; i <= 8; i++) {
                float fi = float(i);
                float w = 1.0 - abs(fi) / 9.0;
                g += max(tex(uv + vec2(fi, 0.0) * px * 2.0) - 0.7, 0.0) * w;
                g += max(tex(uv + vec2(0.0, fi) * px * 2.0) - 0.7, 0.0) * w;
            }
            return vec4(tex(uv) + (g / 18.0) * p0 * 4.0, 1.0);
        }"""),

    Effect("lens_dust", "Lens Dust", C,
        params = listOf(EffectParam("Amount", 0f, 1f, 0.4f)),
        body = """
        vec4 process(vec2 uv) {
            float n = hash21(floor(uv * uResolution / 3.0));
            return vec4(tex(uv) + smoothstep(0.985, 1.0, n) * p0, 1.0);
        }"""),

    Effect("anamorphic", "Anamorphic Flare", C,
        params = listOf(EffectParam("Intensity", 0f, 1f, 0.5f)),
        body = """
        vec4 process(vec2 uv) {
            vec3 streak = vec3(0.0);
            for (int i = -12; i <= 12; i++) {
                float fi = float(i);
                streak += max(tex(uv + vec2(fi / uResolution.x * 4.0, 0.0)) - 0.75, 0.0) * (1.0 - abs(fi) / 13.0);
            }
            return vec4(tex(uv) + vec3(0.2, 0.4, 1.0) * (streak / 12.0) * p0 * 3.0, 1.0);
        }"""),

    Effect("sun_flare", "Sun Flare", C, animated = true,
        params = listOf(
            EffectParam("Intensity", 0f, 1f, 0.5f),
            EffectParam("Pos X", 0f, 1f, 0.7f),
            EffectParam("Pos Y", 0f, 1f, 0.3f)
        ),
        body = """
        vec4 process(vec2 uv) {
            vec2 sp = vec2(p1, p2);
            float d = length(uv - sp);
            float sun = smoothstep(0.25, 0.0, d);
            float rays = 0.5 + 0.5 * sin(atan(uv.y - sp.y, uv.x - sp.x) * 12.0 + uTime);
            vec3 add = vec3(1.0, 0.85, 0.6) * (sun + sun * rays * 0.5) * p0;
            return vec4(tex(uv) + add, 1.0);
        }"""),

    Effect("spotlight", "Spotlight", C,
        params = listOf(
            EffectParam("Radius", 0.1f, 1f, 0.4f),
            EffectParam("Pos X", 0f, 1f, 0.5f),
            EffectParam("Pos Y", 0f, 1f, 0.5f)
        ),
        body = """
        vec4 process(vec2 uv) {
            float d = length(centered(uv) - vec2((p1 - 0.5) * uAspect, p2 - 0.5));
            float s = smoothstep(p0, p0 * 0.5, d);
            return vec4(tex(uv) * mix(0.2, 1.0, s), 1.0);
        }"""),

    Effect("rim_light", "Rim Light", C,
        params = listOf(EffectParam("Intensity", 0f, 1f, 0.5f)),
        body = """
        vec4 process(vec2 uv) {
            vec2 px = 1.0 / uResolution;
            float gx = luma(tex(uv + vec2(px.x, 0.0))) - luma(tex(uv - vec2(px.x, 0.0)));
            float gy = luma(tex(uv + vec2(0.0, px.y))) - luma(tex(uv - vec2(0.0, px.y)));
            return vec4(tex(uv) + vec3(1.0) * length(vec2(gx, gy)) * p0 * 2.0, 1.0);
        }""")
)
