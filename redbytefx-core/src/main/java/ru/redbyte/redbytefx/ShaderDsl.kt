package ru.redbyte.redbytefx

import java.util.IdentityHashMap
import kotlin.jvm.JvmName

/**
 * Immutable result of [shader].
 *
 * Holds generated source text and [Uniform] handles for runtime upload.
 * Thread-safe to read; compile new programs on any thread, play AGSL on the UI thread,
 * and play GLES on the thread that owns the EGL context.
 */
public class ShaderProgram internal constructor(
    public val target: ShaderTarget,
    private val agsl: String? = null,
    private val vertex: String? = null,
    private val fragment: String? = null,
    internal val bindings: List<UniformBinding>,
    public val uniformBlock: UniformBlock? = null,
    public val storageBlock: StorageBlock? = null,
    private val computeSourceText: String? = null,
    private val geometryText: String? = null,
    private val tessControlText: String? = null,
    private val tessEvalText: String? = null,
) {
    public fun agslSource(): String = agsl ?: error("This shader has no AGSL source")

    public fun vertexSource(): String = vertex ?: error("This shader has no GLES vertex source")

    public fun fragmentSource(): String = fragment ?: error("This shader has no GLES fragment source")

    public fun computeSource(): String = computeSourceText ?: error("This shader has no GLES compute source")

    public fun hasGeometry(): Boolean = geometryText != null

    public fun geometrySource(): String = geometryText ?: error("This shader has no GLES geometry source")

    public fun hasTessellation(): Boolean = tessControlText != null

    public fun tessControlSource(): String =
        tessControlText ?: error("This shader has no GLES tessellation control source")

    public fun tessEvalSource(): String =
        tessEvalText ?: error("This shader has no GLES tessellation evaluation source")

    public fun spelledUniforms(): List<SpelledUniform> =
        bindings.map { SpelledUniform(it.uniform, it.agslName) }

    public fun floatUniform(name: String): HighFloatUniform = lookup(name, highFloatShape())

    public fun vec2Uniform(name: String): HighVec2Uniform = lookup(name, highVecShape(2))

    public fun vec3Uniform(name: String): HighVec3Uniform = lookup(name, highVecShape(3))

    public fun vec4Uniform(name: String): HighVec4Uniform = lookup(name, highVecShape(4))

    public fun mediumFloatUniform(name: String): MedFloatUniform = lookup(name, medFloatShape())

    public fun mediumVec2Uniform(name: String): MedVec2Uniform = lookup(name, medVecShape(2))

    public fun mediumVec3Uniform(name: String): MedVec3Uniform = lookup(name, medVecShape(3))

    public fun mediumVec4Uniform(name: String): MedVec4Uniform = lookup(name, medVecShape(4))

    public fun intUniform(name: String): Uniform<IntS> = lookup(name, intShape())

    public fun boolUniform(name: String): Uniform<BoolS> = lookup(name, boolShape())

    public fun mat2Uniform(name: String): Uniform<Mat2> = lookup(name, Shape.Matrix(2))

    public fun mat3Uniform(name: String): Uniform<Mat3> = lookup(name, Shape.Matrix(3))

    public fun mat4Uniform(name: String): Uniform<Mat4> = lookup(name, Shape.Matrix(4))

    internal fun <T : ShType> lookup(name: String, shape: Shape): Uniform<T> {
        val found = bindings.firstOrNull { it.agslName == name || it.uniform.name == name }?.uniform
            ?: throw IllegalArgumentException("Shader has no uniform named $name")
        require(found.shape == shape) { "Uniform \"$name\" has shape ${found.shape}, was $shape" }
        @Suppress("UNCHECKED_CAST")
        return found as Uniform<T>
    }

    internal fun binding(uniform: Uniform<*>): UniformBinding =
        bindings.firstOrNull { it.uniform === uniform }
            ?: throw IllegalArgumentException("Uniform does not belong to this shader")
}

/** GLES/AGSL name paired with the [Uniform] handle declared in Kotlin. */
public class SpelledUniform internal constructor(
    public val uniform: Uniform<*>,
    public val name: String,
)

/**
 * Compiles a [ShaderProgram] for [target].
 *
 * Declare uniforms and stages inside [block]. Use [ShaderProgram.agslSource] or the GLES
 * `*Source()` accessors to inspect output. AGSL playback: [newAgslInstance]. GLES playback:
 * [ru.redbyte.redbytefx.gl.GlProgramRuntime] or [ru.redbyte.redbytefx.gl.compose.GlSurface].
 */
public fun shader(target: ShaderTarget, block: ShaderDsl.() -> Unit): ShaderProgram {
    val dsl = ShaderDsl(target)
    return dsl.author(block)
}

/**
 * Root authoring scope for one shader program.
 *
 * Call [fragment], [vertex], [compute], [geometry], [tessControl], and [tessEval] as required by
 * [ShaderTarget]. Uniforms are declared here, not inside [FragmentDsl.fn].
 */
