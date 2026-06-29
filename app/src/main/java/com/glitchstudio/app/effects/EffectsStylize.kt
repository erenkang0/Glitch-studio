package com.glitchstudio.app.effects

import com.glitchstudio.app.effects.EffectCategory.STYLIZE

private const val LIB_SOBEL = LIB_LUMA + """
float sobelMag(vec2 uv, vec2 px){
    float tl = luma(texture2D(u_Texture, uv + px * vec2(-1.0, -1.0)).rgb);
    float  t = luma(texture2D(u_Texture, uv + px * vec2( 0.0, -1.0)).rgb);
    float tr = luma(texture2D(u_Texture, uv + px * vec2( 1.0, -1.0)).rgb);
    float  l = luma(texture2D(u_Texture, uv + px * vec2(-1.0,  0.0)).rgb);
    float  r = luma(texture2D(u_Texture, uv + px * vec2( 1.0,  0.0)).rgb);
    float bl = luma(texture2D(u_Texture, uv + px * vec2(-1.0,  1.0)).rgb);
    float  b = luma(texture2D(u_Texture, uv + px * vec2( 0.0,  1.0)).rgb);
    float br = luma(texture2D(u_Texture, uv + px * vec2( 1.0,  1.0)).rgb);
    float gx = -tl - 2.0 * l - bl + tr + 2.0 * r + br;
    float gy = -tl - 2.0 * t - tr + bl + 2.0 * b + br;
    return length(vec2(gx, gy));
}
"""

