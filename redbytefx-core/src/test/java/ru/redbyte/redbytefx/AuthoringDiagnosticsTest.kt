package ru.redbyte.redbytefx

import org.junit.Assert.assertTrue
import org.junit.Test

class AuthoringDiagnosticsTest {

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
}
