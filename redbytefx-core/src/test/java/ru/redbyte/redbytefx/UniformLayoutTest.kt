package ru.redbyte.redbytefx

import org.junit.Assert.assertEquals
import org.junit.Test

class UniformLayoutTest {

    @Test
    fun sanitizeSuggestedIdentifierNormalizesCamelCaseAndSymbols() {
        val identifier = sanitizeSuggestedIdentifier(
            raw = "WaveAmplitude (%)",
            leadingDigitPrefix = "u_"
        )

        assertEquals("wave_amplitude", identifier)
    }

    @Test
    fun sanitizeSuggestedIdentifierProtectsLeadingDigits() {
        val identifier = sanitizeSuggestedIdentifier(
            raw = "2D Glow",
            leadingDigitPrefix = "u_"
        )

        assertEquals("u_2_d_glow", identifier)
    }
}
