package ru.redbyte.redbytefx.stdlib

import ru.redbyte.redbytefx.*

/**
 * Computes the normalized position of [value] inside the `[inputStart, inputEnd]` range.
 *
 * The result is not clamped, so values outside the input range may produce values below `0` or
 * above `1`. Collapsed input ranges are undefined, just like the equivalent hand-written AGSL.
 * This is a small canonical support helper when a shader already has a clear numeric range model
 * and simply needs readable normalization math.
 */
public fun inverseLerp(
    inputStart: FloatExpr,
    inputEnd: FloatExpr,
    value: FloatExpr
): FloatExpr = (value - inputStart) / (inputEnd - inputStart)

/**
 * Computes the normalized position of [value] inside the literal `[inputStart, inputEnd]` range.
 */
public fun inverseLerp(
    inputStart: Float,
    inputEnd: Float,
    value: FloatExpr
): FloatExpr = inverseLerp(float(inputStart), float(inputEnd), value)

/**
 * Remaps [value] from `[inputStart, inputEnd]` into `[outputStart, outputEnd]`.
 *
 * The interpolation amount is not clamped. Use [saturate] on the result when the output should
 * stay inside the destination range. This is the main canonical mapping helper in `stdlib`: it
 * turns raw numeric ranges into readable mask, blend, or motion intensities without inventing
 * ad-hoc inline math every time.
 */
public fun remap(
    value: FloatExpr,
    inputStart: FloatExpr,
    inputEnd: FloatExpr,
    outputStart: FloatExpr,
    outputEnd: FloatExpr
): FloatExpr = mix(outputStart, outputEnd, inverseLerp(inputStart, inputEnd, value))

/**
 * Remaps [value] between two literal ranges.
 */
public fun remap(
    value: FloatExpr,
    inputStart: Float,
    inputEnd: Float,
    outputStart: Float,
    outputEnd: Float
): FloatExpr = remap(
    value = value,
    inputStart = float(inputStart),
    inputEnd = float(inputEnd),
    outputStart = float(outputStart),
    outputEnd = float(outputEnd)
)

/**
 * Posterizes a normalized scalar expression into [levels] discrete values.
 *
 * Inputs are clamped to the `[0, 1]` range before quantization. Values smaller than `2` are
 * treated as `2`, and non-integer level counts are floored. This is intentionally more stylized
 * than [inverseLerp] or [remap], so it belongs after the underlying color or mask path is already
 * clear.
 */
public fun posterize(
    value: FloatExpr,
    levels: FloatExpr
): FloatExpr {
    val safeLevels = max(floor(levels), 2f)
    val steps = safeLevels - 1f
    return floor(saturate(value) * steps + 0.5f) / steps
}

/**
 * Posterizes a normalized scalar expression into a literal number of [levels].
 */
public fun posterize(
    value: FloatExpr,
    levels: Float
): FloatExpr = posterize(value, float(levels))

/**
 * Posterizes the RGB channels of [color] while preserving alpha.
 */
public fun posterize(
    color: ColorExpr,
    levels: FloatExpr
): ColorExpr = color(
    r = posterize(color.r, levels),
    g = posterize(color.g, levels),
    b = posterize(color.b, levels),
    a = color.a
)

/**
 * Posterizes the RGB channels of [color] using a literal number of [levels].
 */
public fun posterize(
    color: ColorExpr,
    levels: Float
): ColorExpr = posterize(color, float(levels))
