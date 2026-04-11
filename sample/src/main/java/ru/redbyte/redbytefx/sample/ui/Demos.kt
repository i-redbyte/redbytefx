package ru.redbyte.redbytefx.sample.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import ru.redbyte.redbytefx.*
import ru.redbyte.redbytefx.compose.bindFloat
import ru.redbyte.redbytefx.compose.bindFloat2
import ru.redbyte.redbytefx.compose.bindTime
import ru.redbyte.redbytefx.compose.redbyteFx
import ru.redbyte.redbytefx.compose.rememberFxController
import ru.redbyte.redbytefx.stdlib.adjustSaturation
import ru.redbyte.redbytefx.stdlib.angularSweep
import ru.redbyte.redbytefx.stdlib.arcMask
import ru.redbyte.redbytefx.stdlib.aspectCenteredUv
import ru.redbyte.redbytefx.stdlib.bandMask
import ru.redbyte.redbytefx.stdlib.blendMultiply
import ru.redbyte.redbytefx.stdlib.blendOverlay
import ru.redbyte.redbytefx.stdlib.blendScreen
import ru.redbyte.redbytefx.stdlib.centerGlow
import ru.redbyte.redbytefx.stdlib.centeredUv
import ru.redbyte.redbytefx.stdlib.chromaticOffset
import ru.redbyte.redbytefx.stdlib.cosinePalette
import ru.redbyte.redbytefx.stdlib.circleMask
import ru.redbyte.redbytefx.stdlib.cornerMask
import ru.redbyte.redbytefx.stdlib.domainWarp
import ru.redbyte.redbytefx.stdlib.easeInOutCubic
import ru.redbyte.redbytefx.stdlib.easeInOutSine
import ru.redbyte.redbytefx.stdlib.fbm
import ru.redbyte.redbytefx.stdlib.gridMask
import ru.redbyte.redbytefx.stdlib.grain
import ru.redbyte.redbytefx.stdlib.horizontalReveal
import ru.redbyte.redbytefx.stdlib.linearRamp
import ru.redbyte.redbytefx.stdlib.maskedMix
import ru.redbyte.redbytefx.stdlib.maskedOverlay
import ru.redbyte.redbytefx.stdlib.maskedScreen
import ru.redbyte.redbytefx.stdlib.normalizedUv
import ru.redbyte.redbytefx.stdlib.pingPong
import ru.redbyte.redbytefx.stdlib.polarCoordinates
import ru.redbyte.redbytefx.stdlib.posterize
import ru.redbyte.redbytefx.stdlib.pulse
import ru.redbyte.redbytefx.stdlib.rectMask
import ru.redbyte.redbytefx.stdlib.remap
import ru.redbyte.redbytefx.stdlib.ringMask
import ru.redbyte.redbytefx.stdlib.alphaMask
import ru.redbyte.redbytefx.stdlib.directionalSweep
import ru.redbyte.redbytefx.stdlib.edgeFade
import ru.redbyte.redbytefx.stdlib.radialReveal
import ru.redbyte.redbytefx.stdlib.radialRamp
import ru.redbyte.redbytefx.stdlib.radialDirection
import ru.redbyte.redbytefx.stdlib.rimLight
import ru.redbyte.redbytefx.stdlib.scanWarp
import ru.redbyte.redbytefx.stdlib.scanlines
import ru.redbyte.redbytefx.stdlib.signalBars
import ru.redbyte.redbytefx.stdlib.sdCircle
import ru.redbyte.redbytefx.stdlib.sdBox
import ru.redbyte.redbytefx.stdlib.sdRoundedBox
import ru.redbyte.redbytefx.stdlib.sdSegment
import ru.redbyte.redbytefx.stdlib.segmentMask
import ru.redbyte.redbytefx.stdlib.segmentPulse
import ru.redbyte.redbytefx.stdlib.sampleUv
import ru.redbyte.redbytefx.stdlib.softFill
import ru.redbyte.redbytefx.stdlib.softStroke
import ru.redbyte.redbytefx.stdlib.stroke
import ru.redbyte.redbytefx.stdlib.valueNoise
import ru.redbyte.redbytefx.stdlib.verticalReveal
import ru.redbyte.redbytefx.stdlib.vignette
import ru.redbyte.redbytefx.stdlib.frameMask

private enum class Axis { X, Y }

private data class MirrorSetup(
    val effect: ru.redbyte.redbytefx.FxEffect,
    val xEnabled: FxParam.Float,
    val xFrom: FxParam.Float,
    val yEnabled: FxParam.Float,
    val yFrom: FxParam.Float
)

private data class WaveSetup(
    val effect: ru.redbyte.redbytefx.FxEffect,
    val amplitude: FxParam.Float,
    val frequency: FxParam.Float
)

private data class PulseSetup(
    val effect: ru.redbyte.redbytefx.FxEffect,
    val time: FxParam.Float,
    val speed: FxParam.Float,
    val grid: FxParam.Float,
    val amount: FxParam.Float
)

private data class SignalSetup(
    val effect: ru.redbyte.redbytefx.FxEffect,
    val density: FxParam.Float,
    val lineWidth: FxParam.Float,
    val amount: FxParam.Float
)

private data class RadarSetup(
    val effect: ru.redbyte.redbytefx.FxEffect,
    val time: FxParam.Float,
    val speed: FxParam.Float,
    val radius: FxParam.Float,
    val amount: FxParam.Float
)

private data class HaloSetup(
    val effect: ru.redbyte.redbytefx.FxEffect,
    val time: FxParam.Float,
    val speed: FxParam.Float,
    val radius: FxParam.Float,
    val amount: FxParam.Float
)

private data class AuroraSetup(
    val effect: ru.redbyte.redbytefx.FxEffect,
    val time: FxParam.Float,
    val amount: FxParam.Float,
    val chromaPx: FxParam.Float,
    val spectral: FxParam.Float,
    val speed: FxParam.Float
)

private data class LiquidGlassSetup(
    val effect: ru.redbyte.redbytefx.FxEffect,
    val time: FxParam.Float,
    val refraction: FxParam.Float,
    val speed: FxParam.Float,
    val chromaPx: FxParam.Float,
    val chromaMix: FxParam.Float,
    val edgeMix: FxParam.Float
)

private data class SigilSetup(
    val effect: ru.redbyte.redbytefx.FxEffect,
    val time: FxParam.Float,
    val speed: FxParam.Float,
    val amount: FxParam.Float
)

private data class DuotoneSetup(
    val effect: ru.redbyte.redbytefx.FxEffect,
    val amount: FxParam.Float,
    val warmth: FxParam.Float
)

private data class PosterizeSetup(
    val effect: ru.redbyte.redbytefx.FxEffect,
    val levels: FxParam.Float,
    val amount: FxParam.Float
)

private data class FilmSetup(
    val effect: ru.redbyte.redbytefx.FxEffect,
    val time: FxParam.Float,
    val grainAmount: FxParam.Float,
    val grainScale: FxParam.Float,
    val vignetteAmount: FxParam.Float
)

private data class GradeSetup(
    val effect: ru.redbyte.redbytefx.FxEffect,
    val amount: FxParam.Float,
    val warmth: FxParam.Float,
    val glow: FxParam.Float
)

private data class WarpSetup(
    val effect: ru.redbyte.redbytefx.FxEffect,
    val time: FxParam.Float,
    val warpAmount: FxParam.Float,
    val scale: FxParam.Float,
    val driftAmount: FxParam.Float
)

private data class PrismSetup(
    val effect: ru.redbyte.redbytefx.FxEffect,
    val amount: FxParam.Float,
    val spread: FxParam.Float,
    val shift: FxParam.Float
)

private data class SpotlightSetup(
    val effect: ru.redbyte.redbytefx.FxEffect,
    val center: FxParam.Float2,
    val radius: FxParam.Float,
    val amount: FxParam.Float
)

private data class BeaconSetup(
    val effect: ru.redbyte.redbytefx.FxEffect,
    val time: FxParam.Float,
    val speed: FxParam.Float,
    val radius: FxParam.Float,
    val amount: FxParam.Float
)

private data class CompositeSetup(
    val effect: ru.redbyte.redbytefx.FxEffect,
    val radius: FxParam.Float,
    val panelWidth: FxParam.Float,
    val amount: FxParam.Float
)

private data class FrameSetup(
    val effect: ru.redbyte.redbytefx.FxEffect,
    val time: FxParam.Float,
    val speed: FxParam.Float,
    val thickness: FxParam.Float,
    val amount: FxParam.Float
)

private data class CornerSetup(
    val effect: ru.redbyte.redbytefx.FxEffect,
    val time: FxParam.Float,
    val size: FxParam.Float,
    val thickness: FxParam.Float,
    val amount: FxParam.Float
)

private data class RevealSetup(
    val effect: ru.redbyte.redbytefx.FxEffect,
    val time: FxParam.Float,
    val speed: FxParam.Float,
    val mode: FxParam.Float,
    val amount: FxParam.Float
)

private data class SweepSetup(
    val effect: ru.redbyte.redbytefx.FxEffect,
    val time: FxParam.Float,
    val speed: FxParam.Float,
    val width: FxParam.Float,
    val amount: FxParam.Float
)

private data class GlitchSetup(
    val effect: ru.redbyte.redbytefx.FxEffect,
    val time: FxParam.Float,
    val density: FxParam.Float,
    val warp: FxParam.Float,
    val amount: FxParam.Float
)

private enum class RevealMode { Horizontal, Vertical, Radial }

@Composable
private fun rememberGeneratedAgsl(effect: FxEffect): String = remember(effect) { effect.agslSource() }

