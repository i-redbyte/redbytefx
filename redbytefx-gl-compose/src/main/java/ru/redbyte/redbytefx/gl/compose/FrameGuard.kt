package ru.redbyte.redbytefx.gl.compose

/**
 * GL thread. Keeps an exception from a frame or a link off the GL thread's uncaught handler.
 *
 * The first exception is reported and halts rendering, so a persistent fault is not reported every
 * frame. [resume] runs again on a fresh EGL context. [Error]s such as out-of-memory are rethrown.
 */
internal class FrameGuard(
    private val report: (Throwable) -> Unit,
) {
    var halted: Boolean = false
        private set

    /** Runs [block] unless halted; returns whether it completed. */
    @Suppress("TooGenericExceptionCaught")
    inline fun run(block: () -> Unit): Boolean {
        if (halted) return false
        try {
            block()
            return true
        } catch (error: Exception) {
            halt(error)
            return false
        }
    }

    fun resume() {
        halted = false
    }

    @PublishedApi
    internal fun halt(error: Exception) {
        halted = true
        report(error)
    }
}
