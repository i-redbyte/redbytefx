package ru.redbyte.redbytefx.scene

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class SceneLitTest {
    @Test
    fun litMeshIsGlesOnly() {
        val lit = litTexturedMesh()
        assertThrows(IllegalStateException::class.java) { lit.program.agslSource() }
        val vertex = lit.program.vertexSource()
        val fragment = lit.program.fragmentSource()
        assertTrue(vertex.contains("a_position"))
        assertTrue(vertex.contains("a_normal"))
        assertTrue(vertex.contains("a_uv"))
        assertTrue(fragment.contains("texture("))
        assertTrue(fragment.contains("normalize("))
        assertTrue(vertex.contains("u_model"))
        assertTrue(vertex.contains("u_normal_matrix"))
        val model = checkNotNull(lit.model.components)
        val normals = checkNotNull(lit.normalMatrix.components)
        assertEquals(1f, model[0], 0f)
        assertEquals(0f, model[1], 0f)
        assertEquals(1f, model[15], 0f)
        assertEquals(1f, normals[0], 0f)
        assertEquals(0f, normals[1], 0f)
        assertEquals(1f, normals[4], 0f)
        assertEquals(1f, normals[8], 0f)
    }

    @Test
    fun aShearNormalMatrixIsTheInverseTranspose() {
        val model = floatArrayOf(
            1f, 0f, 0f, 0f,
            2f, 1f, 0f, 0f,
            0f, 0f, 1f, 0f,
            0f, 0f, 0f, 1f,
        )
        val held = FloatArray(9) { 7f }
        val singular = floatArrayOf(
            0f, 0f, 0f, 0f,
            0f, 0f, 0f, 0f,
            0f, 0f, 0f, 0f,
            0f, 0f, 0f, 1f,
        )
        assertThrows(IllegalArgumentException::class.java) { normalMatrix(singular, held) }
        assertEquals(7f, held[0], 0f)

        val normals = normalMatrix(model)
        assertEquals(1f, normals[0], 0.00001f)
        assertEquals(-2f, normals[1], 0.00001f)
        assertEquals(0f, normals[2], 0.00001f)
        assertEquals(0f, normals[3], 0.00001f)
        assertEquals(1f, normals[4], 0.00001f)
        assertEquals(1f, normals[8], 0.00001f)

        val scaled = floatArrayOf(
            2f, 0f, 0f, 0f,
            0f, 3f, 0f, 0f,
            0f, 0f, 4f, 0f,
            0f, 0f, 0f, 1f,
        )
        val scaleNormals = normalMatrix(scaled)
        assertEquals(0.5f, scaleNormals[0], 0.00001f)
        assertEquals(1f / 3f, scaleNormals[4], 0.00001f)
        assertEquals(0.25f, scaleNormals[8], 0.00001f)
        assertEquals(0f, scaleNormals[1], 0.00001f)
    }
}
