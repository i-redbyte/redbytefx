package ru.redbyte.redbytefx.gl.compose

import ru.redbyte.redbytefx.BoolS
import ru.redbyte.redbytefx.Flt
import ru.redbyte.redbytefx.High
import ru.redbyte.redbytefx.IntS
import ru.redbyte.redbytefx.Mat2
import ru.redbyte.redbytefx.Mat3
import ru.redbyte.redbytefx.Mat4
import ru.redbyte.redbytefx.Sampler2D
import ru.redbyte.redbytefx.SamplerCube
import ru.redbyte.redbytefx.StorageBlock
import ru.redbyte.redbytefx.Uniform
import ru.redbyte.redbytefx.UniformBlock
import ru.redbyte.redbytefx.Vec2
import ru.redbyte.redbytefx.Vec3
import ru.redbyte.redbytefx.Vec4
import ru.redbyte.redbytefx.gl.GlProgramRuntime

/**
 * The latest value written to one uniform or block, waiting for the GL thread.
 *
 * Slots are created once per handle and reused, so a write allocates nothing: it overwrites the
 * stored value and, the first time since the last drain, joins the pending queue.
 */
internal sealed class WriteSlot {
    /** In the pending queue. */
    var pending: Boolean = false

    /** Holds a value that a new EGL context should receive again. */
    abstract val replayable: Boolean

    abstract fun applyTo(runtime: GlProgramRuntime)
}

internal enum class UniformWriteKind {
    None,
    Float,
    Vec2,
    Vec3,
    Vec4,
    Int,
    Bool,
    Matrix,
    Texture2D,
    TextureCube,
}

/** A uniform. Texture names belong to one context, so a texture bind is never replayed. */
internal class UniformSlot(private val uniform: Uniform<*>) : WriteSlot() {
    private var kind = UniformWriteKind.None
    private val components = FloatArray(VECTOR_LANES)
    private var integer = 0
    private var matrix: FloatArray? = null

    override val replayable: Boolean
        get() = kind != UniformWriteKind.None && !isTexture

    val isTexture: Boolean
        get() = kind == UniformWriteKind.Texture2D || kind == UniformWriteKind.TextureCube

    fun storeFloats(kind: UniformWriteKind, x: Float, y: Float = 0f, z: Float = 0f, w: Float = 0f) {
        this.kind = kind
        components[0] = x
        components[1] = y
        components[2] = z
        components[3] = w
    }

    fun storeInt(kind: UniformWriteKind, value: Int) {
        this.kind = kind
        integer = value
    }

    fun storeMatrix(values: FloatArray) {
        kind = UniformWriteKind.Matrix
        val target = matrix?.takeIf { it.size == values.size } ?: FloatArray(values.size).also { matrix = it }
        values.copyInto(target)
    }

    /** Forgets a texture bind that a new context cannot use. */
    fun forgetTexture() {
        if (isTexture) kind = UniformWriteKind.None
    }

    @Suppress("UNCHECKED_CAST", "CyclomaticComplexMethod")
    override fun applyTo(runtime: GlProgramRuntime) {
        val c = components
        when (kind) {
            UniformWriteKind.None -> Unit
            UniformWriteKind.Float -> runtime.set(uniform as Uniform<Flt<High>>, c[0])
            UniformWriteKind.Vec2 -> runtime.set(uniform as Uniform<Vec2<Flt<High>>>, c[0], c[1])
            UniformWriteKind.Vec3 -> runtime.set(uniform as Uniform<Vec3<Flt<High>>>, c[0], c[1], c[2])
            UniformWriteKind.Vec4 -> runtime.set(uniform as Uniform<Vec4<Flt<High>>>, c[0], c[1], c[2], c[3])
            UniformWriteKind.Int -> runtime.set(uniform as Uniform<IntS>, integer)
            UniformWriteKind.Bool -> runtime.set(uniform as Uniform<BoolS>, integer != 0)
            UniformWriteKind.Matrix -> applyMatrix(runtime, checkNotNull(matrix))
            UniformWriteKind.Texture2D -> runtime.bind(uniform as Uniform<Sampler2D>, integer)
            UniformWriteKind.TextureCube -> runtime.bind(uniform as Uniform<SamplerCube>, integer)
        }
    }

    @Suppress("UNCHECKED_CAST")
    private fun applyMatrix(runtime: GlProgramRuntime, values: FloatArray) {
        when (values.size) {
            MAT2_FLOATS -> runtime.set(uniform as Uniform<Mat2>, values)
            MAT3_FLOATS -> runtime.set(uniform as Uniform<Mat3>, values)
            else -> runtime.set(uniform as Uniform<Mat4>, values)
        }
    }
}

/** A uniform or storage block; its floats are copied into a reused array. */
internal class BlockSlot private constructor(
    private val uniformBlock: UniformBlock?,
    private val storageBlock: StorageBlock?,
) : WriteSlot() {
    constructor(block: UniformBlock) : this(block, null)

    constructor(block: StorageBlock) : this(null, block)

    private var values: FloatArray? = null

    override val replayable: Boolean
        get() = values != null

    fun store(source: FloatArray) {
        val target = values?.takeIf { it.size == source.size } ?: FloatArray(source.size).also { values = it }
        source.copyInto(target)
    }

    override fun applyTo(runtime: GlProgramRuntime) {
        val stored = values ?: return
        if (uniformBlock != null) runtime.set(uniformBlock, stored) else runtime.set(checkNotNull(storageBlock), stored)
    }
}

private const val VECTOR_LANES = 4
private const val MAT2_FLOATS = 4
private const val MAT3_FLOATS = 9
