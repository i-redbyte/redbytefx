package ru.redbyte.redbytefx.gl

import android.opengl.GLES30
import android.opengl.GLES31
import android.opengl.GLES32
import android.util.Log
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
    private var pixelReadScratch: ByteBuffer? = null

    internal override fun releaseScratch() {
        pixelReadScratch = null
        directScratch.remove()
    }

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

    override fun attribLocation(program: Int, name: String): Int = GLES30.glGetAttribLocation(program, name)

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

    override fun createTexture(): Int {
        GLES30.glGenTextures(1, statusSlot, 0)
        return statusSlot[0]
    }

    override fun deleteTexture(texture: Int) {
        statusSlot[0] = texture
        GLES30.glDeleteTextures(1, statusSlot, 0)
    }

    override fun texture2DLinearRepeat(texture: Int) {
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, texture)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_REPEAT)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_REPEAT)
    }

    override fun texImage2DRgba(texture: Int, width: Int, height: Int, rgba: ByteArray) {
        require(width > 0 && height > 0) { "Texture size must be positive, was ${width}x$height" }
        val expected = rgbaByteCount(width, height)
        require(rgba.size == expected) {
            "RGBA texture needs $expected bytes, was ${rgba.size}"
        }
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, texture)
        GLES30.glPixelStorei(GLES30.GL_UNPACK_ALIGNMENT, 1)
        GLES30.glTexImage2D(
            GLES30.GL_TEXTURE_2D,
            0,
            GLES30.GL_RGBA,
            width,
            height,
            0,
            GLES30.GL_RGBA,
            GLES30.GL_UNSIGNED_BYTE,
            ByteBuffer.wrap(rgba),
        )
        GLES30.glPixelStorei(GLES30.GL_UNPACK_ALIGNMENT, 4)
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

    override fun maxUniformBufferBindings(): Int {
        GLES30.glGetIntegerv(GLES30.GL_MAX_UNIFORM_BUFFER_BINDINGS, statusSlot, 0)
        return statusSlot[0]
    }

    override fun maxShaderStorageBufferBindings(): Int {
        if (!esAtLeast(3, 1)) return 0
        GLES31.glGetIntegerv(GLES31.GL_MAX_SHADER_STORAGE_BUFFER_BINDINGS, statusSlot, 0)
        return statusSlot[0]
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

    override fun bufferUpdateBarrier() {
        GLES31.glMemoryBarrier(GLES31.GL_BUFFER_UPDATE_BARRIER_BIT)
    }

    override fun mapShaderStorageRead(buffer: Int, bytes: Int): ByteBuffer {
        GLES31.glBindBuffer(GLES31.GL_SHADER_STORAGE_BUFFER, buffer)
        val mapped = GLES31.glMapBufferRange(
            GLES31.GL_SHADER_STORAGE_BUFFER,
            0,
            bytes,
            GLES31.GL_MAP_READ_BIT,
        ) as? ByteBuffer
        check(mapped != null) {
            "Driver did not map the storage buffer, error ${GLES31.glGetError()}"
        }
        return mapped.order(ByteOrder.nativeOrder())
    }

    override fun unmapShaderStorage(buffer: Int) {
        GLES31.glBindBuffer(GLES31.GL_SHADER_STORAGE_BUFFER, buffer)
        check(GLES31.glUnmapBuffer(GLES31.GL_SHADER_STORAGE_BUFFER)) {
            "Driver failed to unmap the storage buffer, error ${GLES31.glGetError()}"
        }
    }

    override fun drawArrays(mode: Int, first: Int, count: Int) {
        GLES30.glDrawArrays(mode, first, count)
    }

    override fun drawElements(mode: Int, count: Int, unsignedInt: Boolean, indexOffset: Int) {
        val type = if (unsignedInt) GLES30.GL_UNSIGNED_INT else GLES30.GL_UNSIGNED_SHORT
        val width = if (unsignedInt) Int.SIZE_BYTES else Short.SIZE_BYTES
        GLES30.glDrawElements(mode, count, type, indexOffset * width)
    }

    override fun drawArraysInstanced(mode: Int, first: Int, count: Int, instances: Int) {
        GLES30.glDrawArraysInstanced(mode, first, count, instances)
    }

    override fun drawElementsInstanced(
        mode: Int,
        count: Int,
        unsignedInt: Boolean,
        instances: Int,
        indexOffset: Int,
    ) {
        val type = if (unsignedInt) GLES30.GL_UNSIGNED_INT else GLES30.GL_UNSIGNED_SHORT
        val width = if (unsignedInt) Int.SIZE_BYTES else Short.SIZE_BYTES
        GLES30.glDrawElementsInstanced(mode, count, type, indexOffset * width, instances)
    }

    override fun arrayBufferData(buffer: Int, data: FloatArray) {
        val bytes = data.asNativeBuffer()
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, buffer)
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, bytes.remaining(), bytes, GLES30.GL_DYNAMIC_DRAW)
    }

    override fun arrayBufferSubData(buffer: Int, data: FloatArray) {
        val bytes = data.asNativeBuffer()
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, buffer)
        GLES30.glBufferSubData(GLES30.GL_ARRAY_BUFFER, 0, bytes.remaining(), bytes)
    }

    override fun unbindVertexArray() {
        GLES30.glBindVertexArray(0)
    }

    override fun elementBufferData(buffer: Int, indices: IntArray, unsignedInt: Boolean) {
        val bytes = if (unsignedInt) indices.asNativeIntBuffer() else indices.asNativeShortBuffer()
        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, buffer)
        GLES30.glBufferData(GLES30.GL_ELEMENT_ARRAY_BUFFER, bytes.remaining(), bytes, GLES30.GL_DYNAMIC_DRAW)
    }

    override fun texSubImage2DRgba(
        texture: Int,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        rgba: ByteArray,
    ) {
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, texture)
        GLES30.glPixelStorei(GLES30.GL_UNPACK_ALIGNMENT, 1)
        GLES30.glTexSubImage2D(
            GLES30.GL_TEXTURE_2D,
            0,
            x,
            y,
            width,
            height,
            GLES30.GL_RGBA,
            GLES30.GL_UNSIGNED_BYTE,
            ByteBuffer.wrap(rgba),
        )
        GLES30.glPixelStorei(GLES30.GL_UNPACK_ALIGNMENT, 4)
    }

    override fun texImageCubeFace(texture: Int, face: CubeFace, width: Int, height: Int, rgba: ByteArray) {
        GLES30.glBindTexture(GLES30.GL_TEXTURE_CUBE_MAP, texture)
        GLES30.glPixelStorei(GLES30.GL_UNPACK_ALIGNMENT, 1)
        GLES30.glTexImage2D(
            cubeTarget(face),
            0,
            GLES30.GL_RGBA,
            width,
            height,
            0,
            GLES30.GL_RGBA,
            GLES30.GL_UNSIGNED_BYTE,
            ByteBuffer.wrap(rgba),
        )
        GLES30.glPixelStorei(GLES30.GL_UNPACK_ALIGNMENT, 4)
    }

    override fun textureCubeLinearClamp(texture: Int) {
        GLES30.glBindTexture(GLES30.GL_TEXTURE_CUBE_MAP, texture)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_CUBE_MAP, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_CUBE_MAP, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_CUBE_MAP, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_CUBE_MAP, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_CUBE_MAP, GLES30.GL_TEXTURE_WRAP_R, GLES30.GL_CLAMP_TO_EDGE)
    }

    override fun texture2DLinearClamp(texture: Int) {
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, texture)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MAG_FILTER, GLES30.GL_LINEAR)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_S, GLES30.GL_CLAMP_TO_EDGE)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_WRAP_T, GLES30.GL_CLAMP_TO_EDGE)
    }

    override fun generateMipmap2D(texture: Int) {
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, texture)
        GLES30.glGenerateMipmap(GLES30.GL_TEXTURE_2D)
    }

    override fun filterMipmap2D(texture: Int) {
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, texture)
        GLES30.glTexParameteri(GLES30.GL_TEXTURE_2D, GLES30.GL_TEXTURE_MIN_FILTER, GLES30.GL_LINEAR_MIPMAP_LINEAR)
    }

    override fun texImage2DRgbaAlloc(texture: Int, width: Int, height: Int) {
        GLES30.glBindTexture(GLES30.GL_TEXTURE_2D, texture)
        GLES30.glTexImage2D(
            GLES30.GL_TEXTURE_2D,
            0,
            GLES30.GL_RGBA,
            width,
            height,
            0,
            GLES30.GL_RGBA,
            GLES30.GL_UNSIGNED_BYTE,
            null,
        )
    }

    override fun createFramebuffer(): Int {
        GLES30.glGenFramebuffers(1, statusSlot, 0)
        return statusSlot[0]
    }

    override fun deleteFramebuffer(framebuffer: Int) {
        statusSlot[0] = framebuffer
        GLES30.glDeleteFramebuffers(1, statusSlot, 0)
    }

    override fun bindFramebuffer(framebuffer: Int) {
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, framebuffer)
    }

    override fun readPixelsRgba(x: Int, y: Int, width: Int, height: Int, rgba: ByteArray) {
        val needed = rgbaByteCount(width, height)
        require(rgba.size >= needed) { "Pixel read needs $needed bytes, was ${rgba.size}" }
        val previous = pixelReadScratch
        val buffer = if (previous != null && previous.capacity() >= needed) {
            previous
        } else {
            ByteBuffer.allocateDirect(needed).order(ByteOrder.nativeOrder()).also { pixelReadScratch = it }
        }
        buffer.clear()
        buffer.limit(needed)
        GLES30.glReadPixels(x, y, width, height, GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, buffer)
        buffer.position(0)
        buffer.get(rgba, 0, needed)
    }

    override fun createRenderbuffer(): Int {
        GLES30.glGenRenderbuffers(1, statusSlot, 0)
        return statusSlot[0]
    }

    override fun deleteRenderbuffer(renderbuffer: Int) {
        statusSlot[0] = renderbuffer
        GLES30.glDeleteRenderbuffers(1, statusSlot, 0)
    }

    override fun framebufferColor(framebuffer: Int, texture: Int) {
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, framebuffer)
        GLES30.glFramebufferTexture2D(
            GLES30.GL_FRAMEBUFFER,
            GLES30.GL_COLOR_ATTACHMENT0,
            GLES30.GL_TEXTURE_2D,
            texture,
            0,
        )
    }

    override fun framebufferDepth(framebuffer: Int, renderbuffer: Int, width: Int, height: Int) {
        GLES30.glBindRenderbuffer(GLES30.GL_RENDERBUFFER, renderbuffer)
        GLES30.glRenderbufferStorage(GLES30.GL_RENDERBUFFER, GLES30.GL_DEPTH_COMPONENT16, width, height)
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, framebuffer)
        GLES30.glFramebufferRenderbuffer(
            GLES30.GL_FRAMEBUFFER,
            GLES30.GL_DEPTH_ATTACHMENT,
            GLES30.GL_RENDERBUFFER,
            renderbuffer,
        )
    }

    override fun framebufferComplete(framebuffer: Int): Boolean {
        GLES30.glBindFramebuffer(GLES30.GL_FRAMEBUFFER, framebuffer)
        return GLES30.glCheckFramebufferStatus(GLES30.GL_FRAMEBUFFER) == GLES30.GL_FRAMEBUFFER_COMPLETE
    }

    override fun vertexAttribDivisor(location: Int, divisor: Int) {
        GLES30.glVertexAttribDivisor(location, divisor)
    }

    override fun disableVertexAttribArray(location: Int) {
        GLES30.glDisableVertexAttribArray(location)
    }

    override fun vertexAttribFloat(location: Int, size: Int, strideFloats: Int, offsetFloats: Int) {
        GLES30.glEnableVertexAttribArray(location)
        GLES30.glVertexAttribPointer(
            location,
            size,
            GLES30.GL_FLOAT,
            false,
            strideFloats * Float.SIZE_BYTES,
            offsetFloats * Float.SIZE_BYTES,
        )
    }

    override fun flushGlErrors(context: String) {
        var error = GLES30.glGetError()
        while (error != GLES30.GL_NO_ERROR) {
            Log.w(LOG_TAG, "OpenGL error 0x${Integer.toHexString(error)} after $context")
            error = GLES30.glGetError()
        }
    }

    override fun takeGlError(): Int {
        var found = GLES30.GL_NO_ERROR
        var error = GLES30.glGetError()
        while (error != GLES30.GL_NO_ERROR) {
            if (found == GLES30.GL_NO_ERROR) found = error
            error = GLES30.glGetError()
        }
        return found
    }

    /** GLES 3.1+ only. ES 3.0 must not query `GL_MAX_SHADER_STORAGE_BUFFER_BINDINGS`. */
    private fun esAtLeast(major: Int, minor: Int): Boolean {
        GLES30.glGetIntegerv(GLES30.GL_MAJOR_VERSION, statusSlot, 0)
        val gotMajor = statusSlot[0]
        GLES30.glGetIntegerv(GLES30.GL_MINOR_VERSION, statusSlot, 0)
        val gotMinor = statusSlot[0]
        return gotMajor > major || (gotMajor == major && gotMinor >= minor)
    }
}

