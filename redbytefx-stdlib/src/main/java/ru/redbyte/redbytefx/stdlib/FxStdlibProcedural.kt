package ru.redbyte.redbytefx.stdlib

import ru.redbyte.redbytefx.*

/**
 * Computes a lightweight scalar hash from a `float2` position.
 *
 * The result is in the `[0, 1)` range and is intended for procedural modulation, not for
 * cryptographic or statistically rigorous randomness. Treat it as a procedural support primitive,
 * not as part of the first canonical authoring path.
 */
public fun hash21(point: Expr<Vec2<Flt<High>>>): Expr<Flt<High>> =
    fract(sin(point.x * 127.1f + point.y * 311.7f) * 43758.5453f)

/**
 * Two-channel hash in `[0, 1)` from a `float2` cell.
 */
public fun hash22(point: Expr<Vec2<Flt<High>>>): Expr<Vec2<Flt<High>>> {
    val hashed = float2(
        dot(point, float2(127.1f, 311.7f)),
        dot(point, float2(269.5f, 183.3f)),
    )
    return fract(sin(hashed) * 43758.5453f)
}

/**
 * Nearest-feature Voronoi distance in `[0, ~1.5]` from a `float2` domain.
 */
public fun voronoi(point: Expr<Vec2<Flt<High>>>): Expr<Flt<High>> {
    val cell = floor(point)
    val local = fract(point)
    var nearest = float(8f)
    for (gy in -1..1) {
        for (gx in -1..1) {
            val offset = float2(gx.toFloat(), gy.toFloat())
            val random = hash22(cell + offset)
            val delta = offset + random - local
            nearest = min(nearest, dot(delta, delta))
        }
    }
    return sqrt(nearest)
}

/**
 * Computes value noise from a `float2` position.
 *
 * The returned value is smoothly interpolated in the `[0, 1]` range. This is useful when a shader
 * is already deliberately entering procedural territory, but it should not crowd out the simpler
 * canonical mask/ramp/shape helpers in the first teaching pass.
 */
public fun valueNoise(point: Expr<Vec2<Flt<High>>>): Expr<Flt<High>> {
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
 * [scale] controls how dense the grain becomes across UV space. This is a secondary style helper
 * for texture/noise passes once the main scene logic is already readable.
 */
public fun grain(
    uv: Expr<Vec2<Flt<High>>>,
    time: Expr<Flt<High>> = float(0f),
    scale: Expr<Flt<High>> = float(180f),
): Expr<Flt<High>> {
    val safeScale = max(scale, 1f)
    val t = fract(time)
    val animatedUv = uv * safeScale + float2(t * 19.19f, t * 37.73f)
    return hash21(animatedUv) * 2f - 1f
}

/**
 * Builds centered grain in the `[-1, 1]` range using literal time and scale values.
 */
public fun grain(
    uv: Expr<Vec2<Flt<High>>>,
    time: Float,
    scale: Float,
): Expr<Flt<High>> = grain(
    uv = uv,
    time = float(time),
    scale = float(scale),
)

/**
 * Builds centered grain in the `[-1, 1]` range using a time expression and a literal scale.
 */
public fun grain(
    uv: Expr<Vec2<Flt<High>>>,
    time: Expr<Flt<High>>,
    scale: Float,
): Expr<Flt<High>> = grain(
    uv = uv,
    time = time,
    scale = float(scale),
)

/**
 * Builds a radial vignette mask from normalized UV coordinates.
 *
 * The result stays near `1` at the center and fades toward `0` near the edges. This is useful for
 * final framing or grading passes, not as the first mask helper to teach.
 */
public fun vignette(
    uv: Expr<Vec2<Flt<High>>>,
    innerRadius: Expr<Flt<High>>,
    outerRadius: Expr<Flt<High>>,
): Expr<Flt<High>> {
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
    uv: Expr<Vec2<Flt<High>>>,
    innerRadius: Float,
    outerRadius: Float,
): Expr<Flt<High>> = vignette(
    uv = uv,
    innerRadius = float(innerRadius),
    outerRadius = float(outerRadius),
)
