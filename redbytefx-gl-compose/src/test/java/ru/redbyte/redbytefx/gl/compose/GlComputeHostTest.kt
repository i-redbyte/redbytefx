package ru.redbyte.redbytefx.gl.compose

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.redbyte.redbytefx.ShaderTarget
import ru.redbyte.redbytefx.StorageBlock
import ru.redbyte.redbytefx.gl.GlProgramRuntime
import ru.redbyte.redbytefx.lit
import ru.redbyte.redbytefx.shader
import ru.redbyte.redbytefx.vec4

class GlComputeHostTest {
    @Test
    fun aComputeHostLinksOnTheGlThreadAndDoesNotDraw() {
        lateinit var cells: StorageBlock
        val program = shader(ShaderTarget.Gles31) {
            cells = storageBlock("cells") { float("value") }
            compute(8) { }
        }
        val device = FloatDevice()
        val runtime = GlProgramRuntime(program, device).also { it.link() }
        val controller = GlController(program, GlSurfaceConfig())
        val slot = GlSlot()
        slot.post = { block -> block() }
        val queued = ArrayDeque<() -> Unit>()
        val queue: (() -> Unit) -> Unit = { block -> queued.addLast(block) }
        slot.queue = queue
        controller.attachQueue(queue)
        var linkCalls = 0
        val renderer = ComputeRenderer(controller, null, slot) { _, _ ->
            linkCalls += 1
            runtime
        }
        renderer.onSurfaceCreated(null, null)
        renderer.onDrawFrame(null)
        assertEquals(1, linkCalls)
        assertTrue(controller.linkState.value is GlLinkState.Linked)
        assertEquals(0, device.drawCalls)

        controller.set(cells, floatArrayOf(3f))
        controller.dispatch(2)
        val into = FloatArray(1)
        var count = -1
        controller.read(cells, into) { count = it }
        while (queued.isNotEmpty()) queued.removeFirst().invoke()
        assertEquals(1, device.dispatchCalls)
        assertEquals(1, count)
        assertEquals(3f, into[0], 0f)
        assertEquals(0, device.drawCalls)
    }

    @Test
    fun aGraphicsProgramIsRejectedBeforeLink() {
        val program = shader(ShaderTarget.Gles30) {
            vertex { glPosition(vec4(0f.lit, 0f.lit, 0f.lit, 1f.lit)) }
            fragment { vec4(0f.lit, 0f.lit, 0f.lit, 1f.lit) }
        }
        val controller = GlController(program, GlSurfaceConfig())
        val slot = GlSlot()
        slot.post = { block -> block() }
        val queue: (() -> Unit) -> Unit = { }
        slot.queue = queue
        controller.attachQueue(queue)
        var linkCalls = 0
        val renderer = ComputeRenderer(controller, null, slot) { _, _ ->
            linkCalls += 1
            error("link")
        }
        renderer.onSurfaceCreated(null, null)
        renderer.onDrawFrame(null)
        assertEquals(0, linkCalls)
        val failed = controller.linkState.value as GlLinkState.Failed
        assertEquals(COMPUTE_HOST_TARGET, failed.message)
    }
}
