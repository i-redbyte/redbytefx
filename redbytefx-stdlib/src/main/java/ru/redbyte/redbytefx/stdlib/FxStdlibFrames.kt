package ru.redbyte.redbytefx.stdlib

import ru.redbyte.redbytefx.*

/**
 * Returns the normalized distance from [uv] to the nearest viewport edge.
 *
 * `0` means the point lies directly on an edge, while larger values move toward the center.
 * This is useful for custom panel, frame, and border math built on top of normalized UV space.
 */
public fun edgeDistance(uv: Float2Expr): FloatExpr = min(
    min(uv.x, 1f - uv.x),
    min(uv.y, 1f - uv.y)
)

/**
 * Creates a normalized inner fade from the viewport edges toward the center.
 *
 * The result stays near `0` at the very edge and rises toward `1` as the point moves inward.
 */
public fun edgeFade(
    uv: Float2Expr,
    feather: FloatExpr
): FloatExpr = smoothstep(0f, max(feather, 0.0001f), edgeDistance(uv))

/**
 * Creates a normalized inner fade using a literal [feather] value.
 */
public fun edgeFade(
    uv: Float2Expr,
    feather: Float
): FloatExpr = edgeFade(
    uv = uv,
    feather = float(feather)
)

/**
 * Builds a normalized rectangular frame mask around the viewport edges.
 *
 * [thickness] controls how far the frame extends inward from the edges, while [feather]
 * softens the transition into the interior.
 */
public fun frameMask(
    uv: Float2Expr,
    thickness: FloatExpr,
    feather: FloatExpr = float(0.02f)
): FloatExpr {
    val safeThickness = max(thickness, 0f)
    val safeFeather = max(feather, 0.0001f)
    return 1f - smoothstep(safeThickness, safeThickness + safeFeather, edgeDistance(uv))
}

/**
 * Builds a normalized rectangular frame mask using literal [thickness] and [feather] values.
 */
public fun frameMask(
    uv: Float2Expr,
    thickness: Float,
    feather: Float = 0.02f
): FloatExpr = frameMask(
    uv = uv,
    thickness = float(thickness),
    feather = float(feather)
)

/**
 * Builds a normalized rectangular frame mask using an expression-driven [thickness] and a
 * literal [feather].
 */
public fun frameMask(
    uv: Float2Expr,
    thickness: FloatExpr,
    feather: Float
): FloatExpr = frameMask(
    uv = uv,
    thickness = thickness,
    feather = float(feather)
)

/**
 * Builds a normalized corner-accent mask around the viewport.
 *
 * [size] controls how far each corner accent extends inward along the edges, while [thickness]
 * controls the visible frame body. The helper is useful for bracket-like panel treatments and
 * terminal HUD corners.
 */
public fun cornerMask(
    uv: Float2Expr,
    size: FloatExpr,
    thickness: FloatExpr,
    feather: FloatExpr = float(0.02f)
): FloatExpr {
    val safeSize = max(size, 0.0001f)
    val safeFeather = max(feather, 0.0001f)
    val frame = frameMask(uv, thickness, feather)
    val left = 1f - smoothstep(safeSize, safeSize + safeFeather, uv.x)
    val right = 1f - smoothstep(safeSize, safeSize + safeFeather, 1f - uv.x)
    val top = 1f - smoothstep(safeSize, safeSize + safeFeather, uv.y)
    val bottom = 1f - smoothstep(safeSize, safeSize + safeFeather, 1f - uv.y)
    val corners = max(
        max(left * top, right * top),
        max(left * bottom, right * bottom)
    )
    return frame * corners
}

/**
 * Builds a normalized corner-accent mask using literal [size], [thickness], and [feather] values.
 */
public fun cornerMask(
    uv: Float2Expr,
    size: Float,
    thickness: Float,
    feather: Float = 0.02f
): FloatExpr = cornerMask(
    uv = uv,
    size = float(size),
    thickness = float(thickness),
    feather = float(feather)
)

/**
 * Builds a normalized corner-accent mask using expression-driven [size] and [thickness] with
 * a literal [feather].
 */
public fun cornerMask(
    uv: Float2Expr,
    size: FloatExpr,
    thickness: FloatExpr,
    feather: Float
): FloatExpr = cornerMask(
    uv = uv,
    size = size,
    thickness = thickness,
    feather = float(feather)
)
