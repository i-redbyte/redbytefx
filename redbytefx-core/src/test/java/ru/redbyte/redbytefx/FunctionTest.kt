package ru.redbyte.redbytefx

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class FunctionTest {

    @Test
    fun functionIfAndTimeSpellInAgsl() {
        val program = shader(ShaderTarget.Agsl) {
            val time = uniformTime()
            fragment {
                val wave = fn(0f.lit, "wave") { t ->
                    val shaped = sin(t).let("shaped")
                    ifElse(shaped.gt(0f.lit), shaped, 0f.lit)
                }
                val sample = wave(time.expr)
                vec4(sample, sample, sample, 1f.lit)
            }
        }
        val source = program.agslSource()
        assertTrue(source.contains("uniform float u_time;"))
        assertTrue(source.contains("float wave(float p0) {"))
        assertTrue(source.contains("float shaped = sin(p0);"))
        assertTrue(source.contains("return ((shaped > 0.0) ? shaped : 0.0);"))
        assertTrue(source.contains("wave(u_time)"))
        val helper = source.substringBefore("half4 main")
        assertTrue(helper.contains("float shaped = sin(p0);"))
        assertTrue(!source.substringAfter("half4 main").contains("float shaped"))
    }

    @Test
    fun functionSpellsTheSameCallInGlsl() {
        val program = shader(ShaderTarget.Gles30) {
            val time = uniformTime()
            vertex { glPosition(attributeVec4("position")) }
            fragment {
                val wave = fn(0f.lit, "wave") { t -> ifElse(t.gt(0.5f.lit), 1f.lit, 0f.lit) }
                val sample = wave(time.expr)
                vec4(sample, sample, sample, 1f.lit)
            }
        }
        val fragment = program.fragmentSource()
        assertTrue(fragment.contains("highp float wave(highp float p0) {"))
        assertTrue(fragment.contains("return ((p0 > 0.5) ? 1.0 : 0.0);"))
        assertTrue(fragment.contains("wave(u_time)"))
    }

    @Test
    fun aVaryingReadInsideAFunctionMustBeWrittenByTheVertex() {
        val error = assertThrows(ProgramException::class.java) {
            shader(ShaderTarget.Gles30) {
                val uv = varyingVec2("uv")
                vertex { glPosition(attributeVec4("position")) }
                fragment {
                    val sample = fn(vec2(0f.lit, 0f.lit), "sample") { coord -> coord.x + uv.expr.x }
                    vec4(sample(vec2(0f.lit, 0f.lit)), 0f.lit, 0f.lit, 1f.lit)
                }
            }
        }
        assertEquals(ProgramCode.VaryingNotWritten, error.code)
    }

    @Test
    fun aVertexFunctionCannotBeCalledFromTheFragment() {
        lateinit var bump: Fn1<Flt<High>, Flt<High>>
        val error = assertThrows(ProgramException::class.java) {
            shader(ShaderTarget.Gles30) {
                vertex {
                    val position = attributeVec4("position")
                    bump = fn(0f.lit, "bump") { t -> t + position.x }
                    glPosition(position)
                }
                fragment {
                    val y = bump(0f.lit)
                    vec4(y, y, y, 1f.lit)
                }
            }
        }
        assertEquals(ProgramCode.FunctionWrongStage, error.code)
    }

    @Test
    fun aPureFunctionIsCopiedIntoBothStages() {
        lateinit var bump: Fn1<Flt<High>, Flt<High>>
        val program = shader(ShaderTarget.Gles30) {
            val time = uniformTime()
            vertex {
                bump = fn(0f.lit, "bump") { t -> t + time.expr }
                glPosition(vec4(bump(0f.lit), 0f.lit, 0f.lit, 1f.lit))
            }
            fragment {
                val y = bump(time.expr)
                vec4(y, y, y, 1f.lit)
            }
        }
        val vertex = program.vertexSource()
        val fragment = program.fragmentSource()
        assertTrue(vertex.contains("highp float bump(highp float p0)"))
        assertTrue(fragment.contains("highp float bump(highp float p0)"))
        assertTrue(vertex.contains("bump(0.0)"))
        assertTrue(fragment.contains("bump(u_time)"))
    }

    @Test
    fun recursionIsRejectedBeforeEmission() {
        val direct = assertThrows(ProgramException::class.java) {
            shader(ShaderTarget.Agsl) {
                fragment {
                    fn(0f.lit, "loop") { t -> recur(t) }
                    vec4(0f.lit, 0f.lit, 0f.lit, 1f.lit)
                }
            }
        }
        assertEquals(ProgramCode.RecursiveFunction, direct.code)

        val shape = Shape.Scalar(ScalarKind.Float, Precision.High)
        val param = Expr<Flt<High>>(shape, ExprNode.Param("p0"))
        val first = UserFunction(
            "earlier",
            AuthoringPlace.Fragment,
            listOf(Formal("p0", shape)),
            param,
            shape,
        )
        val second = UserFunction(
            "later",
            AuthoringPlace.Fragment,
            listOf(Formal("p0", shape)),
            param,
            shape,
        )
        first.body = Expr<ShType>(shape, ExprNode.UserCall(second, listOf(param)))
        second.body = Expr<ShType>(shape, ExprNode.UserCall(first, listOf(param)))
        val cycle = assertThrows(ProgramException::class.java) {
            rejectRecursion(listOf(first, second))
        }
        assertEquals(ProgramCode.RecursiveFunction, cycle.code)
    }

    @Test
    fun nestedFunctionAndUniformAreRejectedAndFragmentCallsAreNot() {
        val nested = assertThrows(AuthoringException::class.java) {
            shader(ShaderTarget.Agsl) {
                fragment {
                    fn {
                        this@fragment.fn { 1f.lit }
                        1f.lit
                    }
                    vec4(0f.lit, 0f.lit, 0f.lit, 1f.lit)
                }
            }
        }
        assertEquals(AuthoringCode.NestedFunction, nested.code)

        val uniform = assertThrows(AuthoringException::class.java) {
            shader(ShaderTarget.Agsl) {
                fragment {
                    fn {
                        this@shader.uniform("inside", 1f)
                        1f.lit
                    }
                    vec4(0f.lit, 0f.lit, 0f.lit, 1f.lit)
                }
            }
        }
        assertEquals(AuthoringCode.UniformInsideFunction, uniform.code)

        val sampled = shader(ShaderTarget.Agsl) {
            fragment {
                val tone = fn { this@fragment.sample() }
                tone()
            }
        }
        assertTrue(sampled.agslSource().contains("rb_sample"))
        lateinit var image: Uniform<Sampler2D>
        val textured = shader(ShaderTarget.Gles30) {
            image = sampler2D("image")
            vertex { glPosition(attributeVec4("position")) }
            fragment {
                val color = fn(vec2(0f.lit, 0f.lit)) { uv -> this@fragment.texture(image, uv) }
                color(vec2(0f.lit, 0f.lit))
            }
        }
        assertTrue(textured.fragmentSource().contains("texture("))
    }

    @Test
    fun threeAndFourArgumentFunctionsStayOutOfMain() {
        val program = shader(ShaderTarget.Agsl) {
            fragment {
                val mix3 = fn(0f.lit, 0f.lit, 0f.lit, "mix3") { left, mid, right ->
                    val sum = (left + mid + right).let("p0")
                    sum
                }
                val mix4 = fn(0f.lit, 0f.lit, 0f.lit, 0f.lit, "mix4") { a, b, c, d ->
                    a + b + c + d
                }
                val sample = mix4(mix3(1f.lit, 2f.lit, 3f.lit), 0f.lit, 0f.lit, 0f.lit)
                vec4(sample, sample, sample, 1f.lit)
            }
        }
        val source = program.agslSource()
        assertTrue(source.contains("half4 main"))
        assertTrue(source.contains("float mix3(float p0, float p1, float p2) {"))
        assertTrue(source.contains("float p0_1 = ((p0 + p1) + p2);"))
        assertTrue(source.contains("float mix4(float p0, float p1, float p2, float p3) {"))
        val main = source.substringAfter("half4 main")
        assertTrue(!main.contains("float p0_1"))
        assertTrue(main.contains("mix4(mix3(1.0, 2.0, 3.0), 0.0, 0.0, 0.0)"))
    }

    @Test
    fun aThreeArgumentFunctionRejectsTheWrongShape() {
        lateinit var mix3: Fn3<Flt<High>, Flt<High>, Flt<High>, Flt<High>>
        shader(ShaderTarget.Agsl) {
            fragment {
                mix3 = fn(0f.lit, 0f.lit, 0f.lit, "mix3") { left, mid, right -> left + mid + right }
                vec4(0f.lit, 0f.lit, 0f.lit, 1f.lit)
            }
        }
        @Suppress("UNCHECKED_CAST")
        val lie = vec2(0f.lit, 1f.lit) as Expr<Flt<High>>
        assertThrows(IllegalArgumentException::class.java) { mix3(lie, 0f.lit, 0f.lit) }
        assertThrows(IllegalArgumentException::class.java) { mix3(0f.lit, lie, 0f.lit) }
        assertThrows(IllegalArgumentException::class.java) { mix3(0f.lit, 0f.lit, lie) }
    }

    @Test
    fun aVertexFunctionIsAbsentFromTheFragmentStage() {
        val program = shader(ShaderTarget.Gles30) {
            vertex {
                val lift = fn(0f.lit, 0f.lit, 0f.lit, 0f.lit, "lift") { a, b, c, d -> a + b + c + d }
                val y = lift(0f.lit, 0f.lit, 0f.lit, 1f.lit)
                glPosition(vec4(0f.lit, y, 0f.lit, 1f.lit))
            }
            fragment {
                vec4(0f.lit, 0f.lit, 0f.lit, 1f.lit)
            }
        }
        val vertex = program.vertexSource()
        val fragment = program.fragmentSource()
        assertTrue(vertex.contains("highp float lift(highp float p0, highp float p1, highp float p2, highp float p3)"))
        assertTrue(vertex.contains("lift(0.0, 0.0, 0.0, 1.0)"))
        assertTrue(!fragment.contains("lift"))
    }

    @Test
    fun aFunctionRejectsAnArgumentOfTheWrongShape() {
        lateinit var wave: Fn1<Flt<High>, Flt<High>>
        shader(ShaderTarget.Agsl) {
            fragment {
                wave = fn(0f.lit, "wave") { t -> t }
                vec4(0f.lit, 0f.lit, 0f.lit, 1f.lit)
            }
        }
        @Suppress("UNCHECKED_CAST")
        val lie = vec2(0f.lit, 1f.lit) as Expr<Flt<High>>
        assertThrows(IllegalArgumentException::class.java) { wave(lie) }
    }
}
