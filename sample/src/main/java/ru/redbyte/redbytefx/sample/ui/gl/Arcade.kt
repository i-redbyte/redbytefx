package ru.redbyte.redbytefx.sample.ui.gl

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.sin
import kotlinx.coroutines.isActive
import ru.redbyte.redbytefx.sample.ui.russian

internal fun gameDsl(note: String): String = "// $note\n$PLAYFIELD_DSL"

@Composable
private fun ArcadeScreen(
    scene: Playfield,
    dsl: String,
    caption: String,
    flash: () -> Float,
    mesh: () -> FloatArray,
    step: (Float, PointerState) -> Unit,
) {
    val pointer = remember { PointerState() }
    val play = rememberUpdatedState(step)
    LaunchedEffect(scene) {
        var last = 0L
        while (isActive) {
            withFrameNanos { nanos ->
                val dt = if (last == 0L) 0f else ((nanos - last) * 1.0e-9f).coerceIn(0f, 0.05f)
                last = nanos
                play.value(dt, pointer)
            }
        }
    }
    GlCanvas(scene.program, scene.mesh, pointer = pointer, caption = caption, dsl = dsl) { frame ->
        frame.runtime.set(scene.aspect, frame.aspect)
        frame.runtime.set(scene.flash, flash())
        val vertices = mesh()
        if (vertices.isNotEmpty()) frame.replace(vertices)
    }
}

internal class TunnelRun {
    var ship = 0f
    var distance = 0f
    var score = 0
    var flash = 0f
    private var gate = 4f

    @Volatile var mesh: FloatArray = FloatArray(0)

    init {
        rebuild()
    }

    fun step(dt: Float, target: Float) {
        val aim = target.coerceIn(-0.7f, 0.7f)
        ship += (aim - ship) * (dt * 6f).coerceAtMost(1f)
        distance += dt * SPEED
        flash = (flash - dt * 2.4f).coerceAtLeast(0f)
        val nose = distance + NOSE
        while (gate <= nose) {
            if (abs(ship) <= HOLE) score += 1 else flash = 1f
            gate += GAP
        }
        rebuild()
    }

    fun caption(russian: Boolean): String = if (russian) "Кольца: $score" else "Rings: $score"

    private fun rebuild() {
        val pen = WorldPen(0f, 0f, distance)
        var z = gate - GAP * 2f
        repeat(6) {
            ring(pen, z)
            z += GAP
        }
        pen.box(ship, -0.05f, distance + NOSE, 0.07f, 0.025f, 0.12f, 1f, 0.62f, 0.18f)
        pen.box(ship, -0.02f, distance + NOSE - 0.08f, 0.11f, 0.02f, 0.05f, 0.95f, 0.78f, 0.3f)
        mesh = pen.toArray()
    }

    private fun ring(pen: WorldPen, z: Float) {
        val span = 0.58f
        val bar = 0.16f
        pen.box(-span, 0f, z, bar, span + bar, 0.04f, 0.15f, 0.85f, 1f)
        pen.box(span, 0f, z, bar, span + bar, 0.04f, 0.15f, 0.85f, 1f)
        pen.box(0f, span, z, span, bar, 0.04f, 0.15f, 0.85f, 1f)
        pen.box(0f, -span, z, span, bar, 0.04f, 0.15f, 0.85f, 1f)
    }

    private companion object {
        const val SPEED = 2.6f
        const val NOSE = 1.15f
        const val HOLE = 0.36f
        const val GAP = 3.1f
    }
}

internal class MazeRun {
    var x = 1.5f
    var z = 1.5f
    var vx = 0f
    var vz = 0f
    var won = false
    var flash = 0f

    @Volatile var mesh: FloatArray = FloatArray(0)

    init {
        rebuild()
    }

    fun step(dt: Float, tiltX: Float, tiltY: Float) {
        flash = (flash - dt * 2f).coerceAtLeast(0f)
        if (!won) {
            vx = (vx + tiltX.coerceIn(-1f, 1f) * 7f * dt) * (1f - dt * 1.6f).coerceAtLeast(0f)
            vz = (vz + tiltY.coerceIn(-1f, 1f) * 7f * dt) * (1f - dt * 1.6f).coerceAtLeast(0f)
            vx = vx.coerceIn(-3f, 3f)
            vz = vz.coerceIn(-3f, 3f)
            val nextX = x + vx * dt
            if (fits(nextX, z)) x = nextX else vx = 0f
            val nextZ = z + vz * dt
            if (fits(x, nextZ)) z = nextZ else vz = 0f
            if (cell(x, z) == 'E') {
                won = true
                flash = 1f
            }
        }
        rebuild()
    }

