package ru.redbyte.redbytefx.gl

/**
 * Upper bound on `glGetError` reads per drain. GLES has one flag per error kind, so a real queue
 * empties in a few reads; a lost robust context can keep returning `GL_CONTEXT_LOST`.
 */
internal const val MAX_GL_ERRORS_PER_DRAIN: Int = 16

/**
 * Reads [nextError] until it returns 0 or [MAX_GL_ERRORS_PER_DRAIN] reads have been made.
 * Each error is passed to [onError]. Returns the first error, or 0 when there was none.
 */
internal inline fun drainGlErrors(nextError: () -> Int, onError: (Int) -> Unit): Int {
    var first = 0
    var reads = 0
    while (reads < MAX_GL_ERRORS_PER_DRAIN) {
        val error = nextError()
        reads += 1
        if (error == 0) break
        if (first == 0) first = error
        onError(error)
    }
    return first
}
