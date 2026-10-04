package ru.redbyte.redbytefx

import kotlin.jvm.JvmName

/**
 * Literal constructors and authoring sugar that mirror GLSL spelling.
 *
 * - [float], [float2], [float3], [float4] turn Kotlin constants or [Expr] values into shader literals
 *   and vectors (same names as in GLSL, not generic helpers).
 * - [color] builds mediump RGBA ([MedVec4]) for typical fragment output.
 * - [gte] / [gt] / … in this file are infix comparisons; names match shader operators (`>=`, `>`).
 * - [Float.lit], [Float.med], and [Int.lit] (in [Arith.kt]) promote Kotlin numbers to [Expr].
 *
 * See also [Builtin] for portable math (`sin`, `mix`, …) and [SwizzleAccess] for `.xy` / `.rgba`.
 */

/** Kotlin [Float] → highp shader literal (`float` in GLSL). */
public fun float(value: Float): HighFloat = value.lit

/** Builds a `float2` / `vec2` from two expressions of the same precision. */
public fun <P : Prec> float2(x: Expr<Flt<P>>, y: Expr<Flt<P>>): Expr<Vec2<Flt<P>>> = vec2(x, y)

public fun float2(x: Float, y: Float): HighVec2 = vec2(x.lit, y.lit)

public fun <P : Prec> float2(x: Expr<Flt<P>>, y: Float): Expr<Vec2<Flt<P>>> = vec2(x, y.litSame(x))

public fun <P : Prec> float2(x: Float, y: Expr<Flt<P>>): Expr<Vec2<Flt<P>>> = vec2(x.litSame(y), y)

public fun <P : Prec> float3(
    x: Expr<Flt<P>>,
    y: Expr<Flt<P>>,
    z: Expr<Flt<P>>,
): Expr<Vec3<Flt<P>>> = vec3(x, y, z)

public fun float3(x: Float, y: Float, z: Float): HighVec3 = vec3(x.lit, y.lit, z.lit)

public fun <P : Prec> float4(
    x: Expr<Flt<P>>,
    y: Expr<Flt<P>>,
    z: Expr<Flt<P>>,
    w: Expr<Flt<P>>,
): Expr<Vec4<Flt<P>>> = vec4(x, y, z, w)

public fun float4(x: Float, y: Float, z: Float, w: Float): HighVec4 =
    vec4(x.lit, y.lit, z.lit, w.lit)

@JvmName("colorHigh")
public fun color(
    r: Expr<Flt<High>>,
    g: Expr<Flt<High>>,
    b: Expr<Flt<High>>,
    a: Expr<Flt<High>>,
): MedVec4 = vec4(r.toMed(), g.toMed(), b.toMed(), a.toMed())

@JvmName("colorMed")
public fun color(
    r: Expr<Flt<Med>>,
    g: Expr<Flt<Med>>,
    b: Expr<Flt<Med>>,
    a: Expr<Flt<Med>>,
): MedVec4 = vec4(r, g, b, a)

@JvmName("colorVec3HighAlphaHigh")
public fun color(rgb: Expr<Vec3<Flt<High>>>, a: Expr<Flt<High>>): MedVec4 =
    color(rgb.x, rgb.y, rgb.z, a)

@JvmName("colorVec3HighAlphaMed")
public fun color(rgb: Expr<Vec3<Flt<High>>>, a: Expr<Flt<Med>>): MedVec4 =
    vec4(rgb.x.toMed(), rgb.y.toMed(), rgb.z.toMed(), a)

@JvmName("colorVec3Med")
public fun color(rgb: Expr<Vec3<Flt<Med>>>, a: Expr<Flt<Med>>): MedVec4 =
    vec4(rgb.x, rgb.y, rgb.z, a)

@JvmName("colorVec3HighAlphaFloat")
public fun color(rgb: Expr<Vec3<Flt<High>>>, a: Float): MedVec4 = color(rgb, a.lit)

@JvmName("colorFromHighVec4")
public fun color(value: Expr<Vec4<Flt<High>>>): MedVec4 = value.toMed()

@JvmName("colorFromMedVec4")
public fun color(value: Expr<Vec4<Flt<Med>>>): MedVec4 = value

@JvmName("colorHighChannelsMedAlpha")
public fun color(
    r: Expr<Flt<High>>,
    g: Expr<Flt<High>>,
    b: Expr<Flt<High>>,
    a: Expr<Flt<Med>>,
): MedVec4 = vec4(r.toMed(), g.toMed(), b.toMed(), a)

@JvmName("timesMedVec4High")
public operator fun Expr<Vec4<Flt<Med>>>.times(rhs: Expr<Flt<High>>): Expr<Vec4<Flt<Med>>> = this * rhs.toMed()

