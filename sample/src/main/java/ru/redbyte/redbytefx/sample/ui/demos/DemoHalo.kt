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
import ru.redbyte.redbytefx.stdlib.aspectCenteredUv
import ru.redbyte.redbytefx.stdlib.centerGlow
import ru.redbyte.redbytefx.stdlib.centeredUv
import ru.redbyte.redbytefx.stdlib.easeInOutSine
import ru.redbyte.redbytefx.stdlib.maskedOverlay
import ru.redbyte.redbytefx.stdlib.maskedScreen
import ru.redbyte.redbytefx.stdlib.pingPong
import ru.redbyte.redbytefx.stdlib.radialDirection
import ru.redbyte.redbytefx.stdlib.rimLight

import ru.redbyte.redbytefx.sample.ui.DemoLayout
import ru.redbyte.redbytefx.sample.ui.DemoPreviewStage
import ru.redbyte.redbytefx.sample.ui.SliderRow
import ru.redbyte.redbytefx.sample.ui.SwitchRow


private data class HaloSetup(
    val effect: ru.redbyte.redbytefx.FxEffect,
    val time: FxParam.Float,
    val speed: FxParam.Float,
    val radius: FxParam.Float,
    val amount: FxParam.Float
)

@Composable
fun DemoHalo() {
    var playing by rememberSaveable { mutableStateOf(true) }
    var speedUi by rememberSaveable { mutableFloatStateOf(68f) }
    var radiusUi by rememberSaveable { mutableFloatStateOf(24f) }
    var amountUi by rememberSaveable { mutableFloatStateOf(82f) }

    val setup = remember {
        var timeParam: FxParam.Float? = null
        var speedParam: FxParam.Float? = null
        var radiusParam: FxParam.Float? = null
        var amountParam: FxParam.Float? = null
        val effect = redbytefx {
            val time by autoUniformTime()
            val speed by autoUniformFloat(0.68f)
            val radius by autoUniformFloat(0.24f)
            val amount by autoUniformFloat(0.82f)
            timeParam = time
            speedParam = speed
            radiusParam = radius
            amountParam = amount

            val base = let(sample(), "base")
            val uv = let(fragCoord / resolution, "uv")
            val local = let(centeredUv(uv), "local")
            val aspectLocal = let(aspectCenteredUv(uv, resolution), "aspect_local")
            val dir = let(radialDirection(uv, resolution), "dir")
            val phase = let(easeInOutSine(pingPong(time * speed * 0.18f, 1f)), "phase")
            val glow = let(
                centerGlow(
                    uv = uv,
                    resolution = resolution,
                    radius = radius * (0.82f + phase * 0.32f),
                    feather = 0.18f
                ),
                "glow"
            )
            val rim = let(
                rimLight(
                    uv = uv,
                    resolution = resolution,
                    radius = radius + 0.08f * phase,
                    width = 0.075f,
                    feather = 0.024f
                ),
                "rim"
            )
            val drift = let(saturate(0.5f + aspectLocal.x * 0.55f - local.y * 0.25f), "drift")
            val facing = let(saturate(dir.x * 0.5f - dir.y * 0.35f + 0.5f), "facing")
            val mask = let(max(glow * 0.9f, rim), "mask")
            val tint = let(
                color(
                    mix(0.04f, 0.18f, drift),
                    mix(0.22f, 1f, glow + rim * 0.45f),
                    mix(0.12f, 0.72f, facing),
                    base.a
                ),
                "tint"
            )
            val screened = let(maskedScreen(base, tint, mask, amount), "screened")

            maskedOverlay(
                base = screened,
                blend = color(float3(0.82f, 1f, 0.74f), base.a),
                mask = rim,
                amount = amount * 0.28f
            )
        }
        HaloSetup(
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
            DemoPreviewStage(
                modifier = Modifier.redbyteFx(fx),
                label = "Halo//Light"
            )
        },
        controls = {
            SwitchRow("Play", playing) {
                playing = it
            }
            SliderRow("Speed", speedUi, 20f..140f) {
                speedUi = it
            }
            SliderRow("Radius", radiusUi, 12f..36f) {
                radiusUi = it
            }
            SliderRow("Amount", amountUi, 0f..100f) {
                amountUi = it
            }
        }
    )
}
