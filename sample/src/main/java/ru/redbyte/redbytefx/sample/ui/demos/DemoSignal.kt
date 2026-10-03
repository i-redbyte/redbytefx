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
import ru.redbyte.redbytefx.stdlib.gridMask
import ru.redbyte.redbytefx.stdlib.normalizedUv
import ru.redbyte.redbytefx.stdlib.pulse
import ru.redbyte.redbytefx.stdlib.scanlines

import ru.redbyte.redbytefx.sample.ui.DemoLayout
import ru.redbyte.redbytefx.sample.ui.DemoPreviewStage
import ru.redbyte.redbytefx.sample.ui.SliderRow


private data class SignalSetup(
    val effect: ru.redbyte.redbytefx.ShaderProgram,
    val density: Uniform<Flt<High>>,
    val lineWidth: Uniform<Flt<High>>,
    val amount: Uniform<Flt<High>>
)

@Composable
fun DemoSignal() {
    var density by rememberSaveable { mutableFloatStateOf(8f) }
    var lineWidthUi by rememberSaveable { mutableFloatStateOf(8f) }
    var amountUi by rememberSaveable { mutableFloatStateOf(85f) }

    val setup = remember {
        var densityParam: Uniform<Flt<High>>? = null
        var lineWidthParam: Uniform<Flt<High>>? = null
        var amountParam: Uniform<Flt<High>>? = null
        val effect = shader(ShaderTarget.Agsl) {
            val densityUniform = uniform("signal_density", 8f)
            val lineWidthUniform = uniform("signal_line_width", 0.08f)
            val amountUniform = uniform("signal_amount", 0.85f)
            densityParam = densityUniform
            lineWidthParam = lineWidthUniform
            amountParam = amountUniform
            fragment {
                val pulseBand = fn(0f.lit, 0f.lit, "pulse_band") { phase, threshold ->
                    step(threshold, smoothstep(0.08f, 0.92f, fract(phase)))
                }
                val base = let(sample(), "base")
                val uv = let(normalizedUv(), "uv")
                val grid = let(gridMask(uv, densityUniform.expr, lineWidthUniform.expr), "grid")
                val scan = let(scanlines(fragCoord.y, 14f, 3f), "scan")
                val pulse = let(
                    pulseBand(uv.y * densityUniform.expr * 0.5f + grid * 0.35f, 0.55f.lit),
                    "pulse"
                )
                val hardMask = let(step(0.45f, scan * pulse), "hard_mask")
                val active = let((grid gt 0.05f) or (hardMask gt 0.5f), "active")
                val accent = let(color(float3(0.05f, 0.95f, 0.82f), base.a), "accent")
                val mixed = let(
                    mix(
                        base,
                        accent,
                        min(grid * 0.85f + hardMask * 0.35f, 1f)
                    ),
                    "mixed"
                )
                ifElse(active, mix(base, mixed, amountUniform.expr), base)
            }
        }
        SignalSetup(effect, densityParam!!, lineWidthParam!!, amountParam!!)
    }

    val fx = rememberFxController(setup.effect)
    fx.bindFloat(setup.density, density)
    fx.bindFloat(setup.lineWidth, lineWidthUi / 100f)
    fx.bindFloat(setup.amount, amountUi / 100f)

    DemoLayout(
        generatedAgsl = rememberGeneratedAgsl(setup.effect),
        preview = {
            DemoPreviewStage(modifier = Modifier.redbyteFx(fx))
        },
        controls = {
            SliderRow("Density", density, 2f..20f) {
                density = it
            }
            SliderRow(
                title = "Line Width",
                value = lineWidthUi,
                range = 2f..20f,
                formatValue = { "${it / 100f}" }
            ) {
                lineWidthUi = it
            }
            SliderRow("Amount", amountUi, 0f..100f) {
                amountUi = it
            }
        }
    )
}
