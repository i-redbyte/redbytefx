package ru.redbyte.redbytefx

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class GeometryShaderTest {

    @Test
    fun gles32SpellsGeometryAndTessellationAtVersion320() {
        val program = shader(ShaderTarget.Gles32) {
            vertex { glPosition(attributeVec4("position")) }
            tessControl(3) {
                tessLevelOuter(0, 1f.lit)
                tessLevelOuter(1, 1f.lit)
                tessLevelOuter(2, 1f.lit)
                tessLevelInner(0, 1f.lit)
                passPosition()
            }
            tessEval(TessPrimitive.Triangles) {
                glPosition(glIn(0))
            }
            geometry(GeometryInput.Triangles, GeometryOutput.TriangleStrip, 3) {
                glPosition(glIn(0))
                emitVertex()
                endPrimitive()
            }
            fragment { vec4(1f.lit, 0f.lit, 0f.lit, 1f.lit) }
        }
        assertTrue(program.vertexSource().contains("#version 320 es"))
        assertTrue(program.fragmentSource().contains("#version 320 es"))
        val control = program.tessControlSource()
        assertTrue(control.contains("layout(vertices = 3) out;"))
        assertTrue(control.contains("gl_TessLevelOuter[0] = 1.0;"))
        assertTrue(control.contains("gl_out[gl_InvocationID].gl_Position = gl_in[gl_InvocationID].gl_Position;"))
        val evaluation = program.tessEvalSource()
        assertTrue(evaluation.contains("layout(triangles, equal_spacing, ccw) in;"))
        assertTrue(evaluation.contains("gl_Position = gl_in[0].gl_Position;"))
        val geometry = program.geometrySource()
        assertTrue(geometry.contains("layout(triangles) in;"))
        assertTrue(geometry.contains("layout(triangle_strip, max_vertices = 3) out;"))
        assertTrue(geometry.contains("EmitVertex();"))
        assertTrue(geometry.contains("EndPrimitive();"))
        assertFalse(program.vertexSource().contains("EmitVertex"))
    }

    @Test
    fun geometryAndALoneTessellationStageAreRejected() {
        val onGles30 = assertThrows(AuthoringException::class.java) {
            shader(ShaderTarget.Gles30) {
                geometry(GeometryInput.Points, GeometryOutput.Points, 1) {
                    glPosition(glIn(0))
                    emitVertex()
                }
                vertex { glPosition(attributeVec4("position")) }
                fragment { vec4(0f.lit, 0f.lit, 0f.lit, 1f.lit) }
            }
        }
        assertEquals(AuthoringCode.GeometryOutsideGles32, onGles30.code)

        val missing = assertThrows(ProgramException::class.java) {
            shader(ShaderTarget.Gles32) {
                vertex { glPosition(attributeVec4("position")) }
                tessControl(3) { passPosition() }
                fragment { vec4(0f.lit, 0f.lit, 0f.lit, 1f.lit) }
            }
        }
        assertEquals(ProgramCode.TessStageMissing, missing.code)
    }
}
