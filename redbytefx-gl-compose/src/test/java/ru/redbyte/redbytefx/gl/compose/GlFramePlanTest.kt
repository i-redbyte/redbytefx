package ru.redbyte.redbytefx.gl.compose

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.redbyte.redbytefx.ShaderTarget
import ru.redbyte.redbytefx.gl.GlProgramRuntime
import ru.redbyte.redbytefx.lit
import ru.redbyte.redbytefx.shader
import ru.redbyte.redbytefx.vec4

class GlFramePlanTest {
    private val triangle = GlMesh(
        vertices = floatArrayOf(0f, 0f, 0f, 1f, 0f, 0f, 0f, 1f, 0f),
        stride = 3,
        attribs = listOf(GlAttrib("a_position", 3, 0)),
    )

    @Test
    fun aPresentDrawIsOnlyValidOnTheScreenOfASurfaceWithAPresentProgram() {
        val list = DrawList()
        assertThrows(IllegalStateException::class.java) { list.draw(triangle, 0, 3, null, true, false, 3) }
        assertThrows(IllegalArgumentException::class.java) {
            list.offscreen { list.draw(triangle, 0, 3, null, true, true, 3) }
        }
        list.draw(triangle, 0, 3, null, true, true, 3)
        assertTrue(list.screen().single().present)
    }

    @Test
    fun anEmptyDrawIsNotRecorded() {
        val list = DrawList()
        list.draw(triangle, 0, 0, null, false, false, 3)
        list.draw(triangle, 0, 3, FloatArray(0), false, false, 3)
        list.draw(triangle, 3, -1, null, false, false, 3)
        assertEquals(0, list.recordedCount())
        list.draw(triangle, 0, 3, null, false, false, 3)
        assertEquals(1, list.recordedCount())
    }

    @Test
    fun aWholeMeshDrawStartsAtFirstAndReadsTheCountWhenItRuns() {
        val list = DrawList()
        list.draw(triangle, 1, -1, null, false, false, 3)
        val draw = list.screen().single()
        assertTrue(draw.wholeMesh)
        assertEquals(2, draw.count)
        assertEquals(1, executedDrawCount(wholeMesh = true, first = 1, requested = 2, indexCount = null, vertexCount = 2))
        assertEquals(17, executedDrawCount(wholeMesh = true, first = 1, requested = 2, indexCount = 18, vertexCount = 24))
        assertEquals(6, executedDrawCount(wholeMesh = false, first = 0, requested = 6, indexCount = 18, vertexCount = 24))
        assertThrows(IllegalArgumentException::class.java) {
            executedDrawCount(wholeMesh = false, first = 0, requested = 6, indexCount = 3, vertexCount = 24)
        }
        assertThrows(IllegalArgumentException::class.java) {
            executedDrawCount(wholeMesh = true, first = 4, requested = 0, indexCount = 3, vertexCount = 24)
        }
    }

    @Test
    fun aRangePastTheMeshIsAnArgumentError() {
        val list = DrawList()
        assertThrows(IllegalArgumentException::class.java) { list.draw(triangle, 2, 2, null, false, false, 3) }
        assertThrows(IllegalArgumentException::class.java) { list.draw(triangle, 4, -1, null, false, false, 3) }
        assertThrows(IllegalArgumentException::class.java) { list.draw(triangle, -1, 1, null, false, false, 3) }
        assertThrows(IllegalArgumentException::class.java) {
            list.draw(triangle, 0, 1, FloatArray(MODEL_MATRIX_FLOATS + 1), false, false, 3)
        }
        assertThrows(IllegalArgumentException::class.java) { requireIndexRange(intArrayOf(0, 4), 3) }
        assertThrows(IllegalArgumentException::class.java) { requireIndexRange(intArrayOf(), 3) }
        requireIndexRange(intArrayOf(0, 2), 3)
    }

