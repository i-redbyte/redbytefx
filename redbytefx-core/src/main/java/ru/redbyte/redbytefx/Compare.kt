package ru.redbyte.redbytefx

import kotlin.jvm.JvmName

/**
 * Comparisons and boolean logic on shader expressions.
 *
 * Scalar floats use infix [gt], [lt], [ge], [le], [eq], [ne] (spell as `>`, `<`, … in GLSL).
 * Vector compares return [BVec2] / [BVec3] / [BVec4]; reduce with [any] or [all].
 * [ifElse] is the portable ternary. [not] spells `!`. Prefer [gte] in [Sugar.kt] when `>=` reads more clearly in Kotlin.
 */

/** Float greater-than; result is a shader `bool`. */
public infix fun <P : Prec> Expr<Flt<P>>.gt(other: Expr<Flt<P>>): Expr<BoolS> = compare(CompareOp.Gt, this, other)

public infix fun <P : Prec> Expr<Flt<P>>.lt(other: Expr<Flt<P>>): Expr<BoolS> = compare(CompareOp.Lt, this, other)

public infix fun <P : Prec> Expr<Flt<P>>.ge(other: Expr<Flt<P>>): Expr<BoolS> = compare(CompareOp.Ge, this, other)

public infix fun <P : Prec> Expr<Flt<P>>.le(other: Expr<Flt<P>>): Expr<BoolS> = compare(CompareOp.Le, this, other)

public infix fun <P : Prec> Expr<Flt<P>>.eq(other: Expr<Flt<P>>): Expr<BoolS> = compare(CompareOp.Eq, this, other)

public infix fun <P : Prec> Expr<Flt<P>>.ne(other: Expr<Flt<P>>): Expr<BoolS> = compare(CompareOp.Ne, this, other)

@JvmName("gtVec2")
public infix fun <P : Prec> Expr<Vec2<Flt<P>>>.gt(other: Expr<Vec2<Flt<P>>>): Expr<BVec2> =
    vectorCompare(CompareOp.Gt, this, other)

@JvmName("ltVec2")
public infix fun <P : Prec> Expr<Vec2<Flt<P>>>.lt(other: Expr<Vec2<Flt<P>>>): Expr<BVec2> =
    vectorCompare(CompareOp.Lt, this, other)

@JvmName("geVec2")
public infix fun <P : Prec> Expr<Vec2<Flt<P>>>.ge(other: Expr<Vec2<Flt<P>>>): Expr<BVec2> =
    vectorCompare(CompareOp.Ge, this, other)

@JvmName("leVec2")
public infix fun <P : Prec> Expr<Vec2<Flt<P>>>.le(other: Expr<Vec2<Flt<P>>>): Expr<BVec2> =
    vectorCompare(CompareOp.Le, this, other)

@JvmName("eqVec2")
public infix fun <P : Prec> Expr<Vec2<Flt<P>>>.eq(other: Expr<Vec2<Flt<P>>>): Expr<BVec2> =
    vectorCompare(CompareOp.Eq, this, other)

@JvmName("neVec2")
public infix fun <P : Prec> Expr<Vec2<Flt<P>>>.ne(other: Expr<Vec2<Flt<P>>>): Expr<BVec2> =
    vectorCompare(CompareOp.Ne, this, other)

@JvmName("gtVec3")
public infix fun <P : Prec> Expr<Vec3<Flt<P>>>.gt(other: Expr<Vec3<Flt<P>>>): Expr<BVec3> =
    vectorCompare(CompareOp.Gt, this, other)

@JvmName("ltVec3")
public infix fun <P : Prec> Expr<Vec3<Flt<P>>>.lt(other: Expr<Vec3<Flt<P>>>): Expr<BVec3> =
    vectorCompare(CompareOp.Lt, this, other)

@JvmName("geVec3")
public infix fun <P : Prec> Expr<Vec3<Flt<P>>>.ge(other: Expr<Vec3<Flt<P>>>): Expr<BVec3> =
    vectorCompare(CompareOp.Ge, this, other)

@JvmName("leVec3")
public infix fun <P : Prec> Expr<Vec3<Flt<P>>>.le(other: Expr<Vec3<Flt<P>>>): Expr<BVec3> =
    vectorCompare(CompareOp.Le, this, other)

@JvmName("eqVec3")
public infix fun <P : Prec> Expr<Vec3<Flt<P>>>.eq(other: Expr<Vec3<Flt<P>>>): Expr<BVec3> =
    vectorCompare(CompareOp.Eq, this, other)

@JvmName("neVec3")
public infix fun <P : Prec> Expr<Vec3<Flt<P>>>.ne(other: Expr<Vec3<Flt<P>>>): Expr<BVec3> =
    vectorCompare(CompareOp.Ne, this, other)

@JvmName("gtVec4")
public infix fun <P : Prec> Expr<Vec4<Flt<P>>>.gt(other: Expr<Vec4<Flt<P>>>): Expr<BVec4> =
    vectorCompare(CompareOp.Gt, this, other)

@JvmName("ltVec4")
public infix fun <P : Prec> Expr<Vec4<Flt<P>>>.lt(other: Expr<Vec4<Flt<P>>>): Expr<BVec4> =
    vectorCompare(CompareOp.Lt, this, other)

@JvmName("geVec4")
public infix fun <P : Prec> Expr<Vec4<Flt<P>>>.ge(other: Expr<Vec4<Flt<P>>>): Expr<BVec4> =
    vectorCompare(CompareOp.Ge, this, other)

