package ru.redbyte.redbytefx.stdlib

import ru.redbyte.redbytefx.*

/**
 * Builds fractal Brownian motion from [valueNoise].
 *
 * [octaves] is clamped to the `1..6` range to keep generated shader code small and predictable.
 * This helper is intentionally broader and more exploratory than the first canonical starter path:
 * it is useful once the author already understands the simpler coordinate/mask/compositing flow.
 */
public fun fbm(
    point: Float2Expr,
    octaves: Int = 4,
    lacunarity: FloatExpr = float(2f),
    gain: FloatExpr = float(0.5f)
): FloatExpr {
    val safeOctaves = octaves.coerceIn(1, 6)
    var sum: FloatExpr = float(0f)
    var amplitude: FloatExpr = float(0.5f)
    var frequency: FloatExpr = float(1f)

    repeat(safeOctaves) {
        sum += valueNoise(point * frequency) * amplitude
        frequency *= lacunarity
        amplitude *= gain
    }

    return sum
}

/**
 * Builds fractal Brownian motion using literal [lacunarity] and [gain] values.
 */
public fun fbm(
    point: Float2Expr,
    octaves: Int,
    lacunarity: Float,
    gain: Float
): FloatExpr = fbm(
    point = point,
    octaves = octaves,
    lacunarity = float(lacunarity),
    gain = float(gain)
)

/**
 * Applies a lightweight domain warp to [point] using two fBm fields.
 *
 * [amount] controls how strongly the input space is bent. Treat this as a style-building helper
 * rather than a first-teaching-surface primitive; it is most useful after the shader already reads
 * clearly in ordinary UV or local coordinate space.
 */
public fun domainWarp(
    point: Float2Expr,
    time: FloatExpr = float(0f),
    amount: FloatExpr = float(0.35f)
): Float2Expr {
    val q = float2(
        fbm(point + float2(time * 0.11f + 1.7f, 9.2f)),
        fbm(point + float2(8.3f, time * 0.13f + 2.8f))
    )
    return point + (q * 2f - float2(1f, 1f)) * amount
}

/**
 * Applies a lightweight domain warp using literal time and amount values.
 */
public fun domainWarp(
    point: Float2Expr,
    time: Float,
    amount: Float
): Float2Expr = domainWarp(
    point = point,
    time = float(time),
    amount = float(amount)
)

/**
 * Applies a lightweight domain warp using an animated time expression and a literal amount.
 */
public fun domainWarp(
    point: Float2Expr,
    time: FloatExpr,
    amount: Float
): Float2Expr = domainWarp(
    point = point,
    time = time,
    amount = float(amount)
)
