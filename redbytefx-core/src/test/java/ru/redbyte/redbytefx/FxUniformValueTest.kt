package ru.redbyte.redbytefx

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FxUniformValueTest {

    @Test
    fun sameFloatUniformValueTreatsNaNPayloadAsStable() {
        assertTrue(sameFloatUniformValue(Float.NaN, Float.NaN))
    }

    @Test
    fun sameFloatUniformValuePreservesSignedZeroDifference() {
        assertFalse(sameFloatUniformValue(0f, -0f))
    }
}
