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

internal const val STAMP_SIZE: Int = 64

internal const val STAMP_CELLS: Int = 4

internal val stampDsl = """
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

internal class StampScene(
    val program: ShaderProgram,
    val mesh: GlMesh,
    val image: Uniform<Sampler2D>,
)

private class StampImages {
    var runtime: GlProgramRuntime? = null
    var texture: Int = 0
    var serial: Int = -1
}

internal fun stampScene(): StampScene {
    lateinit var image: Uniform<Sampler2D>
    val program = shader(ShaderTarget.Gles30) {
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
    return StampScene(program, stampMesh(), image)
}

internal fun stampMesh(): GlMesh = GlMesh(
    vertices = floatArrayOf(
        -0.85f, -0.85f, 0f, 0f,
        0.85f, -0.85f, 1f, 0f,
        0.85f, 0.85f, 1f, 1f,
        -0.85f, 0.85f, 0f, 1f,
    ),
    stride = 4,
    attribs = listOf(GlAttrib("a_corner", 2, 0), GlAttrib("a_uv", 2, 2)),
    clearR = 0.02f,
    clearG = 0.025f,
    clearB = 0.04f,
    indices = intArrayOf(0, 1, 2, 0, 2, 3),
)

internal fun stampCell(x: Float, y: Float, cells: Int = STAMP_CELLS): Pair<Int, Int> {
    val column = ((x + 1f) * 0.5f * cells).toInt().coerceIn(0, cells - 1)
    val row = ((y + 1f) * 0.5f * cells).toInt().coerceIn(0, cells - 1)
    return column to row
}

/** One opaque color over an [edge] x [edge] RGBA square. */
internal fun stampFill(edge: Int, red: Int, green: Int, blue: Int): ByteArray {
    val rgba = ByteArray(edge * edge * 4)
    var index = 0
    while (index < rgba.size) {
        rgba[index] = red.toByte()
        rgba[index + 1] = green.toByte()
        rgba[index + 2] = blue.toByte()
        rgba[index + 3] = -1
        index += 4
    }
    return rgba
}

@Composable
internal fun DemoStamp() {
    val scene = remember { stampScene() }
    val images = remember { StampImages() }
    val pointer = remember { PointerState() }
    GlCanvas(
        program = scene.program,
        mesh = scene.mesh,
        pointer = pointer,
        caption = say(
            "Drag to paint the texture in place",
            "Ведите пальцем — краска ложится в ту же текстуру",
        ),
        dsl = stampDsl,
    ) { frame ->
        if (images.runtime !== frame.runtime) {
            images.texture = frame.runtime.uploadRgba(STAMP_SIZE, STAMP_SIZE, stampFill(STAMP_SIZE, 30, 70, 140))
            images.runtime = frame.runtime
            images.serial = -1
        }
        val touch = pointer.snapshot()
        val serial = touch.serial
        if (serial != 0 && serial != images.serial) {
            val (column, row) = stampCell(touch.x, touch.y)
            val cell = STAMP_SIZE / STAMP_CELLS
            val ink = if (serial % 2 == 0) stampFill(cell, 220, 70, 40) else stampFill(cell, 240, 200, 60)
            frame.runtime.texSubImage2DRgba(
                images.texture,
                STAMP_SIZE,
                STAMP_SIZE,
                column * cell,
                row * cell,
                cell,
                cell,
                ink,
            )
            images.serial = serial
        }
        frame.runtime.bind(scene.image, images.texture)
    }
}
