package ru.redbyte.redbytefx

import android.os.Build

/**
 * Runtime platform checks shared by AGSL and Compose entry points.
 */
public object RedByteFxPlatform {
    internal var sdkInt: () -> Int = { Build.VERSION.SDK_INT }

    /**
     * Verifies that the current device can execute AGSL shaders.
     *
     * @throws AgslNotSupportedException when [sdkInt] is below [RedByteFxApis.AGSL_MIN_SDK].
     */
    public fun requireAgslRuntime() {
        val level = sdkInt()
        if (level < RedByteFxApis.AGSL_MIN_SDK) {
            throw AgslNotSupportedException(agslNotSupportedMessage(level))
        }
    }
}
