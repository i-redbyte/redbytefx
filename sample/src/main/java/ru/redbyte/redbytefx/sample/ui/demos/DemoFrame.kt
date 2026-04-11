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
import ru.redbyte.redbytefx.stdlib.maskedOverlay
import ru.redbyte.redbytefx.stdlib.maskedScreen
import ru.redbyte.redbytefx.stdlib.pingPong
import ru.redbyte.redbytefx.stdlib.directionalSweep
import ru.redbyte.redbytefx.stdlib.edgeFade
import ru.redbyte.redbytefx.stdlib.frameMask

import ru.redbyte.redbytefx.sample.ui.DemoLayout
import ru.redbyte.redbytefx.sample.ui.DemoPreviewStage
import ru.redbyte.redbytefx.sample.ui.SliderRow
import ru.redbyte.redbytefx.sample.ui.SwitchRow


private data class FrameSetup(
    val effect: ru.redbyte.redbytefx.FxEffect,
    val time: FxParam.Float,
    val speed: FxParam.Float,
    val thickness: FxParam.Float,
    val amount: FxParam.Float
)

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
