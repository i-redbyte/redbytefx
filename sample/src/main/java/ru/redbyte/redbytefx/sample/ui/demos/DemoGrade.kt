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
import ru.redbyte.redbytefx.stdlib.adjustSaturation
import ru.redbyte.redbytefx.stdlib.blendMultiply
import ru.redbyte.redbytefx.stdlib.blendOverlay
import ru.redbyte.redbytefx.stdlib.blendScreen
import ru.redbyte.redbytefx.stdlib.remap

private data class GradeSetup(
    val effect: ru.redbyte.redbytefx.ShaderProgram,
    val amount: Uniform<Flt<High>>,
    val warmth: Uniform<Flt<High>>,
    val glow: Uniform<Flt<High>>,
)

@Composable
fun DemoGrade() {
    var amountUi by rememberSaveable { mutableFloatStateOf(82f) }
    var warmthUi by rememberSaveable { mutableFloatStateOf(58f) }
    var glowUi by rememberSaveable { mutableFloatStateOf(38f) }

    val setup = remember {
        var amountParam: Uniform<Flt<High>>? = null
        var warmthParam: Uniform<Flt<High>>? = null
        var glowParam: Uniform<Flt<High>>? = null
        val effect = shader(ShaderTarget.Agsl) {
            val amount = uniform("amount", 0.82f)
            val warmth = uniform("warmth", 0.58f)
            val glow = uniform("glow", 0.38f)
            amountParam = amount
            warmthParam = warmth
            glowParam = glow
            fragment {
                val base = let(sample(), "base")
                val grade = remap(amount.expr, 0f, 1f, 0.55f, 1.55f)
                val saturated = let(
                    adjustSaturation(base, grade),
                    "saturated",
                )
                val tint = let(
                    color(
                        mix(0.26f, 0.94f, warmth.expr),
                        mix(0.48f, 0.72f, warmth.expr),
                        mix(0.92f, 0.38f, warmth.expr),
                        base.a,
                    ),
                    "tint",
                )
                val multiplied = let(blendMultiply(saturated, tint, 0.25f), "multiplied")
                val screened = let(blendScreen(multiplied, tint, glow.expr), "screened")
                blendOverlay(base, screened, amount.expr)
            }
        }
        GradeSetup(
            effect = effect,
            amount = amountParam!!,
            warmth = warmthParam!!,
            glow = glowParam!!,
        )
    }

    val fx = rememberFxController(setup.effect)
    fx.bindFloat(setup.amount, amountUi / 100f)
    fx.bindFloat(setup.warmth, warmthUi / 100f)
    fx.bindFloat(setup.glow, glowUi / 100f)

    DemoLayout(
        generatedAgsl = rememberGeneratedAgsl(setup.effect),
        preview = {
            DemoPreviewStage(modifier = Modifier.redbyteFx(fx))
        },
        controls = {
            SliderRow("Amount", amountUi, 0f..100f) {
                amountUi = it
            }
            SliderRow("Warmth", warmthUi, 0f..100f) {
                warmthUi = it
            }
            SliderRow("Glow", glowUi, 0f..100f) {
                glowUi = it
            }
        },
    )
}
