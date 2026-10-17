package ru.redbyte.redbytefx.gl.compose

import ru.redbyte.redbytefx.gl.GlProgramRuntime
import ru.redbyte.redbytefx.scene.LitMesh
import ru.redbyte.redbytefx.scene.normalMatrix

/**
 * Uploads [model] and the matching [LitMesh.normalMatrix] for [litTexturedMesh].
 *
 * Call whenever the model matrix changes at runtime so lighting stays correct under non-uniform scale.
 * The normal matrix is computed into a buffer owned by the GL thread, so a per-frame call allocates
 * nothing.
 */
public fun GlProgramRuntime.setLitModel(lit: LitMesh, model: FloatArray) {
    set(lit.model, model)
    val normals = checkNotNull(normalScratch.get())
    normalMatrix(model, normals)
    set(lit.normalMatrix, normals)
}

private const val NORMAL_MATRIX_FLOATS = 9

/** The runtime copies uniform values into its own cache, so one array per thread is enough. */
private val normalScratch: ThreadLocal<FloatArray> = ThreadLocal.withInitial { FloatArray(NORMAL_MATRIX_FLOATS) }
