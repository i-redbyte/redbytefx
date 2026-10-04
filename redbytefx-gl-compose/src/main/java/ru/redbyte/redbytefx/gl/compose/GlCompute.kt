package ru.redbyte.redbytefx.gl.compose

import android.opengl.GLSurfaceView
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
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import ru.redbyte.redbytefx.ShaderProgram
import ru.redbyte.redbytefx.ShaderTarget
import ru.redbyte.redbytefx.gl.GlException
import ru.redbyte.redbytefx.gl.GlProgramRuntime
import ru.redbyte.redbytefx.gl.GlTextureUnits

/** Shown when [GlCompute] is given a program that is not GLES 3.1 compute. */
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
                        setEGLConfigChooser(8, 8, 8, 8, 0, 0)
                        slot.post = { block -> post { block() } }
                        slot.queue = { block ->
                            queueEvent(block)
                            requestRender()
                        }
                        tag = slot
                        setRenderer(ComputeRenderer(controller, requirement, slot))
                        renderMode = GLSurfaceView.RENDERMODE_WHEN_DIRTY
                        requestRender()
                    }
                    controller.attachQueue(slot.queue)
                    glSurfaceView = surfaceView
                    surfaceView
                },
                onRelease = { view ->
                    if (glSurfaceView === view) glSurfaceView = null
                    val held = view.tag as GlSlot
                    if (controller.detachQueue(held.queue)) controller.linkStateValue = GlLinkState.Pending
                    view.queueEvent {
                        held.releaseGl?.invoke()
                        held.releaseGl = null
                        held.runtime?.let { linked ->
                            controller.detachRuntime(linked)
                            linked.destroy()
                        }
                        held.runtime = null
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
                    Lifecycle.Event.ON_RESUME -> {
                        surface.onResume()
                        surface.requestRender()
                    }
                    Lifecycle.Event.ON_PAUSE -> surface.onPause()
                    else -> Unit
                }
            }
            lifecycle.addObserver(observer)
            if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) {
                surface.onResume()
                surface.requestRender()
            }
            onDispose {
                lifecycle.removeObserver(observer)
            }
        }
        overlay(linkState)
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

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        slot.runtime?.let { controller.detachRuntime(it) }
        slot.runtime = null
        reported = false
        publish(GlLinkState.Pending)
        if (controller.program.target != ShaderTarget.Gles31) {
            publish(GlLinkState.Failed(COMPUTE_HOST_TARGET))
            return
        }
        val runtime = try {
            link(controller.program, GlTextureUnits())
        } catch (error: GlException) {
            fail(error)
            return
        }
        slot.runtime = runtime
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
