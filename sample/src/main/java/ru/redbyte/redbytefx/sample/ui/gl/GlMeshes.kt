package ru.redbyte.redbytefx.sample.ui.gl

import android.opengl.GLES32
import ru.redbyte.redbytefx.gl.compose.GlAttrib
import ru.redbyte.redbytefx.gl.compose.GlMesh
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

internal fun uvGrid(columns: Int, rows: Int): FloatArray {
    val data = FloatArray(columns * rows * 6 * 2)
    var cursor = 0
    for (row in 0 until rows) {
        val v0 = row.toFloat() / rows
        val v1 = (row + 1f) / rows
        for (column in 0 until columns) {
            val u0 = column.toFloat() / columns
            val u1 = (column + 1f) / columns
            val quad = floatArrayOf(u0, v0, u1, v0, u0, v1, u1, v0, u1, v1, u0, v1)
            quad.copyInto(data, cursor)
            cursor += quad.size
        }
    }
    return data
}

internal fun sheetMesh(columns: Int, rows: Int, depth: Boolean): GlMesh = GlMesh(
    vertices = uvGrid(columns, rows),
    stride = 2,
    attribs = listOf(GlAttrib("a_uv", 2, 0)),
    depth = depth,
    clearR = 0.03f,
    clearG = 0.035f,
    clearB = 0.06f,
)

internal fun floorMesh(scroll: Float): FloatArray {
    val columns = 14
    val rows = 22
    val near = 0.45f
    val span = 7.2f
    val step = span / rows
    val distances = FloatArray(rows + 1) { index ->
        val raw = near + index * step - scroll
        val wrapped = ((raw - near) % span + span) % span + near
        wrapped
    }
    val into = ArrayList<Float>(columns * rows * 12)
    for (row in 0 until rows) {
        val z0 = distances[row]
        val z1 = distances[row + 1]
        if (kotlin.math.abs(z1 - z0) > step * 1.5f) continue
        for (column in 0 until columns) {
            val x0 = column.toFloat() / columns * 2f - 1f
            val x1 = (column + 1f) / columns * 2f - 1f
            into += x0
            into += z0
            into += x1
            into += z0
            into += x0
            into += z1
            into += x1
            into += z0
            into += x1
            into += z1
            into += x0
            into += z1
        }
    }
    return into.toFloatArray()
}

internal fun floorMeshHolder(): GlMesh = GlMesh(
    vertices = floorMesh(0f),
    stride = 2,
    attribs = listOf(GlAttrib("a_xz", 2, 0)),
    depth = true,
    clearR = 0.01f,
    clearG = 0.02f,
    clearB = 0.05f,
)

internal fun solidPositions(radius: Float): GlMesh = GlMesh(
    vertices = icosahedron(radius, shaded = false),
    stride = 4,
    attribs = listOf(GlAttrib("a_position", 4, 0)),
    depth = true,
    clearR = 0.02f,
    clearG = 0.02f,
    clearB = 0.035f,
)

internal fun solidShaded(radius: Float): GlMesh = GlMesh(
    vertices = icosahedron(radius, shaded = true),
    stride = 8,
    attribs = listOf(GlAttrib("a_position", 4, 0), GlAttrib("a_normal", 4, 4)),
    depth = true,
    clearR = 0.03f,
    clearG = 0.02f,
    clearB = 0.04f,
)

internal fun cityMesh(): GlMesh {
    val into = ArrayList<Float>()
    val blocks = arrayOf(
        floatArrayOf(-0.75f, 0.28f, -0.35f, 0.32f, 0.28f, 0.32f, 0f),
        floatArrayOf(0.2f, 0.55f, -0.15f, 0.26f, 0.55f, 0.26f, 1f),
        floatArrayOf(0.75f, 0.22f, 0.35f, 0.38f, 0.22f, 0.34f, 2f),
        floatArrayOf(-0.15f, 0.42f, 0.55f, 0.28f, 0.42f, 0.28f, 3f),
        floatArrayOf(0.05f, 0.16f, -0.85f, 0.5f, 0.16f, 0.28f, 4f),
        floatArrayOf(0f, -0.02f, 0f, 1.7f, 0.02f, 1.7f, 5f),
    )
    for (block in blocks) {
        writeBox(block[0], block[1], block[2], block[3], block[4], block[5]) { x, y, z, _ ->
            into += x
            into += y
            into += z
            into += block[6]
        }
    }
    return GlMesh(
        vertices = into.toFloatArray(),
        stride = 4,
        attribs = listOf(GlAttrib("a_position", 4, 0)),
        depth = true,
        clearR = 0.02f,
        clearG = 0.03f,
        clearB = 0.06f,
    )
}

internal fun rainbowArch(): GlMesh {
    val arcs = 48
    val tube = 12
    val start = 0.22f
    val sweep = PI.toFloat() - start * 2f
    val into = ArrayList<Float>(arcs * tube * 6 * 8)
    for (arc in 0 until arcs) {
        val t0 = start + sweep * arc / arcs
        val t1 = start + sweep * (arc + 1) / arcs
        for (slice in 0 until tube) {
            val p0 = PI.toFloat() * 2f * slice / tube
            val p1 = PI.toFloat() * 2f * (slice + 1) / tube
            addArchTriangle(into, t0, p0, t1, p0, t1, p1, start)
            addArchTriangle(into, t0, p0, t1, p1, t0, p1, start)
        }
    }
    return GlMesh(
        vertices = into.toFloatArray(),
        stride = 8,
        attribs = listOf(GlAttrib("a_position", 4, 0), GlAttrib("a_tint", 4, 4)),
        depth = true,
        clearR = 0.015f,
        clearG = 0.016f,
        clearB = 0.03f,
    )
}

