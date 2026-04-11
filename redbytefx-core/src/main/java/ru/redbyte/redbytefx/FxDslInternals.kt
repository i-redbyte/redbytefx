package ru.redbyte.redbytefx

internal class FloatExprImpl(
    val emitter: (EmitContext) -> String
) : FloatExpr

internal class LocalFloatExprImpl(
    val suggestedName: String?,
    val initializer: FloatExpr
) : FloatExpr

internal class ParamFloatExprImpl(
    val name: String
) : FloatExpr

internal class BoolExprImpl(
    val emitter: (EmitContext) -> String
) : BoolExpr

internal class LocalBoolExprImpl(
    val suggestedName: String?,
    val initializer: BoolExpr
) : BoolExpr

internal class ParamBoolExprImpl(
    val name: String
) : BoolExpr

internal class Float2ExprImpl(
    val emitter: (EmitContext) -> String
) : Float2Expr

internal class LocalFloat2ExprImpl(
    val suggestedName: String?,
    val initializer: Float2Expr
) : Float2Expr

internal class ParamFloat2ExprImpl(
    val name: String
) : Float2Expr

internal class Float3ExprImpl(
    val emitter: (EmitContext) -> String
) : Float3Expr

internal class LocalFloat3ExprImpl(
    val suggestedName: String?,
    val initializer: Float3Expr
) : Float3Expr

internal class ParamFloat3ExprImpl(
    val name: String
) : Float3Expr

internal class Float4ExprImpl(
    val emitter: (EmitContext) -> String
) : Float4Expr

internal class LocalFloat4ExprImpl(
    val suggestedName: String?,
    val initializer: Float4Expr
) : Float4Expr

internal class ParamFloat4ExprImpl(
    val name: String
) : Float4Expr

internal class ColorExprImpl(
    val emitter: (EmitContext) -> String
) : ColorExpr

internal class LocalColorExprImpl(
    val suggestedName: String?,
    val initializer: ColorExpr
) : ColorExpr

internal class ParamColorExprImpl(
    val name: String
) : ColorExpr

internal val FRAG_COORD_EXPR: Float2Expr = Float2ExprImpl { "fragCoord" }
internal val RESOLUTION_EXPR: Float2Expr = Float2ExprImpl { RB_RESOLUTION_UNIFORM }

internal fun emit(expr: FloatExpr, ctx: EmitContext): String =
    when (expr) {
        is FloatExprImpl -> expr.emitter(ctx)
        is LocalFloatExprImpl -> ctx.localName(expr)
        is ParamFloatExprImpl -> expr.name
        is FxParam.Float -> ctx.uniformName(expr)
        else -> error(unsupportedDslImplementationMessage("FloatExpr", expr))
    }

internal fun emit(expr: BoolExpr, ctx: EmitContext): String =
    when (expr) {
        is BoolExprImpl -> expr.emitter(ctx)
        is LocalBoolExprImpl -> ctx.localName(expr)
        is ParamBoolExprImpl -> expr.name
        else -> error(unsupportedDslImplementationMessage("BoolExpr", expr))
    }

internal fun emit(expr: Float2Expr, ctx: EmitContext): String =
    when (expr) {
        is Float2ExprImpl -> expr.emitter(ctx)
        is LocalFloat2ExprImpl -> ctx.localName(expr)
        is ParamFloat2ExprImpl -> expr.name
        is FxParam.Float2 -> ctx.uniformName(expr)
        else -> error(unsupportedDslImplementationMessage("Float2Expr", expr))
    }

internal fun emit(expr: Float3Expr, ctx: EmitContext): String =
    when (expr) {
        is Float3ExprImpl -> expr.emitter(ctx)
        is LocalFloat3ExprImpl -> ctx.localName(expr)
        is ParamFloat3ExprImpl -> expr.name
        is FxParam.Float3 -> ctx.uniformName(expr)
        else -> error(unsupportedDslImplementationMessage("Float3Expr", expr))
    }

internal fun emit(expr: Float4Expr, ctx: EmitContext): String =
    when (expr) {
        is Float4ExprImpl -> expr.emitter(ctx)
        is LocalFloat4ExprImpl -> ctx.localName(expr)
        is ParamFloat4ExprImpl -> expr.name
        is FxParam.Float4 -> ctx.uniformName(expr)
        else -> error(unsupportedDslImplementationMessage("Float4Expr", expr))
    }

internal fun emit(expr: ColorExpr, ctx: EmitContext): String =
    when (expr) {
        is ColorExprImpl -> expr.emitter(ctx)
        is LocalColorExprImpl -> ctx.localName(expr)
        is ParamColorExprImpl -> expr.name
        else -> error(unsupportedDslImplementationMessage("ColorExpr", expr))
    }

