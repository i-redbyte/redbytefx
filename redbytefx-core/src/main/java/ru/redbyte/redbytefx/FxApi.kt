package ru.redbyte.redbytefx

import android.graphics.RenderEffect

/**
 * Immutable compiled RedByteFX effect.
 *
 * Create a new runtime instance with [newInstance] when you want to apply the effect to content.
 * The compiled effect itself is reusable and thread-agnostic; mutable runtime state lives in the
 * returned [FxInstance].
 */
public interface FxEffect {
    /**
     * Creates a new mutable runtime instance for this compiled effect.
     */
    public fun newInstance(): FxInstance

    /**
     * Returns the generated AGSL source for debugging, inspection, screenshot tests, or docs.
     */
    public fun agslSource(): String
}

/**
 * Mutable runtime handle for a compiled [FxEffect].
 *
 * Use it to update uniforms and obtain the platform [RenderEffect].
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
     */
    public fun setFloat(param: FxParam.Float, value: Float)

    /**
     * Updates a `float2` uniform.
     *
     * The [param] handle must come from the same compiled [FxEffect] that created this instance.
     */
    public fun setFloat2(param: FxParam.Float2, x: Float, y: Float)

    /**
     * Updates a `float3` uniform.
     *
     * The [param] handle must come from the same compiled [FxEffect] that created this instance.
     */
    public fun setFloat3(param: FxParam.Float3, x: Float, y: Float, z: Float)

    /**
     * Updates a `float4` uniform.
     *
     * The [param] handle must come from the same compiled [FxEffect] that created this instance.
     */
    public fun setFloat4(param: FxParam.Float4, x: Float, y: Float, z: Float, w: Float)

    /**
     * Updates the logical resolution used by the shader in pixels.
     */
    public fun setResolution(widthPx: Float, heightPx: Float)
}

/**
 * Typed handle for a shader uniform declared from the DSL.
 *
 * A uniform handle is also an expression of the matching DSL type, so it can be referenced
 * directly inside the shader body after declaration. Handles are effect-specific: a [FxParam]
 * from one `redbytefx { ... }` block must not be reused with another compiled effect instance.
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
 * The returned effect can be reused to create multiple independent runtime instances via
 * [FxEffect.newInstance], which is how higher layers such as `rememberFxController(...)` keep
 * render-target state isolated.
 */
public fun redbytefx(block: FxDsl.() -> ColorExpr): FxEffect = FxBuilder.build(block)
