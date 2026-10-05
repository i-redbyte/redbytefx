package ru.redbyte.redbytefx.stdlib

import ru.redbyte.redbytefx.Expr
import ru.redbyte.redbytefx.Flt
import ru.redbyte.redbytefx.Prec
import ru.redbyte.redbytefx.div
import ru.redbyte.redbytefx.minus
import ru.redbyte.redbytefx.mix
import ru.redbyte.redbytefx.saturate

/**
 * Normalized position of [value] inside `[inputStart, inputEnd]`.
 *
 * **Relation to [remap]:** [remap] is `mix(outputStart, outputEnd, inverseLerp(inputStart, inputEnd, value))`.
 * Use [inverseLerp] for the raw `t` only; use [remap] when mapping into a destination range.
 *
 * The result is not clamped, so values outside the input range may produce values below `0` or
 * above `1`. Collapsed input ranges are undefined, just like the equivalent hand-written AGSL.
 */
public fun <P : Prec> inverseLerp(
    inputStart: Expr<Flt<P>>,
    inputEnd: Expr<Flt<P>>,
    value: Expr<Flt<P>>,
): Expr<Flt<P>> = (value - inputStart) / (inputEnd - inputStart)

/**
 * Maps [value] from `[inputStart, inputEnd]` into `[outputStart, outputEnd]`.
 *
 * **Formula:** `mix(outputStart, outputEnd, inverseLerp(inputStart, inputEnd, value))` - the only
 * canonical remapping path; literal overloads delegate here without a second formula.
 *
 * The interpolation amount is not clamped. Use [remapClamped] to keep the result inside the
 * destination range, including when its endpoints lie outside `[0, 1]`.
 */
public fun <P : Prec> remap(
    value: Expr<Flt<P>>,
    inputStart: Expr<Flt<P>>,
    inputEnd: Expr<Flt<P>>,
    outputStart: Expr<Flt<P>>,
    outputEnd: Expr<Flt<P>>,
): Expr<Flt<P>> = mix(outputStart, outputEnd, inverseLerp(inputStart, inputEnd, value))

/**
 * Like [remap], but clamps the interpolation amount to `[0, 1]` before mixing the output range.
 * Collapsed input ranges remain undefined.
 */
public fun <P : Prec> remapClamped(
    value: Expr<Flt<P>>,
    inputStart: Expr<Flt<P>>,
    inputEnd: Expr<Flt<P>>,
    outputStart: Expr<Flt<P>>,
    outputEnd: Expr<Flt<P>>,
): Expr<Flt<P>> = mix(outputStart, outputEnd, saturate(inverseLerp(inputStart, inputEnd, value)))
