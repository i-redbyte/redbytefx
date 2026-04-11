package ru.redbyte.redbytefx

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FxRuntimeStateTest {

    @Test
    fun applyDefaultsSeedsFloatCacheAndSkipsIdenticalRewrite() {
        val amount = FxParam.Float("amount")
        val layout = UniformLayout().apply { register(amount) }
        val writer = TrackingRuntimeUniformWriter()
        var refreshCalls = 0
        val state = FxRuntimeState(
            program = FxProgram(
                agsl = "",
                layout = layout,
                defaults = mapOf(amount to DefaultValue.F(0.5f))
            ),
            writer = writer,
            onRuntimeChanged = { refreshCalls += 1 }
        )

        state.applyDefaults()
        state.setFloat(amount, 0.5f)

        assertEquals(listOf(FloatWrite("u_amount", 0.5f)), writer.floatWrites)
        assertEquals(0, refreshCalls)
    }

    @Test
    fun setFloatRefreshesOnlyWhenValueChanges() {
        val amount = FxParam.Float("amount")
        val layout = UniformLayout().apply { register(amount) }
        val writer = TrackingRuntimeUniformWriter()
        var refreshCalls = 0
        val state = FxRuntimeState(
            program = FxProgram("", layout, emptyMap()),
            writer = writer,
            onRuntimeChanged = { refreshCalls += 1 }
        )

        state.setFloat(amount, 0.5f)
        state.setFloat(amount, 0.5f)
        state.setFloat(amount, 0.75f)

        assertEquals(
            listOf(
                FloatWrite("u_amount", 0.5f),
                FloatWrite("u_amount", 0.75f)
            ),
            writer.floatWrites
        )
        assertEquals(2, refreshCalls)
    }

    @Test
    fun setFloatReturnsFalseWhenValueUnchanged() {
        val amount = FxParam.Float("amount")
        val layout = UniformLayout().apply { register(amount) }
        val writer = TrackingRuntimeUniformWriter()
        val state = FxRuntimeState(
            program = FxProgram("", layout, emptyMap()),
            writer = writer,
            onRuntimeChanged = {}
        )

        assertTrue(state.setFloat(amount, 0.5f))
        assertFalse(state.setFloat(amount, 0.5f))
    }

    @Test
    fun setFloat4UsesBitwiseStabilityForVectorCache() {
        val rgba = FxParam.Float4("rgba")
        val layout = UniformLayout().apply { register(rgba) }
        val writer = TrackingRuntimeUniformWriter()
        var refreshCalls = 0
        val state = FxRuntimeState(
            program = FxProgram("", layout, emptyMap()),
            writer = writer,
            onRuntimeChanged = { refreshCalls += 1 }
        )

        state.setFloat4(rgba, 1f, Float.NaN, -0f, 0.5f)
        state.setFloat4(rgba, 1f, Float.NaN, -0f, 0.5f)
        state.setFloat4(rgba, 1f, Float.NaN, 0f, 0.5f)

        assertEquals(
            listOf(
                Float4Write("u_rgba", 1f, Float.NaN, -0f, 0.5f),
                Float4Write("u_rgba", 1f, Float.NaN, 0f, 0.5f)
            ),
            writer.float4Writes
        )
        assertEquals(2, refreshCalls)
    }

    @Test
    fun setResolutionClampsNonPositiveValuesAndSkipsDuplicateClampedSize() {
        val writer = TrackingRuntimeUniformWriter()
        var refreshCalls = 0
        val state = FxRuntimeState(
            program = FxProgram("", UniformLayout(), emptyMap()),
            writer = writer,
            onRuntimeChanged = { refreshCalls += 1 }
        )

        state.setResolution(0f, -5f)
        state.setResolution(-10f, 0f)
        state.setResolution(320f, 180f)

        assertEquals(
            listOf(
                Float2Write(RB_RESOLUTION_UNIFORM, 1f, 1f),
                Float2Write(RB_RESOLUTION_UNIFORM, 320f, 180f)
            ),
            writer.float2Writes
        )
        assertEquals(2, refreshCalls)
    }

    @Test
    fun setFloatWithForeignParamUsesHelpfulBindingMessage() {
        val registered = FxParam.Float("amount")
        val foreign = FxParam.Float("amount")
        val layout = UniformLayout().apply { register(registered) }
        val writer = TrackingRuntimeUniformWriter()
        val state = FxRuntimeState(
            program = FxProgram("", layout, emptyMap()),
            writer = writer,
            onRuntimeChanged = {}
        )

        val error = runCatching {
            state.setFloat(foreign, 0.5f)
        }.exceptionOrNull()

        check(error is IllegalStateException) {
            "Expected IllegalStateException for foreign param, got ${error?.javaClass?.name}"
        }

        val message = checkNotNull(error.message)
        assertTrue(message.contains("same redbytefx { ... } effect"))
        assertTrue(message.contains("FxParam handles"))
        assertTrue(message.contains("setFloat(...)"))
        assertTrue(writer.floatWrites.isEmpty())
    }
}

private class TrackingRuntimeUniformWriter : FxRuntimeUniformWriter {
    val floatWrites = mutableListOf<FloatWrite>()
    val float2Writes = mutableListOf<Float2Write>()
    val float4Writes = mutableListOf<Float4Write>()

    override fun setFloat(name: String, value: Float) {
        floatWrites += FloatWrite(name, value)
    }

    override fun setFloat2(name: String, x: Float, y: Float) {
        float2Writes += Float2Write(name, x, y)
    }

    override fun setFloat3(name: String, x: Float, y: Float, z: Float) {
    }

    override fun setFloat4(name: String, x: Float, y: Float, z: Float, w: Float) {
        float4Writes += Float4Write(name, x, y, z, w)
    }
}

private data class FloatWrite(
    val name: String,
    val value: Float
)

private data class Float2Write(
    val name: String,
    val x: Float,
    val y: Float
)

private data class Float4Write(
    val name: String,
    val x: Float,
    val y: Float,
    val z: Float,
    val w: Float
)
