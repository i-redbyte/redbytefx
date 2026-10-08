package ru.redbyte.redbytefx.scene

/**
 * CPU mesh. The draw mode stays with the GLES host: this type has no driver constant.
 * Triangles are the only topology produced here.
 */
public class SceneMesh(
    public val vertices: FloatArray,
    public val stride: Int,
    public val attribs: List<SceneAttrib>,
    public val indices: IntArray,
)

/** One float attribute inside [SceneMesh.vertices]. [offset] counts floats, not bytes. */
public class SceneAttrib(
    public val name: String,
    public val size: Int,
    public val offset: Int,
)

/** Spelled by `attributeVec3("position")`. */
public const val MESH_POSITION: String = "a_position"

/** Spelled by `attributeVec3("normal")`. */
public const val MESH_NORMAL: String = "a_normal"

/** Spelled by `attributeVec2("uv")`. */
public const val MESH_UV: String = "a_uv"

/** Floats per vertex: position, normal, then uv. */
public const val MESH_STRIDE: Int = 8

internal fun checkedVertexCount(count: Long): Int {
    require(count in 1..(Int.MAX_VALUE / MESH_STRIDE).toLong()) { "Mesh vertex count is too large: $count" }
    return count.toInt()
}

internal fun checkedIndexCount(count: Long): Int {
    require(count in 1..Int.MAX_VALUE.toLong()) { "Mesh index count is too large: $count" }
    return count.toInt()
}

internal fun requireFiniteVertex(
    x: Float,
    y: Float,
    z: Float,
    nx: Float,
    ny: Float,
    nz: Float,
    u: Float,
    v: Float,
) {
    require(
        x.isFinite() && y.isFinite() && z.isFinite() &&
            nx.isFinite() && ny.isFinite() && nz.isFinite() &&
            u.isFinite() && v.isFinite(),
    ) { "Generated mesh vertex must be finite" }
}

/** Scene. Attribute list for [MESH_POSITION], [MESH_NORMAL], and [MESH_UV]. */
public fun sceneMeshAttribs(): List<SceneAttrib> = listOf(
    SceneAttrib(MESH_POSITION, 3, 0),
    SceneAttrib(MESH_NORMAL, 3, 3),
    SceneAttrib(MESH_UV, 2, 6),
)
