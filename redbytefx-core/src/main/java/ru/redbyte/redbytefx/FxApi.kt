package ru.redbyte.redbytefx

import android.graphics.RenderEffect

/**
 * Immutable compiled RedByteFX effect.
 *
 * Create a new runtime instance with [newInstance] when you want to apply the effect to content.
 */
public interface FxEffect {
    /**
     * Creates a new mutable runtime instance for this compiled effect.
     */
    public fun newInstance(): FxInstance

    /**
     * Returns the generated AGSL source for debugging, inspection, or tests.
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
     */
    public fun setFloat(param: FxParam.Float, value: Float)

    /**
     * Updates a `float2` uniform.
     */
    public fun setFloat2(param: FxParam.Float2, x: Float, y: Float)

    /**
     * Updates a `float3` uniform.
     */
    public fun setFloat3(param: FxParam.Float3, x: Float, y: Float, z: Float)

    /**
     * Updates a `float4` uniform.
     */
    public fun setFloat4(param: FxParam.Float4, x: Float, y: Float, z: Float, w: Float)

    /**
     * Updates the logical resolution used by the shader in pixels.
     */
    public fun setResolution(widthPx: Float, heightPx: Float)
}

/**
 * Typed handle for a shader uniform declared from the DSL.
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
 */
public fun redbytefx(block: FxDsl.() -> ColorExpr): FxEffect = FxBuilder.build(block)
