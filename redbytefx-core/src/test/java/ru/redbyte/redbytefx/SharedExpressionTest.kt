package ru.redbyte.redbytefx

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SharedExpressionTest {

    @Test(timeout = 5_000)
    fun deeplySharedGraphStaysLinearInGlsl() {
        val source = shader(ShaderTarget.Gles30) {
            fragment {
                var value = fragCoord.x
                for (level in 0 until 40) value = value * value + value
                vec4(value, value, value, 1f.lit)
            }
        }.fragmentSource()

        assertTrue("source has ${source.length} chars", source.length < 8_000)
    }

    @Test(timeout = 5_000)
    fun deeplySharedGraphStaysLinearInAgsl() {
        val source = shader(ShaderTarget.Agsl) {
            fragment {
                var value = fragCoord.x
                for (level in 0 until 40) value = value * value + value
                vec4(value, value, value, 1f.lit)
            }
        }.agslSource()

        assertTrue("source has ${source.length} chars", source.length < 8_000)
    }

    @Test
    fun sharedSubexpressionIsComputedOnce() {
        val main = shader(ShaderTarget.Gles30) {
            fragment {
                val doubled = fragCoord.x * 2f
                vec4(doubled, doubled, doubled, 1f.lit)
            }
        }.fragmentSource().substringAfter("void main() {")

        assertEquals(1, main.occurrences("gl_FragCoord.xy.x * 2.0"))
    }

    @Test
    fun expressionUsedOnceIsNotHoisted() {
        val main = shader(ShaderTarget.Gles30) {
            fragment {
                val doubled = fragCoord.x * 2f
                vec4(doubled, 0f.lit, 0f.lit, 1f.lit)
            }
        }.fragmentSource().substringAfter("void main() {")

        assertTrue(main.contains("oColor = vec4((gl_FragCoord.xy.x * 2.0), 0.0, 0.0, 1.0);"))
    }

    @Test
    fun sharingNeverCrossesAStatementThatWritesALocal() {
        val main = shader(ShaderTarget.Gles30) {
            fragment {
                val counter = local(1f.lit, "counter")
                val next = counter.expr * 2f
                counter.set(next)
                vec4(next, next, next, 1f.lit)
            }
        }.fragmentSource().substringAfter("void main() {")

        val write = main.indexOf("counter = (counter * 2.0);")
        val reread = main.indexOf("= (counter * 2.0);", write + 1)
        assertTrue(main, write >= 0)
        assertTrue("the result re-reads counter after the write", reread > write)
    }

    @Test
    fun sharedSampleIsReadOnceInAgsl() {
        val main = shader(ShaderTarget.Agsl) {
            fragment {
                val color = sample(fragCoord)
                color * color
            }
        }.agslSource().substringAfter("half4 main(float2 fragCoord) {")

        assertEquals(1, main.occurrences("rb_sample(fragCoord)"))
    }

    @Test
    fun sharedArgumentOfUserFunctionBodyIsHoistedInsideTheFunction() {
        val source = shader(ShaderTarget.Gles30) {
            fragment {
                val square = fn(0f.lit, name = "twice_sum") { x ->
                    val sum = x + 1f
                    sum * sum
                }
                val value = square(fragCoord.x)
                vec4(value, value, value, 1f.lit)
            }
        }.fragmentSource()
        val body = source.substringAfter("twice_sum(highp float p0) {").substringBefore("}")

        assertEquals(1, body.occurrences("p0 + 1.0"))
    }

    @Test(timeout = 5_000)
    fun hashingADeeplySharedGraphIsLinear() {
        var value: Expr<Flt<High>> = 1f.lit
        for (level in 0 until 60) value = value * value + value
        val set = HashSet<Expr<*>>()
        set += value

        assertTrue(value in set)
        assertTrue(value == value)
    }

    @Test(timeout = 5_000)
    fun comparingTwoSeparatelyBuiltSharedGraphsIsLinear() {
        fun build(): Expr<Flt<High>> {
            var value: Expr<Flt<High>> = 1f.lit
            for (level in 0 until 60) value = value * value + value
            return value
        }
        val left = build()
        val right = build()

        assertTrue(left == right)
        assertTrue(left in hashSetOf(right))
    }

    private fun String.occurrences(needle: String): Int = windowed(needle.length).count { it == needle }
}
