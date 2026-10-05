package ru.redbyte.redbytefx.stdlib

import ru.redbyte.redbytefx.*
import kotlin.jvm.JvmName

/**
 * Computes the normalized position of [value] inside the literal `[inputStart, inputEnd]` range.
 */
public fun inverseLerp(
    inputStart: Float,
    inputEnd: Float,
    value: Expr<Flt<High>>,
): Expr<Flt<High>> = inverseLerp(float(inputStart), float(inputEnd), value)

/**
 * Remaps [value] between two literal ranges.
 */
public fun remap(
    value: Expr<Flt<High>>,
    inputStart: Float,
    inputEnd: Float,
    outputStart: Float,
    outputEnd: Float,
): Expr<Flt<High>> = remap(
    value = value,
    inputStart = float(inputStart),
    inputEnd = float(inputEnd),
    outputStart = float(outputStart),
    outputEnd = float(outputEnd),
)

/**
 * Posterizes a normalized scalar expression into [levels] discrete values.
 *
 * Inputs are clamped to the `[0, 1]` range before quantization. Values smaller than `2` are
 * treated as `2`, and non-integer level counts are floored. This is intentionally more stylized
 * than [inverseLerp] or [remap], so it belongs after the underlying color or mask path is already
 * clear.
 */
public fun <P : Prec> posterize(
    value: Expr<Flt<P>>,
    levels: Expr<Flt<P>>,
): Expr<Flt<P>> {
    val safeLevels = max(floor(levels), 2f)
    val steps = safeLevels - 1f
    return floor(saturate(value) * steps + 0.5f) / steps
}

/**
 * Posterizes a normalized scalar expression into a literal number of [levels].
 */
public fun posterize(
    value: Expr<Flt<High>>,
    levels: Float,
): Expr<Flt<High>> = posterize(value, float(levels))

/**
 * Posterizes the RGB channels of [color] while preserving alpha.
 */
@JvmName("posterizeColor")
public fun posterize(
    color: Expr<Vec4<Flt<Med>>>,
    levels: Expr<Flt<High>>,
): Expr<Vec4<Flt<Med>>> {
    val medLevels = levels.toMed()
    return color(
        r = posterize(color.r, medLevels),
        g = posterize(color.g, medLevels),
        b = posterize(color.b, medLevels),
        a = color.a,
    )
}

/**
 * Posterizes the RGB channels of [color] using a literal number of [levels].
 */
@JvmName("posterizeColorLevels")
public fun posterize(
    color: Expr<Vec4<Flt<Med>>>,
    levels: Float,
): Expr<Vec4<Flt<Med>>> = posterize(color, float(levels))