private const val LOG_TAG = "RedByteFX"

private val directScratch = ThreadLocal<ByteBuffer>()

/**
 * `glBufferData` reads a direct buffer. A heap [ByteBuffer.wrap] does not qualify and is rejected
 * by the Android GLES bindings. Texture uploads accept a heap buffer, so pixels skip this scratch:
 * it is kept per thread and would otherwise hold the largest image for the life of the GL thread.
 */
private fun scratch(bytes: Int): ByteBuffer {
    val existing = directScratch.get()
    val buffer = if (existing != null && existing.capacity() >= bytes) {
        existing
    } else {
        ByteBuffer.allocateDirect(bytes).also { directScratch.set(it) }
    }
    buffer.order(ByteOrder.nativeOrder())
    buffer.clear()
    return buffer
}

private fun ByteArray.asNativeBuffer(): ByteBuffer {
    val buffer = scratch(size)
    buffer.limit(size)
    buffer.put(this)
    buffer.position(0)
    return buffer
}

private fun FloatArray.asNativeBuffer(): ByteBuffer {
    val bytes = size * Float.SIZE_BYTES
    val buffer = scratch(bytes)
    buffer.limit(bytes)
    buffer.asFloatBuffer().put(this)
    buffer.position(0)
    return buffer
}

private fun IntArray.asNativeIntBuffer(): ByteBuffer {
    val bytes = size * Int.SIZE_BYTES
    val buffer = scratch(bytes)
    buffer.limit(bytes)
    buffer.asIntBuffer().put(this)
    buffer.position(0)
    return buffer
}

