package ru.redbyte.redbytefx.gl

import org.junit.Assert.assertEquals
import org.junit.Test

class Es3ContextTest {
    @Test
    fun anInstrumentedContextAsksForEs32BeforeLowerMinors() {
        assertEquals(listOf(2, 1, 0), es3ContextMinors().toList())
    }
}
