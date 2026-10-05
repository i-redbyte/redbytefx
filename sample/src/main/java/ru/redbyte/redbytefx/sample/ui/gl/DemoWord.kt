package ru.redbyte.redbytefx.sample.ui.gl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import ru.redbyte.redbytefx.Flt
import ru.redbyte.redbytefx.High
import ru.redbyte.redbytefx.ShaderProgram
import ru.redbyte.redbytefx.ShaderTarget
import ru.redbyte.redbytefx.Uniform
import ru.redbyte.redbytefx.div
import ru.redbyte.redbytefx.gl.compose.GlAttrib
import ru.redbyte.redbytefx.gl.compose.GlMesh
import ru.redbyte.redbytefx.gt
import ru.redbyte.redbytefx.ifElse
import ru.redbyte.redbytefx.lit
import ru.redbyte.redbytefx.plus
import ru.redbyte.redbytefx.shader
import ru.redbyte.redbytefx.sin
import ru.redbyte.redbytefx.times
import ru.redbyte.redbytefx.vec2
import ru.redbyte.redbytefx.vec3
import ru.redbyte.redbytefx.vec4
import ru.redbyte.redbytefx.x
import ru.redbyte.redbytefx.y
import ru.redbyte.redbytefx.z

internal class WordScene(
    val program: ShaderProgram,
    val mesh: GlMesh,
    val time: Uniform<Flt<High>>,
    val aspect: Uniform<Flt<High>>,
    val landed: Uniform<Flt<High>>,
)

internal fun wordScene(vertices: FloatArray): WordScene {
    lateinit var time: Uniform<Flt<High>>
    lateinit var aspect: Uniform<Flt<High>>
    lateinit var landed: Uniform<Flt<High>>
    val program = shader(ShaderTarget.Gles30) {
        time = uniformTime()
        aspect = uniform("aspect", 1f)
        landed = uniform("landed", 0f)
        val paint = varyingVec2("paint")
        vertex {
            val position = attributeVec4("position")
            val tint = attributeVec4("tint")
            val depth = position.z + WORD_CAMERA_Z.lit
            paint.set(vec2(tint.x, tint.y))
            glPosition(vec4(position.x / aspect.expr, position.y, position.z * 4f.lit, depth))
        }
        fragment {
            val id = paint.expr.x
            val shade = paint.expr.y
            val hue = time.expr * 0.85f.lit + id * 0.75f.lit
            val color = vec3(
                0.5f.lit + 0.5f.lit * sin(hue),
                0.5f.lit + 0.5f.lit * sin(hue + 2.094f.lit),
                0.5f.lit + 0.5f.lit * sin(hue + 4.188f.lit),
            )
            val metal = vec3(0.72f.lit, 0.9f.lit, 1f.lit)
            val rgb = ifElse(landed.expr gt 0.5f.lit, color, metal) * (0.3f.lit + shade * 0.75f.lit)
            vec4(rgb.x, rgb.y, rgb.z, 1f.lit)
        }
    }
    val mesh = GlMesh(
        vertices = vertices,
        stride = 8,
        attribs = listOf(GlAttrib("a_position", 4, 0), GlAttrib("a_tint", 4, 4)),
        depth = true,
        clearR = 0.015f,
        clearG = 0.02f,
        clearB = 0.04f,
    )
    return WordScene(program, mesh, time, aspect, landed)
}

@Composable
internal fun DemoWord() {
    val model = remember {
        WordModel().also { it.pose(0f) }
    }
    val scene = remember { wordScene(model.posed) }
    GlCanvas(scene.program, scene.mesh, dsl = wordDsl) { frame ->
        model.pose(frame.seconds)
        frame.replace(model.posed)
        frame.runtime.set(scene.time, frame.seconds)
        frame.runtime.set(scene.aspect, frame.aspect)
        frame.runtime.set(scene.landed, if (wordSettled(frame.seconds)) 1f else 0f)
    }
}
