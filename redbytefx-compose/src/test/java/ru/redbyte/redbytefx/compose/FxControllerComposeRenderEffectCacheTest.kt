package ru.redbyte.redbytefx.compose

import android.graphics.RenderEffect
import android.graphics.Shader
import org.junit.Assert.assertNotSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import ru.redbyte.redbytefx.FxInstance
import ru.redbyte.redbytefx.FxParam

/**
 * Ensures [FxController.composeRenderEffect] tracks [FxInstance.renderEffect] identity changes.
 * [FxInstanceImpl] recreates the platform effect after runtime updates; the controller must not
 * keep a stale Compose wrapper.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class FxControllerComposeRenderEffectCacheTest {

    @Test
    fun composeRenderEffectUpdatesWhenPlatformRenderEffectInstanceChanges() {
        val instance = SwappingRenderEffectInstance()
        val controller = FxController(instance)

        val first = controller.composeRenderEffect
        instance.advancePlatformEffect()
        val second = controller.composeRenderEffect

        assertNotSame(first, second)
    }

    private class SwappingRenderEffectInstance : FxInstance {

        private var generation = 0

        private fun effectForGeneration(): RenderEffect =
            RenderEffect.createBlurEffect(
                1f + generation.toFloat(),
                1f + generation.toFloat(),
                Shader.TileMode.CLAMP
            )

        private var current: RenderEffect = effectForGeneration()

        fun advancePlatformEffect() {
            generation += 1
            current = effectForGeneration()
        }

        override fun renderEffect(): RenderEffect = current

        override fun setFloat(param: FxParam.Float, value: Float): Boolean = false

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
}
