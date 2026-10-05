package ru.redbyte.redbytefx.gl

import ru.redbyte.redbytefx.BoolS
import ru.redbyte.redbytefx.Flt
import ru.redbyte.redbytefx.High
import ru.redbyte.redbytefx.IntS
import ru.redbyte.redbytefx.Mat2
import ru.redbyte.redbytefx.Mat3
import ru.redbyte.redbytefx.Mat4
import ru.redbyte.redbytefx.Med
import ru.redbyte.redbytefx.Sampler2D
import ru.redbyte.redbytefx.SamplerCube
import ru.redbyte.redbytefx.ScalarKind
import ru.redbyte.redbytefx.ShaderProgram
import ru.redbyte.redbytefx.ShaderTarget
import ru.redbyte.redbytefx.Shape
import ru.redbyte.redbytefx.StorageBlock
import ru.redbyte.redbytefx.Uniform
import ru.redbyte.redbytefx.UniformBlock
import ru.redbyte.redbytefx.Vec2
import ru.redbyte.redbytefx.Vec3
import ru.redbyte.redbytefx.Vec4
import ru.redbyte.redbytefx.packStd140
import ru.redbyte.redbytefx.packStd430
import ru.redbyte.redbytefx.sameFloatUniformValue
import ru.redbyte.redbytefx.unpackStd430
import java.util.IdentityHashMap
import kotlin.jvm.JvmName

/** Machine-readable reason for [GlException]. */
public enum class GlCode {
    WrongThread,
    WrongTarget,
    NotLinked,
    Destroyed,
    CompileFailed,
    LinkFailed,
    TextureUnitLimit,
    BlockBindingLimit,
    UniformBlockNotBound,
    MissingUniformLocation,
    FramebufferIncomplete,
    FeedbackLoop,
    DriverError,
}

public class GlException(
    public val code: GlCode,
    message: String,
) : IllegalStateException(message)

/**
 * Device. Owns one GLES program on the thread that created it.
 *
 * Uniform locations are queried once, after the device reports a successful link.
 * An unchanged float or texture does not call the device again.
 * [textureUnits] is shared by every runtime that draws on this EGL context.
 *
 * @param strictErrors when true, `glGetError` runs after draw, dispatch, texture upload, and read,
 * and a driver error becomes [GlException] with [GlCode.DriverError]. The default does not query
 * the driver on those calls. A strict link still drains errors after link.
 */
