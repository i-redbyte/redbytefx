package ru.redbyte.redbytefx

/**
 * Returns the sine of a scalar expression.
 */
public fun sin(value: FloatExpr): FloatExpr =
    callFloat("sin", value)

/**
 * Returns the cosine of a scalar expression.
 */
public fun cos(value: FloatExpr): FloatExpr =
    callFloat("cos", value)

/**
 * Returns the arc tangent of a scalar expression.
 */
public fun atan(value: FloatExpr): FloatExpr =
    callFloat("atan", value)

/**
 * Returns the arc tangent of the vector formed by [y] and [x].
 *
 * This maps to AGSL's two-argument `atan(y, x)` form and is useful for polar coordinate helpers.
 */
public fun atan(y: FloatExpr, x: FloatExpr): FloatExpr =
    callFloat("atan", y, x)

/**
 * Returns the arc tangent of the vector formed by [y] and a literal [x].
 */
public fun atan(y: FloatExpr, x: Float): FloatExpr =
    atan(y, floatLiteral(x))

/**
 * Returns the arc tangent of the vector formed by a literal [y] and [x].
 */
public fun atan(y: Float, x: FloatExpr): FloatExpr =
    atan(floatLiteral(y), x)

/**
 * Returns the absolute value of a scalar expression.
 */
public fun abs(value: FloatExpr): FloatExpr =
    callFloat("abs", value)

/**
 * Floors a scalar expression.
 */
public fun floor(value: FloatExpr): FloatExpr =
    callFloat("floor", value)

/**
 * Floors a `float2` expression component-wise.
 */
public fun floor(value: Float2Expr): Float2Expr =
    callFloat2("floor", value)

/**
 * Floors a `float3` expression component-wise.
 */
public fun floor(value: Float3Expr): Float3Expr =
    callFloat3("floor", value)

/**
 * Floors a `float4` expression component-wise.
 */
public fun floor(value: Float4Expr): Float4Expr =
    callFloat4("floor", value)

/**
 * Ceils a scalar expression.
 */
public fun ceil(value: FloatExpr): FloatExpr =
    callFloat("ceil", value)

/**
 * Ceils a `float2` expression component-wise.
 */
public fun ceil(value: Float2Expr): Float2Expr =
    callFloat2("ceil", value)

/**
 * Ceils a `float3` expression component-wise.
 */
public fun ceil(value: Float3Expr): Float3Expr =
    callFloat3("ceil", value)

/**
 * Ceils a `float4` expression component-wise.
 */
public fun ceil(value: Float4Expr): Float4Expr =
    callFloat4("ceil", value)

/**
 * Returns the fractional part of a scalar expression.
 */
public fun fract(value: FloatExpr): FloatExpr =
    callFloat("fract", value)

/**
 * Returns the fractional part of a `float2` expression component-wise.
 */
public fun fract(value: Float2Expr): Float2Expr =
    callFloat2("fract", value)

/**
 * Returns the fractional part of a `float3` expression component-wise.
 */
public fun fract(value: Float3Expr): Float3Expr =
    callFloat3("fract", value)

/**
 * Returns the fractional part of a `float4` expression component-wise.
 */
public fun fract(value: Float4Expr): Float4Expr =
    callFloat4("fract", value)

/**
 * Returns the smaller of two scalar expressions.
 */
public fun min(left: FloatExpr, right: FloatExpr): FloatExpr =
    callFloat("min", left, right)

/**
 * Returns the smaller of a scalar expression and a scalar literal.
 */
public fun min(left: FloatExpr, right: Float): FloatExpr =
    min(left, floatLiteral(right))

/**
 * Returns the larger of two scalar expressions.
 */
public fun max(left: FloatExpr, right: FloatExpr): FloatExpr =
    callFloat("max", left, right)

/**
 * Returns the larger of a scalar expression and a scalar literal.
 */
public fun max(left: FloatExpr, right: Float): FloatExpr =
    max(left, floatLiteral(right))

