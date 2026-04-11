package ru.redbyte.redbytefx.sample.ui.demos

import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import ru.redbyte.redbytefx.*
import ru.redbyte.redbytefx.compose.bindFloat
import ru.redbyte.redbytefx.compose.bindTime
import ru.redbyte.redbytefx.compose.redbyteFx
import ru.redbyte.redbytefx.compose.rememberFxController
import ru.redbyte.redbytefx.stdlib.blendScreen
import ru.redbyte.redbytefx.stdlib.circleMask
import ru.redbyte.redbytefx.stdlib.easeInOutCubic
import ru.redbyte.redbytefx.stdlib.easeInOutSine
import ru.redbyte.redbytefx.stdlib.pingPong
import ru.redbyte.redbytefx.stdlib.ringMask

import ru.redbyte.redbytefx.sample.ui.DemoLayout
import ru.redbyte.redbytefx.sample.ui.DemoPreviewStage
import ru.redbyte.redbytefx.sample.ui.SliderRow
import ru.redbyte.redbytefx.sample.ui.SwitchRow


private data class BeaconSetup(
    val effect: ru.redbyte.redbytefx.FxEffect,
    val time: FxParam.Float,
    val speed: FxParam.Float,
    val radius: FxParam.Float,
    val amount: FxParam.Float
)

@Composable
fun DemoBeacon() {
    var playing by rememberSaveable { mutableStateOf(true) }
    var speedUi by rememberSaveable { mutableFloatStateOf(65f) }
    var radiusUi by rememberSaveable { mutableFloatStateOf(17f) }
    var amountUi by rememberSaveable { mutableFloatStateOf(88f) }

    val setup = remember {
        var timeParam: FxParam.Float? = null
        var speedParam: FxParam.Float? = null
        var radiusParam: FxParam.Float? = null
        var amountParam: FxParam.Float? = null
        val effect = redbytefx {
            val time by autoUniformTime()
            val speed by autoUniformFloat(0.65f)
            val radius by autoUniformFloat(0.17f)
            val amount by autoUniformFloat(0.88f)
            timeParam = time
            speedParam = speed
            radiusParam = radius
            amountParam = amount

            val base = let(sample(), "base")
            val uv = let(fragCoord / resolution, "uv")
            val phase = let(pingPong(time * speed, 1f), "phase")
            val travel = let(easeInOutSine(phase), "travel")
            val glow = let(easeInOutCubic(pingPong(time * speed + 0.22f, 1f)), "glow")
            val center = let(
                float2(
                    mix(0.18f, 0.82f, travel),
                    0.5f + sin(time * 0.8f) * 0.12f
                ),
                "center"
            )
            val focus = let(circleMask(uv, center = center, radius = radius, feather = 0.16f), "focus")
            val halo = let(
                ringMask(
                    uv,
                    center = center,
                    radius = radius + 0.06f,
                    width = 0.08f,
                    feather = 0.05f
                ),
                "halo"
            )
            val dimmed = let(base * mix(0.28f, 1f, focus), "dimmed")
            val beamTint = let(color(float3(0.12f, 0.94f, 0.98f), base.a), "beam_tint")
            val haloTint = let(color(float3(1f, 0.86f, 0.35f), base.a), "halo_tint")
            val focused = let(blendScreen(dimmed, beamTint, focus * amount * 0.7f), "focused")

            blendScreen(focused, haloTint, halo * glow * amount)
        }
        BeaconSetup(
            effect = effect,
            time = timeParam!!,
            speed = speedParam!!,
            radius = radiusParam!!,
            amount = amountParam!!
        )
    }

    val fx = rememberFxController(setup.effect)
    fx.bindTime(setup.time, isPlaying = playing)
    fx.bindFloat(setup.speed, speedUi / 100f)
    fx.bindFloat(setup.radius, radiusUi / 100f)
    fx.bindFloat(setup.amount, amountUi / 100f)

    DemoLayout(
        generatedAgsl = rememberGeneratedAgsl(setup.effect),
        preview = {
            DemoPreviewStage(modifier = Modifier.redbyteFx(fx))
        },
        controls = {
            SwitchRow("Play", playing) {
                playing = it
            }
            SliderRow("Speed", speedUi, 20f..140f) {
                speedUi = it
            }
            SliderRow("Radius", radiusUi, 10f..28f) {
                radiusUi = it
            }
            SliderRow("Amount", amountUi, 0f..100f) {
                amountUi = it
            }
        }
    )
}
