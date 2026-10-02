package ru.redbyte.redbytefx

import java.util.IdentityHashMap

public class ShaderProgram internal constructor(
    public val target: ShaderTarget,
    private val agsl: String? = null,
    private val vertex: String? = null,
    private val fragment: String? = null,
    internal val bindings: List<UniformBinding>,
) {
    public fun agslSource(): String = agsl ?: error("This shader has no AGSL source")

    public fun vertexSource(): String = vertex ?: error("This shader has no GLES vertex source")

    public fun fragmentSource(): String = fragment ?: error("This shader has no GLES fragment source")

    public fun spelledUniforms(): List<SpelledUniform> =
        bindings.map { SpelledUniform(it.uniform, it.agslName) }

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

public class SpelledUniform internal constructor(
    public val uniform: Uniform<*>,
    public val name: String,
)

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
    private val varyings = mutableListOf<Varying<*>>()
    private val attributes = mutableListOf<AttributeHandle>()
    private val varyingWrites = mutableListOf<VaryingWrite>()
    private var fragmentBody: Expr<*>? = null
    private var vertexPosition: Expr<Vec4<Flt<High>>>? = null
    private var vertexBuilt = false

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

    public fun sampler2D(name: String): Uniform<Sampler2D> {
        advance(AuthoringAction.DeclareSampler)
        val handle = createSampler<Sampler2D>(name, Shape.Sampler2D)
        uniforms += handle
        return handle
    }

    public fun varyingVec2(name: String): Varying<Vec2<Flt<High>>> = varying(
        name,
        Shape.Vector(ScalarKind.Float, Precision.High, 2),
    )

    public fun fragment(block: FragmentDsl.() -> Expr<*>) {
        check(fragmentBody == null) { "Shader already has a fragment stage" }
        advance(AuthoringAction.EnterFragment)
        try {
            fragmentBody = FragmentDsl(::advance).block()
        } finally {
            advance(AuthoringAction.LeaveStage)
        }
    }

    public fun vertex(block: VertexDsl.() -> Unit) {
        check(!vertexBuilt) { "Shader already has a vertex stage" }
        advance(AuthoringAction.EnterVertex)
        vertexBuilt = true
        try {
            VertexDsl().block()
        } finally {
            advance(AuthoringAction.LeaveStage)
        }
    }

    internal fun compile(): ShaderProgram = when (target) {
        ShaderTarget.Agsl -> compileAgsl()
        ShaderTarget.Gles30 -> compileGlsl()
    }

    private fun compileAgsl(): ShaderProgram {
        val body = checkNotNull(fragmentBody) { "AGSL shader requires a fragment stage" }
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

    private fun <T : ShType> varying(name: String, shape: Shape): Varying<T> {
        advance(AuthoringAction.DeclareVarying)
        val handle = createVarying<T>(name, shape)
        varyings += handle
        return handle
    }

    private fun compileGlsl(): ShaderProgram {
        if (!vertexBuilt) throw ProgramException(ProgramCode.MissingVertex, "GLES program requires a vertex stage")
        val body = fragmentBody
            ?: throw ProgramException(ProgramCode.MissingFragment, "GLES program requires a fragment stage")
        val position = vertexPosition
            ?: throw ProgramException(ProgramCode.MissingGlPosition, "GLES vertex must assign gl_Position")
        require(isFloatVec4(body.shape)) { "GLES fragment must return a float vec4, was ${body.shape}" }
        val reads = linkedSetOf<Varying<*>>()
        collectVaryings(body, reads)
        for (varying in reads) {
            if (varyingWrites.none { it.varying === varying }) {
                throw ProgramException(
                    ProgramCode.VaryingNotWritten,
                    "Fragment reads varying \"${varying.name}\" that the vertex did not write",
                )
            }
        }
        return linkGlsl(
            uniforms = uniforms,
            varyings = varyings,
            attributes = attributes,
            writes = varyingWrites,
            position = position,
            fragmentBody = body,
        )
    }

    public inner class VertexDsl {
        public fun <T : ShType> Varying<T>.set(value: Expr<T>) {
            require(value.shape == shape) {
                "Varying \"${this.name}\" expects $shape, was ${value.shape}"
            }
            varyingWrites += VaryingWrite(this, value)
        }

        public fun glPosition(value: Expr<Vec4<Flt<High>>>) {
            advance(AuthoringAction.GlPosition)
            check(vertexPosition == null) { "gl_Position is already assigned" }
            vertexPosition = value
        }

        public fun attributeVec2(name: String): Expr<Vec2<Flt<High>>> = attribute(
            name,
            Shape.Vector(ScalarKind.Float, Precision.High, 2),
        )

        public fun attributeVec4(name: String): Expr<Vec4<Flt<High>>> = attribute(
            name,
            Shape.Vector(ScalarKind.Float, Precision.High, 4),
        )

        private fun <T : ShType> attribute(name: String, shape: Shape): Expr<T> {
            advance(AuthoringAction.Attribute)
            val handle = AttributeHandle(name, shape)
            attributes += handle
            return Expr(shape, ExprNode.AttributeRef(handle))
        }
    }
}

private fun isFloatVec4(shape: Shape): Boolean =
    shape is Shape.Vector && shape.kind == ScalarKind.Float && shape.lanes == 4

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

    public fun texture(
        sampler: Uniform<Sampler2D>,
        uv: Expr<Vec2<Flt<High>>>,
    ): Expr<Vec4<Flt<High>>> {
        advance(AuthoringAction.Texture)
        return Expr(
            Shape.Vector(ScalarKind.Float, Precision.High, 4),
            ExprNode.Texture(sampler.expr, uv),
        )
    }

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
        is ExprNode.Texture,
        is ExprNode.AttributeRef,
        is ExprNode.VaryingRef -> error("AGSL cannot spell ${node::class.simpleName}")
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
