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
        assertTrue(source.contains("clamp"))
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
        assertTrue(source.contains("clamp"))
        assertTrue(source.contains("length"))
        assertTrue(source.contains("rb_sample"))
        assertTrue(source.contains("half4"))
    }

    @Test
    fun coordinateConvenienceHelpersCompileIntoGeneratedShader() {
        val effect = redbytefx {
            val amount by autoUniformFloat(0.42f)
            val center = uniformFloat2(0.38f, 0.58f, "center")
            val uv = let(normalizedUv(), "uv")
            val local = let(centeredUv(uv, center), "local")
            val projected = let(inverseLerp(-0.25f, 0.25f, local.x), "projected")
            val radius = let(radialDistance(uv, center), "radius")
            val edge = let(edgeDistance(uv), "edge")
            val fade = let(edgeFade(uv, 0.12f), "fade")
            val reread = let(sampleUv(uv), "reread")
            val ramp = let(
                linearRamp(
                    uv = uv,
                    direction = float2(1f, -0.2f),
                    start = 0.15f,
                    end = 0.85f
                ),
                "ramp"
            )

            color(projected, radius, fade * amount + edge * 0.2f + ramp * 0.2f + reread.b * 0.1f, amount)
        }

        val source = effect.agslSource()

        assertTrue(source.contains("uniform float u_amount;"))
        assertTrue(source.contains("uniform float2 u_center;"))
        assertTrue(source.contains("uResolution"))
        assertTrue(source.contains("length"))
        assertTrue(source.contains("min"))
        assertTrue(source.contains("smoothstep"))
        assertTrue(source.contains("mix"))
        assertTrue(source.contains("rb_sample"))
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

    @Test
    fun frameHelpersCompileIntoGeneratedShader() {
        val effect = redbytefx {
            val time by autoUniformTime()
            val thickness by autoUniformFloat(0.1f)
            val amount by autoUniformFloat(0.82f)
            val base = let(sample(), "base")
            val uv = let(fragCoord / resolution, "uv")
            val frame = let(frameMask(uv, thickness, 0.03f), "frame")
            val interior = let(edgeFade(uv, thickness + 0.08f), "interior")
            val sweepCenter = let(0.12f + pingPong(time * 0.2f, 1f) * 0.76f, "sweep_center")
            val sweep = let(
                directionalSweep(
                    uv = uv,
                    direction = float2(1f, -0.24f),
                    center = sweepCenter,
                    width = 0.2f,
                    feather = 0.08f
                ),
                "sweep"
            )

            maskedOverlay(
                maskedScreen(base, color(float3(0.12f, 0.96f, 0.72f), base.a), frame * sweep, amount),
                color(float3(0.08f, 0.24f, 0.16f), base.a),
                frame + (1f - interior) * 0.28f,
                amount * 0.45f
            )
        }

        val source = effect.agslSource()

        assertTrue(source.contains("uniform float u_time;"))
        assertTrue(source.contains("uniform float u_thickness;"))
        assertTrue(source.contains("uniform float u_amount;"))
        assertTrue(source.contains("min"))
        assertTrue(source.contains("smoothstep"))
        assertTrue(source.contains("clamp"))
        assertTrue(source.contains("mix"))
    }

    @Test
    fun cornerHelpersCompileIntoGeneratedShader() {
        val effect = redbytefx {
            val time by autoUniformTime()
            val size by autoUniformFloat(0.22f)
            val thickness by autoUniformFloat(0.08f)
            val amount by autoUniformFloat(0.84f)
            val base = let(sample(), "base")
            val uv = let(fragCoord / resolution, "uv")
            val corners = let(cornerMask(uv, size = size, thickness = thickness, feather = 0.03f), "corners")
            val sweepCenter = let(0.18f + pingPong(time * 0.18f, 1f) * 0.64f, "sweep_center")
            val sweep = let(
                directionalSweep(
                    uv = uv,
                    direction = float2(1f, -0.2f),
                    center = sweepCenter,
                    width = 0.16f,
                    feather = 0.08f
                ),
                "sweep"
            )

            maskedScreen(base, color(float3(0.1f, 0.98f, 0.68f), base.a), corners * sweep, amount)
        }

        val source = effect.agslSource()

        assertTrue(source.contains("uniform float u_time;"))
        assertTrue(source.contains("uniform float u_size;"))
        assertTrue(source.contains("uniform float u_thickness;"))
        assertTrue(source.contains("uniform float u_amount;"))
        assertTrue(source.contains("min"))
        assertTrue(source.contains("max"))
        assertTrue(source.contains("smoothstep"))
        assertTrue(source.contains("mix"))
    }

    @Test
    fun polarHelpersCompileIntoGeneratedShader() {
        val effect = redbytefx {
            val time by autoUniformTime()
            val speed by autoUniformFloat(0.72f)
            val radius by autoUniformFloat(0.34f)
            val amount by autoUniformFloat(0.86f)
            val base = let(sample(), "base")
            val uv = let(fragCoord / resolution, "uv")
            val polar = let(polarCoordinates(uv), "polar")
            val sweepAngle = let(fract(time * speed * 0.08f), "sweep_angle")
            val sweep = let(angularSweep(uv, angle = sweepAngle, width = 0.12f, feather = 0.03f), "sweep")
            val arc = let(
                arcMask(
                    uv = uv,
                    radius = radius,
                    ringWidth = 0.09f,
                    angle = sweepAngle,
                    arcWidth = 0.18f,
                    feather = 0.03f
                ),
                "arc"
            )

            maskedScreen(
                base = base,
                blend = color(float3(polar.x, polar.y, sweep), base.a),
                mask = sweep + arc,
                amount = amount
            )
        }

        val source = effect.agslSource()

        assertTrue(source.contains("uniform float u_time;"))
        assertTrue(source.contains("uniform float u_speed;"))
        assertTrue(source.contains("uniform float u_radius;"))
        assertTrue(source.contains("uniform float u_amount;"))
        assertTrue(source.contains("atan"))
        assertTrue(source.contains("fract"))
        assertTrue(source.contains("length"))
        assertTrue(source.contains("smoothstep"))
        assertTrue(source.contains("mix"))
    }

    @Test
    fun lightingHelpersCompileIntoGeneratedShader() {
        val effect = redbytefx {
            val time by autoUniformTime()
            val radius by autoUniformFloat(0.24f)
            val amount by autoUniformFloat(0.82f)
            val base = let(sample(), "base")
            val uv = let(fragCoord / resolution, "uv")
            val local = let(centeredUv(uv), "local")
            val dir = let(radialDirection(uv, resolution), "dir")
            val glow = let(centerGlow(uv, resolution, radius = radius, feather = 0.18f), "glow")
            val rim = let(
                rimLight(
                    uv = uv,
                    resolution = resolution,
                    radius = radius + 0.08f,
                    width = 0.075f,
                    feather = 0.024f
                ),
                "rim"
            )

            maskedScreen(
                base = base,
                blend = color(local.x + 0.5f, dir.y * 0.5f + 0.5f, glow + rim * 0.25f, base.a),
                mask = max(glow, rim),
                amount = amount
            )
        }

        val source = effect.agslSource()

        assertTrue(source.contains("uniform float u_time;"))
        assertTrue(source.contains("uniform float u_radius;"))
        assertTrue(source.contains("uniform float u_amount;"))
        assertTrue(source.contains("uResolution"))
        assertTrue(source.contains("length"))
        assertTrue(source.contains("smoothstep"))
        assertTrue(source.contains("max"))
        assertTrue(source.contains("mix"))
    }

    @Test
    fun sigilSdfHelpersCompileIntoGeneratedShader() {
        val effect = redbytefx {
            val time by autoUniformTime()
            val amount by autoUniformFloat(0.84f)
            val uv = let(fragCoord / resolution, "uv")
            val sigil = let(aspectCenteredUv(uv, resolution), "sigil")
            val pulse = let(easeInOutSine(pingPong(time * 0.18f, 1f)), "pulse")
            val frame = let(
                softStroke(
                    distance = sdRoundedBox(sigil, halfSize = float2(0.35f, 0.35f), radius = 0.16f),
                    width = 0.028f,
                    feather = 0.012f
                ),
                "frame"
            )
            val ring = let(
                softStroke(
                    distance = sdCircle(sigil, radius = 0.26f + pulse * 0.03f),
                    width = 0.032f,
                    feather = 0.014f
                ),
                "ring"
            )
            val spine = let(
                softFill(
                    distance = sdBox(sigil, halfSize = float2(0.05f, 0.22f + pulse * 0.05f)),
                    feather = 0.012f
                ),
                "spine"
            )

            maskedScreen(
                base = sample(),
                blend = color(float3(frame, ring, spine), 1f),
                mask = max(frame, max(ring, spine)),
                amount = amount
            )
        }

        val source = effect.agslSource()

        assertTrue(source.contains("uniform float u_time;"))
        assertTrue(source.contains("uniform float u_amount;"))
        assertTrue(source.contains("abs"))
        assertTrue(source.contains("length"))
        assertTrue(source.contains("smoothstep"))
        assertTrue(source.contains("step"))
        assertTrue(source.contains("max"))
        assertTrue(source.contains("mix"))
    }

    @Test
    fun traceSdfHelpersCompileIntoGeneratedShader() {
        val effect = redbytefx {
            val amount by autoUniformFloat(0.88f)
            val uv = let(fragCoord / resolution, "uv")
            val board = let(aspectCenteredUv(uv, resolution), "board")
            val circle = let(fill(sdCircle(board - float2(-0.22f, -0.08f), 0.05f)), "circle")
            val chip = let(
                softFill(
                    sdRoundedBox(board - float2(0.12f, 0.02f), halfSize = float2(0.16f, 0.1f), radius = 0.035f),
                    0.02f
                ),
                "chip"
            )
            val trace = let(
                segmentMask(
                    point = board,
                    start = float2(-0.22f, -0.08f),
                    end = float2(0.12f, 0.02f),
                    thickness = 0.034f,
                    feather = 0.016f
                ),
                "trace"
            )

            color(circle, chip, trace, amount)
        }

        val source = effect.agslSource()

        assertTrue(source.contains("uniform float u_amount;"))
        assertTrue(source.contains("length"))
        assertTrue(source.contains("clamp"))
        assertTrue(source.contains("abs"))
        assertTrue(source.contains("smoothstep"))
        assertTrue(source.contains("step"))
    }

    @Test
    fun routingHelpersCompileIntoGeneratedShader() {
        val effect = redbytefx {
            val time by autoUniformTime()
            val amount by autoUniformFloat(0.9f)
            val uv = let(fragCoord / resolution, "uv")
            val board = let(aspectCenteredUv(uv, resolution), "board")
            val start = let(float2(-0.46f, -0.10f), "start")
            val end = let(float2(0.32f, 0.12f), "end")
            val trace = let(
                segmentMask(
                    point = board,
                    start = start,
                    end = end,
                    thickness = 0.042f,
                    feather = 0.016f
                ),
                "trace"
            )
            val progress = let(segmentProgress(board, start, end), "progress")
            val pulse = let(
                segmentPulse(
                    point = board,
                    start = start,
                    end = end,
                    phase = fract(time * 0.42f),
                    bandWidth = 0.22f,
                    thickness = 0.05f,
                    bandFeather = 0.08f,
                    feather = 0.018f
                ),
                "pulse"
            )

            color(trace, progress, pulse, amount)
        }

        val source = effect.agslSource()

        assertTrue(source.contains("uniform float u_time;"))
        assertTrue(source.contains("uniform float u_amount;"))
        assertTrue(source.contains("dot"))
        assertTrue(source.contains("clamp"))
        assertTrue(source.contains("fract"))
        assertTrue(source.contains("smoothstep"))
        assertTrue(source.contains("length"))
    }

    @Test
    fun canonicalStarterPipelineCompilesIntoGeneratedShader() {
        val effect = redbytefx {
            val time by autoUniformTime()
            val amount by autoUniformFloat(0.86f)
            val base = let(sample(), "base")
            val uv = let(normalizedUv(), "uv")
            val board = let(aspectCenteredUv(uv, resolution), "board")
            val panelMask = let(
                rectMask(
                    uv = uv,
                    center = float2(0.5f, 0.5f),
                    size = float2(0.82f, 0.58f),
                    feather = 0.04f
                ),
                "panel_mask"
            )
            val chipMask = let(
                softFill(
                    distance = sdRoundedBox(
                        point = board - float2(0.08f, -0.02f),
                        halfSize = float2(0.18f, 0.1f),
                        radius = 0.04f
                    ),
                    feather = 0.016f
                ),
                "chip_mask"
            )
            val traceMask = let(
                segmentMask(
                    point = board,
                    start = float2(-0.34f, 0.02f),
                    end = float2(0.08f, -0.02f),
                    thickness = 0.042f,
                    feather = 0.016f
                ),
                "trace_mask"
            )
            val signal = let(
                segmentPulse(
                    point = board,
                    start = float2(-0.34f, 0.02f),
                    end = float2(0.08f, -0.02f),
                    phase = fract(time * 0.42f),
                    bandWidth = 0.22f,
                    thickness = 0.042f,
                    bandFeather = 0.08f,
                    feather = 0.016f
                ),
                "signal"
            )
            val scaffold = let(
                maskedMix(
                    base = base,
                    revealed = color(float3(0.02f, 0.12f, 0.08f), base.a),
                    mask = panelMask,
                    amount = amount
                ),
                "scaffold"
            )
            val energized = let(
                maskedScreen(
                    base = scaffold,
                    blend = alphaMask(
                        color(float3(0.24f, 0.94f, 1f), 1f),
                        max(chipMask, signal),
                        amount
                    ),
                    mask = max(traceMask, chipMask),
                    amount = amount
                ),
                "energized"
            )

            maskedMix(
                base = energized,
                revealed = color(float3(1f, 0.92f, 0.62f), base.a),
                mask = signal,
                amount = amount * 0.45f
            )
        }

        val source = effect.agslSource()

        assertTrue(source.contains("uniform float u_time;"))
        assertTrue(source.contains("uniform float u_amount;"))
        assertTrue(source.contains("uResolution"))
        assertTrue(source.contains("rb_sample"))
        assertTrue(source.contains("dot"))
        assertTrue(source.contains("length"))
        assertTrue(source.contains("smoothstep"))
        assertTrue(source.contains("mix"))
    }

    @Test
    fun exploratorySurfaceHelpersCompileIntoGeneratedShader() {
        val effect = redbytefx {
            val time by autoUniformTime()
            val amount by autoUniformFloat(0.72f)
            val thickness by autoUniformFloat(0.09f)
            val uv = let(normalizedUv(), "uv")
            val warpedUv = let(domainWarp(uv * 3.2f, time * 0.25f, 0.18f), "warped_uv")
            val refracted = let(
                chromaticOffset(
                    offset = 7f,
                    direction = float2(1f, -0.24f),
                    amount = amount,
                    coord = warpedUv * resolution
                ),
                "refracted"
            )
            val frame = let(frameMask(uv, thickness = thickness, feather = 0.03f), "frame")
            val corners = let(
                cornerMask(
                    uv = uv,
                    size = float(0.18f),
                    thickness = thickness,
                    feather = 0.03f
                ),
                "corners"
            )
            val tint = let(
                color(cosinePalette(uv.x + uv.y * 0.35f + time * 0.04f), refracted.a),
                "tint"
            )

            maskedScreen(
                base = refracted,
                blend = tint,
                mask = max(frame, corners),
                amount = amount
            )
        }

        val source = effect.agslSource()

        assertTrue(source.contains("uniform float u_time;"))
        assertTrue(source.contains("uniform float u_amount;"))
        assertTrue(source.contains("uniform float u_thickness;"))
        assertTrue(source.contains("cos"))
        assertTrue(source.contains("fract"))
        assertTrue(source.contains("rb_sample"))
        assertTrue(source.contains("min"))
        assertTrue(source.contains("smoothstep"))
        assertTrue(source.contains("mix"))
    }
}
