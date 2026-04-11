package ru.redbyte.redbytefx.compose

import org.junit.Assert.assertEquals
import org.junit.Test
import ru.redbyte.redbytefx.FxInstance
import ru.redbyte.redbytefx.FxParam
import ru.redbyte.redbytefx.sameFloatUniformValue
import ru.redbyte.redbytefx.redbytefx
import android.graphics.RenderEffect

class FxControllerValueCacheTest {

    @Test
    fun setFloatInvalidatesRuntimeWhenValueChanges() {
        val instance = TrackingFxInstance()
        val controller = FxController(instance)
        val param = testFloatParam()

        controller.setFloat(param, 0.5f)

        assertEquals(1, controller.runtimeInvalidationTick)
        assertEquals(1, instance.floatCalls)
    }

    @Test
    fun setFloatSkipsRuntimeInvalidationWhenValueIsStable() {
        val instance = TrackingFxInstance()
        val controller = FxController(instance)
        val param = testFloatParam()

        controller.setFloat(param, 0.5f)
        controller.setFloat(param, 0.5f)

        assertEquals(1, controller.runtimeInvalidationTick)
        assertEquals(1, instance.floatCalls)
    }

    @Test
    fun setFloat2SkipsRuntimeInvalidationWhenValueIsStable() {
        val instance = TrackingFxInstance()
        val controller = FxController(instance)
        val param = testFloat2Param()

        controller.setFloat2(param, 0.25f, 0.75f)
        controller.setFloat2(param, 0.25f, 0.75f)

        assertEquals(1, controller.runtimeInvalidationTick)
        assertEquals(1, instance.float2Calls)
    }

    @Test
    fun setFloat3SkipsRuntimeInvalidationWhenValueIsStable() {
        val instance = TrackingFxInstance()
        val controller = FxController(instance)
        val param = testFloat3Param()

        controller.setFloat3(param, 0.25f, 0.5f, 0.75f)
        controller.setFloat3(param, 0.25f, 0.5f, 0.75f)

        assertEquals(1, controller.runtimeInvalidationTick)
        assertEquals(1, instance.float3Calls)
    }

    @Test
    fun setFloat4SkipsRuntimeInvalidationWhenValueIsStable() {
        val instance = TrackingFxInstance()
        val controller = FxController(instance)
        val param = testFloat4Param()

        controller.setFloat4(param, 0.1f, 0.2f, 0.3f, 0.4f)
        controller.setFloat4(param, 0.1f, 0.2f, 0.3f, 0.4f)

        assertEquals(1, controller.runtimeInvalidationTick)
        assertEquals(1, instance.float4Calls)
    }

    @Test
    fun setResolutionInvalidatesRuntimeWhenSizeChanges() {
        val instance = TrackingFxInstance()
        val controller = FxController(instance)

        controller.setResolution(320f, 180f)

        assertEquals(1, controller.runtimeInvalidationTick)
        assertEquals(1, instance.resolutionCalls)
        assertEquals(320f, instance.lastResolutionWidth)
        assertEquals(180f, instance.lastResolutionHeight)
    }

    @Test
    fun setResolutionSkipsDuplicateSizeUpdates() {
        val instance = TrackingFxInstance()
        val controller = FxController(instance)

        controller.setResolution(320f, 180f)
        controller.setResolution(320f, 180f)

        assertEquals(1, controller.runtimeInvalidationTick)
        assertEquals(1, instance.resolutionCalls)
    }

    @Test
    fun setResolutionClampsNonPositiveValuesBeforeForwarding() {
        val instance = TrackingFxInstance()
        val controller = FxController(instance)

        controller.setResolution(0f, -5f)

        assertEquals(1, controller.runtimeInvalidationTick)
        assertEquals(1, instance.resolutionCalls)
        assertEquals(1f, instance.lastResolutionWidth)
        assertEquals(1f, instance.lastResolutionHeight)
    }

    @Test
    fun setResolutionSkipsDifferentNonPositiveInputsThatClampSame() {
        val instance = TrackingFxInstance()
        val controller = FxController(instance)

        controller.setResolution(0f, -5f)
        controller.setResolution(-10f, 0f)

        assertEquals(1, controller.runtimeInvalidationTick)
        assertEquals(1, instance.resolutionCalls)
        assertEquals(1f, instance.lastResolutionWidth)
        assertEquals(1f, instance.lastResolutionHeight)
    }

    @Test
    fun syncResolutionDoesNotInvalidateRuntime() {
        val instance = TrackingFxInstance()
        val controller = FxController(instance)

        controller.syncResolution(320f, 180f)

        assertEquals(0, controller.runtimeInvalidationTick)
        assertEquals(1, instance.resolutionCalls)
        assertEquals(320f, instance.lastResolutionWidth)
        assertEquals(180f, instance.lastResolutionHeight)
    }

