package ru.redbyte.redbytefx.sample.ui.demos

import androidx.compose.runtime.setValue
import androidx.compose.runtime.getValue
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import ru.redbyte.redbytefx.*
import ru.redbyte.redbytefx.compose.bindFloat
import ru.redbyte.redbytefx.compose.bindTime
import ru.redbyte.redbytefx.compose.rememberFxController
import ru.redbyte.redbytefx.stdlib.aspectCenteredUv
import ru.redbyte.redbytefx.stdlib.blendScreen
import ru.redbyte.redbytefx.stdlib.domainWarp
import ru.redbyte.redbytefx.stdlib.normalizedUv
import ru.redbyte.redbytefx.stdlib.rimLight
import ru.redbyte.redbytefx.stdlib.sampleUv

import ru.redbyte.redbytefx.sample.ui.DemoLayout
import ru.redbyte.redbytefx.sample.ui.DemoLiquidGlassPreviewStage
import ru.redbyte.redbytefx.sample.ui.SliderRow
import ru.redbyte.redbytefx.sample.ui.SwitchRow


private data class LiquidGlassSetup(
    val effect: ru.redbyte.redbytefx.FxEffect,
    val time: FxParam.Float,
    val refraction: FxParam.Float,
    val speed: FxParam.Float,
    val chromaPx: FxParam.Float,
    val chromaMix: FxParam.Float,
    val edgeMix: FxParam.Float
)

@Composable
fun DemoLiquidGlass() {
    var playing by rememberSaveable { mutableStateOf(true) }
    var refractionUi by rememberSaveable { mutableFloatStateOf(7.2f) }
    var speedUi by rememberSaveable { mutableFloatStateOf(48f) }
    var chromaPxUi by rememberSaveable { mutableFloatStateOf(5.5f) }
    var chromaMixUi by rememberSaveable { mutableFloatStateOf(52f) }
    var edgeMixUi by rememberSaveable { mutableFloatStateOf(72f) }

    val setup = remember {
        var timeParam: FxParam.Float? = null
        var refractionParam: FxParam.Float? = null
        var speedParam: FxParam.Float? = null
        var chromaPxParam: FxParam.Float? = null
        var chromaMixParam: FxParam.Float? = null
        var edgeMixParam: FxParam.Float? = null
        val effect = redbytefx {
            val time by autoUniformTime()
            val refraction by autoUniformFloat(0.072f)
            val speed by autoUniformFloat(0.48f)
            val chromaPx by autoUniformFloat(5.5f)
            val chromaMix by autoUniformFloat(0.52f)
            val edgeMix by autoUniformFloat(0.72f)
            timeParam = time
            refractionParam = refraction
            speedParam = speed
            chromaPxParam = chromaPx
            chromaMixParam = chromaMix
            edgeMixParam = edgeMix

            val uv = let(normalizedUv(), "uv")
            val warp = let(domainWarp(uv * 3.2f, time * speed, refraction), "warp")
            val warpedUv = let(
                float2(saturate(warp.x), saturate(warp.y)),
                "warped_uv"
            )
            val glass = let(sampleUv(warpedUv), "glass")
            val px = chromaPx / max(resolution.x, 0.0001f)
            val r = sampleUv(warpedUv - float2(px, 0f)).r
            val g = glass.g
            val b = sampleUv(warpedUv + float2(px, 0f)).b
            val chromaGlass = let(color(r, g, b, glass.a), "chroma_glass")
            val dist = length(aspectCenteredUv(uv, resolution))
            val shell = let(1f - smoothstep(0.38f, 0.52f, dist), "shell")
            val rim = let(
                rimLight(
                    uv = uv,
                    resolution = resolution,
                    radius = 0.4f,
                    width = 0.068f,
                    feather = 0.032f
                ),
                "rim"
            )
            val edge = let(saturate(max(shell, rim)), "edge")
            val spec = let(pow(edge, 1.75f), "spec")
            val glassBody = let(mix(glass, chromaGlass, edge * chromaMix), "glass_body")
            val ice = color(float3(0.82f, 0.92f, 1f), 0.14f)

            blendScreen(glassBody, ice, spec * edgeMix)
        }
        LiquidGlassSetup(
            effect = effect,
            time = timeParam!!,
            refraction = refractionParam!!,
            speed = speedParam!!,
            chromaPx = chromaPxParam!!,
            chromaMix = chromaMixParam!!,
            edgeMix = edgeMixParam!!
        )
    }

    val fx = rememberFxController(setup.effect)
    fx.bindTime(setup.time, isPlaying = playing)
    fx.bindFloat(setup.refraction, refractionUi / 100f)
    fx.bindFloat(setup.speed, speedUi / 100f)
    fx.bindFloat(setup.chromaPx, chromaPxUi)
    fx.bindFloat(setup.chromaMix, chromaMixUi / 100f)
    fx.bindFloat(setup.edgeMix, edgeMixUi / 100f)

    DemoLayout(
        generatedAgsl = rememberGeneratedAgsl(setup.effect),
        preview = {
            DemoLiquidGlassPreviewStage(fx = fx)
        },
        controls = {
            SwitchRow("Play", playing) {
                playing = it
            }
            SliderRow("Refraction", refractionUi, 1.5f..14f) {
                refractionUi = it
            }
            SliderRow("Flow", speedUi, 15f..95f) {
                speedUi = it
            }
            SliderRow("Chroma px", chromaPxUi, 0f..14f) {
                chromaPxUi = it
            }
            SliderRow("Edge RGB", chromaMixUi, 0f..100f) {
                chromaMixUi = it
            }
            SliderRow("Ice edge", edgeMixUi, 0f..100f) {
                edgeMixUi = it
            }
        }
    )
}
