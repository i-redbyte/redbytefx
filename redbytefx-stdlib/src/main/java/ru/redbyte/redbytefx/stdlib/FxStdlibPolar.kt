package ru.redbyte.redbytefx.stdlib

import ru.redbyte.redbytefx.*

private const val TAU: Float = 6.2831855f

/**
 * Returns the radial distance from [center] in normalized UV space.
 *
 * This is a small convenience helper around `length(uv - center)` that keeps polar math readable
 * in higher-level shader recipes. Treat it as one of the tiny canonical polar primitives.
 */
public fun radialDistance(
    uv: Expr<Vec2<Flt<High>>>,
    center: Expr<Vec2<Flt<High>>> = float2(0.5f, 0.5f)
): Expr<Flt<High>> = length(uv - center)

/**
 * Returns the normalized polar angle of [uv] around [center] in the `[0, 1)` range.
 *
 * `0` points to the positive X axis and the value increases counter-clockwise. This is the
 * canonical angular primitive to inspect before reaching for higher-level polar masks.
 */
public fun polarAngle01(
    uv: Expr<Vec2<Flt<High>>>,
    center: Expr<Vec2<Flt<High>>> = float2(0.5f, 0.5f)
): Expr<Flt<High>> {
    val delta = uv - center
    return fract(atan(delta.y, delta.x) / TAU + 1f)
}

/**
 * Packs the radial distance and normalized angle of [uv] around [center] into a `float2`.
 *
 * The resulting vector is laid out as `(radius, polarAngle01)`. Use this when the shader wants to
 * stay in explicit polar space instead of recomputing radius/angle separately.
 */
public fun polarCoordinates(
    uv: Expr<Vec2<Flt<High>>>,
    center: Expr<Vec2<Flt<High>>> = float2(0.5f, 0.5f)
): Expr<Vec2<Flt<High>>> = float2(
    radialDistance(uv, center),
    polarAngle01(uv, center)
)

/**
 * Builds a soft angular sweep mask around [angle] in normalized polar space.
 *
 * [width] and [feather] are also interpreted in normalized angle units, where `1` is a full turn.
 * This is the canonical polar mask helper for radar sweeps, arcs, and rotating wedges.
 */
public fun angularSweep(
    uv: Expr<Vec2<Flt<High>>>,
    center: Expr<Vec2<Flt<High>>> = float2(0.5f, 0.5f),
    angle: Expr<Flt<High>>,
    width: Expr<Flt<High>>,
    feather: Expr<Flt<High>> = float(0.02f)
): Expr<Flt<High>> {
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
    uv: Expr<Vec2<Flt<High>>>,
    center: Expr<Vec2<Flt<High>>> = float2(0.5f, 0.5f),
    angle: Expr<Flt<High>>,
    width: Float,
    feather: Float = 0.02f
): Expr<Flt<High>> = angularSweep(
    uv = uv,
    center = center,
    angle = angle,
    width = float(width),
    feather = float(feather)
)

/**
 * Builds a soft polar arc mask by intersecting a ring with an angular sweep.
 *
 * This is useful for rotating radar arcs, orbital indicators, and other circular UI accents. It is
 * a good second-step polar helper once [ringMask] and [angularSweep] already make sense on their
 * own.
 */
public fun arcMask(
    uv: Expr<Vec2<Flt<High>>>,
    center: Expr<Vec2<Flt<High>>> = float2(0.5f, 0.5f),
    radius: Expr<Flt<High>>,
    ringWidth: Expr<Flt<High>>,
    angle: Expr<Flt<High>>,
    arcWidth: Expr<Flt<High>>,
    feather: Expr<Flt<High>> = float(0.02f)
): Expr<Flt<High>> = ringMask(
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
    uv: Expr<Vec2<Flt<High>>>,
    center: Expr<Vec2<Flt<High>>> = float2(0.5f, 0.5f),
    radius: Expr<Flt<High>>,
    ringWidth: Float,
    angle: Expr<Flt<High>>,
    arcWidth: Float,
    feather: Float = 0.02f
): Expr<Flt<High>> = arcMask(
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
    uv: Expr<Vec2<Flt<High>>>,
    center: Expr<Vec2<Flt<High>>> = float2(0.5f, 0.5f),
    radius: Float,
    ringWidth: Float,
    angle: Expr<Flt<High>>,
    arcWidth: Float,
    feather: Float = 0.02f
): Expr<Flt<High>> = arcMask(
    uv = uv,
    center = center,
    radius = float(radius),
    ringWidth = float(ringWidth),
    angle = angle,
    arcWidth = float(arcWidth),
    feather = float(feather)
)
