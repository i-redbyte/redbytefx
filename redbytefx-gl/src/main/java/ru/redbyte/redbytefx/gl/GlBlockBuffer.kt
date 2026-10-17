package ru.redbyte.redbytefx.gl

import ru.redbyte.redbytefx.StorageBlock
import ru.redbyte.redbytefx.UniformBlock
import ru.redbyte.redbytefx.packStd140Into
import ru.redbyte.redbytefx.packStd430Into

/**
 * One uniform or storage block buffer at [binding].
 *
 * The binding point is context state, so another program that uses the same point replaces it.
 * [bind] must run each time the owning program is made current, not only after a write.
 */
internal class GlBlockBuffer(
    private val device: GlDevice,
    private val storage: Boolean,
    private val binding: Int,
) {
    var name: Int = 0
        private set
    private var packing: ByteArray? = null
    private var storedSize: Int = 0
    private var floats: FloatArray? = null

    /** The packing buffer for [size] bytes, reused while the size stays the same. */
    fun writableBytes(size: Int): ByteArray {
        val available = packing
        return if (available != null && available.size == size) available else ByteArray(size).also { packing = it }
    }

    fun writeStd140(block: UniformBlock, values: FloatArray): Boolean {
        require(values.size == block.floatCount) {
            "Uniform block \"${block.name}\" expects ${block.floatCount} floats, was ${values.size}"
        }
        val packed = writableBytes(block.byteSize)
        packStd140Into(block, values, packed)
        return write(packed)
    }

    fun writeStd430(block: StorageBlock, values: FloatArray): Boolean {
        val packed = writableBytes(block.byteSize(values.size))
        packStd430Into(block, values, packed)
        return write(packed)
    }

    /**
     * True when [values] differ from the last upload the device accepted. This float comparison is
     * the only change check: equal floats always pack to equal bytes.
     */
    fun pending(values: FloatArray): Boolean {
        val previous = floats
        return previous == null || !previous.contentEquals(values)
    }

    /** Records [values] after packing and the device write have both succeeded. */
    fun remember(values: FloatArray) {
        val previous = floats
        floats = if (previous != null && previous.size == values.size) {
            values.copyInto(previous)
        } else {
            values.copyOf()
        }
    }

    /** A driver error means the bytes may not have reached the GPU; the next write is a full one. */
    fun invalidate() {
        storedSize = 0
        floats = null
    }

    /** Uploads [packed]; a first upload or a size change reallocates the store. */
    fun write(packed: ByteArray): Boolean {
        val full = name == 0 || storedSize != packed.size
        if (name == 0) {
            name = device.createBuffer()
            require(name != 0) { "Driver returned no buffer name" }
        }
        upload(packed, full)
        storedSize = packed.size
        return true
    }

    fun storedBytes(): Int = storedSize

    fun storedFloats(): Int = floats?.size ?: 0

    fun bind() {
        if (name == 0) return
        if (storage) device.bindShaderStorageBase(name, binding) else device.bindUniformBufferBase(name, binding)
    }

    /**
     * Deletes the buffer. `glDeleteBuffers` already unbinds it wherever it is bound, so the binding
     * point, which another program on the context may now use, is left alone.
     */
    fun delete() {
        if (name != 0) {
            device.deleteBuffer(name)
            name = 0
        }
        packing = null
        storedSize = 0
        floats = null
    }

    private fun upload(packed: ByteArray, full: Boolean) {
        when {
            storage && full -> device.shaderStorageData(name, packed)
            storage -> device.shaderStorageSubData(name, packed)
            full -> device.uniformBufferData(name, packed)
            else -> device.uniformBufferSubData(name, packed)
        }
    }
}
