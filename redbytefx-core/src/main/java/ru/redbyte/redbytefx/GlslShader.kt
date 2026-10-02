package ru.redbyte.redbytefx

import java.util.IdentityHashMap

internal fun linkGlsl(
    uniforms: List<Uniform<*>>,
    varyings: List<Varying<*>>,
    attributes: List<AttributeHandle>,
    writes: List<VaryingWrite>,
    position: Expr<*>,
    fragmentBody: Expr<*>,
    names: IdentifierAllocator,
    functions: List<UserFunction>,
): ShaderProgram {
    val bindings = uniforms.map { uniform ->
        UniformBinding(uniform, names.reserve(sanitizeIdentifier(uniform.name ?: "value", "u_")))
    }
    val attributeNames = attributes.associateWith { attribute ->
        names.reserve(sanitizeIdentifier(attribute.name, "a_"))
    }
    val varyingNames = varyings.associateWith { varying ->
        names.reserve(sanitizeIdentifier(varying.name, "v_"))
    }
    val uniformNames = bindings.associate { it.uniform to it.agslName }
    val occupied = names.snapshot()
    val vertexEmitter = GlslEmitter(IdentifierAllocator(occupied), uniformNames, attributeNames, varyingNames)
    val fragmentEmitter = GlslEmitter(IdentifierAllocator(occupied), uniformNames, attributeNames, varyingNames)
    val vertexAssignments = writes.map { write ->
        varyingNames.getValue(write.varying) to vertexEmitter.emit(write.value)
    }
    val positionText = vertexEmitter.emit(position)
    val fragmentText = fragmentEmitter.emit(fragmentBody)
    val vertexUniforms = linkedSetOf<Uniform<*>>()
    val fragmentUniforms = linkedSetOf<Uniform<*>>()
    writes.forEach { collectUniforms(it.value, vertexUniforms) }
    collectUniforms(position, vertexUniforms)
    collectUniforms(fragmentBody, fragmentUniforms)
    return ShaderProgram(
        target = ShaderTarget.Gles30,
        vertex = renderStage(
            inputs = attributes.map { "in ${glslDeclaration(it.shape)} ${attributeNames.getValue(it)};" },
            outputs = writes.map { "out ${glslDeclaration(it.varying.shape)} ${varyingNames.getValue(it.varying)};" },
            uniforms = bindings.filter { it.uniform in vertexUniforms },
            declarations = vertexEmitter.declarations,
            statements = vertexAssignments.map { (name, value) -> "  $name = $value;" } +
                "  gl_Position = $positionText;",
            functions = renderGlslFunctions(
                functions.filter { it.stage == AuthoringPlace.Vertex },
                occupied,
                uniformNames,
                attributeNames,
                varyingNames,
            ),
            outputName = null,
            outputValue = null,
        ),
        fragment = renderStage(
            inputs = writes.map { "in ${glslDeclaration(it.varying.shape)} ${varyingNames.getValue(it.varying)};" },
            outputs = listOf("out ${glslDeclaration(fragmentBody.shape)} oColor;"),
            uniforms = bindings.filter { it.uniform in fragmentUniforms },
            declarations = fragmentEmitter.declarations,
            statements = emptyList(),
            functions = renderGlslFunctions(
                functions.filter { it.stage == AuthoringPlace.Fragment },
                occupied,
                uniformNames,
                attributeNames,
                varyingNames,
            ),
            outputName = "oColor",
            outputValue = fragmentText,
        ),
        bindings = bindings,
    )
}

internal fun collectVaryings(expr: Expr<*>, into: MutableSet<Varying<*>>) {
    walk(expr, linkedSetOf()) { node ->
        if (node is ExprNode.VaryingRef) into += node.varying
    }
}

internal fun checkFunctionStage(expr: Expr<*>, stage: AuthoringPlace) {
    walk(expr, linkedSetOf()) { node ->
        if (node is ExprNode.UserCall && node.function.stage != stage) {
            throw ProgramException(
                ProgramCode.FunctionWrongStage,
                "Function \"${node.function.name}\" is not defined in this stage",
            )
        }
    }
}

private fun collectUniforms(expr: Expr<*>, into: MutableSet<Uniform<*>>) {
    walk(expr, linkedSetOf()) { node ->
        if (node is ExprNode.UniformRef) into += node.uniform
    }
}

private fun walk(
    expr: Expr<*>,
    seen: MutableSet<UserFunction>,
    visit: (ExprNode) -> Unit,
) {
    visit(expr.node)
    when (val node = expr.node) {
        is ExprNode.Unary -> walk(node.arg, seen, visit)
        is ExprNode.Binary -> {
            walk(node.left, seen, visit)
            walk(node.right, seen, visit)
        }
        is ExprNode.Construct -> node.args.forEach { walk(it, seen, visit) }
        is ExprNode.Local -> walk(node.initializer, seen, visit)
        is ExprNode.Swizzle -> walk(node.source, seen, visit)
        is ExprNode.Cast -> walk(node.arg, seen, visit)
        is ExprNode.Sample -> walk(node.coord, seen, visit)
        is ExprNode.Texture -> {
            walk(node.sampler, seen, visit)
            walk(node.uv, seen, visit)
        }
        is ExprNode.Call -> node.args.forEach { walk(it, seen, visit) }
        is ExprNode.Compare -> {
            walk(node.left, seen, visit)
            walk(node.right, seen, visit)
        }
        is ExprNode.Select -> {
            walk(node.condition, seen, visit)
            walk(node.ifTrue, seen, visit)
            walk(node.ifFalse, seen, visit)
        }
        is ExprNode.UserCall -> {
            node.args.forEach { walk(it, seen, visit) }
            if (seen.add(node.function)) walk(node.function.body, seen, visit)
        }
        is ExprNode.Literal,
        is ExprNode.IntLiteral,
        is ExprNode.UniformRef,
        is ExprNode.AttributeRef,
        is ExprNode.VaryingRef,
        is ExprNode.Param,
        ExprNode.FragCoord,
        ExprNode.Resolution -> Unit
    }
}

