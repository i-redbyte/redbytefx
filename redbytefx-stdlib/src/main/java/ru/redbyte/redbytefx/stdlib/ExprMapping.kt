package ru.redbyte.redbytefx.stdlib

import ru.redbyte.redbytefx.Expr
import ru.redbyte.redbytefx.Flt
import ru.redbyte.redbytefx.Prec
import ru.redbyte.redbytefx.div
import ru.redbyte.redbytefx.minus
import ru.redbyte.redbytefx.mix

/**
 * Normalized position of [value] inside `[inputStart, inputEnd]`.
 * The result is not clamped.
 */
public fun <P : Prec> inverseLerp(
    inputStart: Expr<Flt<P>>,
    inputEnd: Expr<Flt<P>>,
    value: Expr<Flt<P>>,
): Expr<Flt<P>> = (value - inputStart) / (inputEnd - inputStart)

/**
 * Maps [value] from `[inputStart, inputEnd]` into `[outputStart, outputEnd]`.
 * This is `mix(outputStart, outputEnd, inverseLerp(...))`.
 */
public fun <P : Prec> remap(
    value: Expr<Flt<P>>,
    inputStart: Expr<Flt<P>>,
    inputEnd: Expr<Flt<P>>,
    outputStart: Expr<Flt<P>>,
    outputEnd: Expr<Flt<P>>,
): Expr<Flt<P>> = mix(outputStart, outputEnd, inverseLerp(inputStart, inputEnd, value))
