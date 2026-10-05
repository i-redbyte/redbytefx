package ru.redbyte.redbytefx.sample.ui.demos

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import ru.redbyte.redbytefx.*
import ru.redbyte.redbytefx.compose.bindFloat
import ru.redbyte.redbytefx.compose.bindTime
import ru.redbyte.redbytefx.compose.redbyteFx
import ru.redbyte.redbytefx.compose.rememberFxController
import ru.redbyte.redbytefx.sample.ui.DemoLayout
import ru.redbyte.redbytefx.sample.ui.DemoPreviewStage
import ru.redbyte.redbytefx.sample.ui.SliderRow
import ru.redbyte.redbytefx.sample.ui.SwitchRow
import ru.redbyte.redbytefx.stdlib.aspectCenteredUv
import ru.redbyte.redbytefx.stdlib.easeInOutSine
import ru.redbyte.redbytefx.stdlib.maskedOverlay
import ru.redbyte.redbytefx.stdlib.maskedScreen
import ru.redbyte.redbytefx.stdlib.pingPong
import ru.redbyte.redbytefx.stdlib.pulse
import ru.redbyte.redbytefx.stdlib.sdBox
import ru.redbyte.redbytefx.stdlib.sdCircle
import ru.redbyte.redbytefx.stdlib.sdRoundedBox
import ru.redbyte.redbytefx.stdlib.sdSegment
import ru.redbyte.redbytefx.stdlib.softFill
import ru.redbyte.redbytefx.stdlib.softStroke
import ru.redbyte.redbytefx.stdlib.stroke

private data class SigilSetup(
    val effect: ru.redbyte.redbytefx.ShaderProgram,
    val time: Uniform<Flt<High>>,
    val speed: Uniform<Flt<High>>,
    val amount: Uniform<Flt<High>>,
)

@Composable
fun DemoSigil() {
    var playing by rememberSaveable { mutableStateOf(true) }
    var speedUi by rememberSaveable { mutableFloatStateOf(72f) }
    var amountUi by rememberSaveable { mutableFloatStateOf(84f) }

    val setup = remember {
        var timeParam: Uniform<Flt<High>>? = null
        var speedParam: Uniform<Flt<High>>? = null
        var amountParam: Uniform<Flt<High>>? = null
        val effect = shader(ShaderTarget.Agsl) {
            val time = uniformTime(name = "time")
            val speed = uniform("speed", 0.72f)
            val amount = uniform("amount", 0.84f)
            timeParam = time
            speedParam = speed
            amountParam = amount
            fragment {
                val base = let(sample(), "base")
                val uv = let(fragCoord / resolution, "uv")
                val sigil = let(aspectCenteredUv(uv, resolution), "sigil")
                val pulse = let(easeInOutSine(pingPong(time.expr * speed.expr * 0.18f, 1f)), "pulse")
                val frame = let(
                    softStroke(
                        distance = sdRoundedBox(
                            point = sigil,
                            halfSize = float2(0.35f, 0.35f),
                            radius = 0.16f,
                        ),
                        width = 0.028f,
                        feather = 0.012f,
                    ),
                    "frame",
                )
                val ring = let(
                    softStroke(
                        distance = sdCircle(sigil, radius = 0.26f + pulse * 0.03f),
                        width = 0.032f,
                        feather = 0.014f,
                    ),
                    "ring",
                )
                val core = let(
                    softFill(
                        distance = sdCircle(sigil, radius = 0.10f + pulse * 0.05f),
                        feather = 0.015f,
                    ),
                    "core",
                )
                val spine = let(
                    softFill(
                        distance = sdBox(
                            point = sigil,
                            halfSize = float2(0.05f, 0.22f + pulse * 0.05f),
                        ),
                        feather = 0.012f,
                    ),
                    "spine",
                )
                val cross = let(
                    stroke(
                        distance = sdBox(
                            point = sigil,
                            halfSize = float2(0.21f, 0.05f),
                        ),
                        width = 0.05f,
                    ),
                    "cross",
                )
                val slash = let(
                    softStroke(
                        distance = sdSegment(
                            point = sigil,
                            start = float2(-0.28f, -0.18f),
                            end = float2(0.28f, 0.22f),
                        ),
                        width = 0.024f,
                        feather = 0.01f,
                    ),
                    "slash",
                )
                val mask = let(max(max(frame, ring), max(core, max(spine, max(cross, slash)))), "mask")
                val tint = let(
                    color(
                        mix(0.06f, 0.18f, pulse),
                        mix(0.32f, 1f, ring + core * 0.25f),
                        mix(0.18f, 0.74f, frame + spine * 0.35f),
                        base.a,
                    ),
                    "tint",
                )
                val screened = let(maskedScreen(base, tint, mask, amount.expr), "screened")
                maskedOverlay(
                    base = screened,
                    blend = color(float3(0.90f, 1f, 0.80f), base.a),
                    mask = core + ring * 0.5f,
                    amount = amount.expr * 0.26f,
                )
            }
        }
        SigilSetup(
            effect = effect,
            time = timeParam!!,
            speed = speedParam!!,
            amount = amountParam!!,
        )
    }

    val fx = rememberFxController(setup.effect)
    fx.bindTime(setup.time, isPlaying = playing)
    fx.bindFloat(setup.speed, speedUi / 100f)
    fx.bindFloat(setup.amount, amountUi / 100f)

    DemoLayout(
        generatedAgsl = rememberGeneratedAgsl(setup.effect),
        preview = {
            DemoPreviewStage(
                modifier = Modifier.redbyteFx(fx),
                label = "Sigil//SDF",
            )
        },
        controls = {
            SwitchRow("Play", playing) {
                playing = it
            }
            SliderRow("Speed", speedUi, 20f..140f) {
                speedUi = it
            }
            SliderRow("Amount", amountUi, 0f..100f) {
                amountUi = it
            }
        },
    )
}
