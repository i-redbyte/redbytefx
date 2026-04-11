package ru.redbyte.redbytefx

import java.util.IdentityHashMap

internal interface FxRuntimeUniformWriter {
    fun setFloat(name: String, value: Float)
    fun setFloat2(name: String, x: Float, y: Float)
    fun setFloat3(name: String, x: Float, y: Float, z: Float)
    fun setFloat4(name: String, x: Float, y: Float, z: Float, w: Float)
}

internal class FxRuntimeState(
    private val program: FxProgram,
    private val writer: FxRuntimeUniformWriter,
    private val onRuntimeChanged: () -> Unit
) {
    private var batchDepth: Int = 0
    private var pendingRefreshNotification: Boolean = false
    private var lastResolutionWidth: Float? = null
    private var lastResolutionHeight: Float? = null
    private val floatValues = IdentityHashMap<FxParam.Float, Float>()
    private val float2Values = IdentityHashMap<FxParam.Float2, RuntimeFloat2>()
    private val float3Values = IdentityHashMap<FxParam.Float3, RuntimeFloat3>()
    private val float4Values = IdentityHashMap<FxParam.Float4, RuntimeFloat4>()

    fun applyDefaults() {
        for ((param, value) in program.defaults) {
            when (param) {
                is FxParam.Float -> {
                    val default = value as? DefaultValue.F ?: continue
                    floatValues[param] = default.v
                    writer.setFloat(requireFloatUniformName(param), default.v)
                }

                is FxParam.Float2 -> {
                    val default = value as? DefaultValue.F2 ?: continue
                    float2Values[param] = RuntimeFloat2(default.x, default.y)
                    writer.setFloat2(requireFloat2UniformName(param), default.x, default.y)
                }

                is FxParam.Float3 -> {
                    val default = value as? DefaultValue.F3 ?: continue
                    float3Values[param] = RuntimeFloat3(default.x, default.y, default.z)
                    writer.setFloat3(requireFloat3UniformName(param), default.x, default.y, default.z)
                }

                is FxParam.Float4 -> {
                    val default = value as? DefaultValue.F4 ?: continue
                    float4Values[param] = RuntimeFloat4(default.x, default.y, default.z, default.w)
                    writer.setFloat4(requireFloat4UniformName(param), default.x, default.y, default.z, default.w)
                }
            }
        }
    }

    /**
     * Coalesces [notifyRuntimeChanged] so multiple uniform updates in [block] can produce a single
     * [onRuntimeChanged] callback (one [RenderEffect] rebuild in [FxInstanceImpl]).
     */
    internal fun withBatch(block: () -> Unit) {
        batchDepth++
        try {
            block()
        } finally {
            batchDepth--
            if (batchDepth == 0 && pendingRefreshNotification) {
                pendingRefreshNotification = false
                onRuntimeChanged()
            }
        }
    }

    private fun notifyRuntimeChanged() {
        if (batchDepth > 0) {
            pendingRefreshNotification = true
        } else {
            onRuntimeChanged()
        }
    }

    fun setFloat(param: FxParam.Float, value: Float): Boolean {
        val previous = floatValues[param]
        if (previous != null && sameFloatUniformValue(previous, value)) return false
        floatValues[param] = value
        writer.setFloat(requireFloatUniformName(param), value)
        notifyRuntimeChanged()
        return true
    }

    fun setFloat2(param: FxParam.Float2, x: Float, y: Float): Boolean {
        if (sameRuntimeFloat2(float2Values[param], x, y)) return false
        float2Values[param] = RuntimeFloat2(x, y)
        writer.setFloat2(requireFloat2UniformName(param), x, y)
        notifyRuntimeChanged()
        return true
    }

    fun setFloat3(param: FxParam.Float3, x: Float, y: Float, z: Float): Boolean {
        if (sameRuntimeFloat3(float3Values[param], x, y, z)) return false
        float3Values[param] = RuntimeFloat3(x, y, z)
        writer.setFloat3(requireFloat3UniformName(param), x, y, z)
        notifyRuntimeChanged()
        return true
    }

    fun setFloat4(param: FxParam.Float4, x: Float, y: Float, z: Float, w: Float): Boolean {
        if (sameRuntimeFloat4(float4Values[param], x, y, z, w)) return false
        float4Values[param] = RuntimeFloat4(x, y, z, w)
        writer.setFloat4(requireFloat4UniformName(param), x, y, z, w)
        notifyRuntimeChanged()
        return true
    }

    fun setResolution(widthPx: Float, heightPx: Float): Boolean {
        val width = sanitizeResolution(widthPx)
        val height = sanitizeResolution(heightPx)
        if (sameRuntimeResolution(width, height)) return false
        lastResolutionWidth = width
        lastResolutionHeight = height
        writer.setFloat2(RB_RESOLUTION_UNIFORM, width, height)
        notifyRuntimeChanged()
        return true
    }

    private fun requireFloatUniformName(param: FxParam.Float): String =
        program.layout.floatUniforms[param]
            ?: compileFail(missingUniformBindingDiagnostic("Float", param.debugName))

    private fun requireFloat2UniformName(param: FxParam.Float2): String =
        program.layout.float2Uniforms[param]
            ?: compileFail(missingUniformBindingDiagnostic("Float2", param.debugName))

    private fun requireFloat3UniformName(param: FxParam.Float3): String =
        program.layout.float3Uniforms[param]
            ?: compileFail(missingUniformBindingDiagnostic("Float3", param.debugName))

    private fun requireFloat4UniformName(param: FxParam.Float4): String =
        program.layout.float4Uniforms[param]
            ?: compileFail(missingUniformBindingDiagnostic("Float4", param.debugName))

    private fun sanitizeResolution(value: Float): Float = if (value > 0f) value else 1f

    private fun sameRuntimeResolution(width: Float, height: Float): Boolean =
        lastResolutionWidth != null &&
            lastResolutionHeight != null &&
            sameFloatUniformValue(checkNotNull(lastResolutionWidth), width) &&
            sameFloatUniformValue(checkNotNull(lastResolutionHeight), height)
}

private data class RuntimeFloat2(
    val x: Float,
    val y: Float
)

private data class RuntimeFloat3(
    val x: Float,
    val y: Float,
    val z: Float
)

private data class RuntimeFloat4(
    val x: Float,
    val y: Float,
    val z: Float,
    val w: Float
)

private fun sameRuntimeFloat2(
    value: RuntimeFloat2?,
    x: Float,
    y: Float
): Boolean = value != null &&
    sameFloatUniformValue(value.x, x) &&
    sameFloatUniformValue(value.y, y)

private fun sameRuntimeFloat3(
    value: RuntimeFloat3?,
    x: Float,
    y: Float,
    z: Float
): Boolean = value != null &&
    sameFloatUniformValue(value.x, x) &&
    sameFloatUniformValue(value.y, y) &&
    sameFloatUniformValue(value.z, z)

private fun sameRuntimeFloat4(
    value: RuntimeFloat4?,
    x: Float,
    y: Float,
    z: Float,
    w: Float
): Boolean = value != null &&
    sameFloatUniformValue(value.x, x) &&
    sameFloatUniformValue(value.y, y) &&
    sameFloatUniformValue(value.z, z) &&
    sameFloatUniformValue(value.w, w)
