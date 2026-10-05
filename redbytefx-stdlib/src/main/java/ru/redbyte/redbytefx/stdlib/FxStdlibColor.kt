package ru.redbyte.redbytefx.stdlib

import ru.redbyte.redbytefx.*
import kotlin.jvm.JvmName

private fun blendAmount(amount: Expr<Flt<High>>): Expr<Flt<High>> = saturate(amount)

/** Associates straight RGB with alpha for AGSL fragment output and premultiplied compositing. */
public fun premultiply(color: Expr<Vec4<Flt<Med>>>): Expr<Vec4<Flt<Med>>> = ru.redbyte.redbytefx.color(
    r = color.r * color.a,
    g = color.g * color.a,
    b = color.b * color.a,
    a = color.a,
)

/**
 * Recovers straight RGB from a premultiplied color. Pixels with alpha at or below `0.000001`
 * return black RGB so the conversion remains finite; their original RGB cannot be recovered.
 */
public fun unpremultiply(color: Expr<Vec4<Flt<Med>>>): Expr<Vec4<Flt<Med>>> {
    val alpha = color.a
    val nonzero = alpha gt 0.000001f
    return ru.redbyte.redbytefx.color(
        r = ifElse(nonzero, color.r / max(alpha, 0.000001f), 0f.lit.toMed()),
        g = ifElse(nonzero, color.g / max(alpha, 0.000001f), 0f.lit.toMed()),
        b = ifElse(nonzero, color.b / max(alpha, 0.000001f), 0f.lit.toMed()),
        a = alpha,
    )
}

/**
 * Adjusts RGB saturation while preserving alpha.
 *
 * `amount = 0` produces grayscale, `amount = 1` keeps the original color, and values above `1`
 * exaggerate saturation.
 */
public fun adjustSaturation(
    color: Expr<Vec4<Flt<Med>>>,
    amount: Expr<Flt<High>>,
): Expr<Vec4<Flt<Med>>> {
    val luma = luminance(color).toMed()
    val rgb = mix(
        float3(luma, luma, luma),
        float3(color.r, color.g, color.b),
        amount,
    )
    return color(rgb, color.a)
}

/**
 * Adjusts RGB saturation while preserving alpha using a literal [amount].
 */
public fun adjustSaturation(
    color: Expr<Vec4<Flt<Med>>>,
    amount: Float,
): Expr<Vec4<Flt<Med>>> = adjustSaturation(color, float(amount))

/**
 * Multiplies premultiplied colors, preserving [base] alpha. [blend] alpha scales its influence.
 *
 * [amount] is saturated to the `[0, 1]` range so the helper behaves like a predictable blend
 * intensity rather than an extrapolated overshoot.
 */
public fun blendMultiply(
    base: Expr<Vec4<Flt<Med>>>,
    blend: Expr<Vec4<Flt<Med>>>,
    amount: Expr<Flt<High>> = float(1f),
): Expr<Vec4<Flt<Med>>> {
    val strength = blendAmount(amount).toMed()
    return ru.redbyte.redbytefx.color(
        base.r * (1f - strength * (blend.a - blend.r)),
        base.g * (1f - strength * (blend.a - blend.g)),
        base.b * (1f - strength * (blend.a - blend.b)),
        base.a,
    )
}

/**
 * Multiplies two colors together, then mixes the result back into [base] by a literal [amount].
 */
public fun blendMultiply(
    base: Expr<Vec4<Flt<Med>>>,
    blend: Expr<Vec4<Flt<Med>>>,
    amount: Float,
): Expr<Vec4<Flt<Med>>> = blendMultiply(base, blend, float(amount))

/**
 * Screens premultiplied colors, preserving [base] alpha. [blend] alpha scales its influence.
 *
 * [amount] is saturated to the `[0, 1]` range so screen intensity stays predictable.
 */
public fun blendScreen(
    base: Expr<Vec4<Flt<Med>>>,
    blend: Expr<Vec4<Flt<Med>>>,
    amount: Expr<Flt<High>> = float(1f),
): Expr<Vec4<Flt<Med>>> {
    val strength = blendAmount(amount).toMed()
    return ru.redbyte.redbytefx.color(
        base.r + strength * (base.a - base.r) * blend.r,
        base.g + strength * (base.a - base.g) * blend.g,
        base.b + strength * (base.a - base.b) * blend.b,
        base.a,
    )
}

