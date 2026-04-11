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
import ru.redbyte.redbytefx.stdlib.posterize

import ru.redbyte.redbytefx.sample.ui.DemoLayout
import ru.redbyte.redbytefx.sample.ui.DemoPreviewStage
import ru.redbyte.redbytefx.sample.ui.SliderRow


private data class PosterizeSetup(
    val effect: ru.redbyte.redbytefx.FxEffect,
    val levels: FxParam.Float,
    val amount: FxParam.Float
)

@Composable
fun DemoPosterize() {
    var levelsUi by rememberSaveable { mutableFloatStateOf(5f) }
    var amountUi by rememberSaveable { mutableFloatStateOf(85f) }

    val setup = remember {
        var levelsParam: FxParam.Float? = null
        var amountParam: FxParam.Float? = null
        val effect = redbytefx {
            val levels by autoUniformFloat(5f)
            val amount by autoUniformFloat(0.85f)
            levelsParam = levels
            amountParam = amount

            val base = let(sample(), "base")
            val quantized = let(posterize(base, levels), "quantized")
            mix(base, quantized, amount)
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
        }
    )
}
