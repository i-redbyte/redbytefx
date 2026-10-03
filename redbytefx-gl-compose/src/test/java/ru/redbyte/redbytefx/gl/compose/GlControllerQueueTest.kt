package ru.redbyte.redbytefx.gl.compose

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import ru.redbyte.redbytefx.Flt
import ru.redbyte.redbytefx.High
import ru.redbyte.redbytefx.ShaderTarget
import ru.redbyte.redbytefx.Uniform
import ru.redbyte.redbytefx.gl.GlCompileStatus
import ru.redbyte.redbytefx.gl.GlDevice
import ru.redbyte.redbytefx.gl.GlProgramRuntime
import ru.redbyte.redbytefx.gl.GlStage
import ru.redbyte.redbytefx.lit
import ru.redbyte.redbytefx.shader
import ru.redbyte.redbytefx.vec4

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

        controller.runtime = runtime
        controller.glQueue = { block -> block() }
        controller.drainPending()

        assertEquals(afterLink + 1, device.floatCalls)
        assertEquals(0.8f, device.lastFloat)
        assertEquals(true, taskRan)
    }
}

private class FloatDevice : GlDevice() {
    var floatCalls: Int = 0
    var lastFloat: Float = Float.NaN

    override fun createShader(stage: GlStage): Int = 1
    override fun shaderSource(shader: Int, source: String) = Unit
    override fun compileShader(shader: Int): GlCompileStatus = GlCompileStatus(true, "")
    override fun deleteShader(shader: Int) = Unit
    override fun createProgram(): Int = 1
    override fun attachShader(program: Int, shader: Int) = Unit
    override fun linkProgram(program: Int): GlCompileStatus = GlCompileStatus(true, "")
    override fun deleteProgram(program: Int) = Unit
    override fun uniformLocation(program: Int, name: String): Int = 1
    override fun uniform1f(location: Int, value: Float) {
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
    override fun bindTexture2D(texture: Int) = Unit
    override fun bindTextureCube(texture: Int) = Unit
    override fun dispatchCompute(x: Int, y: Int, z: Int) = Unit
    override fun shaderStorageBarrier() = Unit
    override fun createBuffer(): Int = 1
    override fun deleteBuffer(buffer: Int) = Unit
    override fun uniformBufferData(buffer: Int, data: ByteArray) = Unit
    override fun uniformBufferSubData(buffer: Int, data: ByteArray) = Unit
    override fun bindUniformBufferBase(buffer: Int, binding: Int) = Unit
    override fun uniformBlockIndex(program: Int, name: String): Int = 0
    override fun uniformBlockBinding(program: Int, blockIndex: Int, binding: Int) = Unit
    override fun shaderStorageData(buffer: Int, data: ByteArray) = Unit
    override fun shaderStorageSubData(buffer: Int, data: ByteArray) = Unit
    override fun bindShaderStorageBase(buffer: Int, binding: Int) = Unit
}
