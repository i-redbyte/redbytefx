package ru.redbyte.redbytefx.gl.compose

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import org.junit.Assert.assertEquals
import org.junit.Test

class SurfaceLifecycleTest {
    private class RecordingSurface : PausableSurface {
        val calls = mutableListOf<String>()
        override fun onResume() {
            calls += "resume"
        }

        override fun onPause() {
            calls += "pause"
        }
    }

    private class Owner : LifecycleOwner {
        val registry = LifecycleRegistry.createUnsafe(this)
        override val lifecycle: Lifecycle get() = registry
    }

    @Test
    fun aSurfaceCreatedBeforeResumeStartsPaused() {
        val owner = Owner().apply { registry.currentState = Lifecycle.State.STARTED }
        val surface = RecordingSurface()
        observeSurfaceLifecycle(owner.lifecycle, surface)
        assertEquals(listOf("pause"), surface.calls)

        owner.registry.currentState = Lifecycle.State.RESUMED
        assertEquals(listOf("pause", "resume"), surface.calls)
    }

    @Test
    fun aResumedHostResumesImmediatelyAndPausesWithTheHost() {
        val owner = Owner().apply { registry.currentState = Lifecycle.State.RESUMED }
        val surface = RecordingSurface()
        val stop = observeSurfaceLifecycle(owner.lifecycle, surface)
        owner.registry.currentState = Lifecycle.State.STARTED
        stop()
        owner.registry.currentState = Lifecycle.State.RESUMED

        assertEquals(listOf("resume", "pause"), surface.calls)
    }
}
