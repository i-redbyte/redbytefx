package ru.redbyte.redbytefx.stdlib

import ru.redbyte.redbytefx.*

/**
 * Builds a horizontal reveal mask from normalized UV coordinates.
 *
 * [progress] is normalized to the `[0, 1]` range. When [fromLeft] is `true`, the reveal expands
 * from the left edge; otherwise it expands from the right edge. This is one of the canonical
 * reveal helpers to teach before richer transition or scene-graph-like flows.
 */
public fun horizontalReveal(
    uv: Expr<Vec2<Flt<High>>>,
    progress: Expr<Flt<High>>,
    feather: Expr<Flt<High>> = float(0.05f),
    fromLeft: Boolean = true,
): Expr<Flt<High>> {
    val safeFeather = max(feather, 0.0001f)
    val axis = if (fromLeft) uv.x else 1f - uv.x
    val t = saturate(progress)
    return smoothstep(axis, axis + safeFeather, t)
}

/**
 * Builds a horizontal reveal mask using literal [progress] and [feather] values.
 */
public fun horizontalReveal(
    uv: Expr<Vec2<Flt<High>>>,
    progress: Float,
    feather: Float = 0.05f,
    fromLeft: Boolean = true,
): Expr<Flt<High>> = horizontalReveal(
    uv = uv,
    progress = float(progress),
    feather = float(feather),
    fromLeft = fromLeft,
)

/**
 * Builds a horizontal reveal mask using an expression-driven [progress] and a literal [feather].
 */
public fun horizontalReveal(
    uv: Expr<Vec2<Flt<High>>>,
    progress: Expr<Flt<High>>,
    feather: Float,
    fromLeft: Boolean = true,
): Expr<Flt<High>> = horizontalReveal(
    uv = uv,
    progress = progress,
    feather = float(feather),
    fromLeft = fromLeft,
)

/**
 * Builds a vertical reveal mask from normalized UV coordinates.
 *
 * [progress] is normalized to the `[0, 1]` range. When [fromTop] is `true`, the reveal expands
 * from the top edge; otherwise it expands from the bottom edge. Treat it as the vertical companion
 * to [horizontalReveal] in the same canonical reveal family.
 */
public fun verticalReveal(
    uv: Expr<Vec2<Flt<High>>>,
    progress: Expr<Flt<High>>,
    feather: Expr<Flt<High>> = float(0.05f),
    fromTop: Boolean = true,
): Expr<Flt<High>> {
    val safeFeather = max(feather, 0.0001f)
    val axis = if (fromTop) uv.y else 1f - uv.y
    val t = saturate(progress)
    return smoothstep(axis, axis + safeFeather, t)
}

/**
 * Builds a vertical reveal mask using literal [progress] and [feather] values.
 */
public fun verticalReveal(
    uv: Expr<Vec2<Flt<High>>>,
    progress: Float,
    feather: Float = 0.05f,
    fromTop: Boolean = true,
): Expr<Flt<High>> = verticalReveal(
    uv = uv,
    progress = float(progress),
    feather = float(feather),
    fromTop = fromTop,
)

/**
 * Builds a vertical reveal mask using an expression-driven [progress] and a literal [feather].
 */
public fun verticalReveal(
    uv: Expr<Vec2<Flt<High>>>,
    progress: Expr<Flt<High>>,
    feather: Float,
    fromTop: Boolean = true,
): Expr<Flt<High>> = verticalReveal(
    uv = uv,
    progress = progress,
    feather = float(feather),
    fromTop = fromTop,
)

/**
 * Builds a radial reveal mask from normalized UV coordinates.
 *
 * [progress] is normalized to the `[0, 1]` range and scales [maxRadius]. This is the circular
 * companion to [horizontalReveal] and [verticalReveal] when the reveal wants to read as a growing
 * focus region instead of an edge wipe.
 */
public fun radialReveal(
    uv: Expr<Vec2<Flt<High>>>,
    progress: Expr<Flt<High>>,
    center: Expr<Vec2<Flt<High>>> = float2(0.5f, 0.5f),
    feather: Expr<Flt<High>> = float(0.08f),
    maxRadius: Expr<Flt<High>> = float(0.85f),
): Expr<Flt<High>> = circleMask(
    uv = uv,
    center = center,
    radius = maxRadius * saturate(progress),
    feather = feather,
)

/**
 * Builds a radial reveal mask using literal [progress], [feather], and [maxRadius] values.
 */
public fun radialReveal(
    uv: Expr<Vec2<Flt<High>>>,
    progress: Float,
    center: Expr<Vec2<Flt<High>>> = float2(0.5f, 0.5f),
    feather: Float = 0.08f,
    maxRadius: Float = 0.85f,
): Expr<Flt<High>> = radialReveal(
    uv = uv,
    progress = float(progress),
    center = center,
    feather = float(feather),
    maxRadius = float(maxRadius),
)

/**
 * Builds a radial reveal mask using an expression-driven [progress] and literal radius settings.
 */
public fun radialReveal(
    uv: Expr<Vec2<Flt<High>>>,
    progress: Expr<Flt<High>>,
    center: Expr<Vec2<Flt<High>>> = float2(0.5f, 0.5f),
    feather: Float = 0.08f,
    maxRadius: Float = 0.85f,
): Expr<Flt<High>> = radialReveal(
    uv = uv,
    progress = progress,
    center = center,
    feather = float(feather),
    maxRadius = float(maxRadius),
)
