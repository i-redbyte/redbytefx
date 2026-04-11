package ru.redbyte.redbytefx

/**
 * Negates a boolean expression.
 */
public operator fun BoolExpr.not(): BoolExpr =
    boolExpr { ctx -> "(!${emit(this@not, ctx)})" }

/**
 * Combines two boolean expressions with logical `and`.
 */
public infix fun BoolExpr.and(other: BoolExpr): BoolExpr =
    binaryBool(this, "&&", other)

/**
 * Combines two boolean expressions with logical `or`.
 */
public infix fun BoolExpr.or(other: BoolExpr): BoolExpr =
    binaryBool(this, "||", other)

/**
 * Compares whether this scalar expression is less than [other].
 */
public infix fun FloatExpr.lt(other: FloatExpr): BoolExpr =
    compareBool(this, "<", other)

/**
 * Compares whether this scalar expression is less than a scalar literal.
 */
public infix fun FloatExpr.lt(other: Float): BoolExpr =
    this lt floatLiteral(other)

/**
 * Compares whether this scalar expression is less than or equal to [other].
 */
public infix fun FloatExpr.lte(other: FloatExpr): BoolExpr =
    compareBool(this, "<=", other)

/**
 * Compares whether this scalar expression is less than or equal to a scalar literal.
 */
public infix fun FloatExpr.lte(other: Float): BoolExpr =
    this lte floatLiteral(other)

/**
 * Compares whether this scalar expression is greater than [other].
 */
public infix fun FloatExpr.gt(other: FloatExpr): BoolExpr =
    compareBool(this, ">", other)

/**
 * Compares whether this scalar expression is greater than a scalar literal.
 */
public infix fun FloatExpr.gt(other: Float): BoolExpr =
    this gt floatLiteral(other)

/**
 * Compares whether this scalar expression is greater than or equal to [other].
 */
public infix fun FloatExpr.gte(other: FloatExpr): BoolExpr =
    compareBool(this, ">=", other)

/**
 * Compares whether this scalar expression is greater than or equal to a scalar literal.
 */
public infix fun FloatExpr.gte(other: Float): BoolExpr =
    this gte floatLiteral(other)

/**
 * Compares whether this scalar expression is equal to [other].
 */
public infix fun FloatExpr.eq(other: FloatExpr): BoolExpr =
    compareBool(this, "==", other)

/**
 * Compares whether this scalar expression is equal to a scalar literal.
 */
public infix fun FloatExpr.eq(other: Float): BoolExpr =
    this eq floatLiteral(other)

/**
 * Compares whether this scalar expression is not equal to [other].
 */
public infix fun FloatExpr.neq(other: FloatExpr): BoolExpr =
    compareBool(this, "!=", other)

/**
 * Compares whether this scalar expression is not equal to a scalar literal.
 */
public infix fun FloatExpr.neq(other: Float): BoolExpr =
    this neq floatLiteral(other)

/**
 * Negates a scalar expression.
 */
public operator fun FloatExpr.unaryMinus(): FloatExpr =
    floatExpr { ctx -> "(-${emit(this@unaryMinus, ctx)})" }

/**
 * Adds two scalar expressions.
 */
public operator fun FloatExpr.plus(other: FloatExpr): FloatExpr =
    binaryFloat(this, "+", other)

/**
 * Adds a scalar literal to a scalar expression.
 */
public operator fun FloatExpr.plus(other: Float): FloatExpr =
    this + floatLiteral(other)

/**
 * Adds a scalar expression to a scalar literal.
 */
public operator fun Float.plus(other: FloatExpr): FloatExpr =
    floatLiteral(this) + other

/**
 * Adds a scalar expression to an integer literal.
 */
public operator fun Int.plus(other: FloatExpr): FloatExpr =
    floatLiteral(this.toFloat()) + other

/**
 * Subtracts one scalar expression from another.
 */
public operator fun FloatExpr.minus(other: FloatExpr): FloatExpr =
    binaryFloat(this, "-", other)

/**
 * Subtracts a scalar literal from a scalar expression.
 */
public operator fun FloatExpr.minus(other: Float): FloatExpr =
    this - floatLiteral(other)

/**
 * Subtracts a scalar expression from a scalar literal.
 */
public operator fun Float.minus(other: FloatExpr): FloatExpr =
    floatLiteral(this) - other

/**
 * Subtracts a scalar expression from an integer literal.
 */
public operator fun Int.minus(other: FloatExpr): FloatExpr =
    floatLiteral(this.toFloat()) - other

/**
 * Multiplies two scalar expressions.
 */
public operator fun FloatExpr.times(other: FloatExpr): FloatExpr =
    binaryFloat(this, "*", other)

/**
 * Multiplies a scalar expression by a scalar literal.
 */
