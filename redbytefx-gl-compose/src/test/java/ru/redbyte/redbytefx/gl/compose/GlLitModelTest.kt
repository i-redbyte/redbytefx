package ru.redbyte.redbytefx.gl.compose

import org.junit.Assert.assertTrue
import org.junit.Test
import ru.redbyte.redbytefx.gl.GlProgramRuntime
import ru.redbyte.redbytefx.scene.litTexturedMesh
import ru.redbyte.redbytefx.scene.rotationY

class GlLitModelTest {
    @Test
    fun updatingTheLitModelEveryFrameAllocatesNothing() {
        val lit = litTexturedMesh()
        val runtime = GlProgramRuntime(lit.program, FloatDevice()).also { it.link() }
        val model = FloatArray(16)
        var angle = 0f
        val frames = 10_000

        val bytes = allocatedBytes {
            repeat(frames) {
                angle += 0.01f
                rotationY(angle, model)
                runtime.setLitModel(lit, model)
            }
        }
        assertTrue("allocated $bytes bytes for $frames frames", bytes < frames)
    }
}