internal fun floatExpr(
    emitter: (EmitContext) -> String
): FloatExpr = FloatExprImpl(emitter)

internal fun boolExpr(
    emitter: (EmitContext) -> String
): BoolExpr = BoolExprImpl(emitter)

internal fun float2Expr(
    emitter: (EmitContext) -> String
): Float2Expr = Float2ExprImpl(emitter)

internal fun float3Expr(
    emitter: (EmitContext) -> String
): Float3Expr = Float3ExprImpl(emitter)

internal fun float4Expr(
    emitter: (EmitContext) -> String
): Float4Expr = Float4ExprImpl(emitter)

internal fun colorExpr(
    emitter: (EmitContext) -> String
): ColorExpr = ColorExprImpl(emitter)

internal fun localFloatExpr(
    suggestedName: String?,
    initializer: FloatExpr
): FloatExpr = LocalFloatExprImpl(suggestedName, initializer)

internal fun localBoolExpr(
    suggestedName: String?,
    initializer: BoolExpr
): BoolExpr = LocalBoolExprImpl(suggestedName, initializer)

internal fun localFloat2Expr(
    suggestedName: String?,
    initializer: Float2Expr
): Float2Expr = LocalFloat2ExprImpl(suggestedName, initializer)

internal fun localFloat3Expr(
    suggestedName: String?,
    initializer: Float3Expr
): Float3Expr = LocalFloat3ExprImpl(suggestedName, initializer)

internal fun localFloat4Expr(
    suggestedName: String?,
    initializer: Float4Expr
): Float4Expr = LocalFloat4ExprImpl(suggestedName, initializer)

internal fun localColorExpr(
    suggestedName: String?,
    initializer: ColorExpr
): ColorExpr = LocalColorExprImpl(suggestedName, initializer)

internal fun floatLiteral(value: Float): FloatExpr =
    FloatExprImpl { formatFloat(value) }

internal fun binaryFloat(
    left: FloatExpr,
    operator: String,
    right: FloatExpr
): FloatExpr = floatExpr { ctx ->
    "(${emit(left, ctx)} $operator ${emit(right, ctx)})"
}

internal fun binaryBool(
    left: BoolExpr,
    operator: String,
    right: BoolExpr
): BoolExpr = boolExpr { ctx ->
    "(${emit(left, ctx)} $operator ${emit(right, ctx)})"
}

internal fun compareBool(
    left: FloatExpr,
    operator: String,
    right: FloatExpr
): BoolExpr = boolExpr { ctx ->
    "(${emit(left, ctx)} $operator ${emit(right, ctx)})"
}

internal fun binaryFloat2(
    left: Float2Expr,
    operator: String,
    right: Float2Expr
): Float2Expr = float2Expr { ctx ->
    "(${emit(left, ctx)} $operator ${emit(right, ctx)})"
}

internal fun binaryFloat3(
    left: Float3Expr,
    operator: String,
    right: Float3Expr
): Float3Expr = float3Expr { ctx ->
    "(${emit(left, ctx)} $operator ${emit(right, ctx)})"
}

internal fun binaryFloat4(
    left: Float4Expr,
    operator: String,
    right: Float4Expr
): Float4Expr = float4Expr { ctx ->
    "(${emit(left, ctx)} $operator ${emit(right, ctx)})"
}

internal fun vectorScalar(
    left: Float2Expr,
    operator: String,
    right: FloatExpr
): Float2Expr = float2Expr { ctx ->
    "(${emit(left, ctx)} $operator ${emit(right, ctx)})"
}

internal fun vector3Scalar(
    left: Float3Expr,
    operator: String,
    right: FloatExpr
): Float3Expr = float3Expr { ctx ->
    "(${emit(left, ctx)} $operator ${emit(right, ctx)})"
}

internal fun vector4Scalar(
    left: Float4Expr,
    operator: String,
    right: FloatExpr
): Float4Expr = float4Expr { ctx ->
    "(${emit(left, ctx)} $operator ${emit(right, ctx)})"
}

internal fun scalarVector(
    left: FloatExpr,
    operator: String,
    right: Float2Expr
): Float2Expr = float2Expr { ctx ->
    "(${emit(left, ctx)} $operator ${emit(right, ctx)})"
}

internal fun scalarVector3(
    left: FloatExpr,
    operator: String,
    right: Float3Expr
): Float3Expr = float3Expr { ctx ->
    "(${emit(left, ctx)} $operator ${emit(right, ctx)})"
}

