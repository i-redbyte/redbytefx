package ru.redbyte.redbytefx

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class AuthoringMachineTest {

    @Test
    fun functionStateRemembersItsParentStage() {
        assertThrows(IllegalArgumentException::class.java) {
            authoringState(ShaderTarget.Agsl, AuthoringPlace.Function)
        }
        assertThrows(IllegalArgumentException::class.java) {
            authoringState(
                ShaderTarget.Gles30,
                AuthoringPlace.Vertex,
                AuthoringPlace.Fragment,
            )
        }

        val entered = authoringStep(agslFragment, AuthoringAction.EnterFunction)
        assertEquals(
            authoringState(ShaderTarget.Agsl, AuthoringPlace.Function, AuthoringPlace.Fragment),
            entered.state,
        )
        assertEquals(null, entered.code)

        val left = authoringStep(entered.state, AuthoringAction.LeaveFunction)
        assertEquals(agslFragment, left.state)
        assertEquals(null, left.code)
    }

    @Test
    fun legalActionsMatchTheStageTable() {
        assertAccepted(agslProgram, AuthoringAction.EnterFragment, agslFragment)
        assertAccepted(glesProgram, AuthoringAction.EnterVertex, glesVertex)
        assertAccepted(glesProgram, AuthoringAction.EnterFragment, glesFragment)
        assertAccepted(glesVertex, AuthoringAction.EnterFunction, glesFunctionFromVertex)
        assertAccepted(agslProgram, AuthoringAction.DeclareUniform, agslProgram)
        assertAccepted(glesProgram, AuthoringAction.DeclareSampler, glesProgram)
        assertAccepted(glesProgram, AuthoringAction.DeclareVarying, glesProgram)
        assertAccepted(agslFragment, AuthoringAction.Sample, agslFragment)
        assertAccepted(glesFragment, AuthoringAction.Texture, glesFragment)
        assertAccepted(glesVertex, AuthoringAction.Attribute, glesVertex)
        assertAccepted(glesVertex, AuthoringAction.GlPosition, glesVertex)
        assertAccepted(agslFragment, AuthoringAction.Let, agslFragment)
        assertAccepted(glesFunctionFromVertex, AuthoringAction.Return, glesFunctionFromVertex)
    }

    @Test
    fun illegalActionsKeepTheStateAndReturnAStableCode() {
        assertRejected(agslProgram, AuthoringAction.EnterVertex, AuthoringCode.VertexOnAgsl)
        assertRejected(agslFragment, AuthoringAction.EnterVertex, AuthoringCode.VertexOutsideProgram)
        assertRejected(glesVertex, AuthoringAction.EnterFragment, AuthoringCode.FragmentOutsideProgram)
        assertRejected(agslProgram, AuthoringAction.EnterFunction, AuthoringCode.FunctionOutsideStage)
        assertRejected(
            agslFunctionFromFragment,
            AuthoringAction.EnterFunction,
            AuthoringCode.NestedFunction,
        )
        assertRejected(agslProgram, AuthoringAction.LeaveFunction, AuthoringCode.LeaveOutsideFunction)
        assertRejected(agslFragment, AuthoringAction.DeclareUniform, AuthoringCode.UniformOutsideProgram)
        assertRejected(
            agslFunctionFromFragment,
            AuthoringAction.DeclareUniform,
            AuthoringCode.UniformInsideFunction,
        )
        assertRejected(agslProgram, AuthoringAction.DeclareSampler, AuthoringCode.SamplerOnAgsl)
        assertRejected(glesFragment, AuthoringAction.DeclareSampler, AuthoringCode.SamplerOutsideProgram)
        assertRejected(agslProgram, AuthoringAction.DeclareVarying, AuthoringCode.VaryingOnAgsl)
        assertRejected(agslProgram, AuthoringAction.DeclareUniformBlock, AuthoringCode.UniformBlockOnAgsl)
        assertRejected(glesFragment, AuthoringAction.DeclareUniformBlock, AuthoringCode.UniformBlockOutsideProgram)
        assertAccepted(glesProgram, AuthoringAction.DeclareUniformBlock, glesProgram)
        assertRejected(glesVertex, AuthoringAction.DeclareVarying, AuthoringCode.VaryingOutsideProgram)
        assertRejected(glesFragment, AuthoringAction.Sample, AuthoringCode.SampleOutsideAgslFragment)
        assertRejected(agslFragment, AuthoringAction.Texture, AuthoringCode.TextureOutsideGlesFragment)
        assertAccepted(agslFunctionFromFragment, AuthoringAction.Sample, agslFunctionFromFragment)
        assertRejected(glesFunctionFromVertex, AuthoringAction.Sample, AuthoringCode.SampleOutsideAgslFragment)
        assertAccepted(
            authoringState(ShaderTarget.Gles30, AuthoringPlace.Function, AuthoringPlace.Fragment),
            AuthoringAction.Texture,
            authoringState(ShaderTarget.Gles30, AuthoringPlace.Function, AuthoringPlace.Fragment),
        )
        assertRejected(glesFunctionFromVertex, AuthoringAction.Texture, AuthoringCode.TextureOutsideGlesFragment)
        assertRejected(
            glesFunctionFromVertex,
            AuthoringAction.Attribute,
            AuthoringCode.AttributeInsideFunction,
        )
        assertRejected(glesFragment, AuthoringAction.Attribute, AuthoringCode.AttributeOutsideVertex)
        assertRejected(glesFragment, AuthoringAction.GlPosition, AuthoringCode.GlPositionOutsideVertex)
        assertAccepted(glesFragment, AuthoringAction.FragmentOut, glesFragment)
        assertRejected(agslFragment, AuthoringAction.FragmentOut, AuthoringCode.FragmentOutOnAgsl)
        assertRejected(glesVertex, AuthoringAction.FragmentOut, AuthoringCode.FragmentOutOutsideFragment)
        assertAccepted(gles31Program, AuthoringAction.DeclareUniformBlock, gles31Program)
        assertAccepted(gles32Program, AuthoringAction.DeclareUniformBlock, gles32Program)
        assertRejected(agslProgram, AuthoringAction.Let, AuthoringCode.LetOutsideStage)
        assertRejected(agslProgram, AuthoringAction.Return, AuthoringCode.ReturnOutsideStage)
    }

    @Test
    fun computeIsItsOwnStageAndKeepsVertexAndFragmentOut() {
        assertAccepted(gles31Program, AuthoringAction.EnterCompute, gles31Compute)
        assertAccepted(gles31Compute, AuthoringAction.LeaveStage, gles31Program)
        assertAccepted(gles31Program, AuthoringAction.DeclareStorage, gles31Program)
        assertAccepted(gles31Compute, AuthoringAction.StorageWrite, gles31Compute)
        assertRejected(glesProgram, AuthoringAction.EnterCompute, AuthoringCode.ComputeOutsideGles31)
        assertRejected(agslProgram, AuthoringAction.EnterCompute, AuthoringCode.ComputeOutsideGles31)
        assertRejected(gles31Compute, AuthoringAction.EnterCompute, AuthoringCode.ComputeOutsideProgram)
        assertRejected(gles31Program, AuthoringAction.EnterVertex, AuthoringCode.VertexOnGles31)
        assertRejected(gles31Program, AuthoringAction.EnterFragment, AuthoringCode.FragmentOnGles31)
        assertRejected(glesProgram, AuthoringAction.DeclareStorage, AuthoringCode.StorageOutsideGles31)
        assertRejected(gles31Compute, AuthoringAction.DeclareStorage, AuthoringCode.StorageOutsideProgram)
        assertRejected(gles31Program, AuthoringAction.StorageWrite, AuthoringCode.StorageWriteOutsideCompute)
        assertAccepted(
            gles31Compute,
            AuthoringAction.EnterFunction,
            authoringState(ShaderTarget.Gles31, AuthoringPlace.Function, AuthoringPlace.Compute),
        )
        assertAccepted(gles32Program, AuthoringAction.EnterVertex, gles32Vertex)
        assertAccepted(gles32Program, AuthoringAction.EnterGeometry, gles32Geometry)
        assertAccepted(gles32Program, AuthoringAction.EnterTessControl, gles32TessControl)
        assertAccepted(gles32Program, AuthoringAction.EnterTessEval, gles32TessEval)
        assertAccepted(gles32Geometry, AuthoringAction.EmitVertex, gles32Geometry)
        assertAccepted(gles32TessControl, AuthoringAction.TessLevel, gles32TessControl)
        assertRejected(glesProgram, AuthoringAction.EnterGeometry, AuthoringCode.GeometryOutsideGles32)
        assertRejected(glesProgram, AuthoringAction.EnterTessControl, AuthoringCode.TessControlOutsideGles32)
        assertRejected(gles32Vertex, AuthoringAction.EmitVertex, AuthoringCode.EmitVertexOutsideGeometry)
        assertAccepted(
            gles32Geometry,
            AuthoringAction.EnterFunction,
            authoringState(ShaderTarget.Gles32, AuthoringPlace.Function, AuthoringPlace.Geometry),
        )
    }

    private fun assertAccepted(
        state: AuthoringState,
        action: AuthoringAction,
        expected: AuthoringState,
    ) {
        val step = authoringStep(state, action)
        assertEquals(expected, step.state)
        assertEquals(null, step.code)
    }

    private fun assertRejected(
        state: AuthoringState,
        action: AuthoringAction,
        code: AuthoringCode,
    ) {
        val step = authoringStep(state, action)
        assertEquals(state, step.state)
        assertEquals(code, step.code)
    }

    private val agslProgram = authoringState(ShaderTarget.Agsl, AuthoringPlace.Program)
    private val agslFragment = authoringState(ShaderTarget.Agsl, AuthoringPlace.Fragment)
    private val agslFunctionFromFragment = authoringState(
        ShaderTarget.Agsl,
        AuthoringPlace.Function,
        AuthoringPlace.Fragment,
    )
    private val glesProgram = authoringState(ShaderTarget.Gles30, AuthoringPlace.Program)
    private val glesVertex = authoringState(ShaderTarget.Gles30, AuthoringPlace.Vertex)
    private val glesFragment = authoringState(ShaderTarget.Gles30, AuthoringPlace.Fragment)
    private val gles32Program = authoringState(ShaderTarget.Gles32, AuthoringPlace.Program)
    private val gles32Vertex = authoringState(ShaderTarget.Gles32, AuthoringPlace.Vertex)
    private val gles32Geometry = authoringState(ShaderTarget.Gles32, AuthoringPlace.Geometry)
    private val gles32TessControl = authoringState(ShaderTarget.Gles32, AuthoringPlace.TessControl)
    private val gles32TessEval = authoringState(ShaderTarget.Gles32, AuthoringPlace.TessEval)
    private val gles31Program = authoringState(ShaderTarget.Gles31, AuthoringPlace.Program)
    private val gles31Compute = authoringState(ShaderTarget.Gles31, AuthoringPlace.Compute)
    private val glesFunctionFromVertex = authoringState(
        ShaderTarget.Gles30,
        AuthoringPlace.Function,
        AuthoringPlace.Vertex,
    )
}
