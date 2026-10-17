package ru.redbyte.redbytefx.gl.compose

import android.graphics.PixelFormat
import android.opengl.GLES30
import android.opengl.GLSurfaceView
import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.requiredSizeIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import ru.redbyte.redbytefx.ShaderProgram
import ru.redbyte.redbytefx.ShaderTarget
import ru.redbyte.redbytefx.gl.GlException
import ru.redbyte.redbytefx.gl.GlProgramRuntime
import ru.redbyte.redbytefx.gl.GlTextureUnits
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

/** Shown when [GlCompute] is given a program that is not GLES 3.1 compute. */
private const val COMPUTE_LOG_TAG = "RedByteFX"

internal const val COMPUTE_HOST_TARGET: String = "GlCompute requires a GLES 3.1 compute program"

/**
 * Scene. Hosts a GLES 3.1 compute program. There is no mesh and the frame does not rasterize.
 *
 * Link runs on the GL thread. [GlController.dispatch] and [GlController.read] are queued to that
 * thread; this view does not draw and stays transparent. It still needs a window surface, so it is
 * laid out at least one dp wide and tall even inside a zero-size [modifier].
 */
@Composable
public fun GlCompute(
    controller: GlController,
    modifier: Modifier = Modifier,
    requirement: String? = controller.program.glLinkRequirementHint(),
    failureOverlay: @Composable (Throwable) -> Unit = DefaultFailureOverlay,
    overlay: @Composable (GlLinkState) -> Unit = DefaultLinkOverlay,
) {
    val linkState by controller.linkState
    val failure by controller.failure
    var glSurfaceView by remember { mutableStateOf<GLSurfaceView?>(null) }
    Box(modifier = modifier) {
        key(controller) {
            AndroidView(
                // GLSurfaceView creates no EGL context at 0x0, so a compute host is never smaller
                // than one dp; it is transparent, so that dp is never visible.
                modifier = Modifier.fillMaxSize().requiredSizeIn(minWidth = 1.dp, minHeight = 1.dp),
                factory = { context ->
                    val slot = GlSlot()
                    val surfaceView = GLSurfaceView(context).apply {
                        configureEs3(eglClientMinor(controller.program), depthBits = 0, slot = slot)
                        holder.setFormat(PixelFormat.TRANSLUCENT)
                        slot.queue = { block ->
                            queueEvent(block)
                            requestRender()
                        }
                        setRenderer(ComputeRenderer(controller, requirement, slot))
                        renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY
                        requestRender()
                    }
                    controller.attachQueue(slot.queue, slot.post)
                    glSurfaceView = surfaceView
                    surfaceView
                },
                onRelease = { view ->
                    if (glSurfaceView === view) glSurfaceView = null
                    releaseGlView(view, controller)
                },
            )
        }
        GlViewLifecycle(glSurfaceView, requestFrame = true)
        overlay(linkState)
        failure?.let { failureOverlay(it) }
    }
}

internal class ComputeRenderer(
    private val controller: GlController,
    private val requirement: String?,
    private val slot: GlSlot,
    private val clearFrame: () -> Unit = ::clearTransparent,
    private val link: (ShaderProgram, GlTextureUnits) -> GlProgramRuntime = { program, units ->
        linkProgram(
            program,
            controller.config.strictUniformLocations,
            units,
            controller.config.strictErrors,
        )
    },
) : GLSurfaceView.Renderer {
    private var reported = false
    private val guard = FrameGuard { error ->
        Log.e(COMPUTE_LOG_TAG, "GlCompute failed to link", error)
        if (error is GlException && error.code in LINK_CODES) fail(error) else controller.reportFailure(error)
    }

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        slot.runtime?.let { controller.detachRuntime(it) }
        slot.runtime = null
        slot.eglContextHandle = 0L
        reported = false
        publish(GlLinkState.Pending)
        if (controller.program.target != ShaderTarget.Gles31) {
            publish(GlLinkState.Failed(COMPUTE_HOST_TARGET))
            return
        }
        guard.resume()
        var linked: GlProgramRuntime? = null
        guard.run { linked = link(controller.program, GlTextureUnits()) }
        val runtime = linked ?: return
        slot.runtime = runtime
        slot.captureContext()
        publish(GlLinkState.Linked)
        controller.attachRuntime(slot.queue, runtime)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) = Unit

    override fun onDrawFrame(gl: GL10?) = clearFrame()

    private fun fail(error: GlException) {
        if (reported) return
        reported = true
        publish(GlLinkState.Failed(linkMessage(requirement, error)))
    }

    private fun publish(state: GlLinkState) {
        slot.post { if (controller.ownsQueue(slot.queue)) controller.linkStateValue = state }
    }
}

/** A compute host shows nothing: each requested frame is cleared to transparent. */
private fun clearTransparent() {
    GLES30.glClearColor(0f, 0f, 0f, 0f)
    GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)
}

/** Binary-compatible 1.1.1 entry point; Kotlin callers resolve to the overload with `failureOverlay`. */
@Deprecated("Kept for binaries compiled against 1.1.1", level = DeprecationLevel.HIDDEN)
@Composable
public fun GlCompute(
    controller: GlController,
    modifier: Modifier = Modifier,
    requirement: String? = controller.program.glLinkRequirementHint(),
    overlay: @Composable (GlLinkState) -> Unit = DefaultLinkOverlay,
) {
    GlCompute(
        controller = controller,
        modifier = modifier,
        requirement = requirement,
        failureOverlay = DefaultFailureOverlay,
        overlay = overlay,
    )
}
