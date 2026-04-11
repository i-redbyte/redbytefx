package ru.redbyte.redbytefx

internal object FxCompiler {

    fun compile(
        output: ColorExpr,
        defaults: Map<FxParam, DefaultValue>,
        functions: List<UserFunctionDefinition>
    ): FxProgram {
        val layout = UniformLayout(functions.mapTo(linkedSetOf()) { it.name })
        defaults.keys.forEach(layout::register)

        val mainCtx = EmitContext(layout)
        val outputExpr = emit(output, mainCtx)
        val functionSources = functions.map { compileFunction(it, layout) }

        val agsl = buildAglsl(
            layout = layout,
            functionSources = functionSources,
            localDeclarations = mainCtx.declarations(),
            outputExpr = outputExpr
        )

        return FxProgram(
            agsl = agsl,
            layout = layout,
            defaults = defaults
        )
    }

    private fun compileFunction(
        definition: UserFunctionDefinition,
        layout: UniformLayout
    ): String {
        validateFnBodyMatchesReturnType(
            functionName = definition.name,
            returnType = definition.returnType,
            body = definition.body
        )
        val ctx = EmitContext(layout)
        val bodyExpr = emitAny(definition.body, ctx)
        val params = definition.parameters.joinToString(", ") {
            "${it.type.agslType} ${it.name}"
        }

        return buildString {
            append(definition.returnType.agslType)
            append(' ')
            append(definition.name)
            append('(')
            append(params)
            append(") {\n")
            for (line in ctx.declarations()) {
                append(line).append('\n')
            }
            append("  return ").append(bodyExpr).append(";\n")
            append("}\n")
        }
    }

    private fun buildAglsl(
        layout: UniformLayout,
        functionSources: List<String>,
        localDeclarations: List<String>,
        outputExpr: String
    ): String {
        val sb = StringBuilder()

        sb.append("uniform shader ").append(RB_INPUT_UNIFORM).append(";\n")
        sb.append("uniform float2 ").append(RB_RESOLUTION_UNIFORM).append(";\n")

        for (name in layout.floatUniforms.values) {
            sb.append("uniform float ").append(name).append(";\n")
        }
        for (name in layout.float2Uniforms.values) {
            sb.append("uniform float2 ").append(name).append(";\n")
        }
        for (name in layout.float3Uniforms.values) {
            sb.append("uniform float3 ").append(name).append(";\n")
        }
        for (name in layout.float4Uniforms.values) {
            sb.append("uniform float4 ").append(name).append(";\n")
        }

        sb.append("\n")
        sb.append("float2 rb_maxCoord(float2 res) {\n")
        sb.append("  float2 e = float2(0.001);\n")
        sb.append("  return max(res - e, float2(0.0));\n")
        sb.append("}\n")
        sb.append("half4 rb_sample(float2 coord) {\n")
        sb.append("  float2 p = coord;\n")
        sb.append("  if (").append(RB_RESOLUTION_UNIFORM).append(".x > 2.0 && ")
            .append(RB_RESOLUTION_UNIFORM).append(".y > 2.0) {\n")
        sb.append("    p = clamp(p, float2(0.0), rb_maxCoord(").append(RB_RESOLUTION_UNIFORM).append("));\n")
        sb.append("  }\n")
        sb.append("  return ").append(RB_INPUT_UNIFORM).append(".eval(p);\n")
        sb.append("}\n")
        sb.append("\n")

        for (source in functionSources) {
            sb.append(source).append('\n')
        }

        sb.append("half4 main(float2 fragCoord) {\n")
        for (line in localDeclarations) {
            sb.append(line).append('\n')
        }
        sb.append("  return ").append(outputExpr).append(";\n")
        sb.append("}\n")

        return sb.toString()
    }
}

internal class EmitContext(
    private val layout: UniformLayout
) {
    private var boolLocalIndex = 0
    private var floatLocalIndex = 0
    private var float2LocalIndex = 0
    private var float3LocalIndex = 0
    private var float4LocalIndex = 0
    private var colorLocalIndex = 0

    private val identifierAllocator = IdentifierAllocator(layout.occupiedIdentifiers())
    private val statements = mutableListOf<String>()
    private val boolLocals = LinkedHashMap<LocalBoolExprImpl, String>()
    private val floatLocals = LinkedHashMap<LocalFloatExprImpl, String>()
    private val float2Locals = LinkedHashMap<LocalFloat2ExprImpl, String>()
    private val float3Locals = LinkedHashMap<LocalFloat3ExprImpl, String>()
    private val float4Locals = LinkedHashMap<LocalFloat4ExprImpl, String>()
    private val colorLocals = LinkedHashMap<LocalColorExprImpl, String>()

    fun uniformName(param: FxParam.Float): String = layout.uniformName(param)
    fun uniformName(param: FxParam.Float2): String = layout.uniformName(param)
    fun uniformName(param: FxParam.Float3): String = layout.uniformName(param)
    fun uniformName(param: FxParam.Float4): String = layout.uniformName(param)

    fun localName(expr: LocalBoolExprImpl): String =
        boolLocals.getOrPut(expr) {
            val name = nextLocalName(expr.suggestedName, "l_b${boolLocalIndex++}")
            val initializer = emit(expr.initializer, this)
            statements += "  bool $name = $initializer;"
            name
        }

    fun localName(expr: LocalFloatExprImpl): String =
        floatLocals.getOrPut(expr) {
            val name = nextLocalName(expr.suggestedName, "l_f${floatLocalIndex++}")
            val initializer = emit(expr.initializer, this)
            statements += "  float $name = $initializer;"
            name
        }

    fun localName(expr: LocalFloat2ExprImpl): String =
        float2Locals.getOrPut(expr) {
            val name = nextLocalName(expr.suggestedName, "l_v${float2LocalIndex++}")
            val initializer = emit(expr.initializer, this)
            statements += "  float2 $name = $initializer;"
            name
        }

    fun localName(expr: LocalFloat3ExprImpl): String =
        float3Locals.getOrPut(expr) {
            val name = nextLocalName(expr.suggestedName, "l_v3_${float3LocalIndex++}")
            val initializer = emit(expr.initializer, this)
            statements += "  float3 $name = $initializer;"
            name
        }

    fun localName(expr: LocalFloat4ExprImpl): String =
        float4Locals.getOrPut(expr) {
            val name = nextLocalName(expr.suggestedName, "l_v4_${float4LocalIndex++}")
            val initializer = emit(expr.initializer, this)
            statements += "  float4 $name = $initializer;"
            name
        }

    fun localName(expr: LocalColorExprImpl): String =
        colorLocals.getOrPut(expr) {
            val name = nextLocalName(expr.suggestedName, "l_c${colorLocalIndex++}")
            val initializer = emit(expr.initializer, this)
            statements += "  half4 $name = $initializer;"
            name
        }

    fun declarations(): List<String> = statements.toList()

    private fun nextLocalName(
        suggestedName: String?,
        fallback: String
    ): String {
        val base = suggestedName
            ?.takeIf { it.isNotBlank() }
            ?.let { sanitizeIdentifier(it, "l_") }
            ?: fallback

        return identifierAllocator.reserve(base)
    }
}
