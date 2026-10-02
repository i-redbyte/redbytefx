package ru.redbyte.redbytefx.gl

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.redbyte.redbytefx.Flt
import ru.redbyte.redbytefx.High
import ru.redbyte.redbytefx.Sampler2D
import ru.redbyte.redbytefx.ShaderTarget
import ru.redbyte.redbytefx.Uniform
import ru.redbyte.redbytefx.lit
import ru.redbyte.redbytefx.med
import ru.redbyte.redbytefx.shader
import ru.redbyte.redbytefx.vec4

class GlProgramRuntimeTest {

    @Test
    fun linkKeepsTheProgramOnlyWhenTheDeviceReportsSuccess() {
        val device = RecordingGlDevice()
        val runtime = GlProgramRuntime(passthrough(), device)
        runtime.link()
        assertTrue(device.liveShaders.isEmpty())
        assertEquals(1, device.livePrograms.size)
        runtime.link()
        assertEquals(1, device.createProgramCalls)
    }

    @Test
    fun compileOrLinkFailureDeletesEveryNameTheDriverReturned() {
        val compile = RecordingGlDevice(failFragmentCompile = true)
        val compileRuntime = GlProgramRuntime(passthrough(), compile)
        val compileError = assertThrows(GlException::class.java) { compileRuntime.link() }
        assertEquals(GlCode.CompileFailed, compileError.code)
        assertTrue(compile.liveShaders.isEmpty())
        assertTrue(compile.livePrograms.isEmpty())

        val link = RecordingGlDevice(failLink = true)
        val linkError = assertThrows(GlException::class.java) { GlProgramRuntime(passthrough(), link).link() }
        assertEquals(GlCode.LinkFailed, linkError.code)
        assertTrue(link.liveShaders.isEmpty())
        assertTrue(link.livePrograms.isEmpty())
        val stillOpen = assertThrows(GlException::class.java) { compileRuntime.use() }
        assertEquals(GlCode.NotLinked, stillOpen.code)
    }

    @Test
    fun anotherThreadCannotTouchTheProgram() {
        val device = RecordingGlDevice()
        val runtime = GlProgramRuntime(passthrough(), device)
        val codes = mutableListOf<GlCode>()
        val worker = Thread {
            try {
                runtime.link()
            } catch (error: GlException) {
                codes += error.code
            }
        }
        worker.start()
        worker.join()
        assertEquals(listOf(GlCode.WrongThread), codes)
        assertEquals(0, device.createShaderCalls)
    }

    @Test
    fun unchangedFloatAndTextureDoNotCallTheDeviceAgain() {
        val device = RecordingGlDevice()
        lateinit var amount: Uniform<Flt<High>>
        lateinit var image: Uniform<Sampler2D>
        val program = shader(ShaderTarget.Gles30) {
            val uv = varyingVec2("uv")
            amount = uniform("amount", 0f)
            image = sampler2D("image")
            vertex {
                uv.set(attributeVec2("uv"))
                glPosition(attributeVec4("position"))
            }
            fragment { texture(image, uv.expr) }
        }
        val runtime = GlProgramRuntime(program, device)
        runtime.link()
        assertEquals(1, device.uniform1fCalls)
        assertEquals(1, device.locationQueries["u_amount"])
        assertFalse(runtime.set(amount, 0f))
        assertEquals(1, device.uniform1fCalls)
        assertTrue(runtime.set(amount, -0f))
        assertEquals(2, device.uniform1fCalls)
        assertEquals(1, device.locationQueries["u_amount"])

        assertTrue(runtime.bind(image, 7))
        assertFalse(runtime.bind(image, 7))
        assertTrue(runtime.bind(image, 8))
        assertEquals(1, device.uniform1iCalls)
        assertEquals(listOf(0, 0), device.textureUnits)
        assertEquals(listOf(7, 8), device.boundTextures)
    }

