package ru.redbyte.redbytefx.stdlib

import ru.redbyte.redbytefx.*

/**
 * Builds a normalized sine pulse in the `[0, 1]` range.
 *
 * This is a small authoring helper for temporal animation and procedural modulation.
 */
public fun pulse(
    time: FloatExpr,
    speed: FloatExpr,
    phase: FloatExpr = float(0f)
): FloatExpr = 0.5f + 0.5f * sin(time * speed + phase)

/**
 * Builds a normalized sine pulse in the `[0, 1]` range using literal speed and phase values.
 */
public fun pulse(
    time: FloatExpr,
    speed: Float,
    phase: Float = 0f
): FloatExpr = pulse(
    time = time,
    speed = float(speed),
    phase = float(phase)
)

/**
 * Creates a grid-like mask from normalized UV coordinates.
 *
 * [density] controls how many cells appear across the UV space, while [lineWidth] controls
 * the soft line thickness at cell borders.
 */
public fun gridMask(
    uv: Float2Expr,
    density: FloatExpr,
    lineWidth: FloatExpr
): FloatExpr {
    val safeDensity = max(density, 1f)
    val safeLineWidth = max(lineWidth, 0.0001f)
    val cell = fract(uv * safeDensity)
    val edgeX = min(cell.x, 1f - cell.x)
    val edgeY = min(cell.y, 1f - cell.y)
    return max(
        1f - smoothstep(0f, safeLineWidth, edgeX),
        1f - smoothstep(0f, safeLineWidth, edgeY)
    )
}

/**
 * Creates a grid-like mask from normalized UV coordinates using literal density and line width.
 */
public fun gridMask(
    uv: Float2Expr,
    density: Float,
    lineWidth: Float
): FloatExpr = gridMask(
    uv = uv,
    density = float(density),
    lineWidth = float(lineWidth)
)

/**
 * Creates a horizontal scanline mask from a coordinate, repeated every [spacing] units.
 *
 * [softness] controls how quickly the line fades away from its origin.
 */
public fun scanlines(
    position: FloatExpr,
    spacing: FloatExpr,
    softness: FloatExpr
): FloatExpr {
    val safeSpacing = max(spacing, 1f)
    val safeSoftness = max(softness, 0.0001f)
    return 1f - smoothstep(0f, safeSoftness, mod(position, safeSpacing))
}

/**
 * Creates a horizontal scanline mask from a coordinate using literal spacing and softness.
 */
public fun scanlines(
    position: FloatExpr,
    spacing: Float,
    softness: Float
): FloatExpr = scanlines(
    position = position,
    spacing = float(spacing),
    softness = float(softness)
)
