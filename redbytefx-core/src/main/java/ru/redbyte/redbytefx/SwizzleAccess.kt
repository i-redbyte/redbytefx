package ru.redbyte.redbytefx

import kotlin.jvm.JvmName

internal fun swizzleShape(source: Shape, components: List<Int>): Shape {
    require(source is Shape.Vector) { "Swizzle requires a vector, was $source" }
    require(components.isNotEmpty() && components.size <= 4) { "Swizzle width must be 1..4" }
    for (component in components) {
        require(component in 0 until source.lanes) {
            "Component $component is outside ${source.lanes} lanes"
        }
    }
    return if (components.size == 1) {
        Shape.Scalar(source.kind, source.precision)
    } else {
        Shape.Vector(source.kind, source.precision, components.size)
    }
}

internal fun swizzle(source: Expr<*>, mask: String): Expr<*> {
    val outcome = readSwizzle(mask)
    require(outcome is SwizzleOutcome.Accepted) {
        "Swizzle \"$mask\" was rejected: ${(outcome as SwizzleOutcome.Rejected).code}"
    }
    return Expr<ShType>(
        swizzleShape(source.shape, outcome.components),
        ExprNode.Swizzle(source, mask),
    )
}

@Suppress("UNCHECKED_CAST")
private fun <T : ShType> swizzleAs(source: Expr<*>, mask: String): Expr<T> =
    swizzle(source, mask) as Expr<T>

@get:JvmName("xVec2")
public val <P : Prec> Expr<Vec2<Flt<P>>>.x: Expr<Flt<P>> get() = swizzleAs(this, "x")

@get:JvmName("yVec2")
public val <P : Prec> Expr<Vec2<Flt<P>>>.y: Expr<Flt<P>> get() = swizzleAs(this, "y")

@get:JvmName("xyVec2")
public val <P : Prec> Expr<Vec2<Flt<P>>>.xy: Expr<Vec2<Flt<P>>> get() = swizzleAs(this, "xy")

@get:JvmName("xIntVec3")
public val Expr<Vec3<IntS>>.x: Expr<IntS> get() = swizzleAs(this, "x")

@get:JvmName("yIntVec3")
public val Expr<Vec3<IntS>>.y: Expr<IntS> get() = swizzleAs(this, "y")

@get:JvmName("zIntVec3")
public val Expr<Vec3<IntS>>.z: Expr<IntS> get() = swizzleAs(this, "z")

@get:JvmName("xVec3")
public val <P : Prec> Expr<Vec3<Flt<P>>>.x: Expr<Flt<P>> get() = swizzleAs(this, "x")

@get:JvmName("yVec3")
public val <P : Prec> Expr<Vec3<Flt<P>>>.y: Expr<Flt<P>> get() = swizzleAs(this, "y")

@get:JvmName("zVec3")
public val <P : Prec> Expr<Vec3<Flt<P>>>.z: Expr<Flt<P>> get() = swizzleAs(this, "z")

@get:JvmName("xyVec3")
public val <P : Prec> Expr<Vec3<Flt<P>>>.xy: Expr<Vec2<Flt<P>>> get() = swizzleAs(this, "xy")

@get:JvmName("xzVec3")
public val <P : Prec> Expr<Vec3<Flt<P>>>.xz: Expr<Vec2<Flt<P>>> get() = swizzleAs(this, "xz")

@get:JvmName("xyzVec3")
public val <P : Prec> Expr<Vec3<Flt<P>>>.xyz: Expr<Vec3<Flt<P>>> get() = swizzleAs(this, "xyz")

@get:JvmName("rgbVec3")
public val <P : Prec> Expr<Vec3<Flt<P>>>.rgb: Expr<Vec3<Flt<P>>> get() = swizzleAs(this, "rgb")

@get:JvmName("xVec4")
public val <P : Prec> Expr<Vec4<Flt<P>>>.x: Expr<Flt<P>> get() = swizzleAs(this, "x")

@get:JvmName("yVec4")
public val <P : Prec> Expr<Vec4<Flt<P>>>.y: Expr<Flt<P>> get() = swizzleAs(this, "y")

@get:JvmName("zVec4")
public val <P : Prec> Expr<Vec4<Flt<P>>>.z: Expr<Flt<P>> get() = swizzleAs(this, "z")

@get:JvmName("wVec4")
public val <P : Prec> Expr<Vec4<Flt<P>>>.w: Expr<Flt<P>> get() = swizzleAs(this, "w")

@get:JvmName("xyVec4")
public val <P : Prec> Expr<Vec4<Flt<P>>>.xy: Expr<Vec2<Flt<P>>> get() = swizzleAs(this, "xy")

@get:JvmName("xzVec4")
public val <P : Prec> Expr<Vec4<Flt<P>>>.xz: Expr<Vec2<Flt<P>>> get() = swizzleAs(this, "xz")

@get:JvmName("xyzVec4")
public val <P : Prec> Expr<Vec4<Flt<P>>>.xyz: Expr<Vec3<Flt<P>>> get() = swizzleAs(this, "xyz")

@get:JvmName("xyzwVec4")
public val <P : Prec> Expr<Vec4<Flt<P>>>.xyzw: Expr<Vec4<Flt<P>>> get() = swizzleAs(this, "xyzw")

@get:JvmName("rVec4")
public val <P : Prec> Expr<Vec4<Flt<P>>>.r: Expr<Flt<P>> get() = swizzleAs(this, "r")

@get:JvmName("gVec4")
public val <P : Prec> Expr<Vec4<Flt<P>>>.g: Expr<Flt<P>> get() = swizzleAs(this, "g")

@get:JvmName("bVec4")
public val <P : Prec> Expr<Vec4<Flt<P>>>.b: Expr<Flt<P>> get() = swizzleAs(this, "b")

@get:JvmName("aVec4")
public val <P : Prec> Expr<Vec4<Flt<P>>>.a: Expr<Flt<P>> get() = swizzleAs(this, "a")

@get:JvmName("rgVec4")
public val <P : Prec> Expr<Vec4<Flt<P>>>.rg: Expr<Vec2<Flt<P>>> get() = swizzleAs(this, "rg")

@get:JvmName("rgbVec4")
public val <P : Prec> Expr<Vec4<Flt<P>>>.rgb: Expr<Vec3<Flt<P>>> get() = swizzleAs(this, "rgb")

@get:JvmName("rgbaVec4")
public val <P : Prec> Expr<Vec4<Flt<P>>>.rgba: Expr<Vec4<Flt<P>>> get() = swizzleAs(this, "rgba")
