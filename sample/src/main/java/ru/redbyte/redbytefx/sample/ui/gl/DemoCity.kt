package ru.redbyte.redbytefx.sample.ui.gl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import ru.redbyte.redbytefx.gl.compose.GlMesh
import kotlin.math.cos
import kotlin.math.sin
import ru.redbyte.redbytefx.Flt
import ru.redbyte.redbytefx.High
import ru.redbyte.redbytefx.ShaderProgram
import ru.redbyte.redbytefx.ShaderTarget
import ru.redbyte.redbytefx.Uniform
import ru.redbyte.redbytefx.UniformBlock
import ru.redbyte.redbytefx.abs
import ru.redbyte.redbytefx.div
import ru.redbyte.redbytefx.gt
import ru.redbyte.redbytefx.ifElse
import ru.redbyte.redbytefx.lit
import ru.redbyte.redbytefx.minus
import ru.redbyte.redbytefx.plus
import ru.redbyte.redbytefx.saturate
import ru.redbyte.redbytefx.shader
import ru.redbyte.redbytefx.sin as wave
import ru.redbyte.redbytefx.times
import ru.redbyte.redbytefx.vec2
import ru.redbyte.redbytefx.vec3
import ru.redbyte.redbytefx.vec4
import ru.redbyte.redbytefx.w
import ru.redbyte.redbytefx.x
import ru.redbyte.redbytefx.y
import ru.redbyte.redbytefx.z

internal class CityScene(
    val program: ShaderProgram,
    val mesh: GlMesh,
    val frameBlock: UniformBlock,
    val aspect: Uniform<Flt<High>>,
)

internal fun cityScene(): CityScene {
    lateinit var frameBlock: UniformBlock
    lateinit var aspect: Uniform<Flt<High>>
    val program = shader(ShaderTarget.Gles30) {
        aspect = uniform("aspect", 1f)
        val mark = varyingVec2("mark")
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
                val position = attributeVec4("position")
                val dx = position.x - eyeX
                val dy = position.y - eyeY
                val dz = position.z - eyeZ
                val viewX = dx * rightX + dz * rightZ
                val viewZ = dx * forwardX + dz * forwardZ
                mark.set(vec2(position.w, position.y))
                glPosition(vec4(viewX / aspect.expr, dy, viewZ * 0.2f.lit - 0.5f.lit, viewZ))
            }
            fragment {
                val id = mark.expr.x
                val height = saturate(mark.expr.y * 0.7f.lit + 0.25f.lit)
                val ground = id gt 4.5f.lit
                val breathe = 0.82f.lit + 0.18f.lit * abs(wave(time * 1.4f.lit + id))
                val facade = vec3(
                    0.28f.lit + 0.55f.lit * abs(wave(id * 1.3f.lit)),
                    0.32f.lit + 0.4f.lit * abs(wave(id * 2.1f.lit + 1f.lit)),
                    0.5f.lit + 0.35f.lit * abs(wave(id * 0.8f.lit + 2f.lit)),
                ) * (0.4f.lit + height * 0.75f.lit) * breathe
                val street = vec3(0.04f.lit, 0.05f.lit, 0.07f.lit)
                val rgb = ifElse(ground, street, facade)
                vec4(rgb.x, rgb.y, rgb.z, 1f.lit)
            }
        }
    }
    return CityScene(program, cityMesh(), frameBlock, aspect)
}

@Composable
internal fun DemoCity() {
    val scene = remember { cityScene() }
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
        frame.runtime.set(scene.frameBlock, floatArrayOf(
            frame.seconds,
            eyeX,
            eyeY,
            eyeZ,
            rightX,
            rightZ,
            forwardX,
            forwardZ,
        ))
        frame.runtime.set(scene.aspect, frame.aspect)
    }
}
