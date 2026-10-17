package ru.redbyte.redbytefx

import org.junit.Assert.assertEquals
import org.junit.Test

class FrameClockTest {
    @Test
    fun theFirstFrameStartsAtZeroAndLaterFramesAddTheirDelta() {
        val clock = FrameClock()
        assertEquals(0f, clock.tick(5_000_000_000L), 0f)
        assertEquals(0.016f, clock.tick(5_016_000_000L), 1e-6f)
        assertEquals(0.032f, clock.tick(5_032_000_000L), 1e-6f)
    }

    @Test
    fun aPauseDropsTheGapInsteadOfJumpingForward() {
        val clock = FrameClock()
        clock.tick(0L)
        clock.tick(100_000_000L)
        clock.pause()
        assertEquals(0.1f, clock.tick(60_000_000_000L), 1e-6f)
        assertEquals(0.116f, clock.tick(60_016_000_000L), 1e-6f)
    }

    @Test
    fun aLongStallAdvancesAtMostOneMaximumStep() {
        val clock = FrameClock(maxStepNanos = 250_000_000L)
        clock.tick(0L)
        assertEquals(0.25f, clock.tick(10_000_000_000L), 1e-6f)
    }

    @Test
    fun backwardsTimestampsDoNotRewind() {
        val clock = FrameClock()
        clock.tick(1_000_000_000L)
        clock.tick(1_100_000_000L)
        assertEquals(0.1f, clock.tick(1_050_000_000L), 1e-6f)
    }

    @Test
    fun manyHoursOfFramesAccumulateWithoutDrift() {
        val clock = FrameClock()
        val frame = 16_666_667L
        var now = 0L
        clock.tick(now)
        repeat(60 * 60 * 60 * 6) {
            now += frame
            clock.tick(now)
        }
        assertEquals(now / 1e9, clock.seconds.toDouble(), 0.01)
    }

    @Test
    fun anOffsetShiftsTheReportedTimeOnly() {
        val clock = FrameClock()
        clock.tick(0L)
        clock.tick(200_000_000L)
        assertEquals(2.2f, clock.secondsWithOffset(2f), 1e-6f)
        assertEquals(0.2f, clock.seconds, 1e-6f)
    }
}
