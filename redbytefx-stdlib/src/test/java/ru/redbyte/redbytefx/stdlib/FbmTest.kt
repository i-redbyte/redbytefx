package ru.redbyte.redbytefx.stdlib

import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.redbyte.redbytefx.ShaderTarget
import ru.redbyte.redbytefx.div
import ru.redbyte.redbytefx.float2
import ru.redbyte.redbytefx.lit
import ru.redbyte.redbytefx.shader
import ru.redbyte.redbytefx.vec4

class FbmTest {

    @Test
    fun fbmRejectsOctavesOutsideItsDocumentedRange() {
        for (octaves in listOf(0, 7)) {
            assertThrows(IllegalArgumentException::class.java) {
                fbm(float2(0.2f, 0.4f), octaves)
            }
            assertThrows(IllegalArgumentException::class.java) {
                fbm(float2(0.2f, 0.4f), octaves, 2f, 0.5f)
            }
        }
    }

    @Test(timeout = 10_000)
    fun layeredNoiseCompilesToCompactSource() {
        val source = shader(ShaderTarget.Gles30) {
            fragment {
                val value = fbm(domainWarp(fragCoord / resolution), 6)
                vec4(value, value, value, 1f.lit)
            }
        }.fragmentSource()

        assertTrue("source has ${source.length} chars", source.length < 60_000)
    }
}
