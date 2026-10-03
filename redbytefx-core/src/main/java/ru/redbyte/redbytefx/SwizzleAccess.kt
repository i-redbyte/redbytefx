package ru.redbyte.redbytefx

import kotlin.jvm.JvmName

/**
 * Vector **swizzle** accessors for [Expr] values.
 *
 * Names are single letters on purpose: they mirror GLSL/AGSL swizzle masks (`color.rgba`, `uv.xy`, …)
 * and compile to the same `.mask` syntax in generated shader source. They are not abbreviated Kotlin
 * identifiers — do not read `.a` as “a generic parameter”; on `vec4` it means the **alpha** channel
 * in the `rgba` set (documented on the `.a` property below).
 *
 * Two letter sets cannot be mixed in one mask (`xg` is rejected at compile time). Use either
 * `.xyzw` or `.rgba` style for a given chain.
 */
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

/** First component (`x` / `r` mask) of a `float2`. */
@get:JvmName("xVec2")
public val <P : Prec> Expr<Vec2<Flt<P>>>.x: Expr<Flt<P>> get() = swizzleAs(this, "x")

/** Second component (`y` mask) of a `float2`. */
@get:JvmName("yVec2")
public val <P : Prec> Expr<Vec2<Flt<P>>>.y: Expr<Flt<P>> get() = swizzleAs(this, "y")

/** `xy` swizzle as `float2`. */
@get:JvmName("xyVec2")
public val <P : Prec> Expr<Vec2<Flt<P>>>.xy: Expr<Vec2<Flt<P>>> get() = swizzleAs(this, "xy")

/** First lane of an `int3` vector. */
@get:JvmName("xIntVec3")
public val Expr<Vec3<IntS>>.x: Expr<IntS> get() = swizzleAs(this, "x")

/** Second lane of an `int3` vector. */
@get:JvmName("yIntVec3")
public val Expr<Vec3<IntS>>.y: Expr<IntS> get() = swizzleAs(this, "y")

/** Third lane of an `int3` vector. */
@get:JvmName("zIntVec3")
public val Expr<Vec3<IntS>>.z: Expr<IntS> get() = swizzleAs(this, "z")

/** First component (`x` mask) of a `float3`. */
@get:JvmName("xVec3")
public val <P : Prec> Expr<Vec3<Flt<P>>>.x: Expr<Flt<P>> get() = swizzleAs(this, "x")

/** Second component (`y` mask) of a `float3`. */
@get:JvmName("yVec3")
public val <P : Prec> Expr<Vec3<Flt<P>>>.y: Expr<Flt<P>> get() = swizzleAs(this, "y")

/** Third component (`z` mask) of a `float3`. */
@get:JvmName("zVec3")
public val <P : Prec> Expr<Vec3<Flt<P>>>.z: Expr<Flt<P>> get() = swizzleAs(this, "z")

/** `xy` swizzle as `float2`. */
@get:JvmName("xyVec3")
public val <P : Prec> Expr<Vec3<Flt<P>>>.xy: Expr<Vec2<Flt<P>>> get() = swizzleAs(this, "xy")

/** `xz` swizzle as `float2`. */
@get:JvmName("xzVec3")
public val <P : Prec> Expr<Vec3<Flt<P>>>.xz: Expr<Vec2<Flt<P>>> get() = swizzleAs(this, "xz")

/** Full `xyz` swizzle as `float3`. */
@get:JvmName("xyzVec3")
public val <P : Prec> Expr<Vec3<Flt<P>>>.xyz: Expr<Vec3<Flt<P>>> get() = swizzleAs(this, "xyz")

/** `rgb` alias for `xyz` on `float3` (color vectors). */
@get:JvmName("rgbVec3")
public val <P : Prec> Expr<Vec3<Flt<P>>>.rgb: Expr<Vec3<Flt<P>>> get() = swizzleAs(this, "rgb")

/** First component (`x` mask) of a `float4`. */
@get:JvmName("xVec4")
public val <P : Prec> Expr<Vec4<Flt<P>>>.x: Expr<Flt<P>> get() = swizzleAs(this, "x")

/** Second component (`y` mask) of a `float4`. */
@get:JvmName("yVec4")
public val <P : Prec> Expr<Vec4<Flt<P>>>.y: Expr<Flt<P>> get() = swizzleAs(this, "y")

/** Third component (`z` mask) of a `float4`. */
@get:JvmName("zVec4")
public val <P : Prec> Expr<Vec4<Flt<P>>>.z: Expr<Flt<P>> get() = swizzleAs(this, "z")

/** Fourth component (`w` mask) of a `float4`. */
@get:JvmName("wVec4")
public val <P : Prec> Expr<Vec4<Flt<P>>>.w: Expr<Flt<P>> get() = swizzleAs(this, "w")

/** `xy` swizzle as `float2`. */
@get:JvmName("xyVec4")
public val <P : Prec> Expr<Vec4<Flt<P>>>.xy: Expr<Vec2<Flt<P>>> get() = swizzleAs(this, "xy")

/** `xz` swizzle as `float2`. */
@get:JvmName("xzVec4")
public val <P : Prec> Expr<Vec4<Flt<P>>>.xz: Expr<Vec2<Flt<P>>> get() = swizzleAs(this, "xz")

/** `xyz` swizzle as `float3`. */
@get:JvmName("xyzVec4")
public val <P : Prec> Expr<Vec4<Flt<P>>>.xyz: Expr<Vec3<Flt<P>>> get() = swizzleAs(this, "xyz")

/** Full `xyzw` swizzle as `float4`. */
@get:JvmName("xyzwVec4")
public val <P : Prec> Expr<Vec4<Flt<P>>>.xyzw: Expr<Vec4<Flt<P>>> get() = swizzleAs(this, "xyzw")

/** Red channel (`r` in `rgba`; same lane as `.x`). */
@get:JvmName("rVec4")
public val <P : Prec> Expr<Vec4<Flt<P>>>.r: Expr<Flt<P>> get() = swizzleAs(this, "r")

/** Green channel (`g` in `rgba`; same lane as `.y`). */
@get:JvmName("gVec4")
public val <P : Prec> Expr<Vec4<Flt<P>>>.g: Expr<Flt<P>> get() = swizzleAs(this, "g")

/** Blue channel (`b` in `rgba`; same lane as `.z`). */
@get:JvmName("bVec4")
public val <P : Prec> Expr<Vec4<Flt<P>>>.b: Expr<Flt<P>> get() = swizzleAs(this, "b")

/**
 * Alpha channel (`a` in `rgba`; same lane as `.w`).
 *
 * The Kotlin name is `a` because shaders spell this component `.a` in source, not because the API
 * uses meaningless one-letter symbols elsewhere.
 */
@get:JvmName("aVec4")
public val <P : Prec> Expr<Vec4<Flt<P>>>.a: Expr<Flt<P>> get() = swizzleAs(this, "a")

/** `rg` swizzle as `float2`. */
@get:JvmName("rgVec4")
public val <P : Prec> Expr<Vec4<Flt<P>>>.rg: Expr<Vec2<Flt<P>>> get() = swizzleAs(this, "rg")

/** `rgb` swizzle as `float3`. */
@get:JvmName("rgbVec4")
public val <P : Prec> Expr<Vec4<Flt<P>>>.rgb: Expr<Vec3<Flt<P>>> get() = swizzleAs(this, "rgb")

/** `rgba` swizzle as `float4`. */
@get:JvmName("rgbaVec4")
public val <P : Prec> Expr<Vec4<Flt<P>>>.rgba: Expr<Vec4<Flt<P>>> get() = swizzleAs(this, "rgba")
