package com.glitchstudio.app.gl

import android.opengl.GLES20
import com.glitchstudio.app.effects.ShaderEffect

/**
 * A compiled GLSL program for one [ShaderEffect]. Wraps attribute/uniform
 * locations and draws the shared full-screen quad. Must be built and used on a
 * thread with a current EGL context.
 */
class ShaderProgram private constructor(
    private val program: Int,
    private val aPosition: Int,
    private val aTexCoord: Int,
    private val uTexture: Int,
    private val uResolution: Int,
    private val uTime: Int,
    private val paramLocations: IntArray,
) {

    fun draw(textureId: Int, values: FloatArray, width: Int, height: Int, time: Float) {
        GLES20.glUseProgram(program)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)
        GLES20.glUniform1i(uTexture, 0)

        if (uResolution >= 0) GLES20.glUniform2f(uResolution, width.toFloat(), height.toFloat())
        if (uTime >= 0) GLES20.glUniform1f(uTime, time)

        for (i in paramLocations.indices) {
            val loc = paramLocations[i]
            if (loc >= 0 && i < values.size) GLES20.glUniform1f(loc, values[i])
        }

        GLES20.glEnableVertexAttribArray(aPosition)
        GLES20.glVertexAttribPointer(aPosition, 2, GLES20.GL_FLOAT, false, 0, GlUtil.QUAD_POSITIONS)
        GLES20.glEnableVertexAttribArray(aTexCoord)
        GLES20.glVertexAttribPointer(aTexCoord, 2, GLES20.GL_FLOAT, false, 0, GlUtil.QUAD_TEXCOORDS)

        GLES20.glDrawArrays(GLES20.GL_TRIANGLE_STRIP, 0, 4)

        GLES20.glDisableVertexAttribArray(aPosition)
        GLES20.glDisableVertexAttribArray(aTexCoord)
    }

    fun release() {
        GLES20.glDeleteProgram(program)
    }

    companion object {
        const val VERTEX_SHADER = """
attribute vec4 a_Position;
attribute vec2 a_TexCoord;
varying vec2 v_TexCoord;
void main() {
    v_TexCoord = a_TexCoord;
    gl_Position = a_Position;
}
"""

        private const val FRAGMENT_HEADER = """
#ifdef GL_FRAGMENT_PRECISION_HIGH
precision highp float;
#else
precision mediump float;
#endif
varying vec2 v_TexCoord;
uniform sampler2D u_Texture;
uniform vec2 u_Resolution;
uniform float u_Time;
"""

        fun buildFragmentSource(effect: ShaderEffect): String {
            val sb = StringBuilder(FRAGMENT_HEADER)
            for (p in effect.params) sb.append("uniform float ${p.uniform};\n")
            sb.append('\n')
            sb.append(effect.fragmentBody)
            return sb.toString()
        }

        fun build(effect: ShaderEffect): ShaderProgram {
            val program = GlUtil.linkProgram(VERTEX_SHADER, buildFragmentSource(effect))
            val aPosition = GLES20.glGetAttribLocation(program, "a_Position")
            val aTexCoord = GLES20.glGetAttribLocation(program, "a_TexCoord")
            val uTexture = GLES20.glGetUniformLocation(program, "u_Texture")
            val uResolution = GLES20.glGetUniformLocation(program, "u_Resolution")
            val uTime = GLES20.glGetUniformLocation(program, "u_Time")
            val paramLocations = IntArray(effect.params.size) { i ->
                GLES20.glGetUniformLocation(program, effect.params[i].uniform)
            }
            return ShaderProgram(
                program, aPosition, aTexCoord, uTexture, uResolution, uTime, paramLocations
            )
        }
    }
}
