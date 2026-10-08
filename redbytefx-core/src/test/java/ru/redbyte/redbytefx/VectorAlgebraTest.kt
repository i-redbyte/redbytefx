package ru.redbyte.redbytefx

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class VectorAlgebraTest {

    @Test
    fun swizzlePreservesPrecisionAndUsesTheMaskAutomaton() {
        val high = vec4(1f.lit, 2f.lit, 3f.lit, 4f.lit)
        val med = vec4(1f.med, 0f.med, 1f.med, 1f.med)

        assertEquals(Shape.Vector(ScalarKind.Float, Precision.High, 2), high.xy.shape)
        assertEquals(Shape.Vector(ScalarKind.Float, Precision.High, 2), high.xz.shape)
        assertEquals(Shape.Scalar(ScalarKind.Float, Precision.High), high.w.shape)
        assertEquals(Shape.Vector(ScalarKind.Float, Precision.Med, 3), med.rgb.shape)
        assertEquals("xz", (high.xz.node as ExprNode.Swizzle).mask)
        val agsl = shader(ShaderTarget.Agsl) {
            fragment { vec4(high.xz.x, high.xz.y, 0f.lit, 1f.lit) }
        }.agslSource()
        assertTrue(agsl.contains("float4(1.0, 2.0, 3.0, 4.0).xz"))

        assertThrows(IllegalArgumentException::class.java) { swizzle(high, "xg") }
        assertThrows(IllegalArgumentException::class.java) { swizzle(high, "xyzwx") }
        assertThrows(IllegalArgumentException::class.java) { swizzle(vec2(0f.lit, 1f.lit), "z") }
        assertThrows(IllegalArgumentException::class.java) {
            swizzleShape(Shape.Matrix(2), listOf(0))
        }
    }

    @Test
    fun intArithmeticSpellsAsAgslInts() {
        val sum = 1.intLit + 2.intLit
        assertEquals(Shape.Scalar(ScalarKind.Int, null), sum.shape)
        assertEquals(Shape.Scalar(ScalarKind.Float, Precision.High), sum.toFloat().shape)
        val agsl = shader(ShaderTarget.Agsl) {
            fragment {
                val converted = sum.toFloat()
                val negative = (-3.intLit).toFloat()
                vec4(converted, negative, 0f.lit, 1f.lit)
            }
        }.agslSource()
        assertTrue(agsl.contains("float((1 + 2))"))
        assertTrue(agsl.contains("float((-3))"))
        assertThrows(IllegalArgumentException::class.java) {
            integralArithShape(ArithOp.Add, 1.intLit.shape, 1f.lit.shape)
        }
    }

    @Test
    fun matrixVectorProductMatchesLanesAndPrecision() {
        val matrix = mat2(vec2(1f.lit, 0f.lit), vec2(0f.lit, 1f.lit))
        val vector = vec2(2f.lit, 3f.lit)

        assertEquals(vector.shape, matVecShape(matrix.shape, vector.shape))
        assertEquals(vector.shape, (matrix * vector).shape)
        assertThrows(IllegalArgumentException::class.java) {
            matVecShape(Shape.Matrix(4), vector.shape)
        }
        assertThrows(IllegalArgumentException::class.java) {
            matVecShape(matrix.shape, vec2(1f.med, 0f.med).shape)
        }
    }

    @Test
    fun shaderSpellsSwizzleMatrixAndIntCast() {
        val program = shader(ShaderTarget.Agsl) {
            fragment {
                val rows = mat2(vec2(1f.lit, 0f.lit), vec2(0f.lit, 1f.lit)).let("rows")
                val shift = (1.intLit + 2.intLit).toFloat()
                sample((rows * fragCoord).xy + vec2(shift, 0f.lit))
            }
        }

        val agsl = program.agslSource()
        assertTrue(agsl.contains("float2x2 rows = float2x2(float2(1.0, 0.0), float2(0.0, 1.0));"))
        assertTrue(agsl.contains("(rows * fragCoord).xy"))
        assertTrue(agsl.contains("float((1 + 2))"))
    }

    @Test
    fun matrixProductSpellsOrdinaryMultiplication() {
        val left = mat4(
            vec4(1f.lit, 0f.lit, 0f.lit, 0f.lit),
            vec4(0f.lit, 1f.lit, 0f.lit, 0f.lit),
            vec4(0f.lit, 0f.lit, 1f.lit, 0f.lit),
            vec4(0f.lit, 0f.lit, 0f.lit, 1f.lit),
        )
        val right = mat4(
            vec4(2f.lit, 0f.lit, 0f.lit, 0f.lit),
            vec4(0f.lit, 2f.lit, 0f.lit, 0f.lit),
            vec4(0f.lit, 0f.lit, 2f.lit, 0f.lit),
            vec4(0f.lit, 0f.lit, 0f.lit, 1f.lit),
        )
        assertEquals(Shape.Matrix(4), (left * right).shape)
        val painted = (left * right) * vec4(1f.lit, 0f.lit, 0f.lit, 0f.lit)
        val agsl = shader(ShaderTarget.Agsl) {
            fragment { vec4(painted.x, painted.y, painted.z, painted.w) }
        }
        assertTrue(agsl.agslSource().contains(" * float4x4("))
        val glsl = shader(ShaderTarget.Gles30) {
            vertex { glPosition(attributeVec4("position")) }
            fragment { vec4(painted.x, painted.y, painted.z, painted.w) }
        }
        assertTrue(glsl.fragmentSource().contains(" * mat4("))
        assertThrows(IllegalArgumentException::class.java) {
            matMatShape(Shape.Matrix(2), Shape.Matrix(4))
        }
    }
}
