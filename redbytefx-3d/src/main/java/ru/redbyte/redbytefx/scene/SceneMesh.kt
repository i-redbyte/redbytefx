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

/** Scene. Attribute list for [MESH_POSITION], [MESH_NORMAL], and [MESH_UV]. */
public fun sceneMeshAttribs(): List<SceneAttrib> = listOf(
    SceneAttrib(MESH_POSITION, 3, 0),
    SceneAttrib(MESH_NORMAL, 3, 3),
    SceneAttrib(MESH_UV, 2, 6),
)
