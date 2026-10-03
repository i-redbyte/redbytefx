package ru.redbyte.redbytefx.sample.ui.gl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import ru.redbyte.redbytefx.gl.compose.GlMesh
import ru.redbyte.redbytefx.gl.compose.glEs32LinkRequirement
import ru.redbyte.redbytefx.Flt
import ru.redbyte.redbytefx.GeometryInput
import ru.redbyte.redbytefx.GeometryOutput
import ru.redbyte.redbytefx.High
import ru.redbyte.redbytefx.HighVec4
import ru.redbyte.redbytefx.ShaderProgram
import ru.redbyte.redbytefx.ShaderTarget
import ru.redbyte.redbytefx.Uniform
import ru.redbyte.redbytefx.cos
import ru.redbyte.redbytefx.div
import ru.redbyte.redbytefx.length
import ru.redbyte.redbytefx.lit
import ru.redbyte.redbytefx.max
import ru.redbyte.redbytefx.minus
import ru.redbyte.redbytefx.plus
import ru.redbyte.redbytefx.shader
import ru.redbyte.redbytefx.sin
import ru.redbyte.redbytefx.times
import ru.redbyte.redbytefx.vec3
import ru.redbyte.redbytefx.vec4
import ru.redbyte.redbytefx.x
import ru.redbyte.redbytefx.y
import ru.redbyte.redbytefx.z

internal class HedgehogScene(
    val program: ShaderProgram,
    val mesh: GlMesh,
    val time: Uniform<Flt<High>>,
)

internal fun hedgehogScene(): HedgehogScene {
    lateinit var time: Uniform<Flt<High>>
    val program = shader(ShaderTarget.Gles32) {
        time = uniformTime()
        vertex {
            val position = attributeVec4("position")
            val angle = time.expr * 0.55f.lit
            val turn = cos(angle)
            val lift = sin(angle)
            val x = position.x * turn + position.z * lift
            val z = position.z * turn - position.x * lift + 1.55f.lit
            glPosition(vec4(x, position.y, z, 1f.lit))
        }
        geometry(GeometryInput.Triangles, GeometryOutput.TriangleStrip, 12) {
            val a = glIn(0)
            val b = glIn(1)
            val c = glIn(2)
            val abx = b.x - a.x
            val aby = b.y - a.y
            val abz = b.z - a.z
            val acx = c.x - a.x
            val acy = c.y - a.y
            val acz = c.z - a.z
            val nx = aby * acz - abz * acy
            val ny = abz * acx - abx * acz
            val nz = abx * acy - aby * acx
            val scale = 0.34f.lit / max(length(vec3(nx, ny, nz)), 0.0001f.lit)
            val tip = vec4(
                (a.x + b.x + c.x) / 3f.lit + nx * scale,
                (a.y + b.y + c.y) / 3f.lit + ny * scale,
                (a.z + b.z + c.z) / 3f.lit + nz * scale,
                1f.lit,
            )
            emitEye(a, b, c)
            emitEye(a, b, tip)
            emitEye(b, c, tip)
            emitEye(c, a, tip)
        }
        fragment {
            val pulse = 0.55f.lit + 0.45f.lit * sin(time.expr * 2.2f.lit)
            vec4(0.95f.lit, 0.32f.lit + pulse * 0.35f.lit, 0.12f.lit, 1f.lit)
        }
    }
    return HedgehogScene(program, solidPositions(0.62f), time)
}

@Composable
internal fun DemoHedgehog() {
    val scene = remember { hedgehogScene() }
    GlCanvas(
        scene.program,
        scene.mesh,
        requirement = glEs32LinkRequirement(),
        dsl = hedgehogDsl,
    ) { frame ->
        frame.runtime.set(scene.time, frame.seconds)
    }
}

private fun ru.redbyte.redbytefx.GeometryDsl.emitEye(a: HighVec4, b: HighVec4, c: HighVec4) {
    glPosition(projectEye(a))
    emitVertex()
    glPosition(projectEye(b))
    emitVertex()
    glPosition(projectEye(c))
    emitVertex()
    endPrimitive()
}

