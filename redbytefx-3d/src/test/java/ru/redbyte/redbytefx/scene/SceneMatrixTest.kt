package ru.redbyte.redbytefx.scene

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI

class SceneMatrixTest {
    @Test
    fun transposeSupportsCallerStorageAndInPlaceUpdates() {
        val source = FloatArray(MATRIX_FLOATS) { it.toFloat() }
        val expected = floatArrayOf(
            0f, 4f, 8f, 12f,
            1f, 5f, 9f, 13f,
            2f, 6f, 10f, 14f,
            3f, 7f, 11f, 15f,
        )
        val output = FloatArray(MATRIX_FLOATS)
        assertSame(output, transpose(source, output))
        assertArrayEquals(expected, output, 0f)
        assertSame(source, transpose(source, source))
        assertArrayEquals(expected, source, 0f)
    }

    @Test
    fun inverseWorksForModelAndProjectionMatricesWithoutTemporaryArrays() {
        val model = multiply(translation(2f, -3f, 5f), multiply(rotationY(0.4f), scale(2f, 3f, 4f)))
        val projection = perspective(PI.toFloat() / 3f, 1.5f, 0.1f, 100f)
        for (matrix in listOf(model, projection, multiply(projection, model))) {
            val original = matrix.copyOf()
            val inverseStorage = FloatArray(MATRIX_FLOATS)
            assertSame(inverseStorage, inverse(matrix, inverseStorage))
            val product = multiply(original, inverseStorage)
            for (index in 0 until MATRIX_FLOATS) {
                assertEquals(IDENTITY[index], product[index], 0.0001f)
            }
            assertSame(matrix, inverse(matrix, matrix))
            assertArrayEquals(inverseStorage, matrix, 0.0001f)
        }
    }

    @Test
    fun inverseRejectsSingularAndNonFiniteInputsWithoutChangingOutput() {
        val output = FloatArray(MATRIX_FLOATS) { 7f }
        val singular = identity().also { it[0] = 0f }
        assertThrows(IllegalArgumentException::class.java) { inverse(singular, output) }
        assertTrue(output.all { it == 7f })
        val nonFinite = identity().also { it[5] = Float.NaN }
        assertThrows(IllegalArgumentException::class.java) { inverse(nonFinite, output) }
        assertTrue(output.all { it == 7f })
        val tooSmall = identity().also { it[0] = 1.0e-40f }
        assertThrows(IllegalArgumentException::class.java) { inverse(tooSmall, output) }
        assertTrue(output.all { it == 7f })
    }

    @Test
    fun pointUsesTranslationAndPerspectiveWhileDirectionIgnoresTranslation() {
        val model = multiply(translation(3f, 4f, 5f), scale(2f, 3f, 4f))
        val point = FloatArray(3)
        val direction = FloatArray(3)
        assertSame(point, transformPoint(model, 1f, 2f, 3f, point))
        assertArrayEquals(floatArrayOf(5f, 10f, 17f), point, 0f)
        assertSame(direction, transformDirection(model, 1f, 2f, 3f, direction))
        assertArrayEquals(floatArrayOf(2f, 6f, 12f), direction, 0f)

        val projection = perspective(PI.toFloat() / 2f, 1f, 1f, 10f)
        val projected = transformPoint(projection, 1f, 0f, -2f)
        assertEquals(0.5f, projected[0], 0.0001f)
        assertEquals(0f, projected[1], 0f)
        assertThrows(IllegalArgumentException::class.java) {
            transformPoint(projection, 0f, 0f, 0f, point)
        }
        assertArrayEquals(floatArrayOf(5f, 10f, 17f), point, 0f)
    }

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
