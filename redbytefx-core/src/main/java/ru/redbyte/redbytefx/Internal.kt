package ru.redbyte.redbytefx

import android.graphics.RenderEffect
import android.graphics.RuntimeShader

internal sealed interface DefaultValue {
    data class F(val v: Float) : DefaultValue
    data class F2(val x: Float, val y: Float) : DefaultValue
    data class F3(val x: Float, val y: Float, val z: Float) : DefaultValue
    data class F4(val x: Float, val y: Float, val z: Float, val w: Float) : DefaultValue
}

internal data class FxProgram(
    val agsl: String,
    val layout: UniformLayout,
    val defaults: Map<FxParam, DefaultValue>
)

internal fun sanitizeIdentifier(
    raw: String,
    prefix: String
): String {
    val cleaned = sanitizeSuggestedIdentifier(raw, leadingDigitPrefix = prefix)

    val normalized = if (cleaned.firstOrNull()?.isDigit() == true) {
        "${prefix}${cleaned}"
    } else {
        cleaned
    }

    return if (normalized.startsWith(prefix)) normalized else prefix + normalized
}

internal fun sanitizeSuggestedIdentifier(
    raw: String,
    leadingDigitPrefix: String
): String {
    val cleaned = buildString {
        raw.forEachIndexed { index, ch ->
            when {
                ch == '_' || !ch.isLetterOrDigit() -> {
                    if (isNotEmpty() && last() != '_') append('_')
                }
                ch.isUpperCase() -> {
                    val previous = raw.getOrNull(index - 1)
                    val next = raw.getOrNull(index + 1)
                    val boundaryFromLowerOrDigit = previous?.let { it.isLowerCase() || it.isDigit() } == true
                    val boundaryBeforeWordTail = previous?.isUpperCase() == true && next?.isLowerCase() == true
                    if (isNotEmpty() && last() != '_' && (boundaryFromLowerOrDigit || boundaryBeforeWordTail)) {
                        append('_')
                    }
                    append(ch.lowercaseChar())
                }
                else -> append(ch.lowercaseChar())
            }
        }
    }.trim('_').ifBlank { "value" }

    return if (cleaned.firstOrNull()?.isDigit() == true) {
        "$leadingDigitPrefix$cleaned"
    } else {
        cleaned
    }
}

internal fun missingUniformBindingMessage(
    typeLabel: String,
    debugName: String?
): String {
    val readableType = typeLabel.lowercase()
    val uniformLabel = debugName
        ?.takeIf { it.isNotBlank() }
        ?.let { " '$it'" }
        ?: " with a generated name"

    return "This effect instance does not contain the $readableType uniform$uniformLabel. " +
        "Bind only params declared by the same redbytefx { ... } effect when calling " +
        "set$typeLabel(...) or the matching controller bind API."
}

internal fun nonFiniteFloatLiteralMessage(value: Float): String =
    "Only finite float literals are supported in shader source, got $value. " +
        "Use uniformFloat(...) or another uniform helper for runtime-driven values."

internal fun unsupportedExpressionArgumentMessage(expr: Any): String {
    val typeName = expr::class.qualifiedName ?: expr::class.simpleName ?: "unknown"
    return "Unsupported expression argument: $typeName. " +
        "Shader helpers only accept DSL expressions such as FloatExpr, Float2Expr, Float3Expr, " +
        "Float4Expr, BoolExpr, or ColorExpr. Convert raw values with float(...), float2(...), " +
        "float3(...), float4(...), or color(...), or declare a uniform for runtime-driven inputs."
}

internal class UniformLayout(
    additionalOccupied: Set<String> = emptySet()
) {
    private var floatIndex = 0
    private var float2Index = 0
    private var float3Index = 0
    private var float4Index = 0
    private val occupiedNames = linkedSetOf(
        RB_INPUT_UNIFORM,
        RB_RESOLUTION_UNIFORM,
        "fragCoord",
        "main",
        "rb_maxCoord",
        "rb_sample"
    ).apply { addAll(additionalOccupied) }

    val floatUniforms = LinkedHashMap<FxParam.Float, String>()
    val float2Uniforms = LinkedHashMap<FxParam.Float2, String>()
    val float3Uniforms = LinkedHashMap<FxParam.Float3, String>()
    val float4Uniforms = LinkedHashMap<FxParam.Float4, String>()

    fun register(param: FxParam) {
        when (param) {
            is FxParam.Float -> uniformName(param)
            is FxParam.Float2 -> uniformName(param)
            is FxParam.Float3 -> uniformName(param)
            is FxParam.Float4 -> uniformName(param)
        }
    }

    fun uniformName(param: FxParam.Float): String =
        floatUniforms.getOrPut(param) { nextName(param.debugName, "uF${floatIndex++}") }

    fun uniformName(param: FxParam.Float2): String =
        float2Uniforms.getOrPut(param) { nextName(param.debugName, "uV${float2Index++}") }

    fun uniformName(param: FxParam.Float3): String =
        float3Uniforms.getOrPut(param) { nextName(param.debugName, "uV3_${float3Index++}") }

    fun uniformName(param: FxParam.Float4): String =
        float4Uniforms.getOrPut(param) { nextName(param.debugName, "uV4_${float4Index++}") }

    private fun nextName(debugName: String?, fallback: String): String {
        val base = debugName
            ?.takeIf { it.isNotBlank() }
            ?.let { sanitizeIdentifier(it, "u_") }
            ?: fallback

        var candidate = base
        var suffix = 1
        while (!occupiedNames.add(candidate)) {
            candidate = "${base}_${suffix++}"
        }
        return candidate
    }

    fun occupiedIdentifiers(): Set<String> = occupiedNames.toSet()
}

