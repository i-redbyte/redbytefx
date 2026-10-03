package ru.redbyte.redbytefx

/**
 * Value passed from the vertex stage to the fragment stage.
 *
 * [expr] reads it. [ShaderDsl.VertexDsl.set] writes it. Both stages use one shape.
 */
public class Varying<T : ShType> internal constructor(
    public val name: String,
    public val shape: Shape,
) {
    public lateinit var expr: Expr<T>
        internal set
}

internal fun <T : ShType> createVarying(name: String, shape: Shape): Varying<T> {
    val handle = Varying<T>(name, shape)
    handle.expr = Expr(shape, ExprNode.VaryingRef(handle))
    return handle
}

internal enum class ProgramCode {
    MissingVertex,
    MissingFragment,
    MissingGlPosition,
    VaryingNotWritten,
    FunctionWrongStage,
    RecursiveFunction,
    FragmentOutNotWritten,
    MissingCompute,
    TessStageMissing,
    ForeignUniform,
    EmitVertexWithoutPosition,
    VaryingForward,
    TessInterpolation,
    LocalInsideRepeat,
    UnsizedStorageNotLast,
}

internal class ProgramException(
    val code: ProgramCode,
    message: String,
) : IllegalStateException(message)
