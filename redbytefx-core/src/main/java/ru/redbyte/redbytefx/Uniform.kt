package ru.redbyte.redbytefx

/**
 * Effect-owned uniform handle.
 *
 * [expr] is the shader value. The handle itself is what runtime code passes to [ShaderRuntime.set].
 * Two handles with the same debug name from different shaders are not interchangeable.
 */
public class Uniform<T : ShType> internal constructor(
    public val name: String?,
    public val shape: Shape,
    public val default: Float?,
) {
    public lateinit var expr: Expr<T>
        internal set
}

internal fun <T : ShType> createUniform(
    name: String?,
    shape: Shape,
    default: Float,
): Uniform<T> {
    val handle = Uniform<T>(name, shape, default)
    handle.expr = Expr(shape, ExprNode.UniformRef(handle))
    return handle
}

internal fun <T : ShType> createSampler(name: String, shape: Shape): Uniform<T> {
    val handle = Uniform<T>(name, shape, null)
    handle.expr = Expr(shape, ExprNode.UniformRef(handle))
    return handle
}
