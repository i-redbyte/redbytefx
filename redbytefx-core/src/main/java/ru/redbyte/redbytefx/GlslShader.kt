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
    fragmentWrites: List<FragmentWrite>,
    block: UniformBlock?,
    version: Int = GLSL_300,
    programTarget: ShaderTarget = ShaderTarget.Gles30,
    geometry: String? = null,
    tessControl: String? = null,
    tessEval: String? = null,
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
    val outputNames = fragmentWrites.associate { write ->
        write.output to names.reserve(sanitizeSuggestedIdentifier(write.output.name, "o"))
    }
    val occupied = names.snapshot()
    val vertexEmitter = GlslEmitter(IdentifierAllocator(occupied), uniformNames, attributeNames, varyingNames)
    val fragmentEmitter = GlslEmitter(IdentifierAllocator(occupied), uniformNames, attributeNames, varyingNames)
    val vertexAssignments = writes.map { write ->
        varyingNames.getValue(write.varying) to vertexEmitter.emit(write.value)
    }
    val positionText = vertexEmitter.emit(position)
    val fragmentText = fragmentEmitter.emit(fragmentBody)
    val orderedWrites = fragmentWrites.sortedBy { it.output.location }
    val outputText = orderedWrites.map { write ->
        outputNames.getValue(write.output) to fragmentEmitter.emit(write.value)
    }
    val vertexUniforms = linkedSetOf<Uniform<*>>()
    val fragmentUniforms = linkedSetOf<Uniform<*>>()
    writes.forEach { collectUniforms(it.value, vertexUniforms) }
    collectUniforms(position, vertexUniforms)
    collectUniforms(fragmentBody, fragmentUniforms)
    fragmentWrites.forEach { collectUniforms(it.value, fragmentUniforms) }
    return ShaderProgram(
        target = programTarget,
        vertex = renderStage(
            inputs = attributes.map { "in ${glslDeclaration(it.shape)} ${attributeNames.getValue(it)};" },
            outputs = writes.map { "out ${glslDeclaration(it.varying.shape)} ${varyingNames.getValue(it.varying)};" },
            uniforms = bindings.filter { it.uniform in vertexUniforms },
            declarations = vertexEmitter.declarations,
            statements = vertexAssignments.map { (name, value) -> "  $name = $value;" } +
                "  gl_Position = $positionText;",
            functions = renderGlslFunctions(
                functionsForStage(
                    functions,
                    AuthoringPlace.Vertex,
                    writes.map { it.value } + position,
                ),
                occupied,
                uniformNames,
                attributeNames,
                varyingNames,
            ),
            outputName = null,
            outputValue = null,
            blockText = blockText(
                block,
                writes.map { it.value } + position,
            ),
            version = version,
        ),
        fragment = renderStage(
            inputs = writes.map { "in ${glslDeclaration(it.varying.shape)} ${varyingNames.getValue(it.varying)};" },
            outputs = if (orderedWrites.isEmpty()) {
                listOf("out ${glslDeclaration(fragmentBody.shape)} oColor;")
            } else {
                orderedWrites.map { write ->
                    val output = write.output
                    "layout(location = ${output.location}) out ${glslDeclaration(output.shape)} " +
                        "${outputNames.getValue(output)};"
                }
            },
            uniforms = bindings.filter { it.uniform in fragmentUniforms },
            declarations = fragmentEmitter.declarations,
            statements = outputText.map { (name, value) -> "  $name = $value;" },
            functions = renderGlslFunctions(
                functionsForStage(
                    functions,
                    AuthoringPlace.Fragment,
                    listOf(fragmentBody) + fragmentWrites.map { it.value },
                ),
                occupied,
                uniformNames,
                attributeNames,
                varyingNames,
            ),
            outputName = if (orderedWrites.isEmpty()) "oColor" else null,
            outputValue = if (orderedWrites.isEmpty()) fragmentText else null,
            blockText = blockText(
                block,
                listOf(fragmentBody) + fragmentWrites.map { it.value },
            ),
            version = version,
        ),
        bindings = bindings,
        uniformBlock = block,
        geometryText = geometry,
        tessControlText = tessControl,
        tessEvalText = tessEval,
    )
}

private fun blockText(block: UniformBlock?, roots: List<Expr<*>>): String {
    if (block == null || !referencesBlock(roots)) return ""
    return buildString {
        append("layout(std140) uniform ").append(block.typeName).append(" {\n")
        for (member in block.members) {
            append("  ").append(glslDeclaration(member.shape)).append(' ')
                .append(member.memberName).append(";\n")
        }
        append("} ").append(block.instanceName).append(";\n")
    }
}

private fun referencesBlock(roots: List<Expr<*>>): Boolean {
    var found = false
    roots.forEach { root ->
        walk(root, linkedSetOf()) { node ->
            if (node is ExprNode.BlockRef) found = true
        }
    }
    return found
}

