package ru.redbyte.redbytefx.gl.compose

/**
 * EGL and runtime options for [GlSurface].
 */
public data class GlSurfaceConfig(
    /** When true, requests a depth buffer in the EGL config. */
    public val depth: Boolean = false,
    /** When true, inactive spelled uniforms fail [ru.redbyte.redbytefx.gl.GlProgramRuntime.link]. */
    public val strictUniformLocations: Boolean = false,
)