@Composable
fun DemoFlip() {
    var flipX by rememberSaveable { mutableStateOf(false) }
    var flipY by rememberSaveable { mutableStateOf(false) }

    val setup = remember {
        var px: FxParam.Float? = null
        var py: FxParam.Float? = null
        val effect = redbytefx {
            val flipXAmount = uniformFloat(0f, "flip_x")
            val flipYAmount = uniformFloat(0f, "flip_y")
            px = flipXAmount
            py = flipYAmount
            sample(
                flipY(
                    coord = flipX(amount = flipXAmount),
                    amount = flipYAmount
                )
            )
        }
        Triple(effect, px!!, py!!)
    }

    val fx = rememberFxController(setup.first)
    fx.bindFloat(setup.second, if (flipX) 1f else 0f)
    fx.bindFloat(setup.third, if (flipY) 1f else 0f)

    DemoLayout(
        generatedAgsl = rememberGeneratedAgsl(setup.first),
        preview = {
            DemoPreviewStage(modifier = Modifier.redbyteFx(fx))
        },
        controls = {
            SwitchRow("Flip X", flipX) {
                flipX = it
            }
            SwitchRow("Flip Y", flipY) {
                flipY = it
            }
        }
    )
}

@Composable
fun DemoMirror() {
    var enabled by rememberSaveable { mutableStateOf(false) }
    var axis by rememberSaveable { mutableStateOf(Axis.X) }

    var fromX by rememberSaveable { mutableStateOf(MirrorXFrom.Right) }
    var fromY by rememberSaveable { mutableStateOf(MirrorYFrom.Bottom) }

    val setup = remember {
        var xEnabled: FxParam.Float? = null
        var xFrom: FxParam.Float? = null
        var yEnabled: FxParam.Float? = null
        var yFrom: FxParam.Float? = null
        val effect = redbytefx {
            val mirrorXEnabled = uniformFloat(0f, "mirror_x_enabled")
            val mirrorXFrom = uniformFloat(MirrorXFrom.Right.shaderValue, "mirror_x_from")
            val mirrorYEnabled = uniformFloat(0f, "mirror_y_enabled")
            val mirrorYFrom = uniformFloat(MirrorYFrom.Bottom.shaderValue, "mirror_y_from")
            xEnabled = mirrorXEnabled
            xFrom = mirrorXFrom
            yEnabled = mirrorYEnabled
            yFrom = mirrorYFrom
            sample(
                mirrorY(
                    coord = mirrorX(
                        amount = mirrorXEnabled,
                        from = mirrorXFrom
                    ),
                    amount = mirrorYEnabled,
                    from = mirrorYFrom
                )
            )
        }
        MirrorSetup(effect, xEnabled!!, xFrom!!, yEnabled!!, yFrom!!)
    }

    val fx = rememberFxController(setup.effect)
    val useX = axis == Axis.X
    fx.bindFloat(setup.xEnabled, if (enabled && useX) 1f else 0f)
    fx.bindFloat(setup.xFrom, fromX.shaderValue)
    fx.bindFloat(setup.yEnabled, if (enabled && !useX) 1f else 0f)
    fx.bindFloat(setup.yFrom, fromY.shaderValue)

    DemoLayout(
        generatedAgsl = rememberGeneratedAgsl(setup.effect),
        preview = {
            DemoPreviewStage(modifier = Modifier.redbyteFx(fx))
        },
        controls = {
            SwitchRow("Enabled", enabled) {
                enabled = it
            }

            Text(text = "Axis", style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                RadioRow("X", selected = axis == Axis.X) {
                    axis = Axis.X
                }
                RadioRow("Y", selected = axis == Axis.Y) {
                    axis = Axis.Y
                }
            }

            if (axis == Axis.X) {
                Text(text = "From", style = MaterialTheme.typography.titleMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    RadioRow("Right", selected = fromX == MirrorXFrom.Right) {
                        fromX = MirrorXFrom.Right
                    }
                    RadioRow("Left", selected = fromX == MirrorXFrom.Left) {
                        fromX = MirrorXFrom.Left
                    }
                }
            } else {
                Text(text = "From", style = MaterialTheme.typography.titleMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    RadioRow("Bottom", selected = fromY == MirrorYFrom.Bottom) {
                        fromY = MirrorYFrom.Bottom
                    }
                    RadioRow("Top", selected = fromY == MirrorYFrom.Top) {
                        fromY = MirrorYFrom.Top
                    }
                }
            }
        }
    )
}

@Composable
fun DemoRotate() {
    var angle by rememberSaveable { mutableFloatStateOf(0f) }

    val setup = remember {
        var p: FxParam.Float? = null
        val effect = redbytefx {
            val angle = uniformFloat(0f, "angle_deg")
            p = angle
            val pivot = center()
            val delta = fragCoord - pivot
            val theta = radians(angle)
            val s = sin(theta)
            val c = cos(theta)
            val rotated = pivot + float2(
                c * delta.x - s * delta.y,
                s * delta.x + c * delta.y
            )
            sample(rotated)
        }
        Pair(effect, p!!)
    }

    val fx = rememberFxController(setup.first)
    fx.bindFloat(setup.second, angle)

    DemoLayout(
        generatedAgsl = rememberGeneratedAgsl(setup.first),
        preview = {
            DemoPreviewStage(modifier = Modifier.redbyteFx(fx))
        },
        controls = {
            SliderRow("Angle", angle, 0f..360f) {
                angle = it
            }
        }
    )
}

@Composable
fun DemoScale() {
    var sx by rememberSaveable { mutableFloatStateOf(1f) }
    var sy by rememberSaveable { mutableFloatStateOf(1f) }

    val setup = remember {
        var p: FxParam.Float2? = null
        val effect = redbytefx {
            val scaleAmount = uniformFloat2(1f, 1f, "scale")
            p = scaleAmount
            sample(scale(scale = scaleAmount))
        }
        Pair(effect, p!!)
    }

    val fx = rememberFxController(setup.first)
    fx.bindFloat2(setup.second, sx, sy)

    DemoLayout(
        generatedAgsl = rememberGeneratedAgsl(setup.first),
        preview = {
            DemoPreviewStage(modifier = Modifier.redbyteFx(fx))
        },
        controls = {
            SliderRow("Scale X", sx * 100f, 25f..300f) {
                sx = it / 100f
            }
            SliderRow("Scale Y", sy * 100f, 25f..300f) {
                sy = it / 100f
            }
        }
    )
}

@Composable
fun DemoOffset() {
    var dx by rememberSaveable { mutableFloatStateOf(0f) }
    var dy by rememberSaveable { mutableFloatStateOf(0f) }

    val setup = remember {
        var p: FxParam.Float2? = null
        val effect = redbytefx {
            val delta = uniformFloat2(0f, 0f, "offset")
            p = delta
            sample(offset(delta = delta))
        }
        Pair(effect, p!!)
    }

    val fx = rememberFxController(setup.first)
    fx.bindFloat2(setup.second, dx, dy)

    DemoLayout(
        generatedAgsl = rememberGeneratedAgsl(setup.first),
        preview = {
            DemoPreviewStage(modifier = Modifier.redbyteFx(fx))
        },
        controls = {
            SliderRow("Offset X", dx, -200f..200f) {
                dx = it
            }
            SliderRow("Offset Y", dy, -200f..200f) {
                dy = it
            }
        }
    )
}

@Composable
fun DemoWave() {
    var amplitude by rememberSaveable { mutableFloatStateOf(0f) }
    var frequencyUi by rememberSaveable { mutableFloatStateOf(8f) }

    val setup = remember {
        var amplitudeParam: FxParam.Float? = null
        var frequencyParam: FxParam.Float? = null
        val effect = redbytefx {
            val amplitudeUniform = uniformFloat(0f, "wave_amplitude")
            val frequencyUniform = uniformFloat(0.08f, "wave_frequency")
            amplitudeParam = amplitudeUniform
            frequencyParam = frequencyUniform

            val x = let(fragCoord.x, "x")
            val waveOffset = let(float2(
                0f,
                sin(x * frequencyUniform) * amplitudeUniform
            ), "wave_offset")
            sample(fragCoord + waveOffset)
        }
        WaveSetup(effect, amplitudeParam!!, frequencyParam!!)
    }

    val fx = rememberFxController(setup.effect)
    fx.bindFloat(setup.amplitude, amplitude)
    fx.bindFloat(setup.frequency, frequencyUi / 100f)

    DemoLayout(
        generatedAgsl = rememberGeneratedAgsl(setup.effect),
        preview = {
            DemoPreviewStage(modifier = Modifier.redbyteFx(fx))
        },
        controls = {
            SliderRow("Amplitude", amplitude, 0f..120f) {
                amplitude = it
            }
            SliderRow(
                title = "Frequency",
                value = frequencyUi,
                range = 2f..40f,
                formatValue = { "${it / 100f}" }
            ) {
                frequencyUi = it
            }
        }
    )
}

