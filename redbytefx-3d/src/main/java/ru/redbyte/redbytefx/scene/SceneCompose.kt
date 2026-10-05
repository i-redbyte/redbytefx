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
    var vertexFloats = 0L
    var indexCount = 0L
    for (part in parts) {
        require(part.stride == MESH_STRIDE) { "merge needs stride $MESH_STRIDE, was ${part.stride}" }
        require(part.vertices.size % MESH_STRIDE == 0) { "merge needs complete vertices" }
        val vertexCount = part.vertices.size / MESH_STRIDE
        require(part.indices.size % 3 == 0 && part.indices.all { it in 0 until vertexCount }) {
            "merge needs triangle indices inside the vertex range"
        }
        vertexFloats += part.vertices.size
        indexCount += part.indices.size
    }
    require(vertexFloats <= Int.MAX_VALUE && indexCount <= Int.MAX_VALUE) { "Merged mesh is too large" }
    val vertices = FloatArray(vertexFloats.toInt())
    val indices = IntArray(indexCount.toInt())
    var vertexOffset = 0
    var indexOffset = 0
    for (part in parts) {
        part.vertices.copyInto(vertices, vertexOffset)
        val base = vertexOffset / MESH_STRIDE
        for (index in part.indices) indices[indexOffset++] = index + base
        vertexOffset += part.vertices.size
    }
    return SceneMesh(
        vertices = vertices,
        stride = MESH_STRIDE,
        attribs = sceneMeshAttribs(),
        indices = indices,
    )
}

internal class MeshWriter {
    private var vertices = FloatArray(64)
    private var indices = IntArray(96)
    private var vertexFloats = 0
    private var indexCount = 0

    fun vertex(x: Float, y: Float, z: Float, nx: Float, ny: Float, nz: Float, u: Float, v: Float): Int {
        if (vertexFloats + MESH_STRIDE > vertices.size) vertices = vertices.copyOf(vertices.size * 2)
        val id = vertexFloats / MESH_STRIDE
        vertices[vertexFloats] = x
        vertices[vertexFloats + 1] = y
        vertices[vertexFloats + 2] = z
        vertices[vertexFloats + 3] = nx
        vertices[vertexFloats + 4] = ny
        vertices[vertexFloats + 5] = nz
        vertices[vertexFloats + 6] = u
        vertices[vertexFloats + 7] = v
        vertexFloats += MESH_STRIDE
        return id
    }

    fun tri(a: Int, b: Int, c: Int) {
        if (indexCount + 3 > indices.size) indices = indices.copyOf(indices.size * 2)
        indices[indexCount] = a
        indices[indexCount + 1] = b
        indices[indexCount + 2] = c
        indexCount += 3
    }

    fun mesh(): SceneMesh {
        require(indexCount > 0) { "Mesh needs at least one triangle" }
        return SceneMesh(
            vertices = vertices.copyOf(vertexFloats),
            stride = MESH_STRIDE,
            attribs = sceneMeshAttribs(),
            indices = indices.copyOf(indexCount),
        )
    }
}
