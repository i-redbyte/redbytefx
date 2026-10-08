package ru.redbyte.redbytefx

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * One std140 uniform block owned by a GLES 3.0 program.
 *
 * Fields are highp float scalars, vectors, matrices, and sized arrays. An unsized array stays on
 * a storage block. The block is written as a whole. [binding] is the declaration order, starting at 0.
 */
public class UniformBlock internal constructor(
    public val name: String,
    public val typeName: String,
    internal val instanceName: String,
    internal val members: List<BlockMember>,
    offsets: IntArray,
    public val byteSize: Int,
    public val binding: Int,
) {
    private val layoutOffsets = offsets.copyOf()

    /** Number of logical float values accepted by [packStd140] and [packStd140Into]. */
    public val floatCount: Int = members.sumOf { logicalUniformLanes(it) }

    public val offsets: IntArray
        get() = layoutOffsets.copyOf()

    internal fun offsetAt(index: Int): Int = layoutOffsets[index]
}

internal class BlockMember(
    val instanceName: String,
    val memberName: String,
    val shape: Shape,
    val arraySize: Int = 0,
    val shared: Boolean = false,
    val unsized: Boolean = false,
)

@RedByteFxDsl
public class UniformBlockBuilder internal constructor(
    private val instanceName: String,
    private val vertexStage: (ShaderDsl.VertexDsl.() -> Unit) -> Unit,
    private val fragmentStage: (FragmentDsl.() -> Expr<*>) -> Unit,
) {
    private val members = mutableListOf<BlockMember>()

    public fun float(name: String): HighFloat = member(name, Shape.Scalar(ScalarKind.Float, Precision.High))

    public fun vec2(name: String): HighVec2 = member(name, vector(2))

    public fun vec3(name: String): HighVec3 = member(name, vector(3))

    public fun vec4(name: String): HighVec4 = member(name, vector(4))

    public fun mat2(name: String): Expr<Mat2> = member(name, Shape.Matrix(2))

    public fun mat3(name: String): Expr<Mat3> = member(name, Shape.Matrix(3))

    public fun mat4(name: String): Expr<Mat4> = member(name, Shape.Matrix(4))

    public fun floatArray(name: String, size: Int): StorageArray<Flt<High>> =
        array(name, Shape.Scalar(ScalarKind.Float, Precision.High), size)

    public fun vec2Array(name: String, size: Int): StorageArray<Vec2<Flt<High>>> = array(name, vector(2), size)

    public fun vec3Array(name: String, size: Int): StorageArray<Vec3<Flt<High>>> = array(name, vector(3), size)

    public fun vec4Array(name: String, size: Int): StorageArray<Vec4<Flt<High>>> = array(name, vector(4), size)

    public fun vertex(block: ShaderDsl.VertexDsl.() -> Unit) {
        vertexStage(block)
    }

    public fun fragment(block: FragmentDsl.() -> Expr<*>) {
        fragmentStage(block)
    }

    internal fun finish(name: String, typeName: String, binding: Int): UniformBlock {
        require(members.isNotEmpty()) { "Uniform block requires a field" }
        val layout = std140BlockLayout(members)
        return UniformBlock(
            name,
            typeName,
            instanceName,
            members.toList(),
            layout.offsets,
            layout.byteSize,
            binding,
        )
    }

    private fun <T : ShType> array(name: String, shape: Shape, size: Int): StorageArray<T> {
        require(size > 0) { "Uniform array size must be positive, was $size" }
        return StorageArray(field(name, shape, size))
    }

    private fun <T : ShType> member(name: String, shape: Shape): Expr<T> {
        val created = field(name, shape, 0)
        return Expr(shape, ExprNode.BlockRef(created))
    }

    private fun field(name: String, shape: Shape, arraySize: Int): BlockMember {
        require(name.isNotBlank()) { "Uniform block field name must not be blank" }
        val memberName = sanitizeSuggestedIdentifier(name, "f")
        require(members.none { it.memberName == memberName }) {
            "Uniform block already has a field named $memberName"
        }
        val member = BlockMember(instanceName, memberName, shape, arraySize)
        members += member
        return member
    }
}

internal class Std140Layout(
    val offsets: IntArray,
    val byteSize: Int,
)

internal fun std140BlockLayout(members: List<BlockMember>): Std140Layout {
    val offsets = IntArray(members.size)
    var cursor = 0
    members.forEachIndexed { index, member ->
        cursor = roundUp(cursor, std140MemberAlignment(member))
        offsets[index] = cursor
        cursor = checkedLayoutAdd(cursor, std140MemberSize(member))
    }
    return Std140Layout(offsets, roundUp(cursor, VEC4_ALIGNMENT))
}

