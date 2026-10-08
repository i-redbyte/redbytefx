package ru.redbyte.redbytefx

import kotlin.jvm.JvmName

/**
 * Portable math. Each function spells the same call in AGSL and GLSL ES 3.00.
 * A scalar broadcasts only where both languages accept it. [pow] does not.
 */

public fun <T : ShType> sin(value: Expr<T>): Expr<T> = unaryFloat("sin", value)

public fun <T : ShType> cos(value: Expr<T>): Expr<T> = unaryFloat("cos", value)

public fun <T : ShType> tan(value: Expr<T>): Expr<T> = unaryFloat("tan", value)

public fun <T : ShType> sign(value: Expr<T>): Expr<T> = unaryFloat("sign", value)

public fun <T : ShType> abs(value: Expr<T>): Expr<T> = unaryFloat("abs", value)

public fun <T : ShType> floor(value: Expr<T>): Expr<T> = unaryFloat("floor", value)

public fun <T : ShType> ceil(value: Expr<T>): Expr<T> = unaryFloat("ceil", value)

public fun <T : ShType> fract(value: Expr<T>): Expr<T> = unaryFloat("fract", value)

public fun <T : ShType> sqrt(value: Expr<T>): Expr<T> = unaryFloat("sqrt", value)

public fun <T : ShType> dFdx(value: Expr<T>): Expr<T> = derivative("dFdx", value)

public fun <T : ShType> dFdy(value: Expr<T>): Expr<T> = derivative("dFdy", value)

public fun <T : ShType> fwidth(value: Expr<T>): Expr<T> = derivative("fwidth", value)

public fun <T : ShType> radians(value: Expr<T>): Expr<T> = unaryFloat("radians", value)

public fun <T : ShType> atan(value: Expr<T>): Expr<T> = unaryFloat("atan", value)

public fun <T : ShType> reflect(incident: Expr<T>, normal: Expr<T>): Expr<T> =
    sameShape("reflect", incident, normal)

@JvmName("transposeMat2")
public fun transpose(matrix: Expr<Mat2>): Expr<Mat2> = transposeOf(matrix)

@JvmName("transposeMat3")
public fun transpose(matrix: Expr<Mat3>): Expr<Mat3> = transposeOf(matrix)

@JvmName("transposeMat4")
public fun transpose(matrix: Expr<Mat4>): Expr<Mat4> = transposeOf(matrix)

/** Matrix inverse; the result is undefined for singular matrices, as in the shader languages. */
@JvmName("inverseMat2")
public fun inverse(matrix: Expr<Mat2>): Expr<Mat2> = matrixUnary("inverse", matrix)

@JvmName("inverseMat3")
public fun inverse(matrix: Expr<Mat3>): Expr<Mat3> = matrixUnary("inverse", matrix)

@JvmName("inverseMat4")
public fun inverse(matrix: Expr<Mat4>): Expr<Mat4> = matrixUnary("inverse", matrix)

/** Multiplies matching matrix components; use `*` for the usual matrix product. */
@JvmName("matrixCompMultMat2")
public fun matrixCompMult(left: Expr<Mat2>, right: Expr<Mat2>): Expr<Mat2> = matrixCompMultOf(left, right)

@JvmName("matrixCompMultMat3")
public fun matrixCompMult(left: Expr<Mat3>, right: Expr<Mat3>): Expr<Mat3> = matrixCompMultOf(left, right)

@JvmName("matrixCompMultMat4")
public fun matrixCompMult(left: Expr<Mat4>, right: Expr<Mat4>): Expr<Mat4> = matrixCompMultOf(left, right)

public fun <T : ShType> atan(y: Expr<T>, x: Expr<T>): Expr<T> = sameShape("atan", y, x)

public fun <T : ShType> min(left: Expr<T>, right: Expr<*>): Expr<T> =
    broadcast("min", left, right, listOf(left, right))

public fun <T : ShType> max(left: Expr<T>, right: Expr<*>): Expr<T> =
    broadcast("max", left, right, listOf(left, right))

public fun <T : ShType> mod(left: Expr<T>, right: Expr<*>): Expr<T> =
    broadcast("mod", left, right, listOf(left, right))

public fun <T : ShType> pow(base: Expr<T>, exponent: Expr<T>): Expr<T> = sameShape("pow", base, exponent)

