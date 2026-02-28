package ru.redbyte.redbytefx

import android.graphics.RenderEffect
import android.graphics.RuntimeShader

internal sealed interface DefaultValue {
    data class F(val v: Float) : DefaultValue
    data class F2(val x: Float, val y: Float) : DefaultValue
}

internal data class FxGraph(
    val ops: List<Op>,
    val defaults: Map<FxParam, DefaultValue>
)

internal data class FxProgram(
    val agsl: String,
    val layout: UniformLayout,
    val defaults: Map<FxParam, DefaultValue>
)

internal class UniformLayout {
    private var fIndex = 0
    private var v2Index = 0

    val floatUniforms = LinkedHashMap<FxParam.Float, String>()
    val float2Uniforms = LinkedHashMap<FxParam.Float2, String>()

    fun float(p: FxParam.Float): String = floatUniforms.getOrPut(p) { "uF${fIndex++}" }
    fun float2(p: FxParam.Float2): String = float2Uniforms.getOrPut(p) { "uV${v2Index++}" }
}

internal object FxBuilder {
    fun build(block: FxDsl.() -> Unit): FxEffect {
        val ops = mutableListOf<Op>()
        val defaults = LinkedHashMap<FxParam, DefaultValue>()
        val dsl = FxDsl(ops, defaults)
        dsl.block()

        val graph = FxGraph(
            ops = ops.toList(),
            defaults = defaults.toMap()
        )

        val program = FxCompiler.compile(graph)
        return FxEffectImpl(program)
    }
}

internal class FxEffectImpl(
    private val program: FxProgram
) : FxEffect {
    override fun newInstance(): FxInstance = FxInstanceImpl(program)
}

internal class FxInstanceImpl(
    private val program: FxProgram
) : FxInstance {

    private val shader = RuntimeShader(program.agsl)

    init {
        applyDefaults()
        setResolution(1f, 1f)
    }

    override fun renderEffect(): RenderEffect =
        RenderEffect.createRuntimeShaderEffect(shader, RB_INPUT_UNIFORM)

    override fun setFloat(param: FxParam.Float, value: Float) {
        val name = program.layout.floatUniforms[param] ?: error("Unknown param")
        shader.setFloatUniform(name, value)
    }

    override fun setFloat2(param: FxParam.Float2, x: Float, y: Float) {
        val name = program.layout.float2Uniforms[param] ?: error("Unknown param")
        shader.setFloatUniform(name, x, y)
    }

    override fun setResolution(widthPx: Float, heightPx: Float) {
        val w = if (widthPx > 0f) widthPx else 1f
        val h = if (heightPx > 0f) heightPx else 1f
        shader.setFloatUniform(RB_RESOLUTION_UNIFORM, w, h)
    }

    private fun applyDefaults() {
        for ((param, dv) in program.defaults) {
            when (param) {
                is FxParam.Float -> {
                    val name = program.layout.floatUniforms[param] ?: continue
                    val v = (dv as? DefaultValue.F)?.v ?: continue
                    shader.setFloatUniform(name, v)
                }
                is FxParam.Float2 -> {
                    val name = program.layout.float2Uniforms[param] ?: continue
                    val v = dv as? DefaultValue.F2 ?: continue
                    shader.setFloatUniform(name, v.x, v.y)
                }
            }
        }
    }
}