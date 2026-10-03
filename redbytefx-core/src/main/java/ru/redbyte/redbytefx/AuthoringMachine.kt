package ru.redbyte.redbytefx

import androidx.annotation.RequiresApi

/**
 * Compilation target for [shader].
 *
 * AGSL emits a single fragment shader for [android.graphics.RuntimeShader].
 * GLES targets emit GLSL ES sources for [ru.redbyte.redbytefx.gl.GlProgramRuntime].
 */
public enum class ShaderTarget {
    /** Android 12+ fragment shader for Compose [ru.redbyte.redbytefx.compose.redbyteFx]. */
    @RequiresApi(RedByteFxApis.AGSL_MIN_SDK)
    Agsl,

    /** GLES 3.0 vertex and fragment program. */
    Gles30,

    /** GLES 3.1 compute shader. */
    Gles31,

    /** GLES 3.2 program with optional geometry and tessellation stages. */
    Gles32,
}

internal enum class AuthoringPlace {
    Program,
    Vertex,
    Fragment,
    Compute,
    Geometry,
    TessControl,
    TessEval,
    Function,
}

internal enum class AuthoringAction {
    DeclareUniform,
    DeclareSampler,
    DeclareVarying,
    DeclareUniformBlock,
    DeclareStorage,
    EnterVertex,
    EnterFragment,
    EnterCompute,
    EnterGeometry,
    EnterTessControl,
    EnterTessEval,
    EnterFunction,
    LeaveFunction,
    LeaveStage,
    Sample,
    Texture,
    Attribute,
    GlPosition,
    EmitVertex,
    EndPrimitive,
    TessLevel,
    FragmentOut,
    StorageWrite,
    Let,
    Return,
    Discard,
    Derivative,
    DeclareShared,
    Barrier,
    Repeat,
}

internal enum class AuthoringCode {
    UniformOutsideProgram,
    UniformInsideFunction,
    SamplerOutsideProgram,
    SamplerOnAgsl,
    MatrixOnAgsl,
    BoolOnAgsl,
    VaryingOutsideProgram,
    VaryingOnAgsl,
    UniformBlockOnAgsl,
    UniformBlockOutsideProgram,
    VertexOnAgsl,
    VertexOutsideProgram,
    FragmentOutsideProgram,
    FunctionOutsideStage,
    NestedFunction,
    LeaveOutsideFunction,
    LeaveOutsideStage,
    SampleOutsideAgslFragment,
    TextureOutsideGlesFragment,
    AttributeOutsideVertex,
    AttributeInsideFunction,
    GlPositionOutsideVertex,
    FragmentOutOnAgsl,
    FragmentOutOutsideFragment,
    ComputeOutsideGles31,
    ComputeOutsideProgram,
    VertexOnGles31,
    FragmentOnGles31,
    StorageOutsideGles31,
    StorageOutsideProgram,
    StorageWriteOutsideCompute,
    GeometryOutsideGles32,
    GeometryOutsideProgram,
    TessControlOutsideGles32,
    TessControlOutsideProgram,
    TessEvalOutsideGles32,
    TessEvalOutsideProgram,
    EmitVertexOutsideGeometry,
    EndPrimitiveOutsideGeometry,
    TessLevelOutsideTessControl,
    LetOutsideStage,
    ReturnOutsideStage,
    DiscardOutsideFragment,
    DerivativeOutsideFragment,
    SharedOutsideCompute,
    BarrierOutsideCompute,
    RepeatOutsideStage,
}

internal data class AuthoringState(
    val target: ShaderTarget,
    val place: AuthoringPlace,
    val functionParent: AuthoringPlace? = null,
) {
    init {
        val parentOk = when (place) {
            AuthoringPlace.Function -> functionParent in STAGE_PARENTS
            else -> functionParent == null
        }
        require(parentOk) { "Function state requires a stage parent" }
    }
}

internal data class AuthoringStep(
    val state: AuthoringState,
    val code: AuthoringCode?,
)

internal class AuthoringException(val code: AuthoringCode) : IllegalStateException(code.name)

internal fun authoringState(
    target: ShaderTarget,
    place: AuthoringPlace,
    functionParent: AuthoringPlace? = null,
): AuthoringState = AuthoringState(target, place, functionParent)

