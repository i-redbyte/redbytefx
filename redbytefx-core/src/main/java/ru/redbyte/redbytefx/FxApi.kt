package ru.redbyte.redbytefx

import android.graphics.RenderEffect

interface FxEffect {
    fun newInstance(): FxInstance
}

interface FxInstance {
    fun renderEffect(): RenderEffect
    fun setFloat(param: FxParam.Float, value: Float)
    fun setFloat2(param: FxParam.Float2, x: Float, y: Float)
    fun setResolution(widthPx: Float, heightPx: Float)
}

sealed class FxParam {
    class Float internal constructor() : FxParam()
    class Float2 internal constructor() : FxParam()
}

fun redbytefx(block: FxDsl.() -> Unit): FxEffect = FxBuilder.build(block)