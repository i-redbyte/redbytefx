package ru.redbyte.redbytefx

import kotlin.jvm.JvmName

/**
 * Result shape of float addition, subtraction, multiplication, or division.
 *
 * A scalar broadcasts over a vector of the same precision. Mismatched precision, rank, or a
 * non-float form is rejected. All four operators share this rule.
 */
internal fun arithShape(op: ArithOp, left: Shape, right: Shape): Shape {
    require(isFloatValue(left) && isFloatValue(right) && precisionOf(left) == precisionOf(right)) {
        arithMessage(op, left, right)
    }
    val broadcast = when {
        left == right -> left
        isFloatScalar(left) && isFloatVector(right) -> right
        isFloatVector(left) && isFloatScalar(right) -> left
        else -> null
    }
    require(broadcast != null) { arithMessage(op, left, right) }
    return broadcast
}

/** Result shape of float negation. */
internal fun unaryShape(op: UnaryOp, arg: Shape): Shape {
    require(isFloatValue(arg)) {
        "Unary $op is defined on float scalars and vectors, was $arg"
    }
    return arg
}

public val Float.lit: Expr<Flt<High>>
    get() = floatLiteral(this, Precision.High)

public val Float.med: Expr<Flt<Med>>
    get() = floatLiteral(this, Precision.Med)

public val Int.lit: Expr<Flt<High>>
    get() = toFloat().lit

public fun <P : Prec> vec2(x: Expr<Flt<P>>, y: Expr<Flt<P>>): Expr<Vec2<Flt<P>>> =
    vector(x, y)

public fun <P : Prec> vec3(
    x: Expr<Flt<P>>,
    y: Expr<Flt<P>>,
    z: Expr<Flt<P>>,
): Expr<Vec3<Flt<P>>> = vector(x, y, z)

public fun <P : Prec> vec4(
    x: Expr<Flt<P>>,
    y: Expr<Flt<P>>,
    z: Expr<Flt<P>>,
    w: Expr<Flt<P>>,
): Expr<Vec4<Flt<P>>> = vector(x, y, z, w)

@JvmName("negFlt")
public operator fun <P : Prec> Expr<Flt<P>>.unaryMinus(): Expr<Flt<P>> =
    unary(UnaryOp.Neg, this)

@JvmName("negVec2")
public operator fun <P : Prec> Expr<Vec2<Flt<P>>>.unaryMinus(): Expr<Vec2<Flt<P>>> =
    unary(UnaryOp.Neg, this)

@JvmName("negVec3")
public operator fun <P : Prec> Expr<Vec3<Flt<P>>>.unaryMinus(): Expr<Vec3<Flt<P>>> =
    unary(UnaryOp.Neg, this)

@JvmName("negVec4")
public operator fun <P : Prec> Expr<Vec4<Flt<P>>>.unaryMinus(): Expr<Vec4<Flt<P>>> =
    unary(UnaryOp.Neg, this)

@JvmName("plusFlt")
public operator fun <P : Prec> Expr<Flt<P>>.plus(other: Expr<Flt<P>>): Expr<Flt<P>> =
    arith(ArithOp.Add, this, other)

@JvmName("minusFlt")
public operator fun <P : Prec> Expr<Flt<P>>.minus(other: Expr<Flt<P>>): Expr<Flt<P>> =
    arith(ArithOp.Sub, this, other)

@JvmName("timesFlt")
public operator fun <P : Prec> Expr<Flt<P>>.times(other: Expr<Flt<P>>): Expr<Flt<P>> =
    arith(ArithOp.Mul, this, other)

@JvmName("divFlt")
public operator fun <P : Prec> Expr<Flt<P>>.div(other: Expr<Flt<P>>): Expr<Flt<P>> =
    arith(ArithOp.Div, this, other)

@JvmName("plusFltVec2")
public operator fun <P : Prec> Expr<Flt<P>>.plus(other: Expr<Vec2<Flt<P>>>): Expr<Vec2<Flt<P>>> =
    arith(ArithOp.Add, this, other)

@JvmName("minusFltVec2")
public operator fun <P : Prec> Expr<Flt<P>>.minus(other: Expr<Vec2<Flt<P>>>): Expr<Vec2<Flt<P>>> =
    arith(ArithOp.Sub, this, other)

@JvmName("timesFltVec2")
public operator fun <P : Prec> Expr<Flt<P>>.times(other: Expr<Vec2<Flt<P>>>): Expr<Vec2<Flt<P>>> =
    arith(ArithOp.Mul, this, other)

@JvmName("divFltVec2")
public operator fun <P : Prec> Expr<Flt<P>>.div(other: Expr<Vec2<Flt<P>>>): Expr<Vec2<Flt<P>>> =
    arith(ArithOp.Div, this, other)

@JvmName("plusFltVec3")
public operator fun <P : Prec> Expr<Flt<P>>.plus(other: Expr<Vec3<Flt<P>>>): Expr<Vec3<Flt<P>>> =
    arith(ArithOp.Add, this, other)

