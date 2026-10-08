package ru.redbyte.redbytefx.scene

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI

class SceneCameraTest {
    @Test
    fun lookAtLooksDownNegativeZ() {
        val matrix = lookAt(0f, 0f, 5f, 0f, 0f, 0f, 0f, 1f, 0f)
        assertEquals(16, matrix.size)
        assertEquals(1f, matrix[0], 0.0001f)
        assertEquals(-5f, matrix[14], 0.0001f)
        assertTrue(matrix.all { it.isFinite() })
    }

    @Test
    fun theDestinationFormsWriteTheSameMatrixIntoTheCallersArray() {
        val view = FloatArray(MATRIX_FLOATS) { 9f }
        assertSame(view, lookAt(1f, 2f, 5f, 0f, 0f, 0f, 0f, 1f, 0f, view))
        assertArrayEquals(lookAt(1f, 2f, 5f, 0f, 0f, 0f, 0f, 1f, 0f), view, 0f)
        val projection = FloatArray(MATRIX_FLOATS) { 9f }
        assertSame(projection, perspective(0.9f, 1.5f, 0.1f, 20f, projection))
        assertArrayEquals(perspective(0.9f, 1.5f, 0.1f, 20f), projection, 0f)
        assertThrows(IllegalArgumentException::class.java) {
            perspective(0.9f, 1.5f, 0.1f, 20f, FloatArray(9))
        }
        val untouched = FloatArray(MATRIX_FLOATS) { 9f }
        assertThrows(IllegalArgumentException::class.java) {
            lookAt(0f, 0f, 0f, 0f, 0f, 0f, 0f, 1f, 0f, untouched)
        }
        assertTrue(untouched.all { it == 9f })
    }

    @Test
    fun lookAtRejectsADegenerateCameraBeforeWritingNaN() {
        val samePoint = assertThrows(IllegalArgumentException::class.java) {
            lookAt(1f, 2f, 3f, 1f, 2f, 3f, 0f, 1f, 0f)
        }
        assertTrue(samePoint.message.orEmpty().contains("eye"))
        val parallel = assertThrows(IllegalArgumentException::class.java) {
            lookAt(0f, 0f, 0f, 0f, 0f, 1f, 0f, 0f, 1f)
        }
        assertTrue(parallel.message.orEmpty().contains("up"))
    }

    @Test
    fun overflowingCameraMathLeavesTheDestinationUntouched() {
        val destination = FloatArray(MATRIX_FLOATS) { 7f }
        assertThrows(IllegalArgumentException::class.java) {
            lookAt(Float.MAX_VALUE, 0f, 0f, -Float.MAX_VALUE, 0f, 0f, 0f, 1f, 0f, destination)
        }
        assertTrue(destination.all { it == 7f })
        assertThrows(IllegalArgumentException::class.java) {
            perspective(Float.MIN_VALUE, 1f, 0.1f, 10f, destination)
        }
        assertTrue(destination.all { it == 7f })
        assertThrows(IllegalArgumentException::class.java) {
            ortho(0f, Float.MIN_VALUE, 0f, 1f, 0f, 1f, destination)
        }
        assertTrue(destination.all { it == 7f })
    }

    @Test
    fun projectionCalculationsAvoidIntermediateFloatOverflow() {
        val perspective = perspective(1f, 1f, 1.0e20f, 2.0e20f)
        assertEquals(-4.0e20f, perspective[14], 1.0e15f)
        val ortho = ortho(2.0e38f, 3.0e38f, -1f, 1f, 1f, 2f)
        assertEquals(-5f, ortho[12], 0.01f)
        assertTrue(perspective.all { it.isFinite() })
        assertTrue(ortho.all { it.isFinite() })
    }

    @Test
    fun perspectiveUsesOpenGlClipSpace() {
        val matrix = perspective(PI.toFloat() / 2f, 1f, 0.1f, 100f)
        assertEquals(1f, matrix[0], 0.00001f)
        assertEquals((100f + 0.1f) / (0.1f - 100f), matrix[10], 0.00001f)
        assertEquals(-1f, matrix[11], 0.00001f)
        assertThrows(IllegalArgumentException::class.java) {
            perspective(PI.toFloat() / 2f, 1f, 0f, 10f)
        }
        assertThrows(IllegalArgumentException::class.java) {
            perspective(PI.toFloat() / 2f, 1f, 2f, 1f)
        }
    }

    @Test
    fun orthoMatchesTheOpenGlColumnMajorMatrix() {
        val matrix = ortho(0f, 2f, 0f, 4f, 1f, 5f)
        assertEquals(1f, matrix[0], 0f)
        assertEquals(0.5f, matrix[5], 0f)
        assertEquals(-0.5f, matrix[10], 0f)
        assertEquals(-1f, matrix[12], 0f)
        assertEquals(-1f, matrix[13], 0f)
        assertEquals(-1.5f, matrix[14], 0f)
        assertEquals(1f, matrix[15], 0f)
        assertEquals(0f, matrix[11], 0f)

        val held = FloatArray(MATRIX_FLOATS) { 7f }
        assertThrows(IllegalArgumentException::class.java) {
            ortho(1f, 1f, 0f, 1f, 0f, 1f, held)
        }
        assertEquals(7f, held[0], 0f)
        assertThrows(IllegalArgumentException::class.java) {
            ortho(0f, 1f, 0f, 1f, 2f, 1f, held)
        }
        assertEquals(7f, held[0], 0f)
    }
}
