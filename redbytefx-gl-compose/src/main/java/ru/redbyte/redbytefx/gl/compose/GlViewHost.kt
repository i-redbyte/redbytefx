package ru.redbyte.redbytefx.gl.compose

import android.opengl.GLSurfaceView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.lifecycle.compose.LocalLifecycleOwner

/**
 * Configures a GL view shared by [GlSurface] and [GlCompute]: an ES 3.[minor] context that survives
 * pauses, so returning from the background does not relink programs or re-upload meshes.
 */
internal fun GLSurfaceView.configureEs3(minor: Int, depthBits: Int, slot: GlSlot) {
    setEGLContextClientVersion(3)
    setEGLContextFactory(Es3ContextFactory(minor))
    setEGLConfigChooser(8, 8, 8, 8, depthBits, 0)
    preserveEGLContextOnPause = true
    slot.post = { block -> post { block() } }
    tag = slot
}

/** Releases the GL objects of [view] on its GL thread and stops it from drawing. */
internal fun releaseGlView(view: GLSurfaceView, controller: GlController) {
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
}

/** Pauses and resumes [view] with the host lifecycle; [requestFrame] asks a when-dirty view to run. */
@Composable
internal fun GlViewLifecycle(view: GLSurfaceView?, requestFrame: Boolean) {
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner, view) {
        if (view == null) return@DisposableEffect onDispose {}
        val stop = observeSurfaceLifecycle(
            owner.lifecycle,
            object : PausableSurface {
                override fun onResume() {
                    view.onResume()
                    if (requestFrame) view.requestRender()
                }

                override fun onPause() {
                    view.onPause()
                }
            },
        )
        onDispose { stop() }
    }
}
