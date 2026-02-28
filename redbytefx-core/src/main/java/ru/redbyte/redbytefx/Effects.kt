package ru.redbyte.redbytefx

object Effects {
    fun upsideDown(): FxEffect = redbytefx {
        flipY(1f)
    }

    fun mirrorHorizontal(): FxEffect = redbytefx {
        mirrorX(1f)
    }

    fun rotate180(): FxEffect = redbytefx {
        rotate(180f)
    }
}
