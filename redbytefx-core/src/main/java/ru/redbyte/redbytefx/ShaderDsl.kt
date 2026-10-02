package ru.redbyte.redbytefx

import java.util.IdentityHashMap

public class ShaderProgram internal constructor(
    public val target: ShaderTarget,
    private val agsl: String,
    internal val bindings: List<UniformBinding>,
) {
    public fun agslSource(): String = agsl

    public fun <T : ShType> uniform(agslName: String): Uniform<T> {
        val found = bindings.firstOrNull { it.agslName == agslName }?.uniform
            ?: throw IllegalArgumentException("Shader has no uniform named $agslName")
        @Suppress("UNCHECKED_CAST")
        return found as Uniform<T>
    }

    internal fun binding(uniform: Uniform<*>): UniformBinding =
        bindings.firstOrNull { it.uniform === uniform }
            ?: throw IllegalArgumentException("Uniform does not belong to this shader")
}

public fun shader(target: ShaderTarget, block: ShaderDsl.() -> Unit): ShaderProgram {
    val dsl = ShaderDsl(target)
    dsl.block()
    return dsl.compile()
}

public class ShaderDsl internal constructor(
    private val target: ShaderTarget,
) {
    private var state = authoringState(target, AuthoringPlace.Program)
    private val uniforms = mutableListOf<Uniform<*>>()
    private var output: Expr<Vec4<Flt<Med>>>? = null

    public fun uniform(name: String, default: Float): Uniform<Flt<High>> {
        advance(AuthoringAction.DeclareUniform)
        require(default.isFinite()) { "Uniform default must be finite, was $default" }
        val handle = createUniform<Flt<High>>(
            name = name,
            shape = Shape.Scalar(ScalarKind.Float, Precision.High),
            default = default,
        )
        uniforms += handle
        return handle
    }

    public fun fragment(block: FragmentDsl.() -> Expr<Vec4<Flt<Med>>>) {
        check(output == null) { "Shader already has a fragment stage" }
        advance(AuthoringAction.EnterFragment)
        output = FragmentDsl(::advance).block()
    }

    public fun vertex(block: VertexDsl.() -> Unit) {
        advance(AuthoringAction.EnterVertex)
        VertexDsl().block()
    }

    internal fun compile(): ShaderProgram {
        check(target == ShaderTarget.Agsl) { "Only AGSL shaders can be spelled right now" }
        val body = checkNotNull(output) { "AGSL shader requires a fragment stage" }
        val allocator = IdentifierAllocator(agslReservedNames())
        val bindings = uniforms.map { uniform ->
            val agslName = allocator.reserve(sanitizeIdentifier(uniform.name ?: "value", "u_"))
            UniformBinding(uniform, agslName)
        }
        val names = bindings.associateBy { it.uniform }
        val emitter = AgslEmitter(allocator, names)
        val rendered = emitter.emit(body)
        return ShaderProgram(
            target = target,
            agsl = renderAgsl(bindings, emitter.declarations, rendered),
            bindings = bindings,
        )
    }

    private fun advance(action: AuthoringAction) {
        val step = authoringStep(state, action)
        val code = step.code ?: run {
            state = step.state
            return
        }
        throw AuthoringException(code)
    }
}

public class VertexDsl internal constructor()

public class FragmentDsl internal constructor(
    private val advance: (AuthoringAction) -> Unit,
) {
    public val fragCoord: Expr<Vec2<Flt<High>>> = Expr(
        Shape.Vector(ScalarKind.Float, Precision.High, 2),
        ExprNode.FragCoord,
    )

    public val resolution: Expr<Vec2<Flt<High>>> = Expr(
        Shape.Vector(ScalarKind.Float, Precision.High, 2),
        ExprNode.Resolution,
    )

    public fun sample(coord: Expr<Vec2<Flt<High>>> = fragCoord): Expr<Vec4<Flt<Med>>> {
        advance(AuthoringAction.Sample)
        return Expr(
            Shape.Vector(ScalarKind.Float, Precision.Med, 4),
            ExprNode.Sample(coord),
        )
    }

    public fun <T : ShType> Expr<T>.let(name: String? = null): Expr<T> {
        advance(AuthoringAction.Let)
        return Expr(shape, ExprNode.Local(name, this))
    }
}

internal fun emitAgsl(expr: Expr<*>): String =
    AgslEmitter(IdentifierAllocator(agslReservedNames()), emptyMap()).emit(expr)

