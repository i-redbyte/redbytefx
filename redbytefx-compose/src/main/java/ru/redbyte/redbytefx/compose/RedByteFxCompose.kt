package ru.redbyte.redbytefx.compose

import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import ru.redbyte.redbytefx.FxEffect
import ru.redbyte.redbytefx.FxInstance
import ru.redbyte.redbytefx.FxParam

@Immutable
class FxController internal constructor(
    internal val instance: FxInstance,
    private val hostView: View
) {
    private val _version = mutableIntStateOf(0)
    val version: Int get() = _version.intValue

    private var lastW = -1f
    private var lastH = -1f

    fun setFloat(param: FxParam.Float, value: Float) {
        instance.setFloat(param, value)
        bump()
    }

    fun setFloat2(param: FxParam.Float2, x: Float, y: Float) {
        instance.setFloat2(param, x, y)
        bump()
    }

    fun setResolution(widthPx: Float, heightPx: Float) {
        if (widthPx == lastW && heightPx == lastH) return
        lastW = widthPx
        lastH = heightPx
        instance.setResolution(widthPx, heightPx)
        bump()
    }

    private fun bump() {
        _version.intValue = _version.intValue + 1
        hostView.postInvalidateOnAnimation()
    }
}

@Composable
fun rememberFxController(effect: FxEffect): FxController {
    val view = LocalView.current
    val instance = remember(effect) { effect.newInstance() }
    return remember(instance, view) { FxController(instance, view) }
}

fun Modifier.redbyteFx(controller: FxController): Modifier = composed {
    val v = controller.version
    val composeEffect = remember(controller.instance, v) {
        controller.instance.renderEffect().asComposeRenderEffect()
    }

    this
        .graphicsLayer {
            compositingStrategy = CompositingStrategy.Offscreen
            renderEffect = composeEffect
        }
        .drawWithContent {
            controller.setResolution(size.width, size.height)
            drawContent()
        }
}