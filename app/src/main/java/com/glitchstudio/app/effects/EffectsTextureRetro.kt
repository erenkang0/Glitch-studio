package com.glitchstudio.app.effects

import com.glitchstudio.app.effects.EffectCategory.RETRO
import com.glitchstudio.app.effects.EffectCategory.TEXTURE

internal val textureEffects: List<ShaderEffect> = listOf(

    fx("filmgrain", "Film Grain", TEXTURE, "Living monochrome film grain.",
        listOf(P("Amount", 0f, 0.4f, 0.12f)),
        LIB_NOISE + """
        void main(){
            vec3 c = texture2D(u_Texture, v_TexCoord).rgb;
            float g = hash21(v_TexCoord * u_Resolution + fract(u_Time) * 97.0);
            c += (g - 0.5) * u_p0;
            gl_FragColor = vec4(c, 1.0);
        }
        """, animated = true),

    fx("colornoise", "Color Noise", TEXTURE, "Speckled RGB sensor noise.",
        listOf(P("Amount", 0f, 1f, 0.3f)),
        LIB_NOISE + """
        void main(){
            vec3 c = texture2D(u_Texture, v_TexCoord).rgb;
            vec3 n = vec3(
                hash21(v_TexCoord * u_Resolution + 1.0),
                hash21(v_TexCoord * u_Resolution + 5.0),
                hash21(v_TexCoord * u_Resolution + 9.0));
            c += (n - 0.5) * u_p0;
            gl_FragColor = vec4(clamp(c, 0.0, 1.0), 1.0);
        }
        """),

    fx("oldphoto", "Old Photo", TEXTURE, "Faded, grainy, vignetted antique.",
        listOf(P("Age", 0f, 1f, 0.6f)),
        LIB_LUMA + LIB_NOISE + """
        void main(){
            vec2 uv = v_TexCoord;
            vec3 c = texture2D(u_Texture, uv).rgb;
            float l = luma(c);
            vec3 sep = vec3(l) * vec3(1.0, 0.85, 0.65);
            c = mix(c, sep, 0.85 * u_p0);
            float d = distance(uv, vec2(0.5)) / 0.7071;
            c *= 1.0 - smoothstep(0.45, 1.0, d) * 0.6 * u_p0;
            c += (hash21(uv * u_Resolution + fract(u_Time) * 53.0) - 0.5) * u_p0 * 0.18;
            c = c * (1.0 - u_p0 * 0.1) + u_p0 * 0.05;
            gl_FragColor = vec4(c, 1.0);
        }
        """, animated = true),

    fx("scratches", "Scratches", TEXTURE, "Drifting film scratches and dust.",
        listOf(P("Amount", 0f, 1f, 0.4f)),
        LIB_NOISE + """
        void main(){
            vec2 uv = v_TexCoord;
            vec3 c = texture2D(u_Texture, uv).rgb;
            float t = floor(u_Time * 8.0);
            float s = 0.0;
            for (int i = 0; i < 3; i++){
                float x = hash11(float(i) * 1.7 + t);
                s += smoothstep(0.0025, 0.0, abs(uv.x - x));
            }
            float dust = step(0.9985, hash21(floor(uv * u_Resolution) + t));
            c += (s + dust) * u_p0;
            gl_FragColor = vec4(c, 1.0);
        }
        """, animated = true),

    fx("paper", "Paper", TEXTURE, "Mottled paper grain overlay.",
        listOf(P("Amount", 0f, 1f, 0.4f)),
        LIB_NOISE + """
        void main(){
            vec2 uv = v_TexCoord;
            vec3 c = texture2D(u_Texture, uv).rgb;
            float n = vnoise(uv * u_Resolution * 0.15) * 0.5
                    + vnoise(uv * u_Resolution * 0.45) * 0.5;
            c *= mix(1.0, 0.82 + 0.36 * n, u_p0);
            gl_FragColor = vec4(c, 1.0);
        }
        """),
)

