package ru.redbyte.redbytefx.sample.ui.gl

import ru.redbyte.redbytefx.ShaderProgram
import ru.redbyte.redbytefx.gl.GlException
import ru.redbyte.redbytefx.gl.GlProgramRuntime
import ru.redbyte.redbytefx.gl.Gles30Device

internal const val LINK_FALLBACK = "This device cannot link the shader."

internal fun linkMessage(requirement: String?, error: GlException): String {
    val detail = error.message
    return when {
        requirement != null && !detail.isNullOrBlank() -> "$requirement $detail"
        requirement != null -> requirement
        !detail.isNullOrBlank() -> detail
        else -> LINK_FALLBACK
    }
}

/**
 * Links [program] on the GL thread. Returns null and destroys the runtime when linking fails.
 */
internal fun GlSlot.linkGraphics(
    program: ShaderProgram,
    requirement: String? = null,
    onFailure: ((String) -> Unit)? = null,
): GlProgramRuntime? {
    runtime?.destroy()
    releaseGl?.invoke()
    val linked = GlProgramRuntime(program, Gles30Device())
    return try {
        linked.link()
        linked
    } catch (error: GlException) {
        linked.destroy()
        onFailure?.invoke(linkMessage(requirement, error))
        null
    }
}
