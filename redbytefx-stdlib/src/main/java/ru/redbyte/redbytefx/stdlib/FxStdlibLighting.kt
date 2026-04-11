package ru.redbyte.redbytefx.stdlib

import ru.redbyte.redbytefx.*

/**
 * Converts sample-space coordinates into normalized UV space.
 *
 * This is a tiny canonical convenience helper around `fragCoord / resolution` for shaders that
 * want to stay in normalized coordinates for masks, gradients, and procedural math. Reach for it
 * when the shader really wants `[0,1]` UV space; otherwise staying in raw sample space with
 * `fragCoord` often keeps AGSL ports easier to read.
 */
public fun FxDsl.normalizedUv(
    coord: Float2Expr = fragCoord
): Float2Expr = coord / float2(
    max(resolution.x, 0.0001f),
    max(resolution.y, 0.0001f)
)

/**
 * Samples the input content from normalized UV coordinates.
 *
 * This is the inverse convenience of [normalizedUv] and expands to `sample(uv * resolution)`.
 * Prefer plain `sample(...)` when the shader is already operating in pixel/sample coordinates.
 */
public fun FxDsl.sampleUv(
    uv: Float2Expr
): ColorExpr = sample(
    uv * float2(
        max(resolution.x, 0.0001f),
        max(resolution.y, 0.0001f)
    )
)

/**
 * Recenters normalized UV coordinates around [center].
 *
 * This is a tiny readability helper for lighting-style shaders that want to work in a coordinate
 * system centered around the focal point instead of the top-left corner.
 */
public fun centeredUv(
    uv: Float2Expr,
    center: Float2Expr = float2(0.5f, 0.5f)
): Float2Expr = uv - center

/**
 * Recenters normalized UV coordinates and applies aspect correction using [resolution].
 *
 * The returned vector keeps radial math visually round on non-square surfaces by scaling the X
 * component according to the current render target aspect ratio.
 */
public fun aspectCenteredUv(
    uv: Float2Expr,
    resolution: Float2Expr,
    center: Float2Expr = float2(0.5f, 0.5f)
): Float2Expr {
    val centered = centeredUv(uv, center)
    val safeHeight = max(resolution.y, 0.0001f)
    val aspect = max(resolution.x / safeHeight, 0.0001f)
    return float2(centered.x * aspect, centered.y)
}

/**
 * Returns the normalized radial direction from [center] in aspect-corrected UV space.
 *
 * This is useful when a lighting recipe needs a stable direction vector for tinting, falloff, or
 * asymmetric highlights around a focal point.
 */
public fun radialDirection(
    uv: Float2Expr,
    resolution: Float2Expr,
    center: Float2Expr = float2(0.5f, 0.5f)
): Float2Expr {
    val centered = aspectCenteredUv(uv, resolution, center)
    val safeLength = max(length(centered), 0.0001f)
    return centered / safeLength
}

/**
 * Builds a soft radial glow around [center] in aspect-corrected UV space.
 *
 * The glow stays near `1` inside [radius] and fades toward `0` outside it.
 */
public fun centerGlow(
    uv: Float2Expr,
    resolution: Float2Expr,
    center: Float2Expr = float2(0.5f, 0.5f),
    radius: FloatExpr,
    feather: FloatExpr = float(0.02f)
): FloatExpr {
    val safeRadius = max(radius, 0f)
    val safeFeather = max(feather, 0.0001f)
    val distance = length(aspectCenteredUv(uv, resolution, center))
    return 1f - smoothstep(safeRadius, safeRadius + safeFeather, distance)
}

/**
 * Builds a soft radial glow using literal [radius] and [feather] values.
 */
public fun centerGlow(
    uv: Float2Expr,
    resolution: Float2Expr,
    center: Float2Expr = float2(0.5f, 0.5f),
    radius: Float,
    feather: Float = 0.02f
): FloatExpr = centerGlow(
    uv = uv,
    resolution = resolution,
    center = center,
    radius = float(radius),
    feather = float(feather)
)

/**
 * Builds a soft radial glow using an expression-driven [radius] and a literal [feather].
 */
public fun centerGlow(
    uv: Float2Expr,
    resolution: Float2Expr,
    center: Float2Expr = float2(0.5f, 0.5f),
    radius: FloatExpr,
    feather: Float
): FloatExpr = centerGlow(
    uv = uv,
    resolution = resolution,
    center = center,
    radius = radius,
    feather = float(feather)
)

/**
 * Builds a soft aspect-corrected rim light around [center].
 *
 * [radius] controls the ring center while [width] controls the visible light thickness.
 */
public fun rimLight(
    uv: Float2Expr,
    resolution: Float2Expr,
    center: Float2Expr = float2(0.5f, 0.5f),
    radius: FloatExpr,
    width: FloatExpr,
    feather: FloatExpr = float(0.02f)
): FloatExpr {
    val safeRadius = max(radius, 0f)
    val safeWidth = max(width, 0.0001f)
    val safeFeather = max(feather, 0.0001f)
    val halfWidth = safeWidth * 0.5f
    val distance = abs(length(aspectCenteredUv(uv, resolution, center)) - safeRadius)
    return 1f - smoothstep(halfWidth, halfWidth + safeFeather, distance)
}

/**
 * Builds a soft aspect-corrected rim light using literal sizing values.
 */
public fun rimLight(
    uv: Float2Expr,
    resolution: Float2Expr,
    center: Float2Expr = float2(0.5f, 0.5f),
    radius: Float,
    width: Float,
    feather: Float = 0.02f
): FloatExpr = rimLight(
    uv = uv,
    resolution = resolution,
    center = center,
    radius = float(radius),
    width = float(width),
    feather = float(feather)
)

/**
 * Builds a soft aspect-corrected rim light using expression-driven sizing with a literal
 * [feather].
 */
public fun rimLight(
    uv: Float2Expr,
    resolution: Float2Expr,
    center: Float2Expr = float2(0.5f, 0.5f),
    radius: FloatExpr,
    width: FloatExpr,
    feather: Float
): FloatExpr = rimLight(
    uv = uv,
    resolution = resolution,
    center = center,
    radius = radius,
    width = width,
    feather = float(feather)
)

/**
 * Builds a soft aspect-corrected rim light using an expression-driven [radius] with literal
 * [width] and [feather] values.
 */
public fun rimLight(
    uv: Float2Expr,
    resolution: Float2Expr,
    center: Float2Expr = float2(0.5f, 0.5f),
    radius: FloatExpr,
    width: Float,
    feather: Float = 0.02f
): FloatExpr = rimLight(
    uv = uv,
    resolution = resolution,
    center = center,
    radius = radius,
    width = float(width),
    feather = float(feather)
)
