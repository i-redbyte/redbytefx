package ru.redbyte.redbytefx

public enum class MirrorXFrom(public val shaderValue: Float) {
    Left(0f),
    Right(1f),
}

public enum class MirrorYFrom(public val shaderValue: Float) {
    Top(0f),
    Bottom(1f),
}

public fun FragmentDsl.center(): Expr<Vec2<Flt<High>>> = resolution * 0.5f

public fun FragmentDsl.flipX(amount: Expr<Flt<High>>): Expr<Vec2<Flt<High>>> = flipX(fragCoord, amount)

public fun FragmentDsl.flipX(
    coord: Expr<Vec2<Flt<High>>>,
    amount: Expr<Flt<High>>,
): Expr<Vec2<Flt<High>>> = float2(mix(coord.x, resolution.x - coord.x, saturate(amount)), coord.y)

public fun FragmentDsl.flipY(
    coord: Expr<Vec2<Flt<High>>>,
    amount: Expr<Flt<High>>,
): Expr<Vec2<Flt<High>>> = float2(coord.x, mix(coord.y, resolution.y - coord.y, saturate(amount)))

public fun FragmentDsl.mirrorX(
    amount: Expr<Flt<High>>,
    from: Expr<Flt<High>>,
): Expr<Vec2<Flt<High>>> = mirrorX(fragCoord, amount, from)

public fun FragmentDsl.mirrorX(
    coord: Expr<Vec2<Flt<High>>>,
    amount: Expr<Flt<High>>,
    from: Expr<Flt<High>>,
): Expr<Vec2<Flt<High>>> {
    val direction = from * 2f - 1f
    val centerX = resolution.x * 0.5f
    val mirroredX = centerX + direction * abs(coord.x - centerX)
    return float2(mix(coord.x, mirroredX, saturate(amount)), coord.y)
}

public fun FragmentDsl.mirrorY(
    coord: Expr<Vec2<Flt<High>>>,
    amount: Expr<Flt<High>>,
    from: Expr<Flt<High>>,
): Expr<Vec2<Flt<High>>> {
    val direction = from * 2f - 1f
    val centerY = resolution.y * 0.5f
    val mirroredY = centerY + direction * abs(coord.y - centerY)
    return float2(coord.x, mix(coord.y, mirroredY, saturate(amount)))
}

public fun FragmentDsl.scale(factor: Expr<Vec2<Flt<High>>>): Expr<Vec2<Flt<High>>> =
    scale(fragCoord, factor)

public fun FragmentDsl.scale(
    coord: Expr<Vec2<Flt<High>>>,
    factor: Expr<Vec2<Flt<High>>>,
): Expr<Vec2<Flt<High>>> {
    val safeScale = float2(safeScaleComponent(factor.x), safeScaleComponent(factor.y))
    return center() + (coord - center()) / safeScale
}

private fun safeScaleComponent(factor: Expr<Flt<High>>): Expr<Flt<High>> {
    val magnitude = max(abs(factor), 0.0001f)
    return ifElse(factor lt 0f.lit, -magnitude, magnitude)
}

public fun FragmentDsl.offset(delta: Expr<Vec2<Flt<High>>>): Expr<Vec2<Flt<High>>> = fragCoord - delta
