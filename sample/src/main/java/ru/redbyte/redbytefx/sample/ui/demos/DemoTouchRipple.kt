package ru.redbyte.redbytefx.sample.ui.demos

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import ru.redbyte.redbytefx.FxEffect
import ru.redbyte.redbytefx.FxParam
import ru.redbyte.redbytefx.compose.bindFloat
import ru.redbyte.redbytefx.compose.bindFloat2
import ru.redbyte.redbytefx.compose.bindTime
import ru.redbyte.redbytefx.compose.redbyteFx
import ru.redbyte.redbytefx.compose.rememberFxController
import ru.redbyte.redbytefx.redbytefx
import ru.redbyte.redbytefx.sample.ui.DemoLayout
import ru.redbyte.redbytefx.sample.ui.DemoPreviewStage
import ru.redbyte.redbytefx.sample.ui.SliderRow
import ru.redbyte.redbytefx.sample.ui.SwitchRow
import ru.redbyte.redbytefx.*

private data class TouchRippleSetup(
    val effect: FxEffect,
    val pointer: FxParam.Float2,
    val time: FxParam.Float,
    val strength: FxParam.Float,
)

@Composable
fun DemoTouchRipple() {
    var pointerXUi by rememberSaveable { mutableFloatStateOf(50f) }
    var pointerYUi by rememberSaveable { mutableFloatStateOf(50f) }
    var playing by rememberSaveable { mutableStateOf(true) }
    var strengthUi by rememberSaveable { mutableFloatStateOf(72f) }

    val setup = remember {
        var pointerParam: FxParam.Float2? = null
        var timeParam: FxParam.Float? = null
        var strengthParam: FxParam.Float? = null
        val effect = redbytefx {
            val pointer = uniformFloat2(0.5f, 0.5f, "touch_pointer")
            val timeUniform = uniformTime(name = "touch_time")
            val strengthUniform = uniformFloat(0.72f, "touch_strength")
            pointerParam = pointer
            timeParam = timeUniform
            strengthParam = strengthUniform

            val base = let(sample(), "base")
            val uv = let(fragCoord / resolution, "uv")
            val d = let(length(uv - pointer), "d")
            val waves = let(sin(d * 32f - timeUniform * 2.4f) * 0.5f + 0.5f, "waves")
            val atten = let(1f / (1f + d * 14f), "atten")
            val ripple = let(waves * atten, "ripple")
            val tint = color(float3(0.35f, 0.92f, 1f), base.a)
            mix(base, tint, ripple * strengthUniform)
        }
        TouchRippleSetup(effect, pointerParam!!, timeParam!!, strengthParam!!)
    }

    val fx = rememberFxController(setup.effect)
    fx.bindFloat2(setup.pointer, pointerXUi / 100f, pointerYUi / 100f)
    fx.bindTime(setup.time, isPlaying = playing)
    fx.bindFloat(setup.strength, strengthUi / 100f)

    val pointerModifier = Modifier
        .pointerInput(Unit) {
            detectTapGestures { offset ->
                pointerXUi = offset.x / size.width * 100f
                pointerYUi = offset.y / size.height * 100f
            }
        }
        .pointerInput(Unit) {
            detectDragGestures { change, _ ->
                pointerXUi = change.position.x / size.width * 100f
                pointerYUi = change.position.y / size.height * 100f
            }
        }

    DemoLayout(
        generatedAgsl = rememberGeneratedAgsl(setup.effect),
        preview = {
            DemoPreviewStage(
                modifier = pointerModifier.redbyteFx(fx)
            )
        },
        controls = {
            SwitchRow("Play", playing) {
                playing = it
            }
            SliderRow("Strength", strengthUi, 0f..100f) {
                strengthUi = it
            }
        }
    )
}
