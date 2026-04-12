package ru.redbyte.redbytefx.compose

import android.graphics.RenderEffect as AndroidRenderEffect
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.layer.CompositingStrategy
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.platform.LocalView
import java.lang.ref.WeakReference
import ru.redbyte.redbytefx.FxEffect
import ru.redbyte.redbytefx.FxInstance
import ru.redbyte.redbytefx.FxParam

/**
 * Compose-friendly controller for a runtime [FxInstance].
 *
 * A controller owns a single runtime shader instance. Use a separate controller when the same
 * compiled [FxEffect] needs to render independently in multiple places or at different sizes.
 *
 * Use it to update uniforms from Compose state and pass it to [redbyteFx]. Uniform params are
 * still effect-specific: bind and set only params declared by the compiled [FxEffect] that
 * created this controller. If runtime updates throw about a missing uniform, the usual cause is an
 * [FxParam] from a **different** `redbytefx { }` block or effect instance — see **`README.md`**
 * (uniform handles are effect-specific).
 *
 * Uniform deduplication is delegated to [FxInstance]; the controller only invalidates the host
 * when the instance reports an actual change.
 *
 * In composable code, prefer [bindFloat], [bindFloat2], [bindFloat3], [bindFloat4], and
 * [bindTime] so updates happen after successful recomposition. The lower-level [setFloat],
 * [setFloat2], [setFloat3], [setFloat4], and [setResolution] calls remain useful for previews,
 * tests, or imperative runtime hosts.
 */
@Stable
public class FxController internal constructor(
    internal val instance: FxInstance
) {
    private var hostViewRef: WeakReference<View>? = null
    private var controllerBatchDepth: Int = 0
    private var pendingHostInvalidate: Boolean = false
    internal var runtimeInvalidationTick: Int by mutableIntStateOf(0)
        private set
    private var cachedPlatformRenderEffect: AndroidRenderEffect? = null
    private var cachedComposeRenderEffect: androidx.compose.ui.graphics.RenderEffect? = null
    internal val composeRenderEffect: androidx.compose.ui.graphics.RenderEffect
        get() {
            val platformRenderEffect = instance.renderEffect()
            if (cachedPlatformRenderEffect !== platformRenderEffect) {
                cachedPlatformRenderEffect = platformRenderEffect
                cachedComposeRenderEffect = platformRenderEffect.asComposeRenderEffect()
            }
            return checkNotNull(cachedComposeRenderEffect)
        }

    /**
     * Updates a scalar float uniform and invalidates the host view when [FxInstance.setFloat]
     * reports a change.
     *
     * The [param] handle must belong to the compiled effect that created this controller.
     *
     * Compose callers should usually prefer [bindFloat] so the write happens from a side effect
     * after recomposition instead of inline during composition.
     */
    public fun setFloat(param: FxParam.Float, value: Float) {
        maybeInvalidateAfterUniformChange(instance.setFloat(param, value))
    }

    /**
     * Updates a `float2` uniform and invalidates the host view when the instance reports a change.
     *
     * The [param] handle must belong to the compiled effect that created this controller.
     *
     * Compose callers should usually prefer [bindFloat2].
     */
    public fun setFloat2(param: FxParam.Float2, x: Float, y: Float) {
        maybeInvalidateAfterUniformChange(instance.setFloat2(param, x, y))
    }

    /**
     * Updates a `float3` uniform and invalidates the host view when the instance reports a change.
     *
     * The [param] handle must belong to the compiled effect that created this controller.
     *
     * Compose callers should usually prefer [bindFloat3].
     */
    public fun setFloat3(param: FxParam.Float3, x: Float, y: Float, z: Float) {
        maybeInvalidateAfterUniformChange(instance.setFloat3(param, x, y, z))
    }

    /**
     * Updates a `float4` uniform and invalidates the host view when the instance reports a change.
     *
     * The [param] handle must belong to the compiled effect that created this controller.
     *
     * Compose callers should usually prefer [bindFloat4].
     */
    public fun setFloat4(param: FxParam.Float4, x: Float, y: Float, z: Float, w: Float) {
        maybeInvalidateAfterUniformChange(instance.setFloat4(param, x, y, z, w))
    }

    /**
     * Updates the shader resolution in pixels and invalidates the host view when it changes.
     *
     * Most Compose callers should not call this manually because [redbyteFx] keeps the runtime
     * resolution synchronized with the current draw target automatically. Treat this as an escape
     * hatch for imperative hosts or deliberate runtime tooling outside the normal Compose path.
     */
    public fun setResolution(widthPx: Float, heightPx: Float) {
        val safeWidth = sanitizeControllerResolution(widthPx)
        val safeHeight = sanitizeControllerResolution(heightPx)
        maybeInvalidateAfterUniformChange(instance.setResolution(safeWidth, safeHeight))
    }

    /**
     * Runs [block] while coalescing backing [RenderEffect] rebuilds and host invalidation so that
     * multiple imperative uniform updates can produce a single refresh instead of one per setter.
     *
     * Prefer [bindFloat] / [bindTime] from Composable code; this is for imperative multi-write
     * sequences (for example from a `LaunchedEffect` that updates several uniforms together).
     */
    public fun runBatch(block: () -> Unit) {
        controllerBatchDepth++
        try {
            instance.runBatch(block)
        } finally {
            controllerBatchDepth--
            if (controllerBatchDepth == 0 && pendingHostInvalidate) {
                pendingHostInvalidate = false
                invalidateRuntime()
            }
        }
    }

    internal fun attachHost(view: View) {
        if (hostViewRef?.get() === view) return
        hostViewRef = WeakReference(view)
    }

    internal fun syncResolution(widthPx: Float, heightPx: Float) {
        // Size changes already re-enter the draw path, so this keeps the shader resolution current
        // without triggering an extra invalidation loop from inside drawing.
        val safeWidth = sanitizeControllerResolution(widthPx)
        val safeHeight = sanitizeControllerResolution(heightPx)
        instance.setResolution(safeWidth, safeHeight)
    }

    private fun maybeInvalidateAfterUniformChange(changed: Boolean) {
        if (!changed) return
        if (controllerBatchDepth > 0) {
            pendingHostInvalidate = true
        } else {
            invalidateRuntime()
        }
    }

    private fun invalidateRuntime() {
        runtimeInvalidationTick += 1
        hostViewRef?.get()?.postInvalidateOnAnimation()
    }
}

