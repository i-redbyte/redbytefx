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
    point: Expr<Vec2<Flt<High>>>,
    octaves: Int = 4,
    lacunarity: Expr<Flt<High>> = float(2f),
    gain: Expr<Flt<High>> = float(0.5f),
): Expr<Flt<High>> {
    val safeOctaves = octaves.coerceIn(1, 6)
    var sum: Expr<Flt<High>> = float(0f)
    var amplitude: Expr<Flt<High>> = float(0.5f)
    var frequency: Expr<Flt<High>> = float(1f)

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
    point: Expr<Vec2<Flt<High>>>,
    octaves: Int,
    lacunarity: Float,
    gain: Float,
): Expr<Flt<High>> = fbm(
    point = point,
    octaves = octaves,
    lacunarity = float(lacunarity),
    gain = float(gain),
)

/**
 * Applies a lightweight domain warp to [point] using two fBm fields.
 *
 * [amount] controls how strongly the input space is bent. Treat this as a style-building helper
 * rather than a first-teaching-surface primitive; it is most useful after the shader already reads
 * clearly in ordinary UV or local coordinate space.
 */
public fun domainWarp(
    point: Expr<Vec2<Flt<High>>>,
    time: Expr<Flt<High>> = float(0f),
    amount: Expr<Flt<High>> = float(0.35f),
): Expr<Vec2<Flt<High>>> {
    val q = float2(
        fbm(point + float2(time * 0.11f + 1.7f, 9.2f)),
        fbm(point + float2(8.3f, time * 0.13f + 2.8f)),
    )
    return point + (q * 2f - float2(1f, 1f)) * amount
}

/**
 * Applies a lightweight domain warp using literal time and amount values.
 */
public fun domainWarp(
    point: Expr<Vec2<Flt<High>>>,
    time: Float,
    amount: Float,
): Expr<Vec2<Flt<High>>> = domainWarp(
    point = point,
    time = float(time),
    amount = float(amount),
)

/**
 * Applies a lightweight domain warp using an animated time expression and a literal amount.
 */
public fun domainWarp(
    point: Expr<Vec2<Flt<High>>>,
    time: Expr<Flt<High>>,
    amount: Float,
): Expr<Vec2<Flt<High>>> = domainWarp(
    point = point,
    time = time,
    amount = float(amount),
)
