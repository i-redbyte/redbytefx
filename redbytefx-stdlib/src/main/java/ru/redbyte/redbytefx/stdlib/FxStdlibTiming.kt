package ru.redbyte.redbytefx.stdlib

import ru.redbyte.redbytefx.*

private const val PI: Float = 3.1415927f

/**
 * Builds a normalized ping-pong motion in the `[0, 1]` range.
 *
 * [period] is the full round-trip duration. The output moves from `0` to `1` and back to `0`
 * repeatedly as [value] advances.
 */
public fun pingPong(
    value: FloatExpr,
    period: FloatExpr
): FloatExpr {
    val safePeriod = max(period, 0.0001f)
    val phase = fract(value / safePeriod)
    return 1f - abs(phase * 2f - 1f)
}

/**
 * Builds a normalized ping-pong motion using a literal [period].
 */
public fun pingPong(
    value: FloatExpr,
    period: Float
): FloatExpr = pingPong(
    value = value,
    period = float(period)
)

/**
 * Applies a sine-based ease-in-out curve to a normalized value.
 *
 * Inputs are clamped to the `[0, 1]` range before easing.
 */
public fun easeInOutSine(value: FloatExpr): FloatExpr {
    val t = saturate(value)
    return 0.5f - 0.5f * cos(t * PI)
}

/**
 * Applies a cubic ease-in-out curve to a normalized value.
 *
 * Inputs are clamped to the `[0, 1]` range before easing.
 */
public fun easeInOutCubic(value: FloatExpr): FloatExpr {
    val t = saturate(value)
    return ifElse(
        t lt 0.5f,
        4f * t * t * t,
        1f - pow(-2f * t + 2f, 3f) / 2f
    )
}
