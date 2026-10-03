package ru.redbyte.redbytefx.gl.compose

import ru.redbyte.redbytefx.ShaderProgram
import ru.redbyte.redbytefx.gl.GlException
import ru.redbyte.redbytefx.gl.GlProgramRuntime
import ru.redbyte.redbytefx.gl.Gles30Device

/** Shown when link fails and no other detail is available. */
public const val GL_LINK_FALLBACK: String = "This device cannot link the shader."

internal fun linkMessage(requirement: String?, error: GlException): String {
    val detail = error.message
    return when {
        requirement != null && !detail.isNullOrBlank() -> "$requirement $detail"
        requirement != null -> requirement
        !detail.isNullOrBlank() -> detail
        else -> GL_LINK_FALLBACK
    }
}

internal class GlSlot {
    var runtime: GlProgramRuntime? = null
    var releaseGl: (() -> Unit)? = null
    var post: (() -> Unit) -> Unit = {}
}

internal fun GlSlot.linkGraphics(
    program: ShaderProgram,
    strictUniformLocations: Boolean,
    requirement: String?,
    onFailure: ((String) -> Unit)? = null,
): GlProgramRuntime? {
    runtime?.destroy()
    releaseGl?.invoke()
    val linked = GlProgramRuntime(program, Gles30Device(), strictUniformLocations = strictUniformLocations)
    return try {
        linked.link()
        linked
    } catch (error: GlException) {
        linked.destroy()
        onFailure?.invoke(linkMessage(requirement, error))
        null
    }
}
