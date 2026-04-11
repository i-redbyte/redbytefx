package ru.redbyte.redbytefx.stdlib

import ru.redbyte.redbytefx.*

/**
 * Creates a soft one-dimensional band around [center].
 *
 * This is useful for scan windows, lock bands, and other effects that need a reusable
 * horizontal or vertical mask instead of ad-hoc `abs(...)` and `smoothstep(...)` chains. This is
 * the canonical one-dimensional mask helper to teach before richer directional or repeated signal
 * patterns.
 */
public fun bandMask(
    position: FloatExpr,
    center: FloatExpr,
    width: FloatExpr,
    feather: FloatExpr = float(0.05f)
): FloatExpr {
    val safeWidth = max(width, 0.0001f)
    val safeFeather = max(feather, 0.0001f)
    val halfWidth = safeWidth * 0.5f
    val distance = abs(position - center)
    return 1f - smoothstep(halfWidth, halfWidth + safeFeather, distance)
}

/**
 * Creates a soft one-dimensional band around [center] using literal width and feather values.
 */
public fun bandMask(
    position: FloatExpr,
    center: FloatExpr,
    width: Float,
    feather: Float = 0.05f
): FloatExpr = bandMask(
    position = position,
    center = center,
    width = float(width),
    feather = float(feather)
)

/**
 * Creates a soft one-dimensional band around a literal [center].
 */
public fun bandMask(
    position: FloatExpr,
    center: Float,
    width: Float,
    feather: Float = 0.05f
): FloatExpr = bandMask(
    position = position,
    center = float(center),
    width = float(width),
    feather = float(feather)
)

/**
 * Creates repeated soft bars along a single axis.
 *
 * [density] controls how many bars appear across the normalized range, [width] controls the
 * visible body of each bar, and [phase] scrolls the pattern over time or space. Treat this as the
 * repeated/animated companion to [bandMask], not as a replacement for first understanding the
 * simpler band itself.
 */
public fun signalBars(
    position: FloatExpr,
    density: FloatExpr,
    width: FloatExpr = float(0.18f),
    phase: FloatExpr = float(0f),
    feather: FloatExpr = float(0.05f)
): FloatExpr {
    val safeDensity = max(density, 1f)
    val local = fract(position * safeDensity + phase)
    return bandMask(
        position = local,
        center = float(0.5f),
        width = width,
        feather = feather
    )
}

/**
 * Creates repeated soft bars using literal density, width, phase, and feather values.
 */
public fun signalBars(
    position: FloatExpr,
    density: Float,
    width: Float = 0.18f,
    phase: Float = 0f,
    feather: Float = 0.05f
): FloatExpr = signalBars(
    position = position,
    density = float(density),
    width = float(width),
    phase = float(phase),
    feather = float(feather)
)

/**
 * Creates repeated soft bars using an expression [density] and literal width/feather values.
 */
public fun signalBars(
    position: FloatExpr,
    density: FloatExpr,
    width: Float = 0.18f,
    phase: FloatExpr = float(0f),
    feather: Float = 0.05f
): FloatExpr = signalBars(
    position = position,
    density = density,
    width = float(width),
    phase = phase,
    feather = float(feather)
)

/**
 * Applies a lightweight horizontal scan distortion to normalized UV coordinates.
 *
 * The returned UVs stay in normalized space, which makes the helper easy to compose with
 * further sampling, masking, or color processing steps. This is intentionally a secondary
 * distortion helper: use it after the base `normalizedUv() -> sampleUv(...)` sampling path is
 * already explicit and readable.
 */
public fun scanWarp(
    uv: Float2Expr,
    time: FloatExpr = float(0f),
    amplitude: FloatExpr = float(0.03f),
    density: FloatExpr = float(10f),
    speed: FloatExpr = float(1f),
    noiseAmount: FloatExpr = float(0.35f)
): Float2Expr {
    val safeAmplitude = max(amplitude, 0f)
    val safeDensity = max(density, 1f)
    val phase = uv.y * safeDensity * 6.2831855f + time * speed
    val wave = sin(phase)
    val noise = valueNoise(
        float2(
            uv.y * safeDensity * 0.85f + time * speed * 0.21f,
            time * speed * 0.13f + 4.7f
        )
    ) * 2f - 1f
    val offset = (wave * 0.65f + noise * noiseAmount) * safeAmplitude
    return uv + float2(offset, 0f)
}

/**
 * Applies a lightweight horizontal scan distortion using literal amplitude, density, speed,
 * and noise amount values.
 */
public fun scanWarp(
    uv: Float2Expr,
    time: FloatExpr = float(0f),
    amplitude: Float = 0.03f,
    density: Float = 10f,
    speed: Float = 1f,
    noiseAmount: Float = 0.35f
): Float2Expr = scanWarp(
    uv = uv,
    time = time,
    amplitude = float(amplitude),
    density = float(density),
    speed = float(speed),
    noiseAmount = float(noiseAmount)
)

/**
 * Applies a lightweight horizontal scan distortion using expression-driven amplitude and density
 * with literal speed and noise amount values.
 */
public fun scanWarp(
    uv: Float2Expr,
    time: FloatExpr = float(0f),
    amplitude: FloatExpr,
    density: FloatExpr,
    speed: Float = 1f,
    noiseAmount: Float = 0.35f
): Float2Expr = scanWarp(
    uv = uv,
    time = time,
    amplitude = amplitude,
    density = density,
    speed = float(speed),
    noiseAmount = float(noiseAmount)
)