internal fun collectVaryings(expr: Expr<*>, into: MutableSet<Varying<*>>) {
    walk(expr, linkedSetOf()) { node ->
        if (node is ExprNode.VaryingRef) into += node.varying
    }
}

internal fun checkFunctionStage(expr: Expr<*>, stage: AuthoringPlace) {
    walk(expr, linkedSetOf()) { node ->
        if (node is ExprNode.UserCall && node.function.stage != stage && isStageDependent(node.function)) {
            throw ProgramException(
                ProgramCode.FunctionWrongStage,
                "Function \"${node.function.name}\" is not defined in this stage",
            )
        }
    }
}

internal fun rejectRecursion(functions: List<UserFunction>) {
    for (function in functions) {
        val callees = linkedSetOf<UserFunction>()
        walk(function.body, linkedSetOf(function)) { node ->
            if (node is ExprNode.UserCall) callees += node.function
        }
        if (function in callees) {
            throw ProgramException(
                ProgramCode.RecursiveFunction,
                "Function \"${function.name}\" recurses",
            )
        }
    }
}

private fun functionsForStage(
    functions: List<UserFunction>,
    stage: AuthoringPlace,
    roots: List<Expr<*>>,
): List<UserFunction> {
    val reachable = linkedSetOf<UserFunction>()
    roots.forEach { root ->
        walk(root, linkedSetOf()) { node ->
            if (node is ExprNode.UserCall) reachable += node.function
        }
    }
    val owned = functions.filter { it.stage == stage }
    val copies = reachable.filter { it.stage != stage && !isStageDependent(it) }
    return owned + copies
}

private fun isStageDependent(function: UserFunction): Boolean {
    var dependent = false
    walk(function.body, linkedSetOf()) { node ->
        if (isStageNode(node)) dependent = true
    }
    return dependent
}

private fun isStageNode(node: ExprNode): Boolean = when (node) {
    is ExprNode.AttributeRef,
    is ExprNode.Texture,
    is ExprNode.Sample,
    is ExprNode.UnclampedSample,
    ExprNode.FragCoord,
    ExprNode.Resolution,
    -> true
    else -> false
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
        is ExprNode.UnclampedSample -> walk(node.coord, seen, visit)
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
        is ExprNode.BlockRef,
        is ExprNode.GlIn,
        ExprNode.TessCoord,
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
        is ExprNode.BlockRef -> "${node.member.instanceName}.${node.member.memberName}"
        is ExprNode.GlIn -> "gl_in[${node.index}].gl_Position"
        ExprNode.TessCoord -> "gl_TessCoord"
        is ExprNode.Compare -> spellCompare(node.op, node.left, node.right, ::emit)
        is ExprNode.Select -> "(${emit(node.condition)} ? ${emit(node.ifTrue)} : ${emit(node.ifFalse)})"
        is ExprNode.UserCall -> {
            val args = node.args.joinToString(", ") { emit(it) }
            "${node.function.name}($args)"
        }
        is ExprNode.Sample,
        is ExprNode.UnclampedSample,
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
    blockText: String,
    version: Int = GLSL_300,
): String = buildString {
    append("#version ").append(version).append(" es\n")
    append("precision highp float;\n")
    for (line in inputs) append(line).append('\n')
    for (binding in uniforms) {
        append("uniform ").append(glslDeclaration(binding.uniform.shape)).append(' ')
            .        append(binding.agslName).append(";\n")
    }
    append(blockText)
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
    ArithOp.And -> "&&"
    ArithOp.Or -> "||"
}

private fun glslLocalBase(suggested: String?, index: Int): String {
    if (suggested.isNullOrBlank()) return "l$index"
    val safe = suggested.first().let { it.isLetter() || it == '_' } &&
        suggested.all { it == '_' || it.isLetterOrDigit() }
    return if (safe) suggested else sanitizeSuggestedIdentifier(suggested, "l")
}

internal fun spellCompute(
    localSizeX: Int,
    storage: StorageBlock?,
    writes: List<StorageAssignment>,
    uniforms: List<Uniform<*>>,
    names: IdentifierAllocator,
): ShaderProgram {
    val bindings = uniforms.map { uniform ->
        UniformBinding(uniform, names.reserve(sanitizeIdentifier(uniform.name ?: "value", "u_")))
    }
    val uniformNames = bindings.associate { it.uniform to it.agslName }
    val occupied = names.snapshot()
    val emitter = GlslEmitter(IdentifierAllocator(occupied), uniformNames, emptyMap(), emptyMap())
    val statements = writes.map { write ->
        val left = emitter.emit(write.target)
        val right = emitter.emit(write.value)
        "  $left = $right;"
    }
    val used = linkedSetOf<Uniform<*>>()
    writes.forEach { write ->
        collectUniforms(write.target, used)
        collectUniforms(write.value, used)
    }
    val source = buildString {
        append("#version 310 es\n")
        append("precision highp float;\n")
        for (binding in bindings.filter { it.uniform in used }) {
            append("uniform ").append(glslDeclaration(binding.uniform.shape)).append(' ')
                .append(binding.agslName).append(";\n")
        }
        append(storageText(storage))
        append("layout(local_size_x = ").append(localSizeX).append(") in;\n")
        append("void main() {\n")
        for (line in emitter.declarations) append(line).append('\n')
        for (line in statements) append(line).append('\n')
        append("}\n")
    }
    return ShaderProgram(
        target = ShaderTarget.Gles31,
        bindings = bindings,
        storageBlock = storage,
        computeSourceText = source,
    )
}

