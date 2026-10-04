package ru.redbyte.redbytefx.gl

import java.nio.ByteBuffer

public enum class GlStage {
    Vertex,
    TessControl,
    TessEval,
    Geometry,
    Fragment,
    Compute,
}

public class GlCompileStatus(
    public val ok: Boolean,
    public val infoLog: String,
)

/**
 * Driver port. [GlProgramRuntime] never treats a call as success unless this port says so.
 * [Gles30Device] is the OpenGL ES 3.0 implementation; unit tests supply their own port.
 */
public abstract class GlDevice {
    public abstract fun createShader(stage: GlStage): Int

    public abstract fun shaderSource(shader: Int, source: String)

    public abstract fun compileShader(shader: Int): GlCompileStatus

    public abstract fun deleteShader(shader: Int)

    public abstract fun createProgram(): Int

    public abstract fun attachShader(program: Int, shader: Int)

    public abstract fun linkProgram(program: Int): GlCompileStatus

    public abstract fun deleteProgram(program: Int)

    public abstract fun uniformLocation(program: Int, name: String): Int

    /** Device. Location of vertex attribute [name] in a linked [program], or -1. */
    public abstract fun attribLocation(program: Int, name: String): Int

    public abstract fun uniform1f(location: Int, value: Float)

    public abstract fun uniform2f(location: Int, x: Float, y: Float)

    public abstract fun uniform3f(location: Int, x: Float, y: Float, z: Float)

    public abstract fun uniform4f(location: Int, x: Float, y: Float, z: Float, w: Float)

    public abstract fun uniform1i(location: Int, value: Int)

    public abstract fun uniformMatrix2fv(location: Int, values: FloatArray)

    public abstract fun uniformMatrix3fv(location: Int, values: FloatArray)

    public abstract fun uniformMatrix4fv(location: Int, values: FloatArray)

    public abstract fun maxCombinedTextureImageUnits(): Int

    public abstract fun useProgram(program: Int)

    public abstract fun activeTexture(unit: Int)

    public abstract fun bindTexture2D(texture: Int)

    public abstract fun bindTextureCube(texture: Int)

    /** Allocates a `GL_TEXTURE_2D` name. Call on the EGL thread. */
    public abstract fun createTexture(): Int

    /** Deletes a texture name from [createTexture]. */
    public abstract fun deleteTexture(texture: Int)

    /**
     * Binds [texture] as a 2D texture and selects linear min/mag filters with repeat wrap.
     * There is no mip chain.
     */
    public abstract fun texture2DLinearRepeat(texture: Int)

    /**
     * Uploads mip level 0 as RGBA8 (`GL_RGBA`, `GL_UNSIGNED_BYTE`).
     * [rgba] is tightly packed R, G, B, A and its size is `width * height * 4`.
     */
    public abstract fun texImage2DRgba(texture: Int, width: Int, height: Int, rgba: ByteArray)

    public abstract fun dispatchCompute(x: Int, y: Int, z: Int)

    public abstract fun shaderStorageBarrier()

    public abstract fun createBuffer(): Int

    public abstract fun deleteBuffer(buffer: Int)

    public abstract fun uniformBufferData(buffer: Int, data: ByteArray)

    public abstract fun uniformBufferSubData(buffer: Int, data: ByteArray)

    public abstract fun bindUniformBufferBase(buffer: Int, binding: Int)

    public abstract fun uniformBlockIndex(program: Int, name: String): Int

    public abstract fun uniformBlockBinding(program: Int, blockIndex: Int, binding: Int)

    public abstract fun shaderStorageData(buffer: Int, data: ByteArray)

    public abstract fun shaderStorageSubData(buffer: Int, data: ByteArray)

    public abstract fun bindShaderStorageBase(buffer: Int, binding: Int)

    /** Makes shader writes visible to a later [mapShaderStorageRead]. `GL_BUFFER_UPDATE_BARRIER_BIT`. */
    public abstract fun bufferUpdateBarrier()

    /**
     * Maps the first [bytes] of a shader storage buffer for reading.
     * The returned buffer stays valid until [unmapShaderStorage].
     */
    public abstract fun mapShaderStorageRead(buffer: Int, bytes: Int): ByteBuffer

    /** Unmaps a buffer mapped by [mapShaderStorageRead]. */
    public abstract fun unmapShaderStorage(buffer: Int)

    /**
     * Device. `glDrawArrays` on the current EGL context. [GlProgramRuntime] checks the thread first.
     */
    public abstract fun drawArrays(mode: Int, first: Int, count: Int)

    /**
     * Device. `glDrawElements` on the current EGL context.
     * [unsignedInt] selects `GL_UNSIGNED_INT`; otherwise the elements are `GL_UNSIGNED_SHORT`.
     * [indexOffset] counts elements, not bytes.
     */
    public abstract fun drawElements(mode: Int, count: Int, unsignedInt: Boolean, indexOffset: Int)

    /** Device. `glDrawArraysInstanced` on the current EGL context. */
    public abstract fun drawArraysInstanced(mode: Int, first: Int, count: Int, instances: Int)