    @Test
    fun syncResolutionSkipsDuplicateSizeUpdates() {
        val instance = TrackingFxInstance()
        val controller = FxController(instance)

        controller.syncResolution(320f, 180f)
        controller.syncResolution(320f, 180f)

        assertEquals(0, controller.runtimeInvalidationTick)
        assertEquals(1, instance.resolutionCalls)
    }

    @Test
    fun syncResolutionSkipsDifferentNonPositiveInputsThatClampSame() {
        val instance = TrackingFxInstance()
        val controller = FxController(instance)

        controller.syncResolution(0f, -5f)
        controller.syncResolution(-10f, 0f)

        assertEquals(0, controller.runtimeInvalidationTick)
        assertEquals(1, instance.resolutionCalls)
        assertEquals(1f, instance.lastResolutionWidth)
        assertEquals(1f, instance.lastResolutionHeight)
    }
}

private class TrackingFxInstance : FxInstance {
    var floatCalls: Int = 0
    var float2Calls: Int = 0
    var float3Calls: Int = 0
    var float4Calls: Int = 0
    var resolutionCalls: Int = 0
    var lastResolutionWidth: Float? = null
    var lastResolutionHeight: Float? = null

    private var lastFloat: Float? = null
    private var lastFloat2: Pair<Float, Float>? = null
    private var lastFloat3: Triple<Float, Float, Float>? = null
    private var lastFloat4: FloatArray? = null

    override fun renderEffect(): RenderEffect = error("Not needed for this test")

    override fun setFloat(param: FxParam.Float, value: Float): Boolean {
        val previous = lastFloat
        if (previous != null && sameFloatUniformValue(previous, value)) return false
        lastFloat = value
        floatCalls += 1
        return true
    }

    override fun setFloat2(param: FxParam.Float2, x: Float, y: Float): Boolean {
        val previous = lastFloat2
        if (previous != null &&
            sameFloatUniformValue(previous.first, x) &&
            sameFloatUniformValue(previous.second, y)
        ) {
            return false
        }
        lastFloat2 = x to y
        float2Calls += 1
        return true
    }

    override fun setFloat3(param: FxParam.Float3, x: Float, y: Float, z: Float): Boolean {
        val previous = lastFloat3
        if (previous != null &&
            sameFloatUniformValue(previous.first, x) &&
            sameFloatUniformValue(previous.second, y) &&
            sameFloatUniformValue(previous.third, z)
        ) {
            return false
        }
        lastFloat3 = Triple(x, y, z)
        float3Calls += 1
        return true
    }

    override fun setFloat4(
        param: FxParam.Float4,
        x: Float,
        y: Float,
        z: Float,
        w: Float
    ): Boolean {
        val previous = lastFloat4
        if (previous != null &&
            sameFloatUniformValue(previous[0], x) &&
            sameFloatUniformValue(previous[1], y) &&
            sameFloatUniformValue(previous[2], z) &&
            sameFloatUniformValue(previous[3], w)
        ) {
            return false
        }
        lastFloat4 = floatArrayOf(x, y, z, w)
        float4Calls += 1
        return true
    }

    override fun setResolution(widthPx: Float, heightPx: Float): Boolean {
        val w = if (widthPx > 0f) widthPx else 1f
        val h = if (heightPx > 0f) heightPx else 1f
        val lw = lastResolutionWidth
        val lh = lastResolutionHeight
        if (lw != null && lh != null &&
            sameFloatUniformValue(lw, w) &&
            sameFloatUniformValue(lh, h)
        ) {
            return false
        }
        lastResolutionWidth = w
        lastResolutionHeight = h
        resolutionCalls += 1
        return true
    }
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

private fun testFloat2Param(): FxParam.Float2 {
    var param: FxParam.Float2? = null
    redbytefx {
        val offset = uniformFloat2(0f, 0f)
        param = offset
        sample()
    }
    return checkNotNull(param)
}

private fun testFloat3Param(): FxParam.Float3 {
    var param: FxParam.Float3? = null
    redbytefx {
        val tint = uniformFloat3(0f, 0f, 0f)
        param = tint
        sample()
    }
    return checkNotNull(param)
}

private fun testFloat4Param(): FxParam.Float4 {
    var param: FxParam.Float4? = null
    redbytefx {
        val rgba = uniformFloat4(0f, 0f, 0f, 0f)
        param = rgba
        sample()
    }
    return checkNotNull(param)
}