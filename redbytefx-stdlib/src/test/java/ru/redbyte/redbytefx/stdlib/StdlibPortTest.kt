package ru.redbyte.redbytefx.stdlib

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.redbyte.redbytefx.ShaderTarget
import ru.redbyte.redbytefx.color
import ru.redbyte.redbytefx.div
import ru.redbyte.redbytefx.float2
import ru.redbyte.redbytefx.float3
import ru.redbyte.redbytefx.lit
import ru.redbyte.redbytefx.shader
import ru.redbyte.redbytefx.vec4
import ru.redbyte.redbytefx.x

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

    @Test
    fun screenFillAndStrokeUseFwidth() {
        val agsl = shader(ShaderTarget.Agsl) {
            fragment {
                val distance = fragCoord.x
                val fill = softFillScreen(distance)
                val edge = strokeScreen(distance, 4f.lit)
                color(fill, edge, 0f.lit, 1f.lit)
            }
        }
        val agslSource = agsl.agslSource()
        assertTrue(agslSource.contains("fwidth("))
        assertFalse(agslSource.contains("0.02"))

        val glsl = shader(ShaderTarget.Gles30) {
            vertex { glPosition(attributeVec4("position")) }
            fragment {
                val distance = 1f.lit
                vec4(softFillScreen(distance), strokeScreen(distance, 2f.lit), 0f.lit, 1f.lit)
            }
        }
        val fragment = glsl.fragmentSource()
        assertTrue(fragment.contains("fwidth("))
        assertFalse(fragment.contains("0.02"))
    }
}
