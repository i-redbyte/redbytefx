package ru.redbyte.redbytefx

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.atomic.AtomicReference

class AgslShaderTest {

    @Test
    fun letUsedInsideConditionalAndAfterItIsDeclaredInBothScopes() {
        val main = shader(ShaderTarget.Agsl) {
            fragment {
                val board = let(fragCoord / resolution, "board")
                val boardMask = let(board.x + board.y, "board_mask")
                val active = local(0f.lit, "active")
                whenTrue(fragCoord.x gt 0f.lit) {
                    active.set(boardMask)
                }
                val result = let(active.expr + boardMask, "result")
                vec4(result, result, result, 1f.lit)
            }
        }.agslSource().substringAfter("half4 main(float2 fragCoord) {")

        assertTrue(main.contains("float2 board ="))
        assertTrue(main.contains("float2 board_1 ="))
        assertTrue(main.contains("float board_mask ="))
        assertTrue(main.contains("float board_mask_1 ="))
        assertTrue(main.indexOf("active = board_mask;") < main.indexOf("float result ="))
    }

    @Test
    fun runtimeRejectsWritesFromAnotherThreadBeforeChangingItsCache() {
        lateinit var amount: Uniform<Flt<High>>
        val program = shader(ShaderTarget.Agsl) {
            amount = uniform("amount", 0f)
            fragment { sample() }
        }
        val writer = RecordingUniformWriter()
        val runtime = ShaderRuntime(program, writer) {}
        val failure = AtomicReference<Throwable?>()
        val worker = Thread {
            try {
                runtime.set(amount, 1f)
            } catch (error: Throwable) {
                failure.set(error)
            }
        }
        worker.start()
        worker.join(5_000)
        assertFalse(worker.isAlive)
        assertTrue(failure.get() is IllegalStateException)
        assertFalse(runtime.set(amount, 0f))
        assertEquals(listOf(0f), writer.floatValues("u_amount"))
    }

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
        assertEquals(1, agsl.split("uniform float2 uResolution").size - 1)
        assertNotNull(program.resolution)
        assertTrue(agsl.contains("uniform float u_amount;"))
        assertEquals(1, agsl.split("float2 shifted").size - 1)
        assertTrue(agsl.contains("float2 shifted = (fragCoord + float2(u_amount, 0.0));"))
        assertEquals(1, agsl.split("half4 base").size - 1)
        assertTrue(agsl.contains("return (base + base);"))
        assertSame(amount, program.floatUniform("u_amount"))
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
    fun failedAgslUniformWriteCanBeRetried() {
        lateinit var amount: Uniform<Flt<High>>
        val program = shader(ShaderTarget.Agsl) {
            amount = uniform("amount", 0f)
            fragment { sample() }
        }
        val delegate = RecordingUniformWriter()
        var failNext = false
        val writer = object : UniformWriter by delegate {
            override fun setFloat(name: String, value: Float) {
                if (failNext) {
                    failNext = false
                    error("writer failed")
                }
                delegate.setFloat(name, value)
            }
        }
        val runtime = ShaderRuntime(program, writer) {}
        failNext = true
        assertThrows(IllegalStateException::class.java) { runtime.set(amount, 0.5f) }
        assertTrue(runtime.set(amount, 0.5f))
        assertFalse(runtime.set(amount, 0.5f))
        assertEquals(listOf(0f, 0.5f), delegate.floatValues("u_amount"))
    }

    @Test
    fun differentNaNPayloadsReachTheUniformWriter() {
        lateinit var amount: Uniform<Flt<High>>
        val program = shader(ShaderTarget.Agsl) {
            amount = uniform("amount", 0f)
            fragment { sample() }
        }
        val writer = RecordingUniformWriter()
        val runtime = ShaderRuntime(program, writer) {}
        val first = Float.fromBits(0x7fc00001)
        val second = Float.fromBits(0x7fc00002)

        assertTrue(runtime.set(amount, first))
        assertFalse(runtime.set(amount, first))
        assertTrue(runtime.set(amount, second))
        assertEquals(3, writer.floatValues("u_amount").size)
    }

