package ru.redbyte.redbytefx.sample.ui.demos

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import ru.redbyte.redbytefx.*
import ru.redbyte.redbytefx.compose.bindFloat
import ru.redbyte.redbytefx.compose.redbyteFx
import ru.redbyte.redbytefx.compose.rememberFxController
import ru.redbyte.redbytefx.sample.ui.DemoLayout
import ru.redbyte.redbytefx.sample.ui.DemoPreviewStage
import ru.redbyte.redbytefx.sample.ui.SliderRow
import ru.redbyte.redbytefx.stdlib.posterize

private data class PosterizeSetup(
    val effect: ru.redbyte.redbytefx.ShaderProgram,
    val levels: Uniform<Flt<High>>,
    val amount: Uniform<Flt<High>>,
)

@Composable
fun DemoPosterize() {
    var levelsUi by rememberSaveable { mutableFloatStateOf(5f) }
    var amountUi by rememberSaveable { mutableFloatStateOf(85f) }

    val setup = remember {
        var levelsParam: Uniform<Flt<High>>? = null
        var amountParam: Uniform<Flt<High>>? = null
        val effect = shader(ShaderTarget.Agsl) {
            val levels = uniform("levels", 5f)
            val amount = uniform("amount", 0.85f)
            levelsParam = levels
            amountParam = amount
            fragment {
                val base = let(sample(), "base")
                val quantized = let(posterize(base, levels.expr), "quantized")
                mix(base, quantized, amount.expr)
            }
        }
        PosterizeSetup(effect, levelsParam!!, amountParam!!)
    }

    val fx = rememberFxController(setup.effect)
    fx.bindFloat(setup.levels, levelsUi)
    fx.bindFloat(setup.amount, amountUi / 100f)

    DemoLayout(
        generatedAgsl = rememberGeneratedAgsl(setup.effect),
        preview = {
            DemoPreviewStage(modifier = Modifier.redbyteFx(fx))
        },
        controls = {
            SliderRow("Levels", levelsUi, 2f..12f) {
                levelsUi = it
            }
            SliderRow("Amount", amountUi, 0f..100f) {
                amountUi = it
            }
        },
    )
}
