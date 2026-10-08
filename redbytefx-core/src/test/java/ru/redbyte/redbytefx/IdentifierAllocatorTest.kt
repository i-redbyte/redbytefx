package ru.redbyte.redbytefx

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class IdentifierAllocatorTest {
    @Test
    fun repeatedNamesAdvancePastReservedSuffixes() {
        val names = IdentifierAllocator(setOf("color_2"))
        assertEquals("color", names.reserve("color"))
        assertEquals("color_1", names.reserve("color"))
        assertEquals("color_3", names.reserve("color"))
        assertEquals("color_4", names.reserve("color"))
        assertTrue(names.snapshot().containsAll(setOf("color", "color_1", "color_2", "color_3", "color_4")))
    }

    @Test
    fun manyRepeatedNamesRemainUnique() {
        val names = IdentifierAllocator()
        val generated = (0 until 1_000).map { names.reserve("temporary") }
        assertEquals(1_000, generated.toSet().size)
        assertEquals("temporary_999", generated.last())
    }
}
