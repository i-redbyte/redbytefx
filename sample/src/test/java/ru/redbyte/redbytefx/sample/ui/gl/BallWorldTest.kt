package ru.redbyte.redbytefx.sample.ui.gl

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.sqrt

class BallWorldTest {
    @Test
    fun headOnImpactExchangesTrajectories() {
        val left = ball(x = -0.15f, vx = 1.2f)
        val right = ball(x = 0.15f, vx = -1.2f)
        BallWorld.step(arrayOf(left, right), aspect = 1f, dt = 1f / 60f)
        assertTrue(left.vx < 0f)
        assertTrue(right.vx > 0f)
    }

    @Test
    fun glancingImpactPushesTheRestingBallOffAxis() {
        val moving = ball(x = -0.30f, y = 0f, vx = 2f)
        val resting = ball(x = 0.05f, y = 0.12f, vx = 0f, vy = 0f)
        BallWorld.step(arrayOf(moving, resting), aspect = 1f, dt = 1f / 60f)
        assertTrue(resting.vy > 0f)
        assertTrue(moving.vy < 0f)
    }

    @Test
    fun wallReversesTheOutwardVelocity() {
        val ball = ball(x = 0.9f, z = 1.5f, vx = 1f, radius = 0.2f)
        BallWorld.step(arrayOf(ball), aspect = 0.5f, dt = 1f / 60f)
        assertTrue(ball.vx < 0f)
        assertTrue(ball.inside(0.5f, slop = 0.05f))
    }

    @Test
    fun spawnedBallsStayInsideTheScreenBox() {
        val balls = BallWorld.spawn()
        val aspect = 0.46f
        balls.forEach { ball -> assertTrue(ball.inside(aspect, slop = 0.02f)) }
        for (i in balls.indices) {
            for (j in i + 1 until balls.size) {
                val distance = distance(balls[i], balls[j])
                assertTrue(distance > balls[i].radius + balls[j].radius)
            }
        }
        repeat(360) { BallWorld.step(balls, aspect, 1f / 60f) }
        balls.forEach { ball -> assertTrue(ball.inside(aspect, slop = 0.08f)) }
    }

    @Test
    fun sphereShaderNamesEveryBall() {
        val program = ballProgram()
        val fragment = program.program.fragmentSource()
        val vertex = program.program.vertexSource()
        assertTrue(vertex.contains("gl_Position"))
        assertTrue(fragment.contains("u_aspect"))
        assertTrue(fragment.contains("u_count"))
        assertTrue(fragment.contains("u_c0x"))
        assertTrue(fragment.contains("u_c11z"))
        assertTrue(fragment.length < 80_000)
    }

    @Test
    fun launchedBallAppearsInsideAndHiddenBallStaysPut() {
        val balls = BallWorld.pool()
        val hiddenX = balls[8].x
        val hiddenZ = balls[8].z
        BallWorld.launch(balls[6], 6, balls, 6, 0.46f)
        assertTrue(balls[6].inside(0.46f, slop = 0.05f))
        BallWorld.step(balls, active = 6, aspect = 0.46f, dt = 1f / 60f)
        assertEquals(hiddenX, balls[8].x, 0f)
        assertEquals(hiddenZ, balls[8].z, 0f)
    }

    private fun ball(
        x: Float = 0f,
        y: Float = 0f,
        z: Float = 1.5f,
        vx: Float = 0f,
        vy: Float = 0f,
        vz: Float = 0f,
        radius: Float = 0.2f,
    ) = Ball(
        x = x,
        y = y,
        z = z,
        vx = vx,
        vy = vy,
        vz = vz,
        radius = radius,
        mass = radius * radius * radius,
    )

    private fun distance(first: Ball, second: Ball): Float {
        val dx = first.x - second.x
        val dy = first.y - second.y
        val dz = first.z - second.z
        return sqrt(dx * dx + dy * dy + dz * dz)
    }
}
