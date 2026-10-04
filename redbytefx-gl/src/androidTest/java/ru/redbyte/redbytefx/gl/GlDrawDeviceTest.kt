package ru.redbyte.redbytefx.gl

import android.opengl.GLES30
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.nio.ByteBuffer
import java.nio.ByteOrder
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import ru.redbyte.redbytefx.Expr
import ru.redbyte.redbytefx.Flt
import ru.redbyte.redbytefx.High
import ru.redbyte.redbytefx.Sampler2D
import ru.redbyte.redbytefx.SamplerCube
import ru.redbyte.redbytefx.ShaderProgram
import ru.redbyte.redbytefx.ShaderTarget
import ru.redbyte.redbytefx.Uniform
import ru.redbyte.redbytefx.Vec3
import ru.redbyte.redbytefx.Vec4
import ru.redbyte.redbytefx.lit
import ru.redbyte.redbytefx.plus
import ru.redbyte.redbytefx.shader
import ru.redbyte.redbytefx.vec2
import ru.redbyte.redbytefx.vec4
import ru.redbyte.redbytefx.x
import ru.redbyte.redbytefx.y

private const val SIZE = 4
private val QUAD = floatArrayOf(-1f, -1f, 1f, -1f, 1f, 1f, -1f, 1f)
private val QUAD_INDICES = intArrayOf(0, 1, 2, 0, 2, 3)

/** Draws through [GlProgramRuntime] into a [SIZE]x[SIZE] color target and reads the pixels back. */
@RunWith(AndroidJUnit4::class)
class GlDrawDeviceTest {

    @Test
    fun indexedDrawsFillTheTargetWithShortAndIntIndices() = canvas {
        lateinit var color: Uniform<Vec4<Flt<High>>>
        val runtime = link(
            shader(ShaderTarget.Gles30) {
                color = uniformVec4("color")
                vertex {
                    val position = attributeVec2("position")
                    glPosition(vec4(position.x, position.y, 0f.lit, 1f.lit))
                }
                fragment { color.expr }
            },
        )
        target(runtime)
        val vertices = runtime.createBuffer()
        runtime.replaceArrayBuffer(vertices, 0, QUAD)
        val elements = runtime.createBuffer()
        val short = runtime.elementBufferData(elements, QUAD_INDICES)
        assertEquals(IndexElementKind.UnsignedShort, short)
        runtime.use()
        runtime.set(color, 1f, 0f, 0f, 1f)
        attribute(runtime, vertices, "position", 2, 2, 0)
        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, elements)
        runtime.drawRange(GLES30.GL_TRIANGLES, 4, 0, 6, short, null)
        assertNoGlError("short draw")
        assertPixel(0, 0, 255, 0, 0)
        assertPixel(SIZE - 1, SIZE - 1, 255, 0, 0)

