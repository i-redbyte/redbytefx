package ru.redbyte.redbytefx.gl

import ru.redbyte.redbytefx.Uniform
import ru.redbyte.redbytefx.sameFloatUniformValue
import java.util.IdentityHashMap

/**
 * Device. Last value written to each uniform of one linked program.
 *
 * A write equal to the cached value, compared bit for bit, does not reach the driver. A write that
 * does reach it first makes the program current through [makeCurrent].
 */
internal class GlUniformValues(
    private val device: GlDevice,
    private val makeCurrent: () -> Unit,
) {
    private class FloatCache(var value: Float)

    private class IntCache(var value: Int)

    private val floats = IdentityHashMap<Uniform<*>, FloatCache>()
    private val arrays = IdentityHashMap<Uniform<*>, FloatArray>()
    private val ints = IdentityHashMap<Uniform<*>, IntCache>()

    fun writeFloat(uniform: Uniform<*>, location: Int, value: Float): Boolean {
        val previous = floats[uniform]
        if (previous != null && sameFloatUniformValue(previous.value, value)) return false
        makeCurrent()
        device.uniform1f(location, value)
        if (previous == null) floats[uniform] = FloatCache(value) else previous.value = value
        return true
    }

    fun writeVector(uniform: Uniform<*>, location: Int, value: FloatArray): Boolean = writeVector(
        uniform,
        location,
        value.size,
        value[0],
        value[1],
        value.getOrElse(2) { 0f },
        value.getOrElse(3) { 0f },
    )

    @Suppress("LongParameterList")
    fun writeVector(uniform: Uniform<*>, location: Int, width: Int, x: Float, y: Float, z: Float, w: Float): Boolean {
        require(width in 2..4) { "Vector uniform width must be 2, 3, or 4" }
        val previous = arrays[uniform]
        if (previous != null && previous.size == width && sameComponents(previous, x, y, z, w)) return false
        makeCurrent()
        when (width) {
            2 -> device.uniform2f(location, x, y)
            3 -> device.uniform3f(location, x, y, z)
            else -> device.uniform4f(location, x, y, z, w)
        }
        val cached = if (previous != null && previous.size == width) previous else FloatArray(width)
        cached[0] = x
        cached[1] = y
        if (width > 2) cached[2] = z
        if (width > 3) cached[3] = w
        if (previous !== cached) arrays[uniform] = cached
        return true
    }

    fun writeInt(uniform: Uniform<*>, location: Int, value: Int): Boolean {
        val previous = ints[uniform]
        if (previous != null && previous.value == value) return false
        makeCurrent()
        device.uniform1i(location, value)
        if (previous == null) ints[uniform] = IntCache(value) else previous.value = value
        return true
    }

    fun writeMatrix(uniform: Uniform<*>, location: Int, values: FloatArray): Boolean {
        val previous = arrays[uniform]
        if (previous != null && sameVector(previous, values)) return false
        makeCurrent()
        when (values.size) {
            4 -> device.uniformMatrix2fv(location, values)
            9 -> device.uniformMatrix3fv(location, values)
            16 -> device.uniformMatrix4fv(location, values)
            else -> error("Matrix uniform width must be 4, 9, or 16")
        }
        val cached = if (previous != null && previous.size == values.size) previous else FloatArray(values.size)
        values.copyInto(cached)
        if (previous !== cached) arrays[uniform] = cached
        return true
    }

    fun clear() {
        floats.clear()
        arrays.clear()
        ints.clear()
    }
}

private fun sameComponents(previous: FloatArray, x: Float, y: Float, z: Float, w: Float): Boolean {
    if (!sameFloatUniformValue(previous[0], x) || !sameFloatUniformValue(previous[1], y)) return false
    if (previous.size > 2 && !sameFloatUniformValue(previous[2], z)) return false
    return previous.size <= 3 || sameFloatUniformValue(previous[3], w)
}

private fun sameVector(previous: FloatArray, value: FloatArray): Boolean {
    if (previous.size != value.size) return false
    for (index in previous.indices) {
        if (!sameFloatUniformValue(previous[index], value[index])) return false
    }
    return true
}
