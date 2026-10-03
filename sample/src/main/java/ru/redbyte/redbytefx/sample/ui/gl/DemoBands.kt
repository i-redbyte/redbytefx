package ru.redbyte.redbytefx.sample.ui.gl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import ru.redbyte.redbytefx.gl.compose.screenMesh
import ru.redbyte.redbytefx.Flt
import ru.redbyte.redbytefx.High
import ru.redbyte.redbytefx.ShaderTarget
import ru.redbyte.redbytefx.Uniform
import ru.redbyte.redbytefx.any
import ru.redbyte.redbytefx.cos
import ru.redbyte.redbytefx.gt
import ru.redbyte.redbytefx.ifElse
import ru.redbyte.redbytefx.lit
import ru.redbyte.redbytefx.lt
import ru.redbyte.redbytefx.minus
import ru.redbyte.redbytefx.plus
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

internal fun bandsScene(): TimedScene {
    lateinit var time: Uniform<Flt<High>>
    lateinit var aspect: Uniform<Flt<High>>
    val program = shader(ShaderTarget.Gles30) {
        time = uniformTime()
        aspect = uniform("aspect", 1f)
        val uv = varyingVec2("uv")
        vertex {
            val corner = attributeVec2("corner")
            uv.set(corner)
            glPosition(vec4(corner.x, corner.y, 0f.lit, 1f.lit))
        }
        fragment {
            val px = uv.expr.x * aspect.expr
            val py = uv.expr.y
            val field = sin(px * 3.2f.lit + time.expr) +
                cos(py * 2.4f.lit - time.expr * 0.7f.lit) +
                sin((px + py) * 1.8f.lit + time.expr * 0.4f.lit)
            val probe = vec2(field, sin(field * 2f.lit + time.expr))
            val hot = any(probe gt vec2(1.15f.lit, 0.35f.lit))
            val cool = any(probe lt vec2((-0.85f).lit, 0f.lit))
            val mid = field gt 0.15f.lit
            val deep = vec3(0.05f.lit, 0.06f.lit, 0.16f.lit)
            val tide = vec3(0.1f.lit, 0.28f.lit, 0.62f.lit)
            val sand = vec3(0.85f.lit, 0.45f.lit, 0.18f.lit)
            val rim = vec3(1f.lit, 0.92f.lit, 0.7f.lit)
            val band = ifElse(mid, sand, ifElse(cool, deep, tide))
            val rgb = ifElse(hot, rim, band)
            vec4(rgb.x, rgb.y, rgb.z, 1f.lit)
        }
    }
    return TimedScene(program, screenMesh(0.03f, 0.03f, 0.08f), time, aspect)
}

@Composable
internal fun DemoBands() {
    val scene = remember { bandsScene() }
    GlCanvas(scene.program, scene.mesh, dsl = bandsDsl) { frame -> frame.bind(scene) }
}
