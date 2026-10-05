package ru.redbyte.redbytefx

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class GlslShaderTest {

    @Test
    fun spellUsesGlslNamesAndRejectsAgslChildShaders() {
        assertEquals("float", spell(1f.lit.shape, ShaderTarget.Gles30))
        assertEquals("vec2", spell(vec2(0f.lit, 1f.lit).shape, ShaderTarget.Gles30))
        assertEquals("vec4", spell(vec4(0f.med, 0f.med, 0f.med, 1f.med).shape, ShaderTarget.Gles30))
        assertEquals("mat4", spell(Shape.Matrix(4), ShaderTarget.Gles30))
        assertEquals("int", spell(1.intLit.shape, ShaderTarget.Gles30))
        assertEquals("sampler2D", spell(Shape.Sampler2D, ShaderTarget.Gles30))
        assertThrows(IllegalArgumentException::class.java) {
            spell(Shape.ChildShader, ShaderTarget.Gles30)
        }
    }

    @Test
    fun linkedProgramSpellsMatchingVertexAndFragmentInterfaces() {
        lateinit var image: Uniform<Sampler2D>
        val program = shader(ShaderTarget.Gles30) {
            val uv = varyingVec2("uv")
            image = sampler2D("image")
            vertex {
                uv.set(attributeVec2("uv"))
                glPosition(attributeVec4("position"))
            }
            fragment {
                texture(image, uv.expr)
            }
        }

        val vertex = program.vertexSource()
        val fragment = program.fragmentSource()
        assertTrue(vertex.contains("#version 300 es"))
        assertTrue(vertex.contains("in highp vec4 a_position;"))
        assertTrue(vertex.contains("in highp vec2 a_uv;"))
        assertTrue(vertex.contains("out highp vec2 v_uv;"))
        assertTrue(vertex.contains("v_uv = a_uv;"))
        assertTrue(vertex.contains("gl_Position = a_position;"))
        assertTrue(fragment.contains("uniform highp sampler2D u_image;"))
        assertTrue(fragment.contains("in highp vec2 v_uv;"))
        assertTrue(fragment.contains("out highp vec4 oColor;"))
        assertTrue(fragment.contains("texture(u_image, v_uv)"))
        assertThrows(IllegalStateException::class.java) { program.agslSource() }
    }

    @Test
    fun fragmentCannotReadAVaryingTheVertexDidNotWrite() {
        val error = assertThrows(ProgramException::class.java) {
            shader(ShaderTarget.Gles30) {
                val uv = varyingVec2("uv")
                val image = sampler2D("image")
                vertex { glPosition(attributeVec4("position")) }
                fragment { texture(image, uv.expr) }
            }
        }
        assertEquals(ProgramCode.VaryingNotWritten, error.code)
    }

    @Test
    fun glesRequiresBothStagesAndAPositionWrite() {
        val missingFragment = assertThrows(ProgramException::class.java) {
            shader(ShaderTarget.Gles30) {
                vertex { glPosition(attributeVec4("position")) }
            }
        }
        assertEquals(ProgramCode.MissingFragment, missingFragment.code)

        val missingPosition = assertThrows(ProgramException::class.java) {
            shader(ShaderTarget.Gles30) {
                vertex { attributeVec4("position") }
                fragment { vec4(0f.lit, 0f.lit, 0f.lit, 1f.lit) }
            }
        }
        assertEquals(ProgramCode.MissingGlPosition, missingPosition.code)
    }

    @Test
    fun foreignIntrinsicsStayOnTheirTarget() {
        val sampleOnGles = assertThrows(AuthoringException::class.java) {
            shader(ShaderTarget.Gles30) {
                vertex { glPosition(attributeVec4("position")) }
                fragment { sample() }
            }
        }
        assertEquals(AuthoringCode.SampleOutsideAgslFragment, sampleOnGles.code)

        val textureOnAgsl = assertThrows(AuthoringException::class.java) {
            shader(ShaderTarget.Agsl) {
                val image = sampler2D("image")
                fragment { texture(image, fragCoord) }
            }
        }
        assertEquals(AuthoringCode.SamplerOnAgsl, textureOnAgsl.code)
    }

    @Test
    fun glesFragCoordSpellsGlFragCoordAndResolutionIsUResolution() {
        val program = shader(ShaderTarget.Gles30) {
            vertex { glPosition(attributeVec4("position")) }
            fragment {
                val pixel = fn(vec2(0f.lit, 0f.lit)) { _ -> this@fragment.fragCoord }
                val coord = pixel(vec2(0f.lit, 0f.lit))
                vec4(coord.x, fragCoord.y, 0f.lit, 1f.lit)
            }
        }
        assertTrue(program.fragmentSource().contains("gl_FragCoord.xy"))
        val sized = shader(ShaderTarget.Gles30) {
            vertex { glPosition(attributeVec4("position")) }
            fragment { vec4(resolution.x, 0f.lit, 0f.lit, 1f.lit) }
        }
        assertTrue(sized.fragmentSource().contains("uResolution"))
        assertNotNull(sized.resolution)
    }

    @Test
    fun glesFragmentOnlyInjectsFullscreenCornerVertex() {
        val program = shader(ShaderTarget.Gles30) {
            fragment { vec4(0.2f.lit, 0.3f.lit, 0.4f.lit, 1f.lit) }
        }
        assertTrue(program.vertexSource().contains("a_corner"))
        assertTrue(program.vertexSource().contains("gl_Position"))
    }

    @Test
    fun unusedFragmentFunctionThatReadsResolutionStillBindsUResolution() {
        val program = shader(ShaderTarget.Gles30) {
            vertex { glPosition(attributeVec4("position")) }
            fragment {
                fn { vec4(this@fragment.resolution.x, 0f.lit, 0f.lit, 1f.lit) }
                vec4(0f.lit, 0f.lit, 0f.lit, 1f.lit)
            }
        }
        assertNotNull(program.resolution)
        assertTrue(program.fragmentSource().contains("uResolution"))
    }

    @Test
    fun authorCannotDeclareUResolution() {
        assertThrows(IllegalArgumentException::class.java) {
            shader(ShaderTarget.Gles30) {
                uniform("uResolution", 1f)
                vertex { glPosition(attributeVec4("position")) }
                fragment { vec4(0f.lit, 0f.lit, 0f.lit, 1f.lit) }
            }
        }
        assertThrows(IllegalArgumentException::class.java) {
            shader(ShaderTarget.Agsl) {
                uniformVec2("uResolution", 1f, 1f)
                fragment { sample() }
            }
        }
    }

    @Test
    fun fragmentFunctionNamedACornerDoesNotRenameTheScreenAttribute() {
        val program = shader(ShaderTarget.Gles30) {
            fragment {
                fn("a_corner") { vec4(0.2f.lit, 0.3f.lit, 0.4f.lit, 1f.lit) }
                vec4(0.2f.lit, 0.3f.lit, 0.4f.lit, 1f.lit)
            }
        }
        assertTrue(program.vertexSource().contains("in highp vec2 a_corner;"))
        assertTrue(program.fragmentSource().contains("a_corner_1"))
    }

    @Test
    fun gles32FragmentOnlyInjectsFullscreenCornerVertex() {
        val program = shader(ShaderTarget.Gles32) {
            fragment { vec4(0.2f.lit, 0.3f.lit, 0.4f.lit, 1f.lit) }
        }
        assertTrue(program.vertexSource().contains("#version 320 es"))
        assertTrue(program.vertexSource().contains("a_corner"))
    }
}