public fun packStd140(block: UniformBlock, values: FloatArray): ByteArray {
    validateStd140Values(block, values)
    val packed = ByteArray(block.byteSize)
    writeStd140Into(block, values, packed)
    return packed
}

/** Packs into an existing buffer, including zeroing std140 padding before each write. */
public fun packStd140Into(block: UniformBlock, values: FloatArray, into: ByteArray) {
    validateStd140Values(block, values)
    require(into.size == block.byteSize) {
        "Uniform block \"${block.name}\" needs ${block.byteSize} bytes, was ${into.size}"
    }
    into.fill(0)
    writeStd140Into(block, values, into)
}

private fun validateStd140Values(block: UniformBlock, values: FloatArray) {
    require(values.size == block.floatCount) {
        "Uniform block \"${block.name}\" expects ${block.floatCount} floats, was ${values.size}"
    }
    require(values.all { it.isFinite() }) { "Uniform block values must be finite" }
}

private fun writeStd140Into(block: UniformBlock, values: FloatArray, into: ByteArray) {
    val buffer = ByteBuffer.wrap(into).order(ByteOrder.nativeOrder())
    var cursor = 0
    block.members.forEachIndexed { index, member ->
        cursor = writeStd140(buffer, block.offsetAt(index), member, values, cursor)
    }
}

/**
 * Shade. Copies the logical floats of a std140 [block] into [into].
 * Padding between `vec3` lanes and between matrix columns is skipped.
 */
public fun unpackStd140(block: UniformBlock, packed: ByteBuffer, into: FloatArray): Int {
    require(into.size >= block.floatCount) {
        "Uniform block \"${block.name}\" needs ${block.floatCount} floats, was ${into.size}"
    }
    val view = packed.duplicate().order(ByteOrder.nativeOrder())
    require(view.limit() >= block.byteSize) {
        "Uniform block \"${block.name}\" needs ${block.byteSize} bytes, was ${view.limit()}"
    }
    var cursor = 0
    block.members.forEachIndexed { index, member ->
        cursor = readStd140(view, block.offsetAt(index), member, into, cursor)
    }
    return cursor
}

internal fun logicalUniformLanes(member: BlockMember): Int {
    val perElement = when (val shape = member.shape) {
        is Shape.Matrix -> shape.lanes * shape.lanes
        else -> laneCount(shape)
    }
    return if (member.arraySize == 0) perElement else member.arraySize * perElement
}

internal fun std140MemberAlignment(member: BlockMember): Int = when {
    member.arraySize > 0 || member.shape is Shape.Matrix -> VEC4_ALIGNMENT
    else -> std140Alignment(member.shape)
}

internal fun std140MemberSize(member: BlockMember): Int = when {
    member.arraySize > 0 -> checkedLayoutMultiply(std140ElementStride(member.shape), member.arraySize)
    else -> std140ElementSize(member.shape)
}

internal fun std140ElementStride(shape: Shape): Int = roundUp(std140ElementSize(shape), VEC4_ALIGNMENT)

internal fun std140ElementSize(shape: Shape): Int = when (shape) {
    is Shape.Matrix -> shape.lanes * VEC4_ALIGNMENT
    else -> std140Size(shape)
}

private fun vector(lanes: Int): Shape = Shape.Vector(ScalarKind.Float, Precision.High, lanes)

private fun writeStd140(
    buffer: ByteBuffer,
    offset: Int,
    member: BlockMember,
    values: FloatArray,
    cursor: Int,
): Int = transferStd140(buffer, offset, member, values, cursor, into = null)

private fun readStd140(
    buffer: ByteBuffer,
    offset: Int,
    member: BlockMember,
    into: FloatArray,
    cursor: Int,
): Int = transferStd140(buffer, offset, member, values = null, cursor, into)

private fun transferStd140(
    buffer: ByteBuffer,
    offset: Int,
    member: BlockMember,
    values: FloatArray?,
    cursor: Int,
    into: FloatArray?,
): Int {
    val shape = member.shape
    return if (shape is Shape.Matrix) {
        transferMatrix(buffer, offset, member, shape, values, cursor, into)
    } else {
        transferColumns(buffer, offset, member, laneCount(shape), columnStride(member, shape), values, cursor, into)
    }
}

