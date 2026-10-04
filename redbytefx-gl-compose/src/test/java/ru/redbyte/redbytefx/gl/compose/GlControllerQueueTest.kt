package ru.redbyte.redbytefx.gl.compose

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.redbyte.redbytefx.Flt
import ru.redbyte.redbytefx.High
import ru.redbyte.redbytefx.Sampler2D
import ru.redbyte.redbytefx.float2
import ru.redbyte.redbytefx.ShaderTarget
import ru.redbyte.redbytefx.StorageBlock
import ru.redbyte.redbytefx.Uniform
import ru.redbyte.redbytefx.UniformBlock
import ru.redbyte.redbytefx.gl.GlCompileStatus
import ru.redbyte.redbytefx.gl.GlDevice
import ru.redbyte.redbytefx.gl.GlProgramRuntime
import ru.redbyte.redbytefx.gl.GlStage
import ru.redbyte.redbytefx.lit
import ru.redbyte.redbytefx.shader
import ru.redbyte.redbytefx.vec4
import ru.redbyte.redbytefx.x

class GlControllerQueueTest {
    @Test
    fun uniformWritesBeforeLinkAreCoalescedAndAppliedOnDrain() {
        lateinit var amount: Uniform<Flt<High>>
        val program = shader(ShaderTarget.Gles30) {
            amount = uniform("amount", 0f)
            vertex { glPosition(vec4(0f.lit, 0f.lit, 0f.lit, 1f.lit)) }
            fragment { vec4(amount.expr, 0f.lit, 0f.lit, 1f.lit) }
        }
        val device = FloatDevice()
        val runtime = GlProgramRuntime(program, device)
        runtime.link()
        val afterLink = device.floatCalls
        val controller = GlController(program, GlSurfaceConfig())
        var taskRan = false
        controller.set(amount, 0.2f)
        controller.set(amount, 0.8f)
        controller.runOnGl { taskRan = true }
        assertEquals(afterLink, device.floatCalls)
        assertFalse(taskRan)

        val queue: (() -> Unit) -> Unit = { block -> block() }
        controller.attachQueue(queue)
        controller.attachRuntime(queue, runtime)

        assertEquals(afterLink + 1, device.floatCalls)
        assertEquals(0.8f, device.lastFloat)
        assertEquals(true, taskRan)
    }

    @Test
    fun aNewContextReceivesTheLatestValuesButNotOneShotTasksOrTextureNames() {
        lateinit var amount: Uniform<Flt<High>>
        lateinit var image: Uniform<Sampler2D>
        val program = shader(ShaderTarget.Gles30) {
            amount = uniform("amount", 0f)
            image = sampler2D("image")
            vertex { glPosition(vec4(0f.lit, 0f.lit, 0f.lit, 1f.lit)) }
            fragment {
                val texel = texture(image, float2(0f, 0f))
                vec4(amount.expr, texel.x, 0f.lit, 1f.lit)
            }
        }
        val controller = GlController(program, GlSurfaceConfig())
        val queued = mutableListOf<() -> Unit>()
        val queue: (() -> Unit) -> Unit = { block -> queued += block }
        controller.attachQueue(queue)
        val first = FloatDevice()
        controller.attachRuntime(queue, GlProgramRuntime(program, first).also { it.link() })
        var tasks = 0
        controller.set(amount, 0.6f)
        controller.bind(image, 7)
        controller.runOnGl { tasks += 1 }
        assertEquals(1, queued.size)
        queued.removeAt(0).invoke()
        assertEquals(0.6f, first.lastFloat)
        assertEquals(1, first.textureBinds)
        assertEquals(1, tasks)

        controller.detachRuntime(GlProgramRuntime(program, FloatDevice()))
        assertEquals(true, controller.runtime != null)
        controller.detachRuntime(requireNotNull(controller.runtime))
        val second = FloatDevice()
        controller.attachRuntime(queue, GlProgramRuntime(program, second).also { it.link() })
        assertEquals(0.6f, second.lastFloat)
        assertEquals(0, second.textureBinds)
        assertEquals(1, tasks)
    }

    @Test
    fun aDetachedQueueDoesNotBlockTheNextSurface() {
        val (program, amount) = amountProgram()
        val controller = GlController(program, GlSurfaceConfig())
        val lost = mutableListOf<() -> Unit>()
        val oldQueue: (() -> Unit) -> Unit = { block -> lost += block }
        controller.attachQueue(oldQueue)
        controller.set(amount, 0.3f)
        assertEquals(false, controller.detachQueue { })
        controller.set(amount, 0.4f)
        assertEquals(1, lost.size)
        assertEquals(true, controller.detachQueue(oldQueue))
        val queued = mutableListOf<() -> Unit>()
        val queue: (() -> Unit) -> Unit = { block -> queued += block }
        controller.attachQueue(queue)
        val device = FloatDevice()
        controller.attachRuntime(queue, GlProgramRuntime(program, device).also { it.link() })
        assertEquals(0.4f, device.lastFloat)
        controller.set(amount, 0.9f)
        assertEquals(1, queued.size)
        queued.forEach { it() }
        assertEquals(0.9f, device.lastFloat)
    }

