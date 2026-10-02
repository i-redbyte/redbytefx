package ru.redbyte.redbytefx.gl

import android.opengl.GLES30
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith
import ru.redbyte.redbytefx.Expr
import ru.redbyte.redbytefx.Flt
import ru.redbyte.redbytefx.Vec4
import ru.redbyte.redbytefx.Fn1
import ru.redbyte.redbytefx.High
import ru.redbyte.redbytefx.Sampler2D
import ru.redbyte.redbytefx.ShaderTarget
import ru.redbyte.redbytefx.Uniform
import ru.redbyte.redbytefx.lit
import ru.redbyte.redbytefx.plus
import ru.redbyte.redbytefx.shader
import ru.redbyte.redbytefx.times
import ru.redbyte.redbytefx.vec4

@RunWith(AndroidJUnit4::class)
class Gles30DeviceTest {

    @Test
    fun driverLinksSpelledProgramAndReadsTheUniformBack() {
        EglPbuffer().use {
            assertTrue(GLES30.glGetString(GLES30.GL_VERSION).orEmpty().contains("OpenGL ES 3"))
            drainGlError()
            lateinit var amount: Uniform<Flt<High>>
            lateinit var image: Uniform<Sampler2D>
            val program = shader(ShaderTarget.Gles30) {
                val uv = varyingVec2("uv")
                amount = uniform("amount", 0f)
                image = sampler2D("image")
                vertex {
                    uv.set(attributeVec2("uv"))
                    glPosition(attributeVec4("position"))
                }
                fragment { texture(image, uv.expr) * amount.expr }
            }
            val runtime = GlProgramRuntime(program, Gles30Device())
            runtime.link()
            runtime.use()
            assertNoGlError("link")
            val current = IntArray(1)
            GLES30.glGetIntegerv(GLES30.GL_CURRENT_PROGRAM, current, 0)
            assertTrue(GLES30.glIsProgram(current[0]))

            assertTrue(runtime.set(amount, 0.25f))
            val location = GLES30.glGetUniformLocation(current[0], "u_amount")
            assertTrue(location >= 0)
            val value = FloatArray(1)
            GLES30.glGetUniformfv(current[0], location, value, 0)
            assertEquals(0.25f, value[0], 0f)

            val names = IntArray(1)
            GLES30.glGenTextures(1, names, 0)
            assertTrue(runtime.bind(image, names[0]))
            val sampler = GLES30.glGetUniformLocation(current[0], "u_image")
            val unit = IntArray(1)
            GLES30.glGetUniformiv(current[0], sampler, unit, 0)
            assertEquals(0, unit[0])
            assertNoGlError("uniforms")

            runtime.destroy()
            assertFalse(GLES30.glIsProgram(current[0]))
            val after = IntArray(1)
            GLES30.glGetIntegerv(GLES30.GL_CURRENT_PROGRAM, after, 0)
            assertEquals(0, after[0])
            assertNoGlError("destroy")
            GLES30.glDeleteTextures(1, names, 0)
        }
    }

    @Test
    fun driverLinksAPureFunctionCopiedIntoBothStages() {
        EglPbuffer().use {
            drainGlError()
            lateinit var bump: Fn1<Flt<High>, Flt<High>>
            val program = shader(ShaderTarget.Gles30) {
                val time = uniformTime()
                vertex {
                    bump = fn(0f.lit, "bump") { value -> value + time.expr }
                    glPosition(vec4(bump(0f.lit), 0f.lit, 0f.lit, 1f.lit))
                }
                fragment {
                    val shade = bump(time.expr)
                    vec4(shade, shade, shade, 1f.lit)
                }
            }
            val runtime = GlProgramRuntime(program, Gles30Device())
            runtime.link()
            runtime.destroy()
            assertNoGlError("pure function")
        }
    }

