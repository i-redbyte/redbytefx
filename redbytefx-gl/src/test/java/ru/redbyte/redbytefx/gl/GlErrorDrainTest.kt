package ru.redbyte.redbytefx.gl

import org.junit.Assert.assertEquals
import org.junit.Test

class GlErrorDrainTest {
    @Test
    fun theFirstErrorIsKeptAndTheRestAreDrained() {
        val pending = ArrayDeque(listOf(0x502, 0x500, 0))
        val seen = mutableListOf<Int>()

        val first = drainGlErrors({ pending.removeFirst() }) { seen += it }

        assertEquals(0x502, first)
        assertEquals(listOf(0x502, 0x500), seen)
    }

    @Test
    fun anErrorThatNeverClearsStopsAfterTheLimit() {
        var reads = 0
        val first = drainGlErrors({ reads += 1; 0x0507 }) { }

        assertEquals(0x0507, first)
        assertEquals(MAX_GL_ERRORS_PER_DRAIN, reads)
    }

    @Test
    fun noErrorReadsOnce() {
        var reads = 0
        assertEquals(0, drainGlErrors({ reads += 1; 0 }) { })
        assertEquals(1, reads)
    }
}