@JvmName("minusFltVec3")
public operator fun <P : Prec> Expr<Flt<P>>.minus(other: Expr<Vec3<Flt<P>>>): Expr<Vec3<Flt<P>>> =
    arith(ArithOp.Sub, this, other)

@JvmName("timesFltVec3")
public operator fun <P : Prec> Expr<Flt<P>>.times(other: Expr<Vec3<Flt<P>>>): Expr<Vec3<Flt<P>>> =
    arith(ArithOp.Mul, this, other)

@JvmName("divFltVec3")
public operator fun <P : Prec> Expr<Flt<P>>.div(other: Expr<Vec3<Flt<P>>>): Expr<Vec3<Flt<P>>> =
    arith(ArithOp.Div, this, other)

@JvmName("plusFltVec4")
public operator fun <P : Prec> Expr<Flt<P>>.plus(other: Expr<Vec4<Flt<P>>>): Expr<Vec4<Flt<P>>> =
    arith(ArithOp.Add, this, other)

@JvmName("minusFltVec4")
public operator fun <P : Prec> Expr<Flt<P>>.minus(other: Expr<Vec4<Flt<P>>>): Expr<Vec4<Flt<P>>> =
    arith(ArithOp.Sub, this, other)

@JvmName("timesFltVec4")
public operator fun <P : Prec> Expr<Flt<P>>.times(other: Expr<Vec4<Flt<P>>>): Expr<Vec4<Flt<P>>> =
    arith(ArithOp.Mul, this, other)

@JvmName("divFltVec4")
public operator fun <P : Prec> Expr<Flt<P>>.div(other: Expr<Vec4<Flt<P>>>): Expr<Vec4<Flt<P>>> =
    arith(ArithOp.Div, this, other)

@JvmName("plusVec2Flt")
public operator fun <P : Prec> Expr<Vec2<Flt<P>>>.plus(other: Expr<Flt<P>>): Expr<Vec2<Flt<P>>> =
    arith(ArithOp.Add, this, other)

@JvmName("minusVec2Flt")
public operator fun <P : Prec> Expr<Vec2<Flt<P>>>.minus(other: Expr<Flt<P>>): Expr<Vec2<Flt<P>>> =
    arith(ArithOp.Sub, this, other)

@JvmName("timesVec2Flt")
public operator fun <P : Prec> Expr<Vec2<Flt<P>>>.times(other: Expr<Flt<P>>): Expr<Vec2<Flt<P>>> =
    arith(ArithOp.Mul, this, other)

@JvmName("divVec2Flt")
public operator fun <P : Prec> Expr<Vec2<Flt<P>>>.div(other: Expr<Flt<P>>): Expr<Vec2<Flt<P>>> =
    arith(ArithOp.Div, this, other)

@JvmName("plusVec3Flt")
public operator fun <P : Prec> Expr<Vec3<Flt<P>>>.plus(other: Expr<Flt<P>>): Expr<Vec3<Flt<P>>> =
    arith(ArithOp.Add, this, other)

@JvmName("minusVec3Flt")
public operator fun <P : Prec> Expr<Vec3<Flt<P>>>.minus(other: Expr<Flt<P>>): Expr<Vec3<Flt<P>>> =
    arith(ArithOp.Sub, this, other)

@JvmName("timesVec3Flt")
public operator fun <P : Prec> Expr<Vec3<Flt<P>>>.times(other: Expr<Flt<P>>): Expr<Vec3<Flt<P>>> =
    arith(ArithOp.Mul, this, other)

@JvmName("divVec3Flt")
public operator fun <P : Prec> Expr<Vec3<Flt<P>>>.div(other: Expr<Flt<P>>): Expr<Vec3<Flt<P>>> =
    arith(ArithOp.Div, this, other)

@JvmName("plusVec4Flt")
public operator fun <P : Prec> Expr<Vec4<Flt<P>>>.plus(other: Expr<Flt<P>>): Expr<Vec4<Flt<P>>> =
    arith(ArithOp.Add, this, other)

@JvmName("minusVec4Flt")
public operator fun <P : Prec> Expr<Vec4<Flt<P>>>.minus(other: Expr<Flt<P>>): Expr<Vec4<Flt<P>>> =
    arith(ArithOp.Sub, this, other)

@JvmName("timesVec4Flt")
public operator fun <P : Prec> Expr<Vec4<Flt<P>>>.times(other: Expr<Flt<P>>): Expr<Vec4<Flt<P>>> =
    arith(ArithOp.Mul, this, other)

@JvmName("divVec4Flt")
public operator fun <P : Prec> Expr<Vec4<Flt<P>>>.div(other: Expr<Flt<P>>): Expr<Vec4<Flt<P>>> =
    arith(ArithOp.Div, this, other)

@JvmName("plusVec2")
public operator fun <P : Prec> Expr<Vec2<Flt<P>>>.plus(other: Expr<Vec2<Flt<P>>>): Expr<Vec2<Flt<P>>> =
    arith(ArithOp.Add, this, other)

