package ru.redbyte.redbytefx.compose

import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.Stable
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalView
import java.lang.ref.WeakReference
import java.util.IdentityHashMap
import ru.redbyte.redbytefx.FxEffect
import ru.redbyte.redbytefx.FxInstance
import ru.redbyte.redbytefx.FxParam

/**
 * Compose-friendly controller for a runtime [FxInstance].
 *
 * A controller owns a single runtime shader instance. Use a separate controller when the same
 * compiled [FxEffect] needs to render independently in multiple places.
 *
 * Use it to update uniforms from Compose state and pass it to [redbyteFx].
 */
@Stable
public class FxController internal constructor(
    internal val instance: FxInstance
) {
    internal val composeRenderEffect = instance.renderEffect().asComposeRenderEffect()

    private var lastW = -1f
    private var lastH = -1f
    private var hostViewRef: WeakReference<View>? = null
    private val floatValues = IdentityHashMap<FxParam.Float, Float>()
    private val float2Values = IdentityHashMap<FxParam.Float2, Float2Value>()
    private val float3Values = IdentityHashMap<FxParam.Float3, Float3Value>()
    private val float4Values = IdentityHashMap<FxParam.Float4, Float4Value>()

    /**
     * Updates a scalar float uniform and invalidates the host view.
     */
    public fun setFloat(param: FxParam.Float, value: Float) {
        val previous = floatValues[param]
        if (previous != null && sameFloat(previous, value)) return
        floatValues[param] = value
        instance.setFloat(param, value)
        bump()
    }

    /**
     * Updates a `float2` uniform and invalidates the host view.
     */
    public fun setFloat2(param: FxParam.Float2, x: Float, y: Float) {
        if (sameFloat2(float2Values[param], x, y)) return
        float2Values[param] = Float2Value(x, y)
        instance.setFloat2(param, x, y)
        bump()
    }

    /**
     * Updates a `float3` uniform and invalidates the host view.
     */
    public fun setFloat3(param: FxParam.Float3, x: Float, y: Float, z: Float) {
        if (sameFloat3(float3Values[param], x, y, z)) return
        float3Values[param] = Float3Value(x, y, z)
        instance.setFloat3(param, x, y, z)
        bump()
    }

    /**
     * Updates a `float4` uniform and invalidates the host view.
     */
    public fun setFloat4(param: FxParam.Float4, x: Float, y: Float, z: Float, w: Float) {
        if (sameFloat4(float4Values[param], x, y, z, w)) return
        float4Values[param] = Float4Value(x, y, z, w)
        instance.setFloat4(param, x, y, z, w)
        bump()
    }

    /**
     * Updates the shader resolution in pixels and invalidates the host view when it changes.
     */
    public fun setResolution(widthPx: Float, heightPx: Float) {
        if (widthPx == lastW && heightPx == lastH) return
        lastW = widthPx
        lastH = heightPx
        instance.setResolution(widthPx, heightPx)
        bump()
    }

    internal fun attachHost(view: View) {
        if (hostViewRef?.get() === view) return
        hostViewRef = WeakReference(view)
    }

    private fun bump() {
        hostViewRef?.get()?.postInvalidateOnAnimation()
    }
}

/**
 * Remembers a stable [FxController] for the supplied compiled [effect].
 *
 * The remembered controller owns one runtime instance and is intended to back one render target.
 */
@Composable
public fun rememberFxController(effect: FxEffect): FxController {
    val view = LocalView.current
    val controller = remember(effect) { FxController(effect.newInstance()) }
    SideEffect {
        controller.attachHost(view)
    }
    return controller
}

/**
 * Drives the provided float [param] with elapsed time in seconds.
 *
 * This is intended for uniforms created with `uniformTime(...)`.
 *
 * When [isPlaying] becomes `false`, the current time value is preserved. Resuming continues from
 * the paused value instead of restarting from zero.
 */
