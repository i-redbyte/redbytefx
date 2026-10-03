package ru.redbyte.redbytefx.sample.ui.gl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import ru.redbyte.redbytefx.Flt
import ru.redbyte.redbytefx.GeometryDsl
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
import ru.redbyte.redbytefx.unaryMinus
import ru.redbyte.redbytefx.vec3
import ru.redbyte.redbytefx.vec4
import ru.redbyte.redbytefx.x
import ru.redbyte.redbytefx.y
import ru.redbyte.redbytefx.z

internal class WireScene(
    val program: ShaderProgram,
    val mesh: GlMesh,
    val time: Uniform<Flt<High>>,
)

internal fun wireScene(): WireScene {
    lateinit var time: Uniform<Flt<High>>
    val program = shader(ShaderTarget.Gles32) {
        time = uniformTime()
        vertex {
            val position = attributeVec4("position")
            val angle = time.expr * 0.45f.lit
            val turn = cos(angle)
            val lift = sin(angle)
            val x = position.x * turn + position.z * lift
            val z = position.z * turn - position.x * lift + 1.55f.lit
            glPosition(vec4(x, position.y, z, 1f.lit))
        }
        geometry(GeometryInput.Triangles, GeometryOutput.TriangleStrip, 12) {
            emitEdge(glIn(0), glIn(1))
            emitEdge(glIn(1), glIn(2))
            emitEdge(glIn(2), glIn(0))
        }
        fragment {
            val pulse = 0.65f.lit + 0.35f.lit * sin(time.expr * 1.8f.lit)
            vec4(0.15f.lit, 0.82f.lit * pulse, 0.95f.lit, 1f.lit)
        }
    }
    return WireScene(program, solidPositions(0.68f), time)
}

@Composable
internal fun DemoWire() {
    val scene = remember { wireScene() }
    GlCanvas(scene.program, scene.mesh, requirement = "OpenGL ES 3.2 is required for the wireframe.") { frame ->
        frame.runtime.set(scene.time, frame.seconds)
    }
}

private fun GeometryDsl.emitEdge(start: HighVec4, end: HighVec4) {
    val dx = end.x - start.x
    val dy = end.y - start.y
    val dz = end.z - start.z
    val inv = 0.028f.lit / max(length(vec3(dx, dy, dz)), 0.0001f.lit)
    val px = -dy * inv
    val py = dx * inv
    glPosition(projectEye(vec4(start.x + px, start.y + py, start.z, 1f.lit)))
    emitVertex()
    glPosition(projectEye(vec4(start.x - px, start.y - py, start.z, 1f.lit)))
    emitVertex()
    glPosition(projectEye(vec4(end.x + px, end.y + py, end.z, 1f.lit)))
    emitVertex()
    glPosition(projectEye(vec4(end.x - px, end.y - py, end.z, 1f.lit)))
    emitVertex()
    endPrimitive()
}
