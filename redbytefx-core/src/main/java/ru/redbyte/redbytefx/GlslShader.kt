package ru.redbyte.redbytefx

import java.util.IdentityHashMap

internal fun linkGlsl(
    uniforms: List<Uniform<*>>,
    varyings: List<Varying<*>>,
    attributes: List<AttributeHandle>,
    writes: List<VaryingWrite>,
    position: Expr<*>,
    fragmentBody: Expr<*>,
): ShaderProgram {
    val interfaceNames = IdentifierAllocator(glslReservedNames())
    val bindings = uniforms.map { uniform ->
        UniformBinding(uniform, interfaceNames.reserve(sanitizeIdentifier(uniform.name ?: "value", "u_")))
    }
    val attributeNames = attributes.associateWith { attribute ->
        interfaceNames.reserve(sanitizeIdentifier(attribute.name, "a_"))
    }
    val varyingNames = varyings.associateWith { varying ->
        interfaceNames.reserve(sanitizeIdentifier(varying.name, "v_"))
    }
    val uniformNames = bindings.associate { it.uniform to it.agslName }
    val occupied = interfaceNames.snapshot()
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
            outputName = null,
            outputValue = null,
        ),
        fragment = renderStage(
            inputs = writes.map { "in ${glslDeclaration(it.varying.shape)} ${varyingNames.getValue(it.varying)};" },
            outputs = listOf("out ${glslDeclaration(fragmentBody.shape)} oColor;"),
            uniforms = bindings.filter { it.uniform in fragmentUniforms },
            declarations = fragmentEmitter.declarations,
            statements = emptyList(),
            outputName = "oColor",
            outputValue = fragmentText,
        ),
        bindings = bindings,
    )
}

internal fun collectVaryings(expr: Expr<*>, into: MutableSet<Varying<*>>) {
    walk(expr) { node ->
        if (node is ExprNode.VaryingRef) into += node.varying
    }
}

private fun collectUniforms(expr: Expr<*>, into: MutableSet<Uniform<*>>) {
    walk(expr) { node ->
        if (node is ExprNode.UniformRef) into += node.uniform
    }
}

private fun walk(expr: Expr<*>, visit: (ExprNode) -> Unit) {
    visit(expr.node)
    when (val node = expr.node) {
        is ExprNode.Unary -> walk(node.arg, visit)
        is ExprNode.Binary -> {
            walk(node.left, visit)
            walk(node.right, visit)
        }
        is ExprNode.Construct -> node.args.forEach { walk(it, visit) }
        is ExprNode.Local -> walk(node.initializer, visit)
        is ExprNode.Swizzle -> walk(node.source, visit)
        is ExprNode.Cast -> walk(node.arg, visit)
        is ExprNode.Sample -> walk(node.coord, visit)
        is ExprNode.Texture -> {
            walk(node.sampler, visit)
            walk(node.uv, visit)
        }
        is ExprNode.Call -> node.args.forEach { walk(it, visit) }
        is ExprNode.Literal,
        is ExprNode.IntLiteral,
        is ExprNode.UniformRef,
        is ExprNode.AttributeRef,
        is ExprNode.VaryingRef,
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

private fun renderStage(
    inputs: List<String>,
    outputs: List<String>,
    uniforms: List<UniformBinding>,
    declarations: List<String>,
    statements: List<String>,
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

private fun glslReservedNames(): Set<String> = buildSet {
    addAll(RESERVED_USER_FUNCTION_NAMES)
    add("main")
    add("gl_Position")
    add("oColor")
    add("texture")
}
