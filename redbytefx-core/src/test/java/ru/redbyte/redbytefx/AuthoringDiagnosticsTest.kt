package ru.redbyte.redbytefx

import org.junit.Assert.assertTrue
import org.junit.Test

class AuthoringDiagnosticsTest {

    private class CustomFloatExpr : FloatExpr

    @Test
    fun missingUniformBindingMessageExplainsEffectScope() {
        val message = missingUniformBindingMessage(
            typeLabel = "Float",
            debugName = "amount"
        )

        assertTrue(message.contains("float uniform 'amount'"))
        assertTrue(message.contains("same redbytefx { ... } effect"))
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

        check(error is IllegalArgumentException) {
            "Expected IllegalArgumentException for NaN literal, got ${error?.javaClass?.name}"
        }

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

        check(error is IllegalStateException) {
            "Expected IllegalStateException for unsupported expression argument, got ${error?.javaClass?.name}"
        }

        val message = checkNotNull(error.message)
        assertTrue(message.contains("kotlin.String"))
        assertTrue(message.contains("Shader helpers only accept DSL expressions"))
        assertTrue(message.contains("float(...)"))
        assertTrue(message.contains("uniform"))
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

        check(error is IllegalStateException) {
            "Expected IllegalStateException for custom FloatExpr in let(...), got ${error?.javaClass?.name}"
        }

        val message = checkNotNull(error.message)
        assertTrue(message.contains("Unsupported FloatExpr implementation"))
        assertTrue(message.contains("Do not implement FloatExpr directly"))
        assertTrue(message.contains("let(...)"))
        assertTrue(message.contains("custom marker-interface implementation"))
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

        check(error is IllegalStateException) {
            "Expected IllegalStateException for custom FloatExpr returned from fn(...), got ${error?.javaClass?.name}"
        }

        val message = checkNotNull(error.message)
        assertTrue(message.contains("Unsupported FloatExpr implementation"))
        assertTrue(message.contains("fn(...)"))
        assertTrue(message.contains("return a composed DSL expression"))
    }
}
