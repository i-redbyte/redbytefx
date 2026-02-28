package ru.redbyte.redbytefx

class FxDsl internal constructor(
    private val ops: MutableList<Op>,
    private val defaults: MutableMap<FxParam, DefaultValue>
) {
    fun flipX(enabled: Float = 1f): FxParam.Float {
        val p = FxParam.Float()
        defaults[p] = DefaultValue.F(enabled)
        ops.add(OpFlipX(p))
        return p
    }

    fun flipY(enabled: Float = 1f): FxParam.Float {
        val p = FxParam.Float()
        defaults[p] = DefaultValue.F(enabled)
        ops.add(OpFlipY(p))
        return p
    }

    fun mirrorX(enabled: Float = 1f, from: MirrorXFrom = MirrorXFrom.Right): FxParam.Float {
        val p = FxParam.Float()
        defaults[p] = DefaultValue.F(enabled)
        ops.add(OpMirrorX(p, from))
        return p
    }

    fun mirrorXFromRight(enabled: Float = 1f): FxParam.Float = mirrorX(enabled = enabled, from = MirrorXFrom.Right)
    fun mirrorXFromLeft(enabled: Float = 1f): FxParam.Float = mirrorX(enabled = enabled, from = MirrorXFrom.Left)

    fun mirrorY(enabled: Float = 1f, from: MirrorYFrom = MirrorYFrom.Bottom): FxParam.Float {
        val p = FxParam.Float()
        defaults[p] = DefaultValue.F(enabled)
        ops.add(OpMirrorY(p, from))
        return p
    }

    fun mirrorYFromBottom(enabled: Float = 1f): FxParam.Float = mirrorY(enabled = enabled, from = MirrorYFrom.Bottom)
    fun mirrorYFromTop(enabled: Float = 1f): FxParam.Float = mirrorY(enabled = enabled, from = MirrorYFrom.Top)

    fun rotate(deg: Float = 0f, pivot: Pivot = Pivot.Center): FxParam.Float {
        val p = FxParam.Float()
        defaults[p] = DefaultValue.F(deg)
        ops.add(OpRotate(p, pivot))
        return p
    }

    fun scale(sx: Float = 1f, sy: Float = 1f, pivot: Pivot = Pivot.Center): FxParam.Float2 {
        val p = FxParam.Float2()
        defaults[p] = DefaultValue.F2(sx, sy)
        ops.add(OpScale(p, pivot))
        return p
    }

    fun offset(dx: Float = 0f, dy: Float = 0f): FxParam.Float2 {
        val p = FxParam.Float2()
        defaults[p] = DefaultValue.F2(dx, dy)
        ops.add(OpOffset(p))
        return p
    }
}

enum class MirrorXFrom { Left, Right }
enum class MirrorYFrom { Top, Bottom }

sealed class Pivot {
    data object Center : Pivot()
    data class Fraction(val x: Float, val y: Float) : Pivot()
}