package ru.redbyte.redbytefx.sample.ui.gl

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class ArcadeTest {
    @Test
    fun tunnelCountsARingOnlyInsideTheHole() {
        val inside = TunnelRun()
        repeat(30) { inside.step(0.05f, 0f) }
        assertTrue(inside.score >= 1)
        assertEquals(0f, inside.flash, 0.001f)
        assertTrue(inside.mesh.isNotEmpty())

        val outside = TunnelRun()
        var steps = 0
        while (outside.score == 0 && outside.flash == 0f && steps < 80) {
            outside.step(0.05f, 1f)
            steps += 1
        }
        assertEquals(0, outside.score)
        assertTrue(outside.flash > 0f)
        assertTrue(abs(outside.ship) > 0.36f)
    }

    @Test
    fun mazeStopsOnAWallAndEndsOnTheExit() {
        val maze = MazeRun()
        val start = maze.x
        maze.step(0.2f, -1f, 0f)
        assertTrue(maze.x > 1.15f)
        assertTrue(maze.x <= start)
        assertFalse(maze.fits(0.5f, 0.5f))
        assertTrue(maze.fits(1.5f, 1.5f))

        maze.x = 7.5f
        maze.z = 7.5f
        maze.step(0.016f, 0f, 0f)
        assertTrue(maze.won)
        assertEquals("Выход найден", maze.caption(russian = true))
        assertEquals("Exit reached", maze.caption(russian = false))
    }

    @Test
    fun mazeCameraSeesTheBallFirstThroughTheScreenCenter() {
        val mesh = MazeRun().mesh
        var nearest = Float.MAX_VALUE
        var blueOverRed = 1f
        var index = 0
        while (index < mesh.size) {
            val hit = centerRayHit(mesh, index)
            if (hit >= 0f && hit < nearest) {
                nearest = hit
                blueOverRed = mesh[index + 6] / mesh[index + 4]
            }
            index += 24
        }
        assertTrue(nearest < Float.MAX_VALUE)
        assertTrue(blueOverRed < 0.3f)
    }

    @Test
    fun aPitchedPenPutsThePointOnItsAxisAtTheScreenCenter() {
        val pen = WorldPen(0f, 4f, -3f, pitch = kotlin.math.atan2(4f, 3f))
        pen.box(0f, 0f, 0f, 0.1f, 0.1f, 0.1f, 1f, 1f, 1f)
        val mesh = pen.toArray()
        var sumY = 0f
        var vertices = 0
        var index = 0
        while (index < mesh.size) {
            assertTrue(mesh[index + 2] > 4.5f)
            sumY += mesh[index + 1]
            vertices += 1
            index += 8
        }
        assertEquals(0f, sumY / vertices, 0.05f)
    }

    @Test
    fun reusedPenPublishesIndependentFrameSnapshots() {
        val pen = WorldPen()
        pen.box(0f, 0f, 1f, 0.1f, 0.1f, 0.1f, 1f, 0f, 0f)
        val first = pen.toArray()
        assertTrue(first.isNotEmpty())
        val originalX = first[0]
        pen.reset(1f, 0f, 0f)
        assertEquals(0, pen.toArray().size)
        pen.box(0f, 0f, 1f, 0.1f, 0.1f, 0.1f, 0f, 1f, 0f)
        val second = pen.toArray()
        assertEquals(originalX, first[0], 0f)
        assertTrue(second[0] < first[0])
        assertTrue(first[4] > 0f)
        assertEquals(0f, first[5], 0f)
        assertTrue(second[5] > 0f)
        assertEquals(0f, second[4], 0f)
    }

    private fun centerRayHit(mesh: FloatArray, at: Int): Float {
        val ax = mesh[at]
        val ay = mesh[at + 1]
        val az = mesh[at + 2]
        val e1x = mesh[at + 8] - ax
        val e1y = mesh[at + 9] - ay
        val e1z = mesh[at + 10] - az
        val e2x = mesh[at + 16] - ax
        val e2y = mesh[at + 17] - ay
        val e2z = mesh[at + 18] - az
        val px = -e2y
        val py = e2x
        val det = e1x * px + e1y * py
        if (abs(det) < 1e-7f) return -1f
        val u = (-ax * px - ay * py) / det
        val qx = -ay * e1z + az * e1y
        val qy = -az * e1x + ax * e1z
        val qz = -ax * e1y + ay * e1x
        val v = qz / det
        if (u < 0f || v < 0f || u + v > 1f) return -1f
        return (e2x * qx + e2y * qy + e2z * qz) / det
    }

    @Test
    fun breakoutRemovesTheBrickTheBallHits() {
        val game = BreakoutRun()
        game.ballX = -0.91f
        game.ballY = 1.16f
        game.vy = 2f
        game.step(0.02f, 0f)
        assertFalse(game.alive[0])
        assertEquals(1, game.score)
        assertTrue(game.vy < 0f)
        assertTrue(game.mesh.isNotEmpty())
    }

    @Test
    fun raidShotRemovesAFoeInTheLane() {
        val hit = StrafeRun()
        hit.foes += StrafeRun.Foe(0f, 0f, 5f)
        hit.fire(0f)
        hit.step(0.01f, 0f, serial = 0)
        assertEquals(1, hit.score)
        assertTrue(hit.foes.none { abs(it.z - 5f) < 0.01f && abs(it.x) < 0.01f })

        val miss = StrafeRun()
        miss.foes += StrafeRun.Foe(0.8f, 0f, 5f)
        miss.fire(0f)
        miss.step(0.01f, 0f, serial = 0)
        assertEquals(0, miss.score)
        assertEquals(1, miss.foes.count { abs(it.x - 0.8f) < 0.01f })
    }

    @Test
    fun descentCountsAGateOnlyBetweenThePoles() {
        val through = DescentRun()
        var passed = 0
        while (through.score == 0 && passed < 80) {
            through.step(0.05f, 0f)
            passed += 1
        }
        assertTrue(through.score >= 1)
        assertEquals(0f, through.flash, 0.001f)

        val wide = DescentRun()
        wide.skier = 0.8f
        var missed = 0
        while (wide.score == 0 && wide.flash == 0f && missed < 80) {
            wide.step(0.05f, 1f)
            missed += 1
        }
        assertEquals(0, wide.score)
        assertTrue(wide.flash > 0f)
    }
}
