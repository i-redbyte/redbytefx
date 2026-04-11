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
import ru.redbyte.redbytefx.stdlib.blendScreen
import ru.redbyte.redbytefx.stdlib.chromaticOffset
import ru.redbyte.redbytefx.stdlib.cosinePalette

import ru.redbyte.redbytefx.sample.ui.DemoLayout
import ru.redbyte.redbytefx.sample.ui.DemoPreviewStage
import ru.redbyte.redbytefx.sample.ui.SliderRow


private data class PrismSetup(
    val effect: ru.redbyte.redbytefx.FxEffect,
    val amount: FxParam.Float,
    val spread: FxParam.Float,
    val shift: FxParam.Float
)

@Composable
fun DemoPrism() {
    var amountUi by rememberSaveable { mutableFloatStateOf(76f) }
    var spreadUi by rememberSaveable { mutableFloatStateOf(22f) }
    var shiftUi by rememberSaveable { mutableFloatStateOf(10f) }

    val setup = remember {
        var amountParam: FxParam.Float? = null
        var spreadParam: FxParam.Float? = null
        var shiftParam: FxParam.Float? = null
        val effect = redbytefx {
            val amount by autoUniformFloat(0.76f)
            val spread by autoUniformFloat(0.22f)
            val shift by autoUniformFloat(10f)
            amountParam = amount
            spreadParam = spread
            shiftParam = shift

            val base = let(sample(), "base")
            val uv = let(fragCoord / resolution, "uv")
            val palette = let(cosinePalette(luminance(base) + uv.x * spread), "palette")
            val tint = let(color(palette, base.a), "tint")
            val refracted = let(
                chromaticOffset(
                    offset = shift,
                    direction = float2(1f, 0.3f),
                    amount = amount
                ),
                "refracted"
            )

            blendScreen(refracted, tint, amount)
        }
        PrismSetup(
            effect = effect,
            amount = amountParam!!,
            spread = spreadParam!!,
            shift = shiftParam!!
        )
    }

    val fx = rememberFxController(setup.effect)
    fx.bindFloat(setup.amount, amountUi / 100f)
    fx.bindFloat(setup.spread, spreadUi / 100f)
    fx.bindFloat(setup.shift, shiftUi)

    DemoLayout(
        generatedAgsl = rememberGeneratedAgsl(setup.effect),
        preview = {
            DemoPreviewStage(modifier = Modifier.redbyteFx(fx))
        },
        controls = {
            SliderRow("Amount", amountUi, 0f..100f) {
                amountUi = it
            }
            SliderRow("Spread", spreadUi, 0f..60f) {
                spreadUi = it
            }
            SliderRow("Shift", shiftUi, 0f..24f) {
                shiftUi = it
            }
        }
    )
}
