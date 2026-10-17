package ru.redbyte.redbytefx.gl.compose

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test

class FrameGuardTest {
    @Test
    fun aFailingFrameIsReportedOnceAndLaterFramesAreSkipped() {
        val reported = mutableListOf<Throwable>()
        val guard = FrameGuard { reported += it }
        val problem = IllegalStateException("onFrame failed")
        var frames = 0

        assertFalse(guard.run { frames += 1; throw problem })
        assertFalse(guard.run { frames += 1 })
        assertFalse(guard.run { frames += 1 })

        assertEquals(1, frames)
        assertEquals(1, reported.size)
        assertSame(problem, reported.single())
        assertTrue(guard.halted)
    }

    @Test
    fun aNewContextResumesRendering() {
        val guard = FrameGuard { }
        guard.run { throw IllegalArgumentException("bad range") }
        guard.resume()
        var ran = false

        assertTrue(guard.run { ran = true })
        assertTrue(ran)
        assertFalse(guard.halted)
    }

    @Test(expected = OutOfMemoryError::class)
    fun errorsAreNotSwallowed() {
        FrameGuard { }.run { throw OutOfMemoryError() }
    }
}
