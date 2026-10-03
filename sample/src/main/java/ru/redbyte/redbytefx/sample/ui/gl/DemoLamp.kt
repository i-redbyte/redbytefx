package ru.redbyte.redbytefx.sample.ui.gl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import ru.redbyte.redbytefx.Flt
import ru.redbyte.redbytefx.High
import ru.redbyte.redbytefx.ShaderTarget
import ru.redbyte.redbytefx.Uniform
import ru.redbyte.redbytefx.cos
import ru.redbyte.redbytefx.div
import ru.redbyte.redbytefx.length
import ru.redbyte.redbytefx.lit
import ru.redbyte.redbytefx.max
import ru.redbyte.redbytefx.minus
import ru.redbyte.redbytefx.plus
import ru.redbyte.redbytefx.pow
import ru.redbyte.redbytefx.shader
import ru.redbyte.redbytefx.sin
import ru.redbyte.redbytefx.times
import ru.redbyte.redbytefx.unaryMinus
import ru.redbyte.redbytefx.vec2
import ru.redbyte.redbytefx.vec3
import ru.redbyte.redbytefx.vec4
import ru.redbyte.redbytefx.x
import ru.redbyte.redbytefx.y
import ru.redbyte.redbytefx.z

internal fun lampScene(): TimedScene {
    lateinit var time: Uniform<Flt<High>>
    lateinit var aspect: Uniform<Flt<High>>
    val program = shader(ShaderTarget.Gles30) {
        time = uniformTime()
        aspect = uniform("aspect", 1f)
        val normalXy = varyingVec2("normalXy")
        val normalZ = varyingVec2("normalZ")
        vertex {
            val position = attributeVec4("position")
            val normal = attributeVec4("normal")
            val angle = time.expr * 0.65f.lit
            val turn = cos(angle)
            val lift = sin(angle)
            val x = position.x * turn + position.z * lift
            val z = position.z * turn - position.x * lift + 1.7f.lit
            val nx = normal.x * turn + normal.z * lift
            val nz = normal.z * turn - normal.x * lift
            normalXy.set(vec2(nx, normal.y))
            normalZ.set(vec2(nz, 1f.lit))
            glPosition(vec4(x / aspect.expr, position.y, z * 0.25f.lit - 0.35f.lit, z))
        }
        fragment {
            val nx = normalXy.expr.x
            val ny = normalXy.expr.y
            val nz = normalZ.expr.x
            val normal = vec3(nx, ny, nz)
            val inv = 1f.lit / max(length(normal), 0.0001f.lit)
            val light = vec3((-0.35f).lit, 0.8f.lit, 0.45f.lit)
            val diffuse = max((nx * light.x + ny * light.y + nz * light.z) * inv, 0f.lit)
            val spec = pow(diffuse, 28f)
            val warm = vec3(1f.lit, 0.62f.lit, 0.22f.lit) * (0.16f.lit + diffuse * 0.9f.lit)
            val rgb = warm + vec3(spec, spec * 0.85f.lit, spec * 0.45f.lit)
            vec4(rgb.x, rgb.y, rgb.z, 1f.lit)
        }
    }
    return TimedScene(program, solidShaded(0.72f), time, aspect)
}

@Composable
internal fun DemoLamp() {
    val scene = remember { lampScene() }
    GlCanvas(scene.program, scene.mesh) { frame -> frame.bind(scene) }
}
