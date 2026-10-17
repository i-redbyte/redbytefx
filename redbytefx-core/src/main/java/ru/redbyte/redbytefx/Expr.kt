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
    private var cachedHash = 0
    private var hashed = false

    /**
     * Structural equality. Identical graphs and graphs with different hashes are decided without
     * walking them.
     */
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Expr<*>) return false
        if (hashCode() != other.hashCode()) return false
        return shape == other.shape && node == other.node
    }

    /**
     * Structural hash, computed once. A node hashes its operands through their cached hashes, so a
     * graph that reuses subexpressions hashes in linear time instead of once per path.
     */
    override fun hashCode(): Int {
        if (!hashed) {
            cachedHash = 31 * shape.hashCode() + node.hashCode()
            hashed = true
        }
        return cachedHash
    }

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

    data class UnclampedSample(val coord: Expr<*>) : ExprNode

    data class Texture(val sampler: Expr<*>, val uv: Expr<*>) : ExprNode

    data class AttributeRef(val attribute: AttributeHandle) : ExprNode

    data class VaryingRef(val varying: Varying<*>) : ExprNode

    data class Call(val function: String, val args: List<Expr<*>>) : ExprNode

    data class Param(val name: String) : ExprNode

    data class Compare(val op: CompareOp, val left: Expr<*>, val right: Expr<*>) : ExprNode

    data class Select(val condition: Expr<*>, val ifTrue: Expr<*>, val ifFalse: Expr<*>) : ExprNode

    data class UserCall(val function: UserFunction, val args: List<Expr<*>>) : ExprNode

    data class BlockRef(val member: BlockMember) : ExprNode

    data class GlIn(val index: Expr<*>) : ExprNode

    data object TessCoord : ExprNode

    data class Index(val member: BlockMember, val index: Expr<*>) : ExprNode

    data class VaryingAt(val varying: Varying<*>, val index: Expr<*>) : ExprNode

    data class SlotRef(val slot: LocalSlot) : ExprNode

    data class TextureCube(val sampler: Expr<*>, val direction: Expr<*>) : ExprNode

    data class Invocation(val kind: InvocationKind) : ExprNode
}

internal enum class InvocationKind {
    Global,
    Local,
    WorkGroup,
}

internal enum class CompareOp {
    Gt,
    Lt,
    Ge,
    Le,
    Eq,
    Ne,
}

internal class AttributeHandle(
    val name: String,
    val shape: Shape,
)

internal class VaryingWrite(
    val varying: Varying<*>,
    val value: Expr<*>,
)

internal class LocalSlot(
    val shape: Shape,
    val suggestedName: String?,
)

internal enum class ArithOp {
    Add,
    Sub,
    Mul,
    Div,
    And,
    Or,
}

internal enum class UnaryOp {
    Neg,
    Not,
}
