package ru.redbyte.redbytefx

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class GeometryShaderTest {

    @Test
    fun conditionalPositionDoesNotAuthorizeAnUnconditionalEmit() {
        val missingPosition = assertThrows(ProgramException::class.java) {
            shader(ShaderTarget.Gles32) {
                vertex { glPosition(attributeVec4("position")) }
                geometry(GeometryInput.Points, GeometryOutput.Points, 1) {
                    whenTrue(1f.lit gt 0f.lit) { glPosition(glIn(0)) }
                    emitVertex()
                }
                fragment { vec4(0f.lit, 0f.lit, 0f.lit, 1f.lit) }
            }
        }
        assertEquals(ProgramCode.EmitVertexWithoutPosition, missingPosition.code)

        val valid = shader(ShaderTarget.Gles32) {
            vertex { glPosition(attributeVec4("position")) }
            geometry(GeometryInput.Points, GeometryOutput.Points, 1) {
                repeat(1) { glPosition(glIn(0)) }
                emitVertex()
            }
            fragment { vec4(0f.lit, 0f.lit, 0f.lit, 1f.lit) }
        }
        assertTrue(valid.geometrySource().contains("EmitVertex();"))
    }

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
        assertFalse(program.geometrySource().contains("gl_PerVertex"))
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

    @Test
    fun geometryAndTessellationCarryUniformsAndVaryings() {
        val withGeometry = shader(ShaderTarget.Gles32) {
            val gain = uniform("gain", 0.5f)
            val mark = varyingVec2("mark")
            vertex {
                mark.set(attributeVec2("uv"))
                glPosition(attributeVec4("position"))
            }
            geometry(GeometryInput.Points, GeometryOutput.Points, 1) {
                glPosition(glIn(0) + vec4(gain.expr, 0f.lit, 0f.lit, 0f.lit))
                emitVertex()
            }
            fragment { vec4(mark.expr.x, mark.expr.y, gain.expr, 1f.lit) }
        }
        val declaration = "highp vec2 v_mark"
        assertTrue(withGeometry.vertexSource().contains("out rb_pipe {"))
        assertTrue(withGeometry.vertexSource().contains(declaration))
        assertTrue(withGeometry.geometrySource().contains("in rb_pipe {"))
        assertTrue(withGeometry.geometrySource().contains("out rb_pipe {"))
        assertTrue(withGeometry.geometrySource().contains(declaration))
        assertTrue(withGeometry.geometrySource().contains("uniform highp float u_gain;"))
        assertTrue(withGeometry.geometrySource().contains("gs_out.v_mark = gs_in[0].v_mark;"))
        assertTrue(withGeometry.fragmentSource().contains("in rb_pipe {"))
        assertTrue(withGeometry.fragmentSource().contains(declaration))

        val withTess = shader(ShaderTarget.Gles32) {
            val gain = uniform("gain", 0.25f)
            val mark = varyingVec2("mark")
            vertex {
                mark.set(attributeVec2("uv"))
                glPosition(attributeVec4("position"))
            }
            tessControl(1) {
                tessLevelOuter(0, gain.expr)
                passPosition()
            }
            tessEval(TessPrimitive.Isolines) {
                glPosition(glIn(0))
            }
            fragment { vec4(mark.expr.x, 0f.lit, gain.expr, 1f.lit) }
        }
        assertTrue(withTess.tessControlSource().contains(declaration))
        assertTrue(withTess.tessControlSource().contains("uniform highp float u_gain;"))
        assertTrue(withTess.tessControlSource().contains(
            "tc_out[gl_InvocationID].v_mark = tc_in[gl_InvocationID].v_mark;",
        ))
        assertTrue(withTess.tessEvalSource().contains(declaration))
        assertTrue(withTess.tessEvalSource().contains("te_out.v_mark = te_in[0].v_mark;"))
        assertTrue(withTess.fragmentSource().contains(declaration))
    }

    @Test
    fun geometryRejectsAForeignUniformAMissingPositionAndTheWrongFunction() {
        lateinit var foreign: HighFloat
        shader(ShaderTarget.Gles30) {
            val gain = uniform("gain", 1f)
            foreign = gain.expr
            vertex { glPosition(attributeVec4("position")) }
            fragment { vec4(gain.expr, 0f.lit, 0f.lit, 1f.lit) }
        }
        val foreignUniform = assertThrows(ProgramException::class.java) {
            shader(ShaderTarget.Gles32) {
                vertex { glPosition(attributeVec4("position")) }
                geometry(GeometryInput.Points, GeometryOutput.Points, 1) {
                    glPosition(vec4(foreign, 0f.lit, 0f.lit, 1f.lit))
                    emitVertex()
                }
                fragment { vec4(0f.lit, 0f.lit, 0f.lit, 1f.lit) }
            }
        }
        assertEquals(ProgramCode.ForeignUniform, foreignUniform.code)

        val missingPosition = assertThrows(ProgramException::class.java) {
            shader(ShaderTarget.Gles32) {
                vertex { glPosition(attributeVec4("position")) }
                geometry(GeometryInput.Points, GeometryOutput.Points, 1) {
                    emitVertex()
                }
                fragment { vec4(0f.lit, 0f.lit, 0f.lit, 1f.lit) }
            }
        }
        assertEquals(ProgramCode.EmitVertexWithoutPosition, missingPosition.code)

        lateinit var readPosition: Fn0<Vec4<Flt<High>>>
        val wrongStage = assertThrows(ProgramException::class.java) {
            shader(ShaderTarget.Gles32) {
                vertex {
                    val position = attributeVec4("position")
                    readPosition = fn { position }
                    glPosition(readPosition())
                }
                geometry(GeometryInput.Points, GeometryOutput.Points, 1) {
                    glPosition(readPosition())
                    emitVertex()
                }
                fragment { vec4(0f.lit, 0f.lit, 0f.lit, 1f.lit) }
            }
        }
        assertEquals(ProgramCode.FunctionWrongStage, wrongStage.code)
    }

    @Test
    fun geometryForwardsTheVertexEachEmitJustWrote() {
        val program = shader(ShaderTarget.Gles32) {
            val mark = varyingVec2("mark")
            varyingVec3("spare")
            vertex {
                mark.set(attributeVec2("uv"))
                glPosition(attributeVec4("position"))
            }
            geometry(GeometryInput.Triangles, GeometryOutput.TriangleStrip, 3) {
                glPosition(glIn(0))
                emitVertex()
                glPosition(glIn(1))
                emitVertex()
                glPosition(glIn(2))
                emitVertex()
            }
            fragment { vec4(mark.expr.x, mark.expr.y, 0f.lit, 1f.lit) }
        }
        val geometry = program.geometrySource()
        assertTrue(geometry.contains("gs_out.v_mark = gs_in[0].v_mark;\n  EmitVertex();"))
        assertTrue(geometry.contains("gs_out.v_mark = gs_in[1].v_mark;\n  EmitVertex();"))
        assertTrue(geometry.contains("gs_out.v_mark = gs_in[2].v_mark;\n  EmitVertex();"))
        assertTrue(geometry.contains("gl_PerVertex"))
        assertTrue(program.vertexSource().contains("out gl_PerVertex"))
        assertFalse(geometry.contains("spare"))
        assertFalse(program.vertexSource().contains("spare"))
        assertFalse(program.fragmentSource().contains("spare"))

        val dynamic = shader(ShaderTarget.Gles32) {
            val mark = varyingVec2("mark")
            vertex {
                mark.set(attributeVec2("uv"))
                glPosition(attributeVec4("position"))
            }
            geometry(GeometryInput.Triangles, GeometryOutput.TriangleStrip, 3) {
                repeat(3) { index ->
                    glPosition(glIn(index))
                    emitVertex()
                }
            }
            fragment { vec4(mark.expr.x, mark.expr.y, 0f.lit, 1f.lit) }
        }
        val repeated = dynamic.geometrySource()
        assertTrue(repeated.contains("gl_in[i].gl_Position"))
        assertTrue(repeated.contains("gs_out.v_mark = gs_in[i].v_mark;"))

        val mixed = assertThrows(ProgramException::class.java) {
            shader(ShaderTarget.Gles32) {
                val mark = varyingVec2("mark")
                vertex {
                    mark.set(attributeVec2("uv"))
                    glPosition(attributeVec4("position"))
                }
                geometry(GeometryInput.Triangles, GeometryOutput.TriangleStrip, 3) {
                    glPosition(glIn(0) + glIn(1))
                    emitVertex()
                }
                fragment { vec4(mark.expr.x, mark.expr.y, 0f.lit, 1f.lit) }
            }
        }
        assertEquals(ProgramCode.VaryingForward, mixed.code)
        assertTrue(mixed.message!!.contains("varying.set"))

        val outside = shader(ShaderTarget.Gles32) {
            val mark = varyingVec2("mark")
            vertex {
                mark.set(attributeVec2("uv"))
                glPosition(attributeVec4("position"))
            }
            geometry(GeometryInput.Points, GeometryOutput.Points, 2) {
                glPosition(glIn(0))
                repeat(2) {
                    emitVertex()
                }
            }
            fragment { vec4(mark.expr.x, mark.expr.y, 0f.lit, 1f.lit) }
        }
        val forwarded = outside.geometrySource()
        assertTrue(forwarded.contains("for (int i = 0; i < 2; ++i) {\n    gs_out.v_mark = gs_in[0].v_mark;\n    EmitVertex();\n  }"))

        val consumed = assertThrows(ProgramException::class.java) {
            shader(ShaderTarget.Gles32) {
                val mark = varyingVec2("mark")
                vertex {
                    mark.set(attributeVec2("uv"))
                    glPosition(attributeVec4("position"))
                }
                geometry(GeometryInput.Points, GeometryOutput.Points, 2) {
                    glPosition(glIn(0))
                    emitVertex()
                    repeat(1) { emitVertex() }
                }
                fragment { vec4(mark.expr.x, mark.expr.y, 0f.lit, 1f.lit) }
            }
        }
        assertEquals(ProgramCode.VaryingForward, consumed.code)

        val branched = shader(ShaderTarget.Gles32) {
            val mark = varyingVec2("mark")
            vertex {
                mark.set(attributeVec2("uv"))
                glPosition(attributeVec4("position"))
            }
            geometry(GeometryInput.Points, GeometryOutput.Points, 1) {
                glPosition(glIn(0))
                whenTrue(0f.lit.lt(1f.lit)) {
                    emitVertex()
                }
            }
            fragment { vec4(mark.expr.x, mark.expr.y, 0f.lit, 1f.lit) }
        }
        assertTrue(branched.geometrySource().contains("gs_out.v_mark = gs_in[0].v_mark;"))
    }

    @Test
    fun tessellationEvaluationInterpolatesWithTessCoord() {
        val program = shader(ShaderTarget.Gles32) {
            val mark = varyingVec2("mark")
            vertex {
                mark.set(attributeVec2("uv"))
                glPosition(attributeVec4("position"))
            }
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
            fragment { vec4(mark.expr.x, mark.expr.y, 0f.lit, 1f.lit) }
        }
        val evaluation = program.tessEvalSource()
        assertTrue(evaluation.contains("gl_TessCoord.x * te_in[0].v_mark"))
        assertTrue(evaluation.contains("gl_TessCoord.y * te_in[1].v_mark"))
        assertTrue(evaluation.contains("gl_TessCoord.z * te_in[2].v_mark"))
        assertTrue(evaluation.contains("gl_PerVertex"))

        val rejected = assertThrows(ProgramException::class.java) {
            shader(ShaderTarget.Gles32) {
                val mark = varyingVec2("mark")
                vertex {
                    mark.set(attributeVec2("uv"))
                    glPosition(attributeVec4("position"))
                }
                tessControl(4) {
                    tessLevelOuter(0, 1f.lit)
                    passPosition()
                }
                tessEval(TessPrimitive.Triangles) {
                    glPosition(glIn(0))
                }
                fragment { vec4(mark.expr.x, 0f.lit, 0f.lit, 1f.lit) }
            }
        }
        assertEquals(ProgramCode.TessInterpolation, rejected.code)
    }
}
