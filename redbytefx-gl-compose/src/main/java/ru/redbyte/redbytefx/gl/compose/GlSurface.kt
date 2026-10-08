package ru.redbyte.redbytefx.gl.compose

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
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import ru.redbyte.redbytefx.ShaderProgram

/**
 * Embeds a [GLSurfaceView] that links and draws [controller.program].
 *
 * Link progress is exposed as [GlController.linkState]; the default [overlay] shows [GlLinkErrorOverlay]
 * when linking fails. [onFrame] runs on the GL thread before each draw — use [GlFrame.seconds] for time
 * uniforms and [GlFrame.runtime] for direct [ru.redbyte.redbytefx.gl.GlProgramRuntime] calls.
 *
 * Rendering follows the host [androidx.lifecycle.Lifecycle] (`onResume` / `onPause` on the surface).
 * The EGL context is OpenGL ES 3.x, with a minor version matching the newer of the surface
 * and present programs (3.2, 3.1, or 3.0).
 *
 * When [onFrame] records no draws, the mesh is drawn once. The first recorded draw suppresses that
 * automatic draw. A draw with no triangles or no instances is not recorded.
 * The window always has a depth buffer, so a recorded mesh can enable the depth test even when
 * [mesh] did not. [renderToTexture] allocates [GlFrame.colorTarget] before [onFrame] so the color
 * name is valid on this context. [present] is a second program linked on that same context, not a
 * second [GlController]. Its uniforms are set on [GlFrame.presentRuntime].
 *
 * A new [mesh], [present], or [renderToTexture] recreates the GL view. [onFrame] may change
 * between compositions; the GL thread calls the latest one. A mesh passed to [GlFrame.draw] keeps
 * its buffers while it is drawn; after [HELD_MESH_FRAMES] frames without a draw they are deleted.
 */
@Composable
public fun GlSurface(
    controller: GlController,
    mesh: GlMesh,
    modifier: Modifier = Modifier,
    requirement: String? = controller.program.glLinkRequirementHint(),
    present: ShaderProgram? = null,
    renderToTexture: Boolean = false,
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
    val currentOnFrame by rememberUpdatedState(onFrame)
    Box(modifier = modifier) {
        key(controller, mesh, present, renderToTexture) {
            AndroidView(
                modifier = Modifier.fillMaxSize(),
                factory = { context ->
                    val slot = GlSlot()
                    val surfaceView = GLSurfaceView(context).apply {
                        setEGLContextClientVersion(3)
                        setEGLContextFactory(Es3ContextFactory(eglClientMinor(controller.program, present)))
                        setEGLConfigChooser(8, 8, 8, 8, 16, 0)
                        slot.post = { block -> post { block() } }
                        slot.queue = { block -> queueEvent(block) }
                        tag = slot
                        setRenderer(
                            SceneRenderer(
                                controller = controller,
                                mesh = mesh,
                                requirement = requirement,
                                slot = slot,
                                present = present,
                                renderToTexture = renderToTexture,
                                onFrame = { frame -> currentOnFrame(frame) },
                            ),
                        )
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
                        val contextAlive = held.ownsCurrentContext()
                        held.releaseGl?.invoke(contextAlive)
                        held.releaseGl = null
                        held.runtime?.let { linked ->
                            controller.detachRuntime(linked)
                            if (contextAlive) linked.destroy()
                        }
                        held.runtime = null
                        held.eglContextHandle = 0L
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
 * Maps pointer position to normalized device coordinates (-1..1) and invokes [onPointer] on the UI thread.
 */
@Composable
public fun Modifier.glPointerInput(onPointer: (x: Float, y: Float) -> Unit): Modifier {
    val currentOnPointer by rememberUpdatedState(onPointer)
    return pointerInput(Unit) {
        awaitEachGesture {
            while (true) {
                val event = awaitPointerEvent()
                val change = event.changes.firstOrNull { it.pressed } ?: break
                val width = size.width.coerceAtLeast(1)
                val height = size.height.coerceAtLeast(1)
                val x = change.position.x / width * 2f - 1f
                val y = 1f - change.position.y / height * 2f
                currentOnPointer(x, y)
                change.consume()
            }
        }
    }
}
