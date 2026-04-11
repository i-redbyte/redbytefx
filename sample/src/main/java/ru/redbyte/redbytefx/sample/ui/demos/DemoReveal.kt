package ru.redbyte.redbytefx.sample.ui.demos

import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.redbyte.redbytefx.*
import ru.redbyte.redbytefx.compose.bindFloat
import ru.redbyte.redbytefx.compose.bindTime
import ru.redbyte.redbytefx.compose.redbyteFx
import ru.redbyte.redbytefx.compose.rememberFxController
import ru.redbyte.redbytefx.stdlib.blendScreen
import ru.redbyte.redbytefx.stdlib.easeInOutSine
import ru.redbyte.redbytefx.stdlib.horizontalReveal
import ru.redbyte.redbytefx.stdlib.maskedMix
import ru.redbyte.redbytefx.stdlib.pingPong
import ru.redbyte.redbytefx.stdlib.posterize
import ru.redbyte.redbytefx.stdlib.radialReveal
import ru.redbyte.redbytefx.stdlib.verticalReveal

import ru.redbyte.redbytefx.sample.ui.DemoLayout
import ru.redbyte.redbytefx.sample.ui.DemoPreviewStage
import ru.redbyte.redbytefx.sample.ui.RadioRow
import ru.redbyte.redbytefx.sample.ui.SliderRow
import ru.redbyte.redbytefx.sample.ui.SwitchRow

private data class RevealSetup(
    val effect: ru.redbyte.redbytefx.FxEffect,
    val time: FxParam.Float,
    val speed: FxParam.Float,
    val mode: FxParam.Float,
    val amount: FxParam.Float
)

private enum class RevealMode { Horizontal, Vertical, Radial }

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