@Composable
public fun FxController.bindTime(
    param: FxParam.Float,
    isPlaying: Boolean = true,
    offsetSeconds: Float = 0f
) {
    val state = remember(this, param) { TimeBindingState() }

    LaunchedEffect(this, param, isPlaying, offsetSeconds) {
        if (!isPlaying) {
            state.lastFrameNanos = null
            setFloat(param, offsetSeconds + state.elapsedSeconds)
            return@LaunchedEffect
        }

        while (true) {
            withFrameNanos { frameNanos ->
                val lastFrameNanos = state.lastFrameNanos
                if (lastFrameNanos != null) {
                    state.elapsedSeconds += (frameNanos - lastFrameNanos) / 1_000_000_000f
                }
                state.lastFrameNanos = frameNanos
                setFloat(param, offsetSeconds + state.elapsedSeconds)
            }
        }
    }
}

/**
 * Binds a scalar float uniform to Compose state.
 *
 * The uniform is updated after successful recomposition and only invalidates the host view when
 * the value has actually changed.
 */
@Composable
public fun FxController.bindFloat(
    param: FxParam.Float,
    value: Float
) {
    SideEffect {
        setFloat(param, value)
    }
}

/**
 * Binds a `float2` uniform to Compose state.
 *
 * The uniform is updated after successful recomposition and only invalidates the host view when
 * the value has actually changed.
 */
@Composable
public fun FxController.bindFloat2(
    param: FxParam.Float2,
    x: Float,
    y: Float
) {
    SideEffect {
        setFloat2(param, x, y)
    }
}

/**
 * Binds a `float3` uniform to Compose state.
 *
 * The uniform is updated after successful recomposition and only invalidates the host view when
 * the value has actually changed.
 */
@Composable
public fun FxController.bindFloat3(
    param: FxParam.Float3,
    x: Float,
    y: Float,
    z: Float
) {
    SideEffect {
        setFloat3(param, x, y, z)
    }
}

/**
 * Binds a `float4` uniform to Compose state.
 *
 * The uniform is updated after successful recomposition and only invalidates the host view when
 * the value has actually changed.
 */
@Composable
public fun FxController.bindFloat4(
    param: FxParam.Float4,
    x: Float,
    y: Float,
    z: Float,
    w: Float
) {
    SideEffect {
        setFloat4(param, x, y, z, w)
    }
}

/**
 * Applies a compiled RedByteFX effect to the content drawn by this [Modifier].
 *
 * The supplied [controller] is expected to belong to this render target so its resolution stays
 * in sync with the content size.
 */
public fun Modifier.redbyteFx(controller: FxController): Modifier =
    this
        .graphicsLayer {
            compositingStrategy = CompositingStrategy.Offscreen
            renderEffect = controller.composeRenderEffect
        }
        .drawWithContent {
            controller.setResolution(size.width, size.height)
            drawContent()
        }

@Stable
internal class TimeBindingState {
    var elapsedSeconds: Float = 0f
    var lastFrameNanos: Long? = null
}

internal data class Float2Value(
    val x: Float,
    val y: Float
)

internal data class Float3Value(
    val x: Float,
    val y: Float,
    val z: Float
)

internal data class Float4Value(
    val x: Float,
    val y: Float,
    val z: Float,
    val w: Float
)

internal fun sameFloat(left: Float, right: Float): Boolean = left.toBits() == right.toBits()

internal fun sameFloat2(
    value: Float2Value?,
    x: Float,
    y: Float
): Boolean = value != null && sameFloat(value.x, x) && sameFloat(value.y, y)

internal fun sameFloat3(
    value: Float3Value?,
    x: Float,
    y: Float,
    z: Float
): Boolean = value != null &&
    sameFloat(value.x, x) &&
    sameFloat(value.y, y) &&
    sameFloat(value.z, z)

internal fun sameFloat4(
    value: Float4Value?,
    x: Float,
    y: Float,
    z: Float,
    w: Float
): Boolean = value != null &&
    sameFloat(value.x, x) &&
    sameFloat(value.y, y) &&
    sameFloat(value.z, z) &&
    sameFloat(value.w, w)
