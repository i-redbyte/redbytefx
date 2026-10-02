package ru.redbyte.redbytefx.sample.ui.demos

import android.opengl.GLES30
import android.opengl.GLSurfaceView
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import java.nio.ByteBuffer
import java.nio.ByteOrder
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
import ru.redbyte.redbytefx.sample.ui.DemoLayout

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
    val sources = remember(triangle) {
        triangle.program.vertexSource() + "\n" + triangle.program.fragmentSource()
    }
    DemoLayout(
        generatedAgsl = sources,
        preview = {
            AndroidView(
                modifier = Modifier.fillMaxWidth().height(280.dp),
                factory = { context ->
                    val holder = arrayOfNulls<GlProgramRuntime>(1)
                    GLSurfaceView(context).apply {
                        setEGLContextClientVersion(3)
                        setRenderer(TriangleRenderer(triangle, holder))
                        tag = holder
                    }
                },
                onRelease = { view ->
                    @Suppress("UNCHECKED_CAST")
                    val holder = view.tag as Array<GlProgramRuntime?>
                    view.queueEvent {
                        holder[0]?.destroy()
                        holder[0] = null
                    }
                },
            )
        },
        controls = {},
    )
}

private class TriangleRenderer(
    private val triangle: GlesTriangle,
    private val holder: Array<GlProgramRuntime?>,
) : GLSurfaceView.Renderer {
    private val buffer = IntArray(1)
    private var attrib = -1
    private var startedNanos = 0L

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        val runtime = GlProgramRuntime(triangle.program, Gles30Device())
        runtime.link()
        holder[0] = runtime
        runtime.use()
        val programSlot = IntArray(1)
        GLES30.glGetIntegerv(GLES30.GL_CURRENT_PROGRAM, programSlot, 0)
        val program = programSlot[0]
        attrib = GLES30.glGetAttribLocation(program, "a_position")
        GLES30.glGenBuffers(1, buffer, 0)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, buffer[0])
        val vertices = floatArrayOf(-0.6f, -0.5f, 0.6f, -0.5f, 0f, 0.7f)
        val data = ByteBuffer.allocateDirect(vertices.size * Float.SIZE_BYTES)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
        data.put(vertices).position(0)
        GLES30.glBufferData(GLES30.GL_ARRAY_BUFFER, vertices.size * Float.SIZE_BYTES, data, GLES30.GL_STATIC_DRAW)
        GLES30.glClearColor(0.05f, 0.06f, 0.09f, 1f)
        startedNanos = System.nanoTime()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES30.glViewport(0, 0, width, height)
    }

    override fun onDrawFrame(gl: GL10?) {
        val runtime = holder[0] ?: return
        val seconds = (System.nanoTime() - startedNanos) / 1_000_000_000f
        runtime.set(triangle.time, seconds)
        runtime.use()
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, buffer[0])
        GLES30.glEnableVertexAttribArray(attrib)
        GLES30.glVertexAttribPointer(attrib, 2, GLES30.GL_FLOAT, false, 8, 0)
        GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, 3)
    }
}