    fun fits(px: Float, pz: Float): Boolean =
        open(px - PAD, pz - PAD) &&
            open(px + PAD, pz - PAD) &&
            open(px - PAD, pz + PAD) &&
            open(px + PAD, pz + PAD)

    fun caption(russian: Boolean): String = when {
        won && russian -> "Выход найден"
        won -> "Exit reached"
        russian -> "Наклоните поле"
        else -> "Tilt the board"
    }

    private fun rebuild() {
        val pen = WorldPen(x, 0.28f, z - 2.1f)
        MAZE.forEachIndexed { row, line ->
            line.forEachIndexed { col, mark ->
                val cx = col + 0.5f
                val cz = row + 0.5f
                if (mark == '#') {
                    pen.box(cx, 0.42f, cz, 0.5f, 0.42f, 0.5f, 0.72f, 0.48f, 0.28f)
                } else {
                    val tone = if ((col + row) % 2 == 0) 0.16f else 0.28f
                    val green = if (mark == 'E') 0.85f else tone
                    val red = if (mark == 'E') 0.2f else tone
                    pen.box(cx, -0.02f, cz, 0.5f, 0.02f, 0.5f, red, green, tone)
                }
            }
        }
        pen.sphere(x, 0.2f, z, 0.16f, 0.95f, 0.78f, 0.25f)
        mesh = pen.toArray()
    }

    private fun open(px: Float, pz: Float): Boolean = cell(px, pz) != '#'

    private fun cell(px: Float, pz: Float): Char {
        val col = floor(px).toInt()
        val row = floor(pz).toInt()
        val line = MAZE.getOrNull(row) ?: return '#'
        return line.getOrNull(col) ?: '#'
    }

    private companion object {
        const val PAD = 0.2f
        val MAZE = listOf(
            "#########",
            "#S      #",
            "# ##### #",
            "# #   # #",
            "# # # # #",
            "#   # # #",
            "### # # #",
            "#     #E#",
            "#########",
        )
    }
}

internal class BreakoutRun {
    var ballX = 0f
    var ballY = 0.5f
    var vx = 0.55f
    var vy = 1.15f
    var paddle = 0f
    var score = 0
    var lives = 3
    var flash = 0f
    val alive = BooleanArray(COLS * ROWS) { true }
    private var pause = 0f

    @Volatile var mesh: FloatArray = FloatArray(0)

    init {
        rebuild()
    }

    fun step(dt: Float, target: Float) {
        flash = (flash - dt * 3f).coerceAtLeast(0f)
        val aim = target.coerceIn(-0.9f, 0.9f)
        paddle += (aim - paddle) * (dt * 9f).coerceAtMost(1f)
        if (lives <= 0 || score >= COLS * ROWS) {
            rebuild()
            return
        }
        if (pause > 0f) {
            pause -= dt
            rebuild()
            return
        }
        ballX = (ballX + vx * dt).coerceIn(-1.05f, 1.05f)
        if (ballX <= -1.05f || ballX >= 1.05f) vx = -vx
        ballY += vy * dt
        if (ballY > 2.15f) {
            ballY = 2.15f
            vy = -abs(vy)
        }
        if (vy < 0f && ballY <= 0.28f && ballY >= 0.1f && abs(ballX - paddle) <= 0.38f) {
            ballY = 0.28f
            vy = abs(vy)
            vx = (vx + (ballX - paddle) * 1.6f).coerceIn(-1.8f, 1.8f)
        }
        hitBrick()
        if (ballY < 0.02f) {
            lives -= 1
            flash = 1f
            ballX = paddle
            ballY = 0.5f
            vy = abs(vy)
            pause = 0.45f
        }
        rebuild()
    }

    fun caption(russian: Boolean): String = when {
        lives <= 0 && russian -> "Партия окончена · $score"
        lives <= 0 -> "Run over · $score"
        score >= COLS * ROWS && russian -> "Поле чистое"
        score >= COLS * ROWS -> "Board clear"
        russian -> "Сбито $score из ${COLS * ROWS} · жизни $lives"
        else -> "Cleared $score of ${COLS * ROWS} · lives $lives"
    }

    private fun hitBrick() {
        for (row in 0 until ROWS) {
            for (col in 0 until COLS) {
                val index = row * COLS + col
                if (!alive[index]) continue
                val cx = -0.91f + col * 0.26f
                val cy = 1.2f + row * 0.22f
                if (abs(ballX - cx) < 0.13f && abs(ballY - cy) < 0.1f) {
                    alive[index] = false
                    score += 1
                    vy = -vy
                    flash = 0.7f
                    return
                }
            }
        }
    }

