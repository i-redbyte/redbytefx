package ru.redbyte.redbytefx.stdlib

import ru.redbyte.redbytefx.*

private const val TAU: Float = 6.2831855f

/**
 * Builds a cosine palette from a scalar [tone].
 *
 * [bias], [amplitude], [frequency], and [phase] follow the familiar procedural-art cosine palette
 * convention. The returned RGB values are not clamped automatically. This is a style-oriented
 * palette helper, not part of the first canonical authoring path.
 */
public fun cosinePalette(
    tone: Expr<Flt<High>>,
    bias: Expr<Vec3<Flt<High>>> = float3(0.5f, 0.5f, 0.5f),
    amplitude: Expr<Vec3<Flt<High>>> = float3(0.5f, 0.5f, 0.5f),
    frequency: Expr<Vec3<Flt<High>>> = float3(1f, 1f, 1f),
    phase: Expr<Vec3<Flt<High>>> = float3(0f, 0.33f, 0.67f),
): Expr<Vec3<Flt<High>>> = float3(
    bias.x + amplitude.x * cos(TAU * (frequency.x * tone + phase.x)),
    bias.y + amplitude.y * cos(TAU * (frequency.y * tone + phase.y)),
    bias.z + amplitude.z * cos(TAU * (frequency.z * tone + phase.z)),
)

/**
 * Builds a cosine palette using literal vector parameters.
 */
public fun cosinePalette(
    tone: Expr<Flt<High>>,
    bias: Expr<Vec3<Flt<High>>>,
    amplitude: Expr<Vec3<Flt<High>>>,
    frequency: Expr<Vec3<Flt<High>>>,
    phaseX: Float,
    phaseY: Float,
    phaseZ: Float,
): Expr<Vec3<Flt<High>>> = cosinePalette(
    tone = tone,
    bias = bias,
    amplitude = amplitude,
    frequency = frequency,
    phase = float3(phaseX, phaseY, phaseZ),
)

/**
 * Samples the input content with per-channel offsets and mixes the shifted result back into the
 * original content by [amount].
 *
 * [offset] is interpreted in sample-space units. [direction] is normalized internally so that
 * diagonal offsets stay consistent with horizontal/vertical ones. This helper is intentionally
 * secondary/exploratory: it is useful for stylized distortion once the author already has a clear
 * base sampling path. [amount] is saturated to the `[0, 1]` range so the result behaves like a
 * readable distortion intensity instead of a `mix(...)` overshoot.
 */
public fun FragmentDsl.chromaticOffset(
    offset: Expr<Flt<High>>,
    direction: Expr<Vec2<Flt<High>>> = float2(1f, 0f),
    amount: Expr<Flt<High>> = float(1f),
    coord: Expr<Vec2<Flt<High>>> = fragCoord,
): Expr<Vec4<Flt<Med>>> {
    val safeDirectionLength = max(length(direction), 0.0001f)
    val delta = direction / safeDirectionLength * offset
    val base = sample(coord)
    val shifted = ru.redbyte.redbytefx.color(
        sample(coord - delta).r,
        base.g,
        sample(coord + delta).b,
        base.a,
    )
    return mix(base, shifted, saturate(amount))
}

/**
 * Samples the input content with per-channel offsets using literal [offset] and [amount] values.
 */
public fun FragmentDsl.chromaticOffset(
    offset: Float,
    direction: Expr<Vec2<Flt<High>>> = float2(1f, 0f),
    amount: Float = 1f,
    coord: Expr<Vec2<Flt<High>>> = fragCoord,
): Expr<Vec4<Flt<Med>>> = chromaticOffset(
    offset = float(offset),
    direction = direction,
    amount = float(amount),
    coord = coord,
)

/**
 * Samples the input content with a literal [offset] and an expression-driven [amount].
 */
public fun FragmentDsl.chromaticOffset(
    offset: Float,
    direction: Expr<Vec2<Flt<High>>> = float2(1f, 0f),
    amount: Expr<Flt<High>>,
    coord: Expr<Vec2<Flt<High>>> = fragCoord,
): Expr<Vec4<Flt<Med>>> = chromaticOffset(
    offset = float(offset),
    direction = direction,
    amount = amount,
    coord = coord,
)

/**
 * Samples the input content with an expression-driven [offset] and a literal [amount].
 */
public fun FragmentDsl.chromaticOffset(
    offset: Expr<Flt<High>>,
    direction: Expr<Vec2<Flt<High>>> = float2(1f, 0f),
    amount: Float,
    coord: Expr<Vec2<Flt<High>>> = fragCoord,
): Expr<Vec4<Flt<Med>>> = chromaticOffset(
    offset = offset,
    direction = direction,
    amount = float(amount),
    coord = coord,
)
