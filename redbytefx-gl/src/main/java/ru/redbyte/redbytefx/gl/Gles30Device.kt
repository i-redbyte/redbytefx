package ru.redbyte.redbytefx.gl

import android.util.Log
import android.opengl.GLES30
import android.opengl.GLES31
import android.opengl.GLES32
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * OpenGL ES 3.0 driver calls, plus the ES 3.1 compute shader and storage buffer calls.
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

    override fun uniform2f(location: Int, x: Float, y: Float) {
        GLES30.glUniform2f(location, x, y)
    }

    override fun uniform3f(location: Int, x: Float, y: Float, z: Float) {
        GLES30.glUniform3f(location, x, y, z)
    }

    override fun uniform4f(location: Int, x: Float, y: Float, z: Float, w: Float) {
        GLES30.glUniform4f(location, x, y, z, w)
    }

    override fun uniform1i(location: Int, value: Int) {
        GLES30.glUniform1i(location, value)
    }

    override fun uniformMatrix2fv(location: Int, values: FloatArray) {
        GLES30.glUniformMatrix2fv(location, 1, false, values, 0)
    }

    override fun uniformMatrix3fv(location: Int, values: FloatArray) {
        GLES30.glUniformMatrix3fv(location, 1, false, values, 0)
    }

    override fun uniformMatrix4fv(location: Int, values: FloatArray) {
        GLES30.glUniformMatrix4fv(location, 1, false, values, 0)
    }

    override fun maxCombinedTextureImageUnits(): Int {
        GLES30.glGetIntegerv(GLES30.GL_MAX_COMBINED_TEXTURE_IMAGE_UNITS, statusSlot, 0)
        return statusSlot[0]
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

    override fun bindTextureCube(texture: Int) {
        GLES30.glBindTexture(GLES30.GL_TEXTURE_CUBE_MAP, texture)
    }

    override fun dispatchCompute(x: Int, y: Int, z: Int) {
        GLES31.glDispatchCompute(x, y, z)
    }

    override fun shaderStorageBarrier() {
        GLES31.glMemoryBarrier(GLES31.GL_SHADER_STORAGE_BARRIER_BIT)
    }

    override fun createBuffer(): Int {
        GLES30.glGenBuffers(1, statusSlot, 0)
        return statusSlot[0]
    }

    override fun deleteBuffer(buffer: Int) {
        statusSlot[0] = buffer
        GLES30.glDeleteBuffers(1, statusSlot, 0)
    }

    override fun uniformBufferData(buffer: Int, data: ByteArray) {
        GLES30.glBindBuffer(GLES30.GL_UNIFORM_BUFFER, buffer)
        GLES30.glBufferData(
            GLES30.GL_UNIFORM_BUFFER,
            data.size,
            data.asNativeBuffer(),
            GLES30.GL_DYNAMIC_DRAW,
        )
    }

    override fun uniformBufferSubData(buffer: Int, data: ByteArray) {
        GLES30.glBindBuffer(GLES30.GL_UNIFORM_BUFFER, buffer)
        GLES30.glBufferSubData(GLES30.GL_UNIFORM_BUFFER, 0, data.size, data.asNativeBuffer())
    }

    override fun bindUniformBufferBase(buffer: Int, binding: Int) {
        GLES30.glBindBufferBase(GLES30.GL_UNIFORM_BUFFER, binding, buffer)
    }

    override fun uniformBlockIndex(program: Int, name: String): Int =
        GLES30.glGetUniformBlockIndex(program, name)

    override fun uniformBlockBinding(program: Int, blockIndex: Int, binding: Int) {
        GLES30.glUniformBlockBinding(program, blockIndex, binding)
    }

    override fun shaderStorageData(buffer: Int, data: ByteArray) {
        GLES31.glBindBuffer(GLES31.GL_SHADER_STORAGE_BUFFER, buffer)
        GLES31.glBufferData(
            GLES31.GL_SHADER_STORAGE_BUFFER,
            data.size,
            data.asNativeBuffer(),
            GLES31.GL_DYNAMIC_DRAW,
        )
    }

    override fun shaderStorageSubData(buffer: Int, data: ByteArray) {
        GLES31.glBindBuffer(GLES31.GL_SHADER_STORAGE_BUFFER, buffer)
        GLES31.glBufferSubData(GLES31.GL_SHADER_STORAGE_BUFFER, 0, data.size, data.asNativeBuffer())
    }

    override fun bindShaderStorageBase(buffer: Int, binding: Int) {
        GLES31.glBindBufferBase(GLES31.GL_SHADER_STORAGE_BUFFER, binding, buffer)
    }

    override fun flushGlErrors(context: String) {
        var error = GLES30.glGetError()
        while (error != GLES30.GL_NO_ERROR) {
            Log.w(LOG_TAG, "OpenGL error 0x${Integer.toHexString(error)} after $context")
            error = GLES30.glGetError()
        }
    }
}

private const val LOG_TAG = "RedByteFX"

private val directScratch = ThreadLocal<ByteBuffer>()

/**
 * `glBufferData` reads a direct buffer. A heap [ByteBuffer.wrap] does not qualify and is rejected
 * by the Android GLES bindings.
 */
private fun ByteArray.asNativeBuffer(): ByteBuffer {
    val existing = directScratch.get()
    val buffer = if (existing != null && existing.capacity() >= size) {
        existing
    } else {
        ByteBuffer.allocateDirect(size).also { directScratch.set(it) }
    }
    buffer.order(ByteOrder.nativeOrder())
    buffer.clear()
    buffer.limit(size)
    buffer.put(this)
    buffer.position(0)
    return buffer
}

private fun stageEnum(stage: GlStage): Int = when (stage) {
    GlStage.Vertex -> GLES30.GL_VERTEX_SHADER
    GlStage.Fragment -> GLES30.GL_FRAGMENT_SHADER
    GlStage.TessControl -> GLES32.GL_TESS_CONTROL_SHADER
    GlStage.TessEval -> GLES32.GL_TESS_EVALUATION_SHADER
    GlStage.Geometry -> GLES32.GL_GEOMETRY_SHADER
    GlStage.Compute -> GLES31.GL_COMPUTE_SHADER
}
