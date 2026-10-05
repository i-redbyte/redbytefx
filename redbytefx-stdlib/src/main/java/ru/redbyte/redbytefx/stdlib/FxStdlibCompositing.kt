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

private fun maskedAmount(mask: Expr<Flt<High>>, amount: Expr<Flt<High>>): Expr<Flt<High>> =
    saturate(mask * amount)

/**
 * Mixes [revealed] into [base] through a normalized [mask].
 *
 * [amount] scales the mask intensity before the final mix. This is the main canonical compositing
 * helper in `stdlib`: author a readable mask first, then reveal the next layer through it.
 */
public fun maskedMix(
    base: Expr<Vec4<Flt<Med>>>,
    revealed: Expr<Vec4<Flt<Med>>>,
    mask: Expr<Flt<High>>,
    amount: Expr<Flt<High>> = float(1f),
): Expr<Vec4<Flt<Med>>> = mix(base, revealed, maskedAmount(mask, amount))

/**
 * Mixes [revealed] into [base] through [mask] using a literal [amount].
 */
public fun maskedMix(
    base: Expr<Vec4<Flt<Med>>>,
    revealed: Expr<Vec4<Flt<Med>>>,
    mask: Expr<Flt<High>>,
    amount: Float,
): Expr<Vec4<Flt<Med>>> = maskedMix(
    base = base,
    revealed = revealed,
    mask = mask,
    amount = float(amount),
)

/**
 * Preserves RGB while multiplying alpha by the normalized [mask].
 *
 * This is useful for building matte layers before compositing them back into the scene. Prefer it
 * when the shader wants to prepare a tinted or lit overlay before feeding that layer into
 * [maskedScreen], [maskedOverlay], or plain [maskedMix].
 */
public fun alphaMask(
    color: Expr<Vec4<Flt<Med>>>,
    mask: Expr<Flt<High>>,
    amount: Expr<Flt<High>> = float(1f),
): Expr<Vec4<Flt<Med>>> = ru.redbyte.redbytefx.color(
    r = color.r,
    g = color.g,
    b = color.b,
    a = color.a * maskedAmount(mask, amount).toMed(),
)

/**
 * Preserves RGB while multiplying alpha by [mask] using a literal [amount].
 */
public fun alphaMask(
    color: Expr<Vec4<Flt<Med>>>,
    mask: Expr<Flt<High>>,
    amount: Float,
): Expr<Vec4<Flt<Med>>> = alphaMask(
    color = color,
    mask = mask,
    amount = float(amount),
)

/**
 * Applies screen blending through a normalized [mask].
 *
 * [amount] scales the effective mask intensity before blending. This works best when [blend] is
 * already a deliberate layer, not a replacement for first authoring the mask itself.
 */
public fun maskedScreen(
    base: Expr<Vec4<Flt<Med>>>,
    blend: Expr<Vec4<Flt<Med>>>,
    mask: Expr<Flt<High>>,
    amount: Expr<Flt<High>> = float(1f),
): Expr<Vec4<Flt<Med>>> = blendScreen(base, blend, maskedAmount(mask, amount))

/**
 * Applies screen blending through [mask] using a literal [amount].
 */
public fun maskedScreen(
    base: Expr<Vec4<Flt<Med>>>,
    blend: Expr<Vec4<Flt<Med>>>,
    mask: Expr<Flt<High>>,
    amount: Float,
): Expr<Vec4<Flt<Med>>> = maskedScreen(
    base = base,
    blend = blend,
    mask = mask,
    amount = float(amount),
)

/**
 * Applies overlay blending through a normalized [mask].
 *
 * [amount] scales the effective mask intensity before blending. Treat this as a more stylized
 * companion to [maskedMix] rather than the first compositing helper to teach.
 */
public fun maskedOverlay(
    base: Expr<Vec4<Flt<Med>>>,
    blend: Expr<Vec4<Flt<Med>>>,
    mask: Expr<Flt<High>>,
    amount: Expr<Flt<High>> = float(1f),
): Expr<Vec4<Flt<Med>>> = blendOverlay(base, blend, maskedAmount(mask, amount))

/**
 * Applies overlay blending through [mask] using a literal [amount].
 */
public fun maskedOverlay(
    base: Expr<Vec4<Flt<Med>>>,
    blend: Expr<Vec4<Flt<Med>>>,
    mask: Expr<Flt<High>>,
    amount: Float,
): Expr<Vec4<Flt<Med>>> = maskedOverlay(
    base = base,
    blend = blend,
    mask = mask,
    amount = float(amount),
)