private fun IntArray.asNativeShortBuffer(): ByteBuffer {
    val bytes = size * Short.SIZE_BYTES
    val buffer = scratch(bytes)
    buffer.limit(bytes)
    val view = buffer.asShortBuffer()
    for (value in this) {
        view.put(value.toShort())
    }
    buffer.position(0)
    return buffer
}

private fun cubeTarget(face: CubeFace): Int = when (face) {
    CubeFace.PositiveX -> GLES30.GL_TEXTURE_CUBE_MAP_POSITIVE_X
    CubeFace.NegativeX -> GLES30.GL_TEXTURE_CUBE_MAP_NEGATIVE_X
    CubeFace.PositiveY -> GLES30.GL_TEXTURE_CUBE_MAP_POSITIVE_Y
    CubeFace.NegativeY -> GLES30.GL_TEXTURE_CUBE_MAP_NEGATIVE_Y
    CubeFace.PositiveZ -> GLES30.GL_TEXTURE_CUBE_MAP_POSITIVE_Z
    CubeFace.NegativeZ -> GLES30.GL_TEXTURE_CUBE_MAP_NEGATIVE_Z
}

private fun stageEnum(stage: GlStage): Int = when (stage) {
    GlStage.Vertex -> GLES30.GL_VERTEX_SHADER
    GlStage.Fragment -> GLES30.GL_FRAGMENT_SHADER
    GlStage.TessControl -> GLES32.GL_TESS_CONTROL_SHADER
    GlStage.TessEval -> GLES32.GL_TESS_EVALUATION_SHADER
    GlStage.Geometry -> GLES32.GL_GEOMETRY_SHADER
    GlStage.Compute -> GLES31.GL_COMPUTE_SHADER
}
