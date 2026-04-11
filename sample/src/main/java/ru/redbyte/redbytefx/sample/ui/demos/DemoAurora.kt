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
import ru.redbyte.redbytefx.stdlib.angularSweep
import ru.redbyte.redbytefx.stdlib.blendScreen
import ru.redbyte.redbytefx.stdlib.chromaticOffset
import ru.redbyte.redbytefx.stdlib.cosinePalette
import ru.redbyte.redbytefx.stdlib.maskedMix
import ru.redbyte.redbytefx.stdlib.rimLight

import ru.redbyte.redbytefx.sample.ui.DemoLayout
import ru.redbyte.redbytefx.sample.ui.DemoPreviewStage
import ru.redbyte.redbytefx.sample.ui.SliderRow
import ru.redbyte.redbytefx.sample.ui.SwitchRow


private data class AuroraSetup(
    val effect: ru.redbyte.redbytefx.FxEffect,
    val time: FxParam.Float,
    val amount: FxParam.Float,
    val chromaPx: FxParam.Float,
    val spectral: FxParam.Float,
    val speed: FxParam.Float
)

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
