package ru.redbyte.redbytefx.sample.ui.gl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import ru.redbyte.redbytefx.sample.ui.say
import ru.redbyte.redbytefx.Flt
import ru.redbyte.redbytefx.High
import ru.redbyte.redbytefx.ShaderProgram
import ru.redbyte.redbytefx.ShaderTarget
import ru.redbyte.redbytefx.TessPrimitive
import ru.redbyte.redbytefx.Uniform
import ru.redbyte.redbytefx.lit
import ru.redbyte.redbytefx.mix
import ru.redbyte.redbytefx.minus
import ru.redbyte.redbytefx.plus
import ru.redbyte.redbytefx.shader
import ru.redbyte.redbytefx.sin
import ru.redbyte.redbytefx.times
import ru.redbyte.redbytefx.vec4
import ru.redbyte.redbytefx.x
import ru.redbyte.redbytefx.y
import ru.redbyte.redbytefx.z

internal class OceanScene(
    val program: ShaderProgram,
    val mesh: GlMesh,
    val time: Uniform<Flt<High>>,
)

internal fun oceanScene(): OceanScene {
    lateinit var time: Uniform<Flt<High>>
    val program = shader(ShaderTarget.Gles32) {
        time = uniformTime()
        vertex {
            val corner = attributeVec4("corner")
            glPosition(vec4(corner.x, time.expr, corner.z, 1f.lit))
        }
        tessControl(4) {
            tessLevelOuter(0, 14f.lit)
            tessLevelOuter(1, 14f.lit)
            tessLevelOuter(2, 14f.lit)
            tessLevelOuter(3, 14f.lit)
            tessLevelInner(0, 14f.lit)
            tessLevelInner(1, 14f.lit)
            passPosition()
        }
        tessEval(TessPrimitive.Quads) {
            val u = tessCoord.x
            val v = tessCoord.y
            val x = mix(mix(glIn(0).x, glIn(1).x, u), mix(glIn(3).x, glIn(2).x, u), v)
            val z = mix(mix(glIn(0).z, glIn(1).z, u), mix(glIn(3).z, glIn(2).z, u), v)
            val clock = glIn(0).y
            val height = sin(x * 3.4f.lit + clock * 1.35f.lit) * 0.16f.lit +
                sin(z * 2.3f.lit - clock * 0.9f.lit) * 0.1f.lit
            val depth = z + 0.85f.lit
            glPosition(vec4(x * 0.62f.lit, height - 0.12f.lit, height, depth))
        }
        fragment {
            val crest = 0.5f.lit + 0.5f.lit * sin(time.expr * 1.2f.lit)
            vec4(0.03f.lit, 0.22f.lit + crest * 0.16f.lit, 0.42f.lit + crest * 0.22f.lit, 1f.lit)
        }
    }
    return OceanScene(program, oceanPatch(), time)
}

@Composable
internal fun DemoOcean() {
    val scene = remember { oceanScene() }
    GlCanvas(
        scene.program,
        scene.mesh,
        requirement = say(
            "OpenGL ES 3.2 is required for the ocean.",
            "Для «Океана» нужен OpenGL ES 3.2.",
        ),
        dsl = oceanDsl,
    ) { frame ->
        frame.runtime.set(scene.time, frame.seconds)
    }
}
