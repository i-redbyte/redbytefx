package ru.redbyte.redbytefx

import java.util.IdentityHashMap

internal class LinkedStages(
    val vertexStatements: List<PrimitiveCommand> = emptyList(),
    val fragmentStatements: List<PrimitiveCommand> = emptyList(),
    val geometry: GeometryStage? = null,
    val tessControl: TessControlStage? = null,
    val tessEval: TessEvalStage? = null,
)

internal fun linkGlsl(
    uniforms: List<Uniform<*>>,
    varyings: List<Varying<*>>,
    attributes: List<AttributeHandle>,
    writes: List<VaryingWrite>,
    position: Expr<*>?,
    fragmentBody: Expr<*>,
    names: IdentifierAllocator,
    functions: List<UserFunction>,
    fragmentWrites: List<FragmentWrite>,
    blocks: List<UniformBlock>,
    stages: LinkedStages = LinkedStages(),
    version: Int = GLSL_300,
    programTarget: ShaderTarget = ShaderTarget.Gles30,
): ShaderProgram {
    val vertexStatements = stages.vertexStatements
    val fragmentStatements = stages.fragmentStatements
    val geometry = stages.geometry
    val tessControl = stages.tessControl
    val tessEval = stages.tessEval
    val bindings = uniforms.map { uniform ->
        UniformBinding(uniform, spelledUniformName(uniform.name, names))
    }
    val attributeNames = attributes.associateWith { attribute ->
        if (attribute.name == "corner") {
            RB_SCREEN_CORNER_ATTRIB
        } else {
            names.reserve(sanitizeIdentifier(attribute.name, "a_"))
        }
    }
    val varyingNames = varyings.associateWith { varying ->
        names.reserve(sanitizeIdentifier(varying.name, "v_"))
    }
    val uniformNames = bindings.associate { it.uniform to it.agslName }
    val outputNames = fragmentWrites.associate { write ->
        write.output to names.reserve(sanitizeSuggestedIdentifier(write.output.name, "o"))
    }
    val readers = linkedSetOf<Varying<*>>()
    collectVaryingUses(fragmentBody, readers)
    fragmentWrites.forEach { collectVaryingUses(it.value, readers) }
    commandExprs(fragmentStatements).forEach { collectVaryingUses(it, readers) }
    listOfNotNull(geometry?.commands, tessControl?.commands, tessEval?.commands).forEach { commands ->
        commandExprs(commands).forEach { collectVaryingUses(it, readers) }
    }
    val vertexWritten = writes.map { it.varying }.toSet() + writtenVaryings(vertexStatements)
    val pipeMembers = if (geometry != null || tessControl != null || tessEval != null) {
        varyings.filter { it in vertexWritten && it in readers }
    } else {
        emptyList()
    }
    val piped = pipeMembers.isNotEmpty()
    if (piped) {
        listOf("rb_pipe", "vs_out", "fs_in", "gs_in", "gs_out", "tc_in", "tc_out", "te_in", "te_out")
            .forEach(names::reserve)
    }
    val occupied = names.snapshot()
    val fragmentRead: (Varying<*>) -> String = { varying ->
        if (piped) "fs_in.${varyingNames.getValue(varying)}" else varyingNames.getValue(varying)
    }
    val vertexEmitter = GlslEmitter(IdentifierAllocator(occupied), uniformNames, attributeNames, varyingNames)
    val fragmentEmitter = GlslEmitter(
        IdentifierAllocator(occupied),
        uniformNames,
        attributeNames,
        varyingNames,
        varyingRead = fragmentRead,
    )
    val assignmentPrefix = if (piped) "vs_out." else ""
    val visibleWrites = if (piped) writes.filter { it.varying in pipeMembers } else writes
    bindLocalSlots(vertexStatements, vertexEmitter)
    bindLocalSlots(fragmentStatements, fragmentEmitter)
    val vertexAssignments = visibleWrites.map { write ->
        "$assignmentPrefix${varyingNames.getValue(write.varying)}" to vertexEmitter.emit(write.value)
    }
    val positionText = position?.let { vertexEmitter.emit(it) }
    val fragmentText = fragmentEmitter.emit(fragmentBody)
    val orderedWrites = fragmentWrites.sortedBy { it.output.location }
    val outputText = orderedWrites.map { write ->
        outputNames.getValue(write.output) to fragmentEmitter.emit(write.value)
    }
    val vertexUniforms = linkedSetOf<Uniform<*>>()
    val fragmentUniforms = linkedSetOf<Uniform<*>>()
    writes.forEach { collectUniforms(it.value, vertexUniforms) }
    if (position != null) collectUniforms(position, vertexUniforms)
    commandExprs(vertexStatements).forEach { collectUniforms(it, vertexUniforms) }
    collectUniformsFromStageFunctions(functions, AuthoringPlace.Vertex, vertexUniforms)
    collectUniforms(fragmentBody, fragmentUniforms)
    fragmentWrites.forEach { collectUniforms(it.value, fragmentUniforms) }
    commandExprs(fragmentStatements).forEach { collectUniforms(it, fragmentUniforms) }
    collectUniformsFromStageFunctions(functions, AuthoringPlace.Fragment, fragmentUniforms)
    bindings.firstOrNull { it.agslName == RB_RESOLUTION_UNIFORM }?.let { fragmentUniforms += it.uniform }
    val vertexExtra = spellCommands(
        vertexStatements,
        vertexEmitter,
        varyingOut = { varying -> "$assignmentPrefix${varyingNames.getValue(varying)}" },
        skipVarying = { varying -> piped && varying !in pipeMembers },
    )
    val fragmentExtra = spellCommands(fragmentStatements, fragmentEmitter, varyingOut = fragmentRead)
    return ShaderProgram(
        target = programTarget,
        vertex = renderStage(
            inputs = attributes.map { "in ${glslDeclaration(it.shape)} ${attributeNames.getValue(it)};" },
            outputs = vertexPipeOutputs(piped, visibleWrites, pipeMembers, varyingNames, vertexStatements, position),
            uniforms = bindings.filter { it.uniform in vertexUniforms },
            declarations = vertexEmitter.declarations,
            statements = vertexAssignments.map { (name, value) -> "  $name = $value;" } +
                vertexExtra +
                listOfNotNull(positionText?.let { "  gl_Position = $it;" }),
            functions = renderGlslFunctions(
                functionsForStage(
                    functions,
                    AuthoringPlace.Vertex,
                    writes.map { it.value } + listOfNotNull(position) + commandExprs(vertexStatements),
                ),
                occupied,
                uniformNames,
                attributeNames,
                varyingNames,
            ),
            outputName = null,
            outputValue = null,
            blockText = blockText(
                blocks,
                exprRootsForStage(
                    writes.map { it.value } + listOfNotNull(position) + commandExprs(vertexStatements),
                    functions,
                    AuthoringPlace.Vertex,
                ),
                version,
            ),
            version = version,
        ),
        fragment = renderStage(
            inputs = pipeOutputs(piped, visibleWrites, pipeMembers, varyingNames, "fs_in", array = false, qualifier = "in"),
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
            statements = fragmentExtra + outputText.map { (name, value) -> "  $name = $value;" },
            functions = renderGlslFunctions(
                functionsForStage(
                    functions,
                    AuthoringPlace.Fragment,
                    listOf(fragmentBody) + fragmentWrites.map { it.value } + commandExprs(fragmentStatements),
                ),
                occupied,
                uniformNames,
                attributeNames,
                varyingNames,
                varyingRead = fragmentRead,
            ),
            outputName = if (orderedWrites.isEmpty()) "oColor" else null,
            outputValue = if (orderedWrites.isEmpty()) fragmentText else null,
            blockText = blockText(
                blocks,
                exprRootsForStage(
                    listOf(fragmentBody) + fragmentWrites.map { it.value },
                    functions,
                    AuthoringPlace.Fragment,
                ),
                version,
            ),
            version = version,
        ),
        bindings = bindings,
        uniformBlocks = blocks,
        geometryText = geometry?.let {
            spellGeometry(it, uniformNames, varyingNames, functions, occupied, pipeMembers, blocks)
        },
        tessControlText = tessControl?.let {
            spellTessControl(it, uniformNames, varyingNames, functions, occupied, pipeMembers, blocks)
        },
        tessEvalText = tessEval?.let {
            spellTessEval(
                it,
                uniformNames,
                varyingNames,
                functions,
                occupied,
                pipeMembers,
                blocks,
                tessControl?.vertices ?: 0,
            )
        },
    )
}

