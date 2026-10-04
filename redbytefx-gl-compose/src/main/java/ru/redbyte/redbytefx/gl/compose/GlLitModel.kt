package ru.redbyte.redbytefx.gl.compose

import ru.redbyte.redbytefx.gl.GlProgramRuntime
import ru.redbyte.redbytefx.scene.LitMesh
import ru.redbyte.redbytefx.scene.normalMatrix

/**
 * Uploads [model] and the matching [LitMesh.normalMatrix] for [litTexturedMesh].
 *
 * Call whenever the model matrix changes at runtime so lighting stays correct under non-uniform scale.
 */
public fun GlProgramRuntime.setLitModel(lit: LitMesh, model: FloatArray) {
    set(lit.model, model)
    val normals = FloatArray(9)
    normalMatrix(model, normals)
    set(lit.normalMatrix, normals)
}