public fun <P : Prec> float3(xy: Expr<Vec2<Flt<P>>>, z: Expr<Flt<P>>): Expr<Vec3<Flt<P>>> =
    vec3(xy.x, xy.y, z)

/** Infix `>=` for float expressions (Kotlin cannot use `>=` as an infix name here). */
public infix fun <P : Prec> Expr<Flt<P>>.gte(rhs: Float): Expr<BoolS> = this ge rhs

/** Infix `>=` for float expressions (alias of [ge] from [Compare.kt]). */
public infix fun <P : Prec> Expr<Flt<P>>.gte(other: Expr<Flt<P>>): Expr<BoolS> = this ge other

@JvmName("gteInt")
public infix fun Expr<IntS>.gte(other: Expr<IntS>): Expr<BoolS> = this ge other

public fun grayscale(color: Expr<Vec4<Flt<Med>>>): Expr<Vec4<Flt<Med>>> {
    val luma = luminance(color).toMed()
    return color(luma, luma, luma, color.a)
}

public fun <P : Prec> mix(start: Float, end: Expr<Flt<P>>, amount: Expr<Flt<P>>): Expr<Flt<P>> =
    mix(start.litSame(end), end, amount)

public fun <P : Prec> mix(start: Float, end: Float, amount: Expr<Flt<P>>): Expr<Flt<P>> =
    mix(start.litSame(amount), end.litSame(amount), amount)

@JvmName("mixMedScalarHighAmount")
public fun mix(start: Expr<Flt<Med>>, end: Expr<Flt<Med>>, amount: Expr<Flt<High>>): Expr<Flt<Med>> =
    mix(start, end, amount.toMed())

@JvmName("mixFloatMedHighAmount")
public fun mix(start: Float, end: Expr<Flt<Med>>, amount: Expr<Flt<High>>): Expr<Flt<Med>> =
    mix(start.med, end, amount.toMed())

@JvmName("float4Vec3Same")
public fun <P : Prec> float4(xyz: Expr<Vec3<Flt<P>>>, w: Expr<Flt<P>>): Expr<Vec4<Flt<P>>> =
    vec4(xyz.x, xyz.y, xyz.z, w)

@JvmName("float4Vec3HighMed")
public fun float4(xyz: Expr<Vec3<Flt<High>>>, w: Expr<Flt<Med>>): Expr<Vec4<Flt<Med>>> =
    vec4(xyz.x.toMed(), xyz.y.toMed(), xyz.z.toMed(), w)

public fun luminance(color: Expr<Vec4<Flt<Med>>>): Expr<Flt<High>> =
    color.r.toHigh() * 0.2126f + color.g.toHigh() * 0.7152f + color.b.toHigh() * 0.0722f

@JvmName("toMedScalar")
public fun Expr<Flt<High>>.toMed(): Expr<Flt<Med>> = precisionCast(this, Precision.Med)

public fun Expr<Flt<Med>>.toHigh(): Expr<Flt<High>> = precisionCast(this, Precision.High)

@JvmName("toMedVec3")
public fun Expr<Vec3<Flt<High>>>.toMed(): Expr<Vec3<Flt<Med>>> = precisionCast(this, Precision.Med)

@JvmName("toMedVec4")
public fun Expr<Vec4<Flt<High>>>.toMed(): Expr<Vec4<Flt<Med>>> = precisionCast(this, Precision.Med)

@JvmName("timesMedScalarHigh")
public operator fun Expr<Flt<Med>>.times(rhs: Expr<Flt<High>>): Expr<Flt<Med>> = this * rhs.toMed()

@JvmName("plusMedScalarHigh")
public operator fun Expr<Flt<Med>>.plus(rhs: Expr<Flt<High>>): Expr<Flt<Med>> = this + rhs.toMed()

@JvmName("timesMedVec3HighScalar")
public operator fun Expr<Vec3<Flt<Med>>>.times(rhs: Expr<Flt<High>>): Expr<Vec3<Flt<Med>>> = this * rhs.toMed()

@JvmName("timesMedVec3HighVec3")
public operator fun Expr<Vec3<Flt<Med>>>.times(rhs: Expr<Vec3<Flt<High>>>): Expr<Vec3<Flt<Med>>> = this * rhs.toMed()

@JvmName("plusMedVec3HighVec3")
public operator fun Expr<Vec3<Flt<Med>>>.plus(rhs: Expr<Vec3<Flt<High>>>): Expr<Vec3<Flt<Med>>> = this + rhs.toMed()

public fun <P : Prec> max(left: Expr<Flt<P>>, right: Float): Expr<Flt<P>> = max(left, right.litLike(left))

public fun <P : Prec> max(left: Float, right: Expr<Flt<P>>): Expr<Flt<P>> = max(left.litSame(right), right)