public fun <T : ShType> step(edge: Expr<*>, value: Expr<T>): Expr<T> =
    broadcast("step", value, edge, listOf(edge, value))

public fun <T : ShType> clamp(value: Expr<T>, low: Expr<*>, high: Expr<*>): Expr<T> {
    require(isFloatValue(value.shape)) { "clamp requires a float value, was ${value.shape}" }
    require(low.shape == high.shape) { "clamp bounds must share a shape, was ${low.shape} and ${high.shape}" }
    require(acceptsBound(value.shape, low.shape)) {
        "clamp bounds must match ${value.shape} or be a scalar of the same precision, was ${low.shape}"
    }
    return call(value.shape, "clamp", listOf(value, low, high))
}

public fun <T : ShType> saturate(value: Expr<T>): Expr<T> {
    require(isFloatValue(value.shape)) { "saturate requires a float value, was ${value.shape}" }
    val precision = precisionOf(value.shape) ?: error("Float value is missing precision")
    return clamp(value, literal(0f, precision), literal(1f, precision))
}

public fun <T : ShType> smoothstep(edge0: Expr<*>, edge1: Expr<*>, value: Expr<T>): Expr<T> {
    require(edge0.shape == edge1.shape) {
        "smoothstep edges must share a shape, was ${edge0.shape} and ${edge1.shape}"
    }
    require(isFloatValue(value.shape) && acceptsBound(value.shape, edge0.shape)) {
        "smoothstep edges must match ${value.shape} or be a scalar of the same precision, was ${edge0.shape}"
    }
    return call(value.shape, "smoothstep", listOf(edge0, edge1, value))
}

public fun <T : ShType> mix(left: Expr<T>, right: Expr<T>, amount: Expr<*>): Expr<T> {
    require(left.shape == right.shape) { "mix endpoints must share a shape, was ${left.shape} and ${right.shape}" }
    require(isFloatValue(left.shape) && acceptsBound(left.shape, amount.shape)) {
        "mix amount must match ${left.shape} or be a scalar of the same precision, was ${amount.shape}"
    }
    return call(left.shape, "mix", listOf(left, right, amount))
}

@JvmName("lengthVec2")
public fun <P : Prec> length(value: Expr<Vec2<Flt<P>>>): Expr<Flt<P>> = lengthOf(value)

@JvmName("lengthVec3")
public fun <P : Prec> length(value: Expr<Vec3<Flt<P>>>): Expr<Flt<P>> = lengthOf(value)

@JvmName("lengthVec4")
public fun <P : Prec> length(value: Expr<Vec4<Flt<P>>>): Expr<Flt<P>> = lengthOf(value)

@JvmName("dotVec2")
public fun <P : Prec> dot(left: Expr<Vec2<Flt<P>>>, right: Expr<Vec2<Flt<P>>>): Expr<Flt<P>> = dotOf(left, right)

@JvmName("dotVec3")
public fun <P : Prec> dot(left: Expr<Vec3<Flt<P>>>, right: Expr<Vec3<Flt<P>>>): Expr<Flt<P>> = dotOf(left, right)

@JvmName("dotVec4")
public fun <P : Prec> dot(left: Expr<Vec4<Flt<P>>>, right: Expr<Vec4<Flt<P>>>): Expr<Flt<P>> = dotOf(left, right)

public fun <T : ShType> normalize(value: Expr<T>): Expr<T> = unaryFloatValue("normalize", value)

/**
 * Shade. Lambert weight of a surface: the clamped cosine between [normal] and [light].
 * Both vectors are normalized inside the call. The expression is legal in an AGSL effect and in a GLES scene.
 */
public fun lambert(
    normal: Expr<Vec3<Flt<High>>>,
    light: Expr<Vec3<Flt<High>>>,
): Expr<Flt<High>> = max(dot(normalize(normal), normalize(light)), float(0f))

@JvmName("distanceVec2")
public fun <P : Prec> distance(left: Expr<Vec2<Flt<P>>>, right: Expr<Vec2<Flt<P>>>): Expr<Flt<P>> =
    distanceOf(left, right)

@JvmName("distanceVec3")
public fun <P : Prec> distance(left: Expr<Vec3<Flt<P>>>, right: Expr<Vec3<Flt<P>>>): Expr<Flt<P>> =
    distanceOf(left, right)

