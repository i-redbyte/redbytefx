package ru.redbyte.redbytefx.stdlib

import ru.redbyte.redbytefx.*

/**
 * Computes a lightweight scalar hash from a `float2` position.
 *
 * The result is in the `[0, 1)` range and is intended for procedural modulation, not for
 * cryptographic or statistically rigorous randomness.
 */
public fun hash21(point: Float2Expr): FloatExpr =
    fract(sin(point.x * 127.1f + point.y * 311.7f) * 43758.5453f)

/**
 * Computes value noise from a `float2` position.
 *
 * The returned value is smoothly interpolated in the `[0, 1]` range.
 */
public fun valueNoise(point: Float2Expr): FloatExpr {
    val cell = floor(point)
    val local = fract(point)
    val smooth = local * local * (float2(3f, 3f) - 2f * local)

    val a = hash21(cell)
    val b = hash21(cell + float2(1f, 0f))
    val c = hash21(cell + float2(0f, 1f))
    val d = hash21(cell + float2(1f, 1f))

    val nx0 = mix(a, b, smooth.x)
    val nx1 = mix(c, d, smooth.x)
    return mix(nx0, nx1, smooth.y)
}

/**
 * Builds centered grain in the `[-1, 1]` range from UV coordinates and time.
 *
 * [scale] controls how dense the grain becomes across UV space.
 */
public fun grain(
    uv: Float2Expr,
    time: FloatExpr = float(0f),
    scale: FloatExpr = float(180f)
): FloatExpr {
    val safeScale = max(scale, 1f)
    val animatedUv = uv * safeScale + float2(time * 19.19f, time * 37.73f)
    return hash21(animatedUv) * 2f - 1f
}

/**
 * Builds centered grain in the `[-1, 1]` range using literal time and scale values.
 */
public fun grain(
    uv: Float2Expr,
    time: Float,
    scale: Float
): FloatExpr = grain(
    uv = uv,
    time = float(time),
    scale = float(scale)
)

/**
 * Builds centered grain in the `[-1, 1]` range using a time expression and a literal scale.
 */
public fun grain(
    uv: Float2Expr,
    time: FloatExpr,
    scale: Float
): FloatExpr = grain(
    uv = uv,
    time = time,
    scale = float(scale)
)

/**
 * Builds a radial vignette mask from normalized UV coordinates.
 *
 * The result stays near `1` at the center and fades toward `0` near the edges.
 */
public fun vignette(
    uv: Float2Expr,
    innerRadius: FloatExpr,
    outerRadius: FloatExpr
): FloatExpr {
    val safeInner = max(innerRadius, 0f)
    val safeOuter = max(outerRadius, safeInner + 0.0001f)
    val centered = (uv - float2(0.5f, 0.5f)) * 2f
    val radius = length(centered)
    return 1f - smoothstep(safeInner, safeOuter, radius)
}

/**
 * Builds a radial vignette mask from normalized UV coordinates using literal radii.
 */
public fun vignette(
    uv: Float2Expr,
    innerRadius: Float,
    outerRadius: Float
): FloatExpr = vignette(
    uv = uv,
    innerRadius = float(innerRadius),
    outerRadius = float(outerRadius)
)
