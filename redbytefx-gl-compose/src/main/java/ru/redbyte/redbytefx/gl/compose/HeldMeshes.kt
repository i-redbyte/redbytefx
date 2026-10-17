package ru.redbyte.redbytefx.gl.compose

import java.util.IdentityHashMap

/**
 * GL thread. GPU buffers kept for meshes drawn through [GlFrame.draw], keyed by mesh object.
 *
 * A mesh undrawn for [HELD_MESH_FRAMES] frames is evicted. Eviction walks a parallel list by
 * index and reports into a caller-owned list, so a frame allocates nothing.
 */
/** Something a frame can mark as drawn. */
internal interface FrameStamped {
    val drawnFrame: Long
}

internal class HeldMeshes<T : FrameStamped> {
    private val byMesh = IdentityHashMap<GlMesh, T>()
    private val meshes = ArrayList<GlMesh>()

    operator fun get(mesh: GlMesh): T? = byMesh[mesh]

    fun put(mesh: GlMesh, held: T) {
        if (byMesh.put(mesh, held) == null) meshes += mesh
    }

    /** Moves meshes idle at [frame] out of the cache into [into]. */
    fun evictIdle(frame: Long, into: MutableList<T>) {
        var index = meshes.lastIndex
        while (index >= 0) {
            val mesh = meshes[index]
            val held = byMesh.getValue(mesh)
            if (frame - held.drawnFrame >= HELD_MESH_FRAMES) {
                into += held
                byMesh.remove(mesh)
                meshes[index] = meshes[meshes.lastIndex]
                meshes.removeAt(meshes.lastIndex)
            }
            index -= 1
        }
    }

    inline fun forEachHeld(action: (T) -> Unit) {
        for (index in 0 until size()) action(heldAt(index))
    }

    fun size(): Int = meshes.size

    fun heldAt(index: Int): T = byMesh.getValue(meshes[index])

    fun clear() {
        byMesh.clear()
        meshes.clear()
        meshes.trimToSize()
    }
}
