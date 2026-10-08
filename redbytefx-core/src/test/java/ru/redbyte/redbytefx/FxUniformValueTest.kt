package ru.redbyte.redbytefx

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FxUniformValueTest {

    @Test
    fun sameFloatUniformValueTreatsNaNPayloadAsStable() {
        val first = Float.fromBits(0x7fc00001)
        val second = Float.fromBits(0x7fc00002)
        assertTrue(sameFloatUniformValue(first, first))
        assertFalse(sameFloatUniformValue(first, second))
    }

    @Test
    fun sameFloatUniformValuePreservesSignedZeroDifference() {
        assertFalse(sameFloatUniformValue(0f, -0f))
    }
}