/**
 * Clamps a scalar expression between [minValue] and [maxValue].
 */
public fun clamp(
    value: FloatExpr,
    minValue: FloatExpr,
    maxValue: FloatExpr
): FloatExpr = callFloat("clamp", value, minValue, maxValue)

/**
 * Clamps a scalar expression between scalar literals.
 */
public fun clamp(
    value: FloatExpr,
    minValue: Float,
    maxValue: Float
): FloatExpr = clamp(value, floatLiteral(minValue), floatLiteral(maxValue))

/**
 * Clamps a scalar expression to the `[0, 1]` range.
 */
public fun saturate(value: FloatExpr): FloatExpr =
    clamp(value, 0f, 1f)

/**
 * Computes modulo for two scalar expressions.
 */
public fun mod(left: FloatExpr, right: FloatExpr): FloatExpr =
    callFloat("mod", left, right)

/**
 * Computes modulo for a scalar expression and a scalar literal.
 */
public fun mod(left: FloatExpr, right: Float): FloatExpr =
    mod(left, floatLiteral(right))

/**
 * Computes modulo for a `float2` expression with a scalar expression divisor.
 */
public fun mod(left: Float2Expr, right: FloatExpr): Float2Expr =
    callFloat2("mod", left, right)

/**
 * Computes modulo for a `float2` expression with a scalar literal divisor.
 */
public fun mod(left: Float2Expr, right: Float): Float2Expr =
    mod(left, floatLiteral(right))

/**
 * Computes modulo for a `float3` expression with a scalar expression divisor.
 */
public fun mod(left: Float3Expr, right: FloatExpr): Float3Expr =
    callFloat3("mod", left, right)

/**
 * Computes modulo for a `float3` expression with a scalar literal divisor.
 */
public fun mod(left: Float3Expr, right: Float): Float3Expr =
    mod(left, floatLiteral(right))

/**
 * Computes modulo for a `float4` expression with a scalar expression divisor.
 */
public fun mod(left: Float4Expr, right: FloatExpr): Float4Expr =
    callFloat4("mod", left, right)

/**
 * Computes modulo for a `float4` expression with a scalar literal divisor.
 */
public fun mod(left: Float4Expr, right: Float): Float4Expr =
    mod(left, floatLiteral(right))

/**
 * Raises a scalar expression to a scalar expression exponent.
 */
public fun pow(base: FloatExpr, exponent: FloatExpr): FloatExpr =
    callFloat("pow", base, exponent)

/**
 * Raises a scalar expression to a scalar literal exponent.
 */
public fun pow(base: FloatExpr, exponent: Float): FloatExpr =
    pow(base, floatLiteral(exponent))

/**
 * Raises a scalar literal to a scalar expression exponent.
 */
public fun pow(base: Float, exponent: FloatExpr): FloatExpr =
    pow(floatLiteral(base), exponent)

/**
 * Raises a `float2` expression to a scalar expression exponent component-wise.
 */
public fun pow(base: Float2Expr, exponent: FloatExpr): Float2Expr =
    callFloat2("pow", base, exponent)

/**
 * Raises a `float2` expression to a scalar literal exponent component-wise.
 */
public fun pow(base: Float2Expr, exponent: Float): Float2Expr =
    pow(base, floatLiteral(exponent))

/**
 * Raises a `float3` expression to a scalar expression exponent component-wise.
 */
public fun pow(base: Float3Expr, exponent: FloatExpr): Float3Expr =
    callFloat3("pow", base, exponent)

/**
 * Raises a `float3` expression to a scalar literal exponent component-wise.
 */
public fun pow(base: Float3Expr, exponent: Float): Float3Expr =
    pow(base, floatLiteral(exponent))

/**
 * Raises a `float4` expression to a scalar expression exponent component-wise.
 */
public fun pow(base: Float4Expr, exponent: FloatExpr): Float4Expr =
    callFloat4("pow", base, exponent)

