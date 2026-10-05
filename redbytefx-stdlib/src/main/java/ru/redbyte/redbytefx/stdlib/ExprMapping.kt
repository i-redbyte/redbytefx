package ru.redbyte.redbytefx.stdlib

import ru.redbyte.redbytefx.Expr
import ru.redbyte.redbytefx.Flt
import ru.redbyte.redbytefx.Prec
import ru.redbyte.redbytefx.div
import ru.redbyte.redbytefx.minus
import ru.redbyte.redbytefx.mix

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
 * The interpolation amount is not clamped. Use [saturate] on the result when the output should
 * stay inside the destination range.
 */
public fun <P : Prec> remap(
    value: Expr<Flt<P>>,
    inputStart: Expr<Flt<P>>,
    inputEnd: Expr<Flt<P>>,
    outputStart: Expr<Flt<P>>,
    outputEnd: Expr<Flt<P>>,
): Expr<Flt<P>> = mix(outputStart, outputEnd, inverseLerp(inputStart, inputEnd, value))
