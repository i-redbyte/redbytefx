package ru.redbyte.redbytefx

import android.graphics.RenderEffect

/**
 * Immutable compiled RedByteFX effect.
 *
 * This is the "compile once, inspect once, instantiate as needed" part of the library surface.
 *
 * Create a new runtime instance with [newInstance] when you want to apply the effect to content.
 * Call [agslSource] when you want to inspect the generated shader shape before deciding whether a
 * problem is really runtime-related. The compiled effect itself is reusable and thread-agnostic;
 * mutable runtime state lives in the returned [FxInstance].
 *
 * For a high-level overview and usage, see **`README.md`** in the repository.
 */
public interface FxEffect {
    /**
     * Creates a new mutable runtime instance for this compiled effect.
     */
    public fun newInstance(): FxInstance

    /**
     * Returns the generated AGSL source for debugging, inspection, screenshot tests, or docs.
     *
     * Good first scan order:
     *
     * 1. `uniform ...` declarations
     * 2. locals introduced through `let(...)`
     * 3. user helpers introduced through `fn(...)`
     * 4. the final sampling/compositing path in `main(...)`
     */
    public fun agslSource(): String
}

/**
 * Mutable runtime handle for a compiled [FxEffect].
 *
 * Use it to update uniforms and obtain the platform [RenderEffect].
 *
 * This is the low-level imperative runtime surface. Higher-level UI layers can wrap it with their
 * own controller/binding model, but the same ownership rules still apply here: params come from
 * one compiled effect, mutable runtime state belongs to one runtime instance.
 *
 * **Threading:** call [renderEffect] and all uniform setters from the thread that owns the UI
 * surface (typically the main thread). [android.graphics.RuntimeShader] is not documented for
 * concurrent use.
 *
 * **Performance:** when a setter returns `true`, the implementation may rebuild the backing
 * [RenderEffect] so the GPU picks up new uniform values. Redundant writes that return `false`
 * avoid that work. Use [runBatch] to coalesce multiple imperative updates into a single rebuild
 * where applicable.
 */
public interface FxInstance {
    /**
     * Returns the Android [RenderEffect] backed by the generated runtime shader.
     */
    public fun renderEffect(): RenderEffect

    /**
     * Updates a scalar float uniform.
     *
     * The [param] handle must come from the same compiled [FxEffect] that created this instance.
     *
     * @return `true` if the runtime accepted a new value and refreshed the backing effect; `false`
     * when the value was unchanged (see [sameFloatUniformValue]).
     */
    public fun setFloat(param: FxParam.Float, value: Float): Boolean

    /**
     * Updates a `float2` uniform.
     *
     * The [param] handle must come from the same compiled [FxEffect] that created this instance.
     *
     * @return `true` when a new value was written; `false` when identical to the last write.
     */
    public fun setFloat2(param: FxParam.Float2, x: Float, y: Float): Boolean

    /**
     * Updates a `float3` uniform.
     *
     * The [param] handle must come from the same compiled [FxEffect] that created this instance.
     *
     * @return `true` when a new value was written; `false` when identical to the last write.
     */
    public fun setFloat3(param: FxParam.Float3, x: Float, y: Float, z: Float): Boolean

    /**
     * Updates a `float4` uniform.
     *
     * The [param] handle must come from the same compiled [FxEffect] that created this instance.
     *
     * @return `true` when a new value was written; `false` when identical to the last write.
     */
    public fun setFloat4(param: FxParam.Float4, x: Float, y: Float, z: Float, w: Float): Boolean

    /**
     * Updates the logical resolution used by the shader in pixels.
     *
     * Call this when the host runtime owns the draw size explicitly. If a higher-level UI layer is
     * already synchronizing the shader size from the render target, prefer letting that layer own
     * resolution updates instead of duplicating them manually.
     *
     * Non-positive inputs are clamped to `1f` per component before comparing to the last update.
     *
     * @return `true` when resolution changed; `false` when the clamped size matched the previous
     * update.
     */
    public fun setResolution(widthPx: Float, heightPx: Float): Boolean

    /**
     * Runs [block] while coalescing backing [RenderEffect] rebuild notifications so that multiple
     * uniform updates performed inside the block can produce **one** platform refresh instead of
     * one per setter. The default implementation simply runs [block] (no coalescing); the library
     * implementation batches notifications.
     *
     * [FxController] provides the same entry point and also coalesces host invalidation when
     * multiple imperative setters run in one block.
     */
    public fun runBatch(block: () -> Unit) {
        block()
    }
}

/**
 * Typed handle for a shader uniform declared from the DSL.
 *
 * A uniform handle is also an expression of the matching DSL type, so it can be referenced
 * directly inside the shader body after declaration. Handles are effect-specific: a [FxParam]
 * from one `redbytefx { ... }` block must not be reused with another compiled effect instance or
 * [FxInstance]. Matching debug labels in two different effects do **not** make two params the same
 * handle. See **`README.md`** in the repository (uniform handles are effect-specific).
 */
public sealed class FxParam {
    /**
     * Handle for a scalar `float` uniform.
     */
    public class Float internal constructor(
        internal val debugName: String?
    ) : FxParam(), FloatExpr

    /**
     * Handle for a `float2` uniform.
     */
    public class Float2 internal constructor(
        internal val debugName: String?
    ) : FxParam(), Float2Expr

    /**
     * Handle for a `float3` uniform.
     */
    public class Float3 internal constructor(
        internal val debugName: String?
    ) : FxParam(), Float3Expr

    /**
     * Handle for a `float4` uniform.
     */
    public class Float4 internal constructor(
        internal val debugName: String?
    ) : FxParam(), Float4Expr
}

/**
 * Compiles a RedByteFX shader definition into an immutable [FxEffect].
 *
 * The [block] is written against [FxDsl] and must return the final output color.
 *
 * Typical flow:
 *
 * 1. compile one effect with `redbytefx { ... }`
 * 2. inspect [FxEffect.agslSource] if the generated shader shape is unclear
 * 3. create multiple independent runtime instances via [FxEffect.newInstance] when separate
 *    render targets need isolated mutable state
 *
 * Porting raw AGSL or Shadertoy-style shaders is easiest when you keep `fragCoord` / `resolution`
 * explicit, inspect [FxEffect.agslSource], then bind uniforms from the same compiled effect only.
 *
 * @throws FxDiagnosticException when compilation fails; use [FxDiagnosticException.diagnostics] for
 * structured [FxDiagnostic] entries (stable [FxDiagnosticCode], message, optional hint). The
 * exception is a subtype of [IllegalStateException].
 */
public fun redbytefx(block: FxDsl.() -> ColorExpr): FxEffect = FxBuilder.build(block)
