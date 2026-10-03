package ru.redbyte.redbytefx.gl.compose

import ru.redbyte.redbytefx.gl.GlProgramRuntime

/**
 * Per-frame GLES context passed to [GlSurface] render callbacks.
 */
public class GlFrame(
    public val runtime: GlProgramRuntime,
    public val seconds: Float,
    public val aspect: Float,
    private val upload: (FloatArray) -> Unit,
) {
    /** Replaces the bound VBO contents on the GL thread (same mesh stride as [GlMesh]). */
    public fun replace(vertices: FloatArray) {
        upload(vertices)
    }
}
