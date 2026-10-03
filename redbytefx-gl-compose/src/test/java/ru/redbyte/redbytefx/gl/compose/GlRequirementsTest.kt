package ru.redbyte.redbytefx.gl.compose

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.redbyte.redbytefx.GeometryInput
import ru.redbyte.redbytefx.GeometryOutput
import ru.redbyte.redbytefx.ShaderTarget
import ru.redbyte.redbytefx.lit
import ru.redbyte.redbytefx.shader
import ru.redbyte.redbytefx.vec4

class GlRequirementsTest {
    @Test
    fun gles30ProgramGetsEs30Hint() {
        val program = shader(ShaderTarget.Gles30) {
            vertex {
                glPosition(vec4(0f.lit, 0f.lit, 0f.lit, 1f.lit))
            }
            fragment { vec4(0f.lit, 0f.lit, 0f.lit, 1f.lit) }
        }
        assertEquals(glEs30LinkRequirement(), program.glLinkRequirementHint())
        assertEquals(0, eglClientMinor(program))
    }

    @Test
    fun computeProgramGetsEs31Hint() {
        val program = shader(ShaderTarget.Gles31) {
            compute(1) { }
        }
        assertEquals(glEs31LinkRequirement(), program.glLinkRequirementHint())
        assertEquals(1, eglClientMinor(program))
    }

    @Test
    fun geometryProgramGetsEs32Hint() {
        val program = shader(ShaderTarget.Gles32) {
            vertex {
                glPosition(vec4(0f.lit, 0f.lit, 0f.lit, 1f.lit))
            }
            geometry(GeometryInput.Points, GeometryOutput.Points, 1) {
                glPosition(vec4(0f.lit, 0f.lit, 0f.lit, 1f.lit))
                emitVertex()
            }
            fragment { vec4(0f.lit, 0f.lit, 0f.lit, 1f.lit) }
        }
        assertEquals(glEs32LinkRequirement(), program.glLinkRequirementHint())
        assertEquals(2, eglClientMinor(program))
    }

    @Test
    fun linkMessageCombinesRequirementAndDetail() {
        val message = linkMessage(
            requirement = "Need ES 3.2.",
            error = ru.redbyte.redbytefx.gl.GlException(
                ru.redbyte.redbytefx.gl.GlCode.LinkFailed,
                "compile failed",
            ),
        )
        assertTrue(message.contains("Need ES 3.2."))
        assertTrue(message.contains("compile failed"))
    }
}
