package ru.redbyte.redbytefx.scene

import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

private const val TWO_PI: Float = (2.0 * PI).toFloat()

/**
 * Scene. One triangle. The normal is the unit cross product of `(b - a)` and `(c - a)`,
 * so the corners are counter-clockwise seen from the normal side.
 * UVs are `(0,0)`, `(1,0)`, `(0,1)`. Three vertices and three indices.
 * Collinear corners throw [IllegalArgumentException].
 */
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
): SceneMesh {
    val abx = bx - ax
    val aby = by - ay
    val abz = bz - az
    val acx = cx - ax
    val acy = cy - ay
    val acz = cz - az
    val nx = aby * acz - abz * acy
    val ny = abz * acx - abx * acz
    val nz = abx * acy - aby * acx
    val length = sqrt(nx * nx + ny * ny + nz * nz)
    require(length > DEGENERATE_AREA) { "triangle corners must not be collinear" }
    val builder = SceneBuilder(vertices = 3, indices = 3)
    val a = builder.vertex(ax, ay, az, nx / length, ny / length, nz / length, 0f, 0f)
    val b = builder.vertex(bx, by, bz, nx / length, ny / length, nz / length, 1f, 0f)
    val c = builder.vertex(cx, cy, cz, nx / length, ny / length, nz / length, 0f, 1f)
    builder.tri(a, b, c)
    return builder.mesh()
}

/**
 * Scene. Axis-aligned quad on the XY plane, centered at the origin, facing `+Z`.
 * [width] and [height] are full edge lengths. Four vertices and six indices. UVs cover 0..1.
 */
public fun quad(width: Float = 1f, height: Float = 1f): SceneMesh {
    require(width > 0f) { "quad width must be positive, was $width" }
    require(height > 0f) { "quad height must be positive, was $height" }
    val hx = width * 0.5f
    val hy = height * 0.5f
    val builder = SceneBuilder(vertices = 4, indices = 6)
    builder.face(
        -hx, -hy, 0f,
        hx, -hy, 0f,
        hx, hy, 0f,
        -hx, hy, 0f,
        0f, 0f, 1f,
    )
    return builder.mesh()
}

/**
 * Scene. Axis-aligned box. [centerX], [centerY], and [centerZ] are the center.
 * [halfX], [halfY], and [halfZ] are half extents. Each face keeps its own four vertices so the
 * UV seam stays 0..1, with an outward normal. Twenty-four vertices and thirty-six indices.
 */
public fun box(
    centerX: Float,
    centerY: Float,
    centerZ: Float,
    halfX: Float,
    halfY: Float,
    halfZ: Float,
): SceneMesh {
    require(halfX > 0f && halfY > 0f && halfZ > 0f) {
        "box half extents must be positive, was ($halfX, $halfY, $halfZ)"
    }
    val x0 = centerX - halfX
    val x1 = centerX + halfX
    val y0 = centerY - halfY
    val y1 = centerY + halfY
    val z0 = centerZ - halfZ
    val z1 = centerZ + halfZ
    val builder = SceneBuilder(vertices = 24, indices = 36)
    builder.face(x0, y1, z1, x1, y1, z1, x1, y1, z0, x0, y1, z0, 0f, 1f, 0f)
    builder.face(x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1, 0f, -1f, 0f)
    builder.face(x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1, 0f, 0f, 1f)
    builder.face(x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0, 0f, 0f, -1f)
    builder.face(x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1, 1f, 0f, 0f)
    builder.face(x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0, -1f, 0f, 0f)
    return builder.mesh()
}

/**
 * Scene. UV sphere centered at the origin. Normals point outward and triangles are
 * counter-clockwise seen from outside.
 * [stacks] are latitudinal bands (at least 2) and [slices] are longitudinal bands (at least 3).
 * The seam keeps a second column of vertices so U can be both 0 and 1. The two polar bands
 * are one triangle per slice, so the index count is `slices * (6 * stacks - 6)`.
 */
public fun sphere(radius: Float, stacks: Int = 16, slices: Int = 24): SceneMesh {
    require(radius > 0f) { "sphere radius must be positive, was $radius" }
    require(stacks >= 2) { "sphere stacks must be at least 2, was $stacks" }
    require(slices >= 3) { "sphere slices must be at least 3, was $slices" }
    val columns = slices + 1
    val builder = SceneBuilder(vertices = (stacks + 1) * columns, indices = slices * (6 * stacks - 6))
    for (stack in 0..stacks) {
        val v = stack.toFloat() / stacks
        val theta = v * PI.toFloat()
        val sinTheta = sin(theta)
        val ny = cos(theta)
        for (slice in 0..slices) {
            val u = slice.toFloat() / slices
            val phi = u * TWO_PI
            val nx = sinTheta * cos(phi)
            val nz = sinTheta * sin(phi)
            builder.vertex(nx * radius, ny * radius, nz * radius, nx, ny, nz, u, v)
        }
    }
    for (stack in 0 until stacks) {
        for (slice in 0 until slices) {
            val a = stack * columns + slice
            val b = a + columns
            val c = b + 1
            val d = a + 1
            if (stack != stacks - 1) builder.tri(a, c, b)
            if (stack != 0) builder.tri(a, d, c)
        }
    }
    return builder.mesh()
}

