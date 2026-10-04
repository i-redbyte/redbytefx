package ru.redbyte.redbytefx.sample.ui.gl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlin.math.cos
import kotlin.math.sin
import ru.redbyte.redbytefx.gl.compose.GlMesh
import ru.redbyte.redbytefx.gl.compose.setLitModel
import ru.redbyte.redbytefx.gl.compose.sphere
import ru.redbyte.redbytefx.gl.GlProgramRuntime
import ru.redbyte.redbytefx.scene.LitMesh
import ru.redbyte.redbytefx.scene.MATRIX_FLOATS
import ru.redbyte.redbytefx.scene.litTexturedMesh
import ru.redbyte.redbytefx.scene.lookAt
import ru.redbyte.redbytefx.scene.ortho

private const val LIT_TEX: Int = 32

private class LitTexturedImages {
    var runtime: GlProgramRuntime? = null
    var albedo: Int = 0
}

private fun checkerRgba(size: Int): ByteArray {
    val pixels = ByteArray(size * size * 4)
    for (y in 0 until size) {
        for (x in 0 until size) {
            val checker = ((x / 4) + (y / 4)) % 2 == 0
            val r = if (checker) 220 else 40
            val g = if (checker) 90 else 30
            val b = if (checker) 40 else 120
            val index = (y * size + x) * 4
            pixels[index] = r.toByte()
            pixels[index + 1] = g.toByte()
            pixels[index + 2] = b.toByte()
            pixels[index + 3] = 255.toByte()
        }
    }
    return pixels
}

private fun rotationY(radians: Float, out: FloatArray) {
    val c = cos(radians)
    val s = sin(radians)
    out.fill(0f)
    out[0] = c
    out[1] = 0f
    out[2] = s
    out[3] = 0f
    out[4] = 0f
    out[5] = 1f
    out[6] = 0f
    out[7] = 0f
    out[8] = -s
    out[9] = 0f
    out[10] = c
    out[11] = 0f
    out[12] = 0f
    out[13] = 0f
    out[14] = 0f
    out[15] = 1f
}

@Composable
internal fun DemoLitTextured() {
    val lit: LitMesh = remember { litTexturedMesh() }
    val mesh: GlMesh = remember { sphere(0.55f) }
    val camera = remember { CameraMatrices() }
    val model = remember { FloatArray(MATRIX_FLOATS) }
    val images = remember { LitTexturedImages() }
    GlCanvas(lit.program, mesh, dsl = litTexturedDsl) { frame ->
        val span = 1.35f * frame.aspect.coerceAtLeast(0.25f)
        frame.runtime.set(
            lit.projection,
            ortho(-span, span, -1.35f, 1.35f, 0.1f, 12f, camera.projection),
        )
        frame.runtime.set(
            lit.view,
            lookAt(0f, 0.15f, 3.2f, 0f, 0f, 0f, 0f, 1f, 0f, camera.view),
        )
        frame.runtime.set(lit.light, 0.25f, 0.9f, 0.35f)
        rotationY(frame.seconds * 0.75f, model)
        frame.runtime.setLitModel(lit, model)
        if (images.runtime !== frame.runtime) {
            images.albedo = frame.runtime.uploadRgba(LIT_TEX, LIT_TEX, checkerRgba(LIT_TEX))
            images.runtime = frame.runtime
        }
        frame.runtime.bind(lit.albedo, images.albedo)
    }
}
