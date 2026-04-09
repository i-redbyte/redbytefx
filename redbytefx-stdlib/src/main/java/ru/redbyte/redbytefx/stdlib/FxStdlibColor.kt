package ru.redbyte.redbytefx.stdlib

import ru.redbyte.redbytefx.*

/**
 * Adjusts RGB saturation while preserving alpha.
 *
 * `amount = 0` produces grayscale, `amount = 1` keeps the original color, and values above `1`
 * exaggerate saturation.
 */
public fun adjustSaturation(
    color: ColorExpr,
    amount: FloatExpr
): ColorExpr {
    val luma = luminance(color)
    val rgb = mix(
        float3(luma, luma, luma),
        float3(color.r, color.g, color.b),
        amount
    )
    return ru.redbyte.redbytefx.color(rgb, color.a)
}

/**
 * Adjusts RGB saturation while preserving alpha using a literal [amount].
 */
public fun adjustSaturation(
    color: ColorExpr,
    amount: Float
): ColorExpr = adjustSaturation(color, float(amount))

/**
 * Multiplies two colors together, then mixes the result back into [base] by [amount].
 */
public fun blendMultiply(
    base: ColorExpr,
    blend: ColorExpr,
    amount: FloatExpr = float(1f)
): ColorExpr {
    val multiplied = ru.redbyte.redbytefx.color(
        base.r * blend.r,
        base.g * blend.g,
        base.b * blend.b,
        base.a
    )
    return mix(base, multiplied, amount)
}

/**
 * Multiplies two colors together, then mixes the result back into [base] by a literal [amount].
 */
public fun blendMultiply(
    base: ColorExpr,
    blend: ColorExpr,
    amount: Float
): ColorExpr = blendMultiply(base, blend, float(amount))

/**
 * Applies screen blending and mixes the result back into [base] by [amount].
 */
public fun blendScreen(
    base: ColorExpr,
    blend: ColorExpr,
    amount: FloatExpr = float(1f)
): ColorExpr {
    val screened = ru.redbyte.redbytefx.color(
        1f - (1f - base.r) * (1f - blend.r),
        1f - (1f - base.g) * (1f - blend.g),
        1f - (1f - base.b) * (1f - blend.b),
        base.a
    )
    return mix(base, screened, amount)
}

/**
 * Applies screen blending and mixes the result back into [base] by a literal [amount].
 */
public fun blendScreen(
    base: ColorExpr,
    blend: ColorExpr,
    amount: Float
): ColorExpr = blendScreen(base, blend, float(amount))

/**
 * Applies overlay blending and mixes the result back into [base] by [amount].
 */
public fun blendOverlay(
    base: ColorExpr,
    blend: ColorExpr,
    amount: FloatExpr = float(1f)
): ColorExpr {
    fun overlayChannel(baseChannel: FloatExpr, blendChannel: FloatExpr): FloatExpr =
        ifElse(
            baseChannel lt 0.5f,
            2f * baseChannel * blendChannel,
            1f - 2f * (1f - baseChannel) * (1f - blendChannel)
        )

    val overlaid = ru.redbyte.redbytefx.color(
        overlayChannel(base.r, blend.r),
        overlayChannel(base.g, blend.g),
        overlayChannel(base.b, blend.b),
        base.a
    )
    return mix(base, overlaid, amount)
}

/**
 * Applies overlay blending and mixes the result back into [base] by a literal [amount].
 */
public fun blendOverlay(
    base: ColorExpr,
    blend: ColorExpr,
    amount: Float
): ColorExpr = blendOverlay(base, blend, float(amount))
