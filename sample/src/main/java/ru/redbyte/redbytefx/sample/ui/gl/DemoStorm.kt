package ru.redbyte.redbytefx.sample.ui.gl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlin.random.Random
import ru.redbyte.redbytefx.Flt
import ru.redbyte.redbytefx.High
import ru.redbyte.redbytefx.ShaderProgram
import ru.redbyte.redbytefx.ShaderTarget
import ru.redbyte.redbytefx.Uniform
import ru.redbyte.redbytefx.abs
import ru.redbyte.redbytefx.floor
import ru.redbyte.redbytefx.fract
import ru.redbyte.redbytefx.length
import ru.redbyte.redbytefx.lit
import ru.redbyte.redbytefx.minus
import ru.redbyte.redbytefx.mix
import ru.redbyte.redbytefx.plus
import ru.redbyte.redbytefx.saturate
import ru.redbyte.redbytefx.shader
import ru.redbyte.redbytefx.sin
import ru.redbyte.redbytefx.smoothstep
import ru.redbyte.redbytefx.step
import ru.redbyte.redbytefx.times
import ru.redbyte.redbytefx.unaryMinus
import ru.redbyte.redbytefx.vec2
import ru.redbyte.redbytefx.vec3
import ru.redbyte.redbytefx.vec4
import ru.redbyte.redbytefx.x
import ru.redbyte.redbytefx.y
import ru.redbyte.redbytefx.z

internal class StormScene(
    val program: ShaderProgram,
    val mesh: GlMesh,
    val time: Uniform<Flt<High>>,
    val aspect: Uniform<Flt<High>>,
    val touchX: Uniform<Flt<High>>,
    val touchY: Uniform<Flt<High>>,
    val touchAge: Uniform<Flt<High>>,
    val burstX: Uniform<Flt<High>>,
    val burstAge: Uniform<Flt<High>>,
)

internal fun stormScene(): StormScene {
    lateinit var time: Uniform<Flt<High>>
    lateinit var aspect: Uniform<Flt<High>>
    lateinit var touchX: Uniform<Flt<High>>
    lateinit var touchY: Uniform<Flt<High>>
    lateinit var touchAge: Uniform<Flt<High>>
    lateinit var burstX: Uniform<Flt<High>>
    lateinit var burstAge: Uniform<Flt<High>>
    val program = shader(ShaderTarget.Gles30) {
        time = uniformTime()
        aspect = uniform("aspect", 1f)
        touchX = uniform("touchX", 0f)
        touchY = uniform("touchY", 2f)
        touchAge = uniform("touchAge", 4f)
        burstX = uniform("burstX", 0.2f)
        burstAge = uniform("burstAge", 4f)
        val uv = varyingVec2("uv")
        vertex {
            val corner = attributeVec2("corner")
            uv.set(corner)
            glPosition(vec4(corner.x, corner.y, 0f.lit, 1f.lit))
        }
        fragment {
            val bolt = fn(0f.lit, 0f.lit, 0f.lit, 0f.lit, "bolt") { x, y, origin, age ->
                val jag = sin(y * 14f.lit) * 0.045f.lit +
                    sin(y * 31f.lit + 1.3f.lit) * 0.022f.lit +
                    sin(y * 57f.lit) * 0.01f.lit
                val spine = origin + jag
                val dx = abs(x - spine)
                val core = 1f.lit - smoothstep(0f.lit, 0.007f.lit, dx)
                val glow = 1f.lit - smoothstep(0f.lit, 0.05f.lit, dx)
                val fork = spine + 0.1f.lit + (0.35f.lit - y) * 0.85f.lit
                val branch = (1f.lit - smoothstep(0f.lit, 0.006f.lit, abs(x - fork))) *
                    smoothstep(0.05f.lit, 0.4f.lit, y) *
                    (1f.lit - smoothstep(0.4f.lit, 0.72f.lit, y))
                val column = smoothstep((-1.02f).lit, (-0.82f).lit, y) *
                    (1f.lit - smoothstep(0.9f.lit, 1.02f.lit, y))
                (core * 1.7f.lit + glow * 0.5f.lit + branch) * column * saturate(1f.lit - age * 0.5f.lit)
            }
            val px = let(uv.expr.x * aspect.expr, "px")
            val py = let(uv.expr.y, "py")
            val cellX = floor(px * 26f.lit)
            val cellY = floor(py * 34f.lit)
            val local = vec2(fract(px * 26f.lit), fract(py * 34f.lit)) - 0.5f.lit
            val hash = fract(sin(cellX * 127.1f.lit + cellY * 311.7f.lit) * 43758.5f.lit)
            val spark = (1f.lit - smoothstep(0.012f.lit, 0.06f.lit, length(local))) *
                step(0.972f.lit, hash) *
                (0.45f.lit + 0.55f.lit * sin(time.expr * 2.4f.lit + hash * 30f.lit))
            val sky = mix(
                vec3(0.004f.lit, 0.006f.lit, 0.02f.lit),
                vec3(0.015f.lit, 0.025f.lit, 0.07f.lit),
                saturate(py * 0.35f.lit + 0.55f.lit),
            )
            val finger = bolt(px, py, touchX.expr, touchAge.expr) *
                smoothstep(touchY.expr - 0.03f.lit, touchY.expr + 0.1f.lit, py)
            val stray = bolt(px, py, burstX.expr, burstAge.expr)
            val energy = finger + stray
            val rgb = sky +
                vec3(0.82f.lit, 0.88f.lit, 1f.lit) * spark +
                vec3(0.45f.lit, 0.7f.lit, 1f.lit) * saturate(energy) +
                vec3(0.95f.lit, 0.97f.lit, 1f.lit) * saturate(energy - 0.85f.lit)
            vec4(rgb.x, rgb.y, rgb.z, 1f.lit)
        }
    }
    return StormScene(
        program,
        screenMesh(0.004f, 0.006f, 0.018f),
        time,
        aspect,
        touchX,
        touchY,
        touchAge,
        burstX,
        burstAge,
    )
}

@Composable
internal fun DemoStorm() {
    val scene = remember { stormScene() }
    val pointer = remember { PointerState() }
    val clock = remember { StormClock() }
    GlCanvas(
        program = scene.program,
        mesh = scene.mesh,
        pointer = pointer,
        caption = "Touch the sky",
    ) { frame ->
        clock.publish(scene, pointer, frame)
    }
}

private class StormClock {
    private val random = Random(11)
    private var seen = 0
    private var touchAt = -4f
    private var burstX = 0.2f
    private var burstAt = -4f
    private var nextBurst = 0.6f
    private var heldX = 0f
    private var heldY = -0.2f

    fun publish(scene: StormScene, pointer: PointerState, frame: GlFrame) {
        val serial = pointer.serial()
        if (serial != seen) {
            seen = serial
            heldX = pointer.x
            heldY = pointer.y
            touchAt = frame.seconds
        }
        if (frame.seconds >= nextBurst) {
            burstX = random.nextFloat() * 1.1f - 0.55f
            burstAt = frame.seconds
            nextBurst = frame.seconds + 1.1f + random.nextFloat() * 2.2f
        }
        val runtime = frame.runtime
        runtime.set(scene.time, frame.seconds)
        runtime.set(scene.aspect, frame.aspect)
        runtime.set(scene.touchX, heldX * frame.aspect)
        runtime.set(scene.touchY, heldY)
        runtime.set(scene.touchAge, frame.seconds - touchAt)
        runtime.set(scene.burstX, burstX)
        runtime.set(scene.burstAge, frame.seconds - burstAt)
    }
}
