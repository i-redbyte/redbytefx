package ru.redbyte.redbytefx

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class UniformBlockTest {

    @Test
    fun std140PlacesVectorsOnTheirBaseAlignment() {
        lateinit var frame: UniformBlock
        shader(ShaderTarget.Gles30) {
            frame = uniformBlock("frame") {
                float("time")
                vec2("scale")
                vec4("clip")
            }
            vertex { glPosition(attributeVec4("position")) }
            fragment { vec4(0f.lit, 0f.lit, 0f.lit, 1f.lit) }
        }
        assertEquals(0, frame.offsets[0])
        assertEquals(8, frame.offsets[1])
        assertEquals(16, frame.offsets[2])
        assertEquals(32, frame.byteSize)

        lateinit var colorBlock: UniformBlock
        shader(ShaderTarget.Gles30) {
            colorBlock = uniformBlock("color") {
                vec3("rgb")
                float("gain")
            }
            vertex { glPosition(attributeVec4("position")) }
            fragment { vec4(0f.lit, 0f.lit, 0f.lit, 1f.lit) }
        }
        assertEquals(0, colorBlock.offsets[0])
        assertEquals(12, colorBlock.offsets[1])
        assertEquals(16, colorBlock.byteSize)
    }

    @Test
    fun theBlockIsSpelledOnlyInTheStageThatReadsIt() {
        lateinit var time: Expr<Flt<High>>
        val program = shader(ShaderTarget.Gles30) {
            uniformBlock("frame") {
                time = float("time")
                vec3("color")
            }
            vertex { glPosition(attributeVec4("position")) }
            fragment { vec4(time, time, time, 1f.lit) }
        }
        val fragment = program.fragmentSource()
        assertTrue(fragment.contains("layout(std140) uniform frame {"))
        assertTrue(fragment.contains("highp float time;"))
        assertTrue(fragment.contains("highp vec3 color;"))
        assertTrue(fragment.contains("b_frame.time"))
        assertFalse(program.vertexSource().contains("uniform frame"))
    }

    @Test
    fun agslAndASecondBlockAreRejected() {
        val agsl = assertThrows(AuthoringException::class.java) {
            shader(ShaderTarget.Agsl) {
                uniformBlock("frame") { float("time") }
                fragment { vec4(0f.lit, 0f.lit, 0f.lit, 1f.lit) }
            }
        }
        assertEquals(AuthoringCode.UniformBlockOnAgsl, agsl.code)

        assertThrows(IllegalArgumentException::class.java) {
            shader(ShaderTarget.Gles30) {
                uniformBlock("frame") { float("time") }
                uniformBlock("other") { float("gain") }
                vertex { glPosition(attributeVec4("position")) }
                fragment { vec4(0f.lit, 0f.lit, 0f.lit, 1f.lit) }
            }
        }
    }
}
