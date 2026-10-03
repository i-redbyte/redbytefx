package ru.redbyte.redbytefx.sample.ui.gl

import android.opengl.GLES30
import android.opengl.GLSurfaceView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Alignment
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import java.util.concurrent.atomic.AtomicInteger
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10
import kotlin.math.roundToInt
import ru.redbyte.redbytefx.sample.ui.CyberPanel
import ru.redbyte.redbytefx.sample.ui.say
import ru.redbyte.redbytefx.BoolS
import ru.redbyte.redbytefx.Expr
import ru.redbyte.redbytefx.Flt
import ru.redbyte.redbytefx.FragmentDsl
import ru.redbyte.redbytefx.High
import ru.redbyte.redbytefx.ShaderProgram
import ru.redbyte.redbytefx.ShaderTarget
import ru.redbyte.redbytefx.Uniform
import ru.redbyte.redbytefx.Vec2
import ru.redbyte.redbytefx.Vec3
import ru.redbyte.redbytefx.Vec4
import ru.redbyte.redbytefx.and
import ru.redbyte.redbytefx.div
import ru.redbyte.redbytefx.dot
import ru.redbyte.redbytefx.gl.Gles30Device
import ru.redbyte.redbytefx.gl.GlProgramRuntime
import ru.redbyte.redbytefx.gt
import ru.redbyte.redbytefx.ifElse
import ru.redbyte.redbytefx.length
import ru.redbyte.redbytefx.lit
import ru.redbyte.redbytefx.lt
import ru.redbyte.redbytefx.max
import ru.redbyte.redbytefx.minus
import ru.redbyte.redbytefx.mix
import ru.redbyte.redbytefx.plus
import ru.redbyte.redbytefx.pow
import ru.redbyte.redbytefx.shader
import ru.redbyte.redbytefx.times
import ru.redbyte.redbytefx.unaryMinus
import ru.redbyte.redbytefx.vec3
import ru.redbyte.redbytefx.vec4
import ru.redbyte.redbytefx.x
import ru.redbyte.redbytefx.y
import ru.redbyte.redbytefx.z

internal class BallProgram(
    val program: ShaderProgram,
    val aspect: Uniform<Flt<High>>,
    val count: Uniform<Flt<High>>,
    val xs: Array<Uniform<Flt<High>>>,
    val ys: Array<Uniform<Flt<High>>>,
    val zs: Array<Uniform<Flt<High>>>,
)

internal fun ballProgram(): BallProgram {
    lateinit var aspect: Uniform<Flt<High>>
    lateinit var count: Uniform<Flt<High>>
    lateinit var xs: Array<Uniform<Flt<High>>>
    lateinit var ys: Array<Uniform<Flt<High>>>
    lateinit var zs: Array<Uniform<Flt<High>>>
    val program = shader(ShaderTarget.Gles30) {
        aspect = uniform("aspect", 1f)
        count = uniform("count", BallWorld.INITIAL.toFloat())
        val uv = varyingVec2("uv")
        xs = Array(BallWorld.MAX) { index -> uniform("c${index}x", 0f) }
        ys = Array(BallWorld.MAX) { index -> uniform("c${index}y", 0f) }
        zs = Array(BallWorld.MAX) { index -> uniform("c${index}z", 1.2f) }
        vertex {
            val corner = attributeVec2("corner")
            uv.set(corner)
            glPosition(vec4(corner.x, corner.y, 0f.lit, 1f.lit))
        }
        fragment {
            traceSpheres(uv.expr, aspect, count, xs, ys, zs)
        }
    }
    return BallProgram(program, aspect, count, xs, ys, zs)
}

@Composable
fun DemoBalls() {
    val scene = remember { ballProgram() }
    val requested = remember { AtomicInteger(BallWorld.INITIAL) }
    var shown by remember { mutableIntStateOf(BallWorld.INITIAL) }
    Column(modifier = Modifier.fillMaxSize()) {
        Box(modifier = Modifier.weight(1f)) {
            GlesView { slot ->
                BallRenderer(scene, slot, requested)
            }
            GlCodeCompare(
                program = scene.program,
                dsl = ballsDsl,
                modifier = Modifier.align(Alignment.TopEnd),
            )
        }
        BallCountBar(
            count = shown,
            onChange = { next ->
                shown = next
                requested.set(next)
            },
        )
    }
}

@Composable
private fun BallCountBar(count: Int, onChange: (Int) -> Unit) {
    CyberPanel(
        modifier = Modifier
            .fillMaxWidth()
            .padding(start = 12.dp, end = 12.dp, bottom = 8.dp),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp),
    ) {
        Text(
            text = say("Balls $count", "Шаров: $count"),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurface,
        )
        Slider(
            value = count.toFloat(),
            onValueChange = { value ->
                onChange(value.roundToInt().coerceIn(BallWorld.MIN, BallWorld.MAX))
            },
            valueRange = BallWorld.MIN.toFloat()..BallWorld.MAX.toFloat(),
            steps = BallWorld.MAX - BallWorld.MIN - 1,
        )
    }
}

