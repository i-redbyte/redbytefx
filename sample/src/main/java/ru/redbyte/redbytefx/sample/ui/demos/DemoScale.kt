package ru.redbyte.redbytefx.sample.ui.demos

import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import ru.redbyte.redbytefx.*
import ru.redbyte.redbytefx.compose.bindFloat2
import ru.redbyte.redbytefx.compose.redbyteFx
import ru.redbyte.redbytefx.compose.rememberFxController

import ru.redbyte.redbytefx.sample.ui.DemoLayout
import ru.redbyte.redbytefx.sample.ui.DemoPreviewStage
import ru.redbyte.redbytefx.sample.ui.SliderRow

@Composable
fun DemoScale() {
    var sx by rememberSaveable { mutableFloatStateOf(1f) }
    var sy by rememberSaveable { mutableFloatStateOf(1f) }

    val setup = remember {
        var p: Uniform<Vec2<Flt<High>>>? = null
        val effect = shader(ShaderTarget.Agsl) {
            val scaleAmount = uniformVec2("scale", 1f, 1f)
            p = scaleAmount
            fragment {
                sample(scale(scaleAmount.expr))
            }
        }
        Pair(effect, p!!)
    }

    val fx = rememberFxController(setup.first)
    fx.bindFloat2(setup.second, sx, sy)

    DemoLayout(
        generatedAgsl = rememberGeneratedAgsl(setup.first),
        preview = {
            DemoPreviewStage(modifier = Modifier.redbyteFx(fx))
        },
        controls = {
            SliderRow("Scale X", sx * 100f, 25f..300f) {
                sx = it / 100f
            }
            SliderRow("Scale Y", sy * 100f, 25f..300f) {
                sy = it / 100f
            }
        }
    )
}