    @Test
    fun aNewSurfaceNeverWritesThroughTheProgramOfTheOldOne() {
        val (program, amount) = amountProgram()
        val controller = GlController(program, GlSurfaceConfig())
        val oldQueue: (() -> Unit) -> Unit = { }
        controller.attachQueue(oldQueue)
        val oldDevice = FloatDevice()
        controller.attachRuntime(oldQueue, GlProgramRuntime(program, oldDevice).also { it.link() })
        val oldWrites = oldDevice.floatCalls

        val queued = mutableListOf<() -> Unit>()
        val newQueue: (() -> Unit) -> Unit = { block -> queued += block }
        controller.attachQueue(newQueue)
        controller.set(amount, 0.5f)
        queued.toList().forEach { it() }
        assertEquals(oldWrites, oldDevice.floatCalls)

        controller.attachRuntime(oldQueue, GlProgramRuntime(program, FloatDevice()).also { it.link() })
        controller.set(amount, 0.7f)
        queued.toList().forEach { it() }
        assertEquals(oldWrites, oldDevice.floatCalls)

        val newDevice = FloatDevice()
        controller.attachRuntime(newQueue, GlProgramRuntime(program, newDevice).also { it.link() })
        assertEquals(0.7f, newDevice.lastFloat)
    }

    @Test
    fun aFailingWriteDoesNotDropTheRestOfTheDrain() {
        lateinit var bad: Uniform<Flt<High>>
        lateinit var good: Uniform<Flt<High>>
        val program = shader(ShaderTarget.Gles30) {
            bad = uniform("bad", 0f)
            good = uniform("good", 0f)
            vertex { glPosition(vec4(0f.lit, 0f.lit, 0f.lit, 1f.lit)) }
            fragment { vec4(bad.expr, good.expr, 0f.lit, 1f.lit) }
        }
        val device = FloatDevice()
        device.rejectNegative = true
        val runtime = GlProgramRuntime(program, device).also { it.link() }
        val queued = ArrayDeque<() -> Unit>()
        val queue: (() -> Unit) -> Unit = { queued.addLast(it) }
        val controller = GlController(program, GlSurfaceConfig())
        controller.attachQueue(queue)
        controller.attachRuntime(queue, runtime)
        var followed = false
        controller.set(bad, -1f)
        controller.set(good, 0.5f)
        controller.runOnGl { followed = true }
        var thrown = 0
        var steps = 0
        while (queued.isNotEmpty() && steps < 8) {
            steps += 1
            try {
                queued.removeFirst().invoke()
            } catch (error: IllegalStateException) {
                thrown += 1
            }
        }
        assertEquals(1, thrown)
        assertEquals(0.5f, device.lastFloat)
        assertTrue(followed)
    }

    @Test
    fun the129thGlTaskFailsAndTheFirstOneStays() {
        val (program, amount) = amountProgram()
        val controller = GlController(program, GlSurfaceConfig())
        val queued = ArrayDeque<() -> Unit>()
        val queue: (() -> Unit) -> Unit = { queued.addLast(it) }
        controller.attachQueue(queue)
        controller.attachRuntime(queue, GlProgramRuntime(program, FloatDevice()).also { it.link() })
        repeat(200) { controller.set(amount, 0.1f) }
        val ran = ArrayList<Int>()
        repeat(128) { index -> controller.runOnGl { ran += index } }
        assertThrows(IllegalStateException::class.java) { controller.runOnGl { ran += 128 } }
        while (queued.isNotEmpty()) queued.removeFirst().invoke()
        assertEquals(0, ran.first())
        assertEquals(127, ran.last())
        assertEquals(128, ran.size)
    }

    @Test
    fun blockWritesReplayOnANewContext() {
        lateinit var gain: UniformBlock
        lateinit var cells: StorageBlock
        val program = shader(ShaderTarget.Gles31) {
            gain = uniformBlock("frame") { float("gain") }
            cells = storageBlock("cells") { float("value") }
            compute(8) { }
        }
        val controller = GlController(program, GlSurfaceConfig())
        val queued = mutableListOf<() -> Unit>()
        val queue: (() -> Unit) -> Unit = { block -> queued += block }
        controller.attachQueue(queue)
        controller.set(gain, floatArrayOf(0.25f))
        controller.set(gain, floatArrayOf(0.5f))
        controller.set(cells, floatArrayOf(4f))
        val first = FloatDevice()
        controller.attachRuntime(queue, GlProgramRuntime(program, first).also { it.link() })
        assertEquals(0.5f, firstFloat(first.uniformPayloads.single()), 0f)
        assertEquals(4f, firstFloat(first.storageBytes.values.single()), 0f)

        controller.detachRuntime(requireNotNull(controller.runtime))
        val second = FloatDevice()
        controller.attachRuntime(queue, GlProgramRuntime(program, second).also { it.link() })
        assertEquals(0.5f, firstFloat(second.uniformPayloads.single()), 0f)
        assertEquals(4f, firstFloat(second.storageBytes.values.single()), 0f)
    }

