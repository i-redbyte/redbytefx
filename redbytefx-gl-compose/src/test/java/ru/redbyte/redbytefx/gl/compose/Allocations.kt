package ru.redbyte.redbytefx.gl.compose

import java.lang.management.ManagementFactory

/**
 * Bytes the current thread allocates while [block] runs. Three warm-up runs come first so the JIT
 * has compiled the path; callers compare the result with their call count, so a few bytes of
 * runtime noise never fail a test while one object per call always does.
 */
internal fun allocatedBytes(block: () -> Unit): Long {
    val threads = ManagementFactory.getThreadMXBean() as com.sun.management.ThreadMXBean
    repeat(WARM_UP_RUNS) { block() }
    val before = threads.currentThreadAllocatedBytes
    block()
    return threads.currentThreadAllocatedBytes - before
}

private const val WARM_UP_RUNS = 3
