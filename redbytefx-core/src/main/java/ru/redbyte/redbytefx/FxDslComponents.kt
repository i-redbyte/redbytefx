package ru.redbyte.redbytefx

internal fun floatSwizzle(receiver: Float2Expr, component: String): FloatExpr =
    floatExpr { ctx -> "(${emit(receiver, ctx)}).$component" }

internal fun floatSwizzle(receiver: Float3Expr, component: String): FloatExpr =
    floatExpr { ctx -> "(${emit(receiver, ctx)}).$component" }

internal fun floatSwizzle(receiver: Float4Expr, component: String): FloatExpr =
    floatExpr { ctx -> "(${emit(receiver, ctx)}).$component" }

internal fun floatSwizzle(receiver: ColorExpr, component: String): FloatExpr =
    floatExpr { ctx -> "(${emit(receiver, ctx)}).$component" }

/**
 * Returns the `x` component of a `float2` expression.
 */
public val Float2Expr.x: FloatExpr
    get() = floatSwizzle(this, "x")

/**
 * Returns the `y` component of a `float2` expression.
 */
public val Float2Expr.y: FloatExpr
    get() = floatSwizzle(this, "y")

/**
 * Returns the `x` component of a `float3` expression.
 */
public val Float3Expr.x: FloatExpr
    get() = floatSwizzle(this, "x")

/**
 * Returns the `y` component of a `float3` expression.
 */
public val Float3Expr.y: FloatExpr
    get() = floatSwizzle(this, "y")

/**
 * Returns the `z` component of a `float3` expression.
 */
public val Float3Expr.z: FloatExpr
    get() = floatSwizzle(this, "z")

/**
 * Returns the `x` component of a `float4` expression.
 */
public val Float4Expr.x: FloatExpr
    get() = floatSwizzle(this, "x")

/**
 * Returns the `y` component of a `float4` expression.
 */
public val Float4Expr.y: FloatExpr
    get() = floatSwizzle(this, "y")

/**
 * Returns the `z` component of a `float4` expression.
 */
public val Float4Expr.z: FloatExpr
    get() = floatSwizzle(this, "z")

/**
 * Returns the `w` component of a `float4` expression.
 */
public val Float4Expr.w: FloatExpr
    get() = floatSwizzle(this, "w")

/**
 * Returns the red channel of a color expression.
 */
public val ColorExpr.r: FloatExpr
    get() = floatSwizzle(this, "r")

/**
 * Returns the green channel of a color expression.
 */
public val ColorExpr.g: FloatExpr
    get() = floatSwizzle(this, "g")

/**
 * Returns the blue channel of a color expression.
 */
public val ColorExpr.b: FloatExpr
    get() = floatSwizzle(this, "b")

/**
 * Returns the alpha channel of a color expression.
 */
public val ColorExpr.a: FloatExpr
    get() = floatSwizzle(this, "a")
