package ru.redbyte.redbytefx.gl.compose

import android.opengl.GLSurfaceView
import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
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
 * thread; this view does not draw. A graphics [GlSurface] is unchanged.
 */
@Composable
public fun GlCompute(
    controller: GlController,
    modifier: Modifier = Modifier,
    requirement: String? = controller.program.glLinkRequirementHint(),
    failureOverlay: @Composable (Throwable) -> Unit = { error -> GlLinkErrorOverlay(glFailureMessage(error)) },
    overlay: @Composable (GlLinkState) -> Unit = { state ->
        if (state is GlLinkState.Failed) {
            GlLinkErrorOverlay(state.message)
        }
    },
) {
    val linkState by controller.linkState
    val failure by controller.failure
    var glSurfaceView by remember { mutableStateOf<GLSurfaceView?>(null) }
    Box(modifier = modifier) {
        key(controller) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    val slot = GlSlot()
                    val surfaceView = GLSurfaceView(context).apply {
                        configureEs3(eglClientMinor(controller.program), depthBits = 0, slot = slot)
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

    override fun onDrawFrame(gl: GL10?) = Unit

    private fun fail(error: GlException) {
        if (reported) return
        reported = true
        publish(GlLinkState.Failed(linkMessage(requirement, error)))
    }

    private fun publish(state: GlLinkState) {
        slot.post { if (controller.ownsQueue(slot.queue)) controller.linkStateValue = state }
    }
}
