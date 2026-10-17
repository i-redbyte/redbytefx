package ru.redbyte.redbytefx.gl.compose

/**
 * GL thread. The surface mesh a [GlSurface] draws, following the latest composition.
 *
 * A new mesh replaces the surface buffers on the existing EGL context instead of recreating the
 * view, so passing a different [GlMesh] costs one upload, not a relink.
 */
internal class MeshSwitch(
    initial: GlMesh,
    private val latest: () -> GlMesh,
) {
    var current: GlMesh = initial
        private set

    /** The mesh composition asked for since the last call, or null when it is unchanged. */
    fun takeChange(): GlMesh? {
        val next = latest()
        if (next === current) return null
        current = next
        return next
    }
}
