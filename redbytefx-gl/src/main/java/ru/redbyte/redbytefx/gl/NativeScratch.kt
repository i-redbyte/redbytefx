package ru.redbyte.redbytefx.gl

import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Device. A reusable direct buffer for `glBufferData` uploads, which need native memory.
 *
 * Capacity at least doubles when it grows, so a buffer that grows a little every frame, such as a
 * particle or instance array, reallocates a logarithmic number of times instead of every upload.
 * Direct memory is reclaimed only by the garbage collector, so fewer allocations also mean less
 * native memory waiting to be freed.
 */
internal class NativeScratch {
    private var buffer: ByteBuffer? = null

    /** A cleared, native-order buffer whose limit is [bytes]. */
    fun acquire(bytes: Int): ByteBuffer {
        require(bytes >= 0) { "Scratch size must be non-negative, was $bytes" }
        val existing = buffer
        val target = if (existing != null && existing.capacity() >= bytes) {
            existing
        } else {
            val doubled = ((existing?.capacity() ?: 0).toLong() * 2).coerceAtMost(Int.MAX_VALUE.toLong()).toInt()
            val grown = maxOf(bytes, doubled)
            ByteBuffer.allocateDirect(grown).order(ByteOrder.nativeOrder()).also { buffer = it }
        }
        target.clear()
        target.limit(bytes)
        return target
    }

    fun release() {
        buffer = null
    }
}