internal object FxBuilder {
    fun build(block: FxDsl.() -> ColorExpr): FxEffect {
        val defaults = LinkedHashMap<FxParam, DefaultValue>()
        val functions = mutableListOf<UserFunctionDefinition>()
        val usedFunctionNames = mutableSetOf<String>().apply {
            addAll(RESERVED_USER_FUNCTION_NAMES)
            add(sanitizeSuggestedIdentifier(RB_INPUT_UNIFORM, "fn_"))
            add(sanitizeSuggestedIdentifier(RB_RESOLUTION_UNIFORM, "fn_"))
            add(sanitizeSuggestedIdentifier("fragCoord", "fn_"))
            add(sanitizeSuggestedIdentifier("rb_maxCoord", "fn_"))
            add(sanitizeSuggestedIdentifier("rb_sample", "fn_"))
        }
        val dsl = FxDsl(defaults, functions, usedFunctionNames)
        val output = dsl.block()
        val program = FxCompiler.compile(
            output = output,
            defaults = defaults.toMap(),
            functions = functions.toList()
        )
        return FxEffectImpl(program)
    }
}

internal class FxEffectImpl(
    private val program: FxProgram
) : FxEffect {
    override fun newInstance(): FxInstance = FxInstanceImpl(program)
    override fun agslSource(): String = program.agsl
}

internal class FxInstanceImpl(
    private val program: FxProgram
) : FxInstance {

    private val shader = RuntimeShader(program.agsl)
    // Platform RenderEffect instances do not reliably observe later RuntimeShader uniform updates,
    // so we recreate the effect after runtime state changes.
    private var renderEffect = createRenderEffect()

    init {
        applyDefaults()
        updateResolution(1f, 1f)
        refreshRenderEffect()
    }

    override fun renderEffect(): RenderEffect = renderEffect

    override fun setFloat(param: FxParam.Float, value: Float) {
        val name = program.layout.floatUniforms[param]
            ?: error(missingUniformBindingMessage("Float", param.debugName))
        shader.setFloatUniform(name, value)
        refreshRenderEffect()
    }

    override fun setFloat2(param: FxParam.Float2, x: Float, y: Float) {
        val name = program.layout.float2Uniforms[param]
            ?: error(missingUniformBindingMessage("Float2", param.debugName))
        shader.setFloatUniform(name, x, y)
        refreshRenderEffect()
    }

    override fun setFloat3(param: FxParam.Float3, x: Float, y: Float, z: Float) {
        val name = program.layout.float3Uniforms[param]
            ?: error(missingUniformBindingMessage("Float3", param.debugName))
        shader.setFloatUniform(name, x, y, z)
        refreshRenderEffect()
    }

    override fun setFloat4(param: FxParam.Float4, x: Float, y: Float, z: Float, w: Float) {
        val name = program.layout.float4Uniforms[param]
            ?: error(missingUniformBindingMessage("Float4", param.debugName))
        shader.setFloatUniform(name, x, y, z, w)
        refreshRenderEffect()
    }

    override fun setResolution(widthPx: Float, heightPx: Float) {
        updateResolution(widthPx, heightPx)
        refreshRenderEffect()
    }

    private fun updateResolution(widthPx: Float, heightPx: Float) {
        val w = if (widthPx > 0f) widthPx else 1f
        val h = if (heightPx > 0f) heightPx else 1f
        shader.setFloatUniform(RB_RESOLUTION_UNIFORM, w, h)
    }

    private fun refreshRenderEffect() {
        renderEffect = createRenderEffect()
    }

    private fun createRenderEffect(): RenderEffect =
        RenderEffect.createRuntimeShaderEffect(shader, RB_INPUT_UNIFORM)

    private fun applyDefaults() {
        for ((param, dv) in program.defaults) {
            when (param) {
                is FxParam.Float -> {
                    val name = program.layout.floatUniforms[param] ?: continue
                    val v = (dv as? DefaultValue.F)?.v ?: continue
                    shader.setFloatUniform(name, v)
                }
                is FxParam.Float2 -> {
                    val name = program.layout.float2Uniforms[param] ?: continue
                    val v = dv as? DefaultValue.F2 ?: continue
                    shader.setFloatUniform(name, v.x, v.y)
                }
                is FxParam.Float3 -> {
                    val name = program.layout.float3Uniforms[param] ?: continue
                    val v = dv as? DefaultValue.F3 ?: continue
                    shader.setFloatUniform(name, v.x, v.y, v.z)
                }
                is FxParam.Float4 -> {
                    val name = program.layout.float4Uniforms[param] ?: continue
                    val v = dv as? DefaultValue.F4 ?: continue
                    shader.setFloatUniform(name, v.x, v.y, v.z, v.w)
                }
            }
        }
    }
}
