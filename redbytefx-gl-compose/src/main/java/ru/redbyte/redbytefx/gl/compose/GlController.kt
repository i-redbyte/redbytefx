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
 *
 * Arrays are validated on the calling thread. An exception thrown later on the GL thread, by a
 * queued write, a [runOnGl] block, or the surface's `onFrame`, never reaches the GL thread's
 * uncaught handler: it is published as [failure] and the remaining work still runs.
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

    private val failureHolder = mutableStateOf<Throwable?>(null)

    /** Last exception raised on the GL thread for this controller; null after a successful link. */
    public val failure: State<Throwable?> get() = failureHolder

    private var glQueue: ((() -> Unit) -> Unit)? = null
    private var post: (() -> Unit) -> Unit = { it() }
    private var drainTask: (() -> Unit)? = null

    @Volatile
    internal var runtime: GlProgramRuntime? = null
        private set
    private val lock = Any()
    private val uniformSlots = IdentityHashMap<Uniform<*>, UniformSlot>().apply {
        for (entry in program.spelledUniforms()) put(entry.uniform, UniformSlot(entry.uniform))
    }
    private val uniformBlockSlots = IdentityHashMap<UniformBlock, BlockSlot>().apply {
        for (block in program.uniformBlocks) put(block, BlockSlot(block))
    }
    private val storageBlockSlots = IdentityHashMap<StorageBlock, BlockSlot>().apply {
        for (block in program.storageBlocks) put(block, BlockSlot(block))
    }
    private val allSlots: List<WriteSlot> = uniformSlots.values + uniformBlockSlots.values + storageBlockSlots.values
    private val pendingSlots = ArrayDeque<WriteSlot>()
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
    internal fun attachQueue(queue: (() -> Unit) -> Unit, post: (() -> Unit) -> Unit = { it() }) {
        synchronized(lock) {
            glQueue = queue
            this.post = post
            runtime = null
            discardPendingTextureBindings()
            queueGeneration++
            val generation = queueGeneration
            drainTask = { drainIfCurrent(queue, generation) }
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
        drainTask = null
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
            for (slot in allSlots) {
                if (slot.replayable) markPending(slot)
            }
            queueGeneration
        }
        publishFailure(null)
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
        for (slot in uniformSlots.values) slot.forgetTexture()
        pendingSlots.removeAll { slot ->
            val stale = !slot.replayable
            if (stale) slot.pending = false
            stale
        }
    }

    private fun markPending(slot: WriteSlot) {
        if (slot.pending) return
        slot.pending = true
        pendingSlots.addLast(slot)
    }

    private fun scheduleDrain() {
        val queue: (() -> Unit) -> Unit
        val task: () -> Unit
        synchronized(lock) {
            queue = glQueue ?: return
            task = drainTask ?: return
            if (drainQueued) return
            drainQueued = true
        }
        queue(task)
    }

    private fun drainIfCurrent(queue: (() -> Unit) -> Unit, generation: Long) {
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

    /** GL thread. Publishes [error] as [failure] instead of letting it end the GL thread. */
    internal fun reportFailure(error: Throwable) {
        publishFailure(error)
    }

    private fun publishFailure(error: Throwable?) {
        val poster = synchronized(lock) { post }
        poster { failureHolder.value = error }
    }

    /**
     * Applies pending writes in the order they were first made, then [runOnGl] blocks, one at a
     * time. Must run on the GL thread. Under the lock a write only copies its value aside, so a
     * UI-thread write never waits for the driver and never tears; the GL call happens outside.
     * Anything that throws is reported and not retried; the rest of the drain still runs.
     */
    @Suppress("TooGenericExceptionCaught")
    private fun drainPending(queue: (() -> Unit) -> Unit, generation: Long) {
        while (true) {
            val linked = currentRuntime(queue, generation) ?: return
            val slot = stageNextSlot()
            try {
                if (slot != null) {
                    slot.applyStaged(linked)
                } else {
                    val task = synchronized(lock) { tasks.removeFirstOrNull() } ?: return
                    task(linked)
                }
            } catch (error: Exception) {
                reportFailure(error)
            }
        }
    }

    private fun currentRuntime(queue: (() -> Unit) -> Unit, generation: Long): GlProgramRuntime? =
        synchronized(lock) {
            if (glQueue !== queue || queueGeneration != generation) null else runtime
        }

    /** Under the lock: takes the oldest pending slot and copies its value aside. */
    private fun stageNextSlot(): WriteSlot? = synchronized(lock) {
        val slot = pendingSlots.removeFirstOrNull() ?: return null
        slot.pending = false
        slot.stage()
        slot
    }

    private fun uniformSlot(uniform: Uniform<*>): UniformSlot =
        requireNotNull(uniformSlots[uniform]) { "Uniform does not belong to this shader" }

    private fun requireUniformBlock(block: UniformBlock): BlockSlot =
        requireNotNull(uniformBlockSlots[block]) { "Uniform block does not belong to this shader" }

    private fun requireStorageBlock(block: StorageBlock): BlockSlot =
        requireNotNull(storageBlockSlots[block]) { "Storage block does not belong to this shader" }

    private inline fun write(slot: WriteSlot, store: () -> Unit) {
        synchronized(lock) {
            store()
            markPending(slot)
        }
        scheduleDrain()
    }

    private fun writeFloats(
        uniform: Uniform<*>,
        kind: UniformWriteKind,
        x: Float,
        y: Float = 0f,
        z: Float = 0f,
        w: Float = 0f,
    ) {
        val slot = uniformSlot(uniform)
        write(slot) { slot.storeFloats(kind, x, y, z, w) }
    }

    private fun writeInt(uniform: Uniform<*>, kind: UniformWriteKind, value: Int) {
        val slot = uniformSlot(uniform)
        write(slot) { slot.storeInt(kind, value) }
    }

    private fun writeMatrix(uniform: Uniform<*>, values: FloatArray, floats: Int) {
        val slot = uniformSlot(uniform)
        requireMatrix(values, floats)
        write(slot) { slot.storeMatrix(values) }
    }

    @JvmName("setHighFloat")
    public fun set(uniform: Uniform<Flt<High>>, value: Float) {
        writeFloats(uniform, UniformWriteKind.Float, value)
    }

    @JvmName("setMedFloat")
    public fun set(uniform: Uniform<Flt<Med>>, value: Float) {
        writeFloats(uniform, UniformWriteKind.Float, value)
    }

    public fun set(uniform: Uniform<Vec2<Flt<High>>>, x: Float, y: Float) {
        writeFloats(uniform, UniformWriteKind.Vec2, x, y)
    }

    @JvmName("setMedVec2")
    public fun set(uniform: Uniform<Vec2<Flt<Med>>>, x: Float, y: Float) {
        writeFloats(uniform, UniformWriteKind.Vec2, x, y)
    }

    public fun set(uniform: Uniform<Vec3<Flt<High>>>, x: Float, y: Float, z: Float) {
        writeFloats(uniform, UniformWriteKind.Vec3, x, y, z)
    }

    @JvmName("setMedVec3")
    public fun set(uniform: Uniform<Vec3<Flt<Med>>>, x: Float, y: Float, z: Float) {
        writeFloats(uniform, UniformWriteKind.Vec3, x, y, z)
    }

    public fun set(uniform: Uniform<Vec4<Flt<High>>>, x: Float, y: Float, z: Float, w: Float) {
        writeFloats(uniform, UniformWriteKind.Vec4, x, y, z, w)
    }

    @JvmName("setMedVec4")
    public fun set(uniform: Uniform<Vec4<Flt<Med>>>, x: Float, y: Float, z: Float, w: Float) {
        writeFloats(uniform, UniformWriteKind.Vec4, x, y, z, w)
    }

    public fun set(uniform: Uniform<IntS>, value: Int) {
        writeInt(uniform, UniformWriteKind.Int, value)
    }

    public fun set(uniform: Uniform<BoolS>, value: Boolean) {
        writeInt(uniform, UniformWriteKind.Bool, if (value) 1 else 0)
    }

    @JvmName("bindSampler2D")
    public fun bind(uniform: Uniform<Sampler2D>, texture: Int) {
        writeInt(uniform, UniformWriteKind.Texture2D, texture)
    }

    @JvmName("bindSamplerCube")
    public fun bind(uniform: Uniform<SamplerCube>, texture: Int) {
        writeInt(uniform, UniformWriteKind.TextureCube, texture)
    }

    /** Scene. Copies [values] into the pending write; the caller may reuse the array at once. */
    public fun setMat2(uniform: Uniform<Mat2>, values: FloatArray) {
        writeMatrix(uniform, values, 4)
    }

    /** Scene. Copies [values] into the pending write; the caller may reuse the array at once. */
    public fun setMat3(uniform: Uniform<Mat3>, values: FloatArray) {
        writeMatrix(uniform, values, 9)
    }

    /** Scene. Copies [values] into the pending write; the caller may reuse the array at once. */
    public fun setMat4(uniform: Uniform<Mat4>, values: FloatArray) {
        writeMatrix(uniform, values, 16)
    }

    /**
     * Scene. Uploads [values] to [block] on the GL thread and keeps them for the next context.
     * [values] is copied before the write is queued.
     */
    @JvmName("setUniformBlock")
    public fun set(block: UniformBlock, values: FloatArray) {
        val slot = requireUniformBlock(block)
        require(values.size == block.floatCount) {
            "Uniform block \"${block.name}\" expects ${block.floatCount} floats, was ${values.size}"
        }
        requireFinite(values)
        write(slot) { slot.store(values) }
    }

    /**
     * Scene. Uploads [values] to [block] on the GL thread and keeps them for the next context.
     * [values] is copied before the write is queued.
     */
    @JvmName("setStorageBlock")
    public fun set(block: StorageBlock, values: FloatArray) {
        val slot = requireStorageBlock(block)
        block.byteSize(values.size)
        requireFinite(values)
        write(slot) { slot.store(values) }
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

private fun requireMatrix(values: FloatArray, floats: Int) {
    require(values.size == floats) { "Matrix uniform expects $floats floats, was ${values.size}" }
}

private fun requireFinite(values: FloatArray) {
    for (value in values) require(value.isFinite()) { "Block values must be finite, was $value" }
}