private class AgslEmitter(
    private val allocator: IdentifierAllocator,
    private val uniforms: Map<Uniform<*>, UniformBinding>,
) {
    private val localNames = IdentityHashMap<ExprNode.Local, String>()
    private var localIndex = 0
    val declarations = mutableListOf<String>()

    fun emit(expr: Expr<*>): String = when (val node = expr.node) {
        is ExprNode.Literal -> formatFloat(node.value)
        is ExprNode.IntLiteral -> node.value.toString()
        is ExprNode.Swizzle -> "${emit(node.source)}.${node.mask}"
        is ExprNode.Cast -> "${spell(expr.shape, ShaderTarget.Agsl)}(${emit(node.arg)})"
        is ExprNode.Unary -> "(-${emit(node.arg)})"
        is ExprNode.Binary -> "(${emit(node.left)} ${node.op.symbol} ${emit(node.right)})"
        is ExprNode.Construct -> {
            val args = node.args.joinToString(", ") { emit(it) }
            "${spell(expr.shape, ShaderTarget.Agsl)}($args)"
        }
        is ExprNode.Local -> local(node, expr.shape)
        is ExprNode.UniformRef -> uniforms.getValue(node.uniform).agslName
        ExprNode.FragCoord -> "fragCoord"
        ExprNode.Resolution -> RB_RESOLUTION_UNIFORM
        is ExprNode.Sample -> "rb_sample(${emit(node.coord)})"
    }

    private fun local(node: ExprNode.Local, shape: Shape): String {
        localNames[node]?.let { return it }
        val initializer = emit(node.initializer)
        val name = allocator.reserve(localBase(node.suggestedName, localIndex))
        localIndex += 1
        declarations += "  ${spell(shape, ShaderTarget.Agsl)} $name = $initializer;"
        localNames[node] = name
        return name
    }
}

private val ArithOp.symbol: String
    get() = when (this) {
        ArithOp.Add -> "+"
        ArithOp.Sub -> "-"
        ArithOp.Mul -> "*"
        ArithOp.Div -> "/"
    }

private fun localBase(suggested: String?, index: Int): String {
    if (suggested.isNullOrBlank() || !isSafeIdentifier(suggested)) {
        return suggested?.let { sanitizeSuggestedIdentifier(it, "l") } ?: "l$index"
    }
    return suggested
}

private fun isSafeIdentifier(value: String): Boolean {
    val first = value.first()
    if (!(first.isLetter() || first == '_')) return false
    return value.all { it == '_' || it.isLetterOrDigit() }
}

private fun agslReservedNames(): Set<String> = buildSet {
    addAll(RESERVED_USER_FUNCTION_NAMES)
    add("fragCoord")
    add("main")
    add(RB_INPUT_UNIFORM)
    add(RB_RESOLUTION_UNIFORM)
    add("rb_maxCoord")
    add("rb_sample")
}

private fun renderAgsl(
    bindings: List<UniformBinding>,
    declarations: List<String>,
    output: String,
): String = buildString {
    append("uniform shader ").append(RB_INPUT_UNIFORM).append(";\n")
    append("uniform float2 ").append(RB_RESOLUTION_UNIFORM).append(";\n")
    for (binding in bindings) {
        val type = spell(binding.uniform.shape, ShaderTarget.Agsl)
        append("uniform ").append(type).append(' ').append(binding.agslName).append(";\n")
    }
    append('\n')
    append(AGSL_SAMPLE_HELPER)
    append("half4 main(float2 fragCoord) {\n")
    for (line in declarations) {
        append(line).append('\n')
    }
    append("  return ").append(output).append(";\n")
    append("}\n")
}

private val AGSL_SAMPLE_HELPER = """
    float2 rb_maxCoord(float2 res) {
      float2 e = float2(0.001);
      return max(res - e, float2(0.0));
    }
    half4 rb_sample(float2 coord) {
      float2 p = coord;
      if (${RB_RESOLUTION_UNIFORM}.x > 2.0 && ${RB_RESOLUTION_UNIFORM}.y > 2.0) {
        p = clamp(p, float2(0.0), rb_maxCoord(${RB_RESOLUTION_UNIFORM}));
      }
      return ${RB_INPUT_UNIFORM}.eval(p);
    }

""".trimIndent() + "\n"