public class ShaderDsl internal constructor(
    private val target: ShaderTarget,
) {
    internal fun author(block: ShaderDsl.() -> Unit): ShaderProgram = withAuthoring(::advance) {
        block()
        compile()
    }

    private var state = authoringState(target, AuthoringPlace.Program)
    private val uniforms = mutableListOf<Uniform<*>>()
    private val varyings = mutableListOf<Varying<*>>()
    private val attributes = mutableListOf<AttributeHandle>()
    private val varyingWrites = mutableListOf<VaryingWrite>()
    private val fragmentOutputs = mutableListOf<FragmentOutput>()
    private val fragmentWrites = mutableListOf<FragmentWrite>()
    private var fragmentBody: Expr<*>? = null
    private var vertexPosition: Expr<Vec4<Flt<High>>>? = null
    private var vertexBuilt = false
    private val functions = mutableListOf<UserFunction>()
    private var block: UniformBlock? = null
    private var storage: StorageBlock? = null
    private var buildingStorageMembers: List<BlockMember>? = null
    private var computeLayout: ComputeLayout? = null
    private val sharedMembers = mutableListOf<BlockMember>()
    private val computeStatements = mutableListOf<PrimitiveCommand>()
    private val vertexStatements = mutableListOf<PrimitiveCommand>()
    private val fragmentStatements = mutableListOf<PrimitiveCommand>()
    private val sink = StatementSink()
    private var geometryStage: GeometryStage? = null
    private var tessControlStage: TessControlStage? = null
    private var tessEvalStage: TessEvalStage? = null
    private val names = IdentifierAllocator(
        if (target == ShaderTarget.Agsl) agslReservedNames() else glslReservedNames(),
    )
    private val stageFunctions = StageFunctions(
        advance = ::advance,
        parent = { checkNotNull(state.functionParent) { "Function has no parent stage" } },
        names = names,
        register = functions::add,
        sink = sink,
    )

    public fun uniformBlock(name: String, build: UniformBlockBuilder.() -> Unit): UniformBlock {
        advance(AuthoringAction.DeclareUniformBlock)
        require(block == null) { "Shader already has a uniform block" }
        require(name.isNotBlank()) { "Uniform block name must not be blank" }
        val typeName = sanitizeSuggestedIdentifier(name, "b")
        val builder = UniformBlockBuilder("b_$typeName", ::vertex, ::fragment)
        builder.build()
        val created = builder.finish(name, typeName)
        block = created
        return created
    }

    public fun storageBlock(name: String, build: StorageBlockBuilder.() -> Unit): StorageBlock {
        advance(AuthoringAction.DeclareStorage)
        require(storage == null) { "Shader already has a storage block" }
        require(name.isNotBlank()) { "Storage block name must not be blank" }
        val typeName = names.reserve(sanitizeSuggestedIdentifier(name, "b"))
        val instanceName = names.reserve("b_$typeName")
        lateinit var builder: StorageBlockBuilder
        builder = StorageBlockBuilder(instanceName) { layout, body ->
            buildingStorageMembers = builder.memberSnapshot()
            try {
                compute(layout, body)
            } finally {
                buildingStorageMembers = null
            }
        }
        builder.build()
        val created = builder.finish(name, typeName)
        storage = created
        return created
    }

    public fun compute(localSizeX: Int, build: ComputeDsl.() -> Unit) {
        compute(ComputeLayout(localSizeX, null, null), build)
    }

    public fun compute(localSizeX: Int, localSizeY: Int, build: ComputeDsl.() -> Unit) {
        compute(ComputeLayout(localSizeX, localSizeY, null), build)
    }

    public fun compute(
        localSizeX: Int,
        localSizeY: Int,
        localSizeZ: Int,
        build: ComputeDsl.() -> Unit,
    ) {
        compute(ComputeLayout(localSizeX, localSizeY, localSizeZ), build)
    }

    private fun compute(layout: ComputeLayout, build: ComputeDsl.() -> Unit) {
        require(layout.x > 0 && (layout.y == null || layout.y > 0) && (layout.z == null || layout.z > 0)) {
            "Compute local size must be positive, was ${layout.x}, ${layout.y}, ${layout.z}"
        }
        check(computeLayout == null) { "Shader already has a compute stage" }
        advance(AuthoringAction.EnterCompute)
        sink.stage = computeStatements
        try {
            ComputeDsl(::advance, sink, stageFunctions, names, sharedMembers, ::checkStore).build()
            computeLayout = layout
        } finally {
            advance(AuthoringAction.LeaveStage)
        }
    }

    public fun uniform(name: String, default: Float): HighFloatUniform {
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

    public fun uniformTime(default: Float = 0f, name: String = "time"): HighFloatUniform =
        uniform(name, default)

    public fun uniformVec2(name: String, x: Float = 0f, y: Float = 0f): HighVec2Uniform =
        vectorUniform(name, 2, floatArrayOf(x, y))

    public fun uniformVec3(
        name: String,
        x: Float = 0f,
        y: Float = 0f,
        z: Float = 0f,
    ): HighVec3Uniform = vectorUniform(name, 3, floatArrayOf(x, y, z))

    public fun uniformVec4(
        name: String,
        x: Float = 0f,
        y: Float = 0f,
        z: Float = 0f,
        w: Float = 0f,
    ): HighVec4Uniform = vectorUniform(name, 4, floatArrayOf(x, y, z, w), Precision.High)

    public fun uniformMedium(name: String, default: Float): MedFloatUniform {
        advance(AuthoringAction.DeclareUniform)
        require(default.isFinite()) { "Uniform default must be finite, was $default" }
        val handle = createUniform<Flt<Med>>(
            name = name,
            shape = Shape.Scalar(ScalarKind.Float, Precision.Med),
            default = default,
        )
        uniforms += handle
        return handle
    }

    public fun uniformMediumVec2(name: String, x: Float = 0f, y: Float = 0f): MedVec2Uniform =
        vectorUniform(name, 2, floatArrayOf(x, y), Precision.Med)

    public fun uniformMediumVec3(
        name: String,
        x: Float = 0f,
        y: Float = 0f,
        z: Float = 0f,
    ): MedVec3Uniform = vectorUniform(name, 3, floatArrayOf(x, y, z), Precision.Med)

    public fun uniformMediumVec4(
        name: String,
        x: Float = 0f,
        y: Float = 0f,
        z: Float = 0f,
        w: Float = 0f,
    ): MedVec4Uniform = vectorUniform(name, 4, floatArrayOf(x, y, z, w), Precision.Med)

    public fun sampler2D(name: String): Uniform<Sampler2D> {
        advance(AuthoringAction.DeclareSampler)
        val handle = createSampler<Sampler2D>(name, Shape.Sampler2D)
        uniforms += handle
        return handle
    }

    public fun samplerCube(name: String): Uniform<SamplerCube> {
        advance(AuthoringAction.DeclareSampler)
        val handle = createSampler<SamplerCube>(name, Shape.SamplerCube)
        uniforms += handle
        return handle
    }

    public fun uniformInt(name: String, default: Int): Uniform<IntS> {
        advance(AuthoringAction.DeclareUniform)
        val handle = createIntUniform<IntS>(name, default)
        uniforms += handle
        return handle
    }

    public fun uniformBool(name: String, default: Boolean): Uniform<BoolS> {
        if (target == ShaderTarget.Agsl) throw AuthoringException(AuthoringCode.BoolOnAgsl)
        advance(AuthoringAction.DeclareUniform)
        val handle = createBoolUniform<BoolS>(name, default)
        uniforms += handle
        return handle
    }

    public fun uniformMat2(name: String, default: FloatArray = MAT2_IDENTITY): Uniform<Mat2> =
        matrixUniform(name, 2, default)

    public fun uniformMat3(name: String, default: FloatArray = MAT3_IDENTITY): Uniform<Mat3> =
        matrixUniform(name, 3, default)

    public fun uniformMat4(name: String, default: FloatArray = MAT4_IDENTITY): Uniform<Mat4> =
        matrixUniform(name, 4, default)

    private fun <T : ShType> matrixUniform(name: String, lanes: Int, default: FloatArray): Uniform<T> {
        if (target == ShaderTarget.Agsl) throw AuthoringException(AuthoringCode.MatrixOnAgsl)
        advance(AuthoringAction.DeclareUniform)
        require(default.size == lanes * lanes) {
            "Matrix uniform \"$name\" expects ${lanes * lanes} floats, was ${default.size}"
        }
        require(default.all { it.isFinite() }) { "Uniform default must be finite" }
        val handle = createVectorUniform<T>(name, Shape.Matrix(lanes), default.copyOf())
        uniforms += handle
        return handle
    }

    public fun varyingFloat(name: String): Varying<Flt<High>> = varying(
        name,
        Shape.Scalar(ScalarKind.Float, Precision.High),
    )

    public fun varyingVec2(name: String): Varying<Vec2<Flt<High>>> = varying(
        name,
        Shape.Vector(ScalarKind.Float, Precision.High, 2),
    )

    public fun varyingVec3(name: String): Varying<Vec3<Flt<High>>> = varying(
        name,
        Shape.Vector(ScalarKind.Float, Precision.High, 3),
    )

    public fun varyingVec4(name: String): Varying<Vec4<Flt<High>>> = varying(
        name,
        Shape.Vector(ScalarKind.Float, Precision.High, 4),
    )

    public fun fragment(block: FragmentDsl.() -> Expr<*>) {
        check(fragmentBody == null) { "Shader already has a fragment stage" }
        advance(AuthoringAction.EnterFragment)
        try {
            sink.stage = fragmentStatements
            fragmentBody = FragmentDsl(
                ::advance,
                stageFunctions,
                ::declareFragmentOut,
                ::writeFragmentOut,
                sink,
            ).block()
        } finally {
            advance(AuthoringAction.LeaveStage)
        }
    }

    public fun geometry(
        input: GeometryInput,
        output: GeometryOutput,
        maxVertices: Int,
        build: GeometryDsl.() -> Unit,
    ) {
        require(maxVertices in 1..MAX_GEOMETRY_VERTICES) {
            "Geometry maxVertices must be 1..$MAX_GEOMETRY_VERTICES, was $maxVertices"
        }
        check(geometryStage == null) { "Shader already has a geometry stage" }
        advance(AuthoringAction.EnterGeometry)
        val commands = mutableListOf<PrimitiveCommand>()
        sink.stage = commands
        try {
            GeometryDsl(input, ::advance, sink, stageFunctions, ::ownsVarying).build()
            require(hasEmit(commands)) { "Geometry stage must emit a vertex" }
            requirePositionBeforeEmit(commands)
            geometryStage = GeometryStage(input, output, maxVertices, commands.toList())
        } finally {
            advance(AuthoringAction.LeaveStage)
        }
    }

    public fun tessControl(vertices: Int, build: TessControlDsl.() -> Unit) {
        require(vertices in 1..MAX_PATCH_VERTICES) {
            "Tessellation patch must have 1..$MAX_PATCH_VERTICES vertices, was $vertices"
        }
        check(tessControlStage == null) { "Shader already has a tessellation control stage" }
        advance(AuthoringAction.EnterTessControl)
        val commands = mutableListOf<PrimitiveCommand>()
        sink.stage = commands
        try {
            TessControlDsl(vertices, ::advance, sink, stageFunctions, ::ownsVarying).build()
            tessControlStage = TessControlStage(vertices, commands.toList())
        } finally {
            advance(AuthoringAction.LeaveStage)
        }
    }

    public fun tessEval(
        primitive: TessPrimitive,
        spacing: TessSpacing = TessSpacing.Equal,
        order: TessVertexOrder = TessVertexOrder.Ccw,
        build: TessEvalDsl.() -> Unit,
    ) {
        check(tessEvalStage == null) { "Shader already has a tessellation evaluation stage" }
        advance(AuthoringAction.EnterTessEval)
        val commands = mutableListOf<PrimitiveCommand>()
        sink.stage = commands
        try {
            TessEvalDsl(::advance, sink, stageFunctions, ::ownsVarying).build()
            require(hasPosition(commands)) { "Tessellation evaluation must assign gl_Position" }
            tessEvalStage = TessEvalStage(primitive, spacing, order, commands.toList())
        } finally {
            advance(AuthoringAction.LeaveStage)
        }
    }

    public fun vertex(block: VertexDsl.() -> Unit) {
        check(!vertexBuilt) { "Shader already has a vertex stage" }
        advance(AuthoringAction.EnterVertex)
        vertexBuilt = true
        sink.stage = vertexStatements
        try {
            VertexDsl().block()
        } finally {
            advance(AuthoringAction.LeaveStage)
        }
    }

    internal fun compile(): ShaderProgram {
        rejectRecursion(functions)
        return when (target) {
            ShaderTarget.Agsl -> compileAgsl()
            ShaderTarget.Gles30 -> compileGlsl()
            ShaderTarget.Gles31 -> compileCompute()
            ShaderTarget.Gles32 -> compileGlsl(version = GLSL_320, programTarget = ShaderTarget.Gles32)
        }
    }

    private fun compileCompute(): ShaderProgram {
        val layout = computeLayout
            ?: throw ProgramException(ProgramCode.MissingCompute, "GLES 3.1 program requires a compute stage")
        commandExprs(computeStatements).forEach { checkFunctionStage(it, AuthoringPlace.Compute) }
        return spellCompute(
            layout = layout,
            storage = storage,
            statements = computeStatements.toList(),
            uniforms = uniforms,
            names = names,
            functions = functions,
            block = block,
            shared = sharedMembers.toList(),
        )
    }

    private fun checkStore(target: Expr<*>, value: Expr<*>) {
        val member = when (val node = target.node) {
            is ExprNode.BlockRef -> node.member
            is ExprNode.Index -> node.member
            else -> null
        }
        val storageOwned = (storage?.members ?: buildingStorageMembers)?.any { it === member } == true
        val sharedOwned = member != null && sharedMembers.any { it === member }
        require(member != null && (storageOwned || sharedOwned)) {
            "Storage write requires a field of this shader's storage block"
        }
        require(value.shape == target.shape) {
            "Storage field expects ${target.shape}, was ${value.shape}"
        }
    }

    private fun compileAgsl(): ShaderProgram {
        val body = checkNotNull(fragmentBody) { "AGSL shader requires a fragment stage" }
        require(isFloatVec4(body.shape)) { "AGSL fragment must return a float vec4, was ${body.shape}" }
        checkFunctionStage(body, AuthoringPlace.Fragment)
        val bindings = uniforms.map { uniform ->
            val agslName = names.reserve(sanitizeIdentifier(uniform.name ?: "value", "u_"))
            UniformBinding(uniform, agslName)
        }
        val uniformNames = bindings.associateBy { it.uniform }
        val varyingNames = varyings.associateWith { varying ->
            names.reserve(sanitizeIdentifier(varying.name, "v_"))
        }
        val occupied = names.snapshot()
        val functionText = renderAgslFunctions(functions, occupied, uniformNames, varyingNames)
        val emitter = AgslEmitter(IdentifierAllocator(occupied), uniformNames)
        val statements = spellStageStatements(fragmentStatements, emitter, varyingNames)
        val rendered = emitter.emit(body)
        val output = if (isMedVec4(body.shape)) rendered else "half4($rendered)"
        return ShaderProgram(
            target = target,
            agsl = renderAgsl(bindings, emitter.declarations, functionText, statements, output),
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

    private fun <T : ShType> vectorUniform(
        name: String,
        lanes: Int,
        components: FloatArray,
        precision: Precision = Precision.High,
    ): Uniform<T> {
        advance(AuthoringAction.DeclareUniform)
        require(components.all { it.isFinite() }) { "Uniform default must be finite" }
        val handle = createVectorUniform<T>(
            name = name,
            shape = Shape.Vector(ScalarKind.Float, precision, lanes),
            components = components,
        )
        uniforms += handle
        return handle
    }

    private fun ownsVarying(varying: Varying<*>): Boolean = varyings.any { it === varying }

    private fun <T : ShType> varying(name: String, shape: Shape): Varying<T> {
        advance(AuthoringAction.DeclareVarying)
        val handle = createVarying<T>(name, shape)
        varyings += handle
        return handle
    }

    private fun declareFragmentOut(name: String, location: Int): FragmentOutput {
        advance(AuthoringAction.FragmentOut)
        require(name.isNotBlank()) { "Fragment output name must not be blank" }
        require(location >= 0) { "Fragment output location must be non-negative, was $location" }
        require(fragmentOutputs.none { it.location == location }) {
            "Fragment output location $location is already used"
        }
        val output = FragmentOutput(name, location)
        fragmentOutputs += output
        return output
    }

    private fun writeFragmentOut(output: FragmentOutput, value: Expr<*>) {
        advance(AuthoringAction.FragmentOut)
        require(fragmentOutputs.any { it === output }) {
            "Fragment output \"${output.name}\" does not belong to this shader"
        }
        require(value.shape == output.shape) {
            "Fragment output \"${output.name}\" expects ${output.shape}, was ${value.shape}"
        }
        require(fragmentWrites.none { it.output === output }) {
            "Fragment output \"${output.name}\" is already written"
        }
        fragmentWrites += FragmentWrite(output, value)
    }

    private fun compileGlsl(
        version: Int = GLSL_300,
        programTarget: ShaderTarget = ShaderTarget.Gles30,
    ): ShaderProgram {
        val control = tessControlStage
        val evaluation = tessEvalStage
        if ((control == null) != (evaluation == null)) {
            throw ProgramException(
                ProgramCode.TessStageMissing,
                "Tessellation needs both a control stage and an evaluation stage",
            )
        }
        if (control != null && evaluation != null) {
            commandExprs(evaluation.commands).forEach { rejectGlInPastPatch(it, control.vertices) }
        }
        geometryStage?.let { stage ->
            commandExprs(stage.commands).forEach { expr ->
                checkFunctionStage(expr, AuthoringPlace.Geometry)
            }
        }
        control?.let { stage ->
            commandExprs(stage.commands).forEach { checkFunctionStage(it, AuthoringPlace.TessControl) }
        }
        evaluation?.let { stage ->
            commandExprs(stage.commands).forEach { checkFunctionStage(it, AuthoringPlace.TessEval) }
        }
        if (!vertexBuilt) throw ProgramException(ProgramCode.MissingVertex, "GLES program requires a vertex stage")
        val body = fragmentBody
            ?: throw ProgramException(ProgramCode.MissingFragment, "GLES program requires a fragment stage")
        val position = vertexPosition
        if (position == null && !hasPosition(vertexStatements)) {
            throw ProgramException(ProgramCode.MissingGlPosition, "GLES vertex must assign gl_Position")
        }
        require(isFloatVec4(body.shape)) { "GLES fragment must return a float vec4, was ${body.shape}" }
        if (position != null) checkFunctionStage(position, AuthoringPlace.Vertex)
        commandExprs(vertexStatements).forEach { checkFunctionStage(it, AuthoringPlace.Vertex) }
        commandExprs(fragmentStatements).forEach { checkFunctionStage(it, AuthoringPlace.Fragment) }
        varyingWrites.forEach { checkFunctionStage(it.value, AuthoringPlace.Vertex) }
        checkFunctionStage(body, AuthoringPlace.Fragment)
        fragmentWrites.forEach { checkFunctionStage(it.value, AuthoringPlace.Fragment) }
        for (output in fragmentOutputs) {
            if (fragmentWrites.none { it.output === output }) {
                throw ProgramException(
                    ProgramCode.FragmentOutNotWritten,
                    "Fragment output \"${output.name}\" was not written",
                )
            }
        }
        val reads = linkedSetOf<Varying<*>>()
        collectVaryings(body, reads)
        fragmentWrites.forEach { collectVaryings(it.value, reads) }
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
            names = names,
            functions = functions,
            fragmentWrites = fragmentWrites,
            block = block,
            stages = LinkedStages(
                vertexStatements = vertexStatements.toList(),
                fragmentStatements = fragmentStatements.toList(),
                geometry = geometryStage,
                tessControl = control,
                tessEval = evaluation,
            ),
            version = version,
            programTarget = programTarget,
        )
    }

    public inner class VertexDsl {
        public fun <T : ShType> Varying<T>.set(value: Expr<T>) {
            require(ownsVarying(this)) { "Varying \"${this.name}\" does not belong to this shader" }
            require(value.shape == shape) {
                "Varying \"${this.name}\" expects $shape, was ${value.shape}"
            }
            if (sink.capturing()) sink.add(PrimitiveCommand.VaryingSet(this, value))
            else varyingWrites += VaryingWrite(this, value)
        }

        public fun glPosition(value: Expr<Vec4<Flt<High>>>) {
            advance(AuthoringAction.GlPosition)
            if (sink.capturing()) {
                sink.add(PrimitiveCommand.Position(value))
                return
            }
            check(vertexPosition == null) { "gl_Position is already assigned" }
            vertexPosition = value
        }

        public fun repeat(count: Int, body: (Expr<IntS>) -> Unit) {
            advance(AuthoringAction.Repeat)
            sink.repeat(count, body)
        }

        public fun <T : ShType> local(initializer: Expr<T>, name: String? = null): LocalVar<T> =
            sink.declareLocal(initializer, name)

        public fun whenTrue(condition: Expr<BoolS>, body: () -> Unit) {
            sink.whenTrue(condition, body)
        }

        public fun discard() {
            advance(AuthoringAction.Discard)
        }

        public fun attributeVec2(name: String): Expr<Vec2<Flt<High>>> = attribute(
            name,
            Shape.Vector(ScalarKind.Float, Precision.High, 2),
        )

        public fun attributeVec3(name: String): Expr<Vec3<Flt<High>>> = attribute(
            name,
            Shape.Vector(ScalarKind.Float, Precision.High, 3),
        )

        public fun attributeVec4(name: String): Expr<Vec4<Flt<High>>> = attribute(
            name,
            Shape.Vector(ScalarKind.Float, Precision.High, 4),
        )

        public fun <R : ShType> fn(name: String? = null, block: VertexDsl.() -> Expr<R>): Fn0<R> =
            stageFunctions.fn0(name) { block() }

        public fun <A : ShType, R : ShType> fn(
            witness: Expr<A>,
            name: String? = null,
            block: VertexDsl.(Expr<A>) -> Expr<R>,
        ): Fn1<A, R> = stageFunctions.fn1(name, witness) { block(it) }

        public fun <A : ShType, B : ShType, R : ShType> fn(
            first: Expr<A>,
            second: Expr<B>,
            name: String? = null,
            block: VertexDsl.(Expr<A>, Expr<B>) -> Expr<R>,
        ): Fn2<A, B, R> = stageFunctions.fn2(name, first, second) { left, right -> block(left, right) }

        public fun <A : ShType, B : ShType, C : ShType, R : ShType> fn(
            first: Expr<A>,
            second: Expr<B>,
            third: Expr<C>,
            name: String? = null,
            block: VertexDsl.(Expr<A>, Expr<B>, Expr<C>) -> Expr<R>,
        ): Fn3<A, B, C, R> = stageFunctions.fn3(name, first, second, third) { left, mid, right ->
            block(left, mid, right)
        }

        public fun <A : ShType, B : ShType, C : ShType, D : ShType, R : ShType> fn(
            first: Expr<A>,
            second: Expr<B>,
            third: Expr<C>,
            fourth: Expr<D>,
            name: String? = null,
            block: VertexDsl.(Expr<A>, Expr<B>, Expr<C>, Expr<D>) -> Expr<R>,
        ): Fn4<A, B, C, D, R> = stageFunctions.fn4(name, first, second, third, fourth) { a, b, c, d ->
            block(a, b, c, d)
        }

        public fun <T : ShType> recur(arg: Expr<T>): Expr<T> = stageFunctions.recur(arg)

        public fun <A : ShType, B : ShType, C : ShType, D : ShType, E : ShType, R : ShType> fn(
            first: Expr<A>,
            second: Expr<B>,
            third: Expr<C>,
            fourth: Expr<D>,
            fifth: Expr<E>,
            name: String? = null,
            block: VertexDsl.(Expr<A>, Expr<B>, Expr<C>, Expr<D>, Expr<E>) -> Expr<R>,
        ): Fn5<A, B, C, D, E, R> = stageFunctions.fn5(name, first, second, third, fourth, fifth) { a, b, c, d, e ->
            block(a, b, c, d, e)
        }

        public fun <A : ShType, B : ShType, C : ShType, D : ShType, E : ShType, F : ShType, R : ShType> fn(
            first: Expr<A>,
            second: Expr<B>,
            third: Expr<C>,
            fourth: Expr<D>,
            fifth: Expr<E>,
            sixth: Expr<F>,
            name: String? = null,
            block: VertexDsl.(Expr<A>, Expr<B>, Expr<C>, Expr<D>, Expr<E>, Expr<F>) -> Expr<R>,
        ): Fn6<A, B, C, D, E, F, R> =
            stageFunctions.fn6(name, first, second, third, fourth, fifth, sixth) { a, b, c, d, e, f ->
                block(a, b, c, d, e, f)
            }

        public fun <A : ShType, B : ShType, C : ShType, D : ShType, E : ShType, F : ShType, G : ShType, R : ShType> fn(
            first: Expr<A>,
            second: Expr<B>,
            third: Expr<C>,
            fourth: Expr<D>,
            fifth: Expr<E>,
            sixth: Expr<F>,
            seventh: Expr<G>,
            name: String? = null,
            block: VertexDsl.(Expr<A>, Expr<B>, Expr<C>, Expr<D>, Expr<E>, Expr<F>, Expr<G>) -> Expr<R>,
        ): Fn7<A, B, C, D, E, F, G, R> =
            stageFunctions.fn7(name, first, second, third, fourth, fifth, sixth, seventh) { a, b, c, d, e, f, g ->
                block(a, b, c, d, e, f, g)
            }

        public fun <
            A : ShType,
            B : ShType,
            C : ShType,
            D : ShType,
            E : ShType,
            F : ShType,
            G : ShType,
            H : ShType,
            R : ShType,
            > fn(
            first: Expr<A>,
            second: Expr<B>,
            third: Expr<C>,
            fourth: Expr<D>,
            fifth: Expr<E>,
            sixth: Expr<F>,
            seventh: Expr<G>,
            eighth: Expr<H>,
            name: String? = null,
            block: VertexDsl.(Expr<A>, Expr<B>, Expr<C>, Expr<D>, Expr<E>, Expr<F>, Expr<G>, Expr<H>) -> Expr<R>,
        ): Fn8<A, B, C, D, E, F, G, H, R> =
            stageFunctions.fn8(
                name, first, second, third, fourth, fifth, sixth, seventh, eighth,
            ) { a, b, c, d, e, f, g, h ->
                block(a, b, c, d, e, f, g, h)
            }

        private fun <T : ShType> attribute(name: String, shape: Shape): Expr<T> {
            advance(AuthoringAction.Attribute)
            val handle = AttributeHandle(name, shape)
            attributes += handle
            return Expr(shape, ExprNode.AttributeRef(handle))
        }
    }
}

private fun highFloatShape(): Shape = Shape.Scalar(ScalarKind.Float, Precision.High)

private fun medFloatShape(): Shape = Shape.Scalar(ScalarKind.Float, Precision.Med)

private fun highVecShape(lanes: Int): Shape = Shape.Vector(ScalarKind.Float, Precision.High, lanes)

private fun medVecShape(lanes: Int): Shape = Shape.Vector(ScalarKind.Float, Precision.Med, lanes)

private fun intShape(): Shape = Shape.Scalar(ScalarKind.Int, null)

private fun boolShape(): Shape = Shape.Scalar(ScalarKind.Bool, null)

private val MAT2_IDENTITY = floatArrayOf(1f, 0f, 0f, 1f)

private val MAT3_IDENTITY = floatArrayOf(1f, 0f, 0f, 0f, 1f, 0f, 0f, 0f, 1f)

private val MAT4_IDENTITY = floatArrayOf(
    1f, 0f, 0f, 0f,
    0f, 1f, 0f, 0f,
    0f, 0f, 1f, 0f,
    0f, 0f, 0f, 1f,
)

internal fun isFloatVec4(shape: Shape): Boolean =
    shape is Shape.Vector && shape.kind == ScalarKind.Float && shape.lanes == 4

private fun isMedVec4(shape: Shape): Boolean =
    isFloatVec4(shape) && shape is Shape.Vector && shape.precision == Precision.Med

/**
 * Fragment stage DSL.
 *
 * [fragCoord] and [resolution] are in pixels. [sample] reads the child shader on AGSL only.
 * [texture] samples a [Sampler2D] on GLES only.
 */
public class FragmentDsl internal constructor(
    private val advance: (AuthoringAction) -> Unit,
    private val functions: StageFunctions,
    private val declareOut: (String, Int) -> FragmentOutput,
    private val writeOut: (FragmentOutput, Expr<*>) -> Unit,
    private val sink: StatementSink,
) {
    /** Fragment position in pixels (AGSL and GLES). */
    public val fragCoord: Expr<Vec2<Flt<High>>> = Expr(
        Shape.Vector(ScalarKind.Float, Precision.High, 2),
        ExprNode.FragCoord,
    )

    /** Drawable size in pixels; Compose sets this through [ru.redbyte.redbytefx.compose.redbyteFx]. */
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

    public fun textureCube(
        sampler: Uniform<SamplerCube>,
        direction: Expr<Vec3<Flt<High>>>,
    ): Expr<Vec4<Flt<High>>> {
        advance(AuthoringAction.Texture)
        require(direction.shape == Shape.Vector(ScalarKind.Float, Precision.High, 3)) {
            "textureCube expects a highp vec3, was ${direction.shape}"
        }
        return Expr(
            Shape.Vector(ScalarKind.Float, Precision.High, 4),
            ExprNode.TextureCube(sampler.expr, direction),
        )
    }

    public fun sampleUnclamped(coord: Expr<Vec2<Flt<High>>> = fragCoord): Expr<Vec4<Flt<Med>>> {
        advance(AuthoringAction.Sample)
        return Expr(
            Shape.Vector(ScalarKind.Float, Precision.Med, 4),
            ExprNode.UnclampedSample(coord),
        )
    }

    public fun outVec4(name: String, location: Int): FragmentOutput = declareOut(name, location)

    public fun FragmentOutput.set(value: Expr<Vec4<Flt<High>>>) {
        writeOut(this, value)
    }

    public fun discard() {
        advance(AuthoringAction.Discard)
        sink.add(PrimitiveCommand.Discard)
    }

    public fun discard(condition: Expr<BoolS>) {
        advance(AuthoringAction.Discard)
        require(condition.shape == Shape.Scalar(ScalarKind.Bool, null)) {
            "discard condition must be a bool, was ${condition.shape}"
        }
        sink.add(PrimitiveCommand.DiscardIf(condition))
    }

    public fun <T : ShType> local(initializer: Expr<T>, name: String? = null): LocalVar<T> =
        sink.declareLocal(initializer, name)

    public fun whenTrue(condition: Expr<BoolS>, body: () -> Unit) {
        sink.whenTrue(condition, body)
    }

    public fun sharedFloat(name: String, size: Int): StorageArray<Flt<High>> {
        advance(AuthoringAction.DeclareShared)
        error("shared $name[$size] is only available in compute")
    }

    public fun barrier() {
        advance(AuthoringAction.Barrier)
        throw AuthoringException(AuthoringCode.BarrierOutsideCompute)
    }

    public fun repeat(count: Int, body: (Expr<IntS>) -> Unit) {
        advance(AuthoringAction.Repeat)
        sink.repeat(count, body)
    }

    public fun sample(coord: Expr<Vec2<Flt<High>>> = fragCoord): Expr<Vec4<Flt<Med>>> {
        advance(AuthoringAction.Sample)
        return Expr(
            Shape.Vector(ScalarKind.Float, Precision.Med, 4),
            ExprNode.Sample(coord),
        )
    }

    public fun <R : ShType> fn(name: String? = null, block: FragmentDsl.() -> Expr<R>): Fn0<R> =
        functions.fn0(name) { block() }

    public fun <A : ShType, R : ShType> fn(
        witness: Expr<A>,
        name: String? = null,
        block: FragmentDsl.(Expr<A>) -> Expr<R>,
    ): Fn1<A, R> = functions.fn1(name, witness) { block(it) }

    public fun <A : ShType, B : ShType, R : ShType> fn(
        first: Expr<A>,
        second: Expr<B>,
        name: String? = null,
        block: FragmentDsl.(Expr<A>, Expr<B>) -> Expr<R>,
    ): Fn2<A, B, R> = functions.fn2(name, first, second) { left, right -> block(left, right) }

    public fun <A : ShType, B : ShType, C : ShType, R : ShType> fn(
        first: Expr<A>,
        second: Expr<B>,
        third: Expr<C>,
        name: String? = null,
        block: FragmentDsl.(Expr<A>, Expr<B>, Expr<C>) -> Expr<R>,
    ): Fn3<A, B, C, R> = functions.fn3(name, first, second, third) { left, mid, right ->
        block(left, mid, right)
    }

    public fun <A : ShType, B : ShType, C : ShType, D : ShType, R : ShType> fn(
        first: Expr<A>,
        second: Expr<B>,
        third: Expr<C>,
        fourth: Expr<D>,
        name: String? = null,
        block: FragmentDsl.(Expr<A>, Expr<B>, Expr<C>, Expr<D>) -> Expr<R>,
    ): Fn4<A, B, C, D, R> = functions.fn4(name, first, second, third, fourth) { a, b, c, d ->
        block(a, b, c, d)
    }

    public fun <T : ShType> recur(arg: Expr<T>): Expr<T> = functions.recur(arg)

    public fun <A : ShType, B : ShType, C : ShType, D : ShType, E : ShType, R : ShType> fn(
        first: Expr<A>,
        second: Expr<B>,
        third: Expr<C>,
        fourth: Expr<D>,
        fifth: Expr<E>,
        name: String? = null,
        block: FragmentDsl.(Expr<A>, Expr<B>, Expr<C>, Expr<D>, Expr<E>) -> Expr<R>,
    ): Fn5<A, B, C, D, E, R> = functions.fn5(name, first, second, third, fourth, fifth) { a, b, c, d, e ->
        block(a, b, c, d, e)
    }

    public fun <A : ShType, B : ShType, C : ShType, D : ShType, E : ShType, F : ShType, R : ShType> fn(
        first: Expr<A>,
        second: Expr<B>,
        third: Expr<C>,
        fourth: Expr<D>,
        fifth: Expr<E>,
        sixth: Expr<F>,
        name: String? = null,
        block: FragmentDsl.(Expr<A>, Expr<B>, Expr<C>, Expr<D>, Expr<E>, Expr<F>) -> Expr<R>,
    ): Fn6<A, B, C, D, E, F, R> =
        functions.fn6(name, first, second, third, fourth, fifth, sixth) { a, b, c, d, e, f ->
            block(a, b, c, d, e, f)
        }

    public fun <A : ShType, B : ShType, C : ShType, D : ShType, E : ShType, F : ShType, G : ShType, R : ShType> fn(
        first: Expr<A>,
        second: Expr<B>,
        third: Expr<C>,
        fourth: Expr<D>,
        fifth: Expr<E>,
        sixth: Expr<F>,
        seventh: Expr<G>,
        name: String? = null,
        block: FragmentDsl.(Expr<A>, Expr<B>, Expr<C>, Expr<D>, Expr<E>, Expr<F>, Expr<G>) -> Expr<R>,
    ): Fn7<A, B, C, D, E, F, G, R> =
        functions.fn7(name, first, second, third, fourth, fifth, sixth, seventh) { a, b, c, d, e, f, g ->
            block(a, b, c, d, e, f, g)
        }

    public fun <
        A : ShType,
        B : ShType,
        C : ShType,
        D : ShType,
        E : ShType,
        F : ShType,
        G : ShType,
        H : ShType,
        R : ShType,
        > fn(
        first: Expr<A>,
        second: Expr<B>,
        third: Expr<C>,
        fourth: Expr<D>,
        fifth: Expr<E>,
        sixth: Expr<F>,
        seventh: Expr<G>,
        eighth: Expr<H>,
        name: String? = null,
        block: FragmentDsl.(Expr<A>, Expr<B>, Expr<C>, Expr<D>, Expr<E>, Expr<F>, Expr<G>, Expr<H>) -> Expr<R>,
    ): Fn8<A, B, C, D, E, F, G, H, R> =
        functions.fn8(name, first, second, third, fourth, fifth, sixth, seventh, eighth) { a, b, c, d, e, f, g, h ->
            block(a, b, c, d, e, f, g, h)
        }

    @JvmName("letValue")
    public fun <T : ShType> let(value: Expr<T>, name: String? = null): Expr<T> = value.let(name)

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
        is ExprNode.Cast -> "${spell(expr.shape, ShaderTarget.Agsl)}(${emit(node.arg)})"
        is ExprNode.Unary -> "(-${emit(node.arg)})"
        is ExprNode.Binary -> "(${emit(node.left)} ${node.op.symbol} ${emit(node.right)})"
        is ExprNode.Construct -> {
            val args = node.args.joinToString(", ") { emit(it) }
            "${spell(expr.shape, ShaderTarget.Agsl)}($args)"
        }
        is ExprNode.Local -> local(node, expr.shape)
        is ExprNode.SlotRef -> slotName(node.slot)
        is ExprNode.UniformRef -> uniforms.getValue(node.uniform).agslName
        ExprNode.FragCoord -> "fragCoord"
        ExprNode.Resolution -> RB_RESOLUTION_UNIFORM
        is ExprNode.Sample -> "rb_sample(${emit(node.coord)})"
        is ExprNode.UnclampedSample -> "$RB_INPUT_UNIFORM.eval(${emit(node.coord)})"
        is ExprNode.Call -> call(node)
        is ExprNode.Param -> node.name
        is ExprNode.Compare -> spellCompare(node.op, node.left, node.right, ::emit)
        is ExprNode.Select -> "(${emit(node.condition)} ? ${emit(node.ifTrue)} : ${emit(node.ifFalse)})"
        is ExprNode.UserCall -> call(ExprNode.Call(node.function.name, node.args))
        is ExprNode.Texture,
        is ExprNode.TextureCube,
        is ExprNode.AttributeRef,
        is ExprNode.VaryingRef,
        is ExprNode.VaryingAt,
        is ExprNode.BlockRef,
        is ExprNode.Index,
        is ExprNode.GlIn,
        is ExprNode.Invocation,
        ExprNode.TessCoord -> error("AGSL cannot spell ${node::class.simpleName}")
    }

    private fun call(node: ExprNode.Call): String {
        val args = node.args.joinToString(", ") { emit(it) }
        return "${node.function}($args)"
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

    override fun bindSlot(slot: LocalSlot) {
        if (slotNames.containsKey(slot)) return
        val name = allocator.reserve(localBase(slot.suggestedName, slotIndex))
        slotIndex += 1
        slotNames[slot] = name
        declarations += "  ${spell(slot.shape, ShaderTarget.Agsl)} $name;"
    }

    override fun slotName(slot: LocalSlot): String {
        bindSlot(slot)
        return slotNames.getValue(slot)
    }
}

private val ArithOp.symbol: String
    get() = when (this) {
        ArithOp.Add -> "+"
        ArithOp.Sub -> "-"
        ArithOp.Mul -> "*"
        ArithOp.Div -> "/"
        ArithOp.And -> "&&"
        ArithOp.Or -> "||"
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

private fun renderAgslFunctions(
    functions: List<UserFunction>,
    occupied: Set<String>,
    uniforms: Map<Uniform<*>, UniformBinding>,
    varyingNames: Map<Varying<*>, String>,
): String = buildString {
    for (function in functions) {
        val locals = IdentifierAllocator(occupied + function.parameters.map { it.name })
        val emitter = AgslEmitter(locals, uniforms)
        val statements = spellStageStatements(function.statements, emitter, varyingNames)
        val body = emitter.emit(function.body)
        val signature = function.parameters.joinToString(", ") {
            "${spell(it.shape, ShaderTarget.Agsl)} ${it.name}"
        }
        append(spell(function.result, ShaderTarget.Agsl)).append(' ').append(function.name)
            .append('(').append(signature).append(") {\n")
        emitter.declarations.forEach { append(it).append('\n') }
        statements.forEach { append(it).append('\n') }
        append("  return ").append(body).append(";\n}\n")
    }
}

private fun renderAgsl(
    bindings: List<UniformBinding>,
    declarations: List<String>,
    functions: String,
    statements: List<String>,
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
    append(functions)
    append("half4 main(float2 fragCoord) {\n")
    for (line in declarations) {
        append(line).append('\n')
    }
    for (line in statements) {
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
