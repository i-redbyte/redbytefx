package ru.redbyte.redbytefx.gl.compose

import android.opengl.EGL14
import ru.redbyte.redbytefx.ShaderProgram
import ru.redbyte.redbytefx.gl.GlException
import ru.redbyte.redbytefx.gl.GlProgramRuntime
import ru.redbyte.redbytefx.gl.GlTextureUnits
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

/** State shared by a surface view and its renderer. GL members are touched on the GL thread only. */
internal class GlSlot {
    var runtime: GlProgramRuntime? = null
    var eglContextHandle: Long = 0L
    var releaseGl: ((Boolean) -> Unit)? = null
    var post: (() -> Unit) -> Unit = {}
    var queue: (() -> Unit) -> Unit = {}
    var currentContextHandle: () -> Long = { EGL14.eglGetCurrentContext().nativeHandle }

    fun captureContext() {
        eglContextHandle = currentContextHandle()
    }

    fun ownsCurrentContext(): Boolean {
        return eglContextHandle != 0L && eglContextHandle == currentContextHandle()
    }
}

/** Links [program] on the calling EGL thread. A failed link deletes its names and rethrows. */
internal fun linkProgram(
    program: ShaderProgram,
    strictUniformLocations: Boolean,
    textureUnits: GlTextureUnits,
    strictErrors: Boolean = false,
): GlProgramRuntime {
    val linked = GlProgramRuntime(
        program,
        Gles30Device(),
        strictUniformLocations = strictUniformLocations,
        textureUnits = textureUnits,
        strictErrors = strictErrors,
    )
    try {
        linked.link()
    } catch (error: Throwable) {
        linked.destroy()
        throw error
    }
    return linked
}
