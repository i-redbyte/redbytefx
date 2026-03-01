package ru.redbyte.redbytefx.sample.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import ru.redbyte.redbytefx.FxParam
import ru.redbyte.redbytefx.MirrorXFrom
import ru.redbyte.redbytefx.MirrorXParams
import ru.redbyte.redbytefx.MirrorYFrom
import ru.redbyte.redbytefx.MirrorYParams
import ru.redbyte.redbytefx.compose.redbyteFx
import ru.redbyte.redbytefx.compose.rememberFxController
import ru.redbyte.redbytefx.redbytefx

private enum class Axis { X, Y }

@Composable
fun DemoFlip() {
    var flipX by rememberSaveable { mutableStateOf(false) }
    var flipY by rememberSaveable { mutableStateOf(false) }

    val setup = remember {
        var px: FxParam.Float? = null
        var py: FxParam.Float? = null
        val effect = redbytefx {
            px = flipX(0f)
            py = flipY(0f)
        }
        Triple(effect, px!!, py!!)
    }

    val fx = rememberFxController(setup.first)

    DemoLayout(
        preview = {
            Text(
                text = "RedByteFX",
                style = MaterialTheme.typography.displayLarge,
                modifier = Modifier.redbyteFx(fx)
            )
        },
        controls = {
            SwitchRow("Flip X", flipX) {
                flipX = it
                fx.setFloat(setup.second, if (it) 1f else 0f)
            }
            SwitchRow("Flip Y", flipY) {
                flipY = it
                fx.setFloat(setup.third, if (it) 1f else 0f)
            }
        }
    )
}

@Composable
fun DemoMirror() {
    var enabled by rememberSaveable { mutableStateOf(false) }
    var axis by rememberSaveable { mutableStateOf(Axis.X) }

    var fromX by rememberSaveable { mutableStateOf(MirrorXFrom.Right) }
    var fromY by rememberSaveable { mutableStateOf(MirrorYFrom.Bottom) }

    val setup = remember {
        var mx: MirrorXParams? = null
        var my: MirrorYParams? = null
        val effect = redbytefx {
            mx = mirrorX(enabled = 0f, from = MirrorXFrom.Right)
            my = mirrorY(enabled = 0f, from = MirrorYFrom.Bottom)
        }
        Triple(effect, mx!!, my!!)
    }

    val fx = rememberFxController(setup.first)

    fun apply() {
        val useX = axis == Axis.X
        fx.setFloat(setup.second.enabled, if (enabled && useX) 1f else 0f)
        fx.setFloat(setup.second.from, fromX.v)
        fx.setFloat(setup.third.enabled, if (enabled && !useX) 1f else 0f)
        fx.setFloat(setup.third.from, fromY.v)
    }

    DemoLayout(
        preview = {
            Text(
                text = "RedByteFX",
                style = MaterialTheme.typography.displayLarge,
                modifier = Modifier.redbyteFx(fx)
            )
        },
        controls = {
            SwitchRow("Enabled", enabled) {
                enabled = it
                apply()
            }

            Text(text = "Axis", style = MaterialTheme.typography.titleMedium)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                RadioRow("X", selected = axis == Axis.X) {
                    axis = Axis.X
                    apply()
                }
                RadioRow("Y", selected = axis == Axis.Y) {
                    axis = Axis.Y
                    apply()
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
                        apply()
                    }
                    RadioRow("Left", selected = fromX == MirrorXFrom.Left) {
                        fromX = MirrorXFrom.Left
                        apply()
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
                        apply()
                    }
                    RadioRow("Top", selected = fromY == MirrorYFrom.Top) {
                        fromY = MirrorYFrom.Top
                        apply()
                    }
                }
            }
        }
    )
}

@Composable
fun DemoRotate() {
    var angle by rememberSaveable { mutableFloatStateOf(0f) }

    val setup = remember {
        var p: FxParam.Float? = null
        val effect = redbytefx { p = rotate(0f) }
        Pair(effect, p!!)
    }

    val fx = rememberFxController(setup.first)

    DemoLayout(
        preview = {
            Text(
                text = "RedByteFX",
                style = MaterialTheme.typography.displayLarge,
                modifier = Modifier.redbyteFx(fx)
            )
        },
        controls = {
            SliderRow("Angle", angle, 0f..360f) {
                angle = it
                fx.setFloat(setup.second, it)
            }
        }
    )
}

@Composable
fun DemoScale() {
    var sx by rememberSaveable { mutableFloatStateOf(1f) }
    var sy by rememberSaveable { mutableFloatStateOf(1f) }

    val setup = remember {
        var p: FxParam.Float2? = null
        val effect = redbytefx { p = scale(1f, 1f) }
        Pair(effect, p!!)
    }

    val fx = rememberFxController(setup.first)

    fun apply() {
        fx.setFloat2(setup.second, sx, sy)
    }

    DemoLayout(
        preview = {
            Text(
                text = "RedByteFX",
                style = MaterialTheme.typography.displayLarge,
                modifier = Modifier.redbyteFx(fx)
            )
        },
        controls = {
            SliderRow("Scale X", sx * 100f, 25f..300f) {
                sx = it / 100f
                apply()
            }
            SliderRow("Scale Y", sy * 100f, 25f..300f) {
                sy = it / 100f
                apply()
            }
        }
    )
}

@Composable
fun DemoOffset() {
    var dx by rememberSaveable { mutableFloatStateOf(0f) }
    var dy by rememberSaveable { mutableFloatStateOf(0f) }

    val setup = remember {
        var p: FxParam.Float2? = null
        val effect = redbytefx { p = offset(0f, 0f) }
        Pair(effect, p!!)
    }

    val fx = rememberFxController(setup.first)

    fun apply() {
        fx.setFloat2(setup.second, dx, dy)
    }

    DemoLayout(
        preview = {
            Text(
                text = "RedByteFX",
                style = MaterialTheme.typography.displayLarge,
                modifier = Modifier.redbyteFx(fx)
            )
        },
        controls = {
            SliderRow("Offset X", dx, -200f..200f) {
                dx = it
                apply()
            }
            SliderRow("Offset Y", dy, -200f..200f) {
                dy = it
                apply()
            }
        }
    )
}