/**
 * Raises a `float4` expression to a scalar literal exponent component-wise.
 */
public fun pow(base: Float4Expr, exponent: Float): Float4Expr =
    pow(base, floatLiteral(exponent))

/**
 * Applies `step(edge, value)` to scalar expressions.
 */
public fun step(edge: FloatExpr, value: FloatExpr): FloatExpr =
    callFloat("step", edge, value)

/**
 * Applies `step(edge, value)` using a scalar literal edge.
 */
public fun step(edge: Float, value: FloatExpr): FloatExpr =
    step(floatLiteral(edge), value)

/**
 * Applies `step(edge, value)` to a `float2` expression.
 */
public fun step(edge: FloatExpr, value: Float2Expr): Float2Expr =
    callFloat2("step", edge, value)

/**
 * Applies `step(edge, value)` to a `float2` expression using a scalar literal edge.
 */
public fun step(edge: Float, value: Float2Expr): Float2Expr =
    step(floatLiteral(edge), value)

/**
 * Applies `step(edge, value)` to a `float3` expression.
 */
public fun step(edge: FloatExpr, value: Float3Expr): Float3Expr =
    callFloat3("step", edge, value)

/**
 * Applies `step(edge, value)` to a `float3` expression using a scalar literal edge.
 */
public fun step(edge: Float, value: Float3Expr): Float3Expr =
    step(floatLiteral(edge), value)

/**
 * Applies `step(edge, value)` to a `float4` expression.
 */
public fun step(edge: FloatExpr, value: Float4Expr): Float4Expr =
    callFloat4("step", edge, value)

/**
 * Applies `step(edge, value)` to a `float4` expression using a scalar literal edge.
 */
public fun step(edge: Float, value: Float4Expr): Float4Expr =
    step(floatLiteral(edge), value)

/**
 * Applies `smoothstep(edge0, edge1, value)` to scalar expressions.
 */
public fun smoothstep(
    edge0: FloatExpr,
    edge1: FloatExpr,
    value: FloatExpr
): FloatExpr = callFloat("smoothstep", edge0, edge1, value)

/**
 * Applies `smoothstep(edge0, edge1, value)` using scalar literal edges.
 */
public fun smoothstep(
    edge0: Float,
    edge1: Float,
    value: FloatExpr
): FloatExpr = smoothstep(floatLiteral(edge0), floatLiteral(edge1), value)

/**
 * Applies `smoothstep(edge0, edge1, value)` using a literal lower edge.
 */
public fun smoothstep(
    edge0: Float,
    edge1: FloatExpr,
    value: FloatExpr
): FloatExpr = smoothstep(floatLiteral(edge0), edge1, value)

/**
 * Applies `smoothstep(edge0, edge1, value)` using a literal upper edge.
 */
public fun smoothstep(
    edge0: FloatExpr,
    edge1: Float,
    value: FloatExpr
): FloatExpr = smoothstep(edge0, floatLiteral(edge1), value)

/**
 * Applies `smoothstep(edge0, edge1, value)` to a `float2` expression.
 */
public fun smoothstep(
    edge0: FloatExpr,
    edge1: FloatExpr,
    value: Float2Expr
): Float2Expr = callFloat2("smoothstep", edge0, edge1, value)

/**
 * Applies `smoothstep(edge0, edge1, value)` to a `float2` expression using literal edges.
 */
public fun smoothstep(
    edge0: Float,
    edge1: Float,
    value: Float2Expr
): Float2Expr = smoothstep(floatLiteral(edge0), floatLiteral(edge1), value)

/**
 * Applies `smoothstep(edge0, edge1, value)` to a `float2` expression using a literal lower edge.
 */
public fun smoothstep(
    edge0: Float,
    edge1: FloatExpr,
    value: Float2Expr
): Float2Expr = smoothstep(floatLiteral(edge0), edge1, value)