    @Test
    fun driverLinksTwoFragmentOutputs() {
        EglPbuffer().use {
            drainGlError()
            val program = shader(ShaderTarget.Gles30) {
                vertex { glPosition(attributeVec4("position")) }
                fragment {
                    val albedo = outVec4("albedo", 0)
                    val glow = outVec4("glow", 1)
                    albedo.set(vec4(1f.lit, 0f.lit, 0f.lit, 1f.lit))
                    glow.set(vec4(0f.lit, 0f.lit, 0f.lit, 1f.lit))
                    vec4(0f.lit, 0f.lit, 0f.lit, 1f.lit)
                }
            }
            val runtime = GlProgramRuntime(program, Gles30Device())
            runtime.link()
            runtime.destroy()
            assertNoGlError("fragment outputs")
        }
    }

    @Test
    fun driverLinksAUniformBlock() {
        EglPbuffer().use {
            drainGlError()
            lateinit var time: ru.redbyte.redbytefx.Expr<Flt<High>>
            val program = shader(ShaderTarget.Gles30) {
                uniformBlock("frame") {
                    time = float("time")
                }
                vertex { glPosition(attributeVec4("position")) }
                fragment { vec4(time, time, time, 1f.lit) }
            }
            val runtime = GlProgramRuntime(program, Gles30Device())
            runtime.link()
            runtime.destroy()
            assertNoGlError("uniform block")
        }
    }

    @Test
    fun driverLinksAComputeStorageBlock() {
        EglPbuffer().use {
            drainGlError()
            lateinit var value: Expr<Vec4<Flt<High>>>
            val program = shader(ShaderTarget.Gles31) {
                storageBlock("cells") {
                    value = vec4("value")
                }
                compute(64) { value.store(value) }
            }
            val version = IntArray(2)
            GLES30.glGetIntegerv(GLES30.GL_MAJOR_VERSION, version, 0)
            GLES30.glGetIntegerv(GLES30.GL_MINOR_VERSION, version, 1)
            assumeTrue(version[0] > 3 || (version[0] == 3 && version[1] >= 1))
            val runtime = GlProgramRuntime(program, Gles30Device())
            runtime.link()
            runtime.set(checkNotNull(program.storageBlock), floatArrayOf(1f, 0f, 0f, 1f))
            runtime.destroy()
            assertNoGlError("compute storage")
        }
    }

    @Test
    fun driverRejectsAShaderThatDoesNotCompile() {
        EglPbuffer().use {
            drainGlError()
            val device = Gles30Device()
            val shader = device.createShader(GlStage.Fragment)
            try {
                device.shaderSource(shader, "#version 300 es\nvoid main() { not_glsl }\n")
                val status = device.compileShader(shader)
                assertFalse(status.ok)
                assertNoGlError("compile failure")
            } finally {
                device.deleteShader(shader)
            }
        }
    }

    @Test
    fun driverRejectsALinkWhenTheFragmentInputIsMissing() {
        EglPbuffer().use {
            drainGlError()
            val device = Gles30Device()
            val vertex = compile(device, GlStage.Vertex, VERTEX)
            val fragment = compile(device, GlStage.Fragment, FRAGMENT_MISSING_INPUT)
            val program = device.createProgram()
            try {
                device.attachShader(program, vertex)
                device.attachShader(program, fragment)
                val status = device.linkProgram(program)
                assertFalse(status.infoLog, status.ok)
                assertNoGlError("link failure")
            } finally {
                device.deleteShader(vertex)
                device.deleteShader(fragment)
                device.deleteProgram(program)
            }
        }
    }
}

private const val VERTEX = """
#version 300 es
void main() {
  gl_Position = vec4(0.0);
}
"""

private const val FRAGMENT_MISSING_INPUT = """
#version 300 es
precision highp float;
in vec4 v_missing;
out vec4 oColor;
void main() {
  oColor = v_missing;
}
"""

private fun compile(device: Gles30Device, stage: GlStage, source: String): Int {
    val shader = device.createShader(stage)
    device.shaderSource(shader, source.trimIndent())
    val status = device.compileShader(shader)
    check(status.ok) { status.infoLog }
    return shader
}

private fun drainGlError() {
    GLES30.glGetError()
}

private fun assertNoGlError(where: String) {
    assertEquals(where, GLES30.GL_NO_ERROR, GLES30.glGetError())
}
