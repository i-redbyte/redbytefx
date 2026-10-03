package ru.redbyte.redbytefx.sample.ui.gl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlin.math.cos
import kotlin.math.sin
import ru.redbyte.redbytefx.Mat4
import ru.redbyte.redbytefx.SamplerCube
import ru.redbyte.redbytefx.ShaderProgram
import ru.redbyte.redbytefx.ShaderTarget
import ru.redbyte.redbytefx.Uniform
import ru.redbyte.redbytefx.gl.CubeFace
import ru.redbyte.redbytefx.gl.GlProgramRuntime
import ru.redbyte.redbytefx.gl.compose.GlMesh
import ru.redbyte.redbytefx.gl.compose.sphere
import ru.redbyte.redbytefx.lit
import ru.redbyte.redbytefx.scene.lookAt
import ru.redbyte.redbytefx.scene.perspective
import ru.redbyte.redbytefx.shader
import ru.redbyte.redbytefx.times
import ru.redbyte.redbytefx.vec4
import ru.redbyte.redbytefx.x
import ru.redbyte.redbytefx.y
import ru.redbyte.redbytefx.z

internal val skyDsl = """
shader(ShaderTarget.Gles30) {
    view = uniformMat4("view")
    projection = uniformMat4("projection")
    sky = samplerCube("sky")
    val direction = varyingVec3("direction")
    vertex {
        val position = attributeVec3("position")
        direction.set(position)
        glPosition(projection.expr * (view.expr * vec4(position.x, position.y, position.z, 1f.lit)))
    }
    fragment { textureCube(sky, direction.expr) }
}
""".trimIndent()

internal class SkyScene(
    val program: ShaderProgram,
    val mesh: GlMesh,
    val view: Uniform<Mat4>,
    val projection: Uniform<Mat4>,
    val sky: Uniform<SamplerCube>,
)

private class SkyImages {
    var runtime: GlProgramRuntime? = null
    var texture: Int = 0
}

internal fun skyScene(): SkyScene {
    lateinit var view: Uniform<Mat4>
    lateinit var projection: Uniform<Mat4>
    lateinit var sky: Uniform<SamplerCube>
    val program = shader(ShaderTarget.Gles30) {
        view = uniformMat4("view")
        projection = uniformMat4("projection")
        sky = samplerCube("sky")
        val direction = varyingVec3("direction")
        vertex {
            val position = attributeVec3("position")
            direction.set(position)
            glPosition(projection.expr * (view.expr * vec4(position.x, position.y, position.z, 1f.lit)))
        }
        fragment { textureCube(sky, direction.expr) }
    }
    return SkyScene(program, sphere(1f, 20, 32), view, projection, sky)
}

@Composable
internal fun DemoSky() {
    val scene = remember { skyScene() }
    val images = remember { SkyImages() }
    val camera = remember { CameraMatrices() }
    GlCanvas(scene.program, scene.mesh, dsl = skyDsl) { frame ->
        val yaw = frame.seconds * 0.35f
        frame.runtime.set(scene.view, lookAt(sin(yaw) * 2.6f, 0.35f, cos(yaw) * 2.6f, 0f, 0f, 0f, 0f, 1f, 0f, camera.view))
        frame.runtime.set(scene.projection, perspective(0.9f, frame.aspect, 0.08f, 20f, camera.projection))
        if (images.runtime !== frame.runtime) {
            val name = frame.runtime.createTexture()
            CubeFace.entries.forEach { face ->
                frame.runtime.uploadCubeFace(name, face, 1, 1, skyFace(face))
            }
            images.texture = name
            images.runtime = frame.runtime
        }
        frame.runtime.bind(scene.sky, images.texture)
    }
}

private fun skyFace(face: CubeFace): ByteArray = when (face) {
    CubeFace.PositiveX -> faceColor(220, 48, 48)
    CubeFace.NegativeX -> faceColor(40, 190, 190)
    CubeFace.PositiveY -> faceColor(230, 230, 230)
    CubeFace.NegativeY -> faceColor(40, 150, 60)
    CubeFace.PositiveZ -> faceColor(48, 90, 210)
    CubeFace.NegativeZ -> faceColor(230, 190, 40)
}

private fun faceColor(red: Int, green: Int, blue: Int): ByteArray =
    byteArrayOf(red.toByte(), green.toByte(), blue.toByte(), -1)
