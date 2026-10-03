package ru.redbyte.redbytefx.sample.ui.gl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import ru.redbyte.redbytefx.Flt
import ru.redbyte.redbytefx.High
import ru.redbyte.redbytefx.ShaderTarget
import ru.redbyte.redbytefx.Uniform
import ru.redbyte.redbytefx.div
import ru.redbyte.redbytefx.gt
import ru.redbyte.redbytefx.ifElse
import ru.redbyte.redbytefx.length
import ru.redbyte.redbytefx.lit
import ru.redbyte.redbytefx.max
import ru.redbyte.redbytefx.minus
import ru.redbyte.redbytefx.mix
import ru.redbyte.redbytefx.plus
import ru.redbyte.redbytefx.pow
import ru.redbyte.redbytefx.shader
import ru.redbyte.redbytefx.sin
import ru.redbyte.redbytefx.times
import ru.redbyte.redbytefx.unaryMinus
import ru.redbyte.redbytefx.vec3
import ru.redbyte.redbytefx.vec4
import ru.redbyte.redbytefx.x
import ru.redbyte.redbytefx.y
import ru.redbyte.redbytefx.z

internal fun orbScene(): TimedScene {
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
            val px = let(uv.expr.x * aspect.expr, "px")
            val py = let(uv.expr.y, "py")
            val aim = vec3(px, py, 1f.lit)
            val inv = 1f.lit / max(length(aim), 0.0001f.lit)
            val dx = aim.x * inv
            val dy = aim.y * inv
            val dz = aim.z * inv
            val originX = sin(time.expr * 0.6f.lit) * 0.18f.lit
            val originY = 0.08f.lit
            val originZ = (-2.15f).lit
            val slope = originX * dx + originY * dy + originZ * dz
            val radius = 0.72f.lit
            val curve = originX * originX + originY * originY + originZ * originZ - radius * radius
            val disc = slope * slope - curve
            val travel = -slope - pow(max(disc, 0f.lit), 0.5f.lit)
            val hitX = originX + dx * travel
            val hitY = originY + dy * travel
            val hitZ = originZ + dz * travel
            val nx = hitX / radius
            val ny = hitY / radius
            val nz = hitZ / radius
            val facing = max(-(nx * dx + ny * dy + nz * dz), 0f.lit)
            val fresnel = pow(1f.lit - facing, 3f)
            val sky = mix(
                vec3(0.02f.lit, 0.03f.lit, 0.08f.lit),
                vec3(0.18f.lit, 0.28f.lit, 0.48f.lit),
                py * 0.5f.lit + 0.5f.lit,
            )
            val glass = vec3(0.35f.lit, 0.72f.lit, 0.95f.lit) * (0.25f.lit + facing) +
                vec3(1f.lit, 0.95f.lit, 0.9f.lit) * fresnel +
                vec3(ny * 0.15f.lit + 0.1f.lit, ny * 0.05f.lit, 0.05f.lit)
            val rgb = ifElse(disc gt 0f.lit, glass, sky)
            vec4(rgb.x, rgb.y, rgb.z, 1f.lit)
        }
    }
    return TimedScene(program, screenMesh(0.02f, 0.03f, 0.07f), time, aspect)
}

@Composable
internal fun DemoOrb() {
    val scene = remember { orbScene() }
    GlCanvas(scene.program, scene.mesh, dsl = orbDsl) { frame -> frame.bind(scene) }
}
