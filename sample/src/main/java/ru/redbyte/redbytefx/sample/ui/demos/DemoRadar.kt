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
import ru.redbyte.redbytefx.stdlib.angularSweep
import ru.redbyte.redbytefx.stdlib.arcMask
import ru.redbyte.redbytefx.stdlib.maskedOverlay
import ru.redbyte.redbytefx.stdlib.maskedScreen
import ru.redbyte.redbytefx.stdlib.polarCoordinates
import ru.redbyte.redbytefx.stdlib.ringMask
import ru.redbyte.redbytefx.stdlib.radialRamp

import ru.redbyte.redbytefx.sample.ui.DemoLayout
import ru.redbyte.redbytefx.sample.ui.DemoPreviewStage
import ru.redbyte.redbytefx.sample.ui.SliderRow
import ru.redbyte.redbytefx.sample.ui.SwitchRow


private data class RadarSetup(
    val effect: ru.redbyte.redbytefx.FxEffect,
    val time: FxParam.Float,
    val speed: FxParam.Float,
    val radius: FxParam.Float,
    val amount: FxParam.Float
)

@Composable
fun DemoRadar() {
    var playing by rememberSaveable { mutableStateOf(true) }
    var speedUi by rememberSaveable { mutableFloatStateOf(72f) }
    var radiusUi by rememberSaveable { mutableFloatStateOf(34f) }
    var amountUi by rememberSaveable { mutableFloatStateOf(86f) }

    val setup = remember {
        var timeParam: FxParam.Float? = null
        var speedParam: FxParam.Float? = null
        var radiusParam: FxParam.Float? = null
        var amountParam: FxParam.Float? = null
        val effect = redbytefx {
            val time by autoUniformTime()
            val speed by autoUniformFloat(0.72f)
            val radius by autoUniformFloat(0.34f)
            val amount by autoUniformFloat(0.86f)
            timeParam = time
            speedParam = speed
            radiusParam = radius
            amountParam = amount

            val base = let(sample(), "base")
            val uv = let(fragCoord / resolution, "uv")
            val polar = let(polarCoordinates(uv), "polar")
            val sweepAngle = let(fract(time * speed * 0.08f), "sweep_angle")
            val sweep = let(
                angularSweep(
                    uv = uv,
                    angle = sweepAngle,
                    width = 0.12f,
                    feather = 0.03f
                ),
                "sweep"
            )
            val arc = let(
                arcMask(
                    uv = uv,
                    radius = radius,
                    ringWidth = 0.09f,
                    angle = sweepAngle,
                    arcWidth = 0.18f,
                    feather = 0.03f
                ),
                "arc"
            )
            val outerRing = let(ringMask(uv, radius = radius, width = 0.016f, feather = 0.012f), "outer_ring")
            val innerRing = let(
                ringMask(
                    uv = uv,
                    radius = max(radius * 0.58f, 0.08f),
                    width = 0.014f,
                    feather = 0.012f
                ),
                "inner_ring"
            )
            val beam = let(
                radialRamp(
                    uv = uv,
                    innerRadius = float(0.06f),
                    outerRadius = radius + 0.18f
                ),
                "beam"
            )
            val mask = let(max(max(sweep * beam, arc), max(outerRing, innerRing)), "mask")
            val tint = let(
                color(
                    mix(0.05f, 0.18f, polar.x * 1.4f),
                    mix(0.24f, 1f, sweep + arc * 0.55f),
                    mix(0.10f, 0.62f, polar.y * 0.45f + outerRing * 0.35f),
                    base.a
                ),
                "tint"
            )
            val screened = let(maskedScreen(base, tint, mask, amount), "screened")

            maskedOverlay(
                base = screened,
                blend = color(float3(0.82f, 1f, 0.72f), base.a),
                mask = arc,
                amount = amount * 0.32f
            )
        }
        RadarSetup(
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
                label = "Radar//Polar"
            )
        },
        controls = {
            SwitchRow("Play", playing) {
                playing = it
            }
            SliderRow("Speed", speedUi, 20f..140f) {
                speedUi = it
            }
            SliderRow("Radius", radiusUi, 18f..44f) {
                radiusUi = it
            }
            SliderRow("Amount", amountUi, 0f..100f) {
                amountUi = it
            }
        }
    )
}
