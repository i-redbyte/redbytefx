package ru.redbyte.redbytefx

import android.os.Build
import androidx.annotation.ChecksSdkIntAtLeast

/**
 * Runtime platform checks shared by AGSL and Compose entry points.
 */
public object RedByteFxPlatform {
    internal var sdkInt: () -> Int = { Build.VERSION.SDK_INT }

    /**
     * True when the current device can execute AGSL shaders. Use it to choose a fallback, such as a
     * GLES scene, instead of catching [AgslNotSupportedException]. Compiling an AGSL program with
     * `shader(ShaderTarget.Agsl)` works on any API level; only running it needs this check.
     */
    @ChecksSdkIntAtLeast(api = RedByteFxApis.AGSL_MIN_SDK)
    public fun isAgslSupported(): Boolean = sdkInt() >= RedByteFxApis.AGSL_MIN_SDK

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
