package ru.redbyte.redbytefx.sample.ui.demos

import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.redbyte.redbytefx.*
import ru.redbyte.redbytefx.compose.bindFloat
import ru.redbyte.redbytefx.compose.redbyteFx
import ru.redbyte.redbytefx.compose.rememberFxController

import ru.redbyte.redbytefx.sample.ui.DemoLayout
import ru.redbyte.redbytefx.sample.ui.DemoPreviewStage
import ru.redbyte.redbytefx.sample.ui.RadioRow
import ru.redbyte.redbytefx.sample.ui.SwitchRow

private enum class Axis { X, Y }

private data class MirrorSetup(
    val effect: ru.redbyte.redbytefx.FxEffect,
    val xEnabled: FxParam.Float,
    val xFrom: FxParam.Float,
    val yEnabled: FxParam.Float,
    val yFrom: FxParam.Float
)


@Composable
fun DemoMirror() {
    var enabled by rememberSaveable { mutableStateOf(false) }
    var axis by rememberSaveable { mutableStateOf(Axis.X) }

    var fromX by rememberSaveable { mutableStateOf(MirrorXFrom.Right) }
    var fromY by rememberSaveable { mutableStateOf(MirrorYFrom.Bottom) }

    val setup = remember {
        var xEnabled: FxParam.Float? = null
        var xFrom: FxParam.Float? = null
        var yEnabled: FxParam.Float? = null
        var yFrom: FxParam.Float? = null
        val effect = redbytefx {
            val mirrorXEnabled = uniformFloat(0f, "mirror_x_enabled")
            val mirrorXFrom = uniformFloat(MirrorXFrom.Right.shaderValue, "mirror_x_from")
            val mirrorYEnabled = uniformFloat(0f, "mirror_y_enabled")
            val mirrorYFrom = uniformFloat(MirrorYFrom.Bottom.shaderValue, "mirror_y_from")
            xEnabled = mirrorXEnabled
            xFrom = mirrorXFrom
            yEnabled = mirrorYEnabled
            yFrom = mirrorYFrom
            sample(
                mirrorY(
                    coord = mirrorX(
                        amount = mirrorXEnabled,
                        from = mirrorXFrom
                    ),
                    amount = mirrorYEnabled,
                    from = mirrorYFrom
                )
            )
        }
        MirrorSetup(effect, xEnabled!!, xFrom!!, yEnabled!!, yFrom!!)
    }

    val fx = rememberFxController(setup.effect)
    val useX = axis == Axis.X
    fx.bindFloat(setup.xEnabled, if (enabled && useX) 1f else 0f)
    fx.bindFloat(setup.xFrom, fromX.shaderValue)
    fx.bindFloat(setup.yEnabled, if (enabled && !useX) 1f else 0f)
    fx.bindFloat(setup.yFrom, fromY.shaderValue)

    DemoLayout(
        generatedAgsl = rememberGeneratedAgsl(setup.effect),
        preview = {
            DemoPreviewStage(modifier = Modifier.redbyteFx(fx))
        },
        controls = {
            SwitchRow("Enabled", enabled) {
                enabled = it
            }

            Text(text = "Axis", style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                RadioRow("X", selected = axis == Axis.X) {
                    axis = Axis.X
                }
                RadioRow("Y", selected = axis == Axis.Y) {
                    axis = Axis.Y
                }
            }

            if (axis == Axis.X) {
                Text(text = "From", style = MaterialTheme.typography.titleMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    RadioRow("Right", selected = fromX == MirrorXFrom.Right) {
                        fromX = MirrorXFrom.Right
                    }
                    RadioRow("Left", selected = fromX == MirrorXFrom.Left) {
                        fromX = MirrorXFrom.Left
                    }
                }
            } else {
                Text(text = "From", style = MaterialTheme.typography.titleMedium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    RadioRow("Bottom", selected = fromY == MirrorYFrom.Bottom) {
                        fromY = MirrorYFrom.Bottom
                    }
                    RadioRow("Top", selected = fromY == MirrorYFrom.Top) {
                        fromY = MirrorYFrom.Top
                    }
                }
            }
        }
    )
}
