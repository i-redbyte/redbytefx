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
import ru.redbyte.redbytefx.stdlib.domainWarp
import ru.redbyte.redbytefx.stdlib.fbm

import ru.redbyte.redbytefx.sample.ui.DemoLayout
import ru.redbyte.redbytefx.sample.ui.DemoPreviewStage
import ru.redbyte.redbytefx.sample.ui.SliderRow
import ru.redbyte.redbytefx.sample.ui.SwitchRow


private data class WarpSetup(
    val effect: ru.redbyte.redbytefx.FxEffect,
    val time: FxParam.Float,
    val warpAmount: FxParam.Float,
    val scale: FxParam.Float,
    val driftAmount: FxParam.Float
)

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
