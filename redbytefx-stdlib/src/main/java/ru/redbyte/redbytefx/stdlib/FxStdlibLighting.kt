package ru.redbyte.redbytefx.stdlib

import ru.redbyte.redbytefx.*

/**
 * Converts sample-space coordinates into normalized UV space.
 *
 * This is a tiny canonical convenience helper around `fragCoord / resolution` for shaders that
 * want to stay in normalized coordinates for masks, gradients, and procedural math. Reach for it
 * when the shader really wants `[0,1]` UV space; otherwise staying in raw sample space with
 * `fragCoord` often keeps AGSL ports easier to read.
 *
 * Pairs with [sampleUv]: normalized coordinates → resample. Core `FragmentDsl.sample` stays in pixel
 * space.
 */
public fun FragmentDsl.normalizedUv(
    coord: Expr<Vec2<Flt<High>>> = fragCoord,
): Expr<Vec2<Flt<High>>> = coord / float2(
    max(resolution.x, 0.0001f),
    max(resolution.y, 0.0001f),
)

/**
 * Samples the input content from normalized UV coordinates.
 *
 * This is the inverse convenience of [normalizedUv] and expands to `sample(uv * resolution)`.
 * Prefer plain `sample(...)` when the shader is already operating in pixel/sample coordinates, or
 * when normalized UV is only used for masks/gradients while the actual content read still happens
 * at `fragCoord`.
 *
 * Do not pass pixel coordinates or `fragCoord` here - use core `sample(...)` instead.
 */
public fun FragmentDsl.sampleUv(
    uv: Expr<Vec2<Flt<High>>>,
): Expr<Vec4<Flt<Med>>> = sample(
    uv * float2(
        max(resolution.x, 0.0001f),
        max(resolution.y, 0.0001f),
    ),
)

/**
 * Recenters normalized UV coordinates around [center].
 *
 * This is a tiny readability helper for lighting-style shaders that want to work in a coordinate
 * system centered around the focal point instead of the top-left corner.
 */
public fun centeredUv(
    uv: Expr<Vec2<Flt<High>>>,
    center: Expr<Vec2<Flt<High>>> = float2(0.5f, 0.5f),
): Expr<Vec2<Flt<High>>> = uv - center

/**
 * Recenters normalized UV coordinates and applies aspect correction using [resolution].
 *
 * The returned vector keeps radial math visually round on non-square surfaces by scaling the X
 * component according to the current render target aspect ratio. This is the main canonical bridge
 * from normalized UV space into local shape/light space.
 */
public fun aspectCenteredUv(
    uv: Expr<Vec2<Flt<High>>>,
    resolution: Expr<Vec2<Flt<High>>>,
    center: Expr<Vec2<Flt<High>>> = float2(0.5f, 0.5f),
): Expr<Vec2<Flt<High>>> {
    val centered = centeredUv(uv, center)
    val safeHeight = max(resolution.y, 0.0001f)
    val aspect = max(resolution.x / safeHeight, 0.0001f)
    return float2(centered.x * aspect, centered.y)
}

/**
 * Returns the normalized radial direction from [center] in aspect-corrected UV space.
 *
 * This is useful when a lighting recipe needs a stable direction vector for tinting, falloff, or
 * asymmetric highlights around a focal point.
 */
public fun radialDirection(
    uv: Expr<Vec2<Flt<High>>>,
    resolution: Expr<Vec2<Flt<High>>>,
    center: Expr<Vec2<Flt<High>>> = float2(0.5f, 0.5f),
): Expr<Vec2<Flt<High>>> {
    val centered = aspectCenteredUv(uv, resolution, center)
    val safeLength = max(length(centered), 0.0001f)
    return centered / safeLength
}

/**
 * Builds a soft radial glow around [center] in aspect-corrected UV space.
 *
 * The glow stays near `1` inside [radius] and fades toward `0` outside it.
 */
public fun centerGlow(
    uv: Expr<Vec2<Flt<High>>>,
    resolution: Expr<Vec2<Flt<High>>>,
    center: Expr<Vec2<Flt<High>>> = float2(0.5f, 0.5f),
    radius: Expr<Flt<High>>,
    feather: Expr<Flt<High>> = float(0.02f),
): Expr<Flt<High>> {
    val safeRadius = max(radius, 0f)
    val safeFeather = max(feather, 0.0001f)
    val distance = length(aspectCenteredUv(uv, resolution, center))
    return 1f - smoothstep(safeRadius, safeRadius + safeFeather, distance)
}

/**
 * Builds a soft radial glow using literal [radius] and [feather] values.
 */