    private fun rebuild() {
        val pen = WorldPen(0f, 1f, -2.35f)
        val colors = arrayOf(
            floatArrayOf(0.9f, 0.25f, 0.22f),
            floatArrayOf(0.95f, 0.55f, 0.15f),
            floatArrayOf(0.95f, 0.85f, 0.2f),
            floatArrayOf(0.25f, 0.75f, 0.4f),
        )
        for (row in 0 until ROWS) {
            for (col in 0 until COLS) {
                if (!alive[row * COLS + col]) continue
                val ink = colors[row]
                pen.box(-0.91f + col * 0.26f, 1.2f + row * 0.22f, 0f, 0.11f, 0.07f, 0.08f, ink[0], ink[1], ink[2])
            }
        }
        pen.box(paddle, 0.14f, 0f, 0.32f, 0.035f, 0.06f, 0.2f, 0.85f, 0.95f)
        pen.sphere(ballX, ballY, 0f, 0.055f, 0.95f, 0.95f, 0.98f)
        mesh = pen.toArray()
    }

    companion object {
        const val COLS = 8
        const val ROWS = 4
    }
}

internal class StrafeRun {
    class Foe(val x: Float, val y: Float, val z: Float)

    val foes = ArrayList<Foe>()
    var distance = 0f
    var score = 0
    var flash = 0f
    var beam = 0f
    var beamX = 0f
    private var spawn = 0.4f
    private var seen = 0

    @Volatile var mesh: FloatArray = FloatArray(0)

    init {
        rebuild()
    }

    fun fire(x: Float) {
        beamX = x.coerceIn(-0.7f, 0.7f)
        beam = 0.18f
    }

    fun step(dt: Float, aim: Float, serial: Int) {
        if (serial != seen) {
            seen = serial
            fire(aim)
        }
        distance += dt * 3.4f
        flash = (flash - dt * 4f).coerceAtLeast(0f)
        beam = (beam - dt).coerceAtLeast(0f)
        spawn -= dt
        if (spawn <= 0f) {
            spawn = 0.9f
            val lane = ((foes.size % 5) - 2) * 0.28f
            val height = ((foes.size % 3) - 1) * 0.22f
            foes += Foe(lane, height, distance + 9f)
        }
        val kept = ArrayList<Foe>(foes.size)
        foes.forEach { foe ->
            val ahead = foe.z - distance
            val hit = beam > 0f && abs(foe.x - beamX) < 0.2f && ahead in 0.6f..7.2f
            when {
                hit -> {
                    score += 1
                    flash = 1f
                }
                ahead > 0.45f -> kept += foe
            }
        }
        foes.clear()
        foes += kept
        rebuild()
    }

    fun caption(russian: Boolean): String = if (russian) "Сбито: $score" else "Hits: $score"

    private fun rebuild() {
        val pen = WorldPen(0f, 0.05f, distance)
        foes.forEach { foe ->
            pen.sphere(foe.x, foe.y, foe.z, 0.16f, 0.9f, 0.22f, 0.18f)
        }
        if (beam > 0f) {
            pen.box(beamX, 0f, distance + 3.6f, 0.02f, 0.02f, 3.2f, 1f, 0.9f, 0.35f)
        }
        pen.box(beamX, 0f, distance + 2.2f, 0.03f, 0.03f, 0.03f, 0.4f, 0.9f, 1f)
        mesh = pen.toArray()
    }
}

internal class DescentRun {
    var skier = 0f
    var distance = 0f
    var score = 0
    var flash = 0f
    private var gateZ = 5f
    private var gateIndex = 0

    @Volatile var mesh: FloatArray = FloatArray(0)

    init {
        rebuild()
    }

    fun gateCenter(): Float = sin(gateIndex * 1.7f) * 0.55f

    fun step(dt: Float, target: Float) {
        val aim = target.coerceIn(-0.85f, 0.85f)
        skier += (aim - skier) * (dt * 5f).coerceAtMost(1f)
        distance += dt * 3.1f
        flash = (flash - dt * 2.2f).coerceAtLeast(0f)
        val nose = distance + 1.3f
        while (gateZ <= nose) {
            if (abs(skier - gateCenter()) <= GAP) score += 1 else flash = 1f
            gateIndex += 1
            gateZ += 3.4f
        }
        rebuild()
    }

    fun caption(russian: Boolean): String = if (russian) "Ворота: $score" else "Gates: $score"

