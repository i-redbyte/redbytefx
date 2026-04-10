package ru.redbyte.redbytefx

import java.io.File
import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Full AGSL snapshot tests: catch unintended compiler output changes while keeping substring tests
 * for targeted regressions elsewhere.
 */
class AgslGoldenTest {

    @Test
    fun passThroughSampleMatchesGolden() {
        val effect = redbytefx { sample() }
        assertGolden("pass_through.agsl", effect.agslSource())
    }

    @Test
    fun waveOffsetSampleMatchesGolden() {
        val effect = redbytefx {
            val amp = uniformFloat(12f, "amp")
            val freq = uniformFloat(0.08f, "freq")
            val waveOffset = float2(0f, sin(fragCoord.x * freq) * amp)
            sample(fragCoord + waveOffset)
        }
        assertGolden("wave_offset.agsl", effect.agslSource())
    }

    @Test
    fun namedUserFunctionMatchesGolden() {
        val effect = redbytefx {
            val amount = uniformFloat(0.35f, "amount")
            val palette = fn(
                name = "palette_rgb",
                arg1 = FloatType,
                arg2 = FloatType,
                returns = Float3Type
            ) { tone, warmth ->
                float3(
                    tone,
                    mix(tone, 1f - tone, warmth),
                    1f - tone
                )
            }

            val rgb = let(palette(amount, amount), "rgb")
            val rgba = let(float4(rgb, 1f), "rgba")
            color(rgba)
        }
        assertGolden("user_function_palette.agsl", effect.agslSource())
    }

    private fun assertGolden(resourceName: String, actual: String) {
        val path = "/golden/$resourceName"
        val expected = javaClass.getResourceAsStream(path)?.use { it.reader().readText() }
            ?: error("Missing golden resource: $path")
        assertEquals(normalize(expected), normalize(actual))
    }

    private fun normalize(text: String): String = text.replace("\r\n", "\n").trimEnd()

    /** Run locally once to refresh golden files under src/test/resources/golden/. */
    @Test
    fun dumpGoldenFilesToBuildDir() {
        val outDir = File("build/golden-dump").apply { mkdirs() }
        fun dump(name: String, source: String) = File(outDir, name).writeText(source)

        dump("pass_through.agsl", redbytefx { sample() }.agslSource())
        dump(
            "wave_offset.agsl",
            redbytefx {
                val amp = uniformFloat(12f, "amp")
                val freq = uniformFloat(0.08f, "freq")
                val waveOffset = float2(0f, sin(fragCoord.x * freq) * amp)
                sample(fragCoord + waveOffset)
            }.agslSource()
        )
        dump(
            "user_function_palette.agsl",
            redbytefx {
                val amount = uniformFloat(0.35f, "amount")
                val palette = fn(
                    name = "palette_rgb",
                    arg1 = FloatType,
                    arg2 = FloatType,
                    returns = Float3Type
                ) { tone, warmth ->
                    float3(
                        tone,
                        mix(tone, 1f - tone, warmth),
                        1f - tone
                    )
                }

                val rgb = let(palette(amount, amount), "rgb")
                val rgba = let(float4(rgb, 1f), "rgba")
                color(rgba)
            }.agslSource()
        )
    }
}
