package ru.redbyte.redbytefx.stdlib

import ru.redbyte.redbytefx.*

/**
 * Masked compositing.
 *
 * All `masked*` helpers scale the effective mask weight the same way: [maskedAmount] applies the
 * same **\[0, 1\]** clamp semantics as `saturate(mask * amount)`. The core DSL lowers `saturate`
 * to `clamp(..., 0, 1)` in generated AGSL. Keep [mask] in a normalized `[0, 1]` band when possible;
 * use [amount] to dial intensity without changing the mask shape.
 */

private fun maskedAmount(mask: FloatExpr, amount: FloatExpr): FloatExpr =
    saturate(mask * amount)

/**
 * Mixes [revealed] into [base] through a normalized [mask].
 *
 * [amount] scales the mask intensity before the final mix. This is the main canonical compositing
 * helper in `stdlib`: author a readable mask first, then reveal the next layer through it.
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
 * This is useful for building matte layers before compositing them back into the scene. Prefer it
 * when the shader wants to prepare a tinted or lit overlay before feeding that layer into
 * [maskedScreen], [maskedOverlay], or plain [maskedMix].
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
 * [amount] scales the effective mask intensity before blending. This works best when [blend] is
 * already a deliberate layer, not a replacement for first authoring the mask itself.
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
 * [amount] scales the effective mask intensity before blending. Treat this as a more stylized
 * companion to [maskedMix] rather than the first compositing helper to teach.
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
