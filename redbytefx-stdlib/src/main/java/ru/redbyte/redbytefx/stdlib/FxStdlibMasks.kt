package ru.redbyte.redbytefx.stdlib

import ru.redbyte.redbytefx.*

/**
 * Builds a soft circular mask from normalized UV coordinates.
 *
 * The returned value stays near `1` inside the circle and fades toward `0` outside it.
 */
public fun circleMask(
    uv: Float2Expr,
    center: Float2Expr = float2(0.5f, 0.5f),
    radius: FloatExpr,
    feather: FloatExpr = float(0.02f)
): FloatExpr {
    val safeRadius = max(radius, 0f)
    val safeFeather = max(feather, 0.0001f)
    val distance = length(uv - center)
    return 1f - smoothstep(safeRadius, safeRadius + safeFeather, distance)
}

/**
 * Builds a soft circular mask using literal [radius] and [feather] values.
 */
public fun circleMask(
    uv: Float2Expr,
    center: Float2Expr = float2(0.5f, 0.5f),
    radius: Float,
    feather: Float = 0.02f
): FloatExpr = circleMask(
    uv = uv,
    center = center,
    radius = float(radius),
    feather = float(feather)
)

/**
 * Builds a soft circular mask using an expression-driven [radius] and a literal [feather].
 */
public fun circleMask(
    uv: Float2Expr,
    center: Float2Expr = float2(0.5f, 0.5f),
    radius: FloatExpr,
    feather: Float
): FloatExpr = circleMask(
    uv = uv,
    center = center,
    radius = radius,
    feather = float(feather)
)

/**
 * Builds a soft ring mask from normalized UV coordinates.
 *
 * [radius] controls the ring center, [width] controls its thickness, and [feather] softens the
 * outer falloff around the band.
 */
public fun ringMask(
    uv: Float2Expr,
    center: Float2Expr = float2(0.5f, 0.5f),
    radius: FloatExpr,
    width: FloatExpr,
    feather: FloatExpr = float(0.02f)
): FloatExpr {
    val safeRadius = max(radius, 0f)
    val safeWidth = max(width, 0.0001f)
    val safeFeather = max(feather, 0.0001f)
    val halfWidth = safeWidth * 0.5f
    val distance = abs(length(uv - center) - safeRadius)
    return 1f - smoothstep(halfWidth, halfWidth + safeFeather, distance)
}

/**
 * Builds a soft ring mask using literal [radius], [width], and [feather] values.
 */
public fun ringMask(
    uv: Float2Expr,
    center: Float2Expr = float2(0.5f, 0.5f),
    radius: Float,
    width: Float,
    feather: Float = 0.02f
): FloatExpr = ringMask(
    uv = uv,
    center = center,
    radius = float(radius),
    width = float(width),
    feather = float(feather)
)

/**
 * Builds a soft ring mask using expression-driven [radius] and [width] with a literal [feather].
 */
public fun ringMask(
    uv: Float2Expr,
    center: Float2Expr = float2(0.5f, 0.5f),
    radius: FloatExpr,
    width: FloatExpr,
    feather: Float
): FloatExpr = ringMask(
    uv = uv,
    center = center,
    radius = radius,
    width = width,
    feather = float(feather)
)

/**
 * Builds a soft ring mask using an expression-driven [radius] and a literal [width].
 */
public fun ringMask(
    uv: Float2Expr,
    center: Float2Expr = float2(0.5f, 0.5f),
    radius: FloatExpr,
    width: Float,
    feather: Float = 0.02f
): FloatExpr = ringMask(
    uv = uv,
    center = center,
    radius = radius,
    width = float(width),
    feather = float(feather)
)

/**
 * Builds a soft axis-aligned rectangle mask from normalized UV coordinates.
 *
 * [size] is interpreted as the full rectangle size in UV space, not as half extents.
 */
public fun rectMask(
    uv: Float2Expr,
    center: Float2Expr = float2(0.5f, 0.5f),
    size: Float2Expr,
    feather: FloatExpr = float(0.02f)
): FloatExpr {
    val halfSizeX = max(size.x * 0.5f, 0f)
    val halfSizeY = max(size.y * 0.5f, 0f)
    val safeFeather = max(feather, 0.0001f)
    val local = uv - center
    val xMask = 1f - smoothstep(halfSizeX, halfSizeX + safeFeather, abs(local.x))
    val yMask = 1f - smoothstep(halfSizeY, halfSizeY + safeFeather, abs(local.y))
    return xMask * yMask
}

/**
 * Builds a soft axis-aligned rectangle mask using a literal [feather] value.
 */
public fun rectMask(
    uv: Float2Expr,
    center: Float2Expr = float2(0.5f, 0.5f),
    size: Float2Expr,
    feather: Float
): FloatExpr = rectMask(
    uv = uv,
    center = center,
    size = size,
    feather = float(feather)
)
