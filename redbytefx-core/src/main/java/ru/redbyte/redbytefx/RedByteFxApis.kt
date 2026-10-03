package ru.redbyte.redbytefx

/**
 * Documented Android API floors for RedByteFX features.
 *
 * See the platform requirements page on the project documentation site for the full matrix.
 */
public object RedByteFxApis {
    /** OpenGL ES 3.0 scenes through [ru.redbyte.redbytefx.gl.GlProgramRuntime]. */
    public const val GLES30_MIN_SDK: Int = 24

    /** GLES 3.1 compute shaders. */
    public const val GLES31_MIN_SDK: Int = 24

    /** GLES 3.2 geometry and tessellation (driver-dependent; context 3.2 required). */
    public const val GLES32_MIN_SDK: Int = 24

    /**
     * AGSL [android.graphics.RuntimeShader] and Compose [ru.redbyte.redbytefx.compose.redbyteFx].
     *
     * API 31 (Android 12).
     */
    public const val AGSL_MIN_SDK: Int = 31

    /** Published API reference and platform matrix. */
    public const val DOCS_BASE_URL: String = "https://i-redbyte.github.io/redbytefx/"
}
