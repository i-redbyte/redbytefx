package ru.redbyte.redbytefx.gl.compose

import android.opengl.GLES30
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.redbyte.redbytefx.ShaderTarget
import ru.redbyte.redbytefx.gl.GlProgramRuntime
import ru.redbyte.redbytefx.lit
import ru.redbyte.redbytefx.shader
import ru.redbyte.redbytefx.vec4
import java.util.concurrent.CountDownLatch
import kotlin.concurrent.thread

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
        assertThrows(IllegalArgumentException::class.java) {
            list.draw(triangle, 0, 3, null, true, true, 3, material = 2f)
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
    fun resetReleasesTransientMeshesAndInstanceArrays() {
        val list = DrawList()
        val instances = FloatArray(MODEL_MATRIX_FLOATS)
        repeat(200) { list.draw(triangle, 0, 3, instances, false, false, 3) }
        val first = list.screen().first()
        val last = list.screen().last()
        list.reset()
        assertNull(first.mesh)
        assertNull(first.instances)
        assertNull(last.mesh)
        assertNull(last.instances)
        list.draw(triangle, 0, 3, null, false, false, 3)
        assertSame(first, list.screen().single())
    }

    @Test
    fun aSecondPassDisablesAttribsTheMeshNoLongerUses() {
        val disabled = ArrayList<Int>()
        forEachStaleAttrib(intArrayOf(0, 1, 3), 3, intArrayOf(0, 2), 2) { disabled += it }
        assertEquals(listOf(1, 3), disabled)
        disabled.clear()
        forEachStaleAttrib(intArrayOf(0), 1, intArrayOf(0, 1), 2) { disabled += it }
        assertTrue(disabled.isEmpty())
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
        val frame = GlFrame(runtime(), 2f, 1.5f, { uploaded = it }, 320f, 180f)
        assertEquals(2f, frame.seconds)
        assertEquals(1.5f, frame.aspect)
        assertEquals(320f, frame.widthPx)
        assertEquals(180f, frame.heightPx)
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

    @Test
    fun aConcurrentVertexReplacementKeepsTheLatestIndices() {
        val mesh = GlMesh(FloatArray(9), 3, listOf(GlAttrib("a_position", 3, 0)), indices = intArrayOf(0, 1, 2))
        val updatedIndices = intArrayOf(2, 1, 0)
        val started = CountDownLatch(1)
        var failure: Throwable? = null
        lateinit var worker: Thread
        synchronized(mesh) {
            worker = thread {
                started.countDown()
                try {
                    mesh.replace(FloatArray(9))
                } catch (error: Throwable) {
                    failure = error
                }
            }
            started.await()
            val deadline = System.nanoTime() + 5_000_000_000L
            while (worker.state != Thread.State.BLOCKED && System.nanoTime() < deadline) Thread.yield()
            assertEquals(Thread.State.BLOCKED, worker.state)
            mesh.replace(FloatArray(9), updatedIndices)
        }
        worker.join(5_000)
        assertFalse(worker.isAlive)
        failure?.let { throw it }
        assertSame(updatedIndices, mesh.indices)
    }

    @Test
    fun aPassAppliesItsPipelineAndTheNextPassRestoresTheDefault() {
        val custom = GlPipeline(
            blend = true,
            srcFactor = BlendFactor.SrcAlpha,
            dstFactor = BlendFactor.OneMinusSrcAlpha,
            equation = BlendEquation.Add,
            scissor = true,
            x = 1,
            y = 2,
            width = 4,
            height = 5,
            writeRed = false,
            depthMask = false,
        )
        val list = DrawList()
        list.draw(triangle, 0, 3, null, false, false, 3, custom)
        list.draw(triangle, 0, 3, null, false, false, 3)
        assertTrue(list.screen()[0].pipeline.blend)
        assertFalse(list.screen()[1].pipeline.blend)
        assertSame(GlPipeline.Default, list.screen()[1].pipeline)

        val ops = RecordingOps()
        applyPipeline(custom, ops)
        applyPipeline(GlPipeline.Default, ops)
        assertEquals(
            listOf(
                "blend",
                "func ${GLES30.GL_SRC_ALPHA} ${GLES30.GL_ONE_MINUS_SRC_ALPHA}",
                "eq ${GLES30.GL_FUNC_ADD}",
                "scissorOn",
                "box 1 2 4 5",
                "mask false true true true",
                "depth false",
                "cullOff",
                "blendOff",
                "scissorOff",
                "mask true true true true",
                "depth true",
                "cullOff",
            ),
            ops.lines,
        )
        val culled = GlPipeline(cullFace = CullFace.Back)
        ops.lines.clear()
        applyPipeline(culled, ops)
        assertEquals(listOf("blendOff", "scissorOff", "mask true true true true", "depth true", "cull", "cullFace ${GLES30.GL_BACK}"), ops.lines)
        assertThrows(IllegalArgumentException::class.java) {
            applyPipeline(GlPipeline(scissor = true, width = 0, height = 2), ops)
        }
    }

    @Test
    fun aModelIsCopiedAndCannotBeCombinedWithInstances() {
        val list = DrawList()
        val pose = FloatArray(MODEL_MATRIX_FLOATS) { index -> if (index % 5 == 0) 1f else 0f }
        pose[12] = 3f
        list.draw(triangle, 0, 3, null, false, false, 3, GlPipeline.Default, material = 2f, model = pose)
        pose[12] = 9f
        val draw = list.screen().single()
        assertEquals(3f, draw.instances!![12], 0f)
        assertTrue(draw.hasMaterial)
        assertEquals(2f, draw.materialValue, 0f)
        assertThrows(IllegalArgumentException::class.java) {
            list.draw(
                triangle,
                0,
                3,
                FloatArray(MODEL_MATRIX_FLOATS),
                false,
                false,
                3,
                GlPipeline.Default,
                null,
                pose,
            )
        }
    }

    @Test
    fun meshPublishesVertexAndIndexArraysAsOneRevision() {
        val initial = triangle.arrays
        val replacement = FloatArray(4 * triangle.stride)
        val indices = intArrayOf(0, 1, 2, 0, 2, 3)
        triangle.replace(replacement, indices)
        val published = triangle.arrays
        assertSame(replacement, published.vertices)
        assertSame(indices, published.indices)
        assertEquals(initial.revision + 1, published.revision)
        triangle.replace(replacement, indices)
        assertEquals(published.revision + 1, triangle.arrays.revision)
    }

    @Test
    fun replacingTheSurfaceMeshUpdatesFrameDrawBounds() {
        val frame = GlFrame(runtime(), { _, _ -> }, triangle, null)
        assertThrows(IllegalArgumentException::class.java) { frame.draw(count = 6) }
        triangle.replace(FloatArray(4 * triangle.stride), intArrayOf(0, 1, 2, 0, 2, 3))
        frame.draw(count = 6)
        assertEquals(6, frame.drawList().screen().single().count)
    }

    private fun runtime(): GlProgramRuntime = GlProgramRuntime(
        shader(ShaderTarget.Gles30) {
            vertex { glPosition(vec4(0f.lit, 0f.lit, 0f.lit, 1f.lit)) }
            fragment { vec4(0f.lit, 0f.lit, 0f.lit, 1f.lit) }
        },
        FloatDevice(),
    )
}

private class RecordingOps : PipelineOps {
    val lines = mutableListOf<String>()

    override fun blend(enabled: Boolean) {
        lines += if (enabled) "blend" else "blendOff"
    }

    override fun blendFunc(src: Int, dst: Int) {
        lines += "func $src $dst"
    }

    override fun blendEquation(equation: Int) {
        lines += "eq $equation"
    }

    override fun scissorTest(enabled: Boolean) {
        lines += if (enabled) "scissorOn" else "scissorOff"
    }

    override fun scissor(x: Int, y: Int, width: Int, height: Int) {
        lines += "box $x $y $width $height"
    }

    override fun colorMask(red: Boolean, green: Boolean, blue: Boolean, alpha: Boolean) {
        lines += "mask $red $green $blue $alpha"
    }

    override fun depthMask(enabled: Boolean) {
        lines += "depth $enabled"
    }

    override fun cull(enabled: Boolean) {
        lines += if (enabled) "cull" else "cullOff"
    }

    override fun cullFace(mode: Int) {
        lines += "cullFace $mode"
    }
}