        val far = 65_536
        val wide = FloatArray((far + 4) * 2)
        QUAD.copyInto(wide, far * 2)
        runtime.replaceArrayBuffer(vertices, QUAD.size, wide)
        val int = runtime.elementBufferData(elements, IntArray(6) { far + QUAD_INDICES[it] })
        assertEquals(IndexElementKind.UnsignedInt, int)
        clear()
        runtime.set(color, 0f, 1f, 0f, 1f)
        attribute(runtime, vertices, "position", 2, 2, 0)
        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, elements)
        runtime.drawRange(GLES30.GL_TRIANGLES, far + 4, 0, 6, int, null)
        assertNoGlError("int draw")
        assertPixel(0, 0, 0, 255, 0)
        assertPixel(SIZE - 1, SIZE - 1, 0, 255, 0)
    }

    @Test
    fun anElementUploadDoesNotReplaceTheIndicesOfTheBoundMesh() = canvas {
        lateinit var color: Uniform<Vec4<Flt<High>>>
        val runtime = link(
            shader(ShaderTarget.Gles30) {
                color = uniformVec4("color", 0f, 0f, 1f, 1f)
                vertex {
                    val position = attributeVec2("position")
                    glPosition(vec4(position.x, position.y, 0f.lit, 1f.lit))
                }
                fragment { color.expr }
            },
        )
        target(runtime)
        val vertices = runtime.createBuffer()
        runtime.replaceArrayBuffer(vertices, 0, QUAD)
        val quad = runtime.createBuffer()
        val kind = runtime.elementBufferData(quad, QUAD_INDICES)
        attribute(runtime, vertices, "position", 2, 2, 0)
        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, quad)
        val other = runtime.createBuffer()
        runtime.elementBufferData(other, intArrayOf(0, 0, 0))
        GLES30.glBindVertexArray(vao)
        runtime.use()
        runtime.drawRange(GLES30.GL_TRIANGLES, 4, 0, 6, kind, null)
        assertNoGlError("draw after upload")
        assertPixel(SIZE - 1, SIZE - 1, 0, 0, 255)
    }

    @Test
    fun texturesUploadFromHeapArraysAndSubImagesReplaceTexels() = canvas {
        lateinit var image: Uniform<Sampler2D>
        val runtime = link(
            shader(ShaderTarget.Gles30) {
                image = sampler2D("image")
                val uv = varyingVec2("uv")
                vertex {
                    val position = attributeVec2("position")
                    uv.set(attributeVec2("uv"))
                    glPosition(vec4(position.x, position.y, 0f.lit, 1f.lit))
                }
                fragment { texture(image, uv.expr) }
            },
        )
        target(runtime)
        val texels = ByteArray(SIZE * SIZE * 4)
        for (index in 0 until SIZE * SIZE) {
            texels[index * 4] = (index * 16).toByte()
            texels[index * 4 + 1] = (255 - index * 16).toByte()
            texels[index * 4 + 2] = 64
            texels[index * 4 + 3] = -1
        }
        val texture = runtime.uploadRgba(SIZE, SIZE, texels)
        texturedQuad(runtime)
        runtime.use()
        runtime.bind(image, texture)
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_FAN, 0, 4)
        assertNoGlError("texture draw")
        assertPixel(0, 0, 0, 255, 64)
        assertPixel(2, 1, 6 * 16, 255 - 6 * 16, 64)

        val yellow = byteArrayOf(-1, -1, 0, -1)
        runtime.texSubImage2DRgba(texture, SIZE, SIZE, 2, 1, 1, 1, yellow)
        runtime.bind(image, texture)
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_FAN, 0, 4)
        assertNoGlError("sub image draw")
        assertPixel(2, 1, 255, 255, 0)
        assertPixel(1, 1, 5 * 16, 255 - 5 * 16, 64)
    }

    @Test
    fun aCubeMapIsSampledByDirection() = canvas {
        lateinit var sky: Uniform<SamplerCube>
        lateinit var direction: Uniform<Vec3<Flt<High>>>
        val runtime = link(
            shader(ShaderTarget.Gles30) {
                sky = samplerCube("sky")
                direction = uniformVec3("direction", 1f, 0f, 0f)
                vertex {
                    val position = attributeVec2("position")
                    glPosition(vec4(position.x, position.y, 0f.lit, 1f.lit))
                }
                fragment { textureCube(sky, direction.expr) }
            },
        )
        target(runtime)
        val cube = runtime.createTexture()
        CubeFace.entries.forEachIndexed { index, face ->
            runtime.uploadCubeFace(cube, face, 1, 1, byteArrayOf((index * 40).toByte(), 10, 20, -1))
        }
        positionQuad(runtime)
        runtime.use()
        runtime.bind(sky, cube)
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_FAN, 0, 4)
        assertPixel(1, 1, 0, 10, 20)
        runtime.set(direction, 0f, -1f, 0f)
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_FAN, 0, 4)
        assertNoGlError("cube draw")
        assertPixel(1, 1, CubeFace.NegativeY.ordinal * 40, 10, 20)
    }

    @Test
    fun anInstancedDrawPlacesEveryInstanceThenClearsTheDivisor() = canvas {
        val runtime = link(
            shader(ShaderTarget.Gles30) {
                vertex {
                    val position = attributeVec2("position")
                    val offset = attributeVec4("model3")
                    glPosition(vec4(position.x + offset.x, position.y + offset.y, 0f.lit, 1f.lit))
                }
                fragment { vec4(1f.lit, 0f.lit, 1f.lit, 1f.lit) }
            },
        )
        target(runtime)
        val leftHalf = floatArrayOf(-1f, -1f, 0f, -1f, 0f, 1f, -1f, 1f)
        val vertices = runtime.createBuffer()
        runtime.replaceArrayBuffer(vertices, 0, leftHalf)
        val elements = runtime.createBuffer()
        val kind = runtime.elementBufferData(elements, QUAD_INDICES)
        attribute(runtime, vertices, "position", 2, 2, 0)
        GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, elements)
        val instances = runtime.createBuffer()
        runtime.replaceArrayBuffer(instances, 0, floatArrayOf(0f, 0f, 0f, 0f, 1f, 0f, 0f, 0f))
        val model = runtime.attribLocation("a_model3")
        runtime.vertexAttribFloat(model, 4, 4, 0)
        runtime.vertexAttribDivisor(model, 1)
        runtime.use()
        runtime.drawRange(GLES30.GL_TRIANGLES, 4, 0, 6, kind, 2)
        runtime.vertexAttribDivisor(model, 0)
        runtime.disableVertexAttribArray(model)
        assertNoGlError("instanced draw")
        assertPixel(0, 0, 255, 0, 255)
        assertPixel(SIZE - 1, SIZE - 1, 255, 0, 255)
        val divisor = IntArray(1)
        GLES30.glGetVertexAttribiv(model, GLES30.GL_VERTEX_ATTRIB_ARRAY_DIVISOR, divisor, 0)
        assertEquals(0, divisor[0])
    }

    @Test
    fun twoProgramsKeepTheirOwnUniformBlock() = canvas {
        fun blockProgram(): ShaderProgram = shader(ShaderTarget.Gles30) {
            lateinit var color: Expr<Vec4<Flt<High>>>
            uniformBlock("frame") { color = vec4("color") }
            vertex {
                val position = attributeVec2("position")
                glPosition(vec4(position.x, position.y, 0f.lit, 1f.lit))
            }
            fragment { color }
        }
        val first = blockProgram()
        val second = blockProgram()
        val red = link(first)
        val green = link(second)
        target(red)
        red.set(checkNotNull(first.uniformBlock), floatArrayOf(1f, 0f, 0f, 1f))
        green.set(checkNotNull(second.uniformBlock), floatArrayOf(0f, 1f, 0f, 1f))
        positionQuad(red)
        red.use()
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_FAN, 0, 4)
        assertNoGlError("first block")
        assertPixel(1, 1, 255, 0, 0)
        positionQuad(green)
        green.use()
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_FAN, 0, 4)
        assertPixel(1, 1, 0, 255, 0)
    }

    @Test
    fun twoProgramsOnOneContextDoNotShareATextureUnit() = canvas {
        fun imageProgram(slot: (Uniform<Sampler2D>) -> Unit): ShaderProgram = shader(ShaderTarget.Gles30) {
            val image = sampler2D("image")
            slot(image)
            vertex {
                val position = attributeVec2("position")
                glPosition(vec4(position.x, position.y, 0f.lit, 1f.lit))
            }
            fragment { texture(image, vec2(0.5f.lit, 0.5f.lit)) }
        }
        lateinit var firstImage: Uniform<Sampler2D>
        lateinit var secondImage: Uniform<Sampler2D>
        val units = GlTextureUnits()
        val first = link(imageProgram { firstImage = it }, units)
        val second = link(imageProgram { secondImage = it }, units)
        target(first)
        val red = first.uploadRgba(1, 1, byteArrayOf(-1, 0, 0, -1))
        val blue = second.uploadRgba(1, 1, byteArrayOf(0, 0, -1, -1))
        first.bind(firstImage, red)
        second.bind(secondImage, blue)
        positionQuad(first)
        first.use()
        first.bind(firstImage, red)
        GLES30.glDrawArrays(GLES30.GL_TRIANGLE_FAN, 0, 4)
        assertNoGlError("shared units")
        assertPixel(1, 1, 255, 0, 0)
    }
}