public class GlProgramRuntime(
    private val program: ShaderProgram,
    private val device: GlDevice,
    private val contextThread: Thread = Thread.currentThread(),
    private val strictUniformLocations: Boolean = false,
    private val textureUnits: GlTextureUnits = GlTextureUnits(),
    private val strictErrors: Boolean = false,
) {
    private val locations = IdentityHashMap<Uniform<*>, Int>()
    private val floatValues = IdentityHashMap<Uniform<*>, Float>()
    private val vectorValues = IdentityHashMap<Uniform<*>, FloatArray>()
    private val intValues = IdentityHashMap<Uniform<*>, Int>()
    private val samplers = GlSamplerBindings(device, textureUnits) { programId }
    private var programId = 0
    private var linked = false
    private var destroyed = false
    private val blocks = GlOwnedBlocks(device, program)
    private val ownedTextures = mutableListOf<Int>()
    private val framebufferColors = HashMap<Int, Int>()
    private var boundFramebuffer: Int = 0
    private val attribLocations = HashMap<String, Int>()
    private val deletedUserBuffers = HashSet<Int>()

    /** Compiles and links GLES stages from [ShaderProgram]; safe to call once per instance. */
    public fun link() {
        checkThread()
        if (destroyed) reject(GlCode.Destroyed, "Program is destroyed")
        if (linked) return
        blocks.rejectExcess()
        val id = when (program.target) {
            ShaderTarget.Agsl -> reject(GlCode.WrongTarget, "GL runtime requires a GLES shader")
            ShaderTarget.Gles30, ShaderTarget.Gles32 -> linkGraphicsProgram()
            ShaderTarget.Gles31 -> linkComputeProgram()
        }
        programId = id
        for (slot in program.spelledUniforms()) {
            val location = device.uniformLocation(id, slot.name)
            locations[slot.uniform] = location
            if (location < 0) {
                if (strictUniformLocations) {
                    reject(
                        GlCode.MissingUniformLocation,
                        "Uniform \"${slot.name}\" is not active in the linked program",
                    )
                }
                continue
            }
            val default = slot.uniform.default
            if (default != null && isGlFloatScalar(slot.uniform.shape)) {
                writeFloat(slot.uniform, location, default)
            }
            val components = slot.uniform.components
            if (components != null && isGlFloatVector(slot.uniform.shape)) {
                writeVector(slot.uniform, location, components)
            }
            if (components != null && slot.uniform.shape is Shape.Matrix) {
                writeMatrix(slot.uniform, location, components)
            }
            val intDefault = slot.uniform.intDefault
            if (intDefault != null && isGlIntScalar(slot.uniform.shape)) {
                writeInt(slot.uniform, location, intDefault)
            }
            val boolDefault = slot.uniform.boolDefault
            if (boolDefault != null && isGlBoolScalar(slot.uniform.shape)) {
                writeInt(slot.uniform, location, if (boolDefault) 1 else 0)
            }
        }
        linked = true
        if (strictUniformLocations) {
            device.flushGlErrors("link")
        }
    }

    private fun linkGraphicsProgram(): Int {
        val shaders = mutableListOf<Int>()
        try {
            shaders += compileStage(GlStage.Vertex, program.vertexSource())
            if (program.hasTessellation()) {
                shaders += compileStage(GlStage.TessControl, program.tessControlSource())
                shaders += compileStage(GlStage.TessEval, program.tessEvalSource())
            }
            if (program.hasGeometry()) {
                shaders += compileStage(GlStage.Geometry, program.geometrySource())
            }
            shaders += compileStage(GlStage.Fragment, program.fragmentSource())
        } catch (error: GlException) {
            shaders.forEach(device::deleteShader)
            throw error
        }
        val id = device.createProgram()
        if (id == 0) {
            shaders.forEach(device::deleteShader)
            reject(GlCode.LinkFailed, "Driver returned no program name")
        }
        shaders.forEach { device.attachShader(id, it) }
        val linkedStatus = device.linkProgram(id)
        shaders.forEach(device::deleteShader)
        if (!linkedStatus.ok) {
            device.deleteProgram(id)
            reject(GlCode.LinkFailed, linkedStatus.infoLog)
        }
        return id
    }

    private fun linkComputeProgram(): Int {
        val shader = compileStage(GlStage.Compute, program.computeSource())
        val id = device.createProgram()
        if (id == 0) {
            device.deleteShader(shader)
            reject(GlCode.LinkFailed, "Driver returned no program name")
        }
        device.attachShader(id, shader)
        val linkedStatus = device.linkProgram(id)
        device.deleteShader(shader)
        if (!linkedStatus.ok) {
            device.deleteProgram(id)
            reject(GlCode.LinkFailed, linkedStatus.infoLog)
        }
        return id
    }

    /**
     * Binds this program on the owning GL thread; call before draws and uniform uploads.
     * Also rebinds this program's uniform and storage blocks at their declaration-order binding
     * points, which another program on the same context may have taken. A texture upload binds on the active unit;
     * samplers whose unit no longer holds their texture are bound again here.
     */
    public fun use() {
        checkReady()
        device.useProgram(programId)
        blocks.bind()
        samplers.rebindDisturbed()
    }

    public fun set(uniform: Uniform<Flt<High>>, value: Float): Boolean = writeScalar(uniform, value)

    @JvmName("setMedFloat")
    public fun set(uniform: Uniform<Flt<Med>>, value: Float): Boolean = writeScalar(uniform, value)

    public fun set(uniform: Uniform<Vec2<Flt<High>>>, x: Float, y: Float): Boolean =
        writeVector(uniform, 2, x, y, 0f, 0f)

    @JvmName("setMedVec2")
    public fun set(uniform: Uniform<Vec2<Flt<Med>>>, x: Float, y: Float): Boolean =
        writeVector(uniform, 2, x, y, 0f, 0f)

    public fun set(uniform: Uniform<Vec3<Flt<High>>>, x: Float, y: Float, z: Float): Boolean =
        writeVector(uniform, 3, x, y, z, 0f)

    @JvmName("setMedVec3")
    public fun set(uniform: Uniform<Vec3<Flt<Med>>>, x: Float, y: Float, z: Float): Boolean =
        writeVector(uniform, 3, x, y, z, 0f)

    public fun set(uniform: Uniform<Vec4<Flt<High>>>, x: Float, y: Float, z: Float, w: Float): Boolean =
        writeVector(uniform, 4, x, y, z, w)

    @JvmName("setMedVec4")
    public fun set(uniform: Uniform<Vec4<Flt<Med>>>, x: Float, y: Float, z: Float, w: Float): Boolean =
        writeVector(uniform, 4, x, y, z, w)

    public fun bind(uniform: Uniform<Sampler2D>, texture: Int): Boolean =
        bindSampler(uniform, texture, cube = false)

    /**
     * Uploads an RGBA8 2D texture on the context thread and returns its GL name.
     *
     * Min and mag filters are linear, wrap is repeat, and only level 0 is stored.
     * This call does not build a mip chain and does not change the filter to a mipmap mode.
     * [rgba] is tightly packed R, G, B, A (`width * height * 4` bytes).
     * [destroy] deletes names uploaded here. A name remembered without the runtime that
     * created it is stale after the EGL context is recreated.
     */
    public fun uploadRgba(width: Int, height: Int, rgba: ByteArray): Int {
        checkReady()
        require(width > 0 && height > 0) { "Texture size must be positive, was ${width}x$height" }
        require(rgba.size == width * height * 4) {
            "RGBA texture needs ${width * height * 4} bytes, was ${rgba.size}"
        }
        val name = device.createTexture()
        textureUnits.disturbActive()
        device.texture2DLinearRepeat(name)
        device.texImage2DRgba(name, width, height, rgba)
        ownedTextures += name
        checkDriver("upload")
        return name
    }

    /**
     * Device. Allocates a texture name on the EGL thread that linked this runtime.
     * The name has no target until an upload binds one. [destroy] deletes it.
     */
    public fun createTexture(): Int {
        checkReady()
        val name = device.createTexture()
        ownedTextures += name
        return name
    }

    /** Deletes a texture name. Names from [uploadRgba] and [createTexture] are also deleted in [destroy]. */
    public fun deleteTexture(texture: Int) {
        checkThread()
        if (destroyed) reject(GlCode.Destroyed, "Program is destroyed")
        device.deleteTexture(texture)
        ownedTextures.remove(texture)
        samplers.forget(texture)
    }

    /**
     * Device. Replaces a rectangle of RGBA8 level 0 on the EGL thread that linked this runtime.
     * The rectangle is checked against [textureWidth] and [textureHeight] before the driver is called.
     * [rgba] is tightly packed and its size is `width * height * 4`.
     */
    public fun texSubImage2DRgba(
        texture: Int,
        textureWidth: Int,
        textureHeight: Int,
        x: Int,
        y: Int,
        width: Int,
        height: Int,
        rgba: ByteArray,
    ) {
        checkReady()
        require(textureWidth > 0 && textureHeight > 0) {
            "Texture size must be positive, was ${textureWidth}x$textureHeight"
        }
        require(x >= 0 && y >= 0 && width > 0 && height > 0) {
            "Texture update origin and size must be positive, was ($x, $y, $width, $height)"
        }
        require(x + width <= textureWidth && y + height <= textureHeight) {
            "Texture update ($x, $y, $width, $height) is outside ${textureWidth}x$textureHeight"
        }
        require(rgba.size == width * height * 4) {
            "RGBA update needs ${width * height * 4} bytes, was ${rgba.size}"
        }
        textureUnits.disturbActive()
        device.texSubImage2DRgba(texture, x, y, width, height, rgba)
        checkDriver("upload")
    }

    /**
     * Device. Uploads RGBA8 level 0 of one [face] on the EGL thread that linked this runtime.
     * A missing face is not filled. The filter is linear with clamp-to-edge and no mip chain.
     * [destroy] deletes the name when this runtime allocated it.
     */
    public fun uploadCubeFace(texture: Int, face: CubeFace, width: Int, height: Int, rgba: ByteArray) {
        checkReady()
        require(width > 0 && height > 0) { "Cube face size must be positive, was ${width}x$height" }
        require(rgba.size == width * height * 4) {
            "Cube face needs ${width * height * 4} bytes, was ${rgba.size}"
        }
        textureUnits.disturbActive()
        device.textureCubeLinearClamp(texture)
        device.texImageCubeFace(texture, face, width, height, rgba)
        checkDriver("upload")
    }

    /**
     * Device. Builds mip levels for a 2D texture on the EGL thread that linked this runtime.
     * [uploadRgba] does not call this, and this call does not change the min filter.
     */
    public fun generateMipmap2D(texture: Int) {
        checkReady()
        textureUnits.disturbActive()
        device.generateMipmap2D(texture)
        checkDriver("generateMipmap2D")
    }

    /**
     * Device. Sets the 2D min filter to linear mipmap on the EGL thread that linked this runtime.
     * [generateMipmap2D] does not change the filter, so a minified sample stays on level 0 until this call.
     */
    public fun filterMipmap2D(texture: Int) {
        checkReady()
        textureUnits.disturbActive()
        device.filterMipmap2D(texture)
    }

    /**
     * Device. Allocates a color texture and a depth renderbuffer on the EGL thread that linked
     * this runtime, then checks the framebuffer status. An incomplete target is deleted and
     * reported as [GlCode.FramebufferIncomplete]. The color name is not added to the textures
     * [destroy] deletes; call [deleteColorTarget] while the context is current, before or after
     * [destroy]. Unbind this target before sampling [GlColorTarget.colorTexture]:
     * a pass must not sample the texture of the framebuffer that is currently bound.
     * Names are invalid after the EGL context is recreated.
     */
    public fun createColorTarget(width: Int, height: Int): GlColorTarget {
        checkThread()
        if (destroyed) reject(GlCode.Destroyed, "Program is destroyed")
        require(width > 0 && height > 0) { "Framebuffer size must be positive, was ${width}x$height" }
        val color = device.createTexture()
        textureUnits.disturbActive()
        device.texture2DLinearClamp(color)
        device.texImage2DRgbaAlloc(color, width, height)
        val depth = device.createRenderbuffer()
        val framebuffer = device.createFramebuffer()
        device.framebufferColor(framebuffer, color)
        device.framebufferDepth(framebuffer, depth, width, height)
        if (!device.framebufferComplete(framebuffer)) {
            device.bindFramebuffer(0)
            device.deleteFramebuffer(framebuffer)
            device.deleteRenderbuffer(depth)
            device.deleteTexture(color)
            reject(GlCode.FramebufferIncomplete, "Framebuffer is incomplete")
        }
        device.bindFramebuffer(0)
        framebufferColors[framebuffer] = color
        boundFramebuffer = 0
        return GlColorTarget(framebuffer, color, depth, width, height)
    }

    /**
     * Device. Deletes a color target on the EGL thread that linked this runtime.
     * This does not delete textures created by [uploadRgba].
     * [destroy] leaves these names in place, and this call still deletes them afterward,
     * while that EGL context is current. Do not call it after the context itself is gone.
     */
    public fun deleteColorTarget(target: GlColorTarget) {
        checkThread()
        device.deleteFramebuffer(target.framebuffer)
        device.deleteRenderbuffer(target.depthRenderbuffer)
        device.deleteTexture(target.colorTexture)
        samplers.forget(target.colorTexture)
        framebufferColors.remove(target.framebuffer)
        if (boundFramebuffer == target.framebuffer) boundFramebuffer = 0
    }

    /**
     * Device. Binds a framebuffer on the EGL thread that linked this runtime.
     * Pass zero before sampling a texture that was attached to the previous target.
     */
    public fun bindFramebuffer(framebuffer: Int) {
        checkThread()
        if (destroyed) reject(GlCode.Destroyed, "Program is destroyed")
        if (framebuffer != 0) {
            val color = framebufferColors[framebuffer]
            if (color != null && samplers.samples(color)) {
                reject(GlCode.FeedbackLoop, "Cannot sample the color texture of the bound framebuffer")
            }
        }
        boundFramebuffer = framebuffer
        device.bindFramebuffer(framebuffer)
    }

    /**
     * Device. Reads RGBA8 of the framebuffer that is already bound into [into].
     * [into] must hold `width * height * 4` bytes. The size is checked before `glReadPixels`.
     * The framebuffer binding is left as it was.
     */
    public fun readFramebuffer(width: Int, height: Int, into: ByteArray) {
        checkThread()
        if (destroyed) reject(GlCode.Destroyed, "Program is destroyed")
        readFramebufferPixels(device, width, height, into)
        checkDriver("read")
    }

    /**
     * Device. Reads RGBA8 of [target] into [into].
     * The size is checked before any bind or `glReadPixels`. The framebuffer that was bound
     * before this call is bound again afterward.
     */
    public fun readColorTarget(target: GlColorTarget, into: ByteArray) {
        checkThread()
        if (destroyed) reject(GlCode.Destroyed, "Program is destroyed")
        readColorTargetPixels(device, boundFramebuffer, target, into)
        checkDriver("read")
    }

    /**
     * Device. Allocates a buffer name on the EGL thread that linked this runtime.
     * [destroy] does not delete it. [deleteBuffer] still deletes it afterward, while the
     * EGL context is current. The driver may reuse a deleted name; this call clears that mark
     * so a later [deleteBuffer] of the new object is legal.
     */
    public fun createBuffer(): Int {
        checkReady()
        val name = device.createBuffer()
        deletedUserBuffers -= name
        return name
    }

    /**
     * Device. Deletes a buffer name on the EGL thread that linked this runtime.
     * [destroy] leaves the name in place, and this call still deletes it afterward,
     * while that EGL context is current. A second delete of the same name fails.
     */
    public fun deleteBuffer(buffer: Int) {
        checkThread()
        check(buffer !in deletedUserBuffers) { "Buffer $buffer is already deleted" }
        device.deleteBuffer(buffer)
        deletedUserBuffers += buffer
    }

    /**
     * Device. Uploads [data] into an array buffer on the EGL thread that linked this runtime.
     * A matching [previousCount] uses `glBufferSubData`. Any other count uses `glBufferData`
     * with `GL_DYNAMIC_DRAW`. Returns the new float count.
     */
    public fun replaceArrayBuffer(buffer: Int, previousCount: Int, data: FloatArray): Int {
        checkReady()
        when (planBufferUpload(previousCount, data.size)) {
            BufferUploadKind.Sub -> device.arrayBufferSubData(buffer, data)
            BufferUploadKind.Full -> device.arrayBufferData(buffer, data)
        }
        return data.size
    }

    /**
     * Device. Uploads [indices] on the EGL thread that linked this runtime.
     * An index above [INDEX_SHORT_LIMIT] is stored as `GL_UNSIGNED_INT`.
     * An empty array is an argument error.
     * The upload binds vertex array 0 first, so it does not attach the buffer to the current mesh.
     * Returns the element width; pass it to [drawRange] for draws from this buffer.
     */
    public fun elementBufferData(buffer: Int, indices: IntArray): IndexElementKind {
        checkReady()
        val kind = indexElementKind(indices)
        device.unbindVertexArray()
        device.elementBufferData(buffer, indices, kind == IndexElementKind.UnsignedInt)
        return kind
    }

    /**
     * Device. Issues one draw on the EGL thread that linked this runtime.
     * Buffers are already bound by the caller. A null [elements] draws arrays; otherwise it is the
     * width [elementBufferData] returned for the bound element buffer. A vertex count of zero or a
     * draw [count] of zero issues no call. An [instanceCount] of zero issues no call.
     * An [instanceCount] of one still uses the instanced call.
     * [first] is a vertex index for arrays and an element index for indexed draws.
     */
    public fun drawRange(
        mode: Int,
        vertexCount: Int,
        first: Int,
        count: Int,
        elements: IndexElementKind?,
        instanceCount: Int?,
    ) {
        checkReady()
        require(first >= 0) { "Draw first must be non-negative, was $first" }
        samplers.rebindDisturbed()
        if (count <= 0 || vertexCount <= 0 || instanceCount == 0) return
        val kind = planDraw(
            vertexCount = vertexCount,
            indexCount = if (elements != null) count else null,
            instanceCount = instanceCount,
        )
        val unsignedInt = elements == IndexElementKind.UnsignedInt
        when (kind) {
            DrawKind.None -> Unit
            DrawKind.Arrays -> device.drawArrays(mode, first, count)
            DrawKind.Elements -> device.drawElements(mode, count, unsignedInt, first)
            DrawKind.ArraysInstanced ->
                device.drawArraysInstanced(mode, first, count, requireNotNull(instanceCount))
            DrawKind.ElementsInstanced -> device.drawElementsInstanced(
                mode,
                count,
                unsignedInt,
                requireNotNull(instanceCount),
                first,
            )
        }
        if (kind != DrawKind.None) checkDriver("draw")
    }

    /**
     * Device. Location of vertex attribute [name] in this program, or -1 when the linker
     * removed it. The driver is asked once per name.
     */
    public fun attribLocation(name: String): Int {
        checkReady()
        return attribLocations.getOrPut(name) { device.attribLocation(programId, name) }
    }

    /** Device. Sets one attribute divisor on the EGL thread that linked this runtime. */
    public fun vertexAttribDivisor(location: Int, divisor: Int) {
        checkReady()
        device.vertexAttribDivisor(location, divisor)
    }

    /**
     * Device. Disables one vertex attribute on the EGL thread that linked this runtime.
     * An instanced draw enables its matrix attributes; the host disables them afterward
     * so the next draw of that array does not read the instance buffer.
     */
    public fun disableVertexAttribArray(location: Int) {
        checkReady()
        device.disableVertexAttribArray(location)
    }

    /**
     * Device. Enables a float attribute on the EGL thread that linked this runtime.
     * [strideFloats] and [offsetFloats] count floats, not bytes.
     */
    public fun vertexAttribFloat(location: Int, size: Int, strideFloats: Int, offsetFloats: Int) {
        checkReady()
        device.vertexAttribFloat(location, size, strideFloats, offsetFloats)
    }

    @JvmName("bindCube")
    public fun bind(uniform: Uniform<SamplerCube>, texture: Int): Boolean =
        bindSampler(uniform, texture, cube = true)

    public fun set(uniform: Uniform<IntS>, value: Int): Boolean = writeIntUniform(uniform, value)

    public fun set(uniform: Uniform<BoolS>, value: Boolean): Boolean =
        writeIntUniform(uniform, if (value) 1 else 0)

    @JvmName("setMat2")
    public fun set(uniform: Uniform<Mat2>, values: FloatArray): Boolean = writeMatrixUniform(uniform, values, 4)

    @JvmName("setMat3")
    public fun set(uniform: Uniform<Mat3>, values: FloatArray): Boolean = writeMatrixUniform(uniform, values, 9)

    @JvmName("setMat4")
    public fun set(uniform: Uniform<Mat4>, values: FloatArray): Boolean = writeMatrixUniform(uniform, values, 16)

    public fun dispatch(x: Int, y: Int = 1, z: Int = 1, memoryBarrier: Boolean = true) {
        require(x >= 1 && y >= 1 && z >= 1) {
            "Compute dispatch size must be at least 1, was $x, $y, $z"
        }
        checkReady()
        if (program.target != ShaderTarget.Gles31) {
            reject(GlCode.WrongTarget, "dispatch requires a GLES 3.1 compute program")
        }
        use()
        device.dispatchCompute(x, y, z)
        if (memoryBarrier) {
            device.shaderStorageBarrier()
        }
        checkDriver("dispatch")
    }

    private fun bindSampler(uniform: Uniform<*>, texture: Int, cube: Boolean): Boolean {
        checkReady()
        val location = locationOf(uniform)
        if (boundFramebuffer != 0 && framebufferColors[boundFramebuffer] == texture) {
            reject(GlCode.FeedbackLoop, "Cannot sample the color texture of the bound framebuffer")
        }
        return samplers.bind(uniform, texture, cube, location)
    }

    public fun set(block: UniformBlock, values: FloatArray): Boolean {
        checkReady()
        val buffer = blocks.uniform(block)
        if (buffer.name == 0) {
            val index = device.uniformBlockIndex(programId, block.typeName)
            if (index < 0) {
                reject(
                    GlCode.UniformBlockNotBound,
                    "Uniform block \"${block.typeName}\" is not active in this program",
                )
            }
            device.uniformBlockBinding(programId, index, block.binding)
        }
        if (!buffer.pending(values)) return false
        val wrote = buffer.write(packStd140(block, values))
        buffer.remember(values)
        if (!wrote) return false
        buffer.bind()
        checkDriver("uniformBlock")
        return true
    }

    @JvmName("setStorage")
    public fun set(block: StorageBlock, values: FloatArray): Boolean {
        checkReady()
        val buffer = blocks.storage(block)
        if (!buffer.pending(values)) return false
        val wrote = buffer.write(packStd430(block, values))
        buffer.remember(values)
        if (!wrote) return false
        buffer.bind()
        checkDriver("storageBlock")
        return true
    }

    /**
     * Device. Copies a storage block back into [into] on the EGL thread that linked this runtime.
     *
     * [into] receives logical floats, the same layout [set] accepts, without std430 padding.
     * [set] must have uploaded the block first: that upload fixes the byte size, including an
     * unsized tail. Returns the number of floats written. Shader writes from [dispatch] are
     * visible because this call waits on `GL_BUFFER_UPDATE_BARRIER_BIT`.
     */
    @JvmName("readStorage")
    public fun read(block: StorageBlock, into: FloatArray): Int {
        checkReady()
        val buffer = blocks.storage(block)
        val bytes = buffer.storedBytes()
        require(bytes > 0) { "Storage block \"${block.name}\" has not been uploaded" }
        val count = buffer.storedFloats()
        require(into.size >= count) {
            "Storage block \"${block.name}\" needs $count floats, was ${into.size}"
        }
        device.bufferUpdateBarrier()
        val mapped = device.mapShaderStorageRead(buffer.name, bytes)
        try {
            val countRead = unpackStd430(block, mapped, count, into)
            checkDriver("read")
            return countRead
        } finally {
            device.unmapShaderStorage(buffer.name)
        }
    }

    /** Deletes GL objects; the instance must not be used afterward. */
    public fun destroy() {
        checkThread()
        if (destroyed) return
        destroyed = true
        for (texture in ownedTextures) {
            device.deleteTexture(texture)
            textureUnits.forget(texture)
        }
        ownedTextures.clear()
        framebufferColors.clear()
        boundFramebuffer = 0
        blocks.delete()
        if (programId != 0) {
            device.useProgram(0)
            device.deleteProgram(programId)
            programId = 0
        }
        linked = false
        locations.clear()
        floatValues.clear()
        vectorValues.clear()
        intValues.clear()
        samplers.clear()
    }

    private fun checkDriver(where: String) {
        if (!strictErrors) return
        val error = device.takeGlError()
        if (error != 0) {
            reject(GlCode.DriverError, "OpenGL error 0x${Integer.toHexString(error)} after $where")
        }
    }

    private fun writeScalar(uniform: Uniform<*>, value: Float): Boolean {
        checkReady()
        require(isGlFloatScalar(uniform.shape)) { "GL float uniform must be a float" }
        val location = locationOf(uniform)
        if (location < 0) return false
        return writeFloat(uniform, location, value)
    }

    private fun writeVector(uniform: Uniform<*>, width: Int, x: Float, y: Float, z: Float, w: Float): Boolean {
        checkReady()
        require(isGlFloatVector(uniform.shape)) { "GL vector uniform must be a float vector" }
        val location = locationOf(uniform)
        if (location < 0) return false
        return storeVector(uniform, location, width, x, y, z, w)
    }

    private fun writeVector(uniform: Uniform<*>, location: Int, value: FloatArray): Boolean = storeVector(
        uniform,
        location,
        value.size,
        value[0],
        value[1],
        value.getOrElse(2) { 0f },
        value.getOrElse(3) { 0f },
    )

    private fun storeVector(
        uniform: Uniform<*>,
        location: Int,
        width: Int,
        x: Float,
        y: Float,
        z: Float,
        w: Float,
    ): Boolean {
        require(width in 2..4) { "Vector uniform width must be 2, 3, or 4" }
        val previous = vectorValues[uniform]
        val cached = if (previous != null && previous.size == width) previous else FloatArray(width)
        if (cached === previous && sameComponents(cached, x, y, z, w)) return false
        cached[0] = x
        cached[1] = y
        if (width > 2) cached[2] = z
        if (width > 3) cached[3] = w
        vectorValues[uniform] = cached
        device.useProgram(programId)
        when (width) {
            2 -> device.uniform2f(location, x, y)
            3 -> device.uniform3f(location, x, y, z)
            else -> device.uniform4f(location, x, y, z, w)
        }
        return true
    }

    private fun writeIntUniform(uniform: Uniform<*>, value: Int): Boolean {
        checkReady()
        val location = locationOf(uniform)
        if (location < 0) return false
        return writeInt(uniform, location, value)
    }

    private fun writeInt(uniform: Uniform<*>, location: Int, value: Int): Boolean {
        val previous = intValues[uniform]
        if (previous != null && previous == value) return false
        intValues[uniform] = value
        device.useProgram(programId)
        device.uniform1i(location, value)
        return true
    }

    private fun writeMatrixUniform(uniform: Uniform<*>, values: FloatArray, expected: Int): Boolean {
        checkReady()
        require(values.size == expected) {
            "Matrix uniform expects $expected floats, was ${values.size}"
        }
        require(uniform.shape is Shape.Matrix) { "GL matrix uniform must be a matrix" }
        val location = locationOf(uniform)
        if (location < 0) return false
        return writeMatrix(uniform, location, values)
    }

    private fun writeMatrix(uniform: Uniform<*>, location: Int, values: FloatArray): Boolean {
        val previous = vectorValues[uniform]
        if (previous != null && sameVector(previous, values)) return false
        val cached = values.copyOf()
        vectorValues[uniform] = cached
        device.useProgram(programId)
        when (cached.size) {
            4 -> device.uniformMatrix2fv(location, cached)
            9 -> device.uniformMatrix3fv(location, cached)
            16 -> device.uniformMatrix4fv(location, cached)
            else -> error("Matrix uniform width must be 4, 9, or 16")
        }
        return true
    }

    private fun writeFloat(uniform: Uniform<*>, location: Int, value: Float): Boolean {
        val previous = floatValues[uniform]
        if (previous != null && sameFloatUniformValue(previous, value)) return false
        floatValues[uniform] = value
        device.useProgram(programId)
        device.uniform1f(location, value)
        return true
    }

    private fun locationOf(uniform: Uniform<*>): Int {
        val location = locations[uniform]
        require(location != null) { "Uniform does not belong to this shader" }
        return location
    }

    private fun compileStage(stage: GlStage, source: String): Int {
        val shader = device.createShader(stage)
        device.shaderSource(shader, source)
        val status = device.compileShader(shader)
        if (!status.ok) {
            device.deleteShader(shader)
            reject(GlCode.CompileFailed, status.infoLog)
        }
        return shader
    }

    private fun checkReady() {
        checkThread()
        if (destroyed) reject(GlCode.Destroyed, "Program is destroyed")
        if (!linked) reject(GlCode.NotLinked, "Program is not linked")
    }

    private fun checkThread() {
        if (Thread.currentThread() !== contextThread) {
            reject(GlCode.WrongThread, "GLES calls must stay on the context thread")
        }
    }
}

private fun isGlFloatScalar(shape: Shape): Boolean =
    shape is Shape.Scalar && shape.kind == ScalarKind.Float

private fun isGlFloatVector(shape: Shape): Boolean =
    shape is Shape.Vector && shape.kind == ScalarKind.Float

private fun isGlIntScalar(shape: Shape): Boolean =
    shape is Shape.Scalar && shape.kind == ScalarKind.Int

private fun isGlBoolScalar(shape: Shape): Boolean =
    shape is Shape.Scalar && shape.kind == ScalarKind.Bool

private fun sameComponents(previous: FloatArray, x: Float, y: Float, z: Float, w: Float): Boolean {
    if (!sameFloatUniformValue(previous[0], x) || !sameFloatUniformValue(previous[1], y)) return false
    if (previous.size > 2 && !sameFloatUniformValue(previous[2], z)) return false
    return previous.size <= 3 || sameFloatUniformValue(previous[3], w)
}

private fun sameVector(previous: FloatArray, value: FloatArray): Boolean {
    if (previous.size != value.size) return false
    for (index in previous.indices) {
        if (!sameFloatUniformValue(previous[index], value[index])) return false
    }
    return true
}

internal fun reject(code: GlCode, message: String): Nothing = throw GlException(code, message)