/**
 * Remembers a stable [FxController] for the supplied compiled [effect].
 *
 * The remembered controller owns one runtime instance and is intended to back one render target.
 * If [effect] changes identity, a fresh runtime instance is created for the new compiled shader.
 * Uniform params bound through this controller must come from the same compiled [effect].
 *
 * Keep the compiled [effect] stable and remember one controller per place that renders it. If the
 * same effect is shown in two different composables or at two different sizes, each render target
 * should usually have its own controller.
 *
 * When output looks wrong, inspect `effect.agslSource()` first, then verify param ownership,
 * sampling space, and controller-per-target usage before treating the issue as a Compose/runtime
 * problem.
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
 * The [param] handle must belong to the same compiled effect that created this controller.
 *
 * When [isPlaying] becomes `false`, the current time value is preserved. Resuming continues from
 * the paused value instead of restarting from zero. [offsetSeconds] shifts the reported time
 * without resetting the internally accumulated phase.
 *
 * Prefer this over manually ticking [setFloat] from composable code.
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
 * the value has actually changed. The [param] handle must belong to the effect that created this
 * controller (same compiled [FxEffect] as [rememberFxController]); matching names from another
 * effect are not interchangeable. Outside composition, use [setFloat] directly instead.
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
 * the value has actually changed. The [param] handle must belong to the effect that created this
 * controller. Outside composition, use [setFloat2] directly instead.
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
 * the value has actually changed. The [param] handle must belong to the effect that created this
 * controller. Outside composition, use [setFloat3] directly instead.
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
 * the value has actually changed. The [param] handle must belong to the effect that created this
 * controller. Outside composition, use [setFloat4] directly instead.
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
 * in sync with the content size. Reusing the same controller across unrelated render targets can
 * cause the runtime resolution to flap between sizes, so independent surfaces should normally own
 * independent controllers even if they share the same compiled [FxEffect].
 *
 * Internally this records the content into an offscreen graphics layer and applies the platform
 * render effect produced by the controller's runtime shader instance.
 *
 * If the rendered result looks wrong, debug in this order before suspecting this modifier:
 *
 * 1. inspect the compiled effect's `agslSource()`
 * 2. verify controller/param ownership
 * 3. verify sampling space (`sample(...)` vs `sampleUv(...)`)
 * 4. only then inspect render-target sizing or platform/runtime behavior
 */
public fun Modifier.redbyteFx(controller: FxController): Modifier =
    composed {
        val layer = rememberGraphicsLayer()
        drawWithCache {
            layer.compositingStrategy = CompositingStrategy.Offscreen
            onDrawWithContent {
                controller.runtimeInvalidationTick
                controller.syncResolution(size.width, size.height)
                layer.renderEffect = controller.composeRenderEffect
                layer.record {
                    this@onDrawWithContent.drawContent()
                }
                drawLayer(layer)
            }
        }
    }

@Stable
internal class TimeBindingState {
    var elapsedSeconds: Float = 0f
    var lastFrameNanos: Long? = null
}

internal fun sanitizeControllerResolution(value: Float): Float =
    if (value > 0f) value else 1f
