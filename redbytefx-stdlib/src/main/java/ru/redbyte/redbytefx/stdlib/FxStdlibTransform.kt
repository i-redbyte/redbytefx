package ru.redbyte.redbytefx.stdlib

import ru.redbyte.redbytefx.Expr
import ru.redbyte.redbytefx.Flt
import ru.redbyte.redbytefx.High
import ru.redbyte.redbytefx.Vec2
import ru.redbyte.redbytefx.cos
import ru.redbyte.redbytefx.float
import ru.redbyte.redbytefx.float2
import ru.redbyte.redbytefx.minus
import ru.redbyte.redbytefx.plus
import ru.redbyte.redbytefx.sin
import ru.redbyte.redbytefx.times
import ru.redbyte.redbytefx.x
import ru.redbyte.redbytefx.y

/**
 * Rotates [point] around the origin by [angle] radians (counter-clockwise in XY).
 */
public fun rotate2d(
    point: Expr<Vec2<Flt<High>>>,
    angle: Expr<Flt<High>>,
): Expr<Vec2<Flt<High>>> {
    val c = cos(angle)
    val s = sin(angle)
    return float2(c * point.x - s * point.y, s * point.x + c * point.y)
}

/** Rotates [point] around the origin by a literal [angle] in radians. */
public fun rotate2d(
    point: Expr<Vec2<Flt<High>>>,
    angle: Float,
): Expr<Vec2<Flt<High>>> = rotate2d(point, float(angle))
