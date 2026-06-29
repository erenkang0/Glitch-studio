package com.glitchstudio.app.effects

import com.glitchstudio.app.effects.EffectCategory.DISTORT

internal val distortEffects: List<ShaderEffect> = listOf(

    fx("wave", "Wave", DISTORT, "Rolling sine waves ripple the image.",
        listOf(
            P("Amplitude", 0f, 0.1f, 0.02f),
            P("Frequency", 1f, 50f, 12f),
            P("Speed", 0f, 5f, 1.5f),
        ),
        """
        void main(){
            vec2 uv = v_TexCoord;
            uv.x += sin(uv.y * u_p1 + u_Time * u_p2) * u_p0;
            uv.y += sin(uv.x * u_p1 + u_Time * u_p2) * u_p0 * 0.5;
            gl_FragColor = vec4(texture2D(u_Texture, uv).rgb, 1.0);
        }
        """, animated = true),

    fx("ripple", "Ripple", DISTORT, "Concentric ripples from the centre.",
        listOf(
            P("Amplitude", 0f, 0.08f, 0.02f),
            P("Frequency", 5f, 80f, 40f),
            P("Speed", 0f, 8f, 3f),
        ),
        """
        void main(){
            vec2 uv = v_TexCoord;
            vec2 d = uv - 0.5;
            float r = length(d);
            float w = sin(r * u_p1 - u_Time * u_p2) * u_p0;
            uv += normalize(d + 1e-6) * w;
            gl_FragColor = vec4(texture2D(u_Texture, uv).rgb, 1.0);
        }
        """, animated = true),

    fx("swirl", "Swirl", DISTORT, "Twist pixels around the centre.",
        listOf(
            P("Angle", -6f, 6f, 3f, bipolar = true),
            P("Radius", 0.1f, 1f, 0.5f),
        ),
        """
        void main(){
            vec2 c = vec2(0.5);
            vec2 d = v_TexCoord - c;
            float r = length(d);
            float a = atan(d.y, d.x) + u_p0 * smoothstep(u_p1, 0.0, r);
            vec2 uv = c + vec2(cos(a), sin(a)) * r;
            gl_FragColor = vec4(texture2D(u_Texture, uv).rgb, 1.0);
        }
        """),

    fx("bulge", "Bulge", DISTORT, "Magnify outward like a glass dome.",
        listOf(
            P("Amount", 0f, 0.9f, 0.5f),
            P("Radius", 0.1f, 1f, 0.6f),
        ),
        """
        void main(){
            vec2 c = vec2(0.5);
            vec2 d = v_TexCoord - c;
            float t = smoothstep(u_p1, 0.0, length(d));
            vec2 uv = c + d * (1.0 - u_p0 * t);
            gl_FragColor = vec4(texture2D(u_Texture, uv).rgb, 1.0);
        }
        """),

    fx("pinch", "Pinch", DISTORT, "Squeeze pixels toward the centre.",
        listOf(
            P("Amount", 0f, 1.5f, 0.6f),
            P("Radius", 0.1f, 1f, 0.6f),
        ),
        """
        void main(){
            vec2 c = vec2(0.5);
            vec2 d = v_TexCoord - c;
            float t = smoothstep(u_p1, 0.0, length(d));
            vec2 uv = c + d * (1.0 + u_p0 * t);
            gl_FragColor = vec4(texture2D(u_Texture, uv).rgb, 1.0);
        }
        """),

    fx("fisheye", "Fisheye", DISTORT, "Barrel or pincushion lens warp.",
        listOf(P("Amount", -0.6f, 0.6f, 0.3f, bipolar = true)),
        """
        void main(){
            vec2 d = (v_TexCoord - 0.5) * 2.0;
            float f = 1.0 + dot(d, d) * u_p0;
            vec2 uv = 0.5 + d * f * 0.5;
            gl_FragColor = vec4(texture2D(u_Texture, uv).rgb, 1.0);
        }
        """),

    fx("glass", "Glass", DISTORT, "Refract through rippled frosted glass.",
        listOf(
            P("Amount", 0f, 0.1f, 0.03f),
            P("Scale", 2f, 30f, 10f),
        ),
        LIB_NOISE + """
        void main(){
            vec2 uv = v_TexCoord;
            float n1 = vnoise(uv * u_p1 + u_Time * 0.3);
            float n2 = vnoise(uv * u_p1 + 17.0 - u_Time * 0.2);
            uv += (vec2(n1, n2) - 0.5) * u_p0 * 2.0;
            gl_FragColor = vec4(texture2D(u_Texture, uv).rgb, 1.0);
        }
        """, animated = true),

    fx("kaleidoscope", "Kaleidoscope", DISTORT, "Mirror wedges into a mandala.",
        listOf(
            P("Segments", 2f, 24f, 6f),
            P("Rotation", 0f, 1f, 0f),
        ),
        """
        void main(){
            float aspect = u_Resolution.x / u_Resolution.y;
            vec2 uv = (v_TexCoord - 0.5) * vec2(aspect, 1.0);
            float a = atan(uv.y, uv.x);
            float r = length(uv);
            float seg = floor(max(2.0, u_p0));
            float k = 6.28318 / seg;
            a = mod(a, k);
            a = abs(a - k * 0.5);
            a += u_p1 * 6.28318;
            uv = vec2(cos(a), sin(a)) * r;
            uv = uv / vec2(aspect, 1.0) + 0.5;
            gl_FragColor = vec4(texture2D(u_Texture, uv).rgb, 1.0);
        }
        """),

    fx("mirror", "Mirror", DISTORT, "Fold the frame across an axis.",
        listOf(P("Mode", 0f, 3f, 0f)),
        """
        void main(){
            vec2 uv = v_TexCoord;
            float m = floor(u_p0 + 0.5);
            if (m < 0.5) { if (uv.x > 0.5) uv.x = 1.0 - uv.x; }
            else if (m < 1.5) { if (uv.x < 0.5) uv.x = 1.0 - uv.x; }
            else if (m < 2.5) { if (uv.y > 0.5) uv.y = 1.0 - uv.y; }
            else { if (uv.y < 0.5) uv.y = 1.0 - uv.y; }
            gl_FragColor = vec4(texture2D(u_Texture, uv).rgb, 1.0);
        }
        """),

    fx("shockwave", "Shockwave", DISTORT, "A ripple ring blasts outward.",
        listOf(
            P("Amplitude", 0f, 0.1f, 0.04f),
            P("Speed", 0.1f, 2f, 0.6f),
            P("Range", 0.2f, 1f, 0.8f),
        ),
        """
        void main(){
            vec2 uv = v_TexCoord;
            vec2 d = uv - 0.5;
            float r = length(d);
            float t = fract(u_Time * u_p1) * u_p2;
            float diff = r - t;
            float w = u_p0 * exp(-diff * diff * 200.0);
            uv += normalize(d + 1e-6) * w;
            gl_FragColor = vec4(texture2D(u_Texture, uv).rgb, 1.0);
        }
        """, animated = true),

    fx("zoompulse", "Zoom Pulse", DISTORT, "Breathing zoom in and out.",
        listOf(
            P("Amount", 0f, 0.5f, 0.15f),
            P("Speed", 0f, 8f, 3f),
        ),
        """
        void main(){
            float z = 1.0 + sin(u_Time * u_p1) * u_p0;
            vec2 uv = (v_TexCoord - 0.5) / z + 0.5;
            gl_FragColor = vec4(texture2D(u_Texture, uv).rgb, 1.0);
        }
        """, animated = true),

    fx("stretch", "Stretch", DISTORT, "Scale the axes independently.",
        listOf(
            P("Horizontal", 0.3f, 2f, 1f),
            P("Vertical", 0.3f, 2f, 1f),
        ),
        """
        void main(){
            vec2 uv = v_TexCoord;
            uv.x = (uv.x - 0.5) / u_p0 + 0.5;
            uv.y = (uv.y - 0.5) / u_p1 + 0.5;
            gl_FragColor = vec4(texture2D(u_Texture, uv).rgb, 1.0);
        }
        """),
)
