package ru.redbyte.redbytefx

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class FragmentOutputTest {

    @Test
    fun twoFragmentOutputsAreSpelledWithLocations() {
        val program = shader(ShaderTarget.Gles30) {
            vertex { glPosition(attributeVec4("position")) }
            fragment {
                val albedo = outVec4("albedo", 0)
                val glow = outVec4("glow", 1)
                albedo.set(vec4(1f.lit, 0f.lit, 0f.lit, 1f.lit))
                glow.set(vec4(0f.lit, 1f.lit, 0f.lit, 1f.lit))
                vec4(0f.lit, 0f.lit, 0f.lit, 1f.lit)
            }
        }
        val fragment = program.fragmentSource()
        assertTrue(fragment.contains("layout(location = 0) out highp vec4 albedo;"))
        assertTrue(fragment.contains("layout(location = 1) out highp vec4 glow;"))
        assertTrue(fragment.contains("albedo = vec4(1.0, 0.0, 0.0, 1.0);"))
        assertTrue(fragment.contains("glow = vec4(0.0, 1.0, 0.0, 1.0);"))
        assertFalse(fragment.contains("oColor"))
    }

    @Test
    fun agslRejectsAFragmentOutput() {
        val error = assertThrows(AuthoringException::class.java) {
            shader(ShaderTarget.Agsl) {
                fragment {
                    outVec4("albedo", 0)
                    vec4(0f.lit, 0f.lit, 0f.lit, 1f.lit)
                }
            }
        }
        assertEquals(AuthoringCode.FragmentOutOnAgsl, error.code)
    }

    @Test
    fun anOutputIsWrittenOnce() {
        assertThrows(IllegalArgumentException::class.java) {
            shader(ShaderTarget.Gles30) {
                vertex { glPosition(attributeVec4("position")) }
                fragment {
                    val albedo = outVec4("albedo", 0)
                    val color = vec4(1f.lit, 0f.lit, 0f.lit, 1f.lit)
                    albedo.set(color)
                    albedo.set(color)
                    color
                }
            }
        }
        val missing = assertThrows(ProgramException::class.java) {
            shader(ShaderTarget.Gles30) {
                vertex { glPosition(attributeVec4("position")) }
                fragment {
                    outVec4("albedo", 0)
                    vec4(0f.lit, 0f.lit, 0f.lit, 1f.lit)
                }
            }
        }
        assertEquals(ProgramCode.FragmentOutNotWritten, missing.code)
    }
}
