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
    fun twoProgramsOnOneContextTakeDifferentTextureUnits() {
        val units = GlTextureUnits()
        val firstDevice = RecordingGlDevice()
        val secondDevice = RecordingGlDevice()
        lateinit var firstImage: Uniform<Sampler2D>
        lateinit var secondImage: Uniform<Sampler2D>
        val first = GlProgramRuntime(imageProgram { firstImage = it }, firstDevice, textureUnits = units)
        val second = GlProgramRuntime(imageProgram { secondImage = it }, secondDevice, textureUnits = units)
        first.link()
        second.link()
        assertTrue(first.bind(firstImage, 5))
        assertTrue(second.bind(secondImage, 6))
        assertEquals(listOf(0), firstDevice.textureUnits)
        assertEquals(listOf(1), secondDevice.textureUnits)
    }

    @Test
    fun destroyingAProgramReturnsItsTextureUnit() {
        val units = GlTextureUnits()
        repeat(4) {
            lateinit var image: Uniform<Sampler2D>
            val device = RecordingGlDevice(textureUnitLimit = 1)
            val runtime = GlProgramRuntime(imageProgram { image = it }, device, textureUnits = units)
            runtime.link()
            assertTrue(runtime.bind(image, 3))
            assertEquals(listOf(0), device.textureUnits)
            runtime.destroy()
        }
        lateinit var kept: Uniform<Sampler2D>
        lateinit var temporary: Uniform<Sampler2D>
        lateinit var reused: Uniform<Sampler2D>
        val keptDevice = RecordingGlDevice(textureUnitLimit = 2)
        val temporaryDevice = RecordingGlDevice(textureUnitLimit = 2)
        val reusedDevice = RecordingGlDevice(textureUnitLimit = 2)
        val live = GlProgramRuntime(imageProgram { kept = it }, keptDevice, textureUnits = units)
        val gone = GlProgramRuntime(imageProgram { temporary = it }, temporaryDevice, textureUnits = units)
        live.link()
        gone.link()
        assertTrue(live.bind(kept, 5))
        assertTrue(gone.bind(temporary, 8))
        gone.destroy()
        val next = GlProgramRuntime(imageProgram { reused = it }, reusedDevice, textureUnits = units)
        next.link()
        assertTrue(next.bind(reused, 6))
        assertEquals(listOf(0), keptDevice.textureUnits)
        assertEquals(listOf(1), temporaryDevice.textureUnits)
        assertEquals(listOf(1), reusedDevice.textureUnits)
    }

    @Test
    fun anUploadOnTheActiveUnitForcesTheNextBind() {
        val device = RecordingGlDevice()
        lateinit var image: Uniform<Sampler2D>
        val runtime = GlProgramRuntime(imageProgram { image = it }, device)
        runtime.link()
        assertTrue(runtime.bind(image, 7))
        assertFalse(runtime.bind(image, 7))
        runtime.uploadRgba(1, 1, ByteArray(4))
        assertTrue(runtime.bind(image, 7))
        runtime.deleteTexture(7)
        assertTrue(runtime.bind(image, 7))
        assertEquals(listOf(7, 7, 7), device.boundTextures)
    }

    @Test
    fun aDrawRestoresTheSamplerAnUploadReplaced() {
        val device = RecordingGlDevice()
        lateinit var image: Uniform<Sampler2D>
        val runtime = GlProgramRuntime(imageProgram { image = it }, device)
        runtime.link()
        assertTrue(runtime.bind(image, 7))
        runtime.uploadRgba(1, 1, ByteArray(4))
        runtime.drawRange(4, 3, 0, 3, null, null)
        assertEquals(listOf(7, 7), device.boundTextures)
        runtime.texSubImage2DRgba(7, 1, 1, 0, 0, 1, 1, ByteArray(4))
        runtime.use()
        assertEquals(listOf(7, 7, 7), device.boundTextures)
        runtime.createColorTarget(2, 2)
        runtime.drawRange(4, 3, 0, 3, null, null)
        assertEquals(listOf(7, 7, 7, 7), device.boundTextures)
    }

    @Test
    fun aSecondProgramSeesTheUnitTheFirstProgramReplaced() {
        val units = GlTextureUnits()
        val device = RecordingGlDevice()
        lateinit var first: Uniform<Sampler2D>
        lateinit var second: Uniform<Sampler2D>
        val one = GlProgramRuntime(imageProgram { first = it }, device, textureUnits = units)
        val two = GlProgramRuntime(imageProgram { second = it }, device, textureUnits = units)
        one.link()
        two.link()
        assertTrue(one.bind(first, 3))
        assertTrue(two.bind(second, 4))
        val target = one.createColorTarget(2, 2)
        assertTrue(two.bind(second, target.colorTexture))
        one.deleteColorTarget(target)
        assertTrue(two.bind(second, target.colorTexture))
    }

    @Test
    fun anElementUploadUnbindsTheVertexArrayFirst() {
        val device = RecordingGlDevice()
        val runtime = GlProgramRuntime(passthrough(), device)
        runtime.link()
        assertEquals(IndexElementKind.UnsignedShort, runtime.elementBufferData(runtime.createBuffer(), intArrayOf(0, 1, 2)))
        val unbind = device.writes.indexOf("unbindVao")
        val upload = device.writes.indexOf("elementData")
        assertTrue(unbind >= 0 && unbind < upload)
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

        device.writes.clear()
        runtime.use()
        assertEquals(listOf("use", "bindBuffer"), device.writes)

        assertEquals(2, runtime.attribLocation("a_position"))
        assertEquals(2, runtime.attribLocation("a_position"))
        assertEquals(1, device.locationQueries["a_position"])

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
    fun readStorageCopiesTheDeviceBytesWithoutTheStd430Padding() {
        val device = RecordingGlDevice()
        lateinit var block: StorageBlock
        val runtime = GlProgramRuntime(
            shader(ShaderTarget.Gles31) {
                block = storageBlock("cells") { vec3("value") }
                compute(1) { }
            },
            device,
        )
        runtime.link()
        assertTrue(runtime.set(block, floatArrayOf(1f, 2f, 3f)))
        val read = FloatArray(3)
        assertEquals(3, runtime.read(block, read))
        assertEquals(1f, read[0], 0f)
        assertEquals(2f, read[1], 0f)
        assertEquals(3f, read[2], 0f)
        assertEquals(1, device.bufferUpdateBarriers)

        val stored = device.storageBytes.values.single()
        java.nio.ByteBuffer.wrap(stored).order(java.nio.ByteOrder.nativeOrder()).putFloat(0, 9f)
        assertEquals(3, runtime.read(block, read))
        assertEquals(9f, read[0], 0f)
        assertEquals(2f, read[1], 0f)
        assertEquals(3f, read[2], 0f)

        val short = assertThrows(IllegalArgumentException::class.java) {
            runtime.read(block, FloatArray(2))
        }
        assertTrue(short.message!!.contains("3"))
    }

    @Test
    fun aFailedBlockUploadIsRetriedWithTheSameFloats() {
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
        runtime.link()
        val values = floatArrayOf(1f, 0.2f, 0.4f, 0.6f)
        val bad = floatArrayOf(Float.NaN, 0.2f, 0.4f, 0.6f)
        assertThrows(IllegalArgumentException::class.java) { runtime.set(block, bad) }
        assertThrows(IllegalArgumentException::class.java) { runtime.set(block, bad) }
        device.failNextBufferName = true
        assertThrows(IllegalArgumentException::class.java) { runtime.set(block, values) }
        assertEquals(0, device.bufferDataCalls)
        assertTrue(runtime.set(block, values))
        assertEquals(1, device.bufferDataCalls)
        assertFalse(runtime.set(block, values.copyOf()))

        lateinit var storage: StorageBlock
        val compute = GlProgramRuntime(
            shader(ShaderTarget.Gles31) {
                storage = storageBlock("cells") { float("value") }
                compute(1) { }
            },
            device,
        )
        compute.link()
        device.failNextBufferName = true
        assertThrows(IllegalArgumentException::class.java) { compute.set(storage, floatArrayOf(4f)) }
        val unread = assertThrows(IllegalArgumentException::class.java) {
            compute.read(storage, FloatArray(1))
        }
        assertTrue(unread.message!!.contains("has not been uploaded"))
        assertTrue(compute.set(storage, floatArrayOf(4f)))
        val read = FloatArray(1)
        assertEquals(1, compute.read(storage, read))
        assertEquals(4f, read[0], 0f)
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
        assertEquals(0, device.bufferDataCalls)
        val again = assertThrows(GlException::class.java) {
            runtime.set(block, floatArrayOf(1f))
        }
        assertEquals(GlCode.UniformBlockNotBound, again.code)
        device.writes.clear()
        runtime.use()
        assertEquals(listOf("use"), device.writes)
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

    @Test
    fun uploadRgbaCopiesPixelsOnTheContextThread() {
        val device = RecordingGlDevice()
        val runtime = GlProgramRuntime(passthrough(), device)
        val tooEarly = assertThrows(GlException::class.java) {
            runtime.uploadRgba(1, 1, byteArrayOf(1, 2, 3, 4))
        }
        assertEquals(GlCode.NotLinked, tooEarly.code)

        runtime.link()
        assertThrows(IllegalArgumentException::class.java) {
            runtime.uploadRgba(2, 2, ByteArray(4))
        }
        val pixels = ByteArray(8) { index -> index.toByte() }
        val name = runtime.uploadRgba(1, 2, pixels)
        assertEquals(1, device.createdTextures)
        assertEquals(listOf(name), device.linearRepeat)
        assertEquals(1, device.uploadedWidths.single())
        assertEquals(2, device.uploadedHeights.single())
        assertTrue(device.uploadedBytes.single().contentEquals(pixels))

        runtime.destroy()
        assertEquals(listOf(name), device.deletedTextures)
        assertEquals(0, device.mipmapCalls)
    }

    @Test
    fun uploadRgbaStaysOnTheContextThread() {
        val device = RecordingGlDevice()
        val runtime = GlProgramRuntime(passthrough(), device, contextThread = Thread())
        val error = assertThrows(GlException::class.java) {
            runtime.uploadRgba(1, 1, byteArrayOf(0, 0, 0, 0))
        }
        assertEquals(GlCode.WrongThread, error.code)
        assertEquals(0, device.createdTextures)
    }

    @Test
    fun sameSizeReplaceDoesNotCallBufferData() {
        val device = RecordingGlDevice()
        val runtime = GlProgramRuntime(passthrough(), device)
        runtime.link()
        val buffer = runtime.createBuffer()
        runtime.replaceArrayBuffer(buffer, previousCount = 0, data = floatArrayOf(1f, 2f))
        assertEquals(1, device.arrayDataCalls)
        assertEquals(0, device.arraySubDataCalls)
        runtime.replaceArrayBuffer(buffer, previousCount = 2, data = floatArrayOf(3f, 4f))
        assertEquals(1, device.arrayDataCalls)
        assertEquals(1, device.arraySubDataCalls)
    }

    @Test
    fun drawElementsUsesUnsignedIntPastTheShortLimit() {
        val device = RecordingGlDevice()
        val runtime = GlProgramRuntime(passthrough(), device)
        runtime.link()
        val buffer = runtime.createBuffer()
        val short = runtime.elementBufferData(buffer, intArrayOf(0, 1, 2))
        runtime.drawRange(4, 3, 0, 3, short, null)
        assertEquals(DrawKind.Elements, device.draws.single())
        assertFalse(device.lastUnsignedInt)
        device.draws.clear()
        val wide = runtime.elementBufferData(buffer, intArrayOf(0, 1, INDEX_SHORT_LIMIT + 1))
        assertEquals(IndexElementKind.UnsignedInt, wide)
        runtime.drawRange(4, 3, 0, 3, wide, null)
        assertEquals(DrawKind.Elements, device.draws.single())
        assertTrue(device.lastUnsignedInt)
        assertThrows(IllegalArgumentException::class.java) {
            runtime.elementBufferData(buffer, intArrayOf())
        }
    }

    @Test
    fun aZeroVertexCountIssuesNoDraw() {
        val device = RecordingGlDevice()
        val runtime = GlProgramRuntime(passthrough(), device)
        runtime.link()
        runtime.drawRange(4, 0, 0, 0, null, null)
        runtime.drawRange(4, 3, 0, 3, null, 0)
        assertTrue(device.draws.isEmpty())
        runtime.drawRange(4, 3, 0, 3, null, 1)
        assertEquals(DrawKind.ArraysInstanced, device.draws.single())
    }

    @Test
    fun aDefaultDrawDoesNotQueryTheDriver() {
        val device = RecordingGlDevice()
        val runtime = GlProgramRuntime(passthrough(), device)
        runtime.link()
        runtime.drawRange(4, 3, 0, 3, null, null)
        runtime.uploadRgba(1, 1, ByteArray(4))
        assertEquals(0, device.glErrorChecks)
    }

    @Test
    fun strictErrorsTurnADriverErrorIntoAnException() {
        val device = RecordingGlDevice()
        device.glError = 0x0502
        val runtime = GlProgramRuntime(passthrough(), device, strictErrors = true)
        runtime.link()
        val error = assertThrows(GlException::class.java) {
            runtime.drawRange(4, 3, 0, 3, null, null)
        }
        assertEquals(GlCode.DriverError, error.code)
        assertEquals(1, device.glErrorChecks)
        runtime.uploadRgba(1, 1, ByteArray(4))
        assertEquals(2, device.glErrorChecks)
    }

    @Test
    fun drawRangeRejectsTheWrongThread() {
        val device = RecordingGlDevice()
        val runtime = GlProgramRuntime(passthrough(), device)
        runtime.link()
        val error = assertThrows(GlException::class.java) {
            GlProgramRuntime(passthrough(), device, contextThread = Thread()).drawRange(4, 3, 0, 3, null, null)
        }
        assertEquals(GlCode.WrongThread, error.code)
        assertTrue(device.draws.isEmpty())
    }

    @Test
    fun texSubImageRejectsARectangleOutsideTheTexture() {
        val device = RecordingGlDevice()
        val runtime = GlProgramRuntime(passthrough(), device)
        runtime.link()
        assertThrows(IllegalArgumentException::class.java) {
            runtime.texSubImage2DRgba(1, 4, 4, 2, 0, 4, 1, ByteArray(16))
        }
        assertEquals(0, device.subImageCalls)
    }

    @Test
    fun cubeUploadWritesOneFace() {
        val device = RecordingGlDevice()
        val runtime = GlProgramRuntime(passthrough(), device)
        runtime.link()
        runtime.uploadCubeFace(7, CubeFace.PositiveY, 1, 1, byteArrayOf(1, 2, 3, 4))
        assertEquals(listOf(CubeFace.PositiveY), device.cubeFaces)
    }

    @Test
    fun anIncompleteFramebufferIsReportedAndDeleted() {
        val device = RecordingGlDevice().also { it.framebufferOk = false }
        val runtime = GlProgramRuntime(passthrough(), device)
        runtime.link()
        val error = assertThrows(GlException::class.java) { runtime.createColorTarget(8, 8) }
        assertEquals(GlCode.FramebufferIncomplete, error.code)
        assertEquals(1, device.deletedTextures.size)
        assertEquals(1, device.deletedFramebuffers.size)
        assertTrue(device.boundFramebuffers.contains(0))
    }

    @Test
    fun samplingTheBoundColorTargetIsRejected() {
        val device = RecordingGlDevice()
        lateinit var image: Uniform<Sampler2D>
        val program = shader(ShaderTarget.Gles30) {
            val uv = varyingVec2("uv")
            image = sampler2D("image")
            vertex {
                uv.set(attributeVec2("corner"))
                glPosition(attributeVec4("position"))
            }
            fragment { texture(image, uv.expr) }
        }
        val runtime = GlProgramRuntime(program, device)
        runtime.link()
        val target = runtime.createColorTarget(4, 4)
        runtime.bindFramebuffer(target.framebuffer)
        val whileBound = assertThrows(GlException::class.java) { runtime.bind(image, target.colorTexture) }
        assertEquals(GlCode.FeedbackLoop, whileBound.code)
        runtime.bindFramebuffer(0)
        assertTrue(runtime.bind(image, target.colorTexture))
        val whileSampling = assertThrows(GlException::class.java) { runtime.bindFramebuffer(target.framebuffer) }
        assertEquals(GlCode.FeedbackLoop, whileSampling.code)
        assertEquals(1, device.boundFramebuffers.count { it == target.framebuffer })
    }

    @Test
    fun createTextureIsDeletedWithTheProgramAndMipFilterIsASeparateCall() {
        val device = RecordingGlDevice()
        val runtime = GlProgramRuntime(passthrough(), device)
        runtime.link()
        val name = runtime.createTexture()
        runtime.filterMipmap2D(name)
        assertEquals(1, device.mipFilters)
        runtime.destroy()
        assertTrue(device.deletedTextures.contains(name))
    }

    @Test
    fun destroyDoesNotDeleteAColorTarget() {
        val device = RecordingGlDevice()
        val runtime = GlProgramRuntime(passthrough(), device)
        runtime.link()
        val target = runtime.createColorTarget(4, 4)
        runtime.destroy()
        assertFalse(device.deletedTextures.contains(target.colorTexture))
        assertTrue(device.boundFramebuffers.contains(0))
    }

    @Test
    fun aColorTargetCanBeDeletedAfterTheProgram() {
        val device = RecordingGlDevice()
        val runtime = GlProgramRuntime(passthrough(), device)
        runtime.link()
        val target = runtime.createColorTarget(4, 4)
        runtime.destroy()
        runtime.deleteColorTarget(target)
        assertTrue(device.deletedFramebuffers.contains(target.framebuffer))
        assertTrue(device.deletedTextures.contains(target.colorTexture))
    }

    @Test
    fun pixelReadChecksTheBufferBeforeTouchingTheFramebuffer() {
        val device = RecordingGlDevice()
        val runtime = GlProgramRuntime(passthrough(), device)
        runtime.link()
        val target = runtime.createColorTarget(2, 3)
        runtime.bindFramebuffer(target.framebuffer)
        val bound = device.boundFramebuffers.size
        assertThrows(IllegalArgumentException::class.java) {
            runtime.readFramebuffer(2, 3, ByteArray(3))
        }
        assertEquals(0, device.readPixelCalls)
        assertEquals(bound, device.boundFramebuffers.size)
        val into = ByteArray(2 * 3 * 4)
        runtime.readFramebuffer(2, 3, into)
        assertEquals(1, device.readPixelCalls)
        assertEquals(9, into[0].toInt())
        assertEquals(target.framebuffer, device.boundFramebuffers.last())

        val marks = device.boundFramebuffers.size
        runtime.readColorTarget(target, into)
        assertEquals(marks, device.boundFramebuffers.size)
        assertEquals(2, device.readPixelCalls)

        runtime.bindFramebuffer(0)
        val beforeRestore = device.boundFramebuffers.size
        assertThrows(IllegalArgumentException::class.java) {
            runtime.readColorTarget(target, ByteArray(1))
        }
        assertEquals(beforeRestore, device.boundFramebuffers.size)
        runtime.readColorTarget(target, into)
        assertEquals(listOf(target.framebuffer, 0), device.boundFramebuffers.takeLast(2))
        assertEquals(3, device.readPixelCalls)
    }

    @Test
    fun aBufferCanBeDeletedAfterTheProgramAndNotTwice() {
        val device = RecordingGlDevice()
        val runtime = GlProgramRuntime(passthrough(), device)
        runtime.link()
        val name = runtime.createBuffer()
        runtime.destroy()
        assertEquals(0, device.deleteBufferCalls)
        runtime.deleteBuffer(name)
        assertEquals(1, device.deleteBufferCalls)
        assertThrows(IllegalStateException::class.java) { runtime.deleteBuffer(name) }
        val created = assertThrows(GlException::class.java) { runtime.createBuffer() }
        assertEquals(GlCode.Destroyed, created.code)
    }

    @Test
    fun aRecycledBufferNameCanBeDeletedAgain() {
        val device = RecordingGlDevice()
        val runtime = GlProgramRuntime(passthrough(), device)
        runtime.link()
        val name = runtime.createBuffer()
        runtime.deleteBuffer(name)
        device.reuseBufferName(name)
        val recycled = runtime.createBuffer()
        assertEquals(name, recycled)
        runtime.deleteBuffer(recycled)
        assertEquals(2, device.deleteBufferCalls)
    }

    @Test
    fun aGraphicsProgramDoesNotQueryShaderStorageBindings() {
        val device = RecordingGlDevice()
        GlProgramRuntime(passthrough(), device).link()
        assertEquals(0, device.storageBindingQueries)
        assertEquals(0, device.uniformBindingQueries)
    }

    @Test
    fun aComputeProgramQueriesShaderStorageBindingsOnlyWhenItHasABlock() {
        val empty = RecordingGlDevice()
        GlProgramRuntime(
            shader(ShaderTarget.Gles31) { compute(1) { } },
            empty,
        ).link()
        assertEquals(0, empty.storageBindingQueries)

        val program = shader(ShaderTarget.Gles31) {
            storageBlock("cells") { float("value") }
            compute(1) { }
        }
        val withBlock = RecordingGlDevice()
        GlProgramRuntime(program, withBlock).link()
        assertEquals(1, withBlock.storageBindingQueries)
    }

    @Test
    fun twoBlocksBindAtTheirDeclarationPointsAndTheLimitIsTheContext() {
        lateinit var frame: UniformBlock
        lateinit var color: UniformBlock
        val program = shader(ShaderTarget.Gles30) {
            frame = uniformBlock("frame") { float("time") }
            color = uniformBlock("color") { vec3("rgb") }
            vertex { glPosition(attributeVec4("position")) }
            fragment { vec4(0f.lit, 0f.lit, 0f.lit, 1f.lit) }
        }
        val device = RecordingGlDevice()
        val runtime = GlProgramRuntime(program, device)
        runtime.link()
        assertTrue(runtime.set(frame, floatArrayOf(1f)))
        assertTrue(runtime.set(color, floatArrayOf(0f, 1f, 0f)))
        assertEquals(listOf(0, 1), device.assignedBlockBindings)
        assertEquals(listOf(0, 1), device.uniformBindPoints)

        lateinit var foreign: UniformBlock
        shader(ShaderTarget.Gles30) {
            foreign = uniformBlock("other") { float("time") }
            vertex { glPosition(attributeVec4("position")) }
            fragment { vec4(0f.lit, 0f.lit, 0f.lit, 1f.lit) }
        }
        assertThrows(IllegalArgumentException::class.java) {
            runtime.set(foreign, floatArrayOf(1f))
        }

        val limited = RecordingGlDevice(uniformBindingLimit = 1)
        val rejected = assertThrows(GlException::class.java) {
            GlProgramRuntime(program, limited).link()
        }
        assertEquals(GlCode.BlockBindingLimit, rejected.code)

        lateinit var cells: StorageBlock
        lateinit var gain: StorageBlock
        val compute = shader(ShaderTarget.Gles31) {
            cells = storageBlock("cells") { float("value") }
            gain = storageBlock("gain") { float("amount") }
            compute(8) { }
        }
        val computeDevice = RecordingGlDevice()
        val computeRuntime = GlProgramRuntime(compute, computeDevice)
        computeRuntime.link()
        assertTrue(computeRuntime.set(cells, floatArrayOf(1f)))
        assertTrue(computeRuntime.set(gain, floatArrayOf(2f)))
        assertEquals(listOf(0, 1), computeDevice.storageBindPoints)
        val first = FloatArray(1)
        val second = FloatArray(1)
        assertEquals(1, computeRuntime.read(cells, first))
        assertEquals(1, computeRuntime.read(gain, second))
        assertEquals(1f, first[0], 0f)
        assertEquals(2f, second[0], 0f)
        assertThrows(IllegalArgumentException::class.java) {
            computeRuntime.read(cellsFromAnotherShader(), FloatArray(1))
        }

        val storageLimited = RecordingGlDevice(storageBindingLimit = 1)
        val storageRejected = assertThrows(GlException::class.java) {
            GlProgramRuntime(compute, storageLimited).link()
        }
        assertEquals(GlCode.BlockBindingLimit, storageRejected.code)
    }

    private fun cellsFromAnotherShader(): StorageBlock {
        lateinit var foreign: StorageBlock
        shader(ShaderTarget.Gles31) {
            foreign = storageBlock("other") { float("value") }
            compute(8) { }
        }
        return foreign
    }
}

private fun imageProgram(image: (Uniform<Sampler2D>) -> Unit) = shader(ShaderTarget.Gles30) {
    val uv = varyingVec2("uv")
    val sampler = sampler2D("image")
    image(sampler)
    vertex {
        uv.set(attributeVec2("uv"))
        glPosition(attributeVec4("position"))
    }
    fragment { texture(sampler, uv.expr) }
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
    private val uniformBindingLimit: Int = 24,
    private val storageBindingLimit: Int = 8,
) : GlDevice() {
    var uniformBindingQueries = 0
    var storageBindingQueries = 0
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

    override fun attribLocation(program: Int, name: String): Int {
        locationQueries[name] = (locationQueries[name] ?: 0) + 1
        return if (name in missing) -1 else 2
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

    var createdTextures = 0
    val linearRepeat = mutableListOf<Int>()
    val uploadedWidths = mutableListOf<Int>()
    val uploadedHeights = mutableListOf<Int>()
    val uploadedBytes = mutableListOf<ByteArray>()
    val deletedTextures = mutableListOf<Int>()
    private var nextTexture = 40

    override fun createTexture(): Int {
        createdTextures += 1
        return nextTexture++
    }

    override fun deleteTexture(texture: Int) {
        deletedTextures += texture
    }

    override fun texture2DLinearRepeat(texture: Int) {
        linearRepeat += texture
    }

    override fun texImage2DRgba(texture: Int, width: Int, height: Int, rgba: ByteArray) {
        uploadedWidths += width
        uploadedHeights += height
        uploadedBytes += rgba.copyOf()
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
    var failNextBufferName = false
    private var nextBuffer = 1
    private var reusedName: Int? = null

    fun reuseBufferName(name: Int) {
        reusedName = name
    }

    override fun createBuffer(): Int {
        if (failNextBufferName) {
            failNextBufferName = false
            return 0
        }
        val forced = reusedName
        if (forced != null) {
            reusedName = null
            return forced
        }
        return nextBuffer++
    }

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

    val uniformBindPoints = mutableListOf<Int>()
    val assignedBlockBindings = mutableListOf<Int>()

    override fun bindUniformBufferBase(buffer: Int, binding: Int) {
        writes += "bindBuffer"
        uniformBindPoints += binding
    }

    override fun uniformBlockIndex(program: Int, name: String): Int = uniformBlockIndex

    override fun uniformBlockBinding(program: Int, blockIndex: Int, binding: Int) {
        writes += "blockBinding"
        assignedBlockBindings += binding
    }

    override fun maxUniformBufferBindings(): Int {
        uniformBindingQueries += 1
        return uniformBindingLimit
    }

    override fun maxShaderStorageBufferBindings(): Int {
        storageBindingQueries += 1
        return storageBindingLimit
    }

    var storageDataCalls = 0
    var storageSubDataCalls = 0
    var bufferUpdateBarriers = 0
    val storageBytes = HashMap<Int, ByteArray>()

    override fun shaderStorageData(buffer: Int, data: ByteArray) {
        storageDataCalls += 1
        writes += "storageData"
        storageBytes[buffer] = data.copyOf()
    }

    override fun shaderStorageSubData(buffer: Int, data: ByteArray) {
        storageSubDataCalls += 1
        writes += "storageSubData"
        storageBytes[buffer] = data.copyOf()
    }

    override fun bufferUpdateBarrier() {
        bufferUpdateBarriers += 1
    }

    override fun mapShaderStorageRead(buffer: Int, bytes: Int): java.nio.ByteBuffer {
        val stored = checkNotNull(storageBytes[buffer])
        check(stored.size >= bytes)
        return java.nio.ByteBuffer.wrap(stored, 0, bytes).order(java.nio.ByteOrder.nativeOrder())
    }

    override fun unmapShaderStorage(buffer: Int) = Unit

    val storageBindPoints = mutableListOf<Int>()

    override fun bindShaderStorageBase(buffer: Int, binding: Int) {
        writes += "bindStorage"
        storageBindPoints += binding
    }

    override fun maxCombinedTextureImageUnits(): Int = textureUnitLimit

    val draws = mutableListOf<DrawKind>()
    var lastUnsignedInt = false
    var arrayDataCalls = 0
    var arraySubDataCalls = 0
    var elementUploads = 0
    var lastElementUnsignedInt = false
    var subImageCalls = 0
    val cubeFaces = mutableListOf<CubeFace>()
    var mipmapCalls = 0
    var allocCalls = 0
    val boundFramebuffers = mutableListOf<Int>()
    val deletedFramebuffers = mutableListOf<Int>()
    var framebufferOk = true
    private var nextFramebuffer = 80
    private var nextRenderbuffer = 90

    override fun drawArrays(mode: Int, first: Int, count: Int) {
        draws += DrawKind.Arrays
    }

    override fun drawElements(mode: Int, count: Int, unsignedInt: Boolean, indexOffset: Int) {
        draws += DrawKind.Elements
        lastUnsignedInt = unsignedInt
    }

    override fun drawArraysInstanced(mode: Int, first: Int, count: Int, instances: Int) {
        draws += DrawKind.ArraysInstanced
    }

    override fun drawElementsInstanced(
        mode: Int,
        count: Int,
        unsignedInt: Boolean,
        instances: Int,
        indexOffset: Int,
    ) {
        draws += DrawKind.ElementsInstanced
        lastUnsignedInt = unsignedInt
    }

    override fun arrayBufferData(buffer: Int, data: FloatArray) {
        arrayDataCalls += 1
    }

    override fun arrayBufferSubData(buffer: Int, data: FloatArray) {
        arraySubDataCalls += 1
    }

    override fun unbindVertexArray() {
        writes += "unbindVao"
    }

    override fun elementBufferData(buffer: Int, indices: IntArray, unsignedInt: Boolean) {
        writes += "elementData"
        elementUploads += 1
        lastElementUnsignedInt = unsignedInt
    }

    override fun texSubImage2DRgba(
        texture: Int,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        rgba: ByteArray,
    ) {
        subImageCalls += 1
    }

    override fun texImageCubeFace(texture: Int, face: CubeFace, width: Int, height: Int, rgba: ByteArray) {
        cubeFaces += face
    }

    override fun textureCubeLinearClamp(texture: Int) = Unit

    override fun texture2DLinearClamp(texture: Int) = Unit

    override fun generateMipmap2D(texture: Int) {
        mipmapCalls += 1
    }

    var mipFilters: Int = 0

    override fun filterMipmap2D(texture: Int) {
        mipFilters += 1
    }

    override fun texImage2DRgbaAlloc(texture: Int, width: Int, height: Int) {
        allocCalls += 1
    }

    override fun createFramebuffer(): Int = nextFramebuffer++

    override fun deleteFramebuffer(framebuffer: Int) {
        deletedFramebuffers += framebuffer
    }

    override fun bindFramebuffer(framebuffer: Int) {
        boundFramebuffers += framebuffer
    }

    var glError: Int = 0
    var glErrorChecks: Int = 0

    override fun takeGlError(): Int {
        glErrorChecks += 1
        val error = glError
        glError = 0
        return error
    }

    var readPixelCalls = 0

    override fun readPixelsRgba(x: Int, y: Int, width: Int, height: Int, rgba: ByteArray) {
        readPixelCalls += 1
        rgba.fill(9)
    }

    override fun createRenderbuffer(): Int = nextRenderbuffer++

    override fun deleteRenderbuffer(renderbuffer: Int) = Unit

    override fun framebufferColor(framebuffer: Int, texture: Int) = Unit

    override fun framebufferDepth(framebuffer: Int, renderbuffer: Int, width: Int, height: Int) = Unit

    override fun framebufferComplete(framebuffer: Int): Boolean = framebufferOk

    override fun vertexAttribDivisor(location: Int, divisor: Int) = Unit

    override fun disableVertexAttribArray(location: Int) = Unit

    override fun vertexAttribFloat(location: Int, size: Int, strideFloats: Int, offsetFloats: Int) = Unit
}
