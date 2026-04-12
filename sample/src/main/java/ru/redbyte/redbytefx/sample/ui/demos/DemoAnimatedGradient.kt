package ru.redbyte.redbytefx.sample.ui.demos

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import ru.redbyte.redbytefx.FxEffect
import ru.redbyte.redbytefx.FxParam
import ru.redbyte.redbytefx.compose.bindFloat
import ru.redbyte.redbytefx.compose.bindTime
import ru.redbyte.redbytefx.compose.redbyteFx
import ru.redbyte.redbytefx.compose.rememberFxController
import ru.redbyte.redbytefx.redbytefx
import ru.redbyte.redbytefx.*
import ru.redbyte.redbytefx.sample.ui.DemoLayout
import ru.redbyte.redbytefx.sample.ui.DemoPreviewStage
import ru.redbyte.redbytefx.sample.ui.SliderRow
import ru.redbyte.redbytefx.sample.ui.SwitchRow

private data class AnimatedGradientSetup(
    val effect: FxEffect,
    val time: FxParam.Float,
    val speed: FxParam.Float,
)

/**
 * Port of a minimal AGSL animated RGB gradient:
 *
 * ```
 * float2 uv = fragCoord / u_resolution;
 * float t = u_time;
 * float r = 0.5 + 0.5 * sin(3.0 * uv.x + t * 0.7);
 * float g = 0.5 + 0.5 * sin(3.0 * uv.y + t * 1.1);
 * float b = 0.5 + 0.5 * sin(3.0 * (uv.x + uv.y) + t * 0.9);
 * return half4(r, g, b, 1.0);
 * ```
 *
 * [speed] scales time so the preview can be slowed or paused without touching the timeline uniform.
 */
@Composable
fun DemoAnimatedGradient() {
    var playing by rememberSaveable { mutableStateOf(true) }
    var speedUi by rememberSaveable { mutableFloatStateOf(100f) }

    val setup = remember {
        var timeParam: FxParam.Float? = null
        var speedParam: FxParam.Float? = null
        val effect = redbytefx {
            val timeUniform = uniformTime(name = "u_time")
            val speedUniform = uniformFloat(1f, "gradient_speed")
            timeParam = timeUniform
            speedParam = speedUniform

            val uv = let(fragCoord / resolution, "uv")
            val t = let(timeUniform * speedUniform, "t")
            val r = let(0.5f + 0.5f * sin(3f * uv.x + t * 0.7f), "r")
            val g = let(0.5f + 0.5f * sin(3f * uv.y + t * 1.1f), "g")
            val b = let(0.5f + 0.5f * sin(3f * (uv.x + uv.y) + t * 0.9f), "b")
            color(float3(r, g, b), 1f)
        }
        AnimatedGradientSetup(effect, timeParam!!, speedParam!!)
    }

    val fx = rememberFxController(setup.effect)
    fx.bindTime(setup.time, isPlaying = playing)
    fx.bindFloat(setup.speed, speedUi / 100f)

    DemoLayout(
        generatedAgsl = rememberGeneratedAgsl(setup.effect),
        preview = {
            DemoPreviewStage(modifier = Modifier.redbyteFx(fx))
        },
        controls = {
            SwitchRow("Play", playing) {
                playing = it
            }
            SliderRow(
                title = "Speed",
                value = speedUi,
                range = 0f..300f,
                formatValue = { "${it / 100f}x" }
            ) {
                speedUi = it
            }
        }
    )
}
