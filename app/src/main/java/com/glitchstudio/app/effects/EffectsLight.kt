package com.glitchstudio.app.effects

import com.glitchstudio.app.effects.EffectCategory.LIGHT

internal val lightEffects: List<ShaderEffect> = listOf(

    fx("vignette", "Vignette", LIGHT, "Darken the frame edges to focus the eye.",
        listOf(
            P("Amount", 0f, 1f, 0.7f),
            P("Radius", 0.3f, 1.2f, 0.8f),
        ),
        """
        void main(){
            vec3 c = texture2D(u_Texture, v_TexCoord).rgb;
            float d = distance(v_TexCoord, vec2(0.5)) / 0.7071;
            float vig = 1.0 - smoothstep(u_p1 * 0.5, u_p1, d) * u_p0;
            gl_FragColor = vec4(c * vig, 1.0);
        }
        """),

    fx("bloom", "Bloom", LIGHT, "Bleed light out of the brightest areas.",
        listOf(
            P("Threshold", 0f, 1f, 0.6f),
            P("Intensity", 0f, 3f, 1.3f),
            P("Radius", 0.5f, 5f, 2.2f),
        ),
        """
        void main(){
            vec2 uv = v_TexCoord;
            vec2 px = u_p2 / u_Resolution;
            vec3 c = texture2D(u_Texture, uv).rgb;
            vec3 sum = vec3(0.0);
            for(int i = -3; i <= 3; i++){
                for(int j = -3; j <= 3; j++){
                    vec3 s = texture2D(u_Texture, uv + vec2(float(i), float(j)) * px).rgb;
                    sum += max(s - u_p0, 0.0);
                }
            }
            sum /= 49.0;
            gl_FragColor = vec4(c + sum * u_p1, 1.0);
        }
        """),

    fx("glow", "Soft Glow", LIGHT, "Dreamy diffusion that blooms the midtones.",
        listOf(
            P("Amount", 0f, 1f, 0.5f),
            P("Radius", 0.5f, 6f, 3f),
        ),
        """
        void main(){
            vec2 uv = v_TexCoord;
            vec2 px = u_p1 / u_Resolution;
            vec3 c = texture2D(u_Texture, uv).rgb;
            vec3 b = vec3(0.0);
            for(int i = -3; i <= 3; i++){
                for(int j = -3; j <= 3; j++){
                    b += texture2D(u_Texture, uv + vec2(float(i), float(j)) * px).rgb;
                }
            }
            b /= 49.0;
            vec3 screen = 1.0 - (1.0 - c) * (1.0 - b);
            gl_FragColor = vec4(mix(c, screen, u_p0), 1.0);
        }
        """),

    fx("lightleak", "Light Leak", LIGHT, "Analogue light spill across the frame.",
        listOf(
            P("Angle", 0f, 1f, 0.15f),
            P("Intensity", 0f, 1.2f, 0.55f),
            P("Hue", 0f, 1f, 0.05f),
        ),
        LIB_HSV + """
        void main(){
            vec3 c = texture2D(u_Texture, v_TexCoord).rgb;
            float a = u_p0 * 6.28318;
            vec2 dir = vec2(cos(a), sin(a));
            float t = dot(v_TexCoord - 0.5, dir) + 0.5;
            float leak = smoothstep(0.45, 1.0, t);
            vec3 hue = hsv2rgb(vec3(u_p2, 0.75, 1.0));
            vec3 add = leak * hue * u_p1;
            gl_FragColor = vec4(1.0 - (1.0 - c) * (1.0 - add), 1.0);
        }
        """),

    fx("lensflare", "Lens Flare", LIGHT, "Bright core with a soft anamorphic halo.",
        listOf(
            P("Pos X", 0f, 1f, 0.72f),
            P("Pos Y", 0f, 1f, 0.3f),
            P("Intensity", 0f, 1f, 0.5f),
        ),
        """
        void main(){
            vec2 uv = v_TexCoord;
            vec3 c = texture2D(u_Texture, uv).rgb;
            vec2 pos = vec2(u_p0, u_p1);
            float d = distance(uv, pos);
            float core = u_p2 * 0.02 / (d + 0.02);
            float halo = u_p2 * 0.5 * smoothstep(0.35, 0.0, abs(d - 0.28));
            vec3 add = core * vec3(1.0, 0.92, 0.72) + halo * vec3(0.55, 0.78, 1.0);
            gl_FragColor = vec4(c + add, 1.0);
        }
        """),

    fx("spotlight", "Spotlight", LIGHT, "Pool of light at a point, dark elsewhere.",
        listOf(
            P("Pos X", 0f, 1f, 0.5f),
            P("Pos Y", 0f, 1f, 0.5f),
            P("Size", 0.1f, 1.2f, 0.55f),
        ),
        """
        void main(){
            vec3 c = texture2D(u_Texture, v_TexCoord).rgb;
            float d = distance(v_TexCoord, vec2(u_p0, u_p1));
            float m = 1.0 - smoothstep(u_p2 * 0.5, u_p2, d);
            gl_FragColor = vec4(c * mix(0.12, 1.0, m), 1.0);
        }
        """),

    fx("splittone", "Split Tone", LIGHT, "Tint shadows and highlights independently.",
        listOf(
            P("Shadow Hue", 0f, 1f, 0.6f),
            P("Light Hue", 0f, 1f, 0.1f),
            P("Amount", 0f, 1f, 0.5f),
        ),
        LIB_LUMA + LIB_HSV + """
        void main(){
            vec3 c = texture2D(u_Texture, v_TexCoord).rgb;
            float l = luma(c);
            vec3 sh = hsv2rgb(vec3(u_p0, 0.6, 0.5));
            vec3 hi = hsv2rgb(vec3(u_p1, 0.6, 0.9));
            c += (sh - 0.5) * (1.0 - l) * u_p2;
            c += (hi - 0.5) * l * u_p2;
            gl_FragColor = vec4(clamp(c, 0.0, 1.0), 1.0);
        }
        """),

    fx("centerlight", "Center Light", LIGHT, "Radial exposure boost from the middle.",
        listOf(P("Amount", -1f, 1f, 0.4f, bipolar = true)),
        """
        void main(){
            vec3 c = texture2D(u_Texture, v_TexCoord).rgb;
            float d = distance(v_TexCoord, vec2(0.5));
            c *= 1.0 + u_p0 * (0.5 - d) * 2.0;
            gl_FragColor = vec4(clamp(c, 0.0, 1.0), 1.0);
        }
        """),
)