private fun vertexPipeOutputs(
    piped: Boolean,
    writes: List<VaryingWrite>,
    members: List<Varying<*>>,
    names: Map<Varying<*>, String>,
    statements: List<PrimitiveCommand>,
    position: Expr<*>?,
): List<String> {
    val block = pipeOutputs(piped, writes, members, names, "vs_out", array = false)
    if (!piped || (position == null && !writesGlPosition(statements))) return block
    return listOf(VERTEX_PER_VERTEX) + block
}

private fun pipeOutputs(
    piped: Boolean,
    writes: List<VaryingWrite>,
    members: List<Varying<*>>,
    names: Map<Varying<*>, String>,
    instance: String,
    array: Boolean,
    qualifier: String = "out",
): List<String> {
    if (!piped) {
        return writes.map { write ->
            "$qualifier ${glslDeclaration(write.varying.shape)} ${names.getValue(write.varying)};"
        }
    }
    if (members.isEmpty()) return emptyList()
    return listOf(pipeBlock(qualifier, instance, array, members, names))
}

private fun pipeBlock(
    qualifier: String,
    instance: String,
    array: Boolean,
    members: List<Varying<*>>,
    names: Map<Varying<*>, String>,
): String = buildString {
    append(qualifier).append(" rb_pipe {\n")
    for (member in members) {
        append("  ").append(glslDeclaration(member.shape)).append(' ')
            .append(names.getValue(member)).append(";\n")
    }
    append("} ").append(instance)
    if (array) append("[]")
    append(';')
}

private const val VERTEX_PER_VERTEX = "out gl_PerVertex {\n  vec4 gl_Position;\n};"

private fun blockText(blocks: List<UniformBlock>, roots: List<Expr<*>>, version: Int): String {
    if (blocks.isEmpty()) return ""
    val used = referencedMembers(roots)
    return buildString {
        for (block in blocks) {
            if (block.members.none { it in used }) continue
            append("layout(std140")
            if (version >= GLSL_310) append(", binding = ").append(block.binding)
            append(") uniform ")
                .append(block.typeName).append(" {\n")
            for (member in block.members) {
                append("  ").append(memberDeclaration(member)).append(";\n")
            }
            append("} ").append(block.instanceName).append(";\n")
        }
    }
}

