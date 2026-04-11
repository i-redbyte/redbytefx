package ru.redbyte.redbytefx.sample.ui.demos

import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import ru.redbyte.redbytefx.*
import ru.redbyte.redbytefx.compose.bindFloat
import ru.redbyte.redbytefx.compose.redbyteFx
import ru.redbyte.redbytefx.compose.rememberFxController

import ru.redbyte.redbytefx.sample.ui.DemoLayout
import ru.redbyte.redbytefx.sample.ui.DemoPreviewStage
import ru.redbyte.redbytefx.sample.ui.SwitchRow

@Composable
fun DemoFlip() {
    var flipX by rememberSaveable { mutableStateOf(false) }
    var flipY by rememberSaveable { mutableStateOf(false) }

    val setup = remember {
        var px: FxParam.Float? = null
        var py: FxParam.Float? = null
        val effect = redbytefx {
            val flipXAmount = uniformFloat(0f, "flip_x")
            val flipYAmount = uniformFloat(0f, "flip_y")
            px = flipXAmount
            py = flipYAmount
            sample(
                flipY(
                    coord = flipX(amount = flipXAmount),
                    amount = flipYAmount
                )
            )
        }
        Triple(effect, px!!, py!!)
    }

    val fx = rememberFxController(setup.first)
    fx.bindFloat(setup.second, if (flipX) 1f else 0f)
    fx.bindFloat(setup.third, if (flipY) 1f else 0f)

    DemoLayout(
        generatedAgsl = rememberGeneratedAgsl(setup.first),
        preview = {
            DemoPreviewStage(modifier = Modifier.redbyteFx(fx))
        },
        controls = {
            SwitchRow("Flip X", flipX) {
                flipX = it
            }
            SwitchRow("Flip Y", flipY) {
                flipY = it
            }
        }
    )
}