/**
 * Applies `smoothstep(edge0, edge1, value)` to a `float2` expression using a literal upper edge.
 */
public fun smoothstep(
    edge0: FloatExpr,
    edge1: Float,
    value: Float2Expr
): Float2Expr = smoothstep(edge0, floatLiteral(edge1), value)

/**
 * Applies `smoothstep(edge0, edge1, value)` to a `float3` expression.
 */
public fun smoothstep(
    edge0: FloatExpr,
    edge1: FloatExpr,
    value: Float3Expr
): Float3Expr = callFloat3("smoothstep", edge0, edge1, value)

/**
 * Applies `smoothstep(edge0, edge1, value)` to a `float3` expression using literal edges.
 */
public fun smoothstep(
    edge0: Float,
    edge1: Float,
    value: Float3Expr
): Float3Expr = smoothstep(floatLiteral(edge0), floatLiteral(edge1), value)

/**
 * Applies `smoothstep(edge0, edge1, value)` to a `float3` expression using a literal lower edge.
 */
public fun smoothstep(
    edge0: Float,
    edge1: FloatExpr,
    value: Float3Expr
): Float3Expr = smoothstep(floatLiteral(edge0), edge1, value)

/**
 * Applies `smoothstep(edge0, edge1, value)` to a `float3` expression using a literal upper edge.
 */
public fun smoothstep(
    edge0: FloatExpr,
    edge1: Float,
    value: Float3Expr
): Float3Expr = smoothstep(edge0, floatLiteral(edge1), value)

/**
 * Applies `smoothstep(edge0, edge1, value)` to a `float4` expression.
 */
public fun smoothstep(
    edge0: FloatExpr,
    edge1: FloatExpr,
    value: Float4Expr
): Float4Expr = callFloat4("smoothstep", edge0, edge1, value)

/**
 * Applies `smoothstep(edge0, edge1, value)` to a `float4` expression using literal edges.
 */
public fun smoothstep(
    edge0: Float,
    edge1: Float,
    value: Float4Expr
): Float4Expr = smoothstep(floatLiteral(edge0), floatLiteral(edge1), value)

/**
 * Applies `smoothstep(edge0, edge1, value)` to a `float4` expression using a literal lower edge.
 */
public fun smoothstep(
    edge0: Float,
    edge1: FloatExpr,
    value: Float4Expr
): Float4Expr = smoothstep(floatLiteral(edge0), edge1, value)

/**
 * Applies `smoothstep(edge0, edge1, value)` to a `float4` expression using a literal upper edge.
 */
public fun smoothstep(
    edge0: FloatExpr,
    edge1: Float,
    value: Float4Expr
): Float4Expr = smoothstep(edge0, floatLiteral(edge1), value)

/**
 * Linearly interpolates between two scalar expressions.
 */
public fun mix(
    left: FloatExpr,
    right: FloatExpr,
    amount: FloatExpr
): FloatExpr = callFloat("mix", left, right, amount)

/**
 * Linearly interpolates between a scalar literal and a scalar expression.
 */
public fun mix(
    left: Float,
    right: FloatExpr,
    amount: FloatExpr
): FloatExpr = mix(floatLiteral(left), right, amount)

/**
 * Linearly interpolates between a scalar expression and a scalar literal.
 */
public fun mix(
    left: FloatExpr,
    right: Float,
    amount: FloatExpr
): FloatExpr = mix(left, floatLiteral(right), amount)

/**
 * Linearly interpolates between two scalar literals.
 */
public fun mix(
    left: Float,
    right: Float,
    amount: FloatExpr
): FloatExpr = mix(floatLiteral(left), floatLiteral(right), amount)

/**
 * Linearly interpolates between two scalar expressions using a scalar literal amount.
 */
public fun mix(
    left: FloatExpr,
    right: FloatExpr,
    amount: Float
): FloatExpr = mix(left, right, floatLiteral(amount))

/**
 * Linearly interpolates between two `float2` expressions.
 */
