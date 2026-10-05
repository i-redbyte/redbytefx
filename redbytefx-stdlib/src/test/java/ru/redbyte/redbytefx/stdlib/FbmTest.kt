package ru.redbyte.redbytefx.stdlib

import org.junit.Assert.assertThrows
import org.junit.Test
import ru.redbyte.redbytefx.float2

class FbmTest {

    @Test
    fun fbmRejectsOctavesOutsideItsDocumentedRange() {
        for (octaves in listOf(0, 7)) {
            assertThrows(IllegalArgumentException::class.java) {
                fbm(float2(0.2f, 0.4f), octaves)
            }
            assertThrows(IllegalArgumentException::class.java) {
                fbm(float2(0.2f, 0.4f), octaves, 2f, 0.5f)
            }
        }
    }
}
