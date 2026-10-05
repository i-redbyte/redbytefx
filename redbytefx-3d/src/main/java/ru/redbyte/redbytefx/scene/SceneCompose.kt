package ru.redbyte.redbytefx.scene

/**
 * Scene. Moves [mesh] by a column-major [matrix]. Positions use the full 4×4.
 * Normals use the inverse-transpose 3×3 from [normalMatrix].
 */
public fun transform(mesh: SceneMesh, matrix: FloatArray): SceneMesh {
    require(mesh.stride == MESH_STRIDE) { "transform needs stride $MESH_STRIDE, was ${mesh.stride}" }
    require(matrix.size >= MATRIX_FLOATS) { "Matrix needs $MATRIX_FLOATS floats, was ${matrix.size}" }
    val normals = normalMatrix(matrix)
    val out = mesh.vertices.copyOf()
    var index = 0
    while (index < out.size) {
        val x = out[index]
        val y = out[index + 1]
        val z = out[index + 2]
        val nx = out[index + 3]
        val ny = out[index + 4]
        val nz = out[index + 5]
        out[index] = matrix[0] * x + matrix[4] * y + matrix[8] * z + matrix[12]
        out[index + 1] = matrix[1] * x + matrix[5] * y + matrix[9] * z + matrix[13]
        out[index + 2] = matrix[2] * x + matrix[6] * y + matrix[10] * z + matrix[14]
        out[index + 3] = normals[0] * nx + normals[3] * ny + normals[6] * nz
        out[index + 4] = normals[1] * nx + normals[4] * ny + normals[7] * nz
        out[index + 5] = normals[2] * nx + normals[5] * ny + normals[8] * nz
        index += MESH_STRIDE
    }
    return SceneMesh(out, mesh.stride, mesh.attribs, mesh.indices.copyOf())
}

/** Scene. Writes [u] (and optional [v]) into every vertex UV. */
public fun tagUv(mesh: SceneMesh, u: Float, v: Float? = null): SceneMesh {
    require(mesh.stride == MESH_STRIDE) { "tagUv needs stride $MESH_STRIDE, was ${mesh.stride}" }
    val out = mesh.vertices.copyOf()
    var index = 6
    while (index < out.size) {
        out[index] = u
        if (v != null) out[index + 1] = v
        index += MESH_STRIDE
    }
    return SceneMesh(out, mesh.stride, mesh.attribs, mesh.indices.copyOf())
}

/** Scene. Concatenates indexed triangle meshes that share [MESH_STRIDE]. */
public fun merge(parts: List<SceneMesh>): SceneMesh {
    require(parts.isNotEmpty()) { "merge needs at least one mesh" }
    val vertices = ArrayList<Float>()
    val indices = ArrayList<Int>()
    for (part in parts) {
        require(part.stride == MESH_STRIDE) { "merge needs stride $MESH_STRIDE, was ${part.stride}" }
        val base = vertices.size / MESH_STRIDE
        for (value in part.vertices) vertices += value
        for (index in part.indices) indices += index + base
    }
    return SceneMesh(
        vertices = vertices.toFloatArray(),
        stride = MESH_STRIDE,
        attribs = sceneMeshAttribs(),
        indices = indices.toIntArray(),
    )
}

internal class MeshWriter {
    private val vertices = ArrayList<Float>()
    private val indices = ArrayList<Int>()

    fun vertex(x: Float, y: Float, z: Float, nx: Float, ny: Float, nz: Float, u: Float, v: Float): Int {
        val id = vertices.size / MESH_STRIDE
        vertices += x
        vertices += y
        vertices += z
        vertices += nx
        vertices += ny
        vertices += nz
        vertices += u
        vertices += v
        return id
    }

    fun tri(a: Int, b: Int, c: Int) {
        indices += a
        indices += b
        indices += c
    }

    fun mesh(): SceneMesh {
        require(indices.isNotEmpty()) { "Mesh needs at least one triangle" }
        return SceneMesh(
            vertices = vertices.toFloatArray(),
            stride = MESH_STRIDE,
            attribs = sceneMeshAttribs(),
            indices = indices.toIntArray(),
        )
    }
}
