package ru.redbyte.redbytefx.stdlib

import org.junit.Assert.assertTrue
import org.junit.Test
import ru.redbyte.redbytefx.*

class RepresentativeDemoSmokeTest {

    @Test
    fun waveRuntimeBaselineKeepsRawCoordinateShape() {
        val source = waveRuntimeBaselineEffect().agslSource()

        assertTrue(source.contains("half4 main"))
        assertTrue(source.contains("uniform float u_wave_amplitude;"))
        assertTrue(source.contains("uniform float u_wave_frequency;"))
        assertTrue(source.contains("wave_offset"))
        assertTrue(source.contains("sin"))
        assertTrue(source.contains("rb_sample"))
    }

    @Test
    fun compositeRuntimeBaselineKeepsMaskAndCompositingStack() {
        val source = compositeRuntimeBaselineEffect().agslSource()

        assertTrue(source.contains("uniform float u_radius;"))
        assertTrue(source.contains("uniform float u_panel_width;"))
        assertTrue(source.contains("uniform float u_amount;"))
        assertTrue(source.contains("glow_layer"))
        assertTrue(source.contains("focus"))
        assertTrue(source.contains("halo"))
        assertTrue(source.contains("smoothstep"))
        assertTrue(source.contains("mix"))
    }

    @Test
    fun radarRuntimeBaselineKeepsPolarTimeDrivenShape() {
        val source = radarRuntimeBaselineEffect().agslSource()

        assertTrue(source.contains("half4 main"))
        assertTrue(source.contains("uniform float u_time;"))
        assertTrue(source.contains("uniform float u_speed;"))
        assertTrue(source.contains("uniform float u_radius;"))
        assertTrue(source.contains("polar"))
        assertTrue(source.contains("sweep"))
        assertTrue(source.contains("atan"))
        assertTrue(source.contains("fract"))
        assertTrue(source.contains("smoothstep"))
    }

    @Test
    fun circuitRuntimeBaselineKeepsRoutingSelectionShape() {
        val source = circuitRuntimeBaselineEffect().agslSource()

        assertTrue(source.contains("half4 main"))
        assertTrue(source.contains("uniform float u_time;"))
        assertTrue(source.contains("uniform float u_route;"))
        assertTrue(source.contains("uniform float u_amount;"))
        assertTrue(source.contains("board_mask"))
        assertTrue(source.contains("source_trace"))
        assertTrue(source.contains("output_pulse"))
        assertTrue(source.contains("route_mask"))
        assertTrue(source.contains("?"))
        assertTrue(source.contains("smoothstep"))
    }

    private fun waveRuntimeBaselineEffect() = redbytefx {
        val amplitude = uniformFloat(0f, "wave_amplitude")
        val frequency = uniformFloat(0.08f, "wave_frequency")
        val x = let(fragCoord.x, "x")
        val waveOffset = let(
            float2(0f, sin(x * frequency) * amplitude),
            "wave_offset"
        )

        sample(fragCoord + waveOffset)
    }

    private fun compositeRuntimeBaselineEffect() = redbytefx {
        val radius = uniformFloat(0.2f, "radius")
        val panelWidth = uniformFloat(0.34f, "panel_width")
        val amount = uniformFloat(0.82f, "amount")
        val base = let(sample(), "base")
        val uv = let(normalizedUv(), "uv")
        val focus = let(
            circleMask(
                uv = uv,
                center = float2(0.34f, 0.5f),
                radius = radius,
                feather = 0.16f
            ),
            "focus"
        )
        val halo = let(
            ringMask(
                uv = uv,
                center = float2(0.34f, 0.5f),
                radius = radius + 0.05f,
                width = 0.1f,
                feather = 0.05f
            ),
            "halo"
        )
        val panel = let(
            rectMask(
                uv = uv,
                center = float2(0.77f, 0.5f),
                size = float2(panelWidth, 0.62f),
                feather = 0.04f
            ),
            "panel"
        )
        val glowLayer = let(
            alphaMask(color(float3(0.12f, 0.95f, 1f), 1f), halo, amount),
            "glow_layer"
        )
        val panelTint = let(
            alphaMask(color(float3(1f, 0.79f, 0.3f), 1f), panel, amount * 0.6f),
            "panel_tint"
        )
        val screened = let(maskedScreen(base, glowLayer, halo, amount), "screened")
        val overlaid = let(maskedOverlay(screened, panelTint, panel, amount), "overlaid")

        maskedMix(base, overlaid, focus + halo * 0.2f, amount)
    }

