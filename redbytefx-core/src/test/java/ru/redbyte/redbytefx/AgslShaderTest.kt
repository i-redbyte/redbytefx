package ru.redbyte.redbytefx

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class AgslShaderTest {

    @Test
    fun spellMapsClosedShapesToAgsl() {
        assertEquals("float", spell(1f.lit.shape, ShaderTarget.Agsl))
        assertEquals("half", spell(1f.med.shape, ShaderTarget.Agsl))
        assertEquals("float2", spell(vec2(0f.lit, 1f.lit).shape, ShaderTarget.Agsl))
        assertEquals(
            "half4",
            spell(vec4(0f.med, 0f.med, 0f.med, 1f.med).shape, ShaderTarget.Agsl),
        )
        assertEquals("int", spell(Shape.Scalar(ScalarKind.Int, null), ShaderTarget.Agsl))
        assertEquals("bool", spell(Shape.Scalar(ScalarKind.Bool, null), ShaderTarget.Agsl))
        assertEquals("int3", spell(Shape.Vector(ScalarKind.Int, null, 3), ShaderTarget.Agsl))
        assertEquals("float4x4", spell(Shape.Matrix(4), ShaderTarget.Agsl))
        assertEquals("shader", spell(Shape.ChildShader, ShaderTarget.Agsl))
        assertThrows(IllegalArgumentException::class.java) {
            spell(Shape.Sampler2D, ShaderTarget.Agsl)
        }
        assertEquals("float", spell(1f.lit.shape, ShaderTarget.Gles30))
        assertEquals("vec2", spell(vec2(0f.lit, 1f.lit).shape, ShaderTarget.Gles30))
    }

    @Test
    fun fragmentEmitsASingleLocalAndClampedSample() {
        lateinit var amount: Uniform<Flt<High>>
        val program = shader(ShaderTarget.Agsl) {
            amount = uniform("amount", 0.25f)
            fragment {
                val shifted = (fragCoord + vec2(amount.expr, 0f.lit)).let("shifted")
                val base = sample(shifted).let("base")
                base + base
            }
        }

        val agsl = program.agslSource()
        assertTrue(agsl.contains("uniform shader uContent;"))
        assertTrue(agsl.contains("uniform float2 uResolution;"))
        assertTrue(agsl.contains("uniform float u_amount;"))
        assertEquals(1, agsl.split("float2 shifted").size - 1)
        assertTrue(agsl.contains("float2 shifted = (fragCoord + float2(u_amount, 0.0));"))
        assertEquals(1, agsl.split("half4 base").size - 1)
        assertTrue(agsl.contains("return (base + base);"))
        assertSame(amount, program.uniform<Flt<High>>("u_amount"))
    }

    @Test
    fun agslRejectsVertexAndAMissingFragment() {
        val vertex = assertThrows(AuthoringException::class.java) {
            shader(ShaderTarget.Agsl) {
                vertex { }
            }
        }
        assertEquals(AuthoringCode.VertexOnAgsl, vertex.code)

        assertThrows(IllegalStateException::class.java) {
            shader(ShaderTarget.Agsl) { }
        }
    }

    @Test
    fun runtimeSkipsIdenticalFloatWritesAndRejectsForeignHandles() {
        lateinit var amount: Uniform<Flt<High>>
        val program = shader(ShaderTarget.Agsl) {
            amount = uniform("amount", 0.25f)
            fragment { sample() }
        }
        val writer = RecordingUniformWriter()
        var refreshes = 0
        val runtime = ShaderRuntime(program, writer) { refreshes += 1 }

        assertEquals(1, refreshes)
        assertEquals(listOf(0.25f), writer.floatValues("u_amount"))
        assertFalse(runtime.set(amount, 0.25f))
        assertEquals(1, writer.floatValues("u_amount").size)

        runtime.batch {
            assertTrue(runtime.set(amount, 0.5f))
            assertTrue(runtime.set(amount, 1f))
        }
        assertEquals(listOf(0.25f, 0.5f, 1f), writer.floatValues("u_amount"))
        assertEquals(2, refreshes)

        lateinit var foreign: Uniform<Flt<High>>
        shader(ShaderTarget.Agsl) {
            foreign = uniform("amount", 0f)
            fragment { sample() }
        }
        assertThrows(IllegalArgumentException::class.java) {
            runtime.set(foreign, 1f)
        }
    }

    @Test
    fun resolutionClampsNonPositiveComponents() {
        val program = shader(ShaderTarget.Agsl) {
            fragment { sample() }
        }
        val writer = RecordingUniformWriter()
        val runtime = ShaderRuntime(program, writer) {}

        assertTrue(runtime.setResolution(2f, 3f))
        assertTrue(runtime.setResolution(0f, 3f))
        assertFalse(runtime.setResolution(1f, 3f))
        assertEquals(1f, writer.float2Values("uResolution").last().first)
        assertEquals(3f, writer.float2Values("uResolution").last().second)
    }
}

internal class RecordingUniformWriter : UniformWriter {
    val floats = mutableListOf<Pair<String, Float>>()
    val float2s = mutableListOf<Triple<String, Float, Float>>()

    override fun setFloat(name: String, value: Float) {
        floats += name to value
    }

    override fun setFloat2(name: String, x: Float, y: Float) {
        float2s += Triple(name, x, y)
    }

    override fun setFloat3(name: String, x: Float, y: Float, z: Float) = Unit

    override fun setFloat4(name: String, x: Float, y: Float, z: Float, w: Float) = Unit

    fun floatValues(name: String): List<Float> = floats.filter { it.first == name }.map { it.second }

    fun float2Values(name: String): List<Pair<Float, Float>> =
        float2s.filter { it.first == name }.map { it.second to it.third }
}