private fun addArchTriangle(
    into: MutableList<Float>,
    t0: Float,
    p0: Float,
    t1: Float,
    p1: Float,
    t2: Float,
    p2: Float,
    start: Float,
) {
    addArchSample(into, t0, p0, start)
    addArchSample(into, t1, p1, start)
    addArchSample(into, t2, p2, start)
}

private fun addArchSample(into: MutableList<Float>, theta: Float, phi: Float, start: Float) {
    val major = 0.92f
    val minor = 0.1f
    val apex = PI.toFloat() / 2f
    val ring = major + cos(phi) * minor
    val down = (abs(theta - apex) / (apex - start)).coerceIn(0f, 1f)
    val shade = 0.3f + 0.7f * (-sin(phi)).coerceIn(0f, 1f)
    into += cos(theta) * ring
    into += sin(theta) * ring - 0.22f
    into += sin(phi) * minor
    into += 1f
    into += shade
    into += down
    into += 0f
    into += 1f
}

internal fun oceanPatch(): GlMesh = GlMesh(
    vertices = floatArrayOf(
        -1.15f, 0f, -0.1f, 1f,
        1.15f, 0f, -0.1f, 1f,
        1.15f, 0f, 1.7f, 1f,
        -1.15f, 0f, 1.7f, 1f,
    ),
    stride = 4,
    attribs = listOf(GlAttrib("a_corner", 4, 0)),
    mode = GLES32.GL_PATCHES,
    patchVertices = 4,
    depth = true,
    clearR = 0.01f,
    clearG = 0.04f,
    clearB = 0.08f,
)

internal fun writeBox(
    cx: Float,
    cy: Float,
    cz: Float,
    hx: Float,
    hy: Float,
    hz: Float,
    write: (Float, Float, Float, Float) -> Unit,
) {
    val x0 = cx - hx
    val x1 = cx + hx
    val y0 = cy - hy
    val y1 = cy + hy
    val z0 = cz - hz
    val z1 = cz + hz
    quad(write, 0.95f, x0, y1, z1, x1, y1, z1, x1, y1, z0, x0, y1, z0)
    quad(write, 0.35f, x0, y0, z0, x1, y0, z0, x1, y0, z1, x0, y0, z1)
    quad(write, 0.8f, x0, y0, z1, x1, y0, z1, x1, y1, z1, x0, y1, z1)
    quad(write, 0.55f, x1, y0, z0, x0, y0, z0, x0, y1, z0, x1, y1, z0)
    quad(write, 0.7f, x1, y0, z1, x1, y0, z0, x1, y1, z0, x1, y1, z1)
    quad(write, 0.5f, x0, y0, z0, x0, y0, z1, x0, y1, z1, x0, y1, z0)
}

private fun quad(
    write: (Float, Float, Float, Float) -> Unit,
    shade: Float,
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
) {
    write(ax, ay, az, shade)
    write(bx, by, bz, shade)
    write(cx, cy, cz, shade)
    write(ax, ay, az, shade)
    write(cx, cy, cz, shade)
    write(dx, dy, dz, shade)
}

private fun icosahedron(radius: Float, shaded: Boolean): FloatArray {
    val phi = (1f + sqrt(5f)) / 2f
    val raw = arrayOf(
        floatArrayOf(-1f, phi, 0f),
        floatArrayOf(1f, phi, 0f),
        floatArrayOf(-1f, -phi, 0f),
        floatArrayOf(1f, -phi, 0f),
        floatArrayOf(0f, -1f, phi),
        floatArrayOf(0f, 1f, phi),
        floatArrayOf(0f, -1f, -phi),
        floatArrayOf(0f, 1f, -phi),
        floatArrayOf(phi, 0f, -1f),
        floatArrayOf(phi, 0f, 1f),
        floatArrayOf(-phi, 0f, -1f),
        floatArrayOf(-phi, 0f, 1f),
    )
    val vertices = Array(raw.size) { index ->
        val item = raw[index]
        val length = sqrt(item[0] * item[0] + item[1] * item[1] + item[2] * item[2])
        floatArrayOf(item[0] / length * radius, item[1] / length * radius, item[2] / length * radius)
    }
    val faces = intArrayOf(
        0, 11, 5, 0, 5, 1, 0, 1, 7, 0, 7, 10, 0, 10, 11,
        1, 5, 9, 5, 11, 4, 11, 10, 2, 10, 7, 6, 7, 1, 8,
        3, 9, 4, 3, 4, 2, 3, 2, 6, 3, 6, 8, 3, 8, 9,
        4, 9, 5, 2, 4, 11, 6, 2, 10, 8, 6, 7, 9, 8, 1,
    )
    val into = ArrayList<Float>(faces.size * if (shaded) 8 else 4)
    var face = 0
    while (face < faces.size) {
        val a = vertices[faces[face]]
        val b = vertices[faces[face + 1]]
        val c = vertices[faces[face + 2]]
        val nx = (b[1] - a[1]) * (c[2] - a[2]) - (b[2] - a[2]) * (c[1] - a[1])
        val ny = (b[2] - a[2]) * (c[0] - a[0]) - (b[0] - a[0]) * (c[2] - a[2])
        val nz = (b[0] - a[0]) * (c[1] - a[1]) - (b[1] - a[1]) * (c[0] - a[0])
        val length = sqrt(nx * nx + ny * ny + nz * nz).coerceAtLeast(0.0001f)
        val ux = nx / length
        val uy = ny / length
        val uz = nz / length
        for (point in arrayOf(a, b, c)) {
            into += point[0]
            into += point[1]
            into += point[2]
            into += 1f
            if (shaded) {
                into += ux
                into += uy
                into += uz
                into += 0f
            }
        }
        face += 3
    }
    return into.toFloatArray()
}
