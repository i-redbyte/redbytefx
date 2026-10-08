package ru.redbyte.redbytefx.gl.compose

import android.opengl.EGL14
import android.opengl.GLSurfaceView
import ru.redbyte.redbytefx.ShaderProgram
import ru.redbyte.redbytefx.ShaderTarget
import javax.microedition.khronos.egl.EGL10
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.egl.EGLContext
import javax.microedition.khronos.egl.EGLDisplay

/** Khronos `EGL_CONTEXT_MINOR_VERSION`. Inlined so API 24 does not load `EGL15`. */
private const val EGL_CONTEXT_MINOR_VERSION = 0x30FB

/** Minor version requested for [program]: 2 for GLES 3.2, 1 for GLES 3.1, otherwise 0. */
internal fun eglClientMinor(program: ShaderProgram): Int = when (program.target) {
    ShaderTarget.Gles32 -> 2
    ShaderTarget.Gles31 -> 1
    ShaderTarget.Gles30, ShaderTarget.Agsl -> 0
}

/** Both scene programs share one context, so request the newer version they need. */
internal fun eglClientMinor(program: ShaderProgram, present: ShaderProgram?): Int =
    maxOf(eglClientMinor(program), present?.let(::eglClientMinor) ?: 0)

/**
 * Creates an ES 3 context at [preferredMinor], then lower minor versions if the driver refuses.
 *
 * [GLSurfaceView.setEGLContextClientVersion] only asks for major version 3, which is an ES 3.0
 * context. `#version 310 es` and `#version 320 es` need the matching minor version.
 */
internal class Es3ContextFactory(
    private val preferredMinor: Int,
) : GLSurfaceView.EGLContextFactory {
    override fun createContext(egl: EGL10, display: EGLDisplay, config: EGLConfig): EGLContext {
        for (minor in preferredMinor downTo 0) {
            val context = egl.eglCreateContext(
                display,
                config,
                EGL10.EGL_NO_CONTEXT,
                contextAttributes(minor),
            )
            if (context != null && context != EGL10.EGL_NO_CONTEXT) {
                return context
            }
            egl.eglGetError()
        }
        return EGL10.EGL_NO_CONTEXT
    }

    override fun destroyContext(egl: EGL10, display: EGLDisplay, context: EGLContext) {
        egl.eglDestroyContext(display, context)
    }
}

private fun contextAttributes(minor: Int): IntArray = if (minor == 0) {
    intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 3, EGL10.EGL_NONE)
} else {
    intArrayOf(
        EGL14.EGL_CONTEXT_CLIENT_VERSION,
        3,
        EGL_CONTEXT_MINOR_VERSION,
        minor,
        EGL10.EGL_NONE,
    )
}
