package ru.redbyte.redbytefx.sample.app

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import ru.redbyte.redbytefx.sample.model.DemoId

class SampleLaunchArgsTest {
    @Test
    fun aRequestedDemoOpensWhenAgslRuns() {
        assertEquals(DemoId.Wave, startDemoFrom(" wave ", agslSupported = true))
    }

    @Test
    fun aRequestedDemoIsIgnoredWithoutAgsl() {
        assertNull(startDemoFrom("Wave", agslSupported = false))
    }

    @Test
    fun anUnknownOrMissingDemoOpensNothing() {
        assertNull(startDemoFrom("NoSuchDemo", agslSupported = true))
        assertNull(startDemoFrom(null, agslSupported = true))
    }
}
