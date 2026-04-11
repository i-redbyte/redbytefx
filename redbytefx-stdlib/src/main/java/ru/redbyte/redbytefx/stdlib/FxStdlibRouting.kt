package ru.redbyte.redbytefx.stdlib

import ru.redbyte.redbytefx.*

/**
 * Creates a soft routed trace mask along the segment between [start] and [end].
 *
 * This is useful for PCB-like paths, sci-fi wiring, and other layout helpers where a segment
 * should read as a lit trace rather than as a generic SDF stroke. [point], [start], and [end]
 * should all live in the same local coordinate system, often an [aspectCenteredUv]-style board
 * space for authored scenes.
 */
public fun segmentMask(
    point: Float2Expr,
    start: Float2Expr,
    end: Float2Expr,
    thickness: FloatExpr,
    feather: FloatExpr = float(0.02f)
): FloatExpr = softStroke(
    distance = sdSegment(point = point, start = start, end = end),
    width = thickness,
    feather = feather
)

/**
 * Creates a soft routed trace mask using literal [thickness] and [feather] values.
 */
public fun segmentMask(
    point: Float2Expr,
    start: Float2Expr,
    end: Float2Expr,
    thickness: Float,
    feather: Float = 0.02f
): FloatExpr = segmentMask(
    point = point,
    start = start,
    end = end,
    thickness = float(thickness),
    feather = float(feather)
)

/**
 * Returns the normalized projection of [point] onto the segment between [start] and [end].
 *
 * The result is clamped to the `[0, 1]` range, which makes it useful for routing logic,
 * directional pulses, and segment-based reveals. This is the canonical routing helper to inspect
 * first before reaching for the more stylized [segmentPulse].
 */
public fun segmentProgress(
    point: Float2Expr,
    start: Float2Expr,
    end: Float2Expr
): FloatExpr {
    val local = point - start
    val segment = end - start
    return clamp(dot(local, segment) / max(dot(segment, segment), 0.0001f), 0f, 1f)
}

/**
 * Creates a moving pulse constrained to the segment between [start] and [end].
 *
 * [phase] is expected to advance over time, typically via `fract(time * speed)`. The returned
 * value already includes the trace mask, so it can be mixed directly into color or mask logic.
 * Treat this as the animated companion to [segmentMask], not as a replacement for understanding
 * the underlying segment space.
 */
public fun segmentPulse(
    point: Float2Expr,
    start: Float2Expr,
    end: Float2Expr,
    phase: FloatExpr,
    bandWidth: FloatExpr,
    thickness: FloatExpr,
    bandFeather: FloatExpr = float(0.08f),
    feather: FloatExpr = float(0.02f)
): FloatExpr {
    val safeBandWidth = max(bandWidth, 0.0001f)
    val safeBandFeather = max(bandFeather, 0.0001f)
    val progress = segmentProgress(point = point, start = start, end = end)
    val distance = abs(fract(progress - phase + 0.5f) - 0.5f)
    val band = 1f - smoothstep(
        safeBandWidth * 0.5f,
        safeBandWidth * 0.5f + safeBandFeather,
        distance
    )
    return band * segmentMask(
        point = point,
        start = start,
        end = end,
        thickness = thickness,
        feather = feather
    )
}

/**
 * Creates a moving segment pulse using literal band and stroke values.
 */
public fun segmentPulse(
    point: Float2Expr,
    start: Float2Expr,
    end: Float2Expr,
    phase: FloatExpr,
    bandWidth: Float,
    thickness: Float,
    bandFeather: Float = 0.08f,
    feather: Float = 0.02f
): FloatExpr = segmentPulse(
    point = point,
    start = start,
    end = end,
    phase = phase,
    bandWidth = float(bandWidth),
    thickness = float(thickness),
    bandFeather = float(bandFeather),
    feather = float(feather)
)
