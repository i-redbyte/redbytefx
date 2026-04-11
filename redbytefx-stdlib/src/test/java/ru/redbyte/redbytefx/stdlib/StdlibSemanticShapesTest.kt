package ru.redbyte.redbytefx.stdlib

import org.junit.Assert.assertTrue
import org.junit.Test
import ru.redbyte.redbytefx.*

/**
 * Locks in stable AGSL shapes for canonical stdlib semantics (mapping + masked compositing).
 *
 * Float2 swizzles compile as `(coord).x`. Core `saturate` lowers to `clamp(..., 0, 1)` in AGSL.
 */
class StdlibSemanticShapesTest {

    @Test
    fun inverseLerpEmitsSubtractionAndDivision() {
        val effect = redbytefx {
            val t = let(inverseLerp(float(0f), float(10f), fragCoord.x), "t")
            color(t, t, t)
        }
        val source = effect.agslSource()
        assertTrue(source.contains("l_t ="))
        assertTrue(source.contains("(fragCoord).x"))
        assertTrue(source.contains("10.0"))
        assertTrue(source.contains("/"))
        assertTrue(source.contains("0.0"))
    }

    @Test
    fun remapEmitsMixOfOutputsWithInverseLerpWeight() {
        val effect = redbytefx {
            val m = let(remap(fragCoord.x, float(0f), float(1f), float(0.2f), float(0.8f)), "m")
            color(m, m, m)
        }
        val source = effect.agslSource()
        assertTrue(source.contains("l_m ="))
        assertTrue(source.contains("mix(0.2, 0.8,"))
        assertTrue(source.contains("(fragCoord).x"))
        assertTrue(source.contains("/"))
    }

    @Test
    fun maskedMixScalesMaskByAmountBeforeMix() {
        val effect = redbytefx {
            val base = let(sample(), "base")
            val over = let(color(float(1f), float(0f), float(0f), float(1f)), "over")
            val mask = let(fragCoord.x / resolution.x, "mask")
            maskedMix(base, over, mask, float(0.5f))
        }
        val source = effect.agslSource()
        assertTrue(source.contains("clamp("))
        assertTrue(source.contains("0.0"))
        assertTrue(source.contains("1.0"))
        assertTrue(source.contains("l_mask"))
        assertTrue(source.contains("0.5"))
        assertTrue(source.contains("mix(l_base, l_over,"))
    }

    @Test
    fun maskedScreenUsesClampOnMaskTimesAmountBeforeScreenMix() {
        val effect = redbytefx {
            val base = let(sample(), "base")
            val blend = let(color(float(1f), float(1f), float(0f), float(1f)), "blend")
            val mask = let((fragCoord.x / resolution.x), "mask")
            maskedScreen(base, blend, mask, float(0.4f))
        }
        val source = effect.agslSource()
        assertTrue(source.contains("mix("))
        assertTrue(source.contains("clamp("))
        assertTrue(source.contains("l_mask"))
        assertTrue(source.contains("0.4"))
    }

    @Test
    fun alphaMaskMultipliesAlphaByClampedMaskTimesAmount() {
        val effect = redbytefx {
            val c = let(color(float(1f), float(0f), float(0f), float(1f)), "c")
            val mask = let((fragCoord.y / resolution.y), "mask")
            alphaMask(c, mask, float(0.6f))
        }
        val source = effect.agslSource()
        assertTrue(source.contains("l_c"))
        assertTrue(source.contains("l_mask"))
        assertTrue(source.contains("clamp("))
        assertTrue(source.contains("0.6"))
        assertTrue(source.contains(".a"))
    }
}
