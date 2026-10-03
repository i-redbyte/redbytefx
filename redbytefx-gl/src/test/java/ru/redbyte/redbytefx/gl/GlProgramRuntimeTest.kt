package ru.redbyte.redbytefx.gl

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.redbyte.redbytefx.Flt
import ru.redbyte.redbytefx.High
import ru.redbyte.redbytefx.Med
import ru.redbyte.redbytefx.Vec3
import ru.redbyte.redbytefx.Sampler2D
import ru.redbyte.redbytefx.GeometryInput
import ru.redbyte.redbytefx.GeometryOutput
import ru.redbyte.redbytefx.ShaderTarget
import ru.redbyte.redbytefx.TessPrimitive
import ru.redbyte.redbytefx.StorageBlock
import ru.redbyte.redbytefx.Uniform
import ru.redbyte.redbytefx.UniformBlock
import ru.redbyte.redbytefx.lit
import ru.redbyte.redbytefx.med
import ru.redbyte.redbytefx.plus
import ru.redbyte.redbytefx.shader
import ru.redbyte.redbytefx.toHigh
import ru.redbyte.redbytefx.vec4
import ru.redbyte.redbytefx.x
import ru.redbyte.redbytefx.y
import ru.redbyte.redbytefx.z

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
    fun dispatchReachesTheDeviceOnceWithTheGroupCounts() {
        val device = RecordingGlDevice()
        val program = shader(ShaderTarget.Gles31) {
            compute(1) { }
        }
        val runtime = GlProgramRuntime(program, device)
        runtime.link()
        runtime.use()
        runtime.dispatch(8, 2, 1)
        assertEquals(1, device.dispatchCalls)
        assertEquals(1, device.barrierCalls)
        assertEquals(listOf(8, 2, 1), device.dispatchGroups)
        runtime.dispatch(4, 1, 1, memoryBarrier = false)
        assertEquals(2, device.dispatchCalls)
        assertEquals(1, device.barrierCalls)
        runtime.dispatch(8, 2, 1)
        assertEquals(3, device.dispatchCalls)
        assertEquals(2, device.barrierCalls)
        assertThrows(IllegalArgumentException::class.java) { runtime.dispatch(0, 1, 1) }
        val graphics = GlProgramRuntime(passthrough(), RecordingGlDevice())
        graphics.link()
        val wrong = assertThrows(GlException::class.java) { graphics.dispatch(1, 1, 1) }
        assertEquals(GlCode.WrongTarget, wrong.code)
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
        assertEquals(listOf("use", "uniform1f"), device.writes)
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
    fun strictUniformLocationsFailLinkWhenASpelledUniformIsInactive() {
        val device = RecordingGlDevice(missing = setOf("u_amount"))
        lateinit var amount: Uniform<Flt<High>>
        val program = shader(ShaderTarget.Gles30) {
            amount = uniform("amount", 1f)
            vertex { glPosition(attributeVec4("position")) }
            fragment { vec4(0f.lit, 0f.lit, 0f.lit, 1f.lit) }
        }
        val runtime = GlProgramRuntime(program, device, strictUniformLocations = true)
        val error = assertThrows(GlException::class.java) { runtime.link() }
        assertEquals(GlCode.MissingUniformLocation, error.code)
        assertTrue(error.message!!.contains("u_amount"))
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
    fun linkUploadsVectorAndMediumpDefaultsAndSkipsAnUnchangedWrite() {
        val device = RecordingGlDevice()
        lateinit var tint: Uniform<Vec3<Flt<High>>>
        lateinit var gain: Uniform<Flt<Med>>
        val program = shader(ShaderTarget.Gles30) {
            tint = uniformVec3("tint", 1f, 2f, 3f)
            gain = uniformMedium("gain", 0.5f)
            vertex { glPosition(attributeVec4("position")) }
            fragment { vec4(tint.expr.x, tint.expr.y, tint.expr.z, gain.expr.toHigh()) }
        }
        val runtime = GlProgramRuntime(program, device)
        runtime.link()
        assertEquals(1, device.uniform3fCalls)
        assertEquals(listOf(1f, 2f, 3f), device.uniform3fValues)
        assertEquals(1, device.uniform1fCalls)
        assertFalse(runtime.set(tint, 1f, 2f, 3f))
        assertEquals(1, device.uniform3fCalls)
        assertTrue(runtime.set(gain, 0.25f))
        assertEquals(2, device.uniform1fCalls)
    }

    @Test
    fun aTexturePastTheCombinedUnitLimitIsRejected() {
        val device = RecordingGlDevice(textureUnitLimit = 1)
        lateinit var first: Uniform<Sampler2D>
        lateinit var second: Uniform<Sampler2D>
        val program = shader(ShaderTarget.Gles30) {
            val uv = varyingVec2("uv")
            first = sampler2D("first")
            second = sampler2D("second")
            vertex {
                uv.set(attributeVec2("uv"))
                glPosition(attributeVec4("position"))
            }
            fragment { texture(first, uv.expr) + texture(second, uv.expr) }
        }
        val runtime = GlProgramRuntime(program, device)
        runtime.link()
        assertTrue(runtime.bind(first, 3))
        val error = assertThrows(GlException::class.java) { runtime.bind(second, 4) }
        assertEquals(GlCode.TextureUnitLimit, error.code)
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
    fun unchangedBlockBytesSkipTheUploadAndAForeignBlockIsRejected() {
        val device = RecordingGlDevice()
        lateinit var block: UniformBlock
        val runtime = GlProgramRuntime(
            shader(ShaderTarget.Gles30) {
                block = uniformBlock("frame") {
                    float("time")
                    vec3("color")
                }
                vertex { glPosition(attributeVec4("position")) }
                fragment { vec4(0f.lit, 0f.lit, 0f.lit, 1f.lit) }
            },
            device,
        )
        var wrongThread: GlException? = null
        Thread {
            try {
                runtime.set(block, floatArrayOf(1f, 0f, 0f, 0f))
            } catch (error: GlException) {
                wrongThread = error
            }
        }.apply {
            start()
            join()
        }
        assertEquals(GlCode.WrongThread, wrongThread?.code)
        assertEquals(0, device.bufferDataCalls)

        runtime.link()
        val values = floatArrayOf(1f, 0.2f, 0.4f, 0.6f)
        assertTrue(runtime.set(block, values))
        assertEquals(1, device.bufferDataCalls)
        assertEquals(0, device.bufferSubDataCalls)
        assertFalse(runtime.set(block, values.copyOf()))
        assertEquals(1, device.bufferDataCalls)
        assertEquals(0, device.bufferSubDataCalls)
        assertTrue(runtime.set(block, floatArrayOf(0f, 0.2f, 0.4f, 0.6f)))
        assertEquals(1, device.bufferSubDataCalls)

        lateinit var foreign: UniformBlock
        shader(ShaderTarget.Gles30) {
            foreign = uniformBlock("other") { float("time") }
            vertex { glPosition(attributeVec4("position")) }
            fragment { vec4(0f.lit, 0f.lit, 0f.lit, 1f.lit) }
        }
        assertThrows(IllegalArgumentException::class.java) {
            runtime.set(foreign, floatArrayOf(1f))
        }
        assertEquals(1, device.bufferDataCalls)

        runtime.destroy()
        assertEquals(1, device.deleteBufferCalls)
    }

    @Test
    fun unchangedStorageBytesSkipTheUploadAndAForeignBlockIsRejected() {
        val device = RecordingGlDevice()
        lateinit var block: StorageBlock
        val runtime = GlProgramRuntime(
            shader(ShaderTarget.Gles31) {
                block = storageBlock("cells") { float("value") }
                compute(64) { }
            },
            device,
        )
        var wrongThread: GlException? = null
        Thread {
            try {
                runtime.set(block, floatArrayOf(1f))
            } catch (error: GlException) {
                wrongThread = error
            }
        }.apply {
            start()
            join()
        }
        assertEquals(GlCode.WrongThread, wrongThread?.code)
        assertEquals(0, device.storageDataCalls)

        runtime.link()
        val values = floatArrayOf(1.5f)
        assertTrue(runtime.set(block, values))
        assertEquals(1, device.storageDataCalls)
        assertEquals(0, device.storageSubDataCalls)
        assertFalse(runtime.set(block, values.copyOf()))
        assertEquals(1, device.storageDataCalls)
        assertEquals(0, device.storageSubDataCalls)
        assertTrue(runtime.set(block, floatArrayOf(0f)))
        assertEquals(1, device.storageSubDataCalls)

        lateinit var foreign: StorageBlock
        shader(ShaderTarget.Gles31) {
            foreign = storageBlock("other") { float("value") }
            compute(8) { }
        }
        assertThrows(IllegalArgumentException::class.java) {
            runtime.set(foreign, floatArrayOf(1f))
        }
        assertEquals(1, device.storageDataCalls)

        runtime.destroy()
        assertEquals(1, device.deleteBufferCalls)
    }

    @Test
    fun aLongerUnsizedTailAllocatesTheStorageBufferAgain() {
        val device = RecordingGlDevice()
        lateinit var tail: StorageBlock
        val runtime = GlProgramRuntime(
            shader(ShaderTarget.Gles31) {
                tail = storageBlock("tail") {
                    float("head")
                    floatArray("rest")
                }
                compute(1) { }
            },
            device,
        )
        runtime.link()
        assertTrue(runtime.set(tail, floatArrayOf(1f, 2f)))
        assertEquals(1, device.storageDataCalls)
        assertTrue(runtime.set(tail, floatArrayOf(1f, 2f, 3f, 4f)))
        assertEquals(2, device.storageDataCalls)
        assertEquals(0, device.storageSubDataCalls)
        assertTrue(runtime.set(tail, floatArrayOf(9f, 2f, 3f, 4f)))
        assertEquals(2, device.storageDataCalls)
        assertEquals(1, device.storageSubDataCalls)
    }

    @Test
    fun gles32LinksVertexTessellationGeometryAndFragment() {
        val device = RecordingGlDevice()
        val runtime = GlProgramRuntime(
            shader(ShaderTarget.Gles32) {
                vertex { glPosition(attributeVec4("position")) }
                tessControl(3) { passPosition() }
                tessEval(TessPrimitive.Triangles) { glPosition(glIn(0)) }
                geometry(GeometryInput.Triangles, GeometryOutput.TriangleStrip, 3) {
                    glPosition(glIn(0))
                    emitVertex()
                }
                fragment { vec4(0f.lit, 0f.lit, 0f.lit, 1f.lit) }
            },
            device,
        )
        runtime.link()
        assertEquals(5, device.createShaderCalls)
        runtime.destroy()
    }

    @Test
    fun missingUniformBlockIndexFailsOnFirstUpload() {
        val device = RecordingGlDevice(uniformBlockIndex = -1)
        lateinit var block: UniformBlock
        val runtime = GlProgramRuntime(
            shader(ShaderTarget.Gles30) {
                block = uniformBlock("frame") { float("time") }
                vertex { glPosition(attributeVec4("position")) }
                fragment { vec4(0f.lit, 0f.lit, 0f.lit, 1f.lit) }
            },
            device,
        )
        runtime.link()
        val error = assertThrows(GlException::class.java) {
            runtime.set(block, floatArrayOf(1f))
        }
        assertEquals(GlCode.UniformBlockNotBound, error.code)
        assertEquals(1, device.bufferDataCalls)
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
    private val textureUnitLimit: Int = 8,
    private val uniformBlockIndex: Int = 0,
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
    val writes = mutableListOf<String>()
    val intValues = mutableListOf<Int>()
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
        writes += "uniform1f"
        uniform1fCalls += 1
    }

    var uniform3fCalls = 0
    val uniform3fValues = mutableListOf<Float>()

    override fun uniform2f(location: Int, x: Float, y: Float) = Unit

    override fun uniform3f(location: Int, x: Float, y: Float, z: Float) {
        uniform3fCalls += 1
        uniform3fValues += x
        uniform3fValues += y
        uniform3fValues += z
    }

    override fun uniform4f(location: Int, x: Float, y: Float, z: Float, w: Float) = Unit

    override fun uniform1i(location: Int, value: Int) {
        writes += "uniform1i"
        uniform1iCalls += 1
        intValues += value
    }

    val matrixValues = mutableListOf<FloatArray>()

    override fun uniformMatrix2fv(location: Int, values: FloatArray) {
        matrixValues += values.copyOf()
    }

    override fun uniformMatrix3fv(location: Int, values: FloatArray) {
        matrixValues += values.copyOf()
    }

    override fun uniformMatrix4fv(location: Int, values: FloatArray) {
        matrixValues += values.copyOf()
    }

    override fun useProgram(program: Int) {
        writes += "use"
    }

    override fun activeTexture(unit: Int) {
        textureUnits += unit
    }

    override fun bindTexture2D(texture: Int) {
        boundTextures += texture
    }

    val cubeTextures = mutableListOf<Int>()

    override fun bindTextureCube(texture: Int) {
        cubeTextures += texture
    }

    var dispatchCalls = 0
    val dispatchGroups = mutableListOf<Int>()

    override fun dispatchCompute(x: Int, y: Int, z: Int) {
        dispatchCalls += 1
        dispatchGroups += x
        dispatchGroups += y
        dispatchGroups += z
    }

    var barrierCalls = 0

    override fun shaderStorageBarrier() {
        barrierCalls += 1
    }

    var bufferDataCalls = 0
    var bufferSubDataCalls = 0
    var deleteBufferCalls = 0
    private var nextBuffer = 1

    override fun createBuffer(): Int = nextBuffer++

    override fun deleteBuffer(buffer: Int) {
        deleteBufferCalls += 1
    }

    override fun uniformBufferData(buffer: Int, data: ByteArray) {
        bufferDataCalls += 1
        writes += "bufferData"
    }

    override fun uniformBufferSubData(buffer: Int, data: ByteArray) {
        bufferSubDataCalls += 1
        writes += "bufferSubData"
    }

    override fun bindUniformBufferBase(buffer: Int, binding: Int) {
        writes += "bindBuffer"
    }

    override fun uniformBlockIndex(program: Int, name: String): Int = uniformBlockIndex

    override fun uniformBlockBinding(program: Int, blockIndex: Int, binding: Int) {
        writes += "blockBinding"
    }

    var storageDataCalls = 0
    var storageSubDataCalls = 0

    override fun shaderStorageData(buffer: Int, data: ByteArray) {
        storageDataCalls += 1
        writes += "storageData"
    }

    override fun shaderStorageSubData(buffer: Int, data: ByteArray) {
        storageSubDataCalls += 1
        writes += "storageSubData"
    }

    override fun bindShaderStorageBase(buffer: Int, binding: Int) {
        writes += "bindStorage"
    }

    override fun maxCombinedTextureImageUnits(): Int = textureUnitLimit
}