    @Test
    fun missingLocationAndForeignHandleDoNotWrite() {
        val device = RecordingGlDevice(missing = setOf("u_amount"))
        lateinit var amount: Uniform<Flt<High>>
        val program = shader(ShaderTarget.Gles30) {
            amount = uniform("amount", 1f)
            vertex { glPosition(attributeVec4("position")) }
            fragment { vec4(0f.lit, 0f.lit, 0f.lit, 1f.lit) }
        }
        val runtime = GlProgramRuntime(program, device)
        runtime.link()
        assertFalse(runtime.set(amount, 3f))
        assertEquals(0, device.uniform1fCalls)

        lateinit var foreign: Uniform<Flt<High>>
        shader(ShaderTarget.Gles30) {
            foreign = uniform("other", 1f)
            vertex { glPosition(attributeVec4("position")) }
            fragment { vec4(0f.lit, 0f.lit, 0f.lit, 1f.lit) }
        }
        assertThrows(IllegalArgumentException::class.java) { runtime.set(foreign, 1f) }
    }

    @Test
    fun destroyReleasesTheProgramAndRejectsLaterUse() {
        val device = RecordingGlDevice()
        val runtime = GlProgramRuntime(passthrough(), device)
        runtime.link()
        runtime.destroy()
        runtime.destroy()
        assertTrue(device.livePrograms.isEmpty())
        assertEquals(1, device.deleteProgramCalls)
        val error = assertThrows(GlException::class.java) { runtime.use() }
        assertEquals(GlCode.Destroyed, error.code)
    }

    @Test
    fun agslProgramIsRejectedBeforeAnyDriverCall() {
        val device = RecordingGlDevice()
        val program = shader(ShaderTarget.Agsl) {
            fragment { vec4(0f.med, 0f.med, 0f.med, 1f.med) }
        }
        val error = assertThrows(GlException::class.java) { GlProgramRuntime(program, device).link() }
        assertEquals(GlCode.WrongTarget, error.code)
        assertEquals(0, device.createShaderCalls)
    }
}

private fun passthrough() = shader(ShaderTarget.Gles30) {
    vertex { glPosition(attributeVec4("position")) }
    fragment { vec4(0f.lit, 0f.lit, 0f.lit, 1f.lit) }
}

private class RecordingGlDevice(
    private val failFragmentCompile: Boolean = false,
    private val failLink: Boolean = false,
    private val missing: Set<String> = emptySet(),
) : GlDevice() {
    val liveShaders = mutableSetOf<Int>()
    val livePrograms = mutableSetOf<Int>()
    val locationQueries = mutableMapOf<String, Int>()
    val textureUnits = mutableListOf<Int>()
    val boundTextures = mutableListOf<Int>()
    var createShaderCalls = 0
    var createProgramCalls = 0
    var uniform1fCalls = 0
    var uniform1iCalls = 0
    var deleteProgramCalls = 0
    private var nextId = 1
    private val stages = mutableMapOf<Int, GlStage>()

    override fun createShader(stage: GlStage): Int {
        createShaderCalls += 1
        val id = nextId++
        stages[id] = stage
        liveShaders += id
        return id
    }

    override fun shaderSource(shader: Int, source: String) = Unit

    override fun compileShader(shader: Int): GlCompileStatus {
        val failed = failFragmentCompile && stages[shader] == GlStage.Fragment
        return GlCompileStatus(!failed, if (failed) "fragment" else "")
    }

    override fun deleteShader(shader: Int) {
        liveShaders -= shader
    }

    override fun createProgram(): Int {
        createProgramCalls += 1
        val id = nextId++
        livePrograms += id
        return id
    }

    override fun attachShader(program: Int, shader: Int) = Unit

    override fun linkProgram(program: Int): GlCompileStatus =
        GlCompileStatus(!failLink, if (failLink) "link" else "")

    override fun deleteProgram(program: Int) {
        deleteProgramCalls += 1
        livePrograms -= program
    }

    override fun uniformLocation(program: Int, name: String): Int {
        locationQueries[name] = (locationQueries[name] ?: 0) + 1
        return if (name in missing) -1 else 3
    }

    override fun uniform1f(location: Int, value: Float) {
        uniform1fCalls += 1
    }

    override fun uniform1i(location: Int, value: Int) {
        uniform1iCalls += 1
    }

    override fun useProgram(program: Int) = Unit

    override fun activeTexture(unit: Int) {
        textureUnits += unit
    }

    override fun bindTexture2D(texture: Int) {
        boundTextures += texture
    }
}
