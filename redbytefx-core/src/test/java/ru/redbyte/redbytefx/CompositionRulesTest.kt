package ru.redbyte.redbytefx

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class CompositionRulesTest {
    @Test
    fun compositionRulesStayInPlace() {
        val names = ShaderDsl.VertexDsl::class.java.methods.map { it.name }
        assertFalse(names.contains("discard"))

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
                val pixel = fn(vec2(0f.lit, 0f.lit)) { _ -> this@fragment.fragCoord }
                val coord = pixel(vec2(0f.lit, 0f.lit))
                val sampledColor = color(vec2(0f.lit, 0f.lit))
                val span = distance(vec2(0f.lit, 0f.lit), vec2(3f.lit, 4f.lit))
                val hit = not(2.intLit.gt(1.intLit))
                vec4(ifElse(hit, span, sampledColor.x + coord.x), 0f.lit, 0f.lit, 1f.lit)
            }
        }
        val fragment = textured.fragmentSource()
        assertTrue(fragment.contains("texture("))
        assertTrue(fragment.contains("gl_FragCoord.xy"))
        assertTrue(fragment.contains("distance(vec2(0.0, 0.0), vec2(3.0, 4.0))"))
        assertTrue(fragment.contains("(!(2 > 1))"))

        val rejected = assertThrows(AuthoringException::class.java) {
            shader(ShaderTarget.Gles30) {
                vertex { glPosition(attributeVec4("position")) }
                fragment { vec4(resolution.x, 0f.lit, 0f.lit, 1f.lit) }
            }
        }
        assertEquals(AuthoringCode.ResolutionOnGles, rejected.code)
    }
}
