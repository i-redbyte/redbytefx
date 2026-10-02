package ru.redbyte.redbytefx

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * One std430 shader storage block owned by a GLES 3.1 compute program.
 *
 * Fields are highp float scalars and vectors. The block is written as a whole.
 */
public class StorageBlock internal constructor(
    public val name: String,
    public val typeName: String,
    internal val instanceName: String,
    internal val members: List<BlockMember>,
    public val offsets: IntArray,
    public val byteSize: Int,
)

public class StorageBlockBuilder internal constructor(
    private val instanceName: String,
) {
    private val members = mutableListOf<BlockMember>()

    public fun float(name: String): Expr<Flt<High>> = member(name, Shape.Scalar(ScalarKind.Float, Precision.High))

    public fun vec2(name: String): Expr<Vec2<Flt<High>>> = member(name, vector(2))

    public fun vec3(name: String): Expr<Vec3<Flt<High>>> = member(name, vector(3))

    public fun vec4(name: String): Expr<Vec4<Flt<High>>> = member(name, vector(4))

    internal fun finish(name: String, typeName: String): StorageBlock {
        require(members.isNotEmpty()) { "Storage block requires a field" }
        val layout = std430Layout(members.map { it.shape })
        return StorageBlock(name, typeName, instanceName, members.toList(), layout.offsets, layout.byteSize)
    }

    private fun <T : ShType> member(name: String, shape: Shape): Expr<T> {
        require(name.isNotBlank()) { "Storage block field name must not be blank" }
        val memberName = sanitizeSuggestedIdentifier(name, "f")
        require(members.none { it.memberName == memberName }) {
            "Storage block already has a field named $memberName"
        }
        val member = BlockMember(instanceName, memberName, shape)
        members += member
        return Expr(shape, ExprNode.BlockRef(member))
    }
}

internal class StorageAssignment(
    val target: Expr<*>,
    val value: Expr<*>,
)

internal fun std430Layout(shapes: List<Shape>): Std140Layout {
    val offsets = IntArray(shapes.size)
    var cursor = 0
    var alignment = 1
    shapes.forEachIndexed { index, shape ->
        val memberAlignment = std140Alignment(shape)
        if (memberAlignment > alignment) alignment = memberAlignment
        cursor = roundUp(cursor, memberAlignment)
        offsets[index] = cursor
        cursor += std140Size(shape)
    }
    return Std140Layout(offsets, roundUp(cursor, alignment))
}

public fun packStd430(block: StorageBlock, values: FloatArray): ByteArray {
    val lanes = block.members.sumOf { laneCount(it.shape) }
    require(values.size == lanes) {
        "Storage block \"${block.name}\" expects $lanes floats, was ${values.size}"
    }
    require(values.all { it.isFinite() }) { "Storage block values must be finite" }
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
