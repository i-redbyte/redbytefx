package ru.redbyte.redbytefx.compose

import android.graphics.RenderEffect
import android.graphics.Shader
import org.junit.Assert.assertNotSame
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import ru.redbyte.redbytefx.Flt
import ru.redbyte.redbytefx.High
import ru.redbyte.redbytefx.IntS
import ru.redbyte.redbytefx.Med
import ru.redbyte.redbytefx.Uniform
import ru.redbyte.redbytefx.Vec2
import ru.redbyte.redbytefx.Vec3
import ru.redbyte.redbytefx.Vec4

/**
 * Ensures [FxController.composeRenderEffect] tracks [ShaderControl.renderEffect] identity changes.
 * The controller must not keep a stale Compose wrapper after the platform effect is replaced.
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

    private class SwappingRenderEffectInstance : ShaderControl {

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

        override fun setFloat(uniform: Uniform<Flt<High>>, value: Float): Boolean = false

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
}
