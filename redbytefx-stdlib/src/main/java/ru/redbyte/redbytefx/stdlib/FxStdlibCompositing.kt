package ru.redbyte.redbytefx.stdlib

import ru.redbyte.redbytefx.*

private fun maskedAmount(mask: FloatExpr, amount: FloatExpr): FloatExpr =
    saturate(mask * amount)

/**
 * Mixes [revealed] into [base] through a normalized [mask].
 *
 * [amount] scales the mask intensity before the final mix.
 */
public fun maskedMix(
    base: ColorExpr,
    revealed: ColorExpr,
    mask: FloatExpr,
    amount: FloatExpr = float(1f)
): ColorExpr = mix(base, revealed, maskedAmount(mask, amount))

/**
 * Mixes [revealed] into [base] through [mask] using a literal [amount].
 */
public fun maskedMix(
    base: ColorExpr,
    revealed: ColorExpr,
    mask: FloatExpr,
    amount: Float
): ColorExpr = maskedMix(
    base = base,
    revealed = revealed,
    mask = mask,
    amount = float(amount)
)

/**
 * Preserves RGB while multiplying alpha by the normalized [mask].
 *
 * This is useful for building matte layers before compositing them back into the scene.
 */
public fun alphaMask(
    color: ColorExpr,
    mask: FloatExpr,
    amount: FloatExpr = float(1f)
): ColorExpr = ru.redbyte.redbytefx.color(
    r = color.r,
    g = color.g,
    b = color.b,
    a = color.a * maskedAmount(mask, amount)
)

/**
 * Preserves RGB while multiplying alpha by [mask] using a literal [amount].
 */
public fun alphaMask(
    color: ColorExpr,
    mask: FloatExpr,
    amount: Float
): ColorExpr = alphaMask(
    color = color,
    mask = mask,
    amount = float(amount)
)

/**
 * Applies screen blending through a normalized [mask].
 *
 * [amount] scales the effective mask intensity before blending.
 */
public fun maskedScreen(
    base: ColorExpr,
    blend: ColorExpr,
    mask: FloatExpr,
    amount: FloatExpr = float(1f)
): ColorExpr = blendScreen(base, blend, maskedAmount(mask, amount))

/**
 * Applies screen blending through [mask] using a literal [amount].
 */
public fun maskedScreen(
    base: ColorExpr,
    blend: ColorExpr,
    mask: FloatExpr,
    amount: Float
): ColorExpr = maskedScreen(
    base = base,
    blend = blend,
    mask = mask,
    amount = float(amount)
)

/**
 * Applies overlay blending through a normalized [mask].
 *
 * [amount] scales the effective mask intensity before blending.
 */
public fun maskedOverlay(
    base: ColorExpr,
    blend: ColorExpr,
    mask: FloatExpr,
    amount: FloatExpr = float(1f)
): ColorExpr = blendOverlay(base, blend, maskedAmount(mask, amount))

/**
 * Applies overlay blending through [mask] using a literal [amount].
 */
public fun maskedOverlay(
    base: ColorExpr,
    blend: ColorExpr,
    mask: FloatExpr,
    amount: Float
): ColorExpr = maskedOverlay(
    base = base,
    blend = blend,
    mask = mask,
    amount = float(amount)
)
