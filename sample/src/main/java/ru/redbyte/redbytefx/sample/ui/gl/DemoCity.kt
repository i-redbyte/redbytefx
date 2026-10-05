package ru.redbyte.redbytefx.sample.ui.gl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import ru.redbyte.redbytefx.Flt
import ru.redbyte.redbytefx.High
import ru.redbyte.redbytefx.Sampler2D
import ru.redbyte.redbytefx.ShaderProgram
import ru.redbyte.redbytefx.ShaderTarget
import ru.redbyte.redbytefx.Uniform
import ru.redbyte.redbytefx.UniformBlock
import ru.redbyte.redbytefx.abs
import ru.redbyte.redbytefx.and
import ru.redbyte.redbytefx.div
import ru.redbyte.redbytefx.float2
import ru.redbyte.redbytefx.gl.GlProgramRuntime
import ru.redbyte.redbytefx.gl.compose.GlMesh
import ru.redbyte.redbytefx.gt
import ru.redbyte.redbytefx.ifElse
import ru.redbyte.redbytefx.lit
import ru.redbyte.redbytefx.lt
import ru.redbyte.redbytefx.minus
import ru.redbyte.redbytefx.plus
import ru.redbyte.redbytefx.saturate
import ru.redbyte.redbytefx.shader
import ru.redbyte.redbytefx.times
import ru.redbyte.redbytefx.vec4
import ru.redbyte.redbytefx.x
import ru.redbyte.redbytefx.y
import ru.redbyte.redbytefx.z
import kotlin.math.cos
import kotlin.math.sin
import ru.redbyte.redbytefx.sin as wave

internal const val CITY_TEX: Int = 32

internal class CityScene(
    val program: ShaderProgram,
    val mesh: GlMesh,
    val frameBlock: UniformBlock,
    val aspect: Uniform<Flt<High>>,
    val facade: Uniform<Sampler2D>,
    val ground: Uniform<Sampler2D>,
)

private class CityImages {
    var runtime: GlProgramRuntime? = null
    var facade: Int = 0
    var ground: Int = 0
}

internal fun cityScene(): CityScene {
    lateinit var frameBlock: UniformBlock
    lateinit var aspect: Uniform<Flt<High>>
    lateinit var facade: Uniform<Sampler2D>
    lateinit var ground: Uniform<Sampler2D>
    val program = shader(ShaderTarget.Gles30) {
        aspect = uniform("aspect", 1f)
        facade = sampler2D("facade")
        ground = sampler2D("ground")
        val uv = varyingVec2("uv")
        val facing = varyingVec3("normal")
        val height = varyingFloat("height")
        frameBlock = uniformBlock("frame") {
            val time = float("time")
            val eyeX = float("eyeX")
            val eyeY = float("eyeY")
            val eyeZ = float("eyeZ")
            val rightX = float("rightX")
            val rightZ = float("rightZ")
            val forwardX = float("forwardX")
            val forwardZ = float("forwardZ")
            vertex {
                val position = attributeVec3("position")
                val normal = attributeVec3("normal")
                val texcoord = attributeVec2("uv")
                val dx = position.x - eyeX
                val dy = position.y - eyeY
                val dz = position.z - eyeZ
                val viewX = dx * rightX + dz * rightZ
                val viewZ = dx * forwardX + dz * forwardZ
                uv.set(texcoord)
                facing.set(normal)
                height.set(position.y)
                glPosition(vec4(viewX / aspect.expr, dy, viewZ * 0.2f.lit - 0.5f.lit, viewZ))
            }
            fragment {
                val street = (height.expr lt 0.02f.lit) and (facing.expr.y gt 0.85f.lit)
                val texel = ifElse(
                    street,
                    texture(ground, uv.expr * float2(4f, 4f)),
                    texture(facade, uv.expr * float2(2f, 3f)),
                )
                val breathe = 0.94f.lit + 0.06f.lit * abs(wave(time * 1.4f.lit))
                val light = saturate(facing.expr.y * 0.35f.lit + facing.expr.z * 0.2f.lit + 0.55f.lit) * breathe
                vec4(texel.x * light, texel.y * light, texel.z * light, 1f.lit)
            }
        }
    }
    return CityScene(program, cityMesh(), frameBlock, aspect, facade, ground)
}

@Composable
internal fun DemoCity() {
    val scene = remember { cityScene() }
    val images = remember { CityImages() }
    GlCanvas(scene.program, scene.mesh, dsl = cityDsl) { frame ->
        val angle = frame.seconds * 0.35f
        val eyeX = sin(angle) * 3.1f
        val eyeZ = cos(angle) * 3.1f
        val eyeY = 1.35f
        val length = kotlin.math.hypot(eyeX, eyeZ)
        val forwardX = -eyeX / length
        val forwardZ = -eyeZ / length
        val rightX = forwardZ
        val rightZ = -forwardX
        frame.runtime.set(
            scene.frameBlock,
            floatArrayOf(
                frame.seconds,
                eyeX,
                eyeY,
                eyeZ,
                rightX,
                rightZ,
                forwardX,
                forwardZ,
            ),
        )
        frame.runtime.set(scene.aspect, frame.aspect)
        if (images.runtime !== frame.runtime) {
            images.facade = frame.runtime.uploadRgba(CITY_TEX, CITY_TEX, facadeRgba())
            images.ground = frame.runtime.uploadRgba(CITY_TEX, CITY_TEX, asphaltRgba())
            images.runtime = frame.runtime
        }
        frame.runtime.bind(scene.facade, images.facade)
        frame.runtime.bind(scene.ground, images.ground)
    }
}

/** Dark mortar, blue wall, warm window panes. */
internal fun facadeRgba(): ByteArray = raster(CITY_TEX) { x, y ->
    val mortar = x % 8 == 0 || y % 8 == 0
    val pane = x % 8 in 2..5 && y % 8 in 2..6
    when {
        mortar -> byteColor(28, 32, 40)
        pane -> byteColor(255, 214, 120)
        else -> byteColor(48, 72, 110)
    }
}

/** Gray asphalt with a dark crack grid. Distinct from [facadeRgba]. */
internal fun asphaltRgba(): ByteArray = raster(CITY_TEX) { x, y ->
    val crack = (x + y * 3) % 19 == 0 || (x * 5 + y) % 23 == 0
    val grain = ((x * 3) xor (y * 5)) and 15
    val shade = if (crack) 18 else 46 + grain
    byteColor(shade, shade + 2, shade + 5)
}

private fun raster(size: Int, pixel: (Int, Int) -> ByteArray): ByteArray {
    val rgba = ByteArray(size * size * 4)
    var cursor = 0
    for (y in 0 until size) {
        for (x in 0 until size) {
            val sample = pixel(x, y)
            rgba[cursor] = sample[0]
            rgba[cursor + 1] = sample[1]
            rgba[cursor + 2] = sample[2]
            rgba[cursor + 3] = sample[3]
            cursor += 4
        }
    }
    return rgba
}

private fun byteColor(red: Int, green: Int, blue: Int): ByteArray =
    byteArrayOf(red.toByte(), green.toByte(), blue.toByte(), 255.toByte())
