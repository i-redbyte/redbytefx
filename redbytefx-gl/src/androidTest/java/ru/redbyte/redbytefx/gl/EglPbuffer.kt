package ru.redbyte.redbytefx.gl

import android.opengl.EGL14
import android.opengl.EGLExt

internal class EglPbuffer(
    private val minors: IntArray = es3ContextMinors(),
) : AutoCloseable {
    private val display = EGL14.eglGetDisplay(EGL14.EGL_DEFAULT_DISPLAY)
    private val context: android.opengl.EGLContext
    private val surface: android.opengl.EGLSurface

    init {
        check(display != EGL14.EGL_NO_DISPLAY) { "No EGL display, error ${EGL14.eglGetError()}" }
        val version = IntArray(2)
        check(EGL14.eglInitialize(display, version, 0, version, 1)) {
            "eglInitialize failed, error ${EGL14.eglGetError()}"
        }
        val configAttribs = intArrayOf(
            EGL14.EGL_RENDERABLE_TYPE, EGLExt.EGL_OPENGL_ES3_BIT_KHR,
            EGL14.EGL_SURFACE_TYPE, EGL14.EGL_PBUFFER_BIT,
            EGL14.EGL_RED_SIZE, 8,
            EGL14.EGL_GREEN_SIZE, 8,
            EGL14.EGL_BLUE_SIZE, 8,
            EGL14.EGL_ALPHA_SIZE, 8,
            EGL14.EGL_NONE,
        )
        val configs = arrayOfNulls<android.opengl.EGLConfig>(1)
        val count = IntArray(1)
        check(EGL14.eglChooseConfig(display, configAttribs, 0, configs, 0, 1, count, 0) && count[0] > 0) {
            "No OpenGL ES 3 config, error ${EGL14.eglGetError()}"
        }
        val config = configs[0]
        var created: android.opengl.EGLContext? = null
        for (minor in minors) {
            created = createEsContext(config, minor.takeIf { it > 0 })
            if (created != null) break
        }
        context = checkNotNull(created) {
            "eglCreateContext failed, error ${EGL14.eglGetError()}"
        }
        val surfaceAttribs = intArrayOf(EGL14.EGL_WIDTH, 1, EGL14.EGL_HEIGHT, 1, EGL14.EGL_NONE)
        surface = EGL14.eglCreatePbufferSurface(display, configs[0], surfaceAttribs, 0)
        check(surface != EGL14.EGL_NO_SURFACE) { "eglCreatePbufferSurface failed, error ${EGL14.eglGetError()}" }
        check(EGL14.eglMakeCurrent(display, surface, surface, context)) {
            "eglMakeCurrent failed, error ${EGL14.eglGetError()}"
        }
    }

    private fun createEsContext(config: android.opengl.EGLConfig?, minorVersion: Int?): android.opengl.EGLContext? {
        val attribs = if (minorVersion == null) {
            intArrayOf(EGL14.EGL_CONTEXT_CLIENT_VERSION, 3, EGL14.EGL_NONE)
        } else {
            intArrayOf(
                EGL14.EGL_CONTEXT_CLIENT_VERSION, 3,
                EGL_CONTEXT_MINOR_VERSION, minorVersion,
                EGL14.EGL_NONE,
            )
        }
        val created = EGL14.eglCreateContext(display, config, EGL14.EGL_NO_CONTEXT, attribs, 0)
        return if (created == EGL14.EGL_NO_CONTEXT) null else created
    }

    private companion object {
        const val EGL_CONTEXT_MINOR_VERSION = 0x30FB
    }

    override fun close() {
        EGL14.eglMakeCurrent(display, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_SURFACE, EGL14.EGL_NO_CONTEXT)
        EGL14.eglDestroySurface(display, surface)
        EGL14.eglDestroyContext(display, context)
        EGL14.eglTerminate(display)
    }
}
