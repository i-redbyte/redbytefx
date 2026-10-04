package ru.redbyte.redbytefx.gl.compose

import ru.redbyte.redbytefx.gl.GlColorTarget
import ru.redbyte.redbytefx.gl.GlProgramRuntime

/** Floats in one column-major model matrix used as an instance attribute. */
public const val MODEL_MATRIX_FLOATS: Int = 16

/** Instance attribute spelled by `attributeVec4("model0")`. Divisor is one. */
public const val MODEL_COLUMN_0: String = "a_model0"

/** Instance attribute spelled by `attributeVec4("model1")`. Divisor is one. */
public const val MODEL_COLUMN_1: String = "a_model1"

/** Instance attribute spelled by `attributeVec4("model2")`. Divisor is one. */
public const val MODEL_COLUMN_2: String = "a_model2"

/** Instance attribute spelled by `attributeVec4("model3")`. Divisor is one. */
public const val MODEL_COLUMN_3: String = "a_model3"

internal val MODEL_COLUMNS: Array<String> = arrayOf(
    MODEL_COLUMN_0,
    MODEL_COLUMN_1,
    MODEL_COLUMN_2,
    MODEL_COLUMN_3,
)

/** One recorded draw. Instances are reused across frames by [DrawList]. */
internal class RecordedDraw {
    lateinit var mesh: GlMesh
    var first: Int = 0
    var count: Int = 0
    var instances: FloatArray? = null
    var present: Boolean = false
    var wholeMesh: Boolean = false
}

/** Scene. [indices] must be non-empty and every index must address a vertex. */
internal fun requireIndexRange(indices: IntArray, vertexCount: Int) {
    require(indices.isNotEmpty()) { "Index buffer must contain at least one index" }
    require(vertexCount > 0) { "Vertex count must be positive, was $vertexCount" }
    var highest = -1
    for (index in indices) {
        require(index >= 0) { "Index must be non-negative, was $index" }
        if (index > highest) highest = index
    }
    require(highest < vertexCount) { "Index $highest is outside $vertexCount vertices" }
}

/**
 * Scene. Count a recorded draw issues when it runs. A whole-mesh draw reads the index or vertex
 * count then, after any [GlFrame.replace]. An explicit count must still fit after [first].
 */
internal fun executedDrawCount(
    wholeMesh: Boolean,
    first: Int,
    requested: Int,
    indexCount: Int?,
    vertexCount: Int,
): Int {
    val available = indexCount ?: vertexCount
    require(first <= available) { "Draw first $first is past $available elements" }
    if (wholeMesh) return available - first
    require(requested <= available - first) {
        "Draw range $first + $requested is past $available elements"
    }
    return requested
}

/** Scene. Depth-test a draw when the surface asked for it, or this mesh writes depth. */
internal fun drawDepthTest(force: Boolean, meshDepth: Boolean): Boolean = force || meshDepth

internal fun forEachStaleAttrib(
    enabled: IntArray,
    enabledCount: Int,
    next: IntArray,
    nextCount: Int,
    disable: (Int) -> Unit,
) {
    var index = 0
    while (index < enabledCount) {
        val location = enabled[index]
        var kept = false
        var cursor = 0
        while (cursor < nextCount) {
            if (next[cursor] == location) {
                kept = true
                break
            }
            cursor += 1
        }
        if (!kept) disable(location)
        index += 1
    }
}

internal fun requireOffscreenTarget(renderToTexture: Boolean, offscreenCount: Int) {
    check(offscreenCount == 0 || renderToTexture) {
        "Offscreen draws need renderToTexture so the pass has a color target"
    }
}

/** Draws recorded during one [GlSurface] frame. [reset] keeps the records for the next frame. */
internal class DrawList {
    private val screenDraws = ArrayList<RecordedDraw>()
    private val offscreenDraws = ArrayList<RecordedDraw>()
    private val pool = ArrayList<RecordedDraw>()
    private var pooled = 0
    private var recordingOffscreen = false

    fun screen(): List<RecordedDraw> = screenDraws

    fun offscreen(): List<RecordedDraw> = offscreenDraws

    fun recordedCount(): Int = screenDraws.size + offscreenDraws.size

    fun reset() {
        screenDraws.clear()
        offscreenDraws.clear()
        pooled = 0
        recordingOffscreen = false
    }

    /** Records [mesh]; [available] is its current index count, or vertex count when it has no indices. */
    fun draw(
        mesh: GlMesh,
        first: Int,
        count: Int,
        instances: FloatArray?,
        present: Boolean,
        presentReady: Boolean,
        available: Int,
    ) {
        require(!present || !recordingOffscreen) { "An offscreen draw uses the surface program" }
        check(!present || presentReady) { "This surface has no present program" }
        require(instances == null || instances.size % MODEL_MATRIX_FLOATS == 0) {
            "Instance buffer needs $MODEL_MATRIX_FLOATS floats per model matrix, was ${instances?.size}"
        }
        require(first >= 0) { "Draw first must be non-negative, was $first" }
        require(first <= available) { "Draw first $first is past $available elements" }
        val resolved = if (count < 0) available - first else count
        require(resolved <= available - first) { "Draw range $first + $resolved is past $available elements" }
        if (resolved == 0 || instances?.isEmpty() == true) return
        val record = obtain()
        record.mesh = mesh
        record.first = first
        record.count = resolved
        record.instances = instances
        record.present = present
        record.wholeMesh = count < 0
        if (recordingOffscreen) offscreenDraws += record else screenDraws += record
    }

