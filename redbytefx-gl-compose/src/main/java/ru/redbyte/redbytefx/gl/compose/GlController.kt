package ru.redbyte.redbytefx.gl.compose

import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import ru.redbyte.redbytefx.BoolS
import ru.redbyte.redbytefx.Flt
import ru.redbyte.redbytefx.High
import ru.redbyte.redbytefx.IntS
import ru.redbyte.redbytefx.Mat2
import ru.redbyte.redbytefx.Mat3
import ru.redbyte.redbytefx.Mat4
import ru.redbyte.redbytefx.Med
import ru.redbyte.redbytefx.Sampler2D
import ru.redbyte.redbytefx.SamplerCube
import ru.redbyte.redbytefx.ShaderProgram
import ru.redbyte.redbytefx.Uniform
import ru.redbyte.redbytefx.Vec2
import kotlin.jvm.JvmName
import ru.redbyte.redbytefx.gl.GlProgramRuntime

/**
 * GLES uniform writer for one [ShaderProgram] rendered by [GlSurface].
 *
 * All [set] calls are queued to the GL thread once the surface is attached.
 */
@Stable
public class GlController internal constructor(
    public val program: ShaderProgram,
    public val config: GlSurfaceConfig,
) {
    private val linkStateHolder = mutableStateOf<GlLinkState>(GlLinkState.Pending)
    public val linkState: State<GlLinkState> get() = linkStateHolder
    internal var linkStateValue: GlLinkState
        get() = linkStateHolder.value
        set(value) {
            linkStateHolder.value = value
        }
    internal var glQueue: ((() -> Unit) -> Unit)? = null
    internal var runtime: GlProgramRuntime? = null

    public fun runOnGl(block: () -> Unit) {
        val queue = glQueue
        if (queue != null) {
            queue(block)
        }
    }

    @JvmName("setHighFloat")
    public fun set(uniform: Uniform<Flt<High>>, value: Float) {
        runOnGl {
            val active = runtime ?: return@runOnGl
            active.use()
            active.set(uniform, value)
        }
    }

    @JvmName("setMedFloat")
    public fun set(uniform: Uniform<Flt<Med>>, value: Float) {
        runOnGl {
            val active = runtime ?: return@runOnGl
            active.use()
            active.set(uniform, value)
        }
    }

    public fun set(uniform: Uniform<Vec2<Flt<High>>>, x: Float, y: Float) {
        runOnGl {
            val active = runtime ?: return@runOnGl
            active.use()
            active.set(uniform, x, y)
        }
    }

    public fun set(uniform: Uniform<IntS>, value: Int) {
        runOnGl {
            val active = runtime ?: return@runOnGl
            active.use()
            active.set(uniform, value)
        }
    }

    public fun set(uniform: Uniform<BoolS>, value: Boolean) {
        runOnGl {
            val active = runtime ?: return@runOnGl
            active.use()
            active.set(uniform, value)
        }
    }

    @JvmName("bindSampler2D")
    public fun bind(uniform: Uniform<Sampler2D>, texture: Int) {
        runOnGl {
            val active = runtime ?: return@runOnGl
            active.use()
            active.bind(uniform, texture)
        }
    }

    @JvmName("bindSamplerCube")
    public fun bind(uniform: Uniform<SamplerCube>, texture: Int) {
        runOnGl {
            val active = runtime ?: return@runOnGl
            active.use()
            active.bind(uniform, texture)
        }
    }

    public fun setMat2(uniform: Uniform<Mat2>, values: FloatArray) {
        runOnGl {
            val active = runtime ?: return@runOnGl
            active.use()
            active.set(uniform, values)
        }
    }

    public fun setMat3(uniform: Uniform<Mat3>, values: FloatArray) {
        runOnGl {
            val active = runtime ?: return@runOnGl
            active.use()
            active.set(uniform, values)
        }
    }

    public fun setMat4(uniform: Uniform<Mat4>, values: FloatArray) {
        runOnGl {
            val active = runtime ?: return@runOnGl
            active.use()
            active.set(uniform, values)
        }
    }
}