    @Test
    fun dispatchSurvivesAFailedWriteAndReadFillsTheCallerBuffer() {
        lateinit var bad: Uniform<Flt<High>>
        lateinit var cells: StorageBlock
        val program = shader(ShaderTarget.Gles31) {
            bad = uniform("bad", 0f)
            cells = storageBlock("cells") { float("value") }
            compute(8) { }
        }
        val device = FloatDevice()
        device.rejectNegative = true
        val runtime = GlProgramRuntime(program, device).also { it.link() }
        val queued = ArrayDeque<() -> Unit>()
        val queue: (() -> Unit) -> Unit = { queued.addLast(it) }
        val controller = GlController(program, GlSurfaceConfig())
        controller.attachQueue(queue)
        controller.attachRuntime(queue, runtime)
        controller.set(bad, -1f)
        controller.set(cells, floatArrayOf(4f))
        controller.dispatch(2, 3, 4)
        val into = FloatArray(1)
        var count = -1
        controller.read(cells, into) { count = it }
        assertEquals(-1, count)
        assertEquals(0f, into[0], 0f)
        var thrown = 0
        var steps = 0
        while (queued.isNotEmpty() && steps < 8) {
            steps += 1
            try {
                queued.removeFirst().invoke()
            } catch (error: IllegalStateException) {
                thrown += 1
            }
        }
        assertEquals(1, thrown)
        assertEquals(1, device.dispatchCalls)
        assertEquals(listOf(2, 3, 4), device.dispatchGroups)
        assertEquals(1, count)
        assertEquals(4f, into[0], 0f)
    }

    private fun firstFloat(bytes: ByteArray): Float =
        java.nio.ByteBuffer.wrap(bytes).order(java.nio.ByteOrder.nativeOrder()).getFloat(0)

    private fun amountProgram(): Pair<ru.redbyte.redbytefx.ShaderProgram, Uniform<Flt<High>>> {
        lateinit var amount: Uniform<Flt<High>>
        val program = shader(ShaderTarget.Gles30) {
            amount = uniform("amount", 0f)
            vertex { glPosition(vec4(0f.lit, 0f.lit, 0f.lit, 1f.lit)) }
            fragment { vec4(amount.expr, 0f.lit, 0f.lit, 1f.lit) }
        }
        return program to amount
    }
}

internal class FloatDevice : GlDevice() {
    var floatCalls: Int = 0
    var lastFloat: Float = Float.NaN
    var rejectNegative: Boolean = false

