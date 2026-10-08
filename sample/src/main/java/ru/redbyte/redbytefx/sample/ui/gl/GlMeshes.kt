package ru.redbyte.redbytefx.sample.ui.gl

import android.opengl.GLES32
import ru.redbyte.redbytefx.gl.compose.GlAttrib
import ru.redbyte.redbytefx.gl.compose.GlMesh
import ru.redbyte.redbytefx.gl.compose.MESH_STRIDE
import ru.redbyte.redbytefx.gl.compose.box
import ru.redbyte.redbytefx.gl.compose.meshAttribs

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

internal const val FLOOR_NEAR = 0.45f
internal const val FLOOR_SPAN = 7.2f
internal const val FLOOR_ROWS = 22
internal const val FLOOR_STEP = FLOOR_SPAN / FLOOR_ROWS

/** Static grid: the vertex shader wraps each row, so animation never uploads new vertices. */
internal fun floorMesh(): FloatArray {
    val columns = 14
    val into = FloatArray(columns * FLOOR_ROWS * 6 * 3)
    var cursor = 0
    for (row in 0 until FLOOR_ROWS) {
        val baseZ = FLOOR_NEAR + row * FLOOR_STEP
        for (column in 0 until columns) {
            val x0 = column.toFloat() / columns * 2f - 1f
            val x1 = (column + 1f) / columns * 2f - 1f
            cursor = floorVertex(into, cursor, x0, baseZ, 0f)
            cursor = floorVertex(into, cursor, x1, baseZ, 0f)
            cursor = floorVertex(into, cursor, x0, baseZ, 1f)
            cursor = floorVertex(into, cursor, x1, baseZ, 0f)
            cursor = floorVertex(into, cursor, x1, baseZ, 1f)
            cursor = floorVertex(into, cursor, x0, baseZ, 1f)
        }
    }
    return into
}

private fun floorVertex(into: FloatArray, at: Int, x: Float, baseZ: Float, edge: Float): Int {
    into[at] = x
    into[at + 1] = baseZ
    into[at + 2] = edge
    return at + 3
}

internal fun floorMeshHolder(): GlMesh = GlMesh(
    vertices = floorMesh(),
    stride = 3,
    attribs = listOf(GlAttrib("a_grid", 3, 0)),
    depth = true,
    clearR = 0.01f,
    clearG = 0.02f,
    clearB = 0.05f,
)

internal fun cityMesh(): GlMesh {
    val blocks = arrayOf(
        floatArrayOf(-0.75f, 0.28f, -0.35f, 0.32f, 0.28f, 0.32f),
        floatArrayOf(0.2f, 0.55f, -0.15f, 0.26f, 0.55f, 0.26f),
        floatArrayOf(0.75f, 0.22f, 0.35f, 0.38f, 0.22f, 0.34f),
        floatArrayOf(-0.15f, 0.42f, 0.55f, 0.28f, 0.42f, 0.28f),
        floatArrayOf(0.05f, 0.16f, -0.85f, 0.5f, 0.16f, 0.28f),
        floatArrayOf(0f, -0.02f, 0f, 1.7f, 0.02f, 1.7f),
    )
    return mergeMeshes(
        blocks.map { block -> box(block[0], block[1], block[2], block[3], block[4], block[5]) },
        clearR = 0.02f,
        clearG = 0.03f,
        clearB = 0.06f,
    )
}

internal fun mergeMeshes(
    parts: List<GlMesh>,
    clearR: Float,
    clearG: Float,
    clearB: Float,
): GlMesh {
    val vertices = ArrayList<Float>()
    val indices = ArrayList<Int>()
    for (part in parts) {
        val partIndices = requireNotNull(part.indices) { "mergeMeshes requires an index buffer" }
        val base = vertices.size / MESH_STRIDE
        for (value in part.vertices) vertices += value
        for (index in partIndices) indices += index + base
    }
    return GlMesh(
        vertices = vertices.toFloatArray(),
        stride = MESH_STRIDE,
        attribs = meshAttribs(),
        depth = true,
        clearR = clearR,
        clearG = clearG,
        clearB = clearB,
        indices = indices.toIntArray(),
    )
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
