package ru.redbyte.redbytefx.compose

import android.graphics.RenderEffect
import org.junit.Assert.assertEquals
import org.junit.Test
import ru.redbyte.redbytefx.FxInstance
import ru.redbyte.redbytefx.FxParam
import ru.redbyte.redbytefx.redbytefx

/**
 * [FxController.runBatch] should coalesce host invalidation when multiple imperative setters run
 * in one block (see [FxController.maybeInvalidateAfterUniformChange]).
 */
class FxControllerRunBatchTest {

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

    private class TrackingFxInstance : FxInstance {
        var floatCalls: Int = 0

        override fun renderEffect(): RenderEffect =
            error("Not needed for this test")

        override fun setFloat(param: FxParam.Float, value: Float): Boolean {
            floatCalls += 1
            return true
        }

        override fun setFloat2(param: FxParam.Float2, x: Float, y: Float): Boolean = false

        override fun setFloat3(param: FxParam.Float3, x: Float, y: Float, z: Float): Boolean = false

        override fun setFloat4(
            param: FxParam.Float4,
            x: Float,
            y: Float,
            z: Float,
            w: Float
        ): Boolean = false

        override fun setResolution(widthPx: Float, heightPx: Float): Boolean = false
    }

    private fun testFloatParam(): FxParam.Float {
        var param: FxParam.Float? = null
        redbytefx {
            val amount = uniformFloat(0f)
            param = amount
            sample()
        }
        return checkNotNull(param)
    }
}