    private fun radarRuntimeBaselineEffect() = redbytefx {
        val time = uniformTime(name = "time")
        val speed = uniformFloat(0.72f, "speed")
        val radius = uniformFloat(0.34f, "radius")
        val amount = uniformFloat(0.86f, "amount")
        val base = let(sample(), "base")
        val uv = let(normalizedUv(), "uv")
        val polar = let(polarCoordinates(uv), "polar")
        val sweepAngle = let(fract(time * speed * 0.08f), "sweep_angle")
        val sweep = let(
            angularSweep(
                uv = uv,
                angle = sweepAngle,
                width = 0.12f,
                feather = 0.03f
            ),
            "sweep"
        )
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
        val beam = let(
            radialRamp(
                uv = uv,
                innerRadius = float(0.06f),
                outerRadius = radius + 0.18f
            ),
            "beam"
        )
        val tint = let(
            color(
                mix(0.05f, 0.18f, polar.x * 1.4f),
                mix(0.24f, 1f, sweep + arc * 0.55f),
                mix(0.10f, 0.62f, polar.y * 0.45f),
                base.a
            ),
            "tint"
        )

        maskedScreen(base, tint, sweep * beam + arc, amount)
    }

    private fun circuitRuntimeBaselineEffect() = redbytefx {
        val time = uniformTime(name = "time")
        val route = uniformFloat(0f, "route")
        val amount = uniformFloat(0.9f, "amount")
        val base = let(sample(), "base")
        val uv = let(normalizedUv(), "uv")
        val board = let(aspectCenteredUv(uv, resolution), "board")
        val boardMask = let(
            softFill(
                distance = sdRoundedBox(
                    point = board,
                    halfSize = float2(0.82f, 0.56f),
                    radius = 0.06f
                ),
                feather = 0.03f
            ),
            "board_mask"
        )
        val sourceTrace = let(
            segmentMask(
                point = board,
                start = float2(-0.62f, -0.12f),
                end = float2(-0.08f, 0f),
                thickness = 0.05f,
                feather = 0.018f
            ),
            "source_trace"
        )
        val outputTrace = let(
            segmentMask(
                point = board,
                start = float2(-0.08f, 0f),
                end = float2(0.58f, 0.22f),
                thickness = 0.05f,
                feather = 0.018f
            ),
            "output_trace"
        )
        val phase = let(fract(time * 0.42f), "phase")
        val sourcePulse = let(
            segmentPulse(
                point = board,
                start = float2(-0.62f, -0.12f),
                end = float2(-0.08f, 0f),
                phase = phase,
                bandWidth = 0.22f,
                thickness = 0.05f,
                bandFeather = 0.08f,
                feather = 0.018f
            ),
            "source_pulse"
        )
        val outputPulse = let(
            segmentPulse(
                point = board,
                start = float2(-0.08f, 0f),
                end = float2(0.58f, 0.22f),
                phase = phase,
                bandWidth = 0.22f,
                thickness = 0.05f,
                bandFeather = 0.08f,
                feather = 0.018f
            ),
            "output_pulse"
        )
        val routeMask = let(
            ifElse(
                route lt 0.5f,
                sourceTrace + sourcePulse,
                outputTrace + outputPulse
            ),
            "route_mask"
        )
        val boardTint = let(color(float3(0.02f, 0.12f, 0.08f), 1f), "board_tint")
        val signalTint = let(color(float3(0.34f, 0.86f, 1f), 1f), "signal_tint")
        val substrate = let(maskedMix(base, boardTint, boardMask, 1f), "substrate")

        maskedMix(substrate, signalTint, routeMask, amount)
    }
}