public operator fun FloatExpr.times(other: Float): FloatExpr =
    this * floatLiteral(other)

/**
 * Multiplies a scalar literal by a scalar expression.
 */
public operator fun Float.times(other: FloatExpr): FloatExpr =
    floatLiteral(this) * other

/**
 * Multiplies an integer literal by a scalar expression.
 */
public operator fun Int.times(other: FloatExpr): FloatExpr =
    floatLiteral(this.toFloat()) * other

/**
 * Divides one scalar expression by another.
 */
public operator fun FloatExpr.div(other: FloatExpr): FloatExpr =
    binaryFloat(this, "/", other)

/**
 * Divides a scalar expression by a scalar literal.
 */
public operator fun FloatExpr.div(other: Float): FloatExpr =
    this / floatLiteral(other)

/**
 * Divides a scalar literal by a scalar expression.
 */
public operator fun Float.div(other: FloatExpr): FloatExpr =
    floatLiteral(this) / other

/**
 * Divides an integer literal by a scalar expression.
 */
public operator fun Int.div(other: FloatExpr): FloatExpr =
    floatLiteral(this.toFloat()) / other

/**
 * Negates a `float2` expression.
 */
public operator fun Float2Expr.unaryMinus(): Float2Expr =
    float2Expr { ctx -> "(-${emit(this@unaryMinus, ctx)})" }

/**
 * Adds two `float2` expressions.
 */
public operator fun Float2Expr.plus(other: Float2Expr): Float2Expr =
    binaryFloat2(this, "+", other)

/**
 * Subtracts one `float2` expression from another.
 */
public operator fun Float2Expr.minus(other: Float2Expr): Float2Expr =
    binaryFloat2(this, "-", other)

/**
 * Multiplies two `float2` expressions component-wise.
 */
public operator fun Float2Expr.times(other: Float2Expr): Float2Expr =
    binaryFloat2(this, "*", other)

/**
 * Divides two `float2` expressions component-wise.
 */
public operator fun Float2Expr.div(other: Float2Expr): Float2Expr =
    binaryFloat2(this, "/", other)

/**
 * Multiplies a `float2` expression by a scalar expression.
 */
public operator fun Float2Expr.times(other: FloatExpr): Float2Expr =
    vectorScalar(this, "*", other)

/**
 * Multiplies a `float2` expression by a scalar literal.
 */
public operator fun Float2Expr.times(other: Float): Float2Expr =
    this * floatLiteral(other)

/**
 * Multiplies a scalar expression by a `float2` expression.
 */
public operator fun FloatExpr.times(other: Float2Expr): Float2Expr =
    scalarVector(this, "*", other)

/**
 * Multiplies a scalar literal by a `float2` expression.
 */
public operator fun Float.times(other: Float2Expr): Float2Expr =
    floatLiteral(this) * other

/**
 * Multiplies an integer literal by a `float2` expression.
 */
public operator fun Int.times(other: Float2Expr): Float2Expr =
    floatLiteral(this.toFloat()) * other

/**
 * Divides a `float2` expression by a scalar expression.
 */
public operator fun Float2Expr.div(other: FloatExpr): Float2Expr =
    vectorScalar(this, "/", other)

/**
 * Divides a `float2` expression by a scalar literal.
 */
public operator fun Float2Expr.div(other: Float): Float2Expr =
    this / floatLiteral(other)

/**
 * Negates a `float3` expression.
 */
public operator fun Float3Expr.unaryMinus(): Float3Expr =
    float3Expr { ctx -> "(-${emit(this@unaryMinus, ctx)})" }

/**
 * Adds two `float3` expressions.
 */
public operator fun Float3Expr.plus(other: Float3Expr): Float3Expr =
    binaryFloat3(this, "+", other)

/**
 * Subtracts one `float3` expression from another.
 */
public operator fun Float3Expr.minus(other: Float3Expr): Float3Expr =
    binaryFloat3(this, "-", other)

/**
 * Multiplies two `float3` expressions component-wise.
 */
public operator fun Float3Expr.times(other: Float3Expr): Float3Expr =
    binaryFloat3(this, "*", other)

/**
 * Divides two `float3` expressions component-wise.
 */
public operator fun Float3Expr.div(other: Float3Expr): Float3Expr =
    binaryFloat3(this, "/", other)

/**
 * Multiplies a `float3` expression by a scalar expression.
 */
public operator fun Float3Expr.times(other: FloatExpr): Float3Expr =
    vector3Scalar(this, "*", other)

/**
 * Multiplies a `float3` expression by a scalar literal.
 */
public operator fun Float3Expr.times(other: Float): Float3Expr =
    this * floatLiteral(other)

/**
 * Multiplies a scalar expression by a `float3` expression.
 */
