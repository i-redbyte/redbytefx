package ru.redbyte.redbytefx.sample.ui.gl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import ru.redbyte.redbytefx.Flt
import ru.redbyte.redbytefx.High
import ru.redbyte.redbytefx.Sampler2D
import ru.redbyte.redbytefx.ShaderProgram
import ru.redbyte.redbytefx.ShaderTarget
import ru.redbyte.redbytefx.Uniform
import ru.redbyte.redbytefx.cos
import ru.redbyte.redbytefx.gl.compose.GlAttrib
import ru.redbyte.redbytefx.gl.compose.GlMesh
import ru.redbyte.redbytefx.lit
import ru.redbyte.redbytefx.minus
import ru.redbyte.redbytefx.plus
import ru.redbyte.redbytefx.sample.ui.say
import ru.redbyte.redbytefx.times
import ru.redbyte.redbytefx.shader
import ru.redbyte.redbytefx.sin
import ru.redbyte.redbytefx.vec4
import ru.redbyte.redbytefx.x
import ru.redbyte.redbytefx.y

internal val mirrorDsl = """
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

internal class MirrorScene(
    val program: ShaderProgram,
    val present: ShaderProgram,
    val mesh: GlMesh,
    val triangle: GlMesh,
    val time: Uniform<Flt<High>>,
    val image: Uniform<Sampler2D>,
)

internal fun mirrorPrograms(): MirrorScene {
    lateinit var time: Uniform<Flt<High>>
    lateinit var image: Uniform<Sampler2D>
    val program = shader(ShaderTarget.Gles30) {
        time = uniform("time", 0f)
        val tint = varyingFloat("tint")
        vertex {
            val position = attributeVec2("corner")
            val angle = time.expr
            val turnedX = position.x * cos(angle) - position.y * sin(angle)
            val turnedY = position.x * sin(angle) + position.y * cos(angle)
            tint.set(position.x)
            glPosition(vec4(turnedX, turnedY, 0f.lit, 1f.lit))
        }
        fragment { vec4(0.95f.lit, 0.45f.lit + tint.expr, 0.2f.lit, 1f.lit) }
    }
    val present = shader(ShaderTarget.Gles30) {
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
    return MirrorScene(program, present, mirrorQuad(), mirrorTriangle(), time, image)
}

internal fun mirrorQuad(): GlMesh = GlMesh(
    vertices = floatArrayOf(
        -0.62f, -0.62f, 0f, 0f,
        0.62f, -0.62f, 1f, 0f,
        0.62f, 0.62f, 1f, 1f,
        -0.62f, 0.62f, 0f, 1f,
    ),
    stride = 4,
    attribs = listOf(GlAttrib("a_corner", 2, 0), GlAttrib("a_uv", 2, 2)),
    clearR = 0.015f,
    clearG = 0.02f,
    clearB = 0.035f,
    indices = intArrayOf(0, 1, 2, 0, 2, 3),
)

internal fun mirrorTriangle(): GlMesh = GlMesh(
    vertices = floatArrayOf(-0.45f, -0.4f, 0.45f, -0.4f, 0f, 0.5f),
    stride = 2,
    attribs = listOf(GlAttrib("a_corner", 2, 0)),
    clearR = 0.05f,
    clearG = 0.08f,
    clearB = 0.16f,
)

@Composable
internal fun DemoMirror() {
    val scene = remember { mirrorPrograms() }
    GlCanvas(
        program = scene.program,
        mesh = scene.mesh,
        present = scene.present,
        renderToTexture = true,
        caption = say(
            "The triangle is drawn into a texture, then onto the quad",
            "Треугольник рисуется в текстуру, затем на квадрат",
        ),
        dsl = mirrorDsl,
    ) { frame ->
        frame.runtime.set(scene.time, frame.seconds)
        frame.offscreen { frame.draw(scene.triangle) }
        val target = frame.colorTarget
        val present = frame.presentRuntime
        if (target != null && present != null) {
            present.bind(scene.image, target.colorTexture)
            frame.draw(present = true)
        }
    }
}
