package ru.redbyte.redbytefx

/**
 * Thrown when AGSL playback is requested on an API level below [RedByteFxApis.AGSL_MIN_SDK].
 */
public class AgslNotSupportedException(
    message: String,
) : IllegalStateException(message)

internal fun agslNotSupportedMessage(sdkInt: Int): String =
    "RedByteFX AGSL requires Android API ${RedByteFxApis.AGSL_MIN_SDK}+ (current: $sdkInt). " +
        "Use ShaderTarget.Gles30 or Gles32 with OpenGL Compose, or raise minSdk. " +
        "See ${RedByteFxApis.DOCS_BASE_URL}"
