package ru.redbyte.redbytefx.gl

/**
 * One uniform or storage block buffer at binding point 0.
 *
 * The binding point is context state, so a second program with its own block replaces it.
 * [bind] must run each time the owning program is made current, not only after a write.
 */
internal class GlBlockBuffer(
    private val device: GlDevice,
    private val storage: Boolean,
) {
    var name: Int = 0
        private set
    private var bytes: ByteArray? = null
    private var floats: FloatArray? = null

    /** True when [values] differ from the last upload the device accepted. */
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

    /** Uploads [packed] unless it equals the bytes already in the buffer. */
    fun write(packed: ByteArray): Boolean {
        val previous = bytes
        if (previous != null && previous.contentEquals(packed)) return false
        val full = name == 0 || previous == null || previous.size != packed.size
        if (name == 0) {
            name = device.createBuffer()
            require(name != 0) { "Driver returned no buffer name" }
        }
        upload(packed, full)
        bytes = packed
        return true
    }

    fun storedBytes(): Int = bytes?.size ?: 0

    fun storedFloats(): Int = floats?.size ?: 0

    fun bind() {
        if (name == 0) return
        if (storage) device.bindShaderStorageBase(name, 0) else device.bindUniformBufferBase(name, 0)
    }

    fun delete() {
        if (name != 0) {
            if (storage) device.bindShaderStorageBase(0, 0) else device.bindUniformBufferBase(0, 0)
            device.deleteBuffer(name)
            name = 0
        }
        bytes = null
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
