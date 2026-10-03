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
import ru.redbyte.redbytefx.Vec3
import ru.redbyte.redbytefx.Vec4
import kotlin.jvm.JvmName
import ru.redbyte.redbytefx.gl.GlProgramRuntime
import java.util.IdentityHashMap

private const val MAX_QUEUED_GL_TASKS = 128

/**
 * GLES uniform writer for one [ShaderProgram] rendered by [GlSurface].
 *
 * [set] keeps the latest value per uniform and runs it on the GL thread. Writes that happen
 * before the surface has linked are applied after link, not dropped.
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
    @Volatile
    internal var glQueue: ((() -> Unit) -> Unit)? = null
    @Volatile
    internal var runtime: GlProgramRuntime? = null
    private val lock = Any()
    private val uniformWrites = IdentityHashMap<Uniform<*>, () -> Unit>()
    private val tasks = ArrayDeque<() -> Unit>()

    /**
     * Runs [block] on the GL thread.
     *
     * If the surface is not linked yet, [block] waits until link and then runs. The queue keeps at
     * most [MAX_QUEUED_GL_TASKS] arbitrary blocks; uniform [set] calls are coalesced separately.
     */
    public fun runOnGl(block: () -> Unit) {
        synchronized(lock) {
            if (tasks.size >= MAX_QUEUED_GL_TASKS) {
                tasks.removeFirst()
            }
            tasks.addLast(block)
        }
        scheduleDrain()
    }

    internal fun scheduleDrain() {
        glQueue?.invoke { drainPending() }
    }

    /** Applies queued uniform writes and [runOnGl] blocks. Must run on the GL thread. */
    internal fun drainPending() {
        while (true) {
            val batch = synchronized(lock) {
                if (runtime == null || (uniformWrites.isEmpty() && tasks.isEmpty())) {
                    return
                }
                val ready = ArrayList<() -> Unit>(uniformWrites.size + tasks.size)
                ready.addAll(uniformWrites.values)
                ready.addAll(tasks)
                uniformWrites.clear()
                tasks.clear()
                ready
            }
            batch.forEach { it() }
        }
    }

    private fun enqueueUniform(uniform: Uniform<*>, block: () -> Unit) {
        synchronized(lock) {
            uniformWrites[uniform] = block
        }
        scheduleDrain()
    }

    @JvmName("setHighFloat")
    public fun set(uniform: Uniform<Flt<High>>, value: Float) {
        enqueueUniform(uniform) {
            val active = runtime ?: return@enqueueUniform
            active.use()
            active.set(uniform, value)
        }
    }

    @JvmName("setMedFloat")
    public fun set(uniform: Uniform<Flt<Med>>, value: Float) {
        enqueueUniform(uniform) {
            val active = runtime ?: return@enqueueUniform
            active.use()
            active.set(uniform, value)
        }
    }

    public fun set(uniform: Uniform<Vec2<Flt<High>>>, x: Float, y: Float) {
        enqueueUniform(uniform) {
            val active = runtime ?: return@enqueueUniform
            active.use()
            active.set(uniform, x, y)
        }
    }

    @JvmName("setMedVec2")
    public fun set(uniform: Uniform<Vec2<Flt<Med>>>, x: Float, y: Float) {
        enqueueUniform(uniform) {
            val active = runtime ?: return@enqueueUniform
            active.use()
            active.set(uniform, x, y)
        }
    }

    public fun set(uniform: Uniform<Vec3<Flt<High>>>, x: Float, y: Float, z: Float) {
        enqueueUniform(uniform) {
            val active = runtime ?: return@enqueueUniform
            active.use()
            active.set(uniform, x, y, z)
        }
    }

    @JvmName("setMedVec3")
    public fun set(uniform: Uniform<Vec3<Flt<Med>>>, x: Float, y: Float, z: Float) {
        enqueueUniform(uniform) {
            val active = runtime ?: return@enqueueUniform
            active.use()
            active.set(uniform, x, y, z)
        }
    }

    public fun set(uniform: Uniform<Vec4<Flt<High>>>, x: Float, y: Float, z: Float, w: Float) {
        enqueueUniform(uniform) {
            val active = runtime ?: return@enqueueUniform
            active.use()
            active.set(uniform, x, y, z, w)
        }
    }

    @JvmName("setMedVec4")
    public fun set(uniform: Uniform<Vec4<Flt<Med>>>, x: Float, y: Float, z: Float, w: Float) {
        enqueueUniform(uniform) {
            val active = runtime ?: return@enqueueUniform
            active.use()
            active.set(uniform, x, y, z, w)
        }
    }

    public fun set(uniform: Uniform<IntS>, value: Int) {
        enqueueUniform(uniform) {
            val active = runtime ?: return@enqueueUniform
            active.use()
            active.set(uniform, value)
        }
    }

    public fun set(uniform: Uniform<BoolS>, value: Boolean) {
        enqueueUniform(uniform) {
            val active = runtime ?: return@enqueueUniform
            active.use()
            active.set(uniform, value)
        }
    }

    @JvmName("bindSampler2D")
    public fun bind(uniform: Uniform<Sampler2D>, texture: Int) {
        enqueueUniform(uniform) {
            val active = runtime ?: return@enqueueUniform
            active.use()
            active.bind(uniform, texture)
        }
    }

    @JvmName("bindSamplerCube")
    public fun bind(uniform: Uniform<SamplerCube>, texture: Int) {
        enqueueUniform(uniform) {
            val active = runtime ?: return@enqueueUniform
            active.use()
            active.bind(uniform, texture)
        }
    }

    public fun setMat2(uniform: Uniform<Mat2>, values: FloatArray) {
        enqueueUniform(uniform) {
            val active = runtime ?: return@enqueueUniform
            active.use()
            active.set(uniform, values)
        }
    }

    public fun setMat3(uniform: Uniform<Mat3>, values: FloatArray) {
        enqueueUniform(uniform) {
            val active = runtime ?: return@enqueueUniform
            active.use()
            active.set(uniform, values)
        }
    }

    public fun setMat4(uniform: Uniform<Mat4>, values: FloatArray) {
        enqueueUniform(uniform) {
            val active = runtime ?: return@enqueueUniform
            active.use()
            active.set(uniform, values)
        }
    }
}
