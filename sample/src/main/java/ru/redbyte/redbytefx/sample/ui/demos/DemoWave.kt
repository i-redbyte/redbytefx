package ru.redbyte.redbytefx.sample.ui.demos

import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import ru.redbyte.redbytefx.*
import ru.redbyte.redbytefx.compose.bindFloat
import ru.redbyte.redbytefx.compose.redbyteFx
import ru.redbyte.redbytefx.compose.rememberFxController

import ru.redbyte.redbytefx.sample.ui.DemoLayout
import ru.redbyte.redbytefx.sample.ui.DemoPreviewStage
import ru.redbyte.redbytefx.sample.ui.SliderRow

private data class WaveSetup(
    val effect: ru.redbyte.redbytefx.ShaderProgram,
    val amplitude: Uniform<Flt<High>>,
    val frequency: Uniform<Flt<High>>
)


@Composable
fun DemoWave() {
    var amplitude by rememberSaveable { mutableFloatStateOf(0f) }
    var frequencyUi by rememberSaveable { mutableFloatStateOf(8f) }

    val setup = remember {
        var amplitudeParam: Uniform<Flt<High>>? = null
        var frequencyParam: Uniform<Flt<High>>? = null
        val effect = shader(ShaderTarget.Agsl) {
            val amplitudeUniform = uniform("wave_amplitude", 0f)
            val frequencyUniform = uniform("wave_frequency", 0.08f)
            amplitudeParam = amplitudeUniform
            frequencyParam = frequencyUniform
            fragment {
                val x = let(fragCoord.x, "x")
                val waveOffset = let(float2(
                    0f,
                    sin(x * frequencyUniform.expr) * amplitudeUniform.expr
                ), "wave_offset")
                sample(fragCoord + waveOffset)
            }
        }
        WaveSetup(effect, amplitudeParam!!, frequencyParam!!)
    }

    val fx = rememberFxController(setup.effect)
    fx.bindFloat(setup.amplitude, amplitude)
    fx.bindFloat(setup.frequency, frequencyUi / 100f)

    DemoLayout(
        generatedAgsl = rememberGeneratedAgsl(setup.effect),
        preview = {
            DemoPreviewStage(modifier = Modifier.redbyteFx(fx))
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