    private fun rebuild() {
        val pen = WorldPen(skier * 0.35f, 0.42f, distance)
        var row = 0
        var z = distance + 0.35f
        while (z < distance + 12f) {
            val snow = if (row % 2 == 0) 0.72f else 0.58f
            pen.box(0f, -0.16f, z, 1.35f, 0.02f, 0.38f, snow, snow + 0.05f, 0.82f)
            row += 1
            z += 0.76f
        }
        var mark = gateZ
        var index = gateIndex
        repeat(4) {
            val center = sin(index * 1.7f) * 0.55f
            pen.box(center - 0.48f, 0.28f, mark, 0.04f, 0.32f, 0.04f, 0.9f, 0.18f, 0.16f)
            pen.box(center + 0.48f, 0.28f, mark, 0.04f, 0.32f, 0.04f, 0.9f, 0.18f, 0.16f)
            pen.box(center - 0.48f, 0.6f, mark, 0.05f, 0.012f, 0.012f, 0.95f, 0.3f, 0.2f)
            pen.box(center + 0.48f, 0.6f, mark, 0.05f, 0.012f, 0.012f, 0.95f, 0.3f, 0.2f)
            mark += 3.4f
            index += 1
        }
        pen.box(skier, 0.06f, distance + 1.3f, 0.07f, 0.04f, 0.12f, 0.95f, 0.95f, 0.98f)
        mesh = pen.toArray()
    }

    private companion object {
        const val GAP = 0.34f
    }
}

@Composable
internal fun DemoTunnel() {
    val scene = remember { playfield(0.012f, 0.02f, 0.05f) }
    val run = remember { TunnelRun() }
    val ru = russian()
    var caption by remember(ru) { mutableStateOf(run.caption(ru)) }
    ArcadeScreen(
        scene = scene,
        dsl = gameDsl("The ship follows the finger. A ring counts only when the ship is inside the hole."),
        caption = caption,
        flash = { run.flash },
        mesh = { run.mesh },
    ) { dt, pointer ->
        run.step(dt, pointer.x)
        caption = run.caption(ru)
    }
}

@Composable
internal fun DemoMaze() {
    val scene = remember { playfield(0.03f, 0.025f, 0.02f) }
    val run = remember { MazeRun() }
    val ru = russian()
    var caption by remember(ru) { mutableStateOf(run.caption(ru)) }
    ArcadeScreen(
        scene = scene,
        dsl = gameDsl("The finger tilts the board. The ball stops on a wall and the green cell ends the run."),
        caption = caption,
        flash = { run.flash },
        mesh = { run.mesh },
    ) { dt, pointer ->
        val tiltY = if (pointer.serial() == 0) 0f else pointer.y
        run.step(dt, pointer.x, tiltY)
        caption = run.caption(ru)
    }
}

@Composable
internal fun DemoBreakout() {
    val scene = remember { playfield(0.02f, 0.02f, 0.04f) }
    val run = remember { BreakoutRun() }
    val ru = russian()
    var caption by remember(ru) { mutableStateOf(run.caption(ru)) }
    ArcadeScreen(
        scene = scene,
        dsl = gameDsl("The paddle follows the finger. A brick is removed on the frame the ball overlaps it."),
        caption = caption,
        flash = { run.flash },
        mesh = { run.mesh },
    ) { dt, pointer ->
        run.step(dt, pointer.x)
        caption = run.caption(ru)
    }
}

@Composable
internal fun DemoStrafe() {
    val scene = remember { playfield(0.01f, 0.012f, 0.03f) }
    val run = remember { StrafeRun() }
    val ru = russian()
    var caption by remember(ru) { mutableStateOf(run.caption(ru)) }
    ArcadeScreen(
        scene = scene,
        dsl = gameDsl("A tap fires down the lane under the finger. A foe in that lane flashes and leaves."),
        caption = caption,
        flash = { run.flash },
        mesh = { run.mesh },
    ) { dt, pointer ->
        run.step(dt, pointer.x, pointer.serial())
        caption = run.caption(ru)
    }
}

@Composable
internal fun DemoDescent() {
    val scene = remember { playfield(0.02f, 0.03f, 0.06f) }
    val run = remember { DescentRun() }
    val ru = russian()
    var caption by remember(ru) { mutableStateOf(run.caption(ru)) }
    ArcadeScreen(
        scene = scene,
        dsl = gameDsl("The skier follows the finger down the slope. A gate counts only when the skier is between the poles."),
        caption = caption,
        flash = { run.flash },
        mesh = { run.mesh },
    ) { dt, pointer ->
        run.step(dt, pointer.x)
        caption = run.caption(ru)
    }
}
