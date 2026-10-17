package ru.redbyte.redbytefx

import java.util.IdentityHashMap

/**
 * Spells the expressions of one shader body for a concrete target.
 *
 * Owns the naming state every target shares: `let` locals and their scopes, mutable local slots,
 * and temporaries for subexpressions that one statement reads more than once. Subclasses only
 * spell individual nodes.
 */
internal abstract class ScopedEmitter(
    private val allocator: IdentifierAllocator,
) : CodeEmitter {
    private val localNames = IdentityHashMap<ExprNode.Local, String>()
    private val localScopes = ArrayDeque<MutableList<ExprNode.Local>>()
    private var localIndex = 0
    private val slotNames = IdentityHashMap<LocalSlot, String>()
    private var slotIndex = 0
    private val sharing = StatementSharing()
    private var temporaryIndex = 0
    final override val declarations = mutableListOf<String>()

    /** Declaration type of [shape], including any precision qualifier. */
    protected abstract fun declarationType(shape: Shape): String

    /** Spells one node; children are spelled through [emit]. */
    protected abstract fun spellNode(expr: Expr<*>): String

    final override fun emitRoot(expr: Expr<*>): String =
        sharing.within(expr, localNames::containsKey) { emit(expr) }

    final override fun emit(expr: Expr<*>): String {
        sharing.nameOf(expr)?.let { return it }
        val node = expr.node
        if (node is ExprNode.Local) return local(node, expr.shape)
        val text = spellNode(expr)
        if (!sharing.isShared(expr)) return text
        val name = allocator.reserve("$TEMPORARY_PREFIX$temporaryIndex")
        temporaryIndex += 1
        declarations += "  ${declarationType(expr.shape)} $name = $text;"
        sharing.remember(expr, name)
        return name
    }

    final override fun enterScope() {
        localScopes.addLast(mutableListOf())
    }

    final override fun leaveScope() {
        for (node in localScopes.removeLast()) localNames.remove(node)
    }

    final override fun bindSlot(slot: LocalSlot) {
        if (slotNames.containsKey(slot)) return
        val name = allocator.reserve(localBase(slot.suggestedName, slotIndex))
        slotIndex += 1
        slotNames[slot] = name
        declarations += "  ${declarationType(slot.shape)} $name;"
    }

    final override fun slotName(slot: LocalSlot): String {
        bindSlot(slot)
        return slotNames.getValue(slot)
    }

    private fun local(node: ExprNode.Local, shape: Shape): String {
        localNames[node]?.let { return it }
        val initializer = emit(node.initializer)
        val name = allocator.reserve(localBase(node.suggestedName, localIndex))
        localIndex += 1
        declarations += "  ${declarationType(shape)} $name = $initializer;"
        localNames[node] = name
        localScopes.lastOrNull()?.add(node)
        return name
    }
}

private const val TEMPORARY_PREFIX = "rb_t"

/**
 * Subexpressions that one statement reaches through more than one parent.
 *
 * Sharing is scoped to a single statement: between statements a local slot may be written, so a
 * value computed earlier would be stale.
 */
internal class StatementSharing {
    private val parents = IdentityHashMap<Expr<*>, Int>()
    private val names = IdentityHashMap<Expr<*>, String>()

    fun <T> within(root: Expr<*>, isNamedLocal: (ExprNode.Local) -> Boolean, spell: () -> T): T {
        reset()
        countParents(root, isNamedLocal)
        try {
            return spell()
        } finally {
            reset()
        }
    }

    fun isShared(expr: Expr<*>): Boolean = (parents[expr] ?: 0) > 1 && isWorthNaming(expr.node)

    fun nameOf(expr: Expr<*>): String? = names[expr]

    fun remember(expr: Expr<*>, name: String) {
        names[expr] = name
    }

    private fun reset() {
        parents.clear()
        names.clear()
    }

