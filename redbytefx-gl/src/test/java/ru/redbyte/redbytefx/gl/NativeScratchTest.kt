package ru.redbyte.redbytefx.gl

import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class NativeScratchTest {
    @Test
    fun growingRequestsReuseOneBufferUntilCapacityDoubles() {
        val scratch = NativeScratch()
        assertTrue(scratch.acquire(1_000).isDirect)
        val grown = scratch.acquire(1_001)
        assertTrue(grown.capacity() >= 2_000)
        for (bytes in 1_001..2_000) assertSame(grown, scratch.acquire(bytes))
    }

    @Test
    fun anAcquiredBufferIsClearedAndLimitedToTheRequest() {
        val scratch = NativeScratch()
        scratch.acquire(64).position(10)
        val buffer = scratch.acquire(32)
        assertEquals(0, buffer.position())
        assertEquals(32, buffer.limit())
    }

    @Test
    fun releaseDropsTheBuffer() {
        val scratch = NativeScratch()
        val before = scratch.acquire(16)
        scratch.release()
        assertTrue(before !== scratch.acquire(16))
    }
}
