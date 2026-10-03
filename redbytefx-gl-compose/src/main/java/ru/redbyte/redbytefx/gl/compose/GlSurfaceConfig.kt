package ru.redbyte.redbytefx.gl.compose

/**
 * EGL and runtime options for [GlSurface].
 */
public data class GlSurfaceConfig(
    /**
     * When true, every draw in the frame depth-tests.
     * A mesh with depth set depth-tests that draw on its own.
     * The window always has a depth buffer, including when this flag and every mesh are false.
     */
    public val depth: Boolean = false,
    /** When true, inactive spelled uniforms fail [ru.redbyte.redbytefx.gl.GlProgramRuntime.link]. */
    public val strictUniformLocations: Boolean = false,
)
