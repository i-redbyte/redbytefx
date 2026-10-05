package ru.redbyte.redbytefx.sample.ui.demos

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import ru.redbyte.redbytefx.*
import ru.redbyte.redbytefx.compose.bindFloat
import ru.redbyte.redbytefx.compose.bindTime
import ru.redbyte.redbytefx.compose.redbyteFx
import ru.redbyte.redbytefx.compose.rememberFxController
import ru.redbyte.redbytefx.sample.ui.DemoLayout
import ru.redbyte.redbytefx.sample.ui.DemoPreviewStage
import ru.redbyte.redbytefx.sample.ui.SliderRow
import ru.redbyte.redbytefx.sample.ui.SwitchRow
import ru.redbyte.redbytefx.stdlib.directionalSweep
import ru.redbyte.redbytefx.stdlib.linearRamp
import ru.redbyte.redbytefx.stdlib.maskedOverlay
import ru.redbyte.redbytefx.stdlib.maskedScreen
import ru.redbyte.redbytefx.stdlib.pingPong
import ru.redbyte.redbytefx.stdlib.radialRamp

private data class SweepSetup(
    val effect: ru.redbyte.redbytefx.ShaderProgram,
    val time: Uniform<Flt<High>>,
    val speed: Uniform<Flt<High>>,
    val width: Uniform<Flt<High>>,
    val amount: Uniform<Flt<High>>,
)

@Composable
fun DemoSweep() {
    var playing by rememberSaveable { mutableStateOf(true) }
    var speedUi by rememberSaveable { mutableFloatStateOf(74f) }
    var widthUi by rememberSaveable { mutableFloatStateOf(22f) }
    var amountUi by rememberSaveable { mutableFloatStateOf(84f) }

    val setup = remember {
        var timeParam: Uniform<Flt<High>>? = null
        var speedParam: Uniform<Flt<High>>? = null
        var widthParam: Uniform<Flt<High>>? = null
        var amountParam: Uniform<Flt<High>>? = null
        val effect = shader(ShaderTarget.Agsl) {
            val time = uniformTime(name = "time")
            val speed = uniform("speed", 0.74f)
            val width = uniform("width", 0.22f)
            val amount = uniform("amount", 0.84f)
            timeParam = time
            speedParam = speed
            widthParam = width
            amountParam = amount
            fragment {
                val base = let(sample(), "base")
                val uv = let(fragCoord / resolution, "uv")
                val center = let(0.16f + pingPong(time.expr * speed.expr * 0.22f, 1f) * 0.68f, "center")
                val ramp = let(linearRamp(uv, direction = float2(1f, -0.35f), start = 0.08f, end = 0.92f), "ramp")
                val sweep = let(
                    directionalSweep(
                        uv = uv,
                        direction = float2(1f, -0.35f),
                        center = center,
                        width = width.expr,
                        feather = 0.08f,
                    ),
                    "sweep",
                )
                val radial = let(radialRamp(uv, innerRadius = 0.12f, outerRadius = 0.68f), "radial")
                val tint = let(
                    color(
                        mix(0.06f, 0.28f, ramp),
                        mix(0.24f, 1f, sweep),
                        mix(0.18f, 0.62f, ramp + sweep * 0.4f),
                        base.a,
                    ),
                    "tint",
                )
                val screened = let(maskedScreen(base, tint, sweep * radial, amount.expr), "screened")
                maskedOverlay(
                    base = screened,
                    blend = color(float3(0.92f, 1f, 0.78f), base.a),
                    mask = sweep,
                    amount = amount.expr * 0.34f,
                )
            }
        }
        SweepSetup(
            effect = effect,
            time = timeParam!!,
            speed = speedParam!!,
            width = widthParam!!,
            amount = amountParam!!,
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
                label = "Sweep//Track",
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
        },
    )
}
