package ru.redbyte.redbytefx.gl

import android.opengl.GLES30

/**
 * OpenGL ES 3.0 driver calls.
 *
 * Every method must run on the EGL context thread. [GlProgramRuntime] enforces that
 * before it touches this device. This class does not decide that a link succeeded;
 * it returns the status the driver wrote.
 */
public class Gles30Device : GlDevice() {
    private val statusSlot = IntArray(1)

    override fun createShader(stage: GlStage): Int = GLES30.glCreateShader(stageEnum(stage))

    override fun shaderSource(shader: Int, source: String) {
        GLES30.glShaderSource(shader, source)
    }

    override fun compileShader(shader: Int): GlCompileStatus {
        GLES30.glCompileShader(shader)
        GLES30.glGetShaderiv(shader, GLES30.GL_COMPILE_STATUS, statusSlot, 0)
        return GlCompileStatus(statusSlot[0] == GLES30.GL_TRUE, GLES30.glGetShaderInfoLog(shader).orEmpty())
    }

    override fun deleteShader(shader: Int) {
        GLES30.glDeleteShader(shader)
    }

    override fun createProgram(): Int = GLES30.glCreateProgram()

    override fun attachShader(program: Int, shader: Int) {
        GLES30.glAttachShader(program, shader)
    }

    override fun linkProgram(program: Int): GlCompileStatus {
        GLES30.glLinkProgram(program)
        GLES30.glGetProgramiv(program, GLES30.GL_LINK_STATUS, statusSlot, 0)
        return GlCompileStatus(statusSlot[0] == GLES30.GL_TRUE, GLES30.glGetProgramInfoLog(program).orEmpty())
    }

    override fun deleteProgram(program: Int) {
        GLES30.glDeleteProgram(program)
    }

    override fun uniformLocation(program: Int, name: String): Int = GLES30.glGetUniformLocation(program, name)

    override fun uniform1f(location: Int, value: Float) {
        GLES30.glUniform1f(location, value)
    }

    override fun uniform1i(location: Int, value: Int) {
        GLES30.glUniform1i(location, value)
    }

    override fun useProgram(program: Int) {
        GLES30.glUseProgram(program)
    }

    override fun activeTexture(unit: Int) {
        GLES30.glActiveTexture(GLES30.GL_TEXTURE0 + unit)
    }

    override fun bindTexture2D(texture: Int) {
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, texture)
    }
}

private fun stageEnum(stage: GlStage): Int = when (stage) {
    GlStage.Vertex -> GLES30.GL_VERTEX_SHADER
    GlStage.Fragment -> GLES30.GL_FRAGMENT_SHADER
}