@Composable
fun DemoPulse() {
    var playing by rememberSaveable { mutableStateOf(true) }
    var speedUi by rememberSaveable { mutableFloatStateOf(100f) }
    var gridUi by rememberSaveable { mutableFloatStateOf(10f) }
    var amountUi by rememberSaveable { mutableFloatStateOf(85f) }

    val setup = remember {
        var timeParam: FxParam.Float? = null
        var speedParam: FxParam.Float? = null
        var gridParam: FxParam.Float? = null
        var amountParam: FxParam.Float? = null
        val effect = redbytefx {
            val timeUniform = uniformTime(name = "pulse_time")
            val speedUniform = uniformFloat(1f, "pulse_speed")
            val gridUniform = uniformFloat(10f, "pulse_grid")
            val amountUniform = uniformFloat(0.85f, "pulse_amount")
            timeParam = timeUniform
            speedParam = speedUniform
            gridParam = gridUniform
            amountParam = amountUniform

            val base = let(sample(), "base")
            val uv = let(normalizedUv(), "uv")
            val safeGrid = let(max(gridUniform, 1f), "safe_grid")
            val pixelUv = let(floor(uv * safeGrid) / safeGrid, "pixel_uv")
            val pixelBase = let(sampleUv(pixelUv), "pixel_base")
            val row = let(floor(uv.y * safeGrid), "row")
            val wave = let(
                pulse(timeUniform, speedUniform, row * 0.7f),
                "wave"
            )
            val glow = let(pow(wave, 3f), "glow")
            val column = let(fract(uv.x * safeGrid + timeUniform * 0.25f), "column")
            val active = let(ceil(smoothstep(0.82f, 0.98f, column)), "active")
            val accent = let(
                color(
                    float3(
                        mix(0.08f, 0.25f, glow),
                        mix(0.22f, 0.95f, glow),
                        mix(0.45f, 1f, glow)
                    ),
                    base.a
                ),
                "accent"
            )

            mix(base, mix(pixelBase, accent, active * glow), amountUniform)
        }
        PulseSetup(effect, timeParam!!, speedParam!!, gridParam!!, amountParam!!)
    }

    val fx = rememberFxController(setup.effect)
    fx.bindTime(setup.time, isPlaying = playing)
    fx.bindFloat(setup.speed, speedUi / 100f)
    fx.bindFloat(setup.grid, gridUi)
    fx.bindFloat(setup.amount, amountUi / 100f)

    DemoLayout(
        generatedAgsl = rememberGeneratedAgsl(setup.effect),
        preview = {
            DemoPreviewStage(modifier = Modifier.redbyteFx(fx))
        },
        controls = {
            SwitchRow("Play", playing) {
                playing = it
            }
            SliderRow(
                title = "Speed",
                value = speedUi,
                range = 0f..300f,
                formatValue = { "${it / 100f}x" }
            ) {
                speedUi = it
            }
            SliderRow("Grid", gridUi, 4f..24f) {
                gridUi = it
            }
            SliderRow("Amount", amountUi, 0f..100f) {
                amountUi = it
            }
        }
    )
}

@Composable
fun DemoSignal() {
    var density by rememberSaveable { mutableFloatStateOf(8f) }
    var lineWidthUi by rememberSaveable { mutableFloatStateOf(8f) }
    var amountUi by rememberSaveable { mutableFloatStateOf(85f) }

    val setup = remember {
        var densityParam: FxParam.Float? = null
        var lineWidthParam: FxParam.Float? = null
        var amountParam: FxParam.Float? = null
        val effect = redbytefx {
            val densityUniform = uniformFloat(8f, "signal_density")
            val lineWidthUniform = uniformFloat(0.08f, "signal_line_width")
            val amountUniform = uniformFloat(0.85f, "signal_amount")
            val pulseBand = fn(
                name = "pulse_band",
                arg1 = FloatType,
                arg2 = FloatType,
                returns = FloatType
            ) { phase, threshold ->
                step(threshold, smoothstep(0.08f, 0.92f, fract(phase)))
            }
            densityParam = densityUniform
            lineWidthParam = lineWidthUniform
            amountParam = amountUniform

            val base = let(sample(), "base")
            val uv = let(normalizedUv(), "uv")
            val grid = let(gridMask(uv, densityUniform, lineWidthUniform), "grid")
            val scan = let(scanlines(fragCoord.y, 14f, 3f), "scan")
            val pulse = let(
                pulseBand(uv.y * densityUniform * 0.5f + grid * 0.35f, 0.55f),
                "pulse"
            )
            val hardMask = let(step(0.45f, scan * pulse), "hard_mask")
            val active = let((grid gt 0.05f) or (hardMask gt 0.5f), "active")
            val accent = let(color(float3(0.05f, 0.95f, 0.82f), base.a), "accent")
            val mixed = let(
                mix(
                    base,
                    accent,
                    min(grid * 0.85f + hardMask * 0.35f, 1f)
                ),
                "mixed"
            )

            ifElse(active, mix(base, mixed, amountUniform), base)
        }
        SignalSetup(effect, densityParam!!, lineWidthParam!!, amountParam!!)
    }

    val fx = rememberFxController(setup.effect)
    fx.bindFloat(setup.density, density)
    fx.bindFloat(setup.lineWidth, lineWidthUi / 100f)
    fx.bindFloat(setup.amount, amountUi / 100f)

    DemoLayout(
        generatedAgsl = rememberGeneratedAgsl(setup.effect),
        preview = {
            DemoPreviewStage(modifier = Modifier.redbyteFx(fx))
        },
        controls = {
            SliderRow("Density", density, 2f..20f) {
                density = it
            }
            SliderRow(
                title = "Line Width",
                value = lineWidthUi,
                range = 2f..20f,
                formatValue = { "${it / 100f}" }
            ) {
                lineWidthUi = it
            }
            SliderRow("Amount", amountUi, 0f..100f) {
                amountUi = it
            }
        }
    )
}

@Composable
fun DemoPosterize() {
    var levelsUi by rememberSaveable { mutableFloatStateOf(5f) }
    var amountUi by rememberSaveable { mutableFloatStateOf(85f) }

    val setup = remember {
        var levelsParam: FxParam.Float? = null
        var amountParam: FxParam.Float? = null
        val effect = redbytefx {
            val levels by autoUniformFloat(5f)
            val amount by autoUniformFloat(0.85f)
            levelsParam = levels
            amountParam = amount

            val base = let(sample(), "base")
            val quantized = let(posterize(base, levels), "quantized")
            mix(base, quantized, amount)
        }
        PosterizeSetup(effect, levelsParam!!, amountParam!!)
    }

    val fx = rememberFxController(setup.effect)
    fx.bindFloat(setup.levels, levelsUi)
    fx.bindFloat(setup.amount, amountUi / 100f)

    DemoLayout(
        generatedAgsl = rememberGeneratedAgsl(setup.effect),
        preview = {
            DemoPreviewStage(modifier = Modifier.redbyteFx(fx))
        },
        controls = {
            SliderRow("Levels", levelsUi, 2f..12f) {
                levelsUi = it
            }
            SliderRow("Amount", amountUi, 0f..100f) {
                amountUi = it
            }
        }
    )
}

@Composable
fun DemoFilm() {
    var playing by rememberSaveable { mutableStateOf(true) }
    var grainAmountUi by rememberSaveable { mutableFloatStateOf(18f) }
    var grainScaleUi by rememberSaveable { mutableFloatStateOf(190f) }
    var vignetteAmountUi by rememberSaveable { mutableFloatStateOf(55f) }

    val setup = remember {
        var timeParam: FxParam.Float? = null
        var grainAmountParam: FxParam.Float? = null
        var grainScaleParam: FxParam.Float? = null
        var vignetteAmountParam: FxParam.Float? = null
        val effect = redbytefx {
            val time by autoUniformTime()
            val grainAmount by autoUniformFloat(0.18f)
            val grainScale by autoUniformFloat(190f)
            val vignetteAmount by autoUniformFloat(0.55f)
            timeParam = time
            grainAmountParam = grainAmount
            grainScaleParam = grainScale
            vignetteAmountParam = vignetteAmount

            val base = let(sample(), "base")
            val uv = let(normalizedUv(), "uv")
            val noise = let(grain(uv, time, grainScale), "noise")
            val drift = let(
                remap(
                    valueNoise(uv * 6f + float2(time * 0.08f, 0f)),
                    0f,
                    1f,
                    0.92f,
                    1.05f
                ),
                "drift"
            )
            val mask = let(mix(1f, vignette(uv, 0.35f, 1.05f), vignetteAmount), "mask")
            val lift = let(noise * grainAmount, "lift")
            val grainRgb = let(
                float3(
                    saturate(base.r * drift + lift),
                    saturate(base.g * drift + lift),
                    saturate(base.b * drift + lift)
                ),
                "grain_rgb"
            )

            color(
                grainRgb.x * mask,
                grainRgb.y * mask,
                grainRgb.z * mask,
                base.a
            )
        }
        FilmSetup(
            effect = effect,
            time = timeParam!!,
            grainAmount = grainAmountParam!!,
            grainScale = grainScaleParam!!,
            vignetteAmount = vignetteAmountParam!!
        )
    }

    val fx = rememberFxController(setup.effect)
    fx.bindTime(setup.time, isPlaying = playing)
    fx.bindFloat(setup.grainAmount, grainAmountUi / 100f)
    fx.bindFloat(setup.grainScale, grainScaleUi)
    fx.bindFloat(setup.vignetteAmount, vignetteAmountUi / 100f)

    DemoLayout(
        generatedAgsl = rememberGeneratedAgsl(setup.effect),
        preview = {
            DemoPreviewStage(modifier = Modifier.redbyteFx(fx))
        },
        controls = {
            SwitchRow("Play", playing) {
                playing = it
            }
            SliderRow("Grain", grainAmountUi, 0f..40f) {
                grainAmountUi = it
            }
            SliderRow("Scale", grainScaleUi, 60f..320f) {
                grainScaleUi = it
            }
            SliderRow("Vignette", vignetteAmountUi, 0f..100f) {
                vignetteAmountUi = it
            }
        }
    )
}

