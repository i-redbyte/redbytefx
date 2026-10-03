package ru.redbyte.redbytefx.gl.compose

import ru.redbyte.redbytefx.scene.MESH_NORMAL as SCENE_NORMAL
import ru.redbyte.redbytefx.scene.MESH_POSITION as SCENE_POSITION
import ru.redbyte.redbytefx.scene.MESH_STRIDE as SCENE_STRIDE
import ru.redbyte.redbytefx.scene.MESH_UV as SCENE_UV
import ru.redbyte.redbytefx.scene.SceneMesh
import ru.redbyte.redbytefx.scene.box as sceneBox
import ru.redbyte.redbytefx.scene.quad as sceneQuad
import ru.redbyte.redbytefx.scene.sceneMeshAttribs
import ru.redbyte.redbytefx.scene.sphere as sceneSphere
import ru.redbyte.redbytefx.scene.torus as sceneTorus
import ru.redbyte.redbytefx.scene.triangle as sceneTriangle

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

internal fun SceneMesh.toGlMesh(): GlMesh = GlMesh(
    vertices = vertices,
    stride = stride,
    attribs = attribs.map { attrib -> GlAttrib(attrib.name, attrib.size, attrib.offset) },
    depth = true,
    indices = indices,
)
