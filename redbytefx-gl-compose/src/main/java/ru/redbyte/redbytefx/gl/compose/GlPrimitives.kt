package ru.redbyte.redbytefx.gl.compose

import ru.redbyte.redbytefx.scene.SceneMesh
import ru.redbyte.redbytefx.scene.sceneMeshAttribs
import ru.redbyte.redbytefx.scene.MESH_NORMAL as SCENE_NORMAL
import ru.redbyte.redbytefx.scene.MESH_POSITION as SCENE_POSITION
import ru.redbyte.redbytefx.scene.MESH_STRIDE as SCENE_STRIDE
import ru.redbyte.redbytefx.scene.MESH_UV as SCENE_UV
import ru.redbyte.redbytefx.scene.box as sceneBox
import ru.redbyte.redbytefx.scene.disc as sceneDisc
import ru.redbyte.redbytefx.scene.extrudePolygon as sceneExtrude
import ru.redbyte.redbytefx.scene.merge as sceneMerge
import ru.redbyte.redbytefx.scene.quad as sceneQuad
import ru.redbyte.redbytefx.scene.sphere as sceneSphere
import ru.redbyte.redbytefx.scene.tagUv as sceneTagUv
import ru.redbyte.redbytefx.scene.torus as sceneTorus
import ru.redbyte.redbytefx.scene.transform as sceneTransform
import ru.redbyte.redbytefx.scene.triangle as sceneTriangle
import ru.redbyte.redbytefx.scene.tubeAlong as sceneTube

/**
 * Triangle meshes that share one vertex layout.
 *
 * Each vertex is [MESH_STRIDE] floats:
 * - [MESH_POSITION] (`a_position`), vec3, offset 0 — spelled by `attributeVec3("position")`
 * - [MESH_NORMAL] (`a_normal`), vec3, offset 3 — spelled by `attributeVec3("normal")`
 * - [MESH_UV] (`a_uv`), vec2, offset 6, in 0..1 — spelled by `attributeVec2("uv")`
 *
 * The positions are CPU data from `redbytefx-3d`. This file only adapts them to [GlMesh].
 * A shader that does not declare one of these attributes leaves that location unused.
 */
public const val MESH_POSITION: String = SCENE_POSITION

public const val MESH_NORMAL: String = SCENE_NORMAL

public const val MESH_UV: String = SCENE_UV

public const val MESH_STRIDE: Int = SCENE_STRIDE

/** Attribute list for [MESH_POSITION], [MESH_NORMAL], and [MESH_UV]. */
public fun meshAttribs(): List<GlAttrib> = sceneMeshAttribs().map { attrib ->
    GlAttrib(attrib.name, attrib.size, attrib.offset)
}

/** Scene. Adapts a CPU triangle to a [GlMesh] with the same indices. */
public fun triangle(
    ax: Float,
    ay: Float,
    az: Float,
    bx: Float,
    by: Float,
    bz: Float,
    cx: Float,
    cy: Float,
    cz: Float,
): GlMesh = sceneTriangle(ax, ay, az, bx, by, bz, cx, cy, cz).toGlMesh()

/** Scene. Adapts a CPU quad to a [GlMesh] with the same indices. */
public fun quad(width: Float = 1f, height: Float = 1f): GlMesh = sceneQuad(width, height).toGlMesh()

/** Scene. Adapts a CPU box to a [GlMesh] with the same indices. */
public fun box(
    centerX: Float,
    centerY: Float,
    centerZ: Float,
    halfX: Float,
    halfY: Float,
    halfZ: Float,
): GlMesh = sceneBox(centerX, centerY, centerZ, halfX, halfY, halfZ).toGlMesh()

/** Scene. Adapts a CPU sphere to a [GlMesh] with the same indices. */
public fun sphere(radius: Float, stacks: Int = 16, slices: Int = 24): GlMesh =
    sceneSphere(radius, stacks, slices).toGlMesh()

/** Scene. Adapts a CPU torus to a [GlMesh] with the same indices. */
public fun torus(
    major: Float,
    minor: Float,
    majorSegments: Int = 32,
    minorSegments: Int = 16,
): GlMesh = sceneTorus(major, minor, majorSegments, minorSegments).toGlMesh()

/** Scene. Filled disc in XY with a short rim. */
public fun disc(radius: Float, halfZ: Float, segments: Int = 48): GlMesh =
    sceneDisc(radius, halfZ, segments).toGlMesh()

/** Scene. Prism along Z from a closed XY outline. */
public fun extrudePolygon(outline: List<Pair<Float, Float>>, halfZ: Float): GlMesh =
    sceneExtrude(outline, halfZ).toGlMesh()

/** Scene. Tube along a closed XY path. */
public fun tubeAlong(
    path: List<Pair<Float, Float>>,
    radius: Float,
    rings: Int = 8,
    z: Float = 0f,
): GlMesh = sceneTube(path, radius, rings, z).toGlMesh()

/** Scene. Moves [mesh] by a column-major [matrix]. */
public fun transform(mesh: GlMesh, matrix: FloatArray): GlMesh =
    sceneTransform(mesh.toSceneMesh(), matrix).toGlMesh(mesh)

/** Scene. Writes [u] (and optional [v]) into every vertex UV. */
public fun tagUv(mesh: GlMesh, u: Float, v: Float? = null): GlMesh =
    sceneTagUv(mesh.toSceneMesh(), u, v).toGlMesh(mesh)

/** Scene. Concatenates indexed triangle meshes that share [MESH_STRIDE]. */
public fun merge(parts: List<GlMesh>): GlMesh {
    require(parts.isNotEmpty()) { "merge needs at least one mesh" }
    val head = parts.first()
    return sceneMerge(parts.map { it.toSceneMesh() }).toGlMesh(head)
}

internal fun SceneMesh.toGlMesh(): GlMesh = GlMesh(
    vertices = vertices,
    stride = stride,
    attribs = attribs.map { attrib -> GlAttrib(attrib.name, attrib.size, attrib.offset) },
    depth = true,
    indices = indices,
)

private fun SceneMesh.toGlMesh(style: GlMesh): GlMesh = GlMesh(
    vertices = vertices,
    stride = stride,
    attribs = style.attribs,
    mode = style.mode,
    patchVertices = style.patchVertices,
    depth = style.depth,
    clearR = style.clearR,
    clearG = style.clearG,
    clearB = style.clearB,
    indices = indices,
)

private fun GlMesh.toSceneMesh(): SceneMesh {
    val elements = requireNotNull(indices) { "transform needs indices" }
    return SceneMesh(vertices, stride, sceneMeshAttribs(), elements)
}