    fun offscreen(block: () -> Unit) {
        val previous = recordingOffscreen
        recordingOffscreen = true
        try {
            block()
        } finally {
            recordingOffscreen = previous
        }
    }

    private fun obtain(): RecordedDraw {
        if (pooled == pool.size) pool += RecordedDraw()
        val record = pool[pooled]
        pooled += 1
        return record
    }
}

/**
 * Per-frame GLES context passed to [GlSurface] render callbacks.
 *
 * [draw] records a mesh. If [onFrame][GlSurface] records no draws, the surface draws its mesh once.
 * The first recorded draw suppresses that automatic draw. [offscreen] records draws into
 * [colorTarget] when the surface was created with a render-to-texture pass. Bind framebuffer 0
 * before sampling [GlColorTarget.colorTexture]; the host does that between the two lists.
 * [presentRuntime] is a second program on the same EGL context. Write its uniforms here.
 * [GlController.set] does not see them.
 *
 * The host reuses one frame object for the life of the EGL context. Do not keep it after
 * [onFrame][GlSurface] returns.
 */
public class GlFrame internal constructor(
    public val runtime: GlProgramRuntime,
    private val upload: (FloatArray, IntArray?) -> Unit,
    private val surfaceMesh: GlMesh?,
    public val presentRuntime: GlProgramRuntime?,
) {
    /** A frame with no surface mesh: [replace] forwards to [upload], and [draw] needs a mesh. */
    public constructor(
        runtime: GlProgramRuntime,
        seconds: Float,
        aspect: Float,
        upload: (FloatArray) -> Unit,
    ) : this(runtime, { vertices, _ -> upload(vertices) }, null, null) {
        this.seconds = seconds
        this.aspect = aspect
    }

    private var surfaceVertices: Int = surfaceMesh?.let { it.vertices.size / it.stride } ?: 0
    private var surfaceIndices: IntArray? = surfaceMesh?.indices

    /** Seconds since the surface was created on this context. */
    public var seconds: Float = 0f
        internal set

    /** Surface width divided by height. */
    public var aspect: Float = 1f
        internal set

    /** Offscreen target when the surface renders to a texture; null otherwise. */
    public var colorTarget: GlColorTarget? = null
        internal set

    private val draws = DrawList()

    /**
     * Scene. Replaces the surface vertex buffer on the GL thread.
     * [indices], when passed, replaces the element buffer. When omitted, the current indices
     * must still address the new vertex list. The host does not keep [vertices]; it keeps [indices]
     * to size later draws, so do not change that array afterward.
     */
    @JvmOverloads
    public fun replace(vertices: FloatArray, indices: IntArray? = null) {
        val mesh = surfaceMesh
        if (mesh != null) {
            require(vertices.size % mesh.stride == 0) {
                "Vertex buffer size ${vertices.size} is not a multiple of stride ${mesh.stride}"
            }
            val next = indices ?: surfaceIndices
            if (next != null) requireIndexRange(next, vertices.size / mesh.stride)
            surfaceVertices = vertices.size / mesh.stride
            surfaceIndices = next
        }
        upload(vertices, indices)
    }

    /**
     * Scene. Records one draw of [mesh]; null is the surface mesh. A negative [count] draws from
     * [first] to the end, read when the draw runs. A count of zero, or an [instances] array with
     * no matrices, is ignored and does not suppress the automatic draw. A range past the mesh is an
     * argument error. [instances] is column-major model matrices, [MODEL_MATRIX_FLOATS] floats each,
     * bound as [MODEL_COLUMN_0] through [MODEL_COLUMN_3] with divisor 1. The array is read after
     * [onFrame][GlSurface] returns, so do not change it during this frame. [present] selects
     * [presentRuntime] and is only valid for a screen draw.
     */
    public fun draw(
        mesh: GlMesh? = null,
        first: Int = 0,
        count: Int = -1,
        instances: FloatArray? = null,
        present: Boolean = false,
    ) {
        val target = mesh ?: requireNotNull(surfaceMesh) { "This frame has no surface mesh" }
        val available = if (target === surfaceMesh) {
            surfaceIndices?.size ?: surfaceVertices
        } else {
            target.indices?.size ?: (target.vertices.size / target.stride)
        }
        draws.draw(target, first, count, instances, present, presentRuntime != null, available)
    }

    /**
     * Scene. Records [block] into the offscreen list. The surface must be created with
     * `renderToTexture` so [colorTarget] receives the pass. The host clears that target once,
     * draws the list, then binds framebuffer 0 before any screen draw.
     */
    public fun offscreen(block: () -> Unit) {
        draws.offscreen(block)
    }

    internal fun drawList(): DrawList = draws
}