    @Test
    fun failedEffectRefreshCanBeRetriedWithoutRewritingTheUniform() {
        lateinit var amount: Uniform<Flt<High>>
        val program = shader(ShaderTarget.Agsl) {
            amount = uniform("amount", 0f)
            fragment { sample() }
        }
        val writer = RecordingUniformWriter()
        var failNextRefresh = false
        var refreshes = 0
        val runtime = ShaderRuntime(program, writer) {
            if (failNextRefresh) {
                failNextRefresh = false
                error("effect refresh failed")
            }
            refreshes++
        }
        failNextRefresh = true
        assertThrows(IllegalStateException::class.java) { runtime.set(amount, 0.5f) }
        assertTrue(runtime.set(amount, 0.5f))
        assertFalse(runtime.set(amount, 0.5f))
        assertEquals(listOf(0f, 0.5f), writer.floatValues("u_amount"))
        assertEquals(2, refreshes)
    }

    @Test
    fun aBatchRetryReportsThePendingEffectRefresh() {
        lateinit var amount: Uniform<Flt<High>>
        val program = shader(ShaderTarget.Agsl) {
            amount = uniform("amount", 0f)
            fragment { sample() }
        }
        var failNextRefresh = false
        val runtime = ShaderRuntime(program, RecordingUniformWriter()) {
            if (failNextRefresh) {
                failNextRefresh = false
                error("effect refresh failed")
            }
        }
        failNextRefresh = true
        assertThrows(IllegalStateException::class.java) {
            runtime.batch { runtime.set(amount, 0.5f) }
        }
        runtime.batch { assertTrue(runtime.set(amount, 0.5f)) }
        assertFalse(runtime.set(amount, 0.5f))
    }

    @Test
    fun agslRejectsHandlesFromAnotherProgram() {
        lateinit var foreignUniform: HighFloatUniform
        lateinit var foreignFunction: Fn1<Flt<High>, Flt<High>>
        shader(ShaderTarget.Agsl) {
            foreignUniform = uniform("gain", 1f)
            fragment {
                foreignFunction = fn(0f.lit, "boost") { value -> value + 1f.lit }
                sample()
            }
        }

        val uniformError = assertThrows(ProgramException::class.java) {
            shader(ShaderTarget.Agsl) {
                fragment {
                    val gain = foreignUniform.expr
                    vec4(gain, gain, gain, 1f.lit)
                }
            }
        }
        assertEquals(ProgramCode.ForeignUniform, uniformError.code)

        val functionError = assertThrows(ProgramException::class.java) {
            shader(ShaderTarget.Agsl) {
                fragment {
                    local(foreignFunction(0f.lit))
                    sample()
                }
            }
        }
        assertEquals(ProgramCode.ForeignFunction, functionError.code)
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
        assertFalse(runtime.setResolution(Float.POSITIVE_INFINITY, 3f))
        assertEquals(1f, writer.float2Values("uResolution").last().first)
        assertEquals(3f, writer.float2Values("uResolution").last().second)
        assertNotNull(program.resolution)
    }

    @Test
    fun vectorUniformDefaultIsWrittenOnce() {
        lateinit var offset: Uniform<Vec2<Flt<High>>>
        val program = shader(ShaderTarget.Agsl) {
            offset = uniformVec2("offset", 0.25f, 0.5f)
            fragment { sample() }
        }
        offset.components!![0] = 9f
        val writer = RecordingUniformWriter()
        val runtime = ShaderRuntime(program, writer) {}

        assertEquals(0.25f, offset.components!![0])
        assertEquals(listOf(0.25f to 0.5f), writer.float2Values("u_offset"))
        assertFalse(runtime.set(offset, 0.25f, 0.5f))
        assertEquals(1, writer.float2Values("u_offset").size)
        assertTrue(runtime.set(offset, 1f, 0.5f))
        assertEquals(listOf(0.25f to 0.5f, 1f to 0.5f), writer.float2Values("u_offset"))
    }

