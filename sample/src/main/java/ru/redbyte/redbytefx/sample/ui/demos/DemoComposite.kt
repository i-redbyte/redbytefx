package ru.redbyte.redbytefx.sample.ui.demos

import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import ru.redbyte.redbytefx.*
import ru.redbyte.redbytefx.compose.bindFloat
import ru.redbyte.redbytefx.compose.redbyteFx
import ru.redbyte.redbytefx.compose.rememberFxController
import ru.redbyte.redbytefx.stdlib.circleMask
import ru.redbyte.redbytefx.stdlib.maskedMix
import ru.redbyte.redbytefx.stdlib.maskedOverlay
import ru.redbyte.redbytefx.stdlib.maskedScreen
import ru.redbyte.redbytefx.stdlib.rectMask
import ru.redbyte.redbytefx.stdlib.ringMask
import ru.redbyte.redbytefx.stdlib.alphaMask

import ru.redbyte.redbytefx.sample.ui.DemoLayout
import ru.redbyte.redbytefx.sample.ui.DemoPreviewStage
import ru.redbyte.redbytefx.sample.ui.SliderRow


private data class CompositeSetup(
    val effect: ru.redbyte.redbytefx.ShaderProgram,
    val radius: Uniform<Flt<High>>,
    val panelWidth: Uniform<Flt<High>>,
    val amount: Uniform<Flt<High>>
)

@Composable
fun DemoComposite() {
    var radiusUi by rememberSaveable { mutableFloatStateOf(20f) }
    var panelWidthUi by rememberSaveable { mutableFloatStateOf(34f) }
    var amountUi by rememberSaveable { mutableFloatStateOf(82f) }

    val setup = remember {
        var radiusParam: Uniform<Flt<High>>? = null
        var panelWidthParam: Uniform<Flt<High>>? = null
        var amountParam: Uniform<Flt<High>>? = null
        val effect = shader(ShaderTarget.Agsl) {
            val radius = uniform("radius", 0.2f)
            val panelWidth = uniform("panelWidth", 0.34f)
            val amount = uniform("amount", 0.82f)
            radiusParam = radius
            panelWidthParam = panelWidth
            amountParam = amount
            fragment {
                val base = let(sample(), "base")
                val uv = let(fragCoord / resolution, "uv")
                val focus = let(
                    circleMask(
                        uv,
                        center = float2(0.34f, 0.5f),
                        radius = radius.expr,
                        feather = 0.16f
                    ),
                    "focus"
                )
                val halo = let(
                    ringMask(
                        uv,
                        center = float2(0.34f, 0.5f),
                        radius = radius.expr + 0.05f,
                        width = 0.1f,
                        feather = 0.05f
                    ),
                    "halo"
                )
                val panel = let(
                    rectMask(
                        uv,
                        center = float2(0.77f, 0.5f),
                        size = float2(panelWidth.expr, 0.62f),
                        feather = 0.04f
                    ),
                    "panel"
                )
                val glowLayer = let(
                    alphaMask(color(float3(0.12f, 0.95f, 1f), 1f), halo, amount.expr),
                    "glow_layer"
                )
                val panelTint = let(
                    alphaMask(color(float3(1f, 0.79f, 0.3f), 1f), panel, amount.expr * 0.6f),
                    "panel_tint"
                )
                val screened = let(maskedScreen(base, glowLayer, halo, amount.expr), "screened")
                val overlaid = let(maskedOverlay(screened, panelTint, panel, amount.expr), "overlaid")
                val focusTint = let(color(float3(0.92f, 0.98f, 1f), base.a), "focus_tint")
                maskedMix(base, maskedMix(overlaid, focusTint, focus, amount.expr * 0.45f), focus + halo * 0.2f, amount.expr)
            }
        }
        CompositeSetup(
            effect = effect,
            radius = radiusParam!!,
            panelWidth = panelWidthParam!!,
            amount = amountParam!!
        )
    }

    val fx = rememberFxController(setup.effect)
    fx.bindFloat(setup.radius, radiusUi / 100f)
    fx.bindFloat(setup.panelWidth, panelWidthUi / 100f)
    fx.bindFloat(setup.amount, amountUi / 100f)

    DemoLayout(
        generatedAgsl = rememberGeneratedAgsl(setup.effect),
        preview = {
            DemoPreviewStage(modifier = Modifier.redbyteFx(fx))
        },
        controls = {
            SliderRow("Radius", radiusUi, 10f..32f) {
                radiusUi = it
            }
            SliderRow("Panel Width", panelWidthUi, 20f..50f) {
                panelWidthUi = it
            }
            SliderRow("Amount", amountUi, 0f..100f) {
                amountUi = it
            }
        }
    )
}
