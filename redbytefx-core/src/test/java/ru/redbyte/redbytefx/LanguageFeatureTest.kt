package ru.redbyte.redbytefx

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class LanguageFeatureTest {

    @Test
    fun sqrtDiscardDerivativesAndRepeatSpellInTheFragment() {
        val program = shader(ShaderTarget.Agsl) {
            fragment {
                val root = sqrt(fragCoord.x)
                repeat(4) { discard() }
                vec4(root, dFdx(root), dFdy(root), fwidth(root))
            }
        }
        val source = program.agslSource()
        assertTrue(source.contains("sqrt("))
        assertTrue(source.contains("dFdx("))
        assertTrue(source.contains("dFdy("))
        assertTrue(source.contains("fwidth("))
        assertTrue(source.contains("discard;"))
        assertTrue(source.contains("for (int i = 0; i < 4; ++i)"))
        assertFalse(source.contains("pow(max("))

        val glsl = shader(ShaderTarget.Gles30) {
            vertex { glPosition(attributeVec4("position")) }
            fragment {
                val root = sqrt(1f.lit)
                discard()
                vec4(root, fwidth(root), 0f.lit, 1f.lit)
            }
        }
        val fragment = glsl.fragmentSource()
        assertTrue(fragment.contains("sqrt(1.0)"))
        assertTrue(fragment.contains("discard;"))
        assertTrue(fragment.contains("fwidth("))
    }

    @Test
    fun derivativesAndDiscardStayInTheFragment() {
        val derivative = assertThrows(AuthoringException::class.java) {
            shader(ShaderTarget.Gles30) {
                vertex {
                    glPosition(vec4(dFdx(1f.lit), 0f.lit, 0f.lit, 1f.lit))
                }
                fragment { vec4(0f.lit, 0f.lit, 0f.lit, 1f.lit) }
            }
        }
        assertEquals(AuthoringCode.DerivativeOutsideFragment, derivative.code)

        val outside = assertThrows(AuthoringException::class.java) { fwidth(1f.lit) }
        assertEquals(AuthoringCode.DerivativeOutsideFragment, outside.code)
    }

    @Test
    fun uniformLookupUsesTheAuthorNameAndTheSpelledName() {
        val program = shader(ShaderTarget.Gles30) {
            uniform("amount", 1f)
            uniformVec3("tint", 1f, 2f, 3f)
            vertex { glPosition(attributeVec4("position")) }
            fragment { vec4(0f.lit, 0f.lit, 0f.lit, 1f.lit) }
        }
        assertEquals(1f, program.floatUniform("amount").default)
        assertEquals(1f, program.floatUniform("u_amount").default)
        assertEquals(3, program.vec3Uniform("tint").components!!.size)
        val mismatch = assertThrows(IllegalArgumentException::class.java) {
            program.floatUniform("tint")
        }
        assertTrue(mismatch.message!!.contains("shape"))
    }

    @Test
    fun repeatRejectsACountOutsideOneToSixtyFour() {
        assertThrows(IllegalArgumentException::class.java) {
            shader(ShaderTarget.Agsl) {
                fragment {
                    repeat(0) { }
                    vec4(0f.lit, 0f.lit, 0f.lit, 1f.lit)
                }
            }
        }
        assertThrows(IllegalArgumentException::class.java) {
            shader(ShaderTarget.Agsl) {
                fragment {
                    repeat(65) { }
                    vec4(0f.lit, 0f.lit, 0f.lit, 1f.lit)
                }
            }
        }
    }

    @Test
    fun localAccumulatesInsideRepeatAndDiscardCanBeConditional() {
        val agsl = shader(ShaderTarget.Agsl) {
            fragment {
                val acc = local(0f.lit, "acc")
                repeat(4) { acc.set(acc.expr + 1f.lit) }
                discard(acc.expr.lt(0.5f.lit))
                vec4(acc.expr, 0f.lit, 0f.lit, 1f.lit)
            }
        }
        val agslSource = agsl.agslSource()
        assertTrue(agslSource.contains("float acc;"))
        assertTrue(agslSource.contains("acc = (acc + 1.0);"))
        assertTrue(agslSource.indexOf("float acc;") < agslSource.indexOf("for (int i = 0; i < 4; ++i)"))
        assertTrue(agslSource.contains("if ((acc < 0.5)) discard;"))
        assertTrue(agslSource.contains("acc"))

        val gles = shader(ShaderTarget.Gles30) {
            vertex {
                val shift = local(0f.lit, "shift")
                glPosition(vec4(shift.expr, 0f.lit, 0f.lit, 1f.lit))
            }
            fragment {
                val acc = local(0f.lit, "acc")
                repeat(4) { acc.set(acc.expr + 1f.lit) }
                discard(acc.expr.lt(0.5f.lit))
                vec4(acc.expr, 0f.lit, 0f.lit, 1f.lit)
            }
        }
        assertTrue(gles.vertexSource().contains("highp float shift;"))
        assertTrue(gles.vertexSource().contains("gl_Position = vec4(shift, 0.0, 0.0, 1.0);"))
        val fragment = gles.fragmentSource()
        assertTrue(fragment.contains("highp float acc;"))
        assertTrue(fragment.contains("acc = (acc + 1.0);"))
        assertTrue(fragment.contains("if ((acc < 0.5)) discard;"))

        val nested = assertThrows(ProgramException::class.java) {
            shader(ShaderTarget.Agsl) {
                fragment {
                    repeat(2) { local(0f.lit) }
                    vec4(0f.lit, 0f.lit, 0f.lit, 1f.lit)
                }
            }
        }
        assertEquals(ProgramCode.LocalInsideRepeat, nested.code)
    }

    @Test
    fun intUniformReachesAgslAndMatrixBoolAndCubeStayOffIt() {
        lateinit var count: Uniform<IntS>
        val program = shader(ShaderTarget.Agsl) {
            count = uniformInt("count", 3)
            fragment { vec4(count.expr.toFloat(), 0f.lit, 0f.lit, 1f.lit) }
        }
        assertTrue(program.agslSource().contains("uniform int u_count;"))
        val writer = RecordingUniformWriter()
        val runtime = ShaderRuntime(program, writer) {}
        assertEquals(listOf(3), writer.ints.filter { it.first == "u_count" }.map { it.second })
        assertFalse(runtime.set(count, 3))
        assertEquals(1, writer.ints.count { it.first == "u_count" })

        val matrix = assertThrows(AuthoringException::class.java) {
            shader(ShaderTarget.Agsl) {
                uniformMat4("model")
                fragment { vec4(0f.lit, 0f.lit, 0f.lit, 1f.lit) }
            }
        }
        assertEquals(AuthoringCode.MatrixOnAgsl, matrix.code)
        val bool = assertThrows(AuthoringException::class.java) {
            shader(ShaderTarget.Agsl) {
                uniformBool("flag", true)
                fragment { vec4(0f.lit, 0f.lit, 0f.lit, 1f.lit) }
            }
        }
        assertEquals(AuthoringCode.BoolOnAgsl, bool.code)
        val cube = assertThrows(AuthoringException::class.java) {
            shader(ShaderTarget.Agsl) {
                samplerCube("sky")
                fragment { vec4(0f.lit, 0f.lit, 0f.lit, 1f.lit) }
            }
        }
        assertEquals(AuthoringCode.SamplerOnAgsl, cube.code)
    }

    @Test
    fun theVertexStageHasNoDiscard() {
        val names = ShaderDsl.VertexDsl::class.java.methods.map { it.name }
        assertFalse(names.contains("discard"))
    }
}