    /**
     * Device. `glDrawElementsInstanced` on the current EGL context.
     * [indexOffset] counts elements, not bytes.
     */
    public abstract fun drawElementsInstanced(
        mode: Int,
        count: Int,
        unsignedInt: Boolean,
        instances: Int,
        indexOffset: Int,
    )

    /** Device. `glBufferData` of floats into an array buffer with `GL_DYNAMIC_DRAW`. */
    public abstract fun arrayBufferData(buffer: Int, data: FloatArray)

    /** Device. `glBufferSubData` of floats into an array buffer, starting at offset zero. */
    public abstract fun arrayBufferSubData(buffer: Int, data: FloatArray)

    /**
     * Device. Binds vertex array 0.
     * An element-buffer upload calls this first. `GL_ELEMENT_ARRAY_BUFFER` is state of the
     * bound vertex array, so uploading into whichever array is current would retarget that mesh.
     */
    public abstract fun unbindVertexArray()

    /**
     * Device. Uploads an element buffer. [unsignedInt] stores `GL_UNSIGNED_INT`;
     * otherwise each index is stored as `GL_UNSIGNED_SHORT`.
     */
    public abstract fun elementBufferData(buffer: Int, indices: IntArray, unsignedInt: Boolean)

    /** Device. `glTexSubImage2D` of RGBA8 into level 0. The caller has already checked bounds. */
    public abstract fun texSubImage2DRgba(
        texture: Int,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        rgba: ByteArray,
    )

    /** Device. RGBA8 level 0 of one cube face. Other faces are left untouched. */
    public abstract fun texImageCubeFace(
        texture: Int,
        face: CubeFace,
        width: Int,
        height: Int,
        rgba: ByteArray,
    )

    /** Device. Linear min/mag and clamp-to-edge on a cube map. Does not allocate faces. */
    public abstract fun textureCubeLinearClamp(texture: Int)

    /** Device. Linear min/mag and clamp-to-edge on a 2D texture. There is no mip chain. */
    public abstract fun texture2DLinearClamp(texture: Int)

    /** Device. `glGenerateMipmap` for a 2D texture. Does not change the min filter. */
    public abstract fun generateMipmap2D(texture: Int)

    /** Device. Sets the 2D min filter to linear mipmap. Call after [generateMipmap2D]. */
    public abstract fun filterMipmap2D(texture: Int)

    /** Device. Allocates RGBA8 level 0 with no pixel source. */
    public abstract fun texImage2DRgbaAlloc(texture: Int, width: Int, height: Int)

    /** Device. Allocates a framebuffer name on the current EGL context. */
    public abstract fun createFramebuffer(): Int

    /** Device. Deletes a framebuffer name. */
    public abstract fun deleteFramebuffer(framebuffer: Int)

    /** Device. Binds a framebuffer. Zero binds the default framebuffer. */
    public abstract fun bindFramebuffer(framebuffer: Int)

    /** Device. Allocates a renderbuffer name on the current EGL context. */
    public abstract fun createRenderbuffer(): Int

    /** Device. Deletes a renderbuffer name. */
    public abstract fun deleteRenderbuffer(renderbuffer: Int)

    /** Device. Attaches [texture] as the color target of [framebuffer]. */
    public abstract fun framebufferColor(framebuffer: Int, texture: Int)

    /** Device. Allocates a depth renderbuffer and attaches it to [framebuffer]. */
    public abstract fun framebufferDepth(framebuffer: Int, renderbuffer: Int, width: Int, height: Int)

    /** Device. `true` when the framebuffer status is complete. */
    public abstract fun framebufferComplete(framebuffer: Int): Boolean

    /** Device. Sets the instance divisor of one vertex attribute. */
    public abstract fun vertexAttribDivisor(location: Int, divisor: Int)

    /** Device. `glDisableVertexAttribArray` for one location. */
    public abstract fun disableVertexAttribArray(location: Int)

    /**
     * Device. Enables a float attribute and sets its pointer.
     * [strideFloats] and [offsetFloats] count floats, not bytes.
     */
    public abstract fun vertexAttribFloat(location: Int, size: Int, strideFloats: Int, offsetFloats: Int)

    /**
     * Drains pending `glGetError` values. The default port ignores errors; [Gles30Device] logs them.
     */
    public open fun flushGlErrors(context: String): Unit = Unit
}

/** One face of a cube map, in `GL_TEXTURE_CUBE_MAP_*` order. */
public enum class CubeFace {
    PositiveX,
    NegativeX,
    PositiveY,
    NegativeY,
    PositiveZ,
    NegativeZ,
}

/**
 * Color texture and depth renderbuffer owned by one framebuffer.
 *
 * The names die with the EGL context. Delete the target on that context before the surface
 * is destroyed. [GlProgramRuntime.destroy] does not delete [colorTexture].
 */
public class GlColorTarget(
    public val framebuffer: Int,
    public val colorTexture: Int,
    public val depthRenderbuffer: Int,
    public val width: Int,
    public val height: Int,
)