    @Test
    fun failedVectorWriteCanBeRetriedWithoutCachingTheFailedValue() {
        lateinit var offset: Uniform<Vec2<Flt<High>>>
        val program = shader(ShaderTarget.Agsl) {
            offset = uniformVec2("offset", 0f, 0f)
            fragment { sample() }
        }
        var failNext = false
        val delegate = RecordingUniformWriter()
        val writer = object : UniformWriter by delegate {
            override fun setFloat2(name: String, x: Float, y: Float) {
                if (failNext) {
                    failNext = false
                    error("writer failed")
                }
                delegate.setFloat2(name, x, y)
            }
        }
        val runtime = ShaderRuntime(program, writer) {}

        failNext = true
        assertThrows(IllegalStateException::class.java) { runtime.set(offset, 1f, 2f) }
        assertTrue(runtime.set(offset, 1f, 2f))
        assertFalse(runtime.set(offset, 1f, 2f))
        assertEquals(listOf(0f to 0f, 1f to 2f), delegate.float2Values("u_offset"))
    }

    @Test
    fun vectorAndIntDefaultsSkipRepeatedWrites() {
        lateinit var position: Uniform<Vec3<Flt<High>>>
        lateinit var color: Uniform<Vec4<Flt<High>>>
        lateinit var mode: Uniform<IntS>
        val program = shader(ShaderTarget.Agsl) {
            position = uniformVec3("position", 1f, 2f, 3f)
            color = uniformVec4("color", 0f, 0f, 0f, 1f)
            mode = uniformInt("mode", 2)
            fragment { sample() }
        }
        val writer = RecordingUniformWriter()
        val runtime = ShaderRuntime(program, writer) {}

        assertFalse(runtime.set(position, 1f, 2f, 3f))
        assertFalse(runtime.set(color, 0f, 0f, 0f, 1f))
        assertFalse(runtime.set(mode, 2))
        assertTrue(runtime.set(position, 1f, 2f, 4f))
        assertTrue(runtime.set(color, 1f, 0f, 0f, 1f))
        assertTrue(runtime.set(mode, 3))
        assertEquals(2, writer.float3Calls)
        assertEquals(2, writer.float4Calls)
        assertEquals(listOf("u_mode" to 2, "u_mode" to 3), writer.ints)
    }

    @Test
    fun proceduralShaderOmitsUnusedSamplingCodeAndFunctions() {
        val program = shader(ShaderTarget.Agsl) {
            fragment {
                fn("unused") { this@fragment.sample() }
                vec4(1f.lit, 0f.lit, 0f.lit, 1f.lit)
            }
        }
        val source = program.agslSource()

        assertFalse(source.contains("rb_maxCoord"))
        assertFalse(source.contains("rb_sample"))
        assertFalse(source.contains("unused("))
        assertTrue(source.contains("uniform shader uContent;"))
    }
}

internal class RecordingUniformWriter : UniformWriter {
    val floats = mutableListOf<Pair<String, Float>>()
    val float2s = mutableListOf<Triple<String, Float, Float>>()
    var float3Calls = 0
    var float4Calls = 0

    override fun setFloat(name: String, value: Float) {
        floats += name to value
    }

    override fun setFloat2(name: String, x: Float, y: Float) {
        float2s += Triple(name, x, y)
    }

    override fun setFloat3(name: String, x: Float, y: Float, z: Float) {
        float3Calls++
    }

    override fun setFloat4(name: String, x: Float, y: Float, z: Float, w: Float) {
        float4Calls++
    }

    override fun setInt(name: String, value: Int) {
        ints += name to value
    }

    val ints = mutableListOf<Pair<String, Int>>()

    fun floatValues(name: String): List<Float> = floats.filter { it.first == name }.map { it.second }

    fun float2Values(name: String): List<Pair<Float, Float>> =
        float2s.filter { it.first == name }.map { it.second to it.third }
}
