package ru.redbyte.redbytefx.compose

import androidx.annotation.MainThread
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.drawscope.ContentDrawScope
import androidx.compose.ui.graphics.layer.CompositingStrategy
import androidx.compose.ui.graphics.layer.GraphicsLayer
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.node.DrawModifierNode
import androidx.compose.ui.node.ModifierNodeElement
import androidx.compose.ui.node.invalidateDraw
import androidx.compose.ui.node.requireGraphicsContext
import androidx.compose.ui.platform.InspectorInfo
import ru.redbyte.redbytefx.AgslInstance
import ru.redbyte.redbytefx.Flt
import ru.redbyte.redbytefx.High
import ru.redbyte.redbytefx.IntS
import ru.redbyte.redbytefx.Med
import ru.redbyte.redbytefx.RedByteFxApis
import ru.redbyte.redbytefx.RedByteFxPlatform
import ru.redbyte.redbytefx.ShaderProgram
import ru.redbyte.redbytefx.Uniform
import ru.redbyte.redbytefx.Vec2
import ru.redbyte.redbytefx.Vec3
import ru.redbyte.redbytefx.Vec4
import ru.redbyte.redbytefx.newAgslInstance
import kotlin.jvm.JvmName
import android.graphics.RenderEffect as AndroidRenderEffect

/**
 * Compose-friendly controller for one AGSL [ru.redbyte.redbytefx.AgslInstance].
 *
 * A controller owns a single runtime shader control. Use a separate controller when the same
 * compiled [ru.redbyte.redbytefx.ShaderProgram] needs to render independently in multiple places
 * or at different sizes.
 *
 * Use it to update uniforms from Compose state and pass it to [redbyteFx]. Uniform handles belong
 * to the program that created this controller. If runtime updates throw about a missing uniform,
 * the usual cause is a [ru.redbyte.redbytefx.Uniform] from a different `shader { }` program - see
 * **`README.md`**.
 *
 * Uniform deduplication is delegated to the AGSL instance; the controller only invalidates the host
 * when the instance reports an actual change.
 *
 * In composable code, prefer [bindFloat], [bindFloat2], [bindFloat3], [bindFloat4], and
 * [bindTime] so updates happen after successful recomposition. The lower-level [setFloat],
 * [setFloat2], [setFloat3], [setFloat4], and [setResolution] calls remain useful for previews,
 * tests, or imperative runtime hosts.
 *
 * Call setters and [runBatch] from the UI thread. The batch depth and pending
 * invalidation flag are plain fields, not synchronized.
 */