internal fun authoringStep(state: AuthoringState, action: AuthoringAction): AuthoringStep =
    when (action) {
        AuthoringAction.DeclareUniform -> declareUniform(state)
        AuthoringAction.DeclareSampler -> declareSampler(state)
        AuthoringAction.DeclareVarying -> declareVarying(state)
        AuthoringAction.DeclareUniformBlock -> declareUniformBlock(state)
        AuthoringAction.DeclareStorage -> declareStorage(state)
        AuthoringAction.EnterVertex -> enterVertex(state)
        AuthoringAction.EnterFragment -> enterFragment(state)
        AuthoringAction.EnterCompute -> enterCompute(state)
        AuthoringAction.EnterGeometry -> enterGeometry(state)
        AuthoringAction.EnterTessControl -> enterTessControl(state)
        AuthoringAction.EnterTessEval -> enterTessEval(state)
        AuthoringAction.StorageWrite -> storageWrite(state)
        AuthoringAction.EmitVertex -> emitVertex(state)
        AuthoringAction.EndPrimitive -> endPrimitive(state)
        AuthoringAction.TessLevel -> tessLevel(state)
        AuthoringAction.EnterFunction -> enterFunction(state)
        AuthoringAction.LeaveFunction -> leaveFunction(state)
        AuthoringAction.LeaveStage -> leaveStage(state)
        AuthoringAction.Sample -> allow(
            state,
            state.target == ShaderTarget.Agsl && state.place == AuthoringPlace.Fragment,
            AuthoringCode.SampleOutsideAgslFragment,
        )
        AuthoringAction.Texture -> allow(
            state,
            (state.target == ShaderTarget.Gles30 || state.target == ShaderTarget.Gles32) &&
                state.place == AuthoringPlace.Fragment,
            AuthoringCode.TextureOutsideGlesFragment,
        )
        AuthoringAction.Attribute -> attribute(state)
        AuthoringAction.GlPosition -> allow(
            state,
            glPositionLegal(state),
            AuthoringCode.GlPositionOutsideVertex,
        )
        AuthoringAction.FragmentOut -> fragmentOut(state)
        AuthoringAction.Let -> allow(
            state,
            state.place != AuthoringPlace.Program,
            AuthoringCode.LetOutsideStage,
        )
        AuthoringAction.Return -> allow(
            state,
            state.place != AuthoringPlace.Program,
            AuthoringCode.ReturnOutsideStage,
        )
        AuthoringAction.Discard -> allow(
            state,
            fragmentOnly(state),
            AuthoringCode.DiscardOutsideFragment,
        )
        AuthoringAction.Derivative -> allow(
            state,
            fragmentOnly(state),
            AuthoringCode.DerivativeOutsideFragment,
        )
        AuthoringAction.DeclareShared -> allow(
            state,
            computeOnly(state),
            AuthoringCode.SharedOutsideCompute,
        )
        AuthoringAction.Barrier -> allow(
            state,
            computeOnly(state),
            AuthoringCode.BarrierOutsideCompute,
        )
        AuthoringAction.Repeat -> allow(
            state,
            state.place != AuthoringPlace.Program,
            AuthoringCode.RepeatOutsideStage,
        )
    }

private fun declareUniform(state: AuthoringState): AuthoringStep = when (state.place) {
    AuthoringPlace.Program -> accept(state)
    AuthoringPlace.Function -> reject(state, AuthoringCode.UniformInsideFunction)
    AuthoringPlace.Vertex, AuthoringPlace.Fragment, AuthoringPlace.Compute,
    AuthoringPlace.Geometry, AuthoringPlace.TessControl, AuthoringPlace.TessEval,
    -> reject(state, AuthoringCode.UniformOutsideProgram)
}

private fun declareSampler(state: AuthoringState): AuthoringStep = when {
    state.target == ShaderTarget.Agsl -> reject(state, AuthoringCode.SamplerOnAgsl)
    state.place == AuthoringPlace.Program -> accept(state)
    else -> reject(state, AuthoringCode.SamplerOutsideProgram)
}

private fun declareUniformBlock(state: AuthoringState): AuthoringStep = when {
    state.target == ShaderTarget.Agsl -> reject(state, AuthoringCode.UniformBlockOnAgsl)
    state.place == AuthoringPlace.Program && state.target != ShaderTarget.Agsl -> accept(state)
    else -> reject(state, AuthoringCode.UniformBlockOutsideProgram)
}

