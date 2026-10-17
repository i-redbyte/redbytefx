package ru.redbyte.redbytefx

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RedByteFxPlatformTest {
    @After
    fun restoreSdk() {
        RedByteFxPlatform.sdkInt = { android.os.Build.VERSION.SDK_INT }
    }

    @Test
    fun agslSupportIsReportedWithoutThrowing() {
        RedByteFxPlatform.sdkInt = { RedByteFxApis.AGSL_MIN_SDK - 1 }
        assertFalse(RedByteFxPlatform.isAgslSupported())
        RedByteFxPlatform.sdkInt = { RedByteFxApis.AGSL_MIN_SDK }
        assertTrue(RedByteFxPlatform.isAgslSupported())
    }

    @Test
    fun anAgslProgramCompilesOnAnyApiLevel() {
        RedByteFxPlatform.sdkInt = { 24 }
        val source = shader(ShaderTarget.Agsl) { fragment { sample() } }.agslSource()
        assertTrue(source.contains("half4 main("))
    }

    @Test
    fun requireAgslRuntimePassesAtMinimumApi() {
        RedByteFxPlatform.sdkInt = { RedByteFxApis.AGSL_MIN_SDK }
        RedByteFxPlatform.requireAgslRuntime()
    }

    @Test
    fun requireAgslRuntimeFailsBelowMinimumWithActionableMessage() {
        RedByteFxPlatform.sdkInt = { RedByteFxApis.AGSL_MIN_SDK - 1 }
        val error = kotlin.runCatching { RedByteFxPlatform.requireAgslRuntime() }.exceptionOrNull()
        assertTrue(error is AgslNotSupportedException)
        val message = error!!.message!!
        assertTrue(message.contains("API ${RedByteFxApis.AGSL_MIN_SDK}"))
        assertTrue(message.contains("Gles30"))
        assertEquals(RedByteFxApis.DOCS_BASE_URL, message.substringAfter("See ").trim())
    }
}
