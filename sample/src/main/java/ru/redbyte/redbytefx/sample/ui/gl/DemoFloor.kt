package ru.redbyte.redbytefx.sample.ui.gl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import ru.redbyte.redbytefx.Flt
import ru.redbyte.redbytefx.High
import ru.redbyte.redbytefx.ShaderTarget
import ru.redbyte.redbytefx.Uniform
import ru.redbyte.redbytefx.abs
import ru.redbyte.redbytefx.div
import ru.redbyte.redbytefx.fract
import ru.redbyte.redbytefx.ifElse
import ru.redbyte.redbytefx.lit
import ru.redbyte.redbytefx.lt
import ru.redbyte.redbytefx.min
import ru.redbyte.redbytefx.minus
import ru.redbyte.redbytefx.mix
import ru.redbyte.redbytefx.plus
import ru.redbyte.redbytefx.saturate
import ru.redbyte.redbytefx.shader
import ru.redbyte.redbytefx.sin
import ru.redbyte.redbytefx.smoothstep
import ru.redbyte.redbytefx.times
import ru.redbyte.redbytefx.vec2
import ru.redbyte.redbytefx.vec3
import ru.redbyte.redbytefx.vec4
import ru.redbyte.redbytefx.x
import ru.redbyte.redbytefx.y
import ru.redbyte.redbytefx.z

internal fun floorScene(): TimedScene {
    lateinit var time: Uniform<Flt<High>>
    lateinit var aspect: Uniform<Flt<High>>
    val program = shader(ShaderTarget.Gles30) {
        time = uniformTime()
        aspect = uniform("aspect", 1f)
        val world = varyingVec2("world")
        vertex {
            val grid = attributeVec3("grid")
            val start = fract((grid.y - time.expr * 0.7f.lit - FLOOR_NEAR.lit) / FLOOR_SPAN.lit) *
                FLOOR_SPAN.lit + FLOOR_NEAR.lit
            val distance = start + grid.z * FLOOR_STEP.lit
            world.set(vec2(grid.x * 2.6f.lit, distance))
            val clip = vec4(grid.x * 1.25f.lit / aspect.expr, (-0.5f).lit, 0f.lit, distance)
            glPosition(
                ifElse(
                    start lt (FLOOR_NEAR + FLOOR_SPAN - FLOOR_STEP).lit,
                    clip,
                    vec4(2f.lit, 2f.lit, 0f.lit, 1f.lit),
                ),
            )
        }
        fragment {
            val cellX = abs(fract(world.expr.x) - 0.5f.lit)
            val cellZ = abs(fract(world.expr.y * 1.4f.lit) - 0.5f.lit)
            val edge = min(cellX, cellZ)
            val neon = 1f.lit - smoothstep(0.0f.lit, 0.045f.lit, edge)
            val pulse = 0.65f.lit + 0.35f.lit * sin(time.expr * 2f.lit + world.expr.y)
            val fog = saturate((world.expr.y - 0.4f.lit) / 6.5f.lit)
            val glow = vec3(0.15f.lit, 0.9f.lit, 0.95f.lit) * neon * pulse
            val ground = vec3(0.02f.lit, 0.035f.lit, 0.07f.lit)
            val rgb = mix(ground + glow, vec3(0.01f.lit, 0.015f.lit, 0.03f.lit), fog)
            vec4(rgb.x, rgb.y, rgb.z, 1f.lit)
        }
    }
    return TimedScene(program, floorMeshHolder(), time, aspect)
}

@Composable
internal fun DemoFloor() {
    val scene = remember { floorScene() }
    GlCanvas(scene.program, scene.mesh, dsl = floorDsl) { frame ->
        frame.bind(scene)
    }
}