public fun mix(
    left: Float2Expr,
    right: Float2Expr,
    amount: FloatExpr
): Float2Expr = callFloat2("mix", left, right, amount)

/**
 * Linearly interpolates between two `float2` expressions using a scalar literal amount.
 */
public fun mix(
    left: Float2Expr,
    right: Float2Expr,
    amount: Float
): Float2Expr = mix(left, right, floatLiteral(amount))

/**
 * Linearly interpolates between two `float3` expressions.
 */
public fun mix(
    left: Float3Expr,
    right: Float3Expr,
    amount: FloatExpr
): Float3Expr = callFloat3("mix", left, right, amount)

/**
 * Linearly interpolates between two `float3` expressions using a scalar literal amount.
 */
public fun mix(
    left: Float3Expr,
    right: Float3Expr,
    amount: Float
): Float3Expr = mix(left, right, floatLiteral(amount))

/**
 * Linearly interpolates between two `float4` expressions.
 */
public fun mix(
    left: Float4Expr,
    right: Float4Expr,
    amount: FloatExpr
): Float4Expr = callFloat4("mix", left, right, amount)

/**
 * Linearly interpolates between two `float4` expressions using a scalar literal amount.
 */
public fun mix(
    left: Float4Expr,
    right: Float4Expr,
    amount: Float
): Float4Expr = mix(left, right, floatLiteral(amount))

/**
 * Linearly interpolates between two color expressions.
 */
public fun mix(
    left: ColorExpr,
    right: ColorExpr,
    amount: FloatExpr
): ColorExpr = callColor("mix", left, right, amount)

/**
 * Linearly interpolates between two color expressions using a scalar literal amount.
 */
public fun mix(
    left: ColorExpr,
    right: ColorExpr,
    amount: Float
): ColorExpr = mix(left, right, floatLiteral(amount))

/**
 * Returns the Euclidean length of a `float2` expression.
 */
public fun length(value: Float2Expr): FloatExpr =
    callFloat("length", value)

/**
 * Returns the Euclidean length of a `float3` expression.
 */
public fun length(value: Float3Expr): FloatExpr =
    callFloat("length", value)

/**
 * Returns the Euclidean length of a `float4` expression.
 */
public fun length(value: Float4Expr): FloatExpr =
    callFloat("length", value)

/**
 * Returns the dot product of two `float2` expressions.
 */
public fun dot(left: Float2Expr, right: Float2Expr): FloatExpr =
    callFloat("dot", left, right)

/**
 * Returns the dot product of two `float3` expressions.
 */
public fun dot(left: Float3Expr, right: Float3Expr): FloatExpr =
    callFloat("dot", left, right)

/**
 * Returns the dot product of two `float4` expressions.
 */
public fun dot(left: Float4Expr, right: Float4Expr): FloatExpr =
    callFloat("dot", left, right)

/**
 * Converts degrees to radians.
 */
public fun radians(value: FloatExpr): FloatExpr =
    value * 0.01745329252f

/**
 * Replaces the alpha channel of a color expression.
 */
public fun withAlpha(
    color: ColorExpr,
    alpha: FloatExpr
): ColorExpr = ru.redbyte.redbytefx.color(color.r, color.g, color.b, alpha)

/**
 * Replaces the alpha channel of a color expression using a scalar literal.
 */
public fun withAlpha(
    color: ColorExpr,
    alpha: Float
): ColorExpr = withAlpha(color, floatLiteral(alpha))

/**
 * Computes perceptual luminance from a color expression.
 */
public fun luminance(color: ColorExpr): FloatExpr =
    color.r * 0.2126f + color.g * 0.7152f + color.b * 0.0722f

/**
 * Converts a color expression to grayscale while preserving alpha.
 */
public fun grayscale(color: ColorExpr): ColorExpr {
    val l = luminance(color)
    return ru.redbyte.redbytefx.color(l, l, l, color.a)
}