    @Test
    fun resetReusesRecordsAndLeavesOffscreenMode() {
        val list = DrawList()
        list.offscreen { list.draw(triangle, 0, 3, null, false, false, 3) }
        val first = list.offscreen().single()
        list.reset()
        assertEquals(0, list.recordedCount())
        list.draw(triangle, 0, 3, null, false, false, 3)
        assertSame(first, list.screen().single())
        assertTrue(list.offscreen().isEmpty())
    }

    @Test
    fun offscreenDrawsWithoutARenderTargetAreRejected() {
        val error = assertThrows(IllegalStateException::class.java) {
            requireOffscreenTarget(renderToTexture = false, offscreenCount = 1)
        }
        assertTrue(error.message!!.contains("renderToTexture"))
        requireOffscreenTarget(renderToTexture = true, offscreenCount = 2)
        requireOffscreenTarget(renderToTexture = false, offscreenCount = 0)
    }

    @Test
    fun theDepthTestFollowsTheSurfaceFlagOrTheMesh() {
        assertFalse(drawDepthTest(force = false, meshDepth = false))
        assertTrue(drawDepthTest(force = false, meshDepth = true))
        assertTrue(drawDepthTest(force = true, meshDepth = false))
    }

    @Test
    fun aSurfaceDrawIsSizedByTheLastReplace() {
        val uploads = mutableListOf<Int>()
        val frame = GlFrame(runtime(), { vertices, _ -> uploads += vertices.size }, triangle, null)
        assertThrows(IllegalArgumentException::class.java) { frame.draw(first = 0, count = 4) }
        frame.replace(FloatArray(5 * 3))
        frame.draw(first = 0, count = 5)
        assertEquals(5, frame.drawList().screen().single().count)
        assertThrows(IllegalArgumentException::class.java) { frame.replace(FloatArray(4)) }
        assertThrows(IllegalArgumentException::class.java) {
            frame.replace(FloatArray(2 * 3), intArrayOf(0, 1, 2))
        }
        frame.replace(FloatArray(3 * 3), intArrayOf(0, 1, 2, 2, 1, 0))
        frame.draw()
        assertEquals(6, frame.drawList().screen()[1].count)
        assertEquals(listOf(15, 9), uploads)
    }

    @Test
    fun theReleasedConstructorForwardsVerticesAndNeedsAnExplicitMesh() {
        var uploaded: FloatArray? = null
        val vertices = FloatArray(6)
        val frame = GlFrame(runtime(), 2f, 1.5f) { uploaded = it }
        assertEquals(2f, frame.seconds)
        assertEquals(1.5f, frame.aspect)
        frame.replace(vertices)
        assertSame(vertices, uploaded)
        assertThrows(IllegalArgumentException::class.java) { frame.draw() }
        frame.draw(triangle)
        assertEquals(3, frame.drawList().screen().single().count)
    }

    @Test
    fun aMeshRejectsALayoutPastItsStrideOrIndicesPastItsVertices() {
        val position = listOf(GlAttrib("a_position", 3, 0))
        assertThrows(IllegalArgumentException::class.java) { GlMesh(FloatArray(6), 0, position) }
        assertThrows(IllegalArgumentException::class.java) { GlMesh(FloatArray(7), 3, position) }
        assertThrows(IllegalArgumentException::class.java) {
            GlMesh(FloatArray(6), 2, position)
        }
        assertThrows(IllegalArgumentException::class.java) {
            GlMesh(FloatArray(6), 3, position, indices = intArrayOf(0, 2))
        }
        assertThrows(IllegalArgumentException::class.java) {
            GlMesh(FloatArray(6), 3, position, indices = intArrayOf())
        }
    }

    private fun runtime(): GlProgramRuntime = GlProgramRuntime(
        shader(ShaderTarget.Gles30) {
            vertex { glPosition(vec4(0f.lit, 0f.lit, 0f.lit, 1f.lit)) }
            fragment { vec4(0f.lit, 0f.lit, 0f.lit, 1f.lit) }
        },
        FloatDevice(),
    )
}
