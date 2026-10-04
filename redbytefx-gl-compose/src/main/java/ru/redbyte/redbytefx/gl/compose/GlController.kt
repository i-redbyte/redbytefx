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
import ru.redbyte.redbytefx.StorageBlock
import ru.redbyte.redbytefx.Uniform
import ru.redbyte.redbytefx.UniformBlock
import ru.redbyte.redbytefx.Vec2
import ru.redbyte.redbytefx.Vec3
import ru.redbyte.redbytefx.Vec4
import kotlin.jvm.JvmName
import ru.redbyte.redbytefx.gl.GlProgramRuntime
import java.util.IdentityHashMap
import java.util.concurrent.atomic.AtomicBoolean

private const val MAX_QUEUED_GL_TASKS = 128

private typealias UniformWrite = (GlProgramRuntime) -> Unit

/**
 * GLES uniform writer for one [ShaderProgram] rendered by [GlSurface].
 *
 * [set] keeps the latest value per uniform and runs it on the GL thread. Writes that happen
 * before the surface has linked are applied after link, not dropped. When the EGL context is
 * recreated, the latest value of every [set] uniform is written to the new program. A uniform
 * block and a storage block are retained the same way. A texture name from [bind] belongs to one
 * context, so it is not replayed: bind again after re-uploading. [dispatch] and [read] run once
 * on the GL thread. [read] returns immediately and fills the caller's buffer from that queue.
 * [runOnGl] blocks run once.
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
    private var glQueue: ((() -> Unit) -> Unit)? = null

    @Volatile
    internal var runtime: GlProgramRuntime? = null
        private set
    private val lock = Any()
    private val latest = IdentityHashMap<Uniform<*>, UniformWrite>()
    private val pending = IdentityHashMap<Uniform<*>, UniformWrite>()
    private val uniformBlockWrites = CoalescedWrites<UniformBlock>()
    private val storageBlockWrites = CoalescedWrites<StorageBlock>()
    private val tasks = ArrayDeque<() -> Unit>()
    private val drainQueued = AtomicBoolean(false)
    private val drainTask: () -> Unit = {
        drainQueued.set(false)
        drainPending()
    }

    /**
     * Runs [block] on the GL thread.
     *
     * If the surface is not linked yet, [block] waits until link and then runs. The queue holds at
     * most [MAX_QUEUED_GL_TASKS] arbitrary blocks and throws [IllegalStateException] instead of
     * dropping the oldest. Uniform [set] calls are coalesced separately and do not count.
     */
    public fun runOnGl(block: () -> Unit) {
        synchronized(lock) {
            check(tasks.size < MAX_QUEUED_GL_TASKS) {
                "GL task queue is full ($MAX_QUEUED_GL_TASKS)"
            }
            tasks.addLast(block)
        }
        scheduleDrain()
    }

    /**
     * Called on the UI thread when a GL view starts; schedules writes made before it existed.
     * The new view has its own GL thread, so a program linked by an older view is dropped here:
     * writing through it from the new thread would fail the runtime's thread check.
     */
    internal fun attachQueue(queue: (() -> Unit) -> Unit) {
        synchronized(lock) {
            glQueue = queue
            runtime = null
            drainQueued.set(false)
        }
        scheduleDrain()
    }

    /**
     * Called when the GL view that attached [queue] goes away. A newer view may already have
     * attached its own queue; that one stays. A drain the old view never ran must not block it.
     * Returns whether [queue] was the current one.
     */
    internal fun detachQueue(queue: (() -> Unit) -> Unit): Boolean = synchronized(lock) {
        if (glQueue !== queue) return false
        glQueue = null
        runtime = null
        drainQueued.set(false)
        true
    }

    /**
     * GL thread of the view that attached [queue]. Makes [linked] current, writes every retained
     * value to it, then drains. A view whose queue was replaced keeps its program to itself.
     */
    internal fun attachRuntime(queue: (() -> Unit) -> Unit, linked: GlProgramRuntime) {
        synchronized(lock) {
            if (glQueue !== queue) return
            runtime = linked
            for (entry in latest) {
                if (!pending.containsKey(entry.key)) pending[entry.key] = entry.value
            }
            replay(uniformBlockWrites)
            replay(storageBlockWrites)
        }
        drainPending()
    }

    /** Whether [queue] belongs to the view this controller currently drives. */
    internal fun ownsQueue(queue: (() -> Unit) -> Unit): Boolean = glQueue === queue

    /** GL thread. [linked] is gone or about to be; writes wait for the next link. */
    internal fun detachRuntime(linked: GlProgramRuntime) {
        synchronized(lock) {
            if (runtime === linked) runtime = null
        }
    }

    private fun scheduleDrain() {
        val queue = glQueue ?: return
        if (drainQueued.compareAndSet(false, true)) queue(drainTask)
    }

    /**
     * Applies queued uniform writes, then [runOnGl] blocks, one at a time.
     * Must run on the GL thread. A write or block that throws is not retried; everything still
     * queued is scheduled again, so one failure does not drop the rest of the drain.
     */
    private fun drainPending() {
        while (true) {
            val action = nextDrainAction() ?: return
            try {
                action()
            } catch (error: Throwable) {
                scheduleDrain()
                throw error
            }
        }
    }

    private fun nextDrainAction(): (() -> Unit)? = synchronized(lock) {
        val linked = runtime ?: return null
        poll(pending)?.let { write -> return { write(linked) } }
        poll(uniformBlockWrites)?.let { write -> return { write(linked) } }
        poll(storageBlockWrites)?.let { write -> return { write(linked) } }
        tasks.removeFirstOrNull()
    }

    private fun poll(pendingWrites: IdentityHashMap<Uniform<*>, UniformWrite>): UniformWrite? {
        val key = pendingWrites.keys.firstOrNull() ?: return null
        return pendingWrites.remove(key)
    }

    private fun <K : Any> poll(writes: CoalescedWrites<K>): UniformWrite? {
        val key = writes.pending.keys.firstOrNull() ?: return null
        return writes.pending.remove(key)
    }

    private fun <K : Any> replay(writes: CoalescedWrites<K>) {
        for (entry in writes.latest) {
            if (!writes.pending.containsKey(entry.key)) writes.pending[entry.key] = entry.value
        }
    }

    private fun <K : Any> enqueueRetained(writes: CoalescedWrites<K>, key: K, write: UniformWrite) {
        synchronized(lock) {
            writes.pending[key] = write
            writes.latest[key] = write
        }
        scheduleDrain()
    }

    private fun enqueue(uniform: Uniform<*>, retained: Boolean, write: UniformWrite) {
        synchronized(lock) {
            pending[uniform] = write
            if (retained) latest[uniform] = write else latest.remove(uniform)
        }
        scheduleDrain()
    }

    @JvmName("setHighFloat")
    public fun set(uniform: Uniform<Flt<High>>, value: Float) {
        enqueue(uniform, retained = true) { it.set(uniform, value) }
    }

    @JvmName("setMedFloat")
    public fun set(uniform: Uniform<Flt<Med>>, value: Float) {
        enqueue(uniform, retained = true) { it.set(uniform, value) }
    }

    public fun set(uniform: Uniform<Vec2<Flt<High>>>, x: Float, y: Float) {
        enqueue(uniform, retained = true) { it.set(uniform, x, y) }
    }

    @JvmName("setMedVec2")
    public fun set(uniform: Uniform<Vec2<Flt<Med>>>, x: Float, y: Float) {
        enqueue(uniform, retained = true) { it.set(uniform, x, y) }
    }

    public fun set(uniform: Uniform<Vec3<Flt<High>>>, x: Float, y: Float, z: Float) {
        enqueue(uniform, retained = true) { it.set(uniform, x, y, z) }
    }

    @JvmName("setMedVec3")
    public fun set(uniform: Uniform<Vec3<Flt<Med>>>, x: Float, y: Float, z: Float) {
        enqueue(uniform, retained = true) { it.set(uniform, x, y, z) }
    }

    public fun set(uniform: Uniform<Vec4<Flt<High>>>, x: Float, y: Float, z: Float, w: Float) {
        enqueue(uniform, retained = true) { it.set(uniform, x, y, z, w) }
    }

    @JvmName("setMedVec4")
    public fun set(uniform: Uniform<Vec4<Flt<Med>>>, x: Float, y: Float, z: Float, w: Float) {
        enqueue(uniform, retained = true) { it.set(uniform, x, y, z, w) }
    }

    public fun set(uniform: Uniform<IntS>, value: Int) {
        enqueue(uniform, retained = true) { it.set(uniform, value) }
    }

    public fun set(uniform: Uniform<BoolS>, value: Boolean) {
        enqueue(uniform, retained = true) { it.set(uniform, value) }
    }

    @JvmName("bindSampler2D")
    public fun bind(uniform: Uniform<Sampler2D>, texture: Int) {
        enqueue(uniform, retained = false) { it.bind(uniform, texture) }
    }

    @JvmName("bindSamplerCube")
    public fun bind(uniform: Uniform<SamplerCube>, texture: Int) {
        enqueue(uniform, retained = false) { it.bind(uniform, texture) }
    }

    /** Scene. [values] is read on the GL thread, after this call returns; do not change it afterward. */
    public fun setMat2(uniform: Uniform<Mat2>, values: FloatArray) {
        enqueue(uniform, retained = true) { it.set(uniform, values) }
    }

    /** Scene. [values] is read on the GL thread, after this call returns; do not change it afterward. */
    public fun setMat3(uniform: Uniform<Mat3>, values: FloatArray) {
        enqueue(uniform, retained = true) { it.set(uniform, values) }
    }

    /** Scene. [values] is read on the GL thread, after this call returns; do not change it afterward. */
    public fun setMat4(uniform: Uniform<Mat4>, values: FloatArray) {
        enqueue(uniform, retained = true) { it.set(uniform, values) }
    }

    /**
     * Scene. Uploads [values] to [block] on the GL thread and keeps them for the next context.
     * [values] is read on that thread; do not change it afterward.
     */
    @JvmName("setUniformBlock")
    public fun set(block: UniformBlock, values: FloatArray) {
        enqueueRetained(uniformBlockWrites, block) { it.set(block, values) }
    }

    /**
     * Scene. Uploads [values] to [block] on the GL thread and keeps them for the next context.
     * [values] is read on that thread; do not change it afterward.
     */
    @JvmName("setStorageBlock")
    public fun set(block: StorageBlock, values: FloatArray) {
        enqueueRetained(storageBlockWrites, block) { it.set(block, values) }
    }

    /**
     * Scene. Queues [dispatch][GlProgramRuntime.dispatch] on the GL thread.
     * A failed uniform write does not drop it.
     */
    public fun dispatch(x: Int, y: Int = 1, z: Int = 1) {
        require(x >= 1 && y >= 1 && z >= 1) {
            "Compute dispatch size must be at least 1, was $x, $y, $z"
        }
        runOnGl { runtime?.dispatch(x, y, z) }
    }

    /**
     * Scene. Queues a storage read on the GL thread and returns immediately.
     * [onResult] runs on that thread with the float count after [into] has been filled.
     */
    public fun read(block: StorageBlock, into: FloatArray, onResult: (Int) -> Unit) {
        runOnGl {
            val linked = runtime ?: return@runOnGl
            onResult(linked.read(block, into))
        }
    }
}

private class CoalescedWrites<K : Any> {
    val latest = IdentityHashMap<K, UniformWrite>()
    val pending = IdentityHashMap<K, UniformWrite>()
}