@Stable
public class FxController internal constructor(
    internal val control: ShaderControl,
) {
    private var controllerBatchDepth: Int = 0
    private val runtimeInvalidationListeners = LinkedHashSet<() -> Unit>()
    private var pendingHostInvalidate: Boolean = false
    internal var runtimeInvalidationTick: Int by mutableIntStateOf(0)
        private set
    private var cachedPlatformRenderEffect: AndroidRenderEffect? = null
    private var cachedComposeRenderEffect: androidx.compose.ui.graphics.RenderEffect? = null
    internal val composeRenderEffect: androidx.compose.ui.graphics.RenderEffect
        get() {
            val platformRenderEffect = control.renderEffect()
            if (cachedPlatformRenderEffect !== platformRenderEffect) {
                cachedPlatformRenderEffect = platformRenderEffect
                cachedComposeRenderEffect = platformRenderEffect.asComposeRenderEffect()
            }
            return checkNotNull(cachedComposeRenderEffect)
        }

    /**
     * Updates a scalar float uniform and invalidates the host view when the AGSL instance
     * reports a change.
     *
     * The [param] handle must belong to the compiled effect that created this controller.
     *
     * Compose callers should usually prefer [bindFloat] so the write happens from a side effect
     * after recomposition instead of inline during composition.
     */
    public fun setFloat(param: Uniform<Flt<High>>, value: Float) {
        maybeInvalidateAfterUniformChange(control.setFloat(param, value))
    }

    @JvmName("setMedFloat")
    public fun setFloat(param: Uniform<Flt<Med>>, value: Float) {
        maybeInvalidateAfterUniformChange(control.setMedFloat(param, value))
    }

    /**
     * Updates a `float2` uniform and invalidates the host view when the instance reports a change.
     *
     * The [param] handle must belong to the compiled effect that created this controller.
     *
     * Compose callers should usually prefer [bindFloat2].
     */
    public fun setFloat2(param: Uniform<Vec2<Flt<High>>>, x: Float, y: Float) {
        maybeInvalidateAfterUniformChange(control.setFloat2(param, x, y))
    }

    @JvmName("setMedFloat2")
    public fun setFloat2(param: Uniform<Vec2<Flt<Med>>>, x: Float, y: Float) {
        maybeInvalidateAfterUniformChange(control.setMedFloat2(param, x, y))
    }

    /**
     * Updates a `float3` uniform and invalidates the host view when the instance reports a change.
     *
     * The [param] handle must belong to the compiled effect that created this controller.
     *
     * Compose callers should usually prefer [bindFloat3].
     */
    public fun setFloat3(param: Uniform<Vec3<Flt<High>>>, x: Float, y: Float, z: Float) {
        maybeInvalidateAfterUniformChange(control.setFloat3(param, x, y, z))
    }

    @JvmName("setMedFloat3")
    public fun setFloat3(param: Uniform<Vec3<Flt<Med>>>, x: Float, y: Float, z: Float) {
        maybeInvalidateAfterUniformChange(control.setMedFloat3(param, x, y, z))
    }

    /**
     * Updates a `float4` uniform and invalidates the host view when the instance reports a change.
     *
     * The [param] handle must belong to the compiled effect that created this controller.
     *
     * Compose callers should usually prefer [bindFloat4].
     */
    public fun setFloat4(param: Uniform<Vec4<Flt<High>>>, x: Float, y: Float, z: Float, w: Float) {
        maybeInvalidateAfterUniformChange(control.setFloat4(param, x, y, z, w))
    }

    @JvmName("setMedFloat4")
    public fun setFloat4(param: Uniform<Vec4<Flt<Med>>>, x: Float, y: Float, z: Float, w: Float) {
        maybeInvalidateAfterUniformChange(control.setMedFloat4(param, x, y, z, w))
    }

    /**
     * Updates a scalar int uniform and invalidates the host view when the AGSL instance
     * reports a change.
     *
     * The [param] handle must belong to the compiled effect that created this controller.
     * Compose callers should usually prefer [bindInt].
     */
    public fun setInt(param: Uniform<IntS>, value: Int) {
        maybeInvalidateAfterUniformChange(control.setInt(param, value))
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
        maybeInvalidateAfterUniformChange(control.setResolution(safeWidth, safeHeight))
    }

    /**
     * Runs [block] while coalescing backing [RenderEffect] rebuilds and host invalidation so that
     * multiple imperative uniform updates can produce a single refresh instead of one per setter.
     *
     * UI thread only. The batch depth and the pending host-invalidation flag are not
     * `@Volatile` and are not locked. Do not call this from a background dispatcher.
     *
     * Prefer [bindFloat] / [bindTime] from Composable code; this is for imperative multi-write
     * sequences (for example from a `LaunchedEffect` that updates several uniforms together).
     */
    @MainThread
    public fun runBatch(block: () -> Unit) {
        controllerBatchDepth++
        try {
            control.runBatch(block)
        } finally {
            controllerBatchDepth--
            if (controllerBatchDepth == 0 && pendingHostInvalidate) {
                pendingHostInvalidate = false
                invalidateRuntime()
            }
        }
    }

    internal fun syncResolution(widthPx: Float, heightPx: Float) {
        // Size changes already re-enter the draw path, so this keeps the shader resolution current
        // without triggering an extra invalidation loop from inside drawing.
        val safeWidth = sanitizeControllerResolution(widthPx)
        val safeHeight = sanitizeControllerResolution(heightPx)
        control.setResolution(safeWidth, safeHeight)
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
        for (listener in runtimeInvalidationListeners) listener()
    }

    internal fun addRuntimeInvalidationListener(listener: () -> Unit) {
        runtimeInvalidationListeners += listener
    }

    internal fun removeRuntimeInvalidationListener(listener: () -> Unit) {
        runtimeInvalidationListeners -= listener
    }
}

