package ru.redbyte.redbytefx.sample.ui.gl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import ru.redbyte.redbytefx.gl.compose.torus
import ru.redbyte.redbytefx.Flt
import ru.redbyte.redbytefx.Fn4
import ru.redbyte.redbytefx.High
import ru.redbyte.redbytefx.ShaderTarget
import ru.redbyte.redbytefx.Uniform
import ru.redbyte.redbytefx.Vec3
import ru.redbyte.redbytefx.abs
import ru.redbyte.redbytefx.cos
import ru.redbyte.redbytefx.div
import ru.redbyte.redbytefx.fract
import ru.redbyte.redbytefx.lit
import ru.redbyte.redbytefx.minus
import ru.redbyte.redbytefx.plus
import ru.redbyte.redbytefx.saturate
import ru.redbyte.redbytefx.shader
import ru.redbyte.redbytefx.sin
import ru.redbyte.redbytefx.times
import ru.redbyte.redbytefx.vec2
import ru.redbyte.redbytefx.vec3
import ru.redbyte.redbytefx.vec4
import ru.redbyte.redbytefx.x
import ru.redbyte.redbytefx.y
import ru.redbyte.redbytefx.z

internal fun paletteScene(): TimedScene {
    lateinit var time: Uniform<Flt<High>>
    lateinit var aspect: Uniform<Flt<High>>
    lateinit var ink: Fn4<Flt<High>, Flt<High>, Flt<High>, Flt<High>, Vec3<Flt<High>>>
    val program = shader(ShaderTarget.Gles30) {
        time = uniformTime()
        aspect = uniform("aspect", 1f)
        val paint = varyingVec2("paint")
        vertex {
            ink = fn(0f.lit, 0f.lit, 0f.lit, 0f.lit, "ink") { down, shade, clock, shift ->
                val hue = fract(down - clock * 0.12f.lit + shift)
                val red = saturate(abs(hue * 6f.lit - 3f.lit) - 1f.lit)
                val green = saturate(2f.lit - abs(hue * 6f.lit - 2f.lit))
                val blue = saturate(2f.lit - abs(hue * 6f.lit - 4f.lit))
                vec3(red, green, blue) * (0.28f.lit + shade * 0.72f.lit)
            }
            val position = attributeVec3("position")
            val normal = attributeVec3("normal")
            val uv = attributeVec2("uv")
            val shade = saturate(normal.y * 0.5f.lit + 0.5f.lit)
            val down = uv.x
            val angle = time.expr * 0.2f.lit
            val turn = cos(angle)
            val lift = sin(angle)
            val x = position.x * turn + position.y * lift
            val y = position.z
            val z = position.y * turn - position.x * lift + 2.15f.lit
            val color = ink(down, shade, time.expr, 0f.lit)
            paint.set(vec2(shade, down))
            glPosition(vec4((x + color.x * 0.02f.lit) / aspect.expr, y, z * 0.22f.lit - 0.3f.lit, z))
        }
        fragment {
            val color = ink(paint.expr.y, paint.expr.x, time.expr, 0f.lit)
            vec4(color.x, color.y, color.z, 1f.lit)
        }
    }
    return TimedScene(program, torus(0.85f, 0.22f), time, aspect)
}

@Composable
internal fun DemoPalette() {
    val scene = remember { paletteScene() }
    GlCanvas(scene.program, scene.mesh, dsl = paletteDsl) { frame -> frame.bind(scene) }
}
