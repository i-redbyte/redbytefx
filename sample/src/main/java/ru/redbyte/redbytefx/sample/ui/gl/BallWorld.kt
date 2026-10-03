package ru.redbyte.redbytefx.sample.ui.gl

import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt

internal class BallKind(
    val radius: Float,
    val red: Float,
    val green: Float,
    val blue: Float,
)

internal val ballKinds = listOf(
    BallKind(0.20f, 0.96f, 0.28f, 0.36f),
    BallKind(0.15f, 0.25f, 0.78f, 0.98f),
    BallKind(0.22f, 0.98f, 0.72f, 0.22f),
    BallKind(0.13f, 0.62f, 0.36f, 0.98f),
    BallKind(0.17f, 0.20f, 0.88f, 0.55f),
    BallKind(0.16f, 0.98f, 0.45f, 0.78f),
    BallKind(0.14f, 0.95f, 0.55f, 0.20f),
    BallKind(0.19f, 0.35f, 0.95f, 0.85f),
    BallKind(0.12f, 0.92f, 0.92f, 0.95f),
    BallKind(0.18f, 0.55f, 0.85f, 0.25f),
    BallKind(0.15f, 0.95f, 0.35f, 0.25f),
    BallKind(0.21f, 0.45f, 0.35f, 0.98f),
)

internal class Ball(
    var x: Float,
    var y: Float,
    var z: Float,
    var vx: Float,
    var vy: Float,
    var vz: Float,
    val radius: Float,
    val mass: Float,
) {
    val invMass: Float = 1f / mass
}

internal object BallWorld {
    const val MIN = 1
    const val INITIAL = 6
    const val MAX = 12
    const val NEAR = 0.85f
    const val FAR = 2.4f

    fun spawn(): Array<Ball> = Array(INITIAL) { index -> placed(index) }

    fun pool(): Array<Ball> = Array(MAX) { index ->
        if (index < INITIAL) placed(index) else parked(index)
    }

    fun launch(ball: Ball, index: Int, peers: Array<Ball>, active: Int, aspect: Float) {
        val angle = index * 2.3999632f
        val speed = 0.45f + ballKinds[index].radius
        ball.x = cos(angle) * 0.16f
        ball.y = sin(angle) * 0.16f
        ball.z = 1.25f + (index % 4) * 0.22f
        ball.vz = if (index % 2 == 0) 0.28f else -0.24f
        repeat(8) {
            for (peer in 0 until active) {
                pushOut(ball, peers[peer])
            }
            contain(ball, aspect)
            contain(ball, aspect)
        }
        ball.vx = cos(angle) * speed
        ball.vy = sin(angle) * speed
    }

    fun step(balls: Array<Ball>, aspect: Float, dt: Float) {
        step(balls, balls.size, aspect, dt)
    }

    fun step(balls: Array<Ball>, active: Int, aspect: Float, dt: Float) {
        val count = active.coerceIn(0, balls.size)
        val slice = dt.coerceIn(0f, 0.05f) / SUBSTEPS
        repeat(SUBSTEPS) {
            for (index in 0 until count) {
                val ball = balls[index]
                ball.x += ball.vx * slice
                ball.y += ball.vy * slice
                ball.z += ball.vz * slice
                contain(ball, aspect)
                contain(ball, aspect)
            }
            collide(balls, count)
            for (index in 0 until count) {
                contain(balls[index], aspect)
            }
        }
    }

    private fun placed(index: Int): Ball {
        val pose = spawnPose[index]
        return ball(index, pose[0], pose[1], pose[2], pose[3], pose[4], pose[5])
    }

    private fun parked(index: Int): Ball = ball(index, 0f, 0f, FAR, 0f, 0f, 0f)

    private fun ball(
        index: Int,
        x: Float,
        y: Float,
        z: Float,
        vx: Float,
        vy: Float,
        vz: Float,
    ): Ball {
        val kind = ballKinds[index]
        return Ball(
            x = x,
            y = y,
            z = z,
            vx = vx,
            vy = vy,
            vz = vz,
            radius = kind.radius,
            mass = kind.radius * kind.radius * kind.radius,
        )
    }

    private const val SUBSTEPS = 4

    private val spawnPose = arrayOf(
        floatArrayOf(-0.12f, -0.42f, 1.25f, 0.72f, 0.38f, 0.22f),
        floatArrayOf(0.22f, -0.08f, 1.55f, -0.64f, 0.58f, -0.18f),
        floatArrayOf(-0.18f, 0.16f, 1.35f, 0.48f, -0.76f, 0.30f),
        floatArrayOf(0.10f, 0.48f, 1.80f, -0.70f, -0.36f, 0.16f),
        floatArrayOf(-0.28f, -0.12f, 1.95f, 0.55f, 0.62f, -0.34f),
        floatArrayOf(0.16f, -0.48f, 1.48f, -0.42f, -0.66f, 0.28f),
    )