internal fun scalarVector4(
    left: FloatExpr,
    operator: String,
    right: Float4Expr
): Float4Expr = float4Expr { ctx ->
    "(${emit(left, ctx)} $operator ${emit(right, ctx)})"
}

internal fun binaryColor(
    left: ColorExpr,
    operator: String,
    right: ColorExpr
): ColorExpr = colorExpr { ctx ->
    "(${emit(left, ctx)} $operator ${emit(right, ctx)})"
}

internal fun colorScalar(
    left: ColorExpr,
    operator: String,
    right: FloatExpr
): ColorExpr = colorExpr { ctx ->
    "(${emit(left, ctx)} $operator ${emit(right, ctx)})"
}

internal fun scalarColor(
    left: FloatExpr,
    operator: String,
    right: ColorExpr
): ColorExpr = colorExpr { ctx ->
    "(${emit(left, ctx)} $operator ${emit(right, ctx)})"
}

internal fun callFloat(
    function: String,
    vararg args: Any
): FloatExpr = floatExpr(functionCallEmitter(function, args.asList()))

internal fun callFloat2(
    function: String,
    vararg args: Any
): Float2Expr = float2Expr(functionCallEmitter(function, args.asList()))

internal fun callFloat3(
    function: String,
    vararg args: Any
): Float3Expr = float3Expr(functionCallEmitter(function, args.asList()))

internal fun callFloat4(
    function: String,
    vararg args: Any
): Float4Expr = float4Expr(functionCallEmitter(function, args.asList()))

internal fun callColor(
    function: String,
    vararg args: Any
): ColorExpr = colorExpr(functionCallEmitter(function, args.asList()))

private fun functionCallEmitter(
    functionName: String,
    args: List<Any>
): (EmitContext) -> String = { ctx ->
    formatFunctionCall(functionName, args, ctx)
}

@Suppress("UNCHECKED_CAST")
internal fun <T> emitterExprForType(
    type: FxValueType<T>,
    emitter: (EmitContext) -> String
): T = when (type) {
    BoolType -> boolExpr(emitter)
    FloatType -> floatExpr(emitter)
    Float2Type -> float2Expr(emitter)
    Float3Type -> float3Expr(emitter)
    Float4Type -> float4Expr(emitter)
    ColorType -> colorExpr(emitter)
} as T

internal fun <T> constructorExpr(
    type: FxValueType<T>,
    agslType: String,
    vararg args: Any
): T = emitterExprForType(type) { ctx ->
    formatFunctionCall(agslType, args.asList(), ctx)
}

internal fun <T> conditionalExpr(
    type: FxValueType<T>,
    condition: BoolExpr,
    ifTrue: Any,
    ifFalse: Any
): T = emitterExprForType(type) { ctx ->
    "(${emit(condition, ctx)} ? ${emitAny(ifTrue, ctx)} : ${emitAny(ifFalse, ctx)})"
}

@Suppress("UNCHECKED_CAST")
private fun <T> parameterExprForType(
    type: FxValueType<T>,
    name: String
): T = when (type) {
    BoolType -> ParamBoolExprImpl(name)
    FloatType -> ParamFloatExprImpl(name)
    Float2Type -> ParamFloat2ExprImpl(name)
    Float3Type -> ParamFloat3ExprImpl(name)
    Float4Type -> ParamFloat4ExprImpl(name)
    ColorType -> ParamColorExprImpl(name)
} as T

internal fun <T> parameterExpr(
    type: FxValueType<T>,
    name: String
): T = parameterExprForType(type, name)

internal fun <T> callFunction(
    returnType: FxValueType<T>,
    functionName: String,
    args: List<Any>
): T = emitterExprForType(returnType, functionCallEmitter(functionName, args))

internal fun emitAny(expr: Any, ctx: EmitContext): String =
    when (expr) {
        is BoolExpr -> emit(expr, ctx)
        is FloatExpr -> emit(expr, ctx)
        is Float2Expr -> emit(expr, ctx)
        is Float3Expr -> emit(expr, ctx)
        is Float4Expr -> emit(expr, ctx)
        is ColorExpr -> emit(expr, ctx)
        else -> error(unsupportedExpressionArgumentMessage(expr))
    }

internal fun formatFunctionCall(
    functionName: String,
    args: List<Any>,
    ctx: EmitContext
): String = args.joinToString(
    prefix = "$functionName(",
    postfix = ")"
) { emitAny(it, ctx) }

internal fun formatFloat(value: Float): String {
    require(value.isFinite()) { nonFiniteFloatLiteralMessage(value) }
    val text = value.toString()
    return if ('.' in text || 'e' in text || 'E' in text) text else "$text.0"
}
