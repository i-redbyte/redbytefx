package ru.redbyte.redbytefx.gl

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

    /**
     * Drains pending `glGetError` values. The default port ignores errors; [Gles30Device] logs them.
     */
    public open fun flushGlErrors(context: String): Unit = Unit
}
