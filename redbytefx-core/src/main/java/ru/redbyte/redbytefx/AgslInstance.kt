package ru.redbyte.redbytefx

import android.graphics.RenderEffect
import android.graphics.RuntimeShader
import androidx.annotation.RequiresApi

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
    init {
        RedByteFxPlatform.requireAgslRuntime()
    }

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

            override fun setInt(name: String, value: Int) {
                shader.setIntUniform(name, value)
            }
        },
        onChanged = ::refresh,
    )

    public fun renderEffect(): RenderEffect {
        runtime.checkThread()
        return renderEffect
    }

    public fun set(uniform: Uniform<Flt<High>>, value: Float): Boolean = runtime.set(uniform, value)

    @JvmName("setMedFloat")
    public fun set(uniform: Uniform<Flt<Med>>, value: Float): Boolean = runtime.set(uniform, value)

    public fun set(uniform: Uniform<Vec2<Flt<High>>>, x: Float, y: Float): Boolean =
        runtime.set(uniform, x, y)

    @JvmName("setMedVec2")
    public fun set(uniform: Uniform<Vec2<Flt<Med>>>, x: Float, y: Float): Boolean =
        runtime.set(uniform, x, y)

    public fun set(uniform: Uniform<Vec3<Flt<High>>>, x: Float, y: Float, z: Float): Boolean =
        runtime.set(uniform, x, y, z)

    @JvmName("setMedVec3")
    public fun set(uniform: Uniform<Vec3<Flt<Med>>>, x: Float, y: Float, z: Float): Boolean =
        runtime.set(uniform, x, y, z)

    public fun set(uniform: Uniform<Vec4<Flt<High>>>, x: Float, y: Float, z: Float, w: Float): Boolean =
        runtime.set(uniform, x, y, z, w)

    @JvmName("setMedVec4")
    public fun set(uniform: Uniform<Vec4<Flt<Med>>>, x: Float, y: Float, z: Float, w: Float): Boolean =
        runtime.set(uniform, x, y, z, w)

    public fun set(uniform: Uniform<IntS>, value: Int): Boolean = runtime.set(uniform, value)

    public fun setResolution(widthPx: Float, heightPx: Float): Boolean =
        runtime.setResolution(widthPx, heightPx)

    public fun batch(block: () -> Unit) {
        runtime.batch(block)
    }

    private fun refresh() {
        renderEffect = RenderEffect.createRuntimeShaderEffect(shader, RB_INPUT_UNIFORM)
    }
}

@RequiresApi(RedByteFxApis.AGSL_MIN_SDK)
public fun ShaderProgram.newAgslInstance(): AgslInstance {
    check(target == ShaderTarget.Agsl) { "GLES programs are not executed as AGSL" }
    RedByteFxPlatform.requireAgslRuntime()
    return AgslInstance(this)
}
