package ru.redbyte.redbytefx.gl.compose

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class GlPrimitivesTest {
    @Test
    fun boxAdaptsTheSceneMesh() {
        val mesh = box(0f, 1f, 0f, 0.5f, 0.25f, 0.5f)
        assertEquals(MESH_STRIDE, mesh.stride)
        assertEquals(listOf(MESH_POSITION, MESH_NORMAL, MESH_UV), mesh.attribs.map { it.name })
        assertEquals(listOf(3, 3, 2), mesh.attribs.map { it.size })
        assertEquals(listOf(0, 3, 6), mesh.attribs.map { it.offset })
        assertEquals(24 * MESH_STRIDE, mesh.vertices.size)
        assertEquals(36, mesh.indices?.size)
        assertNotNull(mesh.indices)
        val discMesh = disc(1f, 0.1f, segments = 8)
        assertEquals(MESH_STRIDE, discMesh.stride)
        assertNotNull(discMesh.indices)
    }
}
