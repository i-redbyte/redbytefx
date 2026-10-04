package ru.redbyte.redbytefx.gl.compose

import android.opengl.GLES30
import android.opengl.GLES32
import android.opengl.GLSurfaceView
import java.util.IdentityHashMap
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import ru.redbyte.redbytefx.ShaderProgram
import ru.redbyte.redbytefx.gl.GlColorTarget
import ru.redbyte.redbytefx.gl.GlException
import ru.redbyte.redbytefx.gl.GlProgramRuntime
import ru.redbyte.redbytefx.gl.GlTextureUnits
import ru.redbyte.redbytefx.gl.IndexElementKind

/** Frames a recorded mesh may go undrawn before the host deletes its buffers. */
internal const val HELD_MESH_FRAMES: Long = 60L

private const val NANOS_PER_SECOND = 1_000_000_000f

/** GPU names for one mesh on the current EGL context. */
private class HeldMesh {
    var buffer = 0
    var floats = 0
    var vertexCount = 0
    var element = 0
    var elements: IndexElementKind? = null
    var indexCount = 0
    var vao = 0
    var drawnFrame = 0L
}

internal class SceneRenderer(
    private val controller: GlController,
    private val mesh: GlMesh,
    private val requirement: String?,
    private val slot: GlSlot,
    private val present: ShaderProgram?,
    private val renderToTexture: Boolean,
    private val onFrame: (GlFrame) -> Unit,
) : GLSurfaceView.Renderer {
    private val nameSlot = IntArray(1)
    private val surface = HeldMesh()
    private val heldMeshes = IdentityHashMap<GlMesh, HeldMesh>()
    private val heldOrder = ArrayList<GlMesh>()
    private val upload: (FloatArray, IntArray?) -> Unit = { vertices, indices -> replaceSurface(vertices, indices) }
    private var frame: GlFrame? = null
    private var presentRuntime: GlProgramRuntime? = null
    private var colorTarget: GlColorTarget? = null
    private var instanceBuffer = 0
    private var instanceFloats = 0
    private var frameNumber = 0L
    private var aspect = 1f
    private var viewWidth = 0
    private var viewHeight = 0
    private var startedNanos = 0L
    private var reported = false

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        releaseGpu(contextAlive = false)
        slot.runtime?.let { controller.detachRuntime(it) }
        slot.runtime = null
        slot.releaseGl = null
        reported = false
        publish(GlLinkState.Pending)
        val textureUnits = GlTextureUnits()
        val strict = controller.config.strictUniformLocations
        val runtime = try {
            linkProgram(controller.program, strict, textureUnits)
        } catch (error: GlException) {
            fail(error)
            return
        }
        val presentProgram = present
        if (presentProgram != null) {
            presentRuntime = try {
                linkProgram(presentProgram, strict, textureUnits)
            } catch (error: GlException) {
                runtime.destroy()
                fail(error)
                return
            }
        }
        fill(runtime, surface, mesh.stride, mesh.vertices, mesh.indices)
        frame = GlFrame(runtime, upload, mesh, presentRuntime)
        slot.runtime = runtime
        slot.releaseGl = { releaseGpu(contextAlive = true) }
        publish(GlLinkState.Linked)
        controller.attachRuntime(slot.queue, runtime)
        GLES30.glClearColor(mesh.clearR, mesh.clearG, mesh.clearB, 1f)
        startedNanos = System.nanoTime()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES30.glViewport(0, 0, width, height)
        viewWidth = width
        viewHeight = height
        aspect = if (height > 0) width.toFloat() / height.toFloat() else 1f
    }

    override fun onDrawFrame(gl: GL10?) {
        val runtime = slot.runtime ?: return
        val current = frame ?: return
        frameNumber += 1
        current.seconds = (System.nanoTime() - startedNanos) / NANOS_PER_SECOND
        current.aspect = aspect
        current.colorTarget = if (renderToTexture) ensureColorTarget(runtime) else null
        val draws = current.drawList()
        draws.reset()
        onFrame(current)
        requireOffscreenTarget(renderToTexture, draws.offscreen().size)
        runtime.use()
        execute(runtime, draws)
        draws.reset()
        evictIdleMeshes(runtime)
    }

    private fun execute(runtime: GlProgramRuntime, draws: DrawList) {
        val offscreen = draws.offscreen()
        requireOffscreenTarget(renderToTexture, offscreen.size)
        if (offscreen.isNotEmpty()) {
            runtime.bindFramebuffer(ensureColorTarget(runtime).framebuffer)
            clear()
            for (index in offscreen.indices) drawRecorded(runtime, offscreen[index])
            runtime.bindFramebuffer(0)
        }
        clear()
        if (draws.recordedCount() == 0) {
            drawMesh(runtime, mesh, surface, first = 0, count = 0, wholeMesh = true, instances = null)
            return
        }
        val screen = draws.screen()
        for (index in screen.indices) {
            val draw = screen[index]
            val active = if (draw.present) checkNotNull(presentRuntime) else runtime
            drawRecorded(active, draw)
        }
    }

    private fun drawRecorded(runtime: GlProgramRuntime, draw: RecordedDraw) {
        val target = draw.mesh
        drawMesh(runtime, target, hold(runtime, target), draw.first, draw.count, draw.wholeMesh, draw.instances)
    }

    @Suppress("LongParameterList")
    private fun drawMesh(
        runtime: GlProgramRuntime,
        target: GlMesh,
        held: HeldMesh,
        first: Int,
        count: Int,
        wholeMesh: Boolean,
        instances: FloatArray?,
    ) {
        if (held.buffer == 0) return
        val indexCount = if (held.element != 0) held.indexCount else null
        val executed = executedDrawCount(wholeMesh, first, count, indexCount, held.vertexCount)
        if (executed == 0) return
        runtime.use()
        depthTest(target)
        GLES30.glBindVertexArray(held.vao)
        GLES30.glBindBuffer(GLES30.GL_ARRAY_BUFFER, held.buffer)
        val attribs = target.attribs
        for (index in attribs.indices) {
            val attrib = attribs[index]
            val location = runtime.attribLocation(attrib.name)
            if (location >= 0) runtime.vertexAttribFloat(location, attrib.size, target.stride, attrib.offset)
        }
        if (held.element != 0) GLES30.glBindBuffer(GLES30.GL_ELEMENT_ARRAY_BUFFER, held.element)
        if (instances != null) bindInstances(runtime, instances)
        if (target.patchVertices > 0) GLES32.glPatchParameteri(GLES32.GL_PATCH_VERTICES, target.patchVertices)
        runtime.drawRange(
            mode = target.mode,
            vertexCount = held.vertexCount,
            first = first,
            count = executed,
            elements = held.elements,
            instanceCount = instances?.let { it.size / MODEL_MATRIX_FLOATS },
        )
        if (instances != null) clearInstances(runtime)
    }

    private fun bindInstances(runtime: GlProgramRuntime, instances: FloatArray) {
        if (instanceBuffer == 0) instanceBuffer = runtime.createBuffer()
        instanceFloats = runtime.replaceArrayBuffer(instanceBuffer, instanceFloats, instances)
        for (column in MODEL_COLUMNS.indices) {
            val location = runtime.attribLocation(MODEL_COLUMNS[column])
            if (location < 0) continue
            runtime.vertexAttribFloat(location, MODEL_COLUMNS.size, MODEL_MATRIX_FLOATS, column * MODEL_COLUMNS.size)
            runtime.vertexAttribDivisor(location, 1)
        }
    }

    private fun clearInstances(runtime: GlProgramRuntime) {
        for (column in MODEL_COLUMNS.indices) {
            val location = runtime.attribLocation(MODEL_COLUMNS[column])
            if (location < 0) continue
            runtime.vertexAttribDivisor(location, 0)
            runtime.disableVertexAttribArray(location)
        }
    }

    private fun hold(runtime: GlProgramRuntime, target: GlMesh): HeldMesh {
        if (target === mesh) return surface
        val existing = heldMeshes[target]
        if (existing != null) {
            existing.drawnFrame = frameNumber
            return existing
        }
        val created = HeldMesh()
        fill(runtime, created, target.stride, target.vertices, target.indices)
        created.drawnFrame = frameNumber
        heldMeshes[target] = created
        heldOrder += target
        return created
    }

    private fun fill(runtime: GlProgramRuntime, held: HeldMesh, stride: Int, vertices: FloatArray, indices: IntArray?) {
        if (held.vao == 0) {
            GLES30.glGenVertexArrays(1, nameSlot, 0)
            held.vao = nameSlot[0]
        }
        if (held.buffer == 0) held.buffer = runtime.createBuffer()
        held.floats = runtime.replaceArrayBuffer(held.buffer, held.floats, vertices)
        held.vertexCount = vertices.size / stride
        if (indices == null) return
        if (held.element == 0) held.element = runtime.createBuffer()
        held.elements = runtime.elementBufferData(held.element, indices)
        held.indexCount = indices.size
    }

    private fun replaceSurface(vertices: FloatArray, indices: IntArray?) {
        val runtime = slot.runtime ?: return
        fill(runtime, surface, mesh.stride, vertices, indices)
    }

    private fun evictIdleMeshes(runtime: GlProgramRuntime) {
        var index = heldOrder.size - 1
        while (index >= 0) {
            val target = heldOrder[index]
            val held = heldMeshes.getValue(target)
            if (frameNumber - held.drawnFrame >= HELD_MESH_FRAMES) {
                deleteNames(runtime, held)
                heldMeshes.remove(target)
                heldOrder.removeAt(index)
            }
            index -= 1
        }
    }

    private fun deleteNames(runtime: GlProgramRuntime, held: HeldMesh) {
        if (held.buffer != 0) runtime.deleteBuffer(held.buffer)
        if (held.element != 0) runtime.deleteBuffer(held.element)
        if (held.vao != 0) {
            nameSlot[0] = held.vao
            GLES30.glDeleteVertexArrays(1, nameSlot, 0)
        }
    }

    /** With a dead context the names are already gone; only the bookkeeping is dropped. */
    private fun releaseGpu(contextAlive: Boolean) {
        val runtime = slot.runtime
        if (contextAlive && runtime != null) {
            deleteNames(runtime, surface)
            for (index in heldOrder.indices) deleteNames(runtime, heldMeshes.getValue(heldOrder[index]))
            if (instanceBuffer != 0) runtime.deleteBuffer(instanceBuffer)
            colorTarget?.let { runtime.deleteColorTarget(it) }
            presentRuntime?.destroy()
        }
        surface.buffer = 0
        surface.floats = 0
        surface.vertexCount = 0
        surface.element = 0
        surface.elements = null
        surface.indexCount = 0
        surface.vao = 0
        heldMeshes.clear()
        heldOrder.clear()
        instanceBuffer = 0
        instanceFloats = 0
        colorTarget = null
        presentRuntime = null
        frame = null
    }

    private fun ensureColorTarget(runtime: GlProgramRuntime): GlColorTarget {
        val width = viewWidth.coerceAtLeast(1)
        val height = viewHeight.coerceAtLeast(1)
        val current = colorTarget
        if (current != null && current.width == width && current.height == height) return current
        if (current != null) runtime.deleteColorTarget(current)
        val created = runtime.createColorTarget(width, height)
        colorTarget = created
        return created
    }

    private fun depthTest(target: GlMesh) {
        if (drawDepthTest(controller.config.depth, target.depth)) {
            GLES30.glEnable(GLES30.GL_DEPTH_TEST)
        } else {
            GLES30.glDisable(GLES30.GL_DEPTH_TEST)
        }
    }

    private fun clear() {
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT or GLES30.GL_DEPTH_BUFFER_BIT)
    }

    private fun fail(error: GlException) {
        if (reported) return
        reported = true
        publish(GlLinkState.Failed(linkMessage(requirement, error)))
    }

    private fun publish(state: GlLinkState) {
        slot.post { if (controller.ownsQueue(slot.queue)) controller.linkStateValue = state }
    }
}
