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
import ru.redbyte.redbytefx.gl.GlProgramRuntime
import java.util.IdentityHashMap
import kotlin.jvm.JvmName

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

    private var glQueue: ((() -> Unit) -> Unit)? = null

    @Volatile
    internal var runtime: GlProgramRuntime? = null
        private set
    private val lock = Any()
    private val ownedUniforms = IdentityHashMap<Uniform<*>, Boolean>().apply {
        for (entry in program.spelledUniforms()) put(entry.uniform, true)
    }
    private val ownedUniformBlocks = IdentityHashMap<UniformBlock, Boolean>().apply {
        for (block in program.uniformBlocks) put(block, true)
    }
    private val ownedStorageBlocks = IdentityHashMap<StorageBlock, Boolean>().apply {
        for (block in program.storageBlocks) put(block, true)
    }
    private val latest = IdentityHashMap<Uniform<*>, UniformWrite>()
    private val pending = IdentityHashMap<Uniform<*>, UniformWrite>()
    private val pendingOrder = ArrayDeque<Uniform<*>>()
    private val uniformBlockWrites = CoalescedWrites<UniformBlock>()
    private val storageBlockWrites = CoalescedWrites<StorageBlock>()
    private val tasks = ArrayDeque<(GlProgramRuntime) -> Unit>()
    private var queueGeneration = 0L
    private var drainQueued = false

    /**
     * Runs [block] on the GL thread.
     *
     * If the surface is not linked yet, [block] waits until link and then runs. The queue holds at
     * most [MAX_QUEUED_GL_TASKS] arbitrary blocks and throws [IllegalStateException] instead of
     * dropping the oldest. Uniform [set] calls are coalesced separately and do not count.
     */
    public fun runOnGl(block: () -> Unit) {
        enqueueTask { block() }
    }

    private fun enqueueTask(block: (GlProgramRuntime) -> Unit) {
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
            discardPendingTextureBindings()
            queueGeneration++
            drainQueued = false
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
        discardPendingTextureBindings()
        queueGeneration++
        drainQueued = false
        true
    }

    /**
     * GL thread of the view that attached [queue]. Makes [linked] current, writes every retained
     * value to it, then drains. A view whose queue was replaced keeps its program to itself.
     */
    internal fun attachRuntime(queue: (() -> Unit) -> Unit, linked: GlProgramRuntime) {
        val generation = synchronized(lock) {
            if (glQueue !== queue) return
            runtime = linked
            for (entry in latest) {
                if (!pending.containsKey(entry.key)) {
                    pending[entry.key] = entry.value
                    pendingOrder.addLast(entry.key)
                }
            }
            replay(uniformBlockWrites)
            replay(storageBlockWrites)
            queueGeneration
        }
        drainPending(queue, generation)
    }

    /** Whether [queue] belongs to the view this controller currently drives. */
    internal fun ownsQueue(queue: (() -> Unit) -> Unit): Boolean = synchronized(lock) { glQueue === queue }

    /** GL thread. [linked] is gone or about to be; writes wait for the next link. */
    internal fun detachRuntime(linked: GlProgramRuntime) {
        synchronized(lock) {
            if (runtime === linked) {
                runtime = null
                discardPendingTextureBindings()
            }
        }
    }

    /** A texture name is valid only on the context that received its bind. */
    private fun discardPendingTextureBindings() {
        val iterator = pending.keys.iterator()
        while (iterator.hasNext()) {
            if (!latest.containsKey(iterator.next())) iterator.remove()
        }
        pendingOrder.removeAll { !pending.containsKey(it) }
    }

    private fun scheduleDrain() {
        val scheduled = synchronized(lock) {
            val queue = glQueue ?: return
            if (drainQueued) return
            drainQueued = true
            queue to queueGeneration
        }
        val (queue, generation) = scheduled
        queue {
            val current = synchronized(lock) {
                if (glQueue !== queue || queueGeneration != generation) {
                    false
                } else {
                    drainQueued = false
                    true
                }
            }
            if (current) drainPending(queue, generation)
        }
    }

    /**
     * Applies queued uniform writes, then [runOnGl] blocks, one at a time.
     * Must run on the GL thread. A write or block that throws is not retried; everything still
     * queued is scheduled again, so one failure does not drop the rest of the drain.
     */
    private fun drainPending(queue: (() -> Unit) -> Unit, generation: Long) {
        while (true) {
            val action = nextDrainAction(queue, generation) ?: return
            try {
                action()
            } catch (error: Throwable) {
                scheduleDrain()
                throw error
            }
        }
    }

    private fun nextDrainAction(queue: (() -> Unit) -> Unit, generation: Long): (() -> Unit)? = synchronized(lock) {
        if (glQueue !== queue || queueGeneration != generation) return null
        val linked = runtime ?: return null
        poll(pending, pendingOrder)?.let { write -> return { write(linked) } }
        poll(uniformBlockWrites)?.let { write -> return { write(linked) } }
        poll(storageBlockWrites)?.let { write -> return { write(linked) } }
        tasks.removeFirstOrNull()?.let { task -> return { task(linked) } }
        null
    }

    private fun <K : Any> poll(
        pendingWrites: IdentityHashMap<K, UniformWrite>,
        order: ArrayDeque<K>,
    ): UniformWrite? {
        val key = order.removeFirstOrNull() ?: return null
        return pendingWrites.remove(key)
    }

    private fun <K : Any> poll(writes: CoalescedWrites<K>): UniformWrite? {
        return poll(writes.pending, writes.pendingOrder)
    }

    private fun <K : Any> replay(writes: CoalescedWrites<K>) {
        for (entry in writes.latest) {
            if (!writes.pending.containsKey(entry.key)) {
                writes.pending[entry.key] = entry.value
                writes.pendingOrder.addLast(entry.key)
            }
        }
    }

    private fun <K : Any> enqueueRetained(writes: CoalescedWrites<K>, key: K, write: UniformWrite) {
        synchronized(lock) {
            if (!writes.pending.containsKey(key)) writes.pendingOrder.addLast(key)
            writes.pending[key] = write
            writes.latest[key] = write
        }
        scheduleDrain()
    }

    private fun enqueue(uniform: Uniform<*>, retained: Boolean, write: UniformWrite) {
        requireUniform(uniform)
        synchronized(lock) {
            if (!pending.containsKey(uniform)) pendingOrder.addLast(uniform)
            pending[uniform] = write
            if (retained) latest[uniform] = write else latest.remove(uniform)
        }
        scheduleDrain()
    }

    private fun requireUniform(uniform: Uniform<*>) {
        require(ownedUniforms.containsKey(uniform)) { "Uniform does not belong to this shader" }
    }

    private fun requireUniformBlock(block: UniformBlock) {
        require(ownedUniformBlocks.containsKey(block)) { "Uniform block does not belong to this shader" }
    }

    private fun requireStorageBlock(block: StorageBlock) {
        require(ownedStorageBlocks.containsKey(block)) { "Storage block does not belong to this shader" }
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

    /** Scene. Copies [values] before queuing the GL write. */
    public fun setMat2(uniform: Uniform<Mat2>, values: FloatArray) {
        requireUniform(uniform)
        val snapshot = values.copyOf()
        enqueue(uniform, retained = true) { it.set(uniform, snapshot) }
    }

    /** Scene. Copies [values] before queuing the GL write. */
    public fun setMat3(uniform: Uniform<Mat3>, values: FloatArray) {
        requireUniform(uniform)
        val snapshot = values.copyOf()
        enqueue(uniform, retained = true) { it.set(uniform, snapshot) }
    }

    /** Scene. Copies [values] before queuing the GL write. */
    public fun setMat4(uniform: Uniform<Mat4>, values: FloatArray) {
        requireUniform(uniform)
        val snapshot = values.copyOf()
        enqueue(uniform, retained = true) { it.set(uniform, snapshot) }
    }

    /**
     * Scene. Uploads [values] to [block] on the GL thread and keeps them for the next context.
     * [values] is copied before the write is queued.
     */
    @JvmName("setUniformBlock")
    public fun set(block: UniformBlock, values: FloatArray) {
        requireUniformBlock(block)
        val snapshot = values.copyOf()
        enqueueRetained(uniformBlockWrites, block) { it.set(block, snapshot) }
    }

    /**
     * Scene. Uploads [values] to [block] on the GL thread and keeps them for the next context.
     * [values] is copied before the write is queued.
     */
    @JvmName("setStorageBlock")
    public fun set(block: StorageBlock, values: FloatArray) {
        requireStorageBlock(block)
        val snapshot = values.copyOf()
        enqueueRetained(storageBlockWrites, block) { it.set(block, snapshot) }
    }

    /**
     * Scene. Queues [dispatch][GlProgramRuntime.dispatch] on the GL thread.
     * A failed uniform write does not discard the queued dispatch.
     */
    public fun dispatch(x: Int, y: Int = 1, z: Int = 1) {
        require(x >= 1 && y >= 1 && z >= 1) {
            "Compute dispatch size must be at least 1, was $x, $y, $z"
        }
        enqueueTask { linked -> linked.dispatch(x, y, z) }
    }

    /**
     * Scene. Queues a storage read on the GL thread and returns immediately.
     * [onResult] runs on that thread with the float count after [into] has been filled.
     */
    public fun read(block: StorageBlock, into: FloatArray, onResult: (Int) -> Unit) {
        requireStorageBlock(block)
        enqueueTask { linked ->
            onResult(linked.read(block, into))
        }
    }
}

private class CoalescedWrites<K : Any> {
    val latest = IdentityHashMap<K, UniformWrite>()
    val pending = IdentityHashMap<K, UniformWrite>()
    val pendingOrder = ArrayDeque<K>()
}
