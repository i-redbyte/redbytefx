package ru.redbyte.redbytefx.sample.ui.gl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import ru.redbyte.redbytefx.Expr
import ru.redbyte.redbytefx.Flt
import ru.redbyte.redbytefx.Fn3
import ru.redbyte.redbytefx.FragmentDsl
import ru.redbyte.redbytefx.High
import ru.redbyte.redbytefx.ShaderTarget
import ru.redbyte.redbytefx.Uniform
import ru.redbyte.redbytefx.div
import ru.redbyte.redbytefx.lit
import ru.redbyte.redbytefx.minus
import ru.redbyte.redbytefx.mix
import ru.redbyte.redbytefx.plus
import ru.redbyte.redbytefx.saturate
import ru.redbyte.redbytefx.shader
import ru.redbyte.redbytefx.sin
import ru.redbyte.redbytefx.smoothstep
import ru.redbyte.redbytefx.times
import ru.redbyte.redbytefx.vec3
import ru.redbyte.redbytefx.vec4
import ru.redbyte.redbytefx.x
import ru.redbyte.redbytefx.y
import ru.redbyte.redbytefx.z

internal fun flagScene(): TimedScene {
    lateinit var time: Uniform<Flt<High>>
    lateinit var aspect: Uniform<Flt<High>>
    lateinit var fold: Fn3<Flt<High>, Flt<High>, Flt<High>, Flt<High>>
    val program = shader(ShaderTarget.Gles30) {
        time = uniformTime()
        aspect = uniform("aspect", 1f)
        val cloth = varyingVec2("cloth")
        vertex {
            fold = fn(0f.lit, 0f.lit, 0f.lit, "fold") { u, v, t ->
                sin(u * 6.2f.lit + t * 1.7f.lit) * (0.08f.lit + v * 0.22f.lit) +
                    sin(v * 4.4f.lit - t * 1.15f.lit) * 0.045f.lit
            }
            val uv = attributeVec2("uv")
            val height = fold(uv.x, uv.y, time.expr)
            val x = uv.x * 1.65f.lit - 0.82f.lit
            val y = uv.y * 0.95f.lit - 0.42f.lit
            cloth.set(uv)
            glPosition(vec4(x / aspect.expr, y, height, height + 2.15f.lit))
        }
        fragment {
            val shade = fold(cloth.expr.x, cloth.expr.y, time.expr)
            val mark = cppMark(cloth.expr.x, cloth.expr.y)
            val red = vec3(0.82f.lit, 0.05f.lit, 0.08f.lit)
            val chalk = vec3(0.96f.lit, 0.94f.lit, 0.9f.lit)
            val light = 0.7f.lit + shade * 1.8f.lit
            val rgb = mix(red, chalk, mark) * light
            vec4(rgb.x, rgb.y, rgb.z, 1f.lit)
        }
    }
    return TimedScene(program, sheetMesh(22, 14, depth = true), time, aspect)
}

@Composable
internal fun DemoFlag() {
    val scene = remember { flagScene() }
    GlCanvas(scene.program, scene.mesh) { frame -> frame.bind(scene) }
}

private fun FragmentDsl.cppMark(u: Expr<Flt<High>>, v: Expr<Flt<High>>): Expr<Flt<High>> {
    val letter = slab(u, v, 0.04f, 0.78f, 0.058f, 0.96f) +
        slab(u, v, 0.04f, 0.935f, 0.115f, 0.96f) +
        slab(u, v, 0.04f, 0.78f, 0.115f, 0.805f)
    val first = slab(u, v, 0.15f, 0.8f, 0.168f, 0.94f) + slab(u, v, 0.128f, 0.855f, 0.19f, 0.885f)
    val second = slab(u, v, 0.22f, 0.8f, 0.238f, 0.94f) + slab(u, v, 0.198f, 0.855f, 0.26f, 0.885f)
    return saturate(letter + first + second)
}

private fun FragmentDsl.slab(
    u: Expr<Flt<High>>,
    v: Expr<Flt<High>>,
    x0: Float,
    y0: Float,
    x1: Float,
    y1: Float,
): Expr<Flt<High>> {
    val across = smoothstep(x0.lit, (x0 + 0.004f).lit, u) * (1f.lit - smoothstep(x1.lit, (x1 + 0.004f).lit, u))
    val upright = smoothstep(y0.lit, (y0 + 0.004f).lit, v) * (1f.lit - smoothstep(y1.lit, (y1 + 0.004f).lit, v))
    return across * upright
}