    override fun createShader(stage: GlStage): Int = 1
    override fun shaderSource(shader: Int, source: String) = Unit
    override fun compileShader(shader: Int): GlCompileStatus = GlCompileStatus(true, "")
    override fun deleteShader(shader: Int) = Unit
    override fun createProgram(): Int = 1
    override fun attachShader(program: Int, shader: Int) = Unit
    override fun linkProgram(program: Int): GlCompileStatus = GlCompileStatus(true, "")
    override fun deleteProgram(program: Int) = Unit
    override fun uniformLocation(program: Int, name: String): Int = 1
    override fun attribLocation(program: Int, name: String): Int = 0
    override fun uniform1f(location: Int, value: Float) {
        if (rejectNegative && value < 0f) error("rejected")
        floatCalls += 1
        lastFloat = value
    }
    override fun uniform2f(location: Int, x: Float, y: Float) = Unit
    override fun uniform3f(location: Int, x: Float, y: Float, z: Float) = Unit
    override fun uniform4f(location: Int, x: Float, y: Float, z: Float, w: Float) = Unit
    override fun uniform1i(location: Int, value: Int) = Unit
    override fun uniformMatrix2fv(location: Int, values: FloatArray) = Unit
    override fun uniformMatrix3fv(location: Int, values: FloatArray) = Unit
    override fun uniformMatrix4fv(location: Int, values: FloatArray) = Unit
    override fun maxCombinedTextureImageUnits(): Int = 8
    override fun useProgram(program: Int) = Unit
    override fun activeTexture(unit: Int) = Unit
    var textureBinds: Int = 0
    override fun bindTexture2D(texture: Int) {
        textureBinds += 1
    }
    override fun bindTextureCube(texture: Int) = Unit
    override fun createTexture(): Int = 1
    override fun deleteTexture(texture: Int) = Unit
    override fun texture2DLinearRepeat(texture: Int) = Unit
    override fun texImage2DRgba(texture: Int, width: Int, height: Int, rgba: ByteArray) = Unit
    var dispatchCalls = 0
    val dispatchGroups = mutableListOf<Int>()
    override fun dispatchCompute(x: Int, y: Int, z: Int) {
        dispatchCalls += 1
        dispatchGroups += x
        dispatchGroups += y
        dispatchGroups += z
    }
    override fun shaderStorageBarrier() = Unit
    private var nextBuffer = 1
    val uniformPayloads = mutableListOf<ByteArray>()
    val storageBytes = HashMap<Int, ByteArray>()
    override fun createBuffer(): Int = nextBuffer++
    override fun deleteBuffer(buffer: Int) = Unit
    override fun uniformBufferData(buffer: Int, data: ByteArray) {
        uniformPayloads += data.copyOf()
    }
    override fun uniformBufferSubData(buffer: Int, data: ByteArray) {
        uniformPayloads += data.copyOf()
    }
    override fun bindUniformBufferBase(buffer: Int, binding: Int) = Unit
    override fun uniformBlockIndex(program: Int, name: String): Int = 0
    override fun uniformBlockBinding(program: Int, blockIndex: Int, binding: Int) = Unit
    override fun maxUniformBufferBindings(): Int = 24
    override fun maxShaderStorageBufferBindings(): Int = 8
    override fun shaderStorageData(buffer: Int, data: ByteArray) {
        storageBytes[buffer] = data.copyOf()
    }
    override fun shaderStorageSubData(buffer: Int, data: ByteArray) {
        storageBytes[buffer] = data.copyOf()
    }
    override fun bindShaderStorageBase(buffer: Int, binding: Int) = Unit
    override fun bufferUpdateBarrier() = Unit
    override fun mapShaderStorageRead(buffer: Int, bytes: Int): java.nio.ByteBuffer {
        val stored = checkNotNull(storageBytes[buffer])
        check(stored.size >= bytes)
        return java.nio.ByteBuffer.wrap(stored, 0, bytes).order(java.nio.ByteOrder.nativeOrder())
    }
    override fun unmapShaderStorage(buffer: Int) = Unit
    var drawCalls = 0
    override fun drawArrays(mode: Int, first: Int, count: Int) {
        drawCalls += 1
    }
    override fun drawElements(mode: Int, count: Int, unsignedInt: Boolean, indexOffset: Int) {
        drawCalls += 1
    }
    override fun drawArraysInstanced(mode: Int, first: Int, count: Int, instances: Int) {
        drawCalls += 1
    }
    override fun drawElementsInstanced(
        mode: Int,
        count: Int,
        unsignedInt: Boolean,
        instances: Int,
        indexOffset: Int,
    ) {
        drawCalls += 1
    }
    override fun arrayBufferData(buffer: Int, data: FloatArray) = Unit
    override fun arrayBufferSubData(buffer: Int, data: FloatArray) = Unit
    override fun unbindVertexArray() = Unit
    override fun elementBufferData(buffer: Int, indices: IntArray, unsignedInt: Boolean) = Unit
    override fun texSubImage2DRgba(
        texture: Int,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        rgba: ByteArray,
    ) = Unit
    override fun texImageCubeFace(texture: Int, face: ru.redbyte.redbytefx.gl.CubeFace, width: Int, height: Int, rgba: ByteArray) = Unit
    override fun textureCubeLinearClamp(texture: Int) = Unit
    override fun texture2DLinearClamp(texture: Int) = Unit
    override fun generateMipmap2D(texture: Int) = Unit
    override fun filterMipmap2D(texture: Int) = Unit
    override fun texImage2DRgbaAlloc(texture: Int, width: Int, height: Int) = Unit
    override fun createFramebuffer(): Int = 1
    override fun deleteFramebuffer(framebuffer: Int) = Unit
    override fun bindFramebuffer(framebuffer: Int) = Unit
    override fun createRenderbuffer(): Int = 1
    override fun deleteRenderbuffer(renderbuffer: Int) = Unit
    override fun framebufferColor(framebuffer: Int, texture: Int) = Unit
    override fun framebufferDepth(framebuffer: Int, renderbuffer: Int, width: Int, height: Int) = Unit
    override fun framebufferComplete(framebuffer: Int): Boolean = true
    override fun vertexAttribDivisor(location: Int, divisor: Int) = Unit
    override fun disableVertexAttribArray(location: Int) = Unit
    override fun vertexAttribFloat(location: Int, size: Int, strideFloats: Int, offsetFloats: Int) = Unit
}
