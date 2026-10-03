package ru.redbyte.redbytefx

import org.junit.Assert.assertEquals
import org.junit.Test

class AliasTest {

    @Test
    fun shortNamesAreTheFloatExpressions() {
        val scalar: HighFloat = 1f.lit
        val vector: HighVec4 = vec4(scalar, scalar, scalar, scalar)
        val color: MedVec4 = color(vector)
        lateinit var handle: HighFloatUniform
        shader(ShaderTarget.Agsl) {
            handle = uniformTime()
            fragment { vec4(handle.expr, handle.expr, handle.expr, 1f.lit) }
        }
        assertEquals(color.shape, color(1f.lit, 0f.lit, 0f.lit, 1f.lit).shape)
        assertEquals("time", handle.name)
    }
}
