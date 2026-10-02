package ru.redbyte.redbytefx

/**
 * Named fragment color output for a GLES 3.0 program.
 *
 * Write it once from the GLES fragment stage. AGSL keeps a single `half4 main`.
 */
public class FragmentOutput internal constructor(
    public val name: String,
    public val location: Int,
) {
    public val shape: Shape = Shape.Vector(ScalarKind.Float, Precision.High, 4)
}

internal class FragmentWrite(
    val output: FragmentOutput,
    val value: Expr<*>,
)