private class GlslEmitter(
    private val allocator: IdentifierAllocator,
    private val uniforms: Map<Uniform<*>, String>,
    private val attributes: Map<AttributeHandle, String>,
    private val varyings: Map<Varying<*>, String>,
) {
    private val localNames = IdentityHashMap<ExprNode.Local, String>()
    private var localIndex = 0
    val declarations = mutableListOf<String>()

    fun emit(expr: Expr<*>): String = when (val node = expr.node) {
        is ExprNode.Literal -> formatFloat(node.value)
        is ExprNode.IntLiteral -> node.value.toString()
        is ExprNode.Swizzle -> "${emit(node.source)}.${node.mask}"
        is ExprNode.Cast -> "${spell(expr.shape, ShaderTarget.Gles30)}(${emit(node.arg)})"
        is ExprNode.Unary -> "(-${emit(node.arg)})"
        is ExprNode.Binary -> "(${emit(node.left)} ${arithSymbol(node.op)} ${emit(node.right)})"
        is ExprNode.Construct -> {
            val args = node.args.joinToString(", ") { emit(it) }
            "${spell(expr.shape, ShaderTarget.Gles30)}($args)"
        }
        is ExprNode.Local -> local(node, expr.shape)
        is ExprNode.UniformRef -> uniforms.getValue(node.uniform)
        is ExprNode.AttributeRef -> attributes.getValue(node.attribute)
        is ExprNode.VaryingRef -> varyings.getValue(node.varying)
        is ExprNode.Texture -> "texture(${emit(node.sampler)}, ${emit(node.uv)})"
        is ExprNode.Call -> {
            val args = node.args.joinToString(", ") { emit(it) }
            "${node.function}($args)"
        }
        is ExprNode.Param -> node.name
        is ExprNode.Compare -> "(${emit(node.left)} ${node.op.symbol} ${emit(node.right)})"
        is ExprNode.Select -> "(${emit(node.condition)} ? ${emit(node.ifTrue)} : ${emit(node.ifFalse)})"
        is ExprNode.UserCall -> {
            val args = node.args.joinToString(", ") { emit(it) }
            "${node.function.name}($args)"
        }
        is ExprNode.Sample,
        ExprNode.FragCoord,
        ExprNode.Resolution -> error("GLSL stage cannot spell ${node::class.simpleName}")
    }

    private fun local(node: ExprNode.Local, shape: Shape): String {
        localNames[node]?.let { return it }
        val initializer = emit(node.initializer)
        val name = allocator.reserve(glslLocalBase(node.suggestedName, localIndex))
        localIndex += 1
        declarations += "  ${glslDeclaration(shape)} $name = $initializer;"
        localNames[node] = name
        return name
    }
}

private fun renderGlslFunctions(
    functions: List<UserFunction>,
    occupied: Set<String>,
    uniforms: Map<Uniform<*>, String>,
    attributes: Map<AttributeHandle, String>,
    varyings: Map<Varying<*>, String>,
): String = buildString {
    for (function in functions) {
        val locals = IdentifierAllocator(occupied + function.parameters.map { it.name })
        val emitter = GlslEmitter(locals, uniforms, attributes, varyings)
        val body = emitter.emit(function.body)
        val signature = function.parameters.joinToString(", ") { "${glslDeclaration(it.shape)} ${it.name}" }
        append(glslDeclaration(function.result)).append(' ').append(function.name)
            .append('(').append(signature).append(") {\n")
        emitter.declarations.forEach { append(it).append('\n') }
        append("  return ").append(body).append(";\n}\n")
    }
}

private fun renderStage(
    inputs: List<String>,
    outputs: List<String>,
    uniforms: List<UniformBinding>,
    declarations: List<String>,
    statements: List<String>,
    functions: String,
    outputName: String?,
    outputValue: String?,
): String = buildString {
    append("#version 300 es\n")
    append("precision highp float;\n")
    for (line in inputs) append(line).append('\n')
    for (binding in uniforms) {
        append("uniform ").append(glslDeclaration(binding.uniform.shape)).append(' ')
            .append(binding.agslName).append(";\n")
    }
    for (line in outputs) append(line).append('\n')
    append(functions)
    append("void main() {\n")
    for (line in declarations) append(line).append('\n')
    for (line in statements) append(line).append('\n')
    if (outputName != null && outputValue != null) {
        append("  ").append(outputName).append(" = ").append(outputValue).append(";\n")
    }
    append("}\n")
}

private fun arithSymbol(op: ArithOp): String = when (op) {
    ArithOp.Add -> "+"
    ArithOp.Sub -> "-"
    ArithOp.Mul -> "*"
    ArithOp.Div -> "/"
}

private fun glslLocalBase(suggested: String?, index: Int): String {
    if (suggested.isNullOrBlank()) return "l$index"
    val safe = suggested.first().let { it.isLetter() || it == '_' } &&
        suggested.all { it == '_' || it.isLetterOrDigit() }
    return if (safe) suggested else sanitizeSuggestedIdentifier(suggested, "l")
}

internal fun glslReservedNames(): Set<String> = buildSet {
    addAll(RESERVED_USER_FUNCTION_NAMES)
    add("main")
    add("gl_Position")
    add("oColor")
    add("texture")
}
