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
    point: Expr<Vec2<Flt<High>>>,
    radius: Expr<Flt<High>>,
): Expr<Flt<High>> = length(point) - max(radius, 0f)

/**
 * Returns the signed distance from [point] to a circle using a literal [radius].
 */
public fun sdCircle(
    point: Expr<Vec2<Flt<High>>>,
    radius: Float,
): Expr<Flt<High>> = sdCircle(
    point = point,
    radius = float(radius),
)

/**
 * Returns the signed distance from [point] to an axis-aligned box centered at the local origin.
 *
 * [halfSize] follows standard SDF terminology and represents half extents on each axis. Use this
 * family when the shader is already thinking in local shape space, not in screen-space masks.
 */
public fun sdBox(
    point: Expr<Vec2<Flt<High>>>,
    halfSize: Expr<Vec2<Flt<High>>>,
): Expr<Flt<High>> {
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
    point: Expr<Vec2<Flt<High>>>,
    halfSize: Expr<Vec2<Flt<High>>>,
    radius: Expr<Flt<High>>,
): Expr<Flt<High>> {
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
    point: Expr<Vec2<Flt<High>>>,
    halfSize: Expr<Vec2<Flt<High>>>,
    radius: Float,
): Expr<Flt<High>> = sdRoundedBox(
    point = point,
    halfSize = halfSize,
    radius = float(radius),
)

/**
 * Returns the signed distance from [point] to the segment between [start] and [end].
 */
public fun sdSegment(
    point: Expr<Vec2<Flt<High>>>,
    start: Expr<Vec2<Flt<High>>>,
    end: Expr<Vec2<Flt<High>>>,
): Expr<Flt<High>> {
    val pa = point - start
    val ba = end - start
    val h = clamp(dot(pa, ba) / max(dot(ba, ba), 0.0001f), 0f, 1f)
    return length(pa - ba * h)
}

/**
 * Signed distance from [point] to a regular hexagon of [radius], centered at the origin.
 */
public fun sdHexagon(
    point: Expr<Vec2<Flt<High>>>,
    radius: Expr<Flt<High>>,
): Expr<Flt<High>> {
    val r = max(radius, 0f)
    val kxy = float2(-0.8660254f, 0.5f)
    val kz = float(0.57735026f)
    val folded = abs(point)
    val t = min(dot(kxy, folded), 0f)
    val p = folded - kxy * (t * 2f)
    val q = p - float2(clamp(p.x, -kz * r, kz * r), r)
    return length(q) * sign(q.y)
}

/** Signed distance from [point] to a hexagon with a literal [radius]. */
public fun sdHexagon(
    point: Expr<Vec2<Flt<High>>>,
    radius: Float,
): Expr<Flt<High>> = sdHexagon(point, float(radius))

/**
 * Signed distance from [point] to a rhombus whose half-extents are [halfSize].
 */
public fun sdRhombus(
    point: Expr<Vec2<Flt<High>>>,
    halfSize: Expr<Vec2<Flt<High>>>,
): Expr<Flt<High>> {
    val b = float2(max(halfSize.x, 0f), max(halfSize.y, 0f))
    val p = abs(point)
    val ndot = (b.x - p.x * 2f) * b.x - (b.y - p.y * 2f) * b.y
    val h = clamp(ndot / max(dot(b, b), 0.0001f), -1f, 1f)
    val offset = b * 0.5f * float2(1f - h, 1f + h)
    val d = length(p - offset)
    return d * sign(p.x * b.y + p.y * b.x - b.x * b.y)
}

/**
 * Signed distance from [point] to an equilateral triangle of [radius], pointing +Y.
 */
public fun sdEquilateralTriangle(
    point: Expr<Vec2<Flt<High>>>,
    radius: Expr<Flt<High>>,
): Expr<Flt<High>> {
    val r = max(radius, 0f)
    val k = float(1.7320508f)
    val px = abs(point.x) - r
    val py = point.y + r / k
    val folded = ifElse(
        (px + k * py) gt 0f,
        float2(px - k * py, -k * px - py) * 0.5f,
        float2(px, py),
    )
    val q = float2(folded.x - clamp(folded.x, r * -2f, 0f.lit), folded.y)
    return -length(q) * sign(q.y)
}

