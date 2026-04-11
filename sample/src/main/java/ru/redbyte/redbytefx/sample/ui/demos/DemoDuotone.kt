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


private data class DuotoneSetup(
    val effect: ru.redbyte.redbytefx.FxEffect,
    val amount: FxParam.Float,
    val warmth: FxParam.Float
)

@Composable
fun DemoDuotone() {
    var amount by rememberSaveable { mutableFloatStateOf(0.75f) }
    var warmth by rememberSaveable { mutableFloatStateOf(0.55f) }

    val setup = remember {
        var amountParam: FxParam.Float? = null
        var warmthParam: FxParam.Float? = null
        val effect = redbytefx {
            val amountUniform = uniformFloat(0.75f, "duotone_amount")
            val warmthUniform = uniformFloat(0.55f, "duotone_warmth")
            val palette = fn(
                name = "palette_rgb",
                arg1 = FloatType,
                arg2 = FloatType,
                returns = Float3Type
            ) { tone, warmth ->
                val phase = let(tone * 6.2831855f, "phase")
                float3(
                    0.24f + 0.45f * sin(phase + warmth * 0.90f + 0.10f),
                    0.30f + 0.42f * sin(phase + warmth * 1.50f + 2.10f),
                    0.42f + 0.36f * sin(phase + warmth * 2.10f + 4.20f)
                )
            }
            amountParam = amountUniform
            warmthParam = warmthUniform

            val base = let(sample(), "base")
            val luma = let(luminance(base), "luma")
            val mono = let(grayscale(base), "mono")
            val tintRgb = let(palette(luma, warmthUniform), "tint_rgb")
            val tintRgba = let(float4(tintRgb, base.a), "tint_rgba")
            val tone = let(color(tintRgba), "tone")
            val lifted = let(
                color(
                    mix(0.08f, tone.r, luma),
                    mix(0.06f, tone.g, luma),
                    mix(0.10f, tone.b, luma),
                    base.a
                ),
                "lifted"
            )

            mix(base, mix(mono, lifted, 0.85f), amountUniform)
        }
        DuotoneSetup(effect, amountParam!!, warmthParam!!)
    }

    val fx = rememberFxController(setup.effect)
    fx.bindFloat(setup.amount, amount)
    fx.bindFloat(setup.warmth, warmth)

    DemoLayout(
        generatedAgsl = rememberGeneratedAgsl(setup.effect),
        preview = {
            DemoPreviewStage(modifier = Modifier.redbyteFx(fx))
        },
        controls = {
            SliderRow(
                title = "Amount",
                value = amount * 100f,
                range = 0f..100f
            ) {
                amount = it / 100f
            }
            SliderRow(
                title = "Warmth",
                value = warmth * 100f,
                range = 0f..100f
            ) {
                warmth = it / 100f
            }
        }
    )
}
