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
fun DemoOffset() {
    var dx by rememberSaveable { mutableFloatStateOf(0f) }
    var dy by rememberSaveable { mutableFloatStateOf(0f) }

    val setup = remember {
        var p: FxParam.Float2? = null
        val effect = redbytefx {
            val delta = uniformFloat2(0f, 0f, "offset")
            p = delta
            sample(offset(delta = delta))
        }
        Pair(effect, p!!)
    }

    val fx = rememberFxController(setup.first)
    fx.bindFloat2(setup.second, dx, dy)

    DemoLayout(
        generatedAgsl = rememberGeneratedAgsl(setup.first),
        preview = {
            DemoPreviewStage(modifier = Modifier.redbyteFx(fx))
        },
        controls = {
            SliderRow("Offset X", dx, -200f..200f) {
                dx = it
            }
            SliderRow("Offset Y", dy, -200f..200f) {
                dy = it
            }
        }
    )
}