private fun declareStorage(state: AuthoringState): AuthoringStep = when {
    state.target != ShaderTarget.Gles31 -> reject(state, AuthoringCode.StorageOutsideGles31)
    state.place == AuthoringPlace.Program -> accept(state)
    else -> reject(state, AuthoringCode.StorageOutsideProgram)
}

private fun declareVarying(state: AuthoringState): AuthoringStep = when {
    state.target == ShaderTarget.Agsl -> reject(state, AuthoringCode.VaryingOnAgsl)
    state.place == AuthoringPlace.Program -> accept(state)
    else -> reject(state, AuthoringCode.VaryingOutsideProgram)
}

private fun enterVertex(state: AuthoringState): AuthoringStep = when {
    state.place != AuthoringPlace.Program -> reject(state, AuthoringCode.VertexOutsideProgram)
    state.target == ShaderTarget.Gles31 -> reject(state, AuthoringCode.VertexOnGles31)
    state.target == ShaderTarget.Gles30 || state.target == ShaderTarget.Gles32 ->
        accept(state.copy(place = AuthoringPlace.Vertex))
    else -> reject(state, AuthoringCode.VertexOnAgsl)
}

private fun enterFragment(state: AuthoringState): AuthoringStep = when {
    state.place != AuthoringPlace.Program -> reject(state, AuthoringCode.FragmentOutsideProgram)
    state.target == ShaderTarget.Gles31 -> reject(state, AuthoringCode.FragmentOnGles31)
    else -> accept(state.copy(place = AuthoringPlace.Fragment))
}

private fun enterCompute(state: AuthoringState): AuthoringStep = when {
    state.place != AuthoringPlace.Program -> reject(state, AuthoringCode.ComputeOutsideProgram)
    state.target != ShaderTarget.Gles31 -> reject(state, AuthoringCode.ComputeOutsideGles31)
    else -> accept(state.copy(place = AuthoringPlace.Compute))
}

private fun enterGeometry(state: AuthoringState): AuthoringStep = when {
    state.place != AuthoringPlace.Program -> reject(state, AuthoringCode.GeometryOutsideProgram)
    state.target != ShaderTarget.Gles32 -> reject(state, AuthoringCode.GeometryOutsideGles32)
    else -> accept(state.copy(place = AuthoringPlace.Geometry))
}

private fun enterTessControl(state: AuthoringState): AuthoringStep = when {
    state.place != AuthoringPlace.Program -> reject(state, AuthoringCode.TessControlOutsideProgram)
    state.target != ShaderTarget.Gles32 -> reject(state, AuthoringCode.TessControlOutsideGles32)
    else -> accept(state.copy(place = AuthoringPlace.TessControl))
}

private fun enterTessEval(state: AuthoringState): AuthoringStep = when {
    state.place != AuthoringPlace.Program -> reject(state, AuthoringCode.TessEvalOutsideProgram)
    state.target != ShaderTarget.Gles32 -> reject(state, AuthoringCode.TessEvalOutsideGles32)
    else -> accept(state.copy(place = AuthoringPlace.TessEval))
}

private fun emitVertex(state: AuthoringState): AuthoringStep = when {
    state.target == ShaderTarget.Gles32 && state.place == AuthoringPlace.Geometry -> accept(state)
    else -> reject(state, AuthoringCode.EmitVertexOutsideGeometry)
}

private fun endPrimitive(state: AuthoringState): AuthoringStep = when {
    state.target == ShaderTarget.Gles32 && state.place == AuthoringPlace.Geometry -> accept(state)
    else -> reject(state, AuthoringCode.EndPrimitiveOutsideGeometry)
}

private fun tessLevel(state: AuthoringState): AuthoringStep = when {
    state.target == ShaderTarget.Gles32 && state.place == AuthoringPlace.TessControl -> accept(state)
    else -> reject(state, AuthoringCode.TessLevelOutsideTessControl)
}

private fun glPositionLegal(state: AuthoringState): Boolean = when (state.place) {
    AuthoringPlace.Vertex -> state.target == ShaderTarget.Gles30 || state.target == ShaderTarget.Gles32
    AuthoringPlace.Geometry, AuthoringPlace.TessEval -> state.target == ShaderTarget.Gles32
    else -> false
}

private fun storageWrite(state: AuthoringState): AuthoringStep = when {
    state.target == ShaderTarget.Gles31 && state.place == AuthoringPlace.Compute -> accept(state)
    else -> reject(state, AuthoringCode.StorageWriteOutsideCompute)
}