/**
 * Scene. Torus centered at the origin. The major circle lies in the XZ plane (tube around Y),
 * the same placement as the stdlib `sdTorus` distance. Triangles are counter-clockwise seen
 * from outside the tube.
 * [major] is the distance from the origin to the tube center. [minor] is the tube radius.
 * The UV seam duplicates the first ring of vertices.
 */
public fun torus(
    major: Float,
    minor: Float,
    majorSegments: Int = 32,
    minorSegments: Int = 16,
): SceneMesh {
    require(major > 0f) { "torus major radius must be positive, was $major" }
    require(minor > 0f) { "torus minor radius must be positive, was $minor" }
    require(majorSegments >= 3) { "torus major segments must be at least 3, was $majorSegments" }
    require(minorSegments >= 3) { "torus minor segments must be at least 3, was $minorSegments" }
    val columns = minorSegments + 1
    val builder = SceneBuilder(
        vertices = (majorSegments + 1) * columns,
        indices = majorSegments * minorSegments * 6,
    )
    for (majorIndex in 0..majorSegments) {
        val u = majorIndex.toFloat() / majorSegments
        val theta = u * TWO_PI
        val cosTheta = cos(theta)
        val sinTheta = sin(theta)
        for (minorIndex in 0..minorSegments) {
            val v = minorIndex.toFloat() / minorSegments
            val phi = v * TWO_PI
            val cosPhi = cos(phi)
            val nx = cosTheta * cosPhi
            val ny = sin(phi)
            val nz = sinTheta * cosPhi
            builder.vertex(
                cosTheta * major + nx * minor,
                ny * minor,
                sinTheta * major + nz * minor,
                nx,
                ny,
                nz,
                u,
                v,
            )
        }
    }
    for (majorIndex in 0 until majorSegments) {
        for (minorIndex in 0 until minorSegments) {
            val a = majorIndex * columns + minorIndex
            val b = a + columns
            val c = b + 1
            val d = a + 1
            builder.tri(a, c, b)
            builder.tri(a, d, c)
        }
    }
    return builder.mesh()
}

private const val DEGENERATE_AREA: Float = 1.0e-12f

private class SceneBuilder(vertices: Int, indices: Int) {
    private val data = FloatArray(vertices * MESH_STRIDE)
    private val index = IntArray(indices)
    private var floats = 0
    private var corners = 0

    fun vertex(
        x: Float,
        y: Float,
        z: Float,
        nx: Float,
        ny: Float,
        nz: Float,
        u: Float,
        v: Float,
    ): Int {
        val id = floats / MESH_STRIDE
        data[floats] = x
        data[floats + 1] = y
        data[floats + 2] = z
        data[floats + 3] = nx
        data[floats + 4] = ny
        data[floats + 5] = nz
        data[floats + 6] = u
        data[floats + 7] = v
        floats += MESH_STRIDE
        return id
    }

    fun tri(a: Int, b: Int, c: Int) {
        index[corners] = a
        index[corners + 1] = b
        index[corners + 2] = c
        corners += 3
    }

    fun face(
        ax: Float,
        ay: Float,
        az: Float,
        bx: Float,
        by: Float,
        bz: Float,
        cx: Float,
        cy: Float,
        cz: Float,
        dx: Float,
        dy: Float,
        dz: Float,
        nx: Float,
        ny: Float,
        nz: Float,
    ) {
        val a = vertex(ax, ay, az, nx, ny, nz, 0f, 0f)
        val b = vertex(bx, by, bz, nx, ny, nz, 1f, 0f)
        val c = vertex(cx, cy, cz, nx, ny, nz, 1f, 1f)
        val d = vertex(dx, dy, dz, nx, ny, nz, 0f, 1f)
        tri(a, b, c)
        tri(a, c, d)
    }

    fun mesh(): SceneMesh {
        check(floats == data.size && corners == index.size) { "Scene mesh size was planned wrong" }
        return SceneMesh(
            vertices = data,
            stride = MESH_STRIDE,
            attribs = sceneMeshAttribs(),
            indices = index,
        )
    }
}
