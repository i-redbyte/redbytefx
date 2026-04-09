package ru.redbyte.redbytefx.compose

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FxControllerValueCacheTest {

    @Test
    fun sameFloatTreatsNaNAsStable() {
        assertTrue(sameFloat(Float.NaN, Float.NaN))
    }

    @Test
    fun sameFloatPreservesSignedZeroDifference() {
        assertFalse(sameFloat(0f, -0f))
    }

    @Test
    fun sameFloat4UsesBitwiseComparisonForAllComponents() {
        assertTrue(
            sameFloat4(
                value = Float4Value(1f, Float.NaN, -0f, 4f),
                x = 1f,
                y = Float.NaN,
                z = -0f,
                w = 4f
            )
        )
        assertFalse(
            sameFloat4(
                value = Float4Value(1f, Float.NaN, -0f, 4f),
                x = 1f,
                y = Float.NaN,
                z = 0f,
                w = 4f
            )
        )
    }
}
