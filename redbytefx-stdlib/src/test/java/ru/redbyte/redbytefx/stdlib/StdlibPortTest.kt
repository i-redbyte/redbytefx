package ru.redbyte.redbytefx.stdlib

import org.junit.Assert.assertTrue
import org.junit.Test
import ru.redbyte.redbytefx.ShaderTarget
import ru.redbyte.redbytefx.color
import ru.redbyte.redbytefx.div
import ru.redbyte.redbytefx.float2
import ru.redbyte.redbytefx.float3
import ru.redbyte.redbytefx.shader

class StdlibPortTest {

    @Test
    fun circleMaskSpellsASmoothstep() {
        val program = shader(ShaderTarget.Agsl) {
            fragment {
                val mask = circleMask(fragCoord / resolution, float2(0.5f, 0.5f), 0.2f)
                color(float3(mask, mask, mask), 1f)
            }
        }
        assertTrue(program.agslSource().contains("smoothstep"))
    }
}
