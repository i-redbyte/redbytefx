package ru.redbyte.redbytefx.gl

import ru.redbyte.redbytefx.Flt
import ru.redbyte.redbytefx.High
import ru.redbyte.redbytefx.Precision
import ru.redbyte.redbytefx.Sampler2D
import ru.redbyte.redbytefx.ScalarKind
import ru.redbyte.redbytefx.ShaderProgram
import ru.redbyte.redbytefx.ShaderTarget
import ru.redbyte.redbytefx.Shape
import ru.redbyte.redbytefx.Uniform
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
) {
    private val locations = IdentityHashMap<Uniform<*>, Int>()
    private val floatValues = IdentityHashMap<Uniform<*>, Float>()
    private val textureUnits = IdentityHashMap<Uniform<*>, Int>()
    private val textureIds = IdentityHashMap<Uniform<*>, Int>()
    private var programId = 0
    private var nextTextureUnit = 0
    private var linked = false
    private var destroyed = false
    private var bufferId = 0
    private var blockBytes: ByteArray? = null
    private var storageBufferId = 0
    private var storageBytes: ByteArray? = null

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
            val default = slot.uniform.default ?: continue
            if (location >= 0 && isHighFloat(slot.uniform.shape)) {
                writeFloat(slot.uniform, location, default)
            }
        }
        linked = true
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

    public fun set(uniform: Uniform<Flt<High>>, value: Float): Boolean {
        checkReady()
        require(isHighFloat(uniform.shape)) { "GL float uniform must be a highp float" }
        val location = locationOf(uniform)
        if (location < 0) return false
        return writeFloat(uniform, location, value)
    }

    public fun bind(uniform: Uniform<Sampler2D>, texture: Int): Boolean {
        checkReady()
        require(uniform.shape == Shape.Sampler2D) { "GL sampler bind requires sampler2D" }
        val location = locationOf(uniform)
        if (location < 0) return false
        val unit = textureUnits[uniform] ?: assignUnit(uniform, location)
        val previous = textureIds[uniform]
        if (previous != null && previous == texture) return false
        textureIds[uniform] = texture
        device.activeTexture(unit)
        device.bindTexture2D(texture)
        return true
    }

    public fun set(block: UniformBlock, values: FloatArray): Boolean {
        checkReady()
        require(block === program.uniformBlock) { "Uniform block does not belong to this shader" }
        val packed = packStd140(block, values)
        val previous = blockBytes
        if (previous != null && previous.contentEquals(packed)) return false
        if (bufferId == 0) {
            val created = device.createBuffer()
            require(created != 0) { "Driver returned no buffer name" }
            bufferId = created
            device.uniformBufferData(bufferId, packed)
            device.useProgram(programId)
            val index = device.uniformBlockIndex(programId, block.typeName)
            if (index >= 0) device.uniformBlockBinding(programId, index, 0)
            device.bindUniformBufferBase(bufferId, 0)
        } else {
            device.uniformBufferSubData(bufferId, packed)
        }
        blockBytes = packed.copyOf()
        return true
    }

    @JvmName("setStorage")
    public fun set(block: StorageBlock, values: FloatArray): Boolean {
        checkReady()
        require(block === program.storageBlock) { "Storage block does not belong to this shader" }
        val packed = packStd430(block, values)
        val previous = storageBytes
        if (previous != null && previous.contentEquals(packed)) return false
        if (storageBufferId == 0) {
            val created = device.createBuffer()
            require(created != 0) { "Driver returned no buffer name" }
            storageBufferId = created
            device.shaderStorageData(storageBufferId, packed)
            device.useProgram(programId)
            device.bindShaderStorageBase(storageBufferId, 0)
        } else {
            device.shaderStorageSubData(storageBufferId, packed)
        }
        storageBytes = packed.copyOf()
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
        if (storageBufferId != 0) {
            device.bindShaderStorageBase(0, 0)
            device.deleteBuffer(storageBufferId)
            storageBufferId = 0
        }
        storageBytes = null
        if (programId != 0) {
            device.useProgram(0)
            device.deleteProgram(programId)
            programId = 0
        }
        linked = false
    }

    private fun assignUnit(uniform: Uniform<*>, location: Int): Int {
        val unit = nextTextureUnit
        nextTextureUnit += 1
        textureUnits[uniform] = unit
        device.useProgram(programId)
        device.uniform1i(location, unit)
        return unit
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

private fun isHighFloat(shape: Shape): Boolean =
    shape is Shape.Scalar && shape.kind == ScalarKind.Float && shape.precision == Precision.High

private fun reject(code: GlCode, message: String): Nothing = throw GlException(code, message)