    private fun countParents(expr: Expr<*>, isNamedLocal: (ExprNode.Local) -> Boolean) {
        val seen = parents[expr]
        parents[expr] = (seen ?: 0) + 1
        if (seen != null) return
        val node = expr.node
        if (node is ExprNode.Local && isNamedLocal(node)) return
        forEachOperand(node) { countParents(it, isNamedLocal) }
    }
}

/**
 * Identifiers, literals, and constructors built only from them are cheaper to repeat than to name,
 * and the driver folds them as constants.
 */
private fun isWorthNaming(node: ExprNode): Boolean = when (node) {
    is ExprNode.Construct -> node.args.any { isWorthNaming(it.node) }
    is ExprNode.Cast -> isWorthNaming(node.arg.node)
    is ExprNode.Unary,
    is ExprNode.Binary,
    is ExprNode.Call,
    is ExprNode.UserCall,
    is ExprNode.Compare,
    is ExprNode.Select,
    is ExprNode.Texture,
    is ExprNode.TextureCube,
    is ExprNode.Sample,
    is ExprNode.UnclampedSample,
    is ExprNode.Index,
    is ExprNode.GlIn,
    is ExprNode.VaryingAt,
    -> true
    is ExprNode.Literal,
    is ExprNode.IntLiteral,
    is ExprNode.Swizzle,
    is ExprNode.Local,
    is ExprNode.UniformRef,
    is ExprNode.AttributeRef,
    is ExprNode.VaryingRef,
    is ExprNode.Param,
    is ExprNode.BlockRef,
    is ExprNode.SlotRef,
    is ExprNode.Invocation,
    ExprNode.FragCoord,
    ExprNode.Resolution,
    ExprNode.TessCoord,
    -> false
}

/** Direct operands of [node]. A user call's body is spelled separately and is not an operand. */
internal inline fun forEachOperand(node: ExprNode, visit: (Expr<*>) -> Unit) {
    when (node) {
        is ExprNode.Unary -> visit(node.arg)
        is ExprNode.Binary -> {
            visit(node.left)
            visit(node.right)
        }
        is ExprNode.Construct -> node.args.forEach(visit)
        is ExprNode.Local -> visit(node.initializer)
        is ExprNode.Swizzle -> visit(node.source)
        is ExprNode.Cast -> visit(node.arg)
        is ExprNode.Sample -> visit(node.coord)
        is ExprNode.UnclampedSample -> visit(node.coord)
        is ExprNode.Texture -> {
            visit(node.sampler)
            visit(node.uv)
        }
        is ExprNode.TextureCube -> {
            visit(node.sampler)
            visit(node.direction)
        }
        is ExprNode.GlIn -> visit(node.index)
        is ExprNode.VaryingAt -> visit(node.index)
        is ExprNode.Call -> node.args.forEach(visit)
        is ExprNode.Compare -> {
            visit(node.left)
            visit(node.right)
        }
        is ExprNode.Select -> {
            visit(node.condition)
            visit(node.ifTrue)
            visit(node.ifFalse)
        }
        is ExprNode.UserCall -> node.args.forEach(visit)
        is ExprNode.Index -> visit(node.index)
        is ExprNode.Literal,
        is ExprNode.IntLiteral,
        is ExprNode.UniformRef,
        is ExprNode.AttributeRef,
        is ExprNode.VaryingRef,
        is ExprNode.Param,
        is ExprNode.BlockRef,
        is ExprNode.SlotRef,
        is ExprNode.Invocation,
        ExprNode.TessCoord,
        ExprNode.FragCoord,
        ExprNode.Resolution,
        -> Unit
    }
}

/** Base name for a `let` local or a mutable slot; unsafe suggestions are sanitized. */
internal fun localBase(suggested: String?, index: Int): String {
    if (suggested.isNullOrBlank()) return "l$index"
    val first = suggested.first()
    val safe = (first.isLetter() || first == '_') && suggested.all { it == '_' || it.isLetterOrDigit() }
    return if (safe) suggested else sanitizeSuggestedIdentifier(suggested, "l")
}
