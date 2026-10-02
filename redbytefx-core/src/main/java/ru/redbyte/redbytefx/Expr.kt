package ru.redbyte.redbytefx

/**
 * Authoring-time shader expression.
 *
 * [T] is a phantom type used by the Kotlin signatures. [shape] is the runtime witness.
 * Building an expression allocates; evaluating a frame must not.
 */
public class Expr<out T : ShType> internal constructor(
    public val shape: Shape,
    internal val node: ExprNode,
) {
    override fun equals(other: Any?): Boolean {
        if (other !is Expr<*>) return false
        return shape == other.shape && node == other.node
    }

    override fun hashCode(): Int = 31 * shape.hashCode() + node.hashCode()

    override fun toString(): String = "Expr($shape, $node)"
}

internal sealed interface ExprNode {
    data class Literal(val value: Float, val precision: Precision) : ExprNode

    data class IntLiteral(val value: Int) : ExprNode

    data class Swizzle(val source: Expr<*>, val mask: String) : ExprNode

    data class Cast(val arg: Expr<*>) : ExprNode

    data class Unary(val op: UnaryOp, val arg: Expr<*>) : ExprNode

    data class Binary(val op: ArithOp, val left: Expr<*>, val right: Expr<*>) : ExprNode

    data class Construct(val args: List<Expr<*>>) : ExprNode

    data class Local(val suggestedName: String?, val initializer: Expr<*>) : ExprNode

    data class UniformRef(val uniform: Uniform<*>) : ExprNode

    data object FragCoord : ExprNode

    data object Resolution : ExprNode

    data class Sample(val coord: Expr<*>) : ExprNode

    data class Texture(val sampler: Expr<*>, val uv: Expr<*>) : ExprNode

    data class AttributeRef(val attribute: AttributeHandle) : ExprNode

    data class VaryingRef(val varying: Varying<*>) : ExprNode

    data class Call(val function: String, val args: List<Expr<*>>) : ExprNode
}

internal class AttributeHandle(
    val name: String,
    val shape: Shape,
)

internal class VaryingWrite(
    val varying: Varying<*>,
    val value: Expr<*>,
)

internal enum class ArithOp {
    Add,
    Sub,
    Mul,
    Div,
}

internal enum class UnaryOp {
    Neg,
}
