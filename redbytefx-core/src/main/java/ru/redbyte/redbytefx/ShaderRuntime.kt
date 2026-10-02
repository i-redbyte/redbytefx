package ru.redbyte.redbytefx

internal interface UniformWriter {
    fun setFloat(name: String, value: Float)
    fun setFloat2(name: String, x: Float, y: Float)
    fun setFloat3(name: String, x: Float, y: Float, z: Float)
    fun setFloat4(name: String, x: Float, y: Float, z: Float, w: Float)
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
    private var resolutionWidth: Float? = null
    private var resolutionHeight: Float? = null

    init {
        batch {
            for (binding in program.bindings) {
                val default = binding.uniform.default ?: continue
                val shape = binding.uniform.shape
                if (shape is Shape.Scalar && shape.kind == ScalarKind.Float) {
                    setFloat(binding.uniform, default)
                }
            }
            setResolution(1f, 1f)
        }
    }

    internal fun set(uniform: Uniform<Flt<High>>, value: Float): Boolean = setFloat(uniform, value)

    internal fun setResolution(widthPx: Float, heightPx: Float): Boolean {
        val width = sanitizeResolution(widthPx)
        val height = sanitizeResolution(heightPx)
        val sameWidth = resolutionWidth?.let { sameFloatUniformValue(it, width) } == true
        val sameHeight = resolutionHeight?.let { sameFloatUniformValue(it, height) } == true
        if (sameWidth && sameHeight) return false
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

    private fun notifyChanged() {
        if (batchDepth > 0) {
            pending = true
        } else {
            onChanged()
        }
    }

    private fun sanitizeResolution(value: Float): Float = if (value > 0f) value else 1f
}
