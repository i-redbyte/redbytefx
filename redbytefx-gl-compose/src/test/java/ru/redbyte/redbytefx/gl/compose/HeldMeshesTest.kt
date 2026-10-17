package ru.redbyte.redbytefx.gl.compose

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class HeldMeshesTest {
    private class Held(override var drawnFrame: Long) : FrameStamped

    @Test
    fun meshesUndrawnForTheIdleWindowAreEvictedAndOthersStay() {
        val held = HeldMeshes<Held>()
        val old = screenMesh()
        val fresh = screenMesh()
        val oldHeld = Held(drawnFrame = 0)
        val freshHeld = Held(drawnFrame = 50)
        held.put(old, oldHeld)
        held.put(fresh, freshHeld)
        val evicted = ArrayList<Held>()

        held.evictIdle(frame = HELD_MESH_FRAMES, into = evicted)

        assertEquals(listOf(oldHeld), evicted)
        assertNull(held[old])
        assertSame(freshHeld, held[fresh])
    }

    @Test
    fun aFrameWithNothingToEvictAllocatesNothing() {
        val held = HeldMeshes<Held>()
        val evicted = ArrayList<Held>(4)
        val empty = allocatedBytes { repeat(1_000) { held.evictIdle(frame = it.toLong(), into = evicted) } }
        held.put(screenMesh(), Held(drawnFrame = Long.MAX_VALUE / 2))
        val busy = allocatedBytes { repeat(1_000) { held.evictIdle(frame = it.toLong(), into = evicted) } }

        assertTrue("empty frames allocated $empty bytes", empty < 1_000)
        assertTrue("busy frames allocated $busy bytes", busy < 1_000)
    }
}
