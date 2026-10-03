package ru.redbyte.redbytefx

import kotlin.jvm.JvmName

/**
 * Integer [Expr] literals and arithmetic for loop indices and compute addressing.
 *
 * Integer math is separate from float [Arith.kt] operators. Use [intLit] for shader `int` constants
 * and [toFloat] when a value must participate in float expressions.
 */

/** Kotlin [Int] → shader `int` literal (for `repeat` bounds, compute indices, etc.). */
public val Int.intLit: Expr<IntS>
    get() = Expr(Shape.Scalar(ScalarKind.Int, null), ExprNode.IntLiteral(this))

/** Widens a shader int expression to highp float (explicit cast in generated source). */
public fun Expr<IntS>.toFloat(): Expr<Flt<High>> = Expr(
    Shape.Scalar(ScalarKind.Float, Precision.High),
    ExprNode.Cast(this),
)

@JvmName("negInt")
public operator fun Expr<IntS>.unaryMinus(): Expr<IntS> = integralUnary(UnaryOp.Neg, this)

@JvmName("plusInt")
public operator fun Expr<IntS>.plus(other: Expr<IntS>): Expr<IntS> =
    integral(ArithOp.Add, this, other)

@JvmName("minusInt")
public operator fun Expr<IntS>.minus(other: Expr<IntS>): Expr<IntS> =
    integral(ArithOp.Sub, this, other)

@JvmName("timesInt")
public operator fun Expr<IntS>.times(other: Expr<IntS>): Expr<IntS> =
    integral(ArithOp.Mul, this, other)

@JvmName("divInt")
public operator fun Expr<IntS>.div(other: Expr<IntS>): Expr<IntS> =
    integral(ArithOp.Div, this, other)

internal fun integralArithShape(op: ArithOp, left: Shape, right: Shape): Shape {
    require(isIntValue(left) && isIntValue(right)) {
        "$op is defined for ints of the same rank, was $left and $right"
    }
    val broadcast = when {
        left == right -> left
        isIntScalar(left) && isIntVector(right) -> right
        isIntVector(left) && isIntScalar(right) -> left
        else -> null
    }
    require(broadcast != null) {
        "$op is defined for ints of the same rank, was $left and $right"
    }
    return broadcast
}

internal fun integralUnaryShape(op: UnaryOp, arg: Shape): Shape {
    require(isIntValue(arg)) { "Unary $op is defined on ints, was $arg" }
    return arg
}

private fun <T : ShType> integral(op: ArithOp, left: Expr<*>, right: Expr<*>): Expr<T> =
    Expr(integralArithShape(op, left.shape, right.shape), ExprNode.Binary(op, left, right))

private fun <T : ShType> integralUnary(op: UnaryOp, arg: Expr<*>): Expr<T> =
    Expr(integralUnaryShape(op, arg.shape), ExprNode.Unary(op, arg))

private fun isIntValue(shape: Shape): Boolean = when (shape) {
    is Shape.Scalar -> shape.kind == ScalarKind.Int
    is Shape.Vector -> shape.kind == ScalarKind.Int
    is Shape.Matrix, Shape.Sampler2D, Shape.SamplerCube, Shape.ChildShader -> false
}

private fun isIntScalar(shape: Shape): Boolean =
    shape is Shape.Scalar && shape.kind == ScalarKind.Int

private fun isIntVector(shape: Shape): Boolean =
    shape is Shape.Vector && shape.kind == ScalarKind.Int
