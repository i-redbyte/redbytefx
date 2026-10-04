package ru.redbyte.redbytefx

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class VectorCompareTest {

    @Test
    fun componentCompareReturnsABoolVectorOfTheSameLength() {
        val pair = vec2(1f.lit, 0f.lit) gt vec2(0f.lit, 1f.lit)
        val triple = vec3(1f.lit, 0f.lit, 1f.lit) lt vec3(0f.lit, 1f.lit, 0f.lit)
        val quad = vec4(1f.lit, 0f.lit, 1f.lit, 0f.lit) ge vec4(0f.lit, 1f.lit, 0f.lit, 1f.lit)

        assertEquals(Shape.Vector(ScalarKind.Bool, null, 2), pair.shape)
        assertEquals(Shape.Vector(ScalarKind.Bool, null, 3), triple.shape)
        assertEquals(Shape.Vector(ScalarKind.Bool, null, 4), quad.shape)
        assertEquals(Shape.Scalar(ScalarKind.Bool, null), any(pair).shape)
        assertEquals(Shape.Scalar(ScalarKind.Bool, null), all(quad).shape)
        assertEquals(Shape.Vector(ScalarKind.Bool, null, 2), (vec2(1f.med, 0f.med) eq vec2(1f.med, 0f.med)).shape)
    }

    @Test
    fun mismatchedPrecisionAndLengthAreRejected() {
        assertThrows(IllegalArgumentException::class.java) {
            vectorCompare<ShType>(CompareOp.Gt, vec2(1f.lit, 0f.lit), vec2(1f.med, 0f.med))
        }
        assertThrows(IllegalArgumentException::class.java) {
            vectorCompare<ShType>(CompareOp.Eq, vec2(1f.lit, 0f.lit), vec3(1f.lit, 0f.lit, 0f.lit))
        }
        assertThrows(IllegalArgumentException::class.java) {
            vectorCompare<ShType>(CompareOp.Lt, 1f.lit, vec2(1f.lit, 0f.lit))
        }
    }

    @Test
    fun bothTargetsSpellComponentFunctionsAndKeepScalarOperators() {
        val agsl = shader(ShaderTarget.Agsl) {
            fragment {
                val mask = let(vec2(1f.lit, 0f.lit) lt vec2(0.5f.lit, 0.5f.lit), "mask")
                val wide = let(vec3(1f.lit, 0f.lit, 1f.lit) gt vec3(0f.lit, 1f.lit, 0f.lit), "wide")
                val same = let(vec4(1f.med, 0f.med, 1f.med, 0f.med) eq vec4(1f.med, 0f.med, 1f.med, 0f.med), "same")
                val apart = vec2(0f.lit, 1f.lit) ne vec2(1f.lit, 0f.lit)
                val below = vec2(0f.lit, 0f.lit) le vec2(1f.lit, 1f.lit)
                val above = vec2(1f.lit, 1f.lit) ge vec2(0f.lit, 0f.lit)
                val hit = any(mask) and all(wide) and any(same) and any(apart) and all(below) and all(above)
                val amount = ifElse(hit, 1f.lit, 0f.lit)
                vec4(amount, amount, amount, 1f.lit)
            }
        }
        val agslSource = agsl.agslSource()
        assertTrue(agslSource.contains("bool2 mask = lessThan(float2(1.0, 0.0), float2(0.5, 0.5));"))
        assertTrue(agslSource.contains("greaterThan("))
        assertTrue(agslSource.contains("equal(half4("))
        assertTrue(agslSource.contains("notEqual("))
        assertTrue(agslSource.contains("lessThanEqual("))
        assertTrue(agslSource.contains("greaterThanEqual("))
        assertTrue(agslSource.contains("any("))
        assertTrue(agslSource.contains("all("))
        assertTrue(agslSource.contains("(hit > 0.0)") || agslSource.contains("? 1.0 : 0.0"))

        val glsl = shader(ShaderTarget.Gles30) {
            vertex { glPosition(attributeVec4("position")) }
            fragment {
                val mask = let(vec2(1f.lit, 0f.lit) lt vec2(0.5f.lit, 0.5f.lit), "mask")
                val hit = any(mask)
                vec4(ifElse(hit, 1f.lit, 0f.lit), 0f.lit, 0f.lit, 1f.lit)
            }
        }
        val fragment = glsl.fragmentSource()
        assertTrue(fragment.contains("bvec2 mask = lessThan(vec2(1.0, 0.0), vec2(0.5, 0.5));"))
        assertTrue(fragment.contains("any(mask)"))
        assertTrue(fragment.contains("(hit > ") || fragment.contains("? 1.0 : 0.0"))
    }

    @Test
    fun intComparisonsSpellTheSameOperatorsInAgslAndGlsl() {
        val agsl = shader(ShaderTarget.Agsl) {
            fragment {
                val hit = 2.intLit.gt(1.intLit) and 1.intLit.lt(2.intLit) and
                    2.intLit.ge(2.intLit) and 2.intLit.le(2.intLit) and
                    2.intLit.eq(2.intLit) and 2.intLit.ne(1.intLit) and
                    2.intLit.gte(1.intLit)
                vec4(ifElse(hit, 1f.lit, 0f.lit), 0f.lit, 0f.lit, 1f.lit)
            }
        }
        val agslSource = agsl.agslSource()
        assertTrue(agslSource.contains("(2 > 1)"))
        assertTrue(agslSource.contains("(1 < 2)"))
        assertTrue(agslSource.contains("(2 >= 2)"))
        assertTrue(agslSource.contains("(2 <= 2)"))
        assertTrue(agslSource.contains("(2 == 2)"))
        assertTrue(agslSource.contains("(2 != 1)"))
        val glsl = shader(ShaderTarget.Gles30) {
            vertex { glPosition(attributeVec4("position")) }
            fragment {
                val hit = 3.intLit.gt(1.intLit)
                vec4(ifElse(hit, 1f.lit, 0f.lit), 0f.lit, 0f.lit, 1f.lit)
            }
        }
        assertTrue(glsl.fragmentSource().contains("(3 > 1)"))
    }

    @Test
    fun booleanNotSpellsABangInAgslAndGlsl() {
        val agsl = shader(ShaderTarget.Agsl) {
            fragment {
                val hit = not(1f.lit.gt(0f.lit))
                vec4(ifElse(hit, 1f.lit, 0f.lit), 0f.lit, 0f.lit, 1f.lit)
            }
        }
        assertTrue(agsl.agslSource().contains("(!(1.0 > 0.0))"))
        val glsl = shader(ShaderTarget.Gles30) {
            vertex { glPosition(attributeVec4("position")) }
            fragment {
                val hit = not(1f.lit.gt(0f.lit))
                vec4(ifElse(hit, 1f.lit, 0f.lit), 0f.lit, 0f.lit, 1f.lit)
            }
        }
        assertTrue(glsl.fragmentSource().contains("(!(1.0 > 0.0))"))
    }
}
