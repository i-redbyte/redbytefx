package ru.redbyte.redbytefx

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class BuiltinCallTest {

    @Test
    fun intrinsicsSpellWithTheSameCallInAgslAndGlsl() {
        val agsl = shader(ShaderTarget.Agsl) {
            val t = uniform("t", 0f)
            fragment {
                val wave = saturate(sin(t.expr))
                vec4(wave, wave, wave, 1f.lit)
            }
        }
        val agslSource = agsl.agslSource()
        assertTrue(agslSource.contains("clamp(sin(u_t), 0.0, 1.0)"))

        val glsl = shader(ShaderTarget.Gles30) {
            val t = uniform("t", 0f)
            vertex { glPosition(attributeVec4("position")) }
            fragment {
                val wave = step(0.5f.lit, mix(0f.lit, 1f.lit, saturate(sin(t.expr))))
                vec4(wave, wave, wave, 1f.lit)
            }
        }
        val fragment = glsl.fragmentSource()
        assertTrue(fragment.contains("step(0.5, mix(0.0, 1.0, clamp(sin(u_t), 0.0, 1.0)))"))
    }

    @Test
    fun lengthAndDotReduceAVectorToAScalar() {
        val program = shader(ShaderTarget.Gles30) {
            vertex { glPosition(attributeVec4("position")) }
            fragment {
                val span = length(vec2(0f.lit, 1f.lit))
                val shade = dot(vec2(span, 0f.lit), vec2(1f.lit, 0f.lit))
                vec4(shade, shade, shade, 1f.lit)
            }
        }
        val fragment = program.fragmentSource()
        assertTrue(fragment.contains("length(vec2(0.0, 1.0))"))
        assertTrue(fragment.contains("dot(vec2("))
    }

    @Test
    fun aVaryingReadInsideACallMustBeWritten() {
        val error = assertThrows(ProgramException::class.java) {
            shader(ShaderTarget.Gles30) {
                val uv = varyingVec2("uv")
                vertex { glPosition(attributeVec4("position")) }
                fragment {
                    val wave = sin(uv.expr.x)
                    vec4(wave, wave, wave, 1f.lit)
                }
            }
        }
        assertEquals(ProgramCode.VaryingNotWritten, error.code)
    }

    @Test
    fun mismatchedShapesAndNonFloatsAreRejected() {
        assertThrows(IllegalArgumentException::class.java) { sin(1.intLit) }
        assertThrows(IllegalArgumentException::class.java) { min(1f.lit, 1f.med) }
        assertThrows(IllegalArgumentException::class.java) {
            clamp(1f.lit, 0f.lit, vec2(0f.lit, 1f.lit))
        }
    }
}