/**
 * Remembers a stable [FxController] for the supplied compiled [program].
 *
 * The remembered controller owns one runtime instance and is intended to back one render target.
 * If [program] changes identity, a fresh runtime instance is created for the new compiled shader.
 * Uniform params bound through this controller must come from the same compiled [program].
 *
 * Keep the compiled [program] stable and remember one controller per place that renders it. If the
 * same effect is shown in two different composables or at two different sizes, each render target
 * should usually have its own controller.
 *
 * When output looks wrong, inspect `program.agslSource()` first, then verify param ownership,
 * sampling space, and controller-per-target usage before treating the issue as a Compose/runtime
 * problem.
 *
 * Each remembered controller owns one [AgslInstance]. Android does not expose an explicit
 * `RuntimeShader` destroy API; when the controller leaves composition, the instance becomes
 * unreachable and is reclaimed by the garbage collector together with its [android.graphics.RenderEffect].
 */
@RequiresApi(RedByteFxApis.AGSL_MIN_SDK)
@Composable
public fun rememberFxController(program: ShaderProgram): FxController {
    RedByteFxPlatform.requireAgslRuntime()
    return remember(program) { FxController(AgslShaderControl(program.newAgslInstance())) }
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
    param: Uniform<Flt<High>>,
    isPlaying: Boolean = true,
    offsetSeconds: Float = 0f,
) {
    val state = remember(this, param) { TimeBindingState() }

    LaunchedEffect(this, param, isPlaying, offsetSeconds) {
        if (!isPlaying) {
            state.hasLastFrame = false
            setFloat(param, offsetSeconds + state.elapsedSeconds)
            return@LaunchedEffect
        }

        while (true) {
            withFrameNanos { frameNanos ->
                if (state.hasLastFrame) {
                    state.elapsedSeconds += (frameNanos - state.lastFrameNanos) / 1_000_000_000f
                }
                state.lastFrameNanos = frameNanos
                state.hasLastFrame = true
                setFloat(param, offsetSeconds + state.elapsedSeconds)
            }
        }
    }
}

/**
 * Binds a scalar float uniform to Compose state.
 *
 * The uniform is updated after successful recomposition and only invalidates the host view when
 * the value has actually changed. The [param] handle must belong to the program that created this
 * controller (same compiled [ru.redbyte.redbytefx.ShaderProgram] as [rememberFxController]); matching
 * names from another program are not interchangeable. Outside composition, use [setFloat] directly
 * instead.
 */
@Composable
public fun FxController.bindFloat(
    param: Uniform<Flt<High>>,
    value: Float,
) {
    SideEffect {
        setFloat(param, value)
    }
}

/**
 * Binds a scalar int uniform to Compose state.
 *
 * The uniform is updated after successful recomposition and only invalidates the host view when
 * the value has actually changed. The [param] handle must belong to the program that created this
 * controller. Outside composition, use [setInt] directly instead.
 */
@Composable
public fun FxController.bindInt(
    param: Uniform<IntS>,
    value: Int,
) {
    SideEffect {
        setInt(param, value)
    }
}

@JvmName("bindMedFloat")
@Composable
public fun FxController.bindFloat(
    param: Uniform<Flt<Med>>,
    value: Float,
) {
    SideEffect {
        setFloat(param, value)
    }
}

/**
 * Binds a `float2` uniform to Compose state.
 *
 * The uniform is updated after successful recomposition and only invalidates the host view when
 * the value has actually changed. The [param] handle must belong to the program that created this
 * controller. Outside composition, use [setFloat2] directly instead.
 */
@Composable
public fun FxController.bindFloat2(
    param: Uniform<Vec2<Flt<High>>>,
    x: Float,
    y: Float,
) {
    SideEffect {
        setFloat2(param, x, y)
    }
}

@JvmName("bindMedFloat2")
@Composable
public fun FxController.bindFloat2(
    param: Uniform<Vec2<Flt<Med>>>,
    x: Float,
    y: Float,
) {
    SideEffect {
        setFloat2(param, x, y)
    }
}

