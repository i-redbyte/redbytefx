package ru.redbyte.redbytefx.sample.ui.gl

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PlanetPhotoTest {
    @Test
    fun photosDecodePremultipliedSoTheyCanBeScaled() {
        assertTrue(planetDecodePlan(4000, 3000, edge = 512).premultiplied)
    }

    @Test
    fun largePhotosAreSubsampledToAtMostTwiceTheEdge() {
        assertEquals(8, planetDecodePlan(8000, 6000, edge = 512).sampleSize)
        assertEquals(1, planetDecodePlan(900, 600, edge = 512).sampleSize)
    }
}