public fun <P : Prec> min(left: Expr<Flt<P>>, right: Float): Expr<Flt<P>> = min(left, right.litLike(left))

public fun <P : Prec> min(left: Float, right: Expr<Flt<P>>): Expr<Flt<P>> = min(left.litSame(right), right)

public fun <T : ShType> clamp(value: Expr<T>, low: Float, high: Float): Expr<T> =
    clamp(value, low.litLike(value), high.litLike(value))

public fun <T : ShType> step(edge: Float, value: Expr<T>): Expr<T> = step(edge.litLike(value), value)

public fun <T : ShType> smoothstep(edge0: Float, edge1: Float, value: Expr<T>): Expr<T> =
    smoothstep(edge0.litLike(value), edge1.litLike(value), value)

public fun <T : ShType> smoothstep(edge0: Float, edge1: Expr<*>, value: Expr<T>): Expr<T> =
    smoothstep(edge0.litLike(value), edge1, value)

public fun <T : ShType> mix(left: Expr<T>, right: Expr<T>, amount: Float): Expr<T> =
    mix(left, right, amount.litLike(left))

public fun <P : Prec> pow(base: Expr<Flt<P>>, exponent: Float): Expr<Flt<P>> = pow(base, exponent.litSame(base))

public infix fun <P : Prec> Expr<Flt<P>>.gt(rhs: Float): Expr<BoolS> = this gt rhs.litSame(this)

public infix fun <P : Prec> Expr<Flt<P>>.lt(rhs: Float): Expr<BoolS> = this lt rhs.litSame(this)

public infix fun <P : Prec> Expr<Flt<P>>.ge(rhs: Float): Expr<BoolS> = this ge rhs.litSame(this)

public infix fun <P : Prec> Expr<Flt<P>>.le(rhs: Float): Expr<BoolS> = this le rhs.litSame(this)

public infix fun <P : Prec> Expr<Flt<P>>.eq(rhs: Float): Expr<BoolS> = this eq rhs.litSame(this)

public infix fun <P : Prec> Expr<Flt<P>>.ne(rhs: Float): Expr<BoolS> = this ne rhs.litSame(this)

@JvmName("mixMedVec4HighAmount")
public fun mix(
    left: Expr<Vec4<Flt<Med>>>,
    right: Expr<Vec4<Flt<Med>>>,
    amount: Expr<Flt<High>>,
): Expr<Vec4<Flt<Med>>> = mix(left, right, amount.toMed())

@JvmName("mixMedVec3HighAmount")
public fun mix(
    left: Expr<Vec3<Flt<Med>>>,
    right: Expr<Vec3<Flt<Med>>>,
    amount: Expr<Flt<High>>,
): Expr<Vec3<Flt<Med>>> = mix(left, right, amount.toMed())

@JvmName("plusScalarFloat")
public operator fun <P : Prec> Expr<Flt<P>>.plus(rhs: Float): Expr<Flt<P>> = arithFloat(ArithOp.Add, rhs)

@JvmName("minusScalarFloat")
public operator fun <P : Prec> Expr<Flt<P>>.minus(rhs: Float): Expr<Flt<P>> = arithFloat(ArithOp.Sub, rhs)

@JvmName("timesScalarFloat")
public operator fun <P : Prec> Expr<Flt<P>>.times(rhs: Float): Expr<Flt<P>> = arithFloat(ArithOp.Mul, rhs)

@JvmName("divScalarFloat")
public operator fun <P : Prec> Expr<Flt<P>>.div(rhs: Float): Expr<Flt<P>> = arithFloat(ArithOp.Div, rhs)

@JvmName("plusFloatScalar")
public operator fun <P : Prec> Float.plus(rhs: Expr<Flt<P>>): Expr<Flt<P>> = rhs.arithFloatLeft(ArithOp.Add, this)

@JvmName("minusFloatScalar")
public operator fun <P : Prec> Float.minus(rhs: Expr<Flt<P>>): Expr<Flt<P>> = rhs.arithFloatLeft(ArithOp.Sub, this)

@JvmName("timesFloatScalar")
public operator fun <P : Prec> Float.times(rhs: Expr<Flt<P>>): Expr<Flt<P>> = rhs.arithFloatLeft(ArithOp.Mul, this)

@JvmName("divFloatScalar")
public operator fun <P : Prec> Float.div(rhs: Expr<Flt<P>>): Expr<Flt<P>> = rhs.arithFloatLeft(ArithOp.Div, this)

@JvmName("plusVec2Float")
public operator fun <P : Prec> Expr<Vec2<Flt<P>>>.plus(rhs: Float): Expr<Vec2<Flt<P>>> = arithFloat(ArithOp.Add, rhs)

