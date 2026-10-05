package ru.redbyte.redbytefx.sample.ui.gl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import ru.redbyte.redbytefx.Flt
import ru.redbyte.redbytefx.High
import ru.redbyte.redbytefx.Mat4
import ru.redbyte.redbytefx.Sampler2D
import ru.redbyte.redbytefx.ShaderProgram
import ru.redbyte.redbytefx.ShaderTarget
import ru.redbyte.redbytefx.Uniform
import ru.redbyte.redbytefx.Vec3
import ru.redbyte.redbytefx.and
import ru.redbyte.redbytefx.float2
import ru.redbyte.redbytefx.gl.GlProgramRuntime
import ru.redbyte.redbytefx.gl.compose.GlMesh
import ru.redbyte.redbytefx.gl.compose.box
import ru.redbyte.redbytefx.gt
import ru.redbyte.redbytefx.ifElse
import ru.redbyte.redbytefx.lit
import ru.redbyte.redbytefx.lt
import ru.redbyte.redbytefx.scene.MATRIX_FLOATS
import ru.redbyte.redbytefx.scene.lookAt
import ru.redbyte.redbytefx.scene.perspective
import ru.redbyte.redbytefx.shader
import ru.redbyte.redbytefx.stdlib.lambert
import ru.redbyte.redbytefx.times
import ru.redbyte.redbytefx.vec4
import ru.redbyte.redbytefx.x
import ru.redbyte.redbytefx.y
import ru.redbyte.redbytefx.z
import kotlin.math.cos
import kotlin.math.sin

internal class CrateScene(
    val program: ShaderProgram,
    val mesh: GlMesh,
    val view: Uniform<Mat4>,
    val projection: Uniform<Mat4>,
    val light: Uniform<Vec3<Flt<High>>>,
    val facade: Uniform<Sampler2D>,
    val ground: Uniform<Sampler2D>,
)

private class CrateImages {
    var runtime: GlProgramRuntime? = null
    var facade: Int = 0
    var ground: Int = 0
}

internal fun crateScene(): CrateScene {
    lateinit var view: Uniform<Mat4>
    lateinit var projection: Uniform<Mat4>
    lateinit var light: Uniform<Vec3<Flt<High>>>
    lateinit var facade: Uniform<Sampler2D>
    lateinit var ground: Uniform<Sampler2D>
    val program = shader(ShaderTarget.Gles30) {
        view = uniformMat4("view")
        projection = uniformMat4("projection")
        light = uniformVec3("light", 0.35f, 0.85f, 0.4f)
        facade = sampler2D("facade")
        ground = sampler2D("ground")
        val uv = varyingVec2("uv")
        val facing = varyingVec3("normal")
        val height = varyingFloat("height")
        vertex {
            val position = attributeVec3("position")
            val normal = attributeVec3("normal")
            val texcoord = attributeVec2("uv")
            uv.set(texcoord)
            facing.set(normal)
            height.set(position.y)
            glPosition(projection.expr * (view.expr * vec4(position.x, position.y, position.z, 1f.lit)))
        }
        fragment {
            val street = (height.expr lt 0.02f.lit) and (facing.expr.y gt 0.85f.lit)
            val texel = ifElse(
                street,
                texture(ground, uv.expr * float2(4f, 4f)),
                texture(facade, uv.expr),
            )
            val shade = lambert(facing.expr, light.expr)
            vec4(texel.x * shade, texel.y * shade, texel.z * shade, 1f.lit)
        }
    }
    return CrateScene(program, crateMesh(), view, projection, light, facade, ground)
}

/** View and projection arrays reused every frame by the camera demos. */
internal class CameraMatrices {
    val view = FloatArray(MATRIX_FLOATS)
    val projection = FloatArray(MATRIX_FLOATS)
}

internal fun crateMesh(): GlMesh = mergeMeshes(
    listOf(
        box(0f, 0.4f, 0f, 0.35f, 0.35f, 0.35f),
        box(0f, -0.02f, 0f, 1.4f, 0.02f, 1.4f),
    ),
    clearR = 0.03f,
    clearG = 0.035f,
    clearB = 0.05f,
)

@Composable
internal fun DemoCrate() {
    val scene = remember { crateScene() }
    val images = remember { CrateImages() }
    val camera = remember { CameraMatrices() }
    GlCanvas(scene.program, scene.mesh, dsl = crateDsl) { frame ->
        val angle = frame.seconds * 0.45f
        val eyeX = sin(angle) * 2.6f
        val eyeZ = cos(angle) * 2.6f
        frame.runtime.set(
            scene.view,
            lookAt(eyeX, 1.35f, eyeZ, 0f, 0.25f, 0f, 0f, 1f, 0f, camera.view),
        )
        frame.runtime.set(scene.projection, perspective(0.9f, frame.aspect, 0.08f, 30f, camera.projection))
        frame.runtime.set(scene.light, 0.35f, 0.85f, 0.4f)
        if (images.runtime !== frame.runtime) {
            images.facade = frame.runtime.uploadRgba(CITY_TEX, CITY_TEX, facadeRgba())
            images.ground = frame.runtime.uploadRgba(CITY_TEX, CITY_TEX, asphaltRgba())
            images.runtime = frame.runtime
        }
        frame.runtime.bind(scene.facade, images.facade)
        frame.runtime.bind(scene.ground, images.ground)
    }
}
