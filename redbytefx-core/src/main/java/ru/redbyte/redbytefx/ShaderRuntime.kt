package ru.redbyte.redbytefx

internal interface UniformWriter {
    fun setFloat(name: String, value: Float)
    fun setFloat2(name: String, x: Float, y: Float)
    fun setFloat3(name: String, x: Float, y: Float, z: Float)
    fun setFloat4(name: String, x: Float, y: Float, z: Float, w: Float)
    fun setInt(name: String, value: Int)
}

internal class UniformBinding(
    val uniform: Uniform<*>,
    val agslName: String,
)

/**
 * Mutable uniform cache for one compiled shader.
 *
 * Identical writes do not touch [writer] and do not ask the host to rebuild a render effect.
 * [batch] collapses several successful writes into one notification.
 */
internal class ShaderRuntime(
    private val program: ShaderProgram,
    private val writer: UniformWriter,
    private val onChanged: () -> Unit,
) {
    private var batchDepth = 0
    private var pending = false
    private val floatValues = java.util.IdentityHashMap<Uniform<*>, Float>()
    private val vectorValues = java.util.IdentityHashMap<Uniform<*>, FloatArray>()
    private val intValues = java.util.IdentityHashMap<Uniform<*>, Int>()
    private var resolutionWidth: Float? = null
    private var resolutionHeight: Float? = null

    init {
        batch {
            for (binding in program.bindings) {
                val shape = binding.uniform.shape
                val default = binding.uniform.default
                if (default != null && shape is Shape.Scalar && shape.kind == ScalarKind.Float) {
                    setFloat(binding.uniform, default)
                }
                val components = binding.uniform.components
                if (components != null && shape is Shape.Vector && shape.kind == ScalarKind.Float) {
                    writeVector(binding.uniform, components)
                }
                val intDefault = binding.uniform.intDefault
                if (intDefault != null && shape is Shape.Scalar && shape.kind == ScalarKind.Int) {
                    setInt(binding.uniform, intDefault)
                }
            }
            setResolution(1f, 1f)
        }
    }

    internal fun set(uniform: Uniform<Flt<High>>, value: Float): Boolean = setFloat(uniform, value)

    @JvmName("setMedFloat")
    internal fun set(uniform: Uniform<Flt<Med>>, value: Float): Boolean = setFloat(uniform, value)

    internal fun set(uniform: Uniform<Vec2<Flt<High>>>, x: Float, y: Float): Boolean {
        if (sameStored2(uniform, x, y)) return false
        return writeVector(uniform, floatArrayOf(x, y))
    }

    internal fun set(uniform: Uniform<Vec3<Flt<High>>>, x: Float, y: Float, z: Float): Boolean {
        if (sameStored3(uniform, x, y, z)) return false
        return writeVector(uniform, floatArrayOf(x, y, z))
    }

    internal fun set(uniform: Uniform<Vec4<Flt<High>>>, x: Float, y: Float, z: Float, w: Float): Boolean {
        if (sameStored4(uniform, x, y, z, w)) return false
        return writeVector(uniform, floatArrayOf(x, y, z, w))
    }

    @JvmName("setMedVec2")
    internal fun set(uniform: Uniform<Vec2<Flt<Med>>>, x: Float, y: Float): Boolean {
        if (sameStored2(uniform, x, y)) return false
        return writeVector(uniform, floatArrayOf(x, y))
    }

    @JvmName("setMedVec3")
    internal fun set(uniform: Uniform<Vec3<Flt<Med>>>, x: Float, y: Float, z: Float): Boolean {
        if (sameStored3(uniform, x, y, z)) return false
        return writeVector(uniform, floatArrayOf(x, y, z))
    }

    @JvmName("setMedVec4")
    internal fun set(uniform: Uniform<Vec4<Flt<Med>>>, x: Float, y: Float, z: Float, w: Float): Boolean {
        if (sameStored4(uniform, x, y, z, w)) return false
        return writeVector(uniform, floatArrayOf(x, y, z, w))
    }

    internal fun set(uniform: Uniform<IntS>, value: Int): Boolean = setInt(uniform, value)

    internal fun setResolution(widthPx: Float, heightPx: Float): Boolean {
        val width = sanitizeResolution(widthPx)
        val height = sanitizeResolution(heightPx)
        val handle = program.resolution
        if (handle != null) {
            return set(handle, width, height)
        }
        val previousWidth = resolutionWidth
        val previousHeight = resolutionHeight
        if (
            previousWidth != null &&
            previousHeight != null &&
            sameFloatUniformValue(previousWidth, width) &&
            sameFloatUniformValue(previousHeight, height)
        ) {
            return false
        }
        resolutionWidth = width
        resolutionHeight = height
        writer.setFloat2(RB_RESOLUTION_UNIFORM, width, height)
        notifyChanged()
        return true
    }

    internal fun batch(block: () -> Unit) {
        batchDepth += 1
        try {
            block()
        } finally {
            batchDepth -= 1
            if (batchDepth == 0 && pending) {
                pending = false
                onChanged()
            }
        }
    }

    private fun setFloat(uniform: Uniform<*>, value: Float): Boolean {
        val binding = program.binding(uniform)
        val previous = floatValues[uniform]
        if (previous != null && sameFloatUniformValue(previous, value)) return false
        floatValues[uniform] = value
        writer.setFloat(binding.agslName, value)
        notifyChanged()
        return true
    }

    private fun sameStored2(uniform: Uniform<*>, x: Float, y: Float): Boolean {
        val previous = vectorValues[uniform] ?: return false
        return previous.size == 2 &&
            sameFloatUniformValue(previous[0], x) &&
            sameFloatUniformValue(previous[1], y)
    }

    private fun sameStored3(uniform: Uniform<*>, x: Float, y: Float, z: Float): Boolean {
        val previous = vectorValues[uniform] ?: return false
        return previous.size == 3 &&
            sameFloatUniformValue(previous[0], x) &&
            sameFloatUniformValue(previous[1], y) &&
            sameFloatUniformValue(previous[2], z)
    }

    private fun sameStored4(uniform: Uniform<*>, x: Float, y: Float, z: Float, w: Float): Boolean {
        val previous = vectorValues[uniform] ?: return false
        return previous.size == 4 &&
            sameFloatUniformValue(previous[0], x) &&
            sameFloatUniformValue(previous[1], y) &&
            sameFloatUniformValue(previous[2], z) &&
            sameFloatUniformValue(previous[3], w)
    }

    private fun writeVector(uniform: Uniform<*>, value: FloatArray): Boolean {
        val binding = program.binding(uniform)
        val previous = vectorValues[uniform]
        if (previous != null && sameVector(previous, value)) return false
        val stored = value.copyOf()
        vectorValues[uniform] = stored
        when (stored.size) {
            2 -> writer.setFloat2(binding.agslName, stored[0], stored[1])
            3 -> writer.setFloat3(binding.agslName, stored[0], stored[1], stored[2])
            4 -> writer.setFloat4(binding.agslName, stored[0], stored[1], stored[2], stored[3])
            else -> error("Vector uniform width must be 2, 3, or 4")
        }
        notifyChanged()
        return true
    }

    private fun setInt(uniform: Uniform<*>, value: Int): Boolean {
        val binding = program.binding(uniform)
        val previous = intValues[uniform]
        if (previous != null && previous == value) return false
        intValues[uniform] = value
        writer.setInt(binding.agslName, value)
        notifyChanged()
        return true
    }

    private fun notifyChanged() {
        if (batchDepth > 0) {
            pending = true
        } else {
            onChanged()
        }
    }

    private fun sanitizeResolution(value: Float): Float = if (value > 0f) value else 1f
}

private fun sameVector(previous: FloatArray, value: FloatArray): Boolean {
    if (previous.size != value.size) return false
    for (index in previous.indices) {
        if (!sameFloatUniformValue(previous[index], value[index])) return false
    }
    return true
}