private class BallRenderer(
    private val scene: BallProgram,
    private val slot: GlSlot,
    private val requestedCount: AtomicInteger,
) : GLSurfaceView.Renderer {
    private val balls = BallWorld.pool()
    private val vertices = floatArrayOf(-1f, -1f, 3f, -1f, -1f, 3f)
    private var active = BallWorld.INITIAL
    private var buffer = 0
    private var attrib = -1
    private var aspect = 1f
    private var lastNanos = 0L

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        slot.runtime?.destroy()
        val runtime = GlProgramRuntime(scene.program, Gles30Device())
        runtime.link()
        slot.runtime = runtime
        runtime.use()
        attrib = attribLocation("a_corner")
        buffer = replaceVec2(buffer, vertices)
        val uploaded = buffer
        slot.releaseGl = {
            if (buffer == uploaded) {
                deleteBuffer(buffer)
                buffer = 0
            }
        }
        GLES30.glClearColor(0.02f, 0.02f, 0.04f, 1f)
        lastNanos = System.nanoTime()
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES30.glViewport(0, 0, width, height)
        aspect = if (height > 0) width.toFloat() / height.toFloat() else 1f
    }

    override fun onDrawFrame(gl: GL10?) {
        val runtime = slot.runtime ?: return
        val now = System.nanoTime()
        val elapsed = if (lastNanos == 0L) 0f else (now - lastNanos) / 1_000_000_000f
        lastNanos = now
        val requested = requestedCount.get().coerceIn(BallWorld.MIN, BallWorld.MAX)
        while (active < requested) {
            BallWorld.launch(balls[active], active, balls, active, aspect)
            active += 1
        }
        active = requested
        BallWorld.step(balls, active, aspect, elapsed)
        runtime.set(scene.aspect, aspect)
        runtime.set(scene.count, active.toFloat())
        for (index in 0 until active) {
            val ball = balls[index]
            runtime.set(scene.xs[index], ball.x)
            runtime.set(scene.ys[index], ball.y)
            runtime.set(scene.zs[index], ball.z)
        }
        runtime.use()
        GLES30.glClear(GLES30.GL_COLOR_BUFFER_BIT)
        drawVec2(buffer, attrib, 3)
    }
}

private fun FragmentDsl.traceSpheres(
    ndc: Expr<Vec2<Flt<High>>>,
    aspect: Uniform<Flt<High>>,
    count: Uniform<Flt<High>>,
    xs: Array<Uniform<Flt<High>>>,
    ys: Array<Uniform<Flt<High>>>,
    zs: Array<Uniform<Flt<High>>>,
): Expr<Vec4<Flt<High>>> {
    val pixelX = let(ndc.x * aspect.expr, "px")
    val pixelY = let(ndc.y, "py")
    val lightRaw = vec3((-0.45f).lit, 0.72f.lit, (-0.55f).lit)
    val light = let(lightRaw / max(length(lightRaw), 0.0001f.lit), "light")
    val sky = let(
        mix(
            vec3(0.02f.lit, 0.03f.lit, 0.07f.lit),
            vec3(0.10f.lit, 0.06f.lit, 0.18f.lit),
            ndc.y * 0.5f.lit + 0.5f.lit,
        ),
        "sky",
    )
    var color: Expr<Vec3<Flt<High>>> = sky
    var bestDepth = let(8f.lit, "far")
    for (index in xs.indices) {
        val disc = sphereDisc(
            pixelX,
            pixelY,
            light,
            xs[index],
            ys[index],
            zs[index],
            ballKinds[index],
            index,
            count,
            bestDepth,
        )
        color = let(ifElse(disc.hit, disc.color, color), "col$index")
        bestDepth = let(ifElse(disc.hit, disc.depth, bestDepth), "best$index")
    }
    return vec4(color.x, color.y, color.z, 1f.lit)
}

private class SphereDisc(
    val hit: Expr<BoolS>,
    val depth: Expr<Flt<High>>,
    val color: Expr<Vec3<Flt<High>>>,
)

private fun FragmentDsl.sphereDisc(
    pixelX: Expr<Flt<High>>,
    pixelY: Expr<Flt<High>>,
    light: Expr<Vec3<Flt<High>>>,
    x: Uniform<Flt<High>>,
    y: Uniform<Flt<High>>,
    z: Uniform<Flt<High>>,
    kind: BallKind,
    index: Int,
    count: Uniform<Flt<High>>,
    bestDepth: Expr<Flt<High>>,
): SphereDisc {
    val depth = let(z.expr, "z$index")
    val invDepth = let(1f.lit / max(depth, 0.2f.lit), "inv$index")
    val centerX = let(x.expr * invDepth, "sx$index")
    val centerY = let(y.expr * invDepth, "sy$index")
    val radius = let(kind.radius.lit * invDepth, "rad$index")
    val offsetX = let(pixelX - centerX, "ox$index")
    val offsetY = let(pixelY - centerY, "oy$index")
    val normalX = let(offsetX / max(radius, 0.0001f.lit), "nx$index")
    val normalY = let(offsetY / max(radius, 0.0001f.lit), "ny$index")
    val facing = let(pow(max(1f.lit - normalX * normalX - normalY * normalY, 0f.lit), 0.5f.lit), "nz$index")
    val normal = vec3(normalX, normalY, -facing)
    val diffuse = let(max(dot(normal, light), 0f.lit), "d$index")
    val spec = let(pow(diffuse, 18f), "s$index")
    val pigment = vec3(kind.red.lit, kind.green.lit, kind.blue.lit)
    val color = let(
        pigment * (0.18f.lit + diffuse * 0.82f.lit) + vec3(spec, spec, spec) * 0.4f.lit,
        "rgb$index",
    )
    val inside = (normalX * normalX + normalY * normalY) lt 1f.lit
    val live = count.expr gt (index + 0.5f)
    val nearer = depth lt bestDepth
    val hit = let(inside and live and nearer, "hit$index")
    return SphereDisc(hit, depth, color)
}
