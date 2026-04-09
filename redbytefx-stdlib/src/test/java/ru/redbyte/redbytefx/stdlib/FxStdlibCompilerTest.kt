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

    @Test
    fun shapeMasksCompileIntoGeneratedShader() {
        val effect = redbytefx {
            val center = uniformFloat2(0.36f, 0.52f, "center")
            val radius = uniformFloat(0.18f, "radius")
            val amount = uniformFloat(0.8f, "amount")
            val uv = let(fragCoord / resolution, "uv")
            val focus = let(circleMask(uv, center = center, radius = radius, feather = 0.14f), "focus")
            val halo = let(
                ringMask(
                    uv,
                    center = center,
                    radius = radius + 0.07f,
                    width = 0.1f,
                    feather = 0.05f
                ),
                "halo"
            )
            val panel = let(
                rectMask(
                    uv,
                    center = float2(0.78f, 0.5f),
                    size = float2(0.26f, 0.58f),
                    feather = 0.04f
                ),
                "panel"
            )

            color(focus, halo, panel, amount)
        }

        val source = effect.agslSource()

        assertTrue(source.contains("uniform float2 u_center;"))
        assertTrue(source.contains("uniform float u_radius;"))
        assertTrue(source.contains("uniform float u_amount;"))
        assertTrue(source.contains("length"))
        assertTrue(source.contains("abs"))
        assertTrue(source.contains("smoothstep"))
    }

    @Test
    fun timingHelpersCompileIntoGeneratedShader() {
        val effect = redbytefx {
            val time = uniformTime(name = "time")
            val speed = uniformFloat(0.65f, "speed")
            val phase = let(pingPong(time * speed, 1f), "phase")
            val eased = let(easeInOutSine(phase), "eased")
            val glow = let(easeInOutCubic(phase), "glow")

            color(eased, glow, phase)
        }

        val source = effect.agslSource()

        assertTrue(source.contains("uniform float u_time;"))
        assertTrue(source.contains("uniform float u_speed;"))
        assertTrue(source.contains("fract"))
        assertTrue(source.contains("abs"))
        assertTrue(source.contains("cos"))
        assertTrue(source.contains("pow"))
        assertTrue(source.contains("?"))
    }

    @Test
    fun compositingHelpersCompileIntoGeneratedShader() {
        val effect = redbytefx {
            val amount = uniformFloat(0.82f, "amount")
            val radius = uniformFloat(0.18f, "radius")
            val uv = let(fragCoord / resolution, "uv")
            val base = let(sample(), "base")
            val mask = let(circleMask(uv, radius = radius, feather = 0.12f), "mask")
            val glow = let(alphaMask(color(float3(0.12f, 0.95f, 1f), 1f), mask, amount), "glow")
            val screened = let(maskedScreen(base, glow, mask, amount), "screened")
            val overlaid = let(
                maskedOverlay(
                    screened,
                    color(float3(1f, 0.82f, 0.32f), base.a),
                    mask,
                    amount * 0.5f
                ),
                "overlaid"
            )

            maskedMix(base, overlaid, mask, amount)
        }

        val source = effect.agslSource()

        assertTrue(source.contains("uniform float u_amount;"))
        assertTrue(source.contains("uniform float u_radius;"))
        assertTrue(source.contains("mix"))
        assertTrue(source.contains("smoothstep"))
        assertTrue(source.contains("half4"))
    }

    @Test
    fun transitionHelpersCompileIntoGeneratedShader() {
        val effect = redbytefx {
            val time = uniformTime(name = "time")
            val speed = uniformFloat(0.75f, "speed")
            val mode = uniformFloat(0f, "mode")
            val amount = uniformFloat(0.88f, "amount")
            val base = let(sample(), "base")
            val uv = let(fragCoord / resolution, "uv")
            val progress = let(easeInOutSine(pingPong(time * speed, 1f)), "progress")
            val horizontal = let(horizontalReveal(uv, progress, feather = 0.07f), "horizontal")
            val vertical = let(verticalReveal(uv, progress, feather = 0.07f, fromTop = false), "vertical")
            val radial = let(radialReveal(uv, progress, feather = 0.08f, maxRadius = 0.9f), "radial")
            val reveal = let(
                ifElse(mode lt 0.5f, horizontal, ifElse(mode lt 1.5f, vertical, radial)),
                "reveal"
            )
            val stylized = let(
                blendScreen(
                    posterize(base, 5f),
                    color(float3(0.16f, 0.94f, 1f), base.a),
                    0.55f
                ),
                "stylized"
            )

            maskedMix(base, stylized, reveal, amount)
        }

        val source = effect.agslSource()

        assertTrue(source.contains("uniform float u_time;"))
        assertTrue(source.contains("uniform float u_speed;"))
        assertTrue(source.contains("uniform float u_mode;"))
        assertTrue(source.contains("uniform float u_amount;"))
        assertTrue(source.contains("fract"))
        assertTrue(source.contains("smoothstep"))
        assertTrue(source.contains("cos"))
        assertTrue(source.contains("?"))
    }

    @Test
    fun signalHelpersCompileIntoGeneratedShader() {
        val effect = redbytefx {
            val time by autoUniformTime()
            val density by autoUniformFloat(7.5f)
            val warp by autoUniformFloat(0.028f)
            val amount by autoUniformFloat(0.82f)
            val base = let(sample(), "base")
            val uv = let(fragCoord / resolution, "uv")
            val driftUv = let(
                scanWarp(
                    uv = uv,
                    time = time,
                    amplitude = warp,
                    density = density,
                    speed = 2.2f,
                    noiseAmount = 0.55f
                ),
                "drift_uv"
            )
            val drifted = let(sample(driftUv * resolution), "drifted")
            val bars = let(
                signalBars(
                    position = uv.y,
                    density = density,
                    width = 0.28f,
                    phase = time * 0.65f,
                    feather = 0.1f
                ),
                "bars"
            )
            val lock = let(
                bandMask(
                    position = uv.y,
                    center = 0.36f,
                    width = 0.14f,
                    feather = 0.08f
                ),
                "lock"
            )

            maskedMix(base, drifted, max(bars, lock), amount)
        }

        val source = effect.agslSource()

        assertTrue(source.contains("uniform float u_time;"))
        assertTrue(source.contains("uniform float u_density;"))
        assertTrue(source.contains("uniform float u_warp;"))
        assertTrue(source.contains("uniform float u_amount;"))
        assertTrue(source.contains("abs"))
        assertTrue(source.contains("fract"))
        assertTrue(source.contains("sin"))
        assertTrue(source.contains("smoothstep"))
        assertTrue(source.contains("rb_sample"))
    }

    @Test
    fun gradientHelpersCompileIntoGeneratedShader() {
        val effect = redbytefx {
            val time by autoUniformTime()
            val amount by autoUniformFloat(0.78f)
            val base = let(sample(), "base")
            val uv = let(fragCoord / resolution, "uv")
            val sweepCenter = let(0.16f + pingPong(time * 0.18f, 1f) * 0.68f, "sweep_center")
            val ramp = let(linearRamp(uv, direction = float2(1f, -0.35f), start = 0.08f, end = 0.92f), "ramp")
            val sweep = let(
                directionalSweep(
                    uv = uv,
                    direction = float2(1f, -0.35f),
                    center = sweepCenter,
                    width = 0.22f,
                    feather = 0.08f
                ),
                "sweep"
            )
            val vignette = let(radialRamp(uv, innerRadius = 0.12f, outerRadius = 0.68f), "vignette")

            maskedScreen(base, color(float3(ramp, sweep, vignette), base.a), sweep * vignette, amount)
        }

        val source = effect.agslSource()

        assertTrue(source.contains("uniform float u_time;"))
        assertTrue(source.contains("uniform float u_amount;"))
        assertTrue(source.contains("length"))
        assertTrue(source.contains("smoothstep"))
        assertTrue(source.contains("clamp"))
        assertTrue(source.contains("mix"))
    }
}
