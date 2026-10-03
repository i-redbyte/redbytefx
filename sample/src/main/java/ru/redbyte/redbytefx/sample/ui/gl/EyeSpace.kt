package ru.redbyte.redbytefx.sample.ui.gl

import ru.redbyte.redbytefx.HighVec4
import ru.redbyte.redbytefx.lit
import ru.redbyte.redbytefx.minus
import ru.redbyte.redbytefx.times
import ru.redbyte.redbytefx.vec4
import ru.redbyte.redbytefx.x
import ru.redbyte.redbytefx.y
import ru.redbyte.redbytefx.z

internal fun projectEye(point: HighVec4): HighVec4 =
    vec4(point.x * 0.92f.lit, point.y * 0.92f.lit, point.z * 0.25f.lit - 0.2f.lit, point.z)
