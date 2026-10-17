package ru.redbyte.redbytefx.gl.compose

import java.lang.management.ManagementFactory

/** Bytes the current thread allocates while [block] runs, after one warm-up run. */
internal fun allocatedBytes(block: () -> Unit): Long {
    val threads = ManagementFactory.getThreadMXBean() as com.sun.management.ThreadMXBean
    block()
    val id = Thread.currentThread().id
    val before = threads.getThreadAllocatedBytes(id)
    block()
    return threads.getThreadAllocatedBytes(id) - before
}