public operator fun FloatExpr.times(other: Float3Expr): Float3Expr =
    scalarVector3(this, "*", other)

/**
 * Multiplies a scalar literal by a `float3` expression.
 */
public operator fun Float.times(other: Float3Expr): Float3Expr =
    floatLiteral(this) * other

/**
 * Multiplies an integer literal by a `float3` expression.
 */
public operator fun Int.times(other: Float3Expr): Float3Expr =
    floatLiteral(this.toFloat()) * other

/**
 * Divides a `float3` expression by a scalar expression.
 */
public operator fun Float3Expr.div(other: FloatExpr): Float3Expr =
    vector3Scalar(this, "/", other)

/**
 * Divides a `float3` expression by a scalar literal.
 */
public operator fun Float3Expr.div(other: Float): Float3Expr =
    this / floatLiteral(other)

/**
 * Negates a `float4` expression.
 */
public operator fun Float4Expr.unaryMinus(): Float4Expr =
    float4Expr { ctx -> "(-${emit(this@unaryMinus, ctx)})" }

/**
 * Adds two `float4` expressions.
 */
public operator fun Float4Expr.plus(other: Float4Expr): Float4Expr =
    binaryFloat4(this, "+", other)

/**
 * Subtracts one `float4` expression from another.
 */
public operator fun Float4Expr.minus(other: Float4Expr): Float4Expr =
    binaryFloat4(this, "-", other)

/**
 * Multiplies two `float4` expressions component-wise.
 */
public operator fun Float4Expr.times(other: Float4Expr): Float4Expr =
    binaryFloat4(this, "*", other)

/**
 * Divides two `float4` expressions component-wise.
 */
public operator fun Float4Expr.div(other: Float4Expr): Float4Expr =
    binaryFloat4(this, "/", other)

/**
 * Multiplies a `float4` expression by a scalar expression.
 */
public operator fun Float4Expr.times(other: FloatExpr): Float4Expr =
    vector4Scalar(this, "*", other)

/**
 * Multiplies a `float4` expression by a scalar literal.
 */
public operator fun Float4Expr.times(other: Float): Float4Expr =
    this * floatLiteral(other)

/**
 * Multiplies a scalar expression by a `float4` expression.
 */
public operator fun FloatExpr.times(other: Float4Expr): Float4Expr =
    scalarVector4(this, "*", other)

/**
 * Multiplies a scalar literal by a `float4` expression.
 */
public operator fun Float.times(other: Float4Expr): Float4Expr =
    floatLiteral(this) * other

/**
 * Multiplies an integer literal by a `float4` expression.
 */
public operator fun Int.times(other: Float4Expr): Float4Expr =
    floatLiteral(this.toFloat()) * other

/**
 * Divides a `float4` expression by a scalar expression.
 */
public operator fun Float4Expr.div(other: FloatExpr): Float4Expr =
    vector4Scalar(this, "/", other)

/**
 * Divides a `float4` expression by a scalar literal.
 */
public operator fun Float4Expr.div(other: Float): Float4Expr =
    this / floatLiteral(other)

/**
 * Adds two color expressions.
 */
public operator fun ColorExpr.plus(other: ColorExpr): ColorExpr =
    binaryColor(this, "+", other)

/**
 * Subtracts one color expression from another.
 */
public operator fun ColorExpr.minus(other: ColorExpr): ColorExpr =
    binaryColor(this, "-", other)

/**
 * Multiplies two color expressions component-wise.
 */
public operator fun ColorExpr.times(other: ColorExpr): ColorExpr =
    binaryColor(this, "*", other)

/**
 * Multiplies a color expression by a scalar expression.
 */
public operator fun ColorExpr.times(other: FloatExpr): ColorExpr =
    colorScalar(this, "*", other)

/**
 * Multiplies a color expression by a scalar literal.
 */
public operator fun ColorExpr.times(other: Float): ColorExpr =
    this * floatLiteral(other)

/**
 * Multiplies a scalar expression by a color expression.
 */
public operator fun FloatExpr.times(other: ColorExpr): ColorExpr =
    scalarColor(this, "*", other)

/**
 * Multiplies a scalar literal by a color expression.
 */
public operator fun Float.times(other: ColorExpr): ColorExpr =
    floatLiteral(this) * other

/**
 * Multiplies an integer literal by a color expression.
 */
public operator fun Int.times(other: ColorExpr): ColorExpr =
    floatLiteral(this.toFloat()) * other

/**
 * Divides a color expression by a scalar expression.
 */
public operator fun ColorExpr.div(other: FloatExpr): ColorExpr =
    colorScalar(this, "/", other)

/**
 * Divides a color expression by a scalar literal.
 */
public operator fun ColorExpr.div(other: Float): ColorExpr =
    this / floatLiteral(other)