private class Canvas {
    val vao: Int
    private val runtimes = mutableListOf<GlProgramRuntime>()
    private val targets = mutableListOf<Pair<GlProgramRuntime, GlColorTarget>>()

    init {
        val names = IntArray(1)
        GLES30.glGenVertexArrays(1, names, 0)
        vao = names[0]
        GLES30.glBindVertexArray(vao)
    }

    fun link(program: ShaderProgram, units: GlTextureUnits = GlTextureUnits()): GlProgramRuntime {
        val runtime = GlProgramRuntime(program, Gles30Device(), textureUnits = units)
        runtime.link()
        runtimes += runtime
        return runtime
    }

    fun target(runtime: GlProgramRuntime) {
        val target = runtime.createColorTarget(SIZE, SIZE)
        targets += runtime to target
        runtime.bindFramebuffer(target.framebuffer)
        GLES30.glViewport(0, 0, SIZE, SIZE)
        GLES30.glDisable(GLES30.GL_DEPTH_TEST)
        clear()
    }

    fun clear() {
        GLES30.glClearColor(0f, 0f, 0f, 1f)
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)
    }

    fun attribute(runtime: GlProgramRuntime, buffer: Int, name: String, size: Int, stride: Int, offset: Int) {
        GLES30.glBindVertexArray(vao)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, buffer)
        val location = runtime.attribLocation("a_$name")
        check(location >= 0) { "a_$name is not active" }
        runtime.vertexAttribFloat(location, size, stride, offset)
    }

    fun positionQuad(runtime: GlProgramRuntime) {
        val buffer = runtime.createBuffer()
        runtime.replaceArrayBuffer(buffer, 0, QUAD)
        attribute(runtime, buffer, "position", 2, 2, 0)
    }

    fun texturedQuad(runtime: GlProgramRuntime) {
        val buffer = runtime.createBuffer()
        runtime.replaceArrayBuffer(
            buffer,
            0,
            floatArrayOf(-1f, -1f, 0f, 0f, 1f, -1f, 1f, 0f, 1f, 1f, 1f, 1f, -1f, 1f, 0f, 1f),
        )
        attribute(runtime, buffer, "position", 2, 4, 0)
        attribute(runtime, buffer, "uv", 2, 4, 2)
    }

    fun assertPixel(x: Int, y: Int, red: Int, green: Int, blue: Int) {
        val pixel = ByteBuffer.allocateDirect(4).order(ByteOrder.nativeOrder())
        GLES30.glReadPixels(x, y, 1, 1, GLES30.GL_RGBA, GLES30.GL_UNSIGNED_BYTE, pixel)
        val actual = intArrayOf(pixel.get(0).toInt() and 0xFF, pixel.get(1).toInt() and 0xFF, pixel.get(2).toInt() and 0xFF)
        val expected = intArrayOf(red, green, blue)
        for (channel in 0 until 3) {
            if (kotlin.math.abs(actual[channel] - expected[channel]) > 2) {
                throw AssertionError(
                    "Pixel ($x, $y) was ${actual.contentToString()}, expected ${expected.contentToString()}",
                )
            }
        }
    }

    fun assertNoGlError(where: String) {
        assertEquals(where, GLES30.GL_NO_ERROR, GLES30.glGetError())
    }

    fun close() {
        for ((runtime, target) in targets) runtime.deleteColorTarget(target)
        for (runtime in runtimes) runtime.destroy()
    }
}

private fun canvas(block: Canvas.() -> Unit) {
    EglPbuffer().use {
        GLES30.glGetError()
        val canvas = Canvas()
        try {
            canvas.block()
        } finally {
            canvas.close()
        }
    }
}
