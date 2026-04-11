package ru.redbyte.redbytefx.sample.ui.demos

import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import ru.redbyte.redbytefx.*
import ru.redbyte.redbytefx.compose.bindFloat
import ru.redbyte.redbytefx.compose.bindTime
import ru.redbyte.redbytefx.compose.redbyteFx
import ru.redbyte.redbytefx.compose.rememberFxController
import ru.redbyte.redbytefx.stdlib.normalizedUv
import ru.redbyte.redbytefx.stdlib.pulse
import ru.redbyte.redbytefx.stdlib.sampleUv

import ru.redbyte.redbytefx.sample.ui.DemoLayout
import ru.redbyte.redbytefx.sample.ui.DemoPreviewStage
import ru.redbyte.redbytefx.sample.ui.SliderRow
import ru.redbyte.redbytefx.sample.ui.SwitchRow

private data class PulseSetup(
    val effect: ru.redbyte.redbytefx.FxEffect,
    val time: FxParam.Float,
    val speed: FxParam.Float,
    val grid: FxParam.Float,
    val amount: FxParam.Float
)

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
