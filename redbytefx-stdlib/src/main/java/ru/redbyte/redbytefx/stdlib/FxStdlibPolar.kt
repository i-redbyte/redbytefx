package ru.redbyte.redbytefx.stdlib

import ru.redbyte.redbytefx.*

private const val TAU: Float = 6.2831855f

/**
 * Returns the radial distance from [center] in normalized UV space.
 *
 * This is a small convenience helper around `length(uv - center)` that keeps polar math readable
 * in higher-level shader recipes.
 */
public fun radialDistance(
    uv: Float2Expr,
    center: Float2Expr = float2(0.5f, 0.5f)
): FloatExpr = length(uv - center)

/**
 * Returns the normalized polar angle of [uv] around [center] in the `[0, 1)` range.
 *
 * `0` points to the positive X axis and the value increases counter-clockwise.
 */
public fun polarAngle01(
    uv: Float2Expr,
    center: Float2Expr = float2(0.5f, 0.5f)
): FloatExpr {
    val delta = uv - center
    return fract(atan(delta.y, delta.x) / TAU + 1f)
}

/**
 * Packs the radial distance and normalized angle of [uv] around [center] into a `float2`.
 *
 * The resulting vector is laid out as `(radius, polarAngle01)`.
 */
public fun polarCoordinates(
    uv: Float2Expr,
    center: Float2Expr = float2(0.5f, 0.5f)
): Float2Expr = float2(
    radialDistance(uv, center),
    polarAngle01(uv, center)
)

/**
 * Builds a soft angular sweep mask around [angle] in normalized polar space.
 *
 * [width] and [feather] are also interpreted in normalized angle units, where `1` is a full turn.
 */
public fun angularSweep(
    uv: Float2Expr,
    center: Float2Expr = float2(0.5f, 0.5f),
    angle: FloatExpr,
    width: FloatExpr,
    feather: FloatExpr = float(0.02f)
): FloatExpr {
    val sweepAngle = polarAngle01(uv, center)
    val safeWidth = max(width, 0.0001f)
    val safeFeather = max(feather, 0.0001f)
    val halfWidth = safeWidth * 0.5f
    val wrappedDistance = abs(fract(sweepAngle - angle + 0.5f) - 0.5f)
    return 1f - smoothstep(halfWidth, halfWidth + safeFeather, wrappedDistance)
}

/**
 * Builds a soft angular sweep mask using literal [width] and [feather] values.
 */
public fun angularSweep(
    uv: Float2Expr,
    center: Float2Expr = float2(0.5f, 0.5f),
    angle: FloatExpr,
    width: Float,
    feather: Float = 0.02f
): FloatExpr = angularSweep(
    uv = uv,
    center = center,
    angle = angle,
    width = float(width),
    feather = float(feather)
)

/**
 * Builds a soft polar arc mask by intersecting a ring with an angular sweep.
 *
 * This is useful for rotating radar arcs, orbital indicators, and other circular UI accents.
 */
public fun arcMask(
    uv: Float2Expr,
    center: Float2Expr = float2(0.5f, 0.5f),
    radius: FloatExpr,
    ringWidth: FloatExpr,
    angle: FloatExpr,
    arcWidth: FloatExpr,
    feather: FloatExpr = float(0.02f)
): FloatExpr = ringMask(
    uv = uv,
    center = center,
    radius = radius,
    width = ringWidth,
    feather = feather
) * angularSweep(
    uv = uv,
    center = center,
    angle = angle,
    width = arcWidth,
    feather = feather
)

/**
 * Builds a soft polar arc mask using an expression-driven [radius] with literal sizing values.
 */
public fun arcMask(
    uv: Float2Expr,
    center: Float2Expr = float2(0.5f, 0.5f),
    radius: FloatExpr,
    ringWidth: Float,
    angle: FloatExpr,
    arcWidth: Float,
    feather: Float = 0.02f
): FloatExpr = arcMask(
    uv = uv,
    center = center,
    radius = radius,
    ringWidth = float(ringWidth),
    angle = angle,
    arcWidth = float(arcWidth),
    feather = float(feather)
)

/**
 * Builds a soft polar arc mask using literal sizing values.
 */
public fun arcMask(
    uv: Float2Expr,
    center: Float2Expr = float2(0.5f, 0.5f),
    radius: Float,
    ringWidth: Float,
    angle: FloatExpr,
    arcWidth: Float,
    feather: Float = 0.02f
): FloatExpr = arcMask(
    uv = uv,
    center = center,
    radius = float(radius),
    ringWidth = float(ringWidth),
    angle = angle,
    arcWidth = float(arcWidth),
    feather = float(feather)
)
