package ru.redbyte.redbytefx.sample.ui.demos

import androidx.compose.foundation.layout.size
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
import ru.redbyte.redbytefx.stdlib.cornerMask
import ru.redbyte.redbytefx.stdlib.directionalSweep
import ru.redbyte.redbytefx.stdlib.maskedScreen
import ru.redbyte.redbytefx.stdlib.pingPong

private data class CornerSetup(
    val effect: ru.redbyte.redbytefx.ShaderProgram,
    val time: Uniform<Flt<High>>,
    val size: Uniform<Flt<High>>,
    val thickness: Uniform<Flt<High>>,
    val amount: Uniform<Flt<High>>,
)

@Composable
fun DemoCorner() {
    var playing by rememberSaveable { mutableStateOf(true) }
    var sizeUi by rememberSaveable { mutableFloatStateOf(22f) }
    var thicknessUi by rememberSaveable { mutableFloatStateOf(8f) }
    var amountUi by rememberSaveable { mutableFloatStateOf(84f) }

    val setup = remember {
        var timeParam: Uniform<Flt<High>>? = null
        var sizeParam: Uniform<Flt<High>>? = null
        var thicknessParam: Uniform<Flt<High>>? = null
        var amountParam: Uniform<Flt<High>>? = null
        val effect = shader(ShaderTarget.Agsl) {
            val time = uniformTime(name = "time")
            val size = uniform("size", 0.22f)
            val thickness = uniform("thickness", 0.08f)
            val amount = uniform("amount", 0.84f)
            timeParam = time
            sizeParam = size
            thicknessParam = thickness
            amountParam = amount
            fragment {
                val base = let(sample(), "base")
                val uv = let(fragCoord / resolution, "uv")
                val corners = let(cornerMask(uv, size = size.expr, thickness = thickness.expr, feather = 0.03f), "corners")
                val sweepCenter = let(0.18f + pingPong(time.expr * 0.18f, 1f) * 0.64f, "sweep_center")
                val sweep = let(
                    directionalSweep(
                        uv = uv,
                        direction = float2(1f, -0.2f),
                        center = sweepCenter,
                        width = 0.16f,
                        feather = 0.08f,
                    ),
                    "sweep",
                )
                val accent = let(color(float3(0.1f, 0.98f, 0.68f), base.a), "accent")
                maskedScreen(base, accent, corners * sweep, amount.expr)
            }
        }
        CornerSetup(
            effect = effect,
            time = timeParam!!,
            size = sizeParam!!,
            thickness = thicknessParam!!,
            amount = amountParam!!,
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
                label = "Corner//HUD",
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
        },
    )
}
