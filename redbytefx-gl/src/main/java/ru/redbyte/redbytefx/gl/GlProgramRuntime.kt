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
import ru.redbyte.redbytefx.sameFloatUniformValue
import java.util.IdentityHashMap

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

    public fun link() {
        checkThread()
        if (destroyed) reject(GlCode.Destroyed, "Program is destroyed")
        if (linked) return
        if (program.target != ShaderTarget.Gles30) {
            reject(GlCode.WrongTarget, "GL runtime requires a GLES 3.0 shader")
        }
        val vertex = compileStage(GlStage.Vertex, program.vertexSource())
        val fragment = try {
            compileStage(GlStage.Fragment, program.fragmentSource())
        } catch (error: GlException) {
            device.deleteShader(vertex)
            throw error
        }
        val id = device.createProgram()
        if (id == 0) {
            device.deleteShader(vertex)
            device.deleteShader(fragment)
            reject(GlCode.LinkFailed, "Driver returned no program name")
        }
        device.attachShader(id, vertex)
        device.attachShader(id, fragment)
        val linkedStatus = device.linkProgram(id)
        device.deleteShader(vertex)
        device.deleteShader(fragment)
        if (!linkedStatus.ok) {
            device.deleteProgram(id)
            reject(GlCode.LinkFailed, linkedStatus.infoLog)
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

    public fun destroy() {
        checkThread()
        if (destroyed) return
        destroyed = true
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
