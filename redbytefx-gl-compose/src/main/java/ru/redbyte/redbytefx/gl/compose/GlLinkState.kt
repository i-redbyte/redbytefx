package ru.redbyte.redbytefx.gl.compose

/**
 * Link lifecycle for a [GlController] attached to [GlSurface].
 */
public sealed interface GlLinkState {
    public data object Pending : GlLinkState

    public data object Linked : GlLinkState

    public data class Failed(public val message: String) : GlLinkState
}
