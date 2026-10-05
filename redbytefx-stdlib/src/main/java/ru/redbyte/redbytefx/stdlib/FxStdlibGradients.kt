package ru.redbyte.redbytefx.stdlib

import ru.redbyte.redbytefx.*

private fun projectedUv(
    uv: Expr<Vec2<Flt<High>>>,
    direction: Expr<Vec2<Flt<High>>>,
): Expr<Flt<High>> {
    val safeLength = max(length(direction), 0.0001f)
    val dir = direction / safeLength
    val centered = uv - float2(0.5f, 0.5f)
    return centered.x * dir.x + centered.y * dir.y + 0.5f
}

/**
 * Builds a normalized linear ramp from UV space along [direction].
 *
 * The returned value is clamped to the `[0, 1]` range and is useful for directional fades,
 * panel lighting, and beam-like compositing masks. This is the main canonical gradient helper in
 * `stdlib`: pair it with [maskedMix], [alphaMask], or reveal logic before reaching for richer
 * directional effects.
 */
public fun linearRamp(
    uv: Expr<Vec2<Flt<High>>>,
    direction: Expr<Vec2<Flt<High>>> = float2(1f, 0f),
    start: Expr<Flt<High>> = float(0f),
    end: Expr<Flt<High>> = float(1f),
): Expr<Flt<High>> {
    val safeEnd = ifElse(abs(end - start) lt 0.0001f, start + 0.0001f, end)
    return saturate(remap(projectedUv(uv, direction), start, safeEnd, float(0f), float(1f)))
}

/**
 * Builds a normalized linear ramp using literal [start] and [end] values.
 */
public fun linearRamp(
    uv: Expr<Vec2<Flt<High>>>,
    direction: Expr<Vec2<Flt<High>>> = float2(1f, 0f),
    start: Float,
    end: Float,
): Expr<Flt<High>> = linearRamp(
    uv = uv,
    direction = direction,
    start = float(start),
    end = float(end),
)

/**
 * Builds a radial ramp around [center].
 *
 * The result stays near `1` inside [innerRadius] and fades toward `0` at [outerRadius]. Treat it
 * as the radial companion to [linearRamp] for soft falloff, glow, or vignette-style masks.
 */
public fun radialRamp(
    uv: Expr<Vec2<Flt<High>>>,
    center: Expr<Vec2<Flt<High>>> = float2(0.5f, 0.5f),
    innerRadius: Expr<Flt<High>>,
    outerRadius: Expr<Flt<High>>,
): Expr<Flt<High>> {
    val safeInner = max(innerRadius, 0f)
    val safeOuter = max(outerRadius, safeInner + 0.0001f)
    return 1f - smoothstep(safeInner, safeOuter, length(uv - center))
}

/**
 * Builds a radial ramp around [center] using literal radii.
 */
public fun radialRamp(
    uv: Expr<Vec2<Flt<High>>>,
    center: Expr<Vec2<Flt<High>>> = float2(0.5f, 0.5f),
    innerRadius: Float,
    outerRadius: Float,
): Expr<Flt<High>> = radialRamp(
    uv = uv,
    center = center,
    innerRadius = float(innerRadius),
    outerRadius = float(outerRadius),
)

/**
 * Creates a soft directional sweep band from UV space.
 *
 * [center] controls the sweep position along the projected direction, while [width] and
 * [feather] control the visible body and softness of the band. This is a richer second-step
 * helper built on the same mental model as [linearRamp] + [bandMask], rather than the first
 * gradient helper to teach.
 */
public fun directionalSweep(
    uv: Expr<Vec2<Flt<High>>>,
    direction: Expr<Vec2<Flt<High>>> = float2(1f, 0f),
    center: Expr<Flt<High>>,
    width: Expr<Flt<High>>,
    feather: Expr<Flt<High>> = float(0.08f),
): Expr<Flt<High>> = bandMask(
    position = projectedUv(uv, direction),
    center = center,
    width = width,
    feather = feather,
)

/**
 * Creates a soft directional sweep band using expression-driven center and width values with a
 * literal [feather].
 */
public fun directionalSweep(
    uv: Expr<Vec2<Flt<High>>>,
    direction: Expr<Vec2<Flt<High>>> = float2(1f, 0f),
    center: Expr<Flt<High>>,
    width: Expr<Flt<High>>,
    feather: Float,
): Expr<Flt<High>> = directionalSweep(
    uv = uv,
    direction = direction,
    center = center,
    width = width,
    feather = float(feather),
)

/**
 * Creates a soft directional sweep band using an expression-driven [center] with literal width
 * and feather values.
 */
public fun directionalSweep(
    uv: Expr<Vec2<Flt<High>>>,
    direction: Expr<Vec2<Flt<High>>> = float2(1f, 0f),
    center: Expr<Flt<High>>,
    width: Float,
    feather: Float = 0.08f,
): Expr<Flt<High>> = directionalSweep(
    uv = uv,
    direction = direction,
    center = center,
    width = float(width),
    feather = float(feather),
)

/**
 * Creates a soft directional sweep band using literal center, width, and feather values.
 */
public fun directionalSweep(
    uv: Expr<Vec2<Flt<High>>>,
    direction: Expr<Vec2<Flt<High>>> = float2(1f, 0f),
    center: Float,
    width: Float,
    feather: Float = 0.08f,
): Expr<Flt<High>> = directionalSweep(
    uv = uv,
    direction = direction,
    center = float(center),
    width = float(width),
    feather = float(feather),
)