private fun referencedMembers(roots: List<Expr<*>>): Set<BlockMember> {
    val found = mutableSetOf<BlockMember>()
    roots.forEach { root ->
        walk(root, linkedSetOf()) { node ->
            when (node) {
                is ExprNode.BlockRef -> found += node.member
                is ExprNode.Index -> found += node.member
                else -> Unit
            }
        }
    }
    return found
}

internal fun exprUsesResolution(expr: Expr<*>): Boolean {
    var used = false
    walk(expr, linkedSetOf()) { node ->
        if (node == ExprNode.Resolution) used = true
    }
    return used
}

internal fun collectVaryings(expr: Expr<*>, into: MutableSet<Varying<*>>) {
    collectVaryingUses(expr, into)
}

internal fun collectVaryingUses(expr: Expr<*>, into: MutableSet<Varying<*>>) {
    walk(expr, linkedSetOf()) { node ->
        when (node) {
            is ExprNode.VaryingRef -> into += node.varying
            is ExprNode.VaryingAt -> into += node.varying
            else -> Unit
        }
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
    if (statementsDepend(function.statements)) dependent = true
    return dependent
}

private fun statementsDepend(commands: List<PrimitiveCommand>): Boolean = commands.any { command ->
    when (command) {
        PrimitiveCommand.Discard, is PrimitiveCommand.DiscardIf, PrimitiveCommand.Barrier,
        PrimitiveCommand.EmitVertex, PrimitiveCommand.EndPrimitive, PrimitiveCommand.PassPosition,
        -> true
        is PrimitiveCommand.Repeat -> statementsDepend(command.body)
        is PrimitiveCommand.When -> statementsDepend(command.body)
        is PrimitiveCommand.Position, is PrimitiveCommand.Store, is PrimitiveCommand.VaryingSet,
        is PrimitiveCommand.OuterLevel, is PrimitiveCommand.InnerLevel, is PrimitiveCommand.LocalSet,
        -> false
    }
}

private fun isStageNode(node: ExprNode): Boolean = when (node) {
    is ExprNode.AttributeRef,
    is ExprNode.Texture,
    is ExprNode.TextureCube,
    is ExprNode.Sample,
    is ExprNode.UnclampedSample,
    is ExprNode.GlIn,
    is ExprNode.VaryingAt,
    is ExprNode.Invocation,
    ExprNode.FragCoord,
    ExprNode.Resolution,
    ExprNode.TessCoord,
    -> true
    is ExprNode.Call -> node.function == "dFdx" || node.function == "dFdy" || node.function == "fwidth"
    else -> false
}

private fun collectUniforms(expr: Expr<*>, into: MutableSet<Uniform<*>>) {
    walk(expr, linkedSetOf()) { node ->
        if (node is ExprNode.UniformRef) into += node.uniform
    }
}

private fun exprRootsForStage(
    roots: List<Expr<*>>,
    functions: List<UserFunction>,
    stage: AuthoringPlace,
): List<Expr<*>> = buildList {
    addAll(roots)
    for (function in functions) {
        if (function.stage != stage) continue
        add(function.body)
        addAll(commandExprs(function.statements))
    }
}

private fun collectUniformsFromStageFunctions(
    functions: List<UserFunction>,
    stage: AuthoringPlace,
    into: MutableSet<Uniform<*>>,
) {
    for (function in functions) {
        if (function.stage != stage) continue
        collectUniforms(function.body, into)
        commandExprs(function.statements).forEach { collectUniforms(it, into) }
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
        is ExprNode.TextureCube -> {
            walk(node.sampler, seen, visit)
            walk(node.direction, seen, visit)
        }
        is ExprNode.GlIn -> walk(node.index, seen, visit)
        is ExprNode.VaryingAt -> walk(node.index, seen, visit)
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
            if (seen.add(node.function)) {
                walk(node.function.body, seen, visit)
                commandExprs(node.function.statements).forEach { walk(it, seen, visit) }
            }
        }
        is ExprNode.Index -> walk(node.index, seen, visit)
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

internal interface CodeEmitter {
    fun emit(expr: Expr<*>): String

    fun bindSlot(slot: LocalSlot)

    fun slotName(slot: LocalSlot): String

    val declarations: MutableList<String>
}

private class GlslEmitter(
    private val allocator: IdentifierAllocator,
    private val uniforms: Map<Uniform<*>, String>,
    private val attributes: Map<AttributeHandle, String>,
    private val varyings: Map<Varying<*>, String>,
    private val varyingRead: (Varying<*>) -> String = { varying -> varyings.getValue(varying) },
    private val varyingAt: (Varying<*>, String) -> String = { _, _ -> "" },
) : CodeEmitter {
    private val localNames = IdentityHashMap<ExprNode.Local, String>()
    private var localIndex = 0
    private val slotNames = IdentityHashMap<LocalSlot, String>()
    private var slotIndex = 0
    override val declarations = mutableListOf<String>()

    override fun emit(expr: Expr<*>): String = when (val node = expr.node) {
        is ExprNode.Literal -> formatFloat(node.value)
        is ExprNode.IntLiteral -> node.value.toString()
        is ExprNode.Swizzle -> "${emit(node.source)}.${node.mask}"
        is ExprNode.Cast -> "${spell(expr.shape, ShaderTarget.Gles30)}(${emit(node.arg)})"
        is ExprNode.Unary -> when (node.op) {
            UnaryOp.Neg -> "(-${emit(node.arg)})"
            UnaryOp.Not -> "(!${emit(node.arg)})"
        }
        is ExprNode.Binary -> "(${emit(node.left)} ${arithSymbol(node.op)} ${emit(node.right)})"
        is ExprNode.Construct -> {
            val args = node.args.joinToString(", ") { emit(it) }
            "${spell(expr.shape, ShaderTarget.Gles30)}($args)"
        }
        is ExprNode.Local -> local(node, expr.shape)
        is ExprNode.UniformRef -> uniforms[node.uniform] ?: throw ProgramException(
            ProgramCode.ForeignUniform,
            "Uniform \"${node.uniform.name}\" does not belong to this shader",
        )
        is ExprNode.AttributeRef -> attributes.getValue(node.attribute)
        is ExprNode.VaryingRef -> varyingRead(node.varying)
        is ExprNode.VaryingAt -> varyingAt(node.varying, emit(node.index))
        is ExprNode.SlotRef -> slotName(node.slot)
        is ExprNode.Index -> indexText(node)
        is ExprNode.Invocation -> invocationText(node.kind)
        is ExprNode.Texture -> "texture(${emit(node.sampler)}, ${emit(node.uv)})"
        is ExprNode.TextureCube -> "texture(${emit(node.sampler)}, ${emit(node.direction)})"
        is ExprNode.Call -> {
            val args = node.args.joinToString(", ") { emit(it) }
            "${node.function}($args)"
        }
        is ExprNode.Param -> node.name
        is ExprNode.BlockRef -> "${node.member.instanceName}.${node.member.memberName}"
        is ExprNode.GlIn -> "gl_in[${emit(node.index)}].gl_Position"
        ExprNode.TessCoord -> "gl_TessCoord"
        is ExprNode.Compare -> spellCompare(node.op, node.left, node.right, ::emit)
        is ExprNode.Select -> "(${emit(node.condition)} ? ${emit(node.ifTrue)} : ${emit(node.ifFalse)})"
        is ExprNode.UserCall -> {
            val args = node.args.joinToString(", ") { emit(it) }
            "${node.function.name}($args)"
        }
        ExprNode.FragCoord -> "gl_FragCoord.xy"
        ExprNode.Resolution -> RB_RESOLUTION_UNIFORM
        is ExprNode.Sample,
        is ExprNode.UnclampedSample,
        -> error("GLSL stage cannot spell ${node::class.simpleName}")
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

    override fun bindSlot(slot: LocalSlot) {
        if (slotNames.containsKey(slot)) return
        val name = allocator.reserve(glslLocalBase(slot.suggestedName, slotIndex))
        slotIndex += 1
        slotNames[slot] = name
        declarations += "  ${glslDeclaration(slot.shape)} $name;"
    }

    override fun slotName(slot: LocalSlot): String {
        bindSlot(slot)
        return slotNames.getValue(slot)
    }

    private fun indexText(node: ExprNode.Index): String {
        val index = emit(node.index)
        val member = node.member
        return if (member.shared) {
            "${member.memberName}[$index]"
        } else {
            "${member.instanceName}.${member.memberName}[$index]"
        }
    }
}

private fun invocationText(kind: InvocationKind): String = when (kind) {
    InvocationKind.Global -> "ivec3(gl_GlobalInvocationID)"
    InvocationKind.Local -> "ivec3(gl_LocalInvocationID)"
    InvocationKind.WorkGroup -> "ivec3(gl_WorkGroupID)"
}

private fun renderGlslFunctions(
    functions: List<UserFunction>,
    occupied: Set<String>,
    uniforms: Map<Uniform<*>, String>,
    attributes: Map<AttributeHandle, String>,
    varyings: Map<Varying<*>, String>,
    varyingRead: (Varying<*>) -> String = { varying -> varyings.getValue(varying) },
    varyingAt: (Varying<*>, String) -> String = { varying, index -> "${varyings.getValue(varying)}[$index]" },
): String = buildString {
    for (function in functions) {
        val locals = IdentifierAllocator(occupied + function.parameters.map { it.name })
        val emitter = GlslEmitter(locals, uniforms, attributes, varyings, varyingRead, varyingAt)
        bindLocalSlots(function.statements, emitter)
        val body = emitter.emit(function.body)
        val signature = function.parameters.joinToString(", ") { "${glslDeclaration(it.shape)} ${it.name}" }
        val statements = spellCommands(function.statements, emitter, varyingOut = { varying ->
            varyings.getValue(varying)
        })
        append(glslDeclaration(function.result)).append(' ').append(function.name)
            .append('(').append(signature).append(") {\n")
        emitter.declarations.forEach { append(it).append('\n') }
        statements.forEach { append(it).append('\n') }
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
            .append(binding.agslName).append(";\n")
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
    layout: ComputeLayout,
    storage: List<StorageBlock>,
    statements: List<PrimitiveCommand>,
    uniforms: List<Uniform<*>>,
    names: IdentifierAllocator,
    functions: List<UserFunction>,
    blocks: List<UniformBlock>,
    shared: List<BlockMember>,
): ShaderProgram {
    val bindings = uniforms.map { uniform ->
        UniformBinding(uniform, spelledUniformName(uniform.name, names))
    }
    val uniformNames = bindings.associate { it.uniform to it.agslName }
    val occupied = names.snapshot()
    val emitter = GlslEmitter(IdentifierAllocator(occupied), uniformNames, emptyMap(), emptyMap())
    bindLocalSlots(statements, emitter)
    val lines = spellCommands(statements, emitter, varyingOut = { varying -> varying.name })
    val roots = commandExprs(statements)
    val used = linkedSetOf<Uniform<*>>()
    roots.forEach { collectUniforms(it, used) }
    collectUniformsFromStageFunctions(functions, AuthoringPlace.Compute, used)
    val functionText = renderGlslFunctions(
        functionsForStage(functions, AuthoringPlace.Compute, roots),
        occupied,
        uniformNames,
        emptyMap(),
        emptyMap(),
    )
    val source = buildString {
        append("#version 310 es\n")
        append("precision highp float;\n")
        for (binding in bindings.filter { it.uniform in used }) {
            append("uniform ").append(glslDeclaration(binding.uniform.shape)).append(' ')
                .append(binding.agslName).append(";\n")
        }
        append(
            blockText(
                blocks,
                exprRootsForStage(roots, functions, AuthoringPlace.Compute),
                GLSL_310,
            ),
        )
        append(storageText(storage))
        for (member in shared) {
            append("shared ").append(glslDeclaration(member.shape)).append(' ')
                .append(member.memberName).append('[').append(member.arraySize).append("];\n")
        }
        append(localSizeText(layout))
        append(functionText)
        append("void main() {\n")
        for (line in emitter.declarations) append(line).append('\n')
        for (line in lines) append(line).append('\n')
        append("}\n")
    }
    return ShaderProgram(
        target = ShaderTarget.Gles31,
        bindings = bindings,
        uniformBlocks = blocks,
        storageBlocks = storage,
        computeSourceText = source,
    )
}

private fun localSizeText(layout: ComputeLayout): String = if (layout.y == null && layout.z == null) {
    "layout(local_size_x = ${layout.x}) in;\n"
} else {
    "layout(local_size_x = ${layout.x}, local_size_y = ${layout.y ?: 1}, local_size_z = ${layout.z ?: 1}) in;\n"
}

private fun storageText(blocks: List<StorageBlock>): String = buildString {
    for (block in blocks) {
        append("layout(std430, binding = ").append(block.binding).append(") buffer ")
            .append(block.typeName).append(" {\n")
        for (member in block.members) {
            append("  ").append(memberDeclaration(member)).append(";\n")
        }
        append("} ").append(block.instanceName).append(";\n")
    }
}

private fun memberDeclaration(member: BlockMember): String {
    val base = "${glslDeclaration(member.shape)} ${member.memberName}"
    return when {
        member.unsized -> "$base[]"
        member.arraySize > 0 -> "$base[${member.arraySize}]"
        else -> base
    }
}

internal fun spellGeometry(
    stage: GeometryStage,
    uniformNames: Map<Uniform<*>, String>,
    varyingNames: Map<Varying<*>, String>,
    functions: List<UserFunction>,
    occupied: Set<String>,
    pipeMembers: List<Varying<*>>,
    blocks: List<UniformBlock>,
): String = spellPrimitive(
    header = listOf(
        "layout(${stage.input.glslName}) in;",
        "layout(${stage.output.glslName}, max_vertices = ${stage.maxVertices}) out;",
    ),
    commands = stage.commands,
    env = StageEnv(
        uniformNames, varyingNames, functions, occupied, pipeMembers, blocks, AuthoringPlace.Geometry,
        inName = "gs_in", outName = "gs_out", inArray = true, outArray = false,
        copy = PipeCopy(beforeEachEmit = true, perVertex = GEOMETRY_PER_VERTEX),
        varyingAt = { name, index -> "gs_in[$index].$name" },
        varyingOut = { name -> "gs_out.$name" },
    ),
)

internal fun spellTessControl(
    stage: TessControlStage,
    uniformNames: Map<Uniform<*>, String>,
    varyingNames: Map<Varying<*>, String>,
    functions: List<UserFunction>,
    occupied: Set<String>,
    pipeMembers: List<Varying<*>>,
    blocks: List<UniformBlock>,
): String = spellPrimitive(
    header = listOf("layout(vertices = ${stage.vertices}) out;"),
    commands = stage.commands,
    env = StageEnv(
        uniformNames, varyingNames, functions, occupied, pipeMembers, blocks, AuthoringPlace.TessControl,
        inName = "tc_in", outName = "tc_out", inArray = true, outArray = true,
        copy = PipeCopy(
            beforeEachEmit = false,
            perVertex = TESS_CONTROL_PER_VERTEX,
            prepend = { name -> "tc_out[gl_InvocationID].$name = tc_in[gl_InvocationID].$name;" },
        ),
        varyingAt = { name, index -> "tc_in[$index].$name" },
        varyingOut = { name -> "tc_out[gl_InvocationID].$name" },
    ),
)

internal fun spellTessEval(
    stage: TessEvalStage,
    uniformNames: Map<Uniform<*>, String>,
    varyingNames: Map<Varying<*>, String>,
    functions: List<UserFunction>,
    occupied: Set<String>,
    pipeMembers: List<Varying<*>>,
    blocks: List<UniformBlock>,
    patchVertices: Int,
): String {
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
    return spellPrimitive(
        listOf(layout),
        stage.commands,
        StageEnv(
            uniformNames, varyingNames, functions, occupied, pipeMembers, blocks, AuthoringPlace.TessEval,
            inName = "te_in", outName = "te_out", inArray = true, outArray = false,
            copy = PipeCopy(
                beforeEachEmit = false,
                perVertex = STAGE_PER_VERTEX,
                prepend = { name -> tessInterpolation(name, patchVertices, stage.primitive) },
            ),
            varyingAt = { name, index -> "te_in[$index].$name" },
            varyingOut = { name -> "te_out.$name" },
        ),
    )
}

internal fun rejectGlInPastPatch(expr: Expr<*>, vertices: Int) {
    walk(expr, linkedSetOf()) { node ->
        val index = when (node) {
            is ExprNode.GlIn -> node.index
            is ExprNode.VaryingAt -> node.index
            else -> null
        } ?: return@walk
        val literal = index.node as? ExprNode.IntLiteral ?: return@walk
        require(literal.value < vertices) {
            "gl_in index ${literal.value} is outside the patch of $vertices vertices"
        }
    }
}

private class PipeCopy(
    val beforeEachEmit: Boolean,
    val perVertex: List<String>,
    val prepend: (String) -> String = { "" },
)

private class StageEnv(
    val uniformNames: Map<Uniform<*>, String>,
    val varyingNames: Map<Varying<*>, String>,
    val functions: List<UserFunction>,
    val occupied: Set<String>,
    val pipeMembers: List<Varying<*>>,
    val blocks: List<UniformBlock>,
    val stage: AuthoringPlace,
    val inName: String,
    val outName: String,
    val inArray: Boolean,
    val outArray: Boolean,
    val copy: PipeCopy,
    val varyingAt: (String, String) -> String,
    val varyingOut: (String) -> String,
)

private fun spellPrimitive(
    header: List<String>,
    commands: List<PrimitiveCommand>,
    env: StageEnv,
): String {
    val roots = commandExprs(commands)
    val used = linkedSetOf<Uniform<*>>()
    roots.forEach { collectUniforms(it, used) }
    collectUniformsFromStageFunctions(env.functions, env.stage, used)
    val emitter = GlslEmitter(
        IdentifierAllocator(env.occupied),
        env.uniformNames,
        emptyMap(),
        env.varyingNames,
        varyingAt = { varying, index ->
            env.varyingAt(env.varyingNames.getValue(varying), index)
        },
    )
    bindLocalSlots(commands, emitter)
    val piped = env.pipeMembers.isNotEmpty()
    val forwarded = if (piped && !env.copy.beforeEachEmit) {
        val written = writtenVaryings(commands)
        env.pipeMembers.filter { it !in written }.map { member ->
            env.copy.prepend(env.varyingNames.getValue(member))
        }
    } else {
        emptyList()
    }
    val beforeEmit: (List<PrimitiveCommand>, Set<Varying<*>>, PrimitiveCommand.Position?) -> List<String> =
        if (piped && env.copy.beforeEachEmit) {
            { span, writtenBefore, inherited ->
                geometryForwards(span, writtenBefore, inherited, env, emitter)
            }
        } else {
            { _, _, _ -> emptyList() }
        }
    val lines = forwarded.map { "  $it" } + spellCommands(
        commands,
        emitter,
        varyingOut = { varying -> env.varyingOut(env.varyingNames.getValue(varying)) },
        beforeEmit = beforeEmit,
        skipVarying = { varying -> piped && varying !in env.pipeMembers },
    )
    val functionText = renderGlslFunctions(
        functionsForStage(env.functions, env.stage, roots),
        env.occupied,
        env.uniformNames,
        emptyMap(),
        env.varyingNames,
        varyingAt = { varying, index -> env.varyingAt(env.varyingNames.getValue(varying), index) },
    )
    return buildString {
        append("#version 320 es\n")
        append("precision highp float;\n")
        header.forEach { append(it).append('\n') }
        for (uniform in env.uniformNames) {
            if (uniform.key !in used) continue
            append("uniform ").append(glslDeclaration(uniform.key.shape)).append(' ')
                .append(uniform.value).append(";\n")
        }
        append(blockText(env.blocks, exprRootsForStage(roots, env.functions, env.stage), GLSL_320))
        if (piped && writesGlPosition(commands)) {
            env.copy.perVertex.forEach { append(it).append('\n') }
        }
        if (piped) {
            append(pipeInterface("in", env.inName, env.inArray, env.pipeMembers, env.varyingNames))
            append(pipeInterface("out", env.outName, env.outArray, env.pipeMembers, env.varyingNames))
        }
        append(functionText)
        append("void main() {\n")
        emitter.declarations.forEach { append(it).append('\n') }
        lines.forEach { append(it).append('\n') }
        append("}\n")
    }
}

private fun pipeInterface(
    qualifier: String,
    instance: String,
    array: Boolean,
    members: List<Varying<*>>,
    names: Map<Varying<*>, String>,
): String = buildString {
    append(qualifier).append(" rb_pipe {\n")
    for (member in members) {
        append("  ").append(glslDeclaration(member.shape)).append(' ')
            .append(names.getValue(member)).append(";\n")
    }
    append("} ").append(instance)
    if (array) append("[]")
    append(";\n")
}

internal fun spellStageStatements(
    commands: List<PrimitiveCommand>,
    emitter: CodeEmitter,
    varyingNames: Map<Varying<*>, String> = emptyMap(),
): List<String> {
    bindLocalSlots(commands, emitter)
    return spellCommands(
        commands,
        emitter,
        varyingOut = { varying -> varyingNames[varying] ?: varying.name },
    )
}

internal fun bindLocalSlots(commands: List<PrimitiveCommand>, emitter: CodeEmitter) {
    fun walk(list: List<PrimitiveCommand>) {
        for (command in list) {
            when (command) {
                is PrimitiveCommand.LocalSet -> emitter.bindSlot(command.slot)
                is PrimitiveCommand.Repeat -> walk(command.body)
                is PrimitiveCommand.When -> walk(command.body)
                else -> Unit
            }
        }
    }
    walk(commands)
}

private fun spellCommands(
    commands: List<PrimitiveCommand>,
    emitter: CodeEmitter,
    varyingOut: (Varying<*>) -> String,
    indent: String = "  ",
    beforeEmit: (List<PrimitiveCommand>, Set<Varying<*>>, PrimitiveCommand.Position?) -> List<String> =
        { _, _, _ -> emptyList() },
    alreadyWritten: Set<Varying<*>> = emptySet(),
    inheritedPosition: PrimitiveCommand.Position? = null,
    skipVarying: (Varying<*>) -> Boolean = { false },
): List<String> {
    val lines = mutableListOf<String>()
    val inline = indent.length > 2
    val span = mutableListOf<PrimitiveCommand>()
    val writtenBefore = alreadyWritten.toMutableSet()
    var carried = inheritedPosition
    fun spellChild(body: List<PrimitiveCommand>, command: PrimitiveCommand, childIndent: String) {
        val visible = lastPosition(span) ?: carried
        lines += spellCommands(
            body,
            emitter,
            varyingOut,
            childIndent,
            beforeEmit,
            writtenBefore + writtenVaryings(span),
            visible,
            skipVarying,
        )
        if (hasEmit(body)) {
            span.clear()
            carried = null
        }
        writtenBefore += writtenVaryings(body)
        span += command
    }
    for (command in commands) {
        when (command) {
            is PrimitiveCommand.Position -> {
                lines += assignment(indent, "gl_Position", emitter, command.value, inline)
                span += command
            }
            PrimitiveCommand.EmitVertex -> {
                beforeEmit(span.toList(), writtenBefore, carried).forEach { lines += "$indent$it" }
                lines += "${indent}EmitVertex();"
                writtenBefore += writtenVaryings(span)
                span.clear()
                carried = null
            }
            PrimitiveCommand.EndPrimitive -> {
                lines += "${indent}EndPrimitive();"
                span += command
            }
            is PrimitiveCommand.OuterLevel -> {
                lines += assignment(indent, "gl_TessLevelOuter[${command.index}]", emitter, command.value, inline)
                span += command
            }
            is PrimitiveCommand.InnerLevel -> {
                lines += assignment(indent, "gl_TessLevelInner[${command.index}]", emitter, command.value, inline)
                span += command
            }
            PrimitiveCommand.PassPosition -> {
                lines += "${indent}gl_out[gl_InvocationID].gl_Position = gl_in[gl_InvocationID].gl_Position;"
                span += command
            }
            is PrimitiveCommand.VaryingSet -> {
                if (!skipVarying(command.varying)) {
                    lines += assignment(indent, varyingOut(command.varying), emitter, command.value, inline)
                }
                span += command
            }
            is PrimitiveCommand.Store -> {
                val left = emitExpr(emitter, command.target, inline)
                val right = emitExpr(emitter, command.value, inline)
                left.first.forEach { lines += it }
                right.first.forEach { lines += it }
                lines += "$indent${left.second} = ${right.second};"
                span += command
            }
            is PrimitiveCommand.LocalSet -> {
                lines += assignment(indent, emitter.slotName(command.slot), emitter, command.value, inline)
                span += command
            }
            PrimitiveCommand.Discard -> {
                lines += "${indent}discard;"
                span += command
            }
            is PrimitiveCommand.DiscardIf -> {
                lines += "${indent}if (${emitter.emit(command.condition)}) discard;"
                span += command
            }
            PrimitiveCommand.Barrier -> {
                lines += "${indent}barrier();"
                span += command
            }
            is PrimitiveCommand.When -> {
                lines += "${indent}if (${emitter.emit(command.condition)}) {"
                spellChild(command.body, command, "$indent  ")
                lines += "$indent}"
            }
            is PrimitiveCommand.Repeat -> {
                val index = command.indexName
                lines += "${indent}for (int $index = 0; $index < ${command.count}; ++$index) {"
                spellChild(command.body, command, "$indent  ")
                lines += "$indent}"
            }
        }
    }
    return lines
}

private fun assignment(
    indent: String,
    target: String,
    emitter: CodeEmitter,
    value: Expr<*>,
    inline: Boolean,
): String {
    val emitted = emitExpr(emitter, value, inline)
    return emitted.first.joinToString("") { "$it\n" } + "$indent$target = ${emitted.second};"
}

private fun emitExpr(emitter: CodeEmitter, expr: Expr<*>, inline: Boolean): Pair<List<String>, String> {
    if (!inline) return emptyList<String>() to emitter.emit(expr)
    val before = emitter.declarations.size
    val text = emitter.emit(expr)
    val fresh = emitter.declarations.drop(before)
    if (before < emitter.declarations.size) {
        emitter.declarations.subList(before, emitter.declarations.size).clear()
    }
    return fresh to text
}

private fun lastPosition(commands: List<PrimitiveCommand>): PrimitiveCommand.Position? =
    commands.asReversed().firstNotNullOfOrNull { it as? PrimitiveCommand.Position }

private fun geometryForwards(
    span: List<PrimitiveCommand>,
    writtenBefore: Set<Varying<*>>,
    inherited: PrimitiveCommand.Position?,
    env: StageEnv,
    emitter: CodeEmitter,
): List<String> {
    val positionAt = span.indexOfLast { it is PrimitiveCommand.Position }
    val position = if (positionAt >= 0) span[positionAt] as PrimitiveCommand.Position else inherited
    if (position == null) {
        val writtenHere = writtenVaryings(span)
        for (varying in env.pipeMembers) {
            if (varying !in writtenBefore && varying !in writtenHere) {
                throw ProgramException(
                    ProgramCode.VaryingForward,
                    "Varying \"${varying.name}\" needs an explicit varying.set before this emit",
                )
            }
        }
        return emptyList()
    }
    val setBetween = if (positionAt >= 0) {
        writtenVaryings(span.subList(positionAt + 1, span.size))
    } else {
        writtenVaryings(span)
    }
    val setEarlier = if (positionAt >= 0) writtenVaryings(span.subList(0, positionAt)) else emptySet()
    val indices = glInIndices(position.value).distinct()
    val lines = mutableListOf<String>()
    for (varying in env.pipeMembers) {
        if (varying in setBetween) continue
        val name = env.varyingNames.getValue(varying)
        if (indices.size == 1) {
            lines += "gs_out.$name = gs_in[${emitter.emit(indices[0])}].$name;"
        } else if (varying !in writtenBefore && varying !in setEarlier) {
            throw ProgramException(
                ProgramCode.VaryingForward,
                "Varying \"${varying.name}\" needs an explicit varying.set before this emit",
            )
        }
    }
    return lines
}

private fun glInIndices(expr: Expr<*>): List<Expr<*>> {
    val found = mutableListOf<Expr<*>>()
    walk(expr, linkedSetOf()) { node ->
        if (node is ExprNode.GlIn) found += node.index
    }
    return found
}

private fun tessInterpolation(name: String, vertices: Int, primitive: TessPrimitive): String {
    val out = "te_out.$name"
    if (vertices == 1) return "$out = te_in[0].$name;"
    return when (primitive) {
        TessPrimitive.Triangles -> {
            if (vertices != 3) rejectTessPatch(vertices, primitive)
            "$out = gl_TessCoord.x * te_in[0].$name + gl_TessCoord.y * te_in[1].$name + " +
                "gl_TessCoord.z * te_in[2].$name;"
        }
        TessPrimitive.Isolines -> {
            if (vertices != 2) rejectTessPatch(vertices, primitive)
            "$out = mix(te_in[0].$name, te_in[1].$name, gl_TessCoord.x);"
        }
        TessPrimitive.Quads -> {
            if (vertices != 4) rejectTessPatch(vertices, primitive)
            "$out = mix(mix(te_in[0].$name, te_in[1].$name, gl_TessCoord.x), " +
                "mix(te_in[3].$name, te_in[2].$name, gl_TessCoord.x), gl_TessCoord.y);"
        }
    }
}

private fun rejectTessPatch(vertices: Int, primitive: TessPrimitive): Nothing {
    throw ProgramException(
        ProgramCode.TessInterpolation,
        "Tessellation evaluation cannot interpolate $vertices vertices of $primitive",
    )
}

private val GEOMETRY_PER_VERTEX = listOf(
    "in gl_PerVertex {\n  vec4 gl_Position;\n} gl_in[];",
    "out gl_PerVertex {\n  vec4 gl_Position;\n};",
)

private val STAGE_PER_VERTEX = GEOMETRY_PER_VERTEX

private val TESS_CONTROL_PER_VERTEX = listOf(
    "in gl_PerVertex {\n  vec4 gl_Position;\n} gl_in[];",
    "out gl_PerVertex {\n  vec4 gl_Position;\n} gl_out[];",
)

internal fun glslReservedNames(): Set<String> = buildSet {
    addAll(RESERVED_USER_FUNCTION_NAMES)
    add("main")
    add("gl_Position")
    add("oColor")
    add("texture")
    add(RB_RESOLUTION_UNIFORM)
    add(RB_SCREEN_CORNER_ATTRIB)
}
