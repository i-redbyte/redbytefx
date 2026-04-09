package ru.redbyte.redbytefx.stdlib

import org.junit.Assert.assertTrue
import org.junit.Test
import ru.redbyte.redbytefx.*

class FxStdlibCompilerTest {

    @Test
    fun remapAndPosterizeCompileIntoGeneratedShader() {
        val effect = redbytefx {
            val levels = uniformFloat(5f, "levels")
            val base = let(sample(), "base")
            val remapped = let(remap(base.r, 0f, 1f, 0.2f, 0.9f), "remapped")
            val quantized = let(posterize(remapped, levels), "quantized")

            color(quantized, quantized, quantized, base.a)
        }

        val source = effect.agslSource()

        assertTrue(source.contains("uniform float u_levels;"))
        assertTrue(source.contains("clamp"))
        assertTrue(source.contains("floor"))
        assertTrue(source.contains("mix"))
    }

    @Test
    fun pulseGridAndScanlinesCompileIntoGeneratedShader() {
        val effect = redbytefx {
            val time = uniformTime(name = "time")
            val density = uniformFloat(8f, "density")
            val width = uniformFloat(0.08f, "width")
            val uv = let(fragCoord / resolution, "uv")
            val grid = let(gridMask(uv, density, width), "grid")
            val scan = let(scanlines(fragCoord.y, 14f, 3f), "scan")
            val modulated = let(pulse(time, float(1.5f), uv.y * density), "modulated")

            color(grid, scan, modulated)
        }

        val source = effect.agslSource()

        assertTrue(source.contains("uniform float u_time;"))
        assertTrue(source.contains("uniform float u_density;"))
        assertTrue(source.contains("uniform float u_width;"))
        assertTrue(source.contains("fract"))
        assertTrue(source.contains("mod"))
        assertTrue(source.contains("smoothstep"))
        assertTrue(source.contains("sin"))
    }

    @Test
    fun proceduralNoiseAndVignetteCompileIntoGeneratedShader() {
        val effect = redbytefx {
            val time by autoUniformTime()
            val amount by autoUniformFloat(0.2f)
            val uv = let(fragCoord / resolution, "uv")
            val drift = let(valueNoise(uv * 6f + float2(time * 0.08f, 0f)), "drift")
            val n = let(grain(uv, time, 180f), "grain")
            val mask = let(vignette(uv, 0.35f, 1.05f), "mask")
            val lifted = let(saturate(drift + n * amount), "lifted")

            color(lifted * mask, lifted * mask, lifted * mask)
        }

        val source = effect.agslSource()

        assertTrue(source.contains("uniform float u_time;"))
        assertTrue(source.contains("uniform float u_amount;"))
        assertTrue(source.contains("floor"))
        assertTrue(source.contains("fract"))
        assertTrue(source.contains("length"))
        assertTrue(source.contains("smoothstep"))
        assertTrue(source.contains("sin"))
    }

    @Test
    fun colorBlendHelpersCompileIntoGeneratedShader() {
        val effect = redbytefx {
            val amount by autoUniformFloat(0.75f)
            val warmth by autoUniformFloat(0.6f)
            val base = let(sample(), "base")
            val saturated = let(adjustSaturation(base, 1.35f), "saturated")
            val tint = let(
                color(
                    mix(0.28f, 0.92f, warmth),
                    mix(0.54f, 0.72f, warmth),
                    mix(0.88f, 0.42f, warmth),
                    base.a
                ),
                "tint"
            )
            val multiplied = let(blendMultiply(saturated, tint, 0.25f), "multiplied")
            val screened = let(blendScreen(multiplied, tint, 0.4f), "screened")

            blendOverlay(base, screened, amount)
        }

        val source = effect.agslSource()

        assertTrue(source.contains("uniform float u_amount;"))
        assertTrue(source.contains("uniform float u_warmth;"))
        assertTrue(source.contains("mix"))
        assertTrue(source.contains("?"))
        assertTrue(source.contains("half4"))
    }

    @Test
    fun fbmAndDomainWarpCompileIntoGeneratedShader() {
        val effect = redbytefx {
            val time by autoUniformTime()
            val amount by autoUniformFloat(0.35f)
            val uv = let(fragCoord / resolution, "uv")
            val warped = let(domainWarp(uv * 3f, time, amount), "warped")
            val noise = let(fbm(warped, octaves = 5), "noise")

            color(noise, noise, noise)
        }

        val source = effect.agslSource()

        assertTrue(source.contains("uniform float u_time;"))
        assertTrue(source.contains("uniform float u_amount;"))
        assertTrue(source.contains("fract"))
        assertTrue(source.contains("floor"))
        assertTrue(source.contains("mix"))
    }

    @Test
    fun paletteAndChromaticOffsetCompileIntoGeneratedShader() {
        val effect = redbytefx {
            val amount by autoUniformFloat(0.5f)
            val base = let(sample(), "base")
            val palette = let(cosinePalette(luminance(base)), "palette")
            val tinted = let(color(palette, base.a), "tinted")

            chromaticOffset(
                offset = 8f,
                direction = float2(1f, 0.25f),
                amount = amount
            ) + tinted * 0.1f
        }

        val source = effect.agslSource()

        assertTrue(source.contains("uniform float u_amount;"))
        assertTrue(source.contains("cos"))
        assertTrue(source.contains("rb_sample"))
        assertTrue(source.contains("half4"))
    }
}
