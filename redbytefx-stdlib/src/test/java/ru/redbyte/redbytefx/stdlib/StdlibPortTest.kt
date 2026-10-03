package ru.redbyte.redbytefx.stdlib

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
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

    @Test
    fun sphereAndTorusCompileOnAgslAndGles() {
        fun program(target: ShaderTarget) = shader(target) {
            if (target != ShaderTarget.Agsl) {
                vertex { glPosition(attributeVec4("position")) }
            }
            fragment {
                val point = float3(0.2f, -0.1f, 0.4f)
                val sphere = sdSphere(point, 0.4f)
                val ring = sdTorus(point, 0.6f, 0.15f)
                val body = sdfUnion(sdfSubtract(sdBox3(point, float3(0.3f, 0.2f, 0.2f)), sphere), ring)
                val capsule = sdCapsule(point, float3(-0.2f, 0f, 0f), float3(0.2f, 0.1f, 0f), 0.05f)
                val plane = sdPlane(point, float3(0f, 1f, 0f), 0.1f)
                val triangle = sdTriangle(
                    point,
                    float3(0f, 0f, 0f),
                    float3(1f, 0f, 0f),
                    float3(0f, 1f, 0f),
                )
                val merged = smoothMin(sdfIntersect(body, plane), sdfUnion(capsule, triangle), 0.08f)
                val distance = rayMarch(float3(0f, 0f, -2f), float3(0f, 0f, 1f), steps = 8) {
                    sdSphere(it, 0.5f)
                }
                val normal = sdfNormal(point) { sdSphere(it, 0.5f) }
                vec4(merged, distance, normal.x, 1f.lit)
            }
        }

        val agsl = program(ShaderTarget.Agsl).agslSource()
        assertTrue(agsl.contains("length("))
        assertTrue(agsl.contains("for (int"))
        assertTrue(agsl.contains("normalize("))

        val gles = program(ShaderTarget.Gles30).fragmentSource()
        assertTrue(gles.contains("length("))
        assertTrue(gles.contains("for (int"))
        assertTrue(gles.contains("normalize("))
    }

    @Test
    fun lambertCompilesOnAgslAndGles() {
        fun program(target: ShaderTarget) = shader(target) {
            if (target != ShaderTarget.Agsl) {
                vertex { glPosition(attributeVec4("position")) }
            }
            fragment {
                val shade = lambert(float3(0f, 1f, 0f), float3(0.2f, 0.8f, 0.1f))
                vec4(shade, shade, shade, 1f.lit)
            }
        }
        assertTrue(program(ShaderTarget.Agsl).agslSource().contains("normalize("))
        assertTrue(program(ShaderTarget.Gles30).fragmentSource().contains("max("))
    }

    @Test
    fun rayMarchNormalizesTheDirectionOnceOutsideTheLoop() {
        val source = shader(ShaderTarget.Gles30) {
            vertex { glPosition(attributeVec4("position")) }
            fragment {
                val distance = rayMarch(float3(0f, 0f, -2f), float3(0f, 0f, 1f), steps = 8) {
                    sdSphere(it, 0.5f)
                }
                vec4(distance, distance, distance, 1f.lit)
            }
        }.fragmentSource()
        assertEquals(1, Regex("normalize\\(").findAll(source).count())
        assertTrue(source.indexOf("normalize(") < source.indexOf("for (int"))
    }

    @Test
    fun rayMarchRejectsAStepCountPastTheRepeatLimit() {
        assertThrows(IllegalArgumentException::class.java) {
            shader(ShaderTarget.Agsl) {
                fragment {
                    val distance = rayMarch(float3(0f, 0f, 0f), float3(0f, 0f, 1f), steps = 0) {
                        sdSphere(it, 1f)
                    }
                    vec4(distance, distance, distance, 1f.lit)
                }
            }
        }
        assertThrows(IllegalArgumentException::class.java) {
            shader(ShaderTarget.Agsl) {
                fragment {
                    val distance = rayMarch(float3(0f, 0f, 0f), float3(0f, 0f, 1f), steps = 65) {
                        sdSphere(it, 1f)
                    }
                    vec4(distance, distance, distance, 1f.lit)
                }
            }
        }
    }
}
