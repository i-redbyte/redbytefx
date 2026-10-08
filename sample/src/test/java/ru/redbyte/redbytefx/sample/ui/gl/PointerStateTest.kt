package ru.redbyte.redbytefx.sample.ui.gl

import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import org.junit.Assert.assertEquals
import org.junit.Test

class PointerStateTest {
    @Test
    fun coordinatesAndSerialBelongToTheSameEvent() {
        val pointer = PointerState()
        val executor = Executors.newSingleThreadExecutor()
        try {
            val writer = executor.submit {
                for (event in 1..50_000) pointer.mark(event.toFloat(), -event.toFloat())
            }
            while (!writer.isDone) {
                val touch = pointer.snapshot()
                assertEquals(touch.serial.toFloat(), touch.x, 0f)
                if (touch.serial != 0) assertEquals(-touch.x, touch.y, 0f)
            }
            writer.get(5, TimeUnit.SECONDS)
            assertEquals(50_000, pointer.snapshot().serial)
        } finally {
            executor.shutdownNow()
        }
    }

    @Test
    fun planetDragStartsAtTheLatestAnchor() {
        val rig = PlanetRig()
        rig.begin(0.2f, 0.1f)
        rig.advance(1f)
        rig.mark(0.3f, 0.2f)
        rig.advance(1.02f)
        assertEquals(0.35f + 0.1f * 2.4f, rig.yaw, 0.0001f)
        assertEquals(0.15f - 0.1f * 1.1f, rig.pitch, 0.0001f)
        rig.begin(-0.4f, -0.3f)
        rig.advance(1.04f)
        assertEquals(0.35f + 0.1f * 2.4f, rig.yaw, 0.0001f)
        rig.mark(-0.3f, -0.2f)
        rig.advance(1.06f)
        assertEquals(0.35f + 0.2f * 2.4f, rig.yaw, 0.0001f)
    }
}
