package ru.redbyte.redbytefx.stdlib

import org.junit.Assert.assertTrue
import org.junit.Test
import ru.redbyte.redbytefx.ShaderTarget
import ru.redbyte.redbytefx.lit
import ru.redbyte.redbytefx.shader
import ru.redbyte.redbytefx.vec4

class ExprMappingTest {

    @Test
    fun remapSpellsThroughTheNewShader() {
        val program = shader(ShaderTarget.Agsl) {
            val value = uniform("value", 0.25f)
            fragment {
                val mapped = remap(value.expr, 0f.lit, 1f.lit, 0.2f.lit, 0.9f.lit)
                vec4(mapped, mapped, mapped, 1f.lit)
            }
        }
        val source = program.agslSource()
        assertTrue(source.contains("mix(0.2, 0.9,"))
        assertTrue(source.contains("((u_value - 0.0) / (1.0 - 0.0))"))
    }
}