internal val retroEffects: List<ShaderEffect> = listOf(

    fx("crt", "CRT", RETRO, "Curved tube with aperture-grille mask.",
        listOf(
            P("Mask", 0f, 1f, 0.5f),
            P("Curve", 0f, 0.5f, 0.15f),
        ),
        """
        void main(){
            vec2 uv = v_TexCoord;
            vec2 cc = uv - 0.5;
            uv += cc * dot(cc, cc) * u_p1;
            if (uv.x < 0.0 || uv.x > 1.0 || uv.y < 0.0 || uv.y > 1.0){
                gl_FragColor = vec4(0.0, 0.0, 0.0, 1.0);
                return;
            }
            vec3 c = texture2D(u_Texture, uv).rgb;
            c *= 0.85 + 0.15 * sin(uv.y * u_Resolution.y * 3.14159);
            float m = mod(floor(uv.x * u_Resolution.x), 3.0);
            vec3 mask = m < 1.0 ? vec3(1.0, 0.6, 0.6)
                      : (m < 2.0 ? vec3(0.6, 1.0, 0.6) : vec3(0.6, 0.6, 1.0));
            c *= mix(vec3(1.0), mask, u_p0);
            float d = distance(v_TexCoord, vec2(0.5)) / 0.7071;
            c *= 1.0 - smoothstep(0.6, 1.1, d) * 0.5;
            gl_FragColor = vec4(c, 1.0);
        }
        """),

    fx("gameboy", "Game Boy", RETRO, "Four-tone dot-matrix green.",
        listOf(P("Amount", 0f, 1f, 1f)),
        LIB_LUMA + """
        void main(){
            vec3 c = texture2D(u_Texture, v_TexCoord).rgb;
            float l = luma(c);
            float idx = floor(clamp(l, 0.0, 0.999) * 4.0);
            vec3 pal = idx < 0.5 ? vec3(0.06, 0.22, 0.06)
                     : (idx < 1.5 ? vec3(0.19, 0.38, 0.19)
                     : (idx < 2.5 ? vec3(0.55, 0.67, 0.06) : vec3(0.61, 0.74, 0.06)));
            gl_FragColor = vec4(mix(c, pal, u_p0), 1.0);
        }
        """),

    fx("eightbit", "8-Bit", RETRO, "Chunky pixels on a tiny palette.",
        listOf(
            P("Colors", 2f, 8f, 4f),
            P("Pixels", 30f, 220f, 120f),
        ),
        """
        void main(){
            vec2 cells = vec2(u_p1, u_p1 * u_Resolution.y / u_Resolution.x);
            vec2 uv = (floor(v_TexCoord * cells) + 0.5) / cells;
            vec3 c = texture2D(u_Texture, uv).rgb;
            float n = max(2.0, floor(u_p0));
            c = floor(c * n) / (n - 1.0);
            gl_FragColor = vec4(clamp(c, 0.0, 1.0), 1.0);
        }
        """),

    fx("polaroid", "Polaroid", RETRO, "Warm instant-film colour cast.",
        listOf(P("Amount", 0f, 1f, 1f)),
        LIB_LUMA + """
        void main(){
            vec2 uv = v_TexCoord;
            vec3 src = texture2D(u_Texture, uv).rgb;
            vec3 c = src * 1.05 + 0.02;
            c.r = pow(c.r, 0.95);
            c.b = pow(c.b, 1.06);
            c = mix(c, vec3(luma(c)), 0.05);
            float d = distance(uv, vec2(0.5)) / 0.7071;
            c *= 1.0 - smoothstep(0.7, 1.1, d) * 0.3;
            gl_FragColor = vec4(mix(src, c, u_p0), 1.0);
        }
        """),

    fx("technicolor", "Technicolor", RETRO, "Saturated three-strip film look.",
        listOf(P("Amount", 0f, 1f, 0.7f)),
        """
        void main(){
            vec3 c = texture2D(u_Texture, v_TexCoord).rgb;
            vec3 t;
            t.r = c.r - (c.g + c.b) * 0.25;
            t.g = c.g - (c.r + c.b) * 0.25;
            t.b = c.b - (c.r + c.g) * 0.25;
            gl_FragColor = vec4(mix(c, clamp(t * 1.6, 0.0, 1.0), u_p0), 1.0);
        }
        """),

    fx("crossprocess", "Cross Process", RETRO, "C-41 in E-6 colour shift.",
        listOf(P("Amount", 0f, 1f, 0.8f)),
        """
        void main(){
            vec3 src = texture2D(u_Texture, v_TexCoord).rgb;
            vec3 c;
            c.r = pow(src.r, 1.2) + 0.05;
            c.g = pow(src.g, 0.9);
            c.b = pow(src.b, 1.4) + 0.05;
            c = (c - 0.5) * 1.15 + 0.5;
            gl_FragColor = vec4(clamp(mix(src, c, u_p0), 0.0, 1.0), 1.0);
        }
        """),

    fx("vhstrack", "VHS Tracking", RETRO, "Rolling tracking error and bleed.",
        listOf(
            P("Amount", 0f, 1f, 0.5f),
            P("Speed", 0f, 1f, 0.2f),
        ),
        LIB_NOISE + """
        void main(){
            vec2 uv = v_TexCoord;
            float roll = fract(u_Time * u_p1);
            float by = fract(uv.y + roll);
            uv.x += smoothstep(0.1, 0.0, by) * 0.05;
            uv.x += (hash11(floor(uv.y * 200.0) + floor(u_Time * 20.0)) - 0.5) * u_p0 * 0.02;
            float sh = 0.004;
            float r = texture2D(u_Texture, fract(uv) + vec2(sh, 0.0)).r;
            float g = texture2D(u_Texture, fract(uv)).g;
            float b = texture2D(u_Texture, fract(uv) - vec2(sh, 0.0)).b;
            vec3 c = vec3(r, g, b);
            c += smoothstep(0.08, 0.0, by) * 0.2;
            c += (hash21(uv * u_Resolution + u_Time) - 0.5) * u_p0 * 0.3;
            gl_FragColor = vec4(c, 1.0);
        }
        """, animated = true),
)
