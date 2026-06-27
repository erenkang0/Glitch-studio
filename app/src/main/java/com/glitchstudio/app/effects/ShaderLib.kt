package com.glitchstudio.app.effects

/**
 * Shared GLSL building blocks. Every effect's fragment shader is assembled as
 * HEADER + effect body + MAIN, so each effect only needs to provide a
 * `vec4 process(vec2 uv)` function.
 */
object ShaderLib {

    const val VERTEX = """
attribute vec2 aPos;
attribute vec2 aTex;
varying vec2 vUv;
void main() {
    vUv = aTex;
    gl_Position = vec4(aPos, 0.0, 1.0);
}
"""

    const val HEADER = """
precision highp float;

uniform sampler2D uTex;
uniform sampler2D uMaskTex;  // painted brush mask (red channel), unit 1
uniform vec2  uResolution;   // size of the rendered image region, in pixels
uniform float uAspect;       // width / height of the rendered region
uniform float uTime;         // seconds, used by animated effects
uniform float p0;
uniform float p1;
uniform float p2;
uniform float p3;
uniform float p4;
uniform float p5;
uniform float p6;
uniform float p7;

// Layer compositing controls (set per layer by the renderer).
uniform float uOpacity;
uniform float uMaskType;     // 0 none, 1 radial, 2 linear
uniform vec2  uMaskCenter;
uniform float uMaskSize;
uniform float uMaskFeather;
uniform float uMaskAngle;
uniform float uMaskInvert;

varying vec2 vUv;

const float PI  = 3.14159265359;
const float TAU = 6.28318530718;

vec4 texA(vec2 uv) { return texture2D(uTex, clamp(uv, 0.0, 1.0)); }
vec3 tex(vec2 uv)  { return texA(uv).rgb; }

float luma(vec3 c) { return dot(c, vec3(0.299, 0.587, 0.114)); }

mat2 rot(float a) {
    float s = sin(a), c = cos(a);
    return mat2(c, -s, s, c);
}

float hash11(float p) {
    p = fract(p * 0.1031);
    p *= p + 33.33;
    p *= p + p;
    return fract(p);
}

float hash21(vec2 p) {
    vec3 p3 = fract(vec3(p.xyx) * 0.1031);
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.x + p3.y) * p3.z);
}

float vnoise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    float a = hash21(i);
    float b = hash21(i + vec2(1.0, 0.0));
    float c = hash21(i + vec2(0.0, 1.0));
    float d = hash21(i + vec2(1.0, 1.0));
    return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);
}

float fbm(vec2 p) {
    float v = 0.0;
    float a = 0.5;
    for (int i = 0; i < 5; i++) {
        v += a * vnoise(p);
        p *= 2.0;
        a *= 0.5;
    }
    return v;
}

vec3 rgb2hsv(vec3 c) {
    vec4 K = vec4(0.0, -1.0 / 3.0, 2.0 / 3.0, -1.0);
    vec4 p = mix(vec4(c.bg, K.wz), vec4(c.gb, K.xy), step(c.b, c.g));
    vec4 q = mix(vec4(p.xyw, c.r), vec4(c.r, p.yzx), step(p.x, c.r));
    float d = q.x - min(q.w, q.y);
    float e = 1.0e-10;
    return vec3(abs(q.z + (q.w - q.y) / (6.0 * d + e)), d / (q.x + e), q.x);
}

vec3 hsv2rgb(vec3 c) {
    vec4 K = vec4(1.0, 2.0 / 3.0, 1.0 / 3.0, 3.0);
    vec3 p = abs(fract(c.xxx + K.xyz) * 6.0 - K.www);
    return c.z * mix(K.xxx, clamp(p - K.xxx, 0.0, 1.0), c.y);
}

// Centered, aspect-corrected coordinates (~ -0.5..0.5 on the short axis).
vec2 centered(vec2 uv) {
    vec2 p = uv - 0.5;
    p.x *= uAspect;
    return p;
}
vec2 uncentered(vec2 p) {
    p.x /= uAspect;
    return p + 0.5;
}

// Per-layer mask: 1.0 where the effect applies, 0.0 where the input shows through.
// uMaskType: 0 none, 1 radial, 2 linear, 3 brush (painted texture).
float maskValue(vec2 uv) {
    if (uMaskType < 0.5) return 1.0;
    float m;
    if (uMaskType > 2.5) {
        m = texture2D(uMaskTex, uv).r;
    } else {
        vec2 p = uv - uMaskCenter;
        if (uMaskType < 1.5) {
            float d = length(vec2(p.x * uAspect, p.y));
            m = 1.0 - smoothstep(uMaskSize, uMaskSize + uMaskFeather + 0.001, d);
        } else {
            vec2 dir = vec2(cos(uMaskAngle), sin(uMaskAngle));
            float d = dot(p, dir);
            m = smoothstep(-uMaskFeather - 0.001, uMaskFeather + 0.001, d);
        }
    }
    if (uMaskInvert > 0.5) m = 1.0 - m;
    return clamp(m, 0.0, 1.0);
}
"""

    /**
     * Composites the effect over the layer input using the mask and opacity, so
     * a stack of effect layers can be applied via ping-pong framebuffers.
     */
    const val MAIN = """
void main() {
    vec3 src = tex(vUv);
    vec4 fx = process(vUv);
    float m = maskValue(vUv) * uOpacity;
    gl_FragColor = vec4(mix(src, fx.rgb, m), 1.0);
}
"""
}
