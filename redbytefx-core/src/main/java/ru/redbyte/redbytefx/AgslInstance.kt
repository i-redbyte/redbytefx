package ru.redbyte.redbytefx

import android.graphics.RenderEffect
import android.graphics.RuntimeShader

/**
 * One AGSL runtime instance.
 *
 * [android.graphics.RuntimeShader] does not publish later uniform writes through an existing
 * [RenderEffect], so the effect is recreated after a real change. Call [renderEffect] and the
 * setters from the UI thread that owns the draw.
 */
public class AgslInstance internal constructor(
    program: ShaderProgram,
) {
    private val shader = RuntimeShader(program.agslSource())
    private lateinit var renderEffect: RenderEffect
    private val runtime = ShaderRuntime(
        program = program,
        writer = object : UniformWriter {
            override fun setFloat(name: String, value: Float) {
                shader.setFloatUniform(name, value)
            }

            override fun setFloat2(name: String, x: Float, y: Float) {
                shader.setFloatUniform(name, x, y)
            }

            override fun setFloat3(name: String, x: Float, y: Float, z: Float) {
                shader.setFloatUniform(name, x, y, z)
            }

            override fun setFloat4(name: String, x: Float, y: Float, z: Float, w: Float) {
                shader.setFloatUniform(name, x, y, z, w)
            }
        },
        onChanged = ::refresh,
    )

    public fun renderEffect(): RenderEffect = renderEffect

    public fun set(uniform: Uniform<Flt<High>>, value: Float): Boolean = runtime.set(uniform, value)

    public fun setResolution(widthPx: Float, heightPx: Float): Boolean =
        runtime.setResolution(widthPx, heightPx)

    public fun batch(block: () -> Unit) {
        runtime.batch(block)
    }

    private fun refresh() {
        renderEffect = RenderEffect.createRuntimeShaderEffect(shader, RB_INPUT_UNIFORM)
    }
}

public fun ShaderProgram.newAgslInstance(): AgslInstance {
    check(target == ShaderTarget.Agsl) { "GLES programs are not executed as AGSL" }
    return AgslInstance(this)
}
