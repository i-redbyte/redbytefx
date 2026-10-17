package ru.redbyte.redbytefx.gl.compose

import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Test

class MeshSwitchTest {
    private val first = screenMesh()
    private val second = screenMesh(clearR = 1f)

    @Test
    fun theInitialMeshIsCurrentWithoutAChange() {
        val switch = MeshSwitch(first) { first }
        assertNull(switch.takeChange())
        assertSame(first, switch.current)
    }

    @Test
    fun aMeshFromALaterCompositionIsTakenOnce() {
        var latest = first
        val switch = MeshSwitch(first) { latest }
        latest = second

        assertSame(second, switch.takeChange())
        assertNull(switch.takeChange())
        assertSame(second, switch.current)
    }

    @Test
    fun anEqualButDistinctMeshIsStillAChange() {
        val twin = screenMesh()
        var latest = first
        val switch = MeshSwitch(first) { latest }
        latest = twin

        assertSame(twin, switch.takeChange())
    }
}
