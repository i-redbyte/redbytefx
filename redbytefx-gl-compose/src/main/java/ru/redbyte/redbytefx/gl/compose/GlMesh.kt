package ru.redbyte.redbytefx.gl.compose

import android.opengl.GLES30

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
    public var vertices: FloatArray = vertices
        private set

    public var indices: IntArray? = indices
        private set

    init {
        require(stride > 0) { "Stride must be positive, was $stride" }
        requireLayout(this.vertices, this.indices)
    }

    /**
     * Scene. Replaces CPU vertex (and optional index) arrays. The next draw uploads them because
     * the references changed.
     */
    public fun replace(vertices: FloatArray, indices: IntArray? = this.indices) {
        requireLayout(vertices, indices)
        this.vertices = vertices
        this.indices = indices
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