@JvmName("crossVec3")
public fun <P : Prec> cross(left: Expr<Vec3<Flt<P>>>, right: Expr<Vec3<Flt<P>>>): Expr<Vec3<Flt<P>>> =
    crossOf(left, right)

private fun <T : ShType> derivative(function: String, value: Expr<T>): Expr<T> {
    requireAuthoring(AuthoringAction.Derivative)
    return unaryFloat(function, value)
}

private fun <T : ShType> unaryFloat(function: String, value: Expr<T>): Expr<T> {
    require(isFloatValue(value.shape)) { "$function requires a float value, was ${value.shape}" }
    return call(value.shape, function, listOf(value))
}

private fun <T : ShType> unaryFloatValue(function: String, value: Expr<T>): Expr<T> {
    require(isFloatValue(value.shape) || isFloatVector(value.shape)) {
        "$function requires a float scalar or vector, was ${value.shape}"
    }
    return call(value.shape, function, listOf(value))
}

private fun <T : ShType> transposeOf(matrix: Expr<*>): Expr<T> = matrixUnary("transpose", matrix)

private fun <T : ShType> matrixUnary(function: String, matrix: Expr<*>): Expr<T> {
    require(matrix.shape is Shape.Matrix) { "$function requires a matrix, was ${matrix.shape}" }
    return call(matrix.shape, function, listOf(matrix))
}

private fun <T : ShType> matrixCompMultOf(left: Expr<*>, right: Expr<*>): Expr<T> {
    require(left.shape is Shape.Matrix && left.shape == right.shape) {
        "matrixCompMult requires matrices of one size, was ${left.shape} and ${right.shape}"
    }
    return call(left.shape, "matrixCompMult", listOf(left, right))
}

private fun <P : Prec> distanceOf(left: Expr<*>, right: Expr<*>): Expr<Flt<P>> {
    require(isFloatVector(left.shape) && left.shape == right.shape) {
        "distance requires float vectors of one precision and length, was ${left.shape} and ${right.shape}"
    }
    return call(floatScalar(left.shape), "distance", listOf(left, right))
}

private fun <P : Prec> crossOf(left: Expr<Vec3<Flt<P>>>, right: Expr<Vec3<Flt<P>>>): Expr<Vec3<Flt<P>>> =
    call(left.shape, "cross", listOf(left, right))

private fun <T : ShType> sameShape(function: String, left: Expr<T>, right: Expr<T>): Expr<T> {
    require(isFloatValue(left.shape) && left.shape == right.shape) {
        "$function requires float values of one shape, was ${left.shape} and ${right.shape}"
    }
    return call(left.shape, function, listOf(left, right))
}

private fun <T : ShType> broadcast(
    function: String,
    value: Expr<T>,
    bound: Expr<*>,
    args: List<Expr<*>>,
): Expr<T> {
    require(isFloatValue(value.shape) && acceptsBound(value.shape, bound.shape)) {
        "$function requires ${value.shape} and a matching float or a scalar of the same precision, was ${bound.shape}"
    }
    return call(value.shape, function, args)
}

private fun <P : Prec> lengthOf(value: Expr<*>): Expr<Flt<P>> {
    require(isFloatVector(value.shape)) { "length requires a float vector, was ${value.shape}" }
    return call(floatScalar(value.shape), "length", listOf(value))
}

private fun <P : Prec> dotOf(left: Expr<*>, right: Expr<*>): Expr<Flt<P>> {
    require(isFloatVector(left.shape) && left.shape == right.shape) {
        "dot requires two float vectors of one shape, was ${left.shape} and ${right.shape}"
    }
    return call(floatScalar(left.shape), "dot", listOf(left, right))
}

private fun acceptsBound(value: Shape, bound: Shape): Boolean {
    if (!isFloatValue(value) || !isFloatValue(bound) || precisionOf(value) != precisionOf(bound)) return false
    return bound == value || isFloatScalar(bound)
}

private fun floatScalar(shape: Shape): Shape =
    Shape.Scalar(ScalarKind.Float, precisionOf(shape))

private fun literal(value: Float, precision: Precision): Expr<*> = when (precision) {
    Precision.High -> value.lit
    Precision.Med -> value.med
}

private fun <T : ShType> call(shape: Shape, function: String, args: List<Expr<*>>): Expr<T> =
    Expr(shape, ExprNode.Call(function, args))
