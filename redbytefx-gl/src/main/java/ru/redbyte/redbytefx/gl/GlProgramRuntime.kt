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
import ru.redbyte.redbytefx.Uniform
import ru.redbyte.redbytefx.Vec2
import ru.redbyte.redbytefx.Vec3
import ru.redbyte.redbytefx.Vec4
import ru.redbyte.redbytefx.StorageBlock
import ru.redbyte.redbytefx.UniformBlock
import ru.redbyte.redbytefx.packStd140
import ru.redbyte.redbytefx.packStd430
import ru.redbyte.redbytefx.sameFloatUniformValue
import java.util.IdentityHashMap
import kotlin.jvm.JvmName

public enum class GlCode {
    WrongThread,
    WrongTarget,
    NotLinked,
    Destroyed,
    CompileFailed,
    LinkFailed,
    TextureUnitLimit,
    UniformBlockNotBound,
    MissingUniformLocation,
}

public class GlException(
    public val code: GlCode,
    message: String,
) : IllegalStateException(message)

/**
 * One GLES program bound to the thread that created it.
 *
 * Uniform locations are queried once, after the device reports a successful link.
 * An unchanged float or texture does not call the device again.
 */
public class GlProgramRuntime(
    private val program: ShaderProgram,
    private val device: GlDevice,
    private val contextThread: Thread = Thread.currentThread(),
    private val strictUniformLocations: Boolean = false,
) {
    private val locations = IdentityHashMap<Uniform<*>, Int>()
    private val floatValues = IdentityHashMap<Uniform<*>, Float>()
    private val vectorValues = IdentityHashMap<Uniform<*>, FloatArray>()
    private val intValues = IdentityHashMap<Uniform<*>, Int>()
    private val textureUnits = IdentityHashMap<Uniform<*>, Int>()
    private val textureIds = IdentityHashMap<Uniform<*>, Int>()
    private var programId = 0
    private var nextTextureUnit = 0
    private var linked = false
    private var destroyed = false
    private var bufferId = 0
    private var blockBytes: ByteArray? = null
    private var blockFloats: FloatArray? = null
    private var storageBufferId = 0
    private var storageBytes: ByteArray? = null
    private var storageFloats: FloatArray? = null

    public fun link() {
        checkThread()
        if (destroyed) reject(GlCode.Destroyed, "Program is destroyed")
        if (linked) return
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

    public fun use() {
        checkReady()
        device.useProgram(programId)
    }

    public fun set(uniform: Uniform<Flt<High>>, value: Float): Boolean = writeScalar(uniform, value)

    @JvmName("setMedFloat")
    public fun set(uniform: Uniform<Flt<Med>>, value: Float): Boolean = writeScalar(uniform, value)

    public fun set(uniform: Uniform<Vec2<Flt<High>>>, x: Float, y: Float): Boolean =
        writeVector(uniform, floatArrayOf(x, y))

    @JvmName("setMedVec2")
    public fun set(uniform: Uniform<Vec2<Flt<Med>>>, x: Float, y: Float): Boolean =
        writeVector(uniform, floatArrayOf(x, y))

    public fun set(uniform: Uniform<Vec3<Flt<High>>>, x: Float, y: Float, z: Float): Boolean =
        writeVector(uniform, floatArrayOf(x, y, z))

    @JvmName("setMedVec3")
    public fun set(uniform: Uniform<Vec3<Flt<Med>>>, x: Float, y: Float, z: Float): Boolean =
        writeVector(uniform, floatArrayOf(x, y, z))

    public fun set(uniform: Uniform<Vec4<Flt<High>>>, x: Float, y: Float, z: Float, w: Float): Boolean =
        writeVector(uniform, floatArrayOf(x, y, z, w))

    @JvmName("setMedVec4")
    public fun set(uniform: Uniform<Vec4<Flt<Med>>>, x: Float, y: Float, z: Float, w: Float): Boolean =
        writeVector(uniform, floatArrayOf(x, y, z, w))

    public fun bind(uniform: Uniform<Sampler2D>, texture: Int): Boolean =
        bindSampler(uniform, texture, Shape.Sampler2D, device::bindTexture2D)

    @JvmName("bindCube")
    public fun bind(uniform: Uniform<SamplerCube>, texture: Int): Boolean =
        bindSampler(uniform, texture, Shape.SamplerCube, device::bindTextureCube)

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
        device.useProgram(programId)
        device.dispatchCompute(x, y, z)
        if (memoryBarrier) {
            device.shaderStorageBarrier()
        }
    }

    private fun bindSampler(
        uniform: Uniform<*>,
        texture: Int,
        expected: Shape,
        bind: (Int) -> Unit,
    ): Boolean {
        checkReady()
        require(uniform.shape == expected) { "GL sampler bind requires $expected" }
        val location = locationOf(uniform)
        if (location < 0) return false
        val unit = textureUnits[uniform] ?: assignUnit(uniform, location)
        val previous = textureIds[uniform]
        if (previous != null && previous == texture) return false
        textureIds[uniform] = texture
        device.activeTexture(unit)
        bind(texture)
        return true
    }

    public fun set(block: UniformBlock, values: FloatArray): Boolean {
        checkReady()
        require(block === program.uniformBlock) { "Uniform block does not belong to this shader" }
        val previousFloats = blockFloats
        if (previousFloats != null && previousFloats.contentEquals(values)) return false
        val packed = packStd140(block, values)
        val previous = blockBytes
        if (previous != null && previous.contentEquals(packed)) {
            blockFloats = values.copyOf()
            return false
        }
        if (bufferId == 0) {
            val created = device.createBuffer()
            require(created != 0) { "Driver returned no buffer name" }
            bufferId = created
            device.uniformBufferData(bufferId, packed)
            device.useProgram(programId)
            val index = device.uniformBlockIndex(programId, block.typeName)
            if (index < 0) {
                reject(
                    GlCode.UniformBlockNotBound,
                    "Uniform block \"${block.typeName}\" is not active in this program",
                )
            }
            device.uniformBlockBinding(programId, index, 0)
            device.bindUniformBufferBase(bufferId, 0)
        } else {
            device.uniformBufferSubData(bufferId, packed)
        }
        blockBytes = packed.copyOf()
        blockFloats = values.copyOf()
        return true
    }

    @JvmName("setStorage")
    public fun set(block: StorageBlock, values: FloatArray): Boolean {
        checkReady()
        require(block === program.storageBlock) { "Storage block does not belong to this shader" }
        val previousFloats = storageFloats
        if (previousFloats != null && previousFloats.contentEquals(values)) return false
        val packed = packStd430(block, values)
        val previous = storageBytes
        if (previous != null && previous.contentEquals(packed)) {
            storageFloats = values.copyOf()
            return false
        }
        if (storageBufferId == 0) {
            val created = device.createBuffer()
            require(created != 0) { "Driver returned no buffer name" }
            storageBufferId = created
            device.shaderStorageData(storageBufferId, packed)
            device.useProgram(programId)
            device.bindShaderStorageBase(storageBufferId, 0)
        } else if (previous == null || previous.size != packed.size) {
            device.shaderStorageData(storageBufferId, packed)
        } else {
            device.shaderStorageSubData(storageBufferId, packed)
        }
        storageBytes = packed.copyOf()
        storageFloats = values.copyOf()
        return true
    }

    public fun destroy() {
        checkThread()
        if (destroyed) return
        destroyed = true
        if (bufferId != 0) {
            device.bindUniformBufferBase(0, 0)
            device.deleteBuffer(bufferId)
            bufferId = 0
        }
        blockBytes = null
        blockFloats = null
        if (storageBufferId != 0) {
            device.bindShaderStorageBase(0, 0)
            device.deleteBuffer(storageBufferId)
            storageBufferId = 0
        }
        storageBytes = null
        storageFloats = null
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
        textureUnits.clear()
        textureIds.clear()
        nextTextureUnit = 0
    }

    private fun assignUnit(uniform: Uniform<*>, location: Int): Int {
        val limit = device.maxCombinedTextureImageUnits()
        if (nextTextureUnit >= limit) {
            reject(
                GlCode.TextureUnitLimit,
                "Texture unit $nextTextureUnit is outside GL_MAX_COMBINED_TEXTURE_IMAGE_UNITS $limit",
            )
        }
        val unit = nextTextureUnit
        nextTextureUnit += 1
        textureUnits[uniform] = unit
        device.useProgram(programId)
        device.uniform1i(location, unit)
        return unit
    }

    private fun writeScalar(uniform: Uniform<*>, value: Float): Boolean {
        checkReady()
        require(isGlFloatScalar(uniform.shape)) { "GL float uniform must be a float" }
        val location = locationOf(uniform)
        if (location < 0) return false
        return writeFloat(uniform, location, value)
    }

    private fun writeVector(uniform: Uniform<*>, value: FloatArray): Boolean {
        checkReady()
        require(isGlFloatVector(uniform.shape)) { "GL vector uniform must be a float vector" }
        val location = locationOf(uniform)
        if (location < 0) return false
        return writeVector(uniform, location, value)
    }

    private fun writeVector(uniform: Uniform<*>, location: Int, value: FloatArray): Boolean {
        val previous = vectorValues[uniform]
        if (previous != null && sameVector(previous, value)) return false
        vectorValues[uniform] = value.copyOf()
        device.useProgram(programId)
        when (value.size) {
            2 -> device.uniform2f(location, value[0], value[1])
            3 -> device.uniform3f(location, value[0], value[1], value[2])
            4 -> device.uniform4f(location, value[0], value[1], value[2], value[3])
            else -> error("Vector uniform width must be 2, 3, or 4")
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
        vectorValues[uniform] = values.copyOf()
        device.useProgram(programId)
        when (values.size) {
            4 -> device.uniformMatrix2fv(location, values)
            9 -> device.uniformMatrix3fv(location, values)
            16 -> device.uniformMatrix4fv(location, values)
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

private fun sameVector(previous: FloatArray, value: FloatArray): Boolean {
    if (previous.size != value.size) return false
    for (index in previous.indices) {
        if (!sameFloatUniformValue(previous[index], value[index])) return false
    }
    return true
}

private fun reject(code: GlCode, message: String): Nothing = throw GlException(code, message)