@Composable
fun DemoGrade() {
    var amountUi by rememberSaveable { mutableFloatStateOf(82f) }
    var warmthUi by rememberSaveable { mutableFloatStateOf(58f) }
    var glowUi by rememberSaveable { mutableFloatStateOf(38f) }

    val setup = remember {
        var amountParam: FxParam.Float? = null
        var warmthParam: FxParam.Float? = null
        var glowParam: FxParam.Float? = null
        val effect = redbytefx {
            val amount by autoUniformFloat(0.82f)
            val warmth by autoUniformFloat(0.58f)
            val glow by autoUniformFloat(0.38f)
            amountParam = amount
            warmthParam = warmth
            glowParam = glow

            val base = let(sample(), "base")
            val saturated = let(
                adjustSaturation(base, mix(0.9f, 1.55f, amount)),
                "saturated"
            )
            val tint = let(
                color(
                    mix(0.26f, 0.94f, warmth),
                    mix(0.48f, 0.72f, warmth),
                    mix(0.92f, 0.38f, warmth),
                    base.a
                ),
                "tint"
            )
            val multiplied = let(blendMultiply(saturated, tint, 0.25f), "multiplied")
            val screened = let(blendScreen(multiplied, tint, glow), "screened")

            blendOverlay(base, screened, amount)
        }
        GradeSetup(
            effect = effect,
            amount = amountParam!!,
            warmth = warmthParam!!,
            glow = glowParam!!
        )
    }

    val fx = rememberFxController(setup.effect)
    fx.bindFloat(setup.amount, amountUi / 100f)
    fx.bindFloat(setup.warmth, warmthUi / 100f)
    fx.bindFloat(setup.glow, glowUi / 100f)

    DemoLayout(
        generatedAgsl = rememberGeneratedAgsl(setup.effect),
        preview = {
            DemoPreviewStage(modifier = Modifier.redbyteFx(fx))
        },
        controls = {
            SliderRow("Amount", amountUi, 0f..100f) {
                amountUi = it
            }
            SliderRow("Warmth", warmthUi, 0f..100f) {
                warmthUi = it
            }
            SliderRow("Glow", glowUi, 0f..100f) {
                glowUi = it
            }
        }
    )
}

@Composable
fun DemoWarp() {
    var playing by rememberSaveable { mutableStateOf(true) }
    var warpAmountUi by rememberSaveable { mutableFloatStateOf(35f) }
    var scaleUi by rememberSaveable { mutableFloatStateOf(320f) }
    var driftAmountUi by rememberSaveable { mutableFloatStateOf(18f) }

    val setup = remember {
        var timeParam: FxParam.Float? = null
        var warpAmountParam: FxParam.Float? = null
        var scaleParam: FxParam.Float? = null
        var driftAmountParam: FxParam.Float? = null
        val effect = redbytefx {
            val time by autoUniformTime()
            val warpAmount by autoUniformFloat(0.35f)
            val scale by autoUniformFloat(3.2f)
            val driftAmount by autoUniformFloat(18f)
            timeParam = time
            warpAmountParam = warpAmount
            scaleParam = scale
            driftAmountParam = driftAmount

            val uv = let(fragCoord / resolution, "uv")
            val noiseUv = let(
                uv * scale + float2(time * 0.06f, -time * 0.04f),
                "noise_uv"
            )
            val warpedUv = let(domainWarp(noiseUv, time * 0.25f, warpAmount), "warped_uv")
            val drift = let((fbm(warpedUv, octaves = 5) * 2f - 1f) * driftAmount, "drift")

            sample(fragCoord + float2(0f, drift))
        }
        WarpSetup(
            effect = effect,
            time = timeParam!!,
            warpAmount = warpAmountParam!!,
            scale = scaleParam!!,
            driftAmount = driftAmountParam!!
        )
    }

    val fx = rememberFxController(setup.effect)
    fx.bindTime(setup.time, isPlaying = playing)
    fx.bindFloat(setup.warpAmount, warpAmountUi / 100f)
    fx.bindFloat(setup.scale, scaleUi / 100f)
    fx.bindFloat(setup.driftAmount, driftAmountUi)

    DemoLayout(
        generatedAgsl = rememberGeneratedAgsl(setup.effect),
        preview = {
            DemoPreviewStage(modifier = Modifier.redbyteFx(fx))
        },
        controls = {
            SwitchRow("Play", playing) {
                playing = it
            }
            SliderRow("Warp", warpAmountUi, 0f..80f) {
                warpAmountUi = it
            }
            SliderRow("Scale", scaleUi, 120f..520f) {
                scaleUi = it
            }
            SliderRow("Drift", driftAmountUi, 0f..48f) {
                driftAmountUi = it
            }
        }
    )
}

@Composable
fun DemoPrism() {
    var amountUi by rememberSaveable { mutableFloatStateOf(76f) }
    var spreadUi by rememberSaveable { mutableFloatStateOf(22f) }
    var shiftUi by rememberSaveable { mutableFloatStateOf(10f) }

    val setup = remember {
        var amountParam: FxParam.Float? = null
        var spreadParam: FxParam.Float? = null
        var shiftParam: FxParam.Float? = null
        val effect = redbytefx {
            val amount by autoUniformFloat(0.76f)
            val spread by autoUniformFloat(0.22f)
            val shift by autoUniformFloat(10f)
            amountParam = amount
            spreadParam = spread
            shiftParam = shift

            val base = let(sample(), "base")
            val uv = let(fragCoord / resolution, "uv")
            val palette = let(cosinePalette(luminance(base) + uv.x * spread), "palette")
            val tint = let(color(palette, base.a), "tint")
            val refracted = let(
                chromaticOffset(
                    offset = shift,
                    direction = float2(1f, 0.3f),
                    amount = amount
                ),
                "refracted"
            )

            blendScreen(refracted, tint, amount)
        }
        PrismSetup(
            effect = effect,
            amount = amountParam!!,
            spread = spreadParam!!,
            shift = shiftParam!!
        )
    }

    val fx = rememberFxController(setup.effect)
    fx.bindFloat(setup.amount, amountUi / 100f)
    fx.bindFloat(setup.spread, spreadUi / 100f)
    fx.bindFloat(setup.shift, shiftUi)

    DemoLayout(
        generatedAgsl = rememberGeneratedAgsl(setup.effect),
        preview = {
            DemoPreviewStage(modifier = Modifier.redbyteFx(fx))
        },
        controls = {
            SliderRow("Amount", amountUi, 0f..100f) {
                amountUi = it
            }
            SliderRow("Spread", spreadUi, 0f..60f) {
                spreadUi = it
            }
            SliderRow("Shift", shiftUi, 0f..24f) {
                shiftUi = it
            }
        }
    )
}

@Composable
fun DemoSpotlight() {
    var centerXUi by rememberSaveable { mutableFloatStateOf(36f) }
    var centerYUi by rememberSaveable { mutableFloatStateOf(52f) }
    var radiusUi by rememberSaveable { mutableFloatStateOf(18f) }
    var amountUi by rememberSaveable { mutableFloatStateOf(82f) }

    val setup = remember {
        var centerParam: FxParam.Float2? = null
        var radiusParam: FxParam.Float? = null
        var amountParam: FxParam.Float? = null
        val effect = redbytefx {
            val center by autoUniformFloat2(0.36f, 0.52f)
            val radius by autoUniformFloat(0.18f)
            val amount by autoUniformFloat(0.82f)
            centerParam = center
            radiusParam = radius
            amountParam = amount

            val base = let(sample(), "base")
            val uv = let(fragCoord / resolution, "uv")
            val focus = let(
                circleMask(
                    uv,
                    center = center,
                    radius = radius,
                    feather = 0.18f
                ),
                "focus"
            )
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
            val dimmed = let(base * mix(0.28f, 1f, focus), "dimmed")
            val haloTint = let(color(float3(0.15f, 0.92f, 0.98f), base.a), "halo_tint")
            val panelTint = let(color(float3(0.98f, 0.73f, 0.26f), base.a), "panel_tint")
            val focused = let(blendScreen(dimmed, haloTint, halo * amount), "focused")

            blendOverlay(focused, panelTint, panel * amount * 0.35f)
        }
        SpotlightSetup(
            effect = effect,
            center = centerParam!!,
            radius = radiusParam!!,
            amount = amountParam!!
        )
    }

    val fx = rememberFxController(setup.effect)
    fx.bindFloat2(setup.center, centerXUi / 100f, centerYUi / 100f)
    fx.bindFloat(setup.radius, radiusUi / 100f)
    fx.bindFloat(setup.amount, amountUi / 100f)

    DemoLayout(
        generatedAgsl = rememberGeneratedAgsl(setup.effect),
        preview = {
            DemoPreviewStage(modifier = Modifier.redbyteFx(fx))
        },
        controls = {
            SliderRow("Center X", centerXUi, 10f..90f) {
                centerXUi = it
            }
            SliderRow("Center Y", centerYUi, 18f..82f) {
                centerYUi = it
            }
            SliderRow("Radius", radiusUi, 8f..32f) {
                radiusUi = it
            }
            SliderRow("Amount", amountUi, 0f..100f) {
                amountUi = it
            }
        }
    )
}