@JvmName("leVec4")
public infix fun <P : Prec> Expr<Vec4<Flt<P>>>.le(other: Expr<Vec4<Flt<P>>>): Expr<BVec4> =
    vectorCompare(CompareOp.Le, this, other)

@JvmName("eqVec4")
public infix fun <P : Prec> Expr<Vec4<Flt<P>>>.eq(other: Expr<Vec4<Flt<P>>>): Expr<BVec4> =
    vectorCompare(CompareOp.Eq, this, other)

@JvmName("neVec4")
public infix fun <P : Prec> Expr<Vec4<Flt<P>>>.ne(other: Expr<Vec4<Flt<P>>>): Expr<BVec4> =
    vectorCompare(CompareOp.Ne, this, other)

@JvmName("anyBVec2")
public fun any(value: Expr<BVec2>): Expr<BoolS> = boolReduce("any", value)

@JvmName("anyBVec3")
public fun any(value: Expr<BVec3>): Expr<BoolS> = boolReduce("any", value)

@JvmName("anyBVec4")
public fun any(value: Expr<BVec4>): Expr<BoolS> = boolReduce("any", value)

@JvmName("allBVec2")
public fun all(value: Expr<BVec2>): Expr<BoolS> = boolReduce("all", value)

@JvmName("allBVec3")
public fun all(value: Expr<BVec3>): Expr<BoolS> = boolReduce("all", value)

@JvmName("allBVec4")
public fun all(value: Expr<BVec4>): Expr<BoolS> = boolReduce("all", value)

public infix fun Expr<BoolS>.and(other: Expr<BoolS>): Expr<BoolS> = boolOp(ArithOp.And, this, other)

public infix fun Expr<BoolS>.or(other: Expr<BoolS>): Expr<BoolS> = boolOp(ArithOp.Or, this, other)

/** Shade. Boolean negation. AGSL and GLSL both spell `!`. */
public fun not(value: Expr<BoolS>): Expr<BoolS> =
    Expr(Shape.Scalar(ScalarKind.Bool, null), ExprNode.Unary(UnaryOp.Not, value))

/** Portable `condition ? ifTrue : ifFalse` for matching expression types. */
public fun <T : ShType> ifElse(condition: Expr<BoolS>, ifTrue: Expr<T>, ifFalse: Expr<T>): Expr<T> {
    require(condition.shape == boolScalar) { "ifElse condition must be a bool, was ${condition.shape}" }
    require(ifTrue.shape == ifFalse.shape) {
        "ifElse branches must share a shape, was ${ifTrue.shape} and ${ifFalse.shape}"
    }
    return Expr(ifTrue.shape, ExprNode.Select(condition, ifTrue, ifFalse))
}

private fun boolOp(op: ArithOp, left: Expr<BoolS>, right: Expr<BoolS>): Expr<BoolS> =
    Expr(Shape.Scalar(ScalarKind.Bool, null), ExprNode.Binary(op, left, right))

private fun <P : Prec> compare(op: CompareOp, left: Expr<Flt<P>>, right: Expr<Flt<P>>): Expr<BoolS> {
    require(isFloatScalar(left.shape) && left.shape == right.shape) {
        "$op requires float scalars of one precision, was ${left.shape} and ${right.shape}"
    }
    return Expr(boolScalar, ExprNode.Compare(op, left, right))
}

internal fun <T : ShType> vectorCompare(op: CompareOp, left: Expr<*>, right: Expr<*>): Expr<T> {
    require(isFloatVector(left.shape) && left.shape == right.shape) {
        "$op requires float vectors of one precision and length, was ${left.shape} and ${right.shape}"
    }
    val lanes = (left.shape as Shape.Vector).lanes
    return Expr(Shape.Vector(ScalarKind.Bool, null, lanes), ExprNode.Compare(op, left, right))
}

private fun boolReduce(function: String, value: Expr<*>): Expr<BoolS> {
    require(value.shape is Shape.Vector && value.shape.kind == ScalarKind.Bool) {
        "$function requires a bool vector, was ${value.shape}"
    }
    return Expr(boolScalar, ExprNode.Call(function, listOf(value)))
}

internal fun spellCompare(
    op: CompareOp,
    left: Expr<*>,
    right: Expr<*>,
    render: (Expr<*>) -> String,
): String {
    val leftText = render(left)
    val rightText = render(right)
    return if (isFloatVector(left.shape)) {
        "${op.componentFunction}($leftText, $rightText)"
    } else {
        "($leftText ${op.symbol} $rightText)"
    }
}

private val boolScalar = Shape.Scalar(ScalarKind.Bool, null)

internal val CompareOp.symbol: String
    get() = when (this) {
        CompareOp.Gt -> ">"
        CompareOp.Lt -> "<"
        CompareOp.Ge -> ">="
        CompareOp.Le -> "<="
        CompareOp.Eq -> "=="
        CompareOp.Ne -> "!="
    }

private val CompareOp.componentFunction: String
    get() = when (this) {
        CompareOp.Gt -> "greaterThan"
        CompareOp.Lt -> "lessThan"
        CompareOp.Ge -> "greaterThanEqual"
        CompareOp.Le -> "lessThanEqual"
        CompareOp.Eq -> "equal"
        CompareOp.Ne -> "notEqual"
    }
