package com.glitchstudio.app.effects

import com.glitchstudio.app.effects.EffectCategory.COLOR

internal val colorEffects: List<ShaderEffect> = listOf(

    fx("exposure", "Exposure", COLOR, "Stops of light, like opening the aperture.",
        listOf(P("Exposure", -3f, 3f, 0f, bipolar = true)),
        """
        void main(){
            vec3 c = texture2D(u_Texture, v_TexCoord).rgb;
            c *= pow(2.0, u_p0);
            gl_FragColor = vec4(clamp(c, 0.0, 1.0), 1.0);
        }
        """),

    fx("brightness", "Brightness", COLOR, "Uniform lift or drop in luminance.",
        listOf(P("Amount", -0.5f, 0.5f, 0f, bipolar = true)),
        """
        void main(){
            vec3 c = texture2D(u_Texture, v_TexCoord).rgb + u_p0;
            gl_FragColor = vec4(clamp(c, 0.0, 1.0), 1.0);
        }
        """),

    fx("contrast", "Contrast", COLOR, "Push tones away from or toward mid-grey.",
        listOf(P("Contrast", 0f, 2f, 1f)),
        """
        void main(){
            vec3 c = texture2D(u_Texture, v_TexCoord).rgb;
            c = (c - 0.5) * u_p0 + 0.5;
            gl_FragColor = vec4(clamp(c, 0.0, 1.0), 1.0);
        }
        """),

    fx("saturation", "Saturation", COLOR, "Intensity of all colours at once.",
        listOf(P("Saturation", 0f, 2f, 1f)),
        LIB_LUMA + """
        void main(){
            vec3 c = texture2D(u_Texture, v_TexCoord).rgb;
            c = mix(vec3(luma(c)), c, u_p0);
            gl_FragColor = vec4(clamp(c, 0.0, 1.0), 1.0);
        }
        """),

    fx("vibrance", "Vibrance", COLOR, "Smart saturation that protects skin tones.",
        listOf(P("Vibrance", -1f, 1f, 0f, bipolar = true)),
        LIB_LUMA + """
        void main(){
            vec3 c = texture2D(u_Texture, v_TexCoord).rgb;
            float mx = max(c.r, max(c.g, c.b));
            float mn = min(c.r, min(c.g, c.b));
            float sat = mx - mn;
            c = mix(vec3(luma(c)), c, 1.0 + u_p0 * (1.0 - sat));
            gl_FragColor = vec4(clamp(c, 0.0, 1.0), 1.0);
        }
        """),

    fx("temperature", "Temperature", COLOR, "Warm the image up or cool it down.",
        listOf(P("Temp", -0.25f, 0.25f, 0f, bipolar = true)),
        """
        void main(){
            vec3 c = texture2D(u_Texture, v_TexCoord).rgb;
            c.r += u_p0; c.b -= u_p0;
            gl_FragColor = vec4(clamp(c, 0.0, 1.0), 1.0);
        }
        """),

    fx("tint", "Tint", COLOR, "Shift between green and magenta.",
        listOf(P("Tint", -0.25f, 0.25f, 0f, bipolar = true)),
        """
        void main(){
            vec3 c = texture2D(u_Texture, v_TexCoord).rgb;
            c.g += u_p0; c.r -= u_p0 * 0.5; c.b -= u_p0 * 0.5;
            gl_FragColor = vec4(clamp(c, 0.0, 1.0), 1.0);
        }
        """),

    fx("hue", "Hue Rotate", COLOR, "Spin every colour around the wheel.",
        listOf(P("Hue", 0f, 1f, 0f)),
        LIB_HSV + """
        void main(){
            vec3 c = texture2D(u_Texture, v_TexCoord).rgb;
            vec3 h = rgb2hsv(c);
            h.x = fract(h.x + u_p0);
            gl_FragColor = vec4(hsv2rgb(h), 1.0);
        }
        """),

    fx("gamma", "Gamma", COLOR, "Bend the tone curve through the mids.",
        listOf(P("Gamma", 0.2f, 3f, 1f)),
        """
        void main(){
            vec3 c = texture2D(u_Texture, v_TexCoord).rgb;
            c = pow(max(c, 0.0), vec3(1.0 / u_p0));
            gl_FragColor = vec4(c, 1.0);
        }
        """),

    fx("hishadow", "Highlights & Shadows", COLOR, "Recover highlights and open shadows.",
        listOf(
            P("Highlights", -0.6f, 0.6f, 0f, bipolar = true),
            P("Shadows", -0.6f, 0.6f, 0f, bipolar = true),
        ),
        LIB_LUMA + """
        void main(){
            vec3 c = texture2D(u_Texture, v_TexCoord).rgb;
            float l = luma(c);
            c += u_p0 * smoothstep(0.5, 1.0, l);
            c += u_p1 * (1.0 - smoothstep(0.0, 0.5, l));
            gl_FragColor = vec4(clamp(c, 0.0, 1.0), 1.0);
        }
        """),

    fx("levels", "Black & White Point", COLOR, "Clip the histogram for punch.",
        listOf(
            P("Blacks", 0f, 0.45f, 0f),
            P("Whites", 0.55f, 1f, 1f),
        ),
        """
        void main(){
            vec3 c = texture2D(u_Texture, v_TexCoord).rgb;
            c = (c - u_p0) / max(0.001, (u_p1 - u_p0));
            gl_FragColor = vec4(clamp(c, 0.0, 1.0), 1.0);
        }
        """),

    fx("invert", "Invert", COLOR, "Photographic negative.",
        listOf(P("Amount", 0f, 1f, 1f)),
        """
        void main(){
            vec3 c = texture2D(u_Texture, v_TexCoord).rgb;
            gl_FragColor = vec4(mix(c, 1.0 - c, u_p0), 1.0);
        }
        """),

    fx("sepia", "Sepia", COLOR, "Warm monochrome of old prints.",
        listOf(P("Amount", 0f, 1f, 1f)),
        LIB_LUMA + """
        void main(){
            vec3 c = texture2D(u_Texture, v_TexCoord).rgb;
            vec3 s = vec3(luma(c)) * vec3(1.07, 0.74, 0.43);
            gl_FragColor = vec4(mix(c, s, u_p0), 1.0);
        }
        """),

    fx("grayscale", "Grayscale", COLOR, "Desaturate to neutral black and white.",
        listOf(P("Amount", 0f, 1f, 1f)),
        LIB_LUMA + """
        void main(){
            vec3 c = texture2D(u_Texture, v_TexCoord).rgb;
            gl_FragColor = vec4(mix(c, vec3(luma(c)), u_p0), 1.0);
        }
        """),

    fx("duotone", "Duotone", COLOR, "Map shadows and highlights to two hues.",
        listOf(
            P("Shadow Hue", 0f, 1f, 0.62f),
            P("Light Hue", 0f, 1f, 0.08f),
            P("Amount", 0f, 1f, 1f),
        ),
        LIB_LUMA + LIB_HSV + """
        void main(){
            vec3 c = texture2D(u_Texture, v_TexCoord).rgb;
            float l = luma(c);
            vec3 a = hsv2rgb(vec3(u_p0, 0.7, 0.18));
            vec3 b = hsv2rgb(vec3(u_p1, 0.55, 1.0));
            vec3 d = mix(a, b, l);
            gl_FragColor = vec4(mix(c, d, u_p2), 1.0);
        }
        """),

    fx("posterize", "Posterize", COLOR, "Quantise tones into flat bands.",
        listOf(P("Levels", 2f, 16f, 6f)),
        """
        void main(){
            vec3 c = texture2D(u_Texture, v_TexCoord).rgb;
            float n = max(2.0, floor(u_p0));
            c = floor(c * n) / (n - 1.0);
            gl_FragColor = vec4(clamp(c, 0.0, 1.0), 1.0);
        }
        """),

    fx("threshold", "Threshold", COLOR, "Hard cut to pure black and white.",
        listOf(P("Level", 0f, 1f, 0.5f)),
        LIB_LUMA + """
        void main(){
            vec3 c = texture2D(u_Texture, v_TexCoord).rgb;
            gl_FragColor = vec4(vec3(step(u_p0, luma(c))), 1.0);
        }
        """),

    fx("solarize", "Solarize", COLOR, "Invert tones past a threshold (Sabattier).",
        listOf(P("Level", 0f, 1f, 0.5f)),
        """
        void main(){
            vec3 c = texture2D(u_Texture, v_TexCoord).rgb;
            c = mix(c, 1.0 - c, step(u_p0, c));
            gl_FragColor = vec4(c, 1.0);
        }
        """),

    fx("fade", "Vintage Fade", COLOR, "Lifted blacks and gentle desaturation.",
        listOf(P("Amount", 0f, 1f, 0.6f)),
        LIB_LUMA + """
        void main(){
            vec3 c = texture2D(u_Texture, v_TexCoord).rgb;
            c = c * (1.0 - u_p0 * 0.25) + u_p0 * 0.12;
            c = mix(c, vec3(luma(c)), u_p0 * 0.2);
            c.r += u_p0 * 0.03; c.b -= u_p0 * 0.02;
            gl_FragColor = vec4(clamp(c, 0.0, 1.0), 1.0);
        }
        """),

    fx("clarity", "Clarity", COLOR, "Local contrast that adds midtone bite.",
        listOf(P("Clarity", -1f, 1f, 0.35f, bipolar = true)),
        """
        void main(){
            vec2 px = 2.0 / u_Resolution;
            vec2 uv = v_TexCoord;
            vec3 c = texture2D(u_Texture, uv).rgb;
            vec3 b = c;
            b += texture2D(u_Texture, uv + vec2(px.x, 0.0)).rgb;
            b += texture2D(u_Texture, uv - vec2(px.x, 0.0)).rgb;
            b += texture2D(u_Texture, uv + vec2(0.0, px.y)).rgb;
            b += texture2D(u_Texture, uv - vec2(0.0, px.y)).rgb;
            b /= 5.0;
            c += (c - b) * u_p0 * 3.0;
            gl_FragColor = vec4(clamp(c, 0.0, 1.0), 1.0);
        }
        """),
)
