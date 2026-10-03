package ru.redbyte.redbytefx.sample.ui.gl

import android.opengl.GLES30
import android.opengl.GLES32
import android.opengl.GLSurfaceView
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.util.concurrent.atomic.AtomicInteger
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import ru.redbyte.redbytefx.ShaderProgram
import ru.redbyte.redbytefx.sample.ui.say
import ru.redbyte.redbytefx.gl.GlProgramRuntime

internal class PointerState {
    @Volatile var x: Float = 0f
    @Volatile var y: Float = 0.2f
    private val ticks = AtomicInteger(0)

    fun mark(x: Float, y: Float) {
        this.x = x
        this.y = y
        ticks.incrementAndGet()
    }

    fun serial(): Int = ticks.get()
}

internal class GlFrame(
    val runtime: GlProgramRuntime,
    val seconds: Float,
    val aspect: Float,
    val replace: (FloatArray) -> Unit,
)

@Composable
internal fun GlCanvas(
    program: ShaderProgram,
    mesh: GlMesh,
    requirement: String? = null,
    pointer: PointerState? = null,
    caption: String? = null,
    dsl: String? = null,
    onFrame: (GlFrame) -> Unit,
) {
    var failure by remember { mutableStateOf<String?>(null) }
    Box(modifier = Modifier.fillMaxSize()) {
        GlesView(depth = mesh.depth) { slot ->
            SceneRenderer(
                program = program,
                mesh = mesh,
                requirement = requirement,
                slot = slot,
                onFrame = onFrame,
                onFailure = { message -> slot.post { failure = message } },
            )
        }
        if (pointer != null) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(pointer) {
                        awaitEachGesture {
                            while (true) {
                                val event = awaitPointerEvent()
                                val change = event.changes.firstOrNull { it.pressed } ?: break
                                val width = size.width.coerceAtLeast(1)
                                val height = size.height.coerceAtLeast(1)
                                pointer.mark(
                                    change.position.x / width * 2f - 1f,
                                    1f - change.position.y / height * 2f,
                                )
                                change.consume()
                            }
                        }
                    },
            )
        }
        if (failure != null) {
            Text(
                text = if (failure == LINK_FALLBACK) {
                    say(LINK_FALLBACK, "Это устройство не может собрать шейдер.")
                } else {
                    failure ?: ""
                },
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .align(Alignment.Center)
                    .padding(24.dp),
            )
        }
        if (caption != null && failure == null) {
            Text(
                text = caption,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .padding(bottom = 28.dp),
            )
        }
        if (dsl != null) {
            GlCodeCompare(
                program = program,
                dsl = dsl,
                modifier = Modifier.align(Alignment.TopEnd),
            )
        }
    }
}

private class SceneRenderer(
    private val program: ShaderProgram,
    private val mesh: GlMesh,
    private val requirement: String?,
    private val slot: GlSlot,
    private val onFrame: (GlFrame) -> Unit,
    private val onFailure: (String) -> Unit,
) : GLSurfaceView.Renderer {
    private var buffer = 0
    private var vertexCount = 0
    private var locations = IntArray(0)
    private var scratch: FloatBuffer? = null
    private var aspect = 1f
    private var startedNanos = 0L
    private var reported = false

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        reported = false
        val runtime = slot.linkGraphics(program, requirement) { message ->
            if (!reported) {
                reported = true
                onFailure(message)
            }
        } ?: return
        slot.runtime = runtime
        runtime.use()
        locations = IntArray(mesh.attribs.size) { index -> attribLocation(mesh.attribs[index].name) }
        buffer = upload(mesh.vertices)
        vertexCount = mesh.vertices.size / mesh.stride
        val uploaded = buffer
        slot.releaseGl = {
            if (buffer == uploaded) {
                deleteBuffer(buffer)
                buffer = 0
            }
        }
        GLES30.glClearColor(mesh.clearR, mesh.clearG, mesh.clearB, 1f)
        startedNanos = System.nanoTime()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES30.glViewport(0, 0, width, height)
        aspect = if (height > 0) width.toFloat() / height.toFloat() else 1f
    }

    override fun onDrawFrame(gl: GL10?) {
        val runtime = slot.runtime ?: return
        val seconds = (System.nanoTime() - startedNanos) / 1_000_000_000f
        runtime.use()
        onFrame(GlFrame(runtime, seconds, aspect) { vertices -> replace(vertices) })
        if (mesh.depth) {
            GLES30.glEnable(GLES30.GL_DEPTH_TEST)
            GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)
        } else {
            GLES30.glDisable(GLES30.GL_DEPTH_TEST)
            GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)
        }
        if (mesh.patchVertices > 0) {
            GLES32.glPatchParameteri(GLES32.GL_PATCH_VERTICES, mesh.patchVertices)
        }
        draw(buffer, vertexCount)
    }

    private fun replace(vertices: FloatArray) {
        if (buffer == 0) return
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, buffer)
        val data = direct(vertices.size)
        data.put(vertices).position(0)
        GLES30.glBufferData(
            GLES30.GL_ARRAY_BUFFER,
            vertices.size * Float.SIZE_BYTES,
            data,
            GLES30.GL_DYNAMIC_DRAW,
        )
        vertexCount = vertices.size / mesh.stride
    }

    private fun upload(vertices: FloatArray): Int {
        val ids = IntArray(1)
        GLES30.glGenBuffers(1, ids, 0)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, ids[0])
        val data = direct(vertices.size)
        data.put(vertices).position(0)
        GLES30.glBufferData(
            GLES30.GL_ARRAY_BUFFER,
            vertices.size * Float.SIZE_BYTES,
            data,
            GLES30.GL_DYNAMIC_DRAW,
        )
        return ids[0]
    }

    private fun direct(count: Int): FloatBuffer {
        val existing = scratch
        if (existing != null && existing.capacity() >= count) {
            existing.clear()
            return existing
        }
        val created = ByteBuffer.allocateDirect(count * Float.SIZE_BYTES)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
        scratch = created
        return created
    }

    private fun draw(buffer: Int, count: Int) {
        if (buffer == 0 || count <= 0) return
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, buffer)
        val strideBytes = mesh.stride * Float.SIZE_BYTES
        mesh.attribs.forEachIndexed { index, attrib ->
            val location = locations.getOrNull(index) ?: -1
            if (location < 0) return@forEachIndexed
            GLES30.glEnableVertexAttribArray(location)
            GLES30.glVertexAttribPointer(
                location,
                attrib.size,
                GLES30.GL_FLOAT,
                false,
                strideBytes,
                attrib.offset * Float.SIZE_BYTES,
            )
        }
        GLES30.glDrawArrays(mesh.mode, 0, count)
    }
}

