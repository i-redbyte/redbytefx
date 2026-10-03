package ru.redbyte.redbytefx.gl

/** Largest vertex index that still fits in a `GL_UNSIGNED_SHORT` element. */
public const val INDEX_SHORT_LIMIT: Int = 65535

/** Which draw call a frame should issue. */
public enum class DrawKind {
    None,
    Arrays,
    Elements,
    ArraysInstanced,
    ElementsInstanced,
}

/** Element width chosen from the index values. */
public enum class IndexElementKind {
    UnsignedShort,
    UnsignedInt,
}

/** Whether an array-buffer upload rewrites the whole store or a matching prefix. */
public enum class BufferUploadKind {
    Full,
    Sub,
}

/**
 * Device. Chooses the draw call from counts the caller already resolved.
 *
 * A null [indexCount] means no index buffer was passed. A null [instanceCount] means no instance
 * buffer was passed. An index count of zero is an argument error: an empty index buffer is not a
 * silent no-op. An instance count of zero, or a vertex count of zero, issues no call.
 * An instance count of one still selects an instanced call.
 */
public fun planDraw(vertexCount: Int, indexCount: Int?, instanceCount: Int?): DrawKind {
    require(vertexCount >= 0) { "Vertex count must be non-negative, was $vertexCount" }
    if (indexCount != null) {
        require(indexCount > 0) { "Index buffer must contain at least one index" }
    }
    if (instanceCount != null) {
        require(instanceCount >= 0) { "Instance count must be non-negative, was $instanceCount" }
    }
    if (vertexCount == 0 || instanceCount == 0) return DrawKind.None
    val indexed = indexCount != null
    val instanced = instanceCount != null
    return when {
        indexed && instanced -> DrawKind.ElementsInstanced
        indexed -> DrawKind.Elements
        instanced -> DrawKind.ArraysInstanced
        else -> DrawKind.Arrays
    }
}

/**
 * Device. `GL_UNSIGNED_INT` when any index is above [INDEX_SHORT_LIMIT], otherwise
 * `GL_UNSIGNED_SHORT`. An empty [indices] array, or a negative index, is an argument error.
 */
public fun indexElementKind(indices: IntArray): IndexElementKind {
    require(indices.isNotEmpty()) { "Index buffer must contain at least one index" }
    var highest = 0
    for (index in indices) {
        require(index >= 0) { "Index must be non-negative, was $index" }
        if (index > highest) highest = index
    }
    return if (highest > INDEX_SHORT_LIMIT) IndexElementKind.UnsignedInt else IndexElementKind.UnsignedShort
}

/**
 * Device. A positive [previousCount] that matches [nextCount] uses a sub-data upload.
 * The first upload, and any size change, uses a full upload.
 */
public fun planBufferUpload(previousCount: Int, nextCount: Int): BufferUploadKind {
    require(previousCount >= 0) { "Previous float count must be non-negative, was $previousCount" }
    require(nextCount >= 0) { "Next float count must be non-negative, was $nextCount" }
    return if (previousCount > 0 && previousCount == nextCount) {
        BufferUploadKind.Sub
    } else {
        BufferUploadKind.Full
    }
}
