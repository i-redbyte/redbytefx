package ru.redbyte.redbytefx

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class EmitContextTest {

    @Test
    fun localNamesAvoidReservedLayoutIdentifiers() {
        val layout = UniformLayout(setOf("l_signal_mask"))
        val ctx = EmitContext(layout)
        val first = LocalFloatExprImpl("signalMask", floatLiteral(0.25f))
        val second = LocalFloatExprImpl("signalMask", floatLiteral(0.5f))

        val firstName = ctx.localName(first)
        val secondName = ctx.localName(second)

        assertEquals("l_signal_mask_1", firstName)
        assertEquals("l_signal_mask_2", secondName)
        assertTrue(
            ctx.declarations().contains("  float l_signal_mask_1 = 0.25;")
        )
        assertTrue(
            ctx.declarations().contains("  float l_signal_mask_2 = 0.5;")
        )
    }

    @Test
    fun localNameMemoizesDeclarationPerExpressionInstance() {
        val ctx = EmitContext(UniformLayout())
        val local = LocalFloatExprImpl("waveValue", floatLiteral(0.5f))

        val firstName = ctx.localName(local)
        val secondName = ctx.localName(local)

        assertEquals("l_wave_value", firstName)
        assertEquals(firstName, secondName)
        assertEquals(1, ctx.declarations().size)
    }
}
