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
import ru.redbyte.redbytefx.ShaderProgram
import ru.redbyte.redbytefx.compose.bindFloat
import ru.redbyte.redbytefx.compose.bindTime
import ru.redbyte.redbytefx.compose.redbyteFx
import ru.redbyte.redbytefx.compose.rememberFxController
import ru.redbyte.redbytefx.sample.ui.DemoLayout
import ru.redbyte.redbytefx.sample.ui.DemoPreviewStage
import ru.redbyte.redbytefx.sample.ui.SliderRow
import ru.redbyte.redbytefx.sample.ui.SwitchRow
import ru.redbyte.redbytefx.stdlib.sdCircle
import ru.redbyte.redbytefx.stdlib.smoothMin
import ru.redbyte.redbytefx.stdlib.softFill
import java.util.Locale

private data class MetaballsSetup(
    val effect: ShaderProgram,
    val time: Uniform<Flt<High>>,
    val blend: Uniform<Flt<High>>,
)

@Composable
fun DemoMetaballs() {
    var playing by rememberSaveable { mutableStateOf(true) }
    var blendUi by rememberSaveable { mutableFloatStateOf(10f) }

    val setup = remember {
        var timeParam: Uniform<Flt<High>>? = null
        var blendParam: Uniform<Flt<High>>? = null
        val effect = shader(ShaderTarget.Agsl) {
            val timeUniform = uniformTime(name = "meta_time")
            val blendK = uniform("meta_blend", 0.1f)
            timeParam = timeUniform
            blendParam = blendK
            fragment {
                val uv = let(fragCoord / resolution, "uv")
                val c1 = let(
                    float2(
                        0.35f + sin(timeUniform.expr * 0.7f) * 0.11f,
                        0.42f + cos(timeUniform.expr * 0.52f) * 0.09f,
                    ),
                    "c1",
                )
                val c2 = let(
                    float2(
                        0.64f + cos(timeUniform.expr * 0.58f) * 0.1f,
                        0.54f + sin(timeUniform.expr * 0.63f) * 0.08f,
                    ),
                    "c2",
                )
                val c3 = let(
                    float2(
                        0.48f + sin(timeUniform.expr * 0.33f) * 0.13f,
                        0.74f + cos(timeUniform.expr * 0.41f) * 0.07f,
                    ),
                    "c3",
                )
                val r = 0.11f
                val d1 = let(sdCircle(uv - c1, r), "d1")
                val d2 = let(sdCircle(uv - c2, r * 1.05f), "d2")
                val d3 = let(sdCircle(uv - c3, r * 0.95f), "d3")
                val m12 = let(smoothMin(d1, d2, 0.085f), "m12")
                val field = let(smoothMin(m12, d3, blendK.expr), "field")
                val blob = softFill(field, feather = 0.035f)
                val bg = color(float3(0.03f, 0.04f, 0.07f), 1f)
                val fill = color(float3(0.15f, 0.95f, 0.82f), 1f)
                val rim = color(float3(0.95f, 0.35f, 0.85f), 1f)
                val shaded = mix(fill, rim, saturate(blob * 1.15f - 0.35f))
                mix(bg, shaded, blob)
            }
        }
        MetaballsSetup(effect, timeParam!!, blendParam!!)
    }

    val fx = rememberFxController(setup.effect)
    fx.bindTime(setup.time, isPlaying = playing)
    fx.bindFloat(setup.blend, blendUi / 100f)

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
                title = "Blend width",
                value = blendUi,
                range = 4f..22f,
                formatValue = { v -> String.format(Locale.US, "%.2f", v / 100f) },
            ) {
                blendUi = it
            }
        },
    )
}
