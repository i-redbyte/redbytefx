package ru.redbyte.redbytefx.gl.compose

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver

/** A rendering surface that follows its host's resumed state. */
internal interface PausableSurface {
    fun onResume()

    fun onPause()
}

/**
 * Keeps [surface] paused unless [lifecycle] is resumed, starting with its current state: a host
 * that is not resumed yet pauses it at once, and a resumed host resumes it through the event the
 * registry replays to a new observer. Returns a function that stops observing.
 */
internal fun observeSurfaceLifecycle(lifecycle: Lifecycle, surface: PausableSurface): () -> Unit {
    if (!lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) surface.onPause()
    val observer = LifecycleEventObserver { _, event ->
        when (event) {
            Lifecycle.Event.ON_RESUME -> surface.onResume()
            Lifecycle.Event.ON_PAUSE -> surface.onPause()
            else -> Unit
        }
    }
    lifecycle.addObserver(observer)
    return { lifecycle.removeObserver(observer) }
}