/**
 * Applies screen blending and mixes the result back into [base] by a literal [amount].
 */
public fun blendScreen(
    base: Expr<Vec4<Flt<Med>>>,
    blend: Expr<Vec4<Flt<Med>>>,
    amount: Float,
): Expr<Vec4<Flt<Med>>> = blendScreen(base, blend, float(amount))

/**
 * Overlays premultiplied colors, preserving [base] alpha. [blend] alpha scales its influence.
 *
 * [amount] is saturated to the `[0, 1]` range so the helper stays aligned with the rest of the
 * stdlib's blend-intensity convention.
 */
public fun blendOverlay(
    base: Expr<Vec4<Flt<Med>>>,
    blend: Expr<Vec4<Flt<Med>>>,
    amount: Expr<Flt<High>> = float(1f),
): Expr<Vec4<Flt<Med>>> {
    val strength = blendAmount(amount).toMed()
    fun overlayChannel(baseChannel: Expr<Flt<Med>>, blendChannel: Expr<Flt<Med>>): Expr<Flt<Med>> {
        val baseWeight = ifElse(baseChannel lt base.a * 0.5f, baseChannel, base.a - baseChannel)
        return baseChannel + strength * baseWeight * (2f * blendChannel - blend.a)
    }
    return ru.redbyte.redbytefx.color(
        overlayChannel(base.r, blend.r),
        overlayChannel(base.g, blend.g),
        overlayChannel(base.b, blend.b),
        base.a,
    )
}

/**
 * Applies overlay blending and mixes the result back into [base] by a literal [amount].
 */
public fun blendOverlay(
    base: Expr<Vec4<Flt<Med>>>,
    blend: Expr<Vec4<Flt<Med>>>,
    amount: Float,
): Expr<Vec4<Flt<Med>>> = blendOverlay(base, blend, float(amount))

/**
 * Rodrigues hue rotation of [rgb] around the `(1,1,1)` axis by [amount] radians.
 */
public fun hueShift(
    rgb: Expr<Vec3<Flt<High>>>,
    amount: Expr<Flt<High>>,
): Expr<Vec3<Flt<High>>> {
    val axis = float3(0.57735026f, 0.57735026f, 0.57735026f)
    val c = cos(amount)
    val s = sin(amount)
    return rgb * c + cross(axis, rgb) * s + axis * dot(axis, rgb) * (1f - c)
}

/** [hueShift] with a literal angle in radians. */
public fun hueShift(
    rgb: Expr<Vec3<Flt<High>>>,
    amount: Float,
): Expr<Vec3<Flt<High>>> = hueShift(rgb, float(amount))

/** Hue-rotates RGB of [color] and keeps alpha. */
@JvmName("hueShiftColor")
public fun hueShift(
    color: Expr<Vec4<Flt<Med>>>,
    amount: Expr<Flt<High>>,
): Expr<Vec4<Flt<Med>>> {
    val rgb = hueShift(
        float3(color.r.toHigh(), color.g.toHigh(), color.b.toHigh()),
        amount,
    )
    return ru.redbyte.redbytefx.color(rgb.toMed(), color.a)
}

/** Hue-rotates [color] by a literal angle in radians. */
@JvmName("hueShiftColorLiteral")
public fun hueShift(
    color: Expr<Vec4<Flt<Med>>>,
    amount: Float,
): Expr<Vec4<Flt<Med>>> = hueShift(color, float(amount))

/**
 * ACES fitted tonemap, then saturates to `[0, 1]`.
 */
public fun filmicTonemap(rgb: Expr<Vec3<Flt<High>>>): Expr<Vec3<Flt<High>>> {
    val a = 2.51f
    val b = 0.03f
    val c = 2.43f
    val d = 0.59f
    val e = 0.14f
    return saturate((rgb * (rgb * a + b)) / (rgb * (rgb * c + d) + e))
}

/** Tonemaps RGB of [color] and keeps alpha. */
@JvmName("filmicTonemapColor")
public fun filmicTonemap(color: Expr<Vec4<Flt<Med>>>): Expr<Vec4<Flt<Med>>> {
    val rgb = filmicTonemap(float3(color.r.toHigh(), color.g.toHigh(), color.b.toHigh()))
    return ru.redbyte.redbytefx.color(rgb.toMed(), color.a)
}
