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
                    when (components.size) {
                        2 -> setVector2(binding.uniform, components[0], components[1])
                        3 -> setVector3(binding.uniform, components[0], components[1], components[2])
                        4 -> setVector4(binding.uniform, components[0], components[1], components[2], components[3])
                        else -> error("Vector uniform width must be 2, 3, or 4")
                    }
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
        return setVector2(uniform, x, y)
    }

    internal fun set(uniform: Uniform<Vec3<Flt<High>>>, x: Float, y: Float, z: Float): Boolean {
        return setVector3(uniform, x, y, z)
    }

    internal fun set(uniform: Uniform<Vec4<Flt<High>>>, x: Float, y: Float, z: Float, w: Float): Boolean {
        return setVector4(uniform, x, y, z, w)
    }

    @JvmName("setMedVec2")
    internal fun set(uniform: Uniform<Vec2<Flt<Med>>>, x: Float, y: Float): Boolean {
        return setVector2(uniform, x, y)
    }

    @JvmName("setMedVec3")
    internal fun set(uniform: Uniform<Vec3<Flt<Med>>>, x: Float, y: Float, z: Float): Boolean {
        return setVector3(uniform, x, y, z)
    }

    @JvmName("setMedVec4")
    internal fun set(uniform: Uniform<Vec4<Flt<Med>>>, x: Float, y: Float, z: Float, w: Float): Boolean {
        return setVector4(uniform, x, y, z, w)
    }

    internal fun set(uniform: Uniform<IntS>, value: Int): Boolean = setInt(uniform, value)

    internal fun setResolution(widthPx: Float, heightPx: Float): Boolean {
        val handle = program.resolution ?: return false
        return set(
            handle,
            sanitizeResolution(widthPx),
            sanitizeResolution(heightPx),
        )
    }

    internal fun batch(block: () -> Unit) {
        batchDepth += 1
        try {
            block()
        } finally {
            batchDepth -= 1
            flushPending()
        }
    }

    private fun setFloat(uniform: Uniform<*>, value: Float): Boolean {
        val binding = program.binding(uniform)
        val previous = floatValues[uniform]
        if (previous != null && sameFloatUniformValue(previous, value)) return flushPending()
        writer.setFloat(binding.agslName, value)
        floatValues[uniform] = value
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

    private fun setVector2(uniform: Uniform<*>, x: Float, y: Float): Boolean {
        if (sameStored2(uniform, x, y)) return flushPending()
        val binding = program.binding(uniform)
        val previous = vectorValues[uniform]
        writer.setFloat2(binding.agslName, x, y)
        val stored = previous ?: FloatArray(2).also { vectorValues[uniform] = it }
        stored[0] = x
        stored[1] = y
        notifyChanged()
        return true
    }

    private fun setVector3(uniform: Uniform<*>, x: Float, y: Float, z: Float): Boolean {
        if (sameStored3(uniform, x, y, z)) return flushPending()
        val binding = program.binding(uniform)
        val previous = vectorValues[uniform]
        writer.setFloat3(binding.agslName, x, y, z)
        val stored = previous ?: FloatArray(3).also { vectorValues[uniform] = it }
        stored[0] = x
        stored[1] = y
        stored[2] = z
        notifyChanged()
        return true
    }

    private fun setVector4(uniform: Uniform<*>, x: Float, y: Float, z: Float, w: Float): Boolean {
        if (sameStored4(uniform, x, y, z, w)) return flushPending()
        val binding = program.binding(uniform)
        val previous = vectorValues[uniform]
        writer.setFloat4(binding.agslName, x, y, z, w)
        val stored = previous ?: FloatArray(4).also { vectorValues[uniform] = it }
        stored[0] = x
        stored[1] = y
        stored[2] = z
        stored[3] = w
        notifyChanged()
        return true
    }

    private fun setInt(uniform: Uniform<*>, value: Int): Boolean {
        val binding = program.binding(uniform)
        val previous = intValues[uniform]
        if (previous != null && previous == value) return flushPending()
        writer.setInt(binding.agslName, value)
        intValues[uniform] = value
        notifyChanged()
        return true
    }

    private fun notifyChanged() {
        pending = true
        flushPending()
    }

    private fun flushPending(): Boolean {
        if (!pending) return false
        if (batchDepth > 0) return true
        onChanged()
        pending = false
        return true
    }

    private fun sanitizeResolution(value: Float): Float = if (value > 0f) value else 1f
}