/**
 * Binds a `float3` uniform to Compose state.
 *
 * The uniform is updated after successful recomposition and only invalidates the host view when
 * the value has actually changed. The [param] handle must belong to the program that created this
 * controller. Outside composition, use [setFloat3] directly instead.
 */
@Composable
public fun FxController.bindFloat3(
    param: Uniform<Vec3<Flt<High>>>,
    x: Float,
    y: Float,
    z: Float,
) {
    SideEffect {
        setFloat3(param, x, y, z)
    }
}

@JvmName("bindMedFloat3")
@Composable
public fun FxController.bindFloat3(
    param: Uniform<Vec3<Flt<Med>>>,
    x: Float,
    y: Float,
    z: Float,
) {
    SideEffect {
        setFloat3(param, x, y, z)
    }
}

/**
 * Binds a `float4` uniform to Compose state.
 *
 * The uniform is updated after successful recomposition and only invalidates the host view when
 * the value has actually changed. The [param] handle must belong to the program that created this
 * controller. Outside composition, use [setFloat4] directly instead.
 */
@Composable
public fun FxController.bindFloat4(
    param: Uniform<Vec4<Flt<High>>>,
    x: Float,
    y: Float,
    z: Float,
    w: Float,
) {
    SideEffect {
        setFloat4(param, x, y, z, w)
    }
}

