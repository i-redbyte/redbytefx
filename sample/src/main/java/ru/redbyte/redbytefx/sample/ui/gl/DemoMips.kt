package ru.redbyte.redbytefx.sample.ui.gl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import ru.redbyte.redbytefx.Sampler2D
import ru.redbyte.redbytefx.ShaderProgram
import ru.redbyte.redbytefx.ShaderTarget
import ru.redbyte.redbytefx.Uniform
import ru.redbyte.redbytefx.gl.GlProgramRuntime
import ru.redbyte.redbytefx.gl.compose.GlAttrib
import ru.redbyte.redbytefx.gl.compose.GlMesh
import ru.redbyte.redbytefx.lit
import ru.redbyte.redbytefx.sample.ui.say
import ru.redbyte.redbytefx.shader
import ru.redbyte.redbytefx.vec4
import ru.redbyte.redbytefx.x
import ru.redbyte.redbytefx.y

internal const val MIP_SIZE: Int = 128

internal val mipsDsl = """
shader(ShaderTarget.Gles30) {
    image = sampler2D("image")
    val uv = varyingVec2("uv")
    vertex {
        val corner = attributeVec2("corner")
        val texcoord = attributeVec2("uv")
        uv.set(texcoord)
        glPosition(vec4(corner.x, corner.y, 0f.lit, 1f.lit))
    }
    fragment { texture(image, uv.expr) }
}
""".trimIndent()

internal class MipScene(
    val program: ShaderProgram,
    val present: ShaderProgram,
    val mesh: GlMesh,
    val right: GlMesh,
    val image: Uniform<Sampler2D>,
    val mipImage: Uniform<Sampler2D>,
)

private class MipImages {
    var runtime: GlProgramRuntime? = null
    var sharp: Int = 0
    var soft: Int = 0
}

internal fun mipPrograms(): MipScene {
    lateinit var image: Uniform<Sampler2D>
    lateinit var mipImage: Uniform<Sampler2D>
    val program = checkerProgram { image = it }
    val present = checkerProgram { mipImage = it }
    return MipScene(program, present, mipQuad(-0.96f, -0.04f), mipQuad(0.04f, 0.96f), image, mipImage)
}

internal fun checkerRgba(size: Int, cell: Int): ByteArray {
    val rgba = ByteArray(size * size * 4)
    var y = 0
    while (y < size) {
        var x = 0
        while (x < size) {
            val on = (x / cell + y / cell) % 2 == 0
            val value = if (on) -1 else 16.toByte()
            val index = (y * size + x) * 4
            rgba[index] = value
            rgba[index + 1] = value
            rgba[index + 2] = value
            rgba[index + 3] = -1
            x += 1
        }
        y += 1
    }
    return rgba
}

@Composable
internal fun DemoMips() {
    val scene = remember { mipPrograms() }
    val images = remember { MipImages() }
    GlCanvas(
        program = scene.program,
        mesh = scene.mesh,
        present = scene.present,
        caption = say(
            "Left is level 0. Right uses the mip chain",
            "Слева только уровень 0. Справа цепочка мип-уровней",
        ),
        dsl = mipsDsl,
    ) { frame ->
        if (images.runtime !== frame.runtime) {
            val pixels = checkerRgba(MIP_SIZE, 1)
            images.sharp = frame.runtime.uploadRgba(MIP_SIZE, MIP_SIZE, pixels)
            images.soft = frame.runtime.uploadRgba(MIP_SIZE, MIP_SIZE, pixels)
            frame.runtime.generateMipmap2D(images.soft)
            frame.runtime.filterMipmap2D(images.soft)
            images.runtime = frame.runtime
        }
        frame.runtime.bind(scene.image, images.sharp)
        frame.presentRuntime?.bind(scene.mipImage, images.soft)
        frame.draw()
        frame.draw(scene.right, present = true)
    }
}

private fun checkerProgram(image: (Uniform<Sampler2D>) -> Unit): ShaderProgram = shader(ShaderTarget.Gles30) {
    val sampler = sampler2D("image")
    image(sampler)
    val uv = varyingVec2("uv")
    vertex {
        val corner = attributeVec2("corner")
        val texcoord = attributeVec2("uv")
        uv.set(texcoord)
        glPosition(vec4(corner.x, corner.y, 0f.lit, 1f.lit))
    }
    fragment { texture(sampler, uv.expr) }
}

private fun mipQuad(x0: Float, x1: Float): GlMesh = GlMesh(
    vertices = floatArrayOf(x0, -0.72f, 0f, 0f, x1, -0.72f, 8f, 0f, x1, 0.72f, 8f, 8f, x0, 0.72f, 0f, 8f),
    stride = 4,
    attribs = listOf(GlAttrib("a_corner", 2, 0), GlAttrib("a_uv", 2, 2)),
    clearR = 0.02f,
    clearG = 0.025f,
    clearB = 0.04f,
    indices = intArrayOf(0, 1, 2, 0, 2, 3),
)
