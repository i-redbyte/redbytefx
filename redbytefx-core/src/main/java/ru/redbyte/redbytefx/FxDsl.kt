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

    fun mirrorX(
        enabled: Float = 1f,
        from: MirrorXFrom = MirrorXFrom.Right
    ): MirrorXParams {
        val en = FxParam.Float()
        val fr = FxParam.Float()
        defaults[en] = DefaultValue.F(enabled)
        defaults[fr] = DefaultValue.F(from.v)
        ops.add(OpMirrorX(en, fr))
        return MirrorXParams(en, fr)
    }

    fun mirrorY(
        enabled: Float = 1f,
        from: MirrorYFrom = MirrorYFrom.Bottom
    ): MirrorYParams {
        val en = FxParam.Float()
        val fr = FxParam.Float()
        defaults[en] = DefaultValue.F(enabled)
        defaults[fr] = DefaultValue.F(from.v)
        ops.add(OpMirrorY(en, fr))
        return MirrorYParams(en, fr)
    }

    fun mirrorYFromBottom(enabled: Float = 1f): MirrorYParams = mirrorY(enabled, MirrorYFrom.Bottom)
    fun mirrorYFromTop(enabled: Float = 1f): MirrorYParams = mirrorY(enabled, MirrorYFrom.Top)

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

enum class MirrorXFrom(val v: Float) {
    Left(0f),
    Right(1f)
}

enum class MirrorYFrom(val v: Float) {
    Top(0f),
    Bottom(1f)
}

data class MirrorYParams(
    val enabled: FxParam.Float,
    val from: FxParam.Float
)

data class MirrorXParams(
    val enabled: FxParam.Float,
    val from: FxParam.Float
)

sealed class Pivot {
    data object Center : Pivot()
    data class Fraction(val x: Float, val y: Float) : Pivot()
}