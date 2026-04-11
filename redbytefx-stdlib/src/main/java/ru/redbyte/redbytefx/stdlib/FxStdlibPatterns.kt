package ru.redbyte.redbytefx.stdlib

import ru.redbyte.redbytefx.*

/**
 * Builds a normalized sine pulse in the `[0, 1]` range.
 *
 * [speed] scales how quickly the pulse advances as [time] changes, while [phase] offsets the wave.
 * This is a canonical modulation helper for temporal animation and procedural shading.
 */
public fun pulse(
    time: FloatExpr,
    speed: FloatExpr = float(1f),
    phase: FloatExpr = float(0f)
): FloatExpr = 0.5f + 0.5f * sin(time * speed + phase)

/**
 * Builds a normalized sine pulse in the `[0, 1]` range using literal speed and phase values.
 */
public fun pulse(
    time: FloatExpr,
    speed: Float = 1f,
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
 * the soft line thickness at cell borders in normalized cell space. This is useful once the author
 * already understands simpler one-dimensional masks like [bandMask] and broad panel masks like
 * [rectMask]; it is not the first pattern helper to teach.
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
 * This helper is typically driven by pixel-space coordinates such as `fragCoord.y`. [softness]
 * controls how quickly the line fades away from its origin. Treat it as a display-style secondary
 * helper after the main content, mask, and sampling path are already clear.
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
