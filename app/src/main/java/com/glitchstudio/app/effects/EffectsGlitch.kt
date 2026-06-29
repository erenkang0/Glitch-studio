package com.glitchstudio.app.effects

import com.glitchstudio.app.effects.EffectCategory.GLITCH

internal val glitchEffects: List<ShaderEffect> = listOf(

    fx("rgbsplit", "RGB Split", GLITCH, "Offset the colour channels apart.",
        listOf(
            P("Amount", 0f, 0.1f, 0.02f),
            P("Angle", 0f, 1f, 0f),
        ),
        """
        void main(){
            vec2 uv = v_TexCoord;
            float a = u_p1 * 6.28318;
            vec2 off = vec2(cos(a), sin(a)) * u_p0;
            float r = texture2D(u_Texture, uv + off).r;
            float g = texture2D(u_Texture, uv).g;
            float b = texture2D(u_Texture, uv - off).b;
            gl_FragColor = vec4(r, g, b, 1.0);
        }
        """),

    fx("chromab", "Chromatic Aberration", GLITCH, "Radial colour fringing from centre.",
        listOf(P("Amount", 0f, 0.12f, 0.03f)),
        """
        void main(){
            vec2 uv = v_TexCoord;
            vec2 d = uv - 0.5;
            float r = texture2D(u_Texture, uv + d * u_p0).r;
            float g = texture2D(u_Texture, uv).g;
            float b = texture2D(u_Texture, uv - d * u_p0).b;
            gl_FragColor = vec4(r, g, b, 1.0);
        }
        """),

    fx("scanlines", "Scanlines", GLITCH, "Dark CRT raster lines.",
        listOf(
            P("Count", 100f, 1200f, 600f),
            P("Intensity", 0f, 1f, 0.5f),
        ),
        """
        void main(){
            vec3 c = texture2D(u_Texture, v_TexCoord).rgb;
            float s = sin(v_TexCoord.y * u_p0 * 3.14159) * 0.5 + 0.5;
            c *= 1.0 - u_p1 * (1.0 - s);
            gl_FragColor = vec4(c, 1.0);
        }
        """),

    fx("vhs", "VHS", GLITCH, "Tape jitter, chroma bleed and noise.",
        listOf(
            P("Distortion", 0f, 0.1f, 0.02f),
            P("Noise", 0f, 0.4f, 0.12f),
        ),
        LIB_NOISE + """
        void main(){
            vec2 uv = v_TexCoord;
            float line = floor(uv.y * u_Resolution.y);
            float jck = step(0.96, hash11(line * 0.07 + floor(u_Time * 12.0)));
            uv.x += (hash11(line + floor(u_Time * 15.0)) - 0.5) * u_p0 * jck;
            float sh = u_p0 * 0.5 + 0.003;
            float r = texture2D(u_Texture, uv + vec2(sh, 0.0)).r;
            float g = texture2D(u_Texture, uv).g;
            float b = texture2D(u_Texture, uv - vec2(sh, 0.0)).b;
            vec3 c = vec3(r, g, b);
            c += (hash21(uv * u_Resolution * 0.5 + u_Time) - 0.5) * u_p1;
            c *= 0.9 + 0.1 * sin(uv.y * u_Resolution.y * 1.5);
            gl_FragColor = vec4(c, 1.0);
        }
        """, animated = true),

    fx("blocks", "Digital Blocks", GLITCH, "Datamosh blocks tear and swap.",
        listOf(
            P("Intensity", 0f, 1f, 0.3f),
            P("Blocks", 4f, 40f, 16f),
            P("Speed", 1f, 30f, 12f),
        ),
        LIB_NOISE + """
        void main(){
            vec2 uv = v_TexCoord;
            float blocks = floor(u_p1);
            vec2 g = floor(uv * blocks);
            float t = floor(u_Time * u_p2);
            float r = hash21(g + t);
            float r2 = hash21(g * 1.7 + t);
            vec2 off = vec2(0.0);
            if (r > 1.0 - u_p0) off.x = (r2 - 0.5) * 0.25;
            vec3 c = texture2D(u_Texture, fract(uv + off)).rgb;
            if (r > 1.0 - u_p0 * 0.4) c = c.gbr;
            gl_FragColor = vec4(c, 1.0);
        }
        """, animated = true),

    fx("sliceshift", "Slice Shift", GLITCH, "Horizontal slices jump sideways.",
        listOf(
            P("Intensity", 0f, 1f, 0.4f),
            P("Slices", 5f, 60f, 24f),
            P("Speed", 1f, 30f, 10f),
        ),
        LIB_NOISE + """
        void main(){
            vec2 uv = v_TexCoord;
            float slices = floor(u_p1);
            float row = floor(uv.y * slices);
            float t = floor(u_Time * u_p2);
            float h = hash21(vec2(row, t));
            float shift = step(1.0 - u_p0, h) * (hash11(row + t) - 0.5) * 0.35;
            uv.x = fract(uv.x + shift);
            gl_FragColor = vec4(texture2D(u_Texture, uv).rgb, 1.0);
        }
        """, animated = true),

    fx("datamosh", "Data Mosh", GLITCH, "Blocks smear in random directions.",
        listOf(
            P("Intensity", 0f, 1f, 0.5f),
            P("Block", 8f, 60f, 24f),
            P("Speed", 0f, 5f, 1f),
        ),
        LIB_NOISE + """
        void main(){
            vec2 uv = v_TexCoord;
            float bs = floor(u_p1);
            vec2 block = floor(uv * bs) / bs;
            float n = vnoise(block * 3.0 + u_Time * u_p2);
            vec2 dir = vec2(vnoise(block + 1.0), vnoise(block + 5.0)) - 0.5;
            uv += dir * step(1.0 - u_p0, n) * 0.15;
            gl_FragColor = vec4(texture2D(u_Texture, fract(uv)).rgb, 1.0);
        }
        """, animated = true),

    fx("badsignal", "Bad Signal", GLITCH, "Static bursts and a rolling band.",
        listOf(P("Static", 0f, 1f, 0.3f)),
        LIB_NOISE + """
        void main(){
            vec2 uv = v_TexCoord;
            float bar = sin((uv.y + u_Time * 0.2) * 6.28318);
            float d = step(0.95, hash11(floor(uv.y * 120.0) + floor(u_Time * 8.0)));
            uv.x += d * (hash11(uv.y + u_Time) - 0.5) * 0.1;
            vec3 c = texture2D(u_Texture, fract(uv)).rgb;
            float n = hash21(uv * u_Resolution + u_Time);
            c = mix(c, vec3(n), u_p0 * (0.5 + 0.5 * bar));
            gl_FragColor = vec4(c, 1.0);
        }
        """, animated = true),

    fx("pixeldrift", "Pixel Drift", GLITCH, "Bright pixels smear sideways.",
        listOf(
            P("Threshold", 0f, 1f, 0.6f),
            P("Length", 0f, 0.3f, 0.1f),
        ),
        LIB_LUMA + """
        void main(){
            vec2 uv = v_TexCoord;
            vec3 c = texture2D(u_Texture, uv).rgb;
            float l = luma(c);
            if (l > u_p0) {
                float drag = (l - u_p0) * u_p1;
                c = texture2D(u_Texture, vec2(fract(uv.x - drag), uv.y)).rgb;
            }
            gl_FragColor = vec4(c, 1.0);
        }
        """),

    fx("chromapulse", "Chromatic Pulse", GLITCH, "Colour fringe that throbs over time.",
        listOf(
            P("Amount", 0f, 0.1f, 0.04f),
            P("Speed", 0f, 10f, 4f),
        ),
        """
        void main(){
            vec2 uv = v_TexCoord;
            vec2 d = uv - 0.5;
            float amt = u_p0 * (0.5 + 0.5 * sin(u_Time * u_p1));
            float r = texture2D(u_Texture, uv + d * amt).r;
            float g = texture2D(u_Texture, uv).g;
            float b = texture2D(u_Texture, uv - d * amt).b;
            gl_FragColor = vec4(r, g, b, 1.0);
        }
        """, animated = true),

    fx("ghosting", "Ghosting", GLITCH, "Analogue TV echo of the image.",
        listOf(
            P("Offset", 0f, 0.1f, 0.03f),
            P("Amount", 0f, 1f, 0.5f),
        ),
        """
        void main(){
            vec2 uv = v_TexCoord;
            vec3 c = texture2D(u_Texture, uv).rgb;
            vec3 ghost = texture2D(u_Texture, uv - vec2(u_p0, 0.0)).rgb;
            c = mix(c, max(c, ghost), u_p1);
            gl_FragColor = vec4(c, 1.0);
        }
        """),

    fx("channelswap", "Channel Swap", GLITCH, "Rotate the RGB channels.",
        listOf(P("Mode", 0f, 3f, 1f)),
        """
        void main(){
            vec3 c = texture2D(u_Texture, v_TexCoord).rgb;
            float m = floor(u_p0 + 0.5);
            if (m < 0.5) c = c.rgb;
            else if (m < 1.5) c = c.gbr;
            else if (m < 2.5) c = c.brg;
            else c = c.bgr;
            gl_FragColor = vec4(c, 1.0);
        }
        """),
)
