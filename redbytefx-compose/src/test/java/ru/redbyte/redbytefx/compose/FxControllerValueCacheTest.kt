package ru.redbyte.redbytefx.compose

import org.junit.Assert.assertEquals
import org.junit.Test
import ru.redbyte.redbytefx.Flt
import ru.redbyte.redbytefx.High
import ru.redbyte.redbytefx.IntS
import ru.redbyte.redbytefx.Med
import ru.redbyte.redbytefx.ShaderTarget
import ru.redbyte.redbytefx.Uniform
import ru.redbyte.redbytefx.Vec2
import ru.redbyte.redbytefx.Vec3
import ru.redbyte.redbytefx.Vec4
import ru.redbyte.redbytefx.sameFloatUniformValue
import ru.redbyte.redbytefx.shader
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
    fun setIntSkipsRuntimeInvalidationWhenValueIsStable() {
        val instance = TrackingFxInstance()
        val controller = FxController(instance)
        val param = testIntParam()

        controller.setInt(param, 2)
        controller.setInt(param, 2)
        controller.setInt(param, 4)

        assertEquals(2, controller.runtimeInvalidationTick)
        assertEquals(2, instance.intCalls)
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

private class TrackingFxInstance : ShaderControl {
    var floatCalls: Int = 0
    var intCalls: Int = 0
    var float2Calls: Int = 0
    var float3Calls: Int = 0
    var float4Calls: Int = 0
    var resolutionCalls: Int = 0
    var lastResolutionWidth: Float? = null
    var lastResolutionHeight: Float? = null

    private var lastFloat: Float? = null
    private var lastInt: Int? = null
    private var lastFloat2: Pair<Float, Float>? = null
    private var lastFloat3: Triple<Float, Float, Float>? = null
    private var lastFloat4: FloatArray? = null

    override fun renderEffect(): RenderEffect = error("Not needed for this test")

    override fun setFloat(uniform: Uniform<Flt<High>>, value: Float): Boolean {
        val previous = lastFloat
        if (previous != null && sameFloatUniformValue(previous, value)) return false
        lastFloat = value
        floatCalls += 1
        return true
    }

    override fun setInt(uniform: Uniform<IntS>, value: Int): Boolean {
        if (lastInt == value) return false
        lastInt = value
        intCalls += 1
        return true
    }

    override fun setFloat2(uniform: Uniform<Vec2<Flt<High>>>, x: Float, y: Float): Boolean {
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

    override fun setFloat3(uniform: Uniform<Vec3<Flt<High>>>, x: Float, y: Float, z: Float): Boolean {
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
        uniform: Uniform<Vec4<Flt<High>>>,
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

    override fun runBatch(block: () -> Unit) {
        block()
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

private fun testIntParam(): Uniform<IntS> {
    var param: Uniform<IntS>? = null
    shader(ShaderTarget.Agsl) {
        param = uniformInt("count", 0)
        fragment { sample() }
    }
    return checkNotNull(param)
}

private fun testFloatParam(): Uniform<Flt<High>> = shader(ShaderTarget.Agsl) {
    val amount = uniform("amount", 0f)
    fragment { sample() }
    amount
}.let { program ->
    checkNotNull(program.floatUniform("u_amount"))
}

private fun testFloat2Param(): Uniform<Vec2<Flt<High>>> {
    var param: Uniform<Vec2<Flt<High>>>? = null
    shader(ShaderTarget.Agsl) {
        param = uniformVec2("offset", 0f, 0f)
        fragment { sample() }
    }
    return checkNotNull(param)
}

private fun testFloat3Param(): Uniform<Vec3<Flt<High>>> {
    var param: Uniform<Vec3<Flt<High>>>? = null
    shader(ShaderTarget.Agsl) {
        param = uniformVec3("tint", 0f, 0f, 0f)
        fragment { sample() }
    }
    return checkNotNull(param)
}

private fun testFloat4Param(): Uniform<Vec4<Flt<High>>> {
    var param: Uniform<Vec4<Flt<High>>>? = null
    shader(ShaderTarget.Agsl) {
        param = uniformVec4("rgba", 0f, 0f, 0f, 0f)
        fragment { sample() }
    }
    return checkNotNull(param)
}