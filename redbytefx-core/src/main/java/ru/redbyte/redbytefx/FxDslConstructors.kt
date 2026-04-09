package ru.redbyte.redbytefx

/**
 * Creates a `float2` expression from two scalar expressions.
 */
public fun float2(x: FloatExpr, y: FloatExpr): Float2Expr =
    float2Expr { ctx -> "float2(${emit(x, ctx)}, ${emit(y, ctx)})" }

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
    float3Expr { ctx -> "float3(${emit(x, ctx)}, ${emit(y, ctx)}, ${emit(z, ctx)})" }

/**
 * Creates a `float3` expression from three scalar literals.
 */
public fun float3(x: Float, y: Float, z: Float): Float3Expr =
    float3(floatLiteral(x), floatLiteral(y), floatLiteral(z))

/**
 * Creates a `float3` expression from a `float2` expression and a scalar expression.
 */
public fun float3(xy: Float2Expr, z: FloatExpr): Float3Expr =
    float3Expr { ctx -> "float3(${emit(xy, ctx)}, ${emit(z, ctx)})" }

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
): Float4Expr = float4Expr { ctx ->
    "float4(${emit(x, ctx)}, ${emit(y, ctx)}, ${emit(z, ctx)}, ${emit(w, ctx)})"
}

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
    float4Expr { ctx -> "float4(${emit(xyz, ctx)}, ${emit(w, ctx)})" }

/**
 * Creates a `float4` expression from a `float3` expression and a scalar literal.
 */
public fun float4(xyz: Float3Expr, w: Float): Float4Expr =
    float4(xyz, floatLiteral(w))

/**
 * Creates a `float4` expression from a `float2` expression and two scalar expressions.
 */
public fun float4(xy: Float2Expr, z: FloatExpr, w: FloatExpr): Float4Expr =
    float4Expr { ctx -> "float4(${emit(xy, ctx)}, ${emit(z, ctx)}, ${emit(w, ctx)})" }

/**
 * Creates a `float4` expression from a `float2` expression and two scalar literals.
 */
public fun float4(xy: Float2Expr, z: Float, w: Float): Float4Expr =
    float4(xy, floatLiteral(z), floatLiteral(w))

/**
 * Reinterprets a color expression as a `float4` expression.
 */
public fun float4(color: ColorExpr): Float4Expr =
    float4Expr { ctx -> "float4(${emit(color, ctx)})" }

/**
 * Creates a color expression from scalar channel expressions.
 */
public fun color(
    r: FloatExpr,
    g: FloatExpr,
    b: FloatExpr,
    a: FloatExpr = floatLiteral(1f)
): ColorExpr = colorExpr { ctx ->
    "half4(${emit(r, ctx)}, ${emit(g, ctx)}, ${emit(b, ctx)}, ${emit(a, ctx)})"
}

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
    colorExpr { ctx -> "half4(${emit(value, ctx)})" }

/**
 * Selects between two scalar expressions using a boolean condition.
 */
public fun ifElse(
    condition: BoolExpr,
    ifTrue: FloatExpr,
    ifFalse: FloatExpr
): FloatExpr = floatExpr { ctx ->
    "(${emit(condition, ctx)} ? ${emit(ifTrue, ctx)} : ${emit(ifFalse, ctx)})"
}

/**
 * Selects between two `float2` expressions using a boolean condition.
 */
public fun ifElse(
    condition: BoolExpr,
    ifTrue: Float2Expr,
    ifFalse: Float2Expr
): Float2Expr = float2Expr { ctx ->
    "(${emit(condition, ctx)} ? ${emit(ifTrue, ctx)} : ${emit(ifFalse, ctx)})"
}

/**
 * Selects between two `float3` expressions using a boolean condition.
 */
public fun ifElse(
    condition: BoolExpr,
    ifTrue: Float3Expr,
    ifFalse: Float3Expr
): Float3Expr = float3Expr { ctx ->
    "(${emit(condition, ctx)} ? ${emit(ifTrue, ctx)} : ${emit(ifFalse, ctx)})"
}

/**
 * Selects between two `float4` expressions using a boolean condition.
 */
public fun ifElse(
    condition: BoolExpr,
    ifTrue: Float4Expr,
    ifFalse: Float4Expr
): Float4Expr = float4Expr { ctx ->
    "(${emit(condition, ctx)} ? ${emit(ifTrue, ctx)} : ${emit(ifFalse, ctx)})"
}

/**
 * Selects between two color expressions using a boolean condition.
 */
public fun ifElse(
    condition: BoolExpr,
    ifTrue: ColorExpr,
    ifFalse: ColorExpr
): ColorExpr = colorExpr { ctx ->
    "(${emit(condition, ctx)} ? ${emit(ifTrue, ctx)} : ${emit(ifFalse, ctx)})"
}
