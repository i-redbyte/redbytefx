package ru.redbyte.redbytefx

public enum class ShaderTarget {
    Agsl,
    Gles30,
    Gles31,
}

internal enum class AuthoringPlace {
    Program,
    Vertex,
    Fragment,
    Compute,
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
    EnterFunction,
    LeaveFunction,
    LeaveStage,
    Sample,
    Texture,
    Attribute,
    GlPosition,
    FragmentOut,
    StorageWrite,
    Let,
    Return,
}

internal enum class AuthoringCode {
    UniformOutsideProgram,
    UniformInsideFunction,
    SamplerOutsideProgram,
    SamplerOnAgsl,
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
    LetOutsideStage,
    ReturnOutsideStage,
}

internal data class AuthoringState(
    val target: ShaderTarget,
    val place: AuthoringPlace,
    val functionParent: AuthoringPlace? = null,
) {
    init {
        val parentOk = when (place) {
            AuthoringPlace.Function ->
                functionParent == AuthoringPlace.Vertex || functionParent == AuthoringPlace.Fragment
            else -> functionParent == null
        }
        require(parentOk) { "Function state requires a vertex or fragment parent" }
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
        AuthoringAction.StorageWrite -> storageWrite(state)
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
            state.target == ShaderTarget.Gles30 && state.place == AuthoringPlace.Fragment,
            AuthoringCode.TextureOutsideGlesFragment,
        )
        AuthoringAction.Attribute -> attribute(state)
        AuthoringAction.GlPosition -> allow(
            state,
            state.target == ShaderTarget.Gles30 && state.place == AuthoringPlace.Vertex,
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
    }

private fun declareUniform(state: AuthoringState): AuthoringStep = when (state.place) {
    AuthoringPlace.Program -> accept(state)
    AuthoringPlace.Function -> reject(state, AuthoringCode.UniformInsideFunction)
    AuthoringPlace.Vertex, AuthoringPlace.Fragment, AuthoringPlace.Compute ->
        reject(state, AuthoringCode.UniformOutsideProgram)
}

private fun declareSampler(state: AuthoringState): AuthoringStep = when {
    state.target == ShaderTarget.Agsl -> reject(state, AuthoringCode.SamplerOnAgsl)
    state.place == AuthoringPlace.Program -> accept(state)
    else -> reject(state, AuthoringCode.SamplerOutsideProgram)
}

private fun declareUniformBlock(state: AuthoringState): AuthoringStep = when {
    state.target == ShaderTarget.Agsl -> reject(state, AuthoringCode.UniformBlockOnAgsl)
    state.target == ShaderTarget.Gles30 && state.place == AuthoringPlace.Program -> accept(state)
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
    state.target != ShaderTarget.Gles30 -> reject(state, AuthoringCode.VertexOnAgsl)
    else -> accept(state.copy(place = AuthoringPlace.Vertex))
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

private fun storageWrite(state: AuthoringState): AuthoringStep = when {
    state.target == ShaderTarget.Gles31 && state.place == AuthoringPlace.Compute -> accept(state)
    else -> reject(state, AuthoringCode.StorageWriteOutsideCompute)
}

private fun enterFunction(state: AuthoringState): AuthoringStep = when (state.place) {
    AuthoringPlace.Function -> reject(state, AuthoringCode.NestedFunction)
    AuthoringPlace.Vertex, AuthoringPlace.Fragment -> accept(
        state.copy(place = AuthoringPlace.Function, functionParent = state.place),
    )
    AuthoringPlace.Program, AuthoringPlace.Compute -> reject(state, AuthoringCode.FunctionOutsideStage)
}

private fun leaveStage(state: AuthoringState): AuthoringStep = when (state.place) {
    AuthoringPlace.Vertex, AuthoringPlace.Fragment, AuthoringPlace.Compute ->
        accept(state.copy(place = AuthoringPlace.Program))
    else -> reject(state, AuthoringCode.LeaveOutsideStage)
}

private fun leaveFunction(state: AuthoringState): AuthoringStep {
    val parent = state.functionParent ?: return reject(state, AuthoringCode.LeaveOutsideFunction)
    return accept(state.copy(place = parent, functionParent = null))
}

private fun fragmentOut(state: AuthoringState): AuthoringStep = when {
    state.target == ShaderTarget.Agsl -> reject(state, AuthoringCode.FragmentOutOnAgsl)
    state.target == ShaderTarget.Gles30 && state.place == AuthoringPlace.Fragment -> accept(state)
    else -> reject(state, AuthoringCode.FragmentOutOutsideFragment)
}

private fun attribute(state: AuthoringState): AuthoringStep = when {
    state.place == AuthoringPlace.Function -> reject(state, AuthoringCode.AttributeInsideFunction)
    state.target == ShaderTarget.Gles30 && state.place == AuthoringPlace.Vertex -> accept(state)
    else -> reject(state, AuthoringCode.AttributeOutsideVertex)
}

private fun allow(state: AuthoringState, legal: Boolean, code: AuthoringCode): AuthoringStep =
    if (legal) accept(state) else reject(state, code)

private fun accept(state: AuthoringState): AuthoringStep = AuthoringStep(state, null)

private fun reject(state: AuthoringState, code: AuthoringCode): AuthoringStep =
    AuthoringStep(state, code)