public fun centerGlow(
    uv: Expr<Vec2<Flt<High>>>,
    resolution: Expr<Vec2<Flt<High>>>,
    center: Expr<Vec2<Flt<High>>> = float2(0.5f, 0.5f),
    radius: Float,
    feather: Float = 0.02f,
): Expr<Flt<High>> = centerGlow(
    uv = uv,
    resolution = resolution,
    center = center,
    radius = float(radius),
    feather = float(feather),
)

/**
 * Builds a soft radial glow using an expression-driven [radius] and a literal [feather].
 */
public fun centerGlow(
    uv: Expr<Vec2<Flt<High>>>,
    resolution: Expr<Vec2<Flt<High>>>,
    center: Expr<Vec2<Flt<High>>> = float2(0.5f, 0.5f),
    radius: Expr<Flt<High>>,
    feather: Float,
): Expr<Flt<High>> = centerGlow(
    uv = uv,
    resolution = resolution,
    center = center,
    radius = radius,
    feather = float(feather),
)

/**
 * Builds a soft aspect-corrected rim light around [center].
 *
 * [radius] controls the ring center while [width] controls the visible light thickness.
 */
public fun rimLight(
    uv: Expr<Vec2<Flt<High>>>,
    resolution: Expr<Vec2<Flt<High>>>,
    center: Expr<Vec2<Flt<High>>> = float2(0.5f, 0.5f),
    radius: Expr<Flt<High>>,
    width: Expr<Flt<High>>,
    feather: Expr<Flt<High>> = float(0.02f),
): Expr<Flt<High>> {
    val safeRadius = max(radius, 0f)
    val safeWidth = max(width, 0.0001f)
    val safeFeather = max(feather, 0.0001f)
    val halfWidth = safeWidth * 0.5f
    val distance = abs(length(aspectCenteredUv(uv, resolution, center)) - safeRadius)
    return 1f - smoothstep(halfWidth, halfWidth + safeFeather, distance)
}

/**
 * Builds a soft aspect-corrected rim light using literal sizing values.
 */
public fun rimLight(
    uv: Expr<Vec2<Flt<High>>>,
    resolution: Expr<Vec2<Flt<High>>>,
    center: Expr<Vec2<Flt<High>>> = float2(0.5f, 0.5f),
    radius: Float,
    width: Float,
    feather: Float = 0.02f,
): Expr<Flt<High>> = rimLight(
    uv = uv,
    resolution = resolution,
    center = center,
    radius = float(radius),
    width = float(width),
    feather = float(feather),
)

/**
 * Builds a soft aspect-corrected rim light using expression-driven sizing with a literal
 * [feather].
 */
public fun rimLight(
    uv: Expr<Vec2<Flt<High>>>,
    resolution: Expr<Vec2<Flt<High>>>,
    center: Expr<Vec2<Flt<High>>> = float2(0.5f, 0.5f),
    radius: Expr<Flt<High>>,
    width: Expr<Flt<High>>,
    feather: Float,
): Expr<Flt<High>> = rimLight(
    uv = uv,
    resolution = resolution,
    center = center,
    radius = radius,
    width = width,
    feather = float(feather),
)

/**
 * Builds a soft aspect-corrected rim light using an expression-driven [radius] with literal
 * [width] and [feather] values.
 */
public fun rimLight(
    uv: Expr<Vec2<Flt<High>>>,
    resolution: Expr<Vec2<Flt<High>>>,
    center: Expr<Vec2<Flt<High>>> = float2(0.5f, 0.5f),
    radius: Expr<Flt<High>>,
    width: Float,
    feather: Float = 0.02f,
): Expr<Flt<High>> = rimLight(
    uv = uv,
    resolution = resolution,
    center = center,
    radius = radius,
    width = float(width),
    feather = float(feather),
)

/**
 * Shade. Lambert weight of a surface. The formula is [ru.redbyte.redbytefx.lambert],
 * so an AGSL effect and a GLES scene share one expression.
 */
public fun lambert(
    normal: Expr<Vec3<Flt<High>>>,
    light: Expr<Vec3<Flt<High>>>,
): Expr<Flt<High>> = ru.redbyte.redbytefx.lambert(normal, light)

/**
 * Shade. [lambert] with a floor so a back-facing surface still has ambient light.
 * [floor] is the unlit weight, typically `0.2`…`0.4`.
 */
public fun wrapLambert(
    normal: Expr<Vec3<Flt<High>>>,
    light: Expr<Vec3<Flt<High>>>,
    floor: Expr<Flt<High>> = float(0.32f),
): Expr<Flt<High>> = max(lambert(normal, light), floor)

/** Shade. [wrapLambert] with a literal floor. */
public fun wrapLambert(
    normal: Expr<Vec3<Flt<High>>>,
    light: Expr<Vec3<Flt<High>>>,
    floor: Float,
): Expr<Flt<High>> = wrapLambert(normal, light, float(floor))