private fun storageText(block: StorageBlock?): String {
    if (block == null) return ""
    return buildString {
        append("layout(std430, binding = 0) buffer ").append(block.typeName).append(" {\n")
        for (member in block.members) {
            append("  ").append(glslDeclaration(member.shape)).append(' ')
                .append(member.memberName).append(";\n")
        }
        append("} ").append(block.instanceName).append(";\n")
    }
}

internal fun spellGeometry(stage: GeometryStage): String = spellPrimitive(
    header = listOf(
        "layout(${stage.input.glslName}) in;",
        "layout(${stage.output.glslName}, max_vertices = ${stage.maxVertices}) out;",
    ),
    commands = stage.commands,
)

internal fun spellTessControl(stage: TessControlStage): String = spellPrimitive(
    header = listOf("layout(vertices = ${stage.vertices}) out;"),
    commands = stage.commands,
)

internal fun spellTessEval(stage: TessEvalStage): String {
    val primitive = when (stage.primitive) {
        TessPrimitive.Triangles -> "triangles"
        TessPrimitive.Quads -> "quads"
        TessPrimitive.Isolines -> "isolines"
    }
    val spacing = when (stage.spacing) {
        TessSpacing.Equal -> "equal_spacing"
        TessSpacing.FractionalEven -> "fractional_even_spacing"
        TessSpacing.FractionalOdd -> "fractional_odd_spacing"
    }
    val order = when (stage.order) {
        TessVertexOrder.Ccw -> "ccw"
        TessVertexOrder.Cw -> "cw"
    }
    val layout = if (stage.primitive == TessPrimitive.Isolines) {
        "layout($primitive, $spacing) in;"
    } else {
        "layout($primitive, $spacing, $order) in;"
    }
    return spellPrimitive(listOf(layout), stage.commands)
}

internal fun rejectGlInPastPatch(expr: Expr<*>, vertices: Int) {
    walk(expr, linkedSetOf()) { node ->
        if (node is ExprNode.GlIn) {
            require(node.index < vertices) {
                "gl_in index ${node.index} is outside the patch of $vertices vertices"
            }
        }
    }
}

private fun spellPrimitive(header: List<String>, commands: List<PrimitiveCommand>): String {
    commands.forEach { command ->
        when (command) {
            is PrimitiveCommand.Position -> rejectUserCalls(command.value)
            is PrimitiveCommand.OuterLevel -> rejectUserCalls(command.value)
            is PrimitiveCommand.InnerLevel -> rejectUserCalls(command.value)
            PrimitiveCommand.EmitVertex, PrimitiveCommand.EndPrimitive, PrimitiveCommand.PassPosition -> Unit
        }
    }
    val emitter = GlslEmitter(IdentifierAllocator(emptySet()), emptyMap(), emptyMap(), emptyMap())
    val lines = commands.map { command ->
        when (command) {
            is PrimitiveCommand.Position -> "  gl_Position = ${emitter.emit(command.value)};"
            PrimitiveCommand.EmitVertex -> "  EmitVertex();"
            PrimitiveCommand.EndPrimitive -> "  EndPrimitive();"
            is PrimitiveCommand.OuterLevel ->
                "  gl_TessLevelOuter[${command.index}] = ${emitter.emit(command.value)};"
            is PrimitiveCommand.InnerLevel ->
                "  gl_TessLevelInner[${command.index}] = ${emitter.emit(command.value)};"
            PrimitiveCommand.PassPosition ->
                "  gl_out[gl_InvocationID].gl_Position = gl_in[gl_InvocationID].gl_Position;"
        }
    }
    return buildString {
        append("#version 320 es\n")
        append("precision highp float;\n")
        header.forEach { append(it).append('\n') }
        append("void main() {\n")
        emitter.declarations.forEach { append(it).append('\n') }
        lines.forEach { append(it).append('\n') }
        append("}\n")
    }
}

private fun rejectUserCalls(expr: Expr<*>) {
    walk(expr, linkedSetOf()) { node ->
        if (node is ExprNode.UserCall) {
            throw ProgramException(
                ProgramCode.FunctionWrongStage,
                "Geometry and tessellation stages cannot call functions",
            )
        }
    }
}

internal fun glslReservedNames(): Set<String> = buildSet {
    addAll(RESERVED_USER_FUNCTION_NAMES)
    add("main")
    add("gl_Position")
    add("oColor")
    add("texture")
}
