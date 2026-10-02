package ru.redbyte.redbytefx.sample.ui.gl

import android.opengl.GLES30
import android.opengl.GLSurfaceView
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import java.nio.ByteBuffer
import java.nio.ByteOrder
import ru.redbyte.redbytefx.gl.GlProgramRuntime

internal class GlSlot {
    var runtime: GlProgramRuntime? = null
    var releaseGl: (() -> Unit)? = null
}

@Composable
internal fun GlesView(
    modifier: Modifier = Modifier,
    renderer: (GlSlot) -> GLSurfaceView.Renderer,
) {
    AndroidView(
        modifier = modifier.fillMaxSize(),
        factory = { context ->
            val slot = GlSlot()
            GLSurfaceView(context).apply {
                setEGLContextClientVersion(3)
                setRenderer(renderer(slot))
                tag = slot
            }
        },
        onRelease = { view ->
            val slot = view.tag as GlSlot
            view.queueEvent {
                slot.releaseGl?.invoke()
                slot.runtime?.destroy()
                slot.runtime = null
            }
        },
    )
}

internal fun replaceVec2(current: Int, vertices: FloatArray): Int {
    deleteBuffer(current)
    return uploadVec2(vertices)
}

internal fun uploadVec2(vertices: FloatArray): Int {
    val ids = IntArray(1)
    GLES30.glGenBuffers(1, ids, 0)
    GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, ids[0])
    val data = ByteBuffer.allocateDirect(vertices.size * Float.SIZE_BYTES)
        .order(ByteOrder.nativeOrder())
        .asFloatBuffer()
    data.put(vertices).position(0)
    GLES30.glBufferData(
        GLES30.GL_ARRAY_BUFFER,
        vertices.size * Float.SIZE_BYTES,
        data,
        GLES30.GL_STATIC_DRAW,
    )
    return ids[0]
}

internal fun drawVec2(buffer: Int, attrib: Int, count: Int) {
    if (attrib < 0 || buffer == 0) return
    GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, buffer)
    GLES30.glEnableVertexAttribArray(attrib)
    GLES30.glVertexAttribPointer(attrib, 2, GLES30.GL_FLOAT, false, Float.SIZE_BYTES * 2, 0)
    GLES30.glDrawArrays(GLES30.GL_TRIANGLES, 0, count)
}

internal fun attribLocation(name: String): Int {
    val programSlot = IntArray(1)
    GLES30.glGetIntegerv(GLES30.GL_CURRENT_PROGRAM, programSlot, 0)
    return GLES30.glGetAttribLocation(programSlot[0], name)
}

internal fun deleteBuffer(id: Int) {
    if (id == 0) return
    GLES30.glDeleteBuffers(1, intArrayOf(id), 0)
}
