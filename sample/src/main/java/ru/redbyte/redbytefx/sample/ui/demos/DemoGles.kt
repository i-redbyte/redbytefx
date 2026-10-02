package ru.redbyte.redbytefx.sample.ui.demos

import android.opengl.GLES30
import android.opengl.GLSurfaceView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import ru.redbyte.redbytefx.Flt
import ru.redbyte.redbytefx.High
import ru.redbyte.redbytefx.ShaderProgram
import ru.redbyte.redbytefx.ShaderTarget
import ru.redbyte.redbytefx.Uniform
import ru.redbyte.redbytefx.gl.Gles30Device
import ru.redbyte.redbytefx.gl.GlProgramRuntime
import ru.redbyte.redbytefx.lit
import ru.redbyte.redbytefx.plus
import ru.redbyte.redbytefx.shader
import ru.redbyte.redbytefx.sin
import ru.redbyte.redbytefx.times
import ru.redbyte.redbytefx.vec4
import ru.redbyte.redbytefx.x
import ru.redbyte.redbytefx.y
import ru.redbyte.redbytefx.sample.ui.gl.GlSlot
import ru.redbyte.redbytefx.sample.ui.gl.GlesView
import ru.redbyte.redbytefx.sample.ui.gl.attribLocation
import ru.redbyte.redbytefx.sample.ui.gl.deleteBuffer
import ru.redbyte.redbytefx.sample.ui.gl.drawVec2
import ru.redbyte.redbytefx.sample.ui.gl.replaceVec2

private class GlesTriangle(
    val program: ShaderProgram,
    val time: Uniform<Flt<High>>,
)

private fun glesTriangle(): GlesTriangle {
    lateinit var time: Uniform<Flt<High>>
    val program = shader(ShaderTarget.Gles30) {
        time = uniformTime()
        vertex {
            val position = attributeVec2("position")
            glPosition(vec4(position.x, position.y, 0f.lit, 1f.lit))
        }
        fragment {
            val wave = sin(time.expr)
            vec4(0.5f.lit + wave * 0.5f.lit, 0.15f.lit, 0.85f.lit, 1f.lit)
        }
    }
    return GlesTriangle(program, time)
}

@Composable
fun DemoGles() {
    val triangle = remember { glesTriangle() }
    GlesView { slot -> TriangleRenderer(triangle, slot) }
}

private class TriangleRenderer(
    private val triangle: GlesTriangle,
    private val slot: GlSlot,
) : GLSurfaceView.Renderer {
    private val vertices = floatArrayOf(-0.6f, -0.5f, 0.6f, -0.5f, 0f, 0.7f)
    private var buffer = 0
    private var attrib = -1
    private var startedNanos = 0L

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        slot.runtime?.destroy()
        val runtime = GlProgramRuntime(triangle.program, Gles30Device())
        runtime.link()
        slot.runtime = runtime
        runtime.use()
        attrib = attribLocation("a_position")
        buffer = replaceVec2(buffer, vertices)
        val uploaded = buffer
        slot.releaseGl = {
            if (buffer == uploaded) {
                deleteBuffer(buffer)
                buffer = 0
            }
        }
        GLES30.glClearColor(0.05f, 0.06f, 0.09f, 1f)
        startedNanos = System.nanoTime()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES30.glViewport(0, 0, width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        val runtime = slot.runtime ?: return
        val seconds = (System.nanoTime() - startedNanos) / 1_000_000_000f
        runtime.set(triangle.time, seconds)
        runtime.use()
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)
        drawVec2(buffer, attrib, 3)
    }
}
