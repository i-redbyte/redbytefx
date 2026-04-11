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
    tone: FloatExpr,
    bias: Float3Expr = float3(0.5f, 0.5f, 0.5f),
    amplitude: Float3Expr = float3(0.5f, 0.5f, 0.5f),
    frequency: Float3Expr = float3(1f, 1f, 1f),
    phase: Float3Expr = float3(0f, 0.33f, 0.67f)
): Float3Expr = float3(
    bias.x + amplitude.x * cos(TAU * (frequency.x * tone + phase.x)),
    bias.y + amplitude.y * cos(TAU * (frequency.y * tone + phase.y)),
    bias.z + amplitude.z * cos(TAU * (frequency.z * tone + phase.z))
)

/**
 * Builds a cosine palette using literal vector parameters.
 */
public fun cosinePalette(
    tone: FloatExpr,
    bias: Float3Expr,
    amplitude: Float3Expr,
    frequency: Float3Expr,
    phaseX: Float,
    phaseY: Float,
    phaseZ: Float
): Float3Expr = cosinePalette(
    tone = tone,
    bias = bias,
    amplitude = amplitude,
    frequency = frequency,
    phase = float3(phaseX, phaseY, phaseZ)
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
public fun FxDsl.chromaticOffset(
    offset: FloatExpr,
    direction: Float2Expr = float2(1f, 0f),
    amount: FloatExpr = float(1f),
    coord: Float2Expr = fragCoord
): ColorExpr {
    val safeDirectionLength = max(length(direction), 0.0001f)
    val delta = direction / safeDirectionLength * offset
    val base = sample(coord)
    val shifted = ru.redbyte.redbytefx.color(
        sample(coord - delta).r,
        base.g,
        sample(coord + delta).b,
        base.a
    )
    return mix(base, shifted, saturate(amount))
}

/**
 * Samples the input content with per-channel offsets using literal [offset] and [amount] values.
 */
public fun FxDsl.chromaticOffset(
    offset: Float,
    direction: Float2Expr = float2(1f, 0f),
    amount: Float = 1f,
    coord: Float2Expr = fragCoord
): ColorExpr = chromaticOffset(
    offset = float(offset),
    direction = direction,
    amount = float(amount),
    coord = coord
)

/**
 * Samples the input content with a literal [offset] and an expression-driven [amount].
 */
public fun FxDsl.chromaticOffset(
    offset: Float,
    direction: Float2Expr = float2(1f, 0f),
    amount: FloatExpr,
    coord: Float2Expr = fragCoord
): ColorExpr = chromaticOffset(
    offset = float(offset),
    direction = direction,
    amount = amount,
    coord = coord
)

/**
 * Samples the input content with an expression-driven [offset] and a literal [amount].
 */
public fun FxDsl.chromaticOffset(
    offset: FloatExpr,
    direction: Float2Expr = float2(1f, 0f),
    amount: Float,
    coord: Float2Expr = fragCoord
): ColorExpr = chromaticOffset(
    offset = offset,
    direction = direction,
    amount = float(amount),
    coord = coord
)
