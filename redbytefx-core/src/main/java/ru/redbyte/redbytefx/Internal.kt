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

internal fun fnBodyReturnTypeMismatchMessage(
    functionName: String,
    expected: FxValueType<*>,
    body: Any
): String {
    val expectedLabel = expected.agslType
    val actual = describeShaderExprKind(body)
    return "fn `$functionName` declares return type `$expectedLabel`, but the body is $actual. " +
        "Return an expression that matches the declared type (for example " +
        "`float(...)` / `float2(...)` / `color(...)`, stdlib helpers, or composed DSL math)."
}

private fun describeShaderExprKind(body: Any): String =
    when (body) {
        is BoolExpr -> "a bool expression"
        is ColorExpr -> "a color expression (half4)"
        is Float2Expr -> "a float2 expression"
        is Float3Expr -> "a float3 expression"
        is Float4Expr -> "a float4 expression"
        is FloatExpr -> "a float expression"
        else -> {
            val name = body::class.qualifiedName ?: body::class.simpleName ?: "unknown"
            "not a DSL expression type ($name)"
        }
    }

internal fun validateFnBodyMatchesReturnType(
    functionName: String,
    returnType: FxValueType<*>,
    body: Any
) {
    val ok = when (returnType) {
        BoolType -> body is BoolExpr
        FloatType -> body is FloatExpr
        Float2Type -> body is Float2Expr
        Float3Type -> body is Float3Expr
        Float4Type -> body is Float4Expr
        ColorType -> body is ColorExpr
    }
    if (!ok) {
        error(fnBodyReturnTypeMismatchMessage(functionName, returnType, body))
    }
}

internal fun unsupportedDslImplementationMessage(
    typeLabel: String,
    expr: Any
): String {
    val typeName = expr::class.qualifiedName ?: expr::class.simpleName ?: "unknown"
    return "Unsupported $typeLabel implementation: $typeName. " +
        "Do not implement $typeLabel directly in author code. Build shader values by composing " +
        "existing DSL expressions, constructors such as float(...), float2(...), float3(...), " +
        "float4(...), color(...), uniforms, and stdlib helpers instead. If this value came from " +
        "let(...) or was returned from fn(...), return a composed DSL expression rather than a " +
        "custom marker-interface implementation."
}

internal class UniformLayout(
    additionalOccupied: Set<String> = emptySet()
) {
    private var floatIndex = 0
    private var float2Index = 0
    private var float3Index = 0
    private var float4Index = 0
    private val identifierAllocator = IdentifierAllocator(
        linkedSetOf(
            RB_INPUT_UNIFORM,
            RB_RESOLUTION_UNIFORM,
            "fragCoord",
            "main",
            "rb_maxCoord",
            "rb_sample"
        ).apply { addAll(additionalOccupied) }
    )

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

        return identifierAllocator.reserve(base)
    }

    fun occupiedIdentifiers(): Set<String> = identifierAllocator.snapshot()
}

internal object FxBuilder {
    fun build(block: FxDsl.() -> ColorExpr): FxEffect {
        val defaults = LinkedHashMap<FxParam, DefaultValue>()
        val functions = mutableListOf<UserFunctionDefinition>()
        val functionNameAllocator = IdentifierAllocator(
            mutableSetOf<String>().apply {
            addAll(RESERVED_USER_FUNCTION_NAMES)
            add(sanitizeSuggestedIdentifier(RB_INPUT_UNIFORM, "fn_"))
            add(sanitizeSuggestedIdentifier(RB_RESOLUTION_UNIFORM, "fn_"))
            add(sanitizeSuggestedIdentifier("fragCoord", "fn_"))
            add(sanitizeSuggestedIdentifier("rb_maxCoord", "fn_"))
            add(sanitizeSuggestedIdentifier("rb_sample", "fn_"))
        }
        )
        val dsl = FxDsl(defaults, functions, functionNameAllocator)
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
    private val runtimeState = FxRuntimeState(
        program = program,
        writer = object : FxRuntimeUniformWriter {
            override fun setFloat(name: String, value: Float) {
                shader.setFloatUniform(name, value)
            }

            override fun setFloat2(name: String, x: Float, y: Float) {
                shader.setFloatUniform(name, x, y)
            }

            override fun setFloat3(name: String, x: Float, y: Float, z: Float) {
                shader.setFloatUniform(name, x, y, z)
            }

            override fun setFloat4(name: String, x: Float, y: Float, z: Float, w: Float) {
                shader.setFloatUniform(name, x, y, z, w)
            }
        },
        onRuntimeChanged = ::refreshRenderEffect
    )

    init {
        runtimeState.applyDefaults()
        runtimeState.setResolution(1f, 1f)
    }

    override fun renderEffect(): RenderEffect = renderEffect

    override fun setFloat(param: FxParam.Float, value: Float): Boolean =
        runtimeState.setFloat(param, value)

    override fun setFloat2(param: FxParam.Float2, x: Float, y: Float): Boolean =
        runtimeState.setFloat2(param, x, y)

    override fun setFloat3(param: FxParam.Float3, x: Float, y: Float, z: Float): Boolean =
        runtimeState.setFloat3(param, x, y, z)

    override fun setFloat4(param: FxParam.Float4, x: Float, y: Float, z: Float, w: Float): Boolean =
        runtimeState.setFloat4(param, x, y, z, w)

    override fun setResolution(widthPx: Float, heightPx: Float): Boolean =
        runtimeState.setResolution(widthPx, heightPx)

    private fun refreshRenderEffect() {
        renderEffect = createRenderEffect()
    }

    private fun createRenderEffect(): RenderEffect =
        RenderEffect.createRuntimeShaderEffect(shader, RB_INPUT_UNIFORM)
}
