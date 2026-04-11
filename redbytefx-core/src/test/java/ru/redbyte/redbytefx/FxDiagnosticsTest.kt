package ru.redbyte.redbytefx

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FxDiagnosticsTest {

    @Test
    fun diagnosticFullTextJoinsMessageAndHint() {
        val d = FxDiagnostic(
            severity = FxDiagnosticSeverity.ERROR,
            code = FxDiagnosticCode.UNSUPPORTED_EMIT_ANY,
            message = "Base.",
            hint = "Hint: more."
        )
        assertEquals("Base. Hint: more.", d.fullText())
    }

    @Test
    fun diagnosticExceptionMessageMatchesFullDiagnostics() {
        val d1 = FxDiagnostic(
            severity = FxDiagnosticSeverity.ERROR,
            code = FxDiagnosticCode.FN_RETURN_TYPE_MISMATCH,
            message = "First line.",
            hint = null
        )
        val d2 = FxDiagnostic(
            severity = FxDiagnosticSeverity.ERROR,
            code = FxDiagnosticCode.MISSING_UNIFORM_BINDING,
            message = "Second line.",
            hint = null
        )
        val ex = FxDiagnosticException(listOf(d1, d2))
        assertEquals("First line.\nSecond line.", ex.message)
        assertEquals(2, ex.diagnostics.size)
        assertEquals(d1, ex.primary)
    }

    @Test
    fun diagnosticExceptionIsIllegalStateException() {
        val ex = FxDiagnosticException(
            FxDiagnostic(
                severity = FxDiagnosticSeverity.ERROR,
                code = FxDiagnosticCode.NON_FINITE_FLOAT_LITERAL,
                message = "x",
                hint = null
            )
        )
        assertTrue(ex is IllegalStateException)
    }
}
