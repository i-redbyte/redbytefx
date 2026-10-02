package ru.redbyte.redbytefx

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * One std140 uniform block owned by a GLES 3.0 program.
 *
 * Fields are highp float scalars and vectors. The block is written as a whole.
 */
public class UniformBlock internal constructor(
    public val name: String,
    public val typeName: String,
    internal val instanceName: String,
    internal val members: List<BlockMember>,
    public val offsets: IntArray,
    public val byteSize: Int,
)

internal class BlockMember(
    val instanceName: String,
    val memberName: String,
    val shape: Shape,
)

public class UniformBlockBuilder internal constructor(
    private val instanceName: String,
) {
    private val members = mutableListOf<BlockMember>()

    public fun float(name: String): Expr<Flt<High>> = member(name, Shape.Scalar(ScalarKind.Float, Precision.High))

    public fun vec2(name: String): Expr<Vec2<Flt<High>>> = member(name, vector(2))

    public fun vec3(name: String): Expr<Vec3<Flt<High>>> = member(name, vector(3))

    public fun vec4(name: String): Expr<Vec4<Flt<High>>> = member(name, vector(4))

    internal fun finish(name: String, typeName: String): UniformBlock {
        require(members.isNotEmpty()) { "Uniform block requires a field" }
        val layout = std140Layout(members.map { it.shape })
        return UniformBlock(name, typeName, instanceName, members.toList(), layout.offsets, layout.byteSize)
    }

    private fun <T : ShType> member(name: String, shape: Shape): Expr<T> {
        require(name.isNotBlank()) { "Uniform block field name must not be blank" }
        val memberName = sanitizeSuggestedIdentifier(name, "f")
        require(members.none { it.memberName == memberName }) {
            "Uniform block already has a field named $memberName"
        }
        val member = BlockMember(instanceName, memberName, shape)
        members += member
        return Expr(shape, ExprNode.BlockRef(member))
    }
}

internal class Std140Layout(
    val offsets: IntArray,
    val byteSize: Int,
)

internal fun std140Layout(shapes: List<Shape>): Std140Layout {
    val offsets = IntArray(shapes.size)
    var cursor = 0
    shapes.forEachIndexed { index, shape ->
        val alignment = std140Alignment(shape)
        cursor = roundUp(cursor, alignment)
        offsets[index] = cursor
        cursor += std140Size(shape)
    }
    return Std140Layout(offsets, roundUp(cursor, VEC4_ALIGNMENT))
}

public fun packStd140(block: UniformBlock, values: FloatArray): ByteArray {
    val lanes = block.members.sumOf { laneCount(it.shape) }
    require(values.size == lanes) {
        "Uniform block \"${block.name}\" expects $lanes floats, was ${values.size}"
    }
    require(values.all { it.isFinite() }) { "Uniform block values must be finite" }
    val buffer = ByteBuffer.allocate(block.byteSize).order(ByteOrder.nativeOrder())
    var cursor = 0
    block.members.forEachIndexed { index, member ->
        buffer.position(block.offsets[index])
        repeat(laneCount(member.shape)) {
            buffer.putFloat(values[cursor])
            cursor += 1
        }
    }
    return buffer.array()
}

private fun vector(lanes: Int): Shape = Shape.Vector(ScalarKind.Float, Precision.High, lanes)

private fun laneCount(shape: Shape): Int = when (shape) {
    is Shape.Scalar -> 1
    is Shape.Vector -> shape.lanes
    else -> error("Uniform block field must be a float scalar or vector")
}

private fun std140Alignment(shape: Shape): Int = when (laneCount(shape)) {
    1 -> FLOAT_ALIGNMENT
    2 -> FLOAT_ALIGNMENT * 2
    else -> VEC4_ALIGNMENT
}

private fun std140Size(shape: Shape): Int = laneCount(shape) * FLOAT_ALIGNMENT

private fun roundUp(value: Int, alignment: Int): Int = (value + alignment - 1) / alignment * alignment

private const val FLOAT_ALIGNMENT = 4
private const val VEC4_ALIGNMENT = 16
