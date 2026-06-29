package com.glitchstudio.app.effects

import com.glitchstudio.app.effects.EffectCategory.BLUR

internal val blurEffects: List<ShaderEffect> = listOf(

    fx("gaussian", "Gaussian Blur", BLUR, "Smooth, weighted softening.",
        listOf(P("Radius", 0f, 8f, 3f)),
        """
        void main(){
            vec2 uv = v_TexCoord;
            vec2 px = u_p0 / u_Resolution;
            vec3 sum = vec3(0.0);
            float wsum = 0.0;
            for (int j = -3; j <= 3; j++){
                for (int i = -3; i <= 3; i++){
                    float w = exp(-float(i * i + j * j) / 8.0);
                    sum += texture2D(u_Texture, uv + vec2(float(i), float(j)) * px).rgb * w;
                    wsum += w;
                }
            }
            gl_FragColor = vec4(sum / wsum, 1.0);
        }
        """),

    fx("boxblur", "Box Blur", BLUR, "Fast uniform averaging blur.",
        listOf(P("Radius", 0f, 10f, 3f)),
        """
        void main(){
            vec2 uv = v_TexCoord;
            vec2 px = u_p0 / u_Resolution;
            vec3 sum = vec3(0.0);
            for (int j = -2; j <= 2; j++){
                for (int i = -2; i <= 2; i++){
                    sum += texture2D(u_Texture, uv + vec2(float(i), float(j)) * px).rgb;
                }
            }
            gl_FragColor = vec4(sum / 25.0, 1.0);
        }
        """),

    fx("zoomblur", "Zoom Blur", BLUR, "Radial streaks rushing to centre.",
        listOf(P("Amount", 0f, 0.5f, 0.15f)),
        """
        void main(){
            vec2 uv = v_TexCoord;
            vec2 dir = 0.5 - uv;
            vec3 sum = vec3(0.0);
            for (int i = 0; i < 16; i++){
                float t = float(i) / 15.0;
                sum += texture2D(u_Texture, uv + dir * t * u_p0).rgb;
            }
            gl_FragColor = vec4(sum / 16.0, 1.0);
        }
        """),

    fx("spinblur", "Spin Blur", BLUR, "Rotational motion around centre.",
        listOf(P("Amount", 0f, 1f, 0.2f)),
        """
        void main(){
            vec2 c = vec2(0.5);
            vec2 d = v_TexCoord - c;
            float r = length(d);
            float a0 = atan(d.y, d.x);
            vec3 sum = vec3(0.0);
            for (int i = 0; i < 16; i++){
                float a = a0 + (float(i) / 15.0 - 0.5) * u_p0;
                sum += texture2D(u_Texture, c + vec2(cos(a), sin(a)) * r).rgb;
            }
            gl_FragColor = vec4(sum / 16.0, 1.0);
        }
        """),

    fx("motionblur", "Motion Blur", BLUR, "Directional smear along an angle.",
        listOf(
            P("Length", 0f, 0.2f, 0.05f),
            P("Angle", 0f, 1f, 0f),
        ),
        """
        void main(){
            vec2 uv = v_TexCoord;
            float ang = u_p1 * 3.14159;
            vec2 dir = vec2(cos(ang), sin(ang));
            vec3 sum = vec3(0.0);
            for (int i = 0; i < 16; i++){
                float t = float(i) / 15.0 - 0.5;
                sum += texture2D(u_Texture, uv + dir * t * u_p0).rgb;
            }
            gl_FragColor = vec4(sum / 16.0, 1.0);
        }
        """),

    fx("tiltshift", "Tilt Shift", BLUR, "Sharp focus band, blurred elsewhere.",
        listOf(
            P("Strength", 0f, 10f, 5f),
            P("Focus", 0f, 1f, 0.5f),
            P("Width", 0f, 0.5f, 0.15f),
        ),
        """
        void main(){
            vec2 uv = v_TexCoord;
            float band = abs(uv.y - u_p1);
            float amt = smoothstep(u_p2, u_p2 + 0.2, band) * u_p0;
            vec2 px = amt / u_Resolution;
            vec3 sum = vec3(0.0);
            float wsum = 0.0;
            for (int j = -3; j <= 3; j++){
                for (int i = -3; i <= 3; i++){
                    float w = exp(-float(i * i + j * j) / 8.0);
                    sum += texture2D(u_Texture, uv + vec2(float(i), float(j)) * px).rgb * w;
                    wsum += w;
                }
            }
            gl_FragColor = vec4(sum / wsum, 1.0);
        }
        """),
)
