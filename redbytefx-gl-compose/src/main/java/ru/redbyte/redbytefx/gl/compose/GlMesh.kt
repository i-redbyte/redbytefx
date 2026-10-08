package ru.redbyte.redbytefx.gl.compose

import android.opengl.GLES30

private val PRESERVE_MESH_INDICES = IntArray(0)

/**
 * Vertex layout uploaded on the GL thread when a [GlSurface] is created.
 *
 * Scene. [stride] and every [GlAttrib] count floats. Each attribute must fit inside one stride,
 * and every index must address a vertex. The host uploads [vertices] and [indices] on first draw.
 * A later draw of the same object skips the upload when those arrays are the same references.
 * After an in-place edit, pass a new array through [replace], or use [GlFrame.replace] for the
 * surface mesh.
 */
public class GlMesh(
    vertices: FloatArray,
    public val stride: Int,
    public val attribs: List<GlAttrib>,
    public val mode: Int = GLES30.GL_TRIANGLES,
    public val patchVertices: Int = 0,
    public val depth: Boolean = false,
    public val clearR: Float = 0.02f,
    public val clearG: Float = 0.025f,
    public val clearB: Float = 0.045f,
    indices: IntArray? = null,
) {
    @Volatile
    internal var arrays: MeshArrays = MeshArrays(vertices, indices, 0L)
        private set

    public val vertices: FloatArray get() = arrays.vertices

    public val indices: IntArray? get() = arrays.indices

    init {
        require(stride > 0) { "Stride must be positive, was $stride" }
        requireLayout(vertices, indices)
    }

    /**
     * Scene. Publishes CPU vertex and index arrays together for the GL thread. The next draw
     * uploads them even when the caller reuses the same arrays. Do not mutate an array while the
     * GL thread may be reading it; prepare its contents before calling this method.
     */
    @Synchronized
    public fun replace(vertices: FloatArray, indices: IntArray? = PRESERVE_MESH_INDICES) {
        val current = arrays
        val nextIndices = if (indices === PRESERVE_MESH_INDICES) current.indices else indices
        requireLayout(vertices, nextIndices)
        arrays = MeshArrays(vertices, nextIndices, current.revision + 1)
    }

    private fun requireLayout(vertices: FloatArray, indices: IntArray?) {
        require(vertices.size % stride == 0) {
            "Vertex buffer size ${vertices.size} is not a multiple of stride $stride"
        }
        for (index in attribs.indices) {
            val attrib = attribs[index]
            require(attrib.size in 1..MAX_ATTRIB_SIZE) { "Attribute ${attrib.name} size must be 1..4" }
            require(attrib.offset >= 0 && attrib.offset + attrib.size <= stride) {
                "Attribute ${attrib.name} does not fit in stride $stride"
            }
        }
        require(patchVertices >= 0) { "Patch vertices must be non-negative, was $patchVertices" }
        if (indices != null) requireIndexRange(indices, vertices.size / stride)
    }
}

internal class MeshArrays(
    val vertices: FloatArray,
    val indices: IntArray?,
    val revision: Long,
)

private const val MAX_ATTRIB_SIZE = 4

/** Scene. One float attribute: [size] floats at [offset] floats into each vertex. */
public class GlAttrib(
    public val name: String,
    public val size: Int,
    public val offset: Int,
)

/** Full-screen triangle for fragment-style GLES scenes. */
public fun screenMesh(
    clearR: Float = 0.02f,
    clearG: Float = 0.025f,
    clearB: Float = 0.045f,
): GlMesh = GlMesh(
    vertices = floatArrayOf(-1f, -1f, 3f, -1f, -1f, 3f),
    stride = 2,
    attribs = listOf(GlAttrib("a_corner", 2, 0)),
    clearR = clearR,
    clearG = clearG,
    clearB = clearB,
)
