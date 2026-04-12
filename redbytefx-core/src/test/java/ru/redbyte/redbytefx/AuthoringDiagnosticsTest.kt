package ru.redbyte.redbytefx

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AuthoringDiagnosticsTest {

    private class CustomFloatExpr : FloatExpr

    private class CustomColorExpr : ColorExpr

    @Test
    fun missingUniformBindingMessageExplainsEffectScope() {
        val message = missingUniformBindingMessage(
            typeLabel = "Float",
            debugName = "amount"
        )

        assertTrue(message.contains("float uniform 'amount'"))
        assertTrue(message.contains("same redbytefx { ... } effect"))
        assertTrue(message.contains("FxParam handles"))
        assertTrue(message.contains("another `redbytefx { }`"))
        assertTrue(message.contains("setFloat(...)"))
    }

    @Test
    fun missingUniformBindingMessageHandlesGeneratedUniforms() {
        val message = missingUniformBindingMessage(
            typeLabel = "Float2",
            debugName = null
        )

        assertTrue(message.contains("float2 uniform with a generated name"))
        assertTrue(message.contains("controller bind API"))
    }

    @Test
    fun nonFiniteFloatLiteralMessageSuggestsUniforms() {
        val error = runCatching {
            redbytefx {
                color(Float.NaN, 0f, 0f)
            }.agslSource()
        }.exceptionOrNull()

        check(error is FxDiagnosticException) {
            "Expected FxDiagnosticException for NaN literal, got ${error?.javaClass?.name}"
        }
        assertEquals(FxDiagnosticCode.NON_FINITE_FLOAT_LITERAL, error.primary.code)

        val message = checkNotNull(error.message)
        assertTrue(message.contains("Only finite float literals"))
        assertTrue(message.contains("NaN"))
        assertTrue(message.contains("uniformFloat(...)"))
    }

    @Test
    fun unsupportedExpressionArgumentMessagePointsToDslConversions() {
        val error = runCatching {
            emitAny("not a shader expr", EmitContext(UniformLayout()))
        }.exceptionOrNull()

        check(error is FxDiagnosticException) {
            "Expected FxDiagnosticException for unsupported expression argument, got ${error?.javaClass?.name}"
        }
        assertEquals(FxDiagnosticCode.UNSUPPORTED_EMIT_ANY, error.primary.code)

        val message = checkNotNull(error.message)
        assertTrue(message.contains("kotlin.String"))
        assertTrue(message.contains("Shader helpers only accept DSL expressions"))
        assertTrue(message.contains("float(...)"))
        assertTrue(message.contains("uniform"))
        assertTrue(message.contains("Hint:"))
    }

    @Test
    fun unsupportedExpressionArgumentHintExplainsRawInteger() {
        val error = runCatching {
            emitAny(42, EmitContext(UniformLayout()))
        }.exceptionOrNull()

        check(error is FxDiagnosticException) {
            "Expected FxDiagnosticException for raw Int, got ${error?.javaClass?.name}"
        }
        assertEquals(FxDiagnosticCode.UNSUPPORTED_EMIT_ANY, error.primary.code)

        val message = checkNotNull(error.message)
        assertTrue(message.contains("kotlin.Int"))
        assertTrue(message.contains("Hint:"))
        assertTrue(message.contains("Integer literals", ignoreCase = true))
        assertTrue(message.contains("float(...)"))
    }

    @Test
    fun customFloatExprInsideLetExplainsHowToFixIt() {
        val error = runCatching {
            redbytefx {
                val custom = CustomFloatExpr()
                val bad = let(custom, "badValue")
                color(bad, bad, bad)
            }.agslSource()
        }.exceptionOrNull()

        check(error is FxDiagnosticException) {
            "Expected FxDiagnosticException for custom FloatExpr in let(...), got ${error?.javaClass?.name}"
        }
        assertEquals(FxDiagnosticCode.UNSUPPORTED_DSL_IMPLEMENTATION, error.primary.code)

        val message = checkNotNull(error.message)
        assertTrue(message.contains("Unsupported FloatExpr implementation"))
        assertTrue(message.contains("Do not implement FloatExpr directly"))
        assertTrue(message.contains("let(...)"))
        assertTrue(message.contains("custom marker-interface implementation"))
    }

    @Test
    fun fnReturnTypeMismatchExplainsExpectedVsActual() {
        // Kotlin's typed DSL forbids color(...) where FloatExpr is required; validate at the same
        // layer the compiler uses (see compileFunction → validateFnBodyMatchesReturnType).
        val error = runCatching {
            validateFnBodyMatchesReturnType(
                functionName = "wrongReturn",
                returnType = FloatType,
                body = CustomColorExpr()
            )
        }.exceptionOrNull()

        check(error is FxDiagnosticException) {
            "Expected FxDiagnosticException for fn return type mismatch, got ${error?.javaClass?.name}"
        }
        assertEquals(FxDiagnosticCode.FN_RETURN_TYPE_MISMATCH, error.primary.code)

        val message = checkNotNull(error.message)
        assertTrue(message.contains("wrongReturn"))
        assertTrue(message.contains("float"))
        assertTrue(message.contains("color expression") || message.contains("half4"))
    }

    @Test
    fun customFloatExprReturnedFromFnExplainsHowToFixIt() {
        val error = runCatching {
            redbytefx {
                val broken = fn(
                    name = "brokenSignal",
                    returns = FloatType
                ) {
                    CustomFloatExpr()
                }
                val value = let(broken(), "value")
                color(value, value, value)
            }.agslSource()
        }.exceptionOrNull()

        check(error is FxDiagnosticException) {
            "Expected FxDiagnosticException for custom FloatExpr returned from fn(...), got ${error?.javaClass?.name}"
        }
        assertEquals(FxDiagnosticCode.UNSUPPORTED_DSL_IMPLEMENTATION, error.primary.code)

        val message = checkNotNull(error.message)
        assertTrue(message.contains("Unsupported FloatExpr implementation"))
        assertTrue(message.contains("fn(...)"))
        assertTrue(message.contains("return a composed DSL expression"))
    }
}