/** Signed distance from [point] to an equilateral triangle with a literal [radius]. */
public fun sdEquilateralTriangle(
    point: Expr<Vec2<Flt<High>>>,
    radius: Float,
): Expr<Flt<High>> = sdEquilateralTriangle(point, float(radius))

/** Rounds an SDF by subtracting [radius]. */
public fun opRound(distance: Expr<Flt<High>>, radius: Expr<Flt<High>>): Expr<Flt<High>> =
    distance - max(radius, 0f)

/** Rounds an SDF by a literal [radius]. */
public fun opRound(distance: Expr<Flt<High>>, radius: Float): Expr<Flt<High>> =
    opRound(distance, float(radius))

/** Builds a ring of [thickness] around an SDF contour. */
public fun opOnion(distance: Expr<Flt<High>>, thickness: Expr<Flt<High>>): Expr<Flt<High>> =
    abs(distance) - max(thickness, 0f)

/** Builds a ring of literal [thickness] around an SDF contour. */
public fun opOnion(distance: Expr<Flt<High>>, thickness: Float): Expr<Flt<High>> =
    opOnion(distance, float(thickness))

/**
 * Fills an SDF shape with a hard edge.
 */
public fun fill(distance: Expr<Flt<High>>): Expr<Flt<High>> =
    1f - step(0f, distance)

/**
 * Fills an SDF shape with a soft edge controlled by [feather].
 *
 * This is one of the main canonical bridges from signed-distance authoring into a normalized mask.
 */
public fun softFill(
    distance: Expr<Flt<High>>,
    feather: Expr<Flt<High>> = float(0.02f),
): Expr<Flt<High>> = 1f - smoothstep(0f, max(feather, 0.0001f), distance)

/**
 * Fills an SDF shape with a soft edge using a literal [feather].
 */
public fun softFill(
    distance: Expr<Flt<High>>,
    feather: Float,
): Expr<Flt<High>> = softFill(distance, float(feather))

/**
 * Builds a hard-edged stroke around an SDF contour.
 */
public fun stroke(
    distance: Expr<Flt<High>>,
    width: Expr<Flt<High>>,
): Expr<Flt<High>> = 1f - step(max(width, 0.0001f) * 0.5f, abs(distance))

/**
 * Builds a hard-edged stroke around an SDF contour using a literal [width].
 */
public fun stroke(
    distance: Expr<Flt<High>>,
    width: Float,
): Expr<Flt<High>> = stroke(distance, float(width))

/**
 * Fills an SDF shape using the screen-space width of [distance].
 */
public fun softFillScreen(distance: Expr<Flt<High>>): Expr<Flt<High>> {
    val edge = max(fwidth(distance), 0.0001f)
    return 1f - smoothstep(0f, edge, distance)
}

/**
 * Strokes an SDF contour and softens the edge by the screen-space width of [distance].
 */
public fun strokeScreen(
    distance: Expr<Flt<High>>,
    width: Expr<Flt<High>>,
): Expr<Flt<High>> {
    val half = width * 0.5f
    val edge = max(fwidth(distance), 0.0001f)
    return 1f - smoothstep(half, half + edge, abs(distance))
}

/**
 * Builds a soft stroke around an SDF contour.
 *
 * This is the stroke-oriented companion to [softFill] and is a canonical way to turn SDF distance
 * fields into readable outline masks.
 */
public fun softStroke(
    distance: Expr<Flt<High>>,
    width: Expr<Flt<High>>,
    feather: Expr<Flt<High>> = float(0.02f),
): Expr<Flt<High>> {
    val halfWidth = max(width, 0.0001f) * 0.5f
    return 1f - smoothstep(halfWidth, halfWidth + max(feather, 0.0001f), abs(distance))
}

/**
 * Builds a soft stroke around an SDF contour using literal [width] and [feather] values.
 */
public fun softStroke(
    distance: Expr<Flt<High>>,
    width: Float,
    feather: Float = 0.02f,
): Expr<Flt<High>> = softStroke(
    distance = distance,
    width = float(width),
    feather = float(feather),
)
