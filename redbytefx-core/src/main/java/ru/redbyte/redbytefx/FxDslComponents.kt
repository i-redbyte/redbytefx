package ru.redbyte.redbytefx

/**
 * Returns the `x` component of a `float2` expression.
 */
public val Float2Expr.x: FloatExpr
    get() {
        val receiver = this
        return floatExpr { ctx -> "(${emit(receiver, ctx)}).x" }
    }

/**
 * Returns the `y` component of a `float2` expression.
 */
public val Float2Expr.y: FloatExpr
    get() {
        val receiver = this
        return floatExpr { ctx -> "(${emit(receiver, ctx)}).y" }
    }

/**
 * Returns the `x` component of a `float3` expression.
 */
public val Float3Expr.x: FloatExpr
    get() {
        val receiver = this
        return floatExpr { ctx -> "(${emit(receiver, ctx)}).x" }
    }

/**
 * Returns the `y` component of a `float3` expression.
 */
public val Float3Expr.y: FloatExpr
    get() {
        val receiver = this
        return floatExpr { ctx -> "(${emit(receiver, ctx)}).y" }
    }

/**
 * Returns the `z` component of a `float3` expression.
 */
public val Float3Expr.z: FloatExpr
    get() {
        val receiver = this
        return floatExpr { ctx -> "(${emit(receiver, ctx)}).z" }
    }

/**
 * Returns the `x` component of a `float4` expression.
 */
public val Float4Expr.x: FloatExpr
    get() {
        val receiver = this
        return floatExpr { ctx -> "(${emit(receiver, ctx)}).x" }
    }

/**
 * Returns the `y` component of a `float4` expression.
 */
public val Float4Expr.y: FloatExpr
    get() {
        val receiver = this
        return floatExpr { ctx -> "(${emit(receiver, ctx)}).y" }
    }

/**
 * Returns the `z` component of a `float4` expression.
 */
public val Float4Expr.z: FloatExpr
    get() {
        val receiver = this
        return floatExpr { ctx -> "(${emit(receiver, ctx)}).z" }
    }

/**
 * Returns the `w` component of a `float4` expression.
 */
public val Float4Expr.w: FloatExpr
    get() {
        val receiver = this
        return floatExpr { ctx -> "(${emit(receiver, ctx)}).w" }
    }

/**
 * Returns the red channel of a color expression.
 */
public val ColorExpr.r: FloatExpr
    get() {
        val receiver = this
        return floatExpr { ctx -> "(${emit(receiver, ctx)}).r" }
    }

/**
 * Returns the green channel of a color expression.
 */
public val ColorExpr.g: FloatExpr
    get() {
        val receiver = this
        return floatExpr { ctx -> "(${emit(receiver, ctx)}).g" }
    }

/**
 * Returns the blue channel of a color expression.
 */
public val ColorExpr.b: FloatExpr
    get() {
        val receiver = this
        return floatExpr { ctx -> "(${emit(receiver, ctx)}).b" }
    }

/**
 * Returns the alpha channel of a color expression.
 */
public val ColorExpr.a: FloatExpr
    get() {
        val receiver = this
        return floatExpr { ctx -> "(${emit(receiver, ctx)}).a" }
    }
