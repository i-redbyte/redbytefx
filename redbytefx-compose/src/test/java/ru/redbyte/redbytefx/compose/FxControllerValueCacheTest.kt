package ru.redbyte.redbytefx.compose

import org.junit.Assert.assertFalse
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.redbyte.redbytefx.FxInstance
import ru.redbyte.redbytefx.FxParam
import ru.redbyte.redbytefx.redbytefx
import android.graphics.RenderEffect

class FxControllerValueCacheTest {

    @Test
    fun sameFloatTreatsNaNAsStable() {
        assertTrue(sameFloat(Float.NaN, Float.NaN))
    }

    @Test
    fun sameFloatPreservesSignedZeroDifference() {
        assertFalse(sameFloat(0f, -0f))
    }

    @Test
    fun sameFloat4UsesBitwiseComparisonForAllComponents() {
        assertTrue(
            sameFloat4(
                value = Float4Value(1f, Float.NaN, -0f, 4f),
                x = 1f,
                y = Float.NaN,
                z = -0f,
                w = 4f
            )
        )
        assertFalse(
            sameFloat4(
                value = Float4Value(1f, Float.NaN, -0f, 4f),
                x = 1f,
                y = Float.NaN,
                z = 0f,
                w = 4f
            )
        )
    }

    @Test
    fun setFloatInvalidatesRuntimeWhenValueChanges() {
        val controller = FxController(FakeFxInstance())
        val param = testFloatParam()

        controller.setFloat(param, 0.5f)

        assertEquals(1, controller.runtimeInvalidationTick)
    }

    @Test
    fun setFloatSkipsRuntimeInvalidationWhenValueIsStable() {
        val controller = FxController(FakeFxInstance())
        val param = testFloatParam()

        controller.setFloat(param, 0.5f)
        controller.setFloat(param, 0.5f)

        assertEquals(1, controller.runtimeInvalidationTick)
    }

    @Test
    fun syncResolutionDoesNotInvalidateRuntime() {
        val controller = FxController(FakeFxInstance())

        controller.syncResolution(320f, 180f)

        assertEquals(0, controller.runtimeInvalidationTick)
    }
}

private class FakeFxInstance : FxInstance {
    override fun renderEffect(): RenderEffect = error("Not needed for this test")
    override fun setFloat(param: FxParam.Float, value: Float) = Unit
    override fun setFloat2(param: FxParam.Float2, x: Float, y: Float) = Unit
    override fun setFloat3(param: FxParam.Float3, x: Float, y: Float, z: Float) = Unit
    override fun setFloat4(param: FxParam.Float4, x: Float, y: Float, z: Float, w: Float) = Unit
    override fun setResolution(widthPx: Float, heightPx: Float) = Unit
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
