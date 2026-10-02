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
import ru.redbyte.redbytefx.stdlib.grain
import ru.redbyte.redbytefx.stdlib.normalizedUv
import ru.redbyte.redbytefx.stdlib.remap
import ru.redbyte.redbytefx.stdlib.valueNoise
import ru.redbyte.redbytefx.stdlib.vignette

import ru.redbyte.redbytefx.sample.ui.DemoLayout
import ru.redbyte.redbytefx.sample.ui.DemoPreviewStage
import ru.redbyte.redbytefx.sample.ui.SliderRow
import ru.redbyte.redbytefx.sample.ui.SwitchRow


private data class FilmSetup(
    val effect: ru.redbyte.redbytefx.ShaderProgram,
    val time: Uniform<Flt<High>>,
    val grainAmount: Uniform<Flt<High>>,
    val grainScale: Uniform<Flt<High>>,
    val vignetteAmount: Uniform<Flt<High>>
)

@Composable
fun DemoFilm() {
    var playing by rememberSaveable { mutableStateOf(true) }
    var grainAmountUi by rememberSaveable { mutableFloatStateOf(18f) }
    var grainScaleUi by rememberSaveable { mutableFloatStateOf(190f) }
    var vignetteAmountUi by rememberSaveable { mutableFloatStateOf(55f) }

    val setup = remember {
        var timeParam: Uniform<Flt<High>>? = null
        var grainAmountParam: Uniform<Flt<High>>? = null
        var grainScaleParam: Uniform<Flt<High>>? = null
        var vignetteAmountParam: Uniform<Flt<High>>? = null
        val effect = shader(ShaderTarget.Agsl) {
            val time = uniformTime(name = "time")
            val grainAmount = uniform("grainAmount", 0.18f)
            val grainScale = uniform("grainScale", 190f)
            val vignetteAmount = uniform("vignetteAmount", 0.55f)
            timeParam = time
            grainAmountParam = grainAmount
            grainScaleParam = grainScale
            vignetteAmountParam = vignetteAmount
            fragment {
                val base = let(sample(), "base")
                val uv = let(normalizedUv(), "uv")
                val noise = let(grain(uv, time.expr, grainScale.expr), "noise")
                val drift = let(
                    remap(
                        valueNoise(uv * 6f + float2(time.expr * 0.08f, 0f)),
                        0f,
                        1f,
                        0.92f,
                        1.05f
                    ),
                    "drift"
                )
                val mask = let(mix(1f, vignette(uv, 0.35f, 1.05f), vignetteAmount.expr), "mask")
                val lift = let(noise * grainAmount.expr, "lift")
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