private fun transferMatrix(
    buffer: ByteBuffer,
    offset: Int,
    member: BlockMember,
    shape: Shape.Matrix,
    values: FloatArray?,
    cursor: Int,
    into: FloatArray?,
): Int {
    val elementStride = if (member.arraySize > 0) std140ElementStride(shape) else std140ElementSize(shape)
    return transferColumns(buffer, offset, member, shape.lanes, elementStride, values, cursor, into)
}

private fun transferColumns(
    buffer: ByteBuffer,
    offset: Int,
    member: BlockMember,
    lanes: Int,
    elementStride: Int,
    values: FloatArray?,
    cursor: Int,
    into: FloatArray?,
): Int {
    val elements = if (member.arraySize == 0) 1 else member.arraySize
    val columnBytes = if (member.shape is Shape.Matrix) VEC4_ALIGNMENT else elementStride
    var local = cursor
    var element = 0
    while (element < elements) {
        var column = 0
        val columns = if (member.shape is Shape.Matrix) lanes else 1
        while (column < columns) {
            val base = offset + element * elementStride + column * columnBytes
            local = copyLanes(buffer, base, lanes, values, local, into)
            column += 1
        }
        element += 1
    }
    return local
}

private fun columnStride(member: BlockMember, shape: Shape): Int =
    if (member.arraySize > 0) std140ElementStride(shape) else std140Size(shape)

private fun copyLanes(
    buffer: ByteBuffer,
    base: Int,
    lanes: Int,
    values: FloatArray?,
    cursor: Int,
    into: FloatArray?,
): Int {
    var local = cursor
    var lane = 0
    while (lane < lanes) {
        val at = base + lane * FLOAT_ALIGNMENT
        if (into != null) into[local] = buffer.getFloat(at) else buffer.putFloat(at, values!![local])
        local += 1
        lane += 1
    }
    return local
}

internal fun std430ArrayStride(shape: Shape): Int = roundUp(std140Size(shape), std140Alignment(shape))

internal fun logicalLanes(member: BlockMember): Int = when {
    member.unsized -> laneCount(member.shape)
    member.arraySize == 0 -> laneCount(member.shape)
    else -> checkedLayoutMultiply(member.arraySize, laneCount(member.shape))
}

internal fun std430Layout(members: List<BlockMember>): Std140Layout {
    val offsets = IntArray(members.size)
    var cursor = 0
    var alignment = 1
    members.forEachIndexed { index, member ->
        val arrayLike = member.arraySize > 0 || member.unsized
        val memberAlignment = if (arrayLike) std430ArrayStride(member.shape) else std140Alignment(member.shape)
        if (memberAlignment > alignment) alignment = memberAlignment
        cursor = roundUp(cursor, memberAlignment)
        offsets[index] = cursor
        val count = if (member.unsized) 0 else member.arraySize
        val size = if (arrayLike) checkedLayoutMultiply(std430ArrayStride(member.shape), count) else std140Size(member.shape)
        cursor = checkedLayoutAdd(cursor, size)
    }
    return Std140Layout(offsets, roundUp(cursor, alignment))
}

internal fun laneCount(shape: Shape): Int = when (shape) {
    is Shape.Scalar -> 1
    is Shape.Vector -> shape.lanes
    else -> error("Block field must be a float scalar or vector")
}

internal fun std140Alignment(shape: Shape): Int = when (laneCount(shape)) {
    1 -> FLOAT_ALIGNMENT
    2 -> FLOAT_ALIGNMENT * 2
    else -> VEC4_ALIGNMENT
}

internal fun std140Size(shape: Shape): Int = laneCount(shape) * FLOAT_ALIGNMENT

internal fun roundUp(value: Int, alignment: Int): Int {
    require(value >= 0 && alignment > 0) { "Layout size must be non-negative and alignment positive" }
    val rounded = ((value.toLong() + alignment - 1) / alignment) * alignment
    require(rounded <= Int.MAX_VALUE) { "Shader block layout is too large" }
    return rounded.toInt()
}

internal fun checkedLayoutAdd(left: Int, right: Int): Int {
    val result = left.toLong() + right
    require(result <= Int.MAX_VALUE) { "Shader block layout is too large" }
    return result.toInt()
}

internal fun checkedLayoutMultiply(left: Int, right: Int): Int {
    val result = left.toLong() * right
    require(result <= Int.MAX_VALUE) { "Shader block layout is too large" }
    return result.toInt()
}

private const val FLOAT_ALIGNMENT = 4
private const val VEC4_ALIGNMENT = 16
