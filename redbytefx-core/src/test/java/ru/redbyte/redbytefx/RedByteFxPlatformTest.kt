package ru.redbyte.redbytefx

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RedByteFxPlatformTest {
    @After
    fun restoreSdk() {
        RedByteFxPlatform.sdkInt = { android.os.Build.VERSION.SDK_INT }
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
