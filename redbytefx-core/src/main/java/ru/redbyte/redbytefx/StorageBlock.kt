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
    private val fixedByteSize: Int,
) {
    public val byteSize: Int
        get() {
            check(members.none { it.unsized }) {
                "Storage block \"$name\" has an unsized array; call byteSize(valueCount)"
            }
            return fixedByteSize
        }

    public fun byteSize(valueCount: Int): Int = std430Layout(concreteMembers(this, valueCount)).byteSize
}

public class StorageArray<T : ShType> internal constructor(
    internal val member: BlockMember,
) {
    public operator fun get(index: Expr<IntS>): Expr<T> =
        Expr(member.shape, ExprNode.Index(member, index))
}

public class StorageBlockBuilder internal constructor(
    private val instanceName: String,
    private val computeStage: (ComputeLayout, ComputeDsl.() -> Unit) -> Unit,
) {
    private val members = mutableListOf<BlockMember>()

    public fun float(name: String): HighFloat = member(name, Shape.Scalar(ScalarKind.Float, Precision.High))

    public fun vec2(name: String): HighVec2 = member(name, vector(2))

    public fun vec3(name: String): HighVec3 = member(name, vector(3))

    public fun vec4(name: String): HighVec4 = member(name, vector(4))

    public fun floatArray(name: String): StorageArray<Flt<High>> =
        unsized(name, Shape.Scalar(ScalarKind.Float, Precision.High))

    public fun floatArray(name: String, size: Int): StorageArray<Flt<High>> =
        array(name, Shape.Scalar(ScalarKind.Float, Precision.High), size)

    public fun vec2Array(name: String): StorageArray<Vec2<Flt<High>>> = unsized(name, vector(2))

    public fun vec2Array(name: String, size: Int): StorageArray<Vec2<Flt<High>>> = array(name, vector(2), size)

    public fun vec3Array(name: String): StorageArray<Vec3<Flt<High>>> = unsized(name, vector(3))

    public fun vec3Array(name: String, size: Int): StorageArray<Vec3<Flt<High>>> = array(name, vector(3), size)

    public fun vec4Array(name: String): StorageArray<Vec4<Flt<High>>> = unsized(name, vector(4))

    public fun vec4Array(name: String, size: Int): StorageArray<Vec4<Flt<High>>> = array(name, vector(4), size)

    public fun compute(localSizeX: Int, build: ComputeDsl.() -> Unit) {
        computeStage(ComputeLayout(localSizeX, null, null), build)
    }

    public fun compute(localSizeX: Int, localSizeY: Int, build: ComputeDsl.() -> Unit) {
        computeStage(ComputeLayout(localSizeX, localSizeY, null), build)
    }

    public fun compute(localSizeX: Int, localSizeY: Int, localSizeZ: Int, build: ComputeDsl.() -> Unit) {
        computeStage(ComputeLayout(localSizeX, localSizeY, localSizeZ), build)
    }

    internal fun memberSnapshot(): List<BlockMember> = members.toList()

    internal fun finish(name: String, typeName: String): StorageBlock {
        require(members.isNotEmpty()) { "Storage block requires a field" }
        val unsizedAt = members.indexOfFirst { it.unsized }
        if (unsizedAt >= 0 && unsizedAt != members.lastIndex) {
            throw ProgramException(
                ProgramCode.UnsizedStorageNotLast,
                "Unsized storage field \"${members[unsizedAt].memberName}\" must be the last member of \"$name\"",
            )
        }
        val layout = std430Layout(members.toList())
        return StorageBlock(name, typeName, instanceName, members.toList(), layout.offsets, layout.byteSize)
    }

    private fun <T : ShType> array(name: String, shape: Shape, size: Int): StorageArray<T> {
        require(size > 0) { "Storage array size must be positive, was $size" }
        return StorageArray(field(name, shape, size, unsized = false))
    }

    private fun <T : ShType> unsized(name: String, shape: Shape): StorageArray<T> =
        StorageArray(field(name, shape, 0, unsized = true))

    private fun field(name: String, shape: Shape, arraySize: Int, unsized: Boolean): BlockMember {
        require(name.isNotBlank()) { "Storage block field name must not be blank" }
        val memberName = sanitizeSuggestedIdentifier(name, "f")
        require(members.none { it.memberName == memberName }) {
            "Storage block already has a field named $memberName"
        }
        val member = BlockMember(instanceName, memberName, shape, arraySize, unsized = unsized)
        members += member
        return member
    }

    private fun <T : ShType> member(name: String, shape: Shape): Expr<T> {
        val created = field(name, shape, 0, unsized = false)
        return Expr(shape, ExprNode.BlockRef(created))
    }
}

internal class ComputeLayout(
    val x: Int,
    val y: Int?,
    val z: Int?,
)

public fun packStd430(block: StorageBlock, values: FloatArray): ByteArray {
    require(values.all { it.isFinite() }) { "Storage block values must be finite" }
    val concrete = concreteMembers(block, values.size)
    val layout = std430Layout(concrete)
    val buffer = ByteBuffer.allocate(layout.byteSize).order(ByteOrder.nativeOrder())
    var cursor = 0
    concrete.forEachIndexed { index, member ->
        buffer.position(layout.offsets[index])
        val lanes = laneCount(member.shape)
        val elements = if (member.arraySize == 0 && !block.members[index].unsized) 1 else member.arraySize
        val stride = if (member.arraySize > 0 || block.members[index].unsized) {
            std430ArrayStride(member.shape) / FLOAT_BYTES
        } else {
            lanes
        }
        repeat(elements) {
            repeat(lanes) {
                buffer.putFloat(values[cursor])
                cursor += 1
            }
            repeat(stride - lanes) { buffer.putFloat(0f) }
        }
    }
    return buffer.array()
}

private fun concreteMembers(block: StorageBlock, valueCount: Int): List<BlockMember> {
    val counts = elementCounts(block, valueCount)
    return block.members.mapIndexed { index, member ->
        if (member.unsized) {
            BlockMember(member.instanceName, member.memberName, member.shape, counts[index])
        } else {
            member
        }
    }
}

private fun elementCounts(block: StorageBlock, valueCount: Int): IntArray {
    val counts = IntArray(block.members.size)
    var fixed = 0
    var unsizedAt = -1
    block.members.forEachIndexed { index, member ->
        if (member.unsized) {
            unsizedAt = index
        } else {
            counts[index] = if (member.arraySize == 0) 1 else member.arraySize
            fixed += logicalLanes(member)
        }
    }
    if (unsizedAt < 0) {
        require(valueCount == fixed) {
            "Storage block \"${block.name}\" expects $fixed floats, was $valueCount"
        }
        return counts
    }
    val lanes = laneCount(block.members[unsizedAt].shape)
    val tail = valueCount - fixed
    require(tail >= 0 && tail % lanes == 0) {
        "Storage block \"${block.name}\" expects $fixed floats plus a multiple of $lanes, was $valueCount"
    }
    counts[unsizedAt] = tail / lanes
    return counts
}

private const val FLOAT_BYTES = 4

private fun vector(lanes: Int): Shape = Shape.Vector(ScalarKind.Float, Precision.High, lanes)
