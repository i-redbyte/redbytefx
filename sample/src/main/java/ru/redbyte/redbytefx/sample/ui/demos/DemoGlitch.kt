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
import ru.redbyte.redbytefx.stdlib.bandMask
import ru.redbyte.redbytefx.stdlib.chromaticOffset
import ru.redbyte.redbytefx.stdlib.maskedMix
import ru.redbyte.redbytefx.stdlib.maskedOverlay
import ru.redbyte.redbytefx.stdlib.maskedScreen
import ru.redbyte.redbytefx.stdlib.pingPong
import ru.redbyte.redbytefx.stdlib.scanWarp
import ru.redbyte.redbytefx.stdlib.signalBars
import ru.redbyte.redbytefx.stdlib.sampleUv

import ru.redbyte.redbytefx.sample.ui.DemoLayout
import ru.redbyte.redbytefx.sample.ui.DemoPreviewStage
import ru.redbyte.redbytefx.sample.ui.SliderRow
import ru.redbyte.redbytefx.sample.ui.SwitchRow

private data class GlitchSetup(
    val effect: ru.redbyte.redbytefx.ShaderProgram,
    val time: Uniform<Flt<High>>,
    val density: Uniform<Flt<High>>,
    val warp: Uniform<Flt<High>>,
    val amount: Uniform<Flt<High>>
)

@Composable
fun DemoGlitch() {
    var playing by rememberSaveable { mutableStateOf(true) }
    var densityUi by rememberSaveable { mutableFloatStateOf(76f) }
    var warpUi by rememberSaveable { mutableFloatStateOf(30f) }
    var amountUi by rememberSaveable { mutableFloatStateOf(82f) }

    val setup = remember {
        var timeParam: Uniform<Flt<High>>? = null
        var densityParam: Uniform<Flt<High>>? = null
        var warpParam: Uniform<Flt<High>>? = null
        var amountParam: Uniform<Flt<High>>? = null
        val effect = shader(ShaderTarget.Agsl) {
            val time = uniformTime(name = "time")
            val density = uniform("density", 7.6f)
            val warp = uniform("warp", 0.03f)
            val amount = uniform("amount", 0.82f)
            timeParam = time
            densityParam = density
            warpParam = warp
            amountParam = amount
            fragment {
                val base = let(sample(), "base")
                val uv = let(fragCoord / resolution, "uv")
                val driftUv = let(
                    scanWarp(
                        uv = uv,
                        time = time.expr,
                        amplitude = warp.expr,
                        density = density.expr,
                        speed = 2.2f,
                        noiseAmount = 0.55f
                    ),
                    "drift_uv"
                )
                val drifted = let(sampleUv(driftUv), "drifted")
                val bars = let(
                    signalBars(
                        position = uv.y,
                        density = density.expr,
                        width = 0.28f,
                        phase = time.expr * 0.65f,
                        feather = 0.1f
                    ),
                    "bars"
                )
                val lockCenter = let(0.22f + pingPong(time.expr * 0.12f, 1f) * 0.56f, "lock_center")
                val lock = let(
                    bandMask(
                        position = uv.y,
                        center = lockCenter,
                        width = 0.14f,
                        feather = 0.08f
                    ),
                    "lock"
                )
                val split = let(
                    chromaticOffset(
                        offset = 9f + bars * 12f,
                        direction = float2(1f, 0.12f),
                        amount = amount.expr * (bars * 0.7f + lock * 0.3f)
                    ),
                    "split"
                )
                val tint = let(color(float3(0.08f, 0.96f, 0.68f), base.a), "tint")
                val screened = let(
                    maskedScreen(
                        base = drifted,
                        blend = split + tint * 0.14f,
                        mask = bars,
                        amount = amount.expr
                    ),
                    "screened"
                )
                val locked = let(
                    maskedOverlay(
                        base = screened,
                        blend = color(float3(0.52f, 1f, 0.24f), base.a),
                        mask = lock,
                        amount = amount.expr * 0.45f
                    ),
                    "locked"
                )
                maskedMix(base, locked, max(bars, lock), amount.expr)
            }
        }
        GlitchSetup(
            effect = effect,
            time = timeParam!!,
            density = densityParam!!,
            warp = warpParam!!,
            amount = amountParam!!
        )
    }

    val fx = rememberFxController(setup.effect)
    fx.bindTime(setup.time, isPlaying = playing)
    fx.bindFloat(setup.density, densityUi / 10f)
    fx.bindFloat(setup.warp, warpUi / 1000f)
    fx.bindFloat(setup.amount, amountUi / 100f)

    DemoLayout(
        generatedAgsl = rememberGeneratedAgsl(setup.effect),
        preview = {
            DemoPreviewStage(
                modifier = Modifier.redbyteFx(fx),
                label = "Signal//Ghost"
            )
        },
        controls = {
            SwitchRow("Play", playing) {
                playing = it
            }
            SliderRow("Density", densityUi, 20f..140f) {
                densityUi = it
            }
            SliderRow("Warp", warpUi, 0f..80f) {
                warpUi = it
            }
            SliderRow("Amount", amountUi, 0f..100f) {
                amountUi = it
            }
        }
    )
}
