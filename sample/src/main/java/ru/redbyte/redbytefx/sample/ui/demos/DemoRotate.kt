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

@Composable
fun DemoRotate() {
    var angle by rememberSaveable { mutableFloatStateOf(0f) }

    val setup = remember {
        var p: FxParam.Float? = null
        val effect = redbytefx {
            val angle = uniformFloat(0f, "angle_deg")
            p = angle
            val pivot = center()
            val delta = fragCoord - pivot
            val theta = radians(angle)
            val s = sin(theta)
            val c = cos(theta)
            val rotated = pivot + float2(
                c * delta.x - s * delta.y,
                s * delta.x + c * delta.y
            )
            sample(rotated)
        }
        Pair(effect, p!!)
    }

    val fx = rememberFxController(setup.first)
    fx.bindFloat(setup.second, angle)

    DemoLayout(
        generatedAgsl = rememberGeneratedAgsl(setup.first),
        preview = {
            DemoPreviewStage(modifier = Modifier.redbyteFx(fx))
        },
        controls = {
            SliderRow("Angle", angle, 0f..360f) {
                angle = it
            }
        }
    )
}
