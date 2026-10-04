package ru.redbyte.redbytefx.gl

/**
 * Minor versions tried when an instrumented ES 3 context is opened.
 *
 * 2 is OpenGL ES 3.2, 1 is 3.1, and 0 omits `EGL_CONTEXT_MINOR_VERSION` (ES 3.0).
 * Asking for 3.1 first hides geometry and tessellation on a 3.2 driver.
 */
internal fun es3ContextMinors(): IntArray = intArrayOf(2, 1, 0)
