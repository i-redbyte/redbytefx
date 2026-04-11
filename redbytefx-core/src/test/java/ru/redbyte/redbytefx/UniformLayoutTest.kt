package ru.redbyte.redbytefx

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
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

    @Test
    fun uniformLayoutSuffixesConflictingSanitizedNames() {
        val first = FxParam.Float("WaveAmplitude")
        val second = FxParam.Float("wave_amplitude")
        val layout = UniformLayout()

        val firstName = layout.uniformName(first)
        val secondName = layout.uniformName(second)

        assertEquals("u_wave_amplitude", firstName)
        assertEquals("u_wave_amplitude_1", secondName)
    }

    @Test
    fun occupiedIdentifiersExposeGeneratedUniformNames() {
        val amount = FxParam.Float("amount")
        val offset = FxParam.Float2("offset")
        val layout = UniformLayout().apply {
            register(amount)
            register(offset)
        }

        val occupied = layout.occupiedIdentifiers()

        assertTrue(occupied.contains("u_amount"))
        assertTrue(occupied.contains("u_offset"))
        assertTrue(occupied.contains(RB_INPUT_UNIFORM))
        assertTrue(occupied.contains(RB_RESOLUTION_UNIFORM))
    }
}