@JvmName("minusVec2")
public operator fun <P : Prec> Expr<Vec2<Flt<P>>>.minus(other: Expr<Vec2<Flt<P>>>): Expr<Vec2<Flt<P>>> =
    arith(ArithOp.Sub, this, other)

@JvmName("timesVec2")
public operator fun <P : Prec> Expr<Vec2<Flt<P>>>.times(other: Expr<Vec2<Flt<P>>>): Expr<Vec2<Flt<P>>> =
    arith(ArithOp.Mul, this, other)

@JvmName("divVec2")
public operator fun <P : Prec> Expr<Vec2<Flt<P>>>.div(other: Expr<Vec2<Flt<P>>>): Expr<Vec2<Flt<P>>> =
    arith(ArithOp.Div, this, other)

@JvmName("plusVec3")
public operator fun <P : Prec> Expr<Vec3<Flt<P>>>.plus(other: Expr<Vec3<Flt<P>>>): Expr<Vec3<Flt<P>>> =
    arith(ArithOp.Add, this, other)

@JvmName("minusVec3")
public operator fun <P : Prec> Expr<Vec3<Flt<P>>>.minus(other: Expr<Vec3<Flt<P>>>): Expr<Vec3<Flt<P>>> =
    arith(ArithOp.Sub, this, other)

@JvmName("timesVec3")
public operator fun <P : Prec> Expr<Vec3<Flt<P>>>.times(other: Expr<Vec3<Flt<P>>>): Expr<Vec3<Flt<P>>> =
    arith(ArithOp.Mul, this, other)

@JvmName("divVec3")
public operator fun <P : Prec> Expr<Vec3<Flt<P>>>.div(other: Expr<Vec3<Flt<P>>>): Expr<Vec3<Flt<P>>> =
    arith(ArithOp.Div, this, other)

@JvmName("plusVec4")
public operator fun <P : Prec> Expr<Vec4<Flt<P>>>.plus(other: Expr<Vec4<Flt<P>>>): Expr<Vec4<Flt<P>>> =
    arith(ArithOp.Add, this, other)

@JvmName("minusVec4")
public operator fun <P : Prec> Expr<Vec4<Flt<P>>>.minus(other: Expr<Vec4<Flt<P>>>): Expr<Vec4<Flt<P>>> =
    arith(ArithOp.Sub, this, other)

@JvmName("timesVec4")
public operator fun <P : Prec> Expr<Vec4<Flt<P>>>.times(other: Expr<Vec4<Flt<P>>>): Expr<Vec4<Flt<P>>> =
    arith(ArithOp.Mul, this, other)

@JvmName("divVec4")
public operator fun <P : Prec> Expr<Vec4<Flt<P>>>.div(other: Expr<Vec4<Flt<P>>>): Expr<Vec4<Flt<P>>> =
    arith(ArithOp.Div, this, other)

private fun <P : Prec> floatLiteral(value: Float, precision: Precision): Expr<Flt<P>> {
    require(value.isFinite()) { "Float literal must be finite, was $value" }
    return Expr(
        Shape.Scalar(ScalarKind.Float, precision),
        ExprNode.Literal(value, precision),
    )
}

private fun <T : ShType> vector(vararg components: Expr<*>): Expr<T> {
    require(components.size in 2..4) { "Vector constructor needs 2, 3, or 4 components" }
    val componentShape = components[0].shape
    require(componentShape is Shape.Scalar && componentShape.kind == ScalarKind.Float) {
        "Vector components must be float scalars"
    }
    for (component in components) {
        require(component.shape == componentShape) {
            "Vector components must share one float precision"
        }
    }
    return Expr(
        Shape.Vector(ScalarKind.Float, componentShape.precision, components.size),
        ExprNode.Construct(components.asList()),
    )
}

private fun <T : ShType> arith(op: ArithOp, left: Expr<*>, right: Expr<*>): Expr<T> =
    Expr(arithShape(op, left.shape, right.shape), ExprNode.Binary(op, left, right))

private fun <T : ShType> unary(op: UnaryOp, arg: Expr<*>): Expr<T> =
    Expr(unaryShape(op, arg.shape), ExprNode.Unary(op, arg))

private fun isFloatValue(shape: Shape): Boolean = when (shape) {
    is Shape.Scalar -> shape.kind == ScalarKind.Float
    is Shape.Vector -> shape.kind == ScalarKind.Float
    is Shape.Matrix, Shape.Sampler2D, Shape.ChildShader -> false
}

private fun isFloatScalar(shape: Shape): Boolean =
    shape is Shape.Scalar && shape.kind == ScalarKind.Float

private fun isFloatVector(shape: Shape): Boolean =
    shape is Shape.Vector && shape.kind == ScalarKind.Float

private fun precisionOf(shape: Shape): Precision? = when (shape) {
    is Shape.Scalar -> shape.precision
    is Shape.Vector -> shape.precision
    is Shape.Matrix, Shape.Sampler2D, Shape.ChildShader -> null
}

private fun arithMessage(op: ArithOp, left: Shape, right: Shape): String =
    "$op is defined for float scalars and vectors of the same precision, was $left and $right"
