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
import ru.redbyte.redbytefx.compose.bindFloat2
import ru.redbyte.redbytefx.compose.redbyteFx
import ru.redbyte.redbytefx.compose.rememberFxController
import ru.redbyte.redbytefx.stdlib.blendOverlay
import ru.redbyte.redbytefx.stdlib.blendScreen
import ru.redbyte.redbytefx.stdlib.circleMask
import ru.redbyte.redbytefx.stdlib.rectMask
import ru.redbyte.redbytefx.stdlib.ringMask

import ru.redbyte.redbytefx.sample.ui.DemoLayout
import ru.redbyte.redbytefx.sample.ui.DemoPreviewStage
import ru.redbyte.redbytefx.sample.ui.SliderRow


private data class SpotlightSetup(
    val effect: ru.redbyte.redbytefx.FxEffect,
    val center: FxParam.Float2,
    val radius: FxParam.Float,
    val amount: FxParam.Float
)

@Composable
fun DemoSpotlight() {
    var centerXUi by rememberSaveable { mutableFloatStateOf(36f) }
    var centerYUi by rememberSaveable { mutableFloatStateOf(52f) }
    var radiusUi by rememberSaveable { mutableFloatStateOf(18f) }
    var amountUi by rememberSaveable { mutableFloatStateOf(82f) }

    val setup = remember {
        var centerParam: FxParam.Float2? = null
        var radiusParam: FxParam.Float? = null
        var amountParam: FxParam.Float? = null
        val effect = redbytefx {
            val center by autoUniformFloat2(0.36f, 0.52f)
            val radius by autoUniformFloat(0.18f)
            val amount by autoUniformFloat(0.82f)
            centerParam = center
            radiusParam = radius
            amountParam = amount

            val base = let(sample(), "base")
            val uv = let(fragCoord / resolution, "uv")
            val focus = let(
                circleMask(
                    uv,
                    center = center,
                    radius = radius,
                    feather = 0.18f
                ),
                "focus"
            )
            val halo = let(
                ringMask(
                    uv,
                    center = center,
                    radius = radius + 0.07f,
                    width = 0.1f,
                    feather = 0.05f
                ),
                "halo"
            )
            val panel = let(
                rectMask(
                    uv,
                    center = float2(0.78f, 0.5f),
                    size = float2(0.26f, 0.58f),
                    feather = 0.04f
                ),
                "panel"
            )
            val dimmed = let(base * mix(0.28f, 1f, focus), "dimmed")
            val haloTint = let(color(float3(0.15f, 0.92f, 0.98f), base.a), "halo_tint")
            val panelTint = let(color(float3(0.98f, 0.73f, 0.26f), base.a), "panel_tint")
            val focused = let(blendScreen(dimmed, haloTint, halo * amount), "focused")

            blendOverlay(focused, panelTint, panel * amount * 0.35f)
        }
        SpotlightSetup(
            effect = effect,
            center = centerParam!!,
            radius = radiusParam!!,
            amount = amountParam!!
        )
    }

    val fx = rememberFxController(setup.effect)
    fx.bindFloat2(setup.center, centerXUi / 100f, centerYUi / 100f)
    fx.bindFloat(setup.radius, radiusUi / 100f)
    fx.bindFloat(setup.amount, amountUi / 100f)

    DemoLayout(
        generatedAgsl = rememberGeneratedAgsl(setup.effect),
        preview = {
            DemoPreviewStage(modifier = Modifier.redbyteFx(fx))
        },
        controls = {
            SliderRow("Center X", centerXUi, 10f..90f) {
                centerXUi = it
            }
            SliderRow("Center Y", centerYUi, 18f..82f) {
                centerYUi = it
            }
            SliderRow("Radius", radiusUi, 8f..32f) {
                radiusUi = it
            }
            SliderRow("Amount", amountUi, 0f..100f) {
                amountUi = it
            }
        }
    )
}