private fun enterFunction(state: AuthoringState): AuthoringStep = when (state.place) {
    AuthoringPlace.Function -> reject(state, AuthoringCode.NestedFunction)
    AuthoringPlace.Vertex, AuthoringPlace.Fragment, AuthoringPlace.Compute,
    AuthoringPlace.Geometry, AuthoringPlace.TessControl, AuthoringPlace.TessEval,
    -> accept(state.copy(place = AuthoringPlace.Function, functionParent = state.place))
    AuthoringPlace.Program -> reject(state, AuthoringCode.FunctionOutsideStage)
}

private fun leaveStage(state: AuthoringState): AuthoringStep = when (state.place) {
    AuthoringPlace.Vertex, AuthoringPlace.Fragment, AuthoringPlace.Compute,
    AuthoringPlace.Geometry, AuthoringPlace.TessControl, AuthoringPlace.TessEval,
    -> accept(state.copy(place = AuthoringPlace.Program))
    else -> reject(state, AuthoringCode.LeaveOutsideStage)
}

private fun leaveFunction(state: AuthoringState): AuthoringStep {
    val parent = state.functionParent ?: return reject(state, AuthoringCode.LeaveOutsideFunction)
    return accept(state.copy(place = parent, functionParent = null))
}

private fun fragmentOut(state: AuthoringState): AuthoringStep = when {
    state.target == ShaderTarget.Agsl -> reject(state, AuthoringCode.FragmentOutOnAgsl)
    (state.target == ShaderTarget.Gles30 || state.target == ShaderTarget.Gles32) &&
        state.place == AuthoringPlace.Fragment -> accept(state)
    else -> reject(state, AuthoringCode.FragmentOutOutsideFragment)
}

private fun attribute(state: AuthoringState): AuthoringStep = when {
    state.place == AuthoringPlace.Function -> reject(state, AuthoringCode.AttributeInsideFunction)
    (state.target == ShaderTarget.Gles30 || state.target == ShaderTarget.Gles32) &&
        state.place == AuthoringPlace.Vertex -> accept(state)
    else -> reject(state, AuthoringCode.AttributeOutsideVertex)
}

private fun allow(state: AuthoringState, legal: Boolean, code: AuthoringCode): AuthoringStep =
    if (legal) accept(state) else reject(state, code)

private fun fragmentOnly(state: AuthoringState): Boolean = when (state.place) {
    AuthoringPlace.Fragment -> true
    AuthoringPlace.Function -> state.functionParent == AuthoringPlace.Fragment
    else -> false
}

private fun computeOnly(state: AuthoringState): Boolean = when (state.place) {
    AuthoringPlace.Compute -> true
    AuthoringPlace.Function -> state.functionParent == AuthoringPlace.Compute
    else -> false
}

private val STAGE_PARENTS = setOf(
    AuthoringPlace.Vertex,
    AuthoringPlace.Fragment,
    AuthoringPlace.Compute,
    AuthoringPlace.Geometry,
    AuthoringPlace.TessControl,
    AuthoringPlace.TessEval,
)

internal fun requireAuthoring(action: AuthoringAction) {
    val gate = authoringGate.get() ?: throw AuthoringException(authoringFallback(action))
    gate(action)
}

internal fun <T> withAuthoring(advance: (AuthoringAction) -> Unit, block: () -> T): T {
    val previous = authoringGate.get()
    authoringGate.set(advance)
    try {
        return block()
    } finally {
        if (previous == null) authoringGate.remove() else authoringGate.set(previous)
    }
}

private fun authoringFallback(action: AuthoringAction): AuthoringCode = when (action) {
    AuthoringAction.Derivative -> AuthoringCode.DerivativeOutsideFragment
    AuthoringAction.Discard -> AuthoringCode.DiscardOutsideFragment
    AuthoringAction.DeclareShared -> AuthoringCode.SharedOutsideCompute
    AuthoringAction.Barrier -> AuthoringCode.BarrierOutsideCompute
    else -> AuthoringCode.RepeatOutsideStage
}

private val authoringGate = ThreadLocal<(AuthoringAction) -> Unit>()

private fun accept(state: AuthoringState): AuthoringStep = AuthoringStep(state, null)

private fun reject(state: AuthoringState, code: AuthoringCode): AuthoringStep =
    AuthoringStep(state, code)
