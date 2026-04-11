package ru.redbyte.redbytefx.stdlib

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Ignore
import org.junit.Test
import ru.redbyte.redbytefx.*

class FxStdlibGoldenTest {

    @Test
    fun coordinatePipelineMatchesGolden() {
        assertGolden("coordinate_pipeline.agsl", coordinatePipelineEffect().agslSource())
    }

    @Test
    fun signalMaskingMatchesGolden() {
        assertGolden("signal_masking.agsl", signalMaskingEffect().agslSource())
    }

    private fun coordinatePipelineEffect() = redbytefx {
        val amount = uniformFloat(0.42f, "amount")
        val center = uniformFloat2(0.38f, 0.58f, "center")
        val uv = let(normalizedUv(), "uv")
        val local = let(centeredUv(uv, center), "local")
        val reread = let(sampleUv(uv), "reread")
        val ramp = let(
            linearRamp(
                uv = uv,
                direction = float2(1f, -0.2f),
                start = 0.15f,
                end = 0.85f
            ),
            "ramp"
        )
        val fade = let(edgeFade(uv, 0.12f), "fade")

        color(
            local.x + 0.5f,
            local.y + 0.5f,
            mix(reread.b, ramp, amount) * fade,
            amount
        )
    }

    private fun signalMaskingEffect() = redbytefx {
        val time = uniformTime(name = "time")
        val density = uniformFloat(7.5f, "density")
        val warp = uniformFloat(0.028f, "warp")
        val amount = uniformFloat(0.82f, "amount")
        val base = let(sample(), "base")
        val uv = let(normalizedUv(), "uv")
        val driftUv = let(
            scanWarp(
                uv = uv,
                time = time,
                amplitude = warp,
                density = density,
                speed = 2.2f,
                noiseAmount = 0.55f
            ),
            "drift_uv"
        )
        val drifted = let(sampleUv(driftUv), "drifted")
        val bars = let(
            signalBars(
                position = uv.y,
                density = density,
                width = 0.28f,
                phase = time * 0.65f,
                feather = 0.1f
            ),
            "bars"
        )
        val lock = let(
            bandMask(
                position = uv.y,
                center = 0.36f,
                width = 0.14f,
                feather = 0.08f
            ),
            "lock"
        )

        maskedMix(base, drifted, max(bars, lock), amount)
    }

    private fun assertGolden(resourceName: String, actual: String) {
        val path = "/golden/$resourceName"
        val expected = javaClass.getResourceAsStream(path)?.use { it.reader().readText() }
            ?: error("Missing golden resource: $path")
        assertEquals(normalize(expected), normalize(actual))
    }

    private fun normalize(text: String): String = text.replace("\r\n", "\n").trimEnd()

    @Ignore("Utility-only snapshot refresh helper.")
    @Test
    fun dumpGoldenFilesToBuildDir() {
        val outDir = File("build/golden-dump").apply { mkdirs() }
        fun dump(name: String, source: String) = File(outDir, name).writeText(source)

        dump("coordinate_pipeline.agsl", coordinatePipelineEffect().agslSource())
        dump("signal_masking.agsl", signalMaskingEffect().agslSource())
    }
}
