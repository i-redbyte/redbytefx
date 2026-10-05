package ru.redbyte.redbytefx

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

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
    fun uncalledFragmentFunctionStillDeclaresItsUniformBlock() {
        val program = shader(ShaderTarget.Gles30) {
            uniformBlock("frame") {
                val gain = float("gain")
                vertex { glPosition(attributeVec4("position")) }
                fragment {
                    fn { vec4(gain, gain, gain, 1f.lit) }
                    vec4(0f.lit, 0f.lit, 0f.lit, 1f.lit)
                }
            }
        }
        val fragment = program.fragmentSource()
        assertTrue(fragment.contains("layout(std140) uniform frame {"))
        assertTrue(fragment.contains("b_frame.gain"))
    }

    @Test
    fun theBlockIsSpelledOnlyInTheStageThatReadsIt() {
        val program = shader(ShaderTarget.Gles30) {
            uniformBlock("frame") {
                val time = float("time")
                vec3("color")
                vertex { glPosition(attributeVec4("position")) }
                fragment { vec4(time, time, time, 1f.lit) }
            }
        }
        val fragment = program.fragmentSource()
        assertTrue(fragment.contains("layout(std140) uniform frame {"))
        assertFalse(fragment.contains("binding"))
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
    }

    @Test
    fun twoUniformBlocksBindInDeclarationOrder() {
        lateinit var frame: UniformBlock
        lateinit var color: UniformBlock
        lateinit var time: HighFloat
        lateinit var rgb: HighVec3
        val program = shader(ShaderTarget.Gles30) {
            frame = uniformBlock("frame") { time = float("time") }
            color = uniformBlock("color") { rgb = vec3("rgb") }
            vertex { glPosition(attributeVec4("position")) }
            fragment { vec4(rgb.x, rgb.y, rgb.z, time) }
        }
        assertEquals(0, frame.binding)
        assertEquals(1, color.binding)
        val fragment = program.fragmentSource()
        assertTrue(fragment.contains("layout(std140) uniform frame {"))
        assertTrue(fragment.contains("layout(std140) uniform color {"))
        assertFalse(fragment.contains("binding"))
        val es32 = shader(ShaderTarget.Gles32) {
            uniformBlock("frame") {
                val time = float("time")
                vertex { glPosition(attributeVec4("position")) }
                fragment { vec4(time, time, time, 1f.lit) }
            }
        }
        assertTrue(es32.fragmentSource().contains("layout(std140, binding = 0) uniform frame {"))
        assertFalse(program.vertexSource().contains("uniform color"))
        assertEquals("frame", program.uniformBlock?.name)
        assertEquals(listOf(0, 1), program.uniformBlocks.map { it.binding })

        assertThrows(IllegalArgumentException::class.java) {
            shader(ShaderTarget.Gles30) {
                uniformBlock("frame") { float("time") }
                uniformBlock("frame") { float("gain") }
                vertex { glPosition(attributeVec4("position")) }
                fragment { vec4(0f.lit, 0f.lit, 0f.lit, 1f.lit) }
            }
        }
    }

    @Test
    fun std140PacksAVec3ArrayAndAMatrixWithoutPadding() {
        lateinit var wide: UniformBlock
        val spelled = shader(ShaderTarget.Gles30) {
            wide = uniformBlock("wide") {
                val colors = vec3Array("colors", 2)
                val basis = mat2("basis")
                vertex { glPosition(attributeVec4("position")) }
                fragment {
                    val tint = colors[0.intLit]
                    val moved = basis * vec2(tint.x, tint.y)
                    vec4(moved.x, moved.y, tint.z, 1f.lit)
                }
            }
        }
        assertEquals(0, wide.offsets[0])
        assertEquals(32, wide.offsets[1])
        assertEquals(64, wide.byteSize)
        val packed = packStd140(
            wide,
            floatArrayOf(1f, 2f, 3f, 4f, 5f, 6f, 7f, 8f, 9f, 10f),
        )
        val into = FloatArray(10)
        assertEquals(10, unpackStd140(wide, ByteBuffer.wrap(packed), into))
        assertEquals(1f, into[0], 0f)
        assertEquals(6f, into[5], 0f)
        assertEquals(7f, into[6], 0f)
        assertEquals(10f, into[9], 0f)
        val view = ByteBuffer.wrap(packed).order(ByteOrder.nativeOrder())
        assertEquals(0f, view.getFloat(12), 0f)
        assertEquals(4f, view.getFloat(16), 0f)
        assertEquals(7f, view.getFloat(32), 0f)
        assertEquals(8f, view.getFloat(36), 0f)
        assertEquals(0f, view.getFloat(40), 0f)
        assertEquals(9f, view.getFloat(48), 0f)
        assertTrue(spelled.fragmentSource().contains("highp vec3 colors[2];"))
        assertTrue(spelled.fragmentSource().contains("highp mat2 basis;"))
        assertTrue(spelled.fragmentSource().contains("b_wide.colors[0]"))
    }
}
