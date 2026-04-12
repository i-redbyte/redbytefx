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
import ru.redbyte.redbytefx.sample.ui.DemoLayout
import ru.redbyte.redbytefx.sample.ui.DemoPreviewStage
import ru.redbyte.redbytefx.sample.ui.SliderRow
import ru.redbyte.redbytefx.sample.ui.SwitchRow
import ru.redbyte.redbytefx.*
import ru.redbyte.redbytefx.stdlib.aspectCenteredUv
import ru.redbyte.redbytefx.stdlib.normalizedUv
import ru.redbyte.redbytefx.stdlib.sampleUv
import ru.redbyte.redbytefx.stdlib.scanlines

private data class CrtTerminalSetup(
    val effect: FxEffect,
    val time: FxParam.Float,
    val barrel: FxParam.Float,
    val aberration: FxParam.Float,
    val scan: FxParam.Float,
)

@Composable
fun DemoCrtTerminal() {
    var playing by rememberSaveable { mutableStateOf(true) }
    var barrelUi by rememberSaveable { mutableFloatStateOf(28f) }
    var aberrationUi by rememberSaveable { mutableFloatStateOf(62f) }
    var scanUi by rememberSaveable { mutableFloatStateOf(48f) }

    val setup = remember {
        var timeParam: FxParam.Float? = null
        var barrelParam: FxParam.Float? = null
        var aberrationParam: FxParam.Float? = null
        var scanParam: FxParam.Float? = null
        val effect = redbytefx {
            val timeUniform = uniformTime(name = "crt_time")
            val barrelUniform = uniformFloat(0.28f, "crt_barrel")
            val aberrationUniform = uniformFloat(0.62f, "crt_aberration")
            val scanUniform = uniformFloat(0.48f, "crt_scan")
            timeParam = timeUniform
            barrelParam = barrelUniform
            aberrationParam = aberrationUniform
            scanParam = scanUniform

            val uv = let(normalizedUv(), "uv")
            val local = let(uv - float2(0.5f, 0.5f), "local")
            val dist2 = let(dot(local, local), "dist2")
            val delta = let(local * dist2 * barrelUniform, "delta")
            val warpedUv = let(
                float2(
                    saturate(uv.x + delta.x),
                    saturate(uv.y + delta.y)
                ),
                "warped_uv"
            )
            val base = let(sampleUv(warpedUv), "base")
            val edge = let(length(aspectCenteredUv(uv, resolution)), "edge")
            val edgeAmt = let(saturate(pow(edge, 1.35f)), "edge_amt")
            val px = let((5f + edgeAmt * 11f) * aberrationUniform / max(resolution.x, 0.0001f), "px")
            val r = sampleUv(warpedUv - float2(px, 0f)).r
            val g = base.g
            val b = sampleUv(warpedUv + float2(px, 0f)).b
            val split = let(color(r, g, b, base.a), "split")
            val rgb = let(mix(base, split, edgeAmt * 0.88f), "rgb")
            val scanLine = let(
                scanlines(fragCoord.y + sin(timeUniform * 1.1f) * 1.4f, 3.4f, 0.85f),
                "scan_line"
            )
            val scanMod = let(0.72f + 0.28f * scanLine * scanUniform, "scan_mod")
            val flicker = let(0.97f + 0.03f * sin(timeUniform * 8.7f), "flicker")
            val vignette = let(1f - edge * 0.38f, "vignette")
            rgb * scanMod * flicker * vignette
        }
        CrtTerminalSetup(
            effect = effect,
            time = timeParam!!,
            barrel = barrelParam!!,
            aberration = aberrationParam!!,
            scan = scanParam!!,
        )
    }

    val fx = rememberFxController(setup.effect)
    fx.bindTime(setup.time, isPlaying = playing)
    fx.bindFloat(setup.barrel, barrelUi / 100f)
    fx.bindFloat(setup.aberration, aberrationUi / 100f)
    fx.bindFloat(setup.scan, scanUi / 100f)

    DemoLayout(
        generatedAgsl = rememberGeneratedAgsl(setup.effect),
        preview = {
            DemoPreviewStage(modifier = Modifier.redbyteFx(fx))
        },
        controls = {
            SwitchRow("Play", playing) {
                playing = it
            }
            SliderRow("Barrel", barrelUi, 0f..80f) {
                barrelUi = it
            }
            SliderRow("RGB split", aberrationUi, 0f..100f) {
                aberrationUi = it
            }
            SliderRow("Scanlines", scanUi, 0f..100f) {
                scanUi = it
            }
        }
    )
}
