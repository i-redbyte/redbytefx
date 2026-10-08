package ru.redbyte.redbytefx

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * One std430 shader storage block owned by a GLES 3.1 compute program.
 *
 * Fields are highp float scalars and vectors. The block is written as a whole.
 * [binding] is the declaration order, starting at 0.
 */
public class StorageBlock internal constructor(
    public val name: String,
    public val typeName: String,
    internal val instanceName: String,
    internal val members: List<BlockMember>,
    offsets: IntArray,
    private val fixedByteSize: Int,
    public val binding: Int,
) {
    private val layoutOffsets = offsets.copyOf()
    private val unsizedAt = members.indexOfFirst { it.unsized }
    private val fixedValueCount = members.fold(0) { count, member ->
        if (member.unsized) count else checkedLayoutAdd(count, logicalLanes(member))
    }
    private val alignment = members.maxOfOrNull { member ->
        if (member.arraySize > 0 || member.unsized) {
            std430ArrayStride(member.shape)
        } else {
            std140Alignment(member.shape)
        }
    } ?: 1

    init {
        require(members.isNotEmpty()) { "Storage block requires a field" }
    }

    public val offsets: IntArray
        get() = layoutOffsets.copyOf()

    public val byteSize: Int
        get() {
            check(unsizedAt < 0) {
                "Storage block \"$name\" has an unsized array; call byteSize(valueCount)"
            }
            return fixedByteSize
        }

    public fun byteSize(valueCount: Int): Int {
        val tailElements = tailElements(valueCount)
        if (unsizedAt < 0) return fixedByteSize
        val tail = members[unsizedAt]
        val end = checkedLayoutAdd(
            layoutOffsets[unsizedAt],
            checkedLayoutMultiply(std430ArrayStride(tail.shape), tailElements),
        )
        return roundUp(end, alignment)
    }

    internal fun offsetAt(index: Int): Int = layoutOffsets[index]

    internal fun elementsAt(index: Int, tailElements: Int): Int = when {
        index == unsizedAt -> tailElements
        members[index].arraySize > 0 -> members[index].arraySize
        else -> 1
    }

    internal fun tailElements(valueCount: Int): Int {
        if (unsizedAt < 0) {
            require(valueCount == fixedValueCount) {
                "Storage block \"$name\" expects $fixedValueCount floats, was $valueCount"
            }
            return 0
        }
        val lanes = laneCount(members[unsizedAt].shape)
        require(valueCount >= fixedValueCount) {
            "Storage block \"$name\" expects $fixedValueCount floats plus a multiple of $lanes, was $valueCount"
        }
        val tail = valueCount - fixedValueCount
        require(tail % lanes == 0) {
            "Storage block \"$name\" expects $fixedValueCount floats plus a multiple of $lanes, was $valueCount"
        }
        return tail / lanes
    }
}

public class StorageArray<T : ShType> internal constructor(
    internal val member: BlockMember,
) {
    /** Reads one element of a storage, uniform, or `shared` array. */
    public operator fun get(index: Expr<IntS>): Expr<T> =
        Expr(member.shape, ExprNode.Index(member, index))
}

@RedByteFxDsl
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

    internal fun finish(name: String, typeName: String, binding: Int): StorageBlock {
        require(members.isNotEmpty()) { "Storage block requires a field" }
        val unsizedAt = members.indexOfFirst { it.unsized }
        if (unsizedAt >= 0 && unsizedAt != members.lastIndex) {
            throw ProgramException(
                ProgramCode.UnsizedStorageNotLast,
                "Unsized storage field \"${members[unsizedAt].memberName}\" must be the last member of \"$name\"",
            )
        }
        val layout = std430Layout(members.toList())
        return StorageBlock(name, typeName, instanceName, members.toList(), layout.offsets, layout.byteSize, binding)
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
    validateStd430Values(values)
    val packed = ByteArray(block.byteSize(values.size))
    writeStd430Into(block, values, packed)
    return packed
}

/** Packs into an existing buffer, including zeroing std430 padding before each write. */
public fun packStd430Into(block: StorageBlock, values: FloatArray, into: ByteArray) {
    validateStd430Values(values)
    val required = block.byteSize(values.size)
    require(into.size == required) {
        "Storage block \"${block.name}\" needs $required bytes, was ${into.size}"
    }
    into.fill(0)
    writeStd430Into(block, values, into)
}

private fun validateStd430Values(values: FloatArray) {
    require(values.all { it.isFinite() }) { "Storage block values must be finite" }
}

private fun writeStd430Into(block: StorageBlock, values: FloatArray, into: ByteArray) {
    val tailElements = block.tailElements(values.size)
    var cursor = 0
    block.members.forEachIndexed { index, member ->
        val lanes = laneCount(member.shape)
        val elements = block.elementsAt(index, tailElements)
        val stride = if (member.arraySize > 0 || member.unsized) {
            std430ArrayStride(member.shape)
        } else {
            lanes * FLOAT_BYTES
        }
        var base = block.offsetAt(index)
        repeat(elements) {
            repeat(lanes) { lane ->
                putFloatNative(into, base + lane * FLOAT_BYTES, values[cursor])
                cursor += 1
            }
            base += stride
        }
    }
}

/**
 * Writes the logical floats of a std430 [block] into [into].
 *
 * [packed] is the byte layout produced by [packStd430] for [valueCount] logical floats,
 * including the padding a `vec3` element inserts. Padding is not copied into [into].
 * [valueCount] is the same count [packStd430] received, which sizes an unsized tail.
 */
public fun unpackStd430(block: StorageBlock, packed: ByteBuffer, valueCount: Int, into: FloatArray): Int {
    require(into.size >= valueCount) {
        "Storage block \"${block.name}\" needs $valueCount floats, was ${into.size}"
    }
    val tailElements = block.tailElements(valueCount)
    val required = block.byteSize(valueCount)
    val view = packed.duplicate().order(ByteOrder.nativeOrder())
    require(view.limit() >= required) {
        "Storage block \"${block.name}\" needs $required bytes, was ${view.limit()}"
    }
    var cursor = 0
    block.members.forEachIndexed { index, member ->
        val lanes = laneCount(member.shape)
        val elements = block.elementsAt(index, tailElements)
        val stride = if (member.arraySize > 0 || member.unsized) {
            std430ArrayStride(member.shape) / FLOAT_BYTES
        } else {
            lanes
        }
        var element = 0
        while (element < elements) {
            val base = block.offsetAt(index) + element * stride * FLOAT_BYTES
            var lane = 0
            while (lane < lanes) {
                into[cursor] = view.getFloat(base + lane * FLOAT_BYTES)
                cursor += 1
                lane += 1
            }
            element += 1
        }
    }
    return cursor
}

private const val FLOAT_BYTES = 4

private fun vector(lanes: Int): Shape = Shape.Vector(ScalarKind.Float, Precision.High, lanes)
