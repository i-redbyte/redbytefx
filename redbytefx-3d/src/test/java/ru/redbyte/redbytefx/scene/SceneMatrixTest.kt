package ru.redbyte.redbytefx.scene

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Test
import kotlin.math.PI

class SceneMatrixTest {
    @Test
    fun identityWritesTheSharedTemplateIntoTheCallerArray() {
        val held = FloatArray(MATRIX_FLOATS) { 9f }
        assertSame(held, identity(held))
        assertArrayEquals(IDENTITY, held, 0f)
        assertEquals(1f, identity()[0], 0f)
        assertEquals(1f, identity()[15], 0f)
    }

    @Test
    fun translationSitsInTheLastColumn() {
        val matrix = translation(2f, 3f, 4f)
        assertEquals(2f, matrix[12], 0f)
        assertEquals(3f, matrix[13], 0f)
        assertEquals(4f, matrix[14], 0f)
        assertEquals(1f, matrix[15], 0f)
    }

    @Test
    fun multiplyIsColumnMajorGlslProduct() {
        val moved = multiply(translation(1f, 0f, 0f), translation(0f, 2f, 0f))
        assertEquals(1f, moved[12], 0f)
        assertEquals(2f, moved[13], 0f)
        val quarter = rotationZ(PI.toFloat() / 2f)
        assertEquals(0f, quarter[0], 0.0001f)
        assertEquals(1f, quarter[1], 0.0001f)
        assertEquals(-1f, quarter[4], 0.0001f)
        assertEquals(0f, quarter[5], 0.0001f)
    }

    @Test
    fun scaleRejectsAZeroFactor() {
        assertThrows(IllegalArgumentException::class.java) { scale(0f, 1f, 1f) }
        val stretched = scale(2f, 3f, 4f)
        assertEquals(2f, stretched[0], 0f)
        assertEquals(3f, stretched[5], 0f)
        assertEquals(4f, stretched[10], 0f)
    }
}