@Composable
fun DemoBeacon() {
    var playing by rememberSaveable { mutableStateOf(true) }
    var speedUi by rememberSaveable { mutableFloatStateOf(65f) }
    var radiusUi by rememberSaveable { mutableFloatStateOf(17f) }
    var amountUi by rememberSaveable { mutableFloatStateOf(88f) }

    val setup = remember {
        var timeParam: FxParam.Float? = null
        var speedParam: FxParam.Float? = null
        var radiusParam: FxParam.Float? = null
        var amountParam: FxParam.Float? = null
        val effect = redbytefx {
            val time by autoUniformTime()
            val speed by autoUniformFloat(0.65f)
            val radius by autoUniformFloat(0.17f)
            val amount by autoUniformFloat(0.88f)
            timeParam = time
            speedParam = speed
            radiusParam = radius
            amountParam = amount

            val base = let(sample(), "base")
            val uv = let(fragCoord / resolution, "uv")
            val phase = let(pingPong(time * speed, 1f), "phase")
            val travel = let(easeInOutSine(phase), "travel")
            val glow = let(easeInOutCubic(pingPong(time * speed + 0.22f, 1f)), "glow")
            val center = let(
                float2(
                    mix(0.18f, 0.82f, travel),
                    0.5f + sin(time * 0.8f) * 0.12f
                ),
                "center"
            )
            val focus = let(circleMask(uv, center = center, radius = radius, feather = 0.16f), "focus")
            val halo = let(
                ringMask(
                    uv,
                    center = center,
                    radius = radius + 0.06f,
                    width = 0.08f,
                    feather = 0.05f
                ),
                "halo"
            )
            val dimmed = let(base * mix(0.28f, 1f, focus), "dimmed")
            val beamTint = let(color(float3(0.12f, 0.94f, 0.98f), base.a), "beam_tint")
            val haloTint = let(color(float3(1f, 0.86f, 0.35f), base.a), "halo_tint")
            val focused = let(blendScreen(dimmed, beamTint, focus * amount * 0.7f), "focused")

            blendScreen(focused, haloTint, halo * glow * amount)
        }
        BeaconSetup(
            effect = effect,
            time = timeParam!!,
            speed = speedParam!!,
            radius = radiusParam!!,
            amount = amountParam!!
        )
    }

    val fx = rememberFxController(setup.effect)
    fx.bindTime(setup.time, isPlaying = playing)
    fx.bindFloat(setup.speed, speedUi / 100f)
    fx.bindFloat(setup.radius, radiusUi / 100f)
    fx.bindFloat(setup.amount, amountUi / 100f)

    DemoLayout(
        generatedAgsl = rememberGeneratedAgsl(setup.effect),
        preview = {
            DemoPreviewStage(modifier = Modifier.redbyteFx(fx))
        },
        controls = {
            SwitchRow("Play", playing) {
                playing = it
            }
            SliderRow("Speed", speedUi, 20f..140f) {
                speedUi = it
            }
            SliderRow("Radius", radiusUi, 10f..28f) {
                radiusUi = it
            }
            SliderRow("Amount", amountUi, 0f..100f) {
                amountUi = it
            }
        }
    )
}