    init {
        require(ballKinds.size == MAX) { "Ball catalog must contain $MAX kinds, was ${ballKinds.size}" }
        require(spawnPose.size == INITIAL) { "Spawn poses must contain $INITIAL entries, was ${spawnPose.size}" }
    }
}

internal fun Ball.inside(aspect: Float, slop: Float = 0.02f): Boolean {
    val safeAspect = if (aspect > 0.05f) aspect else 0.05f
    val hx = safeAspect * z - radius + slop
    val hy = z - radius + slop
    return z >= BallWorld.NEAR + radius - slop &&
        z <= BallWorld.FAR - radius + slop &&
        x >= -hx &&
        x <= hx &&
        y >= -hy &&
        y <= hy
}

private fun contain(ball: Ball, aspect: Float) {
    val safeAspect = if (aspect > 0.05f) aspect else 0.05f
    ball.hitWall(0f, 0f, -1f, BallWorld.NEAR + ball.radius - ball.z)
    ball.hitWall(0f, 0f, 1f, ball.z - (BallWorld.FAR - ball.radius))
    val hx = safeAspect * ball.z - ball.radius
    val hy = ball.z - ball.radius
    ball.hitWall(1f, 0f, -safeAspect, ball.x - hx)
    ball.hitWall(-1f, 0f, -safeAspect, -hx - ball.x)
    ball.hitWall(0f, 1f, -1f, ball.y - hy)
    ball.hitWall(0f, -1f, -1f, -hy - ball.y)
}

private fun collide(balls: Array<Ball>, count: Int) {
    for (i in 0 until count) {
        for (j in i + 1 until count) {
            separate(balls[i], balls[j])
        }
    }
}

private fun pushOut(ball: Ball, other: Ball) {
    val dx = ball.x - other.x
    val dy = ball.y - other.y
    val dz = ball.z - other.z
    val dist2 = dx * dx + dy * dy + dz * dz
    val minDist = ball.radius + other.radius
    if (dist2 >= minDist * minDist || dist2 < 1e-8f) return
    val dist = sqrt(dist2)
    val overlap = minDist - dist
    ball.x += dx / dist * overlap
    ball.y += dy / dist * overlap
    ball.z += dz / dist * overlap
}

private fun separate(first: Ball, second: Ball) {
    val dx = second.x - first.x
    val dy = second.y - first.y
    val dz = second.z - first.z
    val dist2 = dx * dx + dy * dy + dz * dz
    val minDist = first.radius + second.radius
    if (dist2 >= minDist * minDist || dist2 < 1e-8f) return
    val dist = sqrt(dist2)
    val nx = dx / dist
    val ny = dy / dist
    val nz = dz / dist
    val rel = (first.vx - second.vx) * nx + (first.vy - second.vy) * ny + (first.vz - second.vz) * nz
    if (rel > 0f) {
        val impulse = 2f * rel / (first.invMass + second.invMass)
        val firstScale = impulse * first.invMass
        val secondScale = impulse * second.invMass
        first.vx -= firstScale * nx
        first.vy -= firstScale * ny
        first.vz -= firstScale * nz
        second.vx += secondScale * nx
        second.vy += secondScale * ny
        second.vz += secondScale * nz
    }
    val overlap = minDist - dist
    val total = first.mass + second.mass
    val pushFirst = overlap * (second.mass / total)
    val pushSecond = overlap * (first.mass / total)
    first.x -= nx * pushFirst
    first.y -= ny * pushFirst
    first.z -= nz * pushFirst
    second.x += nx * pushSecond
    second.y += ny * pushSecond
    second.z += nz * pushSecond
}

private fun Ball.hitWall(gx: Float, gy: Float, gz: Float, plane: Float) {
    if (plane <= 0f) return
    val len2 = gx * gx + gy * gy + gz * gz
    if (len2 < 1e-8f) return
    val invLen = 1f / sqrt(len2)
    val ix = gx * invLen
    val iy = gy * invLen
    val iz = gz * invLen
    val outward = vx * ix + vy * iy + vz * iz
    if (outward > 0f) {
        val bounce = 2f * outward
        vx -= bounce * ix
        vy -= bounce * iy
        vz -= bounce * iz
    }
    val scale = plane / len2
    x -= gx * scale
    y -= gy * scale
    z -= gz * scale
}
