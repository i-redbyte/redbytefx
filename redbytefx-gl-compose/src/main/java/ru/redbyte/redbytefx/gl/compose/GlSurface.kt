package ru.redbyte.redbytefx.gl.compose

import android.opengl.GLES30
import android.opengl.GLES32
import android.opengl.GLSurfaceView
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.viewinterop.AndroidView
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
/**
 * Embeds a [GLSurfaceView] that links and draws [controller.program].
 *
 * Link progress is exposed as [GlController.linkState]; the default [overlay] shows [GlLinkErrorOverlay]
 * when linking fails. [onFrame] runs on the GL thread before each draw — use [GlFrame.seconds] for time
 * uniforms and [GlFrame.runtime] for direct [ru.redbyte.redbytefx.gl.GlProgramRuntime] calls.
 *
 * Rendering follows the host [androidx.lifecycle.Lifecycle] (`onResume` / `onPause` on the surface).
 * The EGL context is OpenGL ES 3.x, with a minor version matching [controller]'s program (3.2, 3.1, or 3.0).
 */
@Composable
public fun GlSurface(
    controller: GlController,
    mesh: GlMesh,
    modifier: Modifier = Modifier,
    requirement: String? = controller.program.glLinkRequirementHint(),
    onFrame: (GlFrame) -> Unit,
    overlay: @Composable (GlLinkState) -> Unit = { state ->
        if (state is GlLinkState.Failed) {
            GlLinkErrorOverlay(state.message)
        }
    },
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val linkState by controller.linkState
    var glSurfaceView by remember { mutableStateOf<GLSurfaceView?>(null) }
    Box(modifier = modifier) {
        key(controller.program) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    val slot = GlSlot()
                    val surfaceView = GLSurfaceView(context).apply {
                        setEGLContextClientVersion(3)
                        setEGLContextFactory(Es3ContextFactory(eglClientMinor(controller.program)))
                        if (mesh.depth) {
                            setEGLConfigChooser(8, 8, 8, 8, 16, 0)
                        }
                        slot.post = { block -> post { block() } }
                        tag = slot
                        controller.glQueue = { block -> queueEvent(block) }
                        controller.scheduleDrain()
                        setRenderer(
                            SceneRenderer(
                                controller = controller,
                                mesh = mesh,
                                requirement = requirement,
                                slot = slot,
                                onFrame = onFrame,
                            ),
                        )
                    }
                    glSurfaceView = surfaceView
                    surfaceView
                },
                onRelease = { view ->
                    glSurfaceView = null
                    controller.linkStateValue = GlLinkState.Pending
                    val held = view.tag as? GlSlot
                    view.queueEvent {
                        held?.releaseGl?.invoke()
                        held?.releaseGl = null
                        held?.runtime?.destroy()
                        held?.runtime = null
                        controller.runtime = null
                        controller.glQueue = null
                    }
                    view.onPause()
                },
            )
        }
        DisposableEffect(lifecycleOwner, glSurfaceView) {
            val surface = glSurfaceView
            if (surface == null) {
                return@DisposableEffect onDispose {}
            }
            val lifecycle = lifecycleOwner.lifecycle
            val observer = LifecycleEventObserver { _, event ->
                when (event) {
                    Lifecycle.Event.ON_RESUME -> surface.onResume()
                    Lifecycle.Event.ON_PAUSE -> surface.onPause()
                    else -> Unit
                }
            }
            lifecycle.addObserver(observer)
            if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                surface.onResume()
            }
            onDispose {
                lifecycle.removeObserver(observer)
            }
        }
        overlay(linkState)
    }
}

/**
 * Maps pointer position to normalized device coordinates (-1..1) and invokes [onPointer] on the GL thread.
 */
@Composable
public fun Modifier.glPointerInput(onPointer: (x: Float, y: Float) -> Unit): Modifier =
    pointerInput(Unit) {
        awaitEachGesture {
            while (true) {
                val event = awaitPointerEvent()
                val change = event.changes.firstOrNull { it.pressed } ?: break
                val width = size.width.coerceAtLeast(1)
                val height = size.height.coerceAtLeast(1)
                val x = change.position.x / width * 2f - 1f
                val y = 1f - change.position.y / height * 2f
                onPointer(x, y)
                change.consume()
            }
        }
    }

private class SceneRenderer(
    private val controller: GlController,
    private val mesh: GlMesh,
    private val requirement: String?,
    private val slot: GlSlot,
    private val onFrame: (GlFrame) -> Unit,
) : GLSurfaceView.Renderer {
    private var buffer = 0
    private var array = 0
    private var vertexCount = 0
    private var locations = IntArray(0)
    private var scratch: FloatBuffer? = null
    private var aspect = 1f
    private var startedNanos = 0L
    private var reported = false

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        reported = false
        publish(GlLinkState.Pending)
        val runtime = slot.linkGraphics(
            program = controller.program,
            strictUniformLocations = controller.config.strictUniformLocations,
            requirement = requirement,
            onFailure = { message ->
                if (!reported) {
                    reported = true
                    publish(GlLinkState.Failed(message))
                }
            },
        )
        if (runtime == null) {
            controller.runtime = null
            return
        }
        slot.runtime = runtime
        controller.runtime = runtime
        publish(GlLinkState.Linked)
        controller.drainPending()
        runtime.use()
        locations = IntArray(mesh.attribs.size) { index ->
            GlBuffer.attribLocation(mesh.attribs[index].name)
        }
        buffer = upload(mesh.vertices)
        array = createVertexArray()
        vertexCount = mesh.vertices.size / mesh.stride
        val uploaded = buffer
        val createdArray = array
        slot.releaseGl = {
            if (buffer == uploaded) {
                GlBuffer.deleteBuffer(buffer)
                buffer = 0
            }
            if (array == createdArray && array != 0) {
                GLES30.glDeleteVertexArrays(1, intArrayOf(array), 0)
                array = 0
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
        onFrame(
            GlFrame(
                runtime = runtime,
                seconds = seconds,
                aspect = aspect,
                upload = { vertices -> replace(vertices) },
            ),
        )
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
        val data = direct(vertices.size)
        buffer = GlBuffer.uploadDynamic(buffer, vertices, data)
        vertexCount = vertices.size / mesh.stride
    }

    private fun publish(state: GlLinkState) {
        slot.post { controller.linkStateValue = state }
    }

    private fun createVertexArray(): Int {
        val ids = IntArray(1)
        GLES30.glGenVertexArrays(1, ids, 0)
        GLES30.glBindVertexArray(ids[0])
        return ids[0]
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
        if (array != 0) {
            GLES30.glBindVertexArray(array)
        }
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
