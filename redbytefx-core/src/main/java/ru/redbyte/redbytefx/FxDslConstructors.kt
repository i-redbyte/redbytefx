package ru.redbyte.redbytefx

/**
 * Creates a scalar float literal expression.
 *
 * This is mainly useful for extension modules and helper libraries built on top of RedByteFX.
 */
public fun float(value: Float): FloatExpr = floatLiteral(value)

/**
 * Creates a scalar float literal expression from an integer literal.
 *
 * This keeps direct AGSL-style ports readable when the authored math still starts from whole
 * numbers such as `1 - amount` or `2 * uv`.
 */
public fun float(value: Int): FloatExpr = floatLiteral(value.toFloat())

/**
 * Creates a `float2` expression from two scalar expressions.
 */
public fun float2(x: FloatExpr, y: FloatExpr): Float2Expr =
    constructorExpr(Float2Type, "float2", x, y)

/**
 * Creates a `float2` expression from a scalar expression and a literal.
 */
public fun float2(x: FloatExpr, y: Float): Float2Expr =
    float2(x, floatLiteral(y))

/**
 * Creates a `float2` expression from a literal and a scalar expression.
 */
public fun float2(x: Float, y: FloatExpr): Float2Expr =
    float2(floatLiteral(x), y)

/**
 * Creates a `float2` expression from two scalar literals.
 */
public fun float2(x: Float, y: Float): Float2Expr =
    float2(floatLiteral(x), floatLiteral(y))

/**
 * Creates a `float3` expression from three scalar expressions.
 */
public fun float3(x: FloatExpr, y: FloatExpr, z: FloatExpr): Float3Expr =
    constructorExpr(Float3Type, "float3", x, y, z)

/**
 * Creates a `float3` expression from three scalar literals.
 */
public fun float3(x: Float, y: Float, z: Float): Float3Expr =
    float3(floatLiteral(x), floatLiteral(y), floatLiteral(z))

/**
 * Creates a `float3` expression from a `float2` expression and a scalar expression.
 */
public fun float3(xy: Float2Expr, z: FloatExpr): Float3Expr =
    constructorExpr(Float3Type, "float3", xy, z)

/**
 * Creates a `float3` expression from a `float2` expression and a scalar literal.
 */
public fun float3(xy: Float2Expr, z: Float): Float3Expr =
    float3(xy, floatLiteral(z))

/**
 * Creates a `float4` expression from four scalar expressions.
 */
public fun float4(
    x: FloatExpr,
    y: FloatExpr,
    z: FloatExpr,
    w: FloatExpr
): Float4Expr = constructorExpr(Float4Type, "float4", x, y, z, w)

/**
 * Creates a `float4` expression from four scalar literals.
 */
public fun float4(
    x: Float,
    y: Float,
    z: Float,
    w: Float
): Float4Expr = float4(
    floatLiteral(x),
    floatLiteral(y),
    floatLiteral(z),
    floatLiteral(w)
)

/**
 * Creates a `float4` expression from a `float3` expression and a scalar expression.
 */
public fun float4(xyz: Float3Expr, w: FloatExpr): Float4Expr =
    constructorExpr(Float4Type, "float4", xyz, w)

/**
 * Creates a `float4` expression from a `float3` expression and a scalar literal.
 */
public fun float4(xyz: Float3Expr, w: Float): Float4Expr =
    float4(xyz, floatLiteral(w))

/**
 * Creates a `float4` expression from a `float2` expression and two scalar expressions.
 */
public fun float4(xy: Float2Expr, z: FloatExpr, w: FloatExpr): Float4Expr =
    constructorExpr(Float4Type, "float4", xy, z, w)

/**
 * Creates a `float4` expression from a `float2` expression and two scalar literals.
 */
public fun float4(xy: Float2Expr, z: Float, w: Float): Float4Expr =
    float4(xy, floatLiteral(z), floatLiteral(w))

/**
 * Reinterprets a color expression as a `float4` expression.
 */
public fun float4(color: ColorExpr): Float4Expr =
    constructorExpr(Float4Type, "float4", color)

/**
 * Creates a color expression from scalar channel expressions.
 */
public fun color(
    r: FloatExpr,
    g: FloatExpr,
    b: FloatExpr,
    a: FloatExpr = floatLiteral(1f)
): ColorExpr = constructorExpr(ColorType, "half4", r, g, b, a)

/**
 * Creates a color expression from scalar channel literals.
 */
public fun color(
    r: Float,
    g: Float,
    b: Float,
    a: Float = 1f
): ColorExpr = color(
    floatLiteral(r),
    floatLiteral(g),
    floatLiteral(b),
    floatLiteral(a)
)

/**
 * Creates a color expression from an RGB expression and an alpha expression.
 */
public fun color(rgb: Float3Expr, a: FloatExpr = floatLiteral(1f)): ColorExpr =
    color(rgb.x, rgb.y, rgb.z, a)

/**
 * Creates a color expression from an RGB expression and a literal alpha.
 */
public fun color(rgb: Float3Expr, a: Float): ColorExpr =
    color(rgb, floatLiteral(a))

/**
 * Reinterprets a `float4` expression as a color expression.
 */
public fun color(value: Float4Expr): ColorExpr =
    constructorExpr(ColorType, "half4", value)

/**
 * Selects between two scalar expressions using a boolean condition.
 */
public fun ifElse(
    condition: BoolExpr,
    ifTrue: FloatExpr,
    ifFalse: FloatExpr
): FloatExpr = conditionalExpr(FloatType, condition, ifTrue, ifFalse)

/**
 * Selects between two `float2` expressions using a boolean condition.
 */
public fun ifElse(
    condition: BoolExpr,
    ifTrue: Float2Expr,
    ifFalse: Float2Expr
): Float2Expr = conditionalExpr(Float2Type, condition, ifTrue, ifFalse)

/**
 * Selects between two `float3` expressions using a boolean condition.
 */
public fun ifElse(
    condition: BoolExpr,
    ifTrue: Float3Expr,
    ifFalse: Float3Expr
): Float3Expr = conditionalExpr(Float3Type, condition, ifTrue, ifFalse)

/**
 * Selects between two `float4` expressions using a boolean condition.
 */
public fun ifElse(
    condition: BoolExpr,
    ifTrue: Float4Expr,
    ifFalse: Float4Expr
): Float4Expr = conditionalExpr(Float4Type, condition, ifTrue, ifFalse)

/**
 * Selects between two color expressions using a boolean condition.
 */
public fun ifElse(
    condition: BoolExpr,
    ifTrue: ColorExpr,
    ifFalse: ColorExpr
): ColorExpr = conditionalExpr(ColorType, condition, ifTrue, ifFalse)
