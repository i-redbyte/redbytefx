package ru.redbyte.redbytefx.scene

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.hypot

class SceneMeshTest {
    @Test
    fun primitivesSharePositionNormalAndUv() {
        val meshes = listOf(
            triangle(0f, 0f, 0f, 1f, 0f, 0f, 0f, 1f, 0f),
            quad(),
            box(0f, 0f, 0f, 0.5f, 0.4f, 0.3f),
            sphere(1f, stacks = 4, slices = 6),
            torus(1f, 0.2f, majorSegments = 8, minorSegments = 6),
            disc(1f, 0.1f, segments = 8),
            extrudePolygon(listOf(0f to 0f, 1f to 0f, 0f to 1f), 0.1f),
        )
        for (mesh in meshes) {
            assertEquals(MESH_STRIDE, mesh.stride)
            assertEquals(listOf(MESH_POSITION, MESH_NORMAL, MESH_UV), mesh.attribs.map { it.name })
            assertEquals(listOf(3, 3, 2), mesh.attribs.map { it.size })
            assertEquals(listOf(0, 3, 6), mesh.attribs.map { it.offset })
            assertTrue(mesh.indices.isNotEmpty())
            var index = 6
            while (index < mesh.vertices.size) {
                assertTrue(mesh.vertices[index] in 0f..1f)
                assertTrue(mesh.vertices[index + 1] in 0f..1f)
                index += MESH_STRIDE
            }
        }
    }

    @Test
    fun everyTriangleIsCounterClockwiseFromItsNormalSide() {
        val meshes = listOf(
            triangle(0f, 0f, 0f, 1f, 0f, 0f, 0f, 1f, 0f),
            quad(2f, 1f),
            box(0f, 0f, 0f, 0.5f, 0.4f, 0.3f),
            sphere(1f, stacks = 5, slices = 7),
            torus(1f, 0.2f, majorSegments = 8, minorSegments = 6),
            disc(1f, 0.1f, segments = 8),
            extrudePolygon(listOf(0f to 0f, 1f to 0f, 0.2f to 0.8f), 0.1f),
        )
        for (mesh in meshes) {
            val v = mesh.vertices
            var corner = 0
            while (corner < mesh.indices.size) {
                val a = mesh.indices[corner] * MESH_STRIDE
                val b = mesh.indices[corner + 1] * MESH_STRIDE
                val c = mesh.indices[corner + 2] * MESH_STRIDE
                val abx = v[b] - v[a]
                val aby = v[b + 1] - v[a + 1]
                val abz = v[b + 2] - v[a + 2]
                val acx = v[c] - v[a]
                val acy = v[c + 1] - v[a + 1]
                val acz = v[c + 2] - v[a + 2]
                val nx = aby * acz - abz * acy
                val ny = abz * acx - abx * acz
                val nz = abx * acy - aby * acx
                assertTrue("degenerate triangle at $corner", nx * nx + ny * ny + nz * nz > 1.0e-10f)
                val sx = v[a + 3] + v[b + 3] + v[c + 3]
                val sy = v[a + 4] + v[b + 4] + v[c + 4]
                val sz = v[a + 5] + v[b + 5] + v[c + 5]
                assertTrue("triangle at $corner faces inward", nx * sx + ny * sy + nz * sz > 0f)
                corner += 3
            }
        }
    }

    @Test
    fun boxWeldsCornersPerFace() {
        val mesh = box(0f, 1f, 0f, 0.5f, 0.25f, 0.5f)
        assertEquals(24 * MESH_STRIDE, mesh.vertices.size)
        assertEquals(36, mesh.indices.size)
        var upward = 0
        var index = 0
        while (index < mesh.vertices.size) {
            if (mesh.vertices[index + 4] == 1f) {
                assertEquals(1.25f, mesh.vertices[index + 1], 0.0001f)
                upward += 1
            }
            index += MESH_STRIDE
        }
        assertEquals(4, upward)
        assertTrue(mesh.indices.all { it in 0 until 24 })
    }

    @Test
    fun sphereNormalsPointOutward() {
        val mesh = sphere(1f, stacks = 6, slices = 8)
        assertEquals((6 + 1) * (8 + 1) * MESH_STRIDE, mesh.vertices.size)
        assertEquals(8 * (6 * 6 - 6), mesh.indices.size)
        var index = 0
        while (index < mesh.vertices.size) {
            val dot = mesh.vertices[index] * mesh.vertices[index + 3] +
                mesh.vertices[index + 1] * mesh.vertices[index + 4] +
                mesh.vertices[index + 2] * mesh.vertices[index + 5]
            assertTrue(dot > 0.5f)
            index += MESH_STRIDE
        }
    }

    @Test
    fun torusSitsOnTheMajorCircle() {
        val mesh = torus(1f, 0.25f, majorSegments = 8, minorSegments = 4)
        val tube = hypot(hypot(mesh.vertices[0], mesh.vertices[2]) - 1f, mesh.vertices[1])
        assertEquals(0.25f, tube, 0.001f)
        val normal = abs(mesh.vertices[3]) + abs(mesh.vertices[4]) + abs(mesh.vertices[5])
        assertTrue(normal > 0.5f)
    }

    @Test
    fun transformMovesPositionsAndMergesIndexBases() {
        val moved = transform(triangle(0f, 0f, 0f, 1f, 0f, 0f, 0f, 1f, 0f), translation(2f, 0f, 0f))
        assertEquals(2f, moved.vertices[0], 0.0001f)
        val tagged = tagUv(moved, 3f, 0.25f)
        assertEquals(3f, tagged.vertices[6], 0f)
        assertEquals(0.25f, tagged.vertices[7], 0f)
        val combined = merge(listOf(moved, triangle(0f, 0f, 0f, 1f, 0f, 0f, 0f, 1f, 0f)))
        assertEquals(6, combined.indices.size)
        assertEquals(3, combined.indices[3])
        val tube = tubeAlong(circlePath(8), 0.05f, rings = 4, z = 0.1f)
        assertEquals(MESH_STRIDE, tube.stride)
        assertTrue(tube.indices.isNotEmpty())
    }

    private fun circlePath(count: Int): List<Pair<Float, Float>> = List(count) { index ->
        val angle = index / count.toFloat() * (Math.PI * 2).toFloat()
        kotlin.math.cos(angle) to kotlin.math.sin(angle)
    }
}