@JvmName("bindMedFloat4")
@Composable
public fun FxController.bindFloat4(
    param: Uniform<Vec4<Flt<Med>>>,
    x: Float,
    y: Float,
    z: Float,
    w: Float,
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
 * independent controllers even if they share the same compiled [ru.redbyte.redbytefx.ShaderProgram].
 *
 * Internally this records the content into an offscreen graphics layer and applies the platform
 * render effect produced by the controller's runtime shader control. The modifier is a
 * `Modifier.Node`, so Compose can reuse the node across recompositions.
 *
 * If the rendered result looks wrong, debug in this order before suspecting this modifier:
 *
 * 1. inspect the compiled effect's `agslSource()`
 * 2. verify controller/param ownership
 * 3. verify sampling space (`sample(...)` vs `sampleUv(...)`)
 * 4. only then inspect render-target sizing or platform/runtime behavior
 */
@RequiresApi(RedByteFxApis.AGSL_MIN_SDK)
public fun Modifier.redbyteFx(controller: FxController): Modifier =
    this then RedByteFxElement(controller)

private class RedByteFxElement(
    private val controller: FxController,
) : ModifierNodeElement<RedByteFxNode>() {
    override fun create(): RedByteFxNode {
        RedByteFxPlatform.requireAgslRuntime()
        return RedByteFxNode(controller)
    }

    override fun update(node: RedByteFxNode) {
        node.updateController(controller)
    }

    override fun equals(other: Any?): Boolean =
        other is RedByteFxElement && controller === other.controller

    override fun hashCode(): Int = System.identityHashCode(controller)

    override fun InspectorInfo.inspectableProperties() {
        name = "redbyteFx"
        properties["controller"] = controller
    }
}

private class RedByteFxNode(
    private var controller: FxController,
) : Modifier.Node(), DrawModifierNode {
    private var layer: GraphicsLayer? = null
    private var appliedRenderEffect: androidx.compose.ui.graphics.RenderEffect? = null
    private val invalidateListener: () -> Unit = { invalidateDraw() }

    override fun onAttach() {
        val graphicsLayer = requireGraphicsContext().createGraphicsLayer()
        graphicsLayer.compositingStrategy = CompositingStrategy.Offscreen
        graphicsLayer.renderEffect = null
        layer = graphicsLayer
        appliedRenderEffect = null
        controller.addRuntimeInvalidationListener(invalidateListener)
    }

    override fun onDetach() {
        controller.removeRuntimeInvalidationListener(invalidateListener)
        layer?.let { requireGraphicsContext().releaseGraphicsLayer(it) }
        layer = null
        appliedRenderEffect = null
    }

    fun updateController(next: FxController) {
        if (controller === next) return
        if (isAttached) controller.removeRuntimeInvalidationListener(invalidateListener)
        controller = next
        if (isAttached) controller.addRuntimeInvalidationListener(invalidateListener)
        appliedRenderEffect = null
        invalidateDraw()
    }

    override fun ContentDrawScope.draw() {
        val graphicsLayer = layer
        if (graphicsLayer == null) {
            drawContent()
            return
        }
        controller.runtimeInvalidationTick
        controller.syncResolution(size.width, size.height)
        val renderEffect = controller.composeRenderEffect
        if (appliedRenderEffect !== renderEffect) {
            appliedRenderEffect = renderEffect
            graphicsLayer.renderEffect = renderEffect
        }
        graphicsLayer.record {
            this@draw.drawContent()
        }
        drawLayer(graphicsLayer)
    }
}

internal class TimeBindingState {
    var elapsedSeconds: Float = 0f
    var lastFrameNanos: Long = 0L
    var hasLastFrame: Boolean = false
}

internal fun sanitizeControllerResolution(value: Float): Float =
    if (value.isFinite() && value > 0f) value else 1f

internal interface MediumShaderControl {
    fun setMedFloat(uniform: Uniform<Flt<Med>>, value: Float): Boolean
    fun setMedFloat2(uniform: Uniform<Vec2<Flt<Med>>>, x: Float, y: Float): Boolean
    fun setMedFloat3(uniform: Uniform<Vec3<Flt<Med>>>, x: Float, y: Float, z: Float): Boolean
    fun setMedFloat4(uniform: Uniform<Vec4<Flt<Med>>>, x: Float, y: Float, z: Float, w: Float): Boolean
}

internal interface ShaderControl : MediumShaderControl {
    fun renderEffect(): AndroidRenderEffect
    fun setFloat(uniform: Uniform<Flt<High>>, value: Float): Boolean
    fun setFloat2(uniform: Uniform<Vec2<Flt<High>>>, x: Float, y: Float): Boolean
    fun setFloat3(uniform: Uniform<Vec3<Flt<High>>>, x: Float, y: Float, z: Float): Boolean
    fun setFloat4(uniform: Uniform<Vec4<Flt<High>>>, x: Float, y: Float, z: Float, w: Float): Boolean
    fun setInt(uniform: Uniform<IntS>, value: Int): Boolean
    fun setResolution(widthPx: Float, heightPx: Float): Boolean
    fun runBatch(block: () -> Unit)
}

internal class AgslShaderControl(
    private val instance: AgslInstance,
) : ShaderControl {
    override fun renderEffect(): AndroidRenderEffect = instance.renderEffect()

    override fun setFloat(uniform: Uniform<Flt<High>>, value: Float): Boolean = instance.set(uniform, value)

    override fun setMedFloat(uniform: Uniform<Flt<Med>>, value: Float): Boolean = instance.set(uniform, value)

    override fun setFloat2(uniform: Uniform<Vec2<Flt<High>>>, x: Float, y: Float): Boolean =
        instance.set(uniform, x, y)

    override fun setMedFloat2(uniform: Uniform<Vec2<Flt<Med>>>, x: Float, y: Float): Boolean =
        instance.set(uniform, x, y)

    override fun setFloat3(uniform: Uniform<Vec3<Flt<High>>>, x: Float, y: Float, z: Float): Boolean =
        instance.set(uniform, x, y, z)

    override fun setMedFloat3(uniform: Uniform<Vec3<Flt<Med>>>, x: Float, y: Float, z: Float): Boolean =
        instance.set(uniform, x, y, z)

    override fun setFloat4(
        uniform: Uniform<Vec4<Flt<High>>>,
        x: Float,
        y: Float,
        z: Float,
        w: Float,
    ): Boolean = instance.set(uniform, x, y, z, w)

    override fun setMedFloat4(
        uniform: Uniform<Vec4<Flt<Med>>>,
        x: Float,
        y: Float,
        z: Float,
        w: Float,
    ): Boolean = instance.set(uniform, x, y, z, w)

    override fun setInt(uniform: Uniform<IntS>, value: Int): Boolean = instance.set(uniform, value)

    override fun setResolution(widthPx: Float, heightPx: Float): Boolean =
        instance.setResolution(widthPx, heightPx)

    override fun runBatch(block: () -> Unit) {
        instance.batch(block)
    }
}
