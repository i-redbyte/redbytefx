package ru.redbyte.redbytefx.stdlib

import ru.redbyte.redbytefx.*

/**
 * Returns the signed distance from [point] to a circle centered at the local origin.
 *
 * Negative values are inside the shape, positive values are outside, and zero lies on the edge.
 * In practice [point] is usually a centered local coordinate such as [centeredUv] or
 * [aspectCenteredUv], not raw `[0,1]` UV space.
 */
public fun sdCircle(
    point: Float2Expr,
    radius: FloatExpr
): FloatExpr = length(point) - max(radius, 0f)

/**
 * Returns the signed distance from [point] to a circle using a literal [radius].
 */
public fun sdCircle(
    point: Float2Expr,
    radius: Float
): FloatExpr = sdCircle(
    point = point,
    radius = float(radius)
)

/**
 * Returns the signed distance from [point] to an axis-aligned box centered at the local origin.
 *
 * [halfSize] follows standard SDF terminology and represents half extents on each axis. Use this
 * family when the shader is already thinking in local shape space, not in screen-space masks.
 */
public fun sdBox(
    point: Float2Expr,
    halfSize: Float2Expr
): FloatExpr {
    val dx = abs(point.x) - max(halfSize.x, 0f)
    val dy = abs(point.y) - max(halfSize.y, 0f)
    val outside = length(float2(max(dx, 0f), max(dy, 0f)))
    val inside = min(max(dx, dy), 0f)
    return outside + inside
}

/**
 * Returns the signed distance from [point] to a rounded axis-aligned box centered at the local
 * origin.
 *
 * [halfSize] follows standard SDF terminology and represents half extents on each axis.
 */
public fun sdRoundedBox(
    point: Float2Expr,
    halfSize: Float2Expr,
    radius: FloatExpr
): FloatExpr {
    val safeRadius = max(radius, 0f)
    val dx = abs(point.x) - max(halfSize.x - safeRadius, 0f)
    val dy = abs(point.y) - max(halfSize.y - safeRadius, 0f)
    val outside = length(float2(max(dx, 0f), max(dy, 0f)))
    val inside = min(max(dx, dy), 0f)
    return outside + inside - safeRadius
}

/**
 * Returns the signed distance from [point] to a rounded axis-aligned box using a literal
 * [radius].
 */
public fun sdRoundedBox(
    point: Float2Expr,
    halfSize: Float2Expr,
    radius: Float
): FloatExpr = sdRoundedBox(
    point = point,
    halfSize = halfSize,
    radius = float(radius)
)

/**
 * Returns the signed distance from [point] to the segment between [start] and [end].
 */
public fun sdSegment(
    point: Float2Expr,
    start: Float2Expr,
    end: Float2Expr
): FloatExpr {
    val pa = point - start
    val ba = end - start
    val h = clamp(dot(pa, ba) / max(dot(ba, ba), 0.0001f), 0f, 1f)
    return length(pa - ba * h)
}

/**
 * Fills an SDF shape with a hard edge.
 */
public fun fill(distance: FloatExpr): FloatExpr =
    1f - step(0f, distance)

/**
 * Fills an SDF shape with a soft edge controlled by [feather].
 *
 * This is one of the main canonical bridges from signed-distance authoring into a normalized mask.
 */
public fun softFill(
    distance: FloatExpr,
    feather: FloatExpr = float(0.02f)
): FloatExpr = 1f - smoothstep(0f, max(feather, 0.0001f), distance)

/**
 * Fills an SDF shape with a soft edge using a literal [feather].
 */
public fun softFill(
    distance: FloatExpr,
    feather: Float
): FloatExpr = softFill(distance, float(feather))

/**
 * Builds a hard-edged stroke around an SDF contour.
 */
public fun stroke(
    distance: FloatExpr,
    width: FloatExpr
): FloatExpr = 1f - step(max(width, 0.0001f) * 0.5f, abs(distance))

/**
 * Builds a hard-edged stroke around an SDF contour using a literal [width].
 */
public fun stroke(
    distance: FloatExpr,
    width: Float
): FloatExpr = stroke(distance, float(width))

/**
 * Builds a soft stroke around an SDF contour.
 *
 * This is the stroke-oriented companion to [softFill] and is a canonical way to turn SDF distance
 * fields into readable outline masks.
 */
public fun softStroke(
    distance: FloatExpr,
    width: FloatExpr,
    feather: FloatExpr = float(0.02f)
): FloatExpr {
    val halfWidth = max(width, 0.0001f) * 0.5f
    return 1f - smoothstep(halfWidth, halfWidth + max(feather, 0.0001f), abs(distance))
}

/**
 * Builds a soft stroke around an SDF contour using literal [width] and [feather] values.
 */
public fun softStroke(
    distance: FloatExpr,
    width: Float,
    feather: Float = 0.02f
): FloatExpr = softStroke(
    distance = distance,
    width = float(width),
    feather = float(feather)
)
