package ru.redbyte.redbytefx.compose

import android.graphics.RenderEffect
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.atomic.AtomicReference
import ru.redbyte.redbytefx.Flt
import ru.redbyte.redbytefx.High
import ru.redbyte.redbytefx.IntS
import ru.redbyte.redbytefx.Med
import ru.redbyte.redbytefx.ShaderTarget
import ru.redbyte.redbytefx.Uniform
import ru.redbyte.redbytefx.Vec2
import ru.redbyte.redbytefx.Vec3
import ru.redbyte.redbytefx.Vec4
import ru.redbyte.redbytefx.shader

/**
 * [FxController.runBatch] should coalesce host invalidation when multiple imperative setters run
 * in one block (see [FxController.maybeInvalidateAfterUniformChange]).
 */
class FxControllerRunBatchTest {

    @Test
    fun backgroundWriteCannotChangeControllerState() {
        val instance = TrackingFxInstance()
        val controller = FxController(instance)
        val param = testFloatParam()
        val failure = AtomicReference<Throwable?>()
        val worker = Thread {
            try {
                controller.setFloat(param, 1f)
            } catch (error: Throwable) {
                failure.set(error)
            }
        }
        worker.start()
        worker.join(5_000)
        assertFalse(worker.isAlive)
        assertTrue(failure.get() is IllegalStateException)
        assertEquals(0, instance.floatCalls)
        assertEquals(0, controller.runtimeInvalidationTick)
    }

    @Test
    fun listenerCanUnsubscribeDuringInvalidation() {
        val controller = FxController(TrackingFxInstance())
        val param = testFloatParam()
        var calls = 0
        lateinit var listener: () -> Unit
        listener = {
            calls++
            controller.removeRuntimeInvalidationListener(listener)
        }
        controller.addRuntimeInvalidationListener(listener)
        controller.setFloat(param, 1f)
        controller.setFloat(param, 2f)
        assertEquals(1, calls)
    }

    @Test
    fun runBatchCoalescesRuntimeInvalidationTick() {
        val instance = TrackingFxInstance()
        val controller = FxController(instance)
        val param = testFloatParam()

        controller.runBatch {
            controller.setFloat(param, 0.25f)
            controller.setFloat(param, 0.5f)
            controller.setFloat(param, 0.75f)
        }

        assertEquals(3, instance.floatCalls)
        assertEquals(1, controller.runtimeInvalidationTick)
    }

    @Test
    fun withoutRunBatchEachChangeStillInvalidates() {
        val instance = TrackingFxInstance()
        val controller = FxController(instance)
        val param = testFloatParam()

        controller.setFloat(param, 0.25f)
        controller.setFloat(param, 0.75f)

        assertEquals(2, instance.floatCalls)
        assertEquals(2, controller.runtimeInvalidationTick)
    }

    @Test
    fun removingOneHostLeavesTheOtherSubscribed() {
        val controller = FxController(TrackingFxInstance())
        val param = testFloatParam()
        var firstInvalidations = 0
        var secondInvalidations = 0
        val first: () -> Unit = { firstInvalidations++ }
        val second: () -> Unit = { secondInvalidations++ }
        controller.addRuntimeInvalidationListener(first)
        controller.addRuntimeInvalidationListener(second)
        controller.setFloat(param, 1f)
        controller.removeRuntimeInvalidationListener(first)
        controller.setFloat(param, 2f)
        assertEquals(1, firstInvalidations)
        assertEquals(2, secondInvalidations)
    }

    private class TrackingFxInstance : ShaderControl {
        var floatCalls: Int = 0

        override fun renderEffect(): RenderEffect =
            error("Not needed for this test")

        override fun setFloat(uniform: Uniform<Flt<High>>, value: Float): Boolean {
            floatCalls += 1
            return true
        }

        override fun setFloat2(uniform: Uniform<Vec2<Flt<High>>>, x: Float, y: Float): Boolean = false

        override fun setFloat3(uniform: Uniform<Vec3<Flt<High>>>, x: Float, y: Float, z: Float): Boolean = false

        override fun setFloat4(
            uniform: Uniform<Vec4<Flt<High>>>,
            x: Float,
            y: Float,
            z: Float,
            w: Float,
        ): Boolean = false

        override fun setMedFloat(uniform: Uniform<Flt<Med>>, value: Float): Boolean = false

        override fun setMedFloat2(uniform: Uniform<Vec2<Flt<Med>>>, x: Float, y: Float): Boolean = false

        override fun setMedFloat3(uniform: Uniform<Vec3<Flt<Med>>>, x: Float, y: Float, z: Float): Boolean = false

        override fun setMedFloat4(
            uniform: Uniform<Vec4<Flt<Med>>>,
            x: Float,
            y: Float,
            z: Float,
            w: Float,
        ): Boolean = false

        override fun setInt(uniform: Uniform<IntS>, value: Int): Boolean = false

        override fun setResolution(widthPx: Float, heightPx: Float): Boolean = false

        override fun runBatch(block: () -> Unit) {
            block()
        }
    }

    private fun testFloatParam(): Uniform<Flt<High>> {
        val program = shader(ShaderTarget.Agsl) {
            uniform("amount", 0f)
            fragment { sample() }
        }
        return program.floatUniform("u_amount")
    }
}
