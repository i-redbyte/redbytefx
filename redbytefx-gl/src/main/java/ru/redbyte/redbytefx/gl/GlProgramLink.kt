package ru.redbyte.redbytefx.gl

import ru.redbyte.redbytefx.ShaderProgram

internal fun linkGraphicsProgram(program: ShaderProgram, device: GlDevice): Int {
    val shaders = mutableListOf<Int>()
    var id = 0
    var success = false
    try {
        shaders += compileStage(device, GlStage.Vertex, program.vertexSource())
        if (program.hasTessellation()) {
            shaders += compileStage(device, GlStage.TessControl, program.tessControlSource())
            shaders += compileStage(device, GlStage.TessEval, program.tessEvalSource())
        }
        if (program.hasGeometry()) {
            shaders += compileStage(device, GlStage.Geometry, program.geometrySource())
        }
        shaders += compileStage(device, GlStage.Fragment, program.fragmentSource())
        id = device.createProgram()
        if (id == 0) {
            reject(GlCode.LinkFailed, "Driver returned no program name")
        }
        shaders.forEach { device.attachShader(id, it) }
        val linkedStatus = device.linkProgram(id)
        if (!linkedStatus.ok) {
            reject(GlCode.LinkFailed, linkedStatus.infoLog)
        }
        success = true
        return id
    } finally {
        shaders.forEach(device::deleteShader)
        if (!success && id != 0) device.deleteProgram(id)
    }
}

internal fun linkComputeProgram(program: ShaderProgram, device: GlDevice): Int {
    val shader = compileStage(device, GlStage.Compute, program.computeSource())
    var id = 0
    var success = false
    try {
        id = device.createProgram()
        if (id == 0) {
            reject(GlCode.LinkFailed, "Driver returned no program name")
        }
        device.attachShader(id, shader)
        val linkedStatus = device.linkProgram(id)
        if (!linkedStatus.ok) {
            reject(GlCode.LinkFailed, linkedStatus.infoLog)
        }
        success = true
        return id
    } finally {
        device.deleteShader(shader)
        if (!success && id != 0) device.deleteProgram(id)
    }
}

private fun compileStage(device: GlDevice, stage: GlStage, source: String): Int {
    val shader = device.createShader(stage)
    if (shader == 0) reject(GlCode.CompileFailed, "Driver returned no shader name for $stage")
    try {
        device.shaderSource(shader, source)
        val status = device.compileShader(shader)
        if (!status.ok) reject(GlCode.CompileFailed, status.infoLog)
        return shader
    } catch (error: Throwable) {
        device.deleteShader(shader)
        throw error
    }
}