@Composable
fun DemoComposite() {
    var radiusUi by rememberSaveable { mutableFloatStateOf(20f) }
    var panelWidthUi by rememberSaveable { mutableFloatStateOf(34f) }
    var amountUi by rememberSaveable { mutableFloatStateOf(82f) }

    val setup = remember {
        var radiusParam: FxParam.Float? = null
        var panelWidthParam: FxParam.Float? = null
        var amountParam: FxParam.Float? = null
        val effect = redbytefx {
            val radius by autoUniformFloat(0.2f)
            val panelWidth by autoUniformFloat(0.34f)
            val amount by autoUniformFloat(0.82f)
            radiusParam = radius
            panelWidthParam = panelWidth
            amountParam = amount

            val base = let(sample(), "base")
            val uv = let(fragCoord / resolution, "uv")
            val focus = let(
                circleMask(
                    uv,
                    center = float2(0.34f, 0.5f),
                    radius = radius,
                    feather = 0.16f
                ),
                "focus"
            )
            val halo = let(
                ringMask(
                    uv,
                    center = float2(0.34f, 0.5f),
                    radius = radius + 0.05f,
                    width = 0.1f,
                    feather = 0.05f
                ),
                "halo"
            )
            val panel = let(
                rectMask(
                    uv,
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
            val focusTint = let(color(float3(0.92f, 0.98f, 1f), base.a), "focus_tint")

            maskedMix(base, maskedMix(overlaid, focusTint, focus, amount * 0.45f), focus + halo * 0.2f, amount)
        }
        CompositeSetup(
            effect = effect,
            radius = radiusParam!!,
            panelWidth = panelWidthParam!!,
            amount = amountParam!!
        )
    }

    val fx = rememberFxController(setup.effect)
    fx.bindFloat(setup.radius, radiusUi / 100f)
    fx.bindFloat(setup.panelWidth, panelWidthUi / 100f)
    fx.bindFloat(setup.amount, amountUi / 100f)

    DemoLayout(
        generatedAgsl = rememberGeneratedAgsl(setup.effect),
        preview = {
            DemoPreviewStage(modifier = Modifier.redbyteFx(fx))
        },
        controls = {
            SliderRow("Radius", radiusUi, 10f..32f) {
                radiusUi = it
            }
            SliderRow("Panel Width", panelWidthUi, 20f..50f) {
                panelWidthUi = it
            }
            SliderRow("Amount", amountUi, 0f..100f) {
                amountUi = it
            }
        }
    )
}

@Composable
fun DemoFrame() {
    var playing by rememberSaveable { mutableStateOf(true) }
    var speedUi by rememberSaveable { mutableFloatStateOf(72f) }
    var thicknessUi by rememberSaveable { mutableFloatStateOf(10f) }
    var amountUi by rememberSaveable { mutableFloatStateOf(82f) }

    val setup = remember {
        var timeParam: FxParam.Float? = null
        var speedParam: FxParam.Float? = null
        var thicknessParam: FxParam.Float? = null
        var amountParam: FxParam.Float? = null
        val effect = redbytefx {
            val time by autoUniformTime()
            val speed by autoUniformFloat(0.72f)
            val thickness by autoUniformFloat(0.1f)
            val amount by autoUniformFloat(0.82f)
            timeParam = time
            speedParam = speed
            thicknessParam = thickness
            amountParam = amount

            val base = let(sample(), "base")
            val uv = let(fragCoord / resolution, "uv")
            val frame = let(frameMask(uv, thickness, 0.03f), "frame")
            val interior = let(edgeFade(uv, thickness + 0.08f), "interior")
            val sweepCenter = let(0.12f + pingPong(time * speed * 0.2f, 1f) * 0.76f, "sweep_center")
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
            val shellTint = let(color(float3(0.12f, 0.96f, 0.72f), base.a), "shell_tint")
            val innerTint = let(color(float3(0.08f, 0.24f, 0.16f), base.a), "inner_tint")
            val screened = let(maskedScreen(base, shellTint, frame * sweep, amount), "screened")

            maskedOverlay(
                base = screened,
                blend = innerTint,
                mask = frame + (1f - interior) * 0.28f,
                amount = amount * 0.45f
            )
        }
        FrameSetup(
            effect = effect,
            time = timeParam!!,
            speed = speedParam!!,
            thickness = thicknessParam!!,
            amount = amountParam!!
        )
    }

    val fx = rememberFxController(setup.effect)
    fx.bindTime(setup.time, isPlaying = playing)
    fx.bindFloat(setup.speed, speedUi / 100f)
    fx.bindFloat(setup.thickness, thicknessUi / 100f)
    fx.bindFloat(setup.amount, amountUi / 100f)

    DemoLayout(
        generatedAgsl = rememberGeneratedAgsl(setup.effect),
        preview = {
            DemoPreviewStage(
                modifier = Modifier.redbyteFx(fx),
                label = "Frame//Shell"
            )
        },
        controls = {
            SwitchRow("Play", playing) {
                playing = it
            }
            SliderRow("Speed", speedUi, 20f..140f) {
                speedUi = it
            }
            SliderRow("Thickness", thicknessUi, 4f..24f) {
                thicknessUi = it
            }
            SliderRow("Amount", amountUi, 0f..100f) {
                amountUi = it
            }
        }
    )
}

@Composable
fun DemoCorner() {
    var playing by rememberSaveable { mutableStateOf(true) }
    var sizeUi by rememberSaveable { mutableFloatStateOf(22f) }
    var thicknessUi by rememberSaveable { mutableFloatStateOf(8f) }
    var amountUi by rememberSaveable { mutableFloatStateOf(84f) }

    val setup = remember {
        var timeParam: FxParam.Float? = null
        var sizeParam: FxParam.Float? = null
        var thicknessParam: FxParam.Float? = null
        var amountParam: FxParam.Float? = null
        val effect = redbytefx {
            val time by autoUniformTime()
            val size by autoUniformFloat(0.22f)
            val thickness by autoUniformFloat(0.08f)
            val amount by autoUniformFloat(0.84f)
            timeParam = time
            sizeParam = size
            thicknessParam = thickness
            amountParam = amount

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
            val accent = let(color(float3(0.1f, 0.98f, 0.68f), base.a), "accent")

            maskedScreen(base, accent, corners * sweep, amount)
        }
        CornerSetup(
            effect = effect,
            time = timeParam!!,
            size = sizeParam!!,
            thickness = thicknessParam!!,
            amount = amountParam!!
        )
    }

    val fx = rememberFxController(setup.effect)
    fx.bindTime(setup.time, isPlaying = playing)
    fx.bindFloat(setup.size, sizeUi / 100f)
    fx.bindFloat(setup.thickness, thicknessUi / 100f)
    fx.bindFloat(setup.amount, amountUi / 100f)

    DemoLayout(
        generatedAgsl = rememberGeneratedAgsl(setup.effect),
        preview = {
            DemoPreviewStage(
                modifier = Modifier.redbyteFx(fx),
                label = "Corner//HUD"
            )
        },
        controls = {
            SwitchRow("Play", playing) {
                playing = it
            }
            SliderRow("Corner Size", sizeUi, 10f..36f) {
                sizeUi = it
            }
            SliderRow("Thickness", thicknessUi, 4f..18f) {
                thicknessUi = it
            }
            SliderRow("Amount", amountUi, 0f..100f) {
                amountUi = it
            }
        }
    )
}

@Composable
fun DemoReveal() {
    var playing by rememberSaveable { mutableStateOf(true) }
    var speedUi by rememberSaveable { mutableFloatStateOf(72f) }
    var amountUi by rememberSaveable { mutableFloatStateOf(90f) }
    var mode by rememberSaveable { mutableStateOf(RevealMode.Horizontal) }

    val setup = remember {
        var timeParam: FxParam.Float? = null
        var speedParam: FxParam.Float? = null
        var modeParam: FxParam.Float? = null
        var amountParam: FxParam.Float? = null
        val effect = redbytefx {
            val time by autoUniformTime()
            val speed by autoUniformFloat(0.72f)
            val modeValue by autoUniformFloat(0f)
            val amount by autoUniformFloat(0.9f)
            timeParam = time
            speedParam = speed
            modeParam = modeValue
            amountParam = amount

            val base = let(sample(), "base")
            val uv = let(fragCoord / resolution, "uv")
            val progress = let(easeInOutSine(pingPong(time * speed, 1f)), "progress")
            val horizontal = let(horizontalReveal(uv, progress, feather = 0.07f), "horizontal")
            val vertical = let(verticalReveal(uv, progress, feather = 0.07f, fromTop = false), "vertical")
            val radial = let(radialReveal(uv, progress, feather = 0.08f, maxRadius = 0.9f), "radial")
            val reveal = let(
                ifElse(
                    modeValue lt 0.5f,
                    horizontal,
                    ifElse(modeValue lt 1.5f, vertical, radial)
                ),
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
        RevealSetup(
            effect = effect,
            time = timeParam!!,
            speed = speedParam!!,
            mode = modeParam!!,
            amount = amountParam!!
        )
    }

    val fx = rememberFxController(setup.effect)
    fx.bindTime(setup.time, isPlaying = playing)
    fx.bindFloat(setup.speed, speedUi / 100f)
    fx.bindFloat(
        setup.mode,
        when (mode) {
            RevealMode.Horizontal -> 0f
            RevealMode.Vertical -> 1f
            RevealMode.Radial -> 2f
        }
    )
    fx.bindFloat(setup.amount, amountUi / 100f)

    DemoLayout(
        generatedAgsl = rememberGeneratedAgsl(setup.effect),
        preview = {
            DemoPreviewStage(modifier = Modifier.redbyteFx(fx))
        },
        controls = {
            SwitchRow("Play", playing) {
                playing = it
            }
            Text(text = "Mode", style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                RadioRow("Horizontal", selected = mode == RevealMode.Horizontal) {
                    mode = RevealMode.Horizontal
                }
                RadioRow("Vertical", selected = mode == RevealMode.Vertical) {
                    mode = RevealMode.Vertical
                }
                RadioRow("Radial", selected = mode == RevealMode.Radial) {
                    mode = RevealMode.Radial
                }
            }
            SliderRow("Speed", speedUi, 20f..140f) {
                speedUi = it
            }
            SliderRow("Amount", amountUi, 0f..100f) {
                amountUi = it
            }
        }
    )
}

@Composable
fun DemoSweep() {
    var playing by rememberSaveable { mutableStateOf(true) }
    var speedUi by rememberSaveable { mutableFloatStateOf(74f) }
    var widthUi by rememberSaveable { mutableFloatStateOf(22f) }
    var amountUi by rememberSaveable { mutableFloatStateOf(84f) }

    val setup = remember {
        var timeParam: FxParam.Float? = null
        var speedParam: FxParam.Float? = null
        var widthParam: FxParam.Float? = null
        var amountParam: FxParam.Float? = null
        val effect = redbytefx {
            val time by autoUniformTime()
            val speed by autoUniformFloat(0.74f)
            val width by autoUniformFloat(0.22f)
            val amount by autoUniformFloat(0.84f)
            timeParam = time
            speedParam = speed
            widthParam = width
            amountParam = amount

            val base = let(sample(), "base")
            val uv = let(fragCoord / resolution, "uv")
            val center = let(0.16f + pingPong(time * speed * 0.22f, 1f) * 0.68f, "center")
            val ramp = let(linearRamp(uv, direction = float2(1f, -0.35f), start = 0.08f, end = 0.92f), "ramp")
            val sweep = let(
                directionalSweep(
                    uv = uv,
                    direction = float2(1f, -0.35f),
                    center = center,
                    width = width,
                    feather = 0.08f
                ),
                "sweep"
            )
            val radial = let(radialRamp(uv, innerRadius = 0.12f, outerRadius = 0.68f), "radial")
            val tint = let(
                color(
                    mix(0.06f, 0.28f, ramp),
                    mix(0.24f, 1f, sweep),
                    mix(0.18f, 0.62f, ramp + sweep * 0.4f),
                    base.a
                ),
                "tint"
            )
            val screened = let(maskedScreen(base, tint, sweep * radial, amount), "screened")

            maskedOverlay(
                base = screened,
                blend = color(float3(0.92f, 1f, 0.78f), base.a),
                mask = sweep,
                amount = amount * 0.34f
            )
        }
        SweepSetup(
            effect = effect,
            time = timeParam!!,
            speed = speedParam!!,
            width = widthParam!!,
            amount = amountParam!!
        )
    }

    val fx = rememberFxController(setup.effect)
    fx.bindTime(setup.time, isPlaying = playing)
    fx.bindFloat(setup.speed, speedUi / 100f)
    fx.bindFloat(setup.width, widthUi / 100f)
    fx.bindFloat(setup.amount, amountUi / 100f)

    DemoLayout(
        generatedAgsl = rememberGeneratedAgsl(setup.effect),
        preview = {
            DemoPreviewStage(
                modifier = Modifier.redbyteFx(fx),
                label = "Sweep//Track"
            )
        },
        controls = {
            SwitchRow("Play", playing) {
                playing = it
            }
            SliderRow("Speed", speedUi, 20f..140f) {
                speedUi = it
            }
            SliderRow("Width", widthUi, 8f..40f) {
                widthUi = it
            }
            SliderRow("Amount", amountUi, 0f..100f) {
                amountUi = it
            }
        }
    )
}

@Composable
fun DemoRadar() {
    var playing by rememberSaveable { mutableStateOf(true) }
    var speedUi by rememberSaveable { mutableFloatStateOf(72f) }
    var radiusUi by rememberSaveable { mutableFloatStateOf(34f) }
    var amountUi by rememberSaveable { mutableFloatStateOf(86f) }

    val setup = remember {
        var timeParam: FxParam.Float? = null
        var speedParam: FxParam.Float? = null
        var radiusParam: FxParam.Float? = null
        var amountParam: FxParam.Float? = null
        val effect = redbytefx {
            val time by autoUniformTime()
            val speed by autoUniformFloat(0.72f)
            val radius by autoUniformFloat(0.34f)
            val amount by autoUniformFloat(0.86f)
            timeParam = time
            speedParam = speed
            radiusParam = radius
            amountParam = amount

            val base = let(sample(), "base")
            val uv = let(fragCoord / resolution, "uv")
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
            val outerRing = let(ringMask(uv, radius = radius, width = 0.016f, feather = 0.012f), "outer_ring")
            val innerRing = let(
                ringMask(
                    uv = uv,
                    radius = max(radius * 0.58f, 0.08f),
                    width = 0.014f,
                    feather = 0.012f
                ),
                "inner_ring"
            )
            val beam = let(
                radialRamp(
                    uv = uv,
                    innerRadius = float(0.06f),
                    outerRadius = radius + 0.18f
                ),
                "beam"
            )
            val mask = let(max(max(sweep * beam, arc), max(outerRing, innerRing)), "mask")
            val tint = let(
                color(
                    mix(0.05f, 0.18f, polar.x * 1.4f),
                    mix(0.24f, 1f, sweep + arc * 0.55f),
                    mix(0.10f, 0.62f, polar.y * 0.45f + outerRing * 0.35f),
                    base.a
                ),
                "tint"
            )
            val screened = let(maskedScreen(base, tint, mask, amount), "screened")

            maskedOverlay(
                base = screened,
                blend = color(float3(0.82f, 1f, 0.72f), base.a),
                mask = arc,
                amount = amount * 0.32f
            )
        }
        RadarSetup(
            effect = effect,
            time = timeParam!!,
            speed = speedParam!!,
            radius = radiusParam!!,
            amount = amountParam!!
        )
    }

    val fx = rememberFxController(setup.effect)
    fx.bindTime(setup.time, isPlaying = playing)
    fx.bindFloat(setup.speed, speedUi / 100f)
    fx.bindFloat(setup.radius, radiusUi / 100f)
    fx.bindFloat(setup.amount, amountUi / 100f)

    DemoLayout(
        generatedAgsl = rememberGeneratedAgsl(setup.effect),
        preview = {
            DemoPreviewStage(
                modifier = Modifier.redbyteFx(fx),
                label = "Radar//Polar"
            )
        },
        controls = {
            SwitchRow("Play", playing) {
                playing = it
            }
            SliderRow("Speed", speedUi, 20f..140f) {
                speedUi = it
            }
            SliderRow("Radius", radiusUi, 18f..44f) {
                radiusUi = it
            }
            SliderRow("Amount", amountUi, 0f..100f) {
                amountUi = it
            }
        }
    )
}

@Composable
fun DemoHalo() {
    var playing by rememberSaveable { mutableStateOf(true) }
    var speedUi by rememberSaveable { mutableFloatStateOf(68f) }
    var radiusUi by rememberSaveable { mutableFloatStateOf(24f) }
    var amountUi by rememberSaveable { mutableFloatStateOf(82f) }

    val setup = remember {
        var timeParam: FxParam.Float? = null
        var speedParam: FxParam.Float? = null
        var radiusParam: FxParam.Float? = null
        var amountParam: FxParam.Float? = null
        val effect = redbytefx {
            val time by autoUniformTime()
            val speed by autoUniformFloat(0.68f)
            val radius by autoUniformFloat(0.24f)
            val amount by autoUniformFloat(0.82f)
            timeParam = time
            speedParam = speed
            radiusParam = radius
            amountParam = amount

            val base = let(sample(), "base")
            val uv = let(fragCoord / resolution, "uv")
            val local = let(centeredUv(uv), "local")
            val aspectLocal = let(aspectCenteredUv(uv, resolution), "aspect_local")
            val dir = let(radialDirection(uv, resolution), "dir")
            val phase = let(easeInOutSine(pingPong(time * speed * 0.18f, 1f)), "phase")
            val glow = let(
                centerGlow(
                    uv = uv,
                    resolution = resolution,
                    radius = radius * (0.82f + phase * 0.32f),
                    feather = 0.18f
                ),
                "glow"
            )
            val rim = let(
                rimLight(
                    uv = uv,
                    resolution = resolution,
                    radius = radius + 0.08f * phase,
                    width = 0.075f,
                    feather = 0.024f
                ),
                "rim"
            )
            val drift = let(saturate(0.5f + aspectLocal.x * 0.55f - local.y * 0.25f), "drift")
            val facing = let(saturate(dir.x * 0.5f - dir.y * 0.35f + 0.5f), "facing")
            val mask = let(max(glow * 0.9f, rim), "mask")
            val tint = let(
                color(
                    mix(0.04f, 0.18f, drift),
                    mix(0.22f, 1f, glow + rim * 0.45f),
                    mix(0.12f, 0.72f, facing),
                    base.a
                ),
                "tint"
            )
            val screened = let(maskedScreen(base, tint, mask, amount), "screened")

            maskedOverlay(
                base = screened,
                blend = color(float3(0.82f, 1f, 0.74f), base.a),
                mask = rim,
                amount = amount * 0.28f
            )
        }
        HaloSetup(
            effect = effect,
            time = timeParam!!,
            speed = speedParam!!,
            radius = radiusParam!!,
            amount = amountParam!!
        )
    }

    val fx = rememberFxController(setup.effect)
    fx.bindTime(setup.time, isPlaying = playing)
    fx.bindFloat(setup.speed, speedUi / 100f)
    fx.bindFloat(setup.radius, radiusUi / 100f)
    fx.bindFloat(setup.amount, amountUi / 100f)

    DemoLayout(
        generatedAgsl = rememberGeneratedAgsl(setup.effect),
        preview = {
            DemoPreviewStage(
                modifier = Modifier.redbyteFx(fx),
                label = "Halo//Light"
            )
        },
        controls = {
            SwitchRow("Play", playing) {
                playing = it
            }
            SliderRow("Speed", speedUi, 20f..140f) {
                speedUi = it
            }
            SliderRow("Radius", radiusUi, 12f..36f) {
                radiusUi = it
            }
            SliderRow("Amount", amountUi, 0f..100f) {
                amountUi = it
            }
        }
    )
}

@Composable
fun DemoAurora() {
    var playing by rememberSaveable { mutableStateOf(true) }
    var amountUi by rememberSaveable { mutableFloatStateOf(78f) }
    var chromaUi by rememberSaveable { mutableFloatStateOf(11f) }
    var spectralUi by rememberSaveable { mutableFloatStateOf(44f) }
    var speedUi by rememberSaveable { mutableFloatStateOf(42f) }

    val setup = remember {
        var timeParam: FxParam.Float? = null
        var amountParam: FxParam.Float? = null
        var chromaParam: FxParam.Float? = null
        var spectralParam: FxParam.Float? = null
        var speedParam: FxParam.Float? = null
        val effect = redbytefx {
            val time by autoUniformTime()
            val amount by autoUniformFloat(0.78f)
            val chromaPx by autoUniformFloat(11f)
            val spectral by autoUniformFloat(0.44f)
            val speed by autoUniformFloat(0.42f)
            timeParam = time
            amountParam = amount
            chromaParam = chromaPx
            spectralParam = spectral
            speedParam = speed

            val base = let(sample(), "base")
            val uv = let(fragCoord / resolution, "uv")
            val phase = let(fract(time * speed), "phase")
            val sweep = let(
                angularSweep(
                    uv = uv,
                    center = float2(0.5f, 0.5f),
                    angle = phase,
                    width = 0.26f,
                    feather = 0.09f
                ),
                "sweep"
            )
            val rim = let(
                rimLight(
                    uv = uv,
                    resolution = resolution,
                    radius = 0.42f,
                    width = 0.085f,
                    feather = 0.03f
                ),
                "rim"
            )
            val mask = let(saturate(max(rim, sweep * 0.72f)), "mask")
            val luma = let(luminance(base), "luma")
            val pal = let(
                cosinePalette(luma + uv.x * spectral + phase * 0.55f - rim * 0.12f),
                "pal"
            )
            val tint = let(color(pal, base.a), "tint")
            val split = let(
                chromaticOffset(
                    offset = chromaPx,
                    direction = float2(1f, -0.25f),
                    amount = amount
                ),
                "split"
            )
            val irid = let(blendScreen(base, tint, mask * amount), "irid")

            maskedMix(split, irid, mask, amount)
        }
        AuroraSetup(
            effect = effect,
            time = timeParam!!,
            amount = amountParam!!,
            chromaPx = chromaParam!!,
            spectral = spectralParam!!,
            speed = speedParam!!
        )
    }

    val fx = rememberFxController(setup.effect)
    fx.bindTime(setup.time, isPlaying = playing)
    fx.bindFloat(setup.amount, amountUi / 100f)
    fx.bindFloat(setup.chromaPx, chromaUi)
    fx.bindFloat(setup.spectral, spectralUi / 100f)
    fx.bindFloat(setup.speed, speedUi / 100f)

    DemoLayout(
        generatedAgsl = rememberGeneratedAgsl(setup.effect),
        preview = {
            DemoPreviewStage(
                modifier = Modifier.redbyteFx(fx),
                label = "Aurora//Showcase"
            )
        },
        controls = {
            SwitchRow("Play", playing) {
                playing = it
            }
            SliderRow("Mix", amountUi, 0f..100f) {
                amountUi = it
            }
            SliderRow("Chroma px", chromaUi, 0f..24f) {
                chromaUi = it
            }
            SliderRow("Spectral", spectralUi, 12f..85f) {
                spectralUi = it
            }
            SliderRow("Flow", speedUi, 18f..95f) {
                speedUi = it
            }
        }
    )
}

@Composable
fun DemoLiquidGlass() {
    var playing by rememberSaveable { mutableStateOf(true) }
    var refractionUi by rememberSaveable { mutableFloatStateOf(7.2f) }
    var speedUi by rememberSaveable { mutableFloatStateOf(48f) }
    var chromaPxUi by rememberSaveable { mutableFloatStateOf(5.5f) }
    var chromaMixUi by rememberSaveable { mutableFloatStateOf(52f) }
    var edgeMixUi by rememberSaveable { mutableFloatStateOf(72f) }

    val setup = remember {
        var timeParam: FxParam.Float? = null
        var refractionParam: FxParam.Float? = null
        var speedParam: FxParam.Float? = null
        var chromaPxParam: FxParam.Float? = null
        var chromaMixParam: FxParam.Float? = null
        var edgeMixParam: FxParam.Float? = null
        val effect = redbytefx {
            val time by autoUniformTime()
            val refraction by autoUniformFloat(0.072f)
            val speed by autoUniformFloat(0.48f)
            val chromaPx by autoUniformFloat(5.5f)
            val chromaMix by autoUniformFloat(0.52f)
            val edgeMix by autoUniformFloat(0.72f)
            timeParam = time
            refractionParam = refraction
            speedParam = speed
            chromaPxParam = chromaPx
            chromaMixParam = chromaMix
            edgeMixParam = edgeMix

            val uv = let(normalizedUv(), "uv")
            val warp = let(domainWarp(uv * 3.2f, time * speed, refraction), "warp")
            val warpedUv = let(
                float2(saturate(warp.x), saturate(warp.y)),
                "warped_uv"
            )
            val glass = let(sampleUv(warpedUv), "glass")
            val px = chromaPx / max(resolution.x, 0.0001f)
            val r = sampleUv(warpedUv - float2(px, 0f)).r
            val g = glass.g
            val b = sampleUv(warpedUv + float2(px, 0f)).b
            val chromaGlass = let(color(r, g, b, glass.a), "chroma_glass")
            val dist = length(aspectCenteredUv(uv, resolution))
            val shell = let(1f - smoothstep(0.38f, 0.52f, dist), "shell")
            val rim = let(
                rimLight(
                    uv = uv,
                    resolution = resolution,
                    radius = 0.4f,
                    width = 0.068f,
                    feather = 0.032f
                ),
                "rim"
            )
            val edge = let(saturate(max(shell, rim)), "edge")
            val spec = let(pow(edge, 1.75f), "spec")
            val glassBody = let(mix(glass, chromaGlass, edge * chromaMix), "glass_body")
            val ice = color(float3(0.82f, 0.92f, 1f), 0.14f)

            blendScreen(glassBody, ice, spec * edgeMix)
        }
        LiquidGlassSetup(
            effect = effect,
            time = timeParam!!,
            refraction = refractionParam!!,
            speed = speedParam!!,
            chromaPx = chromaPxParam!!,
            chromaMix = chromaMixParam!!,
            edgeMix = edgeMixParam!!
        )
    }

    val fx = rememberFxController(setup.effect)
    fx.bindTime(setup.time, isPlaying = playing)
    fx.bindFloat(setup.refraction, refractionUi / 100f)
    fx.bindFloat(setup.speed, speedUi / 100f)
    fx.bindFloat(setup.chromaPx, chromaPxUi)
    fx.bindFloat(setup.chromaMix, chromaMixUi / 100f)
    fx.bindFloat(setup.edgeMix, edgeMixUi / 100f)

    DemoLayout(
        generatedAgsl = rememberGeneratedAgsl(setup.effect),
        preview = {
            DemoLiquidGlassPreviewStage(fx = fx)
        },
        controls = {
            SwitchRow("Play", playing) {
                playing = it
            }
            SliderRow("Refraction", refractionUi, 1.5f..14f) {
                refractionUi = it
            }
            SliderRow("Flow", speedUi, 15f..95f) {
                speedUi = it
            }
            SliderRow("Chroma px", chromaPxUi, 0f..14f) {
                chromaPxUi = it
            }
            SliderRow("Edge RGB", chromaMixUi, 0f..100f) {
                chromaMixUi = it
            }
            SliderRow("Ice edge", edgeMixUi, 0f..100f) {
                edgeMixUi = it
            }
        }
    )
}

@Composable
fun DemoSigil() {
    var playing by rememberSaveable { mutableStateOf(true) }
    var speedUi by rememberSaveable { mutableFloatStateOf(72f) }
    var amountUi by rememberSaveable { mutableFloatStateOf(84f) }

    val setup = remember {
        var timeParam: FxParam.Float? = null
        var speedParam: FxParam.Float? = null
        var amountParam: FxParam.Float? = null
        val effect = redbytefx {
            val time by autoUniformTime()
            val speed by autoUniformFloat(0.72f)
            val amount by autoUniformFloat(0.84f)
            timeParam = time
            speedParam = speed
            amountParam = amount

            val base = let(sample(), "base")
            val uv = let(fragCoord / resolution, "uv")
            val sigil = let(aspectCenteredUv(uv, resolution), "sigil")
            val pulse = let(easeInOutSine(pingPong(time * speed * 0.18f, 1f)), "pulse")
            val frame = let(
                softStroke(
                    distance = sdRoundedBox(
                        point = sigil,
                        halfSize = float2(0.35f, 0.35f),
                        radius = 0.16f
                    ),
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
            val core = let(
                softFill(
                    distance = sdCircle(sigil, radius = 0.10f + pulse * 0.05f),
                    feather = 0.015f
                ),
                "core"
            )
            val spine = let(
                softFill(
                    distance = sdBox(
                        point = sigil,
                        halfSize = float2(0.05f, 0.22f + pulse * 0.05f)
                    ),
                    feather = 0.012f
                ),
                "spine"
            )
            val cross = let(
                stroke(
                    distance = sdBox(
                        point = sigil,
                        halfSize = float2(0.21f, 0.05f)
                    ),
                    width = 0.05f
                ),
                "cross"
            )
            val mask = let(max(max(frame, ring), max(core, max(spine, cross))), "mask")
            val tint = let(
                color(
                    mix(0.06f, 0.18f, pulse),
                    mix(0.32f, 1f, ring + core * 0.25f),
                    mix(0.18f, 0.74f, frame + spine * 0.35f),
                    base.a
                ),
                "tint"
            )
            val screened = let(maskedScreen(base, tint, mask, amount), "screened")

            maskedOverlay(
                base = screened,
                blend = color(float3(0.90f, 1f, 0.80f), base.a),
                mask = core + ring * 0.5f,
                amount = amount * 0.26f
            )
        }
        SigilSetup(
            effect = effect,
            time = timeParam!!,
            speed = speedParam!!,
            amount = amountParam!!
        )
    }

    val fx = rememberFxController(setup.effect)
    fx.bindTime(setup.time, isPlaying = playing)
    fx.bindFloat(setup.speed, speedUi / 100f)
    fx.bindFloat(setup.amount, amountUi / 100f)

    DemoLayout(
        generatedAgsl = rememberGeneratedAgsl(setup.effect),
        preview = {
            DemoPreviewStage(
                modifier = Modifier.redbyteFx(fx),
                label = "Sigil//SDF"
            )
        },
        controls = {
            SwitchRow("Play", playing) {
                playing = it
            }
            SliderRow("Speed", speedUi, 20f..140f) {
                speedUi = it
            }
            SliderRow("Amount", amountUi, 0f..100f) {
                amountUi = it
            }
        }
    )
}

@Composable
fun DemoGlitch() {
    var playing by rememberSaveable { mutableStateOf(true) }
    var densityUi by rememberSaveable { mutableFloatStateOf(76f) }
    var warpUi by rememberSaveable { mutableFloatStateOf(30f) }
    var amountUi by rememberSaveable { mutableFloatStateOf(82f) }

    val setup = remember {
        var timeParam: FxParam.Float? = null
        var densityParam: FxParam.Float? = null
        var warpParam: FxParam.Float? = null
        var amountParam: FxParam.Float? = null
        val effect = redbytefx {
            val time by autoUniformTime()
            val density by autoUniformFloat(7.6f)
            val warp by autoUniformFloat(0.03f)
            val amount by autoUniformFloat(0.82f)
            timeParam = time
            densityParam = density
            warpParam = warp
            amountParam = amount

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
            val drifted = let(sampleUv(driftUv), "drifted")
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
            val lockCenter = let(0.22f + pingPong(time * 0.12f, 1f) * 0.56f, "lock_center")
            val lock = let(
                bandMask(
                    position = uv.y,
                    center = lockCenter,
                    width = 0.14f,
                    feather = 0.08f
                ),
                "lock"
            )
            val split = let(
                chromaticOffset(
                    offset = 9f + bars * 12f,
                    direction = float2(1f, 0.12f),
                    amount = amount * (bars * 0.7f + lock * 0.3f)
                ),
                "split"
            )
            val tint = let(color(float3(0.08f, 0.96f, 0.68f), base.a), "tint")
            val screened = let(
                maskedScreen(
                    base = drifted,
                    blend = split + tint * 0.14f,
                    mask = bars,
                    amount = amount
                ),
                "screened"
            )
            val locked = let(
                maskedOverlay(
                    base = screened,
                    blend = color(float3(0.52f, 1f, 0.24f), base.a),
                    mask = lock,
                    amount = amount * 0.45f
                ),
                "locked"
            )

            maskedMix(base, locked, max(bars, lock), amount)
        }
        GlitchSetup(
            effect = effect,
            time = timeParam!!,
            density = densityParam!!,
            warp = warpParam!!,
            amount = amountParam!!
        )
    }

    val fx = rememberFxController(setup.effect)
    fx.bindTime(setup.time, isPlaying = playing)
    fx.bindFloat(setup.density, densityUi / 10f)
    fx.bindFloat(setup.warp, warpUi / 1000f)
    fx.bindFloat(setup.amount, amountUi / 100f)

    DemoLayout(
        generatedAgsl = rememberGeneratedAgsl(setup.effect),
        preview = {
            DemoPreviewStage(
                modifier = Modifier.redbyteFx(fx),
                label = "Signal//Ghost"
            )
        },
        controls = {
            SwitchRow("Play", playing) {
                playing = it
            }
            SliderRow("Density", densityUi, 20f..140f) {
                densityUi = it
            }
            SliderRow("Warp", warpUi, 0f..80f) {
                warpUi = it
            }
            SliderRow("Amount", amountUi, 0f..100f) {
                amountUi = it
            }
        }
    )
}

@Composable
fun DemoDuotone() {
    var amount by rememberSaveable { mutableFloatStateOf(0.75f) }
    var warmth by rememberSaveable { mutableFloatStateOf(0.55f) }

    val setup = remember {
        var amountParam: FxParam.Float? = null
        var warmthParam: FxParam.Float? = null
        val effect = redbytefx {
            val amountUniform = uniformFloat(0.75f, "duotone_amount")
            val warmthUniform = uniformFloat(0.55f, "duotone_warmth")
            val palette = fn(
                name = "palette_rgb",
                arg1 = FloatType,
                arg2 = FloatType,
                returns = Float3Type
            ) { tone, warmth ->
                val phase = let(tone * 6.2831855f, "phase")
                float3(
                    0.24f + 0.45f * sin(phase + warmth * 0.90f + 0.10f),
                    0.30f + 0.42f * sin(phase + warmth * 1.50f + 2.10f),
                    0.42f + 0.36f * sin(phase + warmth * 2.10f + 4.20f)
                )
            }
            amountParam = amountUniform
            warmthParam = warmthUniform

            val base = let(sample(), "base")
            val luma = let(luminance(base), "luma")
            val mono = let(grayscale(base), "mono")
            val tintRgb = let(palette(luma, warmthUniform), "tint_rgb")
            val tintRgba = let(float4(tintRgb, base.a), "tint_rgba")
            val tone = let(color(tintRgba), "tone")
            val lifted = let(
                color(
                    mix(0.08f, tone.r, luma),
                    mix(0.06f, tone.g, luma),
                    mix(0.10f, tone.b, luma),
                    base.a
                ),
                "lifted"
            )

            mix(base, mix(mono, lifted, 0.85f), amountUniform)
        }
        DuotoneSetup(effect, amountParam!!, warmthParam!!)
    }

    val fx = rememberFxController(setup.effect)
    fx.bindFloat(setup.amount, amount)
    fx.bindFloat(setup.warmth, warmth)

    DemoLayout(
        generatedAgsl = rememberGeneratedAgsl(setup.effect),
        preview = {
            DemoPreviewStage(modifier = Modifier.redbyteFx(fx))
        },
        controls = {
            SliderRow(
                title = "Amount",
                value = amount * 100f,
                range = 0f..100f
            ) {
                amount = it / 100f
            }
            SliderRow(
                title = "Warmth",
                value = warmth * 100f,
                range = 0f..100f
            ) {
                warmth = it / 100f
            }
        }
    )
}