@JvmName("minusVec2Float")
public operator fun <P : Prec> Expr<Vec2<Flt<P>>>.minus(rhs: Float): Expr<Vec2<Flt<P>>> = arithFloat(ArithOp.Sub, rhs)

@JvmName("timesVec2Float")
public operator fun <P : Prec> Expr<Vec2<Flt<P>>>.times(rhs: Float): Expr<Vec2<Flt<P>>> = arithFloat(ArithOp.Mul, rhs)

@JvmName("divVec2Float")
public operator fun <P : Prec> Expr<Vec2<Flt<P>>>.div(rhs: Float): Expr<Vec2<Flt<P>>> = arithFloat(ArithOp.Div, rhs)

@JvmName("plusFloatVec2")
public operator fun <P : Prec> Float.plus(rhs: Expr<Vec2<Flt<P>>>): Expr<Vec2<Flt<P>>> = rhs.arithFloatLeft(ArithOp.Add, this)

@JvmName("minusFloatVec2")
public operator fun <P : Prec> Float.minus(rhs: Expr<Vec2<Flt<P>>>): Expr<Vec2<Flt<P>>> = rhs.arithFloatLeft(ArithOp.Sub, this)

@JvmName("timesFloatVec2")
public operator fun <P : Prec> Float.times(rhs: Expr<Vec2<Flt<P>>>): Expr<Vec2<Flt<P>>> = rhs.arithFloatLeft(ArithOp.Mul, this)

@JvmName("plusVec3Float")
public operator fun <P : Prec> Expr<Vec3<Flt<P>>>.plus(rhs: Float): Expr<Vec3<Flt<P>>> = arithFloat(ArithOp.Add, rhs)

@JvmName("minusVec3Float")
public operator fun <P : Prec> Expr<Vec3<Flt<P>>>.minus(rhs: Float): Expr<Vec3<Flt<P>>> = arithFloat(ArithOp.Sub, rhs)

@JvmName("timesVec3Float")
public operator fun <P : Prec> Expr<Vec3<Flt<P>>>.times(rhs: Float): Expr<Vec3<Flt<P>>> = arithFloat(ArithOp.Mul, rhs)

@JvmName("divVec3Float")
public operator fun <P : Prec> Expr<Vec3<Flt<P>>>.div(rhs: Float): Expr<Vec3<Flt<P>>> = arithFloat(ArithOp.Div, rhs)

@JvmName("plusVec4Float")
public operator fun <P : Prec> Expr<Vec4<Flt<P>>>.plus(rhs: Float): Expr<Vec4<Flt<P>>> = arithFloat(ArithOp.Add, rhs)

@JvmName("minusVec4Float")
public operator fun <P : Prec> Expr<Vec4<Flt<P>>>.minus(rhs: Float): Expr<Vec4<Flt<P>>> = arithFloat(ArithOp.Sub, rhs)

@JvmName("timesVec4Float")
public operator fun <P : Prec> Expr<Vec4<Flt<P>>>.times(rhs: Float): Expr<Vec4<Flt<P>>> = arithFloat(ArithOp.Mul, rhs)

@JvmName("divVec4Float")
public operator fun <P : Prec> Expr<Vec4<Flt<P>>>.div(rhs: Float): Expr<Vec4<Flt<P>>> = arithFloat(ArithOp.Div, rhs)

private fun <T : ShType> Expr<T>.arithFloat(op: ArithOp, rhs: Float): Expr<T> {
    val other = rhs.litLike(this)
    return Expr(arithShape(op, shape, other.shape), ExprNode.Binary(op, this, other))
}

private fun <T : ShType> Expr<T>.arithFloatLeft(op: ArithOp, lhs: Float): Expr<T> {
    val other = lhs.litLike(this)
    return Expr(arithShape(op, other.shape, shape), ExprNode.Binary(op, other, this))
}

private fun Float.litLike(peer: Expr<*>): Expr<*> = when (precisionOf(peer.shape)) {
    Precision.High -> this.lit
    Precision.Med -> this.med
    null -> error("Float literal has no peer precision")
}

@Suppress("UNCHECKED_CAST")
private fun <P : Prec> Float.litSame(peer: Expr<Flt<P>>): Expr<Flt<P>> = litLike(peer) as Expr<Flt<P>>

private fun <T : ShType> precisionCast(value: Expr<*>, precision: Precision): Expr<T> {
    val target = when (val shape = value.shape) {
        is Shape.Scalar -> Shape.Scalar(shape.kind, precision)
        is Shape.Vector -> Shape.Vector(shape.kind, precision, shape.lanes)
        else -> error("Only float scalars and vectors can change precision")
    }
    return Expr(target, ExprNode.Cast(value))
}