internal val stylizeEffects: List<ShaderEffect> = listOf(

    fx("sharpen", "Sharpen", STYLIZE, "Crisp up detail with an unsharp mask.",
        listOf(P("Amount", 0f, 3f, 1f)),
        """
        void main(){
            vec2 uv = v_TexCoord;
            vec2 px = 1.0 / u_Resolution;
            vec3 c = texture2D(u_Texture, uv).rgb;
            vec3 n = texture2D(u_Texture, uv + vec2(px.x, 0.0)).rgb
                   + texture2D(u_Texture, uv - vec2(px.x, 0.0)).rgb
                   + texture2D(u_Texture, uv + vec2(0.0, px.y)).rgb
                   + texture2D(u_Texture, uv - vec2(0.0, px.y)).rgb;
            vec3 blur = n * 0.25;
            gl_FragColor = vec4(clamp(c + (c - blur) * u_p0, 0.0, 1.0), 1.0);
        }
        """),

    fx("sobel", "Edge Detect", STYLIZE, "Sobel edges, glowing on black.",
        listOf(
            P("Intensity", 0f, 4f, 1.6f),
            P("Thickness", 1f, 3f, 1f),
        ),
        LIB_SOBEL + """
        void main(){
            vec2 px = u_p1 / u_Resolution;
            float g = sobelMag(v_TexCoord, px) * u_p0;
            gl_FragColor = vec4(vec3(clamp(g, 0.0, 1.0)), 1.0);
        }
        """),

    fx("emboss", "Emboss", STYLIZE, "Carve the image into grey relief.",
        listOf(
            P("Strength", 0f, 5f, 2f),
            P("Size", 1f, 4f, 1f),
        ),
        LIB_LUMA + """
        void main(){
            vec2 px = u_p1 / u_Resolution;
            vec3 a = texture2D(u_Texture, v_TexCoord - px).rgb;
            vec3 b = texture2D(u_Texture, v_TexCoord + px).rgb;
            vec3 e = (b - a) * u_p0 + 0.5;
            gl_FragColor = vec4(vec3(luma(e)), 1.0);
        }
        """),

    fx("pixelate", "Pixelate", STYLIZE, "Square mosaic of large pixels.",
        listOf(P("Cells", 8f, 300f, 80f)),
        """
        void main(){
            float cx = floor(u_p0);
            float cy = floor(u_p0 * (u_Resolution.y / u_Resolution.x));
            vec2 cells = vec2(cx, max(1.0, cy));
            vec2 uv = (floor(v_TexCoord * cells) + 0.5) / cells;
            gl_FragColor = vec4(texture2D(u_Texture, uv).rgb, 1.0);
        }
        """),

    fx("hexpixelate", "Hex Pixelate", STYLIZE, "Honeycomb mosaic of hexagons.",
        listOf(P("Size", 10f, 140f, 44f)),
        """
        void main(){
            float aspect = u_Resolution.x / u_Resolution.y;
            vec2 p = v_TexCoord * vec2(aspect, 1.0) * u_p0;
            vec2 r = vec2(1.0, 1.7320508);
            vec2 h = r * 0.5;
            vec2 a = mod(p, r) - h;
            vec2 b = mod(p - h, r) - h;
            vec2 gv = dot(a, a) < dot(b, b) ? a : b;
            vec2 center = p - gv;
            vec2 uv = center / (vec2(aspect, 1.0) * u_p0);
            gl_FragColor = vec4(texture2D(u_Texture, uv).rgb, 1.0);
        }
        """),

    fx("halftone", "Halftone", STYLIZE, "Newsprint dots sized by brightness.",
        listOf(
            P("Scale", 20f, 220f, 90f),
            P("Angle", 0f, 1f, 0.12f),
        ),
        LIB_LUMA + """
        void main(){
            float a = u_p1 * 3.14159;
            mat2 rot = mat2(cos(a), -sin(a), sin(a), cos(a));
            float aspect = u_Resolution.x / u_Resolution.y;
            vec2 p = rot * (v_TexCoord * vec2(aspect, 1.0)) * u_p0;
            vec2 g = fract(p) - 0.5;
            float l = luma(texture2D(u_Texture, v_TexCoord).rgb);
            float radius = sqrt(1.0 - l) * 0.5;
            float d = length(g);
            float dot = smoothstep(radius, radius - 0.06, d);
            gl_FragColor = vec4(vec3(1.0 - dot), 1.0);
        }
        """),

    fx("crosshatch", "Crosshatch", STYLIZE, "Hand-inked hatching by tone.",
        listOf(P("Spacing", 4f, 22f, 9f)),
        LIB_LUMA + """
        void main(){
            vec2 fc = v_TexCoord * u_Resolution;
            float l = luma(texture2D(u_Texture, v_TexCoord).rgb);
            float c = 1.0;
            float s = u_p0;
            if (l < 0.85) { if (mod(fc.x + fc.y, s) < 1.5) c = 0.0; }
            if (l < 0.65) { if (mod(fc.x - fc.y, s) < 1.5) c = 0.0; }
            if (l < 0.45) { if (mod(fc.x + fc.y - s * 0.5, s) < 1.5) c = 0.0; }
            if (l < 0.25) { if (mod(fc.x - fc.y - s * 0.5, s) < 1.5) c = 0.0; }
            gl_FragColor = vec4(vec3(c), 1.0);
        }
        """),

    fx("sketch", "Pencil Sketch", STYLIZE, "Soft graphite strokes on white.",
        listOf(
            P("Strength", 1f, 8f, 4f),
            P("Thickness", 1f, 3f, 1f),
        ),
        LIB_SOBEL + """
        void main(){
            vec2 px = u_p1 / u_Resolution;
            float g = sobelMag(v_TexCoord, px) * u_p0;
            float v = 1.0 - clamp(g, 0.0, 1.0);
            gl_FragColor = vec4(vec3(v), 1.0);
        }
        """),

    fx("cartoon", "Cartoon", STYLIZE, "Flat colour cells with inked edges.",
        listOf(
            P("Edge", 0f, 1f, 0.35f),
            P("Levels", 2f, 10f, 5f),
        ),
        LIB_SOBEL + """
        void main(){
            vec2 uv = v_TexCoord;
            vec3 c = texture2D(u_Texture, uv).rgb;
            float n = floor(u_p1);
            c = floor(c * n) / (n - 1.0);
            float g = sobelMag(uv, 1.0 / u_Resolution);
            float edge = 1.0 - smoothstep(u_p0, u_p0 + 0.12, g);
            gl_FragColor = vec4(clamp(c, 0.0, 1.0) * edge, 1.0);
        }
        """),

    fx("oil", "Oil Paint", STYLIZE, "Kuwahara filter for painterly strokes.",
        listOf(P("Radius", 1f, 5f, 2.5f)),
        LIB_LUMA + """
        vec3 region(vec2 uv, vec2 px, int x0, int x1, int y0, int y1, out float sigma){
            vec3 mean = vec3(0.0);
            vec3 sq = vec3(0.0);
            float n = 0.0;
            for (int j = -4; j <= 4; j++){
                for (int i = -4; i <= 4; i++){
                    if (i < x0 || i > x1 || j < y0 || j > y1) continue;
                    vec3 s = texture2D(u_Texture, uv + vec2(float(i), float(j)) * px).rgb;
                    mean += s; sq += s * s; n += 1.0;
                }
            }
            mean /= n; sq /= n;
            vec3 v = sq - mean * mean;
            sigma = v.r + v.g + v.b;
            return mean;
        }
        void main(){
            vec2 uv = v_TexCoord;
            vec2 px = u_p0 / u_Resolution;
            float s0, s1, s2, s3;
            vec3 m0 = region(uv, px, -4, 0, -4, 0, s0);
            vec3 m1 = region(uv, px,  0, 4, -4, 0, s1);
            vec3 m2 = region(uv, px, -4, 0,  0, 4, s2);
            vec3 m3 = region(uv, px,  0, 4,  0, 4, s3);
            vec3 res = m0; float sm = s0;
            if (s1 < sm){ sm = s1; res = m1; }
            if (s2 < sm){ sm = s2; res = m2; }
            if (s3 < sm){ sm = s3; res = m3; }
            gl_FragColor = vec4(res, 1.0);
        }
        """),

    fx("dither", "Ordered Dither", STYLIZE, "Bayer-matrix retro dithering.",
        listOf(P("Levels", 2f, 8f, 3f)),
        """
        void main(){
            vec2 fc = floor(v_TexCoord * u_Resolution);
            mat4 m = mat4(
                 0.0,  8.0,  2.0, 10.0,
                12.0,  4.0, 14.0,  6.0,
                 3.0, 11.0,  1.0,  9.0,
                15.0,  7.0, 13.0,  5.0);
            int x = int(mod(fc.x, 4.0));
            int y = int(mod(fc.y, 4.0));
            float threshold = 0.0;
            for (int j = 0; j < 4; j++){
                for (int i = 0; i < 4; i++){
                    if (i == x && j == y) threshold = m[j][i];
                }
            }
            threshold = (threshold + 0.5) / 16.0;
            vec3 c = texture2D(u_Texture, v_TexCoord).rgb;
            float levels = max(2.0, floor(u_p0));
            vec3 d = c + (threshold - 0.5) / levels;
            vec3 q = floor(d * (levels - 1.0) + 0.5) / (levels - 1.0);
            gl_FragColor = vec4(clamp(q, 0.0, 1.0), 1.0);
        }
        """),

    fx("thermal", "Thermal", STYLIZE, "False-colour infrared palette.",
        listOf(P("Amount", 0f, 1f, 1f)),
        LIB_LUMA + """
        vec3 heat(float t){
            vec3 c;
            c.r = clamp(t * 3.0 - 0.5, 0.0, 1.0);
            c.g = clamp(t * 3.0 - 1.3, 0.0, 1.0);
            c.b = clamp(sin(t * 3.14159) * 0.7, 0.0, 1.0) + clamp(t * 3.0 - 2.0, 0.0, 1.0);
            return clamp(c, 0.0, 1.0);
        }
        void main(){
            vec3 c = texture2D(u_Texture, v_TexCoord).rgb;
            vec3 h = heat(luma(c));
            gl_FragColor = vec4(mix(c, h, u_p0), 1.0);
        }
        """),

    fx("outline", "Outline", STYLIZE, "Dark contour lines over the photo.",
        listOf(
            P("Strength", 1f, 6f, 3f),
            P("Threshold", 0f, 0.6f, 0.2f),
        ),
        LIB_SOBEL + """
        void main(){
            vec2 uv = v_TexCoord;
            float g = sobelMag(uv, 1.0 / u_Resolution) * u_p0;
            float e = smoothstep(u_p1, u_p1 + 0.05, g);
            vec3 c = texture2D(u_Texture, uv).rgb;
            gl_FragColor = vec4(mix(c, vec3(0.0), e), 1.0);
        }
        """),
)
