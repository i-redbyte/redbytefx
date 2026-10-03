package ru.redbyte.redbytefx.scene

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
    }
}
