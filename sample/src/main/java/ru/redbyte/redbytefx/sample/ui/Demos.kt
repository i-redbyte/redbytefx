package ru.redbyte.redbytefx.sample.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.redbyte.redbytefx.*
import ru.redbyte.redbytefx.compose.bindFloat
import ru.redbyte.redbytefx.compose.bindFloat2
import ru.redbyte.redbytefx.compose.bindTime
import ru.redbyte.redbytefx.compose.redbyteFx
import ru.redbyte.redbytefx.compose.rememberFxController

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

private data class DuotoneSetup(
    val effect: ru.redbyte.redbytefx.FxEffect,
    val amount: FxParam.Float,
    val warmth: FxParam.Float
)

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
            Text(
                text = "RedByteFX",
                style = MaterialTheme.typography.displayLarge,
                modifier = Modifier.redbyteFx(fx)
            )
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
            Text(
                text = "RedByteFX",
                style = MaterialTheme.typography.displayLarge,
                modifier = Modifier.redbyteFx(fx)
            )
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
            Text(
                text = "RedByteFX",
                style = MaterialTheme.typography.displayLarge,
                modifier = Modifier.redbyteFx(fx)
            )
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
            Text(
                text = "RedByteFX",
                style = MaterialTheme.typography.displayLarge,
                modifier = Modifier.redbyteFx(fx)
            )
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
            Text(
                text = "RedByteFX",
                style = MaterialTheme.typography.displayLarge,
                modifier = Modifier.redbyteFx(fx)
            )
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
            Text(
                text = "RedByteFX",
                style = MaterialTheme.typography.displayLarge,
                modifier = Modifier.redbyteFx(fx)
            )
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
            val uv = let(fragCoord / resolution, "uv")
            val safeGrid = let(max(gridUniform, 1f), "safe_grid")
            val pixelUv = let(floor(uv * safeGrid) / safeGrid, "pixel_uv")
            val pixelBase = let(sample(pixelUv * resolution), "pixel_base")
            val row = let(floor(uv.y * safeGrid), "row")
            val wave = let(
                0.5f + 0.5f * sin(timeUniform * speedUniform + row * 0.7f),
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
            Text(
                text = "RedByteFX",
                style = MaterialTheme.typography.displayLarge,
                modifier = Modifier.redbyteFx(fx)
            )
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
            val uv = let(fragCoord / resolution, "uv")
            val cell = let(fract(uv * densityUniform), "cell")
            val edgeX = let(min(cell.x, 1f - cell.x), "edge_x")
            val edgeY = let(min(cell.y, 1f - cell.y), "edge_y")
            val grid = let(
                max(
                    1f - smoothstep(0f, lineWidthUniform, edgeX),
                    1f - smoothstep(0f, lineWidthUniform, edgeY)
                ),
                "grid"
            )
            val scan = let(
                1f - smoothstep(0f, 3f, mod(fragCoord.y, 14f)),
                "scan"
            )
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
            Text(
                text = "RedByteFX",
                style = MaterialTheme.typography.displayLarge,
                modifier = Modifier.redbyteFx(fx)
            )
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
            Text(
                text = "RedByteFX",
                style = MaterialTheme.typography.displayLarge,
                modifier = Modifier.redbyteFx(fx)
            )
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